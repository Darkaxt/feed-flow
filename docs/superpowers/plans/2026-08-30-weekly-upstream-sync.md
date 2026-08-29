# FeedFlow Weekly Upstream Synchronization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Merge the current `prof18/feed-flow` upstream history into `Darkaxt/feed-flow`, preserve and verify the configurable Android widget fork, publish the aligned fork release, and create a weekly manual Codex synchronization heartbeat.

**Architecture:** Preserve the fork's published history by merging `upstream/main` into a dated branch based on `origin/master`, resolving only the three content conflicts identified by `git merge-tree`, and validating the complete merged tree before advancing `master`. Publication remains tag-driven through the existing fork signing workflow; future synchronization is initiated by a weekly Codex heartbeat that repeats the same fail-closed process rather than by a blind GitHub merge job.

**Tech Stack:** Git, GitHub CLI and Actions, Kotlin Multiplatform, Android/Jetpack Glance, Gradle, Maestro documentation, Codex heartbeat automations.

---

## File Map

- `docs/superpowers/specs/2026-08-30-weekly-upstream-sync-design.md` — approved safety and acceptance contract.
- `docs/superpowers/plans/2026-08-30-weekly-upstream-sync.md` — executable synchronization checklist.
- `version.properties` — merged application version; must resolve to upstream `1.17.0`.
- `e2e/maestro/maestro-e2e-tests.md` — human-readable flow inventory; retain upstream additions and the fork's widget-card regression entry.
- `e2e/maestro/maestro-e2e-tests.html` — HTML flow inventory; retain the same combined inventory as the Markdown catalog.
- `.github/workflows/fork-android-release.yml` — existing fork-only signed release pipeline; preserve it through the merge.
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/**` — fork widget rendering, sizing, appearance, and image behavior that must remain after merging.
- `androidApp/src/main/kotlin/com/prof18/feedflow/android/settings/widget/**` — widget configuration UI and state behavior.
- `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepository.kt` — persistent widget appearance settings.
- `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/model/WidgetCardAppearance.kt` — persisted appearance model.
- `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepository.kt` — freshness and serialized payload behavior.
- `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/**` — Android widget regression tests.
- `shared/src/androidHostTest/kotlin/com/prof18/feedflow/shared/{data,domain/model}/**` and `shared/src/commonTest/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepositoryTest.kt` — persistence, freshness, and payload tests.

### Task 1: Commit the Approved Plan and Refresh Remote State

**Files:**
- Create: `docs/superpowers/plans/2026-08-30-weekly-upstream-sync.md`

- [ ] **Step 1: Confirm the planning branch is clean except for this plan**

Run:

```powershell
git status --short
git branch --show-current
```

Expected: only the plan is untracked and the branch is `codex/spec-weekly-upstream-sync`.

- [ ] **Step 2: Commit the implementation plan**

Run:

```powershell
git add -- docs/superpowers/plans/2026-08-30-weekly-upstream-sync.md
git commit -m "Plan safe weekly upstream synchronization"
```

Expected: one commit containing only the plan.

- [ ] **Step 3: Refresh both remotes without modifying the worktree**

Run:

```powershell
git fetch --prune origin
git fetch --prune upstream
git rev-list --left-right --count origin/master...upstream/main
git tag --list "1.17.0-widget-card.*"
```

Expected: the ahead/behind count is recorded and no `1.17.0-widget-card.*` tag exists. If refs changed or a tag now exists, recompute the release tag before continuing.

### Task 2: Merge Upstream and Resolve the Known Conflicts

**Files:**
- Modify: `version.properties`
- Modify: `e2e/maestro/maestro-e2e-tests.md`
- Modify: `e2e/maestro/maestro-e2e-tests.html`
- Verify: `.github/workflows/fork-android-release.yml`
- Verify: `androidApp/src/main/kotlin/com/prof18/feedflow/android/widget/**`
- Verify: `androidApp/src/main/kotlin/com/prof18/feedflow/android/settings/widget/**`
- Verify: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepository.kt`
- Verify: `shared/src/androidMain/kotlin/com/prof18/feedflow/shared/domain/model/WidgetCardAppearance.kt`
- Verify: `shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepository.kt`

- [ ] **Step 1: Create the synchronization branch from the approved planning commit**

Run:

```powershell
git switch -c codex/sync-upstream-2026-08-30
git merge --no-ff upstream/main
```

Expected: Git stops only on `version.properties`, `e2e/maestro/maestro-e2e-tests.md`, and `e2e/maestro/maestro-e2e-tests.html`. Any additional conflict must be inspected rather than resolved generically.

- [ ] **Step 2: Resolve the version metadata to the upstream source version**

Set `version.properties` exactly to:

```properties
MAJOR=1
MINOR=17
PATCH=0
```

Run:

```powershell
git add -- version.properties
```

Expected: `version.properties` is staged without conflict markers.

- [ ] **Step 3: Combine the Maestro Markdown inventory**

Inspect all three stages before editing:

```powershell
git show :1:e2e/maestro/maestro-e2e-tests.md
git show :2:e2e/maestro/maestro-e2e-tests.md
git show :3:e2e/maestro/maestro-e2e-tests.md
```

Resolve the file so it contains every current upstream flow and the fork flow, renumbered to the next free ID as `android/regression/166-widget-card-settings.yaml`, exactly once. Preserve the fork's documented limitation that widget home-screen rendering is covered by unit/RemoteViews tests when Maestro cannot inspect launcher-hosted widgets.

Run:

```powershell
rg -n "166-widget-card-settings|widget.*launcher|launcher.*widget" e2e/maestro/maestro-e2e-tests.md
git add -- e2e/maestro/maestro-e2e-tests.md
```

Expected: one physical-flow entry for `166-widget-card-settings.yaml`, the limitation remains documented, and no conflict markers remain.

- [ ] **Step 4: Combine the Maestro HTML inventory**

Inspect all three stages before editing:

```powershell
git show :1:e2e/maestro/maestro-e2e-tests.html
git show :2:e2e/maestro/maestro-e2e-tests.html
git show :3:e2e/maestro/maestro-e2e-tests.html
```

Resolve the HTML catalog to match the Markdown inventory, retaining upstream additions and the widget-card flow exactly once.

Run:

```powershell
rg -n "166-widget-card-settings" e2e/maestro/maestro-e2e-tests.html
git add -- e2e/maestro/maestro-e2e-tests.html
```

Expected: one widget flow entry and no conflict markers.

- [ ] **Step 5: Audit the automatically merged fork surface**

Run:

```powershell
git diff --check
rg -n "^(<<<<<<<|=======|>>>>>>>)" . --glob '!*.lock'
git diff --cached --name-status
git status --short
```

Expected: no whitespace errors or conflict markers; all three former conflicts are staged; `.github/workflows/fork-android-release.yml` and the widget implementation/tests still exist.

- [ ] **Step 6: Finish the history-preserving merge**

Run:

```powershell
git commit -m "Merge upstream FeedFlow 1.17.0"
```

Expected: a two-parent merge commit whose second parent is the fetched `upstream/main`.

### Task 3: Verify the Synchronized Widget Fork

**Files:**
- Verify: `androidApp/src/test/kotlin/com/prof18/feedflow/android/widget/**`
- Verify: `shared/src/androidHostTest/kotlin/com/prof18/feedflow/shared/data/WidgetSettingsRepositoryTest.kt`
- Verify: `shared/src/androidHostTest/kotlin/com/prof18/feedflow/shared/domain/model/WidgetFreshnessTest.kt`
- Verify: `shared/src/commonTest/kotlin/com/prof18/feedflow/shared/domain/feed/FeedWidgetRepositoryTest.kt`
- Generated: i18n output produced by `.scripts/refresh-translations.sh`

- [ ] **Step 1: Provision only the dummy build configuration needed locally**

Run:

```powershell
Copy-Item -LiteralPath config/dummy-google-services.json -Destination androidApp/src/debug/google-services.json
New-Item -ItemType Directory -Force -Path androidApp/src/release | Out-Null
Copy-Item -LiteralPath config/dummy-google-services.json -Destination androidApp/src/release/google-services.json
```

Expected: local dummy Firebase configuration exists and remains ignored by Git.

- [ ] **Step 2: Refresh generated translations**

Run from Git Bash or WSL:

```bash
bash .scripts/refresh-translations.sh
```

Expected: successful completion. Any generated diff is inspected and committed only when it follows from the merged string inventory.

- [ ] **Step 3: Run the focused widget and repository tests**

Run:

```powershell
.\gradlew.bat --quiet --console=plain :androidApp:testFDroidDebugUnitTest :shared:allTests
```

Expected: all Android widget, settings repository, freshness, feed repository, and shared tests pass.

- [ ] **Step 4: Run the complete release gate**

Run:

```powershell
.\gradlew.bat --quiet --console=plain detekt allTests :androidApp:assembleFDroidRelease
```

Expected: `BUILD SUCCESSFUL` with no Detekt, test, compile, or release assembly failure. No APK is installed or launched.

- [ ] **Step 5: Commit legitimate generated corrections, if any**

Run:

```powershell
git status --short
git diff --check
```

If translation regeneration changed tracked output, inspect it, then run:

```powershell
git add -- i18n
git commit -m "Refresh translations after upstream merge"
```

Expected: the branch is clean except for ignored build output and dummy configuration.

### Task 4: Publish the Validated Merge and Fork Release

**Files:**
- Verify: `.github/workflows/fork-android-release.yml`
- Verify: `version.properties`

- [ ] **Step 1: Recheck remote state before publication**

Run:

```powershell
git fetch --prune origin
git fetch --prune upstream
git merge-base --is-ancestor upstream/main HEAD
git rev-list --left-right --count origin/master...HEAD
git tag --list "1.17.0-widget-card.1"
```

Expected: `HEAD` contains current `upstream/main`, `origin/master` has not advanced unexpectedly, and the release tag is still unused.

- [ ] **Step 2: Advance and push `master` without rewriting history**

Run:

```powershell
git switch master
git merge --ff-only codex/sync-upstream-2026-08-30
git push origin master
```

Expected: `origin/master` advances to the validated merge; no force option is used.

- [ ] **Step 3: Create and push the aligned release tag**

Run:

```powershell
git tag -a 1.17.0-widget-card.1 -m "FeedFlow fork build 1.17.0-widget-card.1"
git push origin 1.17.0-widget-card.1
```

Expected: GitHub starts `.github/workflows/fork-android-release.yml` for the immutable tag.

- [ ] **Step 4: Wait for and inspect the release workflow**

Run:

```powershell
$runId = gh run list --repo Darkaxt/feed-flow --workflow fork-android-release.yml --limit 5 --json databaseId,headBranch,status | ConvertFrom-Json | Where-Object { $_.headBranch -eq '1.17.0-widget-card.1' } | Select-Object -First 1 -ExpandProperty databaseId
if (-not $runId) { throw 'No release workflow run found for 1.17.0-widget-card.1' }
gh run watch $runId --repo Darkaxt/feed-flow --exit-status
gh release view 1.17.0-widget-card.1 --repo Darkaxt/feed-flow --json tagName,isDraft,isPrerelease,assets,url
```

Expected: the workflow succeeds; the release is neither draft nor prerelease and contains exactly one APK plus its `.sha256` file.

- [ ] **Step 5: Verify the freshly published artifact contract**

Download to a dedicated temporary directory under `D:\Temp`, compare the published checksum, and inspect the APK signing certificate:

```powershell
$artifactDir = 'D:\Temp\feed-flow-1.17.0-widget-card.1-release-check'
New-Item -ItemType Directory -Force -Path $artifactDir | Out-Null
gh release download 1.17.0-widget-card.1 --repo Darkaxt/feed-flow --dir $artifactDir
$newApk = Join-Path $artifactDir 'FeedFlow-1.17.0-widget-card.1.apk'
$newChecksum = Join-Path $artifactDir 'FeedFlow-1.17.0-widget-card.1.apk.sha256'
Get-FileHash -Algorithm SHA256 -LiteralPath $newApk
Get-Content -LiteralPath $newChecksum

$priorDir = Join-Path $artifactDir 'prior'
New-Item -ItemType Directory -Force -Path $priorDir | Out-Null
gh release download 1.15.5-widget-card.1 --repo Darkaxt/feed-flow --pattern '*.apk' --dir $priorDir
$priorApk = Get-ChildItem -LiteralPath $priorDir -Filter '*.apk' | Select-Object -Single -ExpandProperty FullName

$sdkRoot = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { $env:ANDROID_HOME }
if (-not $sdkRoot) { throw 'ANDROID_SDK_ROOT and ANDROID_HOME are unset' }
$apksigner = Get-ChildItem -LiteralPath (Join-Path $sdkRoot 'build-tools') -Recurse -Filter 'apksigner.bat' |
  Sort-Object { [version]$_.Directory.Name } |
  Select-Object -Last 1 -ExpandProperty FullName
if (-not $apksigner) { throw 'apksigner.bat was not found' }
& $apksigner verify --verbose --print-certs $priorApk | Tee-Object -Variable priorSigner
& $apksigner verify --verbose --print-certs $newApk | Tee-Object -Variable newSigner
$priorDigest = $priorSigner | Select-String 'Signer #1 certificate SHA-256 digest:' | ForEach-Object { $_.Line.Split(':', 2)[1].Trim() }
$newDigest = $newSigner | Select-String 'Signer #1 certificate SHA-256 digest:' | ForEach-Object { $_.Line.Split(':', 2)[1].Trim() }
if ($newDigest -ne $priorDigest) { throw 'Published APK signer does not match the prior stable fork release' }
if (($newSigner -join "`n") -match 'CN=Android Debug') { throw 'Published APK uses the Android debug certificate' }
```

Expected: the computed SHA-256 equals the checksum file, the signer is not the Android debug certificate, and its certificate SHA-256 digest matches the prior stable fork release.

### Task 5: Create the Weekly Manual Synchronization Heartbeat

**Files:**
- External state: current Codex task heartbeat automation.

- [ ] **Step 1: Inspect existing automations for a matching FeedFlow sync**

Inspect `$CODEX_HOME/automations/*/automation.toml` and record any automation whose name or prompt targets weekly FeedFlow upstream synchronization.

Expected: update an existing match rather than creating a duplicate.

- [ ] **Step 2: Create or update the weekly heartbeat**

Configure an active heartbeat named `Sync FeedFlow upstream weekly` for Monday at 09:00 in `Europe/Madrid`, attached to this task. Its prompt must instruct Codex to:

```text
Manually synchronize Darkaxt/feed-flow master with prof18/feed-flow main using the approved design in docs/superpowers/specs/2026-08-30-weekly-upstream-sync-design.md. Fetch both remotes, record exact ahead/behind commits, and do nothing when no upstream commits are pending. When upstream is ahead, work in a clean D:\Temp checkout and a dated branch, merge without rebasing or force-pushing, manually preserve the configurable Android widget behavior and fork signing workflow, run translation refresh plus detekt, allTests, and :androidApp:assembleFDroidRelease, then push master and publish the next aligned *-widget-card.* release only after every gate passes. Never use ADB or install/launch the APK. On conflicts or failures, leave master, tags, and releases unchanged and report the exact evidence in this task. Clean successful or no-op temporary work.
```

Expected: one active weekly heartbeat attached to the current task.

### Task 6: Final Audit and Cleanup

**Files:**
- Remove after success: `D:\Temp\feed-flow-autosync-design`
- Remove after verification: `D:\Temp\feed-flow-1.17.0-widget-card.1-release-check`

- [ ] **Step 1: Verify the published repository state**

Run:

```powershell
git fetch --prune origin upstream
git rev-list --left-right --count origin/master...upstream/main
git merge-base --is-ancestor upstream/main origin/master
git status --short
```

Expected: upstream ahead count is zero, the ancestry check succeeds, and the local checkout is clean.

- [ ] **Step 2: Record final evidence**

Record the merge commit, upstream commit, release workflow run URL, release URL, APK SHA-256, signer certificate digest, and heartbeat name/schedule.

- [ ] **Step 3: Remove completed temporary directories**

Resolve and verify that both targets are exactly under `D:\Temp`, then remove only:

```text
D:\Temp\feed-flow-autosync-design
D:\Temp\feed-flow-1.17.0-widget-card.1-release-check
```

Expected: completed temporary work is gone without touching any other checkout or user data.
