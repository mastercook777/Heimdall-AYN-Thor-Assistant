# Heimdall Maintainer And Alpha Release Workflow

This document defines the repeatable maintainer workflow after the first public
Alpha. The public repository `main` branch is the source of truth for every
future public build. Private archives may retain internal history, diagnostics,
and signing operations, but must not become a second long-lived copy of the
publishable source.

No script, workflow, or review result grants permission to publish. Creating or
pushing a version tag and approving the protected publish environment always
requires an explicit owner decision.

## Build Channels

| Channel | Identity | Distribution | Purpose |
| --- | --- | --- | --- |
| Pull request CI | `com.mastercook777.heimdall.debug`, ordinary CI signing | Workflow result only | Compile and Lint gate for proposed changes. |
| Tester Debug | `com.mastercook777.heimdall.debug`, stable tester-only key | 14-day Actions artifact | Repeatable owner testing from a commit already contained in `main`. |
| Local Alpha candidate | `com.mastercook777.heimdall`, protected Alpha key | Outside the repository, never public | Exact package/update/signing and AYN Thor acceptance. |
| Public Alpha | `com.mastercook777.heimdall`, protected Alpha key | Immutable versioned GitHub prerelease | Player download and update channel. |

Tester Debug and public Alpha use different package names and different signing
keys. The Alpha key must never be used for Debug builds.

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
   ./gradlew.bat :assistant:assembleDebug :assistant:lintDebug --no-daemon
   ```

5. Review the exact pull-request diff and merge only after CI passes.
6. Confirm the release commit is clean and already contained in `main`.

## Build The Local Real-Signed Candidate

Use the generic candidate builder from a clean checkout. Both the keystore and
candidate output must remain outside the repository.

```powershell
./scripts/build_alpha_candidate.ps1 `
  -KeystorePath <outside-repository-keystore> `
  -OutputDirectory <outside-repository-empty-output-directory>
```

The script derives the tag, package, version code, version name, and expected
certificate from the repository. It refuses dirty source and output overwrite,
runs Release Build/Lint, verifies package metadata, APK signature, certificate,
zip alignment, and SHA-256, and writes a candidate metadata record.

Do not create a version tag yet. Install the exact candidate on AYN Thor and
complete the release-specific acceptance matrix, including update/migration
paths when the change affects persistence, permissions, signing, or install
behavior.

## Publish The Immutable Alpha

After the exact candidate is accepted and the owner explicitly authorizes
publication:

1. Reconfirm the approved source commit and candidate metadata.
2. Create the annotated `vX.Y.Z-alpha.N` tag at that commit.
3. Push only that tag.
4. Review and approve the protected `alpha-release` environment.

The workflow refuses publication when:

- the tag format is invalid;
- the checked-out commit differs from the tag's peeled commit;
- `version.properties` does not match the tag;
- release notes are missing or contain `Draft` / `TBD`;
- package, version code, or version name differs from repository metadata;
- the secret certificate fingerprint differs from `release.properties`;
- the APK signer differs from the approved Alpha certificate; or
- a GitHub Release with the same tag already exists.

On success it publishes only:

- `heimdall-vX.Y.Z-alpha.N.apk`
- `SHA256SUMS.txt`

Tags and published assets are immutable. Corrections use a new Alpha number and
a higher version code.

## Recovery And Post-Publication Verification

`workflow_dispatch` is recovery for an existing immutable Alpha tag only. It
must verify that checkout `HEAD` equals the tag's peeled commit. Never move,
delete, or recreate a published version tag to repair a workflow failure.

After publication, independently download the public assets and verify:

- the Release is a prerelease and immutable;
- the asset set contains only the expected APK and checksum;
- the checksum matches the downloaded APK;
- package, version code, version name, signer count, and certificate match;
- zip alignment and available GitHub attestations pass; and
- the published source commit is the approved release commit.

Record build/static evidence separately from owner-operated Thor acceptance.
