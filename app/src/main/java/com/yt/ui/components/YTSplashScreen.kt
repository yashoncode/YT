package com.yt.ui.components

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yt.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SPLASH_ICON_NAMESPACE = "com.yt"

/** The lit line itself. */
private val CORE_HEIGHT = 3.dp

/** How far the halo reaches above and below the line. */
private val GLOW_HEIGHT = 26.dp

/** Diameter of the white cap that rides the leading edge. */
private val TIP_SIZE = 30.dp

private data class SplashIconOption(
    val componentSuffix: String,
    val drawableRes: Int,
    /** When true, preview bg uses MaterialTheme dynamic colors (Material You variant). */
    val isDynamic: Boolean = false,
)

private val SPLASH_ICONS =
    listOf(
        SplashIconOption(".IconYTRed", R.drawable.ic_yt_logo),
        SplashIconOption(".IconYTLight", R.drawable.ic_yt_logo),
        SplashIconOption(".IconYTPlay", R.drawable.ic_fg_yt_play),
        SplashIconOption(".IconAmoled", R.drawable.splash_icon_amoled),
        SplashIconOption(".IconMonochrome", R.drawable.splash_icon_monochrome),
        SplashIconOption(".IconGhost", R.drawable.splash_icon_ghost),
        SplashIconOption(".IconDynamic", R.drawable.ic_launcher_dynamic_foreground, isDynamic = true),
        SplashIconOption(".IconMaterialSky", R.drawable.ic_yt_logo),
        SplashIconOption(".IconMaterialMint", R.drawable.ic_yt_logo),
    )

@Composable
fun YTSplashScreen(onAnimationFinished: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val loadingTrackColor =
        colorScheme.onBackground.copy(
            alpha = if (colorScheme.background.luminance() < 0.5f) 0.22f else 0.12f,
        )

    // Dim tail to a white-hot head. Spread across the lit part of the bar only, so the white cap
    // rides the leading edge as the bar grows instead of sitting at a fixed spot.
    val loadingGradient =
        listOf(
            colorScheme.primary.copy(alpha = 0f),
            colorScheme.primary,
            colorScheme.tertiary,
            Color.White,
        )

    // Detect the currently active app icon
    val activeIcon =
        remember {
            val pm = context.packageManager
            val pkg = context.packageName
            SPLASH_ICONS.firstOrNull { option ->
                val cn = ComponentName(pkg, "$SPLASH_ICON_NAMESPACE${option.componentSuffix}")
                pm.getComponentEnabledSetting(cn) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } ?: SPLASH_ICONS.first()
        }
    // --- Animation States ---
    val scale = remember { Animatable(0f) } // For the Logo Pop
    val lineProgress = remember { Animatable(0f) } // For the Red Line
    val alpha = remember { Animatable(1f) } // For the Screen Fade Out

    // --- The Choreography ---
    LaunchedEffect(key1 = true) {
        // 1. Logo Springs In (0ms -> 600ms)
        scale.animateTo(
            targetValue = 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
        )

        // 2. The Line Grows (Wait 200ms, then grow)
        launch {
            delay(200)
            lineProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        }

        // 3. Wait for app to be ready, then Fade Out
        delay(1500) // Adjust this based on your actual data loading time
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500),
        )

        // 4. Tell MainActivity to remove the Splash
        onAnimationFinished()
    }

    // --- The UI ---
    // Only render if we are visible
    if (alpha.value > 0f) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colorScheme.background)
                    .alpha(alpha.value),
            // Controls the fade out
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 1. The Logo — rendered differently for static vs dynamic icons
                if (activeIcon.isDynamic) {
                    // Material You: use the same padded foreground as Android themed icons.
                    Box(
                        modifier =
                            Modifier
                                .scale(scale.value)
                                .size(90.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = activeIcon.drawableRes),
                            contentDescription = stringResource(R.string.ui_yt_logo),
                            colorFilter = ColorFilter.tint(colorScheme.onSecondaryContainer),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = activeIcon.drawableRes),
                        contentDescription = stringResource(R.string.ui_yt_logo),
                        modifier =
                            Modifier
                                .scale(scale.value)
                                .size(90.dp),
                    )
                }
            }

            // 3. The "Flow" Loading Line
            // Positioned slightly below center
            //
            // Three layers, none of which is a blur: Modifier.blur needs a RenderEffect and is a
            // no-op below API 31, and this screen is the first thing every device draws. A vertical
            // alpha falloff gives the same contour bloom with a gradient.
            val bloom by
                rememberInfiniteTransition(label = "splashGlow").animateFloat(
                    initialValue = 0.24f,
                    targetValue = 0.52f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "splashGlowAlpha",
                )

            val barWidth = 180.dp
            val barWidthPx = with(LocalDensity.current) { barWidth.toPx() }
            val tipRadiusPx = with(LocalDensity.current) { (TIP_SIZE / 2).toPx() }

            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(top = 180.dp)
                        .width(barWidth)
                        .height(GLOW_HEIGHT),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(CORE_HEIGHT)
                            .clip(CircleShape)
                            .background(loadingTrackColor),
                )

                // The halo. Same colour as the core, faded out top and bottom, so the bar looks lit
                // rather than painted.
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(lineProgress.value)
                            .fillMaxHeight()
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                colorScheme.primary.copy(alpha = bloom),
                                                Color.Transparent,
                                            ),
                                    ),
                            ),
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(lineProgress.value)
                            .height(CORE_HEIGHT)
                            .clip(CircleShape)
                            .background(brush = Brush.horizontalGradient(colors = loadingGradient)),
                )

                // The hot tip, parked on the leading edge.
                Box(
                    modifier =
                        Modifier
                            .size(TIP_SIZE)
                            .offset {
                                IntOffset(
                                    (barWidthPx * lineProgress.value - tipRadiusPx).roundToInt(),
                                    0,
                                )
                            }.background(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                Color.White.copy(alpha = 0.85f * lineProgress.value),
                                                Color.Transparent,
                                            ),
                                    ),
                                shape = CircleShape,
                            ),
                )
            }
        }
    }
}
