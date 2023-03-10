/**
 *  Monitor
 *
 *  Copyright 2015 Smart Dots
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
    name: "Monitor",
    namespace: "",
    author: "Smart Dots",
    description: "A smart app that monitors the state of given doors/windows and regularly notifies the user from when a door/window is open until the door/window is closed.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
//Design a smart app that monitors the state of a given door or window. 
//If that door or window is left open for "X" minutes, then notify user. 
//Once user is notified, continue monitoring and keep notifying the user every "Y" mins thereafter, 
//until the door or window is closed. 
//"X" and "Y" is a parameter that can be configured by the user. 
//The number of times repeat notifications can be sent should also be configurable by the user "N"
//yum
preferences {
    	section("Which doors & windows would you like to monitor?") {
    //multiple: true - so user can select multiple doors and windows to monitor.
    		input "contact1", "capability.contactSensor", multiple: true, title: "Select doors/windows"
	}
    	section("After how many minutes do you want to be notified?") {
        	input(name: "minsUntilNotify", type: "number", title: "Number of minutes")
    	}
    	section("How often do you want to be notified?") {
    		input(name: "minsRepeatNotify", type: "number", title: "Number of minutes")
    	}
    	section("How many times do you want to be notified?") {
    		input(name: "numNotify", type: "number", title: "Number of notifications")
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
	subscribe(contact1, "contact.open", delayHandler)
}

def delayHandler(evt) {
	log.debug "$evt.value"
    	//runIn(minsUntilNotify -> calls method to repetedly notify the user 'N' times)
    	runIn(minsUntilNotify*60, handler)
    
}
    
def handler() {
    log.debug "First push"
    //log.debug "0 $minsRepeatNotify * * *"
    //Display the first push
    sendPush("The ${contact1.displayName} is open!")
    //now schedule push msgs every 'X' mins as specified by user using cron expressions
    schedule("0 $minsRepeatNotify * * *", repeatHandler) //why does this crash? WHY??
}

def repeatHandler() {
    log.debug "On schedule"
    def count = 0
    def latest = contactSensor.currentState("contact1")
    //if the door is still open AND we are under the maximum number of notifications
    if (latest.value == "open" && count < numNotify) {
	sendPush("The ${contact1.displayName} is open!")
        count++
        }
    else {
    	unschedule("repeatHandler")
    }
}     
