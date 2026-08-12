package com.onda.mju.student.ui.screen.my

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)
private val StarYellow = Color(0xFFFBBF24)

private data class ManageItem(val id: String, val title: String, var favorite: Boolean)

@Composable
fun FavoriteManageScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    var tab by remember { mutableStateOf(0) }
    var routes by remember {
        mutableStateOf(
            listOf(
                ManageItem("giheung", "기흥역 통학버스", true),
                ManageItem("myeongji", "명지대역 셔틀", true),
            ),
        )
    }
    var stops by remember {
        mutableStateOf(
            listOf(
                ManageItem("stop1", "기흥역 5번 출구", true),
                ManageItem("stop2", "버스관리사무소", true),
            ),
        )
    }
    val items = if (tab == 0) routes else stops

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
            Text(
                "즐겨찾기 관리",
                color = TitleBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                "편집",
                color = OndaBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .clickable { },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
        ) {
            listOf("노선 관리", "정류장 관리").forEachIndexed { index, label ->
                val selected = tab == index
                Text(
                    label,
                    color = if (selected) Color.White else BodyGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) OndaBlue else Color.Transparent)
                        .clickable { tab = index }
                        .padding(vertical = 10.dp),
                )
            }
        }

        Text(
            "목록을 길게 누르고 드래그하여 순서를 변경하세요.",
            color = BodyGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Menu, null, tint = BodyGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.size(40.dp).background(SoftBlue, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.DirectionsBus, null, tint = OndaBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(item.title, color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (item.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (item.favorite) StarYellow else BodyGray,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                if (tab == 0) {
                                    routes = routes.map {
                                        if (it.id == item.id) it.copy(favorite = !it.favorite) else it
                                    }
                                } else {
                                    stops = stops.map {
                                        if (it.id == item.id) it.copy(favorite = !it.favorite) else it
                                    }
                                }
                            },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    if (tab == 0) routes = routes.map { it.copy(favorite = false) }
                    else stops = stops.map { it.copy(favorite = false) }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OndaBlue),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = OndaBlue),
            ) {
                Text("전체 해제", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onSaveClick,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OndaBlue, contentColor = Color.White),
            ) {
                Text("저장", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
