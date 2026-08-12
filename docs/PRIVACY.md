# Privacy And Capability Boundary

English | [简体中文](PRIVACY.zh-CN.md)

Heimdall does not provide a cloud account or analytics service.

## Local Data

Profiles, macros, layout data, maps, guides, Canvas images, settings, imported
icons, and automatic Profile recovery snapshots are stored locally by the App.
They leave the App only when the player explicitly exports or shares them.

Android platform backup is disabled for this Alpha. Players should use the
explicit Profile export flow before uninstalling, changing signing channels, or
moving to another device.

Current Profile exports are self-contained `.heimdall-profile` bundles. In
addition to Profile configuration, a bundle can contain supported Profile icons,
maps, file Guides, user-imported Macro icons, and Canvas images. The bundle is
written only to the location selected by the player. Legacy configuration-only
JSON remains importable but cannot restore source assets that older builds never
included. See the [Profile bundle format](PROFILE_BUNDLE_FORMAT.md).

## Accessibility

Basic Touch uses an Accessibility service to send compatible upper-screen touch
actions and to obtain conservative upper-App/window context for optional
Profile matching. Heimdall must not use arbitrary Accessibility event text as a
game or ROM identity.

## Shizuku

Controller Enhancement uses an authorized Shizuku UserService for native
controller recording/replay, Virtual Right Stick, Precision Aim, and the
selected Thor touch route. Capabilities are unavailable when Shizuku is not
running or not authorized. Heimdall does not bundle Shizuku.

## Screen Capture And Audio

Screenshots use Android's display-aware Accessibility API where supported.
Recording and the live magnifier require explicit Android MediaProjection
consent. Game-audio recording uses Android Audio Playback Capture and does not
select microphone input; the upper App may refuse playback capture.

## Network

Internet and network-state access support player-configured Interactive Map
pages and connection status. Interactive Map pages may run JavaScript for page
compatibility, but Heimdall exposes no JavaScript interface to them.

## Reports

Before sharing logs, screenshots, recordings, or Profile exports, remove names,
paths, URLs, tokens, account information, and game data that should remain
private. A `.heimdall-profile` bundle may contain the player's imported maps,
Guides, icons, and Canvas images; treat it as private unless its contents have
been reviewed.
