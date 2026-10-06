#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <DHT.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <time.h>
#include <math.h>
#include <string.h>
#include "sensor_config.h"

const char *API_URL = "https://lab.naturalfoliage.com.lk/api/lab-environment";

constexpr uint8_t DHT_PIN = 4;
constexpr uint8_t DHT_TYPE = DHT11;
constexpr uint8_t OLED_SDA_PIN = 8;
constexpr uint8_t OLED_SCL_PIN = 9;
constexpr uint8_t OLED_ADDRESS = 0x3C;
constexpr unsigned long SEND_INTERVAL_MS = 30000;
DHT dht(DHT_PIN, DHT_TYPE);
Adafruit_SSD1306 display(128, 64, &Wire, -1);
bool displayReady = false;
unsigned long lastAttempt = 0;

void showDisplay(float temperature, float humidity, const char *status) {
  if (!displayReady) return;
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 0);
  display.print("NATURAL FOLIAGE LAB");
  display.setTextSize(2);
  display.setCursor(0, 16);
  if (isnan(temperature)) display.print("--.- C");
  else { display.print(temperature, 1); display.print(" C"); }
  display.setCursor(0, 38);
  if (isnan(humidity)) display.print("--% RH");
  else { display.print(humidity, 0); display.print("% RH"); }
  display.setTextSize(1);
  display.setCursor(0, 56);
  display.print(status);
  display.display();
}

bool connectWifi() {
  if (WiFi.status() == WL_CONNECTED) return true;
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  for (int i = 0; i < 30 && WiFi.status() != WL_CONNECTED; ++i) delay(500);
  return WiFi.status() == WL_CONNECTED;
}

bool clockIsReady() {
  if (time(nullptr) > 1700000000) return true;
  configTime(0, 0, "pool.ntp.org", "time.google.com");
  for (int i = 0; i < 20 && time(nullptr) <= 1700000000; ++i) delay(500);
  return time(nullptr) > 1700000000;
}

void setup() {
  Serial.begin(115200);
  dht.begin();
  Wire.begin(OLED_SDA_PIN, OLED_SCL_PIN);
  displayReady = display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDRESS, true, false);
  if (!displayReady) Serial.println("OLED not found; check I2C address and GPIO8/9 wiring.");
  showDisplay(NAN, NAN, "Starting...");
  WiFi.mode(WIFI_STA);
}

void loop() {
  if (lastAttempt != 0 && millis() - lastAttempt < SEND_INTERVAL_MS) return;
  lastAttempt = millis();

  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("DHT11 read failed; check DATA on GPIO4 and 3.3V power.");
    showDisplay(NAN, NAN, "DHT11 read error");
    return;
  }
  showDisplay(temperature, humidity, "Connecting...");
  if (String(DEVICE_TOKEN) == "YOUR_LAB_SENSOR_TOKEN" || strlen(ROOT_CA) < 100) {
    Serial.println("Set the device token and root CA before uploading.");
    showDisplay(temperature, humidity, "Setup needed");
    return;
  }
  if (!connectWifi() || !clockIsReady()) {
    Serial.println("Wi-Fi or network time unavailable; will retry.");
    showDisplay(temperature, humidity, "Wi-Fi/NTP offline");
    return;
  }

  WiFiClientSecure client;
  client.setCACert(ROOT_CA);
  HTTPClient http;
  if (!http.begin(client, API_URL)) {
    Serial.println("Could not start HTTPS request.");
    showDisplay(temperature, humidity, "HTTPS error");
    return;
  }
  http.setTimeout(10000);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Token", DEVICE_TOKEN);
  String body = "{\"temperatureC\":" + String(temperature, 1)
      + ",\"humidityPercent\":" + String(humidity, 0) + "}";
  int status = http.POST(body);
  Serial.printf("Lab sensor upload: HTTP %d, %.1f C, %.0f%% RH\n", status, temperature, humidity);
  showDisplay(temperature, humidity, status == 204 ? "Dashboard updated" : "Upload failed");
  http.end();
}
