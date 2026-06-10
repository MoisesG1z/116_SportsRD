package com.sports.redaccion

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import com.sports.redaccion.data.ServerFeedMonitor

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: SERVER_PORT
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Interceptor simple para CORS
    intercept(ApplicationCallPipeline.Plugins) {
        call.response.headers.append("Access-Control-Allow-Origin", "*")
        call.response.headers.append("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        call.response.headers.append("Access-Control-Allow-Headers", "Content-Type")
        if (call.request.httpMethod == HttpMethod.Options) {
            call.respond(HttpStatusCode.OK)
            finish()
        }
    }

    // Cargar webhook e iniciar monitoreo automático en segundo plano si está configurado
    ServerFeedMonitor.loadWebhook()
    if (ServerFeedMonitor.webhookUrl.isNotBlank()) {
        ServerFeedMonitor.start()
    }

    routing {
        get("/") {
            call.respondText("116Sports Redacción Backend is Running 24/7.")
        }
        
        get("/api/webhook") {
            call.respondText(
                "{\"webhookUrl\":\"${ServerFeedMonitor.webhookUrl}\"}",
                ContentType.Application.Json
            )
        }
        
        post("/api/webhook") {
            val body = call.receiveText()
            val url = extractWebhookUrlFromJson(body)
            ServerFeedMonitor.saveWebhook(url)
            call.respondText("{\"success\":true,\"webhookUrl\":\"${ServerFeedMonitor.webhookUrl}\"}", ContentType.Application.Json)
        }
        
        get("/api/status") {
            val isRunning = ServerFeedMonitor.isRunning
            val totalSent = ServerFeedMonitor.totalSent
            val webhookUrl = ServerFeedMonitor.webhookUrl
            call.respondText(
                "{\"isRunning\":$isRunning,\"totalSent\":$totalSent,\"webhookUrl\":\"$webhookUrl\"}",
                ContentType.Application.Json
            )
        }
        
        post("/api/start") {
            ServerFeedMonitor.start()
            call.respondText("{\"isRunning\":${ServerFeedMonitor.isRunning}}", ContentType.Application.Json)
        }
        
        post("/api/stop") {
            ServerFeedMonitor.stop()
            call.respondText("{\"isRunning\":${ServerFeedMonitor.isRunning}}", ContentType.Application.Json)
        }
        
        get("/api/logs") {
            call.respondText(logsToJson(), ContentType.Application.Json)
        }
        
        get("/api/channels") {
            val json = ServerFeedMonitor.channels.joinToString(prefix = "[", postfix = "]") { ch ->
                """{"id":"${ch.id}","name":"${ch.name}","enabled":${ch.enabled},"sport":"${ch.sport}","url":"${ch.url}","color":${ch.color}}"""
            }
            call.respondText(json, ContentType.Application.Json)
        }
        
        post("/api/channels/toggle") {
            val body = call.receiveText()
            val id = extractIdFromJson(body)
            val enabled = extractEnabledFromJson(body)
            val channel = ServerFeedMonitor.channels.find { it.id == id }
            if (channel != null) {
                channel.enabled = enabled
                ServerFeedMonitor.addLog("Canal [${channel.name}] " + if (enabled) "habilitado" else "deshabilitado")
                call.respondText("{\"success\":true}", ContentType.Application.Json)
            } else {
                call.respondText("{\"success\":false,\"error\":\"Channel not found\"}", ContentType.Application.Json, HttpStatusCode.NotFound)
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

private fun extractIdFromJson(json: String): String {
    val key = "\"id\""
    val idx = json.indexOf(key)
    if (idx == -1) return ""
    val start = json.indexOf("\"", idx + key.length)
    if (start == -1) return ""
    val end = json.indexOf("\"", start + 1)
    if (end == -1) return ""
    return json.substring(start + 1, end)
}

private fun extractEnabledFromJson(json: String): Boolean {
    val key = "\"enabled\""
    val idx = json.indexOf(key)
    if (idx == -1) return false
    val start = json.indexOf(":", idx + key.length)
    if (start == -1) return false
    val remainder = json.substring(start + 1).trim()
    return remainder.startsWith("true")
}

private fun logsToJson(): String {
    return synchronized(ServerFeedMonitor.logMessages) {
        ServerFeedMonitor.logMessages.joinToString(prefix = "[", postfix = "]") { log ->
            """{"time":"${log.time}","message":"${jsonEscape(log.message)}","isSuccess":${log.isSuccess},"isNews":${log.isNews}}"""
        }
    }
}

private fun jsonEscape(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}