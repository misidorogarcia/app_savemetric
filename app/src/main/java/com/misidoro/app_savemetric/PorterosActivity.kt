package com.misidoro.app_savemetric

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misidoro.app_savemetric.data.MatchPorterosStore
import com.misidoro.app_savemetric.data.Portero
import com.misidoro.app_savemetric.data.PorterosRepository
import com.misidoro.app_savemetric.data.SessionManager
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PorterosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App_savemetricTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PorterosScreen()
                }
            }
        }
    }
}

@Composable
private fun PorterosScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PorterosRepository() }
    val isVip = remember { SessionManager.getUser()?.vip == true }

    val selected = remember { mutableStateOf(1) }

    var showVirtualDialog by remember { mutableStateOf(false) }
    val porteros = remember { mutableStateOf<List<Portero>>(emptyList()) }
    val showPorterosList = remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateOf(setOf<Int>()) }
    val displayedPorteros = remember { mutableStateOf<List<Portero>>(emptyList()) }
    val virtualPorteros = remember { mutableStateOf<List<Portero>>(emptyList()) }
    var virtualCreated by remember { mutableStateOf(false) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val msg = result.data?.getStringExtra("created_message") ?: "Portero creado"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }

    LaunchedEffect(Unit) {
        try {
            val sessionList = MatchPorterosStore.getPorteros()
            if (!sessionList.isNullOrEmpty()) {
                displayedPorteros.value = sessionList
                selected.value = sessionList.size.coerceAtLeast(1)
                virtualCreated = sessionList.any { it.id <= 0 }
                selectedIds.value = sessionList.map { it.id }.toSet()
                showPorterosList.value = false
            } else {
                selected.value = 1
            }
        } catch (_: Exception) {
            selected.value = 1
        }
    }

    fun removeFromDisplayed(p: Portero) {
        displayedPorteros.value = displayedPorteros.value.filterNot { it.id == p.id }
        if (selectedIds.value.contains(p.id)) {
            selectedIds.value = selectedIds.value - p.id
        }
        if (virtualPorteros.value.any { it.id == p.id }) {
            virtualPorteros.value = virtualPorteros.value.filterNot { it.id == p.id }
            virtualCreated = virtualPorteros.value.isNotEmpty()
        }
    }

    fun adjustSelectionAfterCountChange(newCount: Int) {
        displayedPorteros.value = displayedPorteros.value.take(newCount)
        val remainingIds = displayedPorteros.value.map { it.id }.toSet()
        selectedIds.value = selectedIds.value.intersect(remainingIds)
    }

    val aceptarEnabled: Boolean =
        displayedPorteros.value.size == selected.value && displayedPorteros.value.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.gest_portero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Contenido desplazable (dejar espacio inferior para la barra de botones)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 96.dp), // evita que el contenido quede oculto por la barra inferior
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Elija el número de porteros", style = MaterialTheme.typography.headlineSmall)
            Spacer(
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 16.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        selected.value = 1
                        adjustSelectionAfterCountChange(1)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected.value == 1) Color(0xFF2E7D32) else Color(
                            0xFFB00020
                        ),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.width(64.dp)
                ) {
                    Text("1")
                }

                Button(
                    onClick = {
                        selected.value = 2
                        adjustSelectionAfterCountChange(2)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected.value == 2) Color(0xFF2E7D32) else Color(
                            0xFFB00020
                        ),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.width(64.dp)
                ) {
                    Text("2")
                }
            }

            Spacer(modifier = Modifier.size(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val user = SessionManager.getUser()
                        if (user == null) {
                            Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }
                        scope.launch {
                            val result = repo.getPorterosForUser(user)
                            result.fold(
                                onSuccess = { list ->
                                    porteros.value = list
                                    showPorterosList.value = true
                                },
                                onFailure = { err ->
                                    Toast.makeText(
                                        context,
                                        "Error cargando porteros: ${err.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    },
                    enabled = isVip
                ) {
                    Text("Mis porteros")
                }

                Button(
                    onClick = {
                        val user = SessionManager.getUser()
                        if (user == null) {
                            Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }
                        val intent = Intent(context, CreatePorteroActivity::class.java)
                        launcher.launch(intent)
                    },
                    enabled = isVip
                ) {
                    Text("Crear Portero")
                }

                Button(
                    onClick = { showVirtualDialog = true },
                    enabled = !isVip
                ) {
                    Text("Porteros virtuales")
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Row centrada: ambos boxes (Mis porteros / Porteros para el partido)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                // Box izquierda: "Mis porteros" (solo visible para VIP y centrado)
                if (isVip) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFB3EFC).copy(alpha = 0.6f))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Mis porteros:", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.size(6.dp))

                            if (showPorterosList.value && porteros.value.isNotEmpty()) {
                                porteros.value.forEach { p ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val checked = selectedIds.value.contains(p.id)
                                        Button(
                                            onClick = {
                                                if (!checked) {
                                                    if (displayedPorteros.value.size < selected.value) {
                                                        selectedIds.value = selectedIds.value + p.id
                                                        displayedPorteros.value =
                                                            (displayedPorteros.value + p).distinctBy { it.id }
                                                                .take(selected.value)
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Ya has seleccionado ${selected.value} portero(s)",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    selectedIds.value = selectedIds.value - p.id
                                                    displayedPorteros.value =
                                                        displayedPorteros.value.filterNot { it.id == p.id }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (checked) Color(0xFF2E7D32) else Color(
                                                    0xFFBDBDBD
                                                ),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text(if (checked) "✔" else "+")
                                        }
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text("${p.nombre ?: ""} ${p.apellidos ?: ""}".trim())
                                    }
                                    Spacer(modifier = Modifier.size(6.dp))
                                }

                                Spacer(modifier = Modifier.size(12.dp))
                                Text(
                                    "Seleccionados: ${selectedIds.value.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(
                                    "Pulse en Mis Porteros para cargar sus porteros",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))
                }

                // Box derecha: "Porteros para el partido" (naranja translúcido)
                if (!isVip) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFE0B2).copy(alpha = 0.6f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Porteros para el partido:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.size(6.dp))

                            if (displayedPorteros.value.isEmpty()) {
                                Text(
                                    "(ninguno seleccionado)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                displayedPorteros.value.forEach { p ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${p.nombre ?: ""} ${p.apellidos ?: ""}".trim(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Button(onClick = { removeFromDisplayed(p) }) {
                                            Text("Quitar")
                                        }
                                    }
                                    Spacer(modifier = Modifier.size(6.dp))
                                }
                            }

                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                "Requeridos: ${selected.value} • Seleccionados: ${displayedPorteros.value.size}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(12.dp))
        }

        // Barra inferior fija con los botones Volver / Aceptar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                val intent = Intent(context, InicioActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }) {
                Text("Volver")
            }

            Spacer(modifier = Modifier.size(12.dp))

            Button(
                onClick = {
                    if (!aceptarEnabled) return@Button
                    MatchPorterosStore.setPorteros(displayedPorteros.value)
                    Toast.makeText(context, "Porteros guardados en sesión", Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(context, InicioActivity::class.java)
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                },
                enabled = aceptarEnabled
            ) {
                Text("Aceptar")
            }
        }

        if (showVirtualDialog) {
            VirtualPorterosDialog(
                count = selected.value,
                onDismiss = { showVirtualDialog = false },
                onSave = { lista ->
                    virtualPorteros.value = lista
                    virtualCreated = true
                    displayedPorteros.value = lista.take(selected.value)
                    selectedIds.value = emptySet()
                    showPorterosList.value = false
                    val resumen =
                        lista.joinToString { "${it.nombre ?: ""} ${it.apellidos ?: ""} (${it.fecha_nacimiento ?: ""})" }
                    Toast.makeText(
                        context,
                        "Porteros virtuales guardados: $resumen",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun VirtualPorterosDialog(
    count: Int,
    onDismiss: () -> Unit,
    onSave: (List<Portero>) -> Unit
) {
    val nameStates = remember(count) { List(count) { index -> mutableStateOf("Portero") } }
    val lastNameStates = remember(count) {
        List(count) { index ->
            val defaultApellidos = when (index) {
                0 -> "Uno Uno"
                1 -> "Dos Dos"
                else -> "Apellidos"
            }
            mutableStateOf(defaultApellidos)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Porteros virtuales") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Edite nombre y apellidos para $count portero(s):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.size(6.dp))
                for (i in 0 until count) {
                    Text("Portero ${i + 1}", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = nameStates[i].value,
                        onValueChange = { nameStates[i].value = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = lastNameStates[i].value,
                        onValueChange = { lastNameStates[i].value = it },
                        label = { Text("Apellidos") },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val lista = nameStates.indices.map { i ->
                    Portero(
                        id = -(i + 1),
                        nombre = nameStates[i].value.trim().ifEmpty { null },
                        apellidos = lastNameStates[i].value.trim().ifEmpty { null },
                        fecha_nacimiento = today
                    )
                }
                onSave(lista)
                onDismiss()
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}