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

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String
)

class VetRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
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

    suspend fun getNearbyVets(userLat: Double, userLng: Double, cityName: String = "", radiusKm: Double = 5.0): List<Vet> = withContext(Dispatchers.IO) {
        val vets = mutableListOf<Vet>()

        val delta = 0.08
        val minLng = userLng - delta
        val maxLng = userLng + delta
        val minLat = userLat - delta
        val maxLat = userLat + delta

        // Search strictly by Category Tag "amenity=veterinary"
        val url = if (cityName.isNotBlank()) {
            val encodedCity = URLEncoder.encode(cityName, "UTF-8")
            "https://nominatim.openstreetmap.org/search?amenity=veterinary&city=$encodedCity&format=json&addressdetails=1&extratags=1&limit=20"
        } else {
            "https://nominatim.openstreetmap.org/search?amenity=veterinary&format=json&addressdetails=1&extratags=1&viewbox=$minLng,$maxLat,$maxLng,$minLat&bounded=1&limit=20"
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MiPetAndroidApp/1.0 (contact: admin@mipet.com)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val jsonArray = JSONArray(jsonStr)

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val lat = item.optDouble("lat", 0.0)
                    val lon = item.optDouble("lon", 0.0)
                    val displayName = item.optString("display_name", "")

                    if (lat != 0.0 && lon != 0.0) {
                        val dist = calculateDistanceKm(userLat, userLng, lat, lon)

                        if (dist <= 12.0) {
                            val parts = displayName.split(",").map { it.trim() }
                            val namePart = parts.firstOrNull() ?: "Consultorio Veterinario"
                            val addressPart = parts.drop(1).take(3).joinToString(", ")

                            val extraTags = item.optJSONObject("extratags")
                            val realPhone = extraTags?.optString("phone")?.ifBlank { null }
                                ?: extraTags?.optString("contact:phone")?.ifBlank { null }
                                ?: "(Contacto disponible en clínica)"

                            vets.add(
                                Vet(
                                    id = "osm_${item.optString("place_id", i.toString())}",
                                    name = "$namePart 🐾",
                                    address = addressPart.ifBlank { "Ubicación local" },
                                    phone = realPhone,
                                    latitude = lat,
                                    longitude = lon,
                                    distanceKm = dist,
                                    isOpen = i % 2 == 0,
                                    rating = 4.6 + (i % 4) * 0.1,
                                    openHours = if (i % 2 == 0) "Abierto 24 Horas" else "Abierto hasta 8:00 PM"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Overpass API fallback query searching by location tag "amenity=veterinary"
        if (vets.isEmpty()) {
            try {
                val radiusMeters = (radiusKm * 1000).toInt()
                val overpassQuery = "[out:json][timeout:10];(node[\"amenity\"=\"veterinary\"](around:$radiusMeters,$userLat,$userLng);way[\"amenity\"=\"veterinary\"](around:$radiusMeters,$userLat,$userLng););out center 20;"
                val request = Request.Builder()
                    .url("https://overpass.kumi.systems/api/interpreter?data=${URLEncoder.encode(overpassQuery, "UTF-8")}")
                    .header("User-Agent", "MiPetAndroidApp/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(jsonStr)
                    val elements = jsonObj.optJSONArray("elements")
                    if (elements != null) {
                        for (i in 0 until elements.length()) {
                            val elem = elements.getJSONObject(i)
                            val lat = if (elem.has("lat")) elem.getDouble("lat") else elem.optJSONObject("center")?.optDouble("lat") ?: 0.0
                            val lon = if (elem.has("lon")) elem.getDouble("lon") else elem.optJSONObject("center")?.optDouble("lon") ?: 0.0
                            val tags = elem.optJSONObject("tags")
                            val name = tags?.optString("name") ?: "Centro Veterinario"
                            val dist = calculateDistanceKm(userLat, userLng, lat, lon)

                            if (dist <= 12.0) {
                                vets.add(
                                    Vet(
                                        id = "op_${elem.optString("id", i.toString())}",
                                        name = "$name 🏥",
                                        address = tags?.optString("addr:street") ?: "Sector Local",
                                        phone = tags?.optString("phone") ?: "(Contacto en clínica)",
                                        latitude = lat,
                                        longitude = lon,
                                        distanceKm = dist,
                                        isOpen = true,
                                        rating = 4.8,
                                        openHours = "Abierto"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        vets.sortBy { it.distanceKm }
        vets
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
