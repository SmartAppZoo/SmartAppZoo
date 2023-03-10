/**
 *  Copyright 2015 Glen McGowan
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
 *  Routine Execute Triggers Open Door Check
 *
 *  Author: Glen McGowan
 */
definition(
    name: "Routine Execute Triggers Open Door Check",
    namespace: "mobious",
    author: "Glen McGowan",
    description: "Check for opent doors when routine executes",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png"
)


preferences{
    page(name: "selectActions")
}

def selectActions() {
    dynamicPage(name: "selectActions", install: true, uninstall: true) {

        // Get the available routines
            def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
            // sort them alphabetically
            actions.sort()
                    section("Routine") {
                        log.trace actions
                		// use the actions as the options for an enum input
                		input "action", "enum", title: "Select a trigger routine", options: actions
                    	}
                    section("Doors to check"){
						input "doors", "capability.contactSensor", title: "Which Door?", multiple: true, required: true
    					}
                    
                    /** Will fix this section later...
                    section("Notification"){
        				input "sendPush", "bool",title: "Send Push Notification?", required: true
        				input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
        				input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
   						}
                        **/    
                    
            }
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
        subscribe(location, "routineExecuted", routineChanged)
    }


def routineChanged(evt) {
 	if (evt.displayName == action)
        checkDoor();
}

def checkDoor() {
	//Find all the doors that are open.
    def open = doors.findAll { it?.latestValue("contact") == "open" }
    //Group all the open doors in a list, insert "is" or "Are" if the list contains more than one entry.
    def list = open.size() > 1 ? "are" : "is"
    if (open) {
    	//format the list and push it.
		def message = "Security Check Failed: ${open.join(', ')} ${list} open"
    	log.info message
        //Hard code push notification for now. 
        if (sendPush) {
     		sendPush(message)
     		}
      }else {
    log.info "Security Check Successful: No open doors detected." 
    sendNotificationEvent("Security Check Successful: No open doors detected.")
	}
    
 }



/** Single Door Check

def checkDoor() {
    if (doors.currentContact == "open") {
		def message = ""Security Check Failed: ${door.displayName} is open." 
    	log.info message
        if (sendPush) {
     		sendPush(message)
            log.info Push Notification Sent
     		}
      }
    if (doors.currentContact == "closed") {
    log.info "Security Check Successful: No open doors detected." 
	}else {
    log.info "Error: Unable to detect contact status."
    }
 }
 
 **/




/**Fancy Notification--Need to Fix later
def checkDoor() {
    log.debug "checkDoor status: ${door.displayName} is ${door.currentContact}"
   	if (door.currentContact == "open") {
       	log.debug "checkDoor Debug: ${door.currentContact}"
        def msg = "${door.displayName} was left open!"
        log.debug msg
        
         if (!phone || pushAndPhone != "No") {
             log.debug "checkDoor: Sending push notificaton"
             sendPush(msg)
         }
         if (phone) {
            log.debug "checkDoor: Sending SMS Notification"
            sendSms(phone, msg)
        
         } else {
       	 log.debug "checkDoor Complete: No Open Doors Detected"
         } 
	} else {
    log.debug "checkDoor Error: Unable to detect contact states"
}
}
**/