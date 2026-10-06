package es.infvinci.driting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import es.infvinci.driting.ui.theme.DritingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DritingTheme { DritingApp() } }
    }
}

private enum class Page(val label: String, val mark: String) {
    HOME("Inicio", "⌂"), ENCOUNTERS("Encuentros", "⇄"), PROFILE("Mi perfil", "◎")
}

@Composable
private fun DritingApp() {
    val context = LocalContext.current
    val store = remember { DrivingStore(context.applicationContext) }
    var state by remember { mutableStateOf(store.load()) }
    var pageName by rememberSaveable { mutableStateOf(Page.HOME.name) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val page = Page.valueOf(pageName)
    fun update(next: DrivingState) { store.save(next); state = next }

    Scaffold(
        bottomBar = {
            if (!state.active) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Page.entries.forEach { destination ->
                    NavigationBarItem(selected = destination == page,
                        onClick = { pageName = destination.name },
                        icon = { Text(destination.mark, fontSize = 25.sp) },
                        label = { Text(destination.label) })
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { BrandHeader() }
            item { Tag("PROTOTIPO · DATOS SIMULADOS") }
            if (state.active) {
                item { Heading("Tu atención,\nen la carretera.", "Trayecto de demostración activo") }
                item { RoadIllustration() }
                item { Panel {
                    Text("Modo conducción", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Las valoraciones estarán disponibles cuando hayas terminado el trayecto y estés estacionado.")
                    Text("Esta demo no registra ubicación ni detecta vehículos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { confirmStop = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Terminar trayecto")
                    }
                } }
            } else when (page) {
                Page.HOME -> {
                    item { Heading("Mejores trayectos.\nEntre todos.", "Una comunidad para conducir con más respeto.") }
                    item { RoadIllustration() }
                    item { Panel {
                        Tag("TU PRÓXIMO TRAYECTO")
                        Text("Conduce. Conecta. Mejora.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Prueba cómo sería compartir carretera y reconocer una buena conducción.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { update(state.startTrip()) }, modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp)) { Text("Iniciar trayecto demo  →") }
                    } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric(state.completedTrips.toString(), "Trayectos demo", Modifier.weight(1f))
                        Metric(state.reviews.size.toString(), "Valoraciones enviadas", Modifier.weight(1f))
                    } }
                    if (state.encounters.any { state.canReview(it.id) }) item {
                        OutlinedButton(onClick = { pageName = Page.ENCOUNTERS.name }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ver encuentros del último trayecto")
                        }
                    }
                    item { Principles() }
                }
                Page.ENCOUNTERS -> {
                    item { Heading("Cada encuentro\npuede sumar.", "Encuentros del último trayecto de demostración.") }
                    if (state.encounters.isEmpty()) item { Panel {
                        Text("Tu primer encuentro empieza aquí", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Inicia y termina un trayecto demo para descubrir tres conductores ficticios y probar las valoraciones.")
                        Button(onClick = { update(state.startTrip()) }) { Text("Probar un trayecto") }
                    } }
                    items(state.encounters, key = { it.id }) { encounter ->
                        val review = state.reviews.firstOrNull { it.encounterId == encounter.id }
                        Panel {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Avatar(encounter.alias.takeLast(4))
                                Column(Modifier.weight(1f)) {
                                    Text(encounter.alias, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(encounter.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (review == null) {
                                OutlinedButton(onClick = { selectedId = encounter.id }, modifier = Modifier.fillMaxWidth()) { Text("Valorar encuentro") }
                            } else {
                                Text(review.behaviour.title, fontWeight = FontWeight.Medium)
                                Text(if (review.behaviour.positive) "Reconocimiento guardado en la demo" else "Pendiente de revisión · sin efecto en reputación",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    item { Panel {
                        Text("Una coincidencia no es una prueba", fontWeight = FontWeight.Bold)
                        Text("Estos perfiles son ficticios. En una versión real habría que verificar la coincidencia y la identidad del participante antes de atribuirle un comportamiento.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                }
                Page.PROFILE -> {
                    item { Heading("Tu forma de conducir\ndeja huella.", "Perfil de demostración · sin cuenta conectada") }
                    item { Panel {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Avatar("TÚ")
                            Column { Text("Mi perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Participante demo", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        HorizontalDivider()
                        Text("Sin puntuación todavía", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Tu reputación se calculará a partir de valoraciones recibidas y verificadas. Las valoraciones que envías no aumentan tu puntuación.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                    item { Text("Tu actividad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric(state.completedTrips.toString(), "Trayectos terminados", Modifier.weight(1f))
                        Metric(state.reviews.size.toString(), "Valoraciones enviadas", Modifier.weight(1f))
                    } }
                    if (state.reviews.isEmpty()) item { Text("Aún no has enviado valoraciones. Prueba tu primer trayecto desde Inicio.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(state.reviews.asReversed()) { review -> Panel {
                        Tag(if (review.behaviour.positive) "RECONOCIMIENTO" else "PENDIENTE DE REVISIÓN")
                        Text(review.behaviour.title, fontWeight = FontWeight.Bold)
                        Text("${review.alias} · trayecto demo ${review.encounterId.substringBefore('-')}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!review.behaviour.positive) Text("No modifica la reputación. Esta demo no incluye un servicio de moderación.", style = MaterialTheme.typography.bodySmall)
                    } }
                    item { Principles() }
                    item { TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Borrar datos de demostración") } }
                }
            }
            item { Text("Driting · Compartir carretera, construir confianza", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    val selected = state.encounters.firstOrNull { it.id == selectedId }
    if (selected != null && state.canReview(selected.id)) ReviewDialog(selected, onDismiss = { selectedId = null }) { behaviour ->
        update(state.review(selected.id, behaviour)); selectedId = null
    }
    if (confirmStop) AlertDialog(onDismissRequest = { confirmStop = false },
        title = { Text("¿Has terminado y estás estacionado?") },
        text = { Text("Confírmalo para salir del modo conducción y ver los encuentros simulados.") },
        confirmButton = { TextButton(onClick = {
            update(state.finishTrip()); pageName = Page.ENCOUNTERS.name; confirmStop = false
        }) { Text("Sí, estoy estacionado") } },
        dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Seguir trayecto") } })
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("Borrar la demostración") }, text = { Text("Se borrarán los trayectos y las valoraciones guardadas en este móvil.") },
        confirmButton = { TextButton(onClick = { update(DrivingState()); confirmReset = false }) { Text("Borrar datos") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } })
}

@Composable
private fun BrandHeader() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text("d", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        }
        Text("driting", fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Spacer(Modifier.weight(1f))
        Text("EN COMUNIDAD", fontSize = 10.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Heading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Tag(text: String) {
    Text(text, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 9.dp, vertical = 5.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Avatar(label: String) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun Principles() {
    Panel {
        Text("La confianza va primero", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text("01  Valora solo cuando estés estacionado.\n02  Una valoración por encuentro.\n03  Las incidencias requieren revisión.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 26.sp)
        Text("Datos locales y ficticios. Sin matrículas, GPS ni publicación de reseñas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RoadIllustration() {
    val roadColor = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.primaryContainer
    Box(Modifier.fillMaxWidth().height(172.dp).clip(RoundedCornerShape(24.dp)).background(background)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            listOf(0.12f to 0.25f, 0.86f to 0.26f, 0.18f to 0.84f, 0.91f to 0.82f).forEach { (x, y) ->
                drawCircle(roadColor.copy(alpha = 0.09f), radius = 27.dp.toPx(), center = Offset(w * x, h * y))
                drawCircle(roadColor.copy(alpha = 0.16f), radius = 13.dp.toPx(), center = Offset(w * x, h * y))
            }
            val road = Path().apply { moveTo(w * 0.36f, h * 1.15f); cubicTo(w * 0.25f, h * 0.4f, w * 0.78f, h * 0.65f, w * 0.61f, -h * 0.15f) }
            drawPath(road, roadColor.copy(alpha = 0.12f), style = Stroke(width = 92.dp.toPx()))
            drawPath(road, roadColor, style = Stroke(width = 80.dp.toPx()))
            drawPath(road, Color.White.copy(alpha = 0.6f), style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 10.dp.toPx()))))
            fun car(x: Float, y: Float, color: Color) {
                val cw = 19.dp.toPx(); val ch = 33.dp.toPx()
                drawRoundRect(Color.Black.copy(alpha = 0.14f), Offset(x - cw / 2 + 3, y - ch / 2 + 4), Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
                drawRoundRect(color, Offset(x - cw / 2, y - ch / 2), Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
                drawRoundRect(roadColor, Offset(x - cw * 0.35f, y - ch * 0.23f), Size(cw * 0.7f, ch * 0.25f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
            car(w * 0.48f, h * 0.77f, Color(0xFFE4EFB3))
            car(w * 0.56f, h * 0.28f, Color.White)
        }
        Text("UN CAMINO COMPARTIDO", Modifier.align(Alignment.BottomEnd).padding(14.dp), fontSize = 9.sp, letterSpacing = 1.sp, color = roadColor)
    }
}

@Composable
private fun ReviewDialog(encounter: Encounter, onDismiss: () -> Unit, onSubmit: (Behaviour) -> Unit) {
    var chosenName by rememberSaveable(encounter.id) { mutableStateOf<String?>(null) }
    val chosen = Behaviour.entries.firstOrNull { it.name == chosenName }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Tag("VALORACIÓN DE DEMOSTRACIÓN")
                Text("¿Qué has observado?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(encounter.alias, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Selecciona un comportamiento. No se publica ni afecta a personas reales.", style = MaterialTheme.typography.bodySmall)
                Behaviour.entries.forEach { behaviour ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (chosen == behaviour) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { chosenName = behaviour.name }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = chosen == behaviour, onClick = { chosenName = behaviour.name })
                        Column(Modifier.weight(1f)) {
                            Text(behaviour.title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text(behaviour.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (chosen?.positive == false) Text("Esta incidencia quedará pendiente de revisión. No reducirá ninguna puntuación.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Button(onClick = { chosen?.let(onSubmit) }, enabled = chosen != null, modifier = Modifier.fillMaxWidth()) { Text("Guardar valoración demo") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        }
    }
}
