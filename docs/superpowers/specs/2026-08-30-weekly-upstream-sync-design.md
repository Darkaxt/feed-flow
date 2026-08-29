# FeedFlow Fork Upstream Synchronization Design

**Date:** 2026-08-30  
**Fork:** `Darkaxt/feed-flow` (`master`)  
**Upstream:** `prof18/feed-flow` (`main`)

## Purpose

Keep the FeedFlow fork current with upstream while retaining the fork's Android widget behavior, release signing identity, and published history. The first run brings the currently diverged fork up to date. Later runs are performed weekly by a Codex heartbeat using the same review and validation contract.

At the time this design was written, the fork is 33 commits ahead of and 147 commits behind `upstream/main`. The fork reports version `1.15.5`; upstream reports `1.17.0`.

## Safety Model

Synchronization is a reviewed merge, not an unattended branch rewrite.

- Fetch `origin/master` and `upstream/main` before making decisions.
- Start from the latest `origin/master` in a temporary checkout under `D:\Temp`.
- Merge `upstream/main` into a dated synchronization branch.
- Preserve the published fork history with a merge commit. Never rebase or force-push `master`.
- Resolve overlapping changes by understanding both sides. Never resolve the complete merge with blanket `ours` or `theirs` selection.
- Do not update `master`, create a tag, or publish a release unless the merge and required verification succeed.
- Keep signing secrets in GitHub Actions. The synchronization process must not retrieve or duplicate the private keystore.
- Remove temporary synchronization work after completion, while preserving any checkout that contains unresolved work needed for diagnosis.

## Fork Behavior That Must Survive

The merge must retain or deliberately re-port these fork-specific contracts onto the current upstream architecture:

1. The widget Card layout is a fully configurable surface.
2. The card/background color and opacity settings support a fully transparent row surface.
3. Card corner radius, separation mode, divider inset, and divider opacity remain configurable and previewed accurately.
4. Dividers remain short of the widget edges and remain legible on the transparent Smart Launcher note background.
5. Article thumbnails fill the available row height with appropriate cropping while respecting bitmap and serialized-payload limits.
6. Widget sizing uses the exact launcher-provided bounds and remains stable across resize and update cycles.
7. Widget freshness filtering, image loading safety, and serialized row-payload bounds remain enforced.
8. Widget settings persist correctly and configuration previews match the rendered widget.
9. Existing widget-focused unit, UI, and end-to-end coverage remains present or is adapted to the upstream test architecture.
10. The fork Android release workflow and its fork signing identity remain intact.

Generated artifacts must be regenerated from their source when a generator exists; generated files should not be treated as the authoritative conflict resolution target.

## Current Catch-up

The initial synchronization will:

1. Create a branch named for the synchronization date from the latest `origin/master`.
2. Merge the fetched `upstream/main` commit into that branch.
3. Resolve conflicts by accepting upstream structural and API evolution, then re-porting the widget contracts above where the old fork implementation no longer applies cleanly.
4. Adopt upstream's `1.17.0` version metadata and publish the synchronized fork as `1.17.0-widget-card.1`, provided that tag is still unused when publishing.
5. Run the validation gates below.
6. Merge or fast-forward the validated synchronization branch into `master`, push it, and push the release tag.
7. Let the existing fork Android release workflow build and publish the signed APK and checksum.

If upstream or fork state changes before publication, all branch, version, and tag calculations must be repeated against freshly fetched refs. A pre-existing `1.17.0-widget-card.1` tag must result in selecting the next unused fork revision rather than replacing the tag.

## Validation Gates

Validation is source and build based; this synchronization does not install, launch, or exercise the APK on an attached device.

The synchronized tree must pass:

- Repository formatting/static-analysis checks required by the upstream project.
- The shared and Android test suites used by the existing fork release workflow.
- The existing release gate: `detekt allTests :androidApp:assembleFDroidRelease`.
- Targeted widget tests covering settings persistence, surface rendering inputs, dividers, sizing, thumbnail payloads, freshness filtering, and row serialization.
- Release artifact checks for expected filename/version, SHA-256 checksum, and signing certificate identity after GitHub Actions publishes the APK.

`master` must remain untouched if a merge conflict is unresolved or any required gate fails. A failing run reports the exact conflicting files or commands and preserves only the minimum diagnostic state needed to resume safely.

## Weekly Codex Heartbeat

A weekly heartbeat attached to the current Codex task will run every Monday morning in the `Europe/Madrid` timezone. It instructs Codex to perform the synchronization manually rather than adding an unattended GitHub merge workflow.

Each run will:

1. Locate or create a clean temporary checkout, verify both remotes, and fetch them.
2. Compare `origin/master` with `upstream/main` and record ahead/behind counts and the exact commits involved.
3. Stop without a commit or release when no upstream commit is pending.
4. When upstream is ahead, create a dated branch, perform the reviewed merge, and preserve all fork behavior listed above.
5. Run the full validation gates.
6. On success, update `origin/master` and publish a fork release only when the synchronized source version requires a new release tag.
7. On conflict or failure, leave `origin/master`, tags, and releases unchanged and report actionable evidence in this task.
8. Clean the temporary checkout after a successful or no-op run.

The heartbeat must not use force-pushes, silently discard fork commits, install to an Android device, or expose signing secrets.

## Acceptance Criteria

The current synchronization is complete when:

- `origin/master` contains the fetched upstream head through a history-preserving merge and is no longer behind that head.
- All required fork widget contracts remain implemented and covered by passing tests.
- Required repository and Android release builds pass.
- The synchronized fork version and release tag are aligned, and the GitHub release contains the correctly signed APK and checksum.
- The weekly Codex heartbeat is active.
- Successful temporary work under `D:\Temp` has been removed.

Future weekly runs meet the contract when they either complete those same gates or fail closed without changing the fork's protected state.
