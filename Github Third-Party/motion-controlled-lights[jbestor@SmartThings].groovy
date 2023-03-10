/**
 *  Motion-controlled Lights
 *
 *  Copyright 2020 Alan Buckner
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
    name: "Motion-controlled Lights",
    namespace: "TexasFlyer",
    author: "Alan Buckner",
    description: "Turn on lights when motion detected and turns off lights after x minutes without movement. Similar to \u2018When quiet\u2019 but without the bugs",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
 
	section {
		input(name: "motion", type: "capability.motionSensor", title: "Pick your motion sensor", description: null, multiple: false, required: true, submitOnChange: true)
		input(name: "dimmer", type: "capability.switchLevel", title: "Pick your dimming lamp(s)", description: null, multiple: false, required: true, ubmitOnChange: true)
		input(name: "dimmerValue", type: "number", title: "Enter dimmer level percentage (no % sign)", required: true, submitOnChange: true)
		input(name: "delayValue", type: "number", title: "Delay (in minutes) before turning off lights", required: true, submitOnChange: true)
	}
}

def installed() {
		log.debug "Installed with settings: ${settings}"
initialize()
}

 
def updated() {
	log.debug "Updated with settings: ${settings}"
	//unsubscribe()
	//unschedule() //Added based on another program. Not sure if needed.
	initialize()
}

def initialize() {
	subscribe(motion, "motion", myMotionHandler)
	dimmer.setLevel(0) // Easier to debug when you know that motion turns on the lights
    sendNotificationEvent("Motion-controlled lights is initializing…")
    state.TurnoffHandlerCalled = false
}

def myMotionHandler(evt) {
  	if ("active" == evt.value) {
		sendNotificationEvent("Motion detected - turning lights turned on")
		dimmer.setLevel(settings.dimmerValue)
		state.isActive = true
	} 
    else if ("inactive" == evt.value) {
		log.debug("Inactivity detected.  TurnoffHandlerCalled = ${state.TurnoffHandlerCalled}")
		state.isActive = false
		if (state.TurnoffHandlerCalled == false) {
			sendNotificationEvent("Calling handlerTurnoff in ${settings.delayValue} minutes.")
			def delaySeconds = settings.delayValue * 60
			runIn(delaySeconds, handlerTurnoff) //in seconds
			state.TurnoffHandlerCalled = true //Avoids calling the handler over and over again
		}
	}
}

def handlerTurnoff() {
	// Check to see if Motion was recently active or not. Avoids turning off the lamp right after an Active event
	// which then requires a period of inactivity and then activity to make the light come back on
	if (state.isActive == false) {
		dimmer.setLevel(0)
		sendNotificationEvent("handlerTurnoff just turn off the motion lamps due to no recent activity")
	} 
    else {
		dimmer.setLevel(settings.dimmerValue)
		sendNotificationEvent("handlerTurnoff just left the lamp on due to recent activity")
	}
	state.TurnoffHandlerCalled = false
}
