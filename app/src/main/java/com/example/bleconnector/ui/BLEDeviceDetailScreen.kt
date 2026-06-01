package com.example.bleconnector.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.bleconnector.BLEConnector
import com.example.bleconnector.DeviceManager
import com.example.bleconnector.resolveUuid

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BLEDeviceDetailScreen(
    address: String,
    deviceManager: DeviceManager,
    navController: NavController,
    connector: BLEConnector
) {
    val device = deviceManager.devices
        .find { it.address == address }
    val context = LocalContext.current
    var selectedUuid by remember {
        mutableStateOf<String?>(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(device?.name ?: "Device")
                        Text(text = device?.address ?: "")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            Text(
                "State: ${device?.connectState}"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = device?.services ?: emptyList(),
                    key = { it.uuid }
                ) { service ->
                    val serviceInfo = resolveUuid(service.uuid)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "[S] ${serviceInfo.name}",
                                fontWeight = FontWeight.Bold
                            )
                            CopyableUuidText(
                                uuid = service.uuid,
                                selected = selectedUuid == service.uuid,
                                onClick = {
                                    selectedUuid = service.uuid
                                }
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            service.characteristics.forEachIndexed { index, ch ->
                                val chInfo = resolveUuid(ch.uuid)
                                val isLastCharacteristic =
                                    index == service.characteristics.lastIndex
                                val branch =
                                    if (isLastCharacteristic) "└─"
                                    else "├─"

                                Column(
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {

                                    Text("$branch [C] ${chInfo.name}")

                                    Column(
                                        modifier = Modifier.padding(start = 16.dp)
                                    ) {
                                        CopyableUuidText(
                                            uuid = ch.uuid,
                                            selected = selectedUuid == ch.uuid,
                                            onClick = {
                                                selectedUuid = ch.uuid
                                            }
                                        )
                                        Text(
                                            "Properties: ${ch.properties}"
                                        )
                                    }

                                    ch.descriptors.forEachIndexed { descIndex, desc ->
                                        val descInfo =
                                            resolveUuid(desc)
                                        val isLastDescriptor =
                                            descIndex == ch.descriptors.lastIndex
                                        val descBranch =
                                            if (isLastDescriptor) "└─"
                                            else "├─"
                                        Column(
                                            modifier = Modifier.padding(start = 16.dp)
                                        ) {
                                            Text(
                                                "$descBranch [D] ${descInfo.name}"
                                            )
                                            Column(
                                                modifier = Modifier.padding(start = 16.dp)
                                            ) {
                                                CopyableUuidText(
                                                    uuid = desc,
                                                    selected = selectedUuid == desc,
                                                    onClick = {
                                                        selectedUuid = desc
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                val granted = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    connector.disconnect(context)
                    navController.popBackStack()
                }
            }) {
                Text("Disconnect")
            }
        }
    }
}


@Composable
fun CopyableUuidText(
    uuid: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Text(
        text = "UUID: $uuid",
        color =
            if (selected) {
                Color(0xFF2196F3)
            } else {
                LocalContentColor.current
            },
        modifier = Modifier.clickable {
            val clipboard =
                context.getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "uuid",
                    uuid
                )
            )
            onClick()
        }
    )
}