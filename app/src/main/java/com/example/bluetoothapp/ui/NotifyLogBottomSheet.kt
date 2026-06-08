package com.example.bluetoothapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bluetoothapp.ble.BleCharacteristicNames
import com.example.bluetoothapp.ble.BleLogManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyLogBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn {
            items(BleLogManager.logs.reversed()) { log ->

                val characteristicName = BleCharacteristicNames.getName(log.uuid)

                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = log.timestamp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp
                        )
                        Text(text = "Service Name :  $characteristicName")
                        Text(text = "Received Data(HEX):  ${log.value.joinToString(" ") { "%02X".format(it) }}")
                        Text(text = "Received Data(UTF-8):  ${String(log.value)}")
                    }
                }
            }
        }
    }
}