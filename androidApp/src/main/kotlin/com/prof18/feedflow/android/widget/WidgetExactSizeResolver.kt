package com.prof18.feedflow.android.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.SizeF
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

private const val ANDROID_12_SDK = 31

internal fun resolveExactSizes(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
    sdkInt: Int,
): WidgetExactSizeResolution {
    val sizes = if (sdkInt >= ANDROID_12_SDK) {
        resolveApi31Sizes(snapshot, currentSize)
    } else {
        resolveLegacySizes(snapshot, currentSize)
    }
    val stableKey = buildStableExactSizeKey(
        snapshot = snapshot,
        currentSize = currentSize,
        sdkBranch = if (sdkInt >= ANDROID_12_SDK) "api31+" else "api26-30",
        resolvedSizes = sizes,
    )
    return WidgetExactSizeResolution(
        stableKey = stableKey,
        sizes = sizes,
    )
}

private fun resolveApi31Sizes(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
): List<DpSize> {
    val explicitSizes = snapshot.explicitSizes
        .orEmpty()
        .filter(WidgetExactSize::isUsable)
        .distinct()
        .map(WidgetExactSize::toDpSize)
    if (explicitSizes.isNotEmpty()) {
        return explicitSizes
    }

    val portraitSize = legacySize(
        widthDp = snapshot.minWidthDp,
        heightDp = snapshot.maxHeightDp,
    )
    val landscapeSize = legacySize(
        widthDp = snapshot.maxWidthDp,
        heightDp = snapshot.minHeightDp,
    )
    if (portraitSize != null && landscapeSize != null) {
        return listOf(portraitSize, landscapeSize).distinct()
    }

    return listOf(currentSize)
}

private fun resolveLegacySizes(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
): List<DpSize> {
    val candidates = listOfNotNull(
        legacySize(
            widthDp = snapshot.minWidthDp,
            heightDp = snapshot.maxHeightDp,
        ),
        legacySize(
            widthDp = snapshot.maxWidthDp,
            heightDp = snapshot.minHeightDp,
        ),
    ).distinct()
    return candidates.ifEmpty { listOf(currentSize) }
}

private fun buildStableExactSizeKey(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
    sdkBranch: String,
    resolvedSizes: List<DpSize>,
): String = buildString {
    append("widget-exact-sizes-v1|")
    append(sdkBranch)
    append("|explicit=")
    val explicitSizes = snapshot.explicitSizes
    if (explicitSizes == null) {
        append("absent")
    } else {
        explicitSizes.joinTo(this, separator = ",") { size -> size.stableValue() }
    }
    append("|legacy=")
    append(snapshot.minWidthDp)
    append(',')
    append(snapshot.maxWidthDp)
    append(',')
    append(snapshot.minHeightDp)
    append(',')
    append(snapshot.maxHeightDp)
    append("|current=")
    append(currentSize.stableValue())
    append("|resolved=")
    resolvedSizes.joinTo(this, separator = ",") { size -> size.stableValue() }
}

private fun legacySize(widthDp: Int?, heightDp: Int?): DpSize? {
    if (widthDp == null || widthDp <= 0 || heightDp == null || heightDp <= 0) {
        return null
    }
    return DpSize(widthDp.dp, heightDp.dp)
}

private fun WidgetExactSize.isUsable(): Boolean =
    widthDp.isFinite() && widthDp > 0f && heightDp.isFinite() && heightDp > 0f

private fun WidgetExactSize.toDpSize(): DpSize = DpSize(widthDp.dp, heightDp.dp)

private fun WidgetExactSize.stableValue(): String =
    "${widthDp.toRawBits()}:${heightDp.toRawBits()}"

private fun DpSize.stableValue(): String =
    "${width.value.toRawBits()}:${height.value.toRawBits()}"

internal data class WidgetExactSizeResolution(
    val stableKey: String,
    val sizes: List<DpSize>,
) {
    val variantCount: Int
        get() = sizes.size
}

internal data class WidgetOptionsSnapshot(
    val explicitSizes: List<WidgetExactSize>?,
    val minWidthDp: Int?,
    val maxWidthDp: Int?,
    val minHeightDp: Int?,
    val maxHeightDp: Int?,
) {
    companion object {
        @Suppress("DEPRECATION")
        fun fromBundle(bundle: Bundle): WidgetOptionsSnapshot {
            val explicitSizes = if (bundle.containsKey(AppWidgetManager.OPTION_APPWIDGET_SIZES)) {
                bundle.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                    .orEmpty()
                    .map { size -> WidgetExactSize(size.width, size.height) }
            } else {
                null
            }
            return WidgetOptionsSnapshot(
                explicitSizes = explicitSizes,
                minWidthDp = bundle.optionalInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                maxWidthDp = bundle.optionalInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                minHeightDp = bundle.optionalInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                maxHeightDp = bundle.optionalInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
            )
        }
    }
}

internal data class WidgetExactSize(
    val widthDp: Float,
    val heightDp: Float,
)

private fun Bundle.optionalInt(key: String): Int? = if (containsKey(key)) getInt(key) else null
