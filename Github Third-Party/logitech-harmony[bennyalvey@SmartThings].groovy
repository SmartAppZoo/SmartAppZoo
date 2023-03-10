/**
 *  Logitech Harmony API Access Application
 *
 *  Author: SmartThings
 *
 *  For complete set of capabilities, attributes, and commands see:
 *
 *  https://graph.api.smartthings.com/ide/doc/capabilities
 *
 *  ---------------------+-------------------+-----------------------------+------------------------------------
 *  Device Type          | Attribute Name    | Commands                    | Attribute Values
 *  ---------------------+-------------------+-----------------------------+------------------------------------
 *  switches             | switch            | on, off                     | on, off
 *  motionSensors        | motion            |                             | active, inactive
 *  contactSensors       | contact           |                             | open, closed
 *  presenceSensors      | presence          |                             | present, 'not present'
 *  temperatureSensors   | temperature       |                             | <numeric, F or C according to unit>
 *  accelerationSensors  | acceleration      |                             | active, inactive
 *  waterSensors         | water             |                             | wet, dry
 *  lightSensors         | illuminance       |                             | <numeric, lux>
 *  humiditySensors      | humidity          |                             | <numeric, percent>
 *  alarms               | alarm             | strobe, siren, both, off    | strobe, siren, both, off
 *  locks                | lock              | lock, unlock                | locked, unlocked
 *  ---------------------+-------------------+-----------------------------+------------------------------------
 */

preferences(defaults: false) {
	section("Allow Logitech Harmony to control these things...") {
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
	path("/devices") {
		action: [
			GET: "listDevices"
	    ]
	}

	path("/devices/:id") {
		action: [
			GET: "getDevice",
			PUT: "updateDevice"
	    ]
	}

	path("/subscriptions") {
		action: [
			GET: "listSubscriptions",
			POST: "addSubscription" // {"deviceId":"xxx", "attributeName":"xxx","callbackUrl":"http://..."}
		]
	}
	path("/subscriptions/:id") {
		action: [
			DELETE: "removeSubscription"
		]
	}
	path("/phrases") {
		action: [
			GET: "listPhrases"
	    ]
	}
	path("/phrases/:id") {
		action: [
			PUT: "executePhrase"
	    ]
	}
	path("/hubs") {
		action: [
			GET: "listHubs"
		]
	}
	path("/hubs/:id") {
		action: [
			GET: "getHub"
		]
	}
}

def installed() {
	log.debug settings
}

def updated() {
	log.debug settings
}

def listDevices() {
	log.debug "getDevices, params: ${params}"
	allDevices.collect {
		deviceItem(it)
	}
}

def getDevice() {
	log.debug "getDevice, params: ${params}"
	def device = allDevices.find { it.id == params.id }
	if (!device) {
		render status: 404, data: '{"msg": "Device not found"}'
	} else {
		deviceItem(device)
	}
}

def updateDevice() {
	def data = request.JSON
	def command = data.command
	def arguments = data.arguments

	log.debug "updateDevice, params: ${params}, request: ${data}"
	if (!command) {
		render status: 400, data: '{"msg": "command is required"}'
	} else {
		def device = allDevices.find { it.id == params.id }
		if (device) {
			if (arguments) {
				device."$command"(*arguments)
			} else {
				device."$command"()
			}
			render status: 204, data: "{}"
		} else {
			render status: 404, data: '{"msg": "Device not found"}'
		}
	}
}

def listSubscriptions() {
	log.debug "listSubscriptions()"
	app.subscriptions?.collect {
		def deviceInfo = state[it.deviceId]
		[
			id: it.id,
			deviceId: it.deviceId,
			attributeName: it.data,
			handler: it.handler,
			callbackUrl: deviceInfo?.callbackUrl
		]
	} ?: []
}

def addSubscription() {
	def data = request.JSON
	def attribute = data.attributeName
	def callbackUrl = data.callbackUrl

	log.debug "addSubscription, params: ${params}, request: ${data}"
	if (!attribute) {
		render status: 400, data: '{"msg": "attributeName is required"}'
	} else {
		def device = allDevices.find { it.id == data.deviceId }
		if (device) {
			log.debug "Adding subscription $callbackUrl"
			state[device.id] = [callbackUrl: callbackUrl]
			def subscription = subscribe(device, attribute, deviceHandler)
			if (subscription && subscription.eventSubscription) {
				[
					id: subscription.id,
					deviceId: subscription.deviceId,
					attributeName: subscription.data,
					handler: subscription.handler,
					callbackUrl: callbackUrl
				]
			} else {
				subscription = app.subscriptions?.find { it.deviceId == data.deviceId && it.data == attribute && it.handler == 'deviceHandler' }
				[
					id: subscription.id,
					deviceId: subscription.deviceId,
					attributeName: subscription.data,
					handler: subscription.handler,
					callbackUrl: callbackUrl
				]
			}
		} else {
			render status: 400, data: '{"msg": "Device not found"}'
		}
	}
}

def removeSubscription() {
	def subscription = app.subscriptions?.find { it.id == params.id }
	def device = subscription?.device

	log.debug "removeSubscription, params: ${params}, subscription: ${subscription}, device: ${device}"
	if (device) {
		log.debug "Removing subscription for device: ${device.id}"
		state.remove(device.id)
		unsubscribe(device)
	}
	render status: 204, data: "{}"
}

def listPhrases() {
	location.helloHome.getPhrases()?.collect {[
		id: it.id,
		label: it.label
	]}
}

def executePhrase() {
	log.debug "executedPhrase, params: ${params}"
	location.helloHome.execute(params.id)
	render status: 204, data: "{}"
}

def deviceHandler(evt) {
	def deviceInfo = state[evt.deviceId]
	if (deviceInfo) {
		if (deviceInfo.callbackUrl) {
			def callback = new URI(deviceInfo.callbackUrl)
			def host = callback.port != -1 ? "${callback.host}:${callback.port}" : callback.host
			def path = callback.query ? "${callback.path}?${callback.query}".toString() : callback.path
			sendHubCommand(new physicalgraph.device.HubAction(
				method: "POST",
				path: path,
				headers: [
					"Host": host,
					"Content-Type": "application/json"
				],
				body: [evt: [deviceId: evt.deviceId, name: evt.name, value: evt.value]]
			))
		} else {
			log.warn "No callbackUrl set for device: ${evt.deviceId}"
		}
	} else {
		log.warn "No subscribed device found for device: ${evt.deviceId}"
	}
}

def listHubs() {
	location.hubs?.findAll { it.type.toString() == "PHYSICAL" }?.collect { hubItem(it) }
}

def getHub() {
	def hub = location.hubs?.findAll { it.type.toString() == "PHYSICAL" }?.find { it.id == params.id }
	if (!hub) {
		render status: 404, data: '{"msg": "Hub not found"}'
	} else {
		hubItem(hub)
	}
}

private getAllDevices() {
	([] + switches + motionSensors + contactSensors + presenceSensors + temperatureSensors + accelerationSensors + waterSensors + lightSensors + humiditySensors + alarms + locks)?.findAll()?.unique { it.id }
}

private deviceItem(device) {
	[
		id: device.id,
		label: device.displayName,
		currentStates: device.currentStates,
		capabilities: device.capabilities?.collect {[
			name: it.name
		]},
		attributes: device.supportedAttributes?.collect {[
			name: it.name,
			dataType: it.dataType,
			values: it.values
		]},
		commands: device.supportedCommands?.collect {[
			name: it.name,
			arguments: it.arguments
		]},
		type: [
			name: device.typeName,
			author: device.typeAuthor
		]
	]
}

private hubItem(hub) {
	[
		id: hub.id,
		name: hub.name,
		ip: hub.localIP,
		port: hub.localSrvPortTCP
	]
}