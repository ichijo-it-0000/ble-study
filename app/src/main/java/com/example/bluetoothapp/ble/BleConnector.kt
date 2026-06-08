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
import android.os.Build
import java.util.UUID
import android.os.Handler
import android.os.Looper

class BleConnector(
    private val context: Context,
    private val onConnected: (BluetoothDevice) -> Unit,
    private val onDisconnected: (BluetoothDevice) -> Unit,
    private val onServicesDiscovered: (List<BluetoothGattService>) -> Unit
) {
    private var gatt: BluetoothGatt? = null
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val BATTERY_UUID = "00002a19-0000-1000-8000-00805f9b34fb"

    private val WRITE_UUID = UUID.fromString("2993f0a0-8d58-42d2-9687-2666fa76081a")
    private val NOTIFY_UUID = UUID.fromString("2993f0a1-8d58-42d2-9687-2666fa76081a")

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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        val gatt = this.gatt ?: return

        Log.d(
            "BLE_WRITE",
            "WRITE REQUEST\nUUID : ${characteristic.uuid}\nDATA : ${data.joinToString(" ") { "%02X".format(it) }}".trimIndent()
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = data

            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
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

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
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
            Log.d(
                "BLE",
                "UTF-8 : ${String(value)}"
            )
            val prop = characteristic.properties
            Log.d("BLE_PROP", "$prop")

            BleLogManager.add(LogType.READ, characteristic.uuid.toString(), value)
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
                // NOTIFY_UUIDから0x01を受け取ったら、WRITE_UUIDからREADする
                if (characteristic.uuid.toString().equals(NOTIFY_UUID.toString(), ignoreCase = true) && 
                    value.isNotEmpty() && value[0] == 0x01.toByte()) {
                    
                    val writeCharacteristic = gatt.services
                        .flatMap { it.characteristics }
                        .find { it.uuid == WRITE_UUID }
                    
                    if (writeCharacteristic != null) {
                        Log.d("BLE_NOTIFY", "0x01 received. Reading from WRITE_UUID...")
                        gatt.readCharacteristic(writeCharacteristic)
                    }
                }
                BleLogManager.add(LogType.NOTIFY, characteristic.uuid.toString(), value)
            }
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

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(
                "BLE_WRITE",
                "WRITE RESULT\n UUID : ${characteristic.uuid} \nSTATUS : $status\n".trimIndent()
            )
                // WRITE 成功時のログ記録
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BleLogManager.add(
                    LogType.WRITE,
                    characteristic.uuid.toString(),
                    characteristic.value ?: ByteArray(0)
                )

                BleLogManager.add(LogType.WRITE, characteristic.uuid.toString(), characteristic.value ?: ByteArray(0))

                // WRITE 後、自動的にそのキャラクタリスティックを READ
                val writeCharacteristic = gatt.services
                    .flatMap { it.characteristics }
                    .find { it.uuid == WRITE_UUID }
                if (writeCharacteristic != null) {
                    Log.d("BLE_NOTIFY", "0x01 received. Reading from WRITE_UUID...")
                    gatt.readCharacteristic(writeCharacteristic)
                }
            }
        }
    }
}