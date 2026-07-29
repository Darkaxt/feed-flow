package com.prof18.feedflow.android.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.SizeF
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetExactSizeResolverTest {

    @Test
    fun `snapshot copies bundle values and explicit sizes immutably`() {
        val sourceSizes = arrayListOf(SizeF(100f, 200f))
        val bundle = legacyBundle(minWidth = 80, maxWidth = 160, minHeight = 60, maxHeight = 240).apply {
            putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sourceSizes)
        }

        val snapshot = WidgetOptionsSnapshot.fromBundle(bundle)
        sourceSizes += SizeF(300f, 400f)
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 999)

        assertEquals(listOf(WidgetExactSize(100f, 200f)), snapshot.explicitSizes)
        assertEquals(80, snapshot.minWidthDp)
    }

    @Test
    fun `api 31 uses usable explicit sizes and removes invalid entries and duplicates`() {
        val snapshot = WidgetOptionsSnapshot(
            explicitSizes = listOf(
                WidgetExactSize(100f, 200f),
                WidgetExactSize(0f, 200f),
                WidgetExactSize(100f, 200f),
                WidgetExactSize(300f, -1f),
                WidgetExactSize(240f, 120f),
            ),
            minWidthDp = 80,
            maxWidthDp = 160,
            minHeightDp = 60,
            maxHeightDp = 240,
        )

        val result = resolveExactSizes(snapshot, CURRENT_SIZE, sdkInt = 31)

        assertEquals(
            listOf(DpSize(100.dp, 200.dp), DpSize(240.dp, 120.dp)),
            result.sizes,
        )
        assertEquals(2, result.variantCount)
    }

    @Test
    fun `api 31 derives portrait and landscape only when all four legacy fields are positive`() {
        val result = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = null,
                minWidthDp = 80,
                maxWidthDp = 160,
                minHeightDp = 60,
                maxHeightDp = 240,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 31,
        )

        assertEquals(
            listOf(DpSize(80.dp, 240.dp), DpSize(160.dp, 60.dp)),
            result.sizes,
        )
    }

    @Test
    fun `api 31 empty explicit list with only one legacy pair falls back to current size`() {
        val result = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = emptyList(),
                minWidthDp = 80,
                maxWidthDp = null,
                minHeightDp = null,
                maxHeightDp = 240,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 35,
        )

        assertEquals(listOf(CURRENT_SIZE), result.sizes)
    }

    @Test
    fun `api 26 through 30 accepts a valid portrait pair independently`() {
        val result = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = listOf(WidgetExactSize(999f, 999f)),
                minWidthDp = 80,
                maxWidthDp = null,
                minHeightDp = null,
                maxHeightDp = 240,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 30,
        )

        assertEquals(listOf(DpSize(80.dp, 240.dp)), result.sizes)
    }

    @Test
    fun `api 26 through 30 accepts a valid landscape pair independently`() {
        val result = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = null,
                minWidthDp = null,
                maxWidthDp = 160,
                minHeightDp = 60,
                maxHeightDp = null,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 26,
        )

        assertEquals(listOf(DpSize(160.dp, 60.dp)), result.sizes)
    }

    @Test
    fun `legacy duplicate candidates are filtered and current size is used when neither pair is valid`() {
        val duplicatePairs = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = null,
                minWidthDp = 100,
                maxWidthDp = 100,
                minHeightDp = 200,
                maxHeightDp = 200,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 30,
        )
        val noPairs = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = null,
                minWidthDp = 0,
                maxWidthDp = -1,
                minHeightDp = 60,
                maxHeightDp = null,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 30,
        )

        assertEquals(listOf(DpSize(100.dp, 200.dp)), duplicatePairs.sizes)
        assertEquals(listOf(CURRENT_SIZE), noPairs.sizes)
    }

    @Test
    fun `stable key and variant count are deterministic from one snapshot current size and sdk branch`() {
        val snapshot = WidgetOptionsSnapshot(
            explicitSizes = listOf(WidgetExactSize(100f, 200f), WidgetExactSize(240f, 120f)),
            minWidthDp = 80,
            maxWidthDp = 160,
            minHeightDp = 60,
            maxHeightDp = 240,
        )

        val first = resolveExactSizes(snapshot, CURRENT_SIZE, sdkInt = 31)
        val second = resolveExactSizes(snapshot, CURRENT_SIZE, sdkInt = 31)
        val legacyBranch = resolveExactSizes(snapshot, CURRENT_SIZE, sdkInt = 30)

        assertEquals(first.stableKey, second.stableKey)
        assertEquals(first.variantCount, second.variantCount)
        assertNotEquals(first.stableKey, legacyBranch.stableKey)
    }

    private fun legacyBundle(
        minWidth: Int,
        maxWidth: Int,
        minHeight: Int,
        maxHeight: Int,
    ): Bundle = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)
    }

    private companion object {
        val CURRENT_SIZE = DpSize(120.dp, 180.dp)
    }
}
