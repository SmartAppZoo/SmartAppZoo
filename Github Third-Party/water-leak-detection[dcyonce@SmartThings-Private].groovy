/**
 *  Water Leak Detection
 *
 *  Copyright 2017 DON YONCE
 *
 */
definition(
    name: "Water Leak Detection",
    namespace: "dcyonce",
    author: "DON YONCE",
    description: "Sends a notification when water is detected",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png")


preferences {
	section("Water Leak Detection - dcyonce") {}
	section("When water is sensed...") {
		input "sensor", "capability.waterSensor", title: "Where?", required: true, multiple: true
	}
	section("Text me at (optional, sends a push notification if not specified)...") {
        input("recipients", "contact", title: "Notify", description: "Send notifications to") {
            input "phone1", "phone1", title: "Phone number?", required: false
            input "phone2", "phone2", title: "Phone number?", required: false
            input "phone3", "phone3", title: "Phone number?", required: false
        }
	}    
	section("Send alert every xxx minutes ...") {
		input "resendTime", "number", title: "Minutes?"
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
 	subscribe(sensor, "water", EventHandler)
    
    state.isWet = "unknown";
}

def EventHandler(evt) {
	log.debug "evt.Value=$evt.value"
    log.debug "Sensor = ${evt.displayName}"
    def message = ""
    
    if (evt.value == "wet") 
    {
		message = "$location ${evt.displayName} has detected a water leak!"
        state.isWet = "true";
        state.sensorName = "${evt.displayName}"
		runIn(resendTime * 60, ResendAlert)         
    }
    else
    {
		message = "$location ${evt.displayName} is now dry."
        state.isWet = "false";
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

def ResendAlert()
{
	if (state.isWet == "true")
    {
    	def message
        // Resend Alert
        message = "$location ${state.sensorName} is still wet!"
	    SendNotification(message)
        runIn(resendTime * 60, ResendAlert)         
	}
}
