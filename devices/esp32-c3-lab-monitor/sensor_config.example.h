#pragma once

// Copy this file to sensor_config.h and enter your own values there.
// sensor_config.h is ignored by Git. Never commit your Wi-Fi password or token.
const char *WIFI_SSID = "YOUR_2_4_GHZ_WIFI";
const char *WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
const char *DEVICE_TOKEN = "YOUR_LAB_SENSOR_TOKEN";

// Paste the trusted root CA for lab.naturalfoliage.com.lk in PEM format.
// Do not use setInsecure(): it would expose the device token to interception.
const char *ROOT_CA = R"PEM(
)PEM";
