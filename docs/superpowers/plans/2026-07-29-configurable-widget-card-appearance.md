# Configurable Widget Card Appearance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add configurable Android widget Card slabs and safe Thumbnail/Fill-row-height article images while preserving the current default Card and List appearance.

**Architecture:** Persist normalized Android-only Card appearance and freshness settings, resolve colors/geometry/freshness through pure helpers, and pass one composition-wide Exact-size image policy to both List and Card renderers. Query at most 100 unread candidates, filter once from one `nowMillis`, budget every Exact-size variant against the actual immutable filtered count with a one-slot empty reserve, validate actual software bitmap allocations before `ImageProvider`, and keep settings/preview state coherent.

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
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetImageBudget.kt` — device-derived actual-count bitmap budget and complete request key.
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

### Task 1: Bound widget candidates with an internal safety capacity

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepository.kt`
- Modify: `shared/src/commonTest/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepositoryTest.kt`

- [ ] **Step 1: Write a failing capacity test**

Add a test that seeds more than 100 items and asserts `getFeeds().first()` emits exactly `WIDGET_FEED_ITEM_SAFETY_CAPACITY` candidates.

```kotlin
@Test
fun `widget feed uses internal safety capacity`() = runTest(testDispatcher) {
    val feedSource = createFeedSource(id = "source-1", title = "Widget Feed")
    databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
    databaseHelper.insertFeedItems(
        items = (0 until WIDGET_FEED_ITEM_SAFETY_CAPACITY + 2).map { index ->
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
        assertEquals(WIDGET_FEED_ITEM_SAFETY_CAPACITY, awaitItem().size)
        cancelAndIgnoreRemainingEvents()
    }
}
```

- [ ] **Step 2: Run the test and verify failure**

```bash
./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.feed.FeedWidgetRepositoryTest"
```

Expected: compilation failure because `WIDGET_FEED_ITEM_SAFETY_CAPACITY` does not exist.

- [ ] **Step 3: Extract and use the constant**

```kotlin
const val WIDGET_FEED_ITEM_SAFETY_CAPACITY = 100

fun getFeeds(): Flow<ImmutableList<FeedItem>> =
    databaseHelper.getFeedWidgetItems(pageSize = WIDGET_FEED_ITEM_SAFETY_CAPACITY.toLong())
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

### Task 7: Implement actual-count device bitmap budgeting

**Files:**
- Create: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/WidgetImageBudget.kt`
- Create: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetImageBudgetTest.kt`

- [ ] Write tests for actual filtered counts 0/1/15/100 across one and multiple variants, the one-slot empty reserve, 6MiB cap, 480×800 device limit/headroom, 512px edge, overflow-safe large dimensions, and count/budget-key changes with unchanged edge.
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
    feedItemCount: Int,
): WidgetImageBudget
```

`payloadCount = maxOf(feedItemCount, 1) * exactSizes.payloadVariantCount`; include actual count and payload count in budget/request identity, reserve 25%, and cap article budget at 6MiB.

- [ ] Run `WidgetImageBudgetTest`; expect PASS.

### Stage 2 specification checkpoint

- [ ] Compatibility Card colors remain exact.
- [ ] Divider alpha replaces, not multiplies, secondary alpha.
- [ ] Fill geometry includes system font scale and 96dp fallback.
- [ ] API 31+ and API 26–30 size extraction match the spec.
- [ ] Budget uses the actual immutable filtered count for every serialized variant, reserves one slot only when empty, and respects the device limit.
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
- [ ] In composition, copy `LocalAppWidgetOptions.current`, resolve exact sizes, calculate one actual-filtered-count budget, and pass it with the same immutable list to both layouts.
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

## Stage 8 Addendum: Article Freshness Window (Supersedes Configurable Total-item Cap)

This addendum withdraws the previously approved 1–15 `Maximum articles` control and every implementation detail tied to it. There is no count slider, count preference, compatibility key, coalesced callback, or user-visible count promise.

### Task 21: Add the shared freshness model, pure policy, and safe persistence

**Files:**
- Replace: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/model/WidgetArticleLimit.kt` with `WidgetFreshness.kt`
- Modify: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepository.kt`
- Modify: `shared/src/androidHostTest/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepositoryTest.kt`
- Test: `shared/src/androidHostTest/kotlin/com/prof18/feedflow/shared/domain/model/WidgetFreshnessTest.kt`

- [ ] Write failing tests for the exact enum values `LAST_24_HOURS`, `LAST_3_DAYS`, and `LAST_7_DAYS`; default `LAST_3_DAYS`; round-trip and `StateFlow` emission; unknown-name and wrong-type stored-value fallback; inclusive cutoff; undated exclusion; future-date inclusion; empty input; and preservation of input order. Every policy test supplies a fixed `nowMillis`.
- [ ] Run `./gradlew --quiet --console=plain :shared:testAndroidHostTest --tests "com.prof18.feedflow.shared.data.WidgetSettingsRepositoryTest" --tests "com.prof18.feedflow.shared.domain.model.WidgetFreshnessTest"` and confirm the new tests fail for missing freshness APIs.
- [ ] Implement the Android-shared `WidgetFreshness` enum and generic immutable filtering policy. Persist enum names under only `WIDGET_FRESHNESS`, expose `getWidgetFreshness()`, `setWidgetFreshness()`, and `StateFlow<WidgetFreshness>`, and fall back safely to `LAST_3_DAYS` for malformed storage.
- [ ] Re-run the focused shared tests and confirm they pass.

### Task 22: Replace the slider with one effective-change dropdown callback

**Files:**
- Modify: `WidgetSettingsState.kt`, both widget ViewModels, `WidgetSettingsContent.kt`, `WidgetSettingsScaffold.kt`, `WidgetSettingsScreen.kt`, `WidgetConfigurationActivity.kt`, `SettingsE2eIds.kt`, English strings, and `E2eSeedActivity.kt`
- Test: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetSettingsViewModelTest.kt`

- [ ] Replace the count/coalescing tests with failing tests proving an effective in-app freshness selection persists immediately and calls `WidgetUpdater` exactly once after coroutine dispatch, an unchanged selection does neither, and configuration persists an effective change without an updater.
- [ ] Run `./gradlew --quiet --console=plain :androidApp:testGooglePlayDebugUnitTest --tests "com.prof18.feedflow.android.widget.WidgetSettingsViewModelTest"` and confirm the freshness tests fail.
- [ ] Remove all maximum-count state, callbacks, imports, persistence counting, accessibility copy, and slider plumbing. Add an `Article age` `CompactSettingDropdownRow` immediately after Feed Layout with localized `Last 24 hours`, `Last 3 days`, and `Last 7 days` options plus stable row and option E2E IDs.
- [ ] Reset debug seeds to `LAST_3_DAYS`. Keep the in-app repository-value guard synchronous and one-callback/one-updater; keep configuration persistence updater-free.
- [ ] Refresh translations and re-run the focused ViewModel tests.

### Task 23: Apply one-snapshot filtering and a 100-item repository safety capacity

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepository.kt`
- Modify: `shared/src/commonTest/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepositoryTest.kt`
- Modify: `FeedFlowWidget.kt`, `WidgetContent.kt`, and `WidgetPreviewSection.kt`
- Replace: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/WidgetArticleLimitTest.kt` with preview freshness coverage

- [ ] Write failing tests that name and enforce `WIDGET_FEED_ITEM_SAFETY_CAPACITY = 100`, plus deterministic preview profiles whose fixed 12-hour and 2-day samples yield row counts 1, 2, and 2 for the three freshness options.
- [ ] Run the focused shared repository and Android preview tests and confirm the new expectations fail.
- [ ] Query at most the 100 newest unread widget candidates while preserving raw nullable `FeedItem.pubDateMillis` and newest-first order.
- [ ] Capture `nowMillis` once in `provideGlance`, before `provideContent` can serialize multiple Exact variants. Collect freshness and filter with the shared policy into one immutable list before resolving the image budget or invoking `WidgetContent`; pass that same list to List/Card rendering and remove all count limiting from `WidgetContent`.
- [ ] Give preview samples fixed publication timestamps relative to an injected/reference `nowMillis`, use the same freshness policy rather than date text, and preserve the existing widget empty state.
- [ ] Re-run focused tests and confirm they pass.

### Task 24: Budget actual filtered items across every Exact variant

**Files:**
- Modify: `WidgetImageBudget.kt`, `WidgetImageBudgetTest.kt`, `components/WidgetBitmapValidatorTest.kt`, and request-identity test fixtures

- [ ] First change tests to cover filtered counts 0, 1, 15, and 100, one and multiple serialized Exact variants, a one-slot reserve for count 0, actual-count payload division, aggregate delivered-allocation validation, and request/budget identity invalidation whenever the actual count changes, including 0 to 1.
- [ ] Run the focused image-budget and bitmap-validator tests and confirm the former fixed-15 behavior fails.
- [ ] Add `feedItemCount` to `resolveWidgetImageBudget`; calculate `payloadCount = maxOf(feedItemCount, 1) * payloadVariantCount` with saturated `Long` multiplication. Preserve the device-derived ceiling, 6 MiB cap, 512-pixel edge cap, software bitmap allocation validation, and full Exact-size/budget identity, while adding actual count and payload count to both budget and request identities.
- [ ] Re-run all focused image-policy tests and confirm they pass.

### Task 25: Deterministic E2E profile, catalog, spec, and physical validation

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/e2e/E2eSeedRunner.kt`, `E2eSeedActivity.kt`, REG-164, the Maestro catalog, and the existing design specification

- [ ] Seed the Android-widget debug profile with publication times derived from one supplied reference `nowMillis` so 24-hour, 3-day, and 7-day selections have deterministic candidates without putting wall-clock reads in tests.
- [ ] Update REG-164 to select `Article age` through stable IDs. State in the catalog that launcher placement, row counts, and visual behavior remain manual physical coverage; do not claim launcher automation.
- [ ] Update the design specification so this addendum explicitly supersedes the count selector and documents default 3 days, inclusive cutoff, undated exclusion, 100-item safety capacity, actual-count multi-variant budgeting, identity invalidation, tests, and physical runtime checks.
- [ ] Run `.scripts/refresh-translations.sh`, focused red/green suites, `./gradlew --quiet --console=plain :shared:allTests`, all Android tests, `:androidApp:compileGooglePlayDebugKotlin`, `:androidApp:assembleGooglePlayDebug`, and `./gradlew --quiet --console=plain detekt allTests`.
- [ ] Run `git diff --check`, inspect `git diff` for dead maximum-count plumbing, confirm the transparent `Color.Transparent` RemoteViews recycling fix remains and no scrollbar/version/publishing change was made, then validate List and Card freshness behavior on the available Android widget host. Record any unavailable physical validation honestly.
- [ ] Create one simple implementation commit with the required harness attribution. Do not push, publish, or bump the version.
