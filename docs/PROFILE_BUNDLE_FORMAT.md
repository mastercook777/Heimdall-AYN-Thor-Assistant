# Heimdall Profile Bundle Format

English | [简体中文](PROFILE_BUNDLE_FORMAT.zh-CN.md)

Heimdall exports Profiles as a self-contained `.heimdall-profile` ZIP bundle. The current format identifier is `heimdall-profile-bundle` and the schema version is `1`.

## Contents

- `manifest.json`: format version, Profile metadata checksum and size, and the complete asset inventory.
- `profiles.json`: normal Heimdall Profile export JSON with bundled asset references replaced by `heimdall-bundle:<sha256>` placeholders.
- `assets/<sha256>.<extension>`: content-addressed asset files.

The manifest records each asset's SHA-256 digest, byte size, media type, safe display name, and exact ZIP path. Identical content is stored once even when several Profile fields reference it. Files with the same display name remain distinct when their content differs.

## Included assets

The exporter collects supported Profile icons, maps, file Guides, user-imported Macro icons, and Canvas images. Built-in Macro icons and online links remain ordinary structured references. A missing, unreadable, unsupported, or oversized required asset fails the whole export instead of producing a partial bundle.

## Import safety

Import is fail-closed. Heimdall copies the selected file to a private staging directory and validates the bundle before presenting append or replace choices. Validation includes:

- the exact format and schema version;
- the complete ZIP entry set and safe fixed paths;
- manifest sizes, total limits, SHA-256 digests, and detected media types;
- every Profile asset reference and the absence of unreferenced payloads;
- supported image bounds and normalized Macro icon requirements.

Validated assets are installed into Heimdall-owned storage with temporary files and atomic renames. Profile references are rewritten to the current package identity, so Debug and public builds do not depend on one another's provider authorities. Replace-all still requires the existing recovery snapshot before current Profile data can be overwritten. A failed import never replaces the current good Profile set; successfully installed but unreferenced content-addressed files may remain harmlessly after a late failure.

## Compatibility and limits

Legacy JSON exports remain importable and are clearly identified as configuration-only. They cannot restore external source files that were not included by the older exporter.

Schema version 1 limits a single asset to 128 MB, total uncompressed assets to 512 MB, the bundle input to 512 MB, the asset count to 512, Profile JSON to 16 MB, and the manifest to 1 MB. Supported stored extensions are JPG, PNG, WebP, GIF, PDF, TXT, Markdown, and HTML, subject to the stricter rules of the field that references each asset.
