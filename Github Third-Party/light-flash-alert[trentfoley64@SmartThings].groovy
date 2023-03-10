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
 *  Light Flash Alert
 *
 *  Author: A. Trent Foley, Sr.
 *  Created: 2/9/2016
 *
 *  Flashes lights when a door is opened or a motion sensor activates
 */
definition (
    name: "Light Flash Alert",
    namespace: "trentfoley64",
    author: "A. Trent Foley, Sr.",
    description: "Flashes lights when a door is opened or a motion sensor activates.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet-luminance@2x.png"
)

preferences {
    section("Activations") {
    	input "buttonDevice", "capability.button", title: "Which?", required: true, multiple: false
    	input "motionSensors", "capability.motionSensor", title: "Motion Sensors?", required: true, multiple: true
    }
	section("Control these dimmers...") {
		input "dimmerDevices", "capability.switchLevel", title: "Which?", required: true, multiple: true
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    initialize()
}

def initialize() {
	log.debug "initialize"
	subscribe(buttonDevice, "button", testHandler)
    subscribe(motionSensors, "motion", eventHandler)
}

def testHandler(evt) {
	log.debug "testHandler: ${evt.value}"
	flashLights()
}

def eventHandler(evt) {
	log.debug "eventHandler: ${evt.value}"
    if (evt.value=="active") {
	   	flashLights()
	    flashLights()
    }
}

def flashLights() {
	def last=null
    //def savedLevels=dimmerDevices.level
    //def savedSwitches=dimmerDevices.switch
    //log.debug "savedLevels=${savedLevels}"
    //log.debug "savedSwitches=${savedSwitches}"
    dimmerDevices.each {
        last?.off()
        it.setLevel(30)
        it.on()
        last=it
    }
    last?.off()
    //dimmerDevices.level=savedLevels
    //dimmerDevices.switch=savedSwitches
}