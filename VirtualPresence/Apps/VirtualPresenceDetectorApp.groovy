/**
 * Virtual Presence Detector App
 *
 * Namespace: tgiphil
 * Author: phil@thinkedge.com
 *
 * Manages one Virtual Presence Detector device based on selected human-activity events.
 * Automatically creates and manages a child virtual presence detector device.
 * Monitors multiple physical device types and aggregates qualifying activity events.
 */

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

preferences {
	page(name: "mainPage", title: "Virtual Presence Detector Configuration", install: true, uninstall: true) {
		section("App Name") {
			input name: "name", type: "text", title: "Name", required: true, description: "Base name used to label this app instance as {Name} - Virtual Detector"
		}
		section("Timeout Settings") {
			input name: "presenceTimeoutSeconds", type: "number", title: "Presence Timeout (seconds)", required: false, defaultValue: 1800
		}
		section("Button Devices") {
			input name: "buttonDevices", type: "capability.pushableButton", title: "Button devices to monitor", required: false, multiple: true
		}
		section("Motion Sensors") {
			input name: "motionDevices", type: "capability.motionSensor", title: "Motion sensors to monitor", required: false, multiple: true
		}
		section("Contact Sensors") {
			input name: "contactDevices", type: "capability.contactSensor", title: "Contact sensors to monitor", required: false, multiple: true
		}
		section("Presence Sensors") {
			input name: "presenceDevices", type: "capability.presenceSensor", title: "Presence sensors to monitor (enables hierarchy)", required: false, multiple: true
		}
		section("Switch Devices") {
			input name: "switchDevices", type: "capability.switch", title: "Switch devices to monitor", required: false, multiple: true
		}
		section("Lock Devices") {
			input name: "lockDevices", type: "capability.lock", title: "Lock devices to monitor", required: false, multiple: true
		}
		section("Garage Door Devices") {
			input name: "garageDoorDevices", type: "capability.garageDoorControl", title: "Garage door devices to monitor", required: false, multiple: true
		}
		section("Acceleration Sensors") {
			input name: "accelerationDevices", type: "capability.accelerationSensor", title: "Acceleration sensors to monitor", required: false, multiple: true
		}
		section("Debug Logging") {
			input name: "enableDebugLogging", type: "bool", title: "Enable debug logging", required: false, defaultValue: false
		}
	}
}

// Called when app is first installed
def installed() {
	logDebug("installed()")
	ensureOutputDevice()
	updated()
}

// Called when settings are changed
def updated() {
	ensureAppLabel()
	logDebug("updated()")

	// Unsubscribe from all events
	unsubscribe()

	// Unschedule any pending timeout
	unschedule()

	// Ensure the managed output device exists
	ensureOutputDevice()

	// Subscribe to all monitored devices
	subscribeToMonitoredDevices()

	// If output device is currently present, reschedule timeout based on its timeout attribute
	def outputDevice = getOutputDevice()
	if (outputDevice && outputDevice.currentValue("presence") == "present") {
		scheduleTimeoutFromDeviceState(outputDevice)
	}

	logDebug("updated() complete")
}

// Called on hub startup
def initialize() {
	logDebug("initialize()")

	// Re-subscribe to all monitored devices
	unsubscribe()
	subscribeToMonitoredDevices()

	// Check if output device is present and reschedule timeout
	def outputDevice = getOutputDevice()
	if (outputDevice && outputDevice.currentValue("presence") == "present") {
		scheduleTimeoutFromDeviceState(outputDevice)
	}

	logDebug("initialize() complete")
}

// Called when app is uninstalled
def uninstalled() {
	logDebug("uninstalled()")

	// Unsubscribe and unschedule
	unsubscribe()
	unschedule()

	// Delete the managed child device
	def outputDevice = getOutputDevice()
	if (outputDevice) {
		logDebug("Deleting managed child device: ${outputDevice.displayName}")
		deleteChildDevice(outputDevice.deviceNetworkId)
	}
}

// ===== DEVICE MANAGEMENT =====

/**
 * Ensure the managed output device exists; create if necessary.
 * Returns the device object if successful, null if creation failed.
 */
def ensureOutputDevice() {
	def existingDevice = getOutputDevice()
	String labelForDevice = managedDeviceLabel()

	if (existingDevice) {
		logDebug("Managed output device already exists: ${existingDevice.displayName}")

		if (existingDevice.label != labelForDevice) {
			existingDevice.label = labelForDevice
			logDebug("Updated device label to: ${labelForDevice}")
		}

		validateOutputDevice(existingDevice)
		return existingDevice
	}

	// Create the child device
	logDebug("Creating managed child device...")

	String dni = "virtual-presence-detector-${app.id}"
	Map options = [label: labelForDevice, isComponent: false]

	try {
		def newDevice = addChildDevice("tgiphil", "Virtual Presence Detector", dni, options)
		logDebug("Successfully created child device: ${newDevice.displayName} (${dni})")
		validateOutputDevice(newDevice)
		return newDevice
	} catch (Exception e) {
		logError("Failed to create child device: ${e.message}")
		return null
	}
}

/**
 * Get the managed output virtual presence detector device.
 * Returns null if not found.
 */
def getOutputDevice() {
	String dni = "virtual-presence-detector-${app.id}"
	return getChildDevice(dni)
}

/**
 * Check if a device is the managed output device (for self-monitoring protection).
 */
Boolean isSelfOutputDevice(def device) {
	if (!device) return false
	def outputDevice = getOutputDevice()
	return outputDevice && device.id == outputDevice.id
}

/**
 * Validate that the managed output device exposes the expected public commands.
 * Logs a warning for any missing command; does not crash.
 */
def validateOutputDevice(def outputDevice) {
	if (!outputDevice) return
	List requiredCommands = ["extendPresence", "expirePresence", "setTimeoutSeconds"]
	for (String cmd in requiredCommands) {
		if (!outputDevice.hasCommand(cmd)) {
			log.warn("[${appLabel()}] Managed output device '${outputDevice.displayName}' is missing expected command: ${cmd}")
		}
	}
}

// ===== EVENT SUBSCRIPTIONS =====

/**
 * Subscribe to all monitored devices based on settings.
 */
def subscribeToMonitoredDevices() {
	logDebug("Subscribing to monitored devices...")

	if (buttonDevices) {
		subscribe(buttonDevices, "pushed", onButtonPushed)
		logDebug("Subscribed to ${buttonDevices.size()} button device(s)")
	}

	if (motionDevices) {
		subscribe(motionDevices, "motion", onMotionEvent)
		logDebug("Subscribed to ${motionDevices.size()} motion sensor(s)")
	}

	if (contactDevices) {
		subscribe(contactDevices, "contact", onContactEvent)
		logDebug("Subscribed to ${contactDevices.size()} contact sensor(s)")
	}

	if (presenceDevices) {
		subscribe(presenceDevices, "presence", onPresenceEvent)
		logDebug("Subscribed to ${presenceDevices.size()} presence sensor(s)")
	}

	if (switchDevices) {
		subscribe(switchDevices, "switch", onSwitchEvent)
		logDebug("Subscribed to ${switchDevices.size()} switch device(s)")
	}

	if (lockDevices) {
		subscribe(lockDevices, "lock", onLockEvent)
		logDebug("Subscribed to ${lockDevices.size()} lock device(s)")
	}

	if (garageDoorDevices) {
		subscribe(garageDoorDevices, "door", onGarageDoorEvent)
		logDebug("Subscribed to ${garageDoorDevices.size()} garage door device(s)")
	}

	if (accelerationDevices) {
		subscribe(accelerationDevices, "acceleration", onAccelerationEvent)
		logDebug("Subscribed to ${accelerationDevices.size()} acceleration sensor(s)")
	}
}

// ===== EVENT HANDLERS =====

def onButtonPushed(evt) {
	String deviceName = evt.device.displayName ?: "Button"
	String activityType = "pushed"
	String activityValue = evt.value
	String activityDescription = evt.descriptionText ?: "${deviceName} ${activityType} ${activityValue}"

	logDebug("Button ${deviceName} pushed (value=${activityValue})")
	onPresentActivity(activityType, activityValue, activityDescription)
}

def onMotionEvent(evt) {
	String normalizedValue = normalizeValue(evt.value)

	// Only active motion counts
	if (normalizedValue != "active") {
		logDebug("Motion ${evt.device.displayName}: ${evt.value} (ignored, not active)")
		return
	}

	String deviceName = evt.device.displayName ?: "Motion Sensor"
	String activityType = "motion"
	String activityValue = evt.value
	String activityDescription = evt.descriptionText ?: "${deviceName} motion ${activityValue}"

	logDebug("Motion ${deviceName} active")
	onPresentActivity(activityType, activityValue, activityDescription)
}

def onContactEvent(evt) {
	String normalizedValue = normalizeValue(evt.value)

	// Both open and closed count
	if (normalizedValue != "open" && normalizedValue != "closed") {
		logDebug("Contact ${evt.device.displayName}: ${evt.value} (ignored, unknown value)")
		return
	}

	String deviceName = evt.device.displayName ?: "Contact Sensor"
	String activityType = "contact"
	String activityValue = evt.value
	String activityDescription = evt.descriptionText ?: "${deviceName} contact ${activityValue}"

	logDebug("Contact ${deviceName} ${activityValue}")
	onPresentActivity(activityType, activityValue, activityDescription)
}

def onPresenceEvent(evt) {
	String normalizedValue = normalizeValue(evt.value)

	// Only 'present' counts; 'not present' is ignored
	if (normalizedValue != "present") {
		logDebug("Presence ${evt.device.displayName}: ${evt.value} (ignored, not present)")
		return
	}

	// Self-monitoring protection: ignore if this event is from the managed output device
	if (isSelfOutputDevice(evt.device)) {
		logDebug("Ignoring presence event from managed output device (self-monitoring protection)")
		return
	}

	String deviceName = evt.device.displayName ?: "Presence Sensor"
	String activityType = "presence"
	String activityValue = evt.value
	String activityDescription = evt.descriptionText ?: "${deviceName} presence ${activityValue}"

	logDebug("Presence ${deviceName} present")
	onPresentActivity(activityType, activityValue, activityDescription)
}

def onSwitchEvent(evt) {
	String normalizedValue = normalizeValue(evt.value)

	// Both on and off count
	if (normalizedValue != "on" && normalizedValue != "off") {
		logDebug("Switch ${evt.device.displayName}: ${evt.value} (ignored, unknown value)")
		return
	}

	String deviceName = evt.device.displayName ?: "Switch"
	String activityType = "switch"
	String activityValue = evt.value
	String activityDescription = evt.descriptionText ?: "${deviceName} switch ${activityValue}"

	logDebug("Switch ${deviceName} ${activityValue}")
	onPresentActivity(activityType, activityValue, activityDescription)
}

def onLockEvent(evt) {
	String normalizedValue = normalizeValue(evt.value)

	// Both locked and unlocked count
	if (normalizedValue != "locked" && normalizedValue != "unlocked") {
		logDebug("Lock ${evt.device.displayName}: ${evt.value} (ignored, unknown value)")
		return
	}

	String deviceName = evt.device.displayName ?: "Lock"
	String activityType = "lock"
	String activityValue = evt.value
	String activityDescription = evt.descriptionText ?: "${deviceName} lock ${activityValue}"

	logDebug("Lock ${deviceName} ${activityValue}")
	onPresentActivity(activityType, activityValue, activityDescription)
}

def onGarageDoorEvent(evt) {
	String normalizedValue = normalizeValue(evt.value)

	// opening, closing, open, and closed all count
	List validValues = ["opening", "closing", "open", "closed"]
	if (!validValues.contains(normalizedValue)) {
		logDebug("Garage door ${evt.device.displayName}: ${evt.value} (ignored, unknown value)")
		return
	}

	String deviceName = evt.device.displayName ?: "Garage Door"
	String activityType = "door"
	String activityValue = evt.value
	String activityDescription = evt.descriptionText ?: "${deviceName} door ${activityValue}"

	logDebug("Garage door ${deviceName} ${activityValue}")
	onPresentActivity(activityType, activityValue, activityDescription)
}

def onAccelerationEvent(evt) {
	String normalizedValue = normalizeValue(evt.value)

	// Only active counts
	if (normalizedValue != "active") {
		logDebug("Acceleration ${evt.device.displayName}: ${evt.value} (ignored, not active)")
		return
	}

	String deviceName = evt.device.displayName ?: "Acceleration Sensor"
	String activityType = "acceleration"
	String activityValue = evt.value
	String activityDescription = evt.descriptionText ?: "${deviceName} acceleration ${activityValue}"

	logDebug("Acceleration ${deviceName} active")
	onPresentActivity(activityType, activityValue, activityDescription)
}

/**
 * Central handler for all qualifying activity events.
 * Called by all event handlers after filtering.
 */
def onPresentActivity(String activityType, String activityValue, String activityDescription) {
	logDebug("onPresentActivity: type=${activityType}, value=${activityValue}, desc=${activityDescription}")

	def outputDevice = getOutputDevice()
	if (!outputDevice) {
		logError("Managed output device not found; cannot process activity")
		return
	}

	Number effectiveTimeout = effectiveTimeoutSeconds()
	String appLabel = appLabel()

	// Call presentActivity on the driver (non-metadata method call)
	try {
		outputDevice.presentActivity(appLabel, activityType, activityValue, activityDescription, effectiveTimeout)
	} catch (Exception e) {
		logError("Failed to call presentActivity on driver: ${e.message}")
		return
	}

	// Unschedule any previous timeout
	unschedule("timeoutHandler")

	// Schedule new timeout
	runIn(effectiveTimeout as Integer, "timeoutHandler")
	logDebug("Scheduled timeout handler for ${effectiveTimeout}s")
}

/**
 * Timeout handler: called when the timeout expires.
 * Checks if any monitored presence sensors are still present.
 * If yes, extends presence; if no, expires presence.
 */
def timeoutHandler() {
	logDebug("timeoutHandler() fired")

	def outputDevice = getOutputDevice()
	if (!outputDevice) {
		logError("Managed output device not found; cannot handle timeout")
		return
	}

	Boolean anyPresenceSensorStillPresent = hasMonitoredPresenceStillPresent()

	if (anyPresenceSensorStillPresent) {
		logDebug("At least one monitored presence sensor is still present; extending presence")
		Number effectiveTimeout = effectiveTimeoutSeconds()

		try {
			outputDevice.extendPresence(effectiveTimeout)
		} catch (Exception e) {
			logError("Failed to call extendPresence on driver: ${e.message}")
			return
		}

		// Schedule another timeout
		runIn(effectiveTimeout as Integer, "timeoutHandler")
		logDebug("Extended presence, scheduled another timeout for ${effectiveTimeout}s")
	} else {
		logDebug("No monitored presence sensors still present; expiring presence")

		try {
			outputDevice.expirePresence("timeout")
		} catch (Exception e) {
			logError("Failed to call expirePresence on driver: ${e.message}")
		}
	}
}

// ===== HELPER METHODS =====

/**
 * Check if any monitored presence sensor is currently present.
 * Excludes the managed output device to prevent self-monitoring loop.
 */
Boolean hasMonitoredPresenceStillPresent() {
	if (!presenceDevices || presenceDevices.size() == 0) {
		return false
	}

	def outputDevice = getOutputDevice()

	for (def device in presenceDevices) {
		// Skip self-monitoring
		if (outputDevice && device.id == outputDevice.id) {
			logDebug("Skipping self-output device during presence check: ${device.displayName}")
			continue
		}

		if (device.currentValue("presence") == "present") {
			logDebug("Found monitored presence sensor still present: ${device.displayName}")
			return true
		}
	}

	return false
}

/**
 * Get the effective timeout in seconds.
 * Validates and coerces the presenceTimeoutSeconds setting.
 */
Number effectiveTimeoutSeconds() {
	return normalizeTimeoutSeconds(settings.presenceTimeoutSeconds)
}

/**
 * Normalize timeout seconds: coerce to number, enforce minimum of 1, default to 1800 on invalid.
 */
Number normalizeTimeoutSeconds(Object value) {
	try {
		Number n = value as Number
		return n < 1 ? 1800 : n
	} catch (Exception e) {
		return 1800
	}
}

/**
 * Format the configured app name into the required app label.
 */
String desiredAppLabel() {
	String configuredName = settings["name"]?.toString()?.trim()
	return configuredName ? "${configuredName} - Virtual Detector" : null
}

/**
 * Ensure the app instance label matches the configured Name.
 */
def ensureAppLabel() {
	String desiredLabel = desiredAppLabel()
	if (!desiredLabel) {
		return
	}

	if (app.getLabel() != desiredLabel) {
		app.updateLabel(desiredLabel)
	}
}

/**
 * Resolve the managed child device label.
 */
String managedDeviceLabel() {
	return appLabel()
}

/**
 * Normalize event values: lowercase, trim whitespace.
 */
String normalizeValue(Object value) {
	return value?.toString()?.trim()?.toLowerCase() ?: ""
}

/**
 * Get app label for diagnostic purposes.
 */
String appLabel() {
	return app.getLabel() ?: desiredAppLabel() ?: "Virtual Presence Detector"
}

/**
 * Schedule timeout based on the driver's recorded timeout attribute.
 * Used during initialize() for hub restart recovery.
 */
def scheduleTimeoutFromDeviceState(def outputDevice) {
	if (!outputDevice) return

	String timeoutAttrValue = outputDevice.currentValue("timeout")
	if (!timeoutAttrValue) {
		logDebug("No timeout attribute found on output device; not scheduling")
		return
	}

	try {
		Date timeoutDate = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", timeoutAttrValue)
		Date now = new Date()
		long remainingMs = timeoutDate.time - now.time

		if (remainingMs <= 0) {
			logDebug("Recorded timeout has already passed; invoking timeout handler immediately")
			timeoutHandler()
		} else {
			long remainingSecs = Math.ceil(remainingMs / 1000.0)
			logDebug("Scheduling timeout handler for ${remainingSecs}s (recovered from hub restart)")
			runIn(remainingSecs as Integer, "timeoutHandler")
		}
	} catch (Exception e) {
		logDebug("Failed to parse timeout attribute: ${e.message}")
	}
}

/**
 * Debug logging helper.
 */
void logDebug(String msg) {
	if (enableDebugLogging) {
		log.debug("[${app.getLabel() ?: 'App'}] ${msg}")
	}
}

/**
 * Error logging helper.
 */
void logError(String msg) {
	log.error("[${app.getLabel() ?: 'App'}] ${msg}")
}
