package com.example.bleconnector

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

class NotifyStore {
    data class NotifyLog(
        val serviceUuid: String,
        val charUuid: String,
        val value: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NotifyLog

            if (serviceUuid != other.serviceUuid) return false
            if (charUuid != other.charUuid) return false
            if (!value.contentEquals(other.value)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = serviceUuid.hashCode()
            result = 31 * result + charUuid.hashCode()
            result = 31 * result + value.contentHashCode()
            return result
        }
    }

    val latestMap = mutableStateMapOf<String, NotifyLog>()
    val history = mutableStateListOf<NotifyLog>()

    fun add(serviceUuid: String, charUuid: String, value: ByteArray) {
        val log = NotifyLog(serviceUuid, charUuid, value)

        latestMap[charUuid] = log
        history.add(log)

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