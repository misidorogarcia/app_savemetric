package com.misidoro.app_savemetric

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EquiposActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App_savemetricTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EquiposScreen()
                }
            }
        }
    }
}

@Composable
private fun EquiposScreen() {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // estados
    var equipo by remember { mutableStateOf("") }
    var rival by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }

    // cargar valores guardados al iniciar
    LaunchedEffect(Unit) {
        equipo = prefs.getString("equipo", "") ?: ""
        rival = prefs.getString("rival", "") ?: ""
        fecha = prefs.getString("fecha", "") ?: ""
        if (fecha.isBlank()) {
            // fecha por defecto: hoy
            fecha = sdf.format(Date())
        }
    }

    val canSave = equipo.isNotBlank() && rival.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = equipo,
            onValueChange = { equipo = it },
            label = { Text("Equipo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.size(12.dp))

        OutlinedTextField(
            value = rival,
            onValueChange = { rival = it },
            label = { Text("Rival") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.size(12.dp))

        Button(
            onClick = {
                // parse fecha actual (o hoy) y abrir DatePicker con esos valores
                val cal = Calendar.getInstance()
                try {
                    val parsed: Date? = sdf.parse(fecha)
                    if (parsed != null) {
                        cal.time = parsed
                    }
                } catch (_: Exception) { /* fallback a hoy */ }

                val dp = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        cal.set(year, month, dayOfMonth)
                        fecha = sdf.format(cal.time)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                dp.show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fecha: $fecha")
        }

        Spacer(modifier = Modifier.size(16.dp))

        // Botón Volver: cierra la activity sin guardar
        Button(
            onClick = { (context as? Activity)?.finish() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Volver")
        }

        Spacer(modifier = Modifier.size(8.dp))

        // Botón Guardar: solo activo cuando equipo y rival están informados
        Button(
            onClick = {
                prefs.edit()
                    .putString("equipo", equipo)
                    .putString("rival", rival)
                    .putString("fecha", fecha)
                    .apply()
                Toast.makeText(context, "Equipos guardados", Toast.LENGTH_SHORT).show()
                (context as? Activity)?.finish()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave
        ) {
            Text("Guardar")
        }
    }
}