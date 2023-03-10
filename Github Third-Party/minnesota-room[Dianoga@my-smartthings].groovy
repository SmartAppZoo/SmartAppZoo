/**
 *  Minnesota Room
 *
 *  Copyright 2015 Brian Steere
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
    name: "Minnesota Room",
    namespace: "dianoga",
    author: "Brian Steere",
    description: "Manage the temperature of a so-called \"Minnesota Room\"",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Minnesota Room") {
		input "roomTemp", "capability.temperatureMeasurement", title: "Temperature", required: true, multiple: false
        input "roomHeaters", "capability.switch", title: "Heater", required: true, multiple: true
        input "roomContactSensors", "capability.contactSensor", title: "Doors and Windows", multiple: true, required: false
        input "roomMinTemp", "number", title: "Minimum Temperature", description: "If temperature drops below this value, turn the heater on"
        input "roomOccupiedTemp", "number", title: "Occupied Temperature", description: "If house door is open, target this temperature"
	}
    
    section("House") {
    	input "houseTemp", "capability.temperatureMeasurement", title: "Temperature", required: true, multiple: false
        input "houseDoor", "capability.contactSensor", title: "Interior Door", required: false, multiple: false
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
	subscribe(roomTemp, 'temperature', eventHandler)
    subscribe(roomContactSensors, 'contact', eventHandler)
    subscribe(houseTemp, 'temperature', eventHandler)
    subscribe(houseDoor, 'contact', eventHandler)
    
    eventHandler();
}

def eventHandler(evt) {
	def currentRoomTemp = settings.roomTemp.currentTemperature
    def currentHouseTemp = settings.houseTemp.currentTemperature
    
    log.debug "Room: ${currentRoomTemp}, House: ${currentHouseTemp}"
    log.debug "Interior Door: ${settings.houseDoor.currentContact}, Room Doors: ${settings.roomContactSensors?.currentContact}"
    
    // If room temp > house temp, turn off heaters and return
    if(currentRoomTemp > currentHouseTemp || settings.roomContactSensors?.currentContact.contains('open')) {
    	settings.roomHeaters?.off();
        return;
    }
    
    // If interior door is open
    if(settings.houseDoor.currentContact == 'open') {
    	// If temp is below occupied temp, turn on heater
    	if(currentRoomTemp < settings.roomOccupiedTemp) {
    		settings.roomHeaters?.on();
        } else {
        	settings.roomHeaters?.off();
        }
    } else {
        // If temperature in room is below minimum, turn on heater
        if(currentRoomTemp < settings.roomMinTemp) {
            settings.roomHeaters?.on();
        } else {
        	settings.roomHeaters?.off();
        }
    }
}