package com.example.bluetoothapp.ble

import android.Manifest
import android.content.Context
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import androidx.annotation.RequiresPermission

class BleConnector(
    private val context: Context,
    private val onConnected: (BluetoothDevice) -> Unit,
    private val onDisconnected: (BluetoothDevice) -> Unit
) {
    private var gatt: BluetoothGatt? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice) {
        gatt = device.connectGatt(context, false, gattCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        gatt?.disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            when {
                status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED -> {
                    onConnected(gatt.device)
                }
                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    gatt.close()
                    onDisconnected(gatt.device)
                }
            }
        }
    }
}