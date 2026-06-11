// ■ 通信接続（GATT通信）
// - セントラル機器(親機)はスキャンをして周囲のアドバタイズを受信することで、
//   周囲にどんなペリフェラル機器(子機)が居るかを認識する。
// - セントラル機器は見つけたペリフェラル機器の中から１対１の通信接続をしたい相手を選び、
//   接続要求を送信することが出来る。
// - ペリフェラル機器はアドバタイズを発信した後、
//   少しの時間、自分に対する接続要求が飛んでこないか待っている。
//   そのタイミングで接続要求を受信するとアドバタイズをやめて１対１の接続通信に切り替える。
//   この１対１の接続通信のことを、GATT通信と呼ぶ。GATTはGeneric attribute profileの略。
// - GATT通信ではServiceとCharacteristicという概念でデータのやり取りをする。
//     - Characteristicはペリフェラル機器がセントラル機器に公開して共有するデータ構造の意味を持つ。
//       ペリフェラル機器とセントラル機器はこのCharacteristicを介してデータのやり取りを行う。
//     - ServiceはCharacteristicを機能単位で一括りにしたラベルのようなもの。

// ■ BLE接続フロー (Android)
// 1. Advertising受信（Scan）
// 2. ScanResult -> BluetoothDevice取得
// 3. GATT Client接続確立（connectGatt）
// 4. GATT Connection State Callback受信
// 5. GATT Service Discovery（discoverServices）
// 6. GATT Characteristic Read / Write / Notification

// GATTはクライアント/サーバモデル
// - Android = GATT Client
// - デバイス = GATT Server
// 全て非同期
// - connect / discover / read / write = 全部非同期
// Callbackが中心設計
// - 結果はすべてBluetoothGattCallbackで受ける

// 参考URL一覧
// - 【サルでもわかるBLE入門】（２） アドバタイズとGATT通信
//   - https://www.musen-connect.co.jp/blog/course/trial-production/ble-beginner-2/
// - Android BLE (GATT接続) 公式ドキュメント
//   - https://developer.android.com/develop/connectivity/bluetooth/ble/connect-gatt-server
// - Android BluetoothGatt (接続・通信コア)
//   - https://developer.android.com/reference/android/bluetooth/BluetoothGatt
// - BluetoothGattCallback
//   - https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback
// - Android BluetoothLeScanner
//   - https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner
// - BluetoothDevice
//   - https://developer.android.com/reference/android/bluetooth/BluetoothDevice

package com.example.bleconnector

// Composeとは、一言でいうとXMLの代わりに、Kotlinだけで画面を作る仕組みのこと。

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bleconnector.ui.BLEConnectScreen
import com.example.bleconnector.ui.BLEDeviceDetailScreen

class MainActivity : ComponentActivity() {
    // BLEServiceはServiceConnection経由で非同期に初期化されるため、setContent実行時点ではnullとなる。
    // そのため、UIはStateの変化を購読し再構成する必要がある。
    private val bleServiceState = mutableStateOf<BLEService?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BLEService.LocalBinder
            bleServiceState.value = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleServiceState.value = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindService(
            Intent(this, BLEService::class.java),
            connection,
            BIND_AUTO_CREATE
        )

        // ここでCompose UIを開始して画面を構成する。
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "scan"
            ){
                composable("scan") {
                    bleServiceState.value?.let { service ->
                        BLEConnectScreen(
                            scanner = service.scanner,
                            connector = service.connector,
                            deviceManager = service.deviceManager,
                            navController = navController
                        )
                    }
                }
                composable("detail/{address}") { backStackEntry ->
                    val address = backStackEntry.arguments?.getString("address")!!
                    bleServiceState.value?.let { service ->
                        BLEDeviceDetailScreen(
                            address = address,
                            deviceManager = service.deviceManager,
                            connector = service.connector,
                            notifyStore = service.notifyStore,
                            writeStore = service.writeStore,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}
