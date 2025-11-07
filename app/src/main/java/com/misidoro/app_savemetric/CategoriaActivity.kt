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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misidoro.app_savemetric.data.MatchCategoriasStore
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        categories.forEach { c ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                RadioButton(
                    selected = selected.value == c,
                    onClick = { selected.value = c }
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = c, color = MaterialTheme.colorScheme.onBackground)
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