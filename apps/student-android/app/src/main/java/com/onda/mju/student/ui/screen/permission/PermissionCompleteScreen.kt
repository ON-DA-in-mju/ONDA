package com.onda.mju.student.ui.screen.permission

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R
import com.onda.mju.student.data.permission.PermissionGrantState
import com.onda.mju.student.ui.theme.ONDAStudentTheme

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val IconCircleBg = Color(0xFFE8F1FE)
private val DeniedGray = Color(0xFF9CA3AF)

/** Fractions from STU-00-02B phone content (414 x 874). */
private const val SideInsetFraction = 24f / 414f
private const val LogoTopFraction = 10f / 874f
private const val LogoWidthFraction = 148f / 414f
private const val LogoHeightFraction = 52f / 874f
private const val BadgeTopGapFraction = 18f / 874f
private const val BadgeHeightFraction = 128f / 874f
private const val TitleTopGapFraction = 16f / 874f
private const val BodyTopGapFraction = 8f / 874f
private const val CardTopGapFraction = 20f / 874f
private const val IllustrationBottomGapFraction = 14f / 874f
private const val ButtonBottomPadFraction = 14f / 874f
/** Intrinsic ratio of permission_complete_illustration.png */
private const val IllustrationAspectRatio = 402f / 166f

@Composable
fun PermissionCompleteScreen(
    grantState: PermissionGrantState,
    modifier: Modifier = Modifier,
    onGoHomeClick: () -> Unit = {},
) {
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

        val sideInset = fracW(SideInsetFraction).coerceIn(18.dp, 26.dp)
        val logoWidth = fracW(LogoWidthFraction).coerceIn(120.dp, 168.dp)
        val logoHeight = fracH(LogoHeightFraction).coerceIn(42.dp, 58.dp)
        val badgeHeight = fracH(BadgeHeightFraction).coerceIn(100.dp, 140.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = sideInset),
        ) {
            Spacer(modifier = Modifier.height(fracH(LogoTopFraction).coerceIn(6.dp, 14.dp)))

            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "ON-DA",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(logoWidth)
                    .height(logoHeight),
                contentScale = ContentScale.Fit,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(fracH(BadgeTopGapFraction).coerceIn(12.dp, 22.dp)))

                Image(
                    painter = painterResource(id = R.drawable.permission_success_badge),
                    contentDescription = "권한 설정 완료",
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(badgeHeight),
                    contentScale = ContentScale.Fit,
                )

                Spacer(modifier = Modifier.height(fracH(TitleTopGapFraction).coerceIn(12.dp, 20.dp)))

                Text(
                    text = "권한 설정이 완료되었어요!",
                    color = TitleBlack,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(fracH(BodyTopGapFraction).coerceIn(6.dp, 12.dp)))

                Text(
                    text = "이제 ON-DA의 주요 기능을\n더 편하게 이용할 수 있어요.",
                    color = BodyGray,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(fracH(CardTopGapFraction).coerceIn(16.dp, 24.dp)))

                PermissionStatusCard(grantState = grantState)

                // Flexible space keeps the illustration above the home button without compressing it.
                Spacer(modifier = Modifier.weight(1f))
            }

            // Outside the weighted column so aspect-ratio height is never clipped.
            Image(
                painter = painterResource(id = R.drawable.permission_complete_illustration),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(IllustrationAspectRatio),
                contentScale = ContentScale.Fit,
            )

            Spacer(
                modifier = Modifier.height(
                    fracH(IllustrationBottomGapFraction).coerceIn(10.dp, 18.dp),
                ),
            )

            Button(
                onClick = onGoHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OndaBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "홈으로 이동",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(
                modifier = Modifier.height(
                    fracH(ButtonBottomPadFraction).coerceIn(10.dp, 18.dp),
                ),
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(grantState: PermissionGrantState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            PermissionStatusRow(
                icon = Icons.Filled.LocationOn,
                label = "위치 권한",
                granted = grantState.locationGranted,
            )
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
            PermissionStatusRow(
                icon = Icons.Filled.Notifications,
                label = "알림 권한",
                granted = grantState.notificationGranted,
            )
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
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

        Text(
            text = label,
            color = TitleBlack,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = if (granted) "허용됨" else "허용 안 함",
            color = if (granted) OndaBlue else DeniedGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_7")
@Composable
private fun PermissionCompleteScreenPreview() {
    ONDAStudentTheme {
        PermissionCompleteScreen(
            grantState = PermissionGrantState(
                locationGranted = true,
                notificationGranted = true,
            ),
        )
    }
}
