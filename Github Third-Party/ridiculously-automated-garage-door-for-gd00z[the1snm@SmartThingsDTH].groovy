/**
 *  Ridiculously Automated Garage Door
 *
 *  Original Author: SmartThings
 *
 * Monitors arrival and departure of car(s) and
 *
 *    1) opens door when car arrives,
 *    2) closes door after car has departed (for N minutes),
 *    3) opens door when car door motion is detected,
 *    4) closes door when door was opened due to arrival and interior door is closed.
 */

definition(
    name: "Ridiculously Automated Garage Door for GD00Z",
    author: "Stephen McDonnell",
    namespace: "the1snm",
    description: "Monitors arrival and departure of car(s) and 1) opens door when car arrives, 2) closes door after car has departed (for N minutes), 3) opens door when car door motion is detected, 4) closes door when door was opened due to arrival and interior door is closed.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {

	section("Garage door") {
		input "door", "capability.doorControl", title: "Which garage door controller?"
		input "openThreshold", "number", title: "Warn when open longer than (optional)",description: "Number of minutes", required: false
		input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
	}
	section("Car(s) using this garage door") {
		input "cars", "capability.presenceSensor", title: "Presence sensor", description: "Which car(s)?", multiple: true, required: false
		input "carDoorSensors", "capability.accelerationSensor", title: "Car door sensor(s)", description: "Which car(s)?", multiple: true, required: false
	}
	section("Interior door (optional)") {
		input "interiorDoorSensor", "capability.contactSensor", title: "Contact sensor?", required: false
	}
	section("False alarm threshold (defaults to 10 min)") {
		input "falseAlarmThreshold", "number", title: "Number of minutes", required: false
	}
}

def installed() {
	log.trace "installed()"
	subscribe()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
	subscribe()
}

def subscribe() {
	log.debug "present: ${cars.collect{it.displayName + ': ' + it.currentPresence}}"
	subscribe(door, "door", garageDoorState)

	subscribe(cars, "presence", carPresence)
	subscribe(carDoorSensors, "acceleration", accelerationActive)

	if (interiorDoorSensor) {
		subscribe(interiorDoorSensor, "contact.closed", interiorDoorClosed)
	}
}

def doorOpenCheck()
{
	final thresholdMinutes = openThreshold
	if (thresholdMinutes) {
		def currentState = door.doorState
		log.debug "doorOpenCheck"
		if (currentState?.value == "open") {
			log.debug "open for ${now() - currentState.date.time}, openDoorNotificationSent: ${state.openDoorNotificationSent}"
			if (!state.openDoorNotificationSent && now() - currentState.date.time > thresholdMinutes * 60 *1000) {
				def msg = "${door.displayName} was been open for ${thresholdMinutes} minutes"
				log.info msg
				sendPush msg
				if (phone) {
					sendSms phone, msg
				}
				state.openDoorNotificationSent = true
			}
		}
		else {
			state.openDoorNotificationSent = false
		}
	}
}

def carPresence(evt)
{
	log.info "$evt.name: $evt.value"
	// time in which there must be no "not present" events in order to open the door
	final openDoorAwayInterval = falseAlarmThreshold ? falseAlarmThreshold * 60 : 600

	if (evt.value == "present") {
		// A car comes home

		def car = getCar(evt)
		def t0 = new Date(now() - (openDoorAwayInterval * 1000))
		def states = car.statesSince("presence", t0)
		def recentNotPresentState = states.find{it.value == "not present"}

		if (recentNotPresentState) {
			log.debug "Not opening ${door.displayName} since car was not present at ${recentNotPresentState.date}, less than ${openDoorAwayInterval} sec ago"
		}
		else {
			if (door.currentDoor == "closed") {
				openDoor()
				sendPush "Opening garage door due to arrival of ${car.displayName}"
				state.appOpenedDoor = now()
			}
			else {
				log.debug "door already open"
			}
		}
	}
	else {
		// A car departs
		if (door.currentDoor == "open") {
			closeDoor()
			log.debug "Closing ${door.displayName} after departure"
			sendPush("Closing ${door.displayName} after departure")
		}
		else {
			log.debug "Not closing ${door.displayName} because its already closed"
		}
	}
}

def garageDoorState(evt)
{
	log.info "garageDoorState, $evt.name: $evt.value"
	if (evt.value == "open") {
		schedule("0 * * * * ?", "doorOpenCheck")
	}
	else {
		unschedule("doorOpenCheck")
	}
}


def interiorDoorClosed(evt)
{
	log.info "interiorContact, $evt.name: $evt.value"

	// time during which closing the interior door will shut the garage door, if the app opened it
	final threshold = 15 * 60 * 1000
	if (state.appOpenedDoor && now() - state.appOpenedDoor < threshold) {
		state.appOpenedDoor = 0
		closeDoor()
	}
	else {
		log.debug "app didn't open door"
	}
}

def accelerationActive(evt)
{
	log.info "$evt.name: $evt.value"

	if (door.currentDoor == "closed") {
		log.debug "opening door when car door opened"
		openDoor()
	}
}

private openDoor()
{
	door.open()
}

private closeDoor()
{
	door.close()
}

private getCar(evt)
{
	cars.find{it.id == evt.deviceId}
}