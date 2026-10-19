package com.prof18.feedflow.android.widget

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LocalAppWidgetOptions
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.util.rememberAndroidFeedFlowStrings
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetConfiguration
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.feed.WidgetRenderState
import com.prof18.feedflow.shared.domain.model.filterWidgetItemsByFreshness
import com.prof18.feedflow.shared.presentation.WidgetRefreshState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal class FeedFlowWidget(
    private val repository: FeedWidgetRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val browserManager: BrowserManager,
    private val settingsRepository: SettingsRepository,
    private val widgetRefreshState: WidgetRefreshState,
    private val feedDownloadWorkerEnqueuer: FeedDownloadWorkerEnqueuer,
) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val widgetStateFlow = widgetSettingsRepository.observeWidgetConfiguration(appWidgetId)
            .flatMapLatest { config ->
                repository.getWidgetRenderState(config.contentFilter).map { renderState ->
                    WidgetContentState(config, renderState)
                }
            }
        // Glance rebuilds can briefly render the collectAsState initial value before the DB flow emits.
        // Preloading the current items avoids flashing the widget empty state during refreshes.
        val initialWidgetState = widgetStateFlow.first()
        val refreshQueuedFlow = feedDownloadWorkerEnqueuer.widgetRefreshQueued
        val initialRefreshQueued = refreshQueuedFlow.first()

        provideContent {
            val lyricist = rememberAndroidFeedFlowStrings(settingsRepository)

            ProvideFeedFlowStrings(lyricist) {
                val widgetState by widgetStateFlow.collectAsState(initialWidgetState)
                val isRefreshing by widgetRefreshState.isRefreshing.collectAsState()
                val isRefreshQueued by refreshQueuedFlow.collectAsState(initialRefreshQueued)
                val filteredFeedItems = filterWidgetItemsByFreshness(
                    items = widgetState.renderState.feedItems,
                    freshness = widgetState.configuration.freshness,
                    nowMillis = System.currentTimeMillis(),
                    pubDateMillis = { it.pubDateMillis },
                )

                val glanceContext = LocalContext.current
                val currentSize = LocalSize.current
                val appWidgetOptions = Bundle(LocalAppWidgetOptions.current)
                val exactSizes = resolveExactSizes(
                    snapshot = WidgetOptionsSnapshot.fromBundle(appWidgetOptions),
                    currentSize = currentSize,
                    sdkInt = Build.VERSION.SDK_INT,
                )
                val displayMetrics = glanceContext.resources.displayMetrics
                val imageBudgetPolicy = resolveWidgetImageBudget(
                    screenWidthPx = displayMetrics.widthPixels,
                    screenHeightPx = displayMetrics.heightPixels,
                    exactSizes = exactSizes,
                    feedItemCount = filteredFeedItems.size,
                )
                val displayFeedItems = limitWidgetFeedItems(
                    feedItems = filteredFeedItems,
                    itemCapacity = imageBudgetPolicy.itemCapacity,
                )
                val displayDensity = displayMetrics.density.takeIf { it.isFinite() && it > 0f } ?: 1f
                val systemFontScale = glanceContext.resources.configuration.fontScale
                    .takeIf { it.isFinite() && it > 0f }
                    ?: 1f

                GlanceTheme {
                    WidgetContent(
                        feedItems = displayFeedItems,
                        feedLayout = widgetState.configuration.feedLayout,
                        browserManager = browserManager,
                        showHeader = widgetState.configuration.showHeader,
                        showRefreshButton = widgetState.configuration.showRefreshButton,
                        isRefreshing = isRefreshQueued || isRefreshing,
                        headerTitle = widgetState.renderState.title,
                        filter = widgetState.renderState.contentFilter,
                        fontScale = widgetState.configuration.fontScale,
                        backgroundColor = widgetState.configuration.backgroundColor,
                        backgroundOpacityPercent = widgetState.configuration.backgroundOpacityPercent,
                        textColorMode = widgetState.configuration.textColorMode,
                        hideImages = widgetState.configuration.hideImages,
                        cardAppearance = widgetState.configuration.cardAppearance,
                        imageBudgetPolicy = imageBudgetPolicy,
                        availableSlabWidthDp = calculateWidgetAvailableSlabWidthDp(
                            widgetWidthDp = currentSize.width.value,
                        ),
                        displayDensity = displayDensity,
                        systemFontScale = systemFontScale,
                    )
                }
            }
        }
    }
}

private data class WidgetContentState(
    val configuration: WidgetConfiguration,
    val renderState: WidgetRenderState,
)

internal fun limitWidgetFeedItems(
    feedItems: ImmutableList<FeedItem>,
    itemCapacity: Int,
): ImmutableList<FeedItem> = feedItems.take(itemCapacity).toImmutableList()
