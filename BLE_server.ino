/*
   Create a BLE server that will send periodic iBeacon frames.
   The design of creating the BLE server is:
   1. Create a BLE Server
   2. Create advertising data
   3. Start advertising.
   4. wait
   5. Stop advertising.
*/

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <BLEBeacon.h>

#include <M5Stack.h>

// UUIDには決まっているもの(意味が標準で定められているもの)があるらしい。
// 例えば、Device名は0x2A00など
// * https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf

#define DEVICE_NAME            "M5Stack"
#define SERVICE_UUID           "3623fd47-f9b8-4504-a7ce-3e598382257b"
#define NOTIFY_CHARACTERISTIC_UUID    "6679c3f5-301c-434b-bdbe-eac49e25adf9"
#define WRITE_CHARACTERISTIC_UUID   "e62715f9-e942-4266-2cb9-50f2b6f8cd0a"

BLEServer *pServer;
BLECharacteristic *pNotifyCharacteristic;
BLECharacteristic *pWriteCharacteristic;
bool deviceConnected = false;
uint8_t value = 0;

void sendAck(const String& msg) {
  if (!deviceConnected) return;

  pNotifyCharacteristic->setValue(msg.c_str());
  pNotifyCharacteristic->notify();
}

// Callbacks associated with the operation of a BLE server.
// * https://lang-ship.com/reference/unofficial/M5StickC/Class/ESP32/BLEServerCallbacks/
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("deviceConnected = true");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("deviceConnected = false");

      // Restart advertising to be visible and connectable again
      // BLE Peripheralは接続成立後、Advertisingを停止する。
      // そのままだとScan結果に表示されなくなり、新規接続や再接続ができなくなるため、切断時にAdvertisingを再開する必要がある。
      BLEAdvertising* pAdvertising;
      pAdvertising = pServer->getAdvertising();
      pAdvertising->start();
      Serial.println("Advertising restarted");
    }
};


// Callbacks that can be associated with a BLE characteristic to inform of events.
// * https://lang-ship.com/reference/unofficial/M5StickC/Class/ESP32/BLECharacteristicCallbacks/
class MyCallbacks: public BLECharacteristicCallbacks {
    // CentralがCharacteristicにWrite
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();

      if (rxValue.length() > 0) {
        Serial.println("*********");
        Serial.print("Received Value: ");
        for (int i = 0; i < rxValue.length(); i++) {
          Serial.print(rxValue[i]);
        }
        Serial.println();
        Serial.println("*********");

      }
    }
};

class WriteCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) override {
    std::string rxValue = pCharacteristic->getValue();

    String cmd = String(rxValue.c_str());

    Serial.println("=== WRITE RECEIVED ===");
    Serial.println(cmd);
    if (cmd == "RED") {
      Serial.println("STATE: RED MODE");
      M5.Lcd.fillScreen(RED);

      sendAck("OK:RED");

    } else if (cmd == "BLACK") {
      Serial.println("STATE: NORMAL MODE");
      M5.Lcd.fillScreen(BLACK);
      sendAck("OK:BLACK");
    } else {
      Serial.println("STATE: UNKNOWN");
      sendAck("ERR:UNKNOWN_CMD");
    }
  }
};

void init_service() {
  // BLEServerが持っているAdvertisingを取得。
  BLEAdvertising* pAdvertising;
  pAdvertising = pServer->getAdvertising();

  // Advertisingは裏で動いている可能性があり、設定変更中に電波出ると不整合になるため停止させる。
  pAdvertising->stop();

  // Create the BLE Service
  // BLEServerのざっくり構造
  // ├ GATT（Service/Characteristic管理）
  // ├ Advertising（電波で公開する仕組み）
  // └ 接続管理

  // GATTざっくり理解
  // * Service = アプリの機能カテゴリ
  //   * e.g. バッテリーサービス
  // * Characteristic = 具体的な機能
  //   * e.g. バッテリー残量
  // * Descriptor = その機能の説明・設定
  //   * e.g. 値の意味定義(%)
  // 今回は標準UUIDは使わず、Service/Characteristicの識別子としてカスタム128bit UUIDを定義している。
  BLEService *pService = pServer->createService(BLEUUID(SERVICE_UUID));

  // Create a BLE Characteristic
  // READ / WRITE / NOTIFY がすべて有効なCharacteristicを作る
  pNotifyCharacteristic = pService->createCharacteristic(
                      NOTIFY_CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  
  pNotifyCharacteristic->setCallbacks(new MyCallbacks());

  pWriteCharacteristic = pService->createCharacteristic(
                      WRITE_CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );

  pWriteCharacteristic->setCallbacks(new WriteCallbacks());

  // 0x2902: Client Characteristic Configuration Descriptor
  // このCharacteristicの通知を受け取るかどうかをクライアントが制御するスイッチ
  // * https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf
  pNotifyCharacteristic->addDescriptor(new BLE2902());
  pWriteCharacteristic->addDescriptor(new BLE2902());
  // Advertisingに Service UUID を含める
  pAdvertising->addServiceUUID(BLEUUID(SERVICE_UUID));

  // Start the service
  pService->start();

  pAdvertising->start();
}


// 基本的な書き方はc++と同じ(コンパイル前に変換されるよう)だが、main()は実装しない代わりに以下の2つの関数がマストらしい。
// setup() ... 最初に一度だけ実行される
// loop() ... main loop。
// なんかUUIDをランダム生成してくれるサイトとかもあるらしい。
// * https://uuidgenerator.dev/ja


void setup() {
  // `Serial`は、Arduino coreに最初から用意されているグローバルオブジェクト。
  // そのため、明示的にincludeしなくても使えるとのこと。
  // USBシリアル通信はPCとM5Stackをつなぐデバッグ通信用
  // * https://docs.arduino.cc/language-reference/en/functions/communication/serial/
  // * https://github.com/espressif/arduino-esp32
  Serial.begin(115200); // opens serial port, sets data rate to 115200 bps. (115200 bps は今の標準ボーレートらしい。)
  Serial.println(); // これでPCに標準出力させられるとのこと。
  Serial.println("Initializing...");
  Serial.flush(); // 送信が全部終わるまで待機するコマンド

  // =============================================
  // 1. Create a BLE Server
  // =============================================
  // Arduino ESP32 BLE ライブラリ
  // APIドキュメントなるものはなく、実装やExamples、READMEを確認する必要があった。
  // * https://espressif-docs.readthedocs-hosted.com/projects/arduino-esp32/en/latest/api/ble.html
  // Arduino BLE ライブラリ(https://docs.arduino.cc/libraries/arduinoble/)はnRF52用らしく、ESP32では使用できないとのこと。
  // ESP32のBluetoothを動かしているエンジン(Bluetoothスタック)がBluedroid / NimBLEというものらしく、互換性がないらしい。
  // NimBLE-Arduinoライブラリも使えるらしいが、ここでは元々のサンプルを踏襲して、Arduino ESP32 BLE ライブラリを使用する。

  BLEDevice::init(DEVICE_NAME); // BLEスタック初期化。デバイス名の設定。
  pServer = BLEDevice::createServer();  // Create a new instance of a GATT server.
  pServer->setCallbacks(new MyServerCallbacks()); // Callback登録。

  M5.begin();

  init_service(); // Service初期化。

  Serial.println("Service defined and advertising!");
}

void loop() {
  if (deviceConnected) {
    Serial.printf("*** NOTIFY: %d ***\n", value);
    pNotifyCharacteristic->setValue(&value, 1);
    pNotifyCharacteristic->notify();
    value++;
  }
  delay(2000);
}
