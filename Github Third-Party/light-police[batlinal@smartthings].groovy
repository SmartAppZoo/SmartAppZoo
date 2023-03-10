/**
 *  Light Police
 *
 *  Copyright 2017 Alex Batlin
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
    name: "Light Police",
    namespace: "alex.batlin",
    author: "Alex Batlin",
    description: "Turns off selected lights every selected number of minutes if sensor lux level is equal to or greater than set value.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select which lights should be switched off how often when light level is above specified threshold.") {
		input "bulbs","capability.switch", multiple: true, title: "Which light bulbs?"
        input "lightSensor", "capability.sensor", title: "Which light sensor?"
        input name: "triggerLux", type: "decimal", title: "Lux minimum value?"
        input name: "interval", type: "enum", title: "Patrol interval?", options: ["5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {        
    switch(interval) {
    	case "5 Minutes":
        	log.debug "Setting interval schedule to: 5 minutes"
        	runEvery5Minutes(policeAction)
            break
        case "10 Minutes":
        	log.debug "Setting interval schedule to: 10 minutes"
        	runEvery10Minutes(policeAction)
            break
        case "15 Minutes":
        	log.debug "Setting interval schedule to: 15 minutes"
        	runEvery15Minutes(policeAction)
            break
        case "30 Minutes":
        	log.debug "Setting interval schedule to: 30 minutes"
        	runEvery30Minutes(policeAction)
            break    
        case "1 Hour":
        	log.debug "Setting interval schedule to: 1 hour"
        	runEvery1Hour(policeAction)
            break
        case "3 Hours":
        	log.debug "Setting interval schedule to: 3 hours"
        	runEvery3Hours(policeAction)
            break            
		default :
        	log.debug "Setting default interval schedule to: 5 minutes"
        	runEvery5Minutes(policeAction)
    }
}

def policeAction() {    
	def currentLux = lightSensor.currentIlluminance
    log.debug "Patrol settings: currentLux=${currentLux}, triggerLux=${triggerLux}"
    
    if (currentLux >= triggerLux) {
    	log.debug "it's sunny so time to switch off lights" 
        bulbs?.each {
            log.debug "${it.name} value ${it.currentSwitch}" 

            if (it.currentSwitch == "on"){
                it.off();
            }
        }
    } else {
    	log.debug "it's dark so keep those lights on" 
    }
}
