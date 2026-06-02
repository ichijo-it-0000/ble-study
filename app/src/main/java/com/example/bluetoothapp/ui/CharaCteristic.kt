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

@Composable
fun CharacteristicsUI(
    characteristic: CharacteristicInfo,
    selectedItem: String?,
    onSelectedChange: (String?) -> Unit,
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
        Text(text = "│   └─ Property : ${characteristic.properties}")
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
