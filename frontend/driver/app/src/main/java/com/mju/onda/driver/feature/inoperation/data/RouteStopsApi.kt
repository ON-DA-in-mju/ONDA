package com.mju.onda.driver.feature.inoperation.data

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URLEncoder

/**
 * Supabase `route_stops` + `stops` 에서 노선 정류장 순서를 조회한다.
 * 실패 시 [RouteStopsCatalog] fallback.
 */
object RouteStopsApi {
    private const val TAG = "RouteStopsApi"

    suspend fun fetchStopsForRouteName(routeName: String): List<RouteStop> = withContext(Dispatchers.IO) {
        val key = routeName.trim()
        if (key.isBlank()) return@withContext emptyList()
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            Log.w(TAG, "supabase not ready → catalog fallback for '$key'")
            return@withContext RouteStopsCatalog.stopsForRouteName(key)
        }

        try {
            val select = "stop_order,stop_id,stops(id,stop_name,latitude,longitude),routes!inner(route_name)"
            val encodedSelect = URLEncoder.encode(select, "UTF-8")
            val encodedName = URLEncoder.encode(key, "UTF-8")
            val path =
                "/rest/v1/route_stops?select=$encodedSelect" +
                    "&routes.route_name=eq.$encodedName" +
                    "&order=stop_order.asc"

            val result = SupabaseClient.request(method = "GET", path = path, authed = true)
            if (result.code !in 200..299) {
                Log.w(TAG, "GET failed HTTP ${result.code}: ${result.body.take(200)}")
                return@withContext RouteStopsCatalog.stopsForRouteName(key)
            }

            val parsed = parse(result.body)
            if (parsed.isEmpty()) {
                Log.w(TAG, "empty route_stops for '$key' → catalog fallback")
                return@withContext RouteStopsCatalog.stopsForRouteName(key)
            }
            Log.d(TAG, "loaded ${parsed.size} stops for '$key' from DB")
            parsed
        } catch (e: Exception) {
            Log.w(TAG, "fetch error for '$key': ${e.message}")
            RouteStopsCatalog.stopsForRouteName(key)
        }
    }

    private fun parse(json: String): List<RouteStop> {
        val arr = JSONArray(json)
        val out = ArrayList<RouteStop>(arr.length())
        for (i in 0 until arr.length()) {
            val row = arr.getJSONObject(i)
            val stop = row.optJSONObject("stops") ?: continue
            val order = row.optInt("stop_order", i + 1)
            val id = stop.optString("id").ifBlank {
                row.optString("stop_id").ifBlank { "stop-$order" }
            }
            val name = stop.optString("stop_name").orEmpty()
            if (name.isBlank()) continue
            val lat = stop.optDouble("latitude", Double.NaN)
            val lng = stop.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue
            out.add(
                RouteStop(
                    id = id,
                    name = name,
                    lat = lat,
                    lng = lng,
                    order = order,
                ),
            )
        }
        return out.sortedBy { it.order }
    }
}
