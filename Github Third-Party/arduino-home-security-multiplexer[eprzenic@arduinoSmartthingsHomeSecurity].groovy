/**
 *  ST_Anything Doors Multiplexer - ST_Anything_Doors_Multiplexer.smartapp.groovy
 *
 *  Copyright 2015 Charles Schwer
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
 *
 */
 
definition(
    name: "Arduino Home Security Multiplexer",
    namespace: "cschwer",
    author: "Charles Schwer",
    description: "Connects single Arduino with multiple ContactSensor and MotionSensor devices to their virtual device counterparts.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {

	section("Select the Motion Sensors (Virtual Motion Sensor devices)") {
		input "hallway", title: "Hallway Motion Sensor", "capability.motionSensor"
	}

	section("Select the House Doors (Virtual Contact Sensor devices)") {
		input "frontdoor", title: "Virtual Contact Sensor for Front Door", "capability.contactSensor"
		input "backdoor", title: "Virtual Contact Sensor for Back Door", "capability.contactSensor"
		input "sidedoor", title: "Virtual Contact Sensor for Dining Room Door", "capability.contactSensor"
	}

	section("Select the Arduino Home Security device") {
		input "arduino", "capability.contactSensor"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe()
}

def subscribe() {
	subscribe(arduino, "hallmotion.active", hallMotionActive)
    subscribe(arduino, "hallmotion.inactive", hallMotionInactive)
    
    subscribe(arduino, "frontdoor.open", frontDoorOpen)
    subscribe(arduino, "frontdoor.closed", frontDoorClosed)
    
    subscribe(arduino, "backdoor.open", backDoorOpen)
    subscribe(arduino, "backdoor.closed", backDoorClosed)

	subscribe(arduino, "sidedoor.open", sideDoorOpen)
    subscribe(arduino, "sidedoor.closed", sideDoorClosed)

}

// --- Front Door --- 
def frontDoorOpen(evt)
{
    if (frontdoor.currentValue("contact") != "open") {
    	log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    	frontdoor.open()
    }
}

def frontDoorClosed(evt)
{
    if (frontdoor.currentValue("contact") != "closed") {
		log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    	frontdoor.close()
    }
}

// --- back Door --- 
def backDoorOpen(evt)
{
    if (backdoor.currentValue("contact") != "open") {
		log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    	backdoor.open()
    }
}

def backDoorClosed(evt)
{
    if (backdoor.currentValue("contact") != "closed") {
		log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    	backdoor.close()
	}
}

// --- Dining Room Door --- 
def sideDoorOpen(evt)
{
    if (sidedoor.currentValue("contact") != "open") {
		log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    	sidedoor.open()
	}
}

def sideDoorClosed(evt)
{
    if (sidedoor.currentValue("contact") != "closed") {
		log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    	sidedoor.close()
	}
}

// --- Hallway Motion Sensor --- 
def hallMotionActive(evt)
{
    if (hallmotion.currentValue("motion") != "active") {
		log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    	hallmotion.active()
	}
}

def hallMotionInactive(evt)
{
    if (hallmotion.currentValue("motion") != "inactive") {
		log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    	hallmotion.inactive()
	}
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}