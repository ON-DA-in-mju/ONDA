package com.mju.onda.driver.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mju.onda.driver.R

@Composable
fun OndaLogo(
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
) {
    Image(
        painter = painterResource(id = R.drawable.onda_logo),
        contentDescription = "ON-DA",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit,
    )
}
