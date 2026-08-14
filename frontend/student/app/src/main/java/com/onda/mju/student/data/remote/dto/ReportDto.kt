package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** public.reports row — admin 웹과 동일 스키마 */
@Serializable
data class ReportDto(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val content: String,
    val status: String = "PENDING",
    val source: String = "STUDENT",
    /** REPORT = 상황 제보, POST = 소통 글쓰기 */
    @SerialName("board_type")
    val boardType: String = "REPORT",
    val category: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("view_count")
    val viewCount: Int? = 0,
)

@Serializable
data class ReportInsertDto(
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val content: String,
    val status: String = "PENDING",
    val source: String = "STUDENT",
    @SerialName("board_type")
    val boardType: String = "REPORT",
    val category: String? = null,
)

@Serializable
data class ReportUpdateDto(
    val title: String,
    val content: String,
    val category: String? = null,
    @SerialName("updated_at")
    val updatedAt: String,
)
