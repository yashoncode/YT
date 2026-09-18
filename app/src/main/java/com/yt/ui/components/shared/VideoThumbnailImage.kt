package com.yt.ui.components.shared

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import coil3.compose.AsyncImage
import com.yt.utils.ThumbnailUrlResolver

/** Enough to lose the detail of a 120px thumbnail without losing its colour layout. */
private val PLACEHOLDER_BLUR = 18.dp
private const val PLACEHOLDER_FADE_MILLIS = 220

@Composable
fun VideoThumbnailImage(
    videoId: String,
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val models =
        remember(videoId, model) {
            when {
                model is String || model == null -> {
                    ThumbnailUrlResolver.resolveVideoThumbnailCandidates(videoId, model as? String)
                }

                else -> {
                    listOf(model)
                }
            }
        }

    // YouTube's smallest still is a few kilobytes and is usually on screen within a frame or two of
    // the request. Blurred up to fill the card it reads as the image arriving, where an empty box
    // reads as the app having lost it.
    val placeholderUrl = remember(videoId) { ThumbnailUrlResolver.buildTinyYoutubeThumbnail(videoId) }

    SafeAsyncImage(
        models = models,
        placeholderUrl = placeholderUrl.takeIf { it.isNotEmpty() },
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@Composable
private fun SafeAsyncImage(
    models: List<Any>,
    placeholderUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    var index by remember(models) { mutableStateOf(0) }
    val currentModel = models.getOrNull(index)

    when {
        currentModel is ImageVector -> {
            Image(
                imageVector = currentModel,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }

        (currentModel is String && currentModel.isNotEmpty()) || currentModel is Int -> {
            var loaded by remember(currentModel) { mutableStateOf(false) }

            Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (placeholderUrl != null) {
                    AnimatedVisibility(
                        visible = !loaded,
                        enter = fadeIn(tween(PLACEHOLDER_FADE_MILLIS)),
                        exit = fadeOut(tween(PLACEHOLDER_FADE_MILLIS)),
                    ) {
                        AsyncImage(
                            model = placeholderUrl,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    // Rectangle, not the default: the blur is behind the sharp image
                                    // and a soft edge would show as a seam around the card.
                                    .blur(PLACEHOLDER_BLUR, BlurredEdgeTreatment.Rectangle),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                AsyncImage(
                    model = currentModel,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    onSuccess = { loaded = true },
                    onError = {
                        index = if (index < models.lastIndex) index + 1 else models.size
                    },
                )
            }
        }

        else -> {
            Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}
