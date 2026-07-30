# Configurable Widget Card Appearance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add configurable Android widget Card slabs and safe Thumbnail/Fill-row-height article images while preserving the current default Card and List appearance.

**Architecture:** Persist one normalized Android-only `WidgetCardAppearance`, resolve colors and geometry through pure Android helpers, and pass one composition-wide Exact-size image policy to both List and Card renderers. Budget every Exact-size variant against the fixed 15-item repository capacity, validate actual software bitmap allocations before `ImageProvider`, and keep settings/preview state grouped around the same appearance value.

**Tech Stack:** Kotlin Multiplatform, Android, Jetpack Glance 1.1.1, Compose Material 3, Coil 3.5.0, Multiplatform Settings, Kotlin coroutines/Flow, JUnit 4, Robolectric, Turbine, Maestro.

**Specification:** `docs/superpowers/specs/2026-07-29-configurable-widget-card-appearance-design.md`

---

## File Structure

### New files

- `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/model/WidgetCardAppearance.kt` — Android widget Card settings, enums, defaults, normalization.
- `shared/src/androidHostTest/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepositoryTest.kt` — persistence/default/normalization coverage.
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetCardAppearanceResolver.kt` — pure Card surface/text/divider resolution.
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetCardLayout.kt` — deterministic row height and width fallback.
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetExactSizeResolver.kt` — immutable options snapshot and SDK-aware exact-size resolution.
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetImageBudget.kt` — device-derived fixed-capacity bitmap budget and request key.
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/components/WidgetArticleImage.kt` — safe Coil loading, conversion, allocation validation, and Glance image rendering.
- `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetCardAppearanceResolverTest.kt`
- `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetCardLayoutTest.kt`
- `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetExactSizeResolverTest.kt`
- `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetImageBudgetTest.kt`
- `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetBitmapValidatorTest.kt`
- `androidApp/src/test/kotlin/com/prof18/feedflow/android/settings/widget/WidgetSettingsViewModelTest.kt`
- `e2e/maestro/android/regression/164-widget-card-appearance-settings.yaml` — settings visibility and preview smoke coverage.

### Modified files

- `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepository.kt`
- `shared/src/commonTest/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepositoryTest.kt`
- `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepository.kt`
- `androidApp/build.gradle.kts`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/FeedFlowWidget.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetContent.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/components/WidgetFeedListItem.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetSettingsState.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetConfigurationViewModel.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/settings/widget/WidgetSettingsViewModel.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetSettingsContent.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetSettingsScaffold.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/settings/widget/WidgetSettingsScreen.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetConfigurationActivity.kt`
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetPreviewSection.kt`
- `androidApp/src/debug/kotlin/com/prof18/feedflow/android/e2e/E2eSeedActivity.kt`
- `i18n/src/commonMain/resources/locale/values/strings.xml`
- `e2e/maestro/maestro-e2e-tests.md`
- `e2e/maestro/maestro-e2e-tests.html`

---

## Stage 1: Persistence Foundation

### Task 1: Share the fixed widget item capacity

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepository.kt`
- Modify: `shared/src/commonTest/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepositoryTest.kt`

- [ ] **Step 1: Write a failing capacity test**

Add a test that seeds more than 15 items and asserts `getFeeds().first()` emits exactly `MAX_WIDGET_FEED_ITEMS`.

```kotlin
@Test
fun `widget feed uses shared maximum item count`() = runTest(testDispatcher) {
    val feedSource = createFeedSource(id = "source-1", title = "Widget Feed")
    databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
    databaseHelper.insertFeedItems(
        items = (0 until MAX_WIDGET_FEED_ITEMS + 2).map { index ->
            buildFeedItem(
                id = "item-$index",
                title = "Item $index",
                pubDateMillis = index.toLong(),
                source = feedSource,
            )
        },
        lastSyncTimestamp = 0,
    )

    createRepository().getFeeds().test {
        assertEquals(MAX_WIDGET_FEED_ITEMS, awaitItem().size)
        cancelAndIgnoreRemainingEvents()
    }
}
```

- [ ] **Step 2: Run the test and verify failure**

```bash
./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.feed.FeedWidgetRepositoryTest"
```

Expected: compilation failure because `MAX_WIDGET_FEED_ITEMS` does not exist.

- [ ] **Step 3: Extract and use the constant**

```kotlin
const val MAX_WIDGET_FEED_ITEMS = 15

fun getFeeds(): Flow<ImmutableList<FeedItem>> =
    databaseHelper.getFeedWidgetItems(pageSize = MAX_WIDGET_FEED_ITEMS)
        .map { items -> items.map { it.toFeedItem(dateFormatter, settings) }.toImmutableList() }
```

- [ ] **Step 4: Re-run the focused test**

Expected: PASS.

### Task 2: Add normalized Card appearance persistence

**Files:**
- Create: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/model/WidgetCardAppearance.kt`
- Modify: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepository.kt`
- Create: `shared/src/androidHostTest/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepositoryTest.kt`

- [ ] **Step 1: Write failing repository tests**

Cover absent-key defaults, round-trip persistence, null color removal, opaque RGB normalization, opacity clamping, radius clamping/odd-upward normalization, and malformed enum fallback.

```kotlin
@Test
fun `card appearance defaults match current renderer`() = runTest {
    val repository = WidgetSettingsRepository(MapSettings())

    assertEquals(WidgetCardAppearance(), repository.widgetCardAppearance.value)
}

@Test
fun `card appearance normalizes before storing and emitting`() = runTest {
    val repository = WidgetSettingsRepository(MapSettings())

    repository.setWidgetCardAppearance(
        WidgetCardAppearance(
            surfaceColor = 0x12010203,
            surfaceOpacityPercent = 130,
            cornerRadiusDp = 15,
            dividerOpacityPercent = -5,
        ),
    )

    assertEquals(
        WidgetCardAppearance(
            surfaceColor = 0xFF010203.toInt(),
            surfaceOpacityPercent = 100,
            cornerRadiusDp = 16,
            dividerOpacityPercent = 0,
        ),
        repository.widgetCardAppearance.value,
    )
}
```

- [ ] **Step 2: Run tests and verify failure**

```bash
./gradlew --quiet --console=plain :shared:testAndroidHostTest --tests "com.prof18.feedflow.shared.data.WidgetSettingsRepositoryTest"
```

Expected: compilation failure for missing Card appearance types/API.

- [ ] **Step 3: Implement the Android-only model**

```kotlin
data class WidgetCardAppearance(
    val surfaceColor: Int? = null,
    val surfaceOpacityPercent: Int = 100,
    val cornerRadiusDp: Int = 16,
    val itemSeparation: WidgetCardItemSeparation = WidgetCardItemSeparation.SPACING,
    val dividerOpacityPercent: Int = 20,
    val imageSizing: WidgetCardImageSizing = WidgetCardImageSizing.THUMBNAIL,
)

enum class WidgetCardItemSeparation { SPACING, DIVIDER, NONE }
enum class WidgetCardImageSizing { THUMBNAIL, FILL_ROW_HEIGHT }

fun WidgetCardAppearance.normalized(): WidgetCardAppearance = copy(
    surfaceColor = surfaceColor?.or(0xFF000000.toInt()),
    surfaceOpacityPercent = surfaceOpacityPercent.coerceIn(0, 100),
    cornerRadiusDp = ((cornerRadiusDp.coerceIn(0, 32) + 1) / 2) * 2,
    dividerOpacityPercent = dividerOpacityPercent.coerceIn(0, 100),
)
```

- [ ] **Step 4: Implement grouped repository persistence**

Add six keys, one `MutableStateFlow`, safe enum parsing, `getWidgetCardAppearance()`, and `setWidgetCardAppearance()` that normalizes once before writing/emitting.

```kotlin
private val widgetCardAppearanceMutableFlow = MutableStateFlow(getWidgetCardAppearance())
val widgetCardAppearance = widgetCardAppearanceMutableFlow.asStateFlow()

fun setWidgetCardAppearance(value: WidgetCardAppearance) {
    val normalized = value.normalized()
    // remove null color, write five non-null values and enum names
    widgetCardAppearanceMutableFlow.value = normalized
}
```

- [ ] **Step 5: Re-run repository and feed tests**

```bash
./gradlew --quiet --console=plain :shared:testAndroidHostTest --tests "com.prof18.feedflow.shared.data.WidgetSettingsRepositoryTest"
./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.feed.FeedWidgetRepositoryTest"
```

Expected: PASS.

### Stage 1 specification checkpoint

- [ ] Defaults equal themed surface/100%/16dp/Spacing/20%/Thumbnail.
- [ ] Reads and writes normalize every new Card field.
- [ ] Unknown enums fall back without throwing.
- [ ] Fixed feed capacity is shared and remains 15.
- [ ] Run `./gradlew --quiet --console=plain :shared:compileKotlinJvm :shared:testAndroidHostTest`.
- [ ] Commit:

```bash
git add shared
git commit -m "Add widget card appearance persistence"
```

---

## Stage 2: Pure Appearance, Geometry, Exact-size, and Budget Policies

### Task 3: Add Android test support

**Files:**
- Modify: `androidApp/build.gradle.kts`

- [ ] Add focused unit-test dependencies:

```kotlin
testImplementation(libs.junit)
testImplementation(libs.koin.test)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.androidx.test.core.ktx)
testImplementation(libs.org.robolectric)
```

- [ ] Run `./gradlew --quiet --console=plain :androidApp:testGooglePlayDebugUnitTest --dry-run` and expect successful task configuration.

### Task 4: Implement Card color resolution

**Files:**
- Create: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetCardAppearanceResolver.kt`
- Create: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetCardAppearanceResolverTest.kt`

- [ ] Write tests for compatibility `onSurface/onSurface`, custom opaque/translucent/transparent surfaces, automatic compositing, Light/Dark override, and divider final alpha.
- [ ] Run the focused test and verify missing symbols.
- [ ] Implement pure models/functions:

```kotlin
data class ResolvedWidgetCardColors(
    val surface: Color?,
    val primaryText: Color,
    val secondaryText: Color,
    val divider: Color,
)

fun resolveWidgetCardColors(
    appearance: WidgetCardAppearance,
    textColorMode: WidgetTextColorMode,
    themedSecondaryContainer: Color,
    themedOnSurface: Color,
    widgetBackground: Color,
    themedWidgetUnderlay: Color,
): ResolvedWidgetCardColors
```

The default themed/opaque/Automatic branch returns themed `onSurface` for both roles. Divider RGB comes from secondary text and final alpha equals its own percentage.

- [ ] Run `WidgetCardAppearanceResolverTest`; expect PASS.

### Task 5: Implement deterministic Card geometry

**Files:**
- Create: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetCardLayout.kt`
- Create: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetCardLayoutTest.kt`

- [ ] Write tests for min/default/max widget settings, representative Android font scales, Fill square geometry, 96dp text minimum, and complete Thumbnail fallback at 256dp.
- [ ] Implement:

```kotlin
data class WidgetCardImageLayout(
    val mode: WidgetCardImageSizing,
    val rowHeightDp: Int?,
    val viewportDp: Int,
)

fun calculateWidgetCardRowHeightDp(fontSizes: WidgetFontSizes, systemFontScale: Float): Int

fun resolveWidgetCardImageLayout(
    selectedMode: WidgetCardImageSizing,
    availableSlabWidthDp: Float,
    rowHeightDp: Int,
): WidgetCardImageLayout
```

Use the spec's one source line, two title lines, one date line, 32dp vertical padding, and 16dp leading/gap in the conservative calculation.

- [ ] Run `WidgetCardLayoutTest`; expect PASS.

### Task 6: Implement SDK-aware Exact-size resolution

**Files:**
- Create: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetExactSizeResolver.kt`
- Create: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetExactSizeResolverTest.kt`

- [ ] Write API 31+ tests for explicit lists, duplicates/invalid entries, complete legacy fallback, and empty explicit list with only one orientation pair returning `currentSize`.
- [ ] Write API 26–30 tests for independent portrait/landscape pairs and singleton fallback.
- [ ] Implement immutable snapshot copying and pure resolution:

```kotlin
data class WidgetOptionsSnapshot(
    val explicitSizes: List<DpSize>,
    val minWidth: Int,
    val maxWidth: Int,
    val minHeight: Int,
    val maxHeight: Int,
)

data class ResolvedExactSizes(val key: WidgetExactSizeKey, val sizes: List<DpSize>)

fun resolveExactSizes(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
    sdkInt: Int,
): ResolvedExactSizes
```

Do not query `GlanceAppWidgetManager`, launch effects, retry, or poll.

- [ ] Run `WidgetExactSizeResolverTest`; expect PASS.

### Task 7: Implement fixed-capacity device bitmap budgeting

**Files:**
- Create: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetImageBudget.kt`
- Create: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetImageBudgetTest.kt`

- [ ] Write tests for 15 × V payloads, 6MiB cap, 480×800 device limit/headroom, 512px edge, overflow-safe large dimensions, and budget-key changes with unchanged edge.
- [ ] Implement saturating `Long` calculations:

```kotlin
data class WidgetImageBudget(
    val exactSizeKey: WidgetExactSizeKey,
    val payloadBudgetBytes: Long,
    val maxRequestEdgePx: Int,
)

fun resolveWidgetImageBudget(
    screenWidthPx: Int,
    screenHeightPx: Int,
    exactSizes: ResolvedExactSizes,
): WidgetImageBudget
```

`payloadCount = MAX_WIDGET_FEED_ITEMS * exactSizes.sizes.size`; reserve 25% and cap article budget at 6MiB.

- [ ] Run `WidgetImageBudgetTest`; expect PASS.

### Stage 2 specification checkpoint

- [ ] Compatibility Card colors remain exact.
- [ ] Divider alpha replaces, not multiplies, secondary alpha.
- [ ] Fill geometry includes system font scale and 96dp fallback.
- [ ] API 31+ and API 26–30 size extraction match the spec.
- [ ] Budget always reserves 15 articles for every variant and respects device limit.
- [ ] Run all five new helper test classes and `:androidApp:compileGooglePlayDebugKotlin`.
- [ ] Commit:

```bash
git add androidApp/build.gradle.kts androidApp/src/main/kotlin/com/prof18/feedflow/android/widget androidApp/src/test/kotlin/com/prof18/feedflow/android/widget
git commit -m "Add widget card rendering policies"
```

---

## Stage 3: Safe Shared Widget Image Loader

### Task 8: Implement bitmap conversion/allocation validation

**Files:**
- Create: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/components/WidgetArticleImage.kt`
- Create: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetBitmapValidatorTest.kt`

- [ ] Write Robolectric tests for ARGB_8888 acceptance, RGBA_F16 conversion, hardware rejection/conversion-or-omission, oversized dimensions, `allocationByteCount` over budget, one further downscale, and final omission.
- [ ] Implement a pure validation boundary:

```kotlin
data class WidgetImageRequestKey(
    val url: String,
    val requestEdgePx: Int,
    val payloadBudgetBytes: Long,
)

internal fun validateWidgetBitmap(
    bitmap: Bitmap,
    key: WidgetImageRequestKey,
): Bitmap?
```

Only return software ARGB_8888 with dimensions within edge and `allocationByteCount <= payloadBudgetBytes`.

- [ ] Run `WidgetBitmapValidatorTest`; expect PASS.

### Task 9: Implement the Glance/Coil image composable

- [ ] Build exact square requests with:

```kotlin
ImageRequest.Builder(context)
    .data(key.url)
    .size(key.requestEdgePx, key.requestEdgePx)
    .precision(Precision.EXACT)
    .scale(Scale.FILL)
    .allowHardware(false)
    .bitmapConfig(Bitmap.Config.ARGB_8888)
    .build()
```

- [ ] Use `remember(key)`, `LaunchedEffect(key)`, and a current-key guard before applying the result.
- [ ] Convert using Coil's bitmap conversion API, validate actual allocation, and render only validated results.
- [ ] Support separate viewport dp and corner radius so List, Thumbnail Card, and Fill Card reuse the loader.
- [ ] Run bitmap tests plus `:androidApp:compileGooglePlayDebugKotlin`.

### Stage 3 specification checkpoint

- [ ] `Resources.getSystem()` is removed from widget image sizing.
- [ ] Full request identity includes URL, bounded dimensions, and payload budget.
- [ ] No hardware/F16/over-budget bitmap reaches `ImageProvider`.
- [ ] Failed/noncompliant images omit the region.
- [ ] Commit:

```bash
git add androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/components/WidgetArticleImage.kt androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetBitmapValidatorTest.kt
git commit -m "Add safe widget image loading"
```

---

## Stage 4: Glance Renderer Integration

### Task 10: Propagate appearance and Exact-size policy

**Files:**
- Modify: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/FeedFlowWidget.kt`
- Modify: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetContent.kt`

- [ ] Override `val sizeMode = SizeMode.Exact`.
- [ ] Collect grouped `widgetCardAppearance`.
- [ ] In composition, copy `LocalAppWidgetOptions.current`, resolve exact sizes, calculate one fixed-capacity budget, and pass it to both layouts.
- [ ] Pass Card appearance into `WidgetContent` without direct repository reads below `FeedFlowWidget`.
- [ ] Compile Android; expect PASS.

### Task 11: Render configurable Card slabs and separators

**Files:**
- Modify: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetContent.kt`
- Modify: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/components/WidgetFeedListItem.kt`

- [ ] Preserve the existing Thumbnail/Spacing modifier order exactly.
- [ ] Add Card parameters for resolved colors, radius, image layout, and request policy.
- [ ] Use indexed lazy items so Divider is emitted only when `index < lastIndex`.
- [ ] Keep separator outside the clickable slab; use 1dp height, 16dp inset, and resolved final-alpha color.
- [ ] Remove vertical wrapper spacing only for Divider/None.
- [ ] In Fill mode, fix row and image to the same height, constrain metadata/title lines, give text 16dp insets, and let the image touch top/bottom/trailing bounds.
- [ ] Preserve independent row click actions and decorative image/divider semantics.

### Task 12: Preserve List behavior through the shared loader

- [ ] Replace the old private `FeedItemImage` with `WidgetArticleImage` for List and Card.
- [ ] Keep List viewport at 50dp, crop, 8dp radius, current content placement, and existing text colors.
- [ ] Confirm List uses the same provider-wide budget but never selects Fill geometry.
- [ ] Run renderer helper tests and compile.

### Stage 4 specification checkpoint

- [ ] Default Card slab/text/spacing/thumbnail matches current behavior.
- [ ] Zero surface opacity emits no slab fill.
- [ ] Spacing/Divider/None and final-divider rule are implemented.
- [ ] Radius 0–32 and Fill geometry are wired.
- [ ] List layout geometry and interactions remain unchanged.
- [ ] Run `./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug`.
- [ ] Commit:

```bash
git add androidApp/src/main/kotlin/com/prof18/feedflow/android/widget
git commit -m "Render configurable widget cards"
```

---

## Stage 5: Settings State, ViewModels, and Controls

### Task 13: Add grouped settings state and callback plumbing

**Files:**
- Modify: `WidgetSettingsState.kt`
- Modify: both widget ViewModels
- Modify: `WidgetSettingsScaffold.kt`
- Modify: `WidgetSettingsScreen.kt`
- Modify: `WidgetConfigurationActivity.kt`

- [ ] Add `cardAppearance: WidgetCardAppearance = WidgetCardAppearance()` to state.
- [ ] Include the grouped repository flow in both state combinations.
- [ ] Add six callbacks to both ViewModels.
- [ ] Configuration callbacks normalize/persist only.
- [ ] In-app callbacks normalize, equality-guard, update state, persist once, and launch exactly one updater call.
- [ ] Thread all callbacks through both settings entry points.
- [ ] Compile Android.

### Task 14: Test immediate in-app widget updates

**Files:**
- Create: `androidApp/src/test/kotlin/com/prof18/feedflow/android/settings/widget/WidgetSettingsViewModelTest.kt`

- [ ] Add a Main dispatcher rule and fake `WidgetUpdater` counter.
- [ ] For each of six callbacks, assert changed normalized values call updater once.
- [ ] Assert unchanged/equivalent normalized values call updater zero times.
- [ ] Assert configuration ViewModel persistence does not acquire updater behavior.
- [ ] Run focused ViewModel tests; expect PASS.

### Task 15: Add conditional Card controls and reusable picker copy

**Files:**
- Modify: `WidgetSettingsContent.kt`
- Modify: `i18n/src/commonMain/resources/locale/values/strings.xml`

- [ ] Add English strings for section, color/default, opacity, radius, separation values, divider opacity, and image sizing values.
- [ ] Parameterize picker title/preview/brightness/hex/default copy.
- [ ] Show Card section only for Card layout.
- [ ] Show Divider opacity only for Divider.
- [ ] Show Image sizing only when images are visible.
- [ ] Use opacity sliders and a 0–32dp radius slider with 15 intermediate steps.
- [ ] Use existing dropdown and accessible setting rows.
- [ ] Run `.scripts/refresh-translations.sh`.
- [ ] Compile Android.

### Stage 5 specification checkpoint

- [ ] Every new setting is reachable under the required visibility condition.
- [ ] Reset removes custom Card color.
- [ ] Values show `%` or `dp` units.
- [ ] In-app effective changes update placed widgets exactly once.
- [ ] Add-widget confirmation behavior remains unchanged.
- [ ] Run focused ViewModel tests and `:androidApp:assembleGooglePlayDebug`.
- [ ] Commit:

```bash
git add androidApp/src/main i18n androidApp/src/test
git commit -m "Add widget card appearance settings"
```

---

## Stage 6: Preview and Debug/E2E Coverage

### Task 16: Make preview use shared policies

**Files:**
- Modify: `WidgetPreviewSection.kt`

- [ ] Pass `cardAppearance` through preview functions.
- [ ] Use the same color resolver and geometry calculations as Glance.
- [ ] Render two sample articles.
- [ ] Render Spacing/Divider/None distinctly and suppress final divider.
- [ ] Render Thumbnail and Fill viewport geometry.
- [ ] Replace fixed 230dp clipping with content-aware minimum height.
- [ ] Keep wallpaper visual-only; automatic contrast uses themed underlay.
- [ ] Add Compose previews for default, transparent-divider-Fill, and translucent custom color on light/dark backdrops.

### Task 17: Reset debug settings and add Maestro flow

**Files:**
- Modify: `androidApp/src/debug/kotlin/com/prof18/feedflow/android/e2e/E2eSeedActivity.kt`
- Create: `e2e/maestro/android/regression/164-widget-card-appearance-settings.yaml`
- Modify: Maestro catalog markdown/html

- [ ] Reset `WidgetCardAppearance()` in `resetWidgetSettings()`.
- [ ] Seed a deterministic Card profile when using the Android-widget profile.
- [ ] Add a flow that opens in-app widget settings, selects Card, confirms conditional controls, changes separation/image sizing, and observes preview text/control state.
- [ ] Document launcher-host placement and visual parity as manual-only.
- [ ] Run the focused Maestro flow when a device is available.

### Stage 6 specification checkpoint

- [ ] Preview exposes every visual choice with two rows and no clipping.
- [ ] Transparent backgrounds show wallpaper while automatic contrast uses themed fallback.
- [ ] Debug reset cannot leak prior Card preferences.
- [ ] Run translation refresh, focused unit tests, and Android assembly.
- [ ] Commit:

```bash
git add androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetPreviewSection.kt androidApp/src/debug i18n e2e
git commit -m "Align widget card preview and coverage"
```

---

## Stage 7: Full Validation and Device Matrix

### Task 18: Automated gates

- [ ] Run translation refresh:

```bash
.scripts/refresh-translations.sh
```

- [ ] Run focused shared and Android tests:

```bash
./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.feed.FeedWidgetRepositoryTest"
./gradlew --quiet --console=plain :shared:testAndroidHostTest --tests "com.prof18.feedflow.shared.data.WidgetSettingsRepositoryTest"
./gradlew --quiet --console=plain :androidApp:testGooglePlayDebugUnitTest
```

- [ ] Run build and full project gate:

```bash
./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug
./gradlew --quiet --console=plain detekt allTests
```

Expected: all commands exit 0.

### Task 19: Device verification

- [ ] Verify `command -v android`; if unavailable, report that Android CLI validation is blocked rather than substituting unapproved tooling.
- [ ] Select the first `adb devices` device; start `Resizable_Experimental` only when none is connected.
- [ ] Install with:

```bash
android run --apks=androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk --device=<serial>
```

- [ ] Validate default Card against the pre-change baseline.
- [ ] Validate transparent, custom translucent, radius 0/32, and all separation modes.
- [ ] Validate no final divider and complete row click targets.
- [ ] Validate Thumbnail and Fill at minimum/default/maximum widget font settings and representative system font scales.
- [ ] Resize narrow → wide → narrow and confirm Thumbnail fallback → Fill → Thumbnail.
- [ ] Validate 15-image Card and List widgets on API 31+ and API 26–30.
- [ ] Validate a 480×800 display without `RemoteViews`/bitmap failures.
- [ ] Validate missing/failed images and preview parity.
- [ ] Capture annotated screenshots for default, translucent, and transparent-divider-Fill layouts.

### Task 20: Final specification audit

- [ ] Compare each Acceptance Criteria bullet in the specification to code/tests/device evidence.
- [ ] Record any unavailable device/API matrix item explicitly; do not mark it passing without evidence.
- [ ] Run `git diff --check` and `git status --short`.
- [ ] Commit any final test/catalog corrections:

```bash
git add .
git commit -m "Validate widget card customization"
```

- [ ] Push the branch if a writable remote exists; otherwise report the existing permission/fork blocker.

---

## Stage 8 Addendum: Configurable Total-item Cap

### Task 21: Persist and expose the normalized global limit

**Files:**
- Create: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/model/WidgetArticleLimit.kt`
- Modify: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepository.kt`
- Modify: `shared/src/androidHostTest/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepositoryTest.kt`

- [ ] Add failing Android-host tests for default 15, round-trip/flow emission, lower clamp to 1, upper clamp to 15, and malformed stored values.
- [ ] Add `WIDGET_MAXIMUM_ARTICLES`, normalized getter/setter, and `StateFlow<Int>` separate from `WidgetCardAppearance`.
- [ ] Run `./gradlew --quiet --console=plain :shared:testAndroidHostTest --tests "com.prof18.feedflow.shared.data.WidgetSettingsRepositoryTest"`.

### Task 22: Wire settings state, ViewModels, and the app-owned slider

**Files:**
- Modify: both widget ViewModels, `WidgetSettingsState.kt`, `WidgetSettingsContent.kt`, `WidgetSettingsScaffold.kt`, both settings entry points, `SettingsE2eIds.kt`, English strings, and `E2eSeedActivity.kt`
- Test: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetSettingsViewModelTest.kt`

- [ ] Add failing tests proving an effective normalized in-app change persists once and invokes `WidgetUpdater` once, while a normalized no-op invokes neither; configuration persists without updater behavior.
- [ ] Add a discrete 1..15 slider immediately after Feed Layout with `Maximum articles: %s`, stable E2E ID, default/reset value 15, and callback plumbing.
- [ ] Run `.scripts/refresh-translations.sh` and the focused ViewModel tests.

### Task 23: Cap renderer and preview without changing capacity or budget

**Files:**
- Modify: `FeedFlowWidget.kt`, `WidgetContent.kt`, `WidgetPreviewSection.kt`, and `WidgetImageBudgetTest.kt`
- Create: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetArticleLimitTest.kt`

- [ ] Add failing tests for caps 1, midrange, 15, invalid low/high values, order, empty input, one-row preview selection, and fixed `MAX_WIDGET_FEED_ITEMS * payloadVariantCount` budgeting.
- [ ] Collect the limit reactively and apply it before the shared List/Card `LazyColumn` branch; do not change `FeedWidgetRepository`'s 15-item query.
- [ ] Keep the preview's first sample only at limit 1 and both deterministic samples at limits 2..15.
- [ ] Do not pass the selected limit into `WidgetImageBudget`; a lower visible cap must never increase per-image budget.

### Task 24: E2E, documentation, and runtime validation

- [ ] Extend REG-164 only to verify the app-owned slider/value plumbing and explicitly retain launcher row count and visual parity as manual coverage.
- [ ] Run shared Android-host tests, Android unit tests, compile/assemble, `detekt allTests`, and `git diff --check` with the project Gradle flags.
- [ ] On an available Android widget host, verify limits 1, midrange, and 15 in both layouts, reactive placed-widget updates, preserved ordering/empty state, preview one-row behavior at 1, and that launcher size—not this setting—determines simultaneous visibility. Record unavailable host validation without claiming it passed.
