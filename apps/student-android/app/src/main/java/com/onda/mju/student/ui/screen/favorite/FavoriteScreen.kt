package com.onda.mju.student.ui.screen.favorite

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R
import com.onda.mju.student.ui.theme.ONDAStudentTheme

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlueBg = Color(0xFFEDF4FE)
private val BadgeGreenBg = Color(0xFFD1FAE5)
private val BadgeGreenText = Color(0xFF0F766E)
private val StarYellow = Color(0xFFFBBF24)
private val TabTrack = Color(0xFFF3F4F6)

/** Shared list-card visual rules for route & stop cards. */
private val CardRadius = 14.dp
private val CardHorizontalPadding = 14.dp
private val CardVerticalPadding = 14.dp
private val LeadingIconSize = 44.dp
private val TrailingActionWidth = 28.dp
private val ListSideInset = 16.dp
private val CardGap = 10.dp

@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    initialTab: FavoriteTab = FavoriteTab.Route,
    onBackClick: () -> Unit = {},
    onManageClick: () -> Unit = {},
    onRouteClick: (String) -> Unit = {},
    /**
     * Future: Notice → 정류장 안내 → stop detail.
     * Keep this callback and wire destination when that screen exists.
     */
    onStopClick: (String) -> Unit = {},
    onNotificationSettingClick: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val routes = remember { sampleFavoriteRoutes() }
    val stops = remember { sampleFavoriteStops() }

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
                text = "즐겨찾기",
                color = TitleBlack,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            FavoriteHeader()
            Spacer(modifier = Modifier.height(14.dp))
            FavoriteTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = ListSideInset),
            )
            Spacer(modifier = Modifier.height(18.dp))
            SectionTitleRow(
                title = if (selectedTab == FavoriteTab.Route) {
                    "즐겨찾는 노선"
                } else {
                    "즐겨찾는 정류장"
                },
                onManageClick = onManageClick,
                modifier = Modifier.padding(horizontal = ListSideInset),
            )
            Spacer(modifier = Modifier.height(10.dp))

            key(selectedTab) {
                when (selectedTab) {
                    FavoriteTab.Route -> {
                        routes.forEach { route ->
                            FavoriteRouteCard(
                                route = route,
                                onClick = { onRouteClick(route.id) },
                                modifier = Modifier
                                    .padding(horizontal = ListSideInset)
                                    .padding(bottom = CardGap),
                            )
                        }
                    }

                    FavoriteTab.Stop -> {
                        stops.forEach { stop ->
                            FavoriteStopCard(
                                stop = stop,
                                onClick = { onStopClick(stop.id) },
                                modifier = Modifier
                                    .padding(horizontal = ListSideInset)
                                    .padding(bottom = CardGap),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FavoriteAlertCard(
                onSettingClick = onNotificationSettingClick,
                modifier = Modifier.padding(horizontal = ListSideInset),
            )
        }
    }
}

@Composable
private fun FavoriteHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ListSideInset)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "즐겨찾기",
                color = TitleBlack,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "자주 이용하는 노선과 정류장을\n빠르게 확인하세요",
                color = BodyGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.sp,
            )
        }
        Image(
            painter = painterResource(id = R.drawable.favorite_header_illustration),
            contentDescription = null,
            modifier = Modifier
                .width(128.dp)
                .aspectRatio(169f / 105f),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun FavoriteTabRow(
    selectedTab: FavoriteTab,
    onTabSelected: (FavoriteTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(TabTrack)
            .padding(4.dp),
    ) {
        FavoriteTabChip(
            label = "노선",
            selected = selectedTab == FavoriteTab.Route,
            onClick = { onTabSelected(FavoriteTab.Route) },
            modifier = Modifier.weight(1f),
        )
        FavoriteTabChip(
            label = "정류장",
            selected = selectedTab == FavoriteTab.Stop,
            onClick = { onTabSelected(FavoriteTab.Stop) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FavoriteTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) OndaBlue else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else BodyGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionTitleRow(
    title: String,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = TitleBlack,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "즐겨찾기 관리",
            color = OndaBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .clickable(onClick = onManageClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FavoriteRouteCard(
    route: FavoriteRoute,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(CardRadius))
            .border(1.dp, CardBorder, RoundedCornerShape(CardRadius))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = CardHorizontalPadding, vertical = CardVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(LeadingIconSize)
                .clip(RoundedCornerShape(12.dp))
                .background(route.iconBg),
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
                    text = route.name,
                    color = TitleBlack,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = route.status,
                    color = BadgeGreenText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(BadgeGreenBg)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    append("다음 출발 ")
                    withStyle(SpanStyle(color = OndaBlue, fontWeight = FontWeight.Bold)) {
                        append(route.nextDeparture)
                    }
                },
                color = BodyGray,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildAnnotatedString {
                    append("현재 ")
                    withStyle(SpanStyle(color = OndaBlue, fontWeight = FontWeight.Bold)) {
                        append(route.operatingCount)
                    }
                    append(" 운행 중")
                },
                color = BodyGray,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        TrailingFavoriteActions()
    }
}

@Composable
private fun FavoriteStopCard(
    stop: FavoriteStop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(CardRadius))
            .border(1.dp, CardBorder, RoundedCornerShape(CardRadius))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = CardHorizontalPadding, vertical = CardVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Design uses a soft circular color chip (no heavy icon).
        Box(
            modifier = Modifier
                .size(LeadingIconSize)
                .clip(CircleShape)
                .background(stop.iconBg),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                color = TitleBlack,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "이용 가능 노선",
                color = BodyGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                stop.routes.forEach { tag ->
                    Text(
                        text = tag.label,
                        color = tag.fg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(tag.bg)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
        }

        TrailingFavoriteActions()
    }
}

@Composable
private fun TrailingFavoriteActions() {
    Column(
        modifier = Modifier.width(TrailingActionWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "즐겨찾기",
            tint = StarYellow,
            modifier = Modifier.size(20.dp),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FavoriteAlertCard(
    onSettingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SoftBlueBg)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = null,
            tint = OndaBlue,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "즐겨찾기 노선 알림",
                color = OndaBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "즐겨찾기한 노선의 출발 예정과\n운행 변경 알림을 받아보세요.",
                color = BodyGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "알림 설정",
            color = OndaBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, OndaBlue), RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable(onClick = onSettingClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun FavoriteScreenRoutePreview() {
    ONDAStudentTheme {
        FavoriteScreen(initialTab = FavoriteTab.Route)
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun FavoriteScreenStopPreview() {
    ONDAStudentTheme {
        FavoriteScreen(initialTab = FavoriteTab.Stop)
    }
}
