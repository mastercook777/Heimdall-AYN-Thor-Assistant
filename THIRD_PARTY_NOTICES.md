# Third-Party Notices

## Shizuku API And Provider

Heimdall uses:

- `dev.rikka.shizuku:api:13.1.5`
- `dev.rikka.shizuku:provider:13.1.5`

Project source: <https://github.com/RikkaApps/Shizuku-API>

License: MIT License. The upstream copyright and license notices distributed
with Shizuku remain applicable to those components.

No Shizuku private key, privileged system component, or Shizuku application
binary is included in this repository or in the Heimdall APK.

## Google ML Kit Text Recognition

Heimdall uses the bundled Android Text Recognition libraries and models:

- `com.google.mlkit:text-recognition:16.0.1`
- `com.google.mlkit:text-recognition-chinese:16.0.1`
- `com.google.mlkit:text-recognition-japanese:16.0.1`
- `com.google.mlkit:text-recognition-korean:16.0.1`

These components are provided under the Google APIs and ML Kit terms rather
than Heimdall's Apache-2.0 license. Google states that ML Kit OCR input is
processed on-device, while the SDK may send diagnostics and utilization metrics
described in its Android data disclosure.

- ML Kit Terms & Privacy: <https://developers.google.com/ml-kit/terms>
- ML Kit Android data disclosure: <https://developers.google.com/ml-kit/android-data-disclosure>
- Text Recognition integration documentation: <https://developers.google.com/ml-kit/vision/text-recognition/v2/android>

## Lucide Icons

Selected Lucide SVG source files were converted to local Android
VectorDrawables for Heimdall's built-in macro icon registry. No Lucide runtime
library or package is included.

Project source: <https://github.com/lucide-icons/lucide>

License: ISC License. The selected arrow and crosshair icons also carry the
upstream Feather MIT notice. The complete upstream license text is packaged at
assistant/src/main/res/raw/lucide_license.txt.
