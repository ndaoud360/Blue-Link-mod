package com.bluedeck.data.chargers

import com.bluedeck.BuildConfig
import com.bluedeck.data.repository.Result
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ChargingStation(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMiles: Double?,
    val network: String?,
    val dcFastPorts: Int,
    val level2Ports: Int,
    val connectors: List<String>,
    val hours: String?
) {
    val isDcFast: Boolean get() = dcFastPorts > 0

    /** Human-readable connector names; the AFDC codes (J1772COMBO, TESLA…) are not user-facing. */
    val connectorLabels: List<String>
        get() = connectors.map {
            when (it.uppercase()) {
                "J1772COMBO" -> "CCS"
                "J1772" -> "J1772"
                "TESLA" -> "NACS"
                "CHADEMO" -> "CHAdeMO"
                "NEMA1450", "NEMA515", "NEMA520" -> "Outlet"
                else -> it
            }
        }.distinct()
}

enum class ChargerFilter { ALL, DC_FAST, LEVEL_2 }

/**
 * Nearby public EV chargers from the U.S. DOE Alternative Fuels Data Center (NREL developer API).
 * This is independent of Bluelink; it only needs the vehicle coordinates and an API key
 * supplied at build time via local.properties (NREL_API_KEY).
 */
@Singleton
class ChargerRepository @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private data class CacheKey(val lat: Double, val lon: Double, val filter: ChargerFilter)
    private var cache: Pair<CacheKey, Pair<Long, List<ChargingStation>>>? = null

    val isConfigured: Boolean get() = BuildConfig.NREL_API_KEY.isNotBlank()

    suspend fun nearby(
        latitude: Double,
        longitude: Double,
        filter: ChargerFilter = ChargerFilter.ALL,
        radiusMiles: Int = 25,
        limit: Int = 30
    ): Result<List<ChargingStation>> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.Error("Add an NREL API key to show nearby chargers.")
        }
        // Round to ~100 m so small GPS jitter reuses the cache; chargers change rarely.
        val key = CacheKey(
            Math.round(latitude * 1000.0) / 1000.0,
            Math.round(longitude * 1000.0) / 1000.0,
            filter
        )
        cache?.let { (k, v) ->
            if (k == key && System.currentTimeMillis() - v.first < CACHE_MS) return@withContext Result.Success(v.second)
        }
        try {
            val url = "https://developer.nrel.gov/api/alt-fuel-stations/v1/nearest.json".toHttpUrl().newBuilder()
                .addQueryParameter("api_key", BuildConfig.NREL_API_KEY)
                .addQueryParameter("latitude", latitude.toString())
                .addQueryParameter("longitude", longitude.toString())
                .addQueryParameter("radius", radiusMiles.toString())
                .addQueryParameter("fuel_type", "ELEC")
                .addQueryParameter("status", "E")
                .addQueryParameter("access", "public")
                .addQueryParameter("limit", (if (filter == ChargerFilter.ALL) limit else limit * 2).toString())
                .build()
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.Error(
                        when (response.code) {
                            401, 403 -> "The NREL API key was rejected."
                            429 -> "Charger lookups are rate-limited. Try again shortly."
                            else -> "Charger data is unavailable right now."
                        },
                        response.code
                    )
                }
                val root = JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
                val stations = root.getAsJsonArray("fuel_stations")
                    ?.mapNotNull { (it as? JsonObject)?.toStation() }
                    .orEmpty()
                    .filter {
                        when (filter) {
                            ChargerFilter.DC_FAST -> it.dcFastPorts > 0
                            ChargerFilter.LEVEL_2 -> it.level2Ports > 0
                            ChargerFilter.ALL -> true
                        }
                    }
                    .sortedBy { it.distanceMiles ?: Double.MAX_VALUE }
                cache = key to (System.currentTimeMillis() to stations)
                Result.Success(stations)
            }
        } catch (e: Exception) {
            Result.Error("Couldn't load nearby chargers. Check your connection.")
        }
    }

    private fun JsonObject.str(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrNull() }?.takeIf { it.isNotBlank() }

    private fun JsonObject.int(name: String): Int =
        get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() } ?: 0

    private fun JsonObject.dbl(name: String): Double? =
        get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asDouble }.getOrNull() }

    private fun JsonElement.stringList(): List<String> =
        if (isJsonArray) asJsonArray.mapNotNull { runCatching { it.asString }.getOrNull() } else emptyList()

    private fun JsonObject.toStation(): ChargingStation? {
        val lat = dbl("latitude") ?: return null
        val lon = dbl("longitude") ?: return null
        val address = listOfNotNull(str("street_address"), str("city"), str("state")).joinToString(", ")
        return ChargingStation(
            id = str("id") ?: "$lat,$lon",
            name = str("station_name") ?: "Charging station",
            address = address,
            latitude = lat,
            longitude = lon,
            distanceMiles = dbl("distance"),
            network = str("ev_network")?.takeUnless { it.equals("Non-Networked", true) },
            dcFastPorts = int("ev_dc_fast_num"),
            level2Ports = int("ev_level2_evse_num"),
            connectors = get("ev_connector_types")?.takeUnless { it.isJsonNull }?.stringList().orEmpty(),
            hours = str("access_days_time")
        )
    }

    private companion object {
        const val CACHE_MS = 30 * 60 * 1000L
    }
}
