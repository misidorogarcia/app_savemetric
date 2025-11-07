package com.misidoro.app_savemetric

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.misidoro.app_savemetric.data.MatchPorterosStore
import com.misidoro.app_savemetric.data.MatchCategoriasStore
import com.misidoro.app_savemetric.data.Portero
import com.misidoro.app_savemetric.data.SessionManager
import com.misidoro.app_savemetric.data.PartidoStore
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme

class InicioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        title = "Inicio"

        // Establecer siempre en sesión la modalidad por defecto según vip
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isVip = SessionManager.getUser()?.vip == true
        val modalidadDefault = if (isVip) "normal" else "basico"
        prefs.edit().putString("modalidad", modalidadDefault).apply()

        setContent {
            App_savemetricTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    InicioScreen()
                }
            }
        }
    }
}

private fun hasEquipos(context: Context): Boolean {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val equipo = prefs.getString("equipo", "") ?: ""
    val rival = prefs.getString("rival", "") ?: ""
    return equipo.isNotBlank() && rival.isNotBlank()
}

@Composable
private fun InicioScreen() {
    val context = LocalContext.current
    val porterosState = remember { mutableStateOf<List<Portero>>(emptyList()) }
    val categoriaState = remember { mutableStateOf(false) }
    val equiposState = remember { mutableStateOf(false) }
    val tiempoState = remember { mutableStateOf(false) }
    val modalidadState = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        porterosState.value = MatchPorterosStore.getPorteros() ?: emptyList()
        categoriaState.value = MatchCategoriasStore.hasCategoria(context)
        equiposState.value = hasEquipos(context)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        tiempoState.value = prefs.getInt("tiempo", -1) >= 0
        modalidadState.value = prefs.getString("modalidad", "")?.isNotBlank() == true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                porterosState.value = MatchPorterosStore.getPorteros() ?: emptyList()
                categoriaState.value = MatchCategoriasStore.hasCategoria(context)
                equiposState.value = hasEquipos(context)
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                tiempoState.value = prefs.getInt("tiempo", -1) >= 0
                modalidadState.value = prefs.getString("modalidad", "")?.isNotBlank() == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasPorteros = porterosState.value.isNotEmpty()
    val hasCategoria = categoriaState.value
    val hasEquipos = equiposState.value
    val hasTiempo = tiempoState.value
    val hasModalidad = modalidadState.value

    val allComplete = hasPorteros && hasCategoria && hasEquipos && hasTiempo && hasModalidad

    val porterosColor = if (hasPorteros) Color(0xFF2E7D32) else Color(0xFFB00020)
    val categoriaColor = if (hasCategoria) Color(0xFF2E7D32) else Color(0xFFB00020)
    val equiposColor = if (hasEquipos) Color(0xFF2E7D32) else Color(0xFFB00020)
    val tiempoColor = if (hasTiempo) Color(0xFF2E7D32) else Color(0xFFB00020)
    val modalidadColor = if (hasModalidad) Color(0xFF2E7D32) else Color(0xFFB00020)
    val contentColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                val intent = Intent(context, PorterosActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = porterosColor, contentColor = contentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Porteros")
        }

        Spacer(modifier = Modifier.size(12.dp))

        Button(
            onClick = {
                val intent = Intent(context, EquiposActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = equiposColor, contentColor = contentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Equipos")
        }

        Spacer(modifier = Modifier.size(12.dp))

        Button(
            onClick = {
                val intent = Intent(context, CategoriasActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = categoriaColor, contentColor = contentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Categoría")
        }

        Spacer(modifier = Modifier.size(12.dp))

        Button(
            onClick = {
                val intent = Intent(context, TiempoActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = tiempoColor, contentColor = contentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tiempo")
        }

        Spacer(modifier = Modifier.size(12.dp))

        // Nuevo botón Modalidad
        Button(
            onClick = {
                val intent = Intent(context, ModalidadActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = modalidadColor, contentColor = contentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Modalidad")
        }

        Spacer(modifier = Modifier.size(12.dp))

        // Botón Ir al partido: crea el partido desde sesión y abre PartidoActivity
        Button(
            onClick = {
                PartidoStore.createFromSession(context)
                val intent = Intent(context, PartidoActivity::class.java)
                context.startActivity(intent)
            },
            enabled = allComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = contentColor
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ir al partido")
        }

        Spacer(modifier = Modifier.size(12.dp))

        // Botón Salir: distinto visualmente, borra sesión y vuelve al login
        Button(
            onClick = {
                // 0) Limpiar stores en memoria directamente (MatchPorterosStore es un object)
                MatchPorterosStore.clear()

                // 1) Borrar SharedPreferences
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                prefs.edit().clear().apply()

                // 2) Intento seguro de limpiar MatchCategoriasStore vía reflexión si no expone método público
                runCatching {
                    val cls2 = Class.forName("com.misidoro.app_savemetric.data.MatchCategoriasStore")
                    val instField = cls2.getDeclaredField("INSTANCE")
                    instField.isAccessible = true
                    val instance = instField.get(null)
                    val method2 = cls2.methods.firstOrNull { m ->
                        val name = m.name.lowercase()
                        (name.contains("clear") || name.contains("remove") || name.contains("delete")) &&
                                m.parameterCount == 0
                    }
                    method2?.invoke(instance)
                }

                // 3) Volver al login y limpiar la pila de Activities
                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)

                // 4) Cerrar esta Activity si es posible
                (context as? ComponentActivity)?.finishAffinity()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salir")
        }

        Spacer(modifier = Modifier.size(16.dp))

        Text(
            text = if (hasPorteros) "Hay porteros en sesión" else "No hay porteros en sesión",
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}