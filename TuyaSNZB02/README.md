# Tuya SNZB-02 Temperature and Humidity Sensor

A Hubitat Groovy Zigbee driver for the Tuya / SNZB-02 temperature and humidity sensor variant identified by model `SNZB-02` and manufacturer `_TZ3000_utwgoauk`.

## Overview

This area contains the driver and specification for the SNZB-02 temperature and humidity sensor.

## Files in this area

- `Drivers/TuyaSnzb02TemperatureHumidityDriver.groovy` — Zigbee driver
- `Specification.md` — detailed implementation specification

## Installation

1. Create a new driver in Hubitat.
2. Paste the contents of `Drivers/TuyaSnzb02TemperatureHumidityDriver.groovy`.
3. Save and publish the driver.

## Supported capabilities

- TemperatureMeasurement
- RelativeHumidityMeasurement
- Battery
- Sensor
- Refresh
- Configuration

## Behavior

- Parses temperature from cluster `0x0402`
- Parses humidity from cluster `0x0405`
- Handles battery reports from cluster `0x0001`
- Supports refresh and configure operations
- Includes an adjustable temperature offset preference

## Documentation

- `Specification.md`
