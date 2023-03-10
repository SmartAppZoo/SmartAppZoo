/**
 *  Double Door In Out Detector
 *
 *  Copyright 2017 Neal Clark
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
    name: "Double Door In Out Detector",
    namespace: "neallclark",
    author: "Neal Clark",
    description: "Detects whether someone is leaving or entering based on order of doors opening / closing",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Inside open/close sensor") {
		input "insideDoorState", "capability.contactSensor", title: "Inside Door?"
	}
	section("Outside open/close sensor") {
		input "outsideDoorState", "capability.contactSensor", title: "Outside Door?" 
	}    
	section("Set this switch if leave detected") {
		input "leaveOutputSwitch", "capability.switch"
	}
    section("Set this switch if enter detected") {
		input "enterOutputSwitch", "capability.switch"
	}
    section("Set this switch for entering in progress") {
		input "transitionIsEntering", "capability.switch"
	}
    section("Set this switch for leaving in progress") {
		input "transitionIsLeaving", "capability.switch"
	}
}

def installed()
{   
	setSubscriptions()    
}

def updated()
{
	unsubscribe()
    setSubscriptions()
}

def setSubscriptions() {
	subscribe(insideDoorState, "contact.closed", onInsideDoorCloseHandler)
	subscribe(insideDoorState, "contact.open", onInsideDoorOpenHandler)
	subscribe(outsideDoorState, "contact.closed", onOutsideDoorCloseHandler)
	subscribe(outsideDoorState, "contact.open", onOutsideDoorOpenHandler)
}

def logHandler(evt) {
	log.debug evt.value
}

def isAlreadyInTransition() {
    return (transitionIsEntering.currentValue("switch") == "on" || transitionIsLeaving.currentValue("switch") == "on")
}

def resetOutputs() {
    leaveOutputSwitch.off()
    enterOutputSwitch.off()
}

def resetTransitionFlags() {
    transitionIsEntering.off()
    transitionIsLeaving.off()
}

def pushbackNoMoreEventsTimeout() {
    runIn(60, onNoMoreEventsHandler)
}

/*
    Door Closed Handlers
*/

def onInsideDoorCloseHandler(evt) {
	log.debug "Running onInsideDoorCloseHandler - ${insideDoorState}:${insideDoorState.currentValue("contact")} ${outsideDoorState}:${outsideDoorState.currentValue("contact")}"
	
    //if transitioning in and outer door is closed then, we have entered
    //otherwise we are still transitioning (or we have left a door open)
    if(transitionIsEntering.currentValue("switch") == "on" && outsideDoorState.currentValue("contact") == "closed") {
        resetTransitionFlags()
        log.debug "Detected Entry"
        enterOutputSwitch.on()
    }
    
    pushbackNoMoreEventsTimeout()
}

def onOutsideDoorCloseHandler(evt) {
	log.debug "Running onOutsideDoorCloseHandler - ${insideDoorState}:${insideDoorState.currentValue("contact")} ${outsideDoorState}:${outsideDoorState.currentValue("contact")}"
	
    //if transitioning out and inner door is closed then, we have left
    //otherwise we are still transitioning (or we have left a door open)
    if(transitionIsLeaving.currentValue("switch") == "on" && insideDoorState.currentValue("contact") == "closed") {
        resetTransitionFlags()
        log.debug "Detected Exit"
        leaveOutputSwitch.on()
    }
    
    pushbackNoMoreEventsTimeout()
}

/*
    Door opened handlers
*/

def onInsideDoorOpenHandler(evt) {
	log.debug "Running onInsideDoorOpenHandler - ${insideDoorState}:${insideDoorState.currentValue("contact")} ${outsideDoorState}:${outsideDoorState.currentValue("contact")}"
    
    resetOutputs()

    // If we aren't already in a transition then set the type of transition (Entering/Leaving)
    if(!isAlreadyInTransition())
        transitionIsLeaving.on()
        
    pushbackNoMoreEventsTimeout()
}

def onOutsideDoorOpenHandler(evt) {
	log.debug "Running onOutsideDoorOpenHandler - ${insideDoorState}:${insideDoorState.currentValue("contact")} ${outsideDoorState}:${outsideDoorState.currentValue("contact")}"
    
    resetOutputs()

    // If we aren't already in a transition then set the type of transition (Entering/Leaving)
    if(!isAlreadyInTransition())
        transitionIsEntering.on()
    
    pushbackNoMoreEventsTimeout()
}


//No more event's handler to reset everything
def onNoMoreEventsHandler(evt) {
	//If a door is still open then don't reset anything, just set the timer again
    if(insideDoorState.currentValue("contact") == "open" || outsideDoorState.currentValue("contact") == "open") {
        pushbackNoMoreEventsTimeout()
    }
    else {
        resetTransitionFlags()
        resetFinalOutput()
        log.debug "Transition timed out, reseting everything"
    }
}