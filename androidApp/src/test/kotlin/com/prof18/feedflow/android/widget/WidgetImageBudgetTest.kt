package com.prof18.feedflow.android.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.domain.feed.WIDGET_FEED_ITEM_SAFETY_CAPACITY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetImageBudgetTest {

    @Test
    fun `host slot cap limits item capacity for one through four payload variants`() {
        val expectedPolicies = listOf(
            Triple(1, 80, 80L),
            Triple(2, 40, 80L),
            Triple(3, 26, 78L),
            Triple(4, 20, 80L),
        )

        expectedPolicies.forEach { (variantCount, expectedCapacity, expectedPayloadCount) ->
            val budget = resolveBudget(
                variantCount = variantCount,
                feedItemCount = WIDGET_FEED_ITEM_SAFETY_CAPACITY,
            )

            assertEquals(WIDGET_FEED_ITEM_SAFETY_CAPACITY, budget.feedItemCount)
            assertEquals("capacity for $variantCount variants", expectedCapacity, budget.itemCapacity)
            assertEquals("payloads for $variantCount variants", expectedPayloadCount, budget.payloadCount)
        }
    }

    @Test
    fun `candidate counts below host capacity do not shrink reserved payload count`() {
        val expectedPayloadCounts = listOf(80L, 80L, 78L, 80L)

        expectedPayloadCounts.forEachIndexed { index, expectedPayloadCount ->
            val variantCount = index + 1
            val budget = resolveBudget(
                variantCount = variantCount,
                feedItemCount = 7,
            )

            assertEquals(7, budget.feedItemCount)
            assertEquals(7, budget.itemCapacity)
            assertEquals(expectedPayloadCount, budget.payloadCount)
        }
    }

    @Test
    fun `zero candidates keeps stable host reservation with zero item capacity`() {
        val budget = resolveBudget(
            variantCount = 2,
            feedItemCount = 0,
        )

        assertEquals(0, budget.feedItemCount)
        assertEquals(0, budget.itemCapacity)
        assertEquals(80L, budget.payloadCount)
    }

    @Test
    fun `more payload variants than host slots yields zero capacity and one reserved slot`() {
        val budget = resolveBudget(
            variantCount = 81,
            feedItemCount = WIDGET_FEED_ITEM_SAFETY_CAPACITY,
        )

        assertEquals(0, budget.itemCapacity)
        assertEquals(1L, budget.payloadCount)
        assertEquals(512, budget.resolveRequest("https://example.com/image", displayTargetPx = 1_000)?.edgePx)
    }

    @Test
    fun `negative candidate count is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            resolveBudget(
                variantCount = 1,
                feedItemCount = -1,
            )
        }
    }

    @Test
    fun `budget uses exact payload variants including unusable layout sizes`() {
        val exactSizes = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = listOf(
                    WidgetExactSize(100f, 200f),
                    WidgetExactSize(0f, 200f),
                    WidgetExactSize(100f, 200f),
                    WidgetExactSize(-40f, 200f),
                ),
                minWidthDp = null,
                maxWidthDp = null,
                minHeightDp = null,
                maxHeightDp = null,
            ),
            currentSize = DpSize(120.dp, 180.dp),
            sdkInt = 31,
        )

        val budget = resolveWidgetImageBudget(
            screenWidthPx = 480,
            screenHeightPx = 800,
            exactSizes = exactSizes,
            feedItemCount = WIDGET_FEED_ITEM_SAFETY_CAPACITY,
        )

        assertEquals(1, exactSizes.variantCount)
        assertEquals(3, exactSizes.payloadVariantCount)
        assertEquals(WIDGET_FEED_ITEM_SAFETY_CAPACITY, budget.feedItemCount)
        assertEquals(26, budget.itemCapacity)
        assertEquals(78L, budget.payloadCount)
        assertTrue(budget.exactSizeKey.contains("payloadVariants=3"))
    }

    @Test
    fun `list policy keeps 50dp viewport and stable host payload budget`() {
        val variantCount = 3
        val imageLayout = resolveWidgetListImageLayout(displayDensity = 2f)
        val budget = resolveBudget(variantCount = variantCount)
        val requests = List(budget.itemCapacity * variantCount) { index ->
            requireNotNull(
                budget.resolveRequest(
                    imageUrl = "https://example.com/list-image-$index",
                    displayTargetPx = imageLayout.displayTargetPx,
                ),
            )
        }

        assertEquals(50, imageLayout.displayViewportDp)
        assertEquals(100, imageLayout.displayTargetPx)
        assertEquals(WIDGET_FEED_ITEM_SAFETY_CAPACITY, budget.feedItemCount)
        assertEquals(26, budget.itemCapacity)
        assertEquals(78L, budget.payloadCount)
        assertTrue(requests.all { it.identity.payloadBudgetBytes == budget.payloadBudgetBytes })
        assertTrue(requests.all { it.identity.exactSizeKey == budget.exactSizeKey })
        assertTrue(requests.all { it.identity.payloadCount == budget.payloadCount })
    }

    @Test
    fun `480 by 800 display uses Android ceiling with 25 percent reserve`() {
        val budget = resolveBudget(
            screenWidthPx = 480,
            screenHeightPx = 800,
            feedItemCount = 15,
            variantCount = 1,
        )

        assertEquals(2_304_000L, budget.remoteViewsLimitBytes)
        assertEquals(1_728_000L, budget.effectiveArticleBudgetBytes)
        assertEquals(21_600L, budget.payloadBudgetBytes)
        assertEquals(73, budget.budgetEdgePx)
    }

    @Test
    fun `large displays cap the effective article budget at 6 MiB`() {
        val budget = resolveBudget(
            screenWidthPx = 4_000,
            screenHeightPx = 4_000,
            feedItemCount = 15,
            variantCount = 1,
        )

        assertEquals(6L * 1024L * 1024L, budget.effectiveArticleBudgetBytes)
        assertEquals(140, budget.budgetEdgePx)
    }

    @Test
    fun `maximum integer dimensions do not overflow into a negative budget`() {
        val budget = resolveBudget(
            screenWidthPx = Int.MAX_VALUE,
            screenHeightPx = Int.MAX_VALUE,
            feedItemCount = WIDGET_FEED_ITEM_SAFETY_CAPACITY,
            variantCount = 2,
        )

        assertTrue(budget.remoteViewsLimitBytes > 0L)
        assertEquals(6L * 1024L * 1024L, budget.effectiveArticleBudgetBytes)
        assertTrue(budget.payloadBudgetBytes > 0L)
    }

    @Test
    fun `request edge is capped by display target and byte budget`() {
        val budget = resolveBudget(
            screenWidthPx = 480,
            screenHeightPx = 800,
            feedItemCount = 15,
            variantCount = 1,
        )

        assertEquals(50, budget.resolveRequest("https://example.com/image", displayTargetPx = 50)?.edgePx)
        assertEquals(73, budget.resolveRequest("https://example.com/image", displayTargetPx = 500)?.edgePx)
        assertNull(budget.resolveRequest("https://example.com/image", displayTargetPx = 0))
    }

    @Test
    fun `candidate count cannot change stable host budget or image identity`() {
        val oneCandidateBudget = resolveBudget(
            variantCount = 2,
            feedItemCount = 1,
            exactSizeKey = "same-sizes",
        )
        val hundredCandidateBudget = resolveBudget(
            variantCount = 2,
            feedItemCount = WIDGET_FEED_ITEM_SAFETY_CAPACITY,
            exactSizeKey = "same-sizes",
        )

        val oneItemRequest = oneCandidateBudget.resolveRequest(
            imageUrl = "https://example.com/image",
            displayTargetPx = 50,
        )
        val hundredItemRequest = hundredCandidateBudget.resolveRequest(
            imageUrl = "https://example.com/image",
            displayTargetPx = 50,
        )

        assertNotEquals(oneCandidateBudget.feedItemCount, hundredCandidateBudget.feedItemCount)
        assertNotEquals(oneCandidateBudget.itemCapacity, hundredCandidateBudget.itemCapacity)
        assertEquals(80L, oneCandidateBudget.payloadCount)
        assertEquals(oneCandidateBudget.payloadCount, hundredCandidateBudget.payloadCount)
        assertEquals(oneCandidateBudget.payloadBudgetBytes, hundredCandidateBudget.payloadBudgetBytes)
        assertEquals(oneCandidateBudget.identity, hundredCandidateBudget.identity)
        assertEquals(oneItemRequest, hundredItemRequest)
    }

    @Test
    fun `smaller budget changes policy and request identity when bounded edge is unchanged`() {
        val largerBudget = resolveBudget(
            screenWidthPx = 480,
            screenHeightPx = 800,
            variantCount = 1,
            exactSizeKey = "larger-screen",
        )
        val smallerBudget = resolveBudget(
            screenWidthPx = 400,
            screenHeightPx = 700,
            variantCount = 1,
            exactSizeKey = "smaller-screen",
        )
        val largerRequest = largerBudget.resolveRequest("https://example.com/image", displayTargetPx = 50)
        val smallerRequest = smallerBudget.resolveRequest("https://example.com/image", displayTargetPx = 50)

        assertTrue(smallerBudget.payloadBudgetBytes < largerBudget.payloadBudgetBytes)
        assertEquals(largerRequest?.edgePx, smallerRequest?.edgePx)
        assertNotEquals(largerBudget.identity, smallerBudget.identity)
        assertNotEquals(largerRequest?.identity, smallerRequest?.identity)
        assertEquals(smallerBudget.payloadBudgetBytes, smallerRequest?.identity?.payloadBudgetBytes)
        assertEquals("smaller-screen", smallerRequest?.identity?.exactSizeKey)
    }

    @Test
    fun `exact-size key participates in policy and request identity even at the same budget`() {
        val first = resolveBudget(variantCount = 1, exactSizeKey = "first")
        val second = resolveBudget(variantCount = 1, exactSizeKey = "second")

        assertEquals(first.payloadBudgetBytes, second.payloadBudgetBytes)
        assertNotEquals(first.identity, second.identity)
        assertNotEquals(
            first.resolveRequest("https://example.com/image", 50)?.identity,
            second.resolveRequest("https://example.com/image", 50)?.identity,
        )
    }

    private fun resolveBudget(
        screenWidthPx: Int = 480,
        screenHeightPx: Int = 800,
        variantCount: Int,
        exactSizeKey: String = "sizes-$variantCount",
        feedItemCount: Int = WIDGET_FEED_ITEM_SAFETY_CAPACITY,
    ): WidgetImageBudgetPolicy = resolveWidgetImageBudget(
        screenWidthPx = screenWidthPx,
        screenHeightPx = screenHeightPx,
        exactSizes = WidgetExactSizeResolution(
            stableKey = exactSizeKey,
            sizes = List(variantCount) { index ->
                DpSize((100 + index).dp, (200 + index).dp)
            },
            payloadVariantCount = variantCount,
        ),
        feedItemCount = feedItemCount,
    )
}
