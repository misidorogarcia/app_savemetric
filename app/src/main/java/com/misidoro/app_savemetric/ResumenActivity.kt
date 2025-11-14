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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.gson.GsonBuilder

class ResumenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // preparar manager y poblarlo a partir del partido almacenado
        val estadisticaManager = EstadisticaManager()
        val porteros = MatchPorterosStore.getPorteros()
        estadisticaManager.initForPorteros(porteros)

        // si hay partido almacenado, registrar sus acciones en el manager
        val partido = PartidoStore.getPartido()
        if (partido != null) {
            for (a in partido.acciones) {
                val pos = Posicion.values().firstOrNull { it.id == a.posicion } ?: Posicion.ND
                val porteroIdNullable = if (a.portero > 0) a.portero else null
                estadisticaManager.recordAccion(porteroIdNullable, pos, a.resultado)
                // también actualizar EstadisticaStore para mantener consistencia de la UI si se usa
                EstadisticaStore.recordAccion(pos, a)
            }
        }

        val envioFalladoInit = intent.getBooleanExtra("envio_fallado", false)

        setContent {
            App_savemetricTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ResumenScreen(estadisticaManager = estadisticaManager, porteros = porteros, envioFalladoInit = envioFalladoInit)
                }
            }
        }
    }
}

@Composable
private fun ResumenScreen(estadisticaManager: EstadisticaManager, porteros: List<Portero>, envioFalladoInit: Boolean) {
    val isVip = SessionManager.getUser()?.vip == true
    var tipo by remember { mutableStateOf(if (isVip) EstadisticaTipo.POR_INTERVENCIONES else EstadisticaTipo.EFECTIVA) }
    var selectedPorteroId by remember { mutableStateOf<Int?>(null) } // null = global
    var showPosDetail by remember { mutableStateOf<Posicion?>(null) }

    // bandera recibida desde PartidoActivity: mostrar aviso y botón reintento si true
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var envioFallado by remember { mutableStateOf(envioFalladoInit) }
    var reenvioEnProgreso by remember { mutableStateOf(false) }

    // colores para estados seleccionados/no seleccionados
    val activeColor = Color(0xFF2E7D32)
    val inactiveColor = Color(0xFFF1F1F1)
    val activeTextColor = Color.White
    val inactiveTextColor = Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(modifier = Modifier.size(6.dp))
        Spacer(modifier = Modifier.size(16.dp))

        // Si el envío falló, mostrar aviso y botones reintento + generar archivo
        if (envioFallado) {
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "No se pudo enviar el partido al servidor.", color = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.size(6.dp))
                Button(
                    onClick = {
                        reenvioEnProgreso = true
                        scope.launch {
                            try {
                                val partido = PartidoStore.getPartido()
                                if (partido == null) {
                                    Toast.makeText(context, "No hay partido para enviar", LENGTH_LONG).show()
                                } else {
                                    PartidoRepository.enviarPartidoSiVip(partido)
                                    envioFallado = false
                                    Toast.makeText(context, "Envío correcto", LENGTH_LONG).show()
                                }
                            } catch (e: Throwable) {
                                Toast.makeText(context, "Error al reenviar: ${e.message ?: "desconocido"}", LENGTH_LONG).show()
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

                // Nuevo botón: Generar archivo JSON con timestamp del usuario
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val partido = PartidoStore.getPartido()
                                if (partido == null) {
                                    Toast.makeText(context, "No hay partido para exportar", LENGTH_LONG).show()
                                    return@launch
                                }
                                val userTimestamp = SessionManager.getUser()?.timestamp
                                val path = generatePartidoJsonFile(context, partido, userTimestamp)
                                if (path != null) {
                                    Toast.makeText(context, "Archivo generado: $path", LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error generando archivo", LENGTH_LONG).show()
                                }
                            } catch (e: Throwable) {
                                Toast.makeText(context, "Error: ${e.message ?: "desconocido"}", LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generar archivo")
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }

        SalirButton()
        // Selector tipo de estadística (solo visible si VIP)
        if (isVip) {
            Text(text = "Tipo de estadística", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { tipo = EstadisticaTipo.POR_INTERVENCIONES },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tipo == EstadisticaTipo.POR_INTERVENCIONES) activeColor else inactiveColor,
                        contentColor = if (tipo == EstadisticaTipo.POR_INTERVENCIONES) activeTextColor else inactiveTextColor
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "por intervenciones")
                }

                Button(
                    onClick = { tipo = EstadisticaTipo.REAL },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tipo == EstadisticaTipo.REAL) activeColor else inactiveColor,
                        contentColor = if (tipo == EstadisticaTipo.REAL) activeTextColor else inactiveTextColor
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "real")
                }

                Button(
                    onClick = { tipo = EstadisticaTipo.EFECTIVA },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tipo == EstadisticaTipo.EFECTIVA) activeColor else inactiveColor,
                        contentColor = if (tipo == EstadisticaTipo.EFECTIVA) activeTextColor else inactiveTextColor
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "efectiva")
                }
            }
        } else {
            // Mostrar el tipo actual (no editable)
            Text(text = "Tipo de estadística: efectiva", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(6.dp))
            tipo = EstadisticaTipo.EFECTIVA
        }

        Spacer(modifier = Modifier.size(12.dp))

        // Selector de portero: opción global + cada portero
        Text(text = "Portero (Global o individual)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Botón Global (null)
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

            // Boteros individuales
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

        Spacer(modifier = Modifier.size(12.dp))

        // Detalle principal según tipo y portero/global
        val estad = estadisticaManager.getForPortero(selectedPorteroId) ?: Estadistica()
        EstadisticaDetalle(estad, tipo)

        Spacer(modifier = Modifier.size(12.dp))

        // Botones por posición; al pulsar muestran el detalle de esa posición
        Text(text = "Detalle por posición", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.size(8.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (pos in Posicion.values()) {
                    // obtener la estadística para portero y posición
                    val estPos = estadisticaManager.getForPorteroAndPos(selectedPorteroId, pos) ?: Estadistica()
                    val pct = pctForTipo(tipo, estPos)
                    Button(onClick = { showPosDetail = pos }, modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = pos.abbr)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(text = pct, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        // Diálogo con detalle por posición si se ha pulsado uno
        if (showPosDetail != null) {
            val pos = showPosDetail!!
            val estPos = estadisticaManager.getForPorteroAndPos(selectedPorteroId, pos) ?: Estadistica()
            AlertDialog(
                onDismissRequest = { showPosDetail = null },
                title = { Text("Detalle - ${pos.abbr}") },
                text = {
                    EstadisticaDetalleInline(estPos, tipo)
                },
                confirmButton = {
                    TextButton(onClick = { showPosDetail = null }) {
                        Text("Cerrar")
                    }
                }
            )
        }

    }
}
@Composable
fun SalirButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            // 0) Limpiar stores en memoria
            try {
                MatchPorterosStore.clear()
            } catch (_: Throwable) { }

            try {
                // liberar acciones del partido actual si existe
                PartidoStore.getPartido()?.clearAcciones()
            } catch (_: Throwable) { }

            try {
                PartidoStore.clear()
            } catch (_: Throwable) { }

            try {
                EstadisticaStore.clearAll()
            } catch (_: Throwable) { }

            // 1) Borrar SharedPreferences pero mantener la clave "user" si existe
            try {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val userJson = prefs.getString("user", null)
                prefs.edit().clear().apply()
                if (userJson != null) prefs.edit().putString("user", userJson).apply()
            } catch (_: Throwable) { }

            // 2) Intento seguro de limpiar MatchCategoriasStore vía reflexión si no expone método público
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

            // 3) Volver a InicioActivity limpiando la pila
            val intent = Intent(context, InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)

            // 4) Cerrar Activitys actuales si es posible
            (context as? ComponentActivity)?.finishAffinity()
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Salir")
    }
}
@Composable
private fun EstadisticaDetalle(estad: Estadistica, tipo: EstadisticaTipo) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        Text(text = "Detalle", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.size(6.dp))
        when (tipo) {
            EstadisticaTipo.POR_INTERVENCIONES -> {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total lanzamientos")
                    Text("${estad.accionesTotales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goles recibidos")
                    Text("${estad.goles}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paradas")
                    Text("${estad.paradasTotales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Errores provocados")
                    Text("${estad.noGolesTotales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("%Intervenciones")
                    Text(pctSafe(estad.paradasTotales + estad.noGolesTotales, estad.accionesTotales))
                }
            }

            EstadisticaTipo.REAL -> {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total lanzamientos a puerta")
                    Text("${estad.accionesReales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goles recibidos")
                    Text("${estad.goles}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total intervenciones reales")
                    Text("${estad.paradasTotales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("%real")
                    Text(pctSafe(estad.paradasTotales, estad.accionesReales))
                }
            }

            EstadisticaTipo.EFECTIVA -> {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total lanzamientos a puerta válidos")
                    Text("${estad.accionesEfectivas}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goles recibidos")
                    Text("${estad.goles}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total intervenciones reales")
                    Text("${estad.paradasValidas}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("%efectivo")
                    Text(pctSafe(estad.paradasValidas, estad.accionesEfectivas))
                }
            }
        }
    }
}

@Composable
private fun EstadisticaDetalleInline(estad: Estadistica, tipo: EstadisticaTipo) {
    Column {
        when (tipo) {
            EstadisticaTipo.POR_INTERVENCIONES -> {
                RowLine("Total lanzamientos", "${estad.accionesTotales}")
                RowLine("Goles", "${estad.goles}")
                RowLine("Paradas totales", "${estad.paradasTotales}")
                RowLine("%Intervenciones", pctSafe(estad.paradasTotales + estad.noGolesTotales, estad.accionesTotales))
            }
            EstadisticaTipo.REAL -> {
                RowLine("Lanzamientos a puerta", "${estad.accionesReales}")
                RowLine("Goles", "${estad.goles}")
                RowLine("Paradas", "${estad.paradasTotales}")
                RowLine("%Real", pctSafe(estad.paradasTotales, estad.accionesReales))
            }
            EstadisticaTipo.EFECTIVA -> {
                RowLine("Lanzamientos válidos", "${estad.accionesEfectivas}")
                RowLine("Goles", "${estad.goles}")
                RowLine("Paradas válidas", "${estad.paradasValidas}")
                RowLine("%Efectivo", pctSafe(estad.paradasValidas, estad.accionesEfectivas))
            }
        }
    }
}

@Composable
private fun RowLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
        EstadisticaTipo.POR_INTERVENCIONES -> pctSafe(e.paradasTotales + e.noGolesTotales, e.accionesTotales)
        EstadisticaTipo.REAL -> pctSafe(e.paradasTotales, e.accionesReales)
        EstadisticaTipo.EFECTIVA -> pctSafe(e.paradasValidas, e.accionesEfectivas)
    }
}

@Composable
private fun AccionRow(index: Int, accion: Accion) {
    val resultadoLabel = Resultado.fromId(accion.resultado)?.key ?: "?"
    val direccionLabel = Direcciones.values().firstOrNull { it.id == accion.direccion }?.key ?: ""
    val posicionLabel = Posicion.values().firstOrNull { it.id == accion.posicion }?.abbr ?: accion.posicion.toString()

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)) {
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
private suspend fun generatePartidoJsonFile(context: Context, partido: Partido, userTimestamp: Long?): String? {
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
                Toast.makeText(context, "Archivo guardado: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
            file.absolutePath
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error guardando archivo: ${e.message}", Toast.LENGTH_LONG).show()
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