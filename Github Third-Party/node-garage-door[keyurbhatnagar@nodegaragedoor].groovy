/**
 *  Copyright 2015 SmartThings
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
 *  Node Garage Door
 *
 *  Author: Keyur
 *  Date: 2019-01-21
 *
 * Uses a nodemcu and door contact and acceleration sensor to simulate a Garage door opener.
 */

definition(
    name: "Node Garage Door",
    namespace: "keyurbhatnagar",
    author: "Keyur Bhatnagar",
    description: "Implements Garage door funtion using nodemcu and a door sensor",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {a

	section("Garage door") {
		input "doorSensor", "capability.contactSensor", title: "Which sensor?"
		input "doorSwitch", "capability.momentary", title: "Which switch?"
        input "garageDoor", "capability.garageDoorControl", title: "Which garage door?"
        input "doorAcceleration", "capability.accelerationSensor", title: "Acceleration sensor?"
		input "openThreshold", "number", title: "Warn when open longer than (optional)",description: "Number of minutes", required: false
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
	}
}

def installed() {
	log.trace "installed()"
    
    /*def dni = "543817191"
    def garageDoor = getChildDevice(dni)
	if(!garageDoor) {
    	garageDoor = addChildDevice("keyurbhatnagar", "Node Garage Door Opener", dni, null, [name:"Garage", label:name])
    	log.debug "created ${garageDoor.displayName} with id $dni"
        if(!garageDoor.currentDoor) {
    		if(doorSensor.currentDoor) {
        		garageDoor.setDoorState(doorSensor.currentDoor)
        	}
    	}
  	} else {
    	log.debug "Device already created"
  	}*/
    
	subscribe()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
	subscribe()
}

def subscribe() {
	log.trace "subscribe()"
    subscribe(garageDoor, "control", garageDoorControl)
	subscribe(doorSensor, "contact", garageDoorContact)
    subscribe(doorAcceleration, "acceleration", accelerationActive)
}

def garageDoorControl(evt)
{
	log.info "garageDoorControl, $evt.name: $evt.value"
    
    //def dni = "543817191"
    //def garageDoor = getChildDevice(dni)
	//if(garageDoor) {
		if (evt.value == "opening") {
        	openDoor()
        }
        
        if (evt.value == "closing") {
        	closeDoor()
        }
    //} else {
    //	log.debug "Device not Found!"
    //}
}

def garageDoorContact(evt)
{
	log.info "garageDoorContact, $evt.name: $evt.value"
    
    //def dni = "543817191"
    //def garageDoor = getChildDevice(dni)
	//if(garageDoor) {
		if (evt.value == "open") {
        	log.debug "Setting Device State: Opening"
    		garageDoor.setDoorState('opening')
		}
		else {
        	log.debug "Setting Device State: Closed"
    		garageDoor.setDoorState('closed')
		}
    //} else {
    //	log.debug "Device not Found!"
    //}
}


def accelerationActive(evt)
{
	log.info "$evt.name: $evt.value"

	//def dni = "543817191"
    //def garageDoor = getChildDevice(dni)
	//if(garageDoor) {
    	log.info "garageDoor.currentDoor: $garageDoor.currentDoor"
		if (garageDoor.currentDoor == "closed") {
        	if(evt.value == "active") {
        		log.debug "Door was closing. Won't set the state, will let the contact sensor set the opening state"
        		//garageDoor.setDoorState('opening')
			} else {
        		log.debug "Current state closed, no change on inactive"
        	}
		}
    
    	if (garageDoor.currentDoor == "opening") {
        	if(evt.value == "inactive") {
				log.debug "setting door state: open"
        		garageDoor.setDoorState('open')
			} else {
        		log.debug "Current state opening, no change on active"
        	}
		}
    
    	if (garageDoor.currentDoor == "closing") {
        	if(evt.value == "inactive") {
				log.debug "Door was closing. Won't set the state, will let the contact sensor set the closed state"
			} else {
        		log.debug "Current state closing, no change on active"
        	}
		}
    
    	if (garageDoor.currentDoor == "open") {
        	if(evt.value == "active") {
				log.debug "setting door state: closing"
        		garageDoor.setDoorState('closing')
			} else {
        		log.debug "Current state open, no change on inactive"
        	}
		}
    //} else {
    //	log.debug "Device not Found!"
    //}
}

private openDoor()
{
	log.debug "Opening Door!"
	//def dni = "543817191"
    //def garageDoor = getChildDevice(dni)
	//if(garageDoor) {
		if (garageDoor.currentDoor == "closed") {
			log.debug "opening door"
			doorSwitch.push()
		}
    //} else {
    //	log.debug "Device not Found!"
    //}
}

private closeDoor()
{
	log.debug "Closing Door!"
	//def dni = "543817191"
    //def garageDoor = getChildDevice(dni)
	//if(garageDoor) {
		if (garageDoor.currentDoor == "open") {
			log.debug "closing door"
			doorSwitch.push()
		}
    //} else {
    //	log.debug "Device not Found!"
    //}
}