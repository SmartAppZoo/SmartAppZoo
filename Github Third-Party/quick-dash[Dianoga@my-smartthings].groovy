/**
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
private String getFirebaseUrl() { "https://project-2731511947915132034.firebaseio.com" }

definition(
	name: "Quick Dash",
	namespace: "dianoga",
	author: "Brian Steere",
	description: "Web dashboard with superpowers.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/unknown/thing/thing-circle.png",
	iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/unknown/thing/thing-circle@2x.png",
	oauth: true
) {
	appSetting "firebaseAuth"
}

preferences {
	page(name: 'devicePage')
	page(name: 'climatePage')
}

mappings {
	path("/command") {
		action: [
			GET: "command",
		]
	}
}

def devicePage() {
	dynamicPage(name: "devicePage", title: "Devices", install: true, uninstall: true) {
		section(name: "devices") {
			input "contactSensors", "capability.contactSensor", title: "Doors/Windows", multiple: true, required: false
			input "doorControl", "capability.doorControl", title: "Controllable Doors", multiple: false, required: false
			input "weatherDevice", "capability.temperatureMeasurement", title: "Weather Station", multiple: false, required: false
			href "climatePage", title: "Climate Sensors", description: climateDescription
			input "switches", "capability.switch", title: "Switches", multiple: true, required: false
			input "motionSensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
			input "smokeSensors", "capability.smokeDetector", title: "Smoke Detectors", multiple: true, required: false
			input "coSensors", "capability.carbonMonoxideDetector", title: "CO Detectors", multiple: true, required: false
			input "waterSensors", "capability.waterSensor", title: "Water Sensors", multiple: true, required: false
		}

		section("Send Notifications?") {
			input("recipients", "contact", title: "Send notifications to") {
				input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
			}
		}
	}
}

def climatePage() {
	dynamicPage(name: 'climatePage', title: 'Setup Climate Sensors', uninstall: false, install: false) {
		floors.each {
			floorPrefs(it.number)
		}

		floorPrefs(floors.size)

		section(name: "other_sensors", title: "Other Climate Sensors") {
			input "otherTemperatureSensors", "capability.temperatureMeasurement", title: "Temperature", multiple: true, required: false
			input "otherHumiditySensors", "capability.relativeHumidityMeasurement", title: "Humidity", multiple: true, required: false
		}
	}
}

def getClimateDescription() {
	"${floors.size} floors configured"
}

def floorPrefs(key) {
	section(name: "floor${key}", title: "Floor ${key}") {
		input "floor${key}TemperatureSensor", "capability.temperatureMeasurement", title: "Temperature", multiple: false, required: false, submitOnChange: true
		input "floor${key}HumiditySensor", "capability.relativeHumidityMeasurement", title: "Humidity", multiple: false, required: false, submitOnChange: true
	}
}

void installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

void updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()

	initialize()
}

void initialize() {
	log.debug "Initializing"

	if (!state.accessToken) {
		createAccessToken()
	}

	subscribe(contactSensors, 'contact', handleEvent)
	subscribe(contactSensors, 'battery', handleEvent)

	subscribe(motionSensors, 'motion', handleEvent)
	subscribe(motionSensors, 'battery', handleEvent)

	subscribe(doorControl, 'door', handleEvent)

	subscribe(temperatureSensors, 'temperature', handleEvent)
	subscribe(temperatureSensors, 'humidity', handleEvent)
	subscribe(temperatureSensors, 'battery', handleEvent)

	subscribe(switches, 'switch', handleEvent)

	subscribe(smokeSensors, 'smoke', handleEvent)
	subscribe(coSensors, 'carbonMonoxide', handleEvent)
	subscribe(waterSensors, 'water', handleEvent)

	subscribe(location, "mode", handleModeChange)
	subscribe(location, "alarmSystemStatus", handleAlarmChange)

	deviceSubscribeAll(weatherDevice)

	firebasePut('devices', deviceData())
	firebasePut('floors', floorData())
	firebasePut('location', locationData())
	firebasePut('settings', [ commandUrl: commandUrl ])

}

void uninstalled() {
	firebaseDelete('devices')
	firebaseDelete('commandUrl')
}

void subscribeAll(devices) {
	devices.each { device ->
		deviceSubscribeAll(device)
	}
}

void deviceSubscribeAll(device) {
	device?.supportedAttributes.each { attr ->
		subscribe(device, attr.name, handleEvent)
	}
}

def getCommandUrl() {
	return apiServerUrl("api/smartapps/installations/${app.id}/command?access_token=${state.accessToken}")
}

def getFloors() {
	def floors = []
	def exists = true

	for(def i = 0; i < 10; i++) {
		if (settings["floor${i}TemperatureSensor"] || settings["floor${i}HumiditySensor"]) {
			floors.push([
				number: i,
				temperatureSensor: settings["floor${i}TemperatureSensor"],
				humiditySensor: settings["floor${i}HumiditySensor"]
			])
		} else {
			exists = false
		}
	}

	return floors
}

def getTemperatureSensors() {
	def sensors = floors.findResults { it.temperatureSensor }

	sensors + otherTemperatureSensors
}

def getHumiditySensors() {
	def sensors = floors.findResults { it.humiditySensor }

	sensors + otherHumiditySensors
}

def getRoutines() {
	location.helloHome?.getPhrases()*.label
}

def hasAlert() {
	return weatherDevice.currentAlert != 'no current weather alerts'
}

void handleModeChange(event) {
	firebasePatch('location', [mode: location.currentMode.name])
}

void handleAlarmChange(event) {
	firebasePatch('location', [alarm: location.currentState("alarmSystemStatus")?.value])
}

void handleEvent(event) {
	def data = [:]

	// Need to handle weather alerts special
	if (event.deviceId == weatherDevice.id && event.name == "alert" && !hasAlert()) {
		data["${event.deviceId}/${event.name}"] = ""
	} else {
		data["${event.deviceId}/${event.name}"] = event.value
	}


	log.debug(data)
	firebasePatch('devices', data)
}

def locationData() {
	def data = [
		mode: location.currentMode.name,
		routines: routines,
		alarm: location.currentState("alarmSystemStatus")?.value
	]

	log.debug "Location: ${data}"
	return data
}

def deviceData() {
	def things = [:]
	contactSensors?.each{
		things[it.id] = createOrMerge(things[it.id], [
			type: createOrMerge(things[it.id]?.type, ["contact"]),
			id: it.id,
			name: it.displayName,
			contact: it.currentContact,
			battery: it.currentBattery as Integer
		])
	}

	switches?.each{
		things[it.id] = createOrMerge(things[it.id], [
			type: createOrMerge(things[it.id]?.type, ["switch"]),
			id: it.id,
			name: it.displayName,
			"switch": it.currentSwitch
		])
	}

	temperatureSensors?.each{
		things[it.id] = createOrMerge(things[it.id], [
			type: createOrMerge(things[it.id]?.type, ["temperature"]),
			id: it.id,
			name: it.displayName,
			temperature: it.currentTemperature,
			battery: it.currentBattery as Integer
		])
	}

	humiditySensors?.each{
		things[it.id] = createOrMerge(things[it.id], [
			type: createOrMerge(things[it.id]?.type, ["humidity"]),
			id: it.id,
			name: it.displayName,
			humidity: it.currentHumidity,
			battery: it.currentBattery as Integer
		])
	}

	motionSensors?.each{
		things[it.id] = createOrMerge(things[it.id], [
			type: createOrMerge(things[it.id]?.type, ["motion"]),
			id: it.id,
			name: it.displayName,
			motion: it.currentMotion,
			battery: it.currentBattery as Integer
		])
	}

	smokeSensors?.each{
		things[it.id] = createOrMerge(things[it.id], [
			type: createOrMerge(things[it.id]?.type, ["smoke"]),
			id: it.id,
			name: it.displayName,
			smoke: it.currentSmoke,
			battery: it.currentBattery as Integer
		])
	}

	coSensors?.each{
		things[it.id] = createOrMerge(things[it.id], [
			type: createOrMerge(things[it.id]?.type, ["carbonMonoxide"]),
			id: it.id,
			name: it.displayName,
			carbonMonoxide: it.currentCarbonMonoxide,
			battery: it.currentBattery as Integer
		])
	}

	waterSensors?.each{
		things[it.id] = createOrMerge(things[it.id], [
			type: createOrMerge(things[it.id]?.type, ["water"]),
			id: it.id,
			name: it.displayName,
			water: it.currentWater,
			battery: it.currentBattery as Integer
		])
	}

	if (weatherDevice) {
		things[weatherDevice.id] = createOrMerge(things[weatherDevice.id], [
			type: createOrMerge(things[weatherDevice.id]?.type, ["outdoorWeather"]),
			name: weatherDevice.displayName,
			id: weatherDevice.id,
			temperature: weatherDevice.currentTemperature,
			humidity: weatherDevice.currentHumidity,
			wind: weatherDevice.currentWind,
			weatherIcon: weatherDevice.currentWeatherIcon,
			alert: hasAlert() ? weatherDevice.currentAlert : "",
			feelsLike: weatherDevice.currentFeelsLike
		])
	}

	if (doorControl) {
		things[doorControl.id] = createOrMerge(things[doorControl.id], [
			type: createOrMerge(things[doorControl.id]?.type, ["door"]),
			id: doorControl.id,
			name: doorControl.displayName,
			door: doorControl.currentDoor
		])
	}

	return things
}

def floorData() {
	def data = floors?.collect {
		[
			number: it.number as String,
			temperature: it.temperatureSensor?.id,
			humidity: it.humiditySensor?.id
		]
	}
}

def command() {
	log.debug "Command Received: ${params}"

	if (params.command == 'refresh') {
		updated()
	}

	if (params.command == 'execute' && routines.contains(params.routine)) {
		location.helloHome?.execute(params.routine)
	}

	if (params.command == 'alarm' && ['off', 'stay', 'away'].contains(params.state)) {
		sendLocationEvent(name: "alarmSystemStatus", value: params.state)
		notify("Alarm state changed to ${params.state} by Quick Dash")
	}

	if (params.type == 'door' && params.device == doorControl.id) {
		if (params.command == 'open') {
			doorControl.open();
		}

		if (params.command == 'close') {
			doorControl.close();
		}
	}

	if (params.type == 'switch') {
		def device = switches.find { it.id == params.device }
		if (!device) {
			return;
		}

		if (params.command == 'on') {
			device.on()
		}

		if (params.command == 'off') {
			device.off()
		}
	}

	def data = "ok"
	render contentType: "application/json", headers: ["Access-Control-Allow-Origin": "*"], data: data
}

void firebasePut(path, data) {
	def params = [
		uri: "${firebaseUrl}/${path}.json",
		query: [
			print: "silent",
			auth: appSettings.firebaseAuth
		],
		body: data
	]

	httpPutJson(params) {
		log.debug "Firebase PUT complete"
	}
}

void firebasePatch(path, data) {
	def params = [
		uri: "${firebaseUrl}/${path}.json",
		query: [
			print: "silent",
			auth: appSettings.firebaseAuth
		],
		headers: [
			"X-HTTP-Method-Override": "PATCH"
		],
		body: data
	]

	httpPostJson(params) {
		log.debug "Firebase PATCH complete"
	}
}

void firebaseDelete(path) {
	def params = [
		uri: "${firebaseUrl}/${path}.json",
		query: [
			print: "silent",
			auth: appSettings.firebaseAuth
		]
	]

	httpDelete(params) {
		log.debug "Firebase DELETE complete"
	}
}

void notify(message) {
	if (location.contactBookEnabled && recipients) {
		sendNotificationToContacts(message, recipients)
	} else if (phone) { // check that the user did select a phone number
		sendSms(phone, message)
	}
}

def createOrMerge(thing1, thing2) {
	if (!thing1) {
		return thing2
	}

	return thing1 + thing2
}
