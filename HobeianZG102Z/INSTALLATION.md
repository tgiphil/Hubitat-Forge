# HOBEIAN ZG-102Z Installation & Usage

## Hubitat Package Manager Installation

If you want to install or share the HOBEIAN ZG-102Z driver through Hubitat Package Manager, use this package manifest URL:

`https://raw.githubusercontent.com/tgiphil/Hubitat-Forge/master/HobeianZG102Z/packageManifest.json`

To make the package available to other users, keep the repository public, keep the manifest version in sync with each release, and publish GitHub releases or tags alongside code updates.

---

## Install the driver

### Option A: Install with Hubitat Package Manager

1. Open Hubitat Package Manager
2. Add a custom package source using the manifest URL above
3. Install **HOBEIAN ZG-102Z Contact Sensor** from the package list
4. Follow the configuration prompts

---

### Option B: Manual Install

1. Open Hubitat Web UI and go to **Code**.
2. Click **+ Create new driver**.
3. Copy the contents of `Drivers/HobeianZg102zContactSensorDriver.groovy`.
4. Paste the code into the driver editor.
5. Save and publish the driver.

## Create or use the device

1. Open the device details page in Hubitat.
2. Select the HOBEIAN ZG-102Z Contact Sensor driver you published.
3. Save the device.

## Configuration

The driver supports:

- contact open/closed reporting
- tamper alert reporting
- battery percentage reporting
- battery voltage reporting
- refresh
- configure

## Behavior

- Contact and tamper state are parsed from IAS Zone cluster `0x0500` zone-status messages
- Battery is parsed from cluster `0x0001`
- Debug logging can be enabled in driver preferences

## Related files

- `README.md`
- `Specification.md`
