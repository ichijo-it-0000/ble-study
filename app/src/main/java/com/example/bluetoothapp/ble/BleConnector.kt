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
import android.bluetooth.BluetoothGattDescriptor
import java.util.UUID

class BleConnector(
    private val context: Context,
    private val onConnected: (BluetoothDevice) -> Unit,
    private val onDisconnected: (BluetoothDevice) -> Unit,
    private val onServicesDiscovered: (List<BluetoothGattService>) -> Unit
) {
    private var gatt: BluetoothGatt? = null
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val BATTERY_UUID = "00002a19-0000-1000-8000-00805f9b34fb"

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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableNotify(characteristic: BluetoothGattCharacteristic) {

        val gatt = this.gatt ?: return

        val result = gatt.setCharacteristicNotification(
            characteristic,
            true
            )

        Log.d(
            "BLE_NOTIFY",
            "setCharacteristicNotification=$result"
        )

        val descriptor = characteristic.getDescriptor(CCCD_UUID)

        if (descriptor == null) {
            Log.e("BLE_NOTIFY", "CCCD not found")
            return
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
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
            val prop = characteristic.properties
            Log.d("BLE_PROP","$prop")
            val battery = value[0].toInt() and 0xFF

            Log.d(
                "BLE",
                "Battery = $battery%"
            )
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.d(
                "BLE_NOTIFY",
                "NOTIFY ${characteristic.uuid}"
            )

            characteristic.value?.let { value ->
                Log.d(
                    "BLE_NOTIFY",
                    value.joinToString(" ") {
                        "%02X".format(it)
                    }
                )
                if (characteristic.uuid.toString().equals(BATTERY_UUID, ignoreCase = true)) {
                    BatteryMonitor.update(value)
                }
            }

            NotifyLogManager.addLog(
                uuid = characteristic.uuid.toString(),
                value = characteristic.value
            )
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d(
                "BLE_NOTIFY",
                "Descriptor Write status=$status"
            )
        }
    }
}