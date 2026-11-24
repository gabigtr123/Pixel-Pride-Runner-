package com.gabigtr123.pixelstride.nativealpha

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

data class PresetCity(
    val name: String,
    val label: String,
    val lat: Double,
    val lon: Double,
)

data class LiveWeatherResult(
    val condition: WeatherCondition,
    val temperatureC: Int,
    val isDay: Boolean,
    val locationLabel: String,
)

object WeatherService {
    private val ioExecutor = Executors.newSingleThreadExecutor()

    val PRESET_CITIES = listOf(
        PresetCity("Bucharest", "BUCHAREST", 44.4268, 26.1025),
        PresetCity("London", "LONDON", 51.5074, -0.1278),
        PresetCity("New York", "NEW YORK", 40.7128, -74.0060),
        PresetCity("Tokyo", "TOKYO", 35.6762, 139.6503),
        PresetCity("Oslo", "OSLO", 59.9139, 10.7522),
    )

    fun mapWmoCode(code: Int): WeatherCondition = when {
        code == 45 || code == 48 -> WeatherCondition.FOG
        code in 95..99 -> WeatherCondition.STORM
        code in 71..77 || code == 85 || code == 86 -> WeatherCondition.SNOW
        code in 51..67 || code in 80..82 -> WeatherCondition.RAIN
        else -> WeatherCondition.CLEAR
    }

    fun fetchAsync(
        lat: Double,
        lon: Double,
        label: String,
        onSuccess: (LiveWeatherResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        ioExecutor.execute {
            try {
                val urlStr = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=weather_code,temperature_2m,is_day",
                    lat,
                    lon,
                )
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val code = conn.responseCode
                if (code !in 200..299) {
                    conn.disconnect()
                    onError("Weather API error ($code)")
                    return@execute
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(body)
                val current = json.optJSONObject("current")
                val wmo = current?.optInt("weather_code", 0) ?: 0
                val temp = current?.optDouble("temperature_2m", 18.0)?.roundToInt() ?: 18
                val isDay = (current?.optInt("is_day", 1) ?: 1) != 0
                onSuccess(
                    LiveWeatherResult(
                        condition = mapWmoCode(wmo),
                        temperatureC = temp,
                        isDay = isDay,
                        locationLabel = label.uppercase(Locale.US),
                    ),
                )
            } catch (_: Throwable) {
                onError("Could not reach Open-Meteo. Check internet!")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        var best: Location? = null
        for (p in providers) {
            try {
                if (!lm.isProviderEnabled(p)) continue
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.accuracy < best.accuracy) {
                    best = loc
                }
            } catch (_: Throwable) {
                // Provider unavailable or not permitted
            }
        }
        return best
    }
}
