/**
 *  Button mode change 
 *
 *  Copyright 2014 Eric Roberts
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
    name: "Button mode change ",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "Sets the mode from a button",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When this button is pressed") {
		input "button", "capability.switch", title: "Which?"
	}
	section("Change to this mode") {
		input "newMode", "mode", title: "Mode?"
	}
    section("Turn off these lights") {
    	input "offSwitches", "capability.switch", title: "Which?", required: false, multiple: true
    }
    section("Turn on these lights") {
    	input "onSwitches", "capability.switch", title: "Which?", required: false, multiple: true
    }
    section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat", required: false
	}
	section("Heat setting...") {
		input "heatingSetpoint", "number", title: "Degrees Fahrenheit?", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
		input "phoneNumber", "phone", title: "Send a Text Message?", required: false
	}

}

def installed() {
	log.debug "Current mode = ${location.mode}"
	createSubscriptions()
}

def updated() {
	log.debug "Current mode = ${location.mode}"
	unsubscribe()
	createSubscriptions()
}

def createSubscriptions()
{
	subscribe(button, "switch.on", onHandler)
	subscribe(location, modeChangeHandler)
    subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
	subscribe(thermostat, "temperature", temperatureHandler)

	if (state.modeStartTime == null) {
		state.modeStartTime = 0
	}
}

def modeChangeHandler(evt) {
	state.modeStartTime = now()
}


def onHandler(evt) {
	log.debug "changeMode, location.mode = $location.mode, newMode = $newMode, location.modes = $location.modes"
	offSwitches?.off()
    onSwitches?.on()
    if (thermostat) {
    	thermostat.setHeatingSetpoint(heatingSetpoint)
    	thermostat.poll()
    }
    if (location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
			send "${label} has changed the mode to '${newMode}'"
		}
		else {
			send "${label} tried to change to undefined mode '${newMode}'"
		}
	}
}

def heatingSetpointHandler(evt)
{
	log.debug "heatingSetpoint: $evt, $settings"
}

def coolingSetpointHandler(evt)
{
	log.debug "coolingSetpoint: $evt, $settings"
}

def temperatureHandler(evt)
{
	log.debug "currentTemperature: $evt, $settings"
}

private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phoneNumber ) {
		log.debug( "sending text message" )
		sendSms( phoneNumber, msg )
	}

	log.debug msg
}