package com.misidoro.app_savemetric

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.misidoro.app_savemetric.data.Estadistica
import com.misidoro.app_savemetric.data.EstadisticaManager
import com.misidoro.app_savemetric.data.MatchPorterosStore
import com.misidoro.app_savemetric.data.PartidoStore
import com.misidoro.app_savemetric.data.Posicion
import com.misidoro.app_savemetric.data.Portero
import com.misidoro.app_savemetric.data.Accion
import com.misidoro.app_savemetric.data.Direcciones
import com.misidoro.app_savemetric.data.Resultado
import com.misidoro.app_savemetric.data.EstadisticaStore
import com.misidoro.app_savemetric.data.Partido
import com.misidoro.app_savemetric.data.AccionPool
import com.misidoro.app_savemetric.data.PartidoRepository
import com.misidoro.app_savemetric.data.SessionManager
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import com.misidoro.app_savemetric.EstadisticaTipo
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast.LENGTH_LONG
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Divider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.gson.GsonBuilder
import kotlin.apply
import kotlin.or
import kotlin.text.clear
import androidx.compose.material3.Divider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


class ResumenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // leer extra que indica envío fallado (por defecto false)
        val envioFalladoInit = intent?.getBooleanExtra("envio_fallado", false) ?: false

        val partido = PartidoStore.getPartido() ?: Partido()
        val estadisticaManager =
            EstadisticaManager().apply { initForPorteros(MatchPorterosStore.getPorteros()) }
        val porteros = MatchPorterosStore.getPorteros()

        setContent {
            App_savemetricTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.est_portero),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // ... fondo/Surface como ya estaba ...
                    ResumenScreen(
                        estadisticaManager = estadisticaManager,
                        porteros = porteros,
                        envioFalladoInit = envioFalladoInit
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumenScreen(
    estadisticaManager: EstadisticaManager,
    porteros: List<Portero>,
    envioFalladoInit: Boolean
) {
    val isVip = SessionManager.getUser()?.vip == true
    var tipo by remember { mutableStateOf(if (isVip) EstadisticaTipo.POR_INTERVENCIONES else EstadisticaTipo.EFECTIVA) }
    var selectedPorteroId by remember { mutableStateOf<Int?>(null) } // null = global
    var showPosDetail by remember { mutableStateOf<Posicion?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var envioFallado by remember { mutableStateOf(envioFalladoInit) }
    var reenvioEnProgreso by remember { mutableStateOf(false) }

    val activeColor = Color(0xFF2E7D32)
    val inactiveColor = Color(0xFFF1F1F1)
    val activeTextColor = Color.White
    val inactiveTextColor = Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        // Contenido desplazable con espacio inferior para el botón fijo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .padding(bottom = 88.dp) // dejar espacio para el botón inferior
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(6.dp))
            Spacer(modifier = Modifier.size(16.dp))

            // Si el envío falló, mostrar aviso y botones reintento + generar archivo (sin cambios)
            if (envioFallado) {
                AlertDialog(
                    onDismissRequest = { envioFallado = false },
                    title = {
                        Text(
                            text = "Error de envío",
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No se pudo enviar el partido al servidor.",
                                color = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.size(6.dp))

                            Button(
                                onClick = {
                                    reenvioEnProgreso = true
                                    scope.launch {
                                        try {
                                            val partido = PartidoStore.getPartido()
                                            if (partido == null) {
                                                Toast.makeText(
                                                    context,
                                                    "No hay partido para enviar",
                                                    LENGTH_LONG
                                                ).show()
                                            } else {
                                                PartidoRepository.enviarPartidoSiVip(partido)
                                                envioFallado = false
                                                Toast.makeText(
                                                    context,
                                                    "Envío correcto",
                                                    LENGTH_LONG
                                                )
                                                    .show()
                                            }
                                        } catch (e: Throwable) {
                                            Toast.makeText(
                                                context,
                                                "Error al reenviar: ${e.message ?: "desconocido"}",
                                                LENGTH_LONG
                                            ).show()
                                        } finally {
                                            reenvioEnProgreso = false
                                        }
                                    }
                                },
                                enabled = !reenvioEnProgreso,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (reenvioEnProgreso) "Reintentando..." else "Reintentar envío")
                            }

                            Spacer(modifier = Modifier.size(8.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val partido = PartidoStore.getPartido()
                                            if (partido == null) {
                                                Toast.makeText(
                                                    context,
                                                    "No hay partido para exportar",
                                                    LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                            val userTimestamp = SessionManager.getUser()?.timestamp
                                            val path =
                                                generatePartidoJsonFile(
                                                    context,
                                                    partido,
                                                    userTimestamp
                                                )
                                            if (path != null) {
                                                Toast.makeText(
                                                    context,
                                                    "Archivo generado: $path",
                                                    LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Error generando archivo",
                                                    LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (e: Throwable) {
                                            Toast.makeText(
                                                context,
                                                "Error: ${e.message ?: "desconocido"}",
                                                LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generar archivo")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { envioFallado = false }) {
                            Text("Cerrar")
                        }
                    }
                )
            }

            // --- NUEVA BOX que agrupa portero, tipo y detalle ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                        .padding(8.dp)
                ) {
                    // Primera fila: selector de porteros (idéntico comportamiento)
                    Text(
                        text = "Panel de estadística",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(bottom = 6.dp)
                    )
                    // Si solo hay un portero, seleccionar y mostrar solo su botón. Si no hay porteros, mostrar solo "Global".
                    LaunchedEffect(porteros) {
                        if (porteros.size == 1 && selectedPorteroId == null) {
                            selectedPorteroId = porteros[0].id
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp) // separación respecto a los bordes de la Box exterior
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.02f))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (porteros.size <= 1) {
                                val p = porteros.firstOrNull()
                                if (p != null) {
                                    val isSelected = selectedPorteroId == p.id
                                    Button(
                                        onClick = { selectedPorteroId = p.id },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) activeColor else inactiveColor,
                                            contentColor = if (isSelected) activeTextColor else inactiveTextColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = (p.nombre.orEmpty() + " " + p.apellidos.orEmpty()).trim())
                                    }
                                } else {
                                    val globalSelected = selectedPorteroId == null
                                    Button(
                                        onClick = { selectedPorteroId = null },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (globalSelected) activeColor else inactiveColor,
                                            contentColor = if (globalSelected) activeTextColor else inactiveTextColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "Global")
                                    }
                                }
                            } else {
                                val globalSelected = selectedPorteroId == null
                                Button(
                                    onClick = { selectedPorteroId = null },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (globalSelected) activeColor else inactiveColor,
                                        contentColor = if (globalSelected) activeTextColor else inactiveTextColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Global")
                                }

                                for (p in porteros) {
                                    val isSelected = selectedPorteroId == p.id
                                    Button(
                                        onClick = { selectedPorteroId = p.id },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) activeColor else inactiveColor,
                                            contentColor = if (isSelected) activeTextColor else inactiveTextColor
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = (p.nombre.orEmpty() + " " + p.apellidos.orEmpty()).trim())
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    // Segunda fila: dos columnas (izquierda: selector tipo vertical; derecha: detalle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.02f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Columna izquierda: selector tipo (vertical)
                        Column(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(Color.LightGray.copy(alpha = 0.85f))
                                .padding(8.dp)
                        ) {
                            if (isVip) {
                                Text(
                                    text = "Tipo de estadística",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { tipo = EstadisticaTipo.POR_INTERVENCIONES },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (tipo == EstadisticaTipo.POR_INTERVENCIONES) activeColor else inactiveColor,
                                            contentColor = if (tipo == EstadisticaTipo.POR_INTERVENCIONES) activeTextColor else inactiveTextColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "por intervenciones")
                                    }

                                    Button(
                                        onClick = { tipo = EstadisticaTipo.REAL },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (tipo == EstadisticaTipo.REAL) activeColor else inactiveColor,
                                            contentColor = if (tipo == EstadisticaTipo.REAL) activeTextColor else inactiveTextColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "real")
                                    }

                                    Button(
                                        onClick = { tipo = EstadisticaTipo.EFECTIVA },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (tipo == EstadisticaTipo.EFECTIVA) activeColor else inactiveColor,
                                            contentColor = if (tipo == EstadisticaTipo.EFECTIVA) activeTextColor else inactiveTextColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "efectiva")
                                    }
                                }
                            } else {
                                Text(
                                    text = "Tipo de estadística: efectiva",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(6.dp)
                                )
                                // forzar tipo efectiva si no es VIP
                                tipo = EstadisticaTipo.EFECTIVA
                            }
                        }

                        // Columna derecha: detalle de la estadística con borde y esquinas redondeadas
                        Column(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.02f))
                                .padding(8.dp)
                        ) {
                            val estad =
                                estadisticaManager.getForPortero(selectedPorteroId) ?: Estadistica()
                            EstadisticaDetalle(estad, tipo)
                        }
                    }
                    // Columna derecha: detalle de la estadística
                    Column(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxWidth()
                    ) {
                        val estad =
                            estadisticaManager.getForPortero(selectedPorteroId) ?: Estadistica()
                        EstadisticaDetalle(estad, tipo)
                    }
                }
            }

            // --- fin de la BOX agrupada ---

            Spacer(modifier = Modifier.size(12.dp))

            // Botones por posición; al pulsar muestran el detalle de esa posición (sin cambios)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(Color.Blue.copy(alpha = 0.65f))
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Detalle por posición",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    Spacer(modifier = Modifier.size(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .padding(8.dp)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(Color.Blue.copy(alpha = 0.85f))
                            .padding(8.dp)
                    ) {
                        val posiciones = Posicion.values().toList()
                        val half = (posiciones.size + 1) / 2
                        val firstRow = posiciones.subList(0, half)
                        val secondRow = posiciones.subList(half, posiciones.size)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (pos in firstRow) {
                                    val estPos =
                                        estadisticaManager.getForPorteroAndPos(
                                            selectedPorteroId,
                                            pos
                                        )
                                            ?: Estadistica()
                                    val pct = pctForTipo(tipo, estPos)
                                    Button(
                                        onClick = { showPosDetail = pos },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = pos.abbr)
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text(
                                                text = pct,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (pos in secondRow) {
                                    val estPos =
                                        estadisticaManager.getForPorteroAndPos(
                                            selectedPorteroId,
                                            pos
                                        )
                                            ?: Estadistica()
                                    val pct = pctForTipo(tipo, estPos)
                                    Button(
                                        onClick = { showPosDetail = pos },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = pos.abbr)
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text(
                                                text = pct,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            // Diálogo con detalle por posición si se ha pulsado uno (sin cambios)
            if (showPosDetail != null) {
                val pos = showPosDetail!!
                val estPos =
                    estadisticaManager.getForPorteroAndPos(selectedPorteroId, pos) ?: Estadistica()
                AlertDialog(
                    onDismissRequest = { showPosDetail = null },
                    title = {
                        Text(
                            "Detalle - ${pos.key}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        EstadisticaDetalleInline(estPos, tipo)
                    },
                    confirmButton = {
                        Button(onClick = { showPosDetail = null }) {
                            Text("Cerrar")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
        }

        // Botón Salir fijo en la parte inferior, centrado y ocupando 1/3 del ancho (sin cambios)
        SalirButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.33f)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
fun SalirButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Button(
        onClick = {
            try {
                MatchPorterosStore.clear()
            } catch (_: Throwable) {
            }
            try {
                PartidoStore.getPartido()?.clearAcciones()
            } catch (_: Throwable) {
            }
            try {
                PartidoStore.clear()
            } catch (_: Throwable) {
            }
            try {
                EstadisticaStore.clearAll()
            } catch (_: Throwable) {
            }

            try {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val userJson = prefs.getString("user", null)
                prefs.edit().clear().apply()
                if (userJson != null) prefs.edit().putString("user", userJson).apply()
            } catch (_: Throwable) {
            }

            runCatching {
                val cls = Class.forName("com.misidoro.app_savemetric.data.MatchCategoriasStore")
                val instField = cls.getDeclaredField("INSTANCE")
                instField.isAccessible = true
                val instance = instField.get(null)
                val method = cls.methods.firstOrNull { m ->
                    val name = m.name.lowercase()
                    (name.contains("clear") || name.contains("remove") || name.contains("delete")) && m.parameterCount == 0
                }
                method?.invoke(instance)
            }

            val intent = Intent(context, InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            (context as? ComponentActivity)?.finishAffinity()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F),
            contentColor = Color.White
        ),
        modifier = modifier
    ) {
        Text("Salir")
    }
}

@Composable
private fun EstadisticaDetalle(estad: Estadistica, tipo: EstadisticaTipo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Detalle",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(4.dp))

            when (tipo) {
                EstadisticaTipo.POR_INTERVENCIONES -> {
                    val headers = listOf(
                        "Total lanzamientos",
                        "Goles",
                        "Total Paradas",
                        "Errores",
                        "%Intervenciones"
                    )
                    val values = listOf(
                        "${estad.accionesTotales}",
                        "${estad.goles}",
                        "${estad.paradasTotales}",
                        "${estad.noGolesTotales}",
                        pctSafe(estad.paradasTotales + estad.noGolesTotales, estad.accionesTotales)
                    )
                    Table(headers, values)
                }

                EstadisticaTipo.REAL -> {
                    val headers =
                        listOf("Lanzamientos a puerta", "Goles", "Intervenciones reales", "%real")
                    val values = listOf(
                        "${estad.accionesReales}",
                        "${estad.goles}",
                        "${estad.paradasTotales}",
                        pctSafe(estad.paradasTotales, estad.accionesReales)
                    )
                    Table(headers, values)
                }

                EstadisticaTipo.EFECTIVA -> {
                    val headers =
                        listOf("Lanzamientos válidos", "Goles", "Paradas válidas", "%efectivo")
                    val values = listOf(
                        "${estad.accionesEfectivas}",
                        "${estad.goles}",
                        "${estad.paradasValidas}",
                        pctSafe(estad.paradasValidas, estad.accionesEfectivas)
                    )
                    Table(headers, values)
                }
            }
        }
    }
}

@Composable
private fun EstadisticaDetalleInline(estad: Estadistica, tipo: EstadisticaTipo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            when (tipo) {
                EstadisticaTipo.POR_INTERVENCIONES -> {
                    Table(
                        listOf("Total", "Goles", "Paradas", "%Int"), listOf(
                            "${estad.accionesTotales}",
                            "${estad.goles}",
                            "${estad.paradasTotales}",
                            pctSafe(
                                estad.paradasTotales + estad.noGolesTotales,
                                estad.accionesTotales
                            )
                        )
                    )
                }

                EstadisticaTipo.REAL -> {
                    Table(
                        listOf("Lanz.", "Goles", "Paradas", "%Real"), listOf(
                            "${estad.accionesReales}",
                            "${estad.goles}",
                            "${estad.paradasTotales}",
                            pctSafe(estad.paradasTotales, estad.accionesReales)
                        )
                    )
                }

                EstadisticaTipo.EFECTIVA -> {
                    Table(
                        listOf("Lanz. válidos", "Goles", "Paradas válidas", "%Ef"), listOf(
                            "${estad.accionesEfectivas}",
                            "${estad.goles}",
                            "${estad.paradasValidas}",
                            pctSafe(estad.paradasValidas, estad.accionesEfectivas)
                        )
                    )
                }
            }
        }
    }
}


// kotlin
@Composable
private fun Table(headers: List<String>, values: List<String>) {
    val cellBg = MaterialTheme.colorScheme.surface
    val cellTextColor = MaterialTheme.colorScheme.onSurface
    val headerTextColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        // Encabezados (primera fila)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((idx, h) in headers.withIndex()) {
                TableHeaderCell(
                    text = h,
                    color = headerTextColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                )
            }
        }

        // Valores (segunda fila)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (v in values) {
                TableValueCell(
                    text = v,
                    color = cellTextColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    minHeight: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(8.dp))
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                ),
                RoundedCornerShape(8.dp)
            )
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun TableValueCell(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    minHeight: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(8.dp))
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                ),
                RoundedCornerShape(8.dp)
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

@Composable
private fun TableHeaderRow(labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        for (label in labels) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TableValueRow(values: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        for (v in values) {
            Text(
                text = v,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun RowLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value)
    }
}

private fun pctSafe(numerador: Int, denominador: Int): String {
    if (denominador <= 0) return "0%"
    val p = (numerador.toDouble() * 100.0) / denominador.toDouble()
    return if (p % 1.0 == 0.0) String.format("%d%%", p.toInt()) else String.format("%.1f%%", p)
}

private fun pctForTipo(tipo: EstadisticaTipo, e: Estadistica): String {
    return when (tipo) {
        EstadisticaTipo.POR_INTERVENCIONES -> pctSafe(
            e.paradasTotales + e.noGolesTotales,
            e.accionesTotales
        )

        EstadisticaTipo.REAL -> pctSafe(e.paradasTotales, e.accionesReales)
        EstadisticaTipo.EFECTIVA -> pctSafe(e.paradasValidas, e.accionesEfectivas)
    }
}

@Composable
private fun AccionRow(index: Int, accion: Accion) {
    val resultadoLabel = Resultado.fromId(accion.resultado)?.key ?: "?"
    val direccionLabel = Direcciones.values().firstOrNull { it.id == accion.direccion }?.key ?: ""
    val posicionLabel = Posicion.values().firstOrNull { it.id == accion.posicion }?.key
        ?: accion.posicion.toString()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("#$index  ${accion.tiempo}  $posicionLabel")
            Text(resultadoLabel)
        }
        if (direccionLabel.isNotEmpty()) {
            Text(text = "Dirección: $direccionLabel", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// --- helpers para generar el JSON y escribir fichero ---
private suspend fun generatePartidoJsonFile(
    context: Context,
    partido: Partido,
    userTimestamp: Long?
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fechaStr = if (partido.fecha.time > 0L) dateFmt.format(partido.fecha) else ""

            // construir objeto JSON
            val data = mutableMapOf<String, Any?>(
                "categoria" to partido.categoria,
                "fecha" to fechaStr,
                "equipo" to partido.equipo,
                "rival" to partido.rival,
                "clave" to (SessionManager.getUser()?.clave ?: "")
            )
            if (userTimestamp != null) data["timestamp"] = userTimestamp

            val acciones = partido.acciones.map { a ->
                mapOf(
                    "portero" to a.portero,
                    "tiempo" to a.tiempo,
                    "posicion" to a.posicion,
                    "direccion" to a.direccion,
                    "resultado" to a.resultado
                )
            }
            data["acciones"] = acciones

            // nombre de fichero sanitizado
            val baseName = sanitizeFilename("${partido.equipo}_${partido.rival}")
            val fileName = "$baseName.json"

            // intentar carpeta pública Documents
            val docsDir = try {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
            } catch (_: Throwable) {
                null
            }

            val targetDir = when {
                docsDir != null -> docsDir
                else -> context.filesDir
            }

            if (!targetDir.exists()) targetDir.mkdirs()
            val file = File(targetDir, fileName)
            file.writeText(gson.toJson(data), Charsets.UTF_8)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Archivo guardado: ${file.absolutePath}", Toast.LENGTH_LONG)
                    .show()
            }
            file.absolutePath
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error guardando archivo: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
            null
        }
    }
}

private fun sanitizeFilename(name: String): String {
    // sustituir caracteres inválidos por guión bajo y acortar si es muy largo
    val replaced = name.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    return if (replaced.length > 120) replaced.take(120) else replaced
}