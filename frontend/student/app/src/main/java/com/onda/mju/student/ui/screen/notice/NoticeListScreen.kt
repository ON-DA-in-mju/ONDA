package com.onda.mju.student.ui.screen.notice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)
private val UrgentBg = Color(0xFFFFF1F2)
private val UrgentRed = Color(0xFFE11D48)

/** Intrinsic ratio of bus_detail_hero.png (159×128). */
private const val NoticeHeaderIllustAspect = 159f / 128f

@Composable
fun NoticeListScreen(
    modifier: Modifier = Modifier,
    notices: List<NoticeItem> = emptyList(),
    onNoticeClick: (String) -> Unit = {},
    onTimetableClick: () -> Unit = {},
    onStopGuideClick: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(NoticeTypeFilter.All) }
    var typeMenuOpen by remember { mutableStateOf(false) }
    val filtered = remember(notices, query, typeFilter) {
        notices.filterByType(typeFilter).search(query)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 8.dp),
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.55f)) {
                Text("공지사항", color = TitleBlack, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("셔틀버스 관련 공지를 확인하세요", color = BodyGray, fontSize = 13.sp)
            }
            Image(
                painter = painterResource(id = R.drawable.bus_detail_hero),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(148.dp)
                    .aspectRatio(NoticeHeaderIllustAspect),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, null, tint = BodyGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("공지 제목을 검색하세요", color = BodyGray, fontSize = 13.sp)
                            }
                            inner()
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .border(
                            width = if (typeMenuOpen) 1.5.dp else 1.dp,
                            color = if (typeMenuOpen || typeFilter != NoticeTypeFilter.All) {
                                OndaBlue
                            } else {
                                CardBorder
                            },
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { typeMenuOpen = !typeMenuOpen }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Tune, null, tint = OndaBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        typeFilter.label,
                        color = TitleBlack,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        null,
                        tint = if (typeMenuOpen) OndaBlue else BodyGray,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (typeMenuOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 240.dp)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                ) {
                    NoticeTypeFilter.entries.forEach { option ->
                        val selected = option == typeFilter
                        Text(
                            text = option.label,
                            color = if (selected) OndaBlue else TitleBlack,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    typeFilter = option
                                    typeMenuOpen = false
                                }
                                .background(if (selected) SoftBlue else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickAction(
                icon = Icons.Filled.Event,
                label = "전체 시간표",
                onClick = onTimetableClick,
                modifier = Modifier.weight(1f),
            )
            QuickAction(
                icon = Icons.Filled.Place,
                label = "정류장 안내",
                onClick = onStopGuideClick,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (filtered.isEmpty()) {
                Text(
                    text = when {
                        notices.isEmpty() -> "등록된 공지가 없습니다."
                        query.isNotBlank() || typeFilter != NoticeTypeFilter.All ->
                            "조건에 맞는 공지가 없습니다."
                        else -> "등록된 공지가 없습니다."
                    },
                    color = BodyGray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                filtered.forEach { item ->
                    if (item.isUrgentCard) {
                        UrgentNoticeCard(item = item, onClick = { onNoticeClick(item.id) })
                    } else {
                        StandardNoticeCard(item = item, onClick = { onNoticeClick(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = OndaBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = TitleBlack, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UrgentNoticeCard(item: NoticeItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(UrgentBg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(64.dp)
                .background(UrgentRed, RoundedCornerShape(999.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, null, tint = UrgentRed, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.badge.label, color = UrgentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.title, color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.datetime, color = BodyGray, fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BodyGray)
    }
}

@Composable
private fun StandardNoticeCard(item: NoticeItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(SoftBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, null, tint = OndaBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    color = TitleBlack,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    item.badge.label,
                    color = item.badge.color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(item.badge.color.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.description, color = BodyGray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (item.edited) "${item.datetime} · 수정" else item.datetime,
                color = BodyGray,
                fontSize = 11.sp,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BodyGray)
    }
}
