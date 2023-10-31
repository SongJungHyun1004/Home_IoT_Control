#include <queue>

#define sendpin D6
#define readpin D7
#define readmode LOW
#define sendmode HIGH
#define plen 21
#define light1 D3
#define light2 D4

std::queue<byte*> dataQueue;

void printPacket(byte* packetData) {
  // 패킷 출력 함수
  for (int i = 0; i < plen; i++) {
    if (packetData[i] < 0x10) {
      Serial.print("0");
    }
    Serial.print(packetData[i], HEX);
  }
  Serial.println();
}

byte makeChecksum(byte* packetData) {
  // checksum 생성 함수
  int sum_buf = 0;
  for (int i = 0; i < 16; i++) {
    int sum = packetData[i];
    sum_buf += sum;
  }
  int chksum = (sum_buf % 256);

  return (byte) chksum;
}

void lightState(byte* packetData) {
  // 전구 상태 조회 함수
  byte* sendData = new byte[plen];
  memcpy(sendData, packetData, plen);
  sendData[2] = 0x30;
  sendData[3] = 0xDC;
  if (digitalRead(light1) == HIGH) {
    sendData[10] = 0xFF;
  }
  else if (digitalRead(light1) == LOW) {
    sendData[10] = 0x00;
  }
  if (digitalRead(light2) == HIGH) {
    sendData[11] = 0xFF;
  }
  else if (digitalRead(light2) == LOW) {
    sendData[11] = 0x00;
  }
  sendData[18] = makeChecksum(sendData);
  printPacket(sendData);
  delete[] sendData;
}

void lightControl(byte* packetData) {
  // 전구 제어 함수
  byte* sendData = new byte[plen];
  memcpy(sendData, packetData, plen);
  sendData[2] = 0x30;
  sendData[3] = 0xDC;
  if (sendData[10] == 0xFF) {
    digitalWrite(light1, HIGH);
  }
  else if (sendData[10] == 0x00) {
    digitalWrite(light1, LOW);
  }
  if (sendData[11] == 0xFF) {
    digitalWrite(light2, HIGH);
  }
  else if (sendData[11] == 0x00) {
    digitalWrite(light2, LOW);
  }
  sendData[18] = makeChecksum(sendData);
  printPacket(sendData);
  delete[] sendData;
}

bool checkPacket1(byte* packetData) {
  // 1차 전구 패킷 확인 함수
  if (packetData[0] == 0xAA && packetData[1] == 0x55 && packetData[4] == 0x00 && packetData[5] == 0x0E) {
    return true;
  }
  else {
    return false;
  }
}

void checkPacket2(byte* packetData) {
  // 2차 전구 패킷 확인 함수
  if (packetData[0] == 0xAA && packetData[1] == 0x55) {
    if (packetData[8] == 0x00 && packetData[9] == 0x3A) {
      byte* queueData = new byte[plen];
      memcpy(queueData, packetData, plen);
      dataQueue.push(queueData);
    }
    else if (packetData[8] == 0x00 && packetData[9] == 0x00) {
      byte* queueData = new byte[plen];
      memcpy(queueData, packetData, plen);
      dataQueue.push(queueData);
    }
  }
}

void checkPacket3(byte* packetData) {
  // 3차 전구 패킷 확인 함수
  if (packetData[0] == 0xAA && packetData[1] == 0x55) {
    if (packetData[8] == 0x00 && packetData[9] == 0x3A) {
      lightState(packetData);
    }
    else if (packetData[8] == 0x00 && packetData[9] == 0x00) {
      lightControl(packetData);
    }
  }
}

void setup() {
  Serial.begin(9600);
  pinMode(readpin, OUTPUT);
  pinMode(sendpin, OUTPUT);
  digitalWrite(readpin, readmode);
  digitalWrite(sendpin, sendmode);
  pinMode(light1, OUTPUT);
  pinMode(light2, OUTPUT);
}

void loop() {
  if (!dataQueue.empty()) {
    byte* queueData = dataQueue.front();
    dataQueue.pop();
    checkPacket3(queueData);
    delete[] queueData;
  }
}

void serialEvent() {
  // 시리얼 통신 이벤트 대기 함수
  while (Serial.available()) {
    byte* receiveData = new byte[plen];
    Serial.readBytes(receiveData, plen);
    if (checkPacket1(receiveData)) { 
      checkPacket2(receiveData);
    }
    delete[] receiveData;
  }
}