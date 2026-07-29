# Configurable Widget Card Appearance Design

Date: 2026-07-29

## Summary

FeedFlow's Android widget already allows the outer widget background to be customized, but every article in the `Card` layout still uses a fixed themed slab and a fixed 50 dp thumbnail.

This change has two purposes:

1. Let users personalize the article slabs in the existing `Card` layout.
2. Let users keep the current thumbnail or use a correctly sized square image that fills the row height.

The current Card appearance remains the default. The `List` layout's appearance, geometry, and settings do not change; its shared thumbnail loader participates in the provider-wide sizing and bitmap-safety rules described below.

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
- Changes to the `List` layout's appearance, geometry, settings, or interactions. Its existing thumbnails still use the shared provider-level Exact-mode request and bitmap-budget safeguards.
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

#### Exact size handling and narrow fallback

`FeedFlowWidget` overrides `sizeMode` with `SizeMode.Exact`. Exact mode is required because the Fill-versus-Thumbnail breakpoint depends on the current host width, calculated row height, widget font setting, and Android system font scale; a fixed set of responsive breakpoints cannot represent that calculation reliably.

Glance 1.1.1 composes one `RemoteViews` variant for every exact size represented by the host options on every supported Android version. Android 12 and newer can provide an explicit size list; Android 8 through 11 use the host options to derive the supported size variants, commonly portrait and landscape estimates. `LocalSize.current` is evaluated independently in each size composition. A narrow-to-wide-to-narrow resize must switch Thumbnail fallback to Fill and back to Thumbnail without requiring a settings change, feed refresh, or widget re-addition.

Before using fill-height geometry in each exact-size composition, calculate the available slab width from `LocalSize.current.width` after the existing outer horizontal insets. Fill mode requires room for:

- A 16 dp leading text inset.
- At least 96 dp of readable text width.
- A 16 dp gap between text and image.
- The square image at the calculated row height.

If the available width is smaller than that total, render the selected `FILL_ROW_HEIGHT` mode as `THUMBNAIL` for that widget render. The fallback uses the complete Thumbnail geometry, including its normal row height and 50 dp image request; the stored user preference remains unchanged. The preview uses the same width rule. This makes the 256 dp minimum widget deterministic at large widget or Android system font scales.

If the date is absent or the image fails to load, the resolved row geometry remains stable and text uses the additional horizontal space.

#### Shared provider-wide image budget

`SizeMode.Exact` applies to the whole widget provider, so the request and bitmap policy covers both Card images and the existing 50 dp List thumbnails. List geometry and image mode remain unchanged. Each composition converts its resolved viewport from dp to `displayTargetPx` with `LocalContext.current.resources.displayMetrics` before applying the shared bound.

The budget deliberately does not use the number of articles in the current feed emission. `FeedWidgetRepository` has a fixed maximum of 15 widget articles. Extract that value into a shared `MAX_WIDGET_FEED_ITEMS = 15` constant used by both the repository query and the Android bitmap-budget resolver so the two limits cannot drift.

Every Exact-size composition synchronously copies `LocalAppWidgetOptions.current` into an immutable `WidgetOptionsSnapshot`. A pure `resolveExactSizes(snapshot, currentSize, sdkInt)` helper derives the stable snapshot key and complete size set from that same immutable value using Glance 1.1.1's SDK-specific rules:

1. On API 31 and newer, normalize and deduplicate a nonempty `OPTION_APPWIDGET_SIZES` list and use it when at least one usable size remains.
2. On API 31 and newer when the explicit list is absent, empty, or has no usable sizes, derive portrait and landscape legacy candidates only when all four minimum/maximum width and height fields are positive. If any one of the four fields is missing or zero, use `currentSize` as the single fallback instead of accepting one partial orientation pair.
3. On API 26 through 30, derive the portrait candidate when minimum width and maximum height are positive, and derive the landscape candidate independently when maximum width and minimum height are positive. One valid pair does not require the other.
4. Remove duplicate candidates. If the selected SDK path produces no usable candidate, use `currentSize` as a single fallback size.

The stable resolution key includes the canonical options snapshot and SDK branch so the size set and key always describe the same Glance behavior.

`V` is the resulting size count, and `P = MAX_WIDGET_FEED_ITEMS * V` is the conservative number of serialized article-bitmap payloads. The same article in two size variants counts twice even if both requests resolve to the same dimensions; the budget does not assume Coil or `RemoteViews` bitmap deduplication.

Because the snapshot key and `V` come from one synchronous immutable input, there is no asynchronous size query, `Unresolved` state, retry loop, timer, or image-suppression interval. Because `P` always reserves all 15 article slots, a current feed transition from one to 15 image-bearing articles cannot change the per-payload budget or leave another Exact-size composition holding a bitmap validated under a larger article-count budget.

Use `Long` arithmetic for all byte and pixel-count calculations:

1. `remoteViewsLimitBytes = screenWidthPx.toLong() * screenHeightPx.toLong() * 4L * 3L / 2L`, matching Android's `1.5 * screenBytes` aggregate bitmap ceiling.
2. `deviceArticleBudgetBytes = remoteViewsLimitBytes * 3L / 4L`, reserving 25 percent for non-article bitmaps and safety headroom.
3. `effectiveArticleBudgetBytes = min(6L * 1024L * 1024L, deviceArticleBudgetBytes)`.
4. `payloadCount = MAX_WIDGET_FEED_ITEMS.toLong() * V.toLong()`.
5. `payloadBudgetBytes = effectiveArticleBudgetBytes / payloadCount`.
6. `budgetEdgePx = floor(sqrt(payloadBudgetBytes.toDouble() / 4.0)).toInt()`.
7. Each square request edge is `min(displayTargetPx, 512, budgetEdgePx)`.

If the calculated edge is less than one pixel, omit article images for that update rather than exceed the device limit. Otherwise, every Coil request must use `allowHardware(false)`, prefer `Bitmap.Config.ARGB_8888`, retain exact requested dimensions, and use the same bounded edge for width and height.

Decoder preferences are not treated as proof of the delivered allocation. Before creating `ImageProvider`:

1. Reject hardware-backed results from the delivery path and convert any non-`ARGB_8888` software result, including `RGBA_F16`, to software `ARGB_8888`.
2. Ensure neither bitmap dimension exceeds `budgetEdgePx`; downscale if necessary.
3. Read `bitmap.allocationByteCount.toLong()` and require it to be at most `payloadBudgetBytes`.
4. If the allocation remains too large, calculate a smaller edge from the actual allocation, downscale again as software `ARGB_8888`, and revalidate dimensions, configuration, and `allocationByteCount`.
5. If conversion or further downscaling fails, or the actual allocation still exceeds `payloadBudgetBytes`, omit the image rather than pass it to `ImageProvider`.

Only a validated software `ARGB_8888` bitmap may enter the `RemoteViews` payload. Because every delivered article bitmap is individually checked against `payloadBudgetBytes`, the aggregate guarantee is based on actual allocations rather than the four-byte estimate alone.

The viewport remains the resolved dp size, so the host may perform limited upscaling when the explicit memory bound is smaller than the display target. On a 480 by 800 pixel display, the Android ceiling is 2,304,000 bytes and the 25-percent reservation leaves an article budget of 1,728,000 bytes rather than 6 MiB.

Bitmap state and asynchronous loading use a request key containing the image URL, final bounded dimensions, and `payloadBudgetBytes`. State is remembered by that complete key, and the loading effect applies a result only if the full key is still current. When the immutable options snapshot, `V`, display metrics, geometry, or the per-payload budget changes, the old state is discarded and an in-flight stale result cannot restore a bitmap validated against an older budget. A budget change must revalidate or reload even when target-size capping or integer rounding leaves the bounded pixel dimensions unchanged. Changes in the current feed count do not change the budget because all 15 repository slots are always reserved.

Exact mode may run an image effect independently for each size composition. Identical List requests may be served from Coil's cache, but correctness and aggregate memory calculations do not rely on cache reuse. Every List and Card article-by-variant payload remains included in `P`.

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
- Failed image loads and bitmap results that cannot be converted and validated within their allocation budget omit the image while leaving article text and click action intact.
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

- `FeedWidgetRepository` and the bitmap resolver use the same `MAX_WIDGET_FEED_ITEMS = 15` constant.
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
- `FeedFlowWidget` uses `SizeMode.Exact`, and the width resolver produces Thumbnail, Fill, then Thumbnail for a narrow-to-wide-to-narrow size sequence without another settings or feed event.
- The 96 dp readable-text rule selects Fill mode when it fits and the complete Thumbnail fallback when it does not, including a 256 dp widget at maximum font scales.
- Thumbnail and fill-height modes produce the correct viewport and bounded request dimensions.
- Fixed-capacity tests prove current feeds with one and 15 image-bearing articles use the same `MAX_WIDGET_FEED_ITEMS * V` payload count, per-payload budget, and request identity for otherwise identical images.
- A budget-key regression test changes `V` or the device-derived budget so `payloadBudgetBytes` decreases while the bounded edge remains unchanged; the old bitmap is removed, its in-flight result is rejected, and only a bitmap validated against the new budget can render.
- Pure options-snapshot tests cover API 31+ explicit size lists, duplicate/invalid entries, complete four-field legacy fallback, and an empty explicit list with only one complete orientation pair—which must use `currentSize`.
- API 26-30 tests cover independently valid portrait and landscape pairs, including either pair without the other, plus the `currentSize` fallback when neither pair is valid.
- The same immutable `WidgetOptionsSnapshot` and SDK branch deterministically produce both the stable resolution key and `V`; size resolution uses no manager query, asynchronous unresolved state, delay, retry, or polling.
- Device-limit tests use `Long` arithmetic and cover a 480 by 800 pixel display, the 6 MiB upper cap, and large dimensions without overflow.
- Loader tests assert `allowHardware(false)` and the `ARGB_8888` preference, then inject hardware and `RGBA_F16` results to verify conversion or omission before `ImageProvider`.
- Allocation tests use bitmaps whose `allocationByteCount` exceeds `width * height * 4`, verify further downscaling and revalidation, and verify omission when no compliant software bitmap can be produced.
- Bitmap-budget tests sum actual validated `allocationByteCount` values across one and multiple exact-size variants. A 15-image update remains within the effective device-derived budget and every decoded edge remains at or below 512 pixels.
- A List regression test uses multiple Exact-mode variants and 15 image-bearing articles, preserves the 50 dp List viewport and existing layout, and subjects every List payload to the same reactive aggregate budget and request-key rules.

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
6. At maximum widget and Android system font scales, resize narrow to wide to narrow. Verify the 256 dp state uses the complete Thumbnail fallback with at least 96 dp for text, the wide state uses Fill, and the final narrow state returns to Thumbnail without a refresh or widget re-addition.
7. Verify changing between image modes and exact sizes requests appropriately bounded images.
8. On Android 12 or newer, keep one active widget session while changing from one to 15 image-bearing articles and from one to at least two host-provided exact sizes. Verify the article-count change leaves the fixed-capacity per-payload budget unchanged, while the new immutable options snapshot synchronously updates `V` for the complete exact-size set.
9. On Android 8 through 11, verify the complete legacy portrait/landscape size result is counted for both Card and List aggregate budgets rather than assuming one composition.
10. On a 480 by 800 pixel display, verify every generated Card variant renders within the device-derived budget without bitmap-allocation, transaction, or `RemoteViews` failure.
11. Repeat the 15-article, multi-size update in `List` layout and verify its 50 dp thumbnails, layout, and interactions remain unchanged while all variants stay within the same aggregate budget.
12. Verify missing and failed images collapse horizontally without breaking the row.
13. Verify article rows remain clickable across text and image regions.
14. Compare the preview with the placed widget at minimum, default, and maximum widget font settings.

## Expected File Areas

Implementation is expected to touch:

- `shared/src/commonMain/.../domain/feed/FeedWidgetRepository.kt` to expose the existing 15-item maximum as a shared constant
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
- `FeedFlowWidget` uses `SizeMode.Exact`; narrow-to-wide-to-narrow resizing changes Thumbnail fallback to Fill and back without another settings or feed event.
- A fill-height image never reduces readable text below 96 dp; the renderer uses the complete Thumbnail fallback when necessary.
- The provider-wide payload count always reserves `MAX_WIDGET_FEED_ITEMS = 15` for every exact-size variant, so feed emissions cannot create independently changing per-variant article budgets.
- One immutable `WidgetOptionsSnapshot` synchronously produces both its stable key and complete `V` using Glance's distinct API 31+ and API 26-30 fallback rules; image sizing uses no asynchronous manager query, unresolved interval, retry loop, timeout, or polling.
- The effective article-bitmap budget is the smaller of 6 MiB and 75 percent of Android's device-derived `RemoteViews` bitmap ceiling, calculated with `Long` arithmetic.
- Only validated software `ARGB_8888` bitmaps whose actual `allocationByteCount` fits the per-payload budget reach `ImageProvider`; incompatible or oversized results are converted, further downscaled, or omitted.
- The image request/state key includes `payloadBudgetBytes`, so a reduced budget invalidates an older bitmap even when its bounded dimensions are unchanged.
- Across every host-provided exact-size variant, the sum of actual allocations for up to 15 image-bearing articles stays within the effective aggregate budget and 512-pixel per-edge cap without assuming bitmap deduplication.
- List rows retain their current 50 dp thumbnail geometry, appearance, and interactions while participating in the same Exact-mode aggregate budget and request-key policy.
- Missing or failed images do not leave an empty image region.
- Preview controls and sample rows show every Card option without clipping.
- Invalid values for the new Card preferences cannot crash configuration or rendering.
- The implementation contains no launcher integration, custom-font work, List-layout changes, or unrelated refactoring.
