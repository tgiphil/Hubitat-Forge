/**
 * Virtual Presence Detector Driver
 *
 * Namespace: tgiphil
 * Author: phil@thinkedge.com
 *
 * A virtual device that represents recent human-triggered activity for a room,
 * area, zone, or whole home. Becomes 'present' when activity occurs and remains
 * 'present' until no qualifying activity occurs for the configured timeout period.
 */

metadata {
	definition(
		name: "Virtual Presence Detector",
		namespace: "tgiphil",
		author: "phil@thinkedge.com"
	) {
		capability "PresenceSensor"
		capability "Notification"
		capability "Sensor"
		capability "Actuator"

		// Public metadata commands
		command "setTimeoutSeconds", [[name: "timeoutSeconds", type: "NUMBER", description: "Timeout in seconds"]]
		command "extendPresence", [[name: "timeoutSeconds", type: "NUMBER", description: "Timeout in seconds"]]
		command "expirePresence", [[name: "reason", type: "STRING", description: "Reason for expiration"]]
		command "resetPresence"

		// Attributes
		attribute "presence", "enum", ["present", "not present"]
		attribute "timeoutSeconds", "number"
		attribute "detected", "string"
		attribute "timeout", "string"
		attribute "expired", "string"
		attribute "lastActivityDevice", "string"
		attribute "lastActivityType", "string"
		attribute "lastActivityValue", "string"
		attribute "lastActivityDescription", "string"
	}

	preferences {
		section ("Debug Logging") {
			input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
		}
	}
}

// Called when driver is initially installed
def installed() {
	logDebug("installed()")
	initialize()
}

// Called when driver code is updated or settings changed
def updated() {
	logDebug("updated()")
	initialize()
}

// Called on hub startup and after driver installation/update
def initialize() {
	logDebug("initialize()")

	// Initialize missing attributes to defaults
	if (device.currentValue("presence") == null) {
		sendEvent(name: "presence", value: "not present")
	}
	if (device.currentValue("timeoutSeconds") == null) {
		sendEvent(name: "timeoutSeconds", value: 1800)
	}
	if (device.currentValue("detected") == null) {
		sendEvent(name: "detected", value: "")
	}
	if (device.currentValue("timeout") == null) {
		sendEvent(name: "timeout", value: "")
	}
	if (device.currentValue("expired") == null) {
		sendEvent(name: "expired", value: "")
	}
	if (device.currentValue("lastActivityDevice") == null) {
		sendEvent(name: "lastActivityDevice", value: "")
	}
	if (device.currentValue("lastActivityType") == null) {
		sendEvent(name: "lastActivityType", value: "")
	}
	if (device.currentValue("lastActivityValue") == null) {
		sendEvent(name: "lastActivityValue", value: "")
	}
	if (device.currentValue("lastActivityDescription") == null) {
		sendEvent(name: "lastActivityDescription", value: "")
	}
}

/**
 * setTimeoutSeconds(timeoutSeconds)
 *
 * Public command: Set the timeout seconds attribute.
 * Used primarily for manual adjustments from the device page.
 * The app does not call this; it passes timeoutSeconds through presentActivity/extendPresence.
 */
def setTimeoutSeconds(timeoutSeconds) {
	timeoutSeconds = normalizeTimeoutSeconds(timeoutSeconds)
	logDebug("setTimeoutSeconds(${timeoutSeconds})")

	sendEvent(name: "timeoutSeconds", value: timeoutSeconds, unit: "seconds")
}

/**
 * presentActivity(deviceName, activityType, activityValue, activityDescription, timeoutSeconds)
 *
 * NON-METADATA METHOD: Intentionally NOT declared as a public command to hide from device UI.
 * Called by the app to report qualifying human-activity events.
 *
 * Updates state, timestamps, and diagnostic attributes. If not currently present,
 * emits a presence = present event.
 */
def presentActivity(String deviceName, String activityType, String activityValue, String activityDescription, Number timeoutSeconds) {
	timeoutSeconds = normalizeTimeoutSeconds(timeoutSeconds)

	String currentPresence = device.currentValue("presence") ?: "not present"
	Date now = new Date()
	String nowTimestamp = now.format("yyyy-MM-dd HH:mm:ss.SSS")
	String timeoutTimestamp = new Date(now.time + timeoutSeconds * 1000).format("yyyy-MM-dd HH:mm:ss.SSS")

	logDebug("presentActivity(device=${deviceName}, type=${activityType}, value=${activityValue}, desc=${activityDescription}, timeout=${timeoutSeconds}s)")

	// Update internal epoch timestamps
	state.detectedEpochMs = now.time
	state.timeoutEpochMs = now.time + (timeoutSeconds * 1000)

	// Update public attributes
	sendEvent(name: "detected", value: nowTimestamp)
	sendEvent(name: "timeout", value: timeoutTimestamp)
	sendEvent(name: "timeoutSeconds", value: timeoutSeconds, unit: "seconds")

	// Update diagnostic attributes
	sendEvent(name: "lastActivityDevice", value: deviceName ?: "")
	sendEvent(name: "lastActivityType", value: activityType ?: "")
	sendEvent(name: "lastActivityValue", value: activityValue ?: "")
	sendEvent(name: "lastActivityDescription", value: activityDescription ?: "")

	// If currently not present, emit presence = present event
	if (currentPresence != "present") {
		String descText = activityDescription ?: "Activity detected"
		logDebug("Emitting presence = present (${descText})")
		sendEvent(name: "presence", value: "present", descriptionText: descText)
	} else {
		logDebug("Already present; updated timeout without re-emitting presence event")
	}
}

/**
 * extendPresence(timeoutSeconds)
 *
 * Public command: Extend the presence timeout when monitored presence sensors
 * are still present at timeout. Called by the app during timeout handler.
 * Presence remains present. Does not update 'detected', only 'timeout'.
 */
def extendPresence(Number timeoutSeconds) {
	timeoutSeconds = normalizeTimeoutSeconds(timeoutSeconds)

	String currentPresence = device.currentValue("presence") ?: "not present"
	Date now = new Date()
	String newTimeoutTimestamp = new Date(now.time + timeoutSeconds * 1000).format("yyyy-MM-dd HH:mm:ss.SSS")

	logDebug("extendPresence(${timeoutSeconds}s)")

	// Update internal epoch timestamp for timeout
	state.timeoutEpochMs = now.time + (timeoutSeconds * 1000)

	// Update timeout and timeoutSeconds attributes
	sendEvent(name: "timeout", value: newTimeoutTimestamp)
	sendEvent(name: "timeoutSeconds", value: timeoutSeconds, unit: "seconds")

	// If presence is unexpectedly not present, set it to present
	// (This should not normally happen if app calls extendPresence correctly)
	if (currentPresence != "present") {
		logDebug("Presence unexpectedly not present; setting to present during extend")
		sendEvent(name: "presence", value: "present", descriptionText: "Presence extended")
	}
}

/**
 * expirePresence(reason)
 *
 * Public command: Expire the presence detector (set to 'not present') after timeout.
 * Called by the app when no monitored presence sensors are still present.
 * Records expiration time. Does not clear 'detected' or 'timeout' for diagnostics.
 */
def expirePresence(String reason) {
	String currentPresence = device.currentValue("presence") ?: "not present"

	// If already not present, do nothing
	if (currentPresence == "not present") {
		logDebug("expirePresence(${reason}) - Already not present, no action")
		return
	}

	Date now = new Date()
	String nowTimestamp = now.format("yyyy-MM-dd HH:mm:ss.SSS")
	String reasonText = reason ?: "timeout"

	logDebug("expirePresence(${reasonText})")

	// Update internal epoch timestamp for expiration
	state.expiredEpochMs = now.time

	// Update expired attribute
	sendEvent(name: "expired", value: nowTimestamp)

	// Emit presence = not present event
	sendEvent(name: "presence", value: "not present", descriptionText: reasonText)
}

/**
 * resetPresence()
 *
 * Public command: Manual reset for testing and troubleshooting.
 * Clears all state, timestamps, and diagnostic attributes.
 * Does not interact with app's scheduled timeout.
 */
def resetPresence() {
	logDebug("resetPresence()")

	// Clear all attributes
	sendEvent(name: "presence", value: "not present")
	sendEvent(name: "detected", value: "")
	sendEvent(name: "timeout", value: "")
	sendEvent(name: "expired", value: "")
	sendEvent(name: "lastActivityDevice", value: "")
	sendEvent(name: "lastActivityType", value: "")
	sendEvent(name: "lastActivityValue", value: "")
	sendEvent(name: "lastActivityDescription", value: "")

	// Clear internal epoch state
	state.detectedEpochMs = null
	state.timeoutEpochMs = null
	state.expiredEpochMs = null
}

/**
 * deviceNotification(text)
 *
 * Notification capability command.
 * Must NOT count as activity. Only logs if debug enabled.
 */
def deviceNotification(String text) {
	if (logEnable) {
		log.debug "Notification received: ${text}"
	}
	// Do not update any presence state, timestamps, or diagnostics
}

// ===== HELPER METHODS =====

/**
 * Normalize timeout seconds: coerce to number, enforce minimum of 1, default to 1800 on invalid
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
 * Simple debug logger
 */
void logDebug(String msg) {
	if (logEnable) {
		log.debug(msg)
	}
}
