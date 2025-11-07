package com.misidoro.app_savemetric

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import java.util.Locale

class TiempoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        title = "Minutos por tiempo"
        setContent {
            App_savemetricTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TiempoScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun TiempoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // leer categoria y tiempo guardado
    val savedTiempo = prefs.getInt("tiempo", -1)
    val categoriaRaw = prefs.getString("categoria", "") ?: ""
    val categoria = categoriaRaw.lowercase(Locale.getDefault())

    val default = when {
        categoria.contains("infantil") -> 25
        categoria.contains("alevin") || categoria.contains("alevín") -> 20
        else -> 30
    }

    var value by remember { mutableStateOf(if (savedTiempo >= 0) savedTiempo else default) }
    val min = 0
    val max = 999

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { if (value > min) value-- },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("-")
            }

            Spacer(modifier = Modifier.size(24.dp))

            Text(
                text = value.toString(),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.size(24.dp))

            Button(
                onClick = { if (value < max) value++ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.size(24.dp))

        // Botón Aceptar: guarda tiempo en sesión y cierra
        Button(
            onClick = {
                prefs.edit().putInt("tiempo", value).apply()
                Toast.makeText(context, "Tiempo guardado: $value", Toast.LENGTH_SHORT).show()
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Aceptar")
        }

        Spacer(modifier = Modifier.size(8.dp))

        // Botón Volver: cierra sin guardar (opcional)
        Button(
            onClick = { onBack() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors()
        ) {
            Text("Volver")
        }
    }
}