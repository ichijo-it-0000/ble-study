package com.example.bleconnector.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.bleconnector.BLEConnector
import com.example.bleconnector.DeviceManager

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

    Column {
        Text("Device: ${device?.address}")
        Text("State: ${device?.connectState}")

        Spacer(modifier = Modifier.height(12.dp))

        device?.services?.forEach { service ->
            Text("SERVICE: ${service.uuid}")
            service.characteristics.forEach { ch ->
                Text("  ├ CHAR: ${ch.uuid}")
                Text("     props: ${ch.properties}")
                ch.descriptors.forEach { desc ->
                    Text("     └ DESC: $desc")
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