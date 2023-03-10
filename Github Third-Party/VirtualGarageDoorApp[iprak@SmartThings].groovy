definition(
	name: "XIP Virtual Garage Door v1",
	namespace: "induprakash",
	author: "Indu Prakash",
	description: "Syncs XIP Virtual Garage Door device with a contact sensor.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot@2x.png")

preferences {
	section("Which sensor can tell if the door is closed?") {
		input "closeSensor", "capability.contactSensor",	title: "Garage Door Close Sensor", required: true
	}
    //section("Which sensor can tell if the door is open?") {
	//	input "openSensor", "capability.contactSensor",	title: "Garage Door Open Sensor", required: false
	//}
	section("Which virtual garage door to use?") {
		input "virtualDoor", "capability.doorControl", title: "Virtual Garage Door", required: true
	}
	section("Check if door opened/closed correctly?") {
		input "checkAfter", "number", title: "Operation Check Delay?", required: true, defaultValue: 25
	}
	section("Notifications") {
		input("recipients", "contact", title: "Send notifications to") {
			input "sendMsg", "enum", title: "Send notification?", options: ["Yes", "No"], required: false, defaultValue: 1
		}
	}
}

def installed() {
	log.debug "installed() with settings: $settings"
	initialize()
}

def updated() {
	log.debug "updated()"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	log.debug "initialize()"

	if (virtualDoor.hasCommand("finishClosing") && virtualDoor.hasCommand("finishOpening")) {
		subscribe(virtualDoor, "door", doorHandler)
		subscribe(closeSensor, "contact", closeSensorHandler)
		if (openSensor) {
			subscribe(openSensor, "contact", openSensorHandler)
		}
		syncVirtual()
	}
	else {
		log.error("Unsupported virtual garage door, it has to be a XIP Virtual Garage Door device.")
	}    
}

def syncVirtual() {
	def sensorCurrentValue = closeSensor.currentValue("contact")
	if (sensorCurrentValue != virtualDoor.currentValue("contact")) {
		if (sensorCurrentValue == "closed") {
			log.debug "syncVirtual() closing virtual door"
			virtualDoor.finishClosing()
		} else if (sensorCurrentValue == "open") {
			log.debug "syncVirtual() opening virtual door"
			virtualDoor.finishOpening()
		}
	}
}

def closeSensorHandler(evt) {
	syncVirtual()
}

def openSensorHandler(evt) {
	state.door = evt.value
	log.debug "openSensorHandler() ${evt.value}"
}

def doorHandler(evt) {
	state.door = evt.value
	log.debug "doorHandler() operation=$state.door"
    
	if (evt.value == "opening") {
		state.doorAction = evt.value
		state.doorActionAt = evt.date
		if (checkAfter) {
			runIn(checkAfter, checkStatus)
		}
	} else if (evt.value == "closing") {
		state.doorAction = evt.value
		state.doorActionAt = evt.date
		if (checkAfter) {
			runIn(checkAfter, checkStatus)
		}
	}
}

def checkStatus() {
	def sensorCurrentValue = closeSensor.currentValue("contact")
	log.debug "checkStatus() $state.doorAction sensorCurrentValue=$sensorCurrentValue"
	if (state.doorAction == "opening") {
		if (sensorCurrentValue != "open") {
			trySendNotification("Door failed to open, door opened at ${getFormattedTime(state.doorActionAt)}.")
		} else {}
	}
	
	if (state.doorAction == "closing") {
		if (sensorCurrentValue != "closed") {         	
			trySendNotification("Door failed to close, door closed at ${getFormattedTime(state.doorActionAt)}.")
		} else {}
	}
}

private trySendNotification(String msg) {
	if (sendMsg != "No") {
		sendPush(msg)
	}
}
private String getFormattedTime(Date dt) {
	if (!dt) { return "" }
	def tz = location.getTimeZone()
    if (!tz) { tz = TimeZone.getTimeZone("CST") }
    return dt.format('h:mm a', tz)
}