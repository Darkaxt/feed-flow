# Weekly upstream synchronization — 2026-10-26

Authority: `../specs/2026-08-30-weekly-upstream-sync-design.md` and the weekly task instructions. No ADB, installation, or APK launch.

Fetched origin/master: `88255b1543928563da968302bf21dfda5fd8a755`.
Fetched upstream/main: `478079e6f750b9f38f8be5fe4681a29062e3b080`.
Initial divergence: 48 fork-only and 72 upstream-only commits. Exact commit inventory is retained in task diagnostics until delivery.

## Stages

1. Source integration — COMPLETE. Retained fork rendering/persistence/sizing/image safety; adopted upstream ReadOnlyComposable annotation and synchronous keyed SideEffect. Preserved upstream Cloudflare recovery and tests. Renumbered fork Maestro coverage to REG-183, retaining upstream REG-181/182. Regenerated the conflicting desktop dependency catalog from the merged graph, rather than choosing either generated side. Fork signing workflow is byte-for-byte unchanged.
2. Verification — COMPLETE. Translation refresh and desktop library export passed through ticket 1771. Detekt and focused widget regressions passed through ticket 1775. Full `detekt allTests :androidApp:assembleFDroidRelease` passed through ticket 1777 (build/helper exit 0, verified daemon exit; inspected XML has zero failures/errors). Local FDroid artifact metadata confirms com.prof18.feedflow version 1.20.0. Desktop focus tests were visible; no APK was installed/launched and no ADB command ran.
3. Delivery — ACTIVE. Source merged and pushed as 683174867afe78ba32c2ab50e9fb8652cc49b44f; immutable tag 1.20.0-widget-card.1 identifies failed workflow 40291541918. Its Linux desktop allTests result has one failure, but quiet output omitted identity/details and no XML artifact was retained. Add tested JUnit failure reporting and a nonpublishing manual diagnostic run to obtain the evidence; preserve every required release gate. No release was published. Independent asset retrieval is additionally impeded by release-assets.githubusercontent.com resolving to 0.0.0.0; user was asked to restore allowed DNS reachability, without bypassing or changing filters.

## Reconciliation

Stage 1 owns contracts 1–10 and preservation of upstream behavior. Stage 2 proves contracts 1–9 and prepublication build correctness. Stage 3 proves contract 10 and all publication/cleanup acceptance criteria. No required work is deferred outside these stages.

All prepublication gates passed. Current blockers: none. Tracked deferrals: none. The ten fork contracts reconcile to unchanged widget behavior plus the verified immutable-modifier adaptation; signed publication and cleanup remain the final acceptance criteria.

## Translation/store audit

Changed app locales: German, French, Italian, Romanian, Vietnamese, Simplified Chinese. Missing English keys respectively: 41, 41, 41, 42, 41, 111; no extra keys. Normal English fallback remains; translators own translations. Translation code refreshed before checks.

Romanian store/screenshot source copy and upstream 1.19.0 release notes changed. Live stores unavailable: no Play config or asc/pcenter CLI. No store upload or screenshot generation is authorized or needed for this GitHub APK release. Upstream marketing actions (outside this fork delivery): shorten Romanian title (33/30), Play short description (104/80), App Store subtitle (42/30), and promotional text (224/170) before store synchronization. The upstream Obsidian Base is unavailable here; no external board edits were made.

Resource profile: explicit Xms512m/Xmx3g replaces the repository's 4 GiB initial heaps, two workers/no parallel, Kotlin 2.4.10 in-process to avoid persistent compiler scratch. Gate profile verified, one 512 MiB fork/fail-fast on configured tests. No device operations.

Detekt 2 integration findings resolved without suppressions or gate changes: immutable modifier composition replaces two local mutable modifiers; the bitmap hardware assertion explicitly groups its existing conjunction. Existing slab rendering and bitmap safety tests passed. Detekt emitted expect/actual analysis warnings and an asynchronous IntelliJ classloader cleanup exception, but completed its full task graph successfully; these are diagnostic limitations, not compilation/test failures. The gate's quiet-period support request was inspected and answered with continuing analysis evidence. Catalog JavaScript, all referenced YAML paths, and regenerated dependency JSON were checked independently.
