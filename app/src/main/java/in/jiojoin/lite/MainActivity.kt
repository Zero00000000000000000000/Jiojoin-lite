package in.jiojoin.lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JioJoinLiteScreen() }
    }
}

@Composable
private fun JioJoinLiteScreen() {
    var number by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not connected") }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Text("JioJoin Lite", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("JioAirFiber calling", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Connection", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(status, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { status = "Router connection is not configured yet" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Connect to JioAirFiber")
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("Enter phone number", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = number, onValueChange = { number = it.filter(Char::isDigit).take(15) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Phone number") })
                Spacer(Modifier.height(18.dp))
                Button(onClick = { status = "Calling is not available yet — SIP engine integration is pending" },
                    modifier = Modifier.fillMaxWidth(), enabled = number.isNotBlank()) { Text("Call") }
                Spacer(Modifier.weight(1f))
                Text("Prototype build • Calling engine not integrated", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}
