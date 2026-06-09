package com.sports.redaccion.data

import kotlinx.coroutines.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

data class LogMessage(
    val time: String,
    val message: String,
    val isSuccess: Boolean = true,
    val isNews: Boolean = false
)

object PlatformTime {
    var formatTime: () -> String = { "00:00:00" }
}

object SocialFeedMonitor {
    private val client = HttpClient()
    private var job: Job? = null
    
    // Configurable state with local persistence
    var webhookUrl = object : androidx.compose.runtime.MutableState<String> {
        private val state = mutableStateOf("")
        
        override var value: String
            get() = state.value
            set(v) {
                state.value = v
                SettingsStorage.saveString("webhook_url", v)
            }
            
        override fun component1(): String = state.component1()
        override fun component2(): (String) -> Unit = { value = it }
    }
    val channels = mutableStateListOf<RssChannel>()
    
    // Monitored items state
    private val forwardedUrls = mutableSetOf<String>()
    private val initializedChannels = mutableSetOf<String>()
    
    // UI state
    val logMessages = mutableStateListOf<LogMessage>()
    val isRunning = mutableStateOf(false)
    val totalSent = mutableStateOf(0)
    
    private var channelIndex = 0

    init {
        // Load saved webhook URL
        webhookUrl.value = SettingsStorage.getString("webhook_url", "")
        // Prepopulate default channels
        channels.addAll(RssSource.defaultChannels)
    }

    fun start(scope: CoroutineScope) {
        if (isRunning.value) return
        isRunning.value = true
        addLog("Motor en tiempo real iniciado (Loop: 1s)")
        
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    // Check if webhook is set
                    if (webhookUrl.value.isBlank()) {
                        addLog("Error: URL del Webhook de Discord no configurada", isSuccess = false)
                        delay(5000)
                        continue
                    }

                    // Filter enabled channels
                    val activeChannels = channels.filter { it.enabled }
                    if (activeChannels.isEmpty()) {
                        delay(2000)
                        continue
                    }

                    // Rotate channels: query one channel per second to avoid rate-limiting
                    val channel = activeChannels[channelIndex % activeChannels.size]
                    channelIndex++

                    checkChannel(channel)
                } catch (e: Exception) {
                    addLog("Error en el loop: ${e.message}", isSuccess = false)
                }
                
                // Sleep exactly 1 second
                delay(1000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        isRunning.value = false
        addLog("Motor en tiempo real detenido")
    }

    private suspend fun checkChannel(channel: RssChannel) {
        try {
            val response: HttpResponse = client.get(channel.url)
            if (response.status.value in 200..299) {
                val xml = response.bodyAsText()
                val items = RssParser.parse(xml)
                
                val isColdStart = !initializedChannels.contains(channel.id)
                if (isColdStart) {
                    initializedChannels.add(channel.id)
                    // Mark all current articles as forwarded so we don't spam old news on startup
                    items.forEach { forwardedUrls.add(it.link) }
                    addLog("Canal [${channel.name}] inicializado con ${items.size} artículos existentes")
                    return
                }

                // Find new articles
                val newItems = items.filter { !forwardedUrls.contains(it.link) }
                if (newItems.isNotEmpty()) {
                    addLog("Detectados ${newItems.size} artículos nuevos en ${channel.name}!")
                    
                    for (item in newItems) {
                        // Forward to Discord
                        val success = DiscordClient.sendWebhook(
                            webhookUrl = webhookUrl.value,
                            title = item.title,
                            link = item.link,
                            description = item.description,
                            sourceName = "${channel.name} • ${channel.sport}",
                            color = channel.color,
                            imageUrl = item.imageUrl
                        )
                        
                        if (success) {
                            forwardedUrls.add(item.link)
                            totalSent.value++
                            addLog("Enviado a Discord: \"${item.title}\" (${channel.name})", isNews = true)
                        } else {
                            addLog("Error al enviar noticia a Discord: ${item.title}", isSuccess = false)
                        }
                    }
                }
            } else {
                addLog("Error HTTP ${response.status.value} en ${channel.name}", isSuccess = false)
            }
        } catch (e: Exception) {
            addLog("Error al consultar ${channel.name}: ${e.message}", isSuccess = false)
        }
    }

    fun addLog(message: String, isSuccess: Boolean = true, isNews: Boolean = false) {
        val timeString = PlatformTime.formatTime()
        // Run on Main/UI Thread safely
        CoroutineScope(Dispatchers.Main).launch {
            if (logMessages.size > 100) {
                logMessages.removeAt(0)
            }
            logMessages.add(LogMessage(timeString, message, isSuccess, isNews))
        }
    }
}
