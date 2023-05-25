#include <SoftwareSerial.h>

#define modepin 13
#define readmode LOW
#define sendmode HIGH
#define plen 21

SoftwareSerial rs485(2, 3); // RX, TX

void printPacket(byte* packetData) {
  for (int i = 0; i < plen; i++) {
    if (packetData[i] < 0x10) {
      Serial.print("0");
    }
    Serial.print(packetData[i], HEX);
  }
  Serial.println();
}

byte makeChecksum(byte* packetData) {
  int sum_buf = 0;
  for (int i = 0; i < 16; i++) {
    int sum = packetData[i];
    sum_buf += sum;
  }
  int chksum = (sum_buf % 256);

  return (byte) chksum;
}

void lightState(byte* packetData) {
  byte* send = new byte[plen];
  memcpy(send, packetData, plen);
  send[2] = 0x30;
  send[3] = 0xDC;
  if (digitalRead(4) == HIGH) {
    send[10] = 0xFF;
  }
  else if (digitalRead(4) == LOW) {
    send[10] = 0x00;
  }
  if (digitalRead(5) == HIGH) {
    send[11] = 0xFF;
  }
  else if (digitalRead(5) == LOW) {
    send[11] = 0x00;
  }
  send[18] = makeChecksum(send);
  Serial.print("Slave Data(send) : ");
  printPacket(send);
  digitalWrite(modepin, sendmode);
  rs485.write(send, plen);
  digitalWrite(modepin, readmode);
  delete[] send;
}

void lightControl(byte* packetData) {
  byte* send = new byte[plen];
  memcpy(send, packetData, plen);
  send[2] = 0x30;
  send[3] = 0xDC;
  if (send[10] == 0xFF) {
    digitalWrite(4, HIGH);
  }
  else if (send[10] == 0x00) {
    digitalWrite(4, LOW);
  }
  if (send[11] == 0xFF) {
    digitalWrite(5, HIGH);
  }
  else if (send[11] == 0x00) {
    digitalWrite(5, LOW);
  }
  send[18] = makeChecksum(send);
  Serial.print("Slave Data(send) : ");
  printPacket(send);
  digitalWrite(modepin, readmode);
  digitalWrite(modepin, sendmode);
  rs485.write(send, plen);
  digitalWrite(modepin, readmode);
  delete[] send;

}

void setup() {
  // put your setup code here, to run once:
  pinMode(modepin, OUTPUT);
  digitalWrite(modepin, readmode); //수신모드
  pinMode(4, OUTPUT); //LED1
  pinMode(5, OUTPUT); //LED2
  rs485.begin(9600);
  Serial.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  if (rs485.available()){
    byte* recv = new byte[plen];
    rs485.readBytes(recv, plen);
    Serial.print("Slave Data(recv) : ");
    printPacket(recv);
    if (recv[0] == 0xAA && recv[1] == 0x55) {
      if (recv[8] == 0x00 && recv[9] == 0x3A) {
        lightState(recv);
      }
      else if (recv[8] == 0x00 && recv[9] == 0x00) {
        lightControl(recv);
      }
    }
    delete[] recv;
  }
}