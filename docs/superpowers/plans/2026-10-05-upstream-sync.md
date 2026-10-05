# FeedFlow weekly upstream sync — 2026-10-05

Authoritative specification: `../specs/2026-08-30-weekly-upstream-sync-design.md`.
Scope is already authorized by the weekly heartbeat. No ADB, installation, application launch, or private signing-key retrieval.

## Exact source window

- Fork master: `f372c8d8d92abbfc20733070b7c819d14f3d7e65`.
- Upstream main: `62d9d05aadba057fa346ab264520e1110ee14347`.
- Fork-only commits: 45; upstream-only commits: 34 (`git rev-list --left-right --count`).
- Clean checkout: `D:\Temp\feed-flow-weekly-sync-2026-10-05`.
- Branch: `codex/sync-upstream-2026-10-05`.
- Upstream version: 1.19.0; next candidate tag: `1.19.0-widget-card.1` (must recheck before push).

## Stages and requirement mapping

1. **COMPLETE — source integration.** Three-way conflicts resolved: upstream locale support and all fork sizing/rendering helpers coexist. No conflict markers or whitespace errors remain. All 164 smoke/regression YAML files match the HTML inventory exactly; Markdown has 71 unique regression rows. Fork widget code differs only in upstream locale injection, and the signing workflow and widget tests/settings/rendering/payload implementations are unchanged. The fork flow was renamed from REG-168 to REG-173 without behavioral changes; device execution is explicitly excluded.
2. **COMPLETE — validation.** Translation generation, focused Android widget tests (gate ticket 126), and `detekt allTests :androidApp:assembleFDroidRelease` (gate ticket 128) passed with exit 0. Inspected 272 XML suites with zero failures/errors, including widget settings, surface/divider rendering, sizing, images, freshness, and shared repository contracts. Local APK metadata is `com.prof18.feedflow`, version 1.19.0. Local signing uses only a disposable validation identity, not the release key. No device testing.
3. **ACTIVE — delivery and cleanup.** Recheck remote heads/tags, commit verified merge, fast-forward master without force, publish through the existing fork workflow, independently verify release APK version/hash/signature continuity, and transactionally remove successful disposable work.

No tracked deferrals or unresolved source/validation blockers. Live store metadata and screenshot publication are outside this Android fork synchronization scope. Delivery, artifact verification, and cleanup remain required before overall completion.

## Validation evidence

- Translation refresh executes exactly the task in `.scripts/refresh-translations.sh`: `:i18n:kspCommonMainKotlinMetadata --rerun-tasks`. Passed via shared gate (ticket 125, acquired=true, exit=0).
- Repository heap initial sizes of 4 GB exceeded the required 3 GB maximum. Invocation-only overrides use 512 MB initial heaps, 3 GB maxima, and retain ParallelGC, metaspace and heap-dump flags. No persistent build configuration was changed.
- Gradle reported two workers, parallel=false, `-Xmx3g`; test configuration markers reported one fork and 512 MB test heaps. Kotlin/native task-specific effective settings will be distinguished from requested budgets.
- Root-cause inspection found the fork's `FeedFlowWidgetTest` constructor fixture omitted upstream's new `SettingsRepository` argument. Added the required dependency using the fixture's existing construction technique; its exact-size and newest-first assertions are preserved.
- Translation source audit: changed locale XML files have no extra keys; existing untranslated keys retain English fallback, including 26 fork-only/new keys in otherwise current locales. No strings translated by the sync. Italian store-copy changes are preserved from upstream; no screenshot copy or generated Play listing changes, no live store action authorized.
- All three Gradle runs acquired `Local\Darka.AndroidGradleBuildGate` and returned exit 0. The effective Gradle profile was verified; a running Kotlin compiler daemon also showed `-Xmx3g`. No actual memory failure or heap-budget increase occurred. Helper memory-pattern matches were the requested `HeapDumpOnOutOfMemoryError` JVM option, not an OOM event. Native concurrency was not claimed as verified.
- Fresh fetch before delivery confirmed unchanged fork/upstream heads and no existing `1.19.0-widget-card.*` tag. Weekly automation configuration is ACTIVE.
