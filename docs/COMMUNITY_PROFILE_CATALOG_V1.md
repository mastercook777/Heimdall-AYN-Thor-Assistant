# Community Profile Catalog V1

Community Profiles reuse the existing self-contained `.heimdall-profile` bundle and
the existing `ProfileBundleStore` validation, asset migration, and installation path.
The catalog is discovery metadata only. It does not introduce a Template or a second
Profile format.

Official V1 content repository:
`https://github.com/mastercook777/heimdall-community-profiles`

Official catalog endpoint:
`https://raw.githubusercontent.com/mastercook777/heimdall-community-profiles/main/catalog.json`

## Transport and trust

- The app fetches `catalog.json`, previews, and bundles over HTTPS only.
- Redirects are followed manually, remain HTTPS, and are capped at five hops.
- A bundle must match both the exact byte size and SHA-256 recorded in the catalog
  before it reaches the existing importer.
- Mirrors are tried in catalog order and all mirrors must serve the same bytes.
- The official curated catalog repository is the V1 publishing trust boundary.
  Checksums protect integrity after catalog retrieval; they are not an independent
  signature.
- The last successfully parsed catalog is stored under `noBackupFilesDir` and may be
  shown when refresh fails. Downloads and previews use `cacheDir`.

## Schema

```json
{
  "version": 1,
  "profiles": [
    {
      "id": "gba-universal",
      "name": "GBA Universal",
      "author": "mastercook777",
      "category": "Emulator",
      "subtitle": "Game Boy Advance",
      "description": "Universal GBA lower-screen layout",
      "icon": "https://example.invalid/gba-profile-icon.png",
      "iconSha256": "64 lowercase hexadecimal characters",
      "iconSize": 12345,
      "preview": "https://example.invalid/gba-universal.webp",
      "previewSha256": "64 lowercase hexadecimal characters",
      "previewSize": 123456,
      "screenshot": "https://example.invalid/gba-universal-home.png",
      "screenshotSha256": "64 lowercase hexadecimal characters",
      "screenshotSize": 234567,
      "minVersionCode": 30,
      "minHeimdallVersion": "0.3.0",
      "sha256": "64 lowercase hexadecimal characters",
      "size": 456789,
      "downloads": [
        {
          "source": "github",
          "url": "https://example.invalid/gba-universal.heimdall-profile"
        }
      ]
    }
  ]
}
```

`minVersionCode`, `sha256`, `size`, and at least one download are mandatory.
`minHeimdallVersion` is display text; compatibility decisions use the monotonic
integer `minVersionCode`. `icon`, `preview`, and `screenshot` are optional. Each
image's checksum and size are optional as a pair. `icon` is the Profile's own
`iconUri` asset extracted unchanged from the distributable bundle. The navigation
uses only `icon`; when it is absent or unavailable, Heimdall shows a local initial
fallback and never substitutes `preview` or another image from the Profile.
`subtitle` is concise navigation context and falls back to `category` for an older
catalog. `screenshot` is the separately curated complete Profile home-page image.
`preview` remains accepted as legacy discovery metadata but is not used as a list
icon or screenshot fallback. Unknown fields are ignored so additive catalog changes
remain possible within V1.

## Import behavior

The gallery accepts exactly one non-legacy Profile bundle per entry. After download
verification it calls `ProfileBundleStore.prepareImportAsync`, then the normal
`ProfileBundleStore.installAsync` path. The only offered action is append as a new
Profile; Community Profiles cannot replace the user's existing Profile list.

## Product presentation

The Thor presentation uses an opaque Settings Content surface with a compact flat
Profile navigation list on the left and a dominant detail pane on the right. Each
48-dp-or-larger list row contains the Profile's small `icon`, its name, and one concise
`subtitle` such as the platform. The navigation column is deliberately narrow and its
type is compact so it cannot compete with or squeeze the detail pane. Unselected rows
do not receive nested panel material; selection uses a leading accent indicator plus
restrained selected-state fill. The complete name and subtitle are exposed together
as the row's accessibility description.

Selecting a row changes only the detail pane. The full name and the primary
`Download & Import` operation remain visible at the top; file size and compatible
Heimdall version follow them as quiet metadata. Integrity verification remains an
internal transport guarantee rather than user-facing promotional copy. A determinate
progress bar appears only while bytes are downloading, uses the catalog size as its
total, resets if the repository falls back to another mirror, and disappears before
the existing validation/import stage. The description then precedes the
uncropped, aspect-ratio-preserving Profile home-page `screenshot` in an independently
scrollable region. Refresh uses the established icon control and Close uses the
established compact close control; both remain secondary to the download action.
Catalog names are presentation metadata and may change while each stable `id`
remains unchanged. The name embedded in an imported bundle continues to belong to
the Profile itself.

Discovery remains inside the existing Settings information architecture rather than
adding a separate Community category. The Settings rail promotes `Profile Center`
directly below initial setup, and the first control in that page is a compact featured
content panel with a frameless layered-layout icon, clear title, player/maintainer
value statement, live catalog count, and a quiet trailing chevron. Horizontal space
belongs to the copy first: the summary normally remains on one line and may wrap to a
second line rather than truncate when the system font or translated copy needs it. The former
duplicate under Data is removed. Opening Profile Center may show the last cached count
and quietly refresh the catalog so the count is never hard-coded; failure stays local
to the count line. Gallery previews and downloads still begin only after the player
opens the Gallery, while Profile draft, Save, validation, and import behavior remain
unchanged.

The initial curated collection is published from the independent content repository.
Release assets are uploaded before their catalog entries become visible, and an
existing release asset is never overwritten. The first productized catalog contains
ten entries, including one Delta Force entry after removing an identical duplicate.

## Thor UI acceptance

On 2026-09-29 the owner accepted the compact navigation, Profile-icon-only list,
dominant detail pane, download-first hierarchy, Chinese description, and independently
scrollable home screenshot on Thor using Debug APK SHA-256
`AC03842513A2BFB133147A8800942D56AE436EBC6241258759BE82006EA49A6E`.
Those geometry, typography, icon-source, and content-order decisions are frozen unless
new device evidence reopens a specific defect. On 2026-09-30 the owner also accepted
the consumer-facing metadata copy and determinate download progress behavior on Thor
using Debug APK SHA-256
`BC2AEC73786D22C039C0625204089997ADEE325EC300E90B31B1646AA455E9B9`.
That follow-up intentionally preserves the accepted layout and freezes the visible
download behavior unless new device evidence reopens a specific defect.

On 2026-09-30 the owner finalized the promoted Profile Center entry using Debug APK
SHA-256 `02EA9D3F03D4D785A5348AB16080A6B56F93A400BE97AC67D3BB31BAF19FC9D0`.
The accepted entry uses the frameless layered-layout icon, compact 78 dp row,
untruncated summary with a two-line fallback, live catalog count, and quiet trailing
chevron. Those entry details are frozen with the previously accepted Gallery and
download/import flow.
