/**
 * HOBEIAN ZG-102Z Contact Sensor Driver
 *
 * Namespace: tgiphil
 * Author: phil@thinkedge.com
 */

import hubitat.zigbee.zcl.DataType

metadata {
	definition(
		name: "HOBEIAN ZG-102Z Contact Sensor",
		namespace: "tgiphil",
		author: "phil@thinkedge.com"
	) {
		capability "ContactSensor"
		capability "TamperAlert"
		capability "Battery"
		capability "Sensor"
		capability "Refresh"
		capability "Configuration"

		attribute "batteryVoltage", "number"

		fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0500,EF00,0001", model: "ZG-102Z", manufacturer: "HOBEIAN"
	}

	preferences {
		section("Debug Logging") {
			input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
		}
	}
}

def installed() {
	logDebug("installed()")
	initialize()
}

def updated() {
	logDebug("updated()")
	initialize()
}

def initialize() {
	unschedule()
	if (logEnable) {
		runIn(1800, "logsOff")
	}
}

def configure() {
	logDebug("configure()")

	List<String> cmds = []

	cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 3600, 21600, 1)
	cmds += zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 3600, 21600, 1)

	cmds += zigbee.readAttribute(0x0001, 0x0021)
	cmds += zigbee.readAttribute(0x0001, 0x0020)

	return cmds ? delayBetween(cmds, 200) : []
}

def refresh() {
	logDebug("refresh()")
	List<String> cmds = []
	cmds += zigbee.readAttribute(0x0001, 0x0021)
	cmds += zigbee.readAttribute(0x0001, 0x0020)
	return cmds ? delayBetween(cmds, 200) : []
}

def parse(String description) {
	logDebug("parse(): ${description}")

	if (!description) {
		return null
	}

	if (description.startsWith("zone status")) {
		handleZoneStatus(description)
		return null
	}

	if (description.startsWith("read attr -") || description.startsWith("catchall:")) {
		Map descMap = zigbee.parseDescriptionAsMap(description)
		if (!descMap) {
			logDebug("Unable to parse Zigbee description map")
			return null
		}
		handleDescriptionMap(descMap)
		return null
	}

	logDebug("Unhandled Zigbee description: ${description}")
	return null
}

private void handleZoneStatus(String description) {
	def matcher = description =~ /zone status 0x([0-9a-fA-F]+)/
	if (!matcher.find()) {
		logDebug("Unable to parse zone status value: ${description}")
		return
	}

	Integer zoneStatus = Integer.parseInt(matcher.group(1), 16)

	boolean alarm1 = (zoneStatus & 0x0001) != 0
	boolean tamper = (zoneStatus & 0x0004) != 0

	String contact = alarm1 ? "open" : "closed"
	sendEvent(name: "contact", value: contact, descriptionText: metricDescription("contact", contact))

	String tamperState = tamper ? "detected" : "clear"
	sendEvent(name: "tamper", value: tamperState, descriptionText: metricDescription("tamper", tamperState))
}

private void handleDescriptionMap(Map descMap) {
	String cluster = (descMap.cluster ?: descMap.clusterId ?: "").toUpperCase()
	String attrId = (descMap.attrId ?: "").toUpperCase()
	String valueHex = (descMap.value ?: "").toUpperCase()

	if (!cluster) {
		logDebug("Ignoring map missing cluster: ${descMap}")
		return
	}

	if (cluster == "0013") {
		logDebug("Ignoring catchall cluster 0013 message")
		return
	}

	if (!attrId || !valueHex) {
		logDebug("Ignoring cluster ${cluster} map missing attr/value: ${descMap}")
		return
	}

	switch (cluster) {
		case "0001":
			handleBattery(attrId, valueHex)
			break

		default:
			logDebug("Unhandled cluster ${cluster}: ${descMap}")
			break
	}
}

private void handleBattery(String attrId, String valueHex) {
	Integer raw = parseHex(valueHex)
	if (raw == null) {
		logDebug("Invalid battery payload attr ${attrId}: ${valueHex}")
		return
	}

	switch (attrId) {
	case "0021":
		Integer percent = Math.max(0, Math.min(100, (Integer) Math.round(raw / 2.0)))
		sendEvent(name: "battery", value: percent, unit: "%", descriptionText: metricDescription("battery", percent))
		break

	case "0020":
		BigDecimal volts = (raw / 10.0).setScale(1, BigDecimal.ROUND_HALF_UP)
		sendEvent(name: "batteryVoltage", value: volts, unit: "V", descriptionText: metricDescription("battery voltage", volts))
		Integer estimatedPercent = estimateBatteryPercentFromVoltage(volts)
		if (estimatedPercent != null) {
			sendEvent(name: "battery", value: estimatedPercent, unit: "%", descriptionText: metricDescription("battery", estimatedPercent))
		}
		break

	default:
		logDebug("Unhandled battery attr ${attrId} value ${valueHex}")
		break
	}
}

private Integer estimateBatteryPercentFromVoltage(BigDecimal volts) {
	if (volts == null) {
		return null
	}

	BigDecimal minVolts = 2.1
	BigDecimal maxVolts = 3.0
	BigDecimal pct = ((volts - minVolts) / (maxVolts - minVolts)) * 100.0
	Integer result = pct.setScale(0, BigDecimal.ROUND_HALF_UP) as Integer
	return Math.max(0, Math.min(100, result))
}

private String metricDescription(String metricName, Object value) {
	String deviceName = device?.displayName ?: "Device"
	return "${deviceName} ${metricName} is ${value}"
}

private Integer parseHex(String valueHex) {
	try {
		return Integer.parseInt(valueHex, 16)
	} catch (Exception e) {
		return null
	}
}

def logsOff() {
	device.updateSetting("logEnable", [value: "false", type: "bool"])
	log.warn "Debug logging disabled"
}

private void logDebug(String msg) {
	if (logEnable) {
		log.debug msg
	}
}
