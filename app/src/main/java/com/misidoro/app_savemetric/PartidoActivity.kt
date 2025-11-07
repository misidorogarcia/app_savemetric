package com.misidoro.app_savemetric

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misidoro.app_savemetric.data.Accion
import com.misidoro.app_savemetric.data.AccionPool
import com.misidoro.app_savemetric.data.Direcciones
import com.misidoro.app_savemetric.data.Estadistica
import com.misidoro.app_savemetric.data.EstadisticaManager
import com.misidoro.app_savemetric.data.EstadisticaStore
import com.misidoro.app_savemetric.data.MatchPorterosStore
import com.misidoro.app_savemetric.data.Partido
import com.misidoro.app_savemetric.data.PartidoRepository
import com.misidoro.app_savemetric.data.PartidoStore
import com.misidoro.app_savemetric.data.Portero
import com.misidoro.app_savemetric.data.Posicion
import com.misidoro.app_savemetric.data.Resultado
import com.misidoro.app_savemetric.data.SessionManager
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SelectedPosTime(val pos: Posicion, val tiempoSec: Long)

// Enum para los tipos de estadística
enum class EstadisticaTipo {
    POR_INTERVENCIONES,
    REAL,
    EFECTIVA
}

class PartidoActivity : ComponentActivity() {
    private lateinit var partido: Partido
    private var modalidadSession: String = ""
    private var matchDurationSec: Long = -1L // segundos por mitad

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // obtener partido (InicioActivity ya llamó PartidoStore.createFromSession)
        partido = PartidoStore.getPartido() ?: Partido()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        modalidadSession = prefs.getString("modalidad", "") ?: ""

        // lectura robusta de "tiempo" en minutos (puede ser Int o String)
        val raw = prefs.all["tiempo"]
        val minutos: Long = when (raw) {
            is Int -> raw.toLong()
            is Long -> raw
            is String -> raw.toLongOrNull() ?: -1L
            else -> -1L
        }
        matchDurationSec = if (minutos > 0L) minutos * 60L else -1L

        setContent {
            App_savemetricTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PartidoScreen(
                        partido = partido,
                        modalidad = modalidadSession,
                        matchDurationSec = matchDurationSec,
                        onFinalizar = { finish() },
                        onSalir = { finish() }
                    )
                }
            }
        }

        // evitar back accidental
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // comport. por defecto: cerrar activity
                finish()
            }
        })
    }
}

@Composable
private fun PorterosToggle(
    estadisticaManager: EstadisticaManager,
    estadisticaTipo: EstadisticaTipo,
    onActiveChanged: (Portero?) -> Unit = {}
) {
    // leer versión para forzar recomposición cuando cambie EstadisticaStore
    val statsVersion by EstadisticaStore.version.collectAsState()

    val porterosState = remember { mutableStateOf<List<Portero>>(emptyList()) }
    val activeIndex = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        porterosState.value = MatchPorterosStore.getPorteros()
        onActiveChanged(porterosState.value.getOrNull(activeIndex.value))
    }

    val porteros = porterosState.value
    if (porteros.isEmpty()) {
        onActiveChanged(null)
        return
    }

    val showCount = kotlin.math.min(2, porteros.size)

    fun fmtPct(numerador: Int, denominador: Int): String {
        if (denominador <= 0) return "X"
        val p = (numerador.toDouble() * 100.0) / denominador.toDouble()
        return if (p % 1.0 == 0.0) String.format("%d%%", p.toInt()) else String.format("%.1f%%", p)
    }

    fun pctForPortero(tipo: EstadisticaTipo, e: Estadistica?): String {
        if (e == null) return "X"
        return when (tipo) {
            EstadisticaTipo.POR_INTERVENCIONES -> fmtPct(e.paradasTotales + e.noGolesTotales, e.accionesTotales)
            EstadisticaTipo.REAL -> fmtPct(e.paradasTotales, e.accionesReales)
            EstadisticaTipo.EFECTIVA -> fmtPct(e.paradasValidas, e.accionesEfectivas)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until showCount) {
                val p = porteros[i]
                val isActive = activeIndex.value == i
                // obtener estadística del manager; si es null, usar objeto vacío para evitar NPE
                val estad = estadisticaManager.getForPortero(p.id) ?: Estadistica()
                // referenciar statsVersion aquí para asegurar que la recomposición utiliza la última versión
                val pctText = run {
                    @Suppress("UNUSED_VARIABLE")
                    val v = statsVersion
                    pctForPortero(estadisticaTipo, estad)
                }

                val bgColor = if (isActive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                Button(
                    onClick = {
                        activeIndex.value = i
                        onActiveChanged(p)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = bgColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "${p.nombre.orEmpty()} ${p.apellidos.orEmpty()} — $pctText")
                }
            }
            if (showCount == 1) {
                Spacer(modifier = Modifier.fillMaxWidth(0.5f))
            }
        }
    }
}

@Composable
private fun EstadisticaTipoSelector(
    current: EstadisticaTipo,
    isVip: Boolean,
    onSelect: (EstadisticaTipo) -> Unit
) {
    val activeColor = Color(0xFF2E7D32)     // color activo (verde)
    val inactiveColor = Color(0xFFBDBDBD)   // color no activo (gris)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Tipo de estadística", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { if (isVip) onSelect(EstadisticaTipo.POR_INTERVENCIONES) },
                enabled = isVip,
                colors = ButtonDefaults.buttonColors(containerColor = if (current == EstadisticaTipo.POR_INTERVENCIONES) activeColor else inactiveColor),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "por intervenciones")
            }

            Button(
                onClick = { if (isVip) onSelect(EstadisticaTipo.REAL) },
                enabled = isVip,
                colors = ButtonDefaults.buttonColors(containerColor = if (current == EstadisticaTipo.REAL) activeColor else inactiveColor),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "real")
            }

            Button(
                onClick = { onSelect(EstadisticaTipo.EFECTIVA) },
                enabled = true,
                colors = ButtonDefaults.buttonColors(containerColor = if (current == EstadisticaTipo.EFECTIVA) activeColor else inactiveColor),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "efectiva")
            }
        }
    }
}

@Composable
private fun EstadisticaPanel(tipo: EstadisticaTipo) {
    val s by EstadisticaStore.global.collectAsState()

    fun pct(numerador: Int, denominador: Int): String {
        if (denominador <= 0) return "0%"
        val p = (numerador.toDouble() * 100.0) / denominador.toDouble()
        val texto = if (p % 1.0 == 0.0) String.format("%d%%", p.toInt()) else String.format("%.1f%%", p)
        return texto
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = "Estadística", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))

        when (tipo) {
            EstadisticaTipo.POR_INTERVENCIONES -> {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total lanzamientos")
                    Text("${s.accionesTotales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goles recibidos")
                    Text("${s.goles}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paradas")
                    Text("${s.paradasTotales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Errores provocados")
                    Text("${s.noGolesTotales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("%Intervenciones")
                    Text(pct(s.paradasTotales + s.noGolesTotales, s.accionesTotales))
                }
            }

            EstadisticaTipo.REAL -> {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total lanzamientos a puerta")
                    Text("${s.accionesReales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goles recibidos")
                    Text("${s.goles}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total intervenciones reales")
                    Text("${s.paradasTotales}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("%real")
                    Text(pct(s.paradasTotales, s.accionesReales))
                }
            }

            EstadisticaTipo.EFECTIVA -> {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total lanzamientos a puerta válidos")
                    Text("${s.accionesEfectivas}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goles recibidos")
                    Text("${s.goles}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total intervenciones reales")
                    Text("${s.paradasValidas}")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("%efectivo")
                    Text(pct(s.paradasValidas, s.accionesEfectivas))
                }
            }
        }
    }
}

private fun formatTimeHMS(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}

@Composable
private fun PartidoScreen(
    partido: Partido,
    modalidad: String,
    matchDurationSec: Long = -1L,
    onFinalizar: () -> Unit,
    onSalir: () -> Unit
) {
    val secondsState = remember { mutableStateOf(0L) } // segundos dentro de la mitad actual
    val runningState = remember { mutableStateOf(false) }
    val timeLimitReached = remember { mutableStateOf(false) } // indica que se alcanzó el límite de la mitad
    val secondHalfOffset = remember { mutableStateOf(0L) } // tiempo acumulado de mitades anteriores
    val secondHalfStarted = remember { mutableStateOf(false) }
    val prorrogaStarted = remember { mutableStateOf(false) } // nuevo: controla prórroga
    val context = LocalContext.current

    // Usar EstadisticaManager para gestionar instancias según porteros
    val estadisticaManager = remember { EstadisticaManager() }
    val estadisticaInitialized = remember { mutableStateOf(false) }

    // detectar VIP y elegir valor por defecto / habilitar opciones
    val isVip = SessionManager.getUser()?.vip == true
    val estadisticaTipo = remember { mutableStateOf(if (isVip) EstadisticaTipo.POR_INTERVENCIONES else EstadisticaTipo.EFECTIVA) }

    val activePortero = remember { mutableStateOf<Portero?>(null) }
    val selectedPos = remember { mutableStateOf<SelectedPosTime?>(null) }

    val showExitConfirm = remember { mutableStateOf(false) }
    val showFinishConfirm = remember { mutableStateOf(false) }
    val sending = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Inicializar EstadisticaManager la primera vez que se arranca el crono
    LaunchedEffect(runningState.value) {
        if (runningState.value && !estadisticaInitialized.value) {
            val porteros = MatchPorterosStore.getPorteros()
            estadisticaManager.initForPorteros(porteros)
            estadisticaInitialized.value = true
        }
    }

    // Tiempo absoluto usado para almacenar acciones
    fun currentAbsoluteSeconds(): Long = secondHalfOffset.value + secondsState.value

    // Temporizador robusto: cap y parada fiable cuando alcanza matchDurationSec (>0)
    LaunchedEffect(runningState.value, matchDurationSec) {
        if (!runningState.value) return@LaunchedEffect
        while (isActive && runningState.value) {
            delay(1000L)
            val next = secondsState.value + 1L
            if (matchDurationSec > 0L && next >= matchDurationSec) {
                secondsState.value = matchDurationSec
                runningState.value = false
                timeLimitReached.value = true
                break
            } else {
                secondsState.value = next
            }
        }
    }

    DisposableEffect(Unit) { onDispose { runningState.value = false } }

    val timerText = String.format(
        "%02d:%02d:%02d",
        currentAbsoluteSeconds() / 3600,
        (currentAbsoluteSeconds() % 3600) / 60,
        currentAbsoluteSeconds() % 60
    )
    val halfLabel = when {
        prorrogaStarted.value -> " (Prórroga)"
        secondHalfStarted.value -> " (2º tiempo)"
        else -> " (1º tiempo)"
    }
    val timerColor = if (timeLimitReached.value) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onBackground
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val fechaText = if (partido.fecha.time > 0L) sdf.format(partido.fecha) else "Sin fecha"

    // OBS: observamos la estadística global para forzar recomposición cuando cambien estadísticas.
    // Esto permite que los porcentajes por posición se actualicen en tiempo real.
    val globalStat by EstadisticaStore.global.collectAsState()

    fun pctForTipo(tipo: EstadisticaTipo, e: Estadistica): String {
        fun fmt(numerador: Int, denominador: Int): String {
            if (denominador <= 0) return "X"
            val p = (numerador.toDouble() * 100.0) / denominador.toDouble()
            return if (p % 1.0 == 0.0) String.format("%d%%", p.toInt()) else String.format("%.1f%%", p)
        }
        return when (tipo) {
            EstadisticaTipo.POR_INTERVENCIONES -> fmt(e.paradasTotales + e.noGolesTotales, e.accionesTotales)
            EstadisticaTipo.REAL -> fmt(e.paradasTotales, e.accionesReales)
            EstadisticaTipo.EFECTIVA -> fmt(e.paradasValidas, e.accionesEfectivas)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Categoría: ${partido.categoria}", color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "Equipo: ${partido.equipo}  —  Rival: ${partido.rival}", color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "Fecha: $fechaText", color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.size(12.dp))

        PorterosToggle(
            estadisticaManager = estadisticaManager,
            estadisticaTipo = estadisticaTipo.value
        ) { nuevo -> activePortero.value = nuevo }

        Spacer(modifier = Modifier.size(8.dp))

        // Selector de tipo de estadística (solo un activo).
        // Si no es VIP: por defecto EFECTIVA y las otras dos están deshabilitadas.
        EstadisticaTipoSelector(current = estadisticaTipo.value, isVip = isVip) { seleccionado ->
            if (isVip) {
                estadisticaTipo.value = seleccionado
            } else {
                // si no es VIP, forzar EFECTIVA y ignorar cambios
                estadisticaTipo.value = EstadisticaTipo.EFECTIVA
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        EstadisticaPanel(tipo = estadisticaTipo.value)

        Spacer(modifier = Modifier.size(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { runningState.value = !runningState.value }) {
                Text(if (runningState.value) "Pausar" else "Iniciar")
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = timerText + (if (matchDurationSec > 0) halfLabel else ""), color = timerColor, modifier = Modifier.padding(start = 12.dp))
        }

        Spacer(modifier = Modifier.size(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (p in Posicion.values()) {
                // obtener estadística para la posición (snapshot)
                val posEst = EstadisticaStore.getForPosition(p)
                Button(onClick = {
                    selectedPos.value = SelectedPosTime(p, currentAbsoluteSeconds())
                }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = p.abbr)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = pctForTipo(estadisticaTipo.value, posEst), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        val currentSelected = selectedPos.value
        if (currentSelected != null) {
AccionForm(
    pos = currentSelected.pos,
    tiempoSec = currentSelected.tiempoSec,
    modalidad = modalidad,
    portero = activePortero.value,
    isTimerRunning = runningState.value,
    onSave = { resultadoId, direccionId, porteroId ->
        val porteroSafe = porteroId ?: 0
        val tiempoStr = formatTimeHMS(currentSelected.tiempoSec)
        // obtener accion desde pool para minimizar GC
        val accion = AccionPool.obtain().apply {
            portero = porteroSafe
            tiempo = tiempoStr
            posicion = currentSelected.pos.id
            direccion = direccionId
            resultado = resultadoId
        }

        // Añadir acción al partido
        partido.addAccion(accion)

        // Registrar estadísticas globales por posición (emitirá cambios vía StateFlow)
        EstadisticaStore.recordAccion(currentSelected.pos, accion)

        // Registrar en EstadisticaManager (total y, si aplica, por portero)
        estadisticaManager.recordAccion(porteroId, currentSelected.pos, resultadoId)

        selectedPos.value = null
    },
    onCancel = { selectedPos.value = null }
)
        }

        Spacer(modifier = Modifier.size(16.dp))

        // Botón para comenzar 2º tiempo (solo si acabó 1º y aún no se ha empezado 2º)
        if (currentSelected == null && timeLimitReached.value && !secondHalfStarted.value && matchDurationSec > 0) {
            Button(onClick = {
                // avanzar offset y reiniciar contador para 2º tiempo
                secondHalfOffset.value = secondHalfOffset.value + matchDurationSec
                secondsState.value = 0L
                secondHalfStarted.value = true
                timeLimitReached.value = false
                runningState.value = true
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Comenzar 2º tiempo")
            }
            Spacer(modifier = Modifier.size(8.dp))
        }

        // Botón para comenzar Prórroga (si acabó 2º tiempo y aún no se ha iniciado prórroga)
        if (currentSelected == null && timeLimitReached.value && secondHalfStarted.value && !prorrogaStarted.value && matchDurationSec > 0) {
            Button(onClick = {
                // añadir offset de 2º tiempo y comenzar prórroga
                secondHalfOffset.value = secondHalfOffset.value + matchDurationSec
                secondsState.value = 0L
                prorrogaStarted.value = true
                timeLimitReached.value = false
                runningState.value = true
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Comenzar Prórroga")
            }
            Spacer(modifier = Modifier.size(8.dp))
        }

        if (currentSelected == null) {
            Button(onClick = { showFinishConfirm.value = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Finalizar partido")
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        if (currentSelected == null) {
            Button(onClick = { showExitConfirm.value = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Salir sin finalizar")
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        if (showFinishConfirm.value) {
            AlertDialog(
                onDismissRequest = { showFinishConfirm.value = false },
                title = { Text("Finalizar partido") },
                text = { Text("¿Deseas finalizar el partido? Si eres VIP se intentará enviar el partido al servidor.") },
                confirmButton = {
                    TextButton(onClick = {
                        showFinishConfirm.value = false
                        // enviar partido si VIP y luego finalizar
                        scope.launch {
                            sending.value = true
                            val ok = try {
                                PartidoRepository.enviarPartidoSiVip(partido)
                            } catch (_: Throwable) {
                                false
                            }
                            sending.value = false
                            if (ok) {
                                // limpiar estado global y terminar
                                PartidoStore.clear()
                                EstadisticaStore.clearAll()
                                // limpiar manager local
                                estadisticaManager.clear()
                                (context as? ComponentActivity)?.runOnUiThread {
                                    Toast.makeText(context, "Partido enviado y finalizado", Toast.LENGTH_SHORT).show()
                                    onFinalizar()
                                }
                            } else {
                                (context as? ComponentActivity)?.runOnUiThread {
                                    Toast.makeText(context, "Error al enviar partido", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Text("Sí")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishConfirm.value = false }) {
                        Text("No")
                    }
                }
            )
        }

        if (showExitConfirm.value) {
            AlertDialog(
                onDismissRequest = { showExitConfirm.value = false },
                title = { Text("Salir") },
                text = { Text("¿Deseas salir sin finalizar el partido?") },
                confirmButton = {
                    TextButton(onClick = {
                        showExitConfirm.value = false
                        // liberar acciones si no se desean conservar
                        // no enviamos nada, solo salimos
                        onSalir()
                    }) {
                        Text("Sí")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirm.value = false }) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
private fun AccionForm(
    pos: Posicion,
    tiempoSec: Long,
    modalidad: String,
    portero: Portero?,
    isTimerRunning: Boolean,
    onSave: (Int, Int, Int?) -> Unit,
    onCancel: () -> Unit
) {
    val ctx = LocalContext.current
    val selectedResultadoId = remember { mutableStateOf<Int?>(null) }
    val defaultDireccionId = Direcciones.ND.id
    val selectedDireccionId = remember { mutableStateOf(defaultDireccionId) }

    val allowedToSave = remember { mutableStateOf(isTimerRunning) }
    LaunchedEffect(isTimerRunning) { if (isTimerRunning) allowedToSave.value = true }

    LaunchedEffect(modalidad) {
        if (!modalidad.equals("detallado", ignoreCase = true)) {
            selectedDireccionId.value = defaultDireccionId
        }
    }

    val tiempoText = String.format("%02d:%02d:%02d", tiempoSec / 3600, (tiempoSec % 3600) / 60, tiempoSec % 60)
    val porteroDisplay = portero?.let { "${it.nombre.orEmpty()} ${it.apellidos.orEmpty()}" } ?: ""

    if (!isTimerRunning && !allowedToSave.value) {
        AlertDialog(
            onDismissRequest = { /* no dismiss */ },
            title = { Text("Tiempo parado") },
            text = { Text("El temporizador está parado. Pulsa Aceptar para permitir registrar la acción.") },
            confirmButton = {
                TextButton(onClick = { allowedToSave.value = true }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = pos.key, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = tiempoText,
            onValueChange = { },
            label = { Text("Tiempo") },
            enabled = false,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.size(8.dp))

        OutlinedTextField(
            value = porteroDisplay,
            onValueChange = { },
            label = { Text("Portero") },
            enabled = false,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.size(8.dp))

        Text(text = "Resultado", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        for (r in Resultado.values()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Checkbox(
                    checked = selectedResultadoId.value == r.id,
                    onCheckedChange = { checked ->
                        if (checked) selectedResultadoId.value = r.id else selectedResultadoId.value = null
                    }
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = r.key)
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        if (modalidad.equals("detallado", ignoreCase = true)) {
            Text(text = "Dirección", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            for (d in Direcciones.values()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Checkbox(
                        checked = selectedDireccionId.value == d.id,
                        onCheckedChange = { checked ->
                            if (checked) selectedDireccionId.value = d.id
                        }
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = d.key)
                }
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Button(
            onClick = {
                val resId = selectedResultadoId.value
                if (resId == null) {
                    Toast.makeText(ctx, "Selecciona un resultado", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!allowedToSave.value) {
                    Toast.makeText(ctx, "El tiempo está parado. Pulsa Aceptar para confirmar.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                onSave(resId, selectedDireccionId.value, portero?.id)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar") }

        Spacer(modifier = Modifier.size(8.dp))

        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}