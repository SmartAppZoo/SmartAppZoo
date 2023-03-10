/**
 *  GLM Service Manager
 *
 *  Copyright 2019 Ian Perry
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
    name: "GLM Service Manager",
    namespace: "cosmicc",
    author: "Ian Perry",
    description: "Galaxy Lighting Module Service Manager",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		input "glms", "capability.presenceSensor", multiple: true
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
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(location, "mode", modeChangeHandler)
    //schedule("*/5 * * * * ?", poll_devices)
}

// TODO: implement event handlers

def poll_devices() {
 	glms.poll()
}

mappings {
    path("/motion/:whichdevice/:motion") {
        action: [PUT: "setmotion"]
    }
    path("/presence/:whichdevice/:presence") {
        action: [GET: "setpresence"]
    }
}

def setmotion() {
	log.debug "Webhook Recieved from Device: ${params.whichdevice} Motion: ${params.motion}"
	for (each in glms) {
    	log.trace "${each}"
    	if (each == params.whichdevice) {
        	log.trace "2"
        	if (params.motion == "on") {
            	log.trace "${each} motion ${params.motion}"
    			each.onmotion()
            } else {
            	log.trace "${each} motion ${params.motion}"
            	each.offmotion()
            }
    	}
    }
}

def setpresence() {
	log.debug "Webhook Recieved from Device: ${params.whichdevice} Presence: ${params.presence}"
	for (each in glms) {
    	if (each == params.whichdevice) {
        	if (params.presence == "present") {
    			each.onpresence()
            } else {
            	each.offpresence()
            }
    	}
    }
}

def modeChangeHandler(evt) {
	if (evt.value == "Away") {
		log.debug "Changing Mode to ${evt.value}"
    	glms.awayon()
    }
    if (evt.value == "Home") {
		log.debug "Changing Mode to ${evt.value}"
   		glms.awayoff()
    }
}