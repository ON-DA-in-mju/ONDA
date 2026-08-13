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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)
private val SoftTeal = Color(0xFFE6FFFA)
private val Teal = Color(0xFF0D9488)
private val StarYellow = Color(0xFFFBBF24)
private val PageBg = Color(0xFFF7F9FC)

@Composable
fun StopGuideDetailScreen(
    stopId: String,
    modifier: Modifier = Modifier,
    stopsCatalog: List<StopGuideItem> = emptyStopGuideItems(),
    onBackClick: () -> Unit = {},
    onOpenMapClick: () -> Unit = {},
    onLiveClick: () -> Unit = {},
) {
    val stop = remember(stopId, stopsCatalog) {
        stopsCatalog.firstOrNull { it.id == stopId }
            ?: stopsCatalog.firstOrNull { it.name == stopId }
            ?: StopGuideItem(
                id = stopId,
                routeId = "city",
                name = stopId,
                address = "$stopId 인근",
                locationGuide = "$stopId 정류장에서 탑승할 수 있습니다.",
                landmarks = listOf(stopId),
                availableRoutes = emptyList(),
                thumbRes = R.drawable.route_thumb_city,
            )
    }
    var favorite by remember { mutableStateOf(false) }

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
                "정류장 안내",
                color = TitleBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(
                onClick = { favorite = !favorite },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "즐겨찾기",
                    tint = if (favorite) StarYellow else BodyGray,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftBlue),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.stop_detail_hero),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(365f / 140f),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    text = stop.name,
                    color = TitleBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .fillMaxWidth(0.45f),
                    textAlign = TextAlign.End,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).background(SoftTeal, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Place, null, tint = Teal, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("정류장 위치 안내", color = Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(stop.locationGuide, color = BodyGray, fontSize = 13.sp, lineHeight = 20.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoChipColumn(
                    title = "주변 랜드마크",
                    titleIcon = Icons.Filled.Apartment,
                    items = stop.landmarks.map { landmark ->
                        landmark to when {
                            landmark.contains("역") -> Icons.Filled.Train
                            landmark.contains("횡단") -> Icons.AutoMirrored.Filled.DirectionsWalk
                            else -> Icons.Filled.DirectionsBus
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                InfoChipColumn(
                    title = "이용 가능 노선",
                    titleIcon = Icons.Filled.DirectionsBus,
                    items = stop.availableRoutes.map { it to Icons.Filled.DirectionsBus },
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text("거리뷰 안내", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("정류장 주변 실제 모습", color = BodyGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    painter = painterResource(id = R.drawable.stop_street_view),
                    contentDescription = "거리뷰",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .aspectRatio(347f / 142f),
                    contentScale = ContentScale.Crop,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.stop_detail_map),
                    contentDescription = "지도",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(347f / 115f)
                        .clickable(onClick = onOpenMapClick),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    text = "지도에서 보기 >",
                    color = OndaBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White)
                        .border(1.dp, CardBorder, RoundedCornerShape(999.dp))
                        .clickable(onClick = onOpenMapClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            Text(
                text = "실시간 도착 정보 보기 >",
                color = OndaBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, OndaBlue, RoundedCornerShape(12.dp))
                    .clickable(onClick = onLiveClick)
                    .padding(vertical = 14.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoChipColumn(
    title: String,
    titleIcon: ImageVector,
    items: List<Pair<String, ImageVector>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(titleIcon, null, tint = OndaBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, color = OndaBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        items.forEach { (label, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = BodyGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = TitleBlack, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
