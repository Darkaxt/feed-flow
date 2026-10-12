# FeedFlow upstream sync — 2026-10-12

Authority: already_authorized; specification: ../specs/2026-08-30-weekly-upstream-sync-design.md including the 2026-10-12 clarification.

Baseline: origin/master 2cc10995bb9e817f9c36766b4a5f462dd633aba3; upstream/main f3995c124235f8213546e8f1acf9150408745e55. Divergence: 46 fork-only / 110 upstream-only commits, exact list in the task diagnostic checkout.

## Source integration — COMPLETE

Map: widget contracts 1–10, reviewed history-preserving merge, generated-source handling, no device operations. Resolve upstream per-widget settings and refresh/filter controls with fork surfaces/freshness/exact sizing/image safety; adapt persistence, UI and E2E coverage. Evidence: conflict-free source, preserved release workflow, targeted regression verification. No tracked deferrals.

Verified: no unresolved merge entries or source conflict markers; fork release workflow unchanged. Focused shared settings/migration/restore tests and Android widget tests passed with no failures, errors, or skips. Card controls and color-picker labels are retained alongside upstream filters/refresh controls. Restore publication regression was reproduced and fixed. Exact sizing, transparent surfaces, inset dividers, full-height image geometry, bitmap safety, preview inputs, and view-model persistence checks passed. REG-178 source replaces the conflicting fork REG-173 identifier; catalog inventory matches disk. No required source-integration criteria remain.

## Release validation — COMPLETE

Map: translation refresh, targeted widget checks, detekt allTests :androidApp:assembleFDroidRelease. Use shared Gradle gate, two workers, 3 GiB budgets and one test fork. Evidence: inspected successful outputs for the integrated tree. No tracked deferrals.

Verified: translation refresh, focused widget checks, repository-wide detekt, and the complete detekt/allTests/F-Droid release gate passed. Final gate ticket 777 acquired Local\Darka.AndroidGradleBuildGate and verified two workers, no parallelism, 3 GiB Gradle/Kotlin heaps, and single 512 MiB test forks. Test reports contain no failures or errors; three native macOS iCloud tests were skipped by upstream platform assumptions. Release assembly and vital lint passed. No device tests or APK installation/launch occurred. No release-validation criteria remain.

## Publication and cleanup — ACTIVE

Map: fresh refs, safe master push, next aligned tag, existing signed workflow, independent published APK/version/checksum/source/certificate verification, active heartbeat, transactional cleanup. Evidence: live GitHub state and downloaded release asset verification. No tracked deferrals.

## Translation audit

Translation refresh completed successfully through the shared build gate. Changed locale resources retain fallback gaps: bg 46, de 24, el 487 (one extra key), et 24, fr 46, gl 60, it 24, ro 25, uk 24, vi 24, zh-rCN 24 missing keys relative to the base resource. No changed non-base locale has a complete key set. These are translation status findings, not justification to remove strings or weaken build gates.

Upstream adds Romanian store and screenshot copy. Google Play live audit credentials are unavailable; live store state is not claimed current or stale. Store uploads, screenshot generation, and device tests are outside this synchronization's GitHub-release scope.
