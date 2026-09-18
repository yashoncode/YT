package com.yt.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default spread of a glow. Wide enough to read as light, short of a visible second edge. */
val GlowDefaultRadius: Dp = 22.dp

/** How much of the source colour survives into the halo. */
const val GLOW_DEFAULT_ALPHA = 0.55f

/**
 * A coloured halo cast by a surface onto whatever is behind it.
 *
 * Glass chrome has no fill of its own to carry colour, so a blurred bar reads as a grey smudge
 * against a colourful backdrop. Giving it a halo in the colour of what it is showing — the artwork
 * on a music bar, the accent of the selected tab on the navigation bar — puts that colour back
 * around the element instead of inside it.
 *
 * Drawn as a tinted elevation shadow rather than a blurred copy of the content: the platform
 * already renders that offscreen, so the halo costs no extra layer. Shadow tinting needs API 28;
 * below it the halo falls back to the platform's plain grey shadow.
 *
 * Apply before `clip`, so the halo lands outside the shape rather than being cut off by it.
 */
fun Modifier.glow(
    color: Color,
    shape: Shape,
    radius: Dp = GlowDefaultRadius,
    alpha: Float = GLOW_DEFAULT_ALPHA,
): Modifier {
    if (color == Color.Unspecified || alpha <= 0f || radius <= 0.dp) return this
    val tint = color.copy(alpha = alpha)
    return shadow(
        elevation = radius,
        shape = shape,
        clip = false,
        ambientColor = tint,
        spotColor = tint,
    )
}
