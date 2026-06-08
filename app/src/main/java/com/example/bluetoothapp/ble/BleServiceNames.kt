package com.example.bluetoothapp.ble

import android.bluetooth.BluetoothGattCharacteristic

data class ServiceInfo(
    val name: String,
    val uuid: String,
    val characteristics: List<CharacteristicInfo>
)

data class CharacteristicInfo(
    val name: String,
    val uuid: String,
    val properties: Int,
    val characteristic: BluetoothGattCharacteristic
)

object BleServiceNames {

    private val services = mapOf(
        "1800" to "Generic Access",
        "1801" to "Generic Attribute",
        "180a" to "Device Information",
        "180f" to "Battery Service",
        "fe2c" to "Google LLC"
    )

    fun getName(uuid: String): String {
        val shortUuid = uuid.substring(4, 8)
        return services[shortUuid] ?: "Unknown Service"
    }
}

object BleCharacteristicNames {

    private val characteristics = mapOf(
        "2a00" to "Device Name",
        "2a01" to "Appearance",
        "2a04" to "Peripheral Preferred Connection Parameters",
        "2a05" to "Service Changed",
        "2a19" to "Battery Level",
        "2a23" to "System ID",
        "2a24" to "Model Number String",
        "2a25" to "Serial Number String",
        "2a26" to "Firmware Revision String",
        "2a27" to "Hardware Revision String",
        "2a28" to "Software Revision String",
        "2a29" to "Manufacturer Name String",
        "2a2a" to "IEEE 11073-20601 Regulatory Certification Data List",
        "2a50" to "PnP ID",
        "2aa6" to "Central Address Resolution",
        "2b29" to "Client Supported Features",
        "2b2a" to "Database Hash",
        "2b3a" to "Enhanced Blood Pressure Measurement",
    )

    fun getName(uuid: String): String {
        val shortUuid = uuid.substring(4, 8)
        return characteristics[shortUuid] ?: "Unknown Characteristic"
    }
}