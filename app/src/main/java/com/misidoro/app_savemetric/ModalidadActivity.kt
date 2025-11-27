package com.misidoro.app_savemetric

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    val options = listOf(
        "basico" to "Básico: no se registran posiciones de lanzamiento ni direcciones del balón",
        "normal" to "Normal: se registran posiciones de lanzamiento pero no direcciones del balón",
        "detallado" to "Detallado: se registran posiciones de lanzamiento y direcciones del balón"
    )

    val savedKey = savedRaw?.let { raw ->
        if (options.any { it.first == raw }) {
            raw
        } else {
            val nRaw = normalize(raw)
            options.firstOrNull { (_, label) -> normalize(label) == nRaw }?.first
        }
    }

    val initialKey = when {
        savedKey != null && (isVip || savedKey == "basico") -> savedKey
        isVip -> "normal"
        else -> "basico"
    }

    var selectedKey by rememberSaveable { mutableStateOf<String?>(initialKey) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.gest_portero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.33f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                options.forEach { (key, label) ->
                    val enabled = isVip || key == "basico"
                    val isSelected = selectedKey == key

                    if (isSelected) {
                        Button(
                            onClick = { if (enabled) selectedKey = key },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { if (enabled) selectedKey = key },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.6f)
                            )
                        ) {
                            Text(text = label)
                        }
                    }

                    Spacer(modifier = Modifier.size(8.dp))
                }

                Spacer(modifier = Modifier.size(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onBack() },
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text("Volver")
                    }

                    Spacer(modifier = Modifier.size(12.dp))

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
    }
}