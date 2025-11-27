package com.misidoro.app_savemetric

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misidoro.app_savemetric.data.MatchCategoriasStore
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import androidx.compose.foundation.layout.Arrangement as LayoutArrangement

class CategoriasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App_savemetricTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CategoriasScreen(
                        onAccept = { categoria ->
                            MatchCategoriasStore.setCategoria(this, categoria)
                            finish()
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriasScreen(onAccept: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val savedCategory = prefs.getString("categoria", null)

    val categories = listOf("Alevin", "Infantil", "Cadete", "Juvenil", "Senior")
    val selected = remember { mutableStateOf<String?>(savedCategory) }

    // Box con imagen de fondo y UI contenida en un cuadro blanco translúcido centrado (~1/3 ancho)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.gest_portero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Contenedor central: ancho ~1/3, fondo blanco translúcido, esquinas redondeadas
        Box(
            modifier = Modifier
                .fillMaxWidth(0.33f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = LayoutArrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                categories.forEach { c ->
                    val isSelected = selected.value == c
                    if (isSelected) {
                        Button(
                            onClick = { selected.value = c },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = c)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selected.value = c },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Text(text = c)
                        }
                    }
                }

                Spacer(modifier = Modifier.size(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onBack() },
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text("Volver")
                    }

                    Button(
                        onClick = { selected.value?.let { onAccept(it) } },
                        enabled = selected.value != null,
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}