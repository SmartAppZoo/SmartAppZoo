/**
 *  Nest (Connect)
 *
 *  Author: Brian Steere
 *
 *  Date: 2014-02-15
 */
preferences {
	page(name: "auth", title: "Nest Login", install: false, uninstall: true, nextPage:"devices") {
		section("Log in to your Nest account") {
			input "username", "text", title: "Email Address", required: true, autoCorrect:false
			input "password", "password", title: "Password", required: true, autoCorrect:false
		}
	}
	page(name: "devices", title: "Nest Thermostats", install: true, uninstall: true, content: "listThermostats")
}

def listThermostats() {
	log.debug "listThermostats()"
	login()

	def devices = [:]
	state.shared = [:]
	state.structures = [:]
	api(null, 'status', []) {
		try {
			// Device name is in shared
			it.data.shared.each() { key, value ->
				state.shared[key] = value
				devices[key] = value.name
			}

			// Get the structure ids and associated devices
			it.data.structure.each() { key, value ->
				value.devices.each { state.structures[it.drop(7)] = key }
			}
		} catch (Throwable e) {
			log.debug "Error: $e"
		}
	}

	return dynamicPage(name: "devices", title: "Choose devices", install: true) {
		section("Devices") {
			input "devices", "enum", title: "Thermostats", required: false, multiple: true, options: devices
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	settings.devices.each {nestId ->
		def device = state.shared.getAt(nestId)

		try {
			def existingDevice = getChildDevice(nestId)
			if(!existingDevice) {
				log.debug "Creating child"
				def childDevice = addChildDevice("dianoga7", "Nest Thermostat", nestId, null, [name: "Nest.${nestId}", label: device.name, completedSetup: true])
			} else {
				log.debug "Device $nestId already exists"
			}
		} catch (e) {
			log.error "Error creating device: ${e}"
		}
	}

	// Cleanup any other devices that need to go away
	def delete = getChildDevices().findAll { !settings.devices.contains(it.deviceNetworkId) }
	log.debug "Delete: $delete"
	delete.each { deleteChildDevice(it.deviceNetworkId) }

	// Do the initial poll and schedule it to run every minute
	poll()
	schedule("* * * * * ?", poll)
}



// handle commands
def setHeatingSetpoint(child, temp) {
	setTargetTemp(child, temp);
}

def setCoolingSetpoint(child, temp) {
	setTargetTemp(child, temp);
}

def setTargetTemp(child, temp) {
	api(child.device.deviceNetworkId, 'temperature', ['target_change_pending': true, 'target_temperature': fToC(temp)]) {
		child?.sendEvent(name: 'coolingSetpoint', value: temp)
		child?.sendEvent(name: 'heatingSetpoint', value: temp)
	}
}

def off(child) {
	log.debug "Manager: off"
	log.debug "Child: $child"
	setThermostatMode(child, 'off')
}

def heat(child) {
	log.debug "Manager: heat"
	setThermostatMode(child, 'heat')
}

def emergencyHeat(child) {
	log.debug "Manager: emergencyHeat"
	setThermostatMode(child, 'heat')
}

def cool(child) {
	log.debug "Manager: cool"
	setThermostatMode(child, 'cool')
}

def setThermostatMode(child, mode) {
	log.debug "Manager: setThermostatMode"
	mode = mode == 'emergency heat'? 'heat' : mode

	api(child.device.deviceNetworkId, 'thermostat_mode', ['target_change_pending': true, 'target_temperature_type': mode]) {
		child?.sendEvent(name: 'thermostatMode', value: mode)
	}
}

def fanOn(child) {
	setThermostatFanMode(child, 'on')
}

def fanAuto(child) {
	setThermostatFanMode(child, 'auto')
}

def fanCirculate(child) {
	setThermostatFanMode(child, 'circulate')
}

def setThermostatFanMode(child, mode) {
	def modes = [
		on: ['fan_mode': 'on'],
		auto: ['fan_mode': 'auto'],
		circulate: ['fan_mode': 'duty-cycle', 'fan_duty_cycle': 900]
	]

	api(child.device.deviceNetworkId, 'fan_mode', modes.getAt(mode)) {
		child?.sendEvent(name: 'thermostatFanMode', value: mode)
	}
}

def away(child) {
	setPresence(child, 'away')
}

def present(child) {
	setPresence(child, 'present')
}

def setPresence(child, status) {
	log.debug "Status: $status"
	api(child.device.deviceNetworkId, 'presence', ['away': status == 'away', 'away_timestamp': new Date().getTime(), 'away_setter': 0]) {
		child?.sendEvent(name: 'presence', value: status)
	}
}

def poll() {
	log.debug "Executing 'poll'"
	api(null, 'status', []) {
		it.data.device.each { serial, value ->
			def child = getChildDevice(serial)
			if(child) {
				def structure = it.data.structure[state.structures[serial]]
				def shared = it.data.shared[serial]

				child?.sendEvent(name: 'humidity', value: value.current_humidity)
				child?.sendEvent(name: 'temperature', value: cToF(shared.current_temperature) as Integer, state: value.target_temperature_type)
				child?.sendEvent(name: 'thermostatFanMode', value: value.fan_mode == 'duty-cycle'? 'circulate' : value.fan_mode)
				child?.sendEvent(name: 'thermostatMode', value: shared.target_temperature_type)
				child?.sendEvent(name: 'coolingSetpoint', value: cToF(shared.target_temperature) as Integer)
				child?.sendEvent(name: 'heatingSetpoint', value: cToF(shared.target_temperature) as Integer)
				child?.sendEvent(name: 'presence', value: structure.away? 'away' : 'present')
			}
		}
	}
}

def login(method = null, args = [], success = {}) {
	def params = [
		uri: 'https://home.nest.com/user/login',
		body: [username: settings.username, password: settings.password]
	]

	httpPost(params) {response ->
		state.auth = response.data
		state.auth.expires_in = Date.parse('EEE, dd-MMM-yyyy HH:mm:ss z', response.data.expires_in).getTime()

		if(method != null) {
			api(method, args, success)
		}
	}
}

def isLoggedIn() {
	if(!state.auth) {
		log.debug "No data.auth"
		return false
	}

	def now = new Date().getTime();
	return state.auth.expires_in > now
}

def api(serial, method, args = [], success = {}) {
	if(!isLoggedIn()) {
		log.debug "Need to login"
		login(method, args, success)
		return
	}

	def methods = [
		'status': [uri: "/v2/mobile/${state.auth.user}", type: 'get'],
		'fan_mode': [uri: "/v2/put/device.${serial}", type: 'post'],
		'thermostat_mode': [uri: "/v2/put/shared.${serial}", type: 'post'],
		'temperature': [uri: "/v2/put/shared.${serial}", type: 'post'],
		'presence': [uri: "/v2/put/structure.${state.structures[serial]}", type: 'post']
	]

	def request = methods.getAt(method)

	log.debug "Logged in"
	doRequest(request.uri, args, request.type, success)
}

// Need to be logged in before this is called. So don't call this. Call api.
def doRequest(uri, args, type, success) {
	log.debug "Calling $type : $uri : $args"

	if(uri.charAt(0) == '/') {
		uri = "${state.auth.urls.transport_url}${uri}"
	}

	def params = [
		uri: uri,
		headers: [
			'X-nl-protocol-version': 1,
			'X-nl-user-id': state.auth.userid,
			'Authorization': "Basic ${state.auth.access_token}"
		],
		body: args
	]

	try {
		if(type == 'post') {
			httpPostJson(params, success)
		} else if (type == 'get') {
			httpGet(params, success)
		}
	} catch (Throwable e) {
		log.debug "Request Error: $e"
		login()
	}
}

def cToF(temp) {
	return temp * 1.8 + 32
}

def fToC(temp) {
	return (temp - 32) / 1.8
}