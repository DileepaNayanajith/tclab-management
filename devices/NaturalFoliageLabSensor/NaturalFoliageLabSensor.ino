// ESP32-C3 Mini + DHT11 + SSD1306 128x64 OLED.
// This is one Arduino sketch: sensor_config.h is not needed.

#include <DHT.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <math.h>

// Set to 1 only after filling the Wi-Fi, token, and CA certificate below.
#define UPLOAD_TO_DASHBOARD 0

#if UPLOAD_TO_DASHBOARD
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <time.h>
#include <string.h>

const char *WIFI_SSID = "YOUR_2_4_GHZ_WIFI_NAME";
const char *WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
const char *DEVICE_TOKEN = "YOUR_LAB_SENSOR_TOKEN";
const char *ROOT_CA = R"PEM(
-----BEGIN CERTIFICATE-----
PASTE_THE_CURRENT_ROOT_CA_CERTIFICATE_HERE
-----END CERTIFICATE-----
)PEM";
const char *API_URL = "https://lab.naturalfoliage.com.lk/api/lab-environment";
#endif

constexpr uint8_t DHT_PIN = 4;
constexpr uint8_t OLED_SDA_PIN = 8;
constexpr uint8_t OLED_SCL_PIN = 9;
constexpr uint8_t OLED_ADDRESS = 0x3C;
constexpr unsigned long READ_INTERVAL_MS = 2500;
constexpr unsigned long UPLOAD_INTERVAL_MS = 30000;

DHT dht(DHT_PIN, DHT11);
Adafruit_SSD1306 display(128, 64, &Wire, -1);
bool oledReady = false;
unsigned long lastReadAt = 0;
unsigned long lastUploadAt = 0;
const char *screenStatus = "OLED only";

void showReading(float temperature, float humidity) {
  if (!oledReady) return;
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
  display.print(screenStatus);
  display.display();
}

#if UPLOAD_TO_DASHBOARD
bool connectWifi() {
  if (WiFi.status() == WL_CONNECTED) return true;
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  for (int i = 0; i < 30 && WiFi.status() != WL_CONNECTED; ++i) delay(500);
  return WiFi.status() == WL_CONNECTED;
}

bool syncClock() {
  if (time(nullptr) > 1700000000) return true;
  configTime(0, 0, "pool.ntp.org", "time.google.com");
  for (int i = 0; i < 20 && time(nullptr) <= 1700000000; ++i) delay(500);
  return time(nullptr) > 1700000000;
}

const char *uploadReading(float temperature, float humidity) {
  if (strcmp(DEVICE_TOKEN, "YOUR_LAB_SENSOR_TOKEN") == 0 ||
      strstr(ROOT_CA, "PASTE_THE_CURRENT_ROOT_CA_CERTIFICATE_HERE") != nullptr) {
    return "Set token / CA";
  }
  if (!connectWifi()) return "Wi-Fi offline";
  if (!syncClock()) return "NTP offline";

  WiFiClientSecure client;
  client.setCACert(ROOT_CA);
  HTTPClient http;
  if (!http.begin(client, API_URL)) return "HTTPS error";
  http.setTimeout(10000);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Token", DEVICE_TOKEN);
  String body = "{\"temperatureC\":" + String(temperature, 1)
      + ",\"humidityPercent\":" + String(humidity, 0) + "}";
  int status = http.POST(body);
  http.end();
  Serial.printf("Dashboard HTTP status: %d\n", status);
  return status == 204 ? "Dashboard updated" : "Upload failed";
}
#endif

void setup() {
  Serial.begin(115200);
  dht.begin();
  Wire.begin(OLED_SDA_PIN, OLED_SCL_PIN);
  oledReady = display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDRESS, true, false);
  if (!oledReady) Serial.println("OLED not found. Check SDA=8, SCL=9, and address 0x3C.");
#if UPLOAD_TO_DASHBOARD
  WiFi.mode(WIFI_STA);
  screenStatus = "Starting Wi-Fi...";
#endif
  showReading(NAN, NAN);
}

void loop() {
  if (lastReadAt != 0 && millis() - lastReadAt < READ_INTERVAL_MS) return;
  lastReadAt = millis();

  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  if (isnan(humidity) || isnan(temperature)) {
    screenStatus = "DHT11 read error";
    Serial.println("DHT11 read failed. Check DATA=GPIO4, VCC=3.3V, and pull-up.");
    showReading(NAN, NAN);
    return;
  }

  Serial.printf("Temperature: %.1f C | Humidity: %.0f%% RH\n", temperature, humidity);
#if UPLOAD_TO_DASHBOARD
  if (lastUploadAt == 0 || millis() - lastUploadAt >= UPLOAD_INTERVAL_MS) {
    lastUploadAt = millis();
    screenStatus = uploadReading(temperature, humidity);
  }
#endif
  showReading(temperature, humidity);
}
