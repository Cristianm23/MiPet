package com.example.mipet.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class AiRequest(val prompt: String)
data class AiResponse(val text: String)

interface AiService {
    @POST("recommend")
    suspend fun getRecommendation(@Body request: AiRequest): AiResponse
}
