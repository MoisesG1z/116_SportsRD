package com.sports.redaccion

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.sports.redaccion.data.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

// Styling Colors (Sporty Dark Theme)
val DarkBg = Color(0xFF0C0C0E)
val CardBg = Color(0xFF16161A)
val BorderColor = Color(0xFF26262F)
val NeonGreen = Color(0xFF00FF66)
val NeonYellow = Color(0xFFCCFF00)
val SoftGray = Color(0xFFA0A0AA)
val PureWhite = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf("dashboard") }
    
    // Bind state from SocialFeedMonitor
    val isRunning by SocialFeedMonitor.isRunning
    val totalSent by SocialFeedMonitor.totalSent
    var webhookUrl by SocialFeedMonitor.webhookUrl
    val channels = SocialFeedMonitor.channels
    val logs = SocialFeedMonitor.logMessages

    var isServerActive by remember { mutableStateOf(false) }

    // Sync loop con el Servidor Backend Ktor
    LaunchedEffect(Unit) {
        val httpClient = io.ktor.client.HttpClient()
        while (true) {
            try {
                val response: io.ktor.client.statement.HttpResponse = httpClient.get("http://localhost:8080/api/status")
                if (response.status.value in 200..299) {
                    isServerActive = true
                    val body = response.bodyAsText()
                    val serverRunning = body.contains("\"isRunning\":true")
                    if (SocialFeedMonitor.isRunning.value != serverRunning) {
                        SocialFeedMonitor.isRunning.value = serverRunning
                    }
                    val serverWebhook = extractWebhookUrlFromJson(body)
                    if (SocialFeedMonitor.webhookUrl.value != serverWebhook && serverWebhook.isNotBlank()) {
                        SocialFeedMonitor.webhookUrl.value = serverWebhook
                    }
                    
                    val logsResponse = httpClient.get("http://localhost:8080/api/logs")
                    val logsJson = logsResponse.bodyAsText()
                    val serverLogs = parseLogsFromJson(logsJson)
                    SocialFeedMonitor.logMessages.clear()
                    SocialFeedMonitor.logMessages.addAll(serverLogs)
                } else {
                    isServerActive = false
                }
            } catch (e: Exception) {
                isServerActive = false
            }
            kotlinx.coroutines.delay(2000)
        }
    }


    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBg,
            surface = CardBg,
            primary = NeonGreen,
            secondary = NeonYellow,
            onBackground = PureWhite,
            onSurface = PureWhite
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "116SPORTS",
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonYellow,
                                letterSpacing = 2.sp,
                                fontSize = 22.sp
                            )
                            Text(
                                "REDACCIÓN",
                                fontWeight = FontWeight.Light,
                                color = PureWhite,
                                letterSpacing = 1.sp,
                                fontSize = 18.sp
                            )
                        }
                    },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Server Connection Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isServerActive) NeonYellow.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f))
                                    .border(1.dp, if (isServerActive) NeonYellow else Color.Gray, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (isServerActive) "SERVIDOR 24/7: CONECTADO" else "MONITOR LOCAL",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Monitor Running Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isRunning) NeonGreen.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f))
                                    .border(1.dp, if (isRunning) NeonGreen else Color.Red, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isRunning) NeonGreen else Color.Red)
                                    )
                                    Text(
                                        if (isRunning) "MONITOR: ACTIVO" else "MONITOR: DETENIDO",
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBg,
                        titleContentColor = PureWhite
                    )
                )
            },
            bottomBar = {
                // Sleek custom tab bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(0.dp))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabButton("Dashboard", activeTab == "dashboard") { activeTab = "dashboard" }
                    TabButton("Editor de Guiones", activeTab == "editor") { activeTab = "editor" }
                    TabButton("Canales RSS", activeTab == "channels") { activeTab = "channels" }
                }
            },
            containerColor = DarkBg
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    "dashboard" -> DashboardScreen(
                        isRunning = isRunning,
                        totalSent = totalSent,
                        webhookUrl = webhookUrl,
                        isServerActive = isServerActive,
                        onWebhookChange = { newUrl ->
                            webhookUrl = newUrl
                            if (isServerActive) {
                                coroutineScope.launch {
                                    try {
                                        val client = io.ktor.client.HttpClient()
                                        client.post("http://localhost:8080/api/webhook") {
                                            setBody("{\"webhookUrl\":\"$newUrl\"}")
                                        }
                                    } catch (e: Exception) {}
                                }
                            }
                        },
                        logs = logs,
                        onStart = {
                            coroutineScope.launch {
                                if (isServerActive) {
                                    try {
                                        val client = io.ktor.client.HttpClient()
                                        client.post("http://localhost:8080/api/start")
                                    } catch (e: Exception) {}
                                } else {
                                    SocialFeedMonitor.start(coroutineScope)
                                }
                            }
                        },
                        onStop = {
                            coroutineScope.launch {
                                if (isServerActive) {
                                    try {
                                        val client = io.ktor.client.HttpClient()
                                        client.post("http://localhost:8080/api/stop")
                                    } catch (e: Exception) {}
                                } else {
                                    SocialFeedMonitor.stop()
                                }
                            }
                        }
                    )
                    "editor" -> EditorScreen(webhookUrl)
                    "channels" -> ChannelsScreen(channels, isServerActive)
                }
            }
        }
    }
}

@Composable
fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (selected) NeonGreen else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) NeonGreen else SoftGray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun DashboardScreen(
    isRunning: Boolean,
    totalSent: Int,
    webhookUrl: String,
    isServerActive: Boolean,
    onWebhookChange: (String) -> Unit,
    logs: List<LogMessage>,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isTestingWebhook by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Boolean?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.weight(1f).border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (isServerActive) "ESTADO DEL MONITOR (SERVIDOR 24/7)" else "ESTADO DEL MONITOR LOCAL", color = SoftGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (isRunning) "Activo" else "Apagado",
                        color = if (isRunning) NeonGreen else Color.Red,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Button(
                        onClick = { if (isRunning) onStop() else onStart() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color.Red else NeonGreen,
                            contentColor = if (isRunning) PureWhite else DarkBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isRunning) {
                                if (isServerActive) "DETENER SERVIDOR" else "DETENER MONITOR"
                            } else {
                                if (isServerActive) "INICIAR SERVIDOR" else "INICIAR MONITOR"
                            }
                        )
                    }
                }
            }
            
            // Stats Card
            Card(
                modifier = Modifier.weight(1f).border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("NOTICIAS DISTRIBUIDAS", color = SoftGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$totalSent",
                        color = NeonYellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        if (isServerActive) "Enviadas desde el servidor 24/7" else "Enviadas en tiempo real a Discord",
                        color = SoftGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Discord Configuration Panel
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("INTEGRACIÓN CON DISCORD", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = onWebhookChange,
                    label = { Text("URL del Webhook de Discord") },
                    placeholder = { Text("https://discord.com/api/webhooks/...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = SoftGray
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTestingWebhook = true
                                testResult = null
                                testResult = DiscordClient.sendWebhook(
                                    webhookUrl = webhookUrl,
                                    title = "📢 Conexión Exitosa con 116Sports",
                                    link = "https://116sports.com",
                                    description = "El sistema de redacción y monitoreo en tiempo real se ha sincronizado correctamente con tu canal de Discord. ¡Listo para el Mundial 2026! ⚽",
                                    sourceName = "Consola de Administración 116Sports",
                                    color = 0x00FF66,
                                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Soccerball.svg/500px-Soccerball.svg.png"
                                )
                                isTestingWebhook = false
                            }
                        },
                        enabled = !isTestingWebhook && webhookUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonYellow,
                            contentColor = DarkBg
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (isTestingWebhook) "PROBANDO..." else "PROBAR CANAL")
                    }
                    
                    testResult?.let { success ->
                        Text(
                            text = if (success) "✓ ¡Mensaje de prueba enviado!" else "✗ Error al conectar",
                            color = if (success) NeonGreen else Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Terminal Logs Panel (Live Output)
        Text("CONSOLA DE MONITOREO (TIEMPO REAL)", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070709))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            val listState = rememberLazyListState()
            
            // Auto scroll to bottom
            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) {
                    listState.animateScrollToItem(logs.size - 1)
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            "Consola en espera. Configura el webhook de Discord e inicia el monitor para ver logs...",
                            color = SoftGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    items(logs) { log ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "[${log.time}]",
                                color = SoftGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                log.message,
                                color = when {
                                    log.isNews -> NeonGreen
                                    !log.isSuccess -> Color.Red
                                    else -> PureWhite
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorScreen(webhookUrl: String) {
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf("Redacción Propia") }
    var selectedColor by remember { mutableStateOf(0x00FF66) } // Default Lime
    
    var isSending by remember { mutableStateOf(false) }
    var sendResult by remember { mutableStateOf<Boolean?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "EDITOR DE GUIONES - DISTRIBUCIÓN DIRECTA",
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Header Meta: Media Origin Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedMedia,
                            onValueChange = { selectedMedia = it },
                            label = { Text("Firma / Medio de Noticia") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        
                        // Simple color picker mock
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(selectedColor or 0xFF000000.toInt()))
                                .clickable {
                                    // Rotate colors for test
                                    val colors = listOf(0x00FF66, 0xFED100, 0xCC0000, 0x0093D1, 0x222222)
                                    val currentIdx = colors.indexOf(selectedColor)
                                    selectedColor = colors[(currentIdx + 1) % colors.size]
                                }
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Color",
                                color = if (selectedColor == 0xFED100 || selectedColor == 0x00FF66) DarkBg else PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título de la Noticia") },
                        placeholder = { Text("¡ÚLTIMA HORA! Gol espectacular que define el partido...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Contenido / Guión del Embed") },
                        placeholder = { Text("Escribe los detalles deportivos de la noticia aquí...") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        maxLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Enlace Original / Enlace del Título") },
                        placeholder = { Text("https://example.com/noticia") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Enlace de Imagen de Portada (Opcional)") },
                        placeholder = { Text("https://example.com/imagen.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSending = true
                        sendResult = null
                        sendResult = DiscordClient.sendWebhook(
                            webhookUrl = webhookUrl,
                            title = title.ifBlank { "Noticia Deportiva Directa" },
                            link = link.ifBlank { "https://116sports.com" },
                            description = description.ifBlank { "Sin descripción detallada." },
                            sourceName = "$selectedMedia • Redacción Manual",
                            color = selectedColor,
                            imageUrl = imageUrl.ifBlank { null }
                        )
                        isSending = false
                        if (sendResult == true) {
                            // Clear form
                            title = ""
                            description = ""
                            link = ""
                            imageUrl = ""
                        }
                    }
                },
                enabled = !isSending && webhookUrl.isNotBlank() && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(if (isSending) "ENVIANDO..." else "DISTRIBUIR A DISCORD")
            }
            
            Button(
                onClick = {
                    title = ""
                    description = ""
                    link = ""
                    imageUrl = ""
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = SoftGray
                ),
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            ) {
                Text("LIMPIAR FORMULARIO")
            }
            
            sendResult?.let { success ->
                Text(
                    text = if (success) "✓ ¡Enviado con éxito!" else "✗ Error al enviar",
                    color = if (success) NeonGreen else Color.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ChannelsScreen(channels: List<RssChannel>, isServerActive: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "CANALES RSS DE FÚTBOL Y MULTIDEPORTE ACTIVOS",
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        
        Text(
            "Habilita o deshabilita los medios deportivos que desees monitorear de forma automatizada. El sistema rotará entre ellos de forma balanceada.",
            color = SoftGray,
            fontSize = 13.sp
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channels) { channel ->
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(channel.color or 0xFF000000.toInt()))
                                )
                                Text(
                                    channel.name,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                "Deporte: ${channel.sport} • URL: ${channel.url}",
                                color = SoftGray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Switch to toggle channels
                        Switch(
                            checked = channel.enabled,
                            onCheckedChange = { enabled ->
                                channel.enabled = enabled
                                SocialFeedMonitor.addLog("Canal [${channel.name}] " + if (enabled) "habilitado" else "deshabilitado")
                                if (isServerActive) {
                                    coroutineScope.launch {
                                        try {
                                            val client = io.ktor.client.HttpClient()
                                            client.post("http://localhost:8080/api/channels/toggle") {
                                                setBody("{\"id\":\"${channel.id}\",\"enabled\":$enabled}")
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = NeonGreen.copy(alpha = 0.5f),
                                uncheckedThumbColor = SoftGray,
                                uncheckedTrackColor = BorderColor
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun extractWebhookUrlFromJson(json: String): String {
    val key = "\"webhookUrl\""
    val idx = json.indexOf(key)
    if (idx == -1) return ""
    val start = json.indexOf("\"", idx + key.length)
    if (start == -1) return ""
    val end = json.indexOf("\"", start + 1)
    if (end == -1) return ""
    return json.substring(start + 1, end).replace("\\/", "/")
}

private fun parseLogsFromJson(json: String): List<LogMessage> {
    val list = mutableListOf<LogMessage>()
    var startIdx = 0
    while (true) {
        val objStart = json.indexOf("{", startIdx)
        if (objStart == -1) break
        val objEnd = json.indexOf("}", objStart)
        if (objEnd == -1) break
        
        val objStr = json.substring(objStart, objEnd + 1)
        
        val timeKey = "\"time\":\""
        val timeStart = objStr.indexOf(timeKey)
        val time = if (timeStart != -1) {
            val s = timeStart + timeKey.length
            val e = objStr.indexOf("\"", s)
            objStr.substring(s, e)
        } else "00:00:00"
        
        val msgKey = "\"message\":\""
        val msgStart = objStr.indexOf(msgKey)
        val message = if (msgStart != -1) {
            val s = msgStart + msgKey.length
            val e = objStr.indexOf("\"", s)
            objStr.substring(s, e).replace("\\\"", "\"").replace("\\n", "\n")
        } else ""
        
        val successKey = "\"isSuccess\":"
        val successStart = objStr.indexOf(successKey)
        val isSuccess = if (successStart != -1) {
            objStr.substring(successStart + successKey.length).trim().startsWith("true")
        } else true
        
        val newsKey = "\"isNews\":"
        val newsStart = objStr.indexOf(newsKey)
        val isNews = if (newsStart != -1) {
            objStr.substring(newsStart + newsKey.length).trim().startsWith("true")
        } else false
        
        list.add(LogMessage(time, message, isSuccess, isNews))
        startIdx = objEnd + 1
    }
    return list
}