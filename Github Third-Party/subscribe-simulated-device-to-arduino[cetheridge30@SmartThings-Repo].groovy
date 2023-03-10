/**
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
 *  Subscribe Simulated Device to Arduino
 *
 *  Author: Christopher Etheridge
 *
 *  Date: 2015-10-17
 */
definition(
	name: "Subscribe Simulated Device to Arduino",
	namespace: "cetheridge30",
	author: "Christopher Etheridge",
	description: "Subscribes and Updates Simulated Device Based on Arduino PINs",
	category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"

)

preferences {
	section("Select arduino that sensors are connected to.") {
		input "arduino", "capability.contactSensor", title: "Select...", required: true
    }
	section("Select the simulated sensor you would like to attach to Office Windows.") {
        input "office", title: "Select...", "capability.contactSensor"
	}
	section("Select the simulated sensor you would like to attach to Dining Room Windows.") {
        input "diningroom", title: "Select...", "capability.contactSensor"
	}
	section("Select the simulated sensor you would like to attach to Bedroom Windows.") {
        input "bedroom", title: "Select...", "capability.contactSensor"
	}
	section("Select the simulated sensor you would like to attach to Livingroom Windows.") {
        input "livingroom", title: "Select...", "capability.contactSensor"
	}
	section("Select the simulated sensor you would like to attach to Kitchen Windows.") {
        input "kitchen", title: "Select...", "capability.contactSensor"
	}
	section("Select the simulated sensor you would like to attach to the Front Door.") {
        input "frontdoor", title: "Select...", "capability.contactSensor"
	}
	section("Select the simulated sensor you would like to attach to the Back Door.") {
        input "backdoor", title: "Select...", "capability.contactSensor"
	}
	section("Select the simulated sensor you would like to attach to the Garage Door.") {
        input "garagedoor", title: "Select...", "capability.contactSensor"
	}   
}

def installed()
{   
	subscribe()
    log.debug "Sending poll to update status on inital install"
    arduino.poll()
}

def updated()
{
	unsubscribe()
   	subscribe()
}

def subscribe()
{

		subscribe(arduino, "office.open", officeOpenHandler)
        subscribe(arduino, "office.closed", officeClosedHandler)
        subscribe(arduino, "diningroom.open", diningroomOpenHandler)
        subscribe(arduino, "diningroom.closed", diningroomClosedHandler)
        subscribe(arduino, "bedroom.open", bedroomOpenHandler)
        subscribe(arduino, "bedroom.closed", bedroomClosedHandler)
        subscribe(arduino, "livingroom.open", livingroomOpenHandler)
        subscribe(arduino, "livingroom.closed", livingroomClosedHandler)
        subscribe(arduino, "kitchen.open", kitchenOpenHandler)
        subscribe(arduino, "kitchen.closed", kitchenClosedHandler)
        subscribe(arduino, "frontdoor.open", frontdoorOpenHandler)
        subscribe(arduino, "frontdoor.closed", frontdoorClosedHandler)
        subscribe(arduino, "backdoor.open", backdoorOpenHandler)
        subscribe(arduino, "backdoor.closed", backdoorClosedHandler)
        subscribe(arduino, "garagedoor.open", garagedoorOpenHandler)
        subscribe(arduino, "garagedoor.closed", garagedoorClosedHandler)
        
 }

def officeOpenHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    office.open()
}

def officeClosedHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    office.close()
}

def diningroomOpenHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    diningroom.open()
}

def diningroomClosedHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    diningroom.close()
}
def bedroomOpenHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    bedroom.open()
}

def bedroomClosedHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    bedroom.close()
}
def livingroomOpenHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    livingroom.open()
}

def livingroomClosedHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    livingroom.close()
}
def kitchenOpenHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    kitchen.open()
}

def kitchenClosedHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    kitchen.close()
}
def frontdoorOpenHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    frontdoor.open()
}

def frontdoorClosedHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    frontdoor.close()
}
def backdoorOpenHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    backdoor.open()
}

def backdoorClosedHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    backdoor.close()
}
def garagedoorOpenHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    garagedoor.open()
}

def garagedoorClosedHandler(evt) {
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    garagedoor.close()
}