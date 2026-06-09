package com.example.bleconnector

enum class UuidType {
    STANDARD,
    CUSTOM
}

data class UuidInfo(
    val type: UuidType,
    val name: String
)

object UuidRegistry {
    private val map = mapOf(
        // GAP
        "00001800-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Generic Access"
        ),
        "00001801-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Generic Attribute"
        ),
        "0000180a-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Device Information"
        ),
        "0000180f-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Battery Service"
        ),
        "0000180d-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Heart Rate"
        ),
        "00001812-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Human Interface Device"
        ),
        "00001805-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Current Time Service"
        ),
        "00001809-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Health Thermometer"
        ),
        "0000181a-0000-1000-8000-00805f9b34fb" to UuidInfo(
            UuidType.STANDARD,
            "Environmental Sensing"
        )
    )

    fun resolve(uuid: String): UuidInfo {
        return map[uuid.lowercase()]
            ?: UuidInfo(
                UuidType.CUSTOM,
                "Custom"
            )
    }
}