# Bluetooth Low Energy pairing and connecting example for uFR Online device

Android Java Bluetooth Low Energy pairing and connecting example for uFR Online device.

## How to enable BLE mode?

1. Open uFR Online WEB frontend settings page and login.
2. Open other settings.
3. If the reader is working in master mode, switch it to slave mode.
4. Navigate to Bluetooth Low Energy section.
5. Click on the section to enable BLE mode.
6. BLE security section will be shown.
7. Navigate to the next page for more details about BLE security modes.
8. Click save and restart.
9. After restart, light blue light will blink.

Notes.
*Light blue blinking LEDs indicates that reader is waiting for smartphone or other BLE capable device                             to initiate a connection. Device is visible also as a WiFi Access point.
*Light blue steady LEDs indicate that the reader is already connected. Connected devices are not visible on BLE scanning. Device is not visible as a WiFi Access point. 

## BLE Security modes

There are three BLE security modes. Details about all three are shown in table 1. Disabled security means that there is no open security channel and MITM protection.

| *                          | Security disabled | Security enabled and PIN is setted to 0 | Security enabled and PIN is 6-digit long |
|----------------------------|-------------------|-----------------------------------------|------------------------------------------|
| Pairing                    | Optional          | Required                                | Required                                 |
| Authentication             | None              | Numerical comparasion                   | Preshared 6 digit PIN                    |
| *Need for user interaction | No                | Yes                                     | Yes                                      |

## References


[uFR Online BLE mode document](https://www.d-logic.net/code/nfc-rfid-reader-sdk/ufr-doc/blob/master/uFR_Online_BLE_mode.pdf)<br/>
[uFR Online Quick start guide](https://www.d-logic.net/code/nfc-rfid-reader-sdk/ufr-doc/blob/master/uFR_Online%20-%20Quick_Start_Guide.pdf)<br/>
[IOS example](https://www.d-logic.net/code/nfc-rfid-reader-sdk/ufr-doc/nfc-rfid-reader-sdk/ufr_online-examples-ios-ble)