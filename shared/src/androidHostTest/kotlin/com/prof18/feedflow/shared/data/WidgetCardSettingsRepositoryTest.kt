package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.domain.model.WidgetFreshness
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WidgetCardSettingsRepositoryTest {

    @Test
    fun `absent freshness setting uses last three days default`() {
        val repository = WidgetSettingsRepository(MapSettings())

        assertEquals(WidgetFreshness.LAST_3_DAYS, repository.getWidgetConfiguration(WIDGET_ID).freshness)
    }

    @Test
    fun `freshness round trips and emits the update`() = runTest {
        val repository = WidgetSettingsRepository(MapSettings())

        repository.observeWidgetConfiguration(WIDGET_ID).map { it.freshness }.test {
            assertEquals(WidgetFreshness.LAST_3_DAYS, awaitItem())

            repository.setWidgetFreshness(WIDGET_ID, WidgetFreshness.LAST_7_DAYS)

            assertEquals(WidgetFreshness.LAST_7_DAYS, awaitItem())
            assertEquals(WidgetFreshness.LAST_7_DAYS, repository.getWidgetConfiguration(WIDGET_ID).freshness)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `freshness falls back safely for unknown and wrong type stored values`() {
        val unknownName = WidgetSettingsRepository(
            MapSettings().apply { this[WIDGET_FRESHNESS_KEY] = "LAST_30_DAYS" },
        )
        val wrongType = WidgetSettingsRepository(
            MapSettings().apply { this[WIDGET_FRESHNESS_KEY] = 3 },
        )

        assertEquals(WidgetFreshness.LAST_3_DAYS, unknownName.getWidgetConfiguration(WIDGET_ID).freshness)
        assertEquals(WidgetFreshness.LAST_3_DAYS, wrongType.getWidgetConfiguration(WIDGET_ID).freshness)
    }

    @Test
    fun `absent card appearance settings use defaults`() {
        val repository = WidgetSettingsRepository(MapSettings())

        assertEquals(WidgetCardAppearance(), repository.getWidgetConfiguration(WIDGET_ID).cardAppearance)
    }

    @Test
    fun `card appearance round trips every field and emits the update`() = runTest {
        val repository = WidgetSettingsRepository(MapSettings())
        val appearance = WidgetCardAppearance(
            surfaceColor = 0xFF123456.toInt(),
            surfaceOpacityPercent = 70,
            cornerRadiusDp = 24,
            itemSeparation = WidgetCardItemSeparation.DIVIDER,
            dividerOpacityPercent = 45,
            imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
        )

        repository.observeWidgetConfiguration(WIDGET_ID).map { it.cardAppearance }.test {
            assertEquals(WidgetCardAppearance(), awaitItem())

            repository.setWidgetCardAppearance(WIDGET_ID, appearance)

            assertEquals(appearance, awaitItem())
            assertEquals(appearance, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `null surface color removes the stored custom color`() {
        val settings = MapSettings()
        val repository = WidgetSettingsRepository(settings)
        repository.setWidgetCardAppearance(
            WIDGET_ID,
            WidgetCardAppearance(surfaceColor = 0xFF123456.toInt()),
        )

        repository.setWidgetCardAppearance(WIDGET_ID, WidgetCardAppearance(surfaceColor = null))

        assertNull(settings.getIntOrNull(WIDGET_CARD_SURFACE_COLOR_KEY))
        assertNull(repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.surfaceColor)
    }

    @Test
    fun `custom surface color is stored as opaque RGB`() {
        val settings = MapSettings()
        val repository = WidgetSettingsRepository(settings)

        repository.setWidgetCardAppearance(
            WIDGET_ID,
            WidgetCardAppearance(surfaceColor = 0x12123456),
        )

        assertEquals(0xFF123456.toInt(), settings.getIntOrNull(WIDGET_CARD_SURFACE_COLOR_KEY))
        assertEquals(0xFF123456.toInt(), repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.surfaceColor)
    }

    @Test
    fun `card appearance clamps opacities and rounds odd radius upward`() {
        val settings = MapSettings()
        val repository = WidgetSettingsRepository(settings)

        repository.setWidgetCardAppearance(
            WIDGET_ID,
            WidgetCardAppearance(
                surfaceOpacityPercent = 101,
                cornerRadiusDp = 17,
                dividerOpacityPercent = -1,
            ),
        )

        val expected = WidgetCardAppearance(
            surfaceOpacityPercent = 100,
            cornerRadiusDp = 18,
            dividerOpacityPercent = 0,
        )
        assertEquals(100, settings.getInt(WIDGET_CARD_SURFACE_OPACITY_PERCENT_KEY, -1))
        assertEquals(18, settings.getInt(WIDGET_CARD_CORNER_RADIUS_DP_KEY, -1))
        assertEquals(0, settings.getInt(WIDGET_CARD_DIVIDER_OPACITY_PERCENT_KEY, -1))
        assertEquals(expected, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance)
    }

    @Test
    fun `card appearance clamps radius and opacity at both boundaries`() {
        val repository = WidgetSettingsRepository(MapSettings())

        repository.setWidgetCardAppearance(
            WIDGET_ID,
            WidgetCardAppearance(
                surfaceOpacityPercent = -1,
                cornerRadiusDp = -1,
                dividerOpacityPercent = 101,
            ),
        )
        assertEquals(0, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.surfaceOpacityPercent)
        assertEquals(0, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.cornerRadiusDp)
        assertEquals(100, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.dividerOpacityPercent)

        repository.setWidgetCardAppearance(WIDGET_ID, WidgetCardAppearance(cornerRadiusDp = 33))
        assertEquals(32, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.cornerRadiusDp)
    }

    @Test
    fun `preloaded card appearance values are normalized on read and initial emission`() {
        val normalizedRadiusByStoredValue = mapOf(
            17 to 18,
            -1 to 0,
            33 to 32,
        )

        normalizedRadiusByStoredValue.forEach { (storedRadius, normalizedRadius) ->
            val settings = MapSettings().apply {
                this[WIDGET_CARD_SURFACE_COLOR_KEY] = 0x12123456
                this[WIDGET_CARD_SURFACE_OPACITY_PERCENT_KEY] = 101
                this[WIDGET_CARD_CORNER_RADIUS_DP_KEY] = storedRadius
                this[WIDGET_CARD_DIVIDER_OPACITY_PERCENT_KEY] = -1
            }
            val repository = WidgetSettingsRepository(settings)
            val expected = WidgetCardAppearance(
                surfaceColor = 0xFF123456.toInt(),
                surfaceOpacityPercent = 100,
                cornerRadiusDp = normalizedRadius,
                dividerOpacityPercent = 0,
            )

            assertEquals(expected, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance)
            assertEquals(expected, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance)
        }
    }

    @Test
    fun `competing card appearance writes keep persisted and emitted snapshots coherent`() = runTest {
        val firstAppearance = WidgetCardAppearance(
            surfaceColor = 0xFF112233.toInt(),
            surfaceOpacityPercent = 20,
            cornerRadiusDp = 10,
            itemSeparation = WidgetCardItemSeparation.SPACING,
            dividerOpacityPercent = 30,
            imageSizing = WidgetCardImageSizing.THUMBNAIL,
        )
        val secondAppearance = WidgetCardAppearance(
            surfaceColor = 0xFF445566.toInt(),
            surfaceOpacityPercent = 80,
            cornerRadiusDp = 28,
            itemSeparation = WidgetCardItemSeparation.NONE,
            dividerOpacityPercent = 70,
            imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
        )
        val settings = CoordinatedSettings(
            firstWriterColor = firstAppearance.surfaceColor,
            secondWriterImageSizing = secondAppearance.imageSizing.name,
        )
        val repository = WidgetSettingsRepository(settings)

        val firstWriter = launch(Dispatchers.Default) {
            repository.setWidgetCardAppearance(WIDGET_ID, firstAppearance)
        }
        assertTrue(settings.awaitFirstWriterColor())

        val secondWriter = launch(Dispatchers.Default) {
            repository.setWidgetCardAppearance(WIDGET_ID, secondAppearance)
        }
        val secondWriterInterleaved = settings.awaitSecondWriterFinalWrite(
            timeout = 1,
            unit = TimeUnit.SECONDS,
        )
        if (secondWriterInterleaved) {
            settings.releaseSecondWriter()
            secondWriter.join()
            settings.releaseFirstWriter()
        } else {
            settings.releaseFirstWriter()
            firstWriter.join()
            assertTrue(settings.awaitSecondWriterFinalWrite())
            settings.releaseSecondWriter()
        }
        firstWriter.join()
        secondWriter.join()

        assertEquals(secondAppearance, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance)
    }

    @Test
    fun `malformed card appearance enums fall back to defaults`() {
        val settings = MapSettings().apply {
            this[WIDGET_CARD_ITEM_SEPARATION_KEY] = "malformed"
            this[WIDGET_CARD_IMAGE_SIZING_KEY] = "malformed"
        }

        val repository = WidgetSettingsRepository(settings)

        assertEquals(
            WidgetCardItemSeparation.SPACING,
            repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.itemSeparation,
        )
        assertEquals(
            WidgetCardImageSizing.THUMBNAIL,
            repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.imageSizing,
        )
    }

    @Test
    fun `legacy appearance migrates without leaking per-widget overrides`() {
        val settings = MapSettings().apply {
            this["WIDGET_CARD_SURFACE_OPACITY_PERCENT"] = 0
            this["WIDGET_CARD_SURFACE_COLOR"] = 0xFF123456.toInt()
            this["WIDGET_CARD_ITEM_SEPARATION"] = "DIVIDER"
            this["WIDGET_FRESHNESS"] = "LAST_7_DAYS"
        }
        val repository = WidgetSettingsRepository(settings)
        assertEquals(0, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance.surfaceOpacityPercent)
        assertEquals(WidgetFreshness.LAST_7_DAYS, repository.getWidgetConfiguration(WIDGET_ID).freshness)
        repository.setWidgetCardAppearance(WIDGET_ID, WidgetCardAppearance())
        repository.setWidgetFreshness(WIDGET_ID, WidgetFreshness.LAST_24_HOURS)
        val reloaded = WidgetSettingsRepository(settings)
        assertEquals(WidgetCardAppearance(), reloaded.getWidgetConfiguration(WIDGET_ID).cardAppearance)
        assertEquals(0, reloaded.getWidgetConfiguration(84).cardAppearance.surfaceOpacityPercent)
        assertEquals(WidgetFreshness.LAST_7_DAYS, reloaded.getWidgetConfiguration(84).freshness)
        reloaded.restoreWidgetConfigurations(intArrayOf(WIDGET_ID), intArrayOf(100))
        assertEquals(WidgetCardAppearance(), reloaded.getWidgetConfiguration(100).cardAppearance)
        assertEquals(WidgetFreshness.LAST_24_HOURS, reloaded.getWidgetConfiguration(100).freshness)
        reloaded.clearWidgetConfiguration(100)
        assertEquals(0, reloaded.getWidgetConfiguration(100).cardAppearance.surfaceOpacityPercent)
    }

    @Test
    fun `restore publishes card appearance and content as one complete configuration`() = runTest {
        val repository = WidgetSettingsRepository(MapSettings())
        repository.setWidgetContentFilter(WIDGET_ID, com.prof18.feedflow.core.model.WidgetContentFilter.Bookmarks)
        repository.setWidgetFreshness(WIDGET_ID, WidgetFreshness.LAST_7_DAYS)
        repository.setWidgetCardAppearance(WIDGET_ID, WidgetCardAppearance(surfaceOpacityPercent = 0))
        val expected = repository.getWidgetConfiguration(WIDGET_ID)

        repository.observeWidgetConfiguration(100).test {
            awaitItem()
            repository.restoreWidgetConfigurations(intArrayOf(WIDGET_ID), intArrayOf(100))
            assertEquals(expected, awaitItem())
            expectNoEvents()
        }
    }

    private class CoordinatedSettings(
        private val firstWriterColor: Int?,
        private val secondWriterImageSizing: String,
        private val delegate: Settings = MapSettings(),
    ) : Settings by delegate {
        private val firstWriterColorStored = CountDownLatch(1)
        private val releaseFirstWriterLatch = CountDownLatch(1)
        private val secondWriterFinalWriteStored = CountDownLatch(1)
        private val releaseSecondWriterLatch = CountDownLatch(1)

        override fun putInt(key: String, value: Int) {
            delegate.putInt(key, value)
            if (key == WIDGET_CARD_SURFACE_COLOR_KEY && value == firstWriterColor) {
                firstWriterColorStored.countDown()
                check(releaseFirstWriterLatch.await(5, TimeUnit.SECONDS))
            }
        }

        override fun putString(key: String, value: String) {
            delegate.putString(key, value)
            if (key == WIDGET_CARD_IMAGE_SIZING_KEY && value == secondWriterImageSizing) {
                secondWriterFinalWriteStored.countDown()
                check(releaseSecondWriterLatch.await(5, TimeUnit.SECONDS))
            }
        }

        fun awaitFirstWriterColor(): Boolean =
            firstWriterColorStored.await(5, TimeUnit.SECONDS)

        fun awaitSecondWriterFinalWrite(
            timeout: Long = 5,
            unit: TimeUnit = TimeUnit.SECONDS,
        ): Boolean = secondWriterFinalWriteStored.await(timeout, unit)

        fun releaseFirstWriter() {
            releaseFirstWriterLatch.countDown()
        }

        fun releaseSecondWriter() {
            releaseSecondWriterLatch.countDown()
        }
    }

    private companion object {
        const val WIDGET_ID = 42
        const val WIDGET_FRESHNESS_KEY = "WIDGET_FRESHNESS_42"
        const val WIDGET_CARD_SURFACE_COLOR_KEY = "WIDGET_CARD_SURFACE_COLOR_42"
        const val WIDGET_CARD_SURFACE_OPACITY_PERCENT_KEY = "WIDGET_CARD_SURFACE_OPACITY_PERCENT_42"
        const val WIDGET_CARD_CORNER_RADIUS_DP_KEY = "WIDGET_CARD_CORNER_RADIUS_DP_42"
        const val WIDGET_CARD_ITEM_SEPARATION_KEY = "WIDGET_CARD_ITEM_SEPARATION_42"
        const val WIDGET_CARD_DIVIDER_OPACITY_PERCENT_KEY = "WIDGET_CARD_DIVIDER_OPACITY_PERCENT_42"
        const val WIDGET_CARD_IMAGE_SIZING_KEY = "WIDGET_CARD_IMAGE_SIZING_42"
    }
}
