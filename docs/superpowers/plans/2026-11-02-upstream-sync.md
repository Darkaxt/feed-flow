# Weekly upstream synchronization — 2026-11-02

Authority: `docs/superpowers/specs/2026-08-30-weekly-upstream-sync-design.md` and the weekly heartbeat; already authorized through verified signed delivery. No ADB, APK installation or launch.

Initial fetched heads: origin/master `c1a705d36996d06856718c8a174f78e3bb9693a9`, upstream/main `c2f17694af5568b24cd4aff3f001bb3073851b35`. Fork-only 51; upstream-only 154. Exact commit sets remain recoverable using `git log origin/master..c2f17694` and its reverse against the recorded origin head.

Temporary standalone clone: `D:\Temp\feed-flow-weekly-sync-2026-11-02`; D: exception for sustained/small-file build I/O. Cleanup transaction `5eb92203d6e3f38eb2513646124b70c6`.

## Stages and reconciliation

1. **COMPLETE — Source integration.** Reviewed merged upstream and retained all ten widget/signing contracts. Adopted generated-license architecture; translation refresh passed ticket 1829; focused widget regression checks passed ticket 1830 with verified daemon exits. Translation audit recorded below. No blockers or tracked deferrals; no material consolidation need found.
2. **COMPLETE — Prepublication verification.** Final full gate ticket1837 passed `detekt allTests :androidApp:assembleFDroidRelease`, effective resource/fail-fast profile and daemon exit verified. Inspected all JUnit XML: zero failures/errors. First full gate1831 exposed invalid-directory activation through the long-path fallback; explicit regression failed1832, fixed by validating the data directory, then all nine real socket tests passed1834. Final merged code passed the full gate. No blockers or tracked deferrals; no material consolidation need found.
3. **ACTIVE — Signed delivery.** Freshly fetched heads unchanged. Commit/push without force; next unused source-version-aligned tag is `1.20.0-widget-card.3`. Existing signing workflow must succeed, followed by independent APK/checksum/version/source/certificate/resource verification. Confirm weekly task active and clean expendable work.

Conflict decisions: retain both Card color-picker label model and upstream font-scale range. Adopt upstream X11 window-settlement test with unchanged width/height bounds and diagnostic messages. Remove committed desktop license JSON because upstream now generates it under build resources from resolved dependencies; no source license data is discarded.

Signing contract: private key stays in Actions; permanent certificate SHA-256 `f6d929b0e4e6aa7f147e6acd941050a9ac433fc45030687003b6e2e2859248fd`.

Review: only the widget font slider changes versus the last verified fork widget implementation (upstream whole-value step correction). All 17 existing widget regression source files remain, rendering/persistence/sizing/bitmap/payload/freshness implementations are unchanged. Upstream's R8 ActionCallback constructor keep-rule is retained. Release workflow is unchanged. No material consolidation need found.

Translation audit: 18 changed localized app-string sets have no extra keys; normal English fallback remains (missing counts: bg24, cs198, de44, et83, fr24, gl184, in191, it67, lv319, pl189, pt-rBR215, ro48, ru190, tr44, uk148, vi47, zh-rCN137, zh-rTW199). Romanian store source changed; no generated Play listing or screenshot-copy changes. Live stores unavailable because gplay/asc/pcenter are not installed; upstream Obsidian Base is not present. Existing Romanian title/short description/subtitle/promo field-limit findings remain translator-owned, not a blocker for the GitHub fork APK. No store upload or screenshot generation performed. Upstream verbatim font license text contains trailing spaces; preserved rather than rewriting legal inputs for a whitespace-only check.

Second full run1835 hit a native provider-surface visibility timeout (Google Drive/light). Added failure-only tracked-window/node coordinate diagnostics without changing assertions, timeouts or product behavior. Isolated complete ProviderContentSpectreTest passed ticket1836 with verified profile and daemon exit; no production UI fix claimed. Full gate must pass before publication.
