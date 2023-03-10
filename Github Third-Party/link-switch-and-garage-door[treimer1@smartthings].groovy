/**
 *  Link Switch and Garage Door
 *
 *  Link state between a switch and a garage door. Switch On <=> Open, Switch Off <=> Close
 *
 *  based on
 *. https://github.com/aderusha/SmartThings/blob/master/Link-Switch-And-Lock.groovy
 *  Copyright 2015 aderusha
 *  Version 1.0.0 - 2015-09-13 - Initial release
 *
 *  I want to be able to control a garage door device from a SmartApp that can only handle switch devices.  
 *  This SmartApp will link a garage door and a button/switch device such that:
 *  - Opening the door will turn on the switch
 *  - Closing the door will turn off the switch
 *  - Turning on the switch will open the door
 *  - Turning off the switch will close the door
 *
 *  To use this SmartApp, first create a virtual device using the On/Off Button Tile device type.
 *  Install this SmartApp, select your virtual On/Off Button Tile, then your garage door.  
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
	name: "Link Switch and Garage Door",
	namespace: "smartthings",
	author: "treimer",
	description: "Link state between a switch and a garage door. Switch On <=> Open, Switch Off <=> Closed",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("When this switch is turned on") {
		input "switch1", "capability.switch", multiple: false, required: true
	}
	section("Open this garage door") {
		input "door1", "capability.doorControl", multiple: false, required: true
	}
}    

def installed()
{   
	subscribe(switch1, "switch.on", onHandler)
	subscribe(switch1, "switch.off", offHandler)
	subscribe(door1, "door.open", openHandler)
	subscribe(door1, "door.closed", closedHandler)
}

def updated()
{
	unsubscribe()
	subscribe(switch1, "switch.on", onHandler)
	subscribe(switch1, "switch.off", offHandler)
	subscribe(door1, "door.open", openHandler)
	subscribe(door1, "door.closed", closedHandler)
}

def onHandler(evt) {
	log.debug evt.value
	log.debug "Opening door: $door1"
	door1.open()
}

def offHandler(evt) {
	log.debug evt.value
	log.debug "Closing door: $door1"
	door1.close()
}

def openHandler(evt) {
	log.debug evt.value
	log.debug "Turning on switch: $switch1"
   	switch1.on()
}

def closedHandler(evt) {
	log.debug evt.value
	log.debug "Turning off switch: $switch1"
   	switch1.off()
}