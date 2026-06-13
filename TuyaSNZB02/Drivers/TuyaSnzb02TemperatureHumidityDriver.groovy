/**
 * Tuya SNZB-02 Temperature Humidity Sensor Driver
 *
 * Namespace: tgiphil
 * Author: phil@thinkedge.com
 */

import hubitat.zigbee.zcl.DataType

metadata {
	definition(
		name: "Tuya SNZB-02 Temperature Humidity Sensor",
		namespace: "tgiphil",
		author: "phil@thinkedge.com"
	) {
		capability "TemperatureMeasurement"
		capability "RelativeHumidityMeasurement"
		capability "Battery"
		capability "Sensor"
		capability "Refresh"
		capability "Configuration"

		attribute "batteryVoltage", "number"

		fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0001,0020,0402,0405", outClusters: "0019", model: "SNZB-02", manufacturer: "_TZ3000_utwgoauk"
	}

	preferences {
		section("Calibration") {
			input name: "temperatureOffset", type: "decimal", title: "Temperature offset (${location.temperatureScale ?: "C"})", defaultValue: 0
		}
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

	cmds += zigbee.configureReporting(0x0402, 0x0000, DataType.INT16, 30, 3600, 10)
	cmds += zigbee.configureReporting(0x0405, 0x0000, DataType.UINT16, 30, 3600, 100)
	cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 3600, 21600, 1)
	cmds += zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 3600, 21600, 1)

	cmds += zigbee.readAttribute(0x0402, 0x0000)
	cmds += zigbee.readAttribute(0x0405, 0x0000)
	cmds += zigbee.readAttribute(0x0001, 0x0021)
	cmds += zigbee.readAttribute(0x0001, 0x0020)

	return cmds ? delayBetween(cmds, 200) : []
}

def refresh() {
	logDebug("refresh()")
	List<String> cmds = []
	cmds += zigbee.readAttribute(0x0402, 0x0000)
	cmds += zigbee.readAttribute(0x0405, 0x0000)
	cmds += zigbee.readAttribute(0x0001, 0x0021)
	cmds += zigbee.readAttribute(0x0001, 0x0020)
	return cmds ? delayBetween(cmds, 200) : []
}

def parse(String description) {
	logDebug("parse(): ${description}")

	if (!description) {
		return null
	}

	Map event = zigbee.getEvent(description)
	if (event) {
		if (event.name == "temperature") {
			BigDecimal t = (event.value as BigDecimal).setScale(2, BigDecimal.ROUND_HALF_UP)
			String unit = event.unit ?: (location.temperatureScale ?: "C")
			sendTemperatureWithUnit(t, unit)
			return null
		}
		if (event.name == "humidity") {
			BigDecimal h = (event.value as BigDecimal).setScale(2, BigDecimal.ROUND_HALF_UP)
			sendEvent(name: "humidity", value: h, unit: "%", descriptionText: metricDescription("humidity", h))
			return null
		}

		sendEvent(event)
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
		case "0402":
			if (attrId == "0000") {
				handleTemperature(valueHex)
			} else {
				logDebug("Unhandled temperature attr ${attrId}: ${descMap}")
			}
			break

		case "0405":
			if (attrId == "0000") {
				handleHumidity(valueHex)
			} else {
				logDebug("Unhandled humidity attr ${attrId}: ${descMap}")
			}
			break

		case "0001":
			handleBattery(attrId, valueHex)
			break

		default:
			logDebug("Unhandled cluster ${cluster}: ${descMap}")
			break
	}
}

private void handleTemperature(String valueHex) {
	Integer raw = parseHex(valueHex)
	if (raw == null) {
		logDebug("Invalid temperature payload: ${valueHex}")
		return
	}

	if (raw > 0x7FFF) {
		raw = raw - 0x10000
	}

	BigDecimal tempC = (raw / 100.0).setScale(2, BigDecimal.ROUND_HALF_UP)
	sendTemperatureEvent(tempC)
}

private void sendTemperatureEvent(BigDecimal tempC) {
	if (tempC == null) {
		return
	}

	String scale = location.temperatureScale ?: "C"
	BigDecimal value = tempC
	if (scale == "F") {
		value = ((tempC * 9.0 / 5.0) + 32.0).setScale(2, BigDecimal.ROUND_HALF_UP)
	} else {
		value = tempC.setScale(2, BigDecimal.ROUND_HALF_UP)
	}

	sendTemperatureWithUnit(value, scale)
}

private void handleHumidity(String valueHex) {
	Integer raw = parseHex(valueHex)
	if (raw == null) {
		logDebug("Invalid humidity payload: ${valueHex}")
		return
	}

	BigDecimal humidity = (raw / 100.0).setScale(2, BigDecimal.ROUND_HALF_UP)
	sendEvent(name: "humidity", value: humidity, unit: "%", descriptionText: metricDescription("humidity", humidity))
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

private void sendTemperatureWithUnit(BigDecimal temperatureValue, String unit) {
	if (temperatureValue == null) {
		return
	}

	BigDecimal offset = 0
	try {
		offset = (settings?.temperatureOffset ?: 0) as BigDecimal
	} catch (Exception ignored) {
		offset = 0
	}

	BigDecimal adjusted = (temperatureValue + offset).setScale(2, BigDecimal.ROUND_HALF_UP)
	sendEvent(name: "temperature", value: adjusted, unit: unit, descriptionText: metricDescription("temperature", adjusted))
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
