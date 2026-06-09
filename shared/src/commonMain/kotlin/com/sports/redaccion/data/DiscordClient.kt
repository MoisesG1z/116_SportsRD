package com.sports.redaccion.data

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

object DiscordClient {
    private val client = HttpClient()

    suspend fun sendWebhook(
        webhookUrl: String,
        title: String,
        link: String,
        description: String,
        sourceName: String,
        color: Int,
        imageUrl: String?
    ): Boolean {
        if (webhookUrl.isBlank()) return false
        
        val escapedTitle = jsonEscape(title)
        val escapedDesc = jsonEscape(description)
        val escapedLink = jsonEscape(link)
        val escapedSource = jsonEscape(sourceName)
        val imageJson = if (!imageUrl.isNullOrBlank()) {
            ",\n      \"image\": {\n        \"url\": ${jsonEscape(imageUrl)}\n      }"
        } else ""
        
        val payload = """
        {
          "username": "116Sports Redacción",
          "avatar_url": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Soccerball.svg/500px-Soccerball.svg.png",
          "embeds": [
            {
              "title": $escapedTitle,
              "description": $escapedDesc,
              "url": $escapedLink,
              "color": $color,
              "footer": {
                "text": $escapedSource
              }$imageJson
            }
          ]
        }
        """.trimIndent()
        
        return try {
            val response: HttpResponse = client.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            // Discord Webhooks return 204 No Content on success
            response.status.value in 200..299
        } catch (e: Exception) {
            println("Error sending to Discord Webhook: ${e.message}")
            false
        }
    }
    
    private fun jsonEscape(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
