# JioJoin Lite — JioAirFiber Android client

An experimental Android client project for JioAirFiber voice calling. The goal is to discover the local Jio router, request JioFiber SIP credentials, and provide a lightweight dialer without depending on the JioJoin app.

## Current status

This is an early Android UI/build scaffold, **not a working calling app yet**. Router discovery and credential exchange are not connected to the UI in this initial repository commit. SIP calling requires the matching Jio SIP identity and a compatible native PJSIP/JNI engine; those pieces still need implementation and device testing.

## Build

Open this repository in Android Studio or use the GitHub Actions workflow under `.github/workflows/android.yml`. The workflow builds a debug APK and uploads it as an Actions artifact.

## Target

- Android (minimum SDK 26)
- Kotlin + Jetpack Compose
- Intended for JioAirFiber routers only

## Safety

Do not share router credentials, OTPs, SIP passwords, or phone numbers in issues or commits. Use only on a Jio connection and router you are authorized to access.

## License

The original JioFiber protocol research/source supplied for this project is GPL-3.0. Any ported code must retain its copyright and license notices. This initial UI scaffold is provided under GPL-3.0; see `LICENSE` before distributing.