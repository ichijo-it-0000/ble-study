package com.example.bleconnector

fun rssiToLevel(rssi: Int): Int {
    return when {
        rssi >= -50 -> 5
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        else -> 1
    }
}

fun signalBar(level: Int): String {
    return when(level) {
        5 -> "█████"
        4 -> "████░"
        3 -> "███░░"
        2 -> "██░░░"
        else -> "█░░░░"
    }
}