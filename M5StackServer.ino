#include <BLEBeacon.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <M5Stack.h>


#define DEVICE_NAME "M5Stack"
/*
charaのUUIDは、識別しやすいようにXXXX部分の値をインクリメントして使う
2993XXXX-8d58-42d2-9687-2666fa76081a
今回はXXXX=f0a0から始めとする。
*/
#define SERVICE_UUID "12c5ce2a-8d58-4aac-9687-2666fa76081a"
#define WRITE_CHARACTERISTIC_UUID "2993f0a0-8d58-42d2-9687-2666fa76081a"
#define NOTIFY_CHARACTERISTIC_UUID "2993f0a1-8d58-42d2-9687-2666fa76081a"

BLECharacteristic *pNotifyCharacteristic;
BLE2902 *pBLE2902_notify;
BLECharacteristic *pWriteCharacteristic;
BLE2902 *pBLE2902_write;

bool deviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
  //BLEサーバーのイベントコールバック
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      M5.Lcd.println("Central Device connected.");

      BLEDevice::stopAdvertising();
      M5.Lcd.println("Advertising Stopped.");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      M5.Lcd.println("Central Device Disconnected.");

      BLEDevice::startAdvertising();
      M5.Lcd.println("Advertising started.");
    }
};


class NotifyCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    // 引数のcharaに何かしらデータが書き込まれる。
    std::string wrote_data = pCharacteristic->getValue();

    if (wrote_data.empty()) return;

    M5.Lcd.printf("Write Callback: data recieved. \n");
    M5.Lcd.printf("Received Value: %s.\n" ,wrote_data.c_str());

    //セントラル側にデータを送信
    if(wrote_data == "on" || wrote_data == "ON"){
      pWriteCharacteristic->setValue("Device is ON.");
      M5.Lcd.printf("Sending notify\n");
      //ACKの通知
      uint8_t ack = 0x01;
      pCharacteristic->setValue(&ack, 1);
      pCharacteristic->notify();
    }
    else if (wrote_data == "off" || wrote_data == "OFF") {
      pWriteCharacteristic->setValue("Device is OFF.");

      uint8_t ack = 0x01;
      pNotifyCharacteristic->setValue(&ack, 1);
      pNotifyCharacteristic->notify();
    }
    else{
      pWriteCharacteristic->setValue("Plase send correct data.");
      uint8_t ack = 0x00;
      pCharacteristic->setValue(&ack, 1);
      pCharacteristic->notify();
    }
  }
};


BLEServer* createServer() {
    BLEServer* pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());
    return pServer;
}


BLEService* createService(BLEServer *pServer) {
  //Serviceの生成
  BLEService *pService = pServer->createService(BLEUUID(SERVICE_UUID));
  // Characteristicの生成
  pWriteCharacteristic = pService->createCharacteristic(
                      WRITE_CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ
                    );

  pNotifyCharacteristic = pService->createCharacteristic(
                      NOTIFY_CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pNotifyCharacteristic->setCallbacks(new NotifyCallbacks());
  pBLE2902_notify = new BLE2902();
  pNotifyCharacteristic->addDescriptor(pBLE2902_notify);
  
  return pService;
}


void startAdvertising(){
  BLEAdvertising* advertising = BLEDevice::getAdvertising();

  advertising->addServiceUUID(BLEUUID(SERVICE_UUID));
  advertising->start();

  Serial.println("Service defined and advertising!");
}


void setup() {
  M5.begin();
  Serial.begin(115200);  // PC側で誤差なく正確に出せる標準規格とのこと。

  Serial.println("Starting BLE Server with M5Stack...");

  //BLEデバイスの初期化
  BLEDevice::init(DEVICE_NAME); // BLEデバイスを初期化し、アドバタイズ名を設定
  // GATTサーバーの作成
  BLEServer *pServer = createServer();
  // サービスの作成
  BLEService *pService = createService(pServer);

  // サービス開始
  pService->start(); 
  // advertising開始
  startAdvertising();
}

void loop() {
  delay(100);
}
