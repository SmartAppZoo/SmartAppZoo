/**
 *  Window Or Door Open!
 *
 *  Copyright 2014 Y.Racine to use on any contact sensor
 *  linkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/ 
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
	name: "Window Or Door Open",
	namespace: "eco-community",
	author: "Yves Racine",
	description: "Choose a contact sensor and get a notification (with voice as an option) when it is left open for too long.  Optionally, turn off the HVAC and set it back to cool/heat when window/door is closed",
	category: "Safety & Security",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("About") {
		paragraph "WindowOrDoorOpen!, the smartapp that warns you if you leave a door or window open (with voice as an option);" +
			"it will turn off your thermostats (optional) after a delay and restore their mode when the contact is closed"
		paragraph "Version 1.9.4" 
		paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
			href url: "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=yracine%40yahoo%2ecom&lc=US&item_name=Maisons%20ecomatiq&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest",
				title:"Paypal donation..."
		paragraph "Copyright©2014 Yves Racine"
			href url:"http://github.com/yracine/device-type.myecobee", style:"embedded", required:false, title:"More information..."  
 				description: "http://github.com/yracine/device-type.myecobee/blob/master/README.md"
	}
	section("Notify me when the following door or window contact is left open...") {
		input "theSensor", "capability.contactSensor", required: true
	}
	section("Notifications") {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
		input "phone", "phone", title: "Send a Text Message?", required: false
	}
	section("Delay between notifications [default=1 minute]") {
		input "frequency", "number", title: "Number of minutes", description: "", required: false
	}
	section("Maximum number of notifications [default=5]") {
		input "givenMaxNotif", "number", title: "Max Number of Notifications", description: "", required: false
	}
	section("Use Speech capability to warn the residents [optional]") {
		input "theVoice", "capability.speechSynthesis", required: false, multiple: true
	}
	section("What do I use as the Master on/off switch for voice notifications? [optional]") {
		input "powerSwitch", "capability.switch", required: false
	}
	section("And, when contact is left open for more than this delay in minutes [default=5 min.]") {
		input "maxOpenTime", "number", title: "Minutes?", required:false
	}
	section("Turn off the thermostat(s) after the delay;revert this action when closed [optional]") {
		input "tstats", "capability.thermostat", multiple: true, required: false
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
	subscribe(theSensor, "contact.closed", sensorTriggered)
	subscribe(theSensor, "contact.open", sensorTriggered)
	clearStatus()
	state?.lastThermostatMode = null
}

def sensorTriggered(evt) {
	def freq = (frequency) ? (frequency * 60) : 60
	def max_open_time_in_min = maxOpenTime ?: 5 // By default, 5 min. is the max open time

	if (evt.value == "closed") {
		def openMinutesCount = (state.count * freq) /60
		if ((tstats) && (openMinutesCount > max_open_time_in_min)) {
			restore_tstats_mode()
		}		            
		def msg = "your $theSensor is now closed"
		send("WindowOrDoorOpen>${msg}")
		if ((theVoice) && (powerSwitch?.currentSwitch == "on")) { //  Notify by voice only if the powerSwitch is on
			theVoice.setLevel(30)
			theVoice.speak(msg)
		}
		clearStatus()
	} else if (evt.value == "open" && state.status != "scheduled") {
		save_tstats_mode()
		runIn(freq, takeAction, [overwrite: false])
		state.status = "scheduled"
		log.debug "$theSensor will be checked every ${(freq/60)} minute(s)"
	}
}

def takeAction() {
	def freq = (frequency) ? (frequency * 60) : 60
	def maxNotif = (givenMaxNotif) ?: 5
	def max_open_time_in_min = maxOpenTime ?: 5 // By default, 5 min. is the max open time
	def contactState = theSensor.currentState("contact")
	def msg
    
	log.trace "takeAction>Contact's status = $contactState.value, state.status=$state.status"

	if ((state.status == "scheduled") && (contactState.value == "open")) {
		state.count = state.count + 1
		log.debug "$theSensor was open too long, sending message (count=$state.count)"
		def openMinutesCount = (state.count * freq) /60
		msg = "your $theSensor has been open for more than ${openMinutesCount} minute(s)!"
		send("WindowOrDoorOpen>${msg}")
		if ((theVoice) && (powerSwitch?.currentSwitch == "on")) { //  Notify by voice only if the powerSwitch is on
			theVoice.setLevel(30)
			theVoice.speak(msg)
		}

		if ((tstats) && (openMinutesCount > max_open_time_in_min)) {
			tstats.off()
			msg = "thermostats are now turned off after ${max_open_time_in_min} minutes"
			send("WindowDoorOpen>${msg}")
		}
		if ((!tstats) && (state.count > maxNotif)) {
			// stop the repeated notifications if there is no thermostats provided and we've reached maxNotif
			clearStatus()
			unschedule(takeAction)
			return
		}
		runIn(freq, takeAction, [overwrite: false])
	} else if (contactState.value == "closed") {
		def openMinutesCount = (state.count * freq) /60
		if ((tstats) && (openMinutesCount > max_open_time_in_min)) {
			restore_tstats_mode()
		}		            
		clearStatus()
		unschedule(takeAction)
	}
}

def clearStatus() {
	state?.status = null
	state?.count = 0
}


private void save_tstats_mode() {

	state?.lastThermostatMode = null

	if (tstats) {
		state.lastThermostatMode = " "
		tstats.each {
			log.debug "save_tstats_mode>thermostat mode reset for $it"
			it.poll() // to get the latest value at thermostat            
			state.lastThermostatMode = state.lastThermostatMode + "${it.currentThermostatMode}" + ","
		}
	}
	log.debug "save_tstats_mode>state.lastThermostatMode= $state.lastThermostatMode"

}


private void restore_tstats_mode() {
	def msg
    
	if (tstats) {
		if (state.lastThermostatMode) {
			def lastThermostatMode = state.lastThermostatMode.toString().split(',')
			int i = 0
			tstats.each {
				def lastSavedMode = lastThermostatMode[i].trim()

				if (lastSavedMode) {
					log.debug "restore_tstats_mod>about to set ${it}, back to saved thermostatMode=${lastSavedMode}"
					if (lastSavedMode == 'cool') {
						it.cool()
					} else if (lastSavedMode.contains('heat')) {
						it.heat()
					} else if (lastSavedMode == 'auto') {
						it.auto()
					} else {
						it.off()
					}
					msg = "thermostat ${it}'s mode is now set back to ${lastSavedMode}"
					send("WindowOrDoorOpen>${theSensor} closed, ${msg}")
					if ((theVoice) && (powerSwitch?.currentSwitch == "on")) { //  Notify by voice only if the powerSwitch is on
						theVoice.speak(msg)
					}
				}
				i++
			}
		} else {
			tstats.auto()
			msg = "thermostats $tstats set to auto"
			send("WindowOrDoorOpen>${msg}")
			if ((theVoice) && (powerSwitch?.currentSwitch == "on")) { //  Notify by voice only if the powerSwitch is on
				theVoice.speak(msg)
			}
		}
	}
}



private send(msg) {
	if (sendPushMessage != "No") {
		log.debug("sending push message")
		sendPush(msg)
	}

	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, msg)
	}
	log.debug msg
}
