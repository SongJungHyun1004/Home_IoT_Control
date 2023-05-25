#include <ESP8266WiFi.h>
#include <SoftwareSerial.h>

#define modepin D7
#define readmode LOW
#define sendmode HIGH
#define PORT 1000
#define plen 21

const char* ssid = "Baek2";
const char* pass = "baekmin777";

SoftwareSerial rs485(D2, D3); // RX, TX
WiFiClient client;
WiFiServer server(PORT);

void printPacket(byte* packetData) {
  for (int i = 0; i < plen; i++) {
    if (packetData[i] < 0x10) {
      Serial.print("0");
    }
    Serial.print(packetData[i], HEX);
  }
  Serial.println();
}

void setup() {
  // put your setup code here, to run once:
  pinMode(modepin, OUTPUT);
  digitalWrite(modepin, readmode); //송신모드
  rs485.begin(9600);
  Serial.begin(9600);
  Serial.println("TCPIP Test");
  Serial.print("Port : ");
  Serial.println(PORT);
  WiFi.begin(ssid, pass);
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(200);
  }
  Serial.println("");
  Serial.println(WiFi.localIP());

  delay(200);
  server.begin();
}

void loop() {
  // put your main code here, to run repeatedly:
  while (!client.connected()) {
    client = server.available();
  }

  if (client.connected()) {
    if (client.available()) {
      byte* packetData = new byte[plen];
      client.readBytes(packetData, plen);
      Serial.print("Master Data(send) : ");
      printPacket(packetData);
      if (packetData[0] == 0xAA && packetData[1] == 0x55 && packetData[4] == 0x00 && packetData[5] == 0x0E) {
        digitalWrite(modepin, sendmode);
        rs485.write(packetData, plen);
        delete[] packetData;
        digitalWrite(modepin, readmode);
      }
      delay(1000);
      if (rs485.available()){
        byte* recv = new byte[plen];
        rs485.readBytes(recv, plen);
        client.write(recv, plen);
        Serial.print("Master Data(recv) : ");
        printPacket(recv);        
        delete[] recv;
      }
    }
  }
}
