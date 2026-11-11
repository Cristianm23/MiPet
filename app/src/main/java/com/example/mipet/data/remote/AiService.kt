package com.example.mipet.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class AiRequest(val prompt: String)

data class IceServer(
    @SerializedName("urls") val urls: Any? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("credential") val credential: String? = null
)

data class VoiceCallConfig(
    @SerializedName(value = "call_id", alternate = ["callId"])
    val callId: String? = null,
    @SerializedName(value = "access_token", alternate = ["accessToken"])
    val accessToken: String? = null,
    @SerializedName("transport")
    val transport: String? = null,
    @SerializedName(value = "ice_servers", alternate = ["iceServers"])
    val iceServers: List<IceServer> = emptyList(),
    @SerializedName(value = "expires_at", alternate = ["expiresAt"])
    val expiresAt: String? = null
)



data class AiResponse(
    @SerializedName("text")
    val text: String = "",

    @SerializedName("voiceAgent")
    val voiceAgent: Boolean = false,

    @SerializedName(value = "call_id", alternate = ["callId"])
    val callId: String? = null,

    @SerializedName(value = "access_token", alternate = ["accessToken"])
    val accessToken: String? = null,

    @SerializedName("transport")
    val transport: String? = null,

    @SerializedName(value = "ice_servers", alternate = ["iceServers"])
    val iceServers: List<IceServer> = emptyList(),

    @SerializedName(value = "expires_at", alternate = ["expiresAt"])
    val expiresAt: String? = null,

    @SerializedName("body")
    val responseBody: VoiceCallConfig? = null,

    @SerializedName("data")
    val gatewayData: VoiceCallConfig? = null,

    @SerializedName("call")
    val gatewayCall: VoiceCallConfig? = null
) {

    val voiceCallConfig: VoiceCallConfig?
        get() {
            // Construir el objeto directamente desde los campos recibidos.
            val inlineConfig = VoiceCallConfig(
                callId = callId,
                accessToken = accessToken,
                transport = transport,
                iceServers = iceServers,
                expiresAt = expiresAt
            )

            val candidates = listOf(
                inlineConfig,
                responseBody,
                gatewayData,
                gatewayCall
            )

            return candidates.firstOrNull { config ->
                config != null &&
                        !config.callId.isNullOrBlank() &&
                        !config.accessToken.isNullOrBlank() &&
                        !config.transport.isNullOrBlank()
            }
        }

    val voiceCallDiagnostic: String?
        get() {
            if (voiceCallConfig != null) return null

            val missingFields = buildList {
                if (callId.isNullOrBlank()) add("call_id")
                if (accessToken.isNullOrBlank()) add("access_token")
                if (transport.isNullOrBlank()) add("transport")
            }

            return "La respuesta de llamadas no incluyó: " +
                    missingFields.joinToString().ifBlank {
                        "una configuración Gateway válida"
                    }
        }
}



interface AiService {
    @POST
    suspend fun getRecommendation(
        @Url webhookUrl: String,
        @Body request: AiRequest
    ): AiResponse
}
