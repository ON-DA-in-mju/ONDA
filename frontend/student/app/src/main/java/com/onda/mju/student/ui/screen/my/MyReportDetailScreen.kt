package com.onda.mju.student.ui.screen.my

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.ui.screen.community.CommunityReport

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val PageBg = Color(0xFFF5F7FA)
private val SoftBlue = Color(0xFFEDF4FE)
private val ValidBg = Color(0xFFD1FAE5)
private val ValidText = Color(0xFF059669)
private val DeleteRed = Color(0xFFEF4444)

@Composable
fun MyReportDetailScreen(
    report: CommunityReport,
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
                "내 제보 상세",
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
                            .background(report.type.color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            report.type.icon,
                            contentDescription = null,
                            tint = report.type.color,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        report.type.label,
                        color = report.type.color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "수정",
                        color = OndaBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onEditClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text(
                        text = "삭제",
                        color = DeleteRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onDeleteClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                StatusChip(valid = report.isValid)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "${report.routeLabel} · ${report.vehicleLabel}",
                    color = BodyGray,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    report.stopName,
                    color = TitleBlack,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${report.registeredAt} · 익명 · 댓글 ${report.commentCount}",
                    color = BodyGray,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionCard(title = "제보 내용") {
                Text(report.body, color = TitleBlack, fontSize = 14.sp, lineHeight = 22.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionCard(title = "반응") {
                InfoRow(label = "공감") {
                    Text(
                        "${report.likeCount}",
                        color = TitleBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "비공감") {
                    Text(
                        "${report.dislikeCount}",
                        color = TitleBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "댓글") {
                    Text(
                        "${report.commentCount}",
                        color = TitleBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionCard(title = "처리 상태") {
                InfoRow(label = "현재 상태") { StatusChip(valid = report.isValid) }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "유효 시간") {
                    Text("등록 후 10분", color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "자동 만료") {
                    Text(
                        if (report.isValid) "예정" else "완료",
                        color = TitleBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionCard(title = "등록 정보") {
                InfoRow(label = "노선") {
                    Text(report.routeLabel, color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "차량") {
                    Text(report.vehicleLabel, color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "정류장") {
                    Text(report.stopName, color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = CardBorder)
                InfoRow(label = "제보 유형") {
                    Text(report.type.label, color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                    "제보는 등록 후 10분 동안 유효하며, 이후 자동으로 만료됩니다. 수정·삭제는 오른쪽 상단에서 할 수 있습니다.",
                    color = OndaBlue,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatusChip(valid: Boolean) {
    val bg = if (valid) ValidBg else Color(0xFFF3F4F6)
    val fg = if (valid) ValidText else BodyGray
    Text(
        text = if (valid) "유효" else "만료",
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
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
