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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.misidoro.app_savemetric.EstadisticaTipo
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import kotlin.collections.get
import androidx.compose.ui.window.Dialog
import kotlin.compareTo
import kotlin.div


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

        val isVip = SessionManager.getUser()?.vip == true

        setContent {
            App_savemetricTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.est_partido),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )

                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.matchParentSize()
                    ) {
                        PartidoScreen(
                            partido = partido,
                            modalidad = modalidadSession,
                            isVip = isVip,
                            matchDurationSec = matchDurationSec,
                            onFinalizar = { finish() },
                            onSalir = { finish() }
                        )
                    }
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


/***
 * sección para seleccionar portero activo
 * se llama en partidoscreen, pero el control de estado se hace aqui
 */
@Composable
private fun PorterosToggle(
    estadisticaManager: EstadisticaManager,
    estadisticaTipo: EstadisticaTipo,
    onActiveChanged: (Portero?) -> Unit = {}
) {
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
            EstadisticaTipo.POR_INTERVENCIONES -> fmtPct(
                e.paradasTotales + e.noGolesTotales,
                e.accionesTotales
            )

            EstadisticaTipo.REAL -> fmtPct(e.paradasTotales, e.accionesReales)
            EstadisticaTipo.EFECTIVA -> fmtPct(e.paradasValidas, e.accionesEfectivas)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // botones en una misma fila y ocupando todo el ancho
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until showCount) {
                val p = porteros[i]
                val isActive = activeIndex.value == i
                val estad = estadisticaManager.getForPortero(p.id) ?: Estadistica()

                @Suppress("UNUSED_VARIABLE")
                val pctText = run {
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
                Spacer(modifier = Modifier.size(0.dp))
            }
        }
    }
}


/***
 * Controlador de los botones de estadistica
 * se llama en partidoscreen, pero el control de estado se hace aqui
 */
@Composable
private fun EstadisticaTipoSelector(
    current: EstadisticaTipo,
    isVip: Boolean,
    onSelect: (EstadisticaTipo) -> Unit
) {
    val activeColor = Color(0xFF2E7D32)
    val inactiveColor = Color(0xFFBDBDBD)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tipo de estadística",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

/***
 * Panel de estadísticas según el tipo seleccionado
 * Se llama en partidoscreen pero se controla aqui
 */
@Composable
fun EstadisticaPanel(tipo: EstadisticaTipo) {
    val s by EstadisticaStore.global.collectAsState()

    fun pct(numerador: Int, denominador: Int): String {
        if (denominador <= 0) return "0%"
        val p = (numerador.toDouble() * 100.0) / denominador.toDouble()
        val texto =
            if (p % 1.0 == 0.0) String.format("%d%%", p.toInt()) else String.format("%.1f%%", p)
        return texto
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        val entries: List<Pair<String, String>> = when (tipo) {
            EstadisticaTipo.POR_INTERVENCIONES -> listOf(
                "Total lanzamientos" to "${s.accionesTotales}",
                "Goles recibidos" to "${s.goles}",
                "Total Paradas" to "${s.paradasTotales}",
                "Errores provocados" to "${s.noGolesTotales}",
                "%Intervenciones" to pct(s.paradasTotales + s.noGolesTotales, s.accionesTotales)
            )

            EstadisticaTipo.REAL -> listOf(
                "Total lanzamientos a puerta" to "${s.accionesReales}",
                "Goles recibidos" to "${s.goles}",
                "Total intervenciones reales" to "${s.paradasTotales}",
                "%real" to pct(s.paradasTotales, s.accionesReales)
            )

            EstadisticaTipo.EFECTIVA -> listOf(
                "Total lanzamientos a puerta válidos" to "${s.accionesEfectivas}",
                "Goles recibidos" to "${s.goles}",
                "Total intervenciones reales" to "${s.paradasValidas}",
                "%efectivo" to pct(s.paradasValidas, s.accionesEfectivas)
            )
        }

        // Encabezados (primera fila)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((idx, pair) in entries.withIndex()) {
                val (header, _) = pair
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            if (idx % 2 == 0)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.04f)
                        )
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }

        // Valores (segunda fila)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((idx, pair) in entries.withIndex()) {
                val (_, value) = pair
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
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
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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

/***
 * Listado de botones para seleccionar posición.
 * Se llama en partidoscreen, pero el control del los botones se hace aquí.
 */

@Composable
fun PosicionesGrid(
    onSelectPos: (Posicion) -> Unit,
    isBasico: Boolean,
    registrarSinTiempo: Boolean,
    currentTimeSec: Long,
    estadisticaManager: EstadisticaManager,
    estadisticaTipo: EstadisticaTipo
) {
    // mantiene la dependencia para forzar recomposición global si hace falta
    val statsVersion by EstadisticaStore.version.collectAsState()
    val porteros = MatchPorterosStore.getPorteros()

    // mapa de (fila,col) 1-based -> lista de abreviaturas
    val placements = mutableMapOf<Pair<Int, Int>, MutableList<String>>().apply {
        put(1 to 1, mutableListOf("EIC"))
        put(2 to 2, mutableListOf("EIA"))
        put(3 to 3, mutableListOf("LI"))
        put(3 to 5, mutableListOf("LD"))
        put(2 to 6, mutableListOf("EDA"))
        put(1 to 7, mutableListOf("EDC"))
        put(3 to 4, mutableListOf("6M", "7M"))
        put(4 to 3, mutableListOf("9M"))
        put(4 to 4, mutableListOf("9M", "CA"))
        put(4 to 5, mutableListOf("9M"))
        put(2 to 4, mutableListOf("ND"))
    }

    fun fmtPct(n: Int, d: Int): String {
        if (d <= 0) return "X"
        val p = (n.toDouble() * 100.0) / d.toDouble()
        return if (p % 1.0 == 0.0) String.format("%d%%", p.toInt()) else String.format("%.1f%%", p)
    }

    fun pctForStat(tipo: EstadisticaTipo, e: Estadistica?): String {
        if (e == null) return "X"
        return when (tipo) {
            EstadisticaTipo.POR_INTERVENCIONES -> fmtPct(
                e.paradasTotales + e.noGolesTotales,
                e.accionesTotales
            )

            EstadisticaTipo.REAL -> fmtPct(e.paradasTotales, e.accionesReales)
            EstadisticaTipo.EFECTIVA -> fmtPct(e.paradasValidas, e.accionesEfectivas)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)),
                RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.pista3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
        )
        val cellSize = maxWidth / 7f

        Column(modifier = Modifier.fillMaxWidth()) {
            for (r in 1..4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (c in 1..7) {
                        val labels = placements[r to c] ?: emptyList()
                        val visibleLabels = if (isBasico) labels.filter { it == "ND" } else labels

                        Box(
                            modifier = Modifier
                                .width(cellSize)
                                .height(cellSize)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                    ), RoundedCornerShape(6.dp)
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (visibleLabels.isEmpty()) {
                                Spacer(modifier = Modifier.size(0.dp))
                            } else {
                                Column(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .wrapContentHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    for (label in visibleLabels) {
                                        val pos = Posicion.values().firstOrNull { it.abbr == label }
                                        if (pos == null) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else {
                                            // ahora usamos el StateFlow por posición para recibir actualizaciones
                                            val posFlow = EstadisticaStore.getForPositionFlow(pos)
                                            val posStat by posFlow.collectAsState()

                                            val totalPct = pctForStat(estadisticaTipo, posStat)

                                            val porteroPctText = if (porteros.size > 1) {
                                                // construir porcentajes por portero (abreviado)
                                                porteros.joinToString(" ") { p ->
                                                    val pst =
                                                        estadisticaManager.getForPorteroAndPos(
                                                            p.id,
                                                            pos
                                                        ) ?: Estadistica()
                                                    val nameShort =
                                                        p.nombre?.split(" ")?.firstOrNull()
                                                            .orEmpty()
                                                    "$nameShort:${pctForStat(estadisticaTipo, pst)}"
                                                }
                                            } else {
                                                ""
                                            }

                                            Button(
                                                onClick = { onSelectPos(pos) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(text = pos.abbr)
                                                    Text(
                                                        text = totalPct,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )

                                                    if (porteros.size > 1) {
                                                        val p0 =
                                                            estadisticaManager.getForPorteroAndPos(
                                                                porteros[0].id,
                                                                pos
                                                            ) ?: Estadistica()
                                                        val p1 =
                                                            estadisticaManager.getForPorteroAndPos(
                                                                porteros[1].id,
                                                                pos
                                                            ) ?: Estadistica()
                                                        val pct0 = pctForStat(estadisticaTipo, p0)
                                                        val pct1 = pctForStat(estadisticaTipo, p1)

                                                        Row(
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = pct0,
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                                ),
                                                                color = Color(0xFF2E7D32)
                                                            )
                                                            Spacer(modifier = Modifier.size(6.dp))
                                                            Text(
                                                                text = "|",
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                            Spacer(modifier = Modifier.size(6.dp))
                                                            Text(
                                                                text = pct1,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onBackground
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.size(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(6.dp))
            }
        }
    }
}

@Composable
private fun PartidoScreen(
    partido: Partido,
    modalidad: String,
    isVip: Boolean,
    matchDurationSec: Long = -1L,
    onFinalizar: () -> Unit,
    onSalir: () -> Unit
) {
    val secondsState = remember { mutableStateOf(0L) }
    val runningState = remember { mutableStateOf(false) }
    val timeLimitReached = remember { mutableStateOf(false) }
    val secondHalfOffset = remember { mutableStateOf(0L) }
    val secondHalfStarted = remember { mutableStateOf(false) }
    val prorrogaStarted = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val lifecycleOwner = LocalLifecycleOwner.current
    val modalidadState = remember { mutableStateOf(modalidad) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                modalidadState.value = prefs.getString("modalidad", "") ?: ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val registrarSinTiempoState =
        remember { mutableStateOf(prefs.getBoolean("registrar_sin_tiempo", false)) }
    LaunchedEffect(Unit) {
        registrarSinTiempoState.value = prefs.getBoolean("registrar_sin_tiempo", false)
        if (registrarSinTiempoState.value) runningState.value = false
    }

    val estadisticaManager = remember {
        EstadisticaManager().apply {
            initForPorteros(MatchPorterosStore.getPorteros())
        }
    }

    val isVipLocal = SessionManager.getUser()?.vip == true
    val estadisticaTipo =
        remember { mutableStateOf(if (isVipLocal) EstadisticaTipo.POR_INTERVENCIONES else EstadisticaTipo.EFECTIVA) }

    val activePortero = remember { mutableStateOf<Portero?>(null) }
    val selectedPos = remember { mutableStateOf<SelectedPosTime?>(null) }

    val showExitConfirm = remember { mutableStateOf(false) }
    val showFinishConfirm = remember { mutableStateOf(false) }
    val sending = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun currentAbsoluteSeconds(): Long = secondHalfOffset.value + secondsState.value

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
    val timerColor =
        if (timeLimitReached.value) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onBackground
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val fechaText = if (partido.fecha.time > 0L) sdf.format(partido.fecha) else "Sin fecha"

    val globalStat by EstadisticaStore.global.collectAsState()

    fun pctForTipo(tipo: EstadisticaTipo, e: Estadistica): String {
        fun fmt(numerador: Int, denominador: Int): String {
            if (denominador <= 0) return "X"
            val p = (numerador.toDouble() * 100.0) / denominador.toDouble()
            return if (p % 1.0 == 0.0) String.format("%d%%", p.toInt()) else String.format(
                "%.1f%%",
                p
            )
        }
        return when (tipo) {
            EstadisticaTipo.POR_INTERVENCIONES -> fmt(
                e.paradasTotales + e.noGolesTotales,
                e.accionesTotales
            )

            EstadisticaTipo.REAL -> fmt(e.paradasTotales, e.accionesReales)
            EstadisticaTipo.EFECTIVA -> fmt(e.paradasValidas, e.accionesEfectivas)
        }
    }

    val isBasico = modalidadState.value.equals("basico", ignoreCase = true)
    val registrarSinTiempo = registrarSinTiempoState.value

    // Estado para mostrar/ocultar la cabecera
    val showHeader = remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cuando la cabecera está oculta mostramos un botón pequeño para recuperarla
        if (!showHeader.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showHeader.value = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(36.dp)
                ) {
                    Text("Mostrar cabecera")
                }
            }
        }
        // --- CABECERA (condicional) ---
        if (showHeader.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.Top
                ) {
                    // Columna izquierda
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .padding(12.dp)
                    )  {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Encabezado de la "tabla"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val headers = listOf("Fecha", "Categoría", "Equipo", "Rival")
                                for ((idx, h) in headers.withIndex()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                                ),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (idx % 2 == 0)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.04f)
                                            )
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = h,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Fila de valores correspondiente (misma orden que encabezados)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val values = listOf(
                                    fechaText,
                                    partido.categoria ?: "",
                                    partido.equipo ?: "",
                                    partido.rival ?: ""
                                )
                                for (v in values) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
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
                                            text = v,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    // Columna derecha
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .padding(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Button(
                                        onClick = { runningState.value = !runningState.value },
                                        enabled = !registrarSinTiempoState.value,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (runningState.value) Color(
                                                0xFF2E7D32
                                            ) else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(if (runningState.value) "Pausar" else "Iniciar")
                                    }

                                    Spacer(modifier = Modifier.size(8.dp))

                                    Button(
                                        onClick = {
                                            val newVal = !registrarSinTiempoState.value
                                            prefs.edit().putBoolean("registrar_sin_tiempo", newVal)
                                                .apply()
                                            registrarSinTiempoState.value = newVal
                                            if (newVal) {
                                                runningState.value = false
                                                timeLimitReached.value = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (registrarSinTiempoState.value) Color(
                                                0xFFD32F2F
                                            ) else Color(0xFF4CAF50),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(if (registrarSinTiempoState.value) "Temporizador OFF" else "Temporizador ON")
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = timerText + (if (matchDurationSec > 0) halfLabel else ""),
                                        color = timerColor
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Modalidad: ${modalidadState.value}",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (isVipLocal) {
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Button(onClick = {
                                            val intent =
                                                Intent(context, ModalidadActivity::class.java)
                                            context.startActivity(intent)
                                        }) {
                                            Text("Cambiar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Botón para ocultar la cabecera, alineado arriba a la derecha del Box
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(end = 8.dp, top = 4.dp)
                    ) {
                        Button(
                            onClick = { showHeader.value = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Ocultar cabecera")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        //SEGUNDA SECCIÓN:  ESTADÍSTICAS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                    ), RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                            ), RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EstadisticaTipoSelector(
                            current = estadisticaTipo.value,
                            isVip = isVipLocal
                        ) { seleccionado ->
                            if (isVipLocal) {
                                estadisticaTipo.value = seleccionado
                            } else {
                                estadisticaTipo.value = EstadisticaTipo.EFECTIVA
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                            ), RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EstadisticaPanel(tipo = estadisticaTipo.value)
                    }
                }
            }
        }

        //SECCION 3: PORTEROS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                    ), RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                            ), RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PorterosToggle(
                            estadisticaManager = estadisticaManager,
                            estadisticaTipo = estadisticaTipo.value
                        ) { nuevo -> activePortero.value = nuevo }
                    }
                }
            }
        }





        Spacer(modifier = Modifier.size(8.dp))
//SECCION 4: CAMPO CON POSICIONES
        PosicionesGrid(
            onSelectPos = { pos ->
                selectedPos.value =
                    SelectedPosTime(pos, if (registrarSinTiempo) 0L else currentAbsoluteSeconds())
            },
            isBasico = isBasico,
            registrarSinTiempo = registrarSinTiempo,
            currentTimeSec = currentAbsoluteSeconds(),
            estadisticaManager = estadisticaManager,
            estadisticaTipo = estadisticaTipo.value
        )

        Spacer(modifier = Modifier.size(12.dp))

        val currentSelected = selectedPos.value
        if (currentSelected != null) {
            val sel = currentSelected
            Dialog(onDismissRequest = { selectedPos.value = null }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentWidth()
                        .wrapContentHeight()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AccionForm(
                            pos = sel.pos,
                            tiempoSec = sel.tiempoSec,
                            modalidad = modalidadState.value,
                            portero = activePortero.value,
                            isTimerRunning = registrarSinTiempo || runningState.value,
                            onSave = { resultadoId, direccionId, porteroId ->
                                val porteroSafe = porteroId ?: 0
                                val tiempoStr =
                                    if (registrarSinTiempo) "00:00:00" else formatTimeHMS(sel.tiempoSec)
                                val accion = AccionPool.obtain().apply {
                                    portero = porteroSafe
                                    tiempo = tiempoStr
                                    posicion = sel.pos.id
                                    direccion = direccionId
                                    resultado = resultadoId
                                }

                                partido.addAccion(accion)
                                EstadisticaStore.recordAccion(sel.pos, accion)
                                estadisticaManager.recordAccion(porteroId, sel.pos, resultadoId)

                                selectedPos.value = null
                            },
                            onCancel = { selectedPos.value = null }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        // (resto de botones y diálogos idénticos a lo anterior)
        if (currentSelected == null && timeLimitReached.value && !secondHalfStarted.value && matchDurationSec > 0 && !registrarSinTiempo) {
            Button(onClick = {
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

        if (currentSelected == null && timeLimitReached.value && secondHalfStarted.value && !prorrogaStarted.value && matchDurationSec > 0 && !registrarSinTiempo) {
            Button(onClick = {
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
            val tieneAcciones = partido.acciones.isNotEmpty() // true si hay al menos una acción registrada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Cancelar partido" - izquierda (1/3)
                Button(
                    onClick = { showExitConfirm.value = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Cancelar partido")
                    }
                }

                // espacio central (1/3)
                Spacer(modifier = Modifier.weight(1f))

                // "Finalizar partido" - derecha (1/3)
Button(
    onClick = { showFinishConfirm.value = true },
    enabled = tieneAcciones,
    modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF2E7D32),
        contentColor = Color.White
    )
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(if (tieneAcciones) "Finalizar partido" else "Finalizar partido (requiere acción)")
    }
}
            }
        }
        Spacer(modifier = Modifier.size(16.dp))


        if (showFinishConfirm.value) {
            AlertDialog(
                onDismissRequest = { /* no dismiss */ },
                title = { Text("Finalizar partido") },
                text = { Text(if (isVipLocal) "¿Deseas finalizar el partido y ver el resumen?" else "¿Desea finalizar el partido?") },
                confirmButton = {
                    TextButton(onClick = {
                        showFinishConfirm.value = false
                        sending.value = true

                        scope.launch {
                            var falloEnvio = false
                            try {
                                PartidoRepository.enviarPartidoSiVip(partido)
                            } catch (t: Throwable) {
                                falloEnvio = true
                            } finally {
                                val destIntent = if (SessionManager.getUser()?.vip == true) {
                                    Intent(context, ResumenActivity::class.java).apply {
                                        putExtra("envio_fallado", falloEnvio)
                                    }
                                } else {
                                    Intent(context, InicioActivity::class.java)
                                }
                                context.startActivity(destIntent)
                                (context as? Activity)?.finish()
                            }
                        }
                    }) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishConfirm.value = false }) {
                        Text("Cancelar")
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

    val tiempoText =
        String.format("%02d:%02d:%02d", tiempoSec / 3600, (tiempoSec % 3600) / 60, tiempoSec % 60)
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

    val activeColor = Color(0xFF2E7D32)
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val lineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = pos.key,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = tiempoText,
                onValueChange = { },
                label = { Text("Tiempo") },
                enabled = false,
                modifier = Modifier.weight(0.3f)
            )

            OutlinedTextField(
                value = porteroDisplay,
                onValueChange = { },
                label = { Text("Portero") },
                enabled = false,
                modifier = Modifier.weight(0.7f)
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = "Resultado",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (r in Resultado.values()) {
                val isSelected = selectedResultadoId.value == r.id
                Button(
                    onClick = { selectedResultadoId.value = if (isSelected) null else r.id },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) activeColor else inactiveColor,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = r.key)
                }
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        if (modalidad.equals("detallado", ignoreCase = true)) {
            Text(
                text = "Dirección",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Tamaño del checkbox (ajustable)
            val cbSize = 28.dp
            val outerSize = cbSize * 3
            val cellHeight = outerSize / 3f
            val strokePx = with(LocalDensity.current) { 1.dp.toPx() }

            // Buscar entradas específicas
            val allDirs = Direcciones.values()
            val ndDir = Direcciones.ND
            val roscaDir = allDirs.firstOrNull {
                it.key.equals("Rosca", ignoreCase = true) || (it.desc?.equals(
                    "rosca",
                    true
                ) == true)
            }
            val vaselinaDir = allDirs.firstOrNull {
                it.key.equals(
                    "Vaselina",
                    ignoreCase = true
                ) || (it.desc?.equals("vaselina", true) == true)
            }

            // Tres columnas que ocupan todo el ancho
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Columna 1: ND alineado a la fila central (segunda)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tres "celdas" verticales para posicionamiento exacto (alto = outerSize)
                    Column(modifier = Modifier.height(outerSize)) {
                        // fila 1 (vacía)
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .fillMaxWidth()
                        ) { /* vacía */ }
                        // fila 2 (ND)
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val checked = selectedDireccionId.value == ndDir.id
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { now ->
                                        if (now) selectedDireccionId.value = ndDir.id
                                        else selectedDireccionId.value = defaultDireccionId
                                    },
                                    modifier = Modifier.size(cbSize)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(text = ndDir.desc ?: ndDir.key)
                            }
                        }
                        // fila 3 (vacía)
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .fillMaxWidth()
                        ) { /* vacía */ }
                    }
                }

                // Columna 2: tabla 3x3 (portería) centrada
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(outerSize)
                            .clip(RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, lineColor), RoundedCornerShape(6.dp))
                    ) {
                        // Líneas internas
                        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            val thirdW = w / 3f
                            val thirdH = h / 3f
                            val c = lineColor
                            drawLine(
                                color = c,
                                start = androidx.compose.ui.geometry.Offset(thirdW, 0f),
                                end = androidx.compose.ui.geometry.Offset(thirdW, h),
                                strokeWidth = strokePx
                            )
                            drawLine(
                                color = c,
                                start = androidx.compose.ui.geometry.Offset(2f * thirdW, 0f),
                                end = androidx.compose.ui.geometry.Offset(2f * thirdW, h),
                                strokeWidth = strokePx
                            )
                            drawLine(
                                color = c,
                                start = androidx.compose.ui.geometry.Offset(0f, thirdH),
                                end = androidx.compose.ui.geometry.Offset(w, thirdH),
                                strokeWidth = strokePx
                            )
                            drawLine(
                                color = c,
                                start = androidx.compose.ui.geometry.Offset(0f, 2f * thirdH),
                                end = androidx.compose.ui.geometry.Offset(w, 2f * thirdH),
                                strokeWidth = strokePx
                            )
                        }

                        // Checkboxes centrados en cada celda, tamaño cbSize
                        Column(modifier = Modifier.matchParentSize()) {
                            for (row in 0 until 3) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    for (col in 0 until 3) {
                                        val idx = row * 3 + col
                                        val d = allDirs.getOrNull(idx)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (d != null) {
                                                val checked = selectedDireccionId.value == d.id
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { checkedNow ->
                                                        if (checkedNow) selectedDireccionId.value =
                                                            d.id
                                                        else selectedDireccionId.value =
                                                            defaultDireccionId
                                                    },
                                                    modifier = Modifier.size(cbSize)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Columna 3: Rosca arriba (fila 1) y Vaselina abajo (fila 3)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(modifier = Modifier.height(outerSize)) {
                        // fila 1 (Rosca)
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (roscaDir != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val checked = selectedDireccionId.value == roscaDir.id
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { now ->
                                            if (now) selectedDireccionId.value = roscaDir.id
                                            else selectedDireccionId.value = defaultDireccionId
                                        },
                                        modifier = Modifier.size(cbSize)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(text = roscaDir.desc ?: roscaDir.key)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) { /* placeholder si falta */ }
                            }
                        }
                        // fila 2 (vacía)
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .fillMaxWidth()
                        ) { /* vacía */ }
                        // fila 3 (Vaselina)
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (vaselinaDir != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val checked = selectedDireccionId.value == vaselinaDir.id
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { now ->
                                            if (now) selectedDireccionId.value = vaselinaDir.id
                                            else selectedDireccionId.value = defaultDireccionId
                                        },
                                        modifier = Modifier.size(cbSize)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(text = vaselinaDir.desc ?: vaselinaDir.key)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) { /* placeholder si falta */ }
                            }
                        }
                    }
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
                    Toast.makeText(
                        ctx,
                        "El tiempo está parado. Pulsa Aceptar para confirmar.",
                        Toast.LENGTH_SHORT
                    ).show()
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