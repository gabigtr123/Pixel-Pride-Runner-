package com.gabigtr123.pixelstride.nativealpha

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
    private val ioExecutor = Executors.newSingleThreadScheduledExecutor()
    private val requestSeq = AtomicInteger(0)

    @Volatile
    private var activeGpsCancel: CancellationSignal? = null

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
        code in 71..77 || code in 85..86 -> WeatherCondition.SNOW
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
        activeGpsCancel?.let { runCatching { it.cancel() } }
        activeGpsCancel = null
        val mySeq = requestSeq.incrementAndGet()
        ioExecutor.execute {
            if (requestSeq.get() != mySeq) return@execute
            var conn: HttpURLConnection? = null
            try {
                val urlStr = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=weather_code,temperature_2m,is_day",
                    lat,
                    lon,
                )
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val code = conn.responseCode
                if (code !in 200..299) {
                    if (requestSeq.get() == mySeq) onError("Weather API error ($code)")
                    return@execute
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                if (requestSeq.get() != mySeq) return@execute

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
                if (requestSeq.get() == mySeq) {
                    onError("Could not reach Open-Meteo. Check internet!")
                }
            } finally {
                runCatching { conn?.disconnect() }
            }
        }
    }

    /**
     * Requests a fresh live GPS/Fused fix on Android 11+ (API 30+) with a 10s indoor timeout,
     * falling back cleanly to cached lastKnownLocation or a clear prompt.
     */
    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(
        context: Context,
        onLocation: (Location) -> Unit,
        onError: (String) -> Unit,
    ) {
        activeGpsCancel?.let { runCatching { it.cancel() } }
        activeGpsCancel = null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            onError("Location service unavailable. Pick a city below!")
            return
        }

        // Fast path: if we already have a reasonably fresh cached fix (< 10 min old), use it immediately
        val cached = getLastKnownLocation(lm)
        if (cached != null && (System.currentTimeMillis() - cached.time) < 10 * 60 * 1000L) {
            onLocation(cached)
            return
        }

        // Active single-shot fix on Android 11+ (API 30+) with 10s indoor timeout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) ->
                    LocationManager.FUSED_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider != null) {
                try {
                    val mySeq = requestSeq.incrementAndGet()
                    val resolved = AtomicBoolean(false)
                    val cancel = CancellationSignal()
                    activeGpsCancel = cancel

                    ioExecutor.schedule({
                        if (resolved.compareAndSet(false, true)) {
                            runCatching { cancel.cancel() }
                            if (requestSeq.get() == mySeq) {
                                if (cached != null) {
                                    onLocation(cached)
                                } else {
                                    onError("GPS timed out indoors. Pick a city below!")
                                }
                            }
                        }
                    }, 10L, TimeUnit.SECONDS)

                    lm.getCurrentLocation(provider, cancel, ioExecutor) { loc ->
                        if (!resolved.compareAndSet(false, true)) return@getCurrentLocation
                        if (requestSeq.get() != mySeq) return@getCurrentLocation
                        if (loc != null) {
                            onLocation(loc)
                        } else if (cached != null) {
                            onLocation(cached)
                        } else {
                            onError("Could not get GPS fix. Enable Location or pick a city!")
                        }
                    }
                    return
                } catch (_: Throwable) {
                    // Fall through to cached fallback
                }
            }
        }

        if (cached != null) {
            onLocation(cached)
        } else {
            onError("Turn on Device Location or pick a city below!")
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(lm: LocationManager): Location? {
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        var best: Location? = null
        for (p in providers) {
            try {
                if (!lm.isProviderEnabled(p)) continue
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.accuracy < best.accuracy) {
                    best = loc
                }
            } catch (_: Throwable) {
            }
        }
        return best
    }
}
