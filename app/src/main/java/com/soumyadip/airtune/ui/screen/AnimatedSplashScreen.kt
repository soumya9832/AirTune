package com.soumyadip.airtune.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import kotlinx.coroutines.delay
import com.soumyadip.airtune.R
import com.soumyadip.airtune.ui.theme.AirTuneTheme
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun AnimatedSplashScreen() {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "SplashAlphaAnimation"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1500),
        label = "SplashScaleAnimation"
    )

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_loading_animation)) // Replace with your Lottie animation resource

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Set background to black
            .alpha(alphaAnim),
    ) {
        val (lottie, appName) = createRefs()

        LottieAnimation(
            composition = composition,
            iterations = 1, // Play once
            modifier = Modifier
                .size(150.dp)
                .scale(scaleAnim)
                .constrainAs(lottie) {
                    centerTo(parent)
                }
        )

        Text(
            text = "AirTune",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .alpha(alphaAnim)
                .constrainAs(appName) {
                    bottom.linkTo(parent.bottom, margin = 32.dp)
                    centerHorizontallyTo(parent)
                }
        )
    }
}

@Preview(showBackground = true, name = "Splash Screen Preview")
@Composable
fun SplashScreenPreview() {
    AirTuneTheme {
        AnimatedSplashScreen()
    }
}