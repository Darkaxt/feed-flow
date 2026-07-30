package com.prof18.feedflow.android.widget

import android.content.Context
import android.graphics.Outline
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 30])
class WidgetCardSlabRemoteViewsTest {

    @Test
    fun `pre S slab layouts declare whole-root outline clipping`() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources

        listOf(
            R.layout.widget_card_slab_themed,
            R.layout.widget_card_slab_resolved,
        ).forEach { layoutResource ->
            resources.getLayout(layoutResource).use { parser ->
                while (parser.next() != XmlPullParser.START_TAG) {
                    // Advance to the root element.
                }
                assertTrue(
                    parser.getAttributeBooleanValue(
                        "http://schemas.android.com/apk/res/android",
                        "clipToOutline",
                        false,
                    ),
                )
            }
        }
    }

    @Test
    fun `mapped outline clips the complete pre S slab at every normalized radius`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        (0..32 step 2).forEach { radiusDp ->
            val slab = inflateSlab(
                context = context,
                radiusDp = radiusDp,
                colorSource = WidgetColorProviderSource.THEMED,
                resolvedSlabFillColor = null,
            )
            slab.measure(exactly(1_000), exactly(1_000))
            slab.layout(0, 0, 1_000, 1_000)
            // Robolectric does not hydrate View's clipToOutline XML attribute on these SDKs.
            slab.clipToOutline = true
            val outline = Outline()
            slab.outlineProvider.getOutline(slab, outline)

            assertTrue(slab.clipToOutline)
            assertTrue(outline.canClip())
            val expectedRadiusPx = radiusDp * context.resources.displayMetrics.density
            assertEquals(
                expectedRadiusPx,
                (slab.background as GradientDrawable).cornerRadius,
                0.01f,
            )
            assertSame(slab, slab.findViewById<View>(R.id.widget_card_slab_content).parent)
        }
    }

    @Test
    fun `themed pre S slab keeps a resource-aware day-night surface`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val originalQualifiers = RuntimeEnvironment.getQualifiers()
        val dayTint: Int?
        val nightTint: Int?
        try {
            RuntimeEnvironment.setQualifiers("+notnight")
            dayTint = inflateSlabBackground(
                context = context,
                colorSource = WidgetColorProviderSource.THEMED,
                resolvedSlabFillColor = null,
            ).imageTintList?.defaultColor
            RuntimeEnvironment.setQualifiers("+night")
            nightTint = inflateSlabBackground(
                context = context,
                colorSource = WidgetColorProviderSource.THEMED,
                resolvedSlabFillColor = null,
            ).imageTintList?.defaultColor
        } finally {
            RuntimeEnvironment.setQualifiers(originalQualifiers)
        }

        assertNotNull(dayTint)
        assertNotNull(nightTint)
        assertNotEquals(dayTint, nightTint)
    }

    @Test
    fun `resolved pre S slab keeps configured surface opacity`() {
        val background = inflateSlabBackground(
            context = ApplicationProvider.getApplicationContext(),
            colorSource = WidgetColorProviderSource.RESOLVED,
            resolvedSlabFillColor = Color(0x59445566),
        )

        assertEquals(89, background.imageAlpha)
        val colorFilter = background.colorFilter as PorterDuffColorFilter
        assertEquals(0xFF445566.toInt(), shadowOf(colorFilter).color)
        assertNull(background.imageTintList)
    }

    private fun inflateSlab(
        context: Context,
        radiusDp: Int,
        colorSource: WidgetColorProviderSource,
        resolvedSlabFillColor: Color?,
    ): FrameLayout {
        val root = createPreSWidgetCardSlabRemoteViews(
            context = context,
            cornerRadiusDp = radiusDp,
            colorSource = colorSource,
            resolvedSlabFillColor = resolvedSlabFillColor,
        ).apply(context, FrameLayout(context))

        return requireNotNull(root.findViewById(R.id.widget_card_slab_root))
    }

    private fun inflateSlabBackground(
        context: Context,
        colorSource: WidgetColorProviderSource,
        resolvedSlabFillColor: Color?,
    ): ImageView = requireNotNull(
        inflateSlab(
            context = context,
            radiusDp = 16,
            colorSource = colorSource,
            resolvedSlabFillColor = resolvedSlabFillColor,
        ).findViewById(R.id.widget_card_slab_background),
    )

    private fun exactly(size: Int): Int = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
}
