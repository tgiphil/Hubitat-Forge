# HOBEIAN ZG-102Z Contact Sensor

HOBEIAN ZG-102Z is a Hubitat Zigbee driver area for the ZG-102Z contact sensor.

## Files in this area

- `Drivers/HobeianZg102zContactSensorDriver.groovy` — Zigbee driver
- `packageManifest.json` — Hubitat Package Manager package definition
- `INSTALLATION.md` — installation and usage
- `Specification.md` — detailed implementation specification

## Summary

- reports contact open/closed via IAS Zone cluster (0x0500)
- reports tamper detected/clear via IAS Zone status bit 2
- reads battery percentage and battery voltage
- supports refresh and configure operations

## Start here

- Use the HPM manifest in `packageManifest.json` or read `INSTALLATION.md` for manual setup
- Read `Specification.md` for implementation details
