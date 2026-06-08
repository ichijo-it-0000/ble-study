package com.example.bluetoothapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.example.bluetoothapp.ble.CharacteristicInfo
import com.example.bluetoothapp.ble.BatteryMonitor

import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.unit.dp
import com.example.bluetoothapp.ble.BleLogManager
import com.example.bluetoothapp.ble.LogType

@Composable
fun CharacteristicsUI(
    characteristic: CharacteristicInfo,
    selectedItem: String?,
    onSelectedChange: (String?) -> Unit,
    onReadRequest: (CharacteristicInfo) -> Unit,
    onWriteRequest: (CharacteristicInfo, String) -> Unit,
    onNotifyRequest: (CharacteristicInfo) -> Unit
) {
    val isSelected = selectedItem == characteristic.uuid

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected)
                    Color(0x332196F3)
                else
                    Color.Transparent
            )
            .clickable {
                onSelectedChange(if (isSelected) null else characteristic.uuid)
                onNotifyRequest(characteristic)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "├─ ${characteristic.name}",
                style = MaterialTheme.typography.titleSmall
            )
            if (isSelected) {
                Text(
                    text = "📋",
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(characteristic.uuid))
                        Toast.makeText(context, "UUIDをコピーしました", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        Text(text = "│   ├─ UUID : ${characteristic.uuid}")
    val canRead = characteristic.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
    val canWrite = characteristic.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
    val canNotify = characteristic.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

    var writeText by remember { mutableStateOf("") }

    Text(text = "│   └─ Property : ${listOfNotNull(
        if (canRead) "READ" else null,
        if (canWrite) "WRITE" else null,
        if (canNotify) "NOTIFY" else null
    ).joinToString(" | ")}")

    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        if (canRead) {
            Button(onClick = { onReadRequest(characteristic) }) {
                Text("READ")
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (canWrite) {
            Button(
                onClick = { onWriteRequest(characteristic, writeText) },
                enabled = writeText.isNotEmpty()
            ) { Text("WRITE") }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (canNotify) {
            Button(onClick = { onNotifyRequest(characteristic) }) {
                Text("NOTIFY")
            }
        }
    }

    if (canWrite) {
        OutlinedTextField(
            value = writeText,
            onValueChange = { writeText = it },
            label = { Text("Write text") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
        if (characteristic.name == "Battery Level") {
            val batteryLevel = BatteryMonitor.level
            Text(
                text = batteryLevel?.let {
                    "│   └─ Battery Percentage : $it % (hex : ${"%02X".format(it)})"
                } ?: "│   └─ Battery Percentage : ---"
            )
        }
    }
}
