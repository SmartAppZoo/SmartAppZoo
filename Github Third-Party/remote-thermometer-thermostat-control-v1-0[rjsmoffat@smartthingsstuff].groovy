/**
 *  Thermometer Heating Radiator Control
 *
 *  Copyright 2018 Stuart Moffat
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
    name: "Remote Thermometer Thermostat Control v1.0",
    namespace: "rjsm",
    author: "Stuart Moffat",
    description: "This app allows you to control multiple heating thermostats from a remote thermometer.",  
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"

)

preferences {
    // Allows a remote thermometer to control another thermostatic valve by setting the thermostat to a maximum value when the thermometer is below it's setpoint and reversing when reached.
    section("Choose Thermometer") {
    	paragraph("Select the thermometer to control a thermostat.")
        input "thermometer", "capability.temperatureMeasurement", multiple: false, required: true, title: "Select"
    }
    
    section("Choose Thermostats") {
        input "thermostats", "capability.Thermostat", multiple: ture, required: true, title: "Select"
    }

	section("Set heating maximum value (<=28)") {
        input "highHeat", "degrees", required: true, title: "Degrees C"
    }
    
	section("Set heating minimum value (>8)") {
        input "lowHeat", "degrees", required: true, title: "Degrees C"
    }    
    

}

def installed(){
log.debug "Installed with settings: ${settings}"
    subscribe(thermometer, "temperature", tempHandler, [filterEvents: false])
    subscribe(thermometer, "heatingSetpoint", setPointHandler, [filterEvents: false])
}

def updated(){
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(thermometer, "temperature", tempHandler, [filterEvents: false])
    subscribe(thermometer, "heatingSetpoint", setPointHandler, [filterEvents: false])
}

// Event Handlers

def setPointHandler(evt) {   // Watch for a change in temp

   if (evt.isPhysical()) {   //  This doesn't really work, it always records a non-physcial need to work on it.
        boolean isStateChange = evt.isStateChange()
        log.debug "Thermometer ${evt.displayName} Physical Changed State: ${isStateChange}"
    } else {    
        boolean isStateChange = evt.isStateChange()
        log.debug "Thermometer ${evt.displayName} non-Physical Changed State: ${isStateChange}"
   }
   
   def result = demandCheck(evt.device)  // check if we need to do anything as a result
}

def tempHandler(evt) {  // Change in temp so check if we need to do anything
    
    log.debug "Temp report ${evt.displayName} ${evt.value}C"

 	def result = demandCheck(evt.device)
}

// Comands

def demandCheck(radreport){  //Check if 

	//log.debug "Temp Check"

    def ambient = radreport.currentValue("temperature")   // initiate radiator ambient and set points as we're goint to check them and write them a few times.
    def setPoint = radreport.currentValue("heatingSetpoint")

    if (setPoint > ambient) {  // Check if heating needed log and turn on
    	log.debug "Heating required at ${radreport} ${ambient} <= ${setPoint}"   
        def result = heatActuator(true)      
    } else {  // if not turn off
        log.debug "Heating not required at ${radreport} ${ambient} > ${setPoint}"
    	def result = heatActuator(false)   
    }
}

def heatActuator(demand){

	def tempValue = 28  //Put a value in

 	if (demand) {   // Check demand and set to highHeat or lowHeat
       	tempValue = highHeat
    } else {
    	tempValue = lowHeat
    }

	for (radiator in thermostats) {   // Set required value for all chosen radiators
    	if (radiator.currentValue("heatingSetpoint") != tempValue) {
       		log.debug "Setting ${radiator} to ${tempValue}"
            radiator.setHeatingSetpoint(tempValue)
    	}
	}
  
    def result = demand 
}