# Heimdall Maintainer And Alpha Release Workflow

This document defines the repeatable maintainer workflow after the first public
Alpha. The public repository `main` branch is the source of truth for every
future public build. Private archives may retain internal history, diagnostics,
and signing operations, but must not become a second long-lived copy of the
publishable source.

No script, workflow, or review result grants permission to publish. Creating or
pushing a version tag and approving the protected publish environment always
requires an explicit owner decision.

The release invariant is exact-artifact promotion: the APK accepted on AYN
Thor and the APK attached to the GitHub Release must have the same SHA-256.
The publish workflow must never rebuild, re-sign, zipalign, or otherwise mutate
the APK after owner acceptance.

## Build Channels

| Channel | Identity | Distribution | Purpose |
| --- | --- | --- | --- |
| Pull request CI | `com.mastercook777.heimdall.debug`, ordinary CI signing | Workflow result only | Compile and Lint gate for proposed changes. |
| Tester Debug | `com.mastercook777.heimdall.debug`, stable tester-only key | 14-day Actions artifact | Repeatable owner testing from a commit already contained in `main`. |
| CI Alpha candidate | `com.mastercook777.heimdall`, protected Alpha key | 30-day immutable Actions artifact | Exact package/update/signing and AYN Thor acceptance. |
| Public Alpha | `com.mastercook777.heimdall`, protected Alpha key | Immutable versioned GitHub prerelease | Player download and update channel. |

Tester Debug and public Alpha use different package names and different signing
keys. The Alpha key must never be used for Debug builds.

## Native packaging and release optimization

The default APK targets ARM64 and ARM32. Intel emulator builds must opt in with
`-PheimdallAbis=arm64-v8a,armeabi-v7a,x86,x86_64`; do not publish such a diagnostic
build as the ordinary Thor APK. Four offline OCR scripts remain bundled.
Release code/resource shrinking is enabled with Heimdall-owned classes and JNI /
Shizuku entrypoints preserved. Acceptance must cover the exact signed optimized
Release candidate; an unminified Debug smoke does not validate R8 behavior.
See `STANDBY_AND_APK_SIZE.md` for the 2026-10-10 size and standby audit.

## Version And Identity Sources

- `version.properties` is the only editable source for `VERSION_CODE` and
  `VERSION_NAME`.
- `VERSION_CODE` must increase for every public APK.
- `VERSION_NAME` must match `X.Y.Z-alpha.N`.
- `release.properties` records the stable public application ID and the public
  SHA-256 fingerprint of the approved Alpha certificate.
- `assistant/build.gradle` consumes `version.properties`; do not duplicate the
  current version inside scripts or workflows.
- Release notes live at `docs/releases/vX.Y.Z-alpha.N.md`.

Changing the public application ID or release certificate is a separate
migration decision, not an ordinary release edit.

## Repository And Environment Setup

Protect `main` with required Android CI, prohibit force pushes, and protect
release tags matching `v*`. A solo maintainer may merge after CI without
requiring a second approval, but the release environment remains owner-gated.

Keep these protected environments:

### `alpha-release`

- `HEIMDALL_RELEASE_KEYSTORE_BASE64`
- `HEIMDALL_RELEASE_STORE_PASSWORD`
- `HEIMDALL_RELEASE_KEY_ALIAS`
- `HEIMDALL_RELEASE_KEY_PASSWORD`
- `HEIMDALL_RELEASE_CERT_SHA256`

The protected fingerprint must equal `RELEASE_CERT_SHA256` in
`release.properties`.

`Build Heimdall Alpha Candidate` and `Promote Heimdall Alpha Candidate` both
use this environment. They are separate runs with separate approvals. Approving
candidate signing does not authorize publication; approving the promotion run
after Thor acceptance is the publication gate. The promotion run reads no
private signing key and never invokes Gradle.

### `tester-debug`

- `HEIMDALL_TEST_KEYSTORE_BASE64`
- `HEIMDALL_TEST_STORE_PASSWORD`
- `HEIMDALL_TEST_KEY_ALIAS`
- `HEIMDALL_TEST_KEY_PASSWORD`
- `HEIMDALL_TEST_CERT_SHA256`

Use a dedicated test-only signing key. Losing it may require reinstalling the
`.debug` App, but must not affect public Alpha update trust.

Keep the Alpha keystore and passwords in at least two secure, offline,
recoverable locations. Never commit or upload any signing key as an artifact.

## Normal Development

1. Start from an up-to-date clean `main` checkout.
2. Create one short-lived branch for one bounded change.
3. Keep runtime compatibility and AYN Thor verification boundaries explicit in
   the pull request.
4. Run the lowest honest local verification for the risk, then open a pull
   request.
5. Require Android CI before merge.
6. Record hardware-dependent paths as needing Thor validation until the owner
   completes them.

Do not maintain a long-lived `develop` branch. Keep `main` buildable and use
short release-preparation pull requests.

## Tester Debug

Use `Build Heimdall Tester Debug` only for a commit already contained in
`main`. The workflow deliberately rejects arbitrary feature-branch commits so a
modified build script cannot receive the stable tester signing secret.

The downloaded Actions artifact contains:

- `heimdall-debug-<commit>.apk`
- `SHA256SUMS.txt`
- `BUILD_METADATA.txt`

It is a temporary test artifact, not a GitHub Release and not a public Alpha.
For pre-merge Thor work, build locally with the tester key or merge only after
the change's stated non-device gates have passed.

## Prepare An Alpha Release Pull Request

1. Select a bounded release scope. Do not use download count or elapsed time as
   the only reason to publish.
2. Update `VERSION_CODE` and `VERSION_NAME` in `version.properties`.
3. Create and finalize `docs/releases/vX.Y.Z-alpha.N.md`. Remove every `Draft`
   and `TBD` marker before candidate creation.
4. Run:

   ```powershell
   ./scripts/verify_release_readiness.ps1
   ./scripts/verify_source_boundary.ps1
   python tools/audit_localization.py --base main
   ./gradlew.bat :assistant:assembleDebug :assistant:lintDebug --no-daemon
   ```

5. Review the changed English and Simplified-Chinese copy using
   `docs/ENGLISH_COPY_STYLE_GUIDE.md`. The script checks structure; candidate
   text fit and meaning still need an exact-APK pass on Thor.
6. Review the exact pull-request diff and merge only after CI passes.
7. Confirm the release commit is clean and already contained in `main`.

## Build The Immutable CI Candidate

1. From Actions, run `Build Heimdall Alpha Candidate` with:

   - `release_tag`: the planned `vX.Y.Z-alpha.N` tag;
   - `source_commit`: the full 40-character commit already contained in public
     `main`.

2. Approve the protected signing job. This approves candidate creation only;
   it does not authorize a tag or Release.
3. The workflow builds and signs once, runs Release Lint, and verifies:

   - package, version code, and version name;
   - exactly one approved signer and APK Signature Scheme v2;
   - ZIP integrity, `extractNativeLibs=false`, and `zipalign -P 16`;
   - every `lib/<abi>/*.so` is uncompressed and correctly aligned in the ZIP;
   - 64-bit ELF load segments are 16K-compatible and 32-bit ELF load segments
     retain valid 4K alignment.

4. Download the exact Actions artifact named
   `heimdall-alpha-candidate-<version>-<run-id>-<attempt>`. Record its workflow
   run ID, artifact name, APK byte size, and SHA-256 from
   `candidate-metadata.properties`.
5. Install that exact APK on AYN Thor and complete the release-specific
   acceptance matrix, including update/migration paths when the change affects
   persistence, permissions, signing, or install behavior.

Do not create a version tag before the exact candidate passes. Actions
artifacts expire after 30 days. If the artifact expires, build a new candidate,
treat its SHA-256 as new evidence, and repeat Thor acceptance.

`scripts/build_alpha_candidate.ps1` remains available for local diagnostics or
signing recovery, with keystore and output outside the repository. A local APK
is not eligible for the automated promotion workflow because it is not bound to
an immutable Actions run and artifact. Do not substitute it after acceptance.

## Publish The Immutable Alpha

After the exact candidate is accepted and the owner explicitly authorizes
publication:

1. Reconfirm the accepted candidate's source commit, workflow run ID, artifact
   name, byte size, and APK SHA-256.
2. Create the annotated `vX.Y.Z-alpha.N` tag at that exact source commit.
3. Push only that tag. Tag push no longer starts publication automatically.
4. From Actions, run `Promote Heimdall Alpha Candidate` with the existing tag,
   candidate run ID, exact candidate artifact name, and owner-accepted APK
   SHA-256.
5. Review and approve this separate protected `alpha-release` promotion.

After approval, the job downloads that exact immutable Actions artifact. It
does not restore the keystore, invoke Gradle, rebuild, re-sign, or rewrite the
APK. Candidate metadata must bind the tag to the same source commit, package,
version, signer, workflow run, artifact name, byte size, and approved SHA-256.

The workflow refuses publication when:

- the tag format is invalid;
- the checked-out commit differs from the tag's peeled commit;
- `version.properties` does not match the tag;
- release notes are missing or contain `Draft` / `TBD`;
- the candidate source commit differs from the tag commit;
- the candidate run ID or artifact name differs from its immutable metadata;
- the supplied owner-approved SHA-256 differs from the candidate APK;
- package, version code, or version name differs from repository metadata;
- the APK signer differs from the approved Alpha certificate; or
- ZIP integrity, native compression, 4K/16K alignment, or manifest packaging
  checks fail; or
- a GitHub Release with the same tag already exists.

The workflow first uploads only the verified APK and checksum to a draft
prerelease, downloads them again, and re-runs all artifact checks. It then makes
the prerelease public, downloads the public assets again, verifies that the APK
SHA-256 still equals the owner-approved candidate, and confirms that the asset
set contains only the APK and checksum.

On success it publishes only:

- `heimdall-vX.Y.Z-alpha.N.apk`
- `SHA256SUMS.txt`

Tags and published assets are immutable. Corrections use a new Alpha number and
a higher version code.

## Recovery And Post-Publication Verification

Promotion is intentionally `workflow_dispatch` only. Recovery for an existing
tag must reuse the same still-retained accepted candidate run, artifact name,
and approved SHA-256. It verifies that checkout `HEAD`, the tag's peeled commit,
and the candidate source commit are identical. Never move, delete, or recreate
a published version tag to repair a workflow failure. Never rebuild after an
approval and call the replacement artifact accepted.

If verification fails after the draft prerelease is created, leave it
unpublished while investigating. A retry deliberately refuses to overwrite
even a draft. After confirming that no public Release exists, the maintainer may
remove only that failed unpublished draft and retry with the same accepted
candidate identity. A public prerelease is never deleted or overwritten by the
workflow.

After publication, independently download the public assets and verify:

- the Release is a prerelease and immutable;
- the asset set contains only the expected APK and checksum;
- the checksum matches the downloaded APK;
- package, version code, version name, signer count, and certificate match;
- zip alignment and available GitHub attestations pass; and
- the published source commit is the approved release commit.

Then transfer that freshly downloaded public APK to Thor and complete a final
install/update plus launch smoke. Record the public asset SHA-256 with that
result. The workflow's post-publish checksum gate proves byte identity, but it
does not claim device installation on the owner's behalf.

Record build/static evidence separately from owner-operated Thor acceptance.
