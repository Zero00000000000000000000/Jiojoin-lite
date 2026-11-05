# JioCall Lite

An experimental Android client for JioAirFiber voice service, initially targeting the JCO500 router and Android 13.

## Current implementation

- Kotlin + Jetpack Compose dialer shell.
- Local router discovery using `jiofiber.local.html`, the Android Wi-Fi gateway, and known Jio router fallback addresses.
- Router account lookup over HTTP port 8080.
- Temporary SIP credential request over HTTPS port 8443.
- Permanent device registration and OTP verification flow.
- SIP XML parsing. Credentials are held in memory only and are not printed to the UI or logs.
- GitHub Actions debug APK build.

## Important status

**This is not yet a functional calling app.** The router provisioning layer is implemented, but the Jio-compatible SIP registration and audio-call engine are not integrated. The Call button is intentionally a placeholder. Do not rely on this build for emergency calls.

## Build

GitHub Actions builds a debug APK on pushes to `main`. Open the Actions tab and download the `JioCallLite-debug-apk` artifact after a successful run.

## Router use

1. Connect the Android phone to the JioAirFiber Wi-Fi.
2. Open JioCall Lite and tap **Find JioAirFiber router**.
3. Request temporary credentials, or register the device and verify the OTP for permanent credentials.

The router endpoints are local-only. HTTPS provisioning uses a router-specific self-signed certificate, so certificate checks are relaxed only inside the router client after local-host validation. This should be reviewed before any public release.

## Compatibility

- Android 13 target device
- JioAirFiber JCO500 (router protocol compatibility must be verified on the actual device)
- Android min SDK 26

## License and attribution

The JioFiber protocol port is derived from the GPL-3.0-licensed JFC-RusTone project supplied as a source archive for this work. Preserve the upstream copyright and GPL-3.0 notices in any distribution. See the upstream project and its `LICENSE` file for the full license text.
