# Virtual Presence Detector

Virtual Presence Detector is a Hubitat presence automation area for creating virtual presence devices from monitored activity.

## Files in this area

- `Apps/VirtualPresenceDetectorApp.groovy` — automation app
- `Drivers/VirtualPresenceDetectorDriver.groovy` — virtual presence device driver
- `packageManifest.json` — Hubitat Package Manager package definition
- `INSTALLATION.md` — installation and troubleshooting
- `Specification.md` — detailed implementation specification

## Summary

- supports room, area, and home-level aggregation
- creates the detector device automatically for each app instance
- handles timeout recovery after hub restarts
- blocks self-monitoring

## Start here

- Use the HPM manifest in `packageManifest.json` or read `INSTALLATION.md` for manual setup
- Read `Specification.md` for implementation details
