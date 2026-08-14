package com.onda.mju.student.ui.screen.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.ui.theme.ONDAStudentTheme

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val SubtitleGray = Color(0xFF9CA3AF)
private val PathGray = Color(0xFF4B5563)
private val MetaGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE5E7EB)
private val FilterBorder = Color(0xFFE5E7EB)
private val RunningBadgeBg = Color(0xFF14B8A6)
private val ScheduledBadgeBg = Color(0xFF60A5FA)
private val EndedBadgeBg = Color(0xFF9CA3AF)

/** Bidirectional arrow used in design (U+21C4). */
private const val BidirectionalArrow = "\u21C4"

/** Design content reference (STU-02-00). */
private const val RefW = 414f
private const val SideFrac = 20f / RefW

/** Intrinsic ratio of route_list_header_illustration.png (wide banner). */
private const val HeaderIllustAspect = 402f / 156f

/** Shared circular thumb size for every route card. */
private val RouteThumbSize = 92.dp

@Composable
fun RouteListScreen(
    modifier: Modifier = Modifier,
    routes: List<RouteUiModel> = sampleRouteList(),
    onRouteClick: (String) -> Unit = {},
) {
    var selectedFilter by remember { mutableStateOf(RouteFilter.ALL) }
    val visibleRoutes = remember(routes, selectedFilter) {
        routes.filterBy(selectedFilter)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val density = LocalDensity.current
        val sideInset = with(density) {
            (maxWidth.toPx() * SideFrac).toDp()
        }.coerceIn(18.dp, 24.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            RouteListHeader(
                sideInset = sideInset,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(18.dp))

            RouteFilterBar(
                selected = selectedFilter,
                onSelected = { selectedFilter = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sideInset),
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (visibleRoutes.isEmpty()) {
                Text(
                    text = "해당 상태의 노선이 없습니다.",
                    color = MetaGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = sideInset, vertical = 24.dp),
                )
            } else {
                visibleRoutes.forEach { route ->
                    RouteCard(
                        route = route,
                        thumbSize = RouteThumbSize,
                        onClick = { onRouteClick(route.id) },
                        modifier = Modifier
                            .padding(horizontal = sideInset)
                            .padding(bottom = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteListHeader(
    sideInset: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HeaderIllustAspect),
    ) {
        Image(
            painter = painterResource(id = com.onda.mju.student.R.drawable.route_list_header_illustration),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alignment = Alignment.Center,
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.52f)
                .padding(start = sideInset, end = 8.dp),
        ) {
            Text(
                text = "노선",
                color = TitleBlack,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "이용할 셔틀 노선을 선택하세요",
                color = SubtitleGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun RouteFilterBar(
    selected: RouteFilter,
    onSelected: (RouteFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, FilterBorder, RoundedCornerShape(999.dp))
            .background(Color.White)
            .padding(3.dp),
    ) {
        FilterTab(
            label = "전체",
            selected = selected == RouteFilter.ALL,
            onClick = { onSelected(RouteFilter.ALL) },
            modifier = Modifier.weight(1f),
        )
        FilterTab(
            label = "운행중",
            selected = selected == RouteFilter.RUNNING,
            onClick = { onSelected(RouteFilter.RUNNING) },
            modifier = Modifier.weight(1f),
        )
        FilterTab(
            label = "운행 예정",
            selected = selected == RouteFilter.SCHEDULED,
            onClick = { onSelected(RouteFilter.SCHEDULED) },
            modifier = Modifier.weight(1.15f),
        )
        FilterTab(
            label = "운행 종료",
            selected = selected == RouteFilter.ENDED,
            onClick = { onSelected(RouteFilter.ENDED) },
            modifier = Modifier.weight(1.15f),
        )
    }
}

@Composable
private fun FilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) OndaBlue else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MetaGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun RouteCard(
    route: RouteUiModel,
    thumbSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = thumbSize + 28.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = route.imageRes),
                contentDescription = route.name,
                modifier = Modifier
                    .size(thumbSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.name,
                    color = TitleBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${route.fromLabel} $BidirectionalArrow ${route.toLabel}",
                    color = PathGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                RouteStatusBadge(status = route.status)
                Spacer(modifier = Modifier.height(6.dp))
                RouteDetailLine(route = route)
            }
        }
    }
}

@Composable
private fun RouteStatusBadge(status: RouteStatus) {
    val (bg, label) = when (status) {
        RouteStatus.RUNNING -> RunningBadgeBg to "운행 중"
        RouteStatus.SCHEDULED -> ScheduledBadgeBg to "운행 예정"
        RouteStatus.ENDED -> EndedBadgeBg to "운행 종료"
    }
    Text(
        text = label,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
private fun RouteDetailLine(route: RouteUiModel) {
    val count = route.activeVehicleCount
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = MetaGray, fontWeight = FontWeight.Medium)) {
                when {
                    route.status == RouteStatus.RUNNING && count != null ->
                        append("현재 ${count}대 운행 중 | 다음 출발 ")
                    route.status == RouteStatus.ENDED ->
                        append("오늘 운행 종료")
                    else ->
                        append("다음 출발 ")
                }
            }
            if (route.status != RouteStatus.ENDED) {
                withStyle(SpanStyle(color = OndaBlue, fontWeight = FontWeight.Bold)) {
                    append(route.nextDeparture)
                }
            }
        },
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun RouteListScreenPreview() {
    ONDAStudentTheme {
        RouteListScreen()
    }
}
