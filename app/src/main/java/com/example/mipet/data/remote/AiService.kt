package com.example.mipet.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class AiRequest(val prompt: String)

data class AiResponse(
    @SerializedName("text") val text: String = "",
    @SerializedName("voiceAgent") val voiceAgent: Boolean = false,
    @SerializedName("accessToken") val accessToken: String? = null
)

interface AiService {
    @POST
    suspend fun getRecommendation(
        @Url webhookUrl: String,
        @Body request: AiRequest
    ): AiResponse
}
