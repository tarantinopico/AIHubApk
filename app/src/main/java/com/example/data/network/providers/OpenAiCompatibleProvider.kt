package com.example.data.network.providers

import com.example.core.AppResult
import com.example.domain.model.MessageRole
import com.example.domain.network.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

class OpenAiCompatibleProvider(
    private val client: HttpClient,
    private val baseUrl: String,
    private val apiKey: String
) : ChatProvider {

    override fun streamChat(request: ChatRequest): Flow<ChatChunk> = flow {
        try {
            val messagesList = mutableListOf<JsonObject>()
            
            if (!request.systemPrompt.isNullOrBlank()) {
                messagesList.add(buildJsonObject {
                    put("role", "system")
                    put("content", request.systemPrompt!!)
                })
            }
            
            for (msg in request.messages) {
                messagesList.add(buildJsonObject {
                    put("role", msg.role.name.lowercase())
                    if (msg.attachmentsJson.isNullOrBlank()) {
                        put("content", msg.content)
                    } else {
                        val contentArray = buildJsonArray {
                            add(buildJsonObject { put("type", "text"); put("text", msg.content) })
                            try {
                                val attachments = Json.parseToJsonElement(msg.attachmentsJson!!).jsonArray
                                for (att in attachments) {
                                    val obj = att.jsonObject
                                    val mime = obj["mimeType"]?.jsonPrimitive?.content ?: "image/jpeg"
                                    val b64 = obj["data"]?.jsonPrimitive?.content ?: ""
                                    add(buildJsonObject {
                                        put("type", "image_url")
                                        put("image_url", buildJsonObject {
                                            put("url", "data:$mime;base64,$b64")
                                        })
                                    })
                                }
                            } catch (e: Exception) { }
                        }
                        put("content", contentArray)
                    }
                })
            }

            val requestBody = buildJsonObject {
                put("model", request.modelId)
                put("messages", JsonArray(messagesList))
                put("stream", true)
                put("temperature", request.temperature)
                if (request.maxTokens != null) {
                    put("max_tokens", request.maxTokens)
                }
            }

            client.preparePost("$baseUrl/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val errorText = response.bodyAsText()
                    throw Exception("HTTP ${response.status.value}: $errorText")
                }
                
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.isBlank()) continue
                    
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6).trim()
                        if (data == "[DONE]") {
                            emit(ChatChunk.Done)
                            break
                        }
                        
                        try {
                            val json = Json.parseToJsonElement(data).jsonObject
                            val choices = json["choices"]?.jsonArray
                            if (choices != null && choices.isNotEmpty()) {
                                val delta = choices[0].jsonObject["delta"]?.jsonObject
                                val content = delta?.get("content")?.jsonPrimitive?.content
                                if (content != null) {
                                    emit(ChatChunk.Text(content))
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun listModels(): AppResult<List<RemoteModelInfo>> {
        return try {
            val response = client.get("$baseUrl/models") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                return AppResult.Error("HTTP ${response.status.value}: $errorText")
            }
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val data = json["data"]?.jsonArray ?: JsonArray(emptyList())
            
            val models = data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }.map {
                RemoteModelInfo(id = it, name = it)
            }
            AppResult.Success(models)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Chyba při stahování modelů", e)
        }
    }
}
