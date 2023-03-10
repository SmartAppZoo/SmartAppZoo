/**
 *  Event Exporter
 *
 *  Author: SmartThings
 *
 *  Posts device events to an external web service
 *
 *  ---------------------+----------------+--------------------------+------------------------------------
 *  Device Type          | Attribute Name | Commands                 | Attribute Values
 *  ---------------------+----------------+--------------------------+------------------------------------
 *  switches             | switch         | on, off                  | on, off
 *  motionSensors        | motion         |                          | active, inactive
 *  contactSensors       | contact        |                          | open, closed
 *  presenceSensors      | presence       |                          | present, 'not present'
 *  temperatureSensors   | temperature    |                          | <numeric, F or C according to unit>
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

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {
	if (switches) {
		subscribe(switches, "switch", handler)
	}
	if (motionSensors) {
		subscribe(motionSensors, "motion", handler)
	}
	if (contactSensors) {
		subscribe(contactSensors, "contact", handler)
	}
	if (presenceSensors) {
		subscribe(presenceSensors, "presence", handler)
	}
	if (temperatureSensors) {
		subscribe(temperatureSensors, "temperature", handler)
	}
}

def handler(evt) {
	log.debug "$evt.name: $evt.value"

	// use httpPost(...) for form-urlencoded rather than JSON
	httpPostJson(uri: "http://florian.org:4567/event", body: [id: evt.deviceId, name: evt.name, value: evt.value, date: evt.dateString]) {resp ->
		log.debug resp.status
	}
}
