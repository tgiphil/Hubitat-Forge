# Tuya SNZB-02 Temperature and Humidity Sensor Driver Specification

Filename: `Specification.TuyaSNZB02.md`

This specification defines a dedicated Hubitat Groovy Zigbee driver for the Tuya/SNZB-02 temperature and humidity sensor variant identified by model `SNZB-02` and manufacturer `_TZ3000_utwgoauk`.

---

## Scope

Implement a standalone device driver that:

- Pairs with the SNZB-02 fingerprint shown below
- Parses standard Zigbee reports for temperature, humidity, and battery
- Emits normalized Hubitat events for automations and dashboards
- Supports refresh and configure operations

Out of scope for this phase:

- App-level orchestration
- Repo/project reorganization
- Multi-model abstraction

---

## Required Namespace And Author

Driver metadata must use:

- `namespace: "tgiphil"`
- `author: "phil@thinkedge.com"`

---

## Device Fingerprint

Use this fingerprint for matching:

- Profile ID: `0x0104`
- Endpoint: `0x01`
- In Clusters: `0000,0003,0001,0020,0402,0405`
- Out Clusters: `0019`
- Model: `SNZB-02`
- Manufacturer: `_TZ3000_utwgoauk`

---

## Driver Metadata Requirements

Driver name:

- `Tuya SNZB-02 Temperature Humidity Sensor`

Capabilities:

- `TemperatureMeasurement`
- `RelativeHumidityMeasurement`
- `Battery`
- `Sensor`
- `Refresh`
- `Configuration`

Preferences:

- Boolean debug logging toggle (`logEnable`)

---

## Zigbee Parsing Requirements

### Cluster `0x0402` Temperature Measurement

- Attribute: `0x0000`
- Encoding seen: `0x29` (signed 16-bit)
- Raw value is centi-degrees Celsius
- Conversion: `temperatureC = signedInt16(raw) / 100.0`
- Emit event:
  - `name: "temperature"`
  - `value`: converted temperature (respecting hub temperature scale for display)
  - `unit`: `°C` or `°F` based on platform conventions

Example from logs:

- `value: 09E0` -> `2528` -> `25.28°C`

### Cluster `0x0405` Relative Humidity

- Attribute: `0x0000`
- Encoding seen: `0x21` (unsigned 16-bit)
- Raw value is centi-%RH
- Conversion: `humidity = raw / 100.0`
- Emit event:
  - `name: "humidity"`
  - `value`: numeric humidity percent
  - `unit`: `%`

Example from logs:

- `value: 11F9` -> `4601` -> `46.01%`

### Cluster `0x0001` Battery

- Supported battery attributes:
  - `0x0020` (battery voltage)
  - `0x0021` (battery percentage remaining)
- Handle both when present.
- Preferred event output:
  - `name: "battery"`
  - `value`: percentage `0..100`
  - `unit`: `%`

Observed examples:

- `attr 0x0020 = 0x1E`
- `attr 0x0021 = 0xC8` (common Zigbee format: half-percent units)

---

## Commands And Lifecycle

Required methods:

- `installed()`
- `updated()`
- `configure()`
- `refresh()`
- `parse(String description)`

Behavior:

- `installed/updated`: initialize logging behavior and optionally schedule debug auto-off if implemented
- `configure`: send zigbee reporting/read configuration for `0402`, `0405`, and battery attributes
- `refresh`: actively read temperature, humidity, and battery attributes

---

## Error Handling

- Ignore unknown clusters/attributes safely
- Do not throw for malformed messages; log debug details when enabled
- Avoid duplicate event spam where practical (Hubitat `isStateChange` defaults are acceptable if no custom suppression is needed)

---

## Validation Criteria

The driver is considered valid when:

1. Device fingerprints correctly during pairing for model/manufacturer above.
2. Raw sample messages from provided logs decode into expected temperature/humidity values.
3. Battery events populate from cluster `0x0001` reports or reads.
4. `refresh()` returns updated values.
5. `configure()` executes without runtime errors.

---

## Sample Log Inputs (Reference)

- `cluster:0402 attrId:0000 encoding:29 value:09E0`
- `cluster:0402 attrId:0000 encoding:29 value:0A54`
- `cluster:0405 attrId:0000 encoding:21 value:11F9`
- `cluster:0405 attrId:0000 encoding:21 value:146C`
- `cluster:0001 attrId:0020 encoding:20 value:1E`
- `cluster:0001 attrId:0021 encoding:20 value:C8`

These inputs should map to stable temperature/humidity/battery attribute events.

## Device Driver Guidelines
- For the Tuya SNZB-02 driver, the event descriptionText should follow: '{Device Name} {temperature, humidity, battery, battery voltage} is {value}'.

## Sensor Event Guidelines
- Provide sensor event descriptions in a strict format.
- When calibrating SNZB-02 temperature readings, include an adjustable temperature offset for this driver.
