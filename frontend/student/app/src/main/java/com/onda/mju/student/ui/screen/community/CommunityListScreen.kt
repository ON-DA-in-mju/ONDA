package com.onda.mju.student.ui.screen.community

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.onda.mju.student.data.remote.repository.RouteRepository
import com.onda.mju.student.data.remote.repository.RouteStopsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val WarnBg = Color(0xFFFFF4E5)
private val WarnText = Color(0xFFEA580C)
private val SoftBlue = Color(0xFFEDF4FE)
private val ReadCardBg = Color(0xFFF3F4F6)
private val ReadGray = Color(0xFF9CA3AF)

private enum class CommunitySort(val label: String) {
    Newest("최신순"),
    Likes("공감순"),
    Comments("댓글순"),
}

@Composable
fun CommunityListScreen(
    modifier: Modifier = Modifier,
    reports: List<CommunityReport> = sampleCommunityReports(),
    posts: List<CommunityPost> = emptyList(),
    readIds: Set<String> = emptySet(),
    onReportClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    onDislikeClick: (String) -> Unit = {},
    onPostLikeClick: (String) -> Unit = {},
    onPostDislikeClick: (String) -> Unit = {},
) {
    var boardTab by remember { mutableStateOf(CommunityBoardTab.Reports) }
    var selectedFilter by remember { mutableStateOf(CommunityFilter.All) }
    var selectedRoute by remember { mutableStateOf<String?>(null) }
    var selectedStop by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf(CommunitySort.Newest) }

    var routeMenuOpen by remember { mutableStateOf(false) }
    var stopMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    var dbRouteNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var dbStopNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val routeRepository = remember { RouteRepository() }
    val routeStopsRepository = remember { RouteStopsRepository() }

    LaunchedEffect(Unit) {
        dbRouteNames = withContext(Dispatchers.IO) {
            runCatching {
                routeRepository.getActiveRoutes()
                    .map { it.routeName.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(selectedRoute) {
        selectedStop = null
        dbStopNames = withContext(Dispatchers.IO) {
            runCatching {
                if (selectedRoute.isNullOrBlank()) {
                    routeStopsRepository.getAllRouteStops()
                        .map { it.name.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                } else {
                    routeStopsRepository.getStopsForRouteName(selectedRoute!!)
                        .map { it.name.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                }
            }.getOrDefault(emptyList())
        }
    }

    val routeOptions = remember(dbRouteNames, reports) {
        (dbRouteNames + reports.map { it.routeLabel.trim() })
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }
    val stopOptions = remember(dbStopNames, reports, selectedRoute) {
        val fromReports = reports
            .asSequence()
            .filter { selectedRoute == null || it.routeLabel == selectedRoute }
            .map { it.stopName.trim() }
            .filter { it.isNotEmpty() }
        (dbStopNames + fromReports)
            .distinct()
            .sorted()
    }

    val filteredReports = remember(
        reports,
        selectedFilter,
        selectedRoute,
        selectedStop,
        selectedSort,
    ) {
        reports
            .filterBy(selectedFilter)
            .filter { selectedRoute == null || it.routeLabel == selectedRoute }
            .filter { selectedStop == null || it.stopName == selectedStop }
            .let { list ->
                when (selectedSort) {
                    CommunitySort.Newest -> list.sortedByDescending { it.registeredAt }
                    CommunitySort.Likes -> list.sortedWith(
                        compareByDescending<CommunityReport> { it.likeCount }
                            .thenByDescending { it.registeredAt },
                    )
                    CommunitySort.Comments -> list.sortedWith(
                        compareByDescending<CommunityReport> { it.commentCount }
                            .thenByDescending { it.registeredAt },
                    )
                }
            }
    }

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
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CommunityBoardTab.entries.forEach { tab ->
                    val selected = boardTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) OndaBlue else SoftBlue)
                            .clickable { boardTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.label,
                            color = if (selected) Color.White else OndaBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (boardTab) {
                CommunityBoardTab.Reports -> {
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChipButton(
                                label = selectedRoute ?: "전체 노선",
                                expanded = routeMenuOpen,
                                onClick = {
                                    routeMenuOpen = !routeMenuOpen
                                    if (routeMenuOpen) {
                                        stopMenuOpen = false
                                        sortMenuOpen = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChipButton(
                                label = selectedStop ?: "전체 정류장",
                                expanded = stopMenuOpen,
                                onClick = {
                                    stopMenuOpen = !stopMenuOpen
                                    if (stopMenuOpen) {
                                        routeMenuOpen = false
                                        sortMenuOpen = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChipButton(
                                label = selectedSort.label,
                                expanded = sortMenuOpen,
                                onClick = {
                                    sortMenuOpen = !sortMenuOpen
                                    if (sortMenuOpen) {
                                        routeMenuOpen = false
                                        stopMenuOpen = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (routeMenuOpen) {
                            FilterOptionsPanel {
                                FilterOptionRow(
                                    text = "전체 노선",
                                    selected = selectedRoute == null,
                                    onClick = {
                                        selectedRoute = null
                                        routeMenuOpen = false
                                    },
                                )
                                if (routeOptions.isEmpty()) {
                                    Text(
                                        "불러온 노선 없음",
                                        color = BodyGray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    )
                                } else {
                                    routeOptions.forEach { name ->
                                        FilterOptionRow(
                                            text = name,
                                            selected = name == selectedRoute,
                                            onClick = {
                                                selectedRoute = name
                                                routeMenuOpen = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        if (stopMenuOpen) {
                            FilterOptionsPanel {
                                FilterOptionRow(
                                    text = "전체 정류장",
                                    selected = selectedStop == null,
                                    onClick = {
                                        selectedStop = null
                                        stopMenuOpen = false
                                    },
                                )
                                if (stopOptions.isEmpty()) {
                                    Text(
                                        "불러온 정류장 없음",
                                        color = BodyGray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    )
                                } else {
                                    stopOptions.forEach { name ->
                                        FilterOptionRow(
                                            text = name,
                                            selected = name == selectedStop,
                                            onClick = {
                                                selectedStop = name
                                                stopMenuOpen = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        if (sortMenuOpen) {
                            FilterOptionsPanel {
                                CommunitySort.entries.forEach { sort ->
                                    FilterOptionRow(
                                        text = sort.label,
                                        selected = sort == selectedSort,
                                        onClick = {
                                            selectedSort = sort
                                            sortMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
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
                        if (filteredReports.isEmpty()) {
                            Text(
                                "조건에 맞는 제보가 없습니다.",
                                color = BodyGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        } else {
                            filteredReports.forEach { report ->
                                ReportCard(
                                    report = report,
                                    isRead = report.id in readIds,
                                    onClick = { onReportClick(report.id) },
                                    onLikeClick = { onLikeClick(report.id) },
                                    onDislikeClick = { onDislikeClick(report.id) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                CommunityBoardTab.Posts -> {
                    InfoBanner(
                        text = "학생 소통 공간입니다. 서로를 존중하는 글을 남겨 주세요.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                        if (posts.isEmpty()) {
                            Text(
                                "아직 작성된 글이 없습니다. 첫 글을 남겨 보세요.",
                                color = BodyGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        } else {
                            posts.forEach { post ->
                                PostCard(
                                    post = post,
                                    isRead = post.id in readIds,
                                    onClick = { onPostClick(post.id) },
                                    onLikeClick = { onPostLikeClick(post.id) },
                                    onDislikeClick = { onPostDislikeClick(post.id) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                when (boardTab) {
                    CommunityBoardTab.Reports -> onCreateReportClick()
                    CommunityBoardTab.Posts -> onCreatePostClick()
                }
            },
            containerColor = OndaBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp),
        ) {
            Icon(
                imageVector = when (boardTab) {
                    CommunityBoardTab.Reports -> Icons.Filled.Edit
                    CommunityBoardTab.Posts -> Icons.Filled.EditNote
                },
                contentDescription = when (boardTab) {
                    CommunityBoardTab.Reports -> "제보 작성"
                    CommunityBoardTab.Posts -> "글쓰기"
                },
            )
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .border(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) OndaBlue else CardBorder,
                shape = RoundedCornerShape(10.dp),
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = TitleBlack,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = if (expanded) OndaBlue else BodyGray,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FilterOptionsPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(max = 220.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        content()
    }
}

@Composable
private fun FilterOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = if (selected) OndaBlue else TitleBlack,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) SoftBlue else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    )
}

@Composable
internal fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    tintBg: Color = WarnBg,
    tint: Color = WarnText,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tintBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Info, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = tint, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
internal fun ReportCard(
    report: CommunityReport,
    onClick: () -> Unit,
    isRead: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    onDislikeClick: (() -> Unit)? = null,
) {
    val typeColor = if (isRead) ReadGray else report.type.color
    val titleColor = if (isRead) ReadGray else TitleBlack
    val metaColor = if (isRead) ReadGray else BodyGray
    val cardBg = if (isRead) ReadCardBg else Color.White
    val likeActive = report.myReaction == ReportReaction.Like
    val dislikeActive = report.myReaction == ReportReaction.Dislike
    val likeTint = when {
        likeActive -> OndaBlue
        else -> metaColor
    }
    val dislikeTint = when {
        dislikeActive -> Color(0xFFEF4444)
        else -> metaColor
    }

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
                "${report.timeLabel} · 댓글 ${report.commentCount}",
                color = metaColor,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            ReactionButton(
                icon = Icons.Filled.ThumbUp,
                count = report.likeCount,
                tint = likeTint,
                active = likeActive,
                enabled = onLikeClick != null,
                onClick = { onLikeClick?.invoke() },
            )
            Spacer(modifier = Modifier.width(6.dp))
            ReactionButton(
                icon = Icons.Filled.ThumbDown,
                count = report.dislikeCount,
                tint = dislikeTint,
                active = dislikeActive,
                enabled = onDislikeClick != null,
                onClick = { onDislikeClick?.invoke() },
            )
            Spacer(modifier = Modifier.width(8.dp))
            ViewCountLabel(count = report.viewCount, tint = metaColor)
        }
    }
}

@Composable
internal fun PostCard(
    post: CommunityPost,
    onClick: () -> Unit,
    showReactions: Boolean = true,
    isRead: Boolean = false,
    onLikeClick: () -> Unit = {},
    onDislikeClick: () -> Unit = {},
) {
    val likeActive = post.myReaction == ReportReaction.Like
    val dislikeActive = post.myReaction == ReportReaction.Dislike
    val titleColor = if (isRead) ReadGray else TitleBlack
    val metaColor = if (isRead) ReadGray else BodyGray
    val cardBg = if (isRead) ReadCardBg else Color.White
    val likeTint = if (likeActive) OndaBlue else metaColor
    val dislikeTint = if (dislikeActive) Color(0xFFEF4444) else metaColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            text = "소통",
            color = if (isRead) ReadGray else OndaBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(if (isRead) ReadCardBg else SoftBlue, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(post.title, color = titleColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = post.body,
            color = metaColor,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${post.timeLabel} · 익명 · 댓글 ${post.commentCount}",
                color = metaColor,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            if (showReactions) {
                ReactionButton(
                    icon = Icons.Filled.ThumbUp,
                    count = post.likeCount,
                    tint = likeTint,
                    active = likeActive,
                    enabled = true,
                    onClick = onLikeClick,
                )
                Spacer(modifier = Modifier.width(6.dp))
                ReactionButton(
                    icon = Icons.Filled.ThumbDown,
                    count = post.dislikeCount,
                    tint = dislikeTint,
                    active = dislikeActive,
                    enabled = true,
                    onClick = onDislikeClick,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            ViewCountLabel(count = post.viewCount, tint = metaColor)
        }
    }
}

@Composable
private fun ViewCountLabel(count: Int, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.RemoveRedEye,
            contentDescription = "조회수",
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count",
            color = tint,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ReactionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    tint: Color,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count",
            color = tint,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
