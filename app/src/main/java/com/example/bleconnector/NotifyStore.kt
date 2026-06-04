package com.example.bleconnector

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

class NotifyStore {
    val latest = mutableStateOf<Triple<String, String,ByteArray>?>(null)
    val history = mutableStateListOf<Triple<String, String,ByteArray>>()

    fun add(serviceUuid: String, charUuid: String, value: ByteArray) {
        val data = Triple(serviceUuid, charUuid, value)
        latest.value = data
        history.add(data)

        // 重くなりすぎないように上限を設定し、古いものから削除。
        if (history.size > 100) {
            history.removeAt(0)
        }
    }

    fun toHex(value: ByteArray): String {
        return value.joinToString(" ") {
            "%02X".format(it)
        }
    }
}
