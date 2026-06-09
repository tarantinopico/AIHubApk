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

class GeminiProvider(
    private val client: HttpClient,
    private val apiKey: String
) : ChatProvider {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    override fun streamChat(request: ChatRequest): Flow<ChatChunk> = flow {
        try {
            val contentsList = mutableListOf<JsonObject>()
            
            for (msg in request.messages) {
                val roleStr = if (msg.role == MessageRole.USER) "user" else "model"
                val partsArray = buildJsonArray {
                    add(buildJsonObject { put("text", msg.content) })
                    
                    if (!msg.attachmentsJson.isNullOrBlank()) {
                        try {
                            val attachments = Json.parseToJsonElement(msg.attachmentsJson!!).jsonArray
                            for (att in attachments) {
                                val obj = att.jsonObject
                                val mime = obj["mimeType"]?.jsonPrimitive?.content ?: "image/jpeg"
                                val b64 = obj["data"]?.jsonPrimitive?.content ?: ""
                                add(buildJsonObject {
                                    put("inlineData", buildJsonObject {
                                        put("mimeType", mime)
                                        put("data", b64)
                                    })
                                })
                            }
                        } catch (e: Exception) {}
                    }
                }
                
                contentsList.add(buildJsonObject {
                    put("role", roleStr)
                    put("parts", partsArray)
                })
            }

            val requestBody = buildJsonObject {
                if (!request.systemPrompt.isNullOrBlank()) {
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", request.systemPrompt!!) })
                        })
                    })
                }
                put("contents", JsonArray(contentsList))
                put("generationConfig", buildJsonObject {
                    put("temperature", request.temperature)
                    if (request.maxTokens != null) {
                        put("maxOutputTokens", request.maxTokens)
                    }
                })
            }

            client.preparePost("$baseUrl/models/${request.modelId}:streamGenerateContent?alt=sse") {
                header("x-goog-api-key", apiKey)
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
                        if (data.isEmpty()) continue
                        
                        try {
                            val json = Json.parseToJsonElement(data).jsonObject
                            val candidates = json["candidates"]?.jsonArray
                            if (candidates != null && candidates.isNotEmpty()) {
                                val parts = candidates[0].jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray
                                if (parts != null && parts.isNotEmpty()) {
                                    val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
                                    if (text != null) {
                                        emit(ChatChunk.Text(text))
                                    }
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }
                emit(ChatChunk.Done())
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun listModels(): AppResult<List<RemoteModelInfo>> {
        return try {
            val response = client.get("$baseUrl/models") {
                header("x-goog-api-key", apiKey)
            }
            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                return AppResult.Error("HTTP ${response.status.value}: $errorText")
            }
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val modelsArr = json["models"]?.jsonArray ?: JsonArray(emptyList())
            
            val models = modelsArr.mapNotNull { 
                val name = it.jsonObject["name"]?.jsonPrimitive?.content?.replace("models/", "")
                val displayName = it.jsonObject["displayName"]?.jsonPrimitive?.content
                if (name != null) RemoteModelInfo(id = name, name = displayName ?: name) else null
            }
            AppResult.Success(models)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Chyba", e)
        }
    }
}
