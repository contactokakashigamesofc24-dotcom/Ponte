package com.luizfernando.ponte

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.CreateRoomParameters
import org.matrix.rustcomponents.sdk.Membership
import org.matrix.rustcomponents.sdk.RoomPreset
import org.matrix.rustcomponents.sdk.RoomVisibility
import org.matrix.rustcomponents.sdk.MessageType
import org.matrix.rustcomponents.sdk.Room
import org.matrix.rustcomponents.sdk.RoomMessageEventContentWithoutRelation
import org.matrix.rustcomponents.sdk.Session
import org.matrix.rustcomponents.sdk.SessionVerificationController
import org.matrix.rustcomponents.sdk.SessionVerificationControllerDelegate
import org.matrix.rustcomponents.sdk.SessionVerificationData
import org.matrix.rustcomponents.sdk.SessionVerificationRequestDetails
import org.matrix.rustcomponents.sdk.VerificationState
import org.matrix.rustcomponents.sdk.SsoHandler
import org.matrix.rustcomponents.sdk.SlidingSyncVersion
import org.matrix.rustcomponents.sdk.SlidingSyncVersionBuilder
import org.matrix.rustcomponents.sdk.SqliteStoreBuilder
import org.matrix.rustcomponents.sdk.TextMessageContent
import org.matrix.rustcomponents.sdk.Timeline
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItem
import org.matrix.rustcomponents.sdk.TimelineItemContent
import org.matrix.rustcomponents.sdk.MsgLikeKind
import org.matrix.rustcomponents.sdk.TimelineListener
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class PonteRoom(val id: String, val title: String)
internal data class PonteMessage(val id: String, val sender: String, val body: String, val mine: Boolean)
internal data class VerificationPrompt(
    val status: String,
    val words: List<String> = emptyList(),
    val numbers: List<Int> = emptyList(),
    val finished: Boolean = false
)

/** Holds the Matrix SDK session in an Android Keystore-encrypted preferences value. */
private class SecureSessionVault(context: Context) {
    private val prefs = context.getSharedPreferences("ponte_secure_session", Context.MODE_PRIVATE)
    private val alias = "PonteMatrixSessionKeyV1"

    fun read(): JSONObject? {
        val packed = prefs.getString("encrypted", null) ?: return null
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, 12)
            val ciphertext = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    fun write(json: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("encrypted", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit())
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}

internal class MatrixRepository(context: Context) {
    private val app = context.applicationContext
    private val vault = SecureSessionVault(app)
    private var client: Client? = null
    private var verificationController: SessionVerificationController? = null
    private var verificationDelegate: SessionVerificationControllerDelegate? = null
    private val verificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingSsoClient: Client? = null
    private var pendingSsoHandler: SsoHandler? = null
    private var pendingSsoDbKey: ByteArray? = null
    private val dataDirectory = app.filesDir.resolve("matrix-store").apply { mkdirs() }
    private val cacheDirectory = app.cacheDir.resolve("matrix-cache").apply { mkdirs() }

    suspend fun beginSsoLogin(homeserver: String): String = withContext(Dispatchers.IO) {
        closeClient()
        closePendingSso()
        val normalizedServer = normalizeHomeserver(homeserver)
        val dbKey = ByteArray(32).also(SecureRandom()::nextBytes)
        val built = newBuilder(normalizedServer, dbKey).build()
        try {
            val handler = built.startSsoLogin("ponte://login", null)
            pendingSsoClient = built
            pendingSsoHandler = handler
            pendingSsoDbKey = dbKey
            handler.url()
        } catch (t: Throwable) {
            built.destroy()
            throw t
        }
    }

    suspend fun finishSsoLogin(callbackUrl: String): String = withContext(Dispatchers.IO) {
        require(callbackUrl.startsWith("ponte://login", ignoreCase = true)) {
            "O retorno do navegador não corresponde ao login do Ponte."
        }
        val active = pendingSsoClient ?: error("A etapa de login expirou. Inicie o acesso pelo navegador novamente.")
        val handler = pendingSsoHandler ?: error("O login pelo navegador não está mais ativo. Tente novamente.")
        val dbKey = pendingSsoDbKey ?: error("A chave temporária do login não está disponível. Tente novamente.")
        try {
            handler.finish(callbackUrl)
            val session = active.session() ?: error("O servidor não devolveu uma sessão Matrix após a autenticação.")
            vault.write(sessionJson(session).put("db_key", Base64.encodeToString(dbKey, Base64.NO_WRAP)))
            handler.close()
            pendingSsoHandler = null
            pendingSsoClient = null
            pendingSsoDbKey = null
            startSync(active)
            client = active
            session.userId
        } catch (t: Throwable) {
            runCatching { handler.close() }
            runCatching { active.destroy() }
            pendingSsoHandler = null
            pendingSsoClient = null
            pendingSsoDbKey = null
            throw t
        }
    }

    suspend fun login(homeserver: String, user: String, password: String): String = withContext(Dispatchers.IO) {
        closePendingSso()
        closeClient()
        val normalizedServer = normalizeHomeserver(homeserver)
        val dbKey = ByteArray(32).also(SecureRandom()::nextBytes)
        val built = newBuilder(normalizedServer, dbKey).build()
        try {
            built.login(user.trim(), password, "Ponte Android", null)
            val session = built.session()
            vault.write(sessionJson(session).put("db_key", Base64.encodeToString(dbKey, Base64.NO_WRAP)))
            startSync(built)
            client = built
            session.userId
        } catch (t: Throwable) {
            built.destroy()
            dataDirectory.deleteRecursively()
            cacheDirectory.deleteRecursively()
            dataDirectory.mkdirs()
            cacheDirectory.mkdirs()
            throw t
        }
    }

    suspend fun restore(): String? = withContext(Dispatchers.IO) {
        if (client != null) return@withContext client?.userId()
        val saved = vault.read() ?: return@withContext null
        val dbKeyText = saved.optString("db_key")
        if (dbKeyText.isBlank()) return@withContext null
        val dbKey = Base64.decode(dbKeyText, Base64.NO_WRAP)
        val session = sessionFromJson(saved)
        val active = newBuilder(session.homeserverUrl, dbKey).build()
        try {
            active.restoreSession(session)
            startSync(active)
            client = active
            session.userId
        } catch (t: Throwable) {
            active.destroy()
            throw t
        }
    }

    suspend fun needsSecureMessagingSetup(): Boolean = withContext(Dispatchers.IO) {
        val active = client ?: return@withContext false
        val encryption = active.encryption()
        try {
            encryption.waitForE2eeInitializationTasks()
            encryption.verificationState() != VerificationState.VERIFIED
        } finally {
            encryption.destroy()
        }
    }

    suspend fun requestDeviceVerification(onUpdate: (VerificationPrompt) -> Unit) = withContext(Dispatchers.IO) {
        val active = client ?: error("A conta Matrix não está conectada.")
        clearVerificationController()
        val controller = active.getSessionVerificationController()
        verificationController = controller
        val notify = { prompt: VerificationPrompt -> onUpdate(prompt) }
        val delegate = object : SessionVerificationControllerDelegate {
            override fun didReceiveVerificationRequest(details: SessionVerificationRequestDetails) {
                notify(VerificationPrompt("Pedido de confirmação recebido de outro dispositivo."))
            }
            override fun didAcceptVerificationRequest() {
                notify(VerificationPrompt("O outro dispositivo aceitou. Preparando os códigos de comparação…"))
                verificationScope.launch {
                    try { controller.startSasVerification() }
                    catch (t: Throwable) { notify(VerificationPrompt("Não foi possível iniciar a comparação: ${t.message ?: t.javaClass.simpleName}")) }
                }
            }
            override fun didStartSasVerification() {
                notify(VerificationPrompt("Comparando os códigos com o outro dispositivo…"))
            }
            override fun didReceiveVerificationData(data: SessionVerificationData) {
                try {
                    when (data) {
                        is SessionVerificationData.Emojis -> {
                            val labels = data.emojis.map { "${it.symbol()} ${it.description()}" }
                            data.emojis.forEach { runCatching { it.destroy() } }
                            notify(VerificationPrompt("Compare estes símbolos com o que aparece no outro dispositivo. Só confirme se forem iguais.", words = labels))
                        }
                        is SessionVerificationData.Decimals -> notify(VerificationPrompt("Compare estes números com o que aparece no outro dispositivo. Só confirme se forem iguais.", numbers = data.values.map { it.toInt() }))
                    }
                } finally {
                    runCatching { data.destroy() }
                }
            }
            override fun didFail() { notify(VerificationPrompt("A verificação falhou. Tente iniciar novamente.")) }
            override fun didCancel() { notify(VerificationPrompt("A verificação foi cancelada.")) }
            override fun didFinish() {
                verificationScope.launch {
                    val verified = runCatching { needsSecureMessagingSetup().not() }.getOrDefault(false)
                    notify(VerificationPrompt(if (verified) "Este dispositivo foi confirmado. As mensagens seguras estão configuradas." else "A confirmação terminou, mas o estado da identidade ainda não foi atualizado." , finished = verified))
                }
            }
        }
        verificationDelegate = delegate
        controller.setDelegate(delegate)
        controller.requestDeviceVerification()
        notify(VerificationPrompt("Pedido enviado. Abra o Element X ou outro dispositivo Matrix verificado e aceite a confirmação do Ponte."))
    }

    suspend fun approveDeviceVerification() = withContext(Dispatchers.IO) {
        val controller = verificationController ?: error("Não há confirmação ativa.")
        controller.approveVerification()
    }

    suspend fun declineDeviceVerification() = withContext(Dispatchers.IO) {
        val controller = verificationController ?: error("Não há confirmação ativa.")
        controller.declineVerification()
    }

    suspend fun cancelDeviceVerification() = withContext(Dispatchers.IO) {
        verificationController?.let { runCatching { it.cancelVerification() } }
        clearVerificationController()
    }

    suspend fun recoverSecureMessaging(recoveryKey: String) = withContext(Dispatchers.IO) {
        val active = client ?: error("A conta Matrix não está conectada.")
        require(recoveryKey.isNotBlank()) { "Digite a chave de recuperação." }
        val encryption = active.encryption()
        try {
            encryption.recover(recoveryKey.trim())
            encryption.waitForE2eeInitializationTasks()
            encryption.verificationState() == VerificationState.VERIFIED
        } finally {
            encryption.destroy()
        }
    }

    private fun clearVerificationController() {
        runCatching { verificationController?.setDelegate(null) }
        runCatching { verificationController?.close() }
        verificationController = null
        verificationDelegate = null
    }

    suspend fun createPrivateTestRoom(): String = withContext(Dispatchers.IO) {
        val active = client ?: error("Entre na conta Matrix antes de criar uma sala.")
        val roomId = active.createRoom(
            CreateRoomParameters(
                name = "Anotações",
                isEncrypted = true,
                visibility = RoomVisibility.Private,
                preset = RoomPreset.PRIVATE_CHAT
            )
        )
        active.awaitRoomRemoteEcho(roomId).use { }
        roomId
    }

    suspend fun startDirectChat(matrixUserId: String): String = withContext(Dispatchers.IO) {
        val invitee = matrixUserId.trim()
        require(invitee.startsWith("@") && invitee.contains(":")) { "Digite o ID completo, por exemplo @nome:matrix.org." }
        val active = client ?: error("Entre na conta Matrix antes de iniciar uma conversa.")
        val roomId = active.createRoom(
            CreateRoomParameters(
                name = invitee,
                isEncrypted = true,
                isDirect = true,
                visibility = RoomVisibility.Private,
                preset = RoomPreset.PRIVATE_CHAT,
                invite = listOf(invitee)
            )
        )
        active.awaitRoomRemoteEcho(roomId).use { }
        roomId
    }

    suspend fun rooms(): List<PonteRoom> = withContext(Dispatchers.IO) {
        val active = client ?: return@withContext emptyList()
        active.rooms().mapNotNull { room ->
            try {
                if (room.membership() != Membership.JOINED || room.isSpace()) return@mapNotNull null
                PonteRoom(room.id(), room.displayName()?.takeIf { it.isNotBlank() } ?: room.id())
            } finally {
                room.destroy()
            }
        }.sortedBy { it.title.lowercase() }
    }

    suspend fun loadMessages(roomId: String, onMessages: (List<PonteMessage>) -> Unit): MessageSubscription = withContext(Dispatchers.IO) {
        val room = client?.getRoom(roomId) ?: error("A conversa não está disponível nesta sessão.")
        val timeline = room.timeline()
        val current = mutableListOf<PonteMessage>()
        val listener = object : TimelineListener {
            override fun onUpdate(diff: List<TimelineDiff>) {
                synchronized(current) {
                    for (change in diff) {
                        when (change) {
                            is TimelineDiff.Append -> change.values.forEach { addTimelineItem(current, it) }
                            is TimelineDiff.PushBack -> addTimelineItem(current, change.value)
                            is TimelineDiff.PushFront -> addTimelineItem(current, change.value)
                            is TimelineDiff.Insert -> addTimelineItem(current, change.value)
                            is TimelineDiff.Set -> setTimelineItem(current, change.index.toInt(), change.value)
                            is TimelineDiff.Remove -> if (change.index.toInt() in current.indices) current.removeAt(change.index.toInt())
                            is TimelineDiff.Clear -> current.clear()
                            is TimelineDiff.PopBack -> if (current.isNotEmpty()) current.removeAt(current.lastIndex)
                            is TimelineDiff.PopFront -> if (current.isNotEmpty()) current.removeAt(0)
                            is TimelineDiff.Truncate -> while (current.size > change.length.toInt()) current.removeAt(current.lastIndex)
                            is TimelineDiff.Reset -> { current.clear(); change.values.forEach { addTimelineItem(current, it) } }
                        }
                    }
                    onMessages(current.toList())
                }
            }
        }
        val handle = timeline.addListener(listener)
        MessageSubscription(room, timeline, handle)
    }

    suspend fun sendText(roomId: String, text: String) = withContext(Dispatchers.IO) {
        val room = client?.getRoom(roomId) ?: error("A conversa não está disponível.")
        try {
            val timeline = room.timeline()
            try {
                val content: RoomMessageEventContentWithoutRelation = timeline.createMessageContent(
                    MessageType.Text(TextMessageContent(text, null))
                ) ?: error("Não foi possível preparar a mensagem.")
                content.use {
                    timeline.send(it)
                }
            } finally {
                timeline.destroy()
            }
        } finally {
            room.destroy()
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        clearVerificationController()
        closePendingSso()
        client?.let { active ->
            try { active.logout() } catch (_: Exception) { }
        }
        closeClient()
        vault.clear()
        dataDirectory.deleteRecursively()
        cacheDirectory.deleteRecursively()
        dataDirectory.mkdirs()
        cacheDirectory.mkdirs()
    }

    fun close() {
        verificationScope.cancel()
        clearVerificationController()
        closePendingSso()
        client?.destroy()
        client = null
    }

    private fun closePendingSso() {
        runCatching { pendingSsoHandler?.close() }
        pendingSsoHandler = null
        runCatching { pendingSsoClient?.destroy() }
        pendingSsoClient = null
        pendingSsoDbKey = null
    }

    private fun closeClient() {
        client?.destroy()
        client = null
    }

    private fun newBuilder(homeserver: String, databaseKey: ByteArray?): ClientBuilder {
        val builder = ClientBuilder()
            .serverNameOrHomeserverUrl(homeserver)
            .slidingSyncVersionBuilder(SlidingSyncVersionBuilder.DISCOVER_NATIVE)
            .sessionPaths(dataDirectory.absolutePath, cacheDirectory.absolutePath)
            .autoEnableBackups(true)
            .autoEnableCrossSigning(true)
            .enableAutomaticBackPagination(true)
        val storeBuilder = SqliteStoreBuilder(dataDirectory.absolutePath, cacheDirectory.absolutePath)
        if (databaseKey != null) {
            storeBuilder.key(databaseKey)
        }
        return builder.sqliteStore(storeBuilder)
    }

    private suspend fun startSync(active: Client) {
        active.syncService().finish().start()
    }

    private fun normalizeHomeserver(input: String): String {
        val value = input.trim().ifBlank { "matrix.org" }
        return when {
            value.startsWith("https://") -> value.trimEnd('/')
            value.startsWith("http://") -> throw IllegalArgumentException("Por segurança, o servidor precisa usar HTTPS.")
            else -> "https://${value.trimEnd('/')}"
        }
    }

    private fun sessionJson(session: Session) = JSONObject()
        .put("access_token", session.accessToken)
        .put("refresh_token", session.refreshToken)
        .put("user_id", session.userId)
        .put("device_id", session.deviceId)
        .put("homeserver_url", session.homeserverUrl)
        .put("oauth_data", session.oauthData)
        .put("sliding_sync_version", session.slidingSyncVersion.name)

    private fun sessionFromJson(json: JSONObject) = Session(
        accessToken = json.getString("access_token"),
        refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
        userId = json.getString("user_id"),
        deviceId = json.getString("device_id"),
        homeserverUrl = json.getString("homeserver_url"),
        oauthData = json.optString("oauth_data").takeIf { it.isNotBlank() },
        slidingSyncVersion = runCatching { SlidingSyncVersion.valueOf(json.optString("sliding_sync_version", "NONE")) }.getOrDefault(SlidingSyncVersion.NONE)
    )

    private fun addTimelineItem(target: MutableList<PonteMessage>, item: TimelineItem) {
        try {
            val event = item.asEvent() ?: return
            try {
                val content = event.content as? TimelineItemContent.MsgLike ?: return
                val kind = content.content.kind as? MsgLikeKind.Message ?: return
                val message = kind.content
                target.removeAll { it.id == event.eventOrTransactionId.toString() }
                target.add(PonteMessage(event.eventOrTransactionId.toString(), ((event.senderProfile as? org.matrix.rustcomponents.sdk.ProfileDetails.Ready)?.displayName) ?: event.sender, message.body, event.isOwn))
            } finally {
                event.destroy()
            }
        } finally {
            item.destroy()
        }
    }

    private fun setTimelineItem(target: MutableList<PonteMessage>, index: Int, item: TimelineItem) {
        val before = target.size
        addTimelineItem(target, item)
        if (before == target.size && index in target.indices) {
            // Non-message items are intentionally omitted from this first text-only prototype.
        }
    }
}

internal class MessageSubscription(
    private val room: Room,
    private val timeline: Timeline,
    private val handle: org.matrix.rustcomponents.sdk.TaskHandle
) {
    fun close() {
        try { handle.destroy() } catch (_: Exception) { }
        try { timeline.destroy() } catch (_: Exception) { }
        try { room.destroy() } catch (_: Exception) { }
    }
}
