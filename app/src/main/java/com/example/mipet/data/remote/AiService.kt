package com.example.mipet.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class AiRequest(val prompt: String)
data class AiResponse(val text: String)

interface AiService {
    @POST
    suspend fun getRecommendation(
        @Url webhookUrl: String,
        @Body request: AiRequest
    ): AiResponse
}
