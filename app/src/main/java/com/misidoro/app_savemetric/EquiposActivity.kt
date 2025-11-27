package com.misidoro.app_savemetric

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    // estados
    var equipo by remember { mutableStateOf("") }
    var rival by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }

    // cargar valores guardados al iniciar ( compatible Long/Int/Float/Double/String )
    LaunchedEffect(Unit) {
        equipo = prefs.getString("equipo", "") ?: ""
        rival = prefs.getString("rival", "") ?: ""

        val raw = prefs.all["fecha"]
        fecha = when (raw) {
            is Long -> sdf.format(Date(raw))
            is Int -> sdf.format(Date(raw.toLong()))
            is Float -> sdf.format(Date(raw.toLong()))
            is Double -> sdf.format(Date(raw.toLong()))
            is String -> raw
            else -> ""
        }

        if (fecha.isBlank()) {
            // fecha por defecto: hoy
            fecha = sdf.format(Date())
        }
    }

    val canSave = equipo.isNotBlank() && rival.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.gest_portero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Caja central...
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
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
                    Button(
                        onClick = {
                            // al guardar, convertir la fecha a millis y guardarla como Long (más robusto)
                            val dateToSave = try {
                                sdf.parse(fecha) ?: Date()
                            } catch (_: Exception) {
                                Date()
                            }
                            prefs.edit()
                                .putString("equipo", equipo)
                                .putString("rival", rival)
                                .putLong("fecha", dateToSave.time)
                                .apply()
                            Toast.makeText(context, "Equipos guardados", Toast.LENGTH_SHORT).show()
                            (context as? Activity)?.finish()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSave
                    ) {
                        Text("Guardar")
                    }
                    Spacer(modifier = Modifier.size(8.dp))

                    Button(
                        onClick = { (context as? Activity)?.finish() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Volver")
                    }
                }
            }
        }
    }
}