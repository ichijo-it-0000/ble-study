package com.example.blescanner

data class BLEDevice(
    val name: String,
    val address: String,
    val rssi: Int
)