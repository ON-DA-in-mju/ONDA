package com.mju.onda.driver.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OndaIllustration(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = contentDescription,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        contentScale = ContentScale.FillWidth,
    )
}
