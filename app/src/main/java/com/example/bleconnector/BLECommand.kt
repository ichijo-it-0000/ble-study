package com.example.bleconnector

enum class BLECommand(val code: Byte) {
    ON(0x01),
    OFF(0x00)
}