package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** public.notifications — 사용자별 알림 */
@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val message: String,
    val type: String = "NOTICE",
    @SerialName("is_read")
    val isRead: Boolean? = false,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class NotificationReadUpdateDto(
    @SerialName("is_read")
    val isRead: Boolean = true,
)
