# Virtual Presence Detector - Installation & Setup Guide

## Overview

The Virtual Presence Detector consists of two components:

1. **Virtual Presence Detector Driver** — manages the virtual presence device state and attributes
2. **Virtual Presence Detector App** — monitors physical devices and controls the virtual detector

Each app instance automatically creates one child virtual presence detector device.

---

## Installation Steps

### Step 1: Install the Driver

1. Open Hubitat Web UI → **Code** page
2. Click **+ Create new driver**
3. Copy the entire contents of `VirtualPresence/Drivers/VirtualPresenceDetectorDriver.groovy`
4. Paste into the new driver code editor
5. Click **Save** at bottom right
6. Click **Publish** (or **Publish for me** if prompted)

**Result:** Your driver is now installed in the `tgiphil` namespace.

---

### Step 2: Install the App

1. Open Hubitat Web UI → **Code** page
2. Click **+ Create new app**
3. Copy the entire contents of `VirtualPresence/Apps/VirtualPresenceDetectorApp.groovy`
4. Paste into the new app code editor
5. Click **Save** at bottom right
6. Click **Publish** (or **Publish for me** if prompted)

**Result:** Your app is now installed in the `tgiphil` namespace and available to create instances.

---

### Step 3: Create an App Instance

1. Open Hubitat Web UI → **Apps** page
2. Click **+ Add user app**
3. Search for or scroll to **Virtual Presence Detector App** (namespace: `tgiphil`)
4. Click to install
5. Configure settings (see Configuration section below)
6. Click **Done** to save

**Result:** The app automatically creates a child Virtual Presence Detector device.

---

## Configuration

When you install an app instance, you'll see these settings:

### Virtual Presence Detector Name (Optional)
- **Description:** Name/label for the automatically created virtual detector device
- **If blank:** Uses the app instance label
- **Example:** "Kitchen Presence", "Living Room Presence", "Home Presence"

### Presence Timeout (seconds)
- **Default:** 1800 (30 minutes)
- **Minimum effective:** 1 second
- **Behavior:** Virtual detector remains `present` until this many seconds pass without qualifying activity

### Monitored Device Types
Select devices from these categories. All are optional:

1. **Button Devices** — any button push event triggers activity
2. **Motion Sensors** — only `motion = active` triggers activity
3. **Contact Sensors** — both `contact = open` and `contact = closed` trigger activity
4. **Presence Sensors** — `presence = present` triggers activity (enables hierarchical presence aggregation)
5. **Switch Devices** — both `switch = on` and `switch = off` trigger activity
6. **Lock Devices** — both `lock = locked` and `lock = unlocked` trigger activity
7. **Garage Door Devices** — `door = opening/closing/open/closed` all trigger activity
8. **Acceleration Sensors** — only `acceleration = active` triggers activity

### Debug Logging
- **Optional:** Enable to see detailed log entries for troubleshooting
- Default is off (recommended for production)

---

## Usage Examples

### Example 1: Room-Level Detector

Create an app instance labeled **"Kitchen Presence"**:
- Monitor motion sensors in the kitchen
- Monitor door contact on kitchen entry
- Timeout: 10 minutes

**Result:** A virtual device named "Kitchen Presence" becomes present when motion or door contact activity occurs, and stays present for 10 minutes of inactivity.

---

### Example 2: Area-Level Aggregation

Create an app instance labeled **"Downstairs Presence"**:
- Monitor the "Kitchen Presence" virtual detector (created above)
- Monitor the "Living Room Presence" virtual detector (created separately)
- Timeout: 15 minutes

**Result:** "Downstairs Presence" becomes and stays present if either room is active, with its own 15-minute timeout.

**Hierarchical aggregation**: Room detectors → Area detectors → Home detector.

---

### Example 3: Home-Level Detector

Create an app instance labeled **"Home Presence"**:
- Monitor all area-level virtual presence detectors (Downstairs, Upstairs, etc.)
- Timeout: 30 minutes

**Result:** "Home Presence" reflects activity across the entire home.

---

## Device Attributes

Once created, your Virtual Presence Detector device has these attributes:

| Attribute | Type | Meaning |
|-----------|------|---------|
| `presence` | enum | "present" or "not present" |
| `timeoutSeconds` | number | Configured timeout in seconds |
| `detected` | timestamp | When most recent activity occurred |
| `timeout` | timestamp | When presence will expire if no new activity |
| `expired` | timestamp | When presence last changed to "not present" due to timeout |
| `lastActivityDevice` | string | App instance label that handled the activity |
| `lastActivityType` | string | Event type (e.g., "motion", "contact", "presence") |
| `lastActivityValue` | string | Original event value (e.g., "active", "open", "present") |
| `lastActivityDescription` | string | Full event description for diagnostics |

---

## Device Commands

Your Virtual Presence Detector device supports these public commands:

### `setTimeoutSeconds(seconds)`
Manually change the timeout. Used on the device page for adjustments.
- The app does NOT call this; it uses `presentActivity()` and `extendPresence()`

### `extendPresence(seconds)`
Extend the presence timeout (called by app if a monitored presence sensor is still present at timeout expiration)

### `expirePresence(reason)`
Force the device to "not present" (useful for testing or reset scenarios)

### `resetPresence()`
Clear all state and diagnostic attributes (testing/troubleshooting only)

### `deviceNotification(text)`
Send a notification to the device (does NOT trigger activity)

---

## Hub Restart Behavior

When your Hubitat hub reboots:

1. **Driver state persists** — all attributes and timestamps are restored
2. **App reschedules timeout** — if a detector is currently `present`, the app recalculates remaining time and reschedules the timeout
3. **No detectors stuck** — if a timeout should have fired during the reboot, it fires immediately

This ensures Virtual Presence Detectors don't remain stuck in the `present` state indefinitely after a hub restart.

---

## Self-Monitoring Protection

If you accidentally select a higher-level detector to monitor itself, the app detects this and:
- Ignores `presence = present` events from itself
- Excludes itself when checking for "still present" during timeout extension

This prevents infinite presence loops.

---

## Testing & Troubleshooting

### Enable Debug Logging
1. Open the app instance settings
2. Check "Enable debug logging"
3. Click "Done"
4. Open Hubitat Web UI → **Logs** page
5. Perform activity and watch for debug messages

### Manual Device Notifications (Testing Only)
On the Virtual Presence Detector device page, you can manually run commands like:
- `resetPresence()` — clears all state
- `setTimeoutSeconds(300)` — set timeout to 5 minutes
- `expirePresence("manual")` — force device to "not present"

### Check Device Attributes
View the device in the **Devices** page to see current values for:
- `detected` — when last activity occurred
- `timeout` — when presence will expire
- `lastActivityDevice` — which app instance triggered it
- `lastActivityDescription` — which physical device caused it

---

## Architecture Notes

### App-Owned Timeout Scheduling
- The app (not the driver) owns the timeout schedule
- When activity occurs, the app cancels any existing `runIn()` and schedules a new one
- The `timeout` attribute on the driver is diagnostic and matches the app's schedule

### Driver-Owned State
- The driver maintains all attribute values
- The app should not read or depend on private driver `state` objects
- The app treats the driver's `timeout` attribute only for hub restart recovery

### Non-Metadata Command: `presentActivity()`
- The app calls `presentActivity()` directly on the driver
- This method is intentionally NOT declared as a public metadata command
- This prevents a manual "Present Activity" UI section from appearing on the device page

---

## Implementation Details

### Event Value Normalization
All event values are normalized before filtering:
- Converted to lowercase
- Whitespace trimmed
- Example: Contact sensor value "  OPEN  " becomes "open" for comparison

### Monitored Device Types & Events

| Device Type | Hubitat Capability | Subscribed Event | Trigger Values |
|-------------|-------------------|------------------|-----------------|
| Button | pushableButton | pushed | any value |
| Motion | motionSensor | motion | "active" only |
| Contact | contactSensor | contact | "open" or "closed" |
| Presence | presenceSensor | presence | "present" only |
| Switch | switch | switch | "on" or "off" |
| Lock | lock | lock | "locked" or "unlocked" |
| Garage Door | garageDoorControl | door | "opening", "closing", "open", or "closed" |
| Acceleration | accelerationSensor | acceleration | "active" only |

---

## Special Cases & Gotchas

### Presence Timeout Race Condition
If you manually call `resetPresence()` on a device while a timeout is scheduled:
1. The driver clears its state immediately
2. The app's scheduled `timeoutHandler()` still fires
3. `timeoutHandler()` checks for present sensors and calls `expirePresence()`
4. Since the device is already "not present", this is a harmless no-op

**This is not a bug** — it's expected behavior. The app's timeout is authoritative, not the driver's state.

### Automation-Controlled Switches
If you select switches that are controlled by automations or scenes, those automation-generated switch changes may extend presence. Choose switches carefully — typically only manual activity switches or presence-indicating switches.

### Notification Command ≠ Activity
The `deviceNotification(text)` command intentionally does NOT count as activity. Notifications from automations don't extend presence.

---

## Limitations & Future Enhancements

### Current Limitations
- No support for brightness, temperature, or other continuous attributes as activity triggers
- Timeout extension only checks monitored **presence** sensors, not all device types
- No UI for manually adjusting which monitored devices extend parent presence (all monitored presence sensors extend)

### Potential Enhancements
- Support for window shades, water sensors, thermostats, or music player activity
- Configurable timeout extension rules per device type
- Time-based filtering (e.g., only count activity during certain hours)

---

## Questions or Issues?

Refer back to the Specification.md file for detailed requirements and design rationale, or check the code comments in the driver and app for implementation details.
