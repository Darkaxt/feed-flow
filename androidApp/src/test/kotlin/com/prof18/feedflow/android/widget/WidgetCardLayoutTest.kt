package com.prof18.feedflow.android.widget

import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCardLayoutTest {

    @Test
    fun `fill row height covers maximum text lines padding and existing date spacing`() {
        assertEquals(
            76,
            calculateWidgetFillRowHeightDp(
                fontSizes = widgetFontSizes(MIN_WIDGET_FONT_SCALE),
                systemFontScale = 0.85f,
            ),
        )
        assertEquals(
            100,
            calculateWidgetFillRowHeightDp(
                fontSizes = widgetFontSizes(0),
                systemFontScale = 1f,
            ),
        )
        assertEquals(
            194,
            calculateWidgetFillRowHeightDp(
                fontSizes = widgetFontSizes(MAX_WIDGET_FONT_SCALE),
                systemFontScale = 1.3f,
            ),
        )
    }

    @Test
    fun `fill geometry uses one fixed square row-height viewport`() {
        val layout = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = 300f,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )

        assertEquals(WidgetCardImageSizing.FILL_ROW_HEIGHT, layout.imageSizing)
        assertEquals(100, layout.fixedRowHeightDp)
        assertEquals(100, layout.imageViewportDp)
        assertEquals(100, layout.displayTargetDp)
    }

    @Test
    fun `per-side scaffold padding makes a 240dp widget fall back`() {
        val widgetWidthDp = 240f
        val unadjustedLayout = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = widgetWidthDp,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )
        val availableSlabWidthDp = calculateWidgetAvailableSlabWidthDp(widgetWidthDp)
        val adjustedLayout = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = availableSlabWidthDp,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )

        assertEquals(WidgetCardImageSizing.FILL_ROW_HEIGHT, unadjustedLayout.imageSizing)
        assertEquals(
            widgetWidthDp - WIDGET_SCAFFOLD_HORIZONTAL_PADDING_DP * 2,
            availableSlabWidthDp,
            0f,
        )
        assertEquals(216f, availableSlabWidthDp, 0f)
        assertEquals(
            84f,
            calculateWidgetCardReadableTextWidthDp(
                availableSlabWidthDp = availableSlabWidthDp,
                imageViewportDp = unadjustedLayout.imageViewportDp,
            ),
            0f,
        )
        assertThumbnailGeometry(adjustedLayout)
    }

    @Test
    fun `per-side scaffold padding preserves fill row at 256dp`() {
        val availableSlabWidthDp = calculateWidgetAvailableSlabWidthDp(widgetWidthDp = 256f)
        val layout = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = availableSlabWidthDp,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )

        assertEquals(232f, availableSlabWidthDp, 0f)
        assertEquals(WidgetCardImageSizing.FILL_ROW_HEIGHT, layout.imageSizing)
        assertTrue(
            calculateWidgetCardReadableTextWidthDp(
                availableSlabWidthDp = availableSlabWidthDp,
                imageViewportDp = layout.imageViewportDp,
            ) >= 96f,
        )
    }

    @Test
    fun `scaffold padding clamps available slab width to nonnegative`() {
        val totalHorizontalPaddingDp = WIDGET_SCAFFOLD_HORIZONTAL_PADDING_DP * 2f

        assertEquals(0f, calculateWidgetAvailableSlabWidthDp(widgetWidthDp = -1f), 0f)
        assertEquals(0f, calculateWidgetAvailableSlabWidthDp(widgetWidthDp = 0f), 0f)
        assertEquals(
            0f,
            calculateWidgetAvailableSlabWidthDp(widgetWidthDp = totalHorizontalPaddingDp - 1f),
            0f,
        )
        assertEquals(
            0f,
            calculateWidgetAvailableSlabWidthDp(widgetWidthDp = totalHorizontalPaddingDp),
            0f,
        )
        assertEquals(
            1f,
            calculateWidgetAvailableSlabWidthDp(widgetWidthDp = totalHorizontalPaddingDp + 1f),
            0f,
        )
    }

    @Test
    fun `fill requires leading inset readable text gap and square image width`() {
        val fitsExactly = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = 228f,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )
        val tooNarrow = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = 227.9f,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )

        assertEquals(WidgetCardImageSizing.FILL_ROW_HEIGHT, fitsExactly.imageSizing)
        assertThumbnailGeometry(tooNarrow)
    }

    @Test
    fun `narrow wide narrow exact sizes re-resolve fill preference for every width`() {
        val modes = listOf(220f, 360f, 220f).map { width ->
            resolveWidgetCardLayout(
                requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
                availableSlabWidthDp = width,
                fontSizes = widgetFontSizes(0),
                systemFontScale = 1f,
            ).imageSizing
        }

        assertEquals(
            listOf(
                WidgetCardImageSizing.THUMBNAIL,
                WidgetCardImageSizing.FILL_ROW_HEIGHT,
                WidgetCardImageSizing.THUMBNAIL,
            ),
            modes,
        )
    }

    @Test
    fun `256dp width at maximum widget and system scale uses complete thumbnail fallback`() {
        val layout = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = calculateWidgetAvailableSlabWidthDp(widgetWidthDp = 256f),
            fontSizes = widgetFontSizes(MAX_WIDGET_FONT_SCALE),
            systemFontScale = 1.3f,
        )

        assertThumbnailGeometry(layout)
    }

    @Test
    fun `thumbnail preference always preserves 50dp viewport and natural row height`() {
        val layout = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.THUMBNAIL,
            availableSlabWidthDp = 500f,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )

        assertThumbnailGeometry(layout)
    }

    @Test
    fun `invalid slab width uses 96dp fallback and complete thumbnail geometry`() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f).forEach { width ->
            val layout = resolveWidgetCardLayout(
                requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
                availableSlabWidthDp = width,
                fontSizes = widgetFontSizes(0),
                systemFontScale = 1f,
            )

            assertThumbnailGeometry(layout)
        }
    }

    @Test
    fun `divider layout emits only between cards with true inset and one dp thickness`() {
        assertEquals(
            WidgetCardDividerLayout(
                horizontalInsetDp = 16,
                thicknessDp = 1,
            ),
            resolveWidgetCardDividerLayout(
                itemSeparation = WidgetCardItemSeparation.DIVIDER,
                itemIndex = 0,
                itemCount = 2,
            ),
        )
        assertNull(
            resolveWidgetCardDividerLayout(
                itemSeparation = WidgetCardItemSeparation.DIVIDER,
                itemIndex = 1,
                itemCount = 2,
            ),
        )
        assertNull(
            resolveWidgetCardDividerLayout(
                itemSeparation = WidgetCardItemSeparation.DIVIDER,
                itemIndex = 0,
                itemCount = 1,
            ),
        )
        WidgetCardItemSeparation.entries
            .filterNot { it == WidgetCardItemSeparation.DIVIDER }
            .forEach { itemSeparation ->
                assertNull(
                    resolveWidgetCardDividerLayout(
                        itemSeparation = itemSeparation,
                        itemIndex = 0,
                        itemCount = 2,
                    ),
                )
            }
    }

    private fun assertThumbnailGeometry(layout: ResolvedWidgetCardLayout) {
        assertEquals(WidgetCardImageSizing.THUMBNAIL, layout.imageSizing)
        assertNull(layout.fixedRowHeightDp)
        assertEquals(50, layout.imageViewportDp)
        assertEquals(50, layout.displayTargetDp)
    }
}
