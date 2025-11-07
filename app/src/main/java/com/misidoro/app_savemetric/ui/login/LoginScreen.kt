package com.misidoro.app_savemetric.ui.login

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.misidoro.app_savemetric.data.AuthRepository
import com.misidoro.app_savemetric.data.RetrofitClient
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

    if (vm.successToken != null) {
        onLoginSuccess(vm.successToken!!)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        OutlinedTextField(
            value = vm.email,
            onValueChange = vm::onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = vm.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (vm.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        } else {
            Button(
                onClick = { vm.login() },
                enabled = !vm.isLoading
            ) {
                Text("Enviar")
            }

            TextButton(
                onClick = onRegisterClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Si eres un usuario nuevo. Regístrate")
            }
        }
    }

    // Mostrar alerta cuando hay mensaje de error desde ViewModel
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

    // Mostrar diálogo de registro exitoso si viene desde RegistroActivity
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