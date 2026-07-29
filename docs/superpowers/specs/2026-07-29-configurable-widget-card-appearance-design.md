# Configurable Widget Card Appearance Design

Date: 2026-07-29

## Summary

FeedFlow's Android feed widget currently lets users configure the outer widget background, but the `Card` feed layout always renders each article with a fully opaque themed surface and a fixed 50 dp thumbnail. This makes it impossible to build a transparent, layered widget composition or adapt the cards to launchers with their own visual treatment.

This change makes the existing `Card` layout fully configurable while preserving its current appearance for existing installations. Users will be able to choose the card surface color and opacity, corner radius, item separation, divider opacity, and image sizing. The feature remains generic and launcher-neutral so it is suitable for an upstream FeedFlow pull request.

## Goals

- Allow card surfaces to be fully transparent independently of the outer widget background.
- Allow a custom card surface color and opacity.
- Support spaced cards, divider-separated cards, and cards with no visual separator.
- Allow article images to retain the current thumbnail treatment or fill the rendered row height.
- Keep the widget preview behaviorally aligned with the real Glance widget.
- Preserve the current appearance when the new settings have never been changed.
- Keep the implementation generic, maintainable, and suitable for upstream review.

## Non-goals

- Integrating with Smart Launcher or any other launcher API.
- Producing or controlling launcher-side wallpaper blur.
- Detecting or synchronizing launcher colors, fonts, opacity, or widget stacking.
- Bundling or selecting custom font families.
- Changing the `List` layout.
- Migrating the widget from Jetpack Glance to hand-written `RemoteViews`.
- Converting global widget preferences into per-widget-instance preferences.
- Adding new feed filtering, sorting, refresh, or interaction behavior.

## Existing Behavior and Problem

The outer widget background is resolved in `WidgetContent` from the configured background color and opacity. In contrast, `WidgetFeedItemCard` independently uses `GlanceTheme.colors.secondaryContainer`, `16.dp` corners, fixed vertical spacing, and themed `onSurface` text. Its article image is always requested and rendered as a cropped `50.dp` square.

Consequently, setting the widget background opacity to zero only removes the outer canvas. It does not affect the opaque article cards. The configuration preview repeats this mismatch by drawing card items with `MaterialTheme.colorScheme.secondaryContainer`, so neither the preview nor the real widget can represent transparent cards.

## Design Principles

1. **Backward-compatible defaults:** an untouched installation must look the same after upgrading.
2. **Generic terminology:** settings describe card surfaces, separators, and image sizing rather than a specific launcher or workaround.
3. **One resolved appearance:** settings are normalized into one appearance value consumed by both renderer-facing state and the preview.
4. **Progressive disclosure:** card-only controls appear only when `Card` is selected, and dependent controls appear only when relevant.
5. **Best-effort automatic contrast:** automatic text colors use the surfaces FeedFlow can observe; manual light or dark text remains the escape hatch for transparent widgets placed over external content.

## User-facing Settings

The existing `Feed Layout`, header, image visibility, widget background, text color, and font size settings remain. When `Feed Layout` is `Card`, the configuration screen adds a `Card appearance` section.

| Setting | Values | Default | Visibility |
| --- | --- | --- | --- |
| Card surface color | Themed default or custom RGB color | Themed default | Card layout |
| Card surface opacity | 0-100 percent | 100 percent | Card layout |
| Card corner radius | 0-32 dp, in 2 dp steps | 16 dp | Card layout |
| Item separation | Spacing, Divider, None | Spacing | Card layout |
| Divider opacity | 0-100 percent | 20 percent | Card layout and Divider separation |
| Image sizing | Thumbnail, Fill row height | Thumbnail | Card layout and images visible |

The established color-picker dialog is reused for card surface color, with parameterized labels rather than a duplicate picker implementation. Resetting the card color removes the custom value and restores the themed default.

Settings continue to save immediately through the existing view-model callbacks. The add-widget confirmation behavior does not change.

### Intended transparent composition

The target layered composition is expressible without a launcher-specific preset:

- Widget background opacity: `0%`
- Card surface opacity: `0%`
- Item separation: `Divider`
- Divider opacity: approximately `20%`
- Image sizing: `Fill row height`
- Text color: `Light` when automatic contrast cannot observe the external background

## Domain and Persistence Model

The card-specific values are defined in a shared `WidgetCardAppearance` model under `shared/src/commonMain/.../domain/model/` rather than passed as unrelated primitive parameters throughout the renderer. The value contains:

- `surfaceColor: Int?`
- `surfaceOpacityPercent: Int`
- `cornerRadiusDp: Int`
- `itemSeparation: WidgetCardItemSeparation`
- `dividerOpacityPercent: Int`
- `imageSizing: WidgetCardImageSizing`

`WidgetCardItemSeparation` has `SPACING`, `DIVIDER`, and `NONE`. `WidgetCardImageSizing` has `THUMBNAIL` and `FILL_ROW_HEIGHT`.

`WidgetSettingsRepository` persists each field alongside the existing widget settings. Enum values are stored by stable enum name, matching the existing layout and text-color conventions. Nullable surface color follows the existing widget-background convention: absence of the preference means themed default.

The persisted keys are:

- `WIDGET_CARD_SURFACE_COLOR`
- `WIDGET_CARD_SURFACE_OPACITY_PERCENT`
- `WIDGET_CARD_CORNER_RADIUS_DP`
- `WIDGET_CARD_ITEM_SEPARATION`
- `WIDGET_CARD_DIVIDER_OPACITY_PERCENT`
- `WIDGET_CARD_IMAGE_SIZING`

### Compatibility and invalid data

No migration is required because every new key has a default matching the current renderer:

- Surface color: themed `secondaryContainer`
- Surface opacity: `100`
- Corner radius: `16`
- Item separation: `SPACING`
- Divider opacity: `20`
- Image sizing: `THUMBNAIL`

Repository reads must clamp numeric values to their supported ranges. A stored radius that is not an even number is rounded to the nearest supported two-dp step, with an exact midpoint rounded upward. Unknown or malformed enum names fall back to their defaults instead of throwing. Existing keys and their semantics are unchanged.

The project currently applies widget appearance globally to all FeedFlow widget instances. This change preserves that behavior; per-instance configuration is a separate feature.

## Appearance Resolution

Card rendering must not directly read individual repository flows. `FeedFlowWidget` collects the new values and constructs the card appearance passed through `WidgetContent` to the card renderer.

The resolved card surface follows these rules:

1. If a custom card color exists, use its opaque RGB value as the surface base.
2. Otherwise use `GlanceTheme.colors.secondaryContainer`.
3. Apply the configured card surface opacity after clamping it to `0..100`.
4. At zero opacity, the card contributes no visible fill.

For automatic text contrast, calculate the effective card background by compositing the card surface over the effective outer widget background. The effective outer background continues to use the existing widget-background resolver. When both layers are transparent, FeedFlow cannot inspect another widget or the live launcher background; automatic contrast therefore falls back to the themed widget underlay. Existing `Light` and `Dark` text modes override automatic contrast and must work unchanged.

Both primary and secondary card text colors come from the same resolved contrast result. This fixes the current behavior in which `Card` mode ignores the configured text-color mode.

The divider color is derived from the resolved secondary text color and then uses the configured divider opacity. It does not introduce a separate color picker.

## Card Layout and Separation

`WidgetFeedItemCard` remains the clickable unit for an article. Its visible surface, geometry, and outer separation are driven by the resolved card appearance.

### Spacing

`SPACING` preserves the current vertical `Spacing.xsmall` gap around each card. No divider is rendered. This is the compatibility default.

### Divider

`DIVIDER` removes the inter-card gap and inserts a one-dp horizontal divider between adjacent items. The divider:

- Is rendered after an item only when another item follows.
- Uses the configured opacity and resolved secondary text color.
- Is horizontally inset by the card content padding so it aligns with native information-widget separators and does not touch the widget edge.
- Is decorative and has no click or accessibility semantics.

### None

`NONE` removes both the inter-card gap and divider. Individual rows remain independently clickable.

Corner radius is applied to the visible card surface in all modes, while the article click target continues to span the complete row. At zero surface opacity the radius has no visible fill effect but remains stored so restoring opacity restores the chosen geometry.

## Image Rendering

Image visibility remains controlled by the existing `Hide article image` setting. If images are visible and an article has an image URL, behavior depends on `Image sizing`.

### Thumbnail

`THUMBNAIL` preserves the current behavior:

- `50.dp` square target.
- Center-cropped content.
- `8.dp` image corner radius.
- Existing trailing placement and padding.

### Fill row height

`FILL_ROW_HEIGHT` makes the image a trailing visual region whose height equals the rendered card row height:

- The image keeps a square viewport, so its width equals the row height.
- Content uses center crop and is never stretched.
- Text retains the existing internal content padding.
- The image has no vertical inset; its top and bottom align with the row bounds.
- The image's outer corners respect the configured card radius, within Glance's supported uniform-corner behavior.
- Text remains limited to the existing maximum of two title lines.

Because Glance is backed by `RemoteViews`, the implementation uses a centralized deterministic row-height calculation derived from the current font scale rather than runtime sibling measurement. The calculated height must be at least the height of the feed-name line, two title lines, date line, their existing inter-line spacing, and 16 dp top and bottom text padding. The image and row have equal rendered heights at every supported font scale without clipping that maximum text case.

Image requests must target the actual display dimensions at device density rather than reuse the current 50-by-50-pixel request. This avoids visibly upscaling a thumbnail into the fill-height viewport. The existing asynchronous loading model remains.

If an image URL is missing or loading fails, omit the image region and let text use the available width. Do not add a placeholder or error badge.

## Configuration Preview

The configuration preview must expose every visual choice before the widget is added or updated.

- It uses the same default values, clamping, color compositing, and text-color resolution as the widget renderer.
- Card preview rows use the configured surface color, opacity, radius, separator, and image sizing.
- The card preview contains at least two sample articles so `Spacing`, `Divider`, and `None` are distinguishable.
- The preview's light/dark backdrop toggle remains and continues to demonstrate contrast behavior.
- At zero widget and card opacity, the preview wallpaper remains visible through both layers.
- Fill-height preview images use representative artwork blocks with the same geometry as the real row.

Shared pure helpers should resolve appearance where possible. Compose preview UI and Glance UI remain separate renderers, but neither should independently reinterpret persistence defaults or percentage values.

## Component Boundaries

The implementation should keep the following responsibilities separate:

- **Persistence:** `WidgetSettingsRepository` owns defaults, validation, storage, and flows.
- **Settings state:** `WidgetSettingsState` exposes one coherent snapshot to both widget configuration entry points.
- **Settings UI:** `WidgetSettingsContent` renders conditional controls and reuses the color picker.
- **Appearance resolution:** pure helpers clamp percentages, resolve themed/custom surfaces, composite known layers, and derive text/divider colors.
- **Glance rendering:** `WidgetContent` arranges the feed; `WidgetFeedItemCard` renders one card using a resolved appearance.
- **Image loading:** the image component chooses request and viewport dimensions from `WidgetCardImageSizing` and resolved row geometry.
- **Preview rendering:** `WidgetPreviewSection` mirrors the resolved appearance without owning persistence logic.

Both `WidgetConfigurationViewModel` and the in-app widget settings view model must expose and update the new fields. Their mapping should use a grouped intermediate value if necessary to keep coroutine `combine` calls readable.

## Accessibility and Interaction

- Article rows retain their current click actions and complete clickable bounds.
- Transparency and separators must not split or shrink the touch target.
- Divider views are decorative and receive no content description.
- Article images remain decorative because the article text identifies the target.
- New setting rows use the existing accessible setting components and minimum touch sizes.
- Slider values are exposed in their visible labels, including percent or dp units.

## Performance

- Do not create per-item surface bitmaps; use Glance colors and layout primitives.
- Do not reload feed data when only appearance settings change.
- Request article bitmaps at the display target appropriate to the selected image mode.
- Preserve the current lazy feed list and asynchronous image loading.
- Updating any setting triggers the existing widget update path once; no polling or background service is introduced.

## Error Handling

- Clamp stored and incoming opacity values to `0..100`.
- Clamp radius values to `0..32` and normalize them to the supported two-dp step.
- Fall back safely on unknown enum values.
- Preserve the current custom-color validation and reset behavior.
- Omit failed article images without affecting row text or click behavior.
- If automatic contrast cannot know an external background, use the existing themed underlay and allow the user to choose explicit light or dark text.

No new user-visible error state is required.

## Internationalization

Add English source strings for:

- Card appearance section title.
- Card surface color and themed-default label.
- Card surface opacity label.
- Card corner radius label.
- Item separation and its three values.
- Divider opacity label.
- Image sizing and its two values.

No strings are hard-coded in Kotlin. Generated translation bindings are refreshed using the repository script. Other-language translations are not authored manually.

## Testing Strategy

### Unit tests

Add behavior-focused tests covering:

- Repository defaults reproduce the existing card appearance when new keys are absent.
- Every card appearance value persists and is emitted through its flow.
- Invalid stored enum names fall back without throwing.
- Opacity and radius values are clamped and radius steps are normalized.
- Surface opacity zero resolves to a transparent card.
- Automatic card text uses the composited card-plus-widget background.
- Explicit light and dark text modes override automatic contrast for cards.
- Divider color derives from secondary text color with the selected opacity.
- Row-height calculation accommodates the minimum and maximum supported font scales.
- Thumbnail and fill-height modes produce their specified image target geometry.

Pure appearance and geometry tests belong in Android unit tests next to the existing widget color tests. Repository tests should use in-memory settings and assert observable values rather than implementation details.

### Configuration UI coverage

Update Compose previews to cover:

- Compatibility-default cards.
- Transparent cards with dividers and fill-height images.
- A custom translucent card surface on both light and dark preview backdrops.

If the existing Maestro/debug-seed infrastructure can open the Android widget settings screen without production-only hooks, add a flow that verifies conditional controls and preview changes. If launcher-hosted widget placement remains impractical in Maestro, document that limitation in the Maestro test catalogue rather than adding a brittle launcher automation.

### Device verification

Validate the actual AppWidget on a connected Android device at minimum, default, and maximum supported font scales:

1. Confirm an untouched Card widget matches the pre-change appearance.
2. Confirm zero card opacity removes every opaque article slab.
3. Confirm dividers appear only between entries and remain subtle over light and dark backgrounds.
4. Confirm fill-height images align with row top and bottom, remain square, center-crop, and do not distort.
5. Confirm missing and failed images collapse cleanly.
6. Confirm every row remains clickable across text and image regions.
7. Confirm the configuration preview is materially consistent with the placed widget.
8. Confirm the transparent FeedFlow widget can be layered above a launcher-provided blurred surface without opaque artifacts.

### Build gates

After implementation:

1. Run `.scripts/refresh-translations.sh`.
2. Run the focused Android widget unit tests during iteration.
3. Run `./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug` and deploy the resulting APK for visual verification.
4. Run `./gradlew --quiet --console=plain detekt allTests` before handoff.

## Expected File Areas

Implementation is expected to touch, but is not rigidly limited to:

- `shared/src/androidMain/.../WidgetSettingsRepository.kt`
- A new `shared/src/commonMain/.../domain/model/WidgetCardAppearance.kt`
- `androidApp/src/main/.../widget/WidgetSettingsState.kt`
- Both Android widget settings view models and their callback plumbing
- `WidgetSettingsContent.kt`
- `WidgetPreviewSection.kt`
- `WidgetContent.kt`
- `components/WidgetFeedListItem.kt`
- Widget appearance, persistence, and geometry unit tests
- English i18n resources and generated translation bindings
- Maestro catalogue or flow if configuration coverage is feasible

No unrelated feed, synchronization, desktop, or iOS behavior should change.

## Pull Request Shape

The upstream pull request should describe the feature as expanded Android widget card customization. It should include before-and-after screenshots demonstrating:

- Existing default cards unchanged.
- A translucent custom-color card layout.
- A fully transparent divider-separated layout with fill-height images.

The PR should explain that transparent surfaces enable composition with launcher-provided backgrounds without naming or depending on a specific launcher. Fonts and launcher palette synchronization remain explicitly outside the change.

## Acceptance Criteria

The implementation is accepted when all of the following are true:

- Existing users see the current Card appearance without changing settings.
- Card color and opacity are independent of the outer widget background.
- A card opacity of zero produces no visible slab behind article content.
- Users can select spacing, dividers, or no separator.
- Dividers never appear after the final article.
- Divider opacity is configurable and uses the resolved secondary text color.
- Card radius is configurable from 0 through 32 dp.
- Users can select current thumbnails or square images that fill row height.
- Fill-height images are not stretched, clipped incorrectly, or loaded only at thumbnail resolution.
- Text-color overrides apply consistently to Card mode.
- Preview behavior matches the placed widget closely enough to make configuration reliable.
- Invalid or missing persisted values cannot crash widget configuration or rendering.
- Unit tests, Android build, device checks, Detekt, and the full test gate pass.
- The implementation contains no Smart Launcher dependency, custom-font work, or unrelated refactoring.
