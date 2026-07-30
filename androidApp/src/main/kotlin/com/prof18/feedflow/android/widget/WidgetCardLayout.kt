package com.prof18.feedflow.android.widget

import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val TEXT_VERTICAL_PADDING_DP = 16
private const val DATE_TOP_SPACING_DP = 4
internal const val WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER = 1.2f
private const val LEADING_TEXT_INSET_DP = 16
private const val MIN_READABLE_TEXT_WIDTH_DP = 96
private const val IMAGE_GAP_DP = 16
internal const val WIDGET_SCAFFOLD_HORIZONTAL_PADDING_DP = 12
internal const val WIDGET_THUMBNAIL_VIEWPORT_DP = 50
private const val FALLBACK_SLAB_WIDTH_DP = 96

internal fun calculateWidgetAvailableSlabWidthDp(widgetWidthDp: Float): Float =
    widgetWidthDp
        .takeIf(Float::isFinite)
        ?.minus(WIDGET_SCAFFOLD_HORIZONTAL_PADDING_DP * 2)
        ?.coerceAtLeast(0f)
        ?: 0f

internal fun calculateWidgetCardReadableTextWidthDp(
    availableSlabWidthDp: Float,
    imageViewportDp: Int,
): Float = (
    availableSlabWidthDp -
        LEADING_TEXT_INSET_DP -
        IMAGE_GAP_DP -
        imageViewportDp
    ).coerceAtLeast(0f)

internal fun resolveWidgetListImageLayout(displayDensity: Float): ResolvedWidgetListImageLayout {
    val imageDensity = displayDensity.takeIf { it.isFinite() && it > 0f } ?: 1f
    return ResolvedWidgetListImageLayout(
        displayViewportDp = WIDGET_THUMBNAIL_VIEWPORT_DP,
        displayTargetPx = (WIDGET_THUMBNAIL_VIEWPORT_DP * imageDensity)
            .roundToInt()
            .coerceAtLeast(1),
    )
}

internal fun calculateWidgetFillRowHeightDp(
    fontSizes: WidgetFontSizes,
    systemFontScale: Float,
): Int {
    require(systemFontScale.isFinite() && systemFontScale > 0f)

    val metaLineHeight = widgetLineHeightDp(fontSizes.meta, systemFontScale)
    val titleLineHeight = widgetLineHeightDp(fontSizes.title, systemFontScale)
    return TEXT_VERTICAL_PADDING_DP * 2 +
        metaLineHeight +
        titleLineHeight * 2 +
        DATE_TOP_SPACING_DP +
        metaLineHeight
}

internal fun resolveWidgetCardLayout(
    requestedImageSizing: WidgetCardImageSizing,
    availableSlabWidthDp: Float,
    fontSizes: WidgetFontSizes,
    systemFontScale: Float,
): ResolvedWidgetCardLayout {
    if (requestedImageSizing == WidgetCardImageSizing.THUMBNAIL) {
        return thumbnailWidgetCardLayout()
    }

    val availableWidthDp = availableSlabWidthDp.takeIf { it.isFinite() && it > 0f }
        ?: FALLBACK_SLAB_WIDTH_DP.toFloat()
    val rowHeightDp = calculateWidgetFillRowHeightDp(
        fontSizes = fontSizes,
        systemFontScale = systemFontScale,
    )
    if (
        calculateWidgetCardReadableTextWidthDp(
            availableSlabWidthDp = availableWidthDp,
            imageViewportDp = rowHeightDp,
        ) < MIN_READABLE_TEXT_WIDTH_DP
    ) {
        return thumbnailWidgetCardLayout()
    }

    return ResolvedWidgetCardLayout(
        imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
        fixedRowHeightDp = rowHeightDp,
        imageViewportDp = rowHeightDp,
        displayTargetDp = rowHeightDp,
    )
}

private fun widgetLineHeightDp(fontSizeSp: Int, systemFontScale: Float): Int =
    ceil(fontSizeSp * systemFontScale * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER).toInt()

private fun thumbnailWidgetCardLayout(): ResolvedWidgetCardLayout = ResolvedWidgetCardLayout(
    imageSizing = WidgetCardImageSizing.THUMBNAIL,
    fixedRowHeightDp = null,
    imageViewportDp = WIDGET_THUMBNAIL_VIEWPORT_DP,
    displayTargetDp = WIDGET_THUMBNAIL_VIEWPORT_DP,
)

internal data class ResolvedWidgetListImageLayout(
    val displayViewportDp: Int,
    val displayTargetPx: Int,
)

internal data class ResolvedWidgetCardLayout(
    val imageSizing: WidgetCardImageSizing,
    val fixedRowHeightDp: Int?,
    val imageViewportDp: Int,
    val displayTargetDp: Int,
)
