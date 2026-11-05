# Architecture and implementation plan

## Current modules

`app/src/main/java/in/jiocall/lite/`

- `MainActivity.kt`: Compose dialer and setup UI.
- `router/JioFiberClient.kt`: local router discovery, account lookup, credential provisioning and OTP exchange.
- `router/RouterDiscovery.kt`: IO-dispatched discovery wrapper and masked line display.

## Router protocol

For the JioAirFiber/JCO500 path currently implemented:

| Purpose | Endpoint |
|---|---|
| Voice line lookup | `http://<router>:8080/request_account` |
| SIP credential provisioning | `https://<router>:8443/` |
| SIP proxy | Router on port `5068`, TLS |

Temporary credentials use `nwk_intf=eth`. Permanent credentials use `nwk_intf=wifi`; device registration adds `op_type=add` and then verifies the router-issued OTP.

The router returns SIP values in XML `parm` elements. Never print credentials or OTPs to logs, crash reports or issue comments.

## SIP engine integration

The repository pins VoiSmart PJSIP Android as a Git submodule at `third_party/pjsip-android` and includes its `sipservice` Android library as a Gradle project dependency. The module supplies PJSUA2 Java bindings, JNI libraries and a background SIP service. GitHub Actions checks out submodules recursively and installs NDK `27.3.13750724`. Upstream revision: `f96e220ec8b7b6e359bb850f8ffaa6d34f45c360` (PJSIP 2.16; see the upstream Apache-2.0 license and bundled third-party notices).

**Integration is build-level only; Jio registration and calling are not enabled.** The JFC-RusTone source uses PJSIP with project-specific behavior for the Jio device identity. The remaining implementation must:

1. Build a supported PJSIP native library for Android ABIs (at least arm64-v8a; add armeabi-v7a only if needed).
2. Preserve the Jio device hash / MAC calculation used during provisioning.
3. Make the SIP Contact `+sip.instance` identity match the same device identity. A generic SIP wrapper may fail registration if this does not match.
4. Configure registrar/home domain from the router response, router proxy on TLS port 5068, and the proper auth identity.
5. Bridge registration, incoming/outgoing call state, DTMF, microphone, speaker and Bluetooth audio to Kotlin.
6. Add an Android foreground service and notification for active calls, plus runtime microphone permission handling.

Do not enable the Call button until the native engine is integrated and tested on the actual JCO500.

## Security notes

- The router HTTPS certificate is self-signed. The current client relaxes certificate verification only in the router client and only after validating a local router host. This is a compatibility compromise, not ideal public-release security.
- Router discovery and provisioning must only run while the phone is connected to the user's own JioAirFiber LAN.
- Credentials remain in process memory for this prototype. A production version should use Android Keystore-backed encrypted storage and provide a clear disconnect/delete-credentials action.
- Do not use this app for emergency calling until the full call stack and emergency-call limitations have been reviewed.

## Device validation checklist

- Confirm the router account endpoint returns a voice line on JCO500 firmware.
- Confirm temporary credentials and their validity window.
- Confirm OTP registration and permanent credential retrieval.
- Confirm TLS SIP registration to port 5068 with matching device identity.
- Test incoming/outgoing calls, DTMF, call audio, speaker, Bluetooth, network changes, process death and Android 13 permissions.
