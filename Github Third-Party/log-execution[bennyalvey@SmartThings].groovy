/**
 *  Log Execution
 *
 *  Author: SmartThings
 */
preferences {
	section("When the door opens/closes...") {
		input "contact1", "capability.contactSensor", title: "Where?"
	}
	section("Turn on/off a light...") {
		input "switch1", "capability.switch"
	}
}

def installed()
{
	subscribe(contact1, "contact", contactHandler)
}

def updated()
{
	unsubscribe()
	subscribe(contact1, "contact", contactHandler)
}

def contactHandler(evt) {
	log.debug "Executing app for event: $evt with settings: $settings and initial state: $state"

	log.debug "myMap.size() == ${state.myMap?.size()}"
	state.myMap?.each { k, v ->
		log.debug "myMap[${k}] == $v (${v.getClass()})"
	}
	log.debug "myMap.size() == ${state.myMap?.size()}"

	state.now = new Date().toSystemFormat()
	state.myMap = [a: 1, b: '2 string', c: new Date(), d: [sub:'map'], e: Math.random(), f: "blamo"]

	log.debug "Executing app for event: $evt with settings: $settings and end state: $state"
}
