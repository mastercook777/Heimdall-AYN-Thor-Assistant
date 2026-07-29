# Alpha Release Process

This process is for repository maintainers. A pushed Alpha tag can create a
public APK, so do not push one until the owner has explicitly approved the
candidate.

## One-Time Repository Setup

1. Create the fresh Heimdall-only repository from the reviewed public snapshot.
2. Keep the old development repository private.
3. Create a protected GitHub Actions environment named `alpha-release`.
4. Store these values as environment secrets:
   - `HEIMDALL_RELEASE_KEYSTORE_BASE64`
   - `HEIMDALL_RELEASE_STORE_PASSWORD`
   - `HEIMDALL_RELEASE_KEY_ALIAS`
   - `HEIMDALL_RELEASE_KEY_PASSWORD`
   - `HEIMDALL_RELEASE_CERT_SHA256`
5. Enable immutable releases in repository settings when the feature is
   available for the repository.
6. Keep the original release keystore and its passwords in at least two secure,
   offline, recoverable locations. Never commit or upload the keystore as an
   artifact.

GitHub stores Actions secrets encrypted and exposes them only to workflows that
explicitly reference them. The `alpha-release` environment should be restricted
to the owner and should require manual approval when the repository plan permits
it.

## Candidate Checklist

1. Confirm `assistant/build.gradle` has the intended `versionCode` and a
   `versionName` exactly matching the tag after removing the leading `v`.
2. Finalize `docs/releases/vX.Y.Z-alpha.N.md`. It must contain neither `Draft`
   nor `TBD`.
3. Run the public source-boundary verifier:

   ```text
   ./scripts/verify_source_boundary.ps1
   ```

4. Run:

   ```text
   ./gradlew :assistant:assembleDebug :assistant:lintDebug --no-daemon
   ```

5. Complete the stated AYN Thor acceptance scope and record remaining
   limitations in the release notes.
6. Review the exact source commit, then obtain explicit approval to create and
   push the version tag.

## Publishing

The release workflow accepts tags matching `vX.Y.Z-alpha.N`. It refuses to
publish when:

- the tag format is invalid;
- the release-notes file is missing or still contains `Draft` / `TBD`;
- the APK package or version does not match;
- release signing variables are incomplete;
- the APK signing certificate does not match
  `HEIMDALL_RELEASE_CERT_SHA256`; or
- a GitHub Release with the same tag already exists.

On success it publishes only:

- `heimdall-vX.Y.Z-alpha.N.apk`
- `SHA256SUMS.txt`

The release body records the source commit, package, version, certificate
fingerprint, and APK SHA-256. Published assets and version tags must never be
overwritten; corrections use the next Alpha number.
