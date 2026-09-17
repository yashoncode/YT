package com.yt.ui.screens.settings

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.yt.R
import kotlin.math.sin
import kotlin.random.Random

private const val HEARTS_PER_TAP = 7
private const val HEART_FLIGHT_MILLIS = 1500
private const val TWO_PI = 6.2831855f
private val HEART_RISE = 260.dp
private val HEART_SPREAD = 110.dp
private val HEART_DRIFT = 22.dp
private val HEART_SIZE = 30.dp
private val HeartPink = Color(0xFFFF8FB8)
private const val HEART_FILL_ALPHA = 0.55f

/**
 * The whole About section: the animation, who built the app, and the version. It sits inline in
 * the settings list because there is nothing else left to put on a page of its own.
 */
@Composable
internal fun AboutCreditCard() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val packageInfo =
        remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) {
                null
            }
        }
    val versionName = packageInfo?.versionName ?: stringResource(R.string.unknown)
    val versionCode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "0"
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString() ?: "0"
        }

    val hearts = remember { mutableStateListOf<FlyingHeart>() }

    SettingsGroup {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                AsyncImage(
                    // Built through a request rather than a bare resource id so the animated decoder
                    // registered on the shared loader is the one that plays it.
                    model =
                        ImageRequest
                            .Builder(context)
                            .data(R.raw.built_by_yashwanth)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                repeat(HEARTS_PER_TAP) { hearts += FlyingHeart.random() }
                            },
                )

                // Drawn over the artwork and deliberately not clipped to it, so the hearts carry on
                // rising past the top of the card.
                hearts.forEach { heart ->
                    key(heart.id) {
                        FlyingHeartIcon(heart = heart, onFinished = { hearts.remove(heart) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.about_built_by),
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 38.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.v_version_template, versionName, versionCode),
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Cursive,
                        letterSpacing = 1.5.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One heart's flight, drawn at spawn time so no two hearts in a tap travel the same path. */
private data class FlyingHeart(
    val id: Long,
    val startFraction: Float,
    val driftPhase: Float,
    val scale: Float,
    val delayMillis: Int,
) {
    companion object {
        private var nextId = 0L

        fun random(): FlyingHeart =
            FlyingHeart(
                id = nextId++,
                startFraction = Random.nextFloat() * 2f - 1f,
                driftPhase = Random.nextFloat() * TWO_PI,
                scale = 0.7f + Random.nextFloat() * 0.55f,
                delayMillis = Random.nextInt(0, 260),
            )
    }
}

@Composable
private fun BoxScope.FlyingHeartIcon(
    heart: FlyingHeart,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val risePx = with(density) { HEART_RISE.toPx() }
    val spreadPx = with(density) { HEART_SPREAD.toPx() }
    val driftPx = with(density) { HEART_DRIFT.toPx() }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(heart.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = HEART_FLIGHT_MILLIS,
                    delayMillis = heart.delayMillis,
                    easing = LinearEasing,
                ),
        )
        onFinished()
    }

    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .size(HEART_SIZE)
                .zIndex(1f)
                .graphicsLayer {
                    val p = progress.value
                    // Starts low over the artwork and climbs out of the top of the card.
                    translationY = risePx * (0.65f - p)
                    translationX =
                        heart.startFraction * spreadPx + sin(heart.driftPhase + p * TWO_PI) * driftPx
                    // Pops in, holds, then fades over the last third of the climb.
                    alpha = (p * 8f).coerceAtMost(1f) * ((1f - p) * 3f).coerceAtMost(1f)
                    val pop = heart.scale * (0.5f + (p * 6f).coerceAtMost(1f) * 0.5f)
                    scaleX = pop
                    scaleY = pop
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = HeartPink.copy(alpha = HEART_FILL_ALPHA),
            modifier = Modifier.size(HEART_SIZE),
        )
        Text(
            // The heart glyph's visual centre sits above the centre of its bounding box.
            modifier = Modifier.offset(y = 1.dp),
            text = "K",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                ),
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}
