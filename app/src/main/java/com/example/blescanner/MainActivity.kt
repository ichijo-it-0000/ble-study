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
import android.os.Bundle // Activityが起動・復元されるときの情報の入れ物
import androidx.activity.ComponentActivity // Androidアプリの画面そのものを作るクラス

// Composeとは、一言でいうとXMLの代わりに、Kotlinだけで画面を作る仕組みのこと。
import androidx.activity.compose.setContent // Compose UIを表示する入口
import com.example.blescanner.ui.BLESCanScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager =
            getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bleScanner = BLEScanner(
            bluetoothManager.adapter.bluetoothLeScanner
        )

        // ここでCompose UIを開始して画面を構成する。
        setContent {
            BLESCanScreen(bleScanner)
        }
    }
}
