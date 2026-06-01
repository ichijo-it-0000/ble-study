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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBottomSheet(
    services: List<ServiceInfo>,
    onDismiss: () -> Unit,
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
                                onNotifyRequest = onNotifyRequest
                            )
                        }
                    }
                }
            }
        }
    }
}