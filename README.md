# Heimdall

English | [简体中文](README.zh-CN.md)

Heimdall is an open-source lower-screen assistant for AYN Thor. It keeps
Profiles, macros, touchpad and right-stick controls, maps, guides, static-image
Canvases, magnification, and Quick Actions on the lower screen while the game
remains on the upper screen.

> Alpha software: Heimdall targets AYN Thor and does not claim compatibility
> with every firmware, game, emulator, or other dual-screen device.

## Download

Install only immutable, versioned prereleases from this repository's Releases
page. Every release publishes:

- `heimdall-vX.Y.Z-alpha.N.apk`
- `SHA256SUMS.txt`
- the source commit and signing-certificate SHA-256

The mutable private-development `debug-latest` channel is not a public release.

## Setup

- Basic Touch requires the Heimdall Accessibility service.
- Controller Enhancement, physical-controller recording/replay, Virtual Right
  Stick, Precision Aim, and mapping-compatible Shizuku Touch require Shizuku to
  be installed, running, and authorized.
- The live upper-screen magnifier and screen recording require Android
  MediaProjection consent.
- Game-audio recording uses Android Audio Playback Capture rather than
  microphone input. The upper App may still forbid playback capture.

See the versioned release notes for migration steps and known limitations.
Release maintainers should also read [docs/RELEASING.md](docs/RELEASING.md).

## Build From Source

Requirements:

- JDK 17
- Android SDK 35
- CMake 3.22.1
- Android NDK `30.0.14904198`

Build the debug APK:

```text
./gradlew :assistant:assembleDebug --no-daemon
```

Run the same static gate used for Alpha:

```text
./gradlew :assistant:lintDebug --no-daemon
```

The public repository contains no signing key. Local debug builds use the
Android development signing identity. Public Alpha builds are created only by
the tag-gated release workflow with an external private signing key.

## Data And Privacy

Profile data stays on the device unless the player explicitly imports or
exports it. Android platform backup is disabled for the Alpha. Heimdall requests
Internet access for user-configured Interactive Map pages; it exposes no
JavaScript bridge to those pages.

See [docs/PRIVACY.md](docs/PRIVACY.md) for the capability and data boundary.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a change. Native
input, cross-display routing, MediaProjection, performance, and physical
ergonomics require clearly scoped AYN Thor evidence.

## License

Heimdall source is licensed under the Apache License 2.0. See [LICENSE](LICENSE)
and [NOTICE](NOTICE). Third-party components retain their own licenses as listed
in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

AYN and Thor are trademarks of their respective owner. Heimdall is a community
project and is not presented as an official AYN application.
