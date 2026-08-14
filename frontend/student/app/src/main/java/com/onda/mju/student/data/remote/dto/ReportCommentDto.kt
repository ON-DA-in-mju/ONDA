package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** public.report_comments — 제보(REPORT)·소통 글(POST) 공통 댓글 (구분은 reports.board_type) */
@Serializable
data class ReportCommentDto(
    val id: String,
    @SerialName("report_id")
    val reportId: String,
    @SerialName("user_id")
    val userId: String,
    val content: String,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class ReportCommentInsertDto(
    @SerialName("report_id")
    val reportId: String,
    @SerialName("user_id")
    val userId: String,
    val content: String,
)

@Serializable
data class ReportCommentContentPatchDto(
    val content: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class ReportCommentSoftDeletePatchDto(
    @SerialName("is_deleted")
    val isDeleted: Boolean,
    @SerialName("updated_at")
    val updatedAt: String,
)
