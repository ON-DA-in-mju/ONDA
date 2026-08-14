package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val name: String = "",
    val email: String? = null,
    @SerialName("student_no")
    val studentNo: String? = null,
)

@Serializable
data class UserNameUpdateDto(
    val name: String,
    @SerialName("updated_at")
    val updatedAt: String,
)
