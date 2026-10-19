# FeedFlow weekly upstream sync — 2026-10-19

Authority: already_authorized. Specification: ../specs/2026-08-30-weekly-upstream-sync-design.md.

Baseline: origin/master a0daab2a852962e5996c438f5ad3abfd844ed4eb; upstream/main 69385652c0ed2f1a06d00e4c73aeaa0eed50bf5a. Divergence: 47 fork-only / 34 upstream-only commits. Exact commit ranges remain reproducible from these immutable heads.

## Source integration — COMPLETE

Requirements: history-preserving merge, all ten fork widget contracts, upstream architecture and behavior, signing workflow. Resolved the Android-owned UI import migration and catalog conflicts without dropping fork controls or upstream coverage. Renamed the fork card scenario from REG-178 to unused REG-181. Source review confirms all fork widget changes in this integration are UI-package import migrations; exact sizing, card/image/divider controls, freshness, persistence, preview, image-budget policy and regression tests remain intact. No unresolved Git entries or conflict markers; catalog includes all 178 ordinary platform flows (device execution excluded). The signing workflow retains secrets and publication safeguards, adding upstream's Linux virtual display/window-manager prerequisite for desktop tests. No duplicated state ownership or stacked workaround was introduced; no consolidation needed. No remaining source-integration blockers or tracked deferrals.

## Release validation — COMPLETE

Requirements: translation refresh, focused complete-widget regression tests, detekt allTests :androidApp:assembleFDroidRelease. Use the installed shared Gradle supervisor and required resource profile. Acceptance: inspect successful current-tree results and lifecycle verification. No device operations. No tracked deferrals.

Verified: translation generation passed (ticket 1499); complete focused shared widget settings/migration/restore and Android widget tests plus repository detekt passed (ticket 1501). XML reports contain zero failures, errors, or skips. Both supervisors verified gate acquisition, two workers/no parallelism/3 GiB heap, native single-use daemon lifecycle and daemon exit; focused tests use one 512 MiB fork. Initial JBR 25 cache-directory finalization failed; diagnostic rerun provisioned it successfully without application or toolchain-policy changes. Gradle host remains Java 21; desktop uses its required JBR 25.

Verified: full detekt/allTests/F-Droid release assembly passed in ticket 1502 with build_exit_code=0, helper exit_code=0, gate/profile verification and post_build_daemons.exit_verified=true. Desktop/shared/Android reports contain no failures or errors; three macOS-only iCloud assumptions skip on Windows. The final APK was assembled. Quiet-period support requests were inspected and acknowledged against fresh worker/compilation diagnostics; no cancellation, weakened check or memory increase was used. Observed separate Kotlin compiler uses -Xmx3g. Native environment hints are not claimed as universal concurrency enforcement. No remaining release-validation blockers or tracked deferrals; source-integration debt assessment remains applicable.

Translation audit: changed app locales retain missing keys relative to English (bg/de/et/it/zh-rCN 96 each; fr/vi 102 each; ro 103; uk 105; no extra keys). No changed locale is complete. Italian App Store subtitle gains terminal punctuation; no screenshot copy/assets changed. Live Google Play credentials and App Store/Microsoft Store clients are unavailable, so live store drift is not claimed. Store uploads, screenshot generation, and upstream-author board writes are outside this fork-release scope. Translation generation remains required before checks.

## Publication and cleanup — ACTIVE

Requirements: refresh refs, preserve history and signing identity, push verified master and unused version-aligned tag, successful private-signing workflow, independently verify downloaded APK/checksum/version/source/signer, active heartbeat, remove expendable temporary checkout transactionally. Acceptance: live release and source evidence plus completed cleanup. No tracked deferrals.

Fresh origin/master and upstream/main remain at the recorded baseline heads. Selected unused tag: 1.19.0-widget-card.3, aligned with source 1.19.0. Advancement blockers (internal): commit/push, signing workflow, independent published-asset verification and cleanup remain; execute those within this stage. No unrelated application, device, store, or account changes are authorized.
