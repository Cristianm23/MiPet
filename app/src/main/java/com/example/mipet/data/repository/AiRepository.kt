package com.example.mipet.data.repository

import com.example.mipet.data.model.Resource
import com.example.mipet.data.remote.AiRequest
import com.example.mipet.data.remote.AiResponse
import com.example.mipet.data.remote.AiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AiRepository {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://hook.us2.make.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(AiService::class.java)

    private val webhookEndpoint = "bquzs3agksy49lyu2hhat6j3lafh3o52"

    suspend fun getRecommendation(prompt: String): Resource<AiResponse> {
        return try {
            val response = service.getRecommendation(
                webhookUrl = webhookEndpoint,
                request = AiRequest(prompt)
            )
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al conectar con la IA de MiPet")
        }
    }
}
