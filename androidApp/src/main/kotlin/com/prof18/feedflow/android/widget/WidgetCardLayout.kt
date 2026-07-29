package com.prof18.feedflow.android.widget

import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import kotlin.math.ceil

private const val TEXT_VERTICAL_PADDING_DP = 16
private const val DATE_TOP_SPACING_DP = 4
private const val TEXT_LINE_HEIGHT_MULTIPLIER = 1.2f
private const val LEADING_TEXT_INSET_DP = 16
private const val MIN_READABLE_TEXT_WIDTH_DP = 96
private const val IMAGE_GAP_DP = 16
private const val THUMBNAIL_VIEWPORT_DP = 50

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

    val rowHeightDp = calculateWidgetFillRowHeightDp(
        fontSizes = fontSizes,
        systemFontScale = systemFontScale,
    )
    val requiredWidthDp = LEADING_TEXT_INSET_DP +
        MIN_READABLE_TEXT_WIDTH_DP +
        IMAGE_GAP_DP +
        rowHeightDp
    if (availableSlabWidthDp < requiredWidthDp) {
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
    ceil(fontSizeSp * systemFontScale * TEXT_LINE_HEIGHT_MULTIPLIER).toInt()

private fun thumbnailWidgetCardLayout(): ResolvedWidgetCardLayout = ResolvedWidgetCardLayout(
    imageSizing = WidgetCardImageSizing.THUMBNAIL,
    fixedRowHeightDp = null,
    imageViewportDp = THUMBNAIL_VIEWPORT_DP,
    displayTargetDp = THUMBNAIL_VIEWPORT_DP,
)

internal data class ResolvedWidgetCardLayout(
    val imageSizing: WidgetCardImageSizing,
    val fixedRowHeightDp: Int?,
    val imageViewportDp: Int,
    val displayTargetDp: Int,
)
