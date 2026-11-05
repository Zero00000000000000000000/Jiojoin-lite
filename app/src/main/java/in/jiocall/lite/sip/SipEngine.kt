package in.jiocall.lite.sip

import in.jiocall.lite.router.SipCredentials

/**
 * Contract for the future Jio-compatible native PJSIP/JNI bridge.
 * No implementation is wired yet: keep call actions disabled until registration
 * and device identity handling are implemented and tested on a JCO500.
 */
data class SipAccountConfig(
    val credentials: SipCredentials,
    val routerHost: String,
    val proxyPort: Int = 5068,
    val useTls: Boolean = true
)

enum class SipRegistrationState { DISCONNECTED, CONNECTING, REGISTERED, FAILED }
enum class SipCallState { IDLE, CALLING, RINGING, ACTIVE, ENDED, FAILED }

interface SipEngine {
    suspend fun register(config: SipAccountConfig)
    suspend fun unregister()
    suspend fun call(destination: String)
    suspend fun answer()
    suspend fun hangup()
    suspend fun sendDtmf(digits: String)
}
