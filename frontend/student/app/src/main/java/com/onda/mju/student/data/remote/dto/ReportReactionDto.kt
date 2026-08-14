package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** public.report_reactions row */
@Serializable
data class ReportReactionDto(
    val id: String? = null,
    @SerialName("report_id")
    val reportId: String,
    @SerialName("user_id")
    val userId: String,
    /** LIKE | DISLIKE */
    val reaction: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class ReportReactionUpsertDto(
    @SerialName("report_id")
    val reportId: String,
    @SerialName("user_id")
    val userId: String,
    val reaction: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class ReportReactionPatchDto(
    val reaction: String,
    @SerialName("updated_at")
    val updatedAt: String,
)
