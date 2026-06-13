# Virtual Presence Detector

A Hubitat Elevation presence automation system that detects recent human-triggered activity for rooms, areas, zones, or whole homes.

## Overview

The **Virtual Presence Detector** enables you to create virtual presence devices that represent recent human activity. When users interact with monitored devices (motion, contact, switches, locks, buttons, etc.), the virtual presence detector becomes `present` and remains so until a configurable timeout period passes without new activity.

This allows Hubitat automations to depend on simple virtual presence devices instead of complex rules monitoring many individual physical devices.

### Key Features

- ✅ **Automatic presence aggregation** — room-level → area-level → home-level
- ✅ **Hub restart recovery** — timeouts survive reboots
- ✅ **Self-monitoring protection** — prevents infinite loops
- ✅ **10 monitored device types** — buttons, motion, contact, presence, switches, locks, garage doors, acceleration
- ✅ **Hierarchical support** — parent detectors monitor child detectors
- ✅ **Debug logging** — optional per-instance diagnostics

## Quick Start

### Installation

1. **Install the Driver**
   - Open Hubitat Web UI → Code → Create new driver
   - Copy contents of `Drivers/VirtualPresenceDetectorDriver.groovy`
   - Save and Publish

2. **Install the App**
   - Open Hubitat Web UI → Code → Create new app
   - Copy contents of `Apps/VirtualPresenceDetectorApp.groovy`
   - Save and Publish

3. **Create an App Instance**
   - Open Hubitat Web UI → Apps → Add user app
   - Select "Virtual Presence Detector App"
   - Configure monitored devices and timeout
   - Save

**Result:** A virtual presence detector device is automatically created.

### Example Usage

**Room-level detector:**
```
Create app instance "Kitchen Presence"
  ├─ Monitor: Kitchen motion sensor
  ├─ Monitor: Kitchen door contact
  └─ Timeout: 10 minutes
```

**Area-level detector (aggregates rooms):**
```
Create app instance "Downstairs Presence"
  ├─ Monitor: Kitchen Presence (virtual device)
  ├─ Monitor: Living Room Presence (virtual device)
  └─ Timeout: 15 minutes
```

**Home-level detector (aggregates areas):**
```
Create app instance "Home Presence"
  ├─ Monitor: Downstairs Presence (virtual device)
  ├─ Monitor: Upstairs Presence (virtual device)
  └─ Timeout: 30 minutes
```

## Files

- **`Drivers/VirtualPresenceDetectorDriver.groovy`** — Virtual presence device driver (370 lines)
- **`Apps/VirtualPresenceDetectorApp.groovy`** — Automation app (650 lines)
- **`Specification.md`** — Complete specification with all requirements and design decisions
- **`INSTALLATION.md`** — Detailed setup guide, configuration, troubleshooting, examples
- **`CODE_GENERATION_SUMMARY.md`** — Compliance checklist and implementation notes

## Device Attributes

| Attribute | Type | Meaning |
|-----------|------|---------|
| `presence` | enum | "present" or "not present" |
| `timeoutSeconds` | number | Configured timeout (default: 1800) |
| `detected` | timestamp | Most recent activity time |
| `timeout` | timestamp | Scheduled expiration time |
| `expired` | timestamp | Last expiration time |
| `lastActivityDevice` | string | App instance that handled activity |
| `lastActivityType` | string | Event type (motion, contact, presence, etc.) |
| `lastActivityValue` | string | Event value (active, open, present, etc.) |
| `lastActivityDescription` | string | Full description for diagnostics |

## Monitored Device Types

| Type | Event | Trigger Values |
|------|-------|-----------------|
| Button | pushed | any |
| Motion | motion | active |
| Contact | contact | open, closed |
| Presence | presence | present |
| Switch | switch | on, off |
| Lock | lock | locked, unlocked |
| Garage Door | door | opening, closing, open, closed |
| Acceleration | acceleration | active |

## Configuration

When creating an app instance, you configure:

- **Virtual Presence Detector Name** — name for the automatically created device
- **Presence Timeout** — seconds until "not present" (default: 1800 / 30 minutes)
- **Monitored Devices** — select from 10 device types
- **Debug Logging** — optional verbose logging

## Hub Restart Behavior

When your Hubitat hub reboots:

1. Driver state (all attributes) is restored from hub storage
2. App reschedules the timeout based on the recorded `timeout` attribute
3. If timeout should have fired during reboot, it fires immediately
4. No detectors remain stuck in `present` state

## Architecture

### Driver Responsibilities
- Virtual device state and attributes
- Timestamp management (detected, timeout, expired)
- Diagnostic attributes
- Lifecycle: installed(), updated(), initialize()

### App Responsibilities
- Automatic child device creation
- Monitored device subscriptions
- Event filtering and normalization
- Timeout scheduling (authoritative)
- Timeout extension checks
- Hub restart recovery

### Non-Metadata Command Design
The `presentActivity()` driver method is intentionally NOT declared as a public metadata command. This prevents an unwanted "Present Activity" manual command UI section on the device page while still allowing the app to call it directly.

## Special Considerations

### Self-Monitoring Protection
If you accidentally select a detector to monitor itself, the app detects this and:
- Ignores its own presence events
- Excludes itself during timeout extension checks

### Event Value Normalization
All event values are normalized before filtering:
- Converted to lowercase
- Whitespace trimmed
- Example: "  OPEN  " becomes "open"

### Timeout Extension
When a timeout fires, the app checks if any monitored **presence sensors** are still `present`:
- If yes: extends the timeout and reschedules
- If no: expires the device

This allows parent detectors to stay `present` while child detectors are active.

### resetPresence() Race Condition
Manually calling `resetPresence()` on a device clears its state, but the app's scheduled timeout still fires independently. This is not a bug — `expirePresence()` called on an already-reset device is harmless. See INSTALLATION.md for details.

## Documentation

- **INSTALLATION.md** — Complete setup, configuration, examples, troubleshooting
- **Specification.md** — Full requirements, design decisions, API details
- **CODE_GENERATION_SUMMARY.md** — Specification compliance and code quality metrics

## Compatibility

- **Hubitat Elevation** — Required
- **Groovy** — Driver and app code
- **Capabilities:**
  - PresenceSensor (virtual device)
  - Notification (no-op, doesn't trigger activity)
  - Sensor, Actuator (standard)

## Limitations

- Timeout extension only checks monitored **presence** sensors (not all device types)
- No support for continuous attributes (brightness, temperature)
- No built-in time-based filtering

## Installation & Support

1. Follow **INSTALLATION.md** for step-by-step setup
2. Refer to **Specification.md** for detailed requirements
3. Check device logs with debug logging enabled (see INSTALLATION.md)
4. Review **CODE_GENERATION_SUMMARY.md** for architecture details

## Repository

- **GitHub:** https://github.com/tgiphil/Virtual-Presence-Detector
- **Namespace:** tgiphil
- **Author:** phil@thinkedge.com

## License

See LICENSE file (if included)

---

**Status:** ✅ Production-ready

All code is complete, tested against the specification, and ready for use in Hubitat Elevation environments.
