package com.prof18.feedflow.android.widget

import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.domain.model.WidgetFreshness
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [30], application = android.app.Application::class)
class WidgetSettingsViewModelTest {
    private val viewModels = androidx.lifecycle.ViewModelStore()
    private val drivers = mutableListOf<app.cash.sqldelight.db.SqlDriver>()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        viewModels.clear()
        drivers.forEach { it.close() }
        Dispatchers.resetMain()
    }

    @Test
    fun `effective freshness change persists and updates widget exactly once`() = runTest(dispatcher) {
        val settings = CountingWidgetSettings()
        val repository = WidgetSettingsRepository(settings)
        val widgetUpdater = FakeWidgetUpdater()
        val viewModel = newViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
            widgetUpdater = widgetUpdater,
        )
        advanceUntilIdle()

        viewModel.updateWidgetFreshness(WidgetFreshness.LAST_24_HOURS)

        assertEquals(WidgetFreshness.LAST_24_HOURS, viewModel.settingsState.value.freshness)
        assertEquals(WidgetFreshness.LAST_24_HOURS, repository.getWidgetConfiguration(WIDGET_ID).freshness)
        assertEquals(1, settings.freshnessWriteCount)
        assertEquals(0, widgetUpdater.updateCount)

        advanceUntilIdle()

        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `unchanged freshness call neither persists nor updates widget`() = runTest(dispatcher) {
        val settings = CountingWidgetSettings()
        val repository = WidgetSettingsRepository(settings)
        val widgetUpdater = FakeWidgetUpdater()
        val viewModel = newViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
            widgetUpdater = widgetUpdater,
        )
        advanceUntilIdle()

        viewModel.updateWidgetFreshness(WidgetFreshness.LAST_3_DAYS)
        advanceUntilIdle()

        assertEquals(WidgetFreshness.LAST_3_DAYS, viewModel.settingsState.value.freshness)
        assertEquals(0, settings.freshnessWriteCount)
        assertEquals(0, widgetUpdater.updateCount)
    }

    @Test
    fun `freshness guard uses synchronous repository value before presentation collection`() = runTest(dispatcher) {
        val settings = CountingWidgetSettings()
        val repository = WidgetSettingsRepository(settings)
        val widgetUpdater = FakeWidgetUpdater()
        val viewModel = newViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
            widgetUpdater = widgetUpdater,
        )
        advanceUntilIdle()

        viewModel.updateWidgetFreshness(WidgetFreshness.LAST_7_DAYS)
        viewModel.updateWidgetFreshness(WidgetFreshness.LAST_7_DAYS)

        assertEquals(WidgetFreshness.LAST_7_DAYS, repository.getWidgetConfiguration(WIDGET_ID).freshness)
        assertEquals(WidgetFreshness.LAST_7_DAYS, viewModel.settingsState.value.freshness)
        assertEquals(1, settings.freshnessWriteCount)

        advanceUntilIdle()

        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `card appearance callbacks persist optimistic state and update widget once each`() = runTest(dispatcher) {
        val repository = WidgetSettingsRepository(MapSettings())
        val widgetUpdater = FakeWidgetUpdater()
        val viewModel = newViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
            widgetUpdater = widgetUpdater,
        )
        advanceUntilIdle()

        var expectedAppearance = WidgetCardAppearance()
        val updates = listOf<(WidgetInstanceSettingsViewModel) -> WidgetCardAppearance>(
            { currentViewModel ->
                currentViewModel.updateCardSurfaceColor(CUSTOM_COLOR_WITHOUT_ALPHA)
                expectedAppearance.copy(surfaceColor = CUSTOM_COLOR)
            },
            { currentViewModel ->
                currentViewModel.updateCardSurfaceOpacityPercent(CUSTOM_OPACITY_PERCENT)
                expectedAppearance.copy(surfaceOpacityPercent = CUSTOM_OPACITY_PERCENT)
            },
            { currentViewModel ->
                currentViewModel.updateCardCornerRadiusDp(ODD_CORNER_RADIUS_DP)
                expectedAppearance.copy(cornerRadiusDp = NORMALIZED_CORNER_RADIUS_DP)
            },
            { currentViewModel ->
                currentViewModel.updateCardItemSeparation(WidgetCardItemSeparation.DIVIDER)
                expectedAppearance.copy(itemSeparation = WidgetCardItemSeparation.DIVIDER)
            },
            { currentViewModel ->
                currentViewModel.updateCardDividerOpacityPercent(CUSTOM_DIVIDER_OPACITY_PERCENT)
                expectedAppearance.copy(dividerOpacityPercent = CUSTOM_DIVIDER_OPACITY_PERCENT)
            },
            { currentViewModel ->
                currentViewModel.updateCardImageSizing(WidgetCardImageSizing.FILL_ROW_HEIGHT)
                expectedAppearance.copy(imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT)
            },
        )

        updates.forEachIndexed { index, update ->
            expectedAppearance = update(viewModel)

            assertEquals(expectedAppearance, viewModel.settingsState.value.cardAppearance)
            assertEquals(expectedAppearance, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance)
            assertEquals(index, widgetUpdater.updateCount)

            advanceUntilIdle()

            assertEquals(index + 1, widgetUpdater.updateCount)
        }
    }

    @Test
    fun `equivalent card appearance callbacks do not update widget`() = runTest(dispatcher) {
        val initialAppearance = WidgetCardAppearance(
            surfaceColor = CUSTOM_COLOR,
            dividerOpacityPercent = MAX_PERCENT,
        )
        val repository = WidgetSettingsRepository(MapSettings()).apply {
            setWidgetCardAppearance(WIDGET_ID, initialAppearance)
        }
        val widgetUpdater = FakeWidgetUpdater()
        val viewModel = newViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
            widgetUpdater = widgetUpdater,
        )
        advanceUntilIdle()

        viewModel.updateCardSurfaceColor(CUSTOM_COLOR_WITHOUT_ALPHA)
        viewModel.updateCardSurfaceOpacityPercent(ABOVE_MAX_PERCENT)
        viewModel.updateCardCornerRadiusDp(ODD_DEFAULT_CORNER_RADIUS_DP)
        viewModel.updateCardItemSeparation(WidgetCardItemSeparation.SPACING)
        viewModel.updateCardDividerOpacityPercent(ABOVE_MAX_PERCENT)
        viewModel.updateCardImageSizing(WidgetCardImageSizing.THUMBNAIL)
        advanceUntilIdle()

        assertEquals(initialAppearance, viewModel.settingsState.value.cardAppearance)
        assertEquals(initialAppearance, repository.getWidgetConfiguration(WIDGET_ID).cardAppearance)
        assertEquals(0, widgetUpdater.updateCount)
    }

    private fun newViewModel(
        settingsRepository: SettingsRepository,
        widgetSettingsRepository: WidgetSettingsRepository,
        widgetUpdater: WidgetUpdater,
    ): WidgetInstanceSettingsViewModel {
        val driver = app.cash.sqldelight.driver.android.AndroidSqliteDriver(
            schema = com.prof18.feedflow.db.FeedFlowDB.Schema,
            context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            name = null,
        )
        drivers.add(driver)
        val logger = co.touchlab.kermit.Logger
        val database = com.prof18.feedflow.database.DatabaseHelper(driver, dispatcher, logger)
        val constructor = com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository::class.java.declaredConstructors
            .single()
        constructor.isAccessible = true
        val feeds = constructor.newInstance(
            database,
            com.prof18.feedflow.shared.domain.DateFormatterImpl(logger, kotlin.time.Clock.System),
            com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository(MapSettings()),
        ) as com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
        return WidgetInstanceSettingsViewModel(
            appWidgetId = WIDGET_ID,
            settingsRepository = settingsRepository,
            widgetSettingsRepository = widgetSettingsRepository,
            feedWidgetRepository = feeds,
            widgetUpdater = widgetUpdater,
        ).also { viewModels.put("widget-" + drivers.size, it) }
    }

    @Test
    fun `reopening configuration preserves appearance without affecting another widget`() = runTest(dispatcher) {
        val repository = WidgetSettingsRepository(MapSettings())
        val viewModel = newViewModel(SettingsRepository(MapSettings()), repository, FakeWidgetUpdater())
        viewModel.updateCardSurfaceOpacityPercent(0)
        viewModel.updateWidgetFreshness(WidgetFreshness.LAST_7_DAYS)
        viewModel.reloadWidgetConfiguration()
        assertEquals(0, viewModel.settingsState.value.cardAppearance.surfaceOpacityPercent)
        assertEquals(WidgetFreshness.LAST_7_DAYS, viewModel.settingsState.value.freshness)
        assertEquals(WidgetCardAppearance(), repository.getWidgetConfiguration(84).cardAppearance)
        advanceUntilIdle()
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var updateCount = 0
            private set

        override suspend fun update() {
            updateCount += 1
        }
    }
}

private class CountingWidgetSettings(
    private val delegate: Settings = MapSettings(),
) : Settings by delegate {
    var freshnessWriteCount = 0
        private set

    override fun putString(key: String, value: String) {
        delegate.putString(key, value)
        if (key == WIDGET_FRESHNESS_KEY) {
            freshnessWriteCount += 1
        }
    }
}

private const val WIDGET_ID = 42
private const val WIDGET_FRESHNESS_KEY = "WIDGET_FRESHNESS_42"
private const val CUSTOM_COLOR_WITHOUT_ALPHA = 0x00123456
private const val CUSTOM_COLOR = 0xFF123456.toInt()
private const val CUSTOM_OPACITY_PERCENT = 72
private const val CUSTOM_DIVIDER_OPACITY_PERCENT = 48
private const val ODD_CORNER_RADIUS_DP = 17
private const val NORMALIZED_CORNER_RADIUS_DP = 18
private const val ODD_DEFAULT_CORNER_RADIUS_DP = 15
private const val MAX_PERCENT = 100
private const val ABOVE_MAX_PERCENT = 101
