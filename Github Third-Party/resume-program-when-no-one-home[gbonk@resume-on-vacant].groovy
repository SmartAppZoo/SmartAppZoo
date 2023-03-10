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
 *  Notify Me When
 */
definition(
		name: "Resume Program when no one home",
		namespace: "lodgeinriverlea",
		author: "Gregory",
		description: "Resume the program when we have all left for the day."
)

preferences {
		input "motion", "capability.motionSensor", title: "Motion Locations", required: true, multiple: true
		input "thermostat", "capability.thermostat", title: "Thermostat", required: true, multiple: false
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents() 
} 

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents() 
}

def subscribeToEvents() {

 	subscribe(motion, "motion", eventHandler)
}

def eventHandler(evt) {
	log.debug "Notify got evt $evt.name: $evt.value"
	
    motion.each { 
        log.debug "State is ${ it.currentState("motion").value } "
    }

// returns a list of the values for all switches
    def allStates = motion.currentState("motion").value
    
    def areActive = allStates.findAll { it == "active"  }
    
    if (areActive?.empty) {
       resumeProgram( evt )
    }
}

private resumeProgram(evt) {

   thermostat.resumeProgram()
   
   sendNotification("I have determined no one is home and I have resumed the program", [method: "push"] )

}
