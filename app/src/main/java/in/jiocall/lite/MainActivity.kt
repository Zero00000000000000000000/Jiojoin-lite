package in.jiocall.lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import in.jiocall.lite.router.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JioCallLiteApp() }
    }
}

@Composable
private fun JioCallLiteApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { JioFiberClient(context.applicationContext) }
    var phone by remember { mutableStateOf("") }
    var host by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Connect your phone to JioAirFiber Wi-Fi") }
    var line by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf<OtpChallenge?>(null) }
    var credentialsReady by remember { mutableStateOf(false) }

    fun runTask(label: String, task: suspend () -> String) {
        scope.launch {
            busy = true
            status = label
            try { status = task() } catch (e: Exception) {
                status = e.message ?: "Operation failed"
            } finally { busy = false }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF55D6A5), background = Color(0xFF101512), surface = Color(0xFF1A211D)
    )) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("JioCall Lite", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("JioAirFiber · JCO500 · Android 13", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Text("BETA", color = Color(0xFF55D6A5), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ROUTER / VOICE LINE", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        Text(status, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        line?.let { Text("Jio number: $it", color = Color(0xFF55D6A5), fontSize = 13.sp) }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            runTask("Searching for JCO500…") {
                                val found = withContext(Dispatchers.IO) { client.discover() }
                                host = found.host
                                line = if (found.line.msisdn.length <= 4) "••••" else "••••••${found.line.msisdn.takeLast(4)}"
                                "Router found at ${found.host}; Jio voice line detected"
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(if (busy) "Please wait…" else "1 · Find JioAirFiber router")
                        }
                        Button(onClick = {
                            val router = host
                            if (router != null) runTask("Requesting temporary SIP credentials…") {
                                withContext(Dispatchers.IO) { client.requestCredentials(router, CredentialMode.TEMPORARY) }
                                credentialsReady = true
                                "Temporary SIP credentials received. SIP engine integration is the next step."
                            } else status = "Find the router first"
                        }, enabled = !busy && host != null, modifier = Modifier.fillMaxWidth()) {
                            Text("2 · Get temporary credentials")
                        }
                        OutlinedButton(onClick = {
                            val router = host
                            if (router != null) runTask("Starting permanent device registration…") {
                                val result = withContext(Dispatchers.IO) { client.registerPermanentDevice(router) }
                                challenge = result
                                "OTP request started. Check your Jio number ${result.maskedDestination.orEmpty()}."
                            } else status = "Find the router first"
                        }, enabled = !busy && host != null, modifier = Modifier.fillMaxWidth()) {
                            Text("Register device (OTP)")
                        }
                        if (challenge != null) {
                            OutlinedTextField(value = otp, onValueChange = { otp = it.filter(Char::isDigit).take(8) },
                                label = { Text("OTP from Jio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                val router = host
                                val current = challenge
                                if (router != null && current != null) runTask("Verifying OTP…") {
                                    withContext(Dispatchers.IO) { client.verifyOtp(router, current, otp) }
                                    val permanent = withContext(Dispatchers.IO) {
                                        client.requestCredentials(router, CredentialMode.PERMANENT)
                                    }
                                    credentialsReady = true
                                    challenge = null
                                    otp = ""
                                    "Permanent SIP credentials received. SIP engine integration is still required."
                                }
                            }, enabled = !busy && otp.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Text("Verify OTP")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("DIALER", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(phone.ifBlank { "Enter number" }, fontSize = 29.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(42.dp))
                val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#"))
                keys.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { key ->
                            Button(onClick = { phone += key }, shape = CircleShape,
                                modifier = Modifier.size(62.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26332C))) {
                                Text(key, fontSize = 22.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { phone = phone.dropLast(1) }) { Text("Delete") }
                    Button(onClick = { status = "Calling is not enabled yet: native Jio-compatible SIP engine is not integrated." },
                        enabled = phone.isNotBlank() && credentialsReady, colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF159B67))) { Text("Call") }
                    TextButton(onClick = { phone = "" }) { Text("Clear") }
                }
                Spacer(Modifier.weight(1f))
                Text("Prototype: router provisioning implemented; SIP registration and audio calling pending.",
                    color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
