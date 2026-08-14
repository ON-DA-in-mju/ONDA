package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StopDto(
    val id: String,
    @SerialName("stop_name")
    val stopName: String,
    val latitude: Double,
    val longitude: Double,
)
