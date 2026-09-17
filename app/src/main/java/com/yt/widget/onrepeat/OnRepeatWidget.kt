package com.yt.widget.onrepeat

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import com.yt.R
import com.yt.widget.core.WIDGET_HERO_CORNER_DP
import com.yt.widget.core.WIDGET_HERO_HEIGHT_PX
import com.yt.widget.core.WIDGET_HERO_WIDTH_PX
import com.yt.widget.core.WIDGET_THUMB_CORNER_DP
import com.yt.widget.core.WIDGET_THUMB_HEIGHT
import com.yt.widget.core.WIDGET_THUMB_WIDTH
import com.yt.widget.core.WidgetDeepLink
import com.yt.widget.core.WidgetImageLoader
import com.yt.widget.core.WidgetVideoItem
import com.yt.widget.core.WidgetVideoPanel
import com.yt.widget.core.YTGlanceTheme
import com.yt.widget.core.widgetColorsFlow
import com.yt.widget.core.widgetEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** The music brain's On Repeat shelf on the home screen — zero network to render. */
class OnRepeatWidget : GlanceAppWidget() {
    companion object {
        private const val MAX_ITEMS = 8
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val density = context.resources.displayMetrics.density
        val thumbWidthPx = (WIDGET_THUMB_WIDTH.value * density).toInt()
        val thumbHeightPx = (WIDGET_THUMB_HEIGHT.value * density).toInt()

        // Rows open the Music tab, where the shelf leads the page.
        val openMusic = WidgetDeepLink.openRoute(context, WidgetDeepLink.ROUTE_MUSIC)

        val items =
            withContext(Dispatchers.IO) {
                widgetEntryPoint(context)
                    .musicBrainEngine()
                    .heavyRotationTracks(MAX_ITEMS)
                    .mapIndexed { index, track ->
                        WidgetVideoItem(
                            videoId = track.videoId,
                            title = track.title,
                            subtitle = track.artist,
                            thumbnail =
                                if (index == 0) {
                                    null
                                } else {
                                    WidgetImageLoader.load(
                                        context,
                                        track.thumbnailUrl,
                                        thumbWidthPx,
                                        thumbHeightPx,
                                        WIDGET_THUMB_CORNER_DP * density,
                                    )
                                },
                            hero =
                                if (index == 0) {
                                    WidgetImageLoader.load(
                                        context,
                                        track.thumbnailUrl,
                                        WIDGET_HERO_WIDTH_PX,
                                        WIDGET_HERO_HEIGHT_PX,
                                        WIDGET_HERO_CORNER_DP * density,
                                    )
                                } else {
                                    null
                                },
                            openIntent = openMusic,
                        )
                    }
            }

        val colorsFlow = widgetColorsFlow(context)
        val initialColors = colorsFlow.first()

        provideContent {
            val colors by colorsFlow.collectAsState(initialColors)
            YTGlanceTheme(colors) {
                WidgetVideoPanel(
                    title = context.getString(R.string.widget_on_repeat),
                    headerIconRes = R.drawable.ic_widget_music,
                    chipBackground = GlanceTheme.colors.secondaryContainer,
                    chipContent = GlanceTheme.colors.onSecondaryContainer,
                    headerAction = actionStartActivity(openMusic),
                    emptyMessage = context.getString(R.string.widget_no_on_repeat),
                    emptyAction = actionStartActivity(openMusic),
                    items = items,
                )
            }
        }
    }
}

class OnRepeatWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OnRepeatWidget()
}
