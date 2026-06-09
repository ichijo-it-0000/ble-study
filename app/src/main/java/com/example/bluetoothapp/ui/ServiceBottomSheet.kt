package com.example.bluetoothapp.ui


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column

import com.example.bluetoothapp.ble.ServiceInfo
import com.example.bluetoothapp.ble.CharacteristicInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import com.example.bluetoothapp.ble.BleLogManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBottomSheet(
    services: List<ServiceInfo>,
    onDismiss: () -> Unit,
    onReadRequest: (CharacteristicInfo) -> Unit,
    onWriteRequest: (CharacteristicInfo, String) -> Unit,
    onNotifyRequest: (CharacteristicInfo) -> Unit
) {
    var selectedItem by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn {
            items(items = services, key = { it.uuid }) { service ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                ) {
                    Column {
                        ServiceUI(
                            service = service,
                            selectedItem = selectedItem,
                            onSelectedChange = { selectedItem = it }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        service.characteristics.forEach { characteristic ->
                            CharacteristicsUI(
                                characteristic = characteristic,
                                selectedItem = selectedItem,
                                onSelectedChange = { selectedItem = it },
                                onReadRequest = onReadRequest,
                                onWriteRequest = onWriteRequest,
                                onNotifyRequest = onNotifyRequest
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    LogDisplayArea(characteristics = service.characteristics)
                }
            }
        }
    }
}

@Composable
fun LogDisplayArea(characteristics: List<CharacteristicInfo>) {
    val characteristicUuids = characteristics.map { it.uuid }.toSet()
    
    val logs = BleLogManager.logs.filter { log -> characteristicUuids.contains(log.uuid) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .padding(12.dp)
    ) {
        Text(
            text = "Connection Logs",
            style = MaterialTheme.typography.titleSmall
        )

        if (logs.isEmpty()) {
            Text(text = "No Logs", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(logs.asReversed()) { log ->
                    val hex = log.value.joinToString(" ") { "%02X".format(it) }
                    val asText = runCatching { String(log.value, Charsets.UTF_8) }.getOrNull() ?: ""

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "[${log.timestamp}] ${log.type.name}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(text = "HEX: $hex", style = MaterialTheme.typography.bodySmall)
                        Text(text = "TEXT: $asText", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}