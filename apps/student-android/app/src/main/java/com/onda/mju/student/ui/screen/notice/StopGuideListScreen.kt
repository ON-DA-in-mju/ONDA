package com.onda.mju.student.ui.screen.notice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)

@Composable
fun StopGuideListScreen(
    routeId: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onStopClick: (String) -> Unit = {},
) {
    val routes = remember { sampleStopGuideRoutes() }
    var selectedRouteId by remember(routeId) { mutableStateOf(routeId) }
    var query by remember { mutableStateOf("") }
    val stops = remember(selectedRouteId, query) {
        sampleStopGuideItems()
            .filter { it.routeId == selectedRouteId }
            .filter {
                query.isBlank() ||
                    it.name.contains(query) ||
                    it.address.contains(query)
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "ON-DA",
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(96.dp)
                    .height(36.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(0.62f)) {
                    Text("정류장", color = TitleBlack, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("이용할 정류장을 확인하세요", color = BodyGray, fontSize = 13.sp)
                }
                Image(
                    painter = painterResource(id = R.drawable.stop_list_header),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(130.dp)
                        .aspectRatio(190f / 124f),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SearchBar(query = query, onQueryChange = { query = it }, placeholder = "정류장 검색")

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Figma order: 시내 셔틀, 기흥역, 명지대역
                listOf("city", "giheung", "myeongji").forEach { id ->
                    val route = routes.first { it.id == id }
                    val selected = selectedRouteId == id
                    Text(
                        text = route.title,
                        color = if (selected) Color.White else BodyGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) OndaBlue else Color.White)
                            .border(1.dp, if (selected) OndaBlue else CardBorder, RoundedCornerShape(999.dp))
                            .clickable { selectedRouteId = id }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                stops.forEach { stop ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onStopClick(stop.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = stop.thumbRes),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stop.name, color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Place, null, tint = BodyGray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stop.address, color = BodyGray, fontSize = 12.sp)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BodyGray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
