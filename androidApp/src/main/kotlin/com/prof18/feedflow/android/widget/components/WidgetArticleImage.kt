package com.prof18.feedflow.android.widget.components

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.size.Precision
import coil3.size.Scale
import com.prof18.feedflow.android.widget.WidgetImageRequestIdentity
import com.prof18.feedflow.android.widget.WidgetImageRequestPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sqrt

@Composable
internal fun WidgetArticleImage(
    requestPolicy: WidgetImageRequestPolicy?,
    displayViewportDp: Dp,
    cornerRadiusDp: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val fullKey = requestPolicy?.identity
    val currentFullKey by rememberUpdatedState(fullKey)
    var loadedBitmap by remember(fullKey) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(fullKey) {
        val requestedKey = fullKey ?: return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            loadWidgetArticleBitmap(
                context = context,
                resources = context.resources,
                key = requestedKey,
            )
        }
        if (currentFullKey == requestedKey) {
            loadedBitmap = result
        }
    }

    loadedBitmap?.let { bitmap ->
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(displayViewportDp)
                .cornerRadius(cornerRadiusDp),
        )
    }
}

internal fun buildWidgetArticleImageRequest(
    context: Context,
    key: WidgetImageRequestIdentity,
): ImageRequest = ImageRequest.Builder(context)
    .data(key.imageUrl)
    .size(key.edgePx, key.edgePx)
    .precision(Precision.EXACT)
    .scale(Scale.FILL)
    .allowHardware(false)
    .bitmapConfig(Bitmap.Config.ARGB_8888)
    .build()

private suspend fun loadWidgetArticleBitmap(
    context: Context,
    resources: Resources,
    key: WidgetImageRequestIdentity,
): Bitmap? = try {
    val result = context.imageLoader.execute(buildWidgetArticleImageRequest(context, key))
    val deliveredBitmap = (result as? SuccessResult)
        ?.image
        ?.asDrawable(resources)
        ?.toBitmapOrNull()
        ?: return null
    WidgetBitmapValidator.validate(
        bitmap = deliveredBitmap,
        requestEdgePx = key.edgePx,
        payloadBudgetBytes = key.payloadBudgetBytes,
    )
} catch (exception: CancellationException) {
    throw exception
} catch (_: Exception) {
    null
}

internal object WidgetBitmapValidator {

    fun validate(
        bitmap: Bitmap,
        requestEdgePx: Int,
        payloadBudgetBytes: Long,
    ): Bitmap? = try {
        validateOrNull(
            bitmap = bitmap,
            requestEdgePx = requestEdgePx,
            payloadBudgetBytes = payloadBudgetBytes,
        )
    } catch (_: Exception) {
        null
    }

    private fun validateOrNull(
        bitmap: Bitmap,
        requestEdgePx: Int,
        payloadBudgetBytes: Long,
    ): Bitmap? {
        if (bitmap.isRecycled || requestEdgePx < 1 || payloadBudgetBytes < 1L) {
            return null
        }
        var candidate = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        }
        val currentEdgePx = max(candidate.width, candidate.height)
        if (currentEdgePx > requestEdgePx) {
            candidate = rerasterizeToEdge(candidate, requestEdgePx)
        }
        val allocationByteCount = candidate.allocationByteCount.toLong()
        if (allocationByteCount > payloadBudgetBytes) {
            val candidateEdgePx = max(candidate.width, candidate.height)
            val reductionScale = sqrt(payloadBudgetBytes.toDouble() / allocationByteCount.toDouble())
            val reducedEdgePx = (candidateEdgePx * reductionScale)
                .toInt()
                .coerceAtMost(candidateEdgePx - 1)
            if (reducedEdgePx < 1) {
                return null
            }
            candidate = rerasterizeToEdge(candidate, reducedEdgePx)
        }
        return candidate.takeIf {
            it.config == Bitmap.Config.ARGB_8888 &&
                it.config != Bitmap.Config.HARDWARE &&
                it.width <= requestEdgePx &&
                it.height <= requestEdgePx &&
                it.allocationByteCount.toLong() <= payloadBudgetBytes
        }
    }

    private fun rerasterizeToEdge(bitmap: Bitmap, targetEdgePx: Int): Bitmap {
        val currentEdgePx = max(bitmap.width, bitmap.height)
        val scale = targetEdgePx.toDouble() / currentEdgePx.toDouble()
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
