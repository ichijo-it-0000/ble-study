package com.example.bleconnector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bleconnector.BLEDevice
import com.example.bleconnector.ConnectState
import com.example.bleconnector.rssiToLevel
import com.example.bleconnector.signalBar

fun connectStateColor(state: ConnectState): Color {
    return when (state) {
        ConnectState.DISCONNECTED -> Color.Gray
        ConnectState.CONNECTING -> Color(0xFFFFA000) // orange
        ConnectState.CONNECTED -> Color(0xFF4CAF50)   // green
    }
}

@Composable
fun DeviceCard(
    device: BLEDevice,
    onConnectClick: (BLEDevice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "NAME : ${device.name}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(
                modifier = Modifier.height(4.dp)
            )
            Text(
                text = "MAC  : ${device.address}",
                fontFamily = FontFamily.Monospace
            )
            Spacer(
                modifier = Modifier.height(4.dp)
            )
            Text(
                text = when (device.connectState) {
                    ConnectState.DISCONNECTED -> "Disconnect"
                    ConnectState.CONNECTING -> "Connecting"
                    ConnectState.CONNECTED -> "Connected"
                },
                color = connectStateColor(device.connectState)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RSSI : ${device.rssi} dBm",
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = signalBar(rssiToLevel(device.rssi)),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(
            Modifier.height(8.dp)
        )
        Button(
            onClick = { onConnectClick(device) }
        ) {
            Text("Connect")
        }
    }
}