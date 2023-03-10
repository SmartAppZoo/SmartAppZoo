/**
 *  Sample Web Services Application
 *
 *
 *  Author: SmartThings
 *
 *  ---------------------+----------------+--------------------------+------------------------------------
 *  Device Type          | Attribute Name | Commands                 | Attribute Values
 *  ---------------------+----------------+--------------------------+------------------------------------
 *  switches             | switch         | on, off                  | on, off
 *  motionSensors        | motion         |                          | active, inactive
 *  contactSensors       | contact        |                          | open, closed
 *  presenceSensors      | presence       |                          | present, 'not present'
 *  temperatureSensors   | temperature    |                          | <numeric, F or C according to unit>
 *  accelerationSensors  | acceleration   |                          | active, inactive
 *  waterSensors         | water          |                          | wet, dry
 *  lightSensors         | illuminance    |                          | <numeric, lux>
 *  humiditySensors      | humidity       |                          | <numeric, percent>
 *  alarms               | alarm          | strobe, siren, both, off | strobe, siren, both, off
 *  locks                | lock           | lock, unlock             | locked, unlocked
 *  ---------------------+----------------+--------------------------+------------------------------------
 */

preferences {
	section("Allow application to control these things...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
		input "motionSensors", "capability.motionSensor", title: "Which Motion Sensors?", multiple: true, required: false
		input "contactSensors", "capability.contactSensor", title: "Which Contact Sensors?", multiple: true, required: false
		input "presenceSensors", "capability.presenceSensor", title: "Which Presence Sensors?", multiple: true, required: false
		input "temperatureSensors", "capability.temperatureMeasurement", title: "Which Temperature Sensors?", multiple: true, required: false
		input "accelerationSensors", "capability.accelerationSensor", title: "Which Vibration Sensors?", multiple: true, required: false
		input "waterSensors", "capability.waterSensor", title: "Which Water Sensors?", multiple: true, required: false
		input "lightSensors", "capability.illuminanceMeasurement", title: "Which Light Sensors?", multiple: true, required: false
		input "humiditySensors", "capability.relativeHumidityMeasurement", title: "Which Relative Humidity Sensors?", multiple: true, required: false
		input "alarms", "capability.alarm", title: "Which Sirens?", multiple: true, required: false
		input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: false
	}
}

mappings {

	path("/:deviceType") {
		action: [
			GET: "list"
		]
	}
	path("/:deviceType/states") {
		action: [
			GET: "listStates"
		]
	}
	path("/:deviceType/subscriptions") {
		action: [
			POST: "addSubscription"
		]
	}
	path("/:deviceType/subscriptions/:id") {
		action: [
			DELETE: "removeSubscription"
		]
	}
	path("/:deviceType/:id") {
		action: [
			GET: "show",
			PUT: "update"
		]
	}
	path("/subscriptions") {
		action: [
			GET: "listSubscriptions"
		]
	}
}

def installed() {
	log.debug settings
}

def updated() {
	log.debug settings
}

def list() {
	log.debug "list, params: ${params}"
	def type = params.deviceType
	settings[type]?.collect{deviceItem(it)} ?: []
}

def listStates() {
	log.debug "states, params: ${params}"
	def type = params.deviceType
	def attributeName = attributeFor(type)
	settings[type]?.collect{deviceState(it, it.currentState(attributeName))} ?: []
}

def listSubscriptions() {
	state
}

def update() {
	def type = params.deviceType
	def data = request.JSON
	def devices = settings[type]
	def command = data.command

	log.debug "update, params: ${params}, request: ${data}, devices: ${devices*.id}"
	if (command) {
		def device = devices?.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
			device."$command"()
		}
	}
}

def show() {
	def type = params.deviceType
	def devices = settings[type]
	def device = devices.find { it.id == params.id }

	log.debug "show, params: ${params}, devices: ${devices*.id}"
	log.debug "show, device: ${device*.id}"
	if (!device) {
		log.warn "Device ${params.id} now found"
		httpError(404, "Device not found")
	}
	else {
		def attributeName = attributeFor(type)
		log.debug attributeName
		def s = device.currentState(attributeName)
		log.debug s
		def result = deviceState(device, s)
		log.debug result
		return result
	}
}

def addSubscription() {
	def type = params.deviceType
	def data = request.JSON
	def attribute = attributeFor(type)
	def devices = settings[type]
	def deviceId = data.deviceId
	def callbackUrl = data.callbackUrl
	def device = devices.find { it.id == deviceId }

	log.debug "addSubscription, params: ${params}, request: ${data}, device: ${device}"
	if (device) {
		log.debug "Adding switch subscription " + callbackUrl
		state[deviceId] = [callbackUrl: callbackUrl]
		unsubscribe(device)
		subscribe(device, attribute, deviceHandler)
	}
	log.info state
}

def removeSubscription() {
	def devices = settings[type]
	def deviceId = params.id
	def device = devices.find { it.id == deviceId }

	log.debug "removeSubscription, params: ${params}, request: ${data}, device: ${device}"
	if (device) {
		log.debug "Removing $device.displayName subscription"
		state.remove(device.id)
		unsubscribe(device)
	}
	log.info state
}

def deviceHandler(evt) {
	def deviceInfo = state[evt.deviceId]
	if (deviceInfo) {
		httpPostJson(uri: deviceInfo.callbackUrl, path: '',  body: [evt: [deviceId: evt.deviceId, name: evt.name, value: evt.value]]) {
			log.debug "Event data successfully posted"
		}
	} else {
		log.debug "No subscribed device found"
	}
}

private deviceItem(it) {
	it ? [id: it.id, label: it.displayName] : null
}

private deviceState(device, s) {
	device && s ? [id: device.id, label: device.displayName, name: s.name, value: s.value, unixTime: s.date.time] : null
}

private attributeFor(type) {
	switch (type) {
		case "switches":
			return "switch"
		case "locks":
			return "lock"
		case "alarms":
			return "alarm"
		default:
			return type - "Sensors"
	}
}
