package com.example.mipet.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.example.mipet.data.model.Vet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.*
import android.util.Log

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String
)

class VetRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    fun calculateDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = r * c
        return (distance * 100).roundToInt() / 100.0
    }


    suspend fun getNearbyVets(
        userLat: Double,
        userLng: Double,
        cityName: String = "",
        radiusKm: Double = 8.0
    ): List<Vet> = withContext(Dispatchers.IO) {

        Log.d("MIPET_VETS", "Entró a getNearbyVets")
        Log.d("MIPET_VETS", "Latitud: $userLat, Longitud: $userLng")
        Log.d("MIPET_VETS", "Radio: $radiusKm km")

        val radiusMeters = (radiusKm * 1000).toInt()

        val query = """
        [out:json][timeout:25];
        (
          node["amenity"="veterinary"](around:$radiusMeters,$userLat,$userLng);
          way["amenity"="veterinary"](around:$radiusMeters,$userLat,$userLng);
          relation["amenity"="veterinary"](around:$radiusMeters,$userLat,$userLng);
        );
        out center tags;
    """.trimIndent()

        val apiKey = com.example.mipet.BuildConfig.OVERPASS_API_KEY

        if (apiKey.isBlank()) {
            throw Exception("No se configuró la API Key de Overpass.")
        }

        val url = "https://api.overpass.ch/v1/$apiKey/interpreter"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MiPetAndroidApp/1.0")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(
                okhttp3.FormBody.Builder()
                    .add("data", query)
                    .build()
            )
            .build()

        Log.d("MIPET_VETS", "Enviando consulta a Overpass...")

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                throw Exception("Error consultando veterinarias: ${response.code}")
            }

            val json = response.body?.string()
                ?: throw Exception("Respuesta vacía del servidor")

            Log.d("MIPET_VETS", "Coordenadas: $userLat, $userLng")
            Log.d("MIPET_VETS", "Respuesta Overpass: $json")

            val elements = JSONObject(json).optJSONArray("elements")
                ?: return@withContext emptyList()

            Log.d("MIPET_VETS", "Elementos encontrados: ${elements.length()}")

            val vets = mutableListOf<Vet>()

            for (i in 0 until elements.length()) {
                val item = elements.getJSONObject(i)
                val tags = item.optJSONObject("tags") ?: continue

                // Solo establecimientos identificados como veterinarias.
                if (tags.optString("amenity") != "veterinary") continue

                val center = item.optJSONObject("center")

                val lat = if (item.has("lat")) {
                    item.optDouble("lat")
                } else {
                    center?.optDouble("lat") ?: continue
                }

                val lng = if (item.has("lon")) {
                    item.optDouble("lon")
                } else {
                    center?.optDouble("lon") ?: continue
                }

                if (!lat.isFinite() || !lng.isFinite()) continue
                if (lat !in -90.0..90.0 || lng !in -180.0..180.0) continue

                val distance = calculateDistanceKm(
                    userLat, userLng, lat, lng
                )

                if (distance > radiusKm) continue

                val name = tags.optString("name")
                    .takeIf { it.isNotBlank() }
                    ?: "Veterinaria sin nombre registrado"

                val street = tags.optString("addr:street")
                val number = tags.optString("addr:housenumber")
                val address = listOf(street, number)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Dirección no registrada" }

                val phone = tags.optString("phone")
                    .takeIf { it.isNotBlank() }
                    ?: tags.optString("contact:phone")
                        .takeIf { it.isNotBlank() }
                    ?: "Teléfono no registrado"

                vets.add(
                    Vet(
                        id = "osm_${item.optString("type")}_${item.optString("id")}",
                        name = name,
                        address = address,
                        phone = phone,
                        latitude = lat,
                        longitude = lng,
                        distanceKm = distance,
                        isOpen = false,
                        rating = 0.0,
                        openHours = "Horario no registrado"
                    )
                )
            }

            vets.distinctBy { it.id }.sortedBy { it.distanceKm }
        }
    }


    suspend fun geocodeAddress(query: String): GeocodeResult? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val name = listOfNotNull(addr.locality, addr.adminArea, addr.countryName).filter { it.isNotBlank() }.joinToString(", ")
                GeocodeResult(
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    displayName = if (name.isNotBlank()) name else query
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
