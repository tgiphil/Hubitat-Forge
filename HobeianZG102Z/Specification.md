# HOBEIAN ZG-102Z Contact Sensor Driver Specification

Filename: `Specification.md`

This specification defines a dedicated Hubitat Groovy Zigbee driver for the HOBEIAN ZG-102Z contact sensor identified by model `ZG-102Z` and manufacturer `HOBEIAN`.

---

## Scope

Implement a standalone device driver that:

- Pairs with the ZG-102Z fingerprint shown below
- Parses IAS Zone cluster zone-status messages for contact open/closed and tamper state
- Parses battery cluster reports for battery percentage and voltage
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
- In Clusters: `0000,0003,0500,EF00,0001`
- Model: `ZG-102Z`
- Manufacturer: `HOBEIAN`

---

## Driver Metadata Requirements

Driver name:

- `HOBEIAN ZG-102Z Contact Sensor`

Capabilities:

- `ContactSensor`
- `TamperAlert`
- `Battery`
- `Sensor`
- `Refresh`
- `Configuration`

Custom attributes:

- `batteryVoltage` — `number` (volts)

Preferences:

- Boolean debug logging toggle (`logEnable`)

---

## Zigbee Parsing Requirements

### IAS Zone Cluster `0x0500` — Zone Status Descriptions

Zone state arrives as raw description strings of the form:

```
zone status 0x<hex> -- extended status 0x00 - sourceEndpoint:01, zoneId:00, delay:0000
```

Parse the 16-bit zone-status hex value and evaluate the following bits:

| Bit | Mask   | Meaning                             |
|-----|--------|-------------------------------------|
| 0   | 0x0001 | Alarm1 — contact is open            |
| 2   | 0x0004 | Tamper — tamper detected            |

Emit events:

- `name: "contact"`, value: `"open"` (bit 0 set) or `"closed"` (bit 0 clear)
- `name: "tamper"`, value: `"detected"` (bit 2 set) or `"clear"` (bit 2 clear)

Examples from logs:

- `zone status 0x0000` → contact closed, tamper clear
- `zone status 0x0001` → contact open, tamper clear

### Cluster `0x0001` Battery

Supported battery attributes (arrive as `read attr -` or `catchall:` descriptions):

- `0x0021` (BatteryPercentageRemaining) — raw value is half-percent units
  - Conversion: `percent = clamp(round(raw / 2), 0, 100)`
  - Emit `name: "battery"`, value: percentage, unit: `"%"`
- `0x0020` (BatteryVoltage) — raw value in 100 mV units
  - Conversion: `volts = raw / 10.0`
  - Emit `name: "batteryVoltage"`, value: volts, unit: `"V"`
  - Also emit estimated `name: "battery"` percentage derived from voltage (2.1 V min, 3.0 V max)

Observed examples:

- `attr 0x0021 = 0x64` → raw 100 → battery 50%
- `attr 0x0020 = 0x1B` → raw 27 → 2.7 V

---

## Commands And Lifecycle

Required methods:

- `installed()`
- `updated()`
- `configure()`
- `refresh()`
- `parse(String description)`

Behavior:

- `installed/updated`: initialize logging behavior; schedule debug auto-off (30 min) when debug logging is enabled
- `configure`: send Zigbee reporting configuration and initial read for battery attributes (cluster `0x0001`, attrs `0x0021` and `0x0020`); IAS Zone enrollment is handled automatically by the Hubitat hub
- `refresh`: actively read battery attributes (contact state is event-driven via zone status; no pull read is available)

---

## Error Handling

- Ignore unknown clusters/attributes safely
- Do not throw for malformed messages; log debug details when enabled
- Descriptions that do not match a known pattern are logged at debug level and ignored

---

## Validation Criteria

The driver is considered valid when:

1. Device fingerprints correctly during pairing for model/manufacturer above.
2. `zone status 0x0001` description produces `contact: open`.
3. `zone status 0x0000` description produces `contact: closed`.
4. Zone status with bit 2 set produces `tamper: detected`.
5. Battery events populate correctly from cluster `0x0001` reports.
6. `refresh()` returns updated battery values without error.
7. `configure()` executes without runtime errors.

---

## Sample Log Inputs (Reference)

- `zone status 0x0000 -- extended status 0x00 - sourceEndpoint:01, zoneId:00, delay:0000`
- `zone status 0x0001 -- extended status 0x00 - sourceEndpoint:01, zoneId:00, delay:0000`
- `cluster:0001 attrId:0021 encoding:20 value:64`
- `cluster:0001 attrId:0020 encoding:20 value:1B`

These inputs should map to stable contact/tamper/battery attribute events.

## Device Driver Guidelines

- Event descriptionText should follow: `'{Device Name} {contact, tamper, battery, battery voltage} is {value}'`.

## Sensor Event Guidelines

- Provide sensor event descriptions in a strict format.
