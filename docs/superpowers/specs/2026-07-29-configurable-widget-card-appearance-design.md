# Configurable Widget Card Appearance Design

Date: 2026-07-29

## Summary

FeedFlow's Android widget already allows the outer widget background to be customized, but every article in the `Card` layout still uses a fixed themed slab and a fixed 50 dp thumbnail.

This change has two purposes:

1. Let users personalize the article slabs in the existing `Card` layout.
2. Let users keep the current thumbnail or use a correctly sized square image that fills the row height.

The current Card appearance remains the default. The `List` layout and unrelated widget behavior do not change.

## Goals

- Configure the Card slab's color, opacity, corner radius, and separation.
- Support spaced slabs, divider-separated slabs, and slabs with no separator.
- Allow fully transparent slabs independently of the outer widget background.
- Keep the current 50 dp thumbnail as the compatibility default.
- Add a square, center-cropped image mode that fills the rendered row height without stretching or reusing the 50 dp thumbnail request; decoded size remains subject to an explicit widget bitmap budget.
- Make the configuration preview use the same normalized settings, separation rules, and image geometry as the placed widget.

## Non-goals

- Launcher integrations, wallpaper blur, or launcher color synchronization.
- Custom fonts or other typography features.
- Changes to the `List` layout.
- Per-widget-instance preferences; widget appearance remains global.
- Changes to feed loading, synchronization, filtering, sorting, refresh, or article interactions.
- Replacing Jetpack Glance with hand-written `RemoteViews`.
- Unrelated preference migrations or repository refactoring.

## Existing Behavior

`WidgetContent` resolves the outer widget background from the existing background color and opacity settings. `WidgetFeedItemCard` separately renders every Card item with:

- `GlanceTheme.colors.secondaryContainer`.
- `16.dp` corner radius.
- The existing `Spacing.xsmall` vertical wrapper padding.
- `GlanceTheme.colors.onSurface` for both primary and secondary text.
- A trailing, center-cropped `50.dp` square image with `8.dp` corners.

The Card renderer currently ignores the existing widget text-color mode. The preview similarly hard-codes its Card surface and text colors.

## User-facing Settings

When `Feed Layout` is `Card`, add a `Card appearance` section with these controls:

| Setting | Values | Default | Visibility |
| --- | --- | --- | --- |
| Card surface color | Themed default or custom RGB color | Themed default | Card layout |
| Card surface opacity | 0-100 percent | 100 percent | Card layout |
| Card corner radius | 0-32 dp in 2 dp steps | 16 dp | Card layout |
| Item separation | Spacing, Divider, None | Spacing | Card layout |
| Divider opacity | 0-100 percent | 20 percent | Card layout and Divider separation |
| Image sizing | Thumbnail, Fill row height | Thumbnail | Card layout and images visible |

The existing color-picker dialog is reused. Its title and descriptive labels are parameters so the same implementation can edit either the widget background or Card surface. Resetting the Card color removes the custom value and restores the themed default.

Each new update method in the in-app `WidgetSettingsViewModel` follows the existing widget-setting pattern: ignore an unchanged value; otherwise update state, persist the normalized value, and call `WidgetUpdater.update()` exactly once for that effective callback. This keeps placed widgets current immediately. The widget configuration view model continues to persist through its existing callbacks, and the existing add-widget confirmation behavior does not change.

## Compatibility

Missing Card preferences resolve to:

- Themed `secondaryContainer` surface.
- 100 percent surface opacity.
- 16 dp corner radius.
- `SPACING` separation.
- 20 percent divider opacity.
- `THUMBNAIL` image sizing.

With those defaults and `AUTOMATIC` text color, the placed widget must retain the current Card rendering, including themed `onSurface` for both text roles. This compatibility path avoids a subtle color or secondary-alpha change after upgrade.

The existing `Light` and `Dark` text modes will begin applying to Card text. That is an intentional correction needed for readable transparent slabs; it may change Card text for an existing user who already selected one of those modes. It does not add a new text setting.

No preference migration is required.

## Settings Model and Persistence

Add an Android-only `WidgetCardAppearance` model under `shared/src/androidMain/.../domain/model/`:

- `surfaceColor: Int?`
- `surfaceOpacityPercent: Int`
- `cornerRadiusDp: Int`
- `itemSeparation: WidgetCardItemSeparation`
- `dividerOpacityPercent: Int`
- `imageSizing: WidgetCardImageSizing`

`WidgetCardItemSeparation` contains `SPACING`, `DIVIDER`, and `NONE`. `WidgetCardImageSizing` contains `THUMBNAIL` and `FILL_ROW_HEIGHT`.

`WidgetCardAppearance` is scoped to `androidMain` because only the Android widget consumes it. It represents normalized user settings rather than theme-resolved renderer colors.

Persist the fields in `WidgetSettingsRepository` using:

- `WIDGET_CARD_SURFACE_COLOR`
- `WIDGET_CARD_SURFACE_OPACITY_PERCENT`
- `WIDGET_CARD_CORNER_RADIUS_DP`
- `WIDGET_CARD_ITEM_SEPARATION`
- `WIDGET_CARD_DIVIDER_OPACITY_PERCENT`
- `WIDGET_CARD_IMAGE_SIZING`

Enum values are stored by enum name. Absence of `WIDGET_CARD_SURFACE_COLOR` means the themed default.

Both reads and writes normalize Card values before returning, storing, or emitting them:

- Opacity is clamped to `0..100`.
- Radius is clamped to `0..32` and normalized to an even value; an odd midpoint rounds upward.
- Unknown enum names fall back to their defaults.
- A custom surface color is treated as opaque RGB; transparency comes only from `surfaceOpacityPercent`.

The repository exposes one coherent Card appearance to `FeedFlowWidget`, `WidgetConfigurationViewModel`, and the in-app widget settings view model. The model is included in `WidgetSettingsState` so the settings UI and preview consume the same normalized values.

## Surface and Text Resolution

The Card surface is resolved in Android code:

1. Use the custom opaque RGB color when present; otherwise use the renderer's themed `secondaryContainer`.
2. Apply the normalized Card surface opacity.
3. At zero opacity, render no visible slab fill.

The existing widget text-color mode applies to Card text:

- `LIGHT` uses the existing light text colors.
- `DARK` uses the existing dark text colors.
- `AUTOMATIC` derives readable text from the effective Card background.

For automatic contrast outside the compatibility path:

1. Resolve the outer widget background using its configured color and opacity.
2. Composite it over the renderer's opaque themed widget underlay because FeedFlow cannot inspect launcher content behind a transparent widget.
3. Composite the Card surface over that effective outer background.
4. Use the existing luminance-based text-color helper on the result.

When the Card uses the themed default surface at 100 percent opacity and text mode is `AUTOMATIC`, use themed `onSurface` for both text roles to preserve the current placed-widget appearance.

The Compose preview and Glance renderer use the same normalization and compositing algorithm, while each supplies its equivalent theme colors. The preview wallpaper is visual context only; it is not treated as observable launcher content when resolving automatic text.

## Separation and Corner Radius

`WidgetFeedItemCard` remains the clickable article slab. Separators are outside the article click target.

### Spacing

`SPACING` preserves the current Card wrapper and modifier order, including the existing `Spacing.xsmall` vertical padding. No divider is rendered. This is the compatibility default.

### Divider

`DIVIDER` removes the Card wrapper's vertical spacing and renders a 1 dp horizontal divider only between adjacent articles.

The divider:

- Is not rendered after the final article.
- Is inset horizontally by 16 dp.
- Uses the RGB of the resolved secondary text color.
- Uses `dividerOpacityPercent / 100f` as its final alpha, replacing rather than multiplying the secondary text alpha.
- Is decorative and has no click action or content description.

### None

`NONE` removes both the Card wrapper's vertical spacing and the divider. Rows remain independently clickable.

The configured radius is applied uniformly to each Card surface in every separation mode using Glance's supported uniform corner radius. In `DIVIDER` or `NONE`, a nonzero per-row radius may reveal the outer background at the edges where rows meet. This is accepted behavior; selecting a zero radius produces continuous straight edges without requiring grouped or per-corner clipping support.

## Image Sizing

The existing `Hide article image` setting remains authoritative. If images are hidden or an article has no image URL, no image region is rendered and text uses the available width.

### Thumbnail

`THUMBNAIL` preserves the current rendering:

- 50 dp square viewport.
- Center crop.
- 8 dp uniform corner radius.
- Existing Card placement, padding, and row-height behavior.

The request target is 50 dp converted to pixels with the widget context's display metrics.

### Fill row height

`FILL_ROW_HEIGHT` uses a deterministic fixed row height because Glance cannot measure a sibling and then size the image from that runtime measurement.

The layout contract is:

- Feed source: at most one line.
- Title: at most two lines.
- Date: at most one line when present.
- The text column has 16 dp top and bottom padding.
- The text retains a 16 dp horizontal inset from the slab edge and image.
- The row height is a centralized conservative calculation that accommodates those maximum lines, their spacing, the selected widget font sizes, and `Resources.configuration.fontScale`.
- The row and image viewport use the same calculated height.
- The image viewport is square, so its width equals the row height.
- The image touches the row's top, bottom, and trailing bounds.
- The image is center-cropped and never stretched.
- The image uses the configured Card radius as its uniform Glance corner radius. Uniform rounding may also round the image's inner corners; per-corner clipping is outside this change.

Before using fill-height geometry, calculate the available slab width from `LocalSize.current.width` after the existing outer horizontal insets. Fill mode requires room for:

- A 16 dp leading text inset.
- At least 96 dp of readable text width.
- A 16 dp gap between text and image.
- The square image at the calculated row height.

If the available width is smaller than that total, render the selected `FILL_ROW_HEIGHT` mode as `THUMBNAIL` for that widget render. The fallback uses the complete Thumbnail geometry, including its normal row height and 50 dp image request; the stored user preference remains unchanged. The preview uses the same width rule. This makes the 256 dp minimum widget deterministic at large widget or Android system font scales.

If the date is absent or the image fails to load, the resolved row geometry remains stable and text uses the additional horizontal space.

Image requests first convert the resolved viewport dimensions through `LocalContext.current.resources.displayMetrics`. They are then bounded by a per-widget bitmap policy. `N` is the number of articles in the widget update that have an image URL while images are enabled:

- Maximum decoded edge: 512 pixels.
- Maximum combined raw article-bitmap payload: 6 MiB per widget update, assuming four bytes per pixel.
- For `N` image-bearing articles, calculate `budgetEdgePx = floor(sqrt((6 * 1024 * 1024) / (4 * max(1, N))))`.
- The square request edge is `min(displayTargetPx, 512, budgetEdgePx)`.

The same bounded edge is used for both request dimensions. The bitmap handed to `ImageProvider` must not exceed that edge in either dimension; an oversized decoder result is downscaled before it enters the `RemoteViews` payload. With 15 image-bearing articles, the calculated requests remain within the 6 MiB raw-pixel budget. The viewport remains the resolved dp size, so the host may perform limited upscaling only when the explicit memory bound is smaller than the display target.

Bitmap state and asynchronous loading are keyed by image URL and the final bounded request dimensions. Switching image mode, widget width, widget font size, density, system font scale, or image-count budget cannot reuse a request with different target dimensions.

## Configuration Preview

The preview must show the selected Card surface color, opacity, radius, separation, divider opacity, image sizing, and text-color mode.

It contains at least two sample articles so all separation modes are visible. Its container may grow with the selected widget font size; it must not clip two fill-height sample rows merely to preserve the current fixed preview height.

The light/dark wallpaper toggle remains to show transparency over contrasting visual backgrounds. Automatic text resolution still uses the renderer's themed fallback, matching what the placed widget can know. Any outline used solely to show preview bounds is preview chrome and must not be presented as part of the placed widget appearance.

## Interaction and Accessibility

- Every article keeps its existing action and independently clickable slab bounds.
- Spacing, transparency, and image mode do not shrink or split the article click target.
- Dividers and article images are decorative.
- New setting controls reuse existing accessible setting components and touch targets.
- Opacity and radius values are shown with percent or dp units.

## Error Handling

- Invalid values for the six new Card preferences fall back or normalize without crashing.
- Failed image loads omit the image and leave the article text and click action intact.
- Fully transparent outer and Card surfaces use the themed underlay only for automatic contrast calculation; they do not render that fallback as an opaque slab.
- No new user-visible error state is required.

## Internationalization

Add English source strings for:

- Card appearance.
- Card surface color and themed-default label.
- Card surface opacity.
- Card corner radius.
- Item separation and its three values.
- Divider opacity.
- Image sizing and its two values.

Do not hard-code strings in Kotlin or manually author other-language translations. Run `.scripts/refresh-translations.sh` after adding the English resources.

## Testing

### Unit tests

Add focused tests for:

- Missing preferences produce the compatibility defaults.
- Every Card value persists and is emitted as part of the grouped appearance.
- Each in-app Card update method calls `WidgetUpdater.update()` once for a changed value and zero times for an unchanged value.
- Card setters and getters normalize opacity, radius, custom color alpha, and unknown enum values.
- The default automatic Card resolves to the current themed surface and themed `onSurface` text.
- Zero Card opacity produces no visible surface color.
- Custom and translucent surfaces use composited automatic contrast.
- Existing Light and Dark modes override automatic Card text.
- Divider alpha is the configured final alpha and no divider is emitted after the final item.
- Row-height calculation accommodates minimum, default, and maximum widget font settings across representative Android system font scales.
- The 96 dp readable-text rule selects Fill mode when it fits and the complete Thumbnail fallback when it does not, including a 256 dp widget at maximum font scales.
- Thumbnail and fill-height modes produce the correct viewport and bounded request dimensions.
- Changing target dimensions or image-count budget changes the image-loading key.
- A 15-image update keeps the calculated raw bitmap payload at or below 6 MiB and every decoded edge at or below 512 pixels.

### Preview coverage

Add Compose previews for:

- Compatibility-default Cards.
- Transparent Cards with dividers and fill-height images.
- A custom translucent Card surface over light and dark preview backdrops.

### Device verification

On a connected Android device:

1. Compare the default Card widget with a pre-change baseline.
2. Verify zero Card opacity removes the article slabs.
3. Verify Spacing, Divider, and None, including no divider after the final article.
4. Verify radius 0 and 32 dp.
5. Verify fill-height images align to row bounds, remain square, center-crop, and do not stretch.
6. Resize to the 256 dp minimum width at maximum widget and Android system font scales and verify the complete Thumbnail fallback leaves at least 96 dp for text.
7. Verify changing between image modes requests an appropriately bounded image.
8. Update a widget containing 15 image-bearing articles and verify it renders without bitmap-allocation or `RemoteViews` failure.
9. Verify missing and failed images collapse horizontally without breaking the row.
10. Verify article rows remain clickable across text and image regions.
11. Compare the preview with the placed widget at minimum, default, and maximum widget font settings.

## Expected File Areas

Implementation is expected to touch:

- `shared/src/androidMain/.../WidgetSettingsRepository.kt`
- `shared/src/androidMain/.../domain/model/WidgetCardAppearance.kt`
- `androidApp/src/main/.../widget/WidgetSettingsState.kt`
- Both Android widget settings view models and their callback plumbing
- `WidgetSettingsContent.kt`
- `WidgetPreviewSection.kt`
- `FeedFlowWidget.kt`
- `WidgetContent.kt`
- `components/WidgetFeedListItem.kt`
- `androidApp/src/debug/.../E2eSeedActivity.kt` so debug reset restores all new defaults
- Focused widget appearance, persistence, and geometry tests
- English i18n resources and generated translation bindings

No unrelated feed, synchronization, desktop, iOS, or launcher behavior changes.

## Verification Gates

After implementation:

1. Run `.scripts/refresh-translations.sh`.
2. Run focused Android widget and repository tests during iteration.
3. Run `./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug` for device verification.
4. Run `./gradlew --quiet --console=plain detekt allTests` before handoff.

## Acceptance Criteria

- Default Card surface, spacing, radius, thumbnail geometry, and automatic themed text match the current placed widget.
- Card color and opacity are independent of the outer widget background.
- Zero Card opacity renders no article slab fill.
- Users can select Spacing, Divider, or None.
- Dividers use the configured final opacity and never appear after the final article.
- Card radius is configurable from 0 through 32 dp.
- Existing Light and Dark widget text modes apply to Card text.
- Thumbnail mode remains 50 dp and visually unchanged.
- Every effective in-app Card setting change updates placed widgets exactly once; unchanged callbacks do not update them.
- Fill-height images are square, center-cropped, aligned to row bounds, and requested at display size up to the explicit bitmap limits.
- A fill-height image never reduces readable text below 96 dp; the renderer uses the complete Thumbnail fallback when necessary.
- Image mode, geometry, or image-count budget changes cannot reuse a request with different bounded dimensions.
- Up to 15 image-bearing articles stay within the 6 MiB raw bitmap budget and 512-pixel per-edge cap.
- Missing or failed images do not leave an empty image region.
- Preview controls and sample rows show every Card option without clipping.
- Invalid values for the new Card preferences cannot crash configuration or rendering.
- The implementation contains no launcher integration, custom-font work, List-layout changes, or unrelated refactoring.
