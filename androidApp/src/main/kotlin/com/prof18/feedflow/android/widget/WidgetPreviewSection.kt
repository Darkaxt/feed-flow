package com.prof18.feedflow.android.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.android.settings.SettingsE2eIds
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.domain.model.WidgetFreshness
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.domain.model.filterWidgetItemsByFreshness
import com.prof18.feedflow.shared.domain.model.normalized
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun WidgetPreviewSection(
    settingsState: WidgetSettingsState,
    modifier: Modifier = Modifier,
) {
    var previewBackdropMode by rememberSaveable { mutableStateOf(WidgetPreviewBackdropMode.LIGHT) }
    var showPreviewNote by rememberSaveable { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current
    val previewAppearance = resolveWidgetPreviewAppearance(
        settingsState = settingsState,
        themedWidgetUnderlayColor = MaterialTheme.colorScheme.surface,
        themedCardSurfaceColor = MaterialTheme.colorScheme.secondaryContainer,
        themedOnSurfaceColor = MaterialTheme.colorScheme.onSurface,
    )

    Column(
        modifier = modifier.testTag("widget_preview"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.widgetBackgroundColorPreview,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { showPreviewNote = true },
                modifier = Modifier.testTag("widget_preview_info"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = strings.widgetPreviewInfo,
                )
            }
            PreviewBackdropToggleButton(
                backdropMode = previewBackdropMode,
                onClick = { previewBackdropMode = previewBackdropMode.next() },
                modifier = Modifier.testTag("widget_preview_toggle_background"),
            )
        }
        WidgetPreviewWallpaper(
            headerTitle = settingsState.widgetContentTitle(),
            feedLayout = settingsState.feedLayout,
            showWidgetHeader = settingsState.showHeader,
            showRefreshButton = settingsState.showRefreshButton,
            hideImages = settingsState.hideImages,
            fontSizes = widgetFontSizes(settingsState.fontScale),
            freshness = settingsState.freshness,
            outerSurfaceColor = previewAppearance.outerSurfaceColor,
            outerTextColors = previewAppearance.outerTextColors,
            cardAppearance = previewAppearance.cardAppearance,
            resolvedCardAppearance = previewAppearance.card,
            backdropMode = previewBackdropMode,
            modifier = Modifier
                .testTag(SettingsE2eIds.WIDGET_PREVIEW)
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular)
                .padding(bottom = Spacing.xsmall)
                .heightIn(min = 148.dp),
        )
    }

    if (showPreviewNote) {
        AlertDialog(
            onDismissRequest = { showPreviewNote = false },
            modifier = Modifier
                .testTag("widget_preview_note_dialog")
                .semantics { testTagsAsResourceId = true },
            title = { Text(text = strings.widgetPreviewInfo) },
            text = { Text(text = strings.widgetPreviewNote) },
            confirmButton = {
                TextButton(
                    onClick = { showPreviewNote = false },
                ) {
                    Text(text = strings.closeButtonContentDescription)
                }
            },
        )
    }
}

@Composable
private fun WidgetPreviewWallpaper(
    feedLayout: WidgetFeedLayout,
    freshness: WidgetFreshness,
    showWidgetHeader: Boolean,
    hideImages: Boolean,
    fontSizes: WidgetFontSizes,
    outerSurfaceColor: Color,
    outerTextColors: WidgetTextColors,
    cardAppearance: WidgetCardAppearance,
    resolvedCardAppearance: ResolvedWidgetCardAppearance,
    backdropMode: WidgetPreviewBackdropMode,
    headerTitle: String,
    showRefreshButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val wallpaperShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular)
            .background(
                brush = Brush.linearGradient(backdropMode.wallpaperColors),
                shape = wallpaperShape,
            )
            .padding(Spacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        WidgetPreview(
            headerTitle = headerTitle,
            showRefreshButton = showRefreshButton,
            feedLayout = feedLayout,
            freshness = freshness,
            showWidgetHeader = showWidgetHeader,
            hideImages = hideImages,
            fontSizes = fontSizes,
            outerSurfaceColor = outerSurfaceColor,
            outerTextColors = outerTextColors,
            cardAppearance = cardAppearance,
            resolvedCardAppearance = resolvedCardAppearance,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PreviewBackdropToggleButton(
    backdropMode: WidgetPreviewBackdropMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val backgroundColor = if (backdropMode == WidgetPreviewBackdropMode.LIGHT) {
        Color.White.copy(alpha = 0.88f)
    } else {
        PreviewToggleDarkChromeColor.copy(alpha = 0.96f)
    }
    val contentColor = if (backdropMode == WidgetPreviewBackdropMode.LIGHT) {
        PreviewToggleDarkChromeColor
    } else {
        Color.White
    }
    val icon = if (backdropMode == WidgetPreviewBackdropMode.LIGHT) {
        Icons.Outlined.LightMode
    } else {
        Icons.Outlined.DarkMode
    }

    IconButton(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color = backgroundColor, shape = CircleShape),
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = strings.widgetPreviewToggleBackground,
            tint = contentColor,
        )
    }
}

@Composable
private fun WidgetPreview(
    headerTitle: String,
    showRefreshButton: Boolean,
    feedLayout: WidgetFeedLayout,
    freshness: WidgetFreshness,
    showWidgetHeader: Boolean,
    hideImages: Boolean,
    fontSizes: WidgetFontSizes,
    outerSurfaceColor: Color,
    outerTextColors: WidgetTextColors,
    cardAppearance: WidgetCardAppearance,
    resolvedCardAppearance: ResolvedWidgetCardAppearance,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier,
        shape = shape,
        color = outerSurfaceColor,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showWidgetHeader || showRefreshButton) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.regular),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showWidgetHeader) {
                        Text(
                            text = headerTitle,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = fontSizes.header.sp,
                            fontWeight = FontWeight.Bold,
                            color = outerTextColors.primary,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (showRefreshButton) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = strings.refreshFeeds,
                            tint = outerTextColors.primary,
                            modifier = Modifier.size(width = 24.dp, height = 24.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xsmall))
            } else {
                Spacer(modifier = Modifier.height(Spacing.small))
            }

            val items = resolveWidgetPreviewSamples(
                freshness = freshness,
                nowMillis = PREVIEW_NOW_MILLIS,
            ).map { sample ->
                if (sample.isRecent) {
                    WidgetPreviewItem(
                        feedSource = strings.settingsFontScaleFeedSourceExample,
                        title = strings.settingsFontScaleTitleExample,
                        date = "25/12 - 14:30",
                    )
                } else {
                    WidgetPreviewItem(
                        feedSource = strings.settingsFontScaleFeedSourceExample,
                        title = strings.settingsFontScaleSubtitleExample,
                        date = "24/12 - 09:15",
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardLayout = resolveWidgetCardLayout(
                    requestedImageSizing = cardAppearance.imageSizing,
                    availableSlabWidthDp = maxWidth.value,
                    fontSizes = fontSizes,
                    systemFontScale = LocalDensity.current.fontScale,
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        when (feedLayout) {
                            WidgetFeedLayout.LIST -> WidgetPreviewListItem(
                                item = item,
                                fontSizes = fontSizes,
                                hideImages = hideImages,
                                textColors = outerTextColors,
                            )
                            WidgetFeedLayout.CARD -> {
                                WidgetPreviewCardItem(
                                    item = item,
                                    fontSizes = fontSizes,
                                    hideImages = hideImages,
                                    appearance = cardAppearance,
                                    resolvedAppearance = resolvedCardAppearance,
                                    cardLayout = cardLayout,
                                )
                                resolveWidgetCardDividerLayout(
                                    itemSeparation = cardAppearance.itemSeparation,
                                    itemIndex = index,
                                    itemCount = items.size,
                                )?.let { dividerLayout ->
                                    HorizontalDivider(
                                        modifier = Modifier.padding(
                                            horizontal = dividerLayout.horizontalInsetDp.dp,
                                        ),
                                        thickness = dividerLayout.thicknessDp.dp,
                                        color = resolvedCardAppearance.dividerColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))
        }
    }
}

internal fun resolveWidgetPreviewSamples(
    freshness: WidgetFreshness,
    nowMillis: Long,
) = filterWidgetItemsByFreshness(
    items = listOf(
        WidgetPreviewSample(
            isRecent = true,
            pubDateMillis = nowMillis - PREVIEW_RECENT_ITEM_AGE_MILLIS,
        ),
        WidgetPreviewSample(
            isRecent = false,
            pubDateMillis = nowMillis - PREVIEW_OLDER_ITEM_AGE_MILLIS,
        ),
    ),
    freshness = freshness,
    nowMillis = nowMillis,
    pubDateMillis = WidgetPreviewSample::pubDateMillis,
)

internal data class WidgetPreviewSample(
    val isRecent: Boolean,
    val pubDateMillis: Long,
)

private data class WidgetPreviewItem(
    val feedSource: String,
    val title: String,
    val date: String,
)

@Composable
private fun WidgetPreviewListItem(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    textColors: WidgetTextColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetPreviewTextContent(
            item = item,
            fontSizes = fontSizes,
            textColors = textColors,
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.regular),
        )
        if (!hideImages) {
            WidgetPreviewImage(
                size = WIDGET_THUMBNAIL_VIEWPORT_DP.dp,
                cornerRadius = 8.dp,
            )
        }
    }
}

@Composable
private fun WidgetPreviewCardItem(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    appearance: WidgetCardAppearance,
    resolvedAppearance: ResolvedWidgetCardAppearance,
    cardLayout: ResolvedWidgetCardLayout,
    modifier: Modifier = Modifier,
) {
    if (appearance.itemSeparation == WidgetCardItemSeparation.SPACING) {
        Box(modifier = modifier.padding(vertical = Spacing.xsmall)) {
            WidgetPreviewCardSlab(
                item = item,
                fontSizes = fontSizes,
                hideImages = hideImages,
                appearance = appearance,
                resolvedAppearance = resolvedAppearance,
                cardLayout = cardLayout,
            )
        }
    } else {
        WidgetPreviewCardSlab(
            item = item,
            fontSizes = fontSizes,
            hideImages = hideImages,
            appearance = appearance,
            resolvedAppearance = resolvedAppearance,
            cardLayout = cardLayout,
            modifier = modifier,
        )
    }
}

@Composable
private fun WidgetPreviewCardSlab(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    appearance: WidgetCardAppearance,
    resolvedAppearance: ResolvedWidgetCardAppearance,
    cardLayout: ResolvedWidgetCardLayout,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(appearance.cornerRadiusDp.dp)
    val slabColor = resolvedAppearance.slabFillColor ?: Color.Transparent

    Surface(
        modifier = when (cardLayout.imageSizing) {
            WidgetCardImageSizing.THUMBNAIL -> modifier.fillMaxWidth()
            WidgetCardImageSizing.FILL_ROW_HEIGHT ->
                modifier
                    .fillMaxWidth()
                    .height(requireNotNull(cardLayout.fixedRowHeightDp).dp)
        },
        shape = shape,
        color = slabColor,
    ) {
        when (cardLayout.imageSizing) {
            WidgetCardImageSizing.THUMBNAIL -> WidgetPreviewThumbnailCardContent(
                item = item,
                fontSizes = fontSizes,
                hideImages = hideImages,
                textColors = resolvedAppearance.textColors,
            )
            WidgetCardImageSizing.FILL_ROW_HEIGHT -> WidgetPreviewFillCardContent(
                item = item,
                fontSizes = fontSizes,
                hideImages = hideImages,
                textColors = resolvedAppearance.textColors,
                rowHeight = requireNotNull(cardLayout.fixedRowHeightDp).dp,
                cornerRadius = appearance.cornerRadiusDp.dp,
            )
        }
    }
}

@Composable
private fun WidgetPreviewThumbnailCardContent(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    textColors: WidgetTextColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetPreviewTextContent(
            item = item,
            fontSizes = fontSizes,
            textColors = textColors,
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.regular),
        )
        if (!hideImages) {
            WidgetPreviewImage(
                size = WIDGET_THUMBNAIL_VIEWPORT_DP.dp,
                cornerRadius = 8.dp,
            )
        }
    }
}

@Composable
private fun WidgetPreviewFillCardContent(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    textColors: WidgetTextColors,
    rowHeight: Dp,
    cornerRadius: Dp,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetPreviewTextContent(
            item = item,
            fontSizes = fontSizes,
            textColors = textColors,
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
        )
        if (!hideImages) {
            WidgetPreviewImage(
                size = rowHeight,
                cornerRadius = cornerRadius,
            )
        }
    }
}

@Composable
private fun WidgetPreviewTextContent(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    textColors: WidgetTextColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = item.feedSource,
            style = MaterialTheme.typography.bodySmall,
            fontSize = fontSizes.meta.sp,
            lineHeight = (fontSizes.meta * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER).sp,
            color = textColors.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = fontSizes.title.sp,
            lineHeight = (fontSizes.title * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER).sp,
            fontWeight = FontWeight.Bold,
            color = textColors.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.date,
            modifier = Modifier.padding(top = Spacing.xsmall),
            style = MaterialTheme.typography.bodySmall,
            fontSize = fontSizes.meta.sp,
            lineHeight = (fontSizes.meta * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER).sp,
            color = textColors.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WidgetPreviewImage(
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            ),
    )
}

private enum class WidgetPreviewBackdropMode(
    val wallpaperColors: List<Color>,
) {
    LIGHT(wallpaperColors = PreviewLightWallpaperColors),
    DARK(wallpaperColors = PreviewDarkWallpaperColors),
}

private fun WidgetPreviewBackdropMode.next(): WidgetPreviewBackdropMode =
    when (this) {
        WidgetPreviewBackdropMode.LIGHT -> WidgetPreviewBackdropMode.DARK
        WidgetPreviewBackdropMode.DARK -> WidgetPreviewBackdropMode.LIGHT
    }

internal fun resolveWidgetPreviewAppearance(
    settingsState: WidgetSettingsState,
    themedWidgetUnderlayColor: Color,
    themedCardSurfaceColor: Color,
    themedOnSurfaceColor: Color,
): ResolvedWidgetPreviewAppearance {
    val normalizedCardAppearance = settingsState.cardAppearance.normalized()
    val backgroundOpacity = settingsState.backgroundOpacityPercent.coerceIn(0, MAX_PERCENT) / PERCENT_DIVISOR
    val outerSurfaceBaseColor = settingsState.backgroundColor?.let(::widgetColorFromArgb)
        ?: themedWidgetUnderlayColor
    val effectiveOuterColor = widgetEffectiveBackgroundColor(
        backgroundColor = outerSurfaceBaseColor,
        backgroundOpacity = backgroundOpacity,
        underlayColor = themedWidgetUnderlayColor,
    )
    val outerTextColors = if (
        settingsState.backgroundColor == null && settingsState.textColorMode == WidgetTextColorMode.AUTOMATIC
    ) {
        WidgetTextColors(
            primary = themedOnSurfaceColor,
            secondary = themedOnSurfaceColor,
        )
    } else {
        widgetTextColorsForMode(
            textColorMode = settingsState.textColorMode,
            backgroundColor = effectiveOuterColor,
        )
    }

    return ResolvedWidgetPreviewAppearance(
        outerSurfaceColor = outerSurfaceBaseColor.copy(alpha = backgroundOpacity),
        effectiveOuterColor = effectiveOuterColor,
        outerTextColors = outerTextColors,
        cardAppearance = normalizedCardAppearance,
        card = resolveWidgetCardAppearance(
            appearance = normalizedCardAppearance,
            textColorMode = settingsState.textColorMode,
            outerSurfaceColor = outerSurfaceBaseColor,
            outerSurfaceOpacityPercent = settingsState.backgroundOpacityPercent,
            themedWidgetUnderlayColor = themedWidgetUnderlayColor,
            themedCardSurfaceColor = themedCardSurfaceColor,
            themedOnSurfaceColor = themedOnSurfaceColor,
        ),
    )
}

internal data class ResolvedWidgetPreviewAppearance(
    val outerSurfaceColor: Color,
    val effectiveOuterColor: Color,
    val outerTextColors: WidgetTextColors,
    val cardAppearance: WidgetCardAppearance,
    val card: ResolvedWidgetCardAppearance,
)

private const val MAX_PERCENT = 100
private const val PERCENT_DIVISOR = 100f
internal const val PREVIEW_NOW_MILLIS = 1_800_000_000_000L
internal const val PREVIEW_RECENT_ITEM_AGE_MILLIS = 12L * 60L * 60L * 1_000L
internal const val PREVIEW_OLDER_ITEM_AGE_MILLIS = 2L * 24L * 60L * 60L * 1_000L
private val PreviewToggleDarkChromeColor = Color(0xFF20283A)
private val PreviewLightWallpaperColors = listOf(
    Color(0xFFE9EEF8),
    Color(0xFFF4F6FB),
    Color(0xFFE1E7F2),
)
private val PreviewDarkWallpaperColors = listOf(
    Color(0xFF0E1420),
    Color(0xFF1A2232),
    Color(0xFF111827),
)

@Preview(name = "Compatibility card default")
@Composable
private fun WidgetPreviewCompatibilityCardPreview() {
    WidgetPreviewToolingSurface(
        settingsState = WidgetSettingsState(
            feedLayout = WidgetFeedLayout.CARD,
            cardAppearance = WidgetCardAppearance(),
        ),
        backdropMode = WidgetPreviewBackdropMode.LIGHT,
    )
}

@Preview(
    name = "Transparent divider fill card",
    widthDp = 480,
)
@Composable
private fun WidgetPreviewTransparentDividerFillPreview() {
    WidgetPreviewToolingSurface(
        settingsState = WidgetSettingsState(
            feedLayout = WidgetFeedLayout.CARD,
            fontScale = MAX_WIDGET_FONT_SCALE,
            backgroundOpacityPercent = 70,
            cardAppearance = WidgetCardAppearance(
                surfaceOpacityPercent = 0,
                cornerRadiusDp = 24,
                itemSeparation = WidgetCardItemSeparation.DIVIDER,
                dividerOpacityPercent = 65,
                imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            ),
        ),
        backdropMode = WidgetPreviewBackdropMode.DARK,
    )
}

@Preview(name = "Custom translucent card - light wallpaper")
@Composable
private fun WidgetPreviewCustomCardLightWallpaperPreview() {
    WidgetPreviewCustomCardToolingSurface(WidgetPreviewBackdropMode.LIGHT)
}

@Preview(name = "Custom translucent card - dark wallpaper")
@Composable
private fun WidgetPreviewCustomCardDarkWallpaperPreview() {
    WidgetPreviewCustomCardToolingSurface(WidgetPreviewBackdropMode.DARK)
}

@Composable
private fun WidgetPreviewCustomCardToolingSurface(backdropMode: WidgetPreviewBackdropMode) {
    WidgetPreviewToolingSurface(
        settingsState = WidgetSettingsState(
            feedLayout = WidgetFeedLayout.CARD,
            backgroundColor = 0xFF455A64.toInt(),
            backgroundOpacityPercent = 55,
            cardAppearance = WidgetCardAppearance(
                surfaceColor = 0xFFFFC107.toInt(),
                surfaceOpacityPercent = 45,
                cornerRadiusDp = 28,
                itemSeparation = WidgetCardItemSeparation.SPACING,
                imageSizing = WidgetCardImageSizing.THUMBNAIL,
            ),
        ),
        backdropMode = backdropMode,
    )
}

@Composable
private fun WidgetPreviewToolingSurface(
    settingsState: WidgetSettingsState,
    backdropMode: WidgetPreviewBackdropMode,
) {
    FeedFlowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val appearance = resolveWidgetPreviewAppearance(
                settingsState = settingsState,
                themedWidgetUnderlayColor = MaterialTheme.colorScheme.surface,
                themedCardSurfaceColor = MaterialTheme.colorScheme.secondaryContainer,
                themedOnSurfaceColor = MaterialTheme.colorScheme.onSurface,
            )
            WidgetPreviewWallpaper(
                headerTitle = settingsState.widgetContentTitle(),
                feedLayout = settingsState.feedLayout,
                freshness = settingsState.freshness,
                showWidgetHeader = settingsState.showHeader,
                showRefreshButton = settingsState.showRefreshButton,
                hideImages = settingsState.hideImages,
                fontSizes = widgetFontSizes(settingsState.fontScale),
                outerSurfaceColor = appearance.outerSurfaceColor,
                outerTextColors = appearance.outerTextColors,
                cardAppearance = appearance.cardAppearance,
                resolvedCardAppearance = appearance.card,
                backdropMode = backdropMode,
            )
        }
    }
}
