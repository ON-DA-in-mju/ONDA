package com.onda.mju.student.ui.screen.notice

import androidx.annotation.DrawableRes
import com.onda.mju.student.R

/**
 * DB 정류장명 ↔ 로컬 정류장 사진 drawable.
 * 원본 이미지 폴더: frontend/image
 */
object StopPhotoResources {
    @DrawableRes
    fun forStopName(stopName: String?): Int {
        val key = normalize(stopName)
        return byNormalizedName[key] ?: R.drawable.stop_photo_bus_office
    }

    private fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim()
            .replace(" ", "")
            .replace("·", "")
            .lowercase()
    }

    private val byNormalizedName: Map<String, Int> = mapOf(
        normalize("채플관 앞") to R.drawable.stop_photo_chapel,
        normalize("기흥역 5번 출구") to R.drawable.stop_photo_giheung_exit5,
        normalize("기흥역5번출구") to R.drawable.stop_photo_giheung_exit5,
        normalize("버스관리사무소") to R.drawable.stop_photo_bus_office,
        normalize("상공회의소") to R.drawable.stop_photo_chamber,
        normalize("진입로(럭스나인 앞)") to R.drawable.stop_photo_luxnine,
        normalize("동부경찰서 중앙지구대") to R.drawable.stop_photo_police,
        normalize("용인CGV") to R.drawable.stop_photo_cgv,
        normalize("용인 CGV") to R.drawable.stop_photo_cgv,
        normalize("중앙공영주차장") to R.drawable.stop_photo_parking,
        normalize("진입로(역북동 주민센터)") to R.drawable.stop_photo_yeokbuk,
        normalize("이마트") to R.drawable.stop_photo_emart,
        normalize("제1공학관") to R.drawable.stop_photo_je1_engineering,
        normalize("제3공학관") to R.drawable.stop_photo_je3_engineering,
        normalize("함박관") to R.drawable.stop_photo_hambak,
        normalize("창조관") to R.drawable.stop_photo_changjo,
        normalize("경전철 명지대역") to R.drawable.stop_photo_myeongji_metro,
        normalize("명지대역 사거리 정류장") to R.drawable.stop_photo_myeongji_intersection,
        normalize("명진당") to R.drawable.stop_photo_myeongjindang,
        normalize("정문") to R.drawable.stop_photo_main_gate,
        normalize("생활관(명현관)") to R.drawable.stop_photo_dorm,
    )
}
