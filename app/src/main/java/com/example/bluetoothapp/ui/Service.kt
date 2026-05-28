package com.example.bluetoothapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.bluetoothapp.ble.ServiceInfo


@Composable
fun ServiceUI(
    service: ServiceInfo,
    selectedItem: String?,
    onSelectedChange: (String?) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isSelected = selectedItem == service.uuid

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
                onSelectedChange(if (isSelected) null else service.uuid)
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.titleMedium
            )

            if (isSelected) {
                Text(text = "📋",
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(service.uuid))
                        Toast.makeText(context, "UUIDをコピーしました", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        Text(text = "UUID : ${service.uuid}")
    }
}
