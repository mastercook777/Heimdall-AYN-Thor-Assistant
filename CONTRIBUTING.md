# Contributing

Heimdall is hardware-specific software. A change that compiles on a desktop or
emulator is not automatically proven on AYN Thor.

Before opening a pull request:

1. Keep work inside the `:assistant` module and do not add keyboard/IME scope.
2. Preserve Profile JSON, macro sequence compatibility, Grid preview plus
   explicit save, dynamic controller discovery, and upper/lower display routing.
3. Never hardcode `/dev/input/eventX`, create a competing virtual controller, or
   commit signing material.
4. Run:

   ```text
   ./gradlew :assistant:assembleDebug :assistant:lintDebug --no-daemon
   ```

5. Describe what was tested on real Thor hardware, including firmware,
   controller mode, game or emulator, connection route, and whether USB was
   disconnected for performance observations.

Bug reports should include reproducible steps and logs or screenshots with
personal information removed. Do not attach copyrighted game files, vendor
APKs, signing keys, Profile exports containing personal data, or unrelated
device diagnostics.
