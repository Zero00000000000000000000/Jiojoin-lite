package in.jiocall.lite.router

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.json.JSONObject
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

// Protocol port based on the GPL-3.0 JFC-RusTone JioFiber implementation.
enum class CredentialMode(val networkInterface: String) {
    TEMPORARY("eth"), PERMANENT("wifi")
}

data class JioLine(
    val msisdn: String,
    val imsi: String,
    val routerMode: String,
    val serialNumber: String,
    val macAddress: String
)

data class SipCredentials(
    val username: String,
    val password: String,
    val homeDomain: String,
    val realm: String?,
    val validitySeconds: Long?
)

data class OtpChallenge(val maskedDestination: String?, internal val cookie: String?)
data class JioDiscovery(val host: String, val line: JioLine)
class JioRouterException(message: String) : Exception(message)

/**
 * JioAirFiber/JioFiber router provisioning client. Intended for a local JCO500
 * router only. The router uses HTTP :8080 for account discovery and HTTPS
 * :8443 with a router-local certificate for credential provisioning.
 *
 * WARNING: TLS certificate verification is relaxed only inside this client
 * because the router presents a self-signed certificate. Host validation
 * restricts requests to local/private addresses or the known router hostname.
 */
class JioFiberClient(private val context: Context) {
    private val hostname: String by lazy {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "android-device"
        "JioCall-${id.take(12)}"
    }

    private val http: OkHttpClient by lazy {
        val trustManagers = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers() = emptyArray<java.security.cert.X509Certificate>()
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
        })
        val ssl = SSLContext.getInstance("TLS").apply {
            init(null, trustManagers, java.security.SecureRandom())
        }
        OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trustManagers[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    }

    fun candidates(): List<String> = linkedSetOf<String>().apply {
        add("jiofiber.local.html")
        defaultGateway()?.let(::add)
        add("192.168.29.1")
        add("192.168.31.1")
    }.toList()

    @Suppress("DEPRECATION")
    private fun defaultGateway(): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val gateway = wifi.dhcpInfo?.gateway ?: return null
        if (gateway == 0) return null
        val bytes = byteArrayOf(
            (gateway and 0xff).toByte(), ((gateway shr 8) and 0xff).toByte(),
            ((gateway shr 16) and 0xff).toByte(), ((gateway shr 24) and 0xff).toByte()
        )
        return runCatching { InetAddress.getByAddress(bytes).hostAddress }.getOrNull()
    }

    fun discover(): JioDiscovery {
        var lastError = "No JioAirFiber voice router found. Connect this phone to JioAirFiber Wi-Fi."
        for (host in candidates()) {
            try {
                return JioDiscovery(host, requestAccount(host))
            } catch (e: Exception) {
                lastError = e.message ?: lastError
            }
        }
        throw JioRouterException(lastError)
    }

    fun requestAccount(host: String): JioLine {
        requireRouterHost(host)
        val request = Request.Builder().url("http://${urlHost(host)}:8080/request_account").get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw JioRouterException("Router account request failed (HTTP ${response.code})")
            val obj = JSONObject(response.body?.string() ?: throw JioRouterException("Empty router response"))
            val number = obj.optString("msisdn").trim()
            if (number.isEmpty()) throw JioRouterException("This router did not report a Jio voice line")
            return JioLine(number, obj.optString("imsi"), obj.optString("mode"),
                obj.optString("SerialNumber"), obj.optString("mac_address"))
        }
    }

    fun requestCredentials(host: String, mode: CredentialMode): SipCredentials {
        val response = httpsGet(host, "/?${credentialsQuery(mode, false)}")
        if (response.code != 200) throw JioRouterException("Router refused ${mode.name.lowercase()} credentials (HTTP ${response.code})")
        return parseCredentials(response.body)
    }

    fun registerPermanentDevice(host: String): OtpChallenge {
        val response = httpsGet(host, "/?${credentialsQuery(CredentialMode.PERMANENT, true)}&")
        if (response.code != 200) throw JioRouterException("Could not start OTP registration (HTTP ${response.code})")
        return OtpChallenge(response.headers["x-amn"], response.headers["set-cookie"])
    }

    fun verifyOtp(host: String, challenge: OtpChallenge, otp: String) {
        val digits = otp.trim()
        if (digits.isEmpty() || digits.any { !it.isDigit() }) throw JioRouterException("Enter the OTP digits only")
        val headers = challenge.cookie?.let { mapOf("Cookie" to it) } ?: emptyMap()
        val response = httpsGet(host, "/?OTP=${encode(digits)}", headers)
        if (response.code != 200) throw JioRouterException("OTP verification failed (HTTP ${response.code})")
    }

    private fun credentialsQuery(mode: CredentialMode, register: Boolean): String {
        val hash = deviceHash(hostname)
        val mac = "00:00:" + (0..3).joinToString(":") { "%02x".format((hash shr (it * 8)) and 0xff) }
        val fields = listOf(
            "terminal_sw_version" to "RCSAndrd", "terminal_vendor" to hostname,
            "terminal_model" to hostname, "SMS_port" to "0", "act_type" to "volatile",
            "IMSI" to "", "msisdn" to "", "IMEI" to "", "vers" to "0", "token" to "",
            "rcs_state" to "0", "rcs_version" to "5.1B", "rcs_profile" to "joyn_blackbird",
            "client_vendor" to "JUIC", "default_sms_app" to "2", "default_vvm_app" to "0",
            "device_type" to "vvm", "client_version" to "JSEAndrd-1.0",
            "mac_address" to mac, "alias" to hostname, "nwk_intf" to mode.networkInterface
        ) + if (register) listOf("op_type" to "add") else emptyList()
        return fields.joinToString("&") { (key, value) -> "$key=${encode(value)}" }
    }

    private fun httpsGet(host: String, path: String, headers: Map<String, String> = emptyMap()): HttpAnswer {
        requireRouterHost(host)
        val request = Request.Builder().url("https://${urlHost(host)}:8443$path")
            .apply { headers.forEach { (k, v) -> header(k, v) } }.get().build()
        http.newCall(request).execute().use { response ->
            return HttpAnswer(response.code, response.headers, response.body?.string().orEmpty())
        }
    }

    private fun parseCredentials(xml: String): SipCredentials {
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            val doc = factory.newDocumentBuilder().parse(xml.byteInputStream())
            val values = mutableMapOf<String, String>()
            val nodes = doc.getElementsByTagName("parm")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as? Element ?: continue
                if (node.hasAttribute("name") && node.hasAttribute("value")) {
                    values[node.getAttribute("name").lowercase()] = node.getAttribute("value").trim()
                }
            }
            val user = values["username"].orEmpty()
            val password = values["userpwd"].orEmpty()
            val domain = values["home_network_domain_name"].orEmpty()
            if (user.isEmpty() || password.isEmpty() || domain.isEmpty()) {
                throw JioRouterException("Router response lacks SIP username, password or domain")
            }
            return SipCredentials(user, password, domain, values["realm"],
                values["validity"]?.toLongOrNull()?.takeIf { it > 0 })
        } catch (e: JioRouterException) {
            throw e
        } catch (_: Exception) {
            throw JioRouterException("Could not parse router SIP configuration")
        }
    }

    private fun requireRouterHost(host: String) {
        val h = host.trim().removePrefix("[").removeSuffix("]")
        val address = runCatching { InetAddress.getByName(h) }.getOrNull()
        val local = address?.let { ip ->
            val bytes = ip.address.map { it.toInt() and 0xff }
            when (bytes.size) {
                4 -> bytes[0] == 10 || (bytes[0] == 172 && bytes[1] in 16..31) ||
                    (bytes[0] == 192 && bytes[1] == 168) || bytes[0] == 127
                16 -> ip.isSiteLocalAddress || ip.isLoopbackAddress || ip.isLinkLocalAddress
                else -> false
            }
        } ?: false
        if (h != "jiofiber.local.html" && !local) throw JioRouterException("Blocked non-local router address")
    }

    private fun urlHost(host: String) = if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    private fun deviceHash(value: String): Long = value.toByteArray(Charsets.UTF_8).fold(0L) { hash, byte ->
        (hash * 33L + (byte.toInt() and 0xff)) and 0xffffffffL
    }

    private data class HttpAnswer(val code: Int, val headers: okhttp3.Headers, val body: String)
}
