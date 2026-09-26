# Privacy And Capability Boundary

English | [简体中文](PRIVACY.zh-CN.md)

Heimdall does not provide a cloud account or operate its own analytics service.

## Local Data

Profiles, macros, layout data, maps, guides, Canvas images, settings, imported
icons, Translation regions, and automatic Profile recovery snapshots are stored
locally by the App. They leave the App only when the player explicitly exports
or shares them, except for recognized Translation text sent to the provider the
player configured as described below.

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

## On-Device OCR And ML Kit Metrics

Translation OCR uses the bundled Google ML Kit Text Recognition libraries for
Latin, Chinese, Japanese, and Korean scripts. The selected image region and OCR
result are processed on-device and are not sent to Google by the ML Kit API.

Google states that the ML Kit SDK may contact Google for fixes, model or
hardware-accelerator compatibility information, and may send SDK performance
and utilization metrics. Its Android disclosure describes device information,
App package/version information, and a per-installation identifier used for
diagnostics and usage analytics. This SDK data is separate from the recognized
game text that Heimdall sends to the player's selected translation provider.
See the official [ML Kit Terms & Privacy](https://developers.google.com/ml-kit/terms)
and [Android data disclosure](https://developers.google.com/ml-kit/android-data-disclosure).

## Network

Internet and network-state access support player-configured Interactive Map
pages and connection status. Interactive Map pages may run JavaScript for page
compatibility, but Heimdall exposes no JavaScript interface to them.

The optional Translation Widget captures the player-selected upper-screen
region through Android Accessibility and performs OCR locally with bundled ML
Kit recognizers. Heimdall sends the recognized text, requested target language,
and a short translation instruction over HTTPS to the configured SiliconFlow or
custom OpenAI-compatible `/chat/completions` endpoint. It does not send the
screenshot itself. The provider's own privacy, retention, account, and billing
terms apply.

Translation provider settings are App-global. The API key is encrypted with an
Android Keystore key and is not placed in Profile JSON or exported Profile
bundles. A custom endpoint must use HTTPS. Removing App data or uninstalling
Heimdall removes the locally stored provider configuration and encrypted key.

## Reports

Before sharing logs, screenshots, recordings, or Profile exports, remove names,
paths, URLs, tokens, account information, translated dialogue, and game data
that should remain private. A `.heimdall-profile` bundle may contain the
player's imported maps, Guides, icons, and Canvas images; treat it as private
unless its contents have been reviewed.
