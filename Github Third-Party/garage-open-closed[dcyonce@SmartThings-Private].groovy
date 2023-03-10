/**
 *  Garage Open/Closed
 *
 *  Copyright 2017 DON YONCE
 *
 */
definition(
    name: "Garage Open/Closed",
    namespace: "dcyonce",
    author: "DON YONCE",
    description: "Send SMS when Garage door is opened or closed",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Meta/garage_contact@2x.png")


preferences {
	section("Garage Open/Closed - dcyonce") {
	}
	section("Garage Doors ...") {
		input "garagedoor", "capability.contactSensor", title: "Which Door?", required: true, multiple: true
	}
	section("Text me at (optional, sends a push notification if not specified)...") {
        input("recipients", "contact", title: "Notify", description: "Send notifications to") {
            input "phone1", "phone1", title: "Phone number?", required: false
            input "phone2", "phone2", title: "Phone number?", required: false
            input "phone3", "phone3", title: "Phone number?", required: false
        }
	}
	section("Max time left open ...") {
		input "maxOpenTime", "number", title: "Minutes?"
	}
	section("Send alert every xxx minutes ...") {
		input "resendTime", "number", title: "Minutes?"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	unsubscribe()
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(garagedoor, "contact", EventHandler)
    
    state.isOpened = "unknown2"
    state.reSend = "false"
}


def EventHandler(evt) {
	log.debug "evt.Value=$evt.value"
    
    def message = ""
    if (evt.value == "open") 
    {
		message = "$location ${evt.displayName} has been opened."
        state.isOpened = "true";
        state.sensorName = "${evt.displayName}"        
		runIn(maxOpenTime * 60, SendOpenedTooLong)         
    }
    else
    {
		message = "$location ${evt.displayName} has been closed."
        state.isOpened = "false";
        state.reSend = "false"
    }

    SendNotification(message)
}

def SendNotification(message) {
	log.debug "Sending: $message"
    if (phone1) { sendSms(phone1, message) }
    if (phone2) { sendSms(phone2, message) }
    if (phone3) { sendSms(phone3, message) }
    sendPush message
}

def SendOpenedTooLong()
{
	if (state.isOpened == "true")
    {
    	def message
        if (state.reSend== "true")
        {
        	// Resend Alert
        	message = "$location ${state.sensorName} is still open!"
        }
        else
        {
        	// Send first notification
	      	message = "$location ${state.sensorName} has been left open more then $maxOpenTime minutes."
        }
        SendNotification(message)
            
        // Send another Alert in xx minutes
        state.reSend = "true"
        runIn(resendTime * 60, SendOpenedTooLong)         
	}
}