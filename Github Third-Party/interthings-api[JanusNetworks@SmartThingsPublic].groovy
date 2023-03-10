definition(
    name: "Interthings API",
    namespace: "JanusNetworks",
    author: "Kevin Corbin",
    description: "interthings.io API integration",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Allow interthings to control these things...") {
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
        input "battery", "capability.battery", title: "Which Batteries?", multiple: true, required: false
	}
}

mappings {
 // returns currenmode e.g. {"mode": "Away"}
    path("/mode") {
        action: [
            GET: "getMode"
        ]
    }
    path("/mode/:mode_name") {
        action: [
            PUT: "setMode"
        ]
    }
    path("/alarm") {
        action: [
            GET: "getAlarmMode"
        ]
    }
    path("/batteries") {
        action: [
            GET: "getBatteries"
        ]
    }

	path("/alarm/:mode") {
        action: [
            PUT: "setAlarmMode"
        ]
    }
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
	path("/:deviceType/subscription") {
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
	def currentDeviceIds = settings.collect { k, devices -> devices }.flatten().collect { it.id }.unique()
	def subscriptionDevicesToRemove = app.subscriptions*.device.findAll { device ->
		!currentDeviceIds.contains(device.id)
	}
	subscriptionDevicesToRemove.each { device ->
		log.debug "Removing $device.displayName subscription"
		state.remove(device.id)
		unsubscribe(device)
	}
	log.debug settings
}

def getModes() {
	def allModes = location.modes 
    return allModes
}

def setMode() {
	def mode_name = params.mode_name
	log.debug('attempting to set mode to ' + mode_name)
	location.setMode(mode_name)

    return 'OK'
}

def getBatteries() {
	log.debug("getting batteries")
	def resp = []
     battery.each {
      resp << [name: it.displayName, value: it.currentValue("battery")]
    }
    //def resp = ["foo": "bar"]
    return resp
}

def getAlarmMode() {
	def mode = location.currentState("alarmSystemStatus")?.value
    log.debug(mode)
    return ['mode': mode]
	//sendLocationEvent(name: "alarmSystemStatus" , value : "away|stay|off" )
}

def setAlarmMode() {
	def mode = params.mode
    log.debug("setting SHM mode to: " + mode)
	sendLocationEvent(name: "alarmSystemStatus", value: mode)
}

def list() {
	log.debug "list devices with params: ${params}"
	def type = params.deviceType
	settings[type]?.collect{deviceItem(it)} ?: []
}

def listStates() {
	log.debug "listing states, params: ${params}"
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
	def device = settings[type]?.find { it.id == params.id }
	def command = data.command

	log.debug "update, params: ${params}, request: ${data}, devices: ${devices*.id}"
	
	if (!device) {
		httpError(404, "Device not found")
	} 
	
	if (validateCommand(device, type, command)) {
		device."$command"()
	} else {
		httpError(403, "Access denied. This command is not supported by current capability.")
	}
}

/**
 * Validating the command passed by the user based on capability.
 * @return boolean
 */
def validateCommand(device, deviceType, command) {
	def capabilityCommands = getDeviceCapabilityCommands(device.capabilities)
	def currentDeviceCapability = getCapabilityName(deviceType)
	if (capabilityCommands[currentDeviceCapability]) {
		return command in capabilityCommands[currentDeviceCapability] ? true : false
	} else {
		// Handling other device types here, which don't accept commands
		httpError(400, "Bad request.")
	}
}

/**
 * Need to get the attribute name to do the lookup. Only
 * doing it for the device types which accept commands
 * @return attribute name of the device type
 */
def getCapabilityName(type) {
    switch(type) {
		case "switches":
			return "Switch"
		case "alarms":
			return "Alarm"
		case "locks":
			return "Lock"
		default:
			return type
	}
}

/**
 * Constructing the map over here of
 * supported commands by device capability
 * @return a map of device capability -> supported commands
 */
def getDeviceCapabilityCommands(deviceCapabilities) {
	def map = [:]
	deviceCapabilities.collect {
		map[it.name] = it.commands.collect{ it.name.toString() }
	}
	return map
}


def show() {
	def type = params.deviceType
	def devices = settings[type]
	def device = devices.find { it.id == params.id }

	log.debug "[PROD] show, params: ${params}, devices: ${devices*.id}"
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = attributeFor(type)
		def s = device.currentState(attributeName)
		deviceState(device, s)
	}
}

def addSubscription() {
	log.debug "[PROD] addSubscription1"
	def type = params.deviceType
	def data = request.JSON
	def attribute = attributeFor(type)
	def devices = settings[type]
	def deviceId = data.deviceId
	def callbackUrl = data.callbackUrl
	def device = devices.find { it.id == deviceId }

	log.debug "[PROD] addSubscription, params: ${params}, request: ${data}, device: ${device}"
	if (device) {
		log.debug "Adding switch subscription " + callbackUrl
		state[deviceId] = [callbackUrl: callbackUrl]
		subscribe(device, attribute, deviceHandler)
	}
	log.info state

}

def removeSubscription() {
	def type = params.deviceType
	def devices = settings[type]
	def deviceId = params.id
	def device = devices.find { it.id == deviceId }

	log.debug "[PROD] removeSubscription, params: ${params}, request: ${data}, device: ${device}"
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
		try {
			httpPostJson(uri: deviceInfo.callbackUrl, path: '',  body: [evt: [deviceId: evt.deviceId, name: evt.name, value: evt.value]]) {
				log.debug "[PROD interthings] Event data successfully posted"
			}
		} catch (groovyx.net.http.ResponseParseException e) {
			log.debug("Error parsing interthings payload ${e}")
		}
	} else {
		log.debug "[PROD] No subscribed device found"
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
		case "lightSensors":
			return "illuminance"
		default:
			return type - "Sensors"
	}
}