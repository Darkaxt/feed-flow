package com.prof18.feedflow.android.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetImageBudgetTest {

    @Test
    fun `payload count uses actual filtered count with one empty reserve across all variants`() {
        listOf(0, 1, 15, 100).forEach { feedItemCount ->
            listOf(1, 3).forEach { variantCount ->
                val budget = resolveBudget(
                    feedItemCount = feedItemCount,
                    variantCount = variantCount,
                )
                val expectedPayloadCount = maxOf(feedItemCount, 1).toLong() * variantCount

                assertEquals(feedItemCount, budget.feedItemCount)
                assertEquals(expectedPayloadCount, budget.payloadCount)
                assertEquals(
                    budget.effectiveArticleBudgetBytes / expectedPayloadCount,
                    budget.payloadBudgetBytes,
                )
            }
        }
    }

    @Test
    fun `budget counts every serialized Glance variant including unusable layout sizes`() {
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
            feedItemCount = 15,
        )

        assertEquals(1, exactSizes.variantCount)
        assertEquals(3, exactSizes.payloadVariantCount)
        assertEquals(45L, budget.payloadCount)
        assertTrue(budget.exactSizeKey.contains("payloadVariants=3"))
    }

    @Test
    fun `list policy keeps 50dp viewport and actual-count multi-variant budget`() {
        val feedItemCount = 15
        val variantCount = 3
        val imageLayout = resolveWidgetListImageLayout(displayDensity = 2f)
        val budget = resolveBudget(
            feedItemCount = feedItemCount,
            variantCount = variantCount,
        )
        val requests = List(feedItemCount * variantCount) { index ->
            requireNotNull(
                budget.resolveRequest(
                    imageUrl = "https://example.com/list-image-$index",
                    displayTargetPx = imageLayout.displayTargetPx,
                ),
            )
        }

        assertEquals(50, imageLayout.displayViewportDp)
        assertEquals(100, imageLayout.displayTargetPx)
        assertEquals(45L, budget.payloadCount)
        assertTrue(requests.all { it.identity.payloadBudgetBytes == budget.payloadBudgetBytes })
        assertTrue(requests.all { it.identity.exactSizeKey == budget.exactSizeKey })
        assertTrue(requests.all { it.identity.feedItemCount == feedItemCount })
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
        assertEquals(115_200L, budget.payloadBudgetBytes)
        assertEquals(169, budget.budgetEdgePx)
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
        assertEquals(323, budget.budgetEdgePx)
    }

    @Test
    fun `maximum integer dimensions do not overflow into a negative budget`() {
        val budget = resolveBudget(
            screenWidthPx = Int.MAX_VALUE,
            screenHeightPx = Int.MAX_VALUE,
            feedItemCount = 100,
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
        assertEquals(169, budget.resolveRequest("https://example.com/image", displayTargetPx = 500)?.edgePx)
        assertNull(budget.resolveRequest("https://example.com/image", displayTargetPx = 0))
    }

    @Test
    fun `every actual count change invalidates budget and request identity`() {
        val budgets = listOf(0, 1, 15, 100).map { feedItemCount ->
            resolveBudget(
                feedItemCount = feedItemCount,
                variantCount = 2,
                exactSizeKey = "same-sizes",
            )
        }

        budgets.zipWithNext().forEach { (first, second) ->
            assertNotEquals(first.identity, second.identity)
            assertNotEquals(
                first.resolveRequest("https://example.com/image", displayTargetPx = 20)?.identity,
                second.resolveRequest("https://example.com/image", displayTargetPx = 20)?.identity,
            )
        }
        assertEquals(budgets[0].payloadBudgetBytes, budgets[1].payloadBudgetBytes)
        assertTrue(budgets[2].payloadBudgetBytes > budgets[3].payloadBudgetBytes)
    }

    @Test
    fun `smaller budget changes policy and request identity when bounded edge is unchanged`() {
        val largerBudget = resolveBudget(
            feedItemCount = 15,
            variantCount = 1,
            exactSizeKey = "one-size",
        )
        val smallerBudget = resolveBudget(
            feedItemCount = 15,
            variantCount = 2,
            exactSizeKey = "two-sizes",
        )
        val largerRequest = largerBudget.resolveRequest("https://example.com/image", displayTargetPx = 50)
        val smallerRequest = smallerBudget.resolveRequest("https://example.com/image", displayTargetPx = 50)

        assertTrue(smallerBudget.payloadBudgetBytes < largerBudget.payloadBudgetBytes)
        assertEquals(largerRequest?.edgePx, smallerRequest?.edgePx)
        assertNotEquals(largerBudget.identity, smallerBudget.identity)
        assertNotEquals(largerRequest?.identity, smallerRequest?.identity)
        assertEquals(smallerBudget.payloadBudgetBytes, smallerRequest?.identity?.payloadBudgetBytes)
        assertEquals("two-sizes", smallerRequest?.identity?.exactSizeKey)
    }

    @Test
    fun `exact-size key participates in policy and request identity even at the same budget`() {
        val first = resolveBudget(feedItemCount = 15, variantCount = 1, exactSizeKey = "first")
        val second = resolveBudget(feedItemCount = 15, variantCount = 1, exactSizeKey = "second")

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
        feedItemCount: Int,
        variantCount: Int,
        exactSizeKey: String = "sizes-$variantCount",
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
