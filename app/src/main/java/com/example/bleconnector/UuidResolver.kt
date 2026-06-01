package com.example.bleconnector

enum class UuidType {
    STANDARD,
    CUSTOM
}

data class UuidInfo(
    val type: UuidType,
    val name: String
)

fun resolveUuid(uuid: String): UuidInfo {

    return when (uuid.lowercase()) {

        "00001800-0000-1000-8000-00805f9b34fb" ->
            UuidInfo(
                UuidType.STANDARD,
                "Generic Access"
            )

        "00001801-0000-1000-8000-00805f9b34fb" ->
            UuidInfo(
                UuidType.STANDARD,
                "Generic Attribute"
            )

        "0000180a-0000-1000-8000-00805f9b34fb" ->
            UuidInfo(
                UuidType.STANDARD,
                "Device Information"
            )

        else ->
            UuidInfo(
                UuidType.CUSTOM,
                "Custom"
            )
    }
}