package com.onda.mju.student.ui.screen.my

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.ui.screen.community.CommunityPost

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val PageBg = Color(0xFFF5F7FA)
private val SoftBlue = Color(0xFFEDF4FE)
private val DeleteRed = Color(0xFFEF4444)

@Composable
fun MyPostDetailScreen(
    post: CommunityPost,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                "내가 쓴 글 상세",
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OndaBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.EditNote,
                            contentDescription = null,
                            tint = OndaBlue,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "소통 글",
                        color = OndaBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    post.title,
                    color = TitleBlack,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${post.registeredAt} · 익명 · 댓글 ${post.commentCount}",
                    color = BodyGray,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionCard(title = "글 내용") {
                Text(post.body, color = TitleBlack, fontSize = 14.sp, lineHeight = 22.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionCard(title = "반응") {
                InfoRow(label = "공감") {
                    Text(
                        "${post.likeCount}",
                        color = TitleBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "비공감") {
                    Text(
                        "${post.dislikeCount}",
                        color = TitleBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "댓글") {
                    Text(
                        "${post.commentCount}",
                        color = TitleBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftBlue, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, null, tint = OndaBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "내가 쓴 글은 수정·삭제할 수 있습니다. 삭제 시 복구할 수 없습니다.",
                    color = OndaBlue,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, OndaBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OndaBlue),
            ) {
                Text("수정하기", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, DeleteRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed),
            ) {
                Text("삭제하기", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(title, color = TitleBlack, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = BodyGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        value()
    }
}
