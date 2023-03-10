/**
 *  Garage Door Controller
 *
 *  Copyright 2019 Indu Prakash
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
definition(
    name: "Garage Door Controller",
    namespace: "induprakash",
    author: "Indu Prakash",
    description: "Controls a Garage Door device with open closed sensors and a switch.",
    category: "Convenience",
	iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation14-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation14-icn@2x.png")   

preferences {
	section() {
    	paragraph "Garage door device to use. This device should be of the type Garage Door Device Handler."
		input "virtualDoor", "capability.doorControl", title: "Garage Door?", required: true
	}
	section() {
    	paragraph "Verify if door opened/closed correctly"
		input "checkAfter", "number", title: "Delay (seconds) after which to check?", required: false, defaultValue: 20
	}
	section("Notifications") {
		input "sendMsg", "boolean", title: "Send notification?", defaultValue: false, displayDuringSetup: true
	}
	section("Logging") {
		input "debugLogging", "boolean", title: "Enable debug logging?", defaultValue: false, displayDuringSetup: true
	}
}

//Public
def installed() {
	log.debug "installed() with settings: $settings"
	initialize()
}
def updated() {
	unsubscribe()
	unschedule()
	initialize()
}
def initialize() {
	if (virtualDoor.hasCommand("setDoorStatus") && virtualDoor.hasCommand("setStatus")) {
		subscribe(virtualDoor, "door", doorHandler)
		subscribe(virtualDoor, "notify", doorNotificationHandler)
        subscribe(virtualDoor, "closedsensor", closedSensorHandler)
        subscribe(virtualDoor, "opensensor", openSensorHandler)
		updateVirtual()
	}
	else {
		log.error("Unsupported virtual garage door, it has to be a XIP Virtual Garage Door device.")
	}

	//runEvery5Minutes(updateVirtual)
}

//Private
def updateVirtual() {	
    def closedSensorCurrentValue = virtualDoor.currentValue("closedsensor")
    def openSensorCurrentValue = virtualDoor.currentValue("opensensor")
	def doorCurrentValue = virtualDoor.currentValue("door")
	logDebug "updateVirtual() doorCurrentValue=$doorCurrentValue, closedSensor=$closedSensorCurrentValue, openSensor=$openSensorCurrentValue"
	if (closedSensorCurrentValue == "closed") {
		if (openSensorCurrentValue == "closed") {
			notifyUsers("Both sensors reported closed, sensors might be malfunctioning.")
		}
		virtualDoor.setDoorStatus("closed")
	}
	if (closedSensorCurrentValue == "open") {
		if (openSensorCurrentValue == "closed") {
			virtualDoor.setDoorStatus("open")
		}
		else if (doorCurrentValue == "opening") {
			//Door is opening, don't change value
		}
		else {
			//Door could be partially open, start with "open" state.
			virtualDoor.setDoorStatus("open")
		}
	}
}
def closedSensorHandler(evt) {
	def doorCurrentValue = virtualDoor.currentValue("door")
	logDebug "closedSensorHandler($evt.value) door is $doorCurrentValue"
	if (evt.value == "open" && doorCurrentValue != "opening") { //Garage door opened through the physical button        
		notifyUsers("Garage door manually opened.")
		virtualDoor.setDoorStatus("opening")
	}
	if (evt.value == "closed" && doorCurrentValue != "closed") {
		virtualDoor.setDoorStatus("closed")
	}
}
def openSensorHandler(evt) {
	def doorCurrentValue = virtualDoor.currentValue("door")
	logDebug "openSensorHandler($evt.value) door is $doorCurrentValue"
	if (evt.value == "closed" && doorCurrentValue != "open") {
		virtualDoor.setDoorStatus("open")
	}
	if (evt.value == "open" && doorCurrentValue != "closing") {
		notifyUsers("Garage door manually closed.")
		virtualDoor.setDoorStatus("closing")
	}
}
def doorNotificationHandler(evt) {
	logDebug "doorNotificationHandler($evt.value)"
	notifyUsers(evt.value)
}
def doorHandler(evt) {
	logDebug "doorHandler($evt.value)"
	if (evt.value == "opening" || evt.value == "closing") {
		if (checkAfter) {
			runIn(checkAfter, checkStatus, [data: [doorActionAt: getFormattedTime(evt.date)]]) //the default behavior is to overwrite the pending schedule
		}
	}
}
def checkStatus(data) {
	def doorCurrentValue = virtualDoor.currentValue("door")	
    def closedSensorCurrentValue = virtualDoor.currentValue("closedsensor")
    def openSensorCurrentValue = virtualDoor.currentValue("opensensor")
	logDebug "checkStatus() door=$doorCurrentValue, closedSensor=$closedSensorCurrentValue, openSensor=$openSensorCurrentValue"

	if (doorCurrentValue == "opening") {
		if (openSensorCurrentValue == "open") {
			if (closedSensorCurrentValue == "closed") {
				notifyUsers("Door failed to open and instead closed, opened at $data.doorActionAt.")
				virtualDoor.setDoorStatus("closed")
			}
			else {
				notifyUsers("Door failed to open, opened at $data.doorActionAt.")
			}
		}
		else if (openSensorCurrentValue == "closed") {
			//Door was quickly closed and opened, openSensor still reports closed. Treat door as open.
			virtualDoor.setDoorStatus("open")
		}
	}
	if (doorCurrentValue == "closing") {
		if (closedSensorCurrentValue == "open") {
			if (openSensorCurrentValue == "closed") {
				notifyUsers("Door failed to close and instead opened, closed at $data.doorActionAt.")
				virtualDoor.setDoorStatus("open")
			}
			else {
				notifyUsers("Door failed to close, closed at $data.doorActionAt.")
			}
		}
		else if (closedSensorCurrentValue == "closed") {
			//Door was quickly opened and closed, closedSensor still reports closed. Treat door as closed.
			virtualDoor.setDoorStatus("closed")
		}
	}
}

def notifyUsers(String msg) {
	if (sendMsg) {
		sendPush(msg)
	}
}
def String getFormattedTime(Date dt) {
	if (!dt) {
		return ""
	}
	def tz = location.getTimeZone()
	if (!tz) {
		tz = TimeZone.getTimeZone("CST")
	}
	return dt.format('h:mm a', tz)
}
def logDebug(String msg) {
	if (debugLogging) {
		log.debug(msg)
	}
}