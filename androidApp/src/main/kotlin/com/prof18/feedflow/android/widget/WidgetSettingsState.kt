package com.prof18.feedflow.android.widget

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.DEFAULT_WIDGET_FRESHNESS
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetFreshness
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class WidgetSettingsState(
    val syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
    val feedLayout: WidgetFeedLayout = WidgetFeedLayout.LIST,
    val freshness: WidgetFreshness = DEFAULT_WIDGET_FRESHNESS,
    val showHeader: Boolean = true,
    val showRefreshButton: Boolean = false,
    val fontScale: Int = 0,
    val backgroundColor: Int? = null,
    val backgroundOpacityPercent: Int = 100,
    val textColorMode: WidgetTextColorMode = WidgetTextColorMode.AUTOMATIC,
    val hideImages: Boolean = false,
    val cardAppearance: WidgetCardAppearance = WidgetCardAppearance(),
    val contentFilter: WidgetContentFilter = WidgetContentFilter.Timeline,
    val categories: ImmutableList<FeedSourceCategory> = persistentListOf(),
    val feedSources: ImmutableList<FeedSource> = persistentListOf(),

)
