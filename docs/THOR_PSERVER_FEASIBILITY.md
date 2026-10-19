# Thor PServer feasibility probe (2026-10-14)

## Scope and baseline

This was an isolated, read-only Android instrumentation probe on an AYN Thor. It did not change Heimdall's production input routing, request Shizuku permission, send an input event, or start a persistent helper. The registered Debug baseline was `2026-10-11-standby-apk-size-thor-accepted`, `codex/alpha-next@b2d978b3ef93245233ec0026d0e93c8efaf0a395`, at `artifacts/alpha-next-0.2.0`; `verify_debug_development_baseline.ps1` reported `BASELINE_OK=true` and a clean tree before the probe. The probe used a short-lived `codex/feature-thor-pserver-probe` branch.

The connected device identified as `AYN Thor`, Android 13 / API 33, firmware fingerprint `qti/kalama/kalama:13/TKQ1.231222.001/eng.Thor.20260206.163241:user/release-keys`. ADB listed and found `PServerBinder`, but `service call PServerBinder 0` from the shell returned `Service PServerBinder does not exist`. Service listing alone was therefore not treated as application access evidence.

## App-process result

An instrumentation process targeting installed `com.mastercook777.heimdall.debug` ran under app UID `10253`. Reflection resolved `android.os.ServiceManager.getService("PServerBinder")`; the Binder was alive and a synchronous, read-only `id` transaction was delivered. Its reply was:

```text
uid=0(root) gid=0(root) groups=0(root) context=u:r:pservice:s0
```

The same process then reported `INPUT_OPEN_1` after opening at least one `/dev/input/event*` node for a zero-byte read, and `RSTOUCH_WRITE_ALLOWED` from a permission check on `/dev/rstouch`. These checks did not consume controller events or emit touch input. The exact controller node was not identified, and `/dev/rstouch` was not opened for output.

The final probe-only test APK was `109,338` bytes, SHA-256 `C9E6FCE429808EFEDDAAE9AB9A5E2060E7C983E9BCAB9D5CB4988DBFE049550D`, signed by the stable Debug certificate `8CC147A021184A1D64F6192BCDE1DB115AC7A084A5FA9DDBF1FF7FD1A08B0456`. `:assistant:assembleDebugAndroidTest` succeeded and `am instrument` returned `INSTRUMENTATION_CODE: -1` with those observations.

## Decision and next experiment

This proves that a regular Heimdall app process on this Thor firmware can reach the stock privileged bridge, and that the bridge has preliminary access to native input nodes. It does not prove a safe, durable replacement for Shizuku. The next isolated prototype should launch one Heimdall-owned helper, verify its caller authentication, single-instance lifetime, explicit stop and reboot recovery, and check access to the resolved physical controller node and mapped-touch output with all-up cleanup. Only after that should one existing native-controller action be routed through it and compared against the Shizuku behavior on Thor.

Keep Shizuku as the working path while this is experimental. Never expose arbitrary PServer commands to UI, Profiles, imported/community content, or IPC callers. The OEM bridge is privileged root execution; command construction and helper authentication are security boundaries. If the service is absent, denied, interrupted, or behaves differently on another firmware, report the capability unavailable and retain the existing Shizuku path. The temporary probe must not remain as the normal Android test runner or installed test package.

Cleanup completed: the original `ProfileBundleStoreInstrumentationTest` runner and source tree were restored, the rebuilt normal AndroidTest APK (SHA-256 `075AC9361BC0A8A3BB1FCBFC7B13D048111BED02E24CB135EC7FDD2E371E60E4`) replaced the probe package on Thor, and `pm list instrumentation` confirmed that runner. The temporary branch was deleted and the checkout returned to `codex/alpha-next`; only this report remains untracked. The final baseline verifier reported `BASELINE_OK=true`, `APK_VERIFIED=true`, and `APK_REFERENCE_MATCH=True` for the unchanged Debug APK.
