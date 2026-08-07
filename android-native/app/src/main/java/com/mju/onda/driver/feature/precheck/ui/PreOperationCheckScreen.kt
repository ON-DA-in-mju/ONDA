package com.mju.onda.driver.feature.precheck.ui



import android.content.Intent

import android.net.Uri

import android.provider.Settings

import android.widget.Toast

import androidx.compose.foundation.Image

import androidx.compose.foundation.background

import androidx.compose.foundation.border

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

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.outlined.CheckCircle

import androidx.compose.material.icons.outlined.ErrorOutline

import androidx.compose.material.icons.outlined.Info

import androidx.compose.material.icons.outlined.WarningAmber

import androidx.compose.material3.Icon

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.DisposableEffect

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.lifecycle.Lifecycle

import androidx.lifecycle.LifecycleEventObserver

import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.lifecycle.viewmodel.compose.viewModel

import com.mju.onda.driver.R

import com.mju.onda.driver.core.theme.OndaColors

import com.mju.onda.driver.core.theme.OndaTypography

import com.mju.onda.driver.core.ui.components.OndaOutlinedButton

import com.mju.onda.driver.core.ui.components.OndaPrimaryButton

import com.mju.onda.driver.core.ui.components.OndaTopBar

import com.mju.onda.driver.feature.precheck.data.CheckStatus

import com.mju.onda.driver.feature.precheck.data.MockPreOperationCheck

import com.mju.onda.driver.feature.precheck.data.PreCheckItem

import com.mju.onda.driver.feature.precheck.viewmodel.PreOperationCheckEvent

import com.mju.onda.driver.feature.precheck.viewmodel.PreOperationCheckViewModel



private val CautionBg = Color(0xFFFFF4E5)

private val CautionFg = Color(0xFFE67E22)

private val ActionBg = Color(0xFFFFF0E8)

private val ActionFg = Color(0xFFF07A3A)



@Composable

fun PreOperationCheckScreen(

    onBack: () -> Unit,

    onOpenComplete: () -> Unit = {},

    viewModel: PreOperationCheckViewModel = viewModel(),

) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current



    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                viewModel.refreshFromSystem(context)

            }

        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }

    }



    LaunchedEffect(Unit) {

        viewModel.events.collect { event ->

            when (event) {

                PreOperationCheckEvent.NavigateBack -> onBack()

                PreOperationCheckEvent.NavigateToComplete -> onOpenComplete()

                PreOperationCheckEvent.OpenAppSettings -> {

                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {

                        data = Uri.fromParts("package", context.packageName, null)

                    }

                    context.startActivity(intent)

                }

                PreOperationCheckEvent.OpenLocationSettings -> {

                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

                }

                PreOperationCheckEvent.ShowStillHasIssues -> {

                    Toast.makeText(

                        context,

                        MockPreOperationCheck.RECHECK_STILL_ISSUES_TOAST,

                        Toast.LENGTH_SHORT,

                    ).show()

                }

            }

        }

    }



    Scaffold(

        containerColor = Color(0xFFF7F9FC),

        topBar = {

            OndaTopBar(

                title = MockPreOperationCheck.SCREEN_TITLE,

                onBack = viewModel::onBack,

            )

        },

    ) { innerPadding ->

        Column(

            modifier = Modifier

                .fillMaxSize()

                .padding(innerPadding),

            horizontalAlignment = Alignment.CenterHorizontally,

        ) {

            Column(

                modifier = Modifier

                    .widthIn(max = 430.dp)

                    .fillMaxSize()

                    .verticalScroll(rememberScrollState())

                    .padding(horizontal = 20.dp)

                    .padding(bottom = 24.dp),

                horizontalAlignment = Alignment.CenterHorizontally,

            ) {

                Image(

                    painter = painterResource(id = R.drawable.precheck_illustration),

                    contentDescription = null,

                    modifier = Modifier

                        .fillMaxWidth()

                        .aspectRatio(251f / 113f),

                    contentScale = ContentScale.FillWidth,

                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(

                    text = MockPreOperationCheck.HEADLINE,

                    style = OndaTypography.headlineLarge.copy(

                        fontSize = 22.sp,

                        fontWeight = FontWeight.ExtraBold,

                    ),

                    textAlign = TextAlign.Center,

                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(

                    text = MockPreOperationCheck.SUBHEADLINE,

                    style = OndaTypography.headlineLarge.copy(

                        fontSize = 20.sp,

                        fontWeight = FontWeight.ExtraBold,

                    ),

                    textAlign = TextAlign.Center,

                )

                Spacer(modifier = Modifier.height(16.dp))



                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .background(OndaColors.Surface, RoundedCornerShape(16.dp))

                        .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))

                        .padding(horizontal = 12.dp, vertical = 6.dp),

                ) {

                    uiState.items.forEachIndexed { index, item ->

                        CheckRow(item = item)

                        if (index != uiState.items.lastIndex) {

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .height(1.dp)

                                    .background(OndaColors.Border.copy(alpha = 0.65f)),

                            )

                        }

                    }

                }



                Spacer(modifier = Modifier.height(12.dp))

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .background(OndaColors.PrimarySoft, RoundedCornerShape(12.dp))

                        .padding(horizontal = 14.dp, vertical = 12.dp),

                    verticalAlignment = Alignment.CenterVertically,

                ) {

                    Icon(

                        imageVector = Icons.Outlined.Info,

                        contentDescription = null,

                        tint = OndaColors.Primary,

                        modifier = Modifier.size(16.dp),

                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(

                        text = MockPreOperationCheck.INFO_NOTICE,

                        style = OndaTypography.bodySmall.copy(

                            fontSize = 12.5.sp,

                            color = OndaColors.Primary,

                            lineHeight = 18.sp,

                        ),

                    )

                }



                Spacer(modifier = Modifier.height(18.dp))

                OndaPrimaryButton(

                    label = MockPreOperationCheck.RECHECK_LABEL,

                    onClick = { viewModel.onRecheck(context) },

                    isLoading = uiState.isRechecking,

                    height = 48.dp,

                )

                Spacer(modifier = Modifier.height(8.dp))

                OndaOutlinedButton(

                    label = MockPreOperationCheck.OPEN_SETTINGS_LABEL,

                    onClick = viewModel::onOpenSettings,

                    enabled = !uiState.isRechecking,

                    height = 48.dp,

                )

                Spacer(modifier = Modifier.height(8.dp))

                OndaPrimaryButton(

                    label = MockPreOperationCheck.START_LABEL,

                    onClick = viewModel::onStartOperation,

                    enabled = uiState.canStart && !uiState.isRechecking,

                    height = 48.dp,

                )

            }

        }

    }

}



@Composable

private fun CheckRow(item: PreCheckItem) {

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 11.dp),

        verticalAlignment = Alignment.CenterVertically,

    ) {

        Icon(

            imageVector = item.icon,

            contentDescription = null,

            tint = OndaColors.Primary,

            modifier = Modifier.size(20.dp),

        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(

            text = "${item.label} - ${item.detail}",

            style = OndaTypography.bodySmall.copy(

                fontSize = 13.sp,

                color = OndaColors.TextPrimary,

            ),

            modifier = Modifier.weight(1f),

        )

        Spacer(modifier = Modifier.width(8.dp))

        StatusBadge(status = item.status)

    }

}



@Composable

private fun StatusBadge(status: CheckStatus) {

    val (bg, fg, icon, label) = when (status) {

        CheckStatus.Normal -> Quad(

            OndaColors.SuccessSoft,

            OndaColors.SuccessText,

            Icons.Outlined.CheckCircle,

            MockPreOperationCheck.STATUS_NORMAL,

        )

        CheckStatus.Caution -> Quad(

            CautionBg,

            CautionFg,

            Icons.Outlined.WarningAmber,

            MockPreOperationCheck.STATUS_CAUTION,

        )

        CheckStatus.ActionRequired -> Quad(

            ActionBg,

            ActionFg,

            Icons.Outlined.ErrorOutline,

            MockPreOperationCheck.STATUS_ACTION,

        )

    }



    Row(

        modifier = Modifier

            .background(bg, RoundedCornerShape(999.dp))

            .padding(horizontal = 8.dp, vertical = 4.dp),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(3.dp),

    ) {

        Icon(

            imageVector = icon,

            contentDescription = null,

            tint = fg,

            modifier = Modifier.size(13.dp),

        )

        Text(

            text = label,

            style = OndaTypography.labelSmall.copy(

                fontSize = 11.sp,

                fontWeight = FontWeight.Bold,

                color = fg,

            ),

        )

    }

}



private data class Quad<A, B, C, D>(

    val first: A,

    val second: B,

    val third: C,

    val fourth: D,

)


