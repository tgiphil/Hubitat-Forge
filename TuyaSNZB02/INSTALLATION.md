# Tuya SNZB-02 Installation & Usage

## Install the driver

1. Open Hubitat Web UI and go to **Code**.
2. Click **+ Create new driver**.
3. Copy the contents of `Drivers/TuyaSnzb02TemperatureHumidityDriver.groovy`.
4. Paste the code into the driver editor.
5. Save and publish the driver.

## Create or use the device

1. Open the device details page in Hubitat.
2. Select the Tuya SNZB-02 driver you published.
3. Save the device.

## Configuration

The driver supports:

- temperature measurement
- humidity measurement
- battery reporting
- refresh
- configure
- temperature offset calibration

## Behavior

- Temperature is parsed from cluster `0x0402`
- Humidity is parsed from cluster `0x0405`
- Battery is parsed from cluster `0x0001`
- Debug logging can be enabled in driver preferences

## Related files

- `README.md`
- `Specification.md`
