package com.onda.mju.student.ui.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.ui.theme.ONDAStudentTheme

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val ChipGrayBg = Color(0xFFF3F4F6)
private val ChipGrayText = Color(0xFF4B5563)
private val NewRed = Color(0xFFE11D48)
private val UnreadDot = Color(0xFF2563EB)
private val ReadMuted = Color(0xFF9CA3AF)

@Composable
fun NotificationListScreen(
    notifications: List<NotificationItem>,
    unreadIds: Set<Int>,
    markAllDone: Boolean,
    modifier: Modifier = Modifier,
    showUnreadOnly: Boolean = false,
    onShowUnreadOnlyChange: (Boolean) -> Unit = {},
    onBackClick: () -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onNotificationClick: (Int) -> Unit = {},
) {
    var selectedFilter by remember { mutableStateOf(NotificationFilter.All) }

    val unreadCount = notifications.count { it.id in unreadIds }
    val filtered = remember(notifications, selectedFilter, showUnreadOnly, unreadIds) {
        when {
            showUnreadOnly -> notifications.filter { it.id in unreadIds }
            selectedFilter == NotificationFilter.All -> notifications
            else -> notifications.filter { selectedFilter in it.filters }
        }
    }
    val unreadHeaderText = if (unreadCount == 0) {
        "읽지 않은 알림이 없습니다."
    } else {
        "읽지 않은 알림 ${unreadCount}개"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TitleBlack,
                )
            }
            Text(
                text = "알림",
                color = TitleBlack,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = unreadHeaderText,
                color = if (unreadCount > 0 || showUnreadOnly) OndaBlue else BodyGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onShowUnreadOnlyChange(true) }
                    .padding(vertical = 2.dp),
            )
            Text(
                text = "모두 읽음",
                color = if (markAllDone) Color.White else ChipGrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (markAllDone) OndaBlue else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (markAllDone) OndaBlue else CardBorder,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable(onClick = onMarkAllRead)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NotificationFilter.entries.forEach { filter ->
                // Unread-only mode clears category selection (all chips unselected / gray).
                val selected = !showUnreadOnly && filter == selectedFilter
                Text(
                    text = filter.label,
                    color = if (selected) Color.White else ChipGrayText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) OndaBlue else ChipGrayBg)
                        .clickable {
                            onShowUnreadOnlyChange(false)
                            selectedFilter = filter
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (showUnreadOnly) {
                                "읽지 않은 알림이 없습니다."
                            } else {
                                "알림이 없습니다."
                            },
                            color = BodyGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        unread = item.id in unreadIds,
                        onClick = { onNotificationClick(item.id) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem,
    unread: Boolean,
    onClick: () -> Unit,
) {
    val titleColor = if (unread) TitleBlack else Color(0xFF4B5563)
    val categoryColor = if (unread) item.categoryColor else ReadMuted
    val iconBg = if (unread) item.iconBg else Color(0xFFD1D5DB)
    val iconTint = if (unread) item.iconTint else Color.White
    val subtitleColor = if (unread) BodyGray else Color(0xFF9CA3AF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.categoryLabel,
                    color = categoryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (unread) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEW",
                        color = NewRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, NewRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!item.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.subtitle,
                    color = subtitleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.timeLabel,
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.height(64.dp),
        ) {
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(UnreadDot),
                )
            } else {
                Spacer(modifier = Modifier.size(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun NotificationListScreenPreview() {
    ONDAStudentTheme {
        NotificationListScreen(
            notifications = sampleNotifications(),
            unreadIds = setOf(1, 2, 3),
            markAllDone = false,
        )
    }
}
