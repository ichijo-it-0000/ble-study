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

@Composable
fun DeviceList(devices: List<BleDevice>, onDeviceClick: (BleDevice) ->Unit) {
    LazyColumn {
        items(devices) {
            device -> DeviceCard(device = device, onClick = onDeviceClick)
        }
    }
}

@Composable
fun DeviceCard(device: BleDevice, onClick: (BleDevice) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp),
         onClick = { onClick(device) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "NAME : ${device.name}")
            Text(text = "RSSI : ${device.rssi}")
            Text(text = "MAC : ${device.address}")
        }
    }
}