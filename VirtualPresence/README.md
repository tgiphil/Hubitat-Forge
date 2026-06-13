# Virtual Presence Detector

A Hubitat Elevation presence automation system that detects recent human-triggered activity for rooms, areas, zones, or whole homes.

## Overview

The Virtual Presence Detector creates a virtual presence device that becomes `present` when monitored devices report qualifying activity, then returns to `not present` after the configured timeout.

This area supports hierarchical aggregation:

- room-level detectors
- area-level detectors
- home-level detectors

## Files in this area

- `Apps/VirtualPresenceDetectorApp.groovy` — automation app
- `Drivers/VirtualPresenceDetectorDriver.groovy` — virtual presence device driver
- `INSTALLATION.md` — setup, troubleshooting, and usage notes
- `Specification.md` — detailed implementation specification

## Quick Start

1. Install the driver from `Drivers/VirtualPresenceDetectorDriver.groovy`.
2. Install the app from `Apps/VirtualPresenceDetectorApp.groovy`.
3. Create an app instance from **Virtual Presence Detector App**.
4. Configure the monitored devices and timeout.

A virtual presence detector device is created automatically for each app instance.

## Example Usage

### Room-level detector

- Monitor a room motion sensor
- Monitor a room contact sensor
- Set a short timeout such as 10 minutes

### Area-level detector

- Monitor room-level virtual presence detectors
- Set a longer timeout such as 15 minutes

### Home-level detector

- Monitor area-level virtual presence detectors
- Set the longest timeout such as 30 minutes

## Key Behavior

- Timeouts survive hub restarts
- Parent detectors can monitor child detectors
- Self-monitoring is blocked
- Event values are normalized before filtering

## Documentation

- `INSTALLATION.md`
- `Specification.md`
