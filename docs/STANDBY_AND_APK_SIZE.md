# Standby and APK size optimization (2026-10-10/11)

## Provenance and scope

Starting cumulative baseline: `2026-10-10-dual-portal-thor-accepted`,
`C:/Users/Administrator/Documents/Heimdall/artifacts/alpha-next-0.2.0`,
`codex/alpha-next@afadbe2ffe7a4f9ea41ff139f0c2b2cc80bdeb96`.
Candidate: `C:/Users/Administrator/Documents/Heimdall/artifacts/standby-apk-size`,
`codex/fix-standby-apk-size`, based on that exact commit. After owner acceptance,
the scoped changes were committed and local `codex/alpha-next` was aligned to
that commit. The starting baseline was clean. All accepted batches, including
dual Portal, and pending RetroArch / PPSSPP Gold remain present. The registered
baseline is now `2026-10-11-standby-apk-size-thor-accepted` at the same path.

Owner report: lid closed around 22:00, opened around 07:00, approximately ten
percentage points battery loss. No Portal, recording, Translation, dynamic Canvas,
macro loop, USB or charger; stock lid behavior. The initial code/build investigation
preceded device connection. There is no device evidence assigning that first
night's loss to Heimdall. The later owner-operated result is recorded below.

## Read-only Thor evidence collected after the initial build

On 2026-10-10, the first ADB session only read existing battery history, logcat
and device-idle status. Local raw captures are ignored under
`build/device-readonly-2026-10-10/` because they may contain private device data.
No device settings or logs were changed.

- Battery history spans 2026-10-09 16:35 to 2026-10-10 11:00 local time. At
  22:00 it recorded 91%, and at 07:00 it recorded 84%: seven percentage points
  in that exact nine-hour window. By about 11:00 it recorded 81%. The owner's
  approximate ten-point observation and the exact historical interval therefore
  use different endpoints; neither should be substituted for the other.
- The screen was on for only about five seconds during 22:00-07:00, while the
  CPU `running` state totalled 2h 58m and the aggregate partial `wake_lock`
  state totalled 2h 16m. These are system-wide history states, not Heimdall CPU
  or wake-lock totals.
- At 22:24:18 Heimdall Debug briefly resumed during a screen wake, then its
  Activity was paused/stopped for sleep by 22:24:23. Battery history has no
  Heimdall-attributed overnight long wake lock or background job. Across the
  broader 18h23m battery-stat window, Heimdall Debug used about 1m24s of CPU;
  its estimated power is dominated by approximately five minutes of screen-on
  attribution, not proof of overnight drain. The public Heimdall package was
  also installed, but its aggregate CPU time was under nine seconds.
- The larger named partial wake locks in the same statistics and overnight
  history came from YouTube background jobs and Play Store scheduler jobs.
  There were also frequent timer-related suspend aborts; history alone cannot
  assign all CPU-running time or battery discharge to one package.
- The device currently reports `mLightEnabled=false`, `mDeepEnabled=false`
  and `cmd deviceidle enabled` returns `0`. Battery statistics record zero light
  or deep Doze time for the entire 18h23m window. This supports a system-wide
  standby concern, but current settings cannot by themselves establish why
  device idle was disabled last night or whether that is stock Thor behavior.

The read-only evidence does not validate the candidate APK, which was built
after the reported night. The owner subsequently authorized one `adb install -r`
of the exact Debug APK on Thor. Installation succeeded, and the installed
`base.apk` SHA-256 matched the local artifact:
`5307E080F3C89DA6F940EF4FD351A7A8D8CC1C97CE9A874C231A3AAB56B903BD`.
The installed package reports `com.mastercook777.heimdall.debug`, version
`0.2.0-alpha.1-debug` (code 5). No candidate-specific crash was found in the
retained crash buffer immediately after installation; this is not a runtime
regression test. No UI launch, key injection, device-idle change or log clearing
was performed. An owner-operated overnight result for the exact installed
Debug artifact is recorded below.

On 2026-10-11, the owner reported that the lower screen remained open on the
same Profile for the following night and battery fell by only three percentage
points. The owner considers this a reasonable standby result. ADB was not
available at report time, so exact timestamps, system wake sources and
candidate-specific CPU activity could not be reconstructed for that night.
This is positive owner-operated standby evidence for the installed Debug APK,
not a controlled attribution of the improvement to Heimdall alone. Other
functional regression checks and the signed Release-only shrinker gate remained
open at that point. The owner subsequently confirmed that the requested Debug
regression checklist was completed without issues. This accepts the exact
installed Debug APK for the scoped standby/size batch; signed Release behavior
still requires its own exact-artifact Thor test.

## Power findings and changes

- Normal Debug initialization previously started a five-second process/thread CPU
  reporter that stopped only at Activity destruction. It now starts only while the
  Activity is started and its lower display is usable, stops at screen sleep or
  Activity stop, and restarts idempotently with fresh CPU counters.
- App-aware Accessibility scans and the 1.2-second resolver ticker previously had
  no explicit device-interactive boundary. They now skip non-interactive devices;
  queued listener work is rejected while the runtime is inactive.
- Activity stop intentionally retains the independent Shizuku ROM detector for
  temporary lower-screen focus changes. That detector could therefore continue
  `/proc` checks / log watching while asleep. A dedicated manual Binder pause
  transaction now parks it on a wait/notify boundary, destroys the current logcat
  reader, and preserves same-PID ROM identity and the sleep replay watermark.
  Queries resume the monitor; cached Activity state is invalidated and the PID is
  verified before retained identity is returned. Query generation rejects stale
  pre-sleep replies. The process suffix/tag are v16 and service version is 15.
- Empty monitor targets now block rather than waking every 500 ms. A pause racing
  with PID resolution cannot reset retained identity. No-target retirement wakes
  the blocking monitor to exit normally.
- An event-driven SCREEN_ON/OFF plus DisplayManager observer protects the visible
  lower-screen runtime even when Thor has not delivered onPause/onStop yet.
  Translation, hardware sampling, Canvas, magnifier view output and WebView pause
  through existing APIs; input sessions are parked through their existing cleanup
  routes. Header status, thermal listeners and the optional composition workaround
  also stop. Wake resumes only when the Activity/display are eligible.

No periodic power observer, wake lock, device Settings/sysfs write, guessed lid
sensor, new foreground service or focus-handoff trigger is introduced. The live
Portal Surface path, recording service, detector parsers/resolver precedence,
Profile/Grid persistence and explicit Save boundaries are preserved. This does
not modify macros' product semantics or restart an IME project.

Scheduled app work can cost CPU while Android is awake; these code findings alone
cannot prove why stock firmware lost ten battery points or whether it entered
and remained in deep sleep.

## Size findings and changes

The prior Debug APK was 53,631,759 bytes. Its four ABI payloads were
44,097,736 bytes, dominated by bundled ML Kit OCR native code. Intel x86/x86_64
payloads alone occupied 24,837,176 bytes.

Default builds now package ARM64 + ARM32. All four OCR scripts remain bundled and
work without downloading models. For an Intel emulator, explicitly use:

```powershell
.\gradlew.bat :assistant:assembleDebug '-PheimdallAbis=arm64-v8a,armeabi-v7a,x86,x86_64' --no-daemon
```

Release enables R8 and resource shrinking using the standard optimized rules.
This first pass preserves all Heimdall application classes/members, including JNI
symbols, reflected Shizuku constructors and diagnostic names; dependency code and
resources are eligible for shrinking. Signing and native-library extraction/
alignment behavior are unchanged.

| Artifact | Bytes | Decimal MB | Comparison |
| --- | ---: | ---: | --- |
| Prior Debug reference | 53,631,759 | 53.63 | Reference |
| Optimized Debug | 27,832,068 | 27.83 | 48.1% smaller than prior Debug |
| Optimized unsigned Release | 24,372,830 | 24.37 | Size measurement only; not a signed candidate |

48 original assets are byte-identical in both new APKs. Both ARM ML Kit native
libraries are byte-identical to the reference. Release adds only two expected
DEX optimization profile assets; no original asset is removed or changed. ZIP
integrity passes. All four packaged native files are uncompressed, have 16 KiB
ZIP alignment, and pass 64-bit ELF 16 KiB / 32-bit ELF 4 KiB alignment checks.

## Local validation and exact artifact

- `assembleDebug`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug`,
  `lintRelease`: PASS. Debug Lint: 0 errors / 135 warnings; Release: 0 / 134.
- `tools/test_standby_runtime.py`: PASS against actual compiled Debug classes with
  minimal host Android stubs. Covers parked monitor state (no periodic idle timer),
  sleep/reset race, retained identity/timestamp/replay watermark, wake cache
  invalidation, monitor retirement, reporter stop/restart and duplicate-wake
  idempotence. This is host logic evidence, not Android Binder/Thor acceptance.
- AndroidTest assertions updated for the new service identity; APK compiled only,
  not installed or executed. No device was available at the initial build.
- Baseline verifier with explicit candidate path and `-VerifyApk`: PASS.
- Public source-boundary verifier: PASS (317 files).
- `git diff --check`: PASS.
- Final Debug packaging was regenerated from the same compiled inputs to remove
  approximately 638 KB of incremental ZIP padding; the final identity below is
  the repackaged artifact accepted on Thor.

Debug APK:
`assistant/build/outputs/apk/debug/heimdall-debug.apk`

Package: `com.mastercook777.heimdall.debug`; label: `Heimdall Debug`;
version: `0.2.0-alpha.1-debug`; bytes: `27832068`.
Debug certificate SHA-256:
`8CC147A021184A1D64F6192BCDE1DB115AC7A084A5FA9DDBF1FF7FD1A08B0456`.
APK SHA-256:
`5307E080F3C89DA6F940EF4FD351A7A8D8CC1C97CE9A874C231A3AAB56B903BD`.

After local alignment, `codex/alpha-next` was rebuilt from the same committed
source. Its verified Debug APK is also 27,832,068 bytes, with SHA-256
`6ECC8F828C46A5D2761B36A70B8150C26C19F91674053181FF997DCDEBB4BC83`.
The registry references this rebuilt artifact; Thor acceptance applies to the
installed candidate hash above, not automatically to this new packaging hash.

Unsigned Release measurement SHA-256:
`2A781AC019393E7C0EEF454C7160D501BECC511534EC1B3717D3220CFB95CCF4`.
It has no Alpha signing/installation acceptance and must not be published.

## Owner-operated Thor acceptance before release preparation

1. The exact Debug artifact is installed. The owner reports completion of the
   requested functional regression checklist without issues, including the
   previously requested Profile/Grid, input and module checks.
2. The owner reports a three-percentage-point overnight drop with the same
   Profile and lower screen left open. Exact timestamps and wake-source data
   are unavailable for this run, so causal attribution remains open.
3. The owner reports completion of the previously requested lid sleep/wake,
   Translation/dynamic Canvas/Portal/recording, App-aware and OCR regressions
   without issues. The report is an aggregate acceptance; individual run logs
   were not captured.
4. During later release preparation, separately install and test the exact signed,
   optimized Release candidate: Debug runtime acceptance cannot validate
   Release-only R8 shrinking/signing/update behavior.

The scoped changes were committed and the local cumulative branch was aligned
after owner acceptance. No push, version change, Alpha signing,
tag or publication occurred. Release preparation is a separate operation.

Reproduce the host regression after compiling Debug:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
python tools/test_standby_runtime.py --android-jar C:/Users/Administrator/AppData/Local/Android/Sdk/platforms/android-35/android.jar
```

Technical references: [Android app optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization),
[APK size](https://developer.android.com/topic/performance/reduce-apk-size),
[PowerManager interactive-state contract](https://developer.android.com/reference/android/os/PowerManager#isInteractive()).
