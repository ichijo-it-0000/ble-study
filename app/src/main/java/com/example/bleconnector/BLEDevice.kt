package com.example.bleconnector

data class BLEDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val connectState: ConnectState,
    val services: List<BLEServiceInfo> = emptyList()
)

data class BLEServiceInfo(
    val uuid: String,
    val characteristics: List<BLECharacteristicInfo>
)

data class BLECharacteristicInfo(
    val uuid: String,
    val properties: Int,
    val descriptors: List<String>
)