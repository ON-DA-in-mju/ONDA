package com.onda.mju.student.ui.screen.community

data class CommunityPost(
    val id: String,
    val userId: String = "",
    val title: String,
    val body: String,
    val timeLabel: String = "방금 전",
    val registeredAt: String = "-",
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val myReaction: ReportReaction? = null,
    val commentCount: Int = 0,
    /** 고유 조회수 (몇 명이 봤는지) */
    val viewCount: Int = 0,
)

data class CommunityComment(
    val id: String,
    val postId: String,
    val userId: String = "",
    val body: String,
    val timeLabel: String = "방금 전",
    val isMine: Boolean = false,
    val isDeleted: Boolean = false,
    /** 글 작성자 = "익명", 그 외 = "익명 1", "익명 2"... (같은 userId면 동일) */
    val authorLabel: String = "익명",
)

/**
 * 댓글 작성자 표시명 부여.
 * - 게시글 작성자: "익명"
 * - 그 외: 댓글에 처음 등장한 순서대로 "익명 1", "익명 2"... (동일 사용자는 번호 고정)
 */
fun List<CommunityComment>.withAnonymousAuthorLabels(postAuthorId: String): List<CommunityComment> {
    val numberByUserId = linkedMapOf<String, Int>()
    var nextNumber = 1
    return map { comment ->
        val label = when {
            postAuthorId.isNotBlank() && comment.userId == postAuthorId -> "익명"
            comment.userId.isBlank() -> {
                val n = numberByUserId.getOrPut("unknown-${comment.id}") { nextNumber++ }
                "익명 $n"
            }
            else -> {
                val n = numberByUserId.getOrPut(comment.userId) { nextNumber++ }
                "익명 $n"
            }
        }
        comment.copy(authorLabel = label)
    }
}

/** 좋아요/싫어요 토글 후 카운트·내 반응을 로컬에 반영 */
fun CommunityPost.withToggledReaction(target: ReportReaction): CommunityPost {
    val next = if (myReaction == target) null else target
    var likes = likeCount
    var dislikes = dislikeCount
    when (myReaction) {
        ReportReaction.Like -> likes = (likes - 1).coerceAtLeast(0)
        ReportReaction.Dislike -> dislikes = (dislikes - 1).coerceAtLeast(0)
        null -> Unit
    }
    when (next) {
        ReportReaction.Like -> likes += 1
        ReportReaction.Dislike -> dislikes += 1
        null -> Unit
    }
    return copy(likeCount = likes, dislikeCount = dislikes, myReaction = next)
}

enum class CommunityBoardTab(val label: String) {
    Reports("제보"),
    Posts("소통"),
}
