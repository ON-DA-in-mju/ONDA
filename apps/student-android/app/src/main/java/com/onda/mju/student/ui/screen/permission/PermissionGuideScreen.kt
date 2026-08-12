package com.onda.mju.student.ui.screen.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.onda.mju.student.R
import com.onda.mju.student.data.permission.PermissionGrantState
import com.onda.mju.student.ui.theme.ONDAStudentTheme

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF4B5568)
private val BadgeTeal = Color(0xFF14B8A6)
private val IconCircleBg = Color(0xFFE8F1FE)
private val CardBorder = Color(0xFFE8EDF2)
private val FooterGray = Color(0xFF6B7280)

/** Fractions from STU-00-02A phone content (414 x 874). */
private const val SideInsetFraction = 22f / 414f
/** Intrinsic ratio of permission_guide_hero.png */
private const val HeroAspect = 401f / 233f
/** Extra air between hero and the title/cards block. */
private const val TitleTopGapFraction = 32f / 874f
private const val BodyTopGapFraction = 10f / 874f
private const val CardsTopGapFraction = 18f / 874f
private const val CardGapFraction = 10f / 874f
private const val ButtonGapFraction = 10f / 874f
private const val FooterTopGapFraction = 12f / 874f
private const val BottomPadFraction = 12f / 874f

@Composable
fun PermissionGuideScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSkipClick: () -> Unit = {},
    onPermissionsConfigured: (PermissionGrantState) -> Unit = {},
) {
    val context = LocalContext.current
    var isRequesting by remember { mutableStateOf(false) }

    fun currentGrantState(): PermissionGrantState {
        val locationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return PermissionGrantState(
            locationGranted = locationGranted,
            notificationGranted = notificationGranted,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        isRequesting = false
        onPermissionsConfigured(currentGrantState())
    }

    fun requestPermissions() {
        if (isRequesting) return
        isRequesting = true

        val needed = buildList {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (needed.isEmpty() || context !is Activity) {
            isRequesting = false
            onPermissionsConfigured(currentGrantState())
            return
        }

        permissionLauncher.launch(needed.toTypedArray())
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val density = LocalDensity.current
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        fun fracH(fraction: Float): Dp =
            with(density) { (screenHeight.toPx() * fraction).toDp() }

        fun fracW(fraction: Float): Dp =
            with(density) { (screenWidth.toPx() * fraction).toDp() }

        val sideInset = fracW(SideInsetFraction).coerceIn(18.dp, 24.dp)
        val buttonHeight = 52.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 2.dp),
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = "권한 안내",
                    color = TitleBlack,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = sideInset),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.permission_guide_hero),
                    contentDescription = "ON-DA 권한 안내",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(HeroAspect),
                    contentScale = ContentScale.Fit,
                )

                Spacer(
                    modifier = Modifier.height(
                        fracH(TitleTopGapFraction).coerceIn(24.dp, 40.dp),
                    ),
                )

                Text(
                    text = "앱 사용을 위해\n필요한 권한을 안내해요",
                    color = TitleBlack,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(fracH(BodyTopGapFraction).coerceIn(8.dp, 14.dp)))

                Text(
                    text = "권한은 선택 사항이며, 허용하지 않아도\n기본 서비스는 이용할 수 있어요.\n언제든지 설정에서 변경할 수 있어요.",
                    color = BodyGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(fracH(CardsTopGapFraction).coerceIn(14.dp, 22.dp)))

                PermissionInfoCard(
                    icon = Icons.Filled.LocationOn,
                    title = "위치 권한",
                    description = "정류장 지도에서 내 위치를 확인하고,\n가까운 정류장을 더 편하게 찾을 수 있어요.",
                )

                Spacer(modifier = Modifier.height(fracH(CardGapFraction).coerceIn(8.dp, 12.dp)))

                PermissionInfoCard(
                    icon = Icons.Filled.Notifications,
                    title = "알림 권한",
                    description = "하차 알림과 운행 변경,\n긴급 공지 알림을 받을 수 있어요.",
                )

                Spacer(modifier = Modifier.height(fracH(16f / 874f).coerceIn(12.dp, 20.dp)))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sideInset)
                    .padding(bottom = fracH(BottomPadFraction).coerceIn(8.dp, 16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = { requestPermissions() },
                    enabled = !isRequesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OndaBlue,
                        contentColor = Color.White,
                        disabledContainerColor = OndaBlue.copy(alpha = 0.7f),
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "권한 설정하고 시작하기",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(fracH(ButtonGapFraction).coerceIn(8.dp, 12.dp)))

                OutlinedButton(
                    onClick = onSkipClick,
                    enabled = !isRequesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OndaBlue),
                    border = BorderStroke(1.5.dp, OndaBlue),
                ) {
                    Text(
                        text = "나중에 설정하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(fracH(FooterTopGapFraction).coerceIn(10.dp, 14.dp)))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = FooterGray,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "권한은 언제든지 설정에서 변경할 수 있어요.",
                        color = FooterGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionInfoCard(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(IconCircleBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OndaBlue,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = TitleBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "선택 권한",
                    color = BadgeTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = BodyGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_7")
@Composable
private fun PermissionGuideScreenPreview() {
    ONDAStudentTheme {
        PermissionGuideScreen()
    }
}
