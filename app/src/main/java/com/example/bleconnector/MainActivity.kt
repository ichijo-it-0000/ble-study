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
import android.os.Bundle // Activityが起動・復元されるときの情報の入れ物
import androidx.activity.ComponentActivity // Androidアプリの画面そのものを作るクラス

// Composeとは、一言でいうとXMLの代わりに、Kotlinだけで画面を作る仕組みのこと。
import androidx.activity.compose.setContent // Compose UIを表示する入口
import com.example.bleconnector.ui.BLEConnectScreen

class MainActivity : ComponentActivity() {

    private lateinit var bleScanner: BLEScanner
    private lateinit var bleConnector: BLEConnector
    private val deviceManager = DeviceManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager =
            getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bleScanner = BLEScanner(
            bluetoothManager.adapter.bluetoothLeScanner
        )
        bleConnector = BLEConnector(
            context = this,
            deviceManager = deviceManager
        )

        // ここでCompose UIを開始して画面を構成する。
        setContent {
            BLEConnectScreen(
                scanner = bleScanner,
                connector = bleConnector,
                deviceManager = deviceManager
            )
        }
    }
}
