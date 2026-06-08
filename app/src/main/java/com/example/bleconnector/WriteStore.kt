package com.example.bleconnector

import androidx.compose.runtime.mutableStateMapOf

class WriteStore {
    data class WriteLog(
        val charUuid: String,
        val input: String,
        val result: String,
        val code: Int?
    )

    private val latestMap = mutableStateMapOf<String, WriteLog>()
    private val historyMap = mutableStateMapOf<String, MutableList<WriteLog>>()
    private val maxSize = 100

    fun history(charUuid: String): List<WriteLog> {
        return historyMap[charUuid] ?: emptyList()
    }

    fun success(charUuid: String, input: String, code: Int) {
        add(charUuid, input, "SUCCESS", code)
    }
    fun failed(charUuid: String, input: String, code: Int) {
        add(charUuid, input, "FAILED", code)
    }
    fun unknown(charUuid: String, input: String) {
        add(charUuid, input, "UNKNOWN", null)
    }

    private fun add(
        charUuid: String,
        input: String,
        result: String,
        code: Int?
    ) {
        val log = WriteLog(
            charUuid = charUuid,
            input = input,
            result = result,
            code = code
        )

        // latest更新
        latestMap[charUuid] = log

        val current = historyMap[charUuid].orEmpty()

        val updated =
            (current + log)
                .takeLast(maxSize)
                .toMutableList()

        historyMap[charUuid] = updated
    }
}