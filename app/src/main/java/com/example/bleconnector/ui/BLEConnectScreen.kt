package com.example.bleconnector.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.bleconnector.BLEConnector
import com.example.bleconnector.BLEScanner
import com.example.bleconnector.DeviceManager
import com.example.bleconnector.FilterType
import com.example.bleconnector.MainActivity
import com.example.bleconnector.ScanFilterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BLEConnectScreen(
    scanner: BLEScanner,
    connector: BLEConnector,
    deviceManager: DeviceManager
) {
    var isScanning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as MainActivity

    var filterType by remember { mutableStateOf(FilterType.DEVICE_NAME) }
    var filterText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Scanner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.DarkGray,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            ScanControlSection(
                onStartScan = {
                    val permissions =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        } else {
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }
                    val hasPermission =
                        permissions.all { permission ->
                            ActivityCompat.checkSelfPermission(
                                context,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        }

                    if (!hasPermission) {
                        ActivityCompat.requestPermissions(
                            activity,
                            permissions,
                            100
                        )
                        return@ScanControlSection
                    }

                    deviceManager.clear()

                    scanner.startScanBLEDevices(
                        filterState = ScanFilterState(
                            type = filterType,
                            text = filterText
                        )
                    ) { device ->
                        deviceManager.upsert(device)
                    }

                    isScanning = true
                },

                onStopScan = {
                    scanner.stopScanBLEDevices(context)

                    isScanning = false
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Filter",
                fontWeight = FontWeight.Bold
            )

            Row {

                Row {
                    RadioButton(
                        selected =
                            filterType == FilterType.DEVICE_NAME,

                        onClick = {
                            filterType = FilterType.DEVICE_NAME
                        }
                    )

                    Text("Name")
                }

                Row {
                    RadioButton(
                        selected =
                            filterType == FilterType.MAC_ADDRESS,

                        onClick = {
                            filterType = FilterType.MAC_ADDRESS
                        }
                    )

                    Text("MAC")
                }
            }

            TextField(
                value = filterText,

                onValueChange = {
                    filterText = it
                },

                modifier = Modifier.fillMaxWidth(),

                label = {
                    Text("Filter text")
                }
            )
            Column {
                Text(
                    text = if (isScanning) { "Scanning..." } else { "Stopped" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                LazyColumn {
                    items(
                        items = deviceManager.devices,
                        key = { it.address }
                    ) { device ->
                        DeviceCard(
                            device = device,
                            onConnectClick = {connector.connect(it)}
                        )
                    }
                }
            }
        }
    }
}