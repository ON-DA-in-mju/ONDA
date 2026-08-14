package com.onda.mju.student.data.favorite

import android.content.Context

/** 즐겨찾기 로컬 캐시 (앱 재시작·오프라인 대비). DB와 동기화. */
class FavoritesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun routeIds(): Set<String> =
        prefs.getStringSet(KEY_ROUTES, emptySet())?.let { HashSet(it) }.orEmpty()

    fun stopIds(): Set<String> =
        prefs.getStringSet(KEY_STOPS, emptySet())?.let { HashSet(it) }.orEmpty()

    fun setRouteIds(ids: Collection<String>) {
        prefs.edit().putStringSet(KEY_ROUTES, HashSet(ids.map { it.trim() }.filter { it.isNotEmpty() })).apply()
    }

    fun setStopIds(ids: Collection<String>) {
        prefs.edit().putStringSet(KEY_STOPS, HashSet(ids.map { it.trim() }.filter { it.isNotEmpty() })).apply()
    }

    fun setAll(routeIds: Collection<String>, stopIds: Collection<String>) {
        prefs.edit()
            .putStringSet(KEY_ROUTES, HashSet(routeIds.map { it.trim() }.filter { it.isNotEmpty() }))
            .putStringSet(KEY_STOPS, HashSet(stopIds.map { it.trim() }.filter { it.isNotEmpty() }))
            .apply()
    }

    fun toggleRoute(id: String): Set<String> {
        val next = HashSet(routeIds())
        if (!next.add(id)) next.remove(id)
        setRouteIds(next)
        return next
    }

    fun toggleStop(id: String): Set<String> {
        val next = HashSet(stopIds())
        if (!next.add(id)) next.remove(id)
        setStopIds(next)
        return next
    }

    companion object {
        private const val PREFS = "onda_student_favorites"
        private const val KEY_ROUTES = "favorite_route_ids"
        private const val KEY_STOPS = "favorite_stop_ids"
    }
}
