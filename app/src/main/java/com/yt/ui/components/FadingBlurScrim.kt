package com.yt.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val SCRIM_BLUR_RADIUS = 24.dp
// Mostly blur, barely any tint: the floating bar sitting on top of this is tinted more heavily,
// and that difference is what keeps it reading as a separate pill rather than one solid band.
private const val SCRIM_TINT_ALPHA = 0.22f

/**
 * A band of blurred backdrop along the top or bottom edge of the window.
 *
 * The blur is strongest at the edge and fades to nothing where the band meets the content, so the
 * chrome floating on top of it has no hard boundary to give itself away. Draw it between the
 * content it blurs and the bar it sits behind.
 *
 * @param atTop true for the status-bar edge, false for the navigation-bar edge.
 */
@Composable
fun FadingBlurScrim(
    hazeState: HazeState,
    height: Dp,
    atTop: Boolean,
    modifier: Modifier = Modifier,
) {
    if (height <= 0.dp) return

    val backdrop = MaterialTheme.colorScheme.background
    val style =
        remember(backdrop) {
            HazeStyle(
                backgroundColor = backdrop,
                tint = HazeTint(backdrop.copy(alpha = SCRIM_TINT_ALPHA)),
                blurRadius = SCRIM_BLUR_RADIUS,
            )
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .hazeEffect(hazeState, style) {
                    progressive =
                        HazeProgressive.verticalGradient(
                            startIntensity = if (atTop) 1f else 0f,
                            endIntensity = if (atTop) 0f else 1f,
                        )
                },
    )
}
