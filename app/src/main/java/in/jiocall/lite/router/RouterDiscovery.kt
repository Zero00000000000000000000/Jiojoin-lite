package in.jiocall.lite.router

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RouterDiscoveryResult(val host: String?, val message: String, val lineNumber: String? = null)

class RouterDiscovery(private val context: Context) {
    suspend fun discover(): RouterDiscoveryResult = withContext(Dispatchers.IO) {
        try {
            val found = JioFiberClient(context).discover()
            RouterDiscoveryResult(found.host,
                "Jio voice line found · ${found.line.routerMode.ifBlank { "Jio router" }}",
                maskNumber(found.line.msisdn))
        } catch (e: Exception) {
            RouterDiscoveryResult(null, e.message ?: "Router not found. Connect to JioAirFiber Wi-Fi.")
        }
    }

    private fun maskNumber(number: String) =
        if (number.length <= 4) "••••" else "••••••${number.takeLast(4)}"
}
