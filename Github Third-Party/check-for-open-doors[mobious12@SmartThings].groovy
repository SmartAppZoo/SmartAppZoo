/**
 *  Check For Open Doors
 *
 *  Copyright 2015 Glen McGowan
 *
 */
definition(
    name: "Check For Open Doors",
    namespace: "mobious",
    author: "Glen McGowan",
    description: "Check whether door is closed after a mode change or specific time.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png"
)


preferences {
	section("Mode transition(s)") {
		input "newMode", "mode", title: "Select Modes", multiple: true, required: false
	}
    section("Check Once a day") {
    	input "timeToCheck", "time", title: "(Optional)", required: false
    }
    section("Doors to check"){
		input "door", "capability.contactSensor", title: "Pick One", multiple: false, required: true
    }
    section("Via a push notification and/or an SMS message"){
        input("recipients", "contact", title: "Send notifications to") {
        input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
        input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
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
    unschedule()
	initialize()
}

def initialize() {
	if (newMode != null) {
		subscribe(location, modeChangeHandler)
    }
    if (timeToCheck != null) {
    	schedule(timeToCheck, checkDoor)
    }
}

def modeChangeHandler(evt) {
	log.debug "Mode change to: ${evt.value}"

    // Have to handle when they select one mode or multiple
    if (newMode.any{ it == evt.value } || newMode == evt.value) {
		checkDoor()
    }
}

def checkDoor() {
    log.debug "Door ${door.displayName} is ${door.currentContact}"
   	if (door.currentContact == "open") {
       	def msg = "${door.displayName} was left open!"
        log.info msg
        
         if (!phone || pushAndPhone != "No") {
             log.debug "sending push"
             sendPush(msg)
         }
         if (phone) {
            log.debug "sending SMS"
            sendSms(phone, msg)
        
         } else {
       	 log.debug "Check For Open Door: No Open Door"
    }
}
}