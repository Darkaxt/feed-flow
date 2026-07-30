package com.prof18.feedflow.shared.domain.model

import com.prof18.feedflow.shared.domain.feed.MAX_WIDGET_FEED_ITEMS

const val MIN_WIDGET_MAXIMUM_ARTICLES = 1
const val DEFAULT_WIDGET_MAXIMUM_ARTICLES = MAX_WIDGET_FEED_ITEMS

fun normalizeWidgetMaximumArticles(value: Int): Int =
    value.coerceIn(
        minimumValue = MIN_WIDGET_MAXIMUM_ARTICLES,
        maximumValue = MAX_WIDGET_FEED_ITEMS,
    )
