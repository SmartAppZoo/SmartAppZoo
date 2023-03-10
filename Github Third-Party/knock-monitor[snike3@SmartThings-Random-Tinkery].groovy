/**
 *  Copyright 2015 Tim Polehna
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
 *  Knock Monitor
 *
 *  Author: Tim Polehna
 *
 *  Sends a message to let you know that there was a knock at the door and it has not been opened. Based on Door Knocker
 *  and Laundry Monitor SmartApps.
 *
 *  Date: 2016-09-16
 */

definition(
	name: "Knock Monitor",
	namespace: "polehna",
	author: "Tim Polehna",
	description: "Sends a message to let you know that there was a knock at the door and it has not been opened.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png"
)

preferences {
	section("Monitor vibration here:"){
		input "sensor1", "capability.accelerationSensor", title: "Sensor?", required: true
        input "threshold", "decimal", title: "Vibration Threshold (in ms)", 
        	Description: "This value varies by the door material and the sensor being used, trial and error is the best way to get this value. A longer threshold should mean harder knocks.",
        	required: true, defaultValue: 13000
	}
	section("Monitor the status of this door:"){
		input "door", "capability.contactSensor", title: "Open/Close Sensor?", required: true
		input "openDelay", "decimal", title: "Time after knock (in seconds)", required: false, defaultValue: 5
        input "lock", "capability.lock", title: "Lock? (Optional)", required: false,
        	description: "Filter out any false positives due to the lock mechanism vibrations."
	}
	section("Notifications"){
    	input "toPush", "bool", title: "Send push notification?", required: false, defaultValue: true
        input("recipients", "contact", title: "Send text message to") {
            input "phone", "phone", title: "Send text message to phone number", required: false
        }
	}
}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(sensor1, "acceleration.active", accelerationActiveHandler)
	subscribe(sensor1, "acceleration.inactive", accelerationInactiveHandler)
}

def accelerationActiveHandler(evt) {
	log.trace "vibration"
	if (!state.isRunning) {
    	def proceed = true
        
    	if (lock) {
        	def lockChanges = lock.eventsSince(new Date(now() - 1000))
            
            if (lockChanges) {
                if (lockChanges.find(it.value == "lock")) {
                    log.debug "Door was locked, ignoring vibration"
                    proceed = false
                } else if (lockChanges.find(it.value == "unlock")) {
                    log.debug "Door was unlocked, ignoring vibration"
                    proceed = false
                }
        	}
        }
        
        if (proceed) {
            log.info "Arming detector"
            state.isRunning = true
            state.startedAt = now()
        }
	}
	state.stoppedAt = null
    
}

def accelerationInactiveHandler(evt) {
	log.trace "no vibration, isRunning: $state.isRunning"
	if (state.isRunning) {
		log.debug "startedAt: ${state.startedAt}, stoppedAt: ${state.stoppedAt}"
		if (!state.stoppedAt) {
			state.stoppedAt = now()
            
            def duration = state.stoppedAt - state.startedAt
            
            log.debug "threshold: ${threshold} duration: ${duration}"
            if (duration >= threshold) {
            	def runDelay = Math.floor((openDelay * 1000 - (duration - threshold)) / 1000).toInteger()
                
                log.debug "checkRunning in ${runDelay}"
            	runIn(runDelay, checkRunning, [overwrite: false])
            } else {
				log.debug "Not sending notification because insufficient amount of vibration was detected"
                disarm()
			}
		}
	}
}

def checkRunning() {
	log.trace "checkRunning()"
	if (state.isRunning) {
        def doorStates = door.statesSince("contact", new Date(state.startedAt))

		if (!doorStates.find{it.value == "open"}) {
            log.debug "Sending notification"

            def msg = "Knock at ${sensor1.displayName}"
            log.info msg

            if (location.contactBookEnabled) {
                sendNotificationToContacts(msg, recipients)
            }
            else {
                if (toPush) {
                    sendPush msg
                }
                if (phone) {
                    sendSms phone, msg
                }
            }
		} else {
			log.debug "skipping notification because door was opened"
		}
        disarm()
	}
	else {
		log.debug "detector was disarmed"
	}
}

def disarm()
{
    state.isRunning = false
    log.info "Disarming detector"
}
