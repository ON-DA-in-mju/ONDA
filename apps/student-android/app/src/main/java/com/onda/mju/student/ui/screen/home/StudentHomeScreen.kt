package com.onda.mju.student.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R
import com.onda.mju.student.core.calendar.AcademicCalendar
import com.onda.mju.student.ui.screen.notification.formatUnreadBadgeLabel
import com.onda.mju.student.ui.screen.route.RouteStatus
import com.onda.mju.student.ui.screen.route.RouteUiModel
import com.onda.mju.student.ui.screen.route.sampleRouteList
import com.onda.mju.student.ui.theme.ONDAStudentTheme
import kotlinx.coroutines.delay

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val SoftBlueBg = Color(0xFFEDF4FE)
private val StatusTeal = Color(0xFF0D9488)
private val BadgeGreenBg = Color(0xFFD1FAE5)
private val BadgeGreenText = Color(0xFF0F766E)
private val BadgePendingBg = Color(0xFFDBEAFE)
private val BadgePendingText = Color(0xFF1D4ED8)
private val CardBorder = Color(0xFFE8EDF2)
private val StarYellow = Color(0xFFFBBF24)
private val SuccessGreen = Color(0xFF22C55E)
private val UpdateBadgeBg = Color(0xFFE8F7F5)
private val UpdateBadgeText = Color(0xFF0F766E)
private val UnreadBadgeRed = Color(0xFFE11D48)

private const val SideInsetFraction = 18f / 414f
/** Intrinsic ratio of home_hero.png (illustration only, phone chrome cropped). */
private const val HeroAspect = 398f / 169f

@Composable
fun StudentHomeScreen(
    modifier: Modifier = Modifier,
    noticeBannerTitle: String = "등록된 공지가 없습니다.",
    unreadNotificationCount: Int = 0,
    /** Epoch millis when mock/live operation data was last received. */
    operationLastUpdatedAtMillis: Long = System.currentTimeMillis(),
    scheduleKindLabel: String = AcademicCalendar.todayScheduleKindLabel(),
    routes: List<RouteUiModel> = sampleRouteList(),
    onNotificationClick: () -> Unit = {},
    onStatusTimetableClick: () -> Unit = {},
    onNoticeBannerClick: () -> Unit = {},
    onFavoriteManageClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onRouteShortcutClick: (String) -> Unit = {},
    onQuickActionClick: (String) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val density = LocalDensity.current
        val sideInset = with(density) {
            (maxWidth.toPx() * SideInsetFraction).toDp()
        }.coerceIn(16.dp, 22.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(HeroAspect),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_hero),
                    contentDescription = "ON-DA",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 6.dp),
                ) {
                    val badgeLabel = formatUnreadBadgeLabel(unreadNotificationCount)
                        .ifEmpty { null }
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "알림",
                            tint = OndaBlue,
                            modifier = Modifier.size(24.dp),
                        )
                        if (badgeLabel != null) {
                            val isMulti = badgeLabel.length > 1
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 1.dp, y = (-1).dp)
                                    .height(15.dp)
                                    .defaultMinSize(minWidth = 15.dp)
                                    .border(1.25.dp, Color.White, RoundedCornerShape(999.dp))
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(UnreadBadgeRed)
                                    .padding(horizontal = if (isMulti) 3.5.dp else 0.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 9.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sideInset)
                    .padding(bottom = 20.dp),
            ) {
                OperationStatusCard(
                    lastUpdatedAtMillis = operationLastUpdatedAtMillis,
                    scheduleKindLabel = scheduleKindLabel,
                    onTimetableClick = onStatusTimetableClick,
                )

                Spacer(modifier = Modifier.height(12.dp))

                NoticeBanner(
                    title = noticeBannerTitle,
                    onClick = onNoticeBannerClick,
                )

                Spacer(modifier = Modifier.height(22.dp))

                SectionHeader(
                    title = "즐겨찾기",
                    actionLabel = "관리",
                    onActionClick = onFavoriteManageClick,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FavoriteCard(
                    route = routes.firstOrNull { it.id == "giheung" } ?: routes.firstOrNull(),
                    onClick = onFavoriteClick,
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "노선 바로가기",
                    color = TitleBlack,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                RouteShortcutRow(
                    routes = routes,
                    onRouteClick = onRouteShortcutClick,
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "바로가기",
                    color = TitleBlack,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                QuickActionRow(onActionClick = onQuickActionClick)
            }
        }
    }
}

@Composable
private fun OperationStatusCard(
    lastUpdatedAtMillis: Long,
    scheduleKindLabel: String,
    onTimetableClick: () -> Unit,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastUpdatedAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val lastUpdatedLabel = remember(lastUpdatedAtMillis, nowMillis) {
        formatOperationLastUpdatedLabel(lastUpdatedAtMillis, nowMillis)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "오늘 정상 운행 중이에요",
                        color = StatusTeal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${scheduleKindLabel} 시간표가 적용됩니다",
                        color = BodyGray,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = lastUpdatedLabel,
                    color = UpdateBadgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(UpdateBadgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onTimetableClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "시간표 확인하기",
                    color = OndaBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = OndaBlue,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun NoticeBanner(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SoftBlueBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Campaign,
            contentDescription = null,
            tint = OndaBlue,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "공식",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(OndaBlue)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color(0xFF1E3A8A),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = OndaBlue,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = TitleBlack,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onActionClick)
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = actionLabel,
                color = BodyGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BodyGray,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    route: RouteUiModel?,
    onClick: () -> Unit,
) {
    val statusLabel = route?.homeStatusBadgeText() ?: "운행 예정"
    val (badgeBg, badgeFg) = route?.homeStatusBadgeColors()
        ?: (BadgePendingBg to BadgePendingText)
    val detailText = route?.homeFavoriteDetailText() ?: "다음 출발 -"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = StarYellow,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SoftBlueBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsBus,
                contentDescription = null,
                tint = OndaBlue,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = route?.name ?: "기흥역 통학버스",
                    color = TitleBlack,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
                StatusPill(text = statusLabel, bg = badgeBg, fg = badgeFg)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detailText,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun RouteShortcutRow(
    routes: List<RouteUiModel>,
    onRouteClick: (String) -> Unit,
) {
    val shortcutRoutes = if (routes.isNotEmpty()) {
        routes
    } else {
        sampleRouteList()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        shortcutRoutes.forEach { route ->
            val (badgeBg, badgeFg) = route.homeStatusBadgeColors()
            RouteShortcutCard(
                icon = route.homeShortcutIcon(),
                title = route.name,
                badge = route.homeShortcutBadgeText(),
                badgeBg = badgeBg,
                badgeFg = badgeFg,
                nextDeparture = "다음 출발 ${route.nextDeparture}",
                onClick = { onRouteClick(route.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun RouteUiModel.homeStatusBadgeText(): String = when (status) {
    RouteStatus.RUNNING -> "운행 중"
    RouteStatus.SCHEDULED -> "운행 예정"
}

private fun RouteUiModel.homeShortcutBadgeText(): String = when (status) {
    RouteStatus.RUNNING -> {
        val count = activeVehicleCount
        if (count != null) "${count}대 운행 중" else "운행 중"
    }
    RouteStatus.SCHEDULED -> "운행 예정"
}

private fun RouteUiModel.homeStatusBadgeColors(): Pair<Color, Color> = when (status) {
    RouteStatus.RUNNING -> BadgeGreenBg to BadgeGreenText
    RouteStatus.SCHEDULED -> BadgePendingBg to BadgePendingText
}

private fun RouteUiModel.homeFavoriteDetailText(): String {
    val count = activeVehicleCount
    return if (status == RouteStatus.RUNNING && count != null) {
        "다음 출발 $nextDeparture / 현재 ${count}대 운행 중"
    } else {
        "다음 출발 $nextDeparture"
    }
}

private fun RouteUiModel.homeShortcutIcon(): ImageVector = when (id) {
    "myeongji_station" -> Icons.Filled.Subway
    "city_shuttle" -> Icons.Filled.Apartment
    else -> Icons.Filled.DirectionsBus
}

@Composable
private fun RouteShortcutCard(
    icon: ImageVector,
    title: String,
    badge: String,
    badgeBg: Color,
    badgeFg: Color,
    nextDeparture: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SoftBlueBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OndaBlue,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            color = TitleBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusPill(text = badge, bg = badgeBg, fg = badgeFg)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = nextDeparture,
            color = OndaBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuickActionRow(onActionClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickActionButton(
            icon = Icons.Outlined.CalendarMonth,
            label = "오늘 시간표",
            onClick = { onActionClick("오늘 시간표") },
            modifier = Modifier.weight(1f),
        )
        QuickActionButton(
            icon = Icons.Outlined.Place,
            label = "정류장 안내",
            onClick = { onActionClick("정류장 안내") },
            modifier = Modifier.weight(1f),
        )
        QuickActionButton(
            icon = Icons.Outlined.Campaign,
            label = "공지사항",
            onClick = { onActionClick("공지사항") },
            modifier = Modifier.weight(1f),
        )
        QuickActionButton(
            icon = Icons.Filled.EditNote,
            label = "간편 제보",
            onClick = { onActionClick("간편 제보") },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SoftBlueBg, RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = OndaBlue,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = TitleBlack,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    bg: Color,
    fg: Color,
) {
    Text(
        text = text,
        color = fg,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_7")
@Composable
private fun StudentHomeScreenPreview() {
    ONDAStudentTheme {
        StudentHomeScreen()
    }
}

/**
 * Builds the home operation-card "마지막 갱신 …" label from the data timestamp.
 * Reuse with live Supabase updates by passing the latest received epoch millis.
 */
internal fun formatOperationLastUpdatedLabel(
    lastUpdatedAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val elapsedSec = ((nowMillis - lastUpdatedAtMillis).coerceAtLeast(0L)) / 1_000L
    val relative = when {
        elapsedSec < 1L -> "방금 전"
        elapsedSec < 60L -> "${elapsedSec}초 전"
        elapsedSec < 3_600L -> "${elapsedSec / 60L}분 전"
        else -> "${elapsedSec / 3_600L}시간 전"
    }
    return "마지막 갱신 $relative"
}
