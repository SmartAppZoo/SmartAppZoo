/**
 *  Frozen
 *
 *  Copyright 2017 Philippe Gravel
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
    name: "Frozen",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Start Heat thermostat if outside temperature drop bellow set temperature",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
        input "thermos", "capability.thermostat", title: "Thermostats:", multiple: true, required: true
        input "lower", "number", title: "Lower than", required: true
        input "setToHeat", "number", title: "Heat At:", required: true
        input "resetTo", "number", title: "Reset to:", required: true
    }    
    
    section("Send Notifications?") {
		input("recipients", "contact", title: "Send notifications to", multiple: true, required: false)

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
	subscribe(thermos, "outsideTemp", temperatureChangeHandler)
    
    CheckTemperature(thermos.currentValue("outsideTemp")[0])
}

def temperatureChangeHandler(evt) {

	def outsideTemp = evt.value
	log.debug "Temp Event: ${evt}\nOutside Temp: ${outsideTemp}" 

	CheckTemperature(outsideTemp)
}

def CheckTemperature(outsideTemp) {
	log.debug "Temp Event: CheckTemperature [${outsideTemp}]" 

	if (outsideTemp <= (-lower)) {
			log.debug "Temp Event: brrrrrr ${checkTemp}" 

   		if ((location.currentMode == "Home") || (location.currentMode == "Evening")) {   
            thermos.each { currentThermos ->
            	
 				log.debug "Temp Event: thermos Name: ${currentThermos}"         
        		if (currentThermos.currentValue("heatingSetpoint") < setToHeat) {    
	        		log.debug "Temp Event: Change thermostats Heat point to ${setToHeat}" 
    	    		currentThermos.setHeatingSetpoint(setToHeat)
                }
            }
    	}
    } else {
        thermos.each { currentThermos ->
            log.debug "Temp Event: thermos Name: ${currentThermos}"         
            if (currentThermos.currentValue("heatingSetpoint") == setToHeat) {
                log.debug "Temp Event: Change thermostats Heat point to ${resetTo}" 
                currentThermos.setHeatingSetpoint(resetTo)
            }
        }
    }
}