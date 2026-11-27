package com.luizfernando.ponte

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF172436)
private val Blue = Color(0xFF4467D8)
private val Pale = Color(0xFFF5F7FC)
private val Muted = Color(0xFF697586)
private val MainHandler = Handler(Looper.getMainLooper())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Pale) {
                    PonteApp(
                        repository = remember { MatrixRepository(applicationContext) },
                        onOpenRegistration = {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://account.matrix.org/register?kind=manage_account")))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PonteApp(repository: MatrixRepository, onOpenRegistration: () -> Unit) {
    val scope = rememberCoroutineScope()
    var userId by remember { mutableStateOf<String?>(null) }
    var pageLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var activeRoom by remember { mutableStateOf<PonteRoom?>(null) }
    val rooms = remember { mutableStateListOf<PonteRoom>() }
    val messages = remember { mutableStateListOf<PonteMessage>() }
    var subscription by remember { mutableStateOf<MessageSubscription?>(null) }

    DisposableEffect(repository) {
        onDispose {
            subscription?.close()
            repository.close()
        }
    }

    LaunchedEffect(repository) {
        try {
            userId = repository.restore()
        } catch (t: Throwable) {
            error = userFacingError(t)
        } finally {
            pageLoading = false
        }
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            while (true) {
                try {
                    val latest = repository.rooms()
                    rooms.clear()
                    rooms.addAll(latest)
                } catch (t: Throwable) {
                    error = userFacingError(t)
                }
                delay(4_000)
            }
        } else {
            rooms.clear()
        }
    }

    LaunchedEffect(activeRoom?.id) {
        subscription?.close()
        subscription = null
        messages.clear()
        val room = activeRoom ?: return@LaunchedEffect
        try {
            subscription = repository.loadMessages(room.id) { updated ->
                MainHandler.post {
                    messages.clear()
                    messages.addAll(updated)
                }
            }
        } catch (t: Throwable) {
            error = userFacingError(t)
        }
    }

    if (pageLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Blue) }
        return
    }
    when {
        activeRoom != null -> ChatScreen(
            room = activeRoom!!,
            messages = messages,
            onBack = { activeRoom = null },
            onSend = { text ->
                scope.launch {
                    error = null
                    try { repository.sendText(activeRoom!!.id, text) }
                    catch (t: Throwable) { error = userFacingError(t) }
                }
            },
            error = error
        )
        userId != null -> RoomsScreen(
            userId = userId!!,
            rooms = rooms,
            error = error,
            onOpenRoom = { activeRoom = it },
            onCreateTestRoom = {
                scope.launch {
                    error = null
                    try {
                        val roomId = repository.createPrivateTestRoom()
                        activeRoom = PonteRoom(roomId, "Anotações")
                    } catch (t: Throwable) { error = userFacingError(t) }
                }
            },
            onStartDirectChat = { matrixId ->
                scope.launch {
                    error = null
                    try {
                        val roomId = repository.startDirectChat(matrixId)
                        activeRoom = PonteRoom(roomId, matrixId)
                    } catch (t: Throwable) { error = userFacingError(t) }
                }
            },
            onLogout = {
                scope.launch {
                    pageLoading = true
                    try {
                        subscription?.close(); subscription = null
                        repository.logout()
                        userId = null
                        activeRoom = null
                        rooms.clear(); messages.clear(); error = null
                    } catch (t: Throwable) { error = userFacingError(t) }
                    finally { pageLoading = false }
                }
            }
        )
        else -> LoginScreen(
            error = error,
            onClearError = { error = null },
            onLogin = { server, user, password ->
                scope.launch {
                    pageLoading = true
                    error = null
                    try {
                        userId = repository.login(server, user, password)
                    } catch (t: Throwable) {
                        error = userFacingError(t)
                    } finally {
                        pageLoading = false
                    }
                }
            },
            onRegister = onOpenRegistration
        )
    }
}

@Composable
private fun LoginScreen(
    error: String?,
    onClearError: () -> Unit,
    onLogin: (String, String, String) -> Unit,
    onRegister: () -> Unit
) {
    var server by remember { mutableStateOf("matrix.org") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(80.dp).background(Blue, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Ponte", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Ink)
        Text("Mensagens pela rede Matrix", color = Muted)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = server,
            onValueChange = { server = it; onClearError() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Servidor Matrix") },
            placeholder = { Text("matrix.org") },
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = user,
            onValueChange = { user = it; onClearError() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Usuário Matrix") },
            placeholder = { Text("@nome:matrix.org ou nome") },
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onClearError() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Senha da conta Matrix") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color(0xFFB3261E), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onLogin(server, user, password) },
            enabled = server.isNotBlank() && user.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) { Text("Entrar", fontWeight = FontWeight.SemiBold) }
        TextButton(onClick = onRegister) { Text("Criar uma conta Matrix", color = Blue) }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Lock, null, tint = Blue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(10.dp))
                Text(
                    "Use uma conta Matrix, não sua senha do Gmail. O token da sessão será guardado criptografado no Android.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RoomsScreen(
    userId: String,
    rooms: List<PonteRoom>,
    error: String?,
    onOpenRoom: (PonteRoom) -> Unit,
    onCreateTestRoom: () -> Unit,
    onStartDirectChat: (String) -> Unit,
    onLogout: () -> Unit
) {
    var showNewChat by remember { mutableStateOf(false) }
    var matrixId by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Ponte", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(userId, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, contentDescription = "Sair", tint = Muted) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCreateTestRoom, modifier = Modifier.weight(1f)) { Text("Criar Anotações") }
            TextButton(onClick = { showNewChat = true }, modifier = Modifier.weight(1f)) { Text("Nova conversa") }
        }
        if (!error.isNullOrBlank()) Text(error, Modifier.padding(16.dp), color = Color(0xFFB3261E), style = MaterialTheme.typography.bodySmall)
        if (rooms.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).background(Color(0xFFE9EEFF), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChatBubbleOutline, null, tint = Blue, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Ainda não há salas nesta conta.", color = Ink, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Toque em “Criar Anotações” para testar uma conversa consigo mesmo, ou “Nova conversa” para convidar outra conta Matrix.", color = Muted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                items(rooms, key = { it.id }) { room ->
                    Card(
                        onClick = { onOpenRoom(room) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).background(Color(0xFFE9EEFF), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.ChatBubbleOutline, null, tint = Blue)
                            }
                            Spacer(Modifier.size(14.dp))
                            Column {
                                Text(room.title, color = Ink, fontWeight = FontWeight.SemiBold)
                                Text("Toque para abrir a conversa", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showNewChat) {
        AlertDialog(
            onDismissRequest = { showNewChat = false },
            title = { Text("Nova conversa Matrix") },
            text = {
                Column {
                    Text("Digite o ID Matrix completo da outra pessoa, por exemplo @nome:matrix.org. Ela precisa ter uma conta Matrix.", color = Muted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = matrixId,
                        onValueChange = { matrixId = it },
                        label = { Text("ID Matrix") },
                        placeholder = { Text("@nome:matrix.org") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = matrixId.trim().startsWith("@") && matrixId.contains(":"),
                    onClick = {
                        val id = matrixId.trim()
                        showNewChat = false
                        onStartDirectChat(id)
                    }
                ) { Text("Convidar") }
            },
            dismissButton = { TextButton(onClick = { showNewChat = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ChatScreen(
    room: PonteRoom,
    messages: List<PonteMessage>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    error: String?
) {
    var draft by remember(room.id) { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Ink) }
            Column {
                Text(room.title, color = Ink, fontWeight = FontWeight.Bold)
                Text("Matrix", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!error.isNullOrBlank()) Text(error, Modifier.padding(horizontal = 14.dp, vertical = 6.dp), color = Color(0xFFB3261E), style = MaterialTheme.typography.bodySmall)
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Ainda não há mensagens para mostrar.", color = Muted)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), reverseLayout = true) {
                items(messages.asReversed(), key = { it.id }) { message ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (message.mine) Color(0xFFDDE5FF) else Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.88f)
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                if (!message.mine) Text(message.sender, color = Blue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                Text(message.body, color = Ink)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensagem") },
                maxLines = 4
            )
            IconButton(onClick = { val text = draft.trim(); if (text.isNotEmpty()) { onSend(text); draft = "" } }, enabled = draft.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = Blue)
            }
        }
    }
}

private fun userFacingError(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        message.contains("password", ignoreCase = true) || message.contains("unauthorized", ignoreCase = true) || message.contains("401") -> "Não foi possível entrar. Confira a conta Matrix, a senha e o servidor."
        message.contains("timeout", ignoreCase = true) || message.contains("connect", ignoreCase = true) -> "Não consegui conectar ao servidor. Confira a internet e tente novamente."
        message.contains("HTTPS", ignoreCase = true) -> message
        message.isNotBlank() -> "Falha no Matrix: $message"
        else -> "Ocorreu um erro ao acessar a conta Matrix. Tente novamente."
    }
}
