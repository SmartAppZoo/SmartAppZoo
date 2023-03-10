/**
 *  Automatic Door Lock with Delay
 *
 *  Copyright 2017 Won Tchoi
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
    name: "Automatic Door Lock with Delay",
    namespace: "emx2500",
    author: "Won Tchoi",
    description: "Automatically lock a door after some delay after the door is closed.  Requires that you have a sensor to detect when the door is closed.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity@2x.png")


preferences {
	section("Select the door to lock:") {
        input "theLock", "capability.lock", required: true, title: "Which lock?"
    }
    
    section("Select the sensor to use:") {
        input "theSensor", "capability.contactSensor", required: true, title: "Which sensor?"
    }
    
    section("Delay in minutes before locking:") {
        input "delayTime", "number", required: true, title: "Delay (in minutes)?"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(theSensor, "contact.closed", sensorClosedHandler)
}

def sensorClosedHandler(evt) {
    log.debug "sensorClosedHandler called: $evt"
    //delay before running lock handler
    runIn(60*delayTime, delayedLockHandler)
}

def delayedLockHandler(evt) {
    log.debug "delayedLockHandler called: $evt"
    theLock.lock()
}