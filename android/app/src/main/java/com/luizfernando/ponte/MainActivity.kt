package com.luizfernando.ponte

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val Ink = Color(0xFF172436)
private val Blue = Color(0xFF4467D8)
private val Pale = Color(0xFFF5F7FC)
private val Muted = Color(0xFF697586)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Pale) {
                    PonteApp(onOpenRegistration = {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://account.matrix.org/register?kind=manage_account")))
                    })
                }
            }
        }
    }
}

private enum class Page { HOME, LOGIN }

@Composable
private fun PonteApp(onOpenRegistration: () -> Unit) {
    var page by remember { mutableStateOf(Page.HOME) }
    when (page) {
        Page.HOME -> WelcomeScreen(
            onLogin = { page = Page.LOGIN },
            onRegister = onOpenRegistration
        )
        Page.LOGIN -> LoginScreen(onBack = { page = Page.HOME })
    }
}

@Composable
private fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(104.dp).background(Blue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("Ponte", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Conversas que aproximam.",
            style = MaterialTheme.typography.titleMedium,
            color = Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) {
            Icon(Icons.Filled.PersonAddAlt1, contentDescription = null)
            Text("  Criar conta Matrix", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text("Já tenho uma conta", color = Blue, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(28.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Blue)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Rede Matrix", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("Servidor inicial: matrix.org", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "Protótipo inicial: o acesso e as mensagens ainda não estão conectados à rede Matrix.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoginScreen(onBack: () -> Unit) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Ink)
        }
        Spacer(Modifier.height(18.dp))
        Text("Entrar no Ponte", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text("Use sua conta Matrix existente.", color = Muted)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = account,
            onValueChange = { account = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Conta Matrix") },
            placeholder = { Text("@nome:matrix.org") },
            singleLine = true
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Text(
                "A conexão Matrix ainda está sendo preparada. Esta tela é apenas visual: não digite uma senha real aqui.",
                modifier = Modifier.padding(16.dp),
                color = Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
