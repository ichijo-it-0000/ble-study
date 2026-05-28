package com.example.bluetoothapp.ble

import android.Manifest
import android.content.Context
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import androidx.annotation.RequiresPermission
import android.bluetooth.BluetoothGattService
import android.util.Log

class BleConnector(
    private val context: Context,
    private val onConnected: (BluetoothDevice) -> Unit,
    private val onDisconnected: (BluetoothDevice) -> Unit,
    private val onServicesDiscovered: (List<BluetoothGattService>) -> Unit
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun discoverServices() {
        gatt?.discoverServices()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic){
        val result = gatt?.readCharacteristic(characteristic)

        Log.d(
            "BleConnector",
            "READ REQUEST uuid=${characteristic.uuid} result=$result"
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            Log.d(
                "BLE",
                "status=$status newState=$newState"
            )
            when {
                status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED -> {
                    this@BleConnector.gatt = gatt
                    onConnected(gatt.device)

                    discoverServices()
                }
                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    gatt.close()
                    onDisconnected(gatt.device)
                }
            }
        }
        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return
            }
            onServicesDiscovered(gatt.services)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d("error", "error occurred.")
                return
            }

            Log.d("BleConnector", "READ ${characteristic.uuid}")


            Log.d("BleConnector", "READ OLD CALLBACK status=$status")
            val value = characteristic.value
            Log.d(
                "BLE",
                value.joinToString(" ") {
                    "%02X".format(it)
                }
            )
            val battery = value[0].toInt() and 0xFF

            Log.d(
                "BLE",
                "Battery = $battery%"
            )
        }
    }
}