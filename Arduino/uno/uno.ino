void printPacket(byte* packetData, int n) {
  // 패킷 출력 함수
  for (int i = 0; i < n; i++) {
    Serial.write(packetData[i]);
  }
  Serial.println();
}

void checkLock(byte* inputData) {
  // 도어락 잠금 함수
  if (inputData[1] == 0x4F && inputData[2] == 0x43 && inputData[3] == 0x4B) {
    pinMode(8, OUTPUT);
    digitalWrite(8, LOW);
    printPacket(inputData, 4);
    delay(500);
    pinMode(8, INPUT);
  }
  else {
    Serial.println("Invalid Command.");
  }
}

void checkUnlock(byte* inputData) {
  // 도어락 잠금 해제 함수
  if (inputData[1] == 0x4E && inputData[2] == 0x4C && inputData[3] == 0x4F && inputData[4] == 0x43 && inputData[5] == 0x4B) {
    pinMode(8, OUTPUT);
    digitalWrite(8, LOW);
    printPacket(inputData, 6);
    delay(500);
    pinMode(8, INPUT);
  }
  else {
    Serial.println("Invalid Command.");
  }
}

void checkInput(byte* inputData) {
  // 도어락 제어 패킷 확인 함수
  if (inputData[0] == 0x4C) {
    checkLock(inputData);
  }
  else if (inputData[0] == 0x55) {
    checkUnlock(inputData);
  }
  else {
    Serial.println("Invalid Command.");
  }
}

void setup() {
  Serial.begin(9600);
  pinMode(8, INPUT);
}

void loop() {
  if (Serial.available()) {
    byte* receiveData = new byte[6];
    Serial.readBytes(receiveData, 6);
    checkInput(receiveData);
  }
}
