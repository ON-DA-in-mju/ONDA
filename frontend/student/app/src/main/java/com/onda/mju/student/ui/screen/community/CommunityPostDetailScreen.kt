package com.onda.mju.student.ui.screen.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)
private val DislikeRed = Color(0xFFEF4444)

@Composable
fun CommunityPostDetailScreen(
    post: CommunityPost,
    modifier: Modifier = Modifier,
    comments: List<CommunityComment> = emptyList(),
    isMine: Boolean = false,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onDislikeClick: () -> Unit = {},
    onSubmitComment: (String) -> Unit = {},
    onEditComment: (commentId: String, text: String) -> Unit = { _, _ -> },
    onDeleteComment: (String) -> Unit = {},
) {
    val likeActive = post.myReaction == ReportReaction.Like
    val dislikeActive = post.myReaction == ReportReaction.Dislike
    var draft by remember(post.id) { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                text = "게시글",
                color = TitleBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "소통",
                        color = OndaBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(SoftBlue, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isMine) {
                        Text(
                            text = "수정",
                            color = OndaBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onEditClick)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                        Text(
                            text = "삭제",
                            color = DislikeRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onDeleteClick)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.title,
                    color = TitleBlack,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${post.registeredAt} · 익명 · 댓글 ${comments.size}",
                    color = BodyGray,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = post.body,
                    color = TitleBlack,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text("이 글이 도움이 됐나요?", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DetailReactionChip(
                        icon = Icons.Filled.ThumbUp,
                        label = "공감",
                        count = post.likeCount,
                        active = likeActive,
                        activeColor = OndaBlue,
                        onClick = onLikeClick,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DetailReactionChip(
                        icon = Icons.Filled.ThumbDown,
                        label = "비공감",
                        count = post.dislikeCount,
                        active = dislikeActive,
                        activeColor = DislikeRed,
                        onClick = onDislikeClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text(
                    "댓글 ${comments.size}",
                    color = TitleBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (comments.isEmpty()) {
                    Text(
                        "아직 댓글이 없습니다. 첫 댓글을 남겨 보세요.",
                        color = BodyGray,
                        fontSize = 13.sp,
                    )
                } else {
                    comments.forEachIndexed { index, comment ->
                        if (index > 0) {
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        CommunityCommentRow(
                            comment = comment,
                            onEdit = { text -> onEditComment(comment.id, text) },
                            onDelete = { onDeleteComment(comment.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OndaBlue),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = OndaBlue,
                ),
            ) {
                Text("목록으로 돌아가기", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, CardBorder))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 500) draft = it },
                placeholder = { Text("댓글을 입력하세요", color = BodyGray, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OndaBlue,
                    unfocusedBorderColor = CardBorder,
                ),
                maxLines = 3,
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = draft.trim()
                    if (text.isEmpty()) return@IconButton
                    onSubmitComment(text)
                    draft = ""
                },
                enabled = draft.trim().isNotEmpty(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "댓글 등록",
                    tint = if (draft.trim().isNotEmpty()) OndaBlue else BodyGray,
                )
            }
        }
    }
}

@Composable
internal fun CommunityCommentRow(
    comment: CommunityComment,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by remember(comment.id, comment.body, comment.isDeleted) {
        mutableStateOf(false)
    }
    var editDraft by remember(comment.id, comment.body) {
        mutableStateOf(comment.body)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(comment.authorLabel, color = TitleBlack, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(comment.timeLabel, color = BodyGray, fontSize = 11.sp)
            Spacer(modifier = Modifier.weight(1f))
            if (comment.isMine && !comment.isDeleted) {
                if (editing) {
                    Text(
                        text = "취소",
                        color = BodyGray,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                editing = false
                                editDraft = comment.body
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Text(
                        text = "저장",
                        color = OndaBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val text = editDraft.trim()
                                if (text.isEmpty()) return@clickable
                                onEdit(text)
                                editing = false
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                } else {
                    Text(
                        text = "수정",
                        color = OndaBlue,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                editDraft = comment.body
                                editing = true
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Text(
                        text = "삭제",
                        color = DislikeRed,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        when {
            comment.isDeleted -> {
                Text(
                    text = "삭제된 댓글입니다.",
                    color = BodyGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
            editing -> {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { if (it.length <= 500) editDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OndaBlue,
                        unfocusedBorderColor = CardBorder,
                    ),
                    maxLines = 4,
                )
            }
            else -> {
                Text(comment.body, color = TitleBlack, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun DetailReactionChip(
    icon: ImageVector,
    label: String,
    count: Int,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (active) activeColor else CardBorder
    val bg = if (active) activeColor.copy(alpha = 0.08f) else Color.White
    val content = if (active) activeColor else BodyGray
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label $count",
            color = content,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
