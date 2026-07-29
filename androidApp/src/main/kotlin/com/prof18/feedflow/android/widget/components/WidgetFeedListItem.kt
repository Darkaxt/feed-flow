package com.prof18.feedflow.android.widget.components

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.widget.ResolvedWidgetCardAppearance
import com.prof18.feedflow.android.widget.ResolvedWidgetCardLayout
import com.prof18.feedflow.android.widget.WIDGET_THUMBNAIL_VIEWPORT_DP
import com.prof18.feedflow.android.widget.WidgetFontSizes
import com.prof18.feedflow.android.widget.WidgetImageBudgetPolicy
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.ReaderModeEligibility
import com.prof18.feedflow.core.model.isReaderMode
import com.prof18.feedflow.core.model.resolveWith
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.ui.style.Spacing

private const val THUMBNAIL_CORNER_RADIUS_DP = 8

@Composable
internal fun WidgetFeedItemList(
    feedItem: FeedItem,
    browserManager: BrowserManager,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current.applicationContext
    val clickAction = createFeedItemClickAction(feedItem, context, browserManager)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(clickAction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThumbnailContent(
            feedItem = feedItem,
            fontSizes = fontSizes,
            hideImages = hideImages,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            imageBudgetPolicy = imageBudgetPolicy,
            imageDisplayTargetPx = imageDisplayTargetPx,
        )
    }
}

@Composable
internal fun WidgetFeedItemCard(
    feedItem: FeedItem,
    browserManager: BrowserManager,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    appearance: WidgetCardAppearance,
    resolvedAppearance: ResolvedWidgetCardAppearance,
    cardLayout: ResolvedWidgetCardLayout,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current.applicationContext
    val clickAction = createFeedItemClickAction(feedItem, context, browserManager)

    if (appearance.itemSeparation == WidgetCardItemSeparation.SPACING) {
        Box(
            modifier = modifier
                .padding(vertical = Spacing.xsmall),
        ) {
            WidgetFeedItemCardSlab(
                feedItem = feedItem,
                fontSizes = fontSizes,
                hideImages = hideImages,
                appearance = appearance,
                resolvedAppearance = resolvedAppearance,
                cardLayout = cardLayout,
                imageBudgetPolicy = imageBudgetPolicy,
                imageDisplayTargetPx = imageDisplayTargetPx,
                clickAction = clickAction,
            )
        }
    } else {
        WidgetFeedItemCardSlab(
            feedItem = feedItem,
            fontSizes = fontSizes,
            hideImages = hideImages,
            appearance = appearance,
            resolvedAppearance = resolvedAppearance,
            cardLayout = cardLayout,
            imageBudgetPolicy = imageBudgetPolicy,
            imageDisplayTargetPx = imageDisplayTargetPx,
            clickAction = clickAction,
            modifier = modifier,
        )
    }
}

@Composable
private fun WidgetFeedItemCardSlab(
    feedItem: FeedItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    appearance: WidgetCardAppearance,
    resolvedAppearance: ResolvedWidgetCardAppearance,
    cardLayout: ResolvedWidgetCardLayout,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    clickAction: Action,
    modifier: GlanceModifier = GlanceModifier,
) {
    val cornerRadius = appearance.cornerRadiusDp.dp
    when (cardLayout.imageSizing) {
        WidgetCardImageSizing.THUMBNAIL -> {
            var slabModifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .cornerRadius(cornerRadius)
            resolvedAppearance.slabFillColor?.let { slabColor ->
                slabModifier = slabModifier.background(ColorProvider(slabColor))
            }
            slabModifier = slabModifier.clickable(clickAction)

            Row(
                modifier = slabModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThumbnailContent(
                    feedItem = feedItem,
                    fontSizes = fontSizes,
                    hideImages = hideImages,
                    primaryTextColor = ColorProvider(resolvedAppearance.textColors.primary),
                    secondaryTextColor = ColorProvider(resolvedAppearance.textColors.secondary),
                    imageBudgetPolicy = imageBudgetPolicy,
                    imageDisplayTargetPx = imageDisplayTargetPx,
                )
            }
        }
        WidgetCardImageSizing.FILL_ROW_HEIGHT -> {
            val rowHeight = requireNotNull(cardLayout.fixedRowHeightDp).dp
            var slabModifier = modifier
                .fillMaxWidth()
                .height(rowHeight)
                .cornerRadius(cornerRadius)
            resolvedAppearance.slabFillColor?.let { slabColor ->
                slabModifier = slabModifier.background(ColorProvider(slabColor))
            }
            slabModifier = slabModifier.clickable(clickAction)

            Row(
                modifier = slabModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FillContent(
                    feedItem = feedItem,
                    fontSizes = fontSizes,
                    hideImages = hideImages,
                    primaryTextColor = ColorProvider(resolvedAppearance.textColors.primary),
                    secondaryTextColor = ColorProvider(resolvedAppearance.textColors.secondary),
                    imageBudgetPolicy = imageBudgetPolicy,
                    imageDisplayTargetPx = imageDisplayTargetPx,
                    rowHeightDp = cardLayout.imageViewportDp,
                    cornerRadiusDp = appearance.cornerRadiusDp,
                )
            }
        }
    }
}

@Composable
private fun RowScope.ThumbnailContent(
    feedItem: FeedItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    val textModifier = modifier.defaultWeight()

    Column(
        modifier = textModifier
            .padding(end = Spacing.regular),
    ) {
        val fontStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = fontSizes.meta.sp,
            color = secondaryTextColor,
        )

        Row {
            Text(
                text = feedItem.feedSource.title,
                style = fontStyle,
            )
        }
        Text(
            text = feedItem.title.orEmpty(),
            maxLines = 2,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = fontSizes.title.sp,
                color = primaryTextColor,
            ),
        )

        feedItem.dateString?.let { dateString ->
            Text(
                modifier = GlanceModifier.padding(top = Spacing.xsmall),
                text = dateString,
                style = fontStyle,
            )
        }
    }

    ArticleImageIfAvailable(
        feedItem = feedItem,
        hideImages = hideImages,
        imageBudgetPolicy = imageBudgetPolicy,
        imageDisplayTargetPx = imageDisplayTargetPx,
        displayViewportDp = WIDGET_THUMBNAIL_VIEWPORT_DP,
        cornerRadiusDp = THUMBNAIL_CORNER_RADIUS_DP,
    )
}

@Composable
private fun RowScope.FillContent(
    feedItem: FeedItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    rowHeightDp: Int,
    cornerRadiusDp: Int,
) {
    val fontStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = fontSizes.meta.sp,
        color = secondaryTextColor,
    )
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .padding(16.dp),
    ) {
        Text(
            text = feedItem.feedSource.title,
            maxLines = 1,
            style = fontStyle,
        )
        Text(
            text = feedItem.title.orEmpty(),
            maxLines = 2,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = fontSizes.title.sp,
                color = primaryTextColor,
            ),
        )
        feedItem.dateString?.let { dateString ->
            Text(
                modifier = GlanceModifier.padding(top = Spacing.xsmall),
                text = dateString,
                maxLines = 1,
                style = fontStyle,
            )
        }
    }

    ArticleImageIfAvailable(
        feedItem = feedItem,
        hideImages = hideImages,
        imageBudgetPolicy = imageBudgetPolicy,
        imageDisplayTargetPx = imageDisplayTargetPx,
        displayViewportDp = rowHeightDp,
        cornerRadiusDp = cornerRadiusDp,
    )
}

@Composable
private fun ArticleImageIfAvailable(
    feedItem: FeedItem,
    hideImages: Boolean,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    displayViewportDp: Int,
    cornerRadiusDp: Int,
) {
    if (hideImages) {
        return
    }
    val imageUrl = feedItem.imageUrl?.takeIf(String::isNotBlank) ?: return
    WidgetArticleImage(
        requestPolicy = imageBudgetPolicy.resolveRequest(
            imageUrl = imageUrl,
            displayTargetPx = imageDisplayTargetPx,
        ),
        displayViewportDp = displayViewportDp.dp,
        cornerRadiusDp = cornerRadiusDp.dp,
    )
}

private fun createFeedItemClickAction(
    feedItem: FeedItem,
    context: Context,
    browserManager: BrowserManager,
): Action {
    // URL-less items can only be shown in the reader; deep-link into the app.
    if (feedItem.url.isBlank()) {
        return createDeepLinkAction(feedItem, context)
    }
    val openMode = feedItem.feedSource.articleOpenMode.resolveWith(browserManager.getArticleOpenMode())
    return if (openMode.isReaderMode() && feedItem.canOpenWebReaderMode()) {
        createDeepLinkAction(feedItem, context)
    } else {
        createBrowserAction(feedItem, browserManager)
    }
}

private fun createBrowserAction(feedItem: FeedItem, browserManager: BrowserManager): Action {
    val intent = Intent(Intent.ACTION_VIEW, feedItem.url.toUri()).apply {
        browserManager.getBrowserPackageNameWithoutInApp()?.let { packageName ->
            setPackage(packageName)
        }
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    return actionStartActivity(intent)
}

private fun createDeepLinkAction(feedItem: FeedItem, context: Context): Action {
    return actionStartActivity(
        Intent(
            context,
            MainActivity::class.java,
        )
            .setAction(Intent.ACTION_VIEW)
            .setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            .setData("feedflow://feed/${feedItem.id}".toUri()),
    )
}

private fun FeedItem.canOpenWebReaderMode(): Boolean =
    ReaderModeEligibility.canOpenReaderMode(url)
