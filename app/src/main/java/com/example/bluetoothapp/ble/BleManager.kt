package com.example.bluetoothapp.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.annotation.RequiresPermission
import android.util.Log

class BleManager(
    private val context: Context,
    bluetoothAdapter: BluetoothAdapter,
    private val onDevicesUpdated: () -> Unit
) {
    private val registry = DeviceRegistry()
    private val scanner = BleScanner(
        bluetoothAdapter = bluetoothAdapter,
        onDeviceFound = { device ->
            registry.addOrUpdate(device)
            onDevicesUpdated()
        }
    )

    private val connector = BleConnector(
        context = context,

        onConnected = { device ->
            Log.i("BleManager", "CONNECTED : ${device.address}")
            registry.setConnectionState(device.address, ConnectionState.CONNECTED)
            onDevicesUpdated()
        },

        onDisconnected = { device ->
            Log.i("BleManager", "DISCONNECTED : ${device.address}")
            registry.setConnectionState(device.address, ConnectionState.DISCONNECTED)
            onDevicesUpdated()
        }
    )
    fun startScan() {
        scanner.startScan()
    }

    fun stopScan() {
        scanner.stopScan()
    }

    fun getDevices(): List<BleDevice> {
        return registry.getDevices()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BleDevice) {
        Log.i("BleManager", "CONNECT START : ${device.address}")

        registry.setConnectionState(device.address, ConnectionState.CONNECTING)
        onDevicesUpdated()
        connector.connect(device.bluetoothDevice)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect(device: BleDevice) {
        Log.i("BleManager", "DISCONNECT START")

        registry.setConnectionState(device.address, ConnectionState.DISCONNECTING)
        onDevicesUpdated()
        connector.disconnect()
    }

}