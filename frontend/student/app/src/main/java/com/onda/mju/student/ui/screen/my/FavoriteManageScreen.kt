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

private data class ManageItem(val id: String, val title: String, val favorite: Boolean)

@Composable
fun FavoriteManageScreen(
    modifier: Modifier = Modifier,
    routeItems: List<Pair<String, String>> = emptyList(),
    favoriteRouteIds: Set<String> = emptySet(),
    onBackClick: () -> Unit = {},
    onSaveClick: (routeIds: Set<String>) -> Unit = {},
) {
    var routes by remember(routeItems, favoriteRouteIds) {
        mutableStateOf(
            routeItems.map { (id, name) ->
                ManageItem(id, name, favorite = id in favoriteRouteIds)
            },
        )
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
            Text(
                "즐겨찾기",
                color = TitleBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Text(
            text = "별을 눌러 즐겨찾기 노선을 선택하세요",
            color = BodyGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (routes.isEmpty()) {
                Text(
                    text = "표시할 노선이 없습니다.",
                    color = BodyGray,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                routes.forEach { item ->
                    ManageRow(
                        item = item,
                        onToggle = {
                            routes = routes.map {
                                if (it.id == item.id) it.copy(favorite = !it.favorite) else it
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
                onClick = { routes = routes.map { it.copy(favorite = false) } },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OndaBlue),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = OndaBlue),
            ) {
                Text("전체 해제", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    onSaveClick(routes.filter { it.favorite }.map { it.id }.toSet())
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OndaBlue, contentColor = Color.White),
            ) {
                Text("저장", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ManageRow(
    item: ManageItem,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SoftBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.DirectionsBus,
                null,
                tint = OndaBlue,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            item.title,
            color = TitleBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (item.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (item.favorite) "즐겨찾기 해제" else "즐겨찾기 등록",
            tint = if (item.favorite) StarYellow else BodyGray,
            modifier = Modifier.size(24.dp),
        )
    }
}
