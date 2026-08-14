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
private val DislikeRed = Color(0xFFEF4444)

@Composable
fun CommunityDetailScreen(
    report: CommunityReport,
    modifier: Modifier = Modifier,
    comments: List<CommunityComment> = emptyList(),
    isMine: Boolean = false,
    onBackClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onDislikeClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onSubmitComment: (String) -> Unit = {},
    onEditComment: (commentId: String, text: String) -> Unit = { _, _ -> },
    onDeleteComment: (String) -> Unit = {},
) {
    val likeActive = report.myReaction == ReportReaction.Like
    val dislikeActive = report.myReaction == ReportReaction.Dislike
    var draft by remember(report.id) { mutableStateOf("") }

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
                text = "제보 상세",
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
                        text = report.type.label,
                        color = report.type.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(report.type.color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
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
                    text = report.stopName,
                    color = TitleBlack,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${report.routeLabel} · ${report.directionLabel}",
                    color = BodyGray,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${report.registeredAt} · 익명 · 댓글 ${comments.size}",
                    color = BodyGray,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = report.body,
                    color = TitleBlack,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            InfoBanner(text = "학생들의 제보입니다. 실제 상황과 다를 수 있어요.")

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text("이 제보가 도움이 됐나요?", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DetailReactionChip(
                        icon = Icons.Filled.ThumbUp,
                        label = "공감",
                        count = report.likeCount,
                        active = likeActive,
                        activeColor = OndaBlue,
                        onClick = onLikeClick,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DetailReactionChip(
                        icon = Icons.Filled.ThumbDown,
                        label = "비공감",
                        count = report.dislikeCount,
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
                Text("관련 정보", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                InfoRow("노선", report.routeLabel)
                HorizontalDivider(color = CardBorder)
                InfoRow("방향", report.directionLabel)
                HorizontalDivider(color = CardBorder)
                InfoRow("정류장", report.stopName)
                HorizontalDivider(color = CardBorder)
                InfoRow("제보 유형", report.type.label)
                HorizontalDivider(color = CardBorder)
                InfoRow("차량", report.vehicleLabel)
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = BodyGray,
            fontSize = 13.sp,
            modifier = Modifier.width(88.dp),
        )
        Text(value, color = TitleBlack, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
