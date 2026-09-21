package com.example.mipet.data.repository

import com.example.mipet.data.model.Resource
import com.example.mipet.data.remote.AiRequest
import com.example.mipet.data.remote.AiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AiRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/ai/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(AiService::class.java)

    suspend fun getRecommendation(prompt: String): Resource<String> {
        return try {
            val response = service.getRecommendation(AiRequest(prompt))
            Resource.Success(response.text)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error de IA")
        }
    }
}
