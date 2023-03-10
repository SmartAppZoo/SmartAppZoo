/**
 *  Heating Control
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
    name: "Heating Control v1.0",
    namespace: "rjsm",
    author: "Stuart Moffat",
    description: "This app allows you to control a heating actuator (boiler relay) independantly from each radiator.",  
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"

)

preferences {
   // Get thermostats and device to control heating and an override device 
    
    section("Choose Thermostats") {
        input "thermostats", "capability.Thermostat", multiple: true, required: true, title: "Select"
    }
    section("Choose Actuator") {
        input "actuator", "capability.Switch", multiple: false, required: true, title: "Select one"
    }
    section("Choose Override Switch") {  //Create as a virtual switch unless you want a real one.
        input "override", "capability.Switch", multiple: false, required: true, title: "Select one"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    subscribe(thermostats, "temperature", tempHandler, [filterEvents: false])
    subscribe(thermostats, "heatingSetpoint", setPointHandler, [filterEvents: false])
}

def updated(){
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(thermostats, "temperature", tempHandler, [filterEvents: false])
	subscribe(thermostats, "heatingSetpoint", setPointHandler, [filterEvents: false])
}

// Event Handlers

def setPointHandler(evt) {  // Set point changed, check if we need to do anything.

   if (evt.isPhysical()) {  // this doesn't seem to work need to work on it all changes regarded as non-physcial 
        boolean isStateChange = evt.isStateChange()
        log.debug "Radiator ${evt.displayName} Physical Changed State: ${isStateChange}"
    } else {    
        boolean isStateChange = evt.isStateChange()
        log.debug "Radiator ${evt.displayName} non-Physical Changed State: ${isStateChange}"
   }
   
   def result = demandCheck(evt.device)
} 

def tempHandler(evt) {   // received a temperature report check if we need to do anything as a result
    
    log.debug "Temp report ${evt.displayName} ${evt.value}C"

 	def result = demandCheck(evt.device)
}

// Comands

def demandCheck(radreport){

	//log.debug "Temp Check"

    def ambient = radreport.currentValue("temperature")  // initiate radiator ambient and set points as we're goint to check them and write them a few times.
    def setPoint = radreport.currentValue("heatingSetpoint")

    if (setPoint > ambient) {  // Check if heating needed notify and turn on
    	log.debug "Heating required at ${radreport} ${ambient} <= ${setPoint}"
        sendNotificationEvent ("Heating required at ${radreport} ${ambient} <= ${setPoint}")
        def result = heatActuator(true)      
    } else {  // Check that we don't need it elsewhere
        log.debug "Heating not required at ${radreport} ${ambient} > ${setPoint}"
    	def result = checkNoHeatRequired()   
    }
}

def checkNoHeatRequired(){

	def demand = false  //We were sent here because the setpoint was less than the ambient so assume no heat required
    
    if (actuator.currentValue("switch")=="off") {  //If boiler off then nothing to do
    	log.debug "Nothing to do"     
    } else { // Boiler on, check if needed
		for (radiator in thermostats) {   // If boiler on then check if any radiator still needs heat
    		if (radiator.currentValue("heatingSetpoint") > radiator.currentValue("temperature")) {
       			log.debug "Found One ${radiator}"
            	demand = true
       	 	}
		}
        if (!demand) {   // If no demand then turn off boiler
        	heatActuator(false)
        }
    }
    
	log.debug "Heat ${demand}"
	
    def result = demand 
}


def heatActuator(demand){
	//Turns on/off boiler / actuator if required and if not overriden or already in that state.
	
    def result = true  // returns true unless heating was overriden
    
    if (override.currentValue("switch")=="off") {   // Check if overide enabled
 		if (demand && actuator.currentValue("switch")=="off") {  //if there is demand and device is off turn on
			log.debug "Turning on"
            sendNotificationEvent ("Turning on ${actuator}")
			actuator.on()
    	} else if (demand) {   // was and is demand but actuator is on
    			log.debug "Already on"
                sendNotificationEvent ("${actuator} already on")
    	} else if (actuator.currentValue("switch")=="on") {  // no demand but actuator on
    		log.debug "Turning off"
            sendNotificationEvent ("Turning off ${actuator}")
       		actuator.off() 
    	} else {   // none of the above.
    		log.debug "Already off"
		}
     } else {
    	log.debug "Demand Overriden"
        result = false
     }
}