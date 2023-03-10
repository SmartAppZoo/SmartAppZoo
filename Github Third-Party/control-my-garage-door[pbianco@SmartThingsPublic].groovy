/**
 *  Control my garage door
 *
 *  Author: Phil Bianco 
 *  Date: 04/08/2014
 *  Modified 10/10/2015
 */
 

// Automatically generated. Make future change here.
definition(
    name: "Control My Garage Door",
    namespace: "",
    author: "Phil Bianco",
    description: "This will control Garage Door(s) based on precence and time.",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("What multisensor monitors your door...") {
		input "multisensor", "capability.contactSensor", title: "Which?", required: true
	}
	section("Switch that controls your door...") {
        input "doorSwitch", "capability.momentary", title: "Which?", required: true
    }
    section("Who should I monitor arriving...") {
		input "arrivalPeople", "capability.presenceSensor", title: "Who?", multiple: true
	}
    section("Who should I monitor departing...") {
		input "departurePeople", "capability.presenceSensor", title: "Who?", multiple: true
	}
    section("What time would you like to close...") {
		input "timeClose", "time", title: "Time of Day", required: false
	}
}

def installed()
{
	state.Open = null
    subscribe(multisensor, "contact", accelerationHandler)
    subscribe(arrivalPeople, "presence.present", presence)
    subscribe(departurePeople, "presence.not present", notPresent)
    schedule(timeClose, "closeDoor")
}

def updated()
{  
    unsubscribe()
    unschedule()
	subscribe(multisensor, "contact", accelerationHandler)
    subscribe(arrivalPeople, "presence.present", presence)
    subscribe(departurePeople, "presence.not present", notPresent)
    schedule(timeClose, "closeDoor")
}


def accelerationHandler(evt) {
	
    log.debug "accelerationHandler $evt.value"
    state.Open = evt.value
    log.debug "Value of isOpen= $state.Open"
}

def presence (evt) {

	log.debug "Presence $evt.value"
    log.debug "Value of $state.Open"
    if (evt.value == "present" && state.Open == "closed") { 
    	log.debug "Opening Door"
        sendPush("Your ${doorSwitch.label ?: doorSwitch.name} was opened")
        doorSwitch.push()
    }  
}

def notPresent (evt) {

	log.debug "Presence $evt.value"
    log.debug " Value of isOpen = $state.Open"
     if (evt.value == "not present" && state.Open == "open") {
    	log.debug "Closing Door"
        sendPush("Your ${doorSwitch.label ?: doorSwitch.name} was closed")
        doorSwitch.push()
    }
}

def closeDoor() {
	log.debug "Closing Door(s) Value of $state.Open"
    
    if (state.Open == "open"){
       log.debug "Closing Door"
       sendPush("Your ${doorSwitch.label ?: doorSwitch.name} was closed")
       doorSwitch.push()
    }
    else {
       log.debug "Door is not Open"
    }
}
