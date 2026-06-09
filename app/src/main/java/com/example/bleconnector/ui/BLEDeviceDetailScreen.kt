package com.example.bleconnector.ui

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.bleconnector.BLECommand
import com.example.bleconnector.BLEConnector
import com.example.bleconnector.DeviceManager
import com.example.bleconnector.NotifyStore
import com.example.bleconnector.UuidRegistry
import com.example.bleconnector.UuidType
import com.example.bleconnector.WriteStore

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BLEDeviceDetailScreen(
    address: String,
    deviceManager: DeviceManager,
    navController: NavController,
    connector: BLEConnector,
    notifyStore: NotifyStore,
    writeStore: WriteStore
) {
    val device = deviceManager.devices
        .find { it.address == address }
    val context = LocalContext.current
    var selectedUuid by remember {
        mutableStateOf<String?>(null)
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val sendTextMap = remember { mutableStateMapOf<String, String>() }

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
                .fillMaxSize()
        ) {

            Text(
                "State: ${device?.connectState}"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            PrimaryTabRow( selectedTabIndex = selectedTab ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                ) {
                    Text("Overview")
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                ) {
                    Text("Operation")
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            when(selectedTab) {
                0 -> {
                    LazyColumn(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                    ) {
                        items(
                            items = device?.services ?: emptyList(),
                            key = { it.uuid }
                        ) { service ->
                            val serviceInfo = UuidRegistry.resolve(service.uuid)
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
                                        val chInfo = UuidRegistry.resolve(ch.uuid)
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
                                                    "Properties: " +
                                                            "R=${ch.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0}, " +
                                                            "W=${ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0}, " +
                                                            "N=${ch.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0}"
                                                )
                                            }

                                            ch.descriptors.forEachIndexed { descIndex, desc ->
                                                val descInfo =
                                                    UuidRegistry.resolve(desc)
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
                }
                1 -> {
                    LazyColumn(modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(device?.services ?: emptyList()) { service ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "Service",
                                        fontWeight = FontWeight.Bold
                                    )

                                    val serviceInfo = UuidRegistry.resolve(service.uuid)
                                    Text(
                                        text = if (serviceInfo.type == UuidType.CUSTOM) {
                                            "Custom Service"
                                        } else {
                                            serviceInfo.name
                                        },
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = service.uuid,
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    /* =========================
                                     * Characteristic
                                     * ========================= */
                                    service.characteristics.forEach { ch ->
                                        val props = ch.properties
                                        val canRead = (props and BluetoothGattCharacteristic.PROPERTY_READ) != 0
                                        val canWrite = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                                        val canNotify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {

                                            Column(Modifier.padding(12.dp)) {
                                                Text("Characteristic", fontWeight = FontWeight.Bold)
                                                Text(ch.uuid)

                                                Spacer(Modifier.height(8.dp))

                                                Text(
                                                    "Properties: " +
                                                            "R=${ch.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0}, " +
                                                            "W=${ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0}, " +
                                                            "N=${ch.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0}"
                                                )

                                                Spacer(Modifier.height(8.dp))

                                                /* =========================
                                                 * NOTIFY
                                                 * ========================= */
                                                if (canNotify) {
                                                    val latest = notifyStore.latestMap[ch.uuid]
                                                    val history = notifyStore.history

                                                    val notifyValue =
                                                        if (latest?.charUuid == ch.uuid) latest else null

                                                    Text("Notify", fontWeight = FontWeight.Bold)

                                                    Column(Modifier.padding(12.dp)) {
                                                        Text("Latest", fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = notifyValue?.let { data ->
                                                                val raw = data.value.joinToString()
                                                                val hex = notifyStore.toHex(data.value)
                                                                """
                                                            Raw: $raw
                                                            Hex: $hex
                                                            """.trimIndent()
                                                            } ?: "No Data"
                                                        )

                                                        Spacer(Modifier.height(6.dp))

                                                        /* =========================
                                                         * HISTORY
                                                         * ========================= */
                                                        val filteredHistory =
                                                            history.filter { it.charUuid == ch.uuid }

                                                        if (filteredHistory.isNotEmpty()) {
                                                            Text(text = "Log", fontWeight = FontWeight.Bold)
                                                            Card(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 4.dp),
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = Color.Transparent
                                                                ),
                                                                border = androidx.compose.foundation.BorderStroke(
                                                                    1.dp,
                                                                    Color.Black
                                                                )
                                                            ) {
                                                                Column(Modifier.padding(8.dp)) {
                                                                    Spacer(modifier = Modifier.height(8.dp))

                                                                    LazyColumn(
                                                                        modifier = Modifier.height(120.dp)
                                                                    ) {
                                                                        items(filteredHistory.reversed()) { item ->
                                                                            val raw = item.value.joinToString()
                                                                            val hex = notifyStore.toHex(item.value)

                                                                            Column(
                                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                                            ) {
                                                                                Text("Raw: $raw")
                                                                                Text("Hex: $hex")
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        Spacer(Modifier.height(8.dp))
                                                    }
                                                }

                                                /* =========================
                                                 * WRITE
                                                 * ========================= */
                                                if (canWrite) {
                                                    Text("Write", fontWeight = FontWeight.Bold)

                                                    OutlinedTextField(
                                                        value = sendTextMap[ch.uuid] ?: "",
                                                        onValueChange = {
                                                            sendTextMap[ch.uuid] = it
                                                        },
                                                        label = { Text("Input") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Button(onClick = {
                                                        val serviceUuid = service.uuid
                                                        val characteristicUuid = ch.uuid
                                                        val text = sendTextMap[ch.uuid] ?: ""
                                                        val (isCommand, command) = when (text.uppercase()) {
                                                            "ON" -> true to BLECommand.ON
                                                            "OFF" -> true to BLECommand.OFF
                                                            else -> false to null
                                                        }
                                                        if (isCommand && command != null) {
                                                            connector.sendCommand(
                                                                command,
                                                                serviceUuid,
                                                                characteristicUuid
                                                            )
                                                        }
                                                        // それ以外はそのまま送信
                                                        else {
                                                            connector.writeCharacteristic(
                                                                serviceUuid = serviceUuid,
                                                                characteristicUuid = characteristicUuid,
                                                                value = text.toByteArray()
                                                            )
                                                        }
                                                    }) {
                                                        Text("Send")
                                                    }

                                                    Column(
                                                        modifier = Modifier.padding(12.dp)
                                                    ) {
                                                        Text("Log", fontWeight = FontWeight.Bold)
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = Color.Transparent
                                                            ),
                                                            border = androidx.compose.foundation.BorderStroke(
                                                                1.dp,
                                                                Color.Black
                                                            )
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(8.dp)
                                                            ) {

                                                                writeStore.history(ch.uuid)
                                                                    .reversed()
                                                                    .forEach { item ->

                                                                        val text = buildString {
                                                                            append("input: ${item.input}")
                                                                            append(" | result: ${item.result}")
                                                                            item.code?.let {
                                                                                append(" | code: $it")
                                                                            }
                                                                        }

                                                                        Text(text)

                                                                        Spacer(
                                                                            modifier = Modifier.height(
                                                                                4.dp
                                                                            )
                                                                        )
                                                                    }
                                                            }
                                                        }
                                                    }
                                                }
                                                if (canRead) {
                                                    Text("Read", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
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
                device?.let {
                    if (granted) {
                        connector.disconnect(context, it.address)
                        navController.popBackStack()
                    }
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