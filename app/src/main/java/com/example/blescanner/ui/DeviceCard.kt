package com.example.blescanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.blescanner.BLEDevice
import com.example.blescanner.rssiToLevel
import com.example.blescanner.signalBar

@Composable
fun DeviceCard(device: BLEDevice) {
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
                fontFamily = FontFamily.Monospace
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
    }
}