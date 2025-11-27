package com.misidoro.app_savemetric.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.misidoro.app_savemetric.data.AuthRepository
import com.misidoro.app_savemetric.data.RetrofitClient
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    registerMessage: String? = null,
    onLoginSuccess: (String) -> Unit = {},
    onRegisterClick: () -> Unit = {}
) {
    val repo = remember { AuthRepository(RetrofitClient.api) }
    val vm: LoginViewModel = viewModel(factory = LoginViewModelFactory(repo))

    var showRegisterDialog by remember { mutableStateOf(registerMessage != null) }
    var registerDialogMessage by remember { mutableStateOf(registerMessage ?: "") }

    // Estado local para mostrar/ocultar contraseña
    var passwordVisible by remember { mutableStateOf(false) }

    // Validación en UI: mínimo 8 caracteres
    val isPasswordValid = vm.password.length >= 8
    val isFormValid = vm.email.isNotBlank() && isPasswordValid && !vm.isLoading

    if (vm.successToken != null) {
        onLoginSuccess(vm.successToken!!)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = com.misidoro.app_savemetric.R.drawable.fondo_2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.85f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
            OutlinedTextField(
                value = vm.email,
                onValueChange = vm::onEmailChange,
                label = { Text("Email") },
                modifier =  Modifier
                    .padding( 8.dp)
                    .background(Color.White.copy(alpha = 0.85f))
            )

            OutlinedTextField(
                value = vm.password,
                onValueChange = vm::onPasswordChange,
                label = { Text("Contraseña") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .padding( 8.dp)
                    .background(Color.White.copy(alpha = 0.85f)),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val desc = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        Icon(imageVector = icon, contentDescription = desc)
                    }
                },
                isError = vm.password.isNotBlank() && !isPasswordValid
            )

            // Mensaje de error inline para contraseña corta
            if (vm.password.isNotBlank() && !isPasswordValid) {
                Text(
                    text = "La contraseña debe tener al menos 8 caracteres",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding( 8.dp)
                )
            } else {
                // espacio entre campos cuando no hay error
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 8.dp))
            }

            if (vm.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding( 16.dp))
            } else {
                Button(
                    onClick = { vm.login() },
                    enabled = isFormValid
                ) {
                    Text("Enviar")
                }

                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier.padding( 8.dp)
                ) {
                    Text("Si eres un usuario nuevo. Regístrate")
                }
            }
        }
    }

    vm.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { vm.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Registro") },
            text = { Text(registerDialogMessage) },
            confirmButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}