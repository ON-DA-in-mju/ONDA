package com.onda.mju.student.ui.screen.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val WarnBg = Color(0xFFFFF4E5)
private val WarnText = Color(0xFFEA580C)

@Composable
fun CommunityListScreen(
    modifier: Modifier = Modifier,
    reports: List<CommunityReport> = sampleCommunityReports(),
    readIds: Set<String> = emptySet(),
    onReportClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
) {
    var selectedFilter by remember { mutableStateOf(CommunityFilter.All) }
    val filtered = remember(reports, selectedFilter) { reports.filterBy(selectedFilter) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "커뮤니티",
                color = TitleBlack,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CommunityFilter.entries.forEach { filter ->
                    val selected = filter == selectedFilter
                    Column(
                        modifier = Modifier
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = filter.label,
                            color = if (selected) OndaBlue else BodyGray,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 28.dp else 0.dp)
                                .height(3.dp)
                                .background(OndaBlue, RoundedCornerShape(999.dp)),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(label = "전체 노선", modifier = Modifier.weight(1f))
                FilterChip(label = "전체 정류장", modifier = Modifier.weight(1f))
                FilterChip(label = "최신순", modifier = Modifier.weight(1f))
            }

            InfoBanner(
                text = "학생들의 제보입니다. 실제 상황과 다를 수 있어요.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                filtered.forEach { report ->
                    ReportCard(
                        report = report,
                        isRead = report.id in readIds,
                        onClick = { onReportClick(report.id) },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        FloatingActionButton(
            onClick = onCreateClick,
            containerColor = OndaBlue,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .fillMaxWidth(0.55f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("제보하기", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(36.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = TitleBlack, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = BodyGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun InfoBanner(text: String, modifier: Modifier = Modifier, tintBg: Color = WarnBg, tint: Color = WarnText) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tintBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = tint, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

private val ReadCardBg = Color(0xFFF3F4F6)
private val ReadGray = Color(0xFF9CA3AF)

@Composable
internal fun ReportCard(
    report: CommunityReport,
    onClick: () -> Unit,
    isRead: Boolean = false,
) {
    val typeColor = if (isRead) ReadGray else report.type.color
    val titleColor = if (isRead) ReadGray else TitleBlack
    val metaColor = if (isRead) ReadGray else BodyGray
    val cardBg = if (isRead) ReadCardBg else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(typeColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(report.type.icon, null, tint = typeColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(report.type.label, color = typeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${report.routeLabel} | ${report.directionLabel}",
                color = metaColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(report.stopName, color = titleColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${report.timeLabel} · ${report.reporterCount}명이 제보",
                color = metaColor,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ThumbUp, null, tint = metaColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("${report.likeCount}", color = metaColor, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.Filled.ThumbDown, null, tint = metaColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("${report.dislikeCount}", color = metaColor, fontSize = 12.sp)
        }
    }
}
