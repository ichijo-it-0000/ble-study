package com.example.bluetoothapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bluetoothapp.ble.BleDevice
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import com.example.bluetoothapp.ble.ConnectionState

@Composable
fun DeviceList(
    devices: List<BleDevice>,
    onConnect: (BleDevice) -> Unit,
    onDisconnect: (BleDevice) -> Unit
) {
    LazyColumn {
        items(devices) { device ->
            DeviceCard(device = device, onConnect = onConnect, onDisconnect = onDisconnect)
        }
    }
}
@Composable
fun DeviceCard(
    device: BleDevice,
    onConnect: (BleDevice) -> Unit,
    onDisconnect: (BleDevice) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "NAME : ${device.name}")
            Text(text = "RSSI : ${device.rssi}")
            Text(text = "MAC : ${device.address}")
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = when (device.connectionState) {
                        ConnectionState.CONNECTED -> "🟢 Connected"
                        ConnectionState.CONNECTING -> "🟡 Connecting..."
                        ConnectionState.DISCONNECTING -> "🟠 Disconnecting..."
                        ConnectionState.DISCONNECTED -> "⚪ Disconnected"
                    }
                )
                Button(
                    enabled = device.connectionState != ConnectionState.CONNECTING &&
                                device.connectionState != ConnectionState.DISCONNECTING,
                    onClick = {
                        if (device.connectionState == ConnectionState.CONNECTED) {
                            onDisconnect(device)
                        } else {
                            onConnect(device)
                        }
                    }
                ) {
                    Text( when (device.connectionState) {
                            ConnectionState.CONNECTED -> "切断"
                            ConnectionState.DISCONNECTED -> "接続"
                            ConnectionState.CONNECTING -> "接続中..."
                            ConnectionState.DISCONNECTING -> "切断中..."
                        }
                    )
                }
            }
        }
    }
}