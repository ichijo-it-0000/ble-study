// =================================
// 参考になる公式ドキュメント一覧
// =================================
// 1. Android公式 BLE概要
//    - https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview?hl=ja
// 2. Bluetooth の権限
//    - https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
// 3. BLE デバイスを探す
//    - https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices?hl=ja
// =================================

package com.example.blescanner

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle // Activityが起動・復元されるときの情報の入れ物
import androidx.activity.ComponentActivity // Androidアプリの画面そのものを作るクラス

// Composeとは、一言でいうとXMLの代わりに、Kotlinだけで画面を作る仕組みのこと。
import androidx.activity.compose.setContent // Compose UIを表示する入口
import androidx.annotation.RequiresPermission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat

data class BLEDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

class MainActivity : ComponentActivity() {
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanBLEDevices(onResult: (BLEDevice) -> Unit) {
        scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                val device = result?.device ?: return
                val bleDevice = BLEDevice(
                    name = device.name ?: "Unknown",
                    address = device.address,
                    rssi = result.rssi,
                )
                onResult(bleDevice)
            }
        }
        // Scanの開始
        bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScanBLEDevices() {
        // Permission check.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            val hasLocationPermission =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission || !hasLocationPermission) {
                return
            }
        }
        // callback存在時にScanを止めるように要求する。
        scanCallback?.let {
            bluetoothLeScanner?.stopScan(it)
        }
        scanCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // getSystemService()でAndroid OSが提供しているAPIを取得
        //   -  Bluetooth接続するためにBluetoothManagerを取得する
        val bluetoothManager =
            getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

        // Manager -> Adapter 経由で bluetoothLeScanner クラスを取得
        // > このクラスは、Bluetooth LE デバイスのスキャン関連の操作を実行するためのメソッドを提供します。
        // > アプリケーションは、特定の種類の Bluetooth LE デバイスをスキャンすることができます。
        // > また、結果を返すために異なる種類のコールバックを要求することもできます。
        bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner

        // ここでCompose UIを開始して画面を構成する
        setContent {
            BLESCanScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BLESCanScreen() {
    // remember: 再描画されても値を消さないための仕組み
    // mutableStateOf: 値が変わると自動で画面が再描画される
    var isScanning by remember { mutableStateOf(false) }
    val devices = remember { mutableStateListOf<BLEDevice>() }
    val activity = LocalContext.current as MainActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Scanner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.DarkGray,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        Column(
            // 見た目・配置を調整するための設定
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {

                        // ① 位置情報（全OS共通でチェック）
                        val hasLocationPermission =
                            ActivityCompat.checkSelfPermission(
                                activity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                        // ② Android12以上だけBluetooth権限チェック
                        val hasBluetoothPermission =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                                val scan = ActivityCompat.checkSelfPermission(
                                    activity,
                                    Manifest.permission.BLUETOOTH_SCAN
                                ) == PackageManager.PERMISSION_GRANTED

                                val connect = ActivityCompat.checkSelfPermission(
                                    activity,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED

                                scan && connect

                            } else {
                                true
                            }

                        // ③ 足りなければ要求
                        if (!hasLocationPermission || !hasBluetoothPermission) {

                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            } else {
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            }

                            ActivityCompat.requestPermissions(
                                activity,
                                permissions,
                                100
                            )

                            return@Button
                        }

                        // ④ スキャン開始
                        devices.clear()

                        activity.startScanBLEDevices { device ->
                            activity.runOnUiThread {

                                val index = devices.indexOfFirst {
                                    it.address == device.address
                                }

                                if (index == -1) {
                                    devices.add(device)
                                } else {
                                    devices[index] = device
                                }
                            }
                        }

                        isScanning = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan Start")
                }
                Button(
                    onClick = {
                        // スキャン停止
                        activity.stopScanBLEDevices()
                        isScanning = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan Stop")
                }
            }
            Column {
                Text(
                    text = if (isScanning) "Scanning..." else "Stopped",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("---------- Devices ----------")
                LazyColumn {
                    items(devices) { device ->
                        Text(
                            text = "Name: ${device.name}\n" + "MAC: ${device.address}\n" + "RSSI: ${device.rssi}"
                        )
                        Text("----------------------------")
                    }
                }
            }
        }
    }
}
