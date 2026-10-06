# ESP32-C3 Mini lab temperature and humidity

This sketch reads a DHT11 on GPIO4 every 30 seconds and securely sends the latest reading to the lab dashboard. The site polls every 15 seconds and flags readings older than two minutes as offline. Only the latest reading is stored, so no sensor-history table grows indefinitely.

## Wiring

- DHT11 VCC → ESP32-C3 3V3; DHT11 GND → GND; DHT11 DATA → GPIO4.
- SSD1306 128×64 OLED VCC → 3V3; GND → GND; SDA → GPIO8; SCL → GPIO9. The sketch assumes I²C address `0x3C`; change `OLED_ADDRESS` if your module uses `0x3D`.
- A bare 4-pin DHT11 normally needs a pull-up resistor between DATA and 3V3; check whether your 3-pin breakout already has one.
- DHT11 is **not I²C**. GPIO8/9 are for the OLED only. They are ESP32-C3 boot-strapping pins; if the board fails to start or upload with the OLED attached, use safe non-strapping I²C pins and change the two `OLED_*_PIN` constants.

## Setup

1. In Railway → Backend → Variables, set `LAB_SENSOR_TOKEN` to a long random secret (for example, generate 32 random bytes). Keep it private. Deploy the backend update before uploading the sketch.
2. In Arduino IDE, select your ESP32-C3 board and install **DHT sensor library**, **Adafruit GFX Library**, and **Adafruit SSD1306**. Install their prompted dependencies, including Adafruit Unified Sensor.
3. Copy `sensor_config.example.h` to `sensor_config.h` in the same folder. Enter the 2.4 GHz Wi-Fi name/password and the *same* token in that private file. It is ignored by Git.
4. Export the trusted root CA certificate for `lab.naturalfoliage.com.lk` from your TLS certificate chain in PEM format and paste it into `ROOT_CA` in `sensor_config.h`. The certificate must be a CA, not a short-lived leaf certificate. If the site changes certificate authority later, update the CA. HTTPS verification is intentionally required; do not use `setInsecure()` with the token.
5. Upload and open Serial Monitor at 115200 baud. A successful upload prints `HTTP 204`. The OLED shows local readings and upload status. Sign in to the lab site; temperature and humidity appear beside date/time. If there is no reading, the page says “Waiting for ESP32 sensor”; if the device stops sending, it says “Sensor offline”.

The ESP32 must be able to reach the public HTTPS site and an NTP time server. The dashboard continues to work without a sensor; only the environment values remain unavailable.
