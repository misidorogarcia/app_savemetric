package com.misidoro.app_savemetric

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.misidoro.app_savemetric.data.AuthRepository
import com.misidoro.app_savemetric.data.RetrofitClient
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RegistroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App_savemetricTheme {
                RegistroScreen()
            }
        }
    }
}

@Composable
private fun RegistroScreen() {
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository(RetrofitClient.api) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registro", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = apellidos,
            onValueChange = { apellidos = it },
            label = { Text("Apellidos") },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        } else {
            Button(onClick = {
                when {
                    nombre.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                        dialogMessage = "Rellena todos los campos"
                        showDialog = true
                    }
                    password != confirmPassword -> {
                        dialogMessage = "Las contraseñas no coinciden"
                        showDialog = true
                    }
                    else -> {
                        isLoading = true
                        scope.launch {
                            val result = repo.register(
                                name = nombre,
                                apellidos = apellidos.ifBlank { null },
                                email = email,
                                password = password,
                                passwordConfirmation = confirmPassword
                            )
                            isLoading = false
                            result.fold(
                                onSuccess = { _resp ->
                                    // Al registrar correctamente, volver al login mostrando mensaje
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        putExtra("register_message", "usuario creado con éxito, ahora ya puede iniciar sesión con sus credenciales")
                                    }
                                    context.startActivity(intent)
                                    (context as? Activity)?.finish()
                                },
                                onFailure = { throwable ->
                                    if (throwable is HttpException) {
                                        val code = throwable.code()
                                        dialogMessage = when (code) {
                                            422 -> "Datos inválidos o email ya registrado"
                                            else -> "Error del servidor: $code"
                                        }
                                    } else {
                                        dialogMessage = throwable.message ?: "Error de red"
                                    }
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }) {
                Text("Enviar")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Registro") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}