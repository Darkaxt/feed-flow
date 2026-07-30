package com.prof18.feedflow.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class WidgetFreshnessTest {

    @Test
    fun `freshness options and default match the approved design`() {
        assertEquals(
            listOf(
                WidgetFreshness.LAST_24_HOURS,
                WidgetFreshness.LAST_3_DAYS,
                WidgetFreshness.LAST_7_DAYS,
            ),
            WidgetFreshness.entries,
        )
        assertEquals(WidgetFreshness.LAST_3_DAYS, DEFAULT_WIDGET_FRESHNESS)
    }

    @Test
    fun `filter includes the inclusive cutoff and excludes older and undated items`() {
        val nowMillis = 10.days.inWholeMilliseconds
        val cutoffMillis = nowMillis - 3.days.inWholeMilliseconds
        val items = listOf(
            TestItem("newer", nowMillis - 1.hours.inWholeMilliseconds),
            TestItem("cutoff", cutoffMillis),
            TestItem("older", cutoffMillis - 1),
            TestItem("undated", null),
        )

        val result = filterWidgetItemsByFreshness(
            items = items,
            freshness = WidgetFreshness.LAST_3_DAYS,
            nowMillis = nowMillis,
            pubDateMillis = TestItem::pubDateMillis,
        )

        assertEquals(listOf("newer", "cutoff"), result.map(TestItem::id))
    }

    @Test
    fun `each freshness option uses its exact approved window`() {
        val nowMillis = 10.days.inWholeMilliseconds
        val items = listOf(
            TestItem("12-hours", nowMillis - 12.hours.inWholeMilliseconds),
            TestItem("2-days", nowMillis - 2.days.inWholeMilliseconds),
            TestItem("5-days", nowMillis - 5.days.inWholeMilliseconds),
        )

        assertEquals(
            listOf("12-hours"),
            filter(items, WidgetFreshness.LAST_24_HOURS, nowMillis),
        )
        assertEquals(
            listOf("12-hours", "2-days"),
            filter(items, WidgetFreshness.LAST_3_DAYS, nowMillis),
        )
        assertEquals(
            listOf("12-hours", "2-days", "5-days"),
            filter(items, WidgetFreshness.LAST_7_DAYS, nowMillis),
        )
    }

    @Test
    fun `filter preserves source order and includes future dates`() {
        val nowMillis = 10.days.inWholeMilliseconds
        val items = listOf(
            TestItem("first", nowMillis - 2.hours.inWholeMilliseconds),
            TestItem("future", nowMillis + 1.hours.inWholeMilliseconds),
            TestItem("third", nowMillis - 1.hours.inWholeMilliseconds),
        )

        assertEquals(
            listOf("first", "future", "third"),
            filter(items, WidgetFreshness.LAST_24_HOURS, nowMillis),
        )
    }

    @Test
    fun `filter preserves the empty state`() {
        assertEquals(
            emptyList<TestItem>(),
            filterWidgetItemsByFreshness(
                items = emptyList(),
                freshness = WidgetFreshness.LAST_7_DAYS,
                nowMillis = 0,
                pubDateMillis = TestItem::pubDateMillis,
            ),
        )
    }

    private fun filter(
        items: List<TestItem>,
        freshness: WidgetFreshness,
        nowMillis: Long,
    ): List<String> = filterWidgetItemsByFreshness(
        items = items,
        freshness = freshness,
        nowMillis = nowMillis,
        pubDateMillis = TestItem::pubDateMillis,
    ).map(TestItem::id)

    private data class TestItem(
        val id: String,
        val pubDateMillis: Long?,
    )
}
