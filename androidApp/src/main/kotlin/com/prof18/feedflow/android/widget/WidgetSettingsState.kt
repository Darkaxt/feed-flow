package com.prof18.feedflow.android.widget

import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.DEFAULT_WIDGET_FRESHNESS
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetFreshness
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode

data class WidgetSettingsState(
    val syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
    val feedLayout: WidgetFeedLayout = WidgetFeedLayout.LIST,
    val freshness: WidgetFreshness = DEFAULT_WIDGET_FRESHNESS,
    val showHeader: Boolean = true,
    val fontScale: Int = 0,
    val backgroundColor: Int? = null,
    val backgroundOpacityPercent: Int = 100,
    val textColorMode: WidgetTextColorMode = WidgetTextColorMode.AUTOMATIC,
    val hideImages: Boolean = false,
    val cardAppearance: WidgetCardAppearance = WidgetCardAppearance(),
)
