package com.misidoro.app_savemetric

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.misidoro.app_savemetric.data.SessionManager
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import java.text.Normalizer
import java.util.Locale

class ModalidadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        title = "Modalidad"
        setContent {
            App_savemetricTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ModalidadScreen(onAccept = { modalidadKey ->
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                        prefs.edit().putString("modalidad", modalidadKey).apply()
                        finish()
                    }, onBack = { finish() })
                }
            }
        }
    }
}

private fun normalize(s: String): String {
    val n = Normalizer.normalize(s, Normalizer.Form.NFD)
    return n.replace("\\p{M}+".toRegex(), "").lowercase(Locale.getDefault()).trim()
}

@Composable
private fun ModalidadScreen(onAccept: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val savedRaw = prefs.getString("modalidad", null)
    val isVip = remember { SessionManager.getUser()?.vip == true }

    // keys internos y textos para mostrar
    val options = listOf(
        "basico" to "Básico: no se registran posiciones de lanzamiento ni direcciones del balón",
        "normal" to "Normal: se registran posiciones de lanzamiento pero no direcciones del balón",
        "detallado" to "Detallado: se registran posiciones de lanzamiento y direcciones del balón"
    )

    // intentar mapear el valor guardado (primero comparar con la clave interna, luego con el texto normalizado)
    val savedKey = savedRaw?.let { raw ->
        // si ya se guardó la clave interna directamente, devolverla
        if (options.any { it.first == raw }) {
            raw
        } else {
            val nRaw = normalize(raw)
            options.firstOrNull { (_, label) -> normalize(label) == nRaw }?.first
        }
    }

    // selección inicial: respeta guardado si está permitido; si no hay guardado usar por defecto según vip
    val initialKey = when {
        savedKey != null && (isVip || savedKey == "basico") -> savedKey
        isVip -> "normal"
        else -> "basico"
    }

    var selectedKey by rememberSaveable { mutableStateOf<String?>(initialKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        options.forEachIndexed { _, pair ->
            val (key, label) = pair
            val enabled = isVip || key == "basico"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                RadioButton(
                    selected = selectedKey == key,
                    onClick = { if (enabled) selectedKey = key },
                    enabled = enabled
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = label, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(modifier = Modifier.size(18.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onBack() },
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Volver")
            }

            Button(
                onClick = { selectedKey?.let { key -> onAccept(key) } },
                enabled = selectedKey != null,
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Aceptar")
            }
        }
    }
}