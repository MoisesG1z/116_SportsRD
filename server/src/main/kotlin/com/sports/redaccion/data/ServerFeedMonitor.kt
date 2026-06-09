package com.sports.redaccion.data

import kotlinx.coroutines.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ServerLogMessage(
    val time: String,
    val message: String,
    val isSuccess: Boolean = true,
    val isNews: Boolean = false
)

object ServerFeedMonitor {
    private val client = HttpClient()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    var webhookUrl = ""
        private set
        
    val channels = mutableListOf<RssChannel>()
    private val forwardedUrls = mutableSetOf<String>()
    private val initializedChannels = mutableSetOf<String>()
    
    val logMessages = mutableListOf<ServerLogMessage>()
    var isRunning = false
        private set
        
    var totalSent = 0
        private set
        
    private var channelIndex = 0
    private val configFile = File("webhook.txt")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    init {
        // Prepopulate default channels
        channels.addAll(RssSource.defaultChannels)
        // Load saved webhook URL
        loadWebhook()
    }
    
    fun loadWebhook(): String {
        return try {
            if (configFile.exists()) {
                val url = configFile.readText().trim()
                webhookUrl = url
                addLog("Webhook cargado desde archivo: $url")
                url
            } else {
                val envUrl = System.getenv("DISCORD_WEBHOOK_URL") ?: ""
                webhookUrl = envUrl
                if (envUrl.isNotBlank()) {
                    addLog("Webhook cargado desde variable de entorno: $envUrl")
                }
                envUrl
            }
        } catch (e: Exception) {
            addLog("Error al cargar webhook: ${e.message}", isSuccess = false)
            ""
        }
    }
    
    fun saveWebhook(url: String) {
        webhookUrl = url.trim()
        try {
            configFile.writeText(webhookUrl)
            addLog("Webhook actualizado y guardado en webhook.txt")
        } catch (e: Exception) {
            addLog("Error al guardar webhook: ${e.message}", isSuccess = false)
        }
    }
    
    fun start() {
        if (isRunning) return
        isRunning = true
        addLog("Motor del servidor iniciado (Loop: 1s)")
        
        job = scope.launch {
            while (isActive) {
                try {
                    if (webhookUrl.isBlank()) {
                        addLog("Error: URL del Webhook de Discord no configurada", isSuccess = false)
                        delay(5000)
                        continue
                    }
                    
                    val activeChannels = channels.filter { it.enabled }
                    if (activeChannels.isEmpty()) {
                        delay(2000)
                        continue
                    }
                    
                    val channel = activeChannels[channelIndex % activeChannels.size]
                    channelIndex++
                    
                    checkChannel(channel)
                } catch (e: Exception) {
                    addLog("Error en el loop: ${e.message}", isSuccess = false)
                }
                delay(1000)
            }
        }
    }
    
    fun stop() {
        job?.cancel()
        isRunning = false
        addLog("Motor del servidor detenido")
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
                    items.forEach { forwardedUrls.add(it.link) }
                    addLog("Canal [${channel.name}] inicializado con ${items.size} artículos existentes")
                    return
                }
                
                val newItems = items.filter { !forwardedUrls.contains(it.link) }
                if (newItems.isNotEmpty()) {
                    addLog("Detectados ${newItems.size} artículos nuevos en ${channel.name}!")
                    
                    for (item in newItems) {
                        val success = DiscordClient.sendWebhook(
                            webhookUrl = webhookUrl,
                            title = item.title,
                            link = item.link,
                            description = item.description,
                            sourceName = "${channel.name} • ${channel.sport}",
                            color = channel.color,
                            imageUrl = item.imageUrl
                        )
                        
                        if (success) {
                            forwardedUrls.add(item.link)
                            totalSent++
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
        val timeString = LocalTime.now().format(timeFormatter)
        synchronized(logMessages) {
            if (logMessages.size > 100) {
                logMessages.removeAt(0)
            }
            logMessages.add(ServerLogMessage(timeString, message, isSuccess, isNews))
        }
        println("[$timeString] $message")
    }
}
