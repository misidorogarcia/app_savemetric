package com.misidoro.app_savemetric

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import com.misidoro.app_savemetric.data.CreatePorteroRequest
import com.misidoro.app_savemetric.data.PorterosRepository
import com.misidoro.app_savemetric.data.Portero
import com.misidoro.app_savemetric.data.SessionManager
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import kotlinx.coroutines.launch
import retrofit2.Response

class CreatePorteroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App_savemetricTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CreatePorteroScreen()
                }
            }
        }
    }
}

@Composable
private fun CreatePorteroScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PorterosRepository() }
    val user = SessionManager.getUser()
    val isVip = user?.vip == true

    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") } // esperar "yyyy-MM-dd"

    Box(modifier = Modifier.fillMaxSize()) {
        // Imagen de fondo
        Image(
            painter = painterResource(id = R.drawable.gest_portero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Contenido encima de la imagen

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.85f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Crear Portero", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.padding( 8.dp)
            )

            OutlinedTextField(
                value = apellidos,
                onValueChange = { apellidos = it },
                label = { Text("Apellidos") },
                modifier = Modifier.padding( 8.dp)
            )

            OutlinedTextField(
                value = fecha,
                onValueChange = { fecha = it },
                label = { Text("Fecha de nacimiento (YYYY-MM-DD)") },
                modifier = Modifier.padding( 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Volver: cierra la actividad y vuelve a la anterior (PorterosActivity si viene de allí)
                Button(
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    enabled = true
                ) {
                    Text("Volver")
                }

                Spacer(modifier = Modifier.size(12.dp))

                // Crear Portero: habilitado sólo cuando todos los campos estén informados
                val crearEnabled = nombre.isNotBlank() && apellidos.isNotBlank() && fecha.isNotBlank()

                Button(
                    onClick = {
                        val activity = (context as? Activity)
                        if (user == null) {
                            Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!crearEnabled) return@Button

                        scope.launch {
                            val req = CreatePorteroRequest(
                                nombre = nombre.trim(),
                                apellidos = apellidos.trim().ifEmpty { null },
                                fecha_nacimiento = fecha.trim()
                            )
                            val result = repo.createPorteroForUser(user, req)
                            result.fold(
                                onSuccess = { resp: Response<Portero> ->
                                    if (resp.isSuccessful) {
                                        Toast.makeText(context, "Portero creado", Toast.LENGTH_SHORT).show()
                                        // devolver resultado a la actividad que lanzó este formulario
                                        val data = Intent().putExtra("created_message", "Portero creado")
                                        activity?.setResult(Activity.RESULT_OK, data)
                                        activity?.finish()
                                    } else {
                                        Toast.makeText(context, "Error al crear portero: ${resp.code()}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    enabled = crearEnabled && isVip
                ) {
                    Text("Crear Portero")
                }
            }
        }
    }
}