package com.example.mipet.data.repository

import com.example.mipet.data.model.Resource
import com.example.mipet.data.remote.AiRequest
import com.example.mipet.data.remote.AiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AiRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://hook.us2.make.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(AiService::class.java)

    private val webhookEndpoint = "bquzs3agksy49lyu2hhat6j3lafh3o52"

    suspend fun getRecommendation(prompt: String): Resource<String> {
        return try {
            val response = service.getRecommendation(
                webhookUrl = webhookEndpoint,
                request = AiRequest(prompt)
            )
            Resource.Success(response.text)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al conectar con la IA de MiPet")
        }
    }
}
