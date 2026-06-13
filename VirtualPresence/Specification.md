# Hubitat Virtual Presence Detector Specification

Filename: Virtual_Presence_Detector_Specification.txt

This specification is intended to be complete enough that the code generator should not need additional clarification unless the requirements are impossible, contradictory, or unsupported by Hubitat.

---

# Final Instruction

Generate production-quality, complete, Hubitat Groovy code for a two-part implementation named **Virtual Presence Detector**.

The implementation must include:

1. **Virtual Presence Detector Driver**
2. **Virtual Presence Detector App**

The output must be complete Hubitat Groovy code with:

- no pseudocode
- no omitted sections
- no placeholder methods
- no “left as an exercise”
- no requirement for network access at runtime
- no dependency on unsupported external libraries

Do not ask clarification questions unless the requirements are impossible, contradictory, or unsupported by Hubitat.

Expected answer format:

1. Brief explanation
2. Complete driver code in a Groovy code block
3. Complete app code in a Groovy code block
4. Setup instructions
5. Notes about hierarchy, child-device creation, monitored device types, timeout behavior, and the hidden Present Activity implementation detail

---

# Required Namespace And Author

Both the driver and app must use:

```groovy
namespace: "tgiphil"
author: "phil@thinkedge.com"
```

The driver metadata definition must use:

```groovy
definition(
    name: "Virtual Presence Detector",
    namespace: "tgiphil",
    author: "phil@thinkedge.com"
)
```

The app definition must use:

```groovy
definition(
    name: "Virtual Presence Detector App",
    namespace: "tgiphil",
    author: "phil@thinkedge.com",
    description: "Manages one Virtual Presence Detector device based on selected human-activity events.",
    category: "Convenience",
    singleInstance: false,
    iconUrl: "https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/apps/images/app-64.png",
    iconX2Url: "https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/apps/images/app-128.png"
)
```

Important:

- Some Hubitat versions require `iconUrl` and `iconX2Url` to be present and non-empty in the app `definition` block.
- These icon URLs are metadata only.
- The app must not depend on those URLs at runtime.
- If a code generator wants to avoid external icon dependency entirely, it may use any non-empty valid HTTPS icon URL strings, but the fields must not be empty.

---

# Purpose

The **Virtual Presence Detector** is a virtual Hubitat presence device that represents recent human-triggered activity for a room, area, zone, or whole home.

The virtual device becomes `present` when one or more selected monitored devices produce qualifying human-activity events.

Qualifying activity may include:

- button pushed
- motion active
- contact open
- contact closed
- another presence sensor becoming present
- switch on
- switch off
- lock locked
- lock unlocked
- garage door opening
- garage door closing
- garage door open
- garage door closed
- acceleration active

The virtual device remains `present` until no qualifying activity occurs for the configured timeout period.

The goal is to allow Hubitat automations to depend on simple virtual presence devices instead of complex rules that monitor many individual physical devices.

---

# Example Hierarchy

Room-level detectors:

- Kitchen Presence
- Living Room Presence
- Office Presence
- Bedroom Presence

Area-level detectors:

- Downstairs Presence
- Upstairs Presence

Home-level detector:

- Home Presence

Example aggregation:

```text
Kitchen Presence
Living Room Presence
        ↓
Downstairs Presence
        ↓
Home Presence
```

Higher-level detectors must be able to monitor lower-level virtual presence detectors.

If any child detector is still `present`, the parent should remain `present` and extend its timeout.

---

# Architecture

Use two Hubitat components:

1. **Virtual Presence Detector Driver**
2. **Virtual Presence Detector App**

Each app instance manages exactly one virtual presence detector device.

Each app instance must automatically create one child virtual presence detector device.

The app must always use its automatically created child device as the managed output device.

The app must not support manual output-device selection and must not expose any UI for selecting an existing output virtual presence detector.

---

# Important Design Decision

The app must control timeout scheduling.

The driver must own virtual device state.

Reason:

- the app can see the configured monitored devices
- the app can check whether monitored child presence sensors are still `present`
- the driver should not need to know which devices are monitored
- the app should not depend on private driver `state`
- the app’s scheduled timeout job is authoritative
- the driver’s public `timeout` attribute is diagnostic and should match the app’s schedule as closely as practical

Automatic child-device creation must be supported and is the only supported output-device mode.

Manual virtual-device selection must not be included.

---

# Important Design Requirement: No Manual Present Activity Driver Command UI

The driver must implement a callable method named:

```groovy
presentActivity(deviceName, activityType, activityValue, activityDescription, timeoutSeconds)
```

However, the driver must **not** declare `presentActivity` as a public metadata command.

Specifically, the driver metadata must **not** contain a command declaration like:

```groovy
command "presentActivity", [...]
```

Reason:

- Declaring `presentActivity` as a metadata command causes Hubitat to display a manual device-page command section named **Present Activity**.
- That unwanted section contains fields such as:
  - `DeviceName`
  - `ActivityType`
  - `ActivityValue`
  - `ActivityDescription`
  - `TimeoutSeconds`
  - a **Run** button
- This manual command UI is not wanted.

The app should call the driver method directly if Hubitat permits direct app-to-child-driver method calls.

If a Hubitat firmware version does not allow the app to invoke a child driver method unless it is declared as a public metadata command, the code should fail gracefully with a warning log entry rather than crashing.

The generated answer should mention this Hubitat tradeoff if relevant:

- Hiding `presentActivity` from the device page requires not declaring it as a public command.
- Some Hubitat versions may require declared commands for app-to-device invocation.
- The requested implementation should prioritize removing the manual **Present Activity** command UI.

---

# Important Design Requirement: Use App Instance Name As Activity Device Name

When the app calls:

```groovy
presentActivity(deviceName, activityType, activityValue, activityDescription, timeoutSeconds)
```

The `deviceName` argument must be the **app instance label/name**, not the triggering physical device display name.

Recommended behavior:

```groovy
String deviceName = appLabel()
```

The actual triggering/source device display name may still be preserved in `activityDescription` for diagnostics.

Example:

- Entered Name: `Kitchen`
- App instance label: `Kitchen - Virtual Detector`
- Triggering device: `Kitchen Motion Sensor`
- Event: `motion = active`

Then:

```text
lastActivityDevice = Kitchen - Virtual Detector
lastActivityType = motion
lastActivityValue = active
lastActivityDescription = Kitchen Motion Sensor motion active
```

This lets the virtual device diagnostics identify the managing app instance as the activity device name while still preserving source event context in the description.

---

# Child Device Creation Requirements

The app must automatically create one child virtual presence detector device per app instance.

The app must use:

```groovy
addChildDevice(
    "tgiphil",
    "Virtual Presence Detector",
    dni,
    options
)
```

The generated child device must use the driver:

```text
Virtual Presence Detector
```

with namespace:

```text
tgiphil
```

Important Hubitat limitation:

- The app can create a child device by calling `addChildDevice()`.
- The app cannot create, install, or update driver code.
- The Virtual Presence Detector driver code must be installed before the app attempts to create the child device.
- If the driver is missing or the namespace/name does not match, child-device creation must fail gracefully with a warning log entry.
- The app must not crash if child creation fails.

Recommended child device network ID:

```groovy
"virtual-presence-detector-${app.id}"
```

The app UI must ask the user for a base name for the app instance.

Recommended input name:

```text
name
```

Recommended input label:

```text
Name
```

Recommended input description:

```text
Base name used to label this app instance as {Name} - Virtual Detector.
```

The entered value should be treated as required for app-instance creation.

The app instance label/name must be set to:

```text
{Name} - Virtual Detector
```

The automatically created child virtual presence detector device should use the app instance label when available.

If the app instance label is not available, default the child device label to:

```text
Virtual Presence Detector
```

If the app instance is renamed through this Name setting, the app should also update the child device label to match the app instance label when implemented cleanly.

---

# Managed Output Device

The app’s managed output device is always its automatically created child virtual presence detector device.

The app should internally use a helper concept equivalent to:

```text
getOutputDevice()
```

or:

```text
getManagedOutputDevice()
```

Behavior:

```text
return the automatically created child virtual presence detector device
```

All app logic that controls the output detector must use this managed output device helper.

This includes:

- command/method validation where applicable
- `presentActivity`
- `extendPresence`
- `expirePresence`
- `setTimeoutSeconds`
- self-monitoring protection
- timeout extension checks

The app must not include:

```text
deviceCreationMode
outputDevice
manual mode
auto/manual output-device selection
existing output virtual presence detector device selection
```

---

# Driver Responsibilities

The driver owns:

- virtual device attributes
- presence state
- diagnostic state
- internal timestamp state
- public commands used by the app, except `presentActivity` must remain a non-metadata method to avoid the manual UI section

The driver must not own monitored-device subscriptions.

The driver must not schedule the authoritative timeout.

---

# App Responsibilities

The app owns:

- device selection UI
- automatic child-device creation
- monitored-device subscriptions
- event filtering
- timeout scheduling
- presence-device extension checks at timeout

---

# App Lifecycle: Initialization and Hub Restart

The app must implement the standard Hubitat app lifecycle methods:

## `installed()`

When the app is first installed:

1. Create the automatically managed child virtual presence detector device as described in [Child Device Creation Requirements](#child-device-creation-requirements).
2. Call `updated()` to initialize configuration and subscriptions.

## `updated()`

When the app settings are updated:

1. Unsubscribe from all device events via `unsubscribe()`.
2. Unschedule any ongoing timeout jobs via `unschedule()`.
3. Re-subscribe to monitored devices based on current settings.
4. If the managed output device is currently `present`, reschedule the nearest timeout based on the driver's `timeout` attribute (if available).
5. Otherwise, do not schedule a timeout until the next activity occurs.

## `initialize()`

Called on hub startup:

1. Re-subscribe to all monitored devices according to current settings.
2. Check the managed output device's current `presence` state.
3. If the managed output device is currently `present`:
   - Read the driver's `timeout` attribute (a string timestamp).
   - Calculate remaining time until the timeout fires.
   - If remaining time is greater than 0, schedule the `timeoutHandler` to fire at the appropriate time.
   - If the timeout has already passed, invoke the timeout handler immediately or call `expirePresence()` directly on the managed output device.
4. If the managed output device is currently `not present`, do nothing until the next activity event.

**Rationale:** `runIn()` scheduled jobs do not survive Hubitat hub reboots. Upon restart, the app must restore the timeout schedule based on the driver's recorded `timeout` attribute to avoid detectors remaining stuck in the `present` state indefinitely.

## `uninstalled()`

When the app is uninstalled:

1. Unsubscribe from all device events.
2. Unschedule all scheduled jobs.
3. Delete the automatically created child virtual presence detector device via `deleteChildDevice()` to avoid orphaned devices.

---

# Driver Requirements

## Driver Name

Use:

```text
Virtual Presence Detector
```

Namespace:

```text
tgiphil
```

Author:

```text
phil@thinkedge.com
```

---

# Driver Capabilities

The driver must support:

```groovy
capability "PresenceSensor"
capability "Notification"
capability "Sensor"
```

Optional but recommended:

```groovy
capability "Actuator"
```

The `PresenceSensor` capability provides the standard `presence` attribute with values:

- `present`
- `not present`

The `Notification` capability provides:

```groovy
deviceNotification(text)
```

However, notification commands/events must not count as activity.

---

# Driver Attributes

## Required Attributes

### `presence`

Type:

```text
enum
```

Values:

```text
present
not present
```

Default:

```text
not present
```

Meaning:

Current virtual presence state.

---

### `timeoutSeconds`

Type:

```text
number
```

Unit:

```text
seconds
```

Default:

```text
1800
```

Meaning:

Configured timeout duration in seconds.

---

### `detected`

Type:

```text
string timestamp
```

Default:

```text
null
```

Meaning:

Most recent qualifying activity time.

---

### `timeout`

Type:

```text
string timestamp
```

Default:

```text
null
```

Meaning:

Current scheduled expiration time.

---

### `expired`

Type:

```text
string timestamp
```

Default:

```text
null
```

Meaning:

Most recent time this detector changed to `not present` because of timeout.

---

# Driver Diagnostic Attributes

Implement these diagnostic attributes.

## `lastActivityDevice`

Type:

```text
string
```

Meaning:

The app instance label/name that most recently triggered activity handling.

Important:

- This must be the app instance label/name, not the triggering physical device display name.
- The triggering/source device display name may be included in `lastActivityDescription`.

---

## `lastActivityType`

Type:

```text
string
```

Meaning:

Event name that most recently triggered activity.

Examples:

```text
pushed
motion
contact
presence
switch
lock
door
acceleration
```

---

## `lastActivityValue`

Type:

```text
string
```

Meaning:

Original event value that most recently triggered activity.

Examples:

```text
1
active
open
closed
present
on
off
locked
unlocked
opening
closing
```

Important:

Preserve the original human-readable event value where possible.

Use normalized values only for comparison logic, not for this diagnostic attribute.

---

## `lastActivityDescription`

Type:

```text
string
```

Meaning:

Event description text from the most recent qualifying activity.

Recommended:

- Preserve the original event `descriptionText` when available.
- If no event description exists, build one that includes the triggering/source device display name, event name, and event value.

---

# Driver Internal State

The driver should maintain internal epoch timestamp values.

Recommended internal state:

```groovy
state.detectedEpochMs
state.timeoutEpochMs
state.expiredEpochMs
```

The public string timestamp attributes are for visibility and diagnostics.

The internal epoch values are for reliable timestamp storage.

Important Hubitat constraint:

- the app should not depend on reading private driver `state`
- the app’s scheduled timeout job should be treated as authoritative
- driver epoch state is still useful for diagnostics and internal consistency

---

# Driver Default Values

On installation, initialize:

```text
presence = not present
timeoutSeconds = 1800
detected = null
timeout = null
expired = null
lastActivityDevice = ""
lastActivityType = ""
lastActivityValue = ""
lastActivityDescription = ""
```

On update:

- initialize only missing defaults
- do not unnecessarily erase existing diagnostic values
- do not reset presence unless missing/null
- do not clear existing detected/timeout/expired values unless explicitly resetting

---

# Driver Lifecycle: Initialization on Hub Restart

The driver must implement the standard Hubitat device lifecycle methods:

## `installed()`

When the driver device is first created:

1. Initialize all attributes to their default values as listed in [Driver Default Values](#driver-default-values).

## `updated()`

When the driver is updated (e.g., after code changes):

1. Initialize only missing/null attribute values; do not reset existing state.
2. Preserve existing diagnostic attributes and timestamps.

## `initialize()`

Called on hub startup:

1. No action required.
2. The driver's internal state and attributes persist across reboots via Hubitat's device state storage.
3. However, the driver's `initialize()` method may be used for optional debug logging or health checks if desired.

**Note:** The app (not the driver) is responsible for rescheduling timeout jobs upon hub restart, as documented in [App Lifecycle: Initialization and Hub Restart](#app-lifecycle-initialization-and-hub-restart).

---

# Driver Commands And Methods

The driver must expose the following public metadata commands:

```text
setTimeoutSeconds(timeoutSeconds)
extendPresence(timeoutSeconds)
expirePresence(reason)
resetPresence()
deviceNotification(text)
```

The driver must implement the following callable method but must **not** declare it as a public metadata command:

```text
presentActivity(deviceName, activityType, activityValue, activityDescription, timeoutSeconds)
```

Rationale:

- `presentActivity` is intended for app-to-driver use only.
- It should not create a manual device-page command section.

---

## `setTimeoutSeconds(timeoutSeconds)`

Purpose:

Set the public `timeoutSeconds` attribute. This command is primarily for **manual** adjustments from the device page and testing.

**Important:** The app does **not** need to call `setTimeoutSeconds()` separately. The app passes `timeoutSeconds` through `presentActivity()` and `extendPresence()`, which update the attribute internally. The app should rely on those methods, not `setTimeoutSeconds().`

Behavior:

- accept numeric input
- coerce invalid or missing values to `1800`
- enforce minimum value of `1`
- send event:

```groovy
sendEvent(
    name: "timeoutSeconds",
    value: seconds,
    unit: "seconds"
)
```

---

## `presentActivity(deviceName, activityType, activityValue, activityDescription, timeoutSeconds)`

Purpose:

Handle a qualifying human-activity event.

Important:

- This must be implemented as a driver method.
- This must **not** be declared as a public metadata command.
- The app calls this method directly if supported by Hubitat.
- The app must pass the app instance label/name as `deviceName`.

Inputs:

```text
deviceName
activityType
activityValue
activityDescription
timeoutSeconds
```

Behavior:

1. Read current `presence`.

   If null or unavailable, treat as:

   ```text
   not present
   ```

2. Set `now` to current hub time.

3. Set `detected` to `now`.

4. Set `timeout` to:

   ```text
   now + timeoutSeconds
   ```

5. Update internal state:

   ```groovy
   state.detectedEpochMs = now epoch milliseconds
   state.timeoutEpochMs = timeout epoch milliseconds
   ```

6. Update public attributes:

   ```text
   detected
   timeout
   timeoutSeconds
   ```

7. Update diagnostic attributes:

   ```text
   lastActivityDevice
   lastActivityType
   lastActivityValue
   lastActivityDescription
   ```

8. If current presence is not `present`, emit one presence event:

   ```groovy
   name: "presence"
   value: "present"
   descriptionText: activityDescription if available
   ```

9. If current presence is already `present`:

   - do not emit duplicate `presence = present`
   - still update `detected`
   - still update `timeout`
   - still update `timeoutSeconds`
   - still update internal state
   - still update diagnostic attributes

---

## `extendPresence(timeoutSeconds)`

Purpose:

Extend the virtual presence detector when timeout occurs but one or more monitored presence devices are still `present`.

Behavior:

1. Set `now` to current hub time.

2. Set new timeout to:

   ```text
   now + timeoutSeconds
   ```

3. Update:

   ```text
   timeout
   timeoutSeconds
   state.timeoutEpochMs
   ```

4. Presence remains `present`.

5. Do not update `expired`.

6. Do not emit `presence = not present`.

7. Prefer not to update `detected`, unless explicitly documented in code comments.

8. If presence is unexpectedly not `present`, recommended behavior is to set it to `present`, because the app calls `extendPresence()` only when preserving active parent presence.

---

## `expirePresence(reason)`

Purpose:

Expire the virtual presence detector after timeout if no monitored presence sensors are still `present`.

Behavior:

1. If current presence is already `not present`, do nothing.

2. Set `now` to current hub time.

3. Set:

   ```groovy
   state.expiredEpochMs = now epoch milliseconds
   ```

4. Update public attribute:

   ```text
   expired = now timestamp
   ```

5. Emit presence event:

   ```groovy
   name: "presence"
   value: "not present"
   descriptionText: reason or "timeout"
   ```

6. Do not clear `detected` or `timeout` on normal expiration.

Reason:

Keeping those values visible helps diagnostics.

---

## `resetPresence()`

Purpose:

Manual reset command for testing and troubleshooting.

Behavior:

- set `presence = not present`
- clear `detected`
- clear `timeout`
- clear `expired`
- clear diagnostic attributes
- clear internal epoch state

**Important Race Condition Warning:**

`resetPresence()` clears driver state immediately, but the app's `runIn()` scheduled timeout job runs independently. If `resetPresence()` is called manually while a timeout is scheduled:

1. The driver state is cleared.
2. The app's scheduled `timeoutHandler` may still fire and call `expirePresence()` on an already-reset device.
3. This is not incorrect behavior — `expirePresence()` called on a device already `not present` is a no-op (per its specification).

However, if stricter control is desired, the app may optionally provide a wrapper method that calls both `resetPresence()` and `unschedule()` on the managed output device to prevent the timer from firing.

---

## `deviceNotification(text)`

Purpose:

Implement the `Notification` capability command.

Important:

`deviceNotification(text)` must not count as activity.

Calling `deviceNotification(text)` must not:

- update `detected`
- update `timeout`
- update `expired`
- update activity diagnostics
- change `presence`
- schedule or affect timeout

It may log the notification text if debug logging is enabled.

Rationale:

Notifications may be generated by automations or apps and are not necessarily human-triggered activity.

---

# App Requirements

## App Name

Use:

```text
Virtual Presence Detector App
```

Namespace:

```text
tgiphil
```

Author:

```text
phil@thinkedge.com
```

The app must allow multiple instances:

```groovy
singleInstance: false
```

The app `definition` block must include non-empty `iconUrl` and `iconX2Url`.

---

# App Configuration UI

Each app instance must allow the user to configure the following.

---

## 1. Name

Input name:

```text
name
```

Type:

```text
text
```

Required:

```text
true
```

Meaning:

Base name used to label the app instance.

The app instance label/name must be set to:

```text
{Name} - Virtual Detector
```

The automatically created child virtual presence detector device should use the app instance label when available.

If the app instance label is not available, default the child device label to:

```text
Virtual Presence Detector
```

The UI should clearly indicate that this Name value controls the app instance label.

The app must not ask for an existing output virtual presence detector device.

---

## 2. Presence Timeout

Input name:

```text
presenceTimeoutSeconds
```

Type:

```text
number
```

Default:

```text
1800
```

Minimum effective value:

```text
1
```

Invalid values should fall back to:

```text
1800
```

---

## 3. Button Devices To Monitor

Input should allow multiple devices supporting:

```text
capability.pushableButton
```

Subscribe to:

```text
pushed
```

---

## 4. Motion Sensors To Monitor

Input should allow multiple devices supporting:

```text
capability.motionSensor
```

Subscribe to:

```text
motion
```

---

## 5. Contact Sensors To Monitor

Input should allow multiple devices supporting:

```text
capability.contactSensor
```

Subscribe to:

```text
contact
```

---

## 6. Presence Sensors To Monitor

Input should allow multiple devices supporting:

```text
capability.presenceSensor
```

Subscribe to:

```text
presence
```

This should support both:

- real presence devices
- other virtual presence detector devices

This enables hierarchical aggregation.

Important:

The app should avoid self-monitoring loops.

If the managed output virtual presence detector is also selected as one of its own monitored presence sensors:

- ignore it during event handling
- ignore it during timeout extension checks
- optionally log a warning

Rationale:

A detector monitoring itself could prevent expiration or create confusing behavior.

---

## 7. Switch Devices To Monitor

Input should allow multiple devices supporting:

```text
capability.switch
```

Subscribe to:

```text
switch
```

---

## 8. Lock Devices To Monitor

Input should allow multiple devices supporting:

```text
capability.lock
```

Subscribe to:

```text
lock
```

---

## 9. Garage Door Devices To Monitor

Input should allow multiple devices supporting:

```text
capability.garageDoorControl
```

Subscribe to:

```text
door
```

The implementation should tolerate selected devices that do not produce expected `door` events.

---

## 10. Acceleration Devices To Monitor

Input should allow multiple devices supporting:

```text
capability.accelerationSensor
```

Subscribe to:

```text
acceleration
```

The `acceleration` attribute values expected by the app are:

```text
active
inactive
```

---

## 11. Debug Logging

Input name:

```text
enableDebugLogging
```

Type:

```text
bool
```

Default:

```text
false
```

Debug logs should be helpful but not excessive.

Logs should include the app label when available.

---

# Explicitly Removed App UI Features

The app must not include generic status monitoring.

The app must not ask for:

```text
statusDevices
statusTriggerValues
additionalStatusTriggerValues
unknownStatusBehavior
```

The app must not include:

```text
deviceCreationMode
outputDevice
manual output-device mode
existing output virtual presence detector selection
```

The app must not include monitor sections for:

```text
Window Shades / Blinds
Water Sensors
Thermostats
Music Players / Speakers
```

Unknown or unsupported event values must be ignored by default.

No user-facing setting should allow unknown event values to trigger activity.

---

# Event Sources And Trigger Rules

The app subscribes to selected monitored devices and filters events according to the rules below.

Only qualifying activity events call:

```groovy
onPresentActivity(evt)
```

---

# Button Events

Event name:

```text
pushed
```

Rule:

Any `pushed` event counts as activity.

Behavior:

```text
pushed = any value -> trigger onPresentActivity(evt)
```

User-facing descriptions may refer to this as a button press.

---

# Motion Events

Event name:

```text
motion
```

Rules:

```text
motion = active -> trigger onPresentActivity(evt)
motion = inactive -> ignore
other values -> ignore
```

---

# Contact Events

Event name:

```text
contact
```

Rules:

```text
contact = open -> trigger onPresentActivity(evt)
contact = closed -> trigger onPresentActivity(evt)
other values -> ignore
```

Rationale:

Both opening and closing a contact sensor can represent human-triggered activity, such as:

- entering a room
- closing a door
- opening a cabinet
- closing a closet
- interacting with an area

---

# Presence Events

Event name:

```text
presence
```

Rules:

```text
presence = present -> trigger onPresentActivity(evt)
presence = not present -> ignore
other values -> ignore
```

Rationale:

This allows room-level virtual presence devices to aggregate into area-level or home-level virtual presence devices.

A monitored device becoming `not present` should not immediately force the parent virtual presence detector to become `not present`.

The parent should expire according to its own timeout logic.

Self-monitoring rule:

```text
If evt.device is the same as the managed output device, ignore it.
```

---

# Switch Events

Event name:

```text
switch
```

Rules:

```text
switch = on -> trigger onPresentActivity(evt)
switch = off -> trigger onPresentActivity(evt)
other values -> ignore
```

Rationale:

Toggling a selected switch may represent human interaction with a room or area.

Important:

If automation-controlled switches are selected, automation-generated switch changes may extend presence.

Users should select only switches that are appropriate activity indicators.

---

# Lock Events

Event name:

```text
lock
```

Rules:

```text
lock = locked -> trigger onPresentActivity(evt)
lock = unlocked -> trigger onPresentActivity(evt)
other values -> ignore
```

Rationale:

Locking or unlocking a selected lock may represent human interaction with an entry, room, area, or home.

---

# Garage Door Events

Event name:

```text
door
```

Rules:

```text
door = opening -> trigger onPresentActivity(evt)
door = closing -> trigger onPresentActivity(evt)
door = open -> trigger onPresentActivity(evt)
door = closed -> trigger onPresentActivity(evt)
other values -> ignore
```

Rationale:

Garage door movement or state changes can indicate human arrival, departure, or interaction with the home.

---

# Acceleration Events

Event name:

```text
acceleration
```

Rules:

```text
acceleration = active -> trigger onPresentActivity(evt)
acceleration = inactive -> ignore
other values -> ignore
```

Rationale:

Acceleration sensors are often used to represent movement or vibration. They may be useful for detecting interaction with objects such as mailboxes, drawers, doors, appliances, or other items.

Important:

Not all acceleration events are human-triggered. Users should select only acceleration sensors that are appropriate activity indicators.

---

# Event Value Normalization

Event value comparison must be:

- case-insensitive
- leading/trailing whitespace-insensitive

Use normalization equivalent to:

```groovy
value?.toString()?.trim()?.toLowerCase()
```

Important:

- Use normalized values only for comparison.
- Preserve the original event value for `lastActivityValue`.
- Unknown or unsupported event values are ignored by default.

---

# App Event Handling

When a qualifying activity event occurs, the app must:

1. Determine effective timeout seconds.
2. Build activity details from the event:
   - `deviceName`: app instance label/name
   - `activityType`: event name
   - `activityValue`: original event value
   - `activityDescription`: event description text, or a generated description that includes the triggering/source device display name
3. Call the managed output device driver method:

```groovy
presentActivity(
    appLabel(),
    activityType,
    activityValue,
    activityDescription,
    timeoutSeconds
)
```

4. Unschedule any previously scheduled timeout handler.
5. Schedule a new timeout handler using:

```groovy
runIn(timeoutSeconds, "timeoutHandler")
```

The app’s scheduled timeout is authoritative.

---

# App Timeout Handling

When the scheduled timeout fires, the app must:

1. Check all monitored presence sensors.
2. Ignore the managed output device if it appears in the monitored presence sensor list.
3. If any monitored presence sensor is currently:

```text
present
```

then:

- call:

```groovy
extendPresence(timeoutSeconds)
```

- schedule another timeout using:

```groovy
runIn(timeoutSeconds, "timeoutHandler")
```

4. If no monitored presence sensor is currently `present`, call:

```groovy
expirePresence("timeout")
```

Important:

- A child virtual presence detector remaining `present` should keep its parent detector `present`.
- A child virtual presence detector becoming `not present` should not immediately make the parent `not present`.
- Parent expiration is controlled only by the parent app’s timeout logic.

---

# Required App Helper Behavior

The app should include helper methods or equivalent logic for:

```text
ensureOutputDevice()
getOutputDevice()
getAutoChildDevice()
getAutoChildDeviceNetworkId()
validateOutputDevice()
safeCallOutputDevice()
safeCallDriverMethod() or equivalent for non-metadata presentActivity
scheduleTimeout()
hasMonitoredPresenceStillPresent()
isSelfOutputDevice()
deviceHasCommand()
effectiveTimeoutSeconds()
normalizeTimeoutSeconds()
normalizeValue()
safeString()
appLabel()
logDebug()
logInfo()
```

Exact method names may vary, but the behavior must be implemented.

The app should not include helpers related to generic status trigger values or manual output-device mode.

---

# Output Device Command And Method Validation

The app should validate that the managed output child device supports these public metadata commands where introspection is available:

```text
extendPresence
expirePresence
setTimeoutSeconds
```

The app should **not** require `presentActivity` to appear in `supportedCommands`, because `presentActivity` must not be declared as a public metadata command.

The app should still attempt to call `presentActivity` as a driver method and catch failures gracefully.

If a public command appears to be missing:

- log a warning
- do not crash
- continue operating as much as possible

