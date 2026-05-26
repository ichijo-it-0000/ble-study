package com.example.bleconnector

data class BLEDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val connectState: ConnectState
)