package com.prof18.feedflow.shared.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val HOURS_PER_DAY = 24L
private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * MILLIS_PER_SECOND

val DEFAULT_WIDGET_FRESHNESS = WidgetFreshness.LAST_3_DAYS

enum class WidgetFreshness(
    days: Long,
) {
    LAST_24_HOURS(days = 1L),
    LAST_3_DAYS(days = 3L),
    LAST_7_DAYS(days = 7L),
    ;

    val windowMillis: Long = days * MILLIS_PER_DAY
}

fun <T> filterWidgetItemsByFreshness(
    items: List<T>,
    freshness: WidgetFreshness,
    nowMillis: Long,
    pubDateMillis: (T) -> Long?,
): ImmutableList<T> {
    val cutoffMillis = saturatedSubtract(nowMillis, freshness.windowMillis)
    return items.filter { item ->
        pubDateMillis(item)?.let { it >= cutoffMillis } == true
    }.toImmutableList()
}

private fun saturatedSubtract(value: Long, amount: Long): Long =
    if (value < Long.MIN_VALUE + amount) Long.MIN_VALUE else value - amount
