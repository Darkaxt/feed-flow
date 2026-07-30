package com.prof18.feedflow.android.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetArticleLimitTest {

    @Test
    fun `renderer limit of one emits only the first feed item`() {
        val feedItems = (1..20).toList()

        assertEquals(listOf(1), limitWidgetFeedItems(feedItems, maximumArticles = 1))
    }

    @Test
    fun `renderer midrange limit preserves feed item order`() {
        val feedItems = (1..20).toList()

        assertEquals((1..7).toList(), limitWidgetFeedItems(feedItems, maximumArticles = 7))
    }

    @Test
    fun `renderer compatibility limit emits at most fifteen feed items`() {
        val feedItems = (1..20).toList()

        assertEquals((1..15).toList(), limitWidgetFeedItems(feedItems, maximumArticles = 15))
    }

    @Test
    fun `renderer clamps invalid limits at both boundaries`() {
        val feedItems = (1..20).toList()

        assertEquals(listOf(1), limitWidgetFeedItems(feedItems, maximumArticles = Int.MIN_VALUE))
        assertEquals((1..15).toList(), limitWidgetFeedItems(feedItems, maximumArticles = Int.MAX_VALUE))
    }

    @Test
    fun `renderer preserves empty feed state`() {
        assertEquals(emptyList<Int>(), limitWidgetFeedItems(emptyList<Int>(), maximumArticles = 1))
    }

    @Test
    fun `preview limit one selects one sample while larger limits keep both samples`() {
        val samples = listOf("first", "second")

        assertEquals(listOf("first"), limitWidgetFeedItems(samples, maximumArticles = 1))
        assertEquals(samples, limitWidgetFeedItems(samples, maximumArticles = 2))
        assertEquals(samples, limitWidgetFeedItems(samples, maximumArticles = 15))
    }
}
