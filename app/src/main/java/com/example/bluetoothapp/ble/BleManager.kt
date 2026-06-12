package com.example.bluetoothapp.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.annotation.RequiresPermission
import android.util.Log
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

class BleManager(
    private val context: Context,
    bluetoothAdapter: BluetoothAdapter,
) {
    private var onDevicesUpdated: (() -> Unit)? = null
    private val registry = DeviceRegistry()
    private val scanner = BleScanner(
        bluetoothAdapter = bluetoothAdapter,
        onDeviceFound = { device ->
            registry.addOrUpdate(device)
            onDevicesUpdated?.invoke()
        }
    )

    private val connector = BleConnector(
        context = context,

        onConnected = { device ->
            Log.i("BleManager", "CONNECTED : ${device.address}")
            registry.setConnectionState(device.address, ConnectionState.DISCOVERING_SERVICES)
            onDevicesUpdated?.invoke()
        },

        onDisconnected = { device ->
            Log.i("BleManager", "DISCONNECTED : ${device.address}")
            registry.setConnectionState(device.address, ConnectionState.DISCONNECTED)
            onDevicesUpdated?.invoke()
        },
        onServicesDiscovered = ::handleServicesDiscovered
    )

    private var currentDeviceAddress: String? = null
    private val discoveredServices = mutableListOf<ServiceInfo>()
    fun getDiscoveredServices(): List<ServiceInfo> {
        return discoveredServices
    }

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
    fun enableNotify(characteristic: BluetoothGattCharacteristic) {
        connector.enableNotify(characteristic)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BleDevice) {
        currentDeviceAddress = device.address
        Log.i("BleManager", "CONNECT START : ${device.address}")

        registry.setConnectionState(device.address, ConnectionState.CONNECTING)
        onDevicesUpdated?.invoke()
        connector.connect(device.bluetoothDevice)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect(device: BleDevice) {
        Log.i("BleManager", "DISCONNECT START")
        Log.d(
            "BleManager",
            "disconnect request state=${device.connectionState}"
        )
        registry.setConnectionState(device.address, ConnectionState.DISCONNECTING)
        onDevicesUpdated?.invoke()
        connector.disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        connector.readCharacteristic(characteristic)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        connector.writeCharacteristic(characteristic, data)
    }

    private val BATTERY_LEVEL_UUID = java.util.UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleServicesDiscovered(services: List<BluetoothGattService>) {
        discoveredServices.clear()
        Log.d("BleManager", "SERVICE COUNT = ${services.size}")
        services.forEach { service ->
            val serviceFullUuid = service.uuid.toString()
            val serviceShortUuid = serviceFullUuid.substring(4, 8)
            val serviceName = BleServiceNames.getName(serviceFullUuid)
            val characteristicList = mutableListOf<CharacteristicInfo>()

            Log.d("BLE", "SERVICE : $serviceName ($serviceShortUuid)")

            service.characteristics.forEach { characteristic ->
                val charaFullUuid = characteristic.uuid.toString()
                val charaShortUuid = charaFullUuid.substring(4, 8)
                val charaName = BleCharacteristicNames.getName(charaFullUuid)
                Log.d("BLE", "$charaName ($charaShortUuid)")
                characteristicList.add(
                    CharacteristicInfo(
                        name = charaName,
                        uuid = charaFullUuid,
                        properties = characteristic.properties,
                        characteristic = characteristic
                    )
                )

                if (characteristic.uuid == BATTERY_LEVEL_UUID) {
                    connector.readCharacteristic(
                        characteristic
                    )
                }
            }
            discoveredServices.add(
                ServiceInfo(
                    name = serviceName,
                    uuid = serviceFullUuid,
                    characteristics = characteristicList
                )
            )
        }
        currentDeviceAddress?.let {
        registry.setConnectionState(it, ConnectionState.CONNECTED)
        onDevicesUpdated?.invoke()
        }
    }

    fun getScannerState(): Boolean = scanner.getScanningState()

    fun setOnDevicesUpdated( callback: () -> Unit) {
        onDevicesUpdated = callback
    }
}