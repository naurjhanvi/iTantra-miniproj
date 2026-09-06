package com.itantra.transceiver

import android.Manifest
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.itantra.transceiver.ui.theme.ITantraTheme
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    ),
                    100
                )
            }
        }
        enableEdgeToEdge()
        setContent {
            ITantraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val bluetoothManager = remember { BluetoothManager() }
    val message = remember { mutableStateOf("") }
    val bluetoothStatus = remember { mutableStateOf("Disconnected 🔴") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "iTANTRA",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Offline Semantic Transceiver"
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Bluetooth: ${bluetoothStatus.value}"
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                val adapter = BluetoothAdapter.getDefaultAdapter()

                if (adapter != null) {
                    val pairedDevices = adapter.bondedDevices

                    val a50 = pairedDevices.firstOrNull { device ->
                        device.name?.contains("A50", ignoreCase = true) == true
                    }

                    if (a50 != null) {
                        bluetoothStatus.value = "Connecting... 🟡"
                        bluetoothManager.connectToDevice(
                            a50,
                            onConnected = {
                                bluetoothStatus.value = "Connected 🟢"
                            },
                            onError = { error ->
                                bluetoothStatus.value = "Connection failed 🔴"
                                println("Bluetooth error: $error")
                            }
                        )
                    } else {
                        bluetoothStatus.value = "A50 not found 🔴"
                    }
                }
            }
        ) {
            Text("CONNECT")
        }

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = message.value,
            onValueChange = {message.value = it },
            label = {
                Text("Message")
            },
            placeholder = {
                Text("Type a message...")
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { }
        ) {
            Text("SEND")
        }
    }
}

