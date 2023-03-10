/**
 *  Smart counter
 *
 *  Copyright 2016 Pavlo Dubovyk
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
    name: "Smart counter",
    namespace: "pdubovyk",
    author: "Pavlo Dubovyk",
    description: "Count of activities after leaving",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		 input "motion", "capability.motionSensor", title: "Motion Here", required: true, multiple: true
    	 input "contact", "capability.contactSensor", title: "pick a contact sensor", required: true, multiple: false
         input "people", "capability.presenceSensor", multiple: true
	}
        section("Pre chek delay (defaults to 3 min)") { input "pre_check_delay", "decimal", title: "Number of minutes", required: false}
        section("False alarm threshold (defaults to 10 min)") { input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false}
        section("Send Push Notification?") {input "sendPushNotification", "bool", required: false,title: "Send Push Notification?"}
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

	subscribe(motion, "motion", motionHandler)
    subscribe(contact, "contact", contactHandler)
    subscribe(people, "presence", presence)
    subscribe(location, "routineExecuted", routineChanged)
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers

def presence(evt) {
    log.debug "evt.name: $evt.value"
    if (evt.value == "not present") {
      
            log.debug "checking if everyone is away"
            if (everyoneIsAway()) {
                log.info "Set Goodbye Check!"
                //Set proper Routine
                location.helloHome?.execute("Goodbye Check!")
            }
        
        else {
            log.debug "mode is the same, not evaluating"
        }
    }
    else {
        log.debug "present; doing nothing"
    }
}


def routineChanged(evt) {
    log.debug "routineChanged: $evt"
    log.debug "evt name: ${evt.name}"
    log.debug "evt value: ${evt.value}"
    log.debug "evt displayName: ${evt.displayName}"
    log.debug "evt descriptionText: ${evt.descriptionText}"
    
    if (evt.displayName == "I'm Back!")
    {
    
    		def message_2 = "Report:"+ (char) 10 + (char) 13 +" "
            message_2 =  message_2 + " Switch Counter ${state.switchCounter}" + (char) 10 + (char) 13 +" "
            message_2 =  message_2 + " Motion Counter ${state.motionCounter}" + (char) 10 + (char) 13 +" "
    
    		log.info message_2
            if (sendPushN())
            {
            
			sendPush(message_2)
    		}
    }
    
    if (evt.displayName == "Goodbye Check!")
    {
    
    	log.info "Start Goodbye Check"
        log.info "Start delay"
        
        def pre_start_delay = findPreCheckDelay()*60
        
        runIn(pre_start_delay, "Goodbye_Check_action")       	      	
    }
    

}

def motionHandler(evt) {

	state.motionCounter = state.motionCounter + 1
    log.debug "motion has been turned on $state.motionCounter times"

}

def contactHandler(evt) {

 	state.switchCounter = state.switchCounter + 1
    log.debug "switch has been turned on $state.switchCounter times"
}

// returns true if all configured sensors are not present,
// false otherwise.
private everyoneIsAway() {
    def result = true
    // iterate over our people variable that we defined
    // in the preferences method
    for (person in people) {
        if (person.currentPresence == "present") {
            // someone is present, so set our our result
            // variable to false and terminate the loop.
            result = false
            break
        }
    }
    log.debug "everyoneIsAway: $result"
    return result
}

// gets the false alarm threshold, in minutes. Defaults to
// 10 minutes if the preference is not defined.
private findFalseAlarmThreshold() {
    // In Groovy, the return statement is implied, and not required.
    // We check to see if the variable we set in the preferences
    // is defined and non-empty, and if it is, return it.  Otherwise,
    // return our default value of 10
    (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold : 10
}


private sendPushN() {
    (sendPushNotification != null && sendPushNotification != "") ? sendPushNotification : true
}


// 3 minutes if the preference is not defined.
private findPreCheckDelay() {
    (pre_check_delay != null && pre_check_delay != "") ? pre_check_delay : 3
}


def Goodbye_Check_action()
{
	if (everyoneIsAway()) {
		log.debug "Start CHECK"
        
        //Get all statuses       
        def latestState_1 = contact.currentState("contact")    
        def motionState_1 = motion.currentState("motion")
       	
        //Create sum message
        def status_message_1 = " Check status:" + (char) 10 + (char) 13 + " "
      	
        status_message_1 = status_message_1 + "Contact#1 is ${latestState_1.value}" + (char) 10 + (char) 13 +" "    
        status_message_1 = status_message_1 + "Motion sensor #1 is ${motionState_1.value}" + (char) 10 + (char) 13 +" "
       
        
        
        log.info "${status_message_1}"
        if (sendPushN())
        {
		sendPush(status_message_1)
        }
        
        //Wait a 10 min and check away status for everybody
        runIn((findFalseAlarmThreshold() -  findPreCheckDelay())* 60, "takeAction_Goodbye", [overwrite: false])    
        
        //
        state.switchCounter = 0
        state.motionCounter = 0
        }

}

def takeAction_Goodbye()
{
	if (everyoneIsAway()) {
		
        /// clarify
        def threshold = 1000 * 60 * findFalseAlarmThreshold() - 1000 -  findPreCheckDelay() *60*1000
		def awayLongEnough = people.findAll { person ->
			def presenceState = person.currentState("presence")
			if (!presenceState) {
				// This device has yet to check in and has no presence state, treat it as not away long enough
				return false
			}
			def elapsed = now() - presenceState.rawDateCreated.time
			elapsed >= threshold
            //clarify            
		}
        
        
        
		log.debug "Found ${awayLongEnough.size()} out of ${people.size()} person(s) who were away long enough"
		if (awayLongEnough.size() == people.size()) {
			// TODO -- uncomment when app label is available
			def message = "SmartThings changed your mode to  because everyone left home"
			log.info message
            if (sendPushN())
            {
				sendPush("SmartThings changed your mode to  because everyone left home")
             }
			//Switch to Goodbye!
        	location.helloHome?.execute("Goodbye!")
		} else {
			log.debug "not everyone has been away long enough; doing nothing"
		}
	} else {
    	log.debug "not everyone is away; doing nothing"
    }
}