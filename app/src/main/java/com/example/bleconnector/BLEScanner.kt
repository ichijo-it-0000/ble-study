package com.example.bleconnector

import android.Manifest
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlin.collections.emptyList

class BLEScanner(private val bluetoothLeScanner: BluetoothLeScanner) {
    private var scanCallback: ScanCallback? = null
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanBLEDevices(
        filterState: ScanFilterState,
        onResult: (BLEDevice) -> Unit
    ) {
        scanCallback?.let {
            bluetoothLeScanner.stopScan(it)
        }

        scanCallback = null

        val settings =
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        val filters =
            if (filterState.text.isBlank()) {
                emptyList()
            } else {
                when (filterState.type) {
                    FilterType.DEVICE_NAME -> {
                        listOf(
                            ScanFilter.Builder()
                                .setDeviceName(filterState.text)
                                .build()
                        )
                    }
                    FilterType.MAC_ADDRESS -> {
                        listOf(
                            ScanFilter.Builder()
                                .setDeviceAddress(filterState.text)
                                .build()
                        )
                    }
                }
            }

        scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                val device = result?.device ?: return
                val bleDevice = BLEDevice(
                    name = device.name ?: "Unknown",
                    address = device.address,
                    rssi = result.rssi,
                    connectState = ConnectState.DISCONNECTED
                )

                onResult(bleDevice)
            }
        }

        bluetoothLeScanner.startScan(
            filters,
            settings,
            scanCallback
        )
    }

    fun stopScanBLEDevices(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        scanCallback?.let {
            bluetoothLeScanner.stopScan(it)
        }

        scanCallback = null
    }
}