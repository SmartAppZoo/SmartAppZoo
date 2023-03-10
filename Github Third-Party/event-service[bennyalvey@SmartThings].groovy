/**
 *  Event Service
 *
 *  Author: SmartThings
 *
 *  Defines a web service for accessing device events
 *
 *  ---------------------+----------------+--------------------------+------------------------------------
 *  Device Type          | Attribute Name | Commands                 | Attribute Values
 *  ---------------------+----------------+--------------------------+------------------------------------
 *  switches             | switch         | on, off                  | on, off
 *  motionSensors        | motion         |                          | active, inactive
 *  contactSensors       | contact        |                          | open, closed
 *  presenceSensors      | presence       |                          | present, 'not present'
 *  temperatureSensors   | temperature    |                          |
 *  ---------------------+----------------+--------------------------+------------------------------------
 */

preferences {
	section("Allow access to these devices...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
		input "motionSensors", "capability.motionSensor", title: "Which Motion Sensors?", multiple: true, required: false
		input "contactSensors", "capability.contactSensor", title: "Which Contact Sensors?", multiple: true, required: false
		input "presenceSensors", "capability.presenceSensor", title: "Which Presence Sensors?", multiple: true, required: false
		input "temperatureSensors", "capability.temperatureMeasurement", title: "Which Temperature Sensors?", multiple: true, required: false
	}
}

mappings {

	path("/:deviceType") {
		action: [
			GET: "list"
		]
	}
	path("/:deviceType/events") {
		action: [
			GET: "listEvents"
		]
	}
	path("/:deviceType/events.csv") {
		action: [
			GET: "listEventsCsv"
		]
	}
	path("/:deviceType/:id") {
		action: [
			GET: "show"
		]
	}
	path("/:deviceType/:id/events") {
		action: [
			GET: "showEvents"
		]
	}
	path("/:deviceType/:id/events/:attribute") {
		action: [
			GET: "showEvents"
		]
	}
	path("/:deviceType/:id/events.csv") {
		action: [
			GET: "showEventsCsv"
		]
	}
	path("/:deviceType/:id/events.csv/:attribute") {
		action: [
			GET: "showEventsCsv"
		]
	}
}

def installed() {
	log.debug "installed()"
}

def updated() {
	log.debug "updated()"
}

def list() {
	log.debug "list, params: ${params}"
	def type = params.deviceType
	devices(type)?.collect{deviceItem(it)} ?: []
}

def show() {
	log.debug "show, params: ${params}"
	def type = params.deviceType
	def devices = devices(type)
	def device = devices.find { it.id == params.id }

	log.debug "show, params: ${params}"
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = attributeFor(type)
		def s = device.currentState(attributeName)
		item(device, s)
	}
}

def listEvents() {
	log.debug "listEvents, params: ${params}"
	def type = params.deviceType
	def attributeName = params.attribute ?: attributeFor(type)
	devices(type)?.collect{events(it, attributeName)}?.flatten() ?: []
}

def listEventsCsv() {
	render status: 200, contentType: "text/csv", data: toCsv(listEvents())
}

def showEvents() {
	def type = params.deviceType
	def devices = devices(type)
	def device = devices.find { it.id == params.id }

	log.debug "showEvents, params: ${params}"
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = attributeFor(type)
		def events = events(device, attributeName)
	}
}

def showEventsCsv() {
	render status: 200, contentType: "text/csv", data: toCsv(showEvents())
}

private events(device, attribute) {
	def events = null
	if (params.since) {
		log.trace "eventsSince(${toDate(params.since)})"
		events = device.eventsSince(toDate(params.since), params)
	}
	else if (params.from && params.to) {
		log.trace "eventsBetween(${toDate(params.from)}, ${toDate(params.to)})"
		events = device.eventsBetween(toDate(params.from),toDate(params.to), params)
	}
	else {
		log.trace "events()"
		events = device.events(params)
	}
	if (attribute) {
		events = events.findAll{it.name == attribute}
	}
	events.collect{item(device, it)}
}

private toCsv(states) {
	def s = ""
	states.each {state ->
		def line = state.collect{k,v -> v}.join(",") + "\n"
		s += line
	}
	s
}

private toDate(value) {
	final tf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	if (value.isNumber()) {
		new Date(value as Long)
	}
	else {
		tf.parse(value)
	}
}

private devices(type) {
	if (type == "all") {
		settings.collect{k, v -> v}.flatten()
	}
	else {
		settings[type]
	}
}

private deviceItem(it) {
	it ? [id: it.id, label: it.displayName] : null
}

private item(device, s) {
	final tf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	device && s ? [id: device.id, label: device.displayName, name: s.name, value: s.value, date: tf.format(s.date)] : null
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
