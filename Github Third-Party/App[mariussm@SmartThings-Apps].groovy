/**
 *  
 */
definition(
    name: "Laundry notification",
    namespace: "smartthings",
    author: "Mariussm",
    description: "Get notified when your laundry is done",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/text.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png"
)

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When this power meter...", required: true, multiple: false, description: null)
        input(name: "belowThreshold", type: "number", title: "Reports below...", required: true, description: "in either watts or kw.")
        input(name: "belowThresholdTimes", type: "number", title: "... Times", required: true, description: "a number")
        input(name: "aboveThreshold", type: "number", title: "After being above...", required: true, description: "in either watts or kw.")
	}
    section {
        /*input("recipients", "contact", title: "Send notifications to") {*/
		input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: true)
		input(name: "notificationText", type: "text", title: "Notification text", required: true)
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
	subscribe(meter, "power", meterHandler)
	atomicState.belowThresholdTimes = 0
	atomicState.active = false
}

def meterHandler(evt) {
	def meterValue = evt.value as double
	log.debug "Laundry notification meterValue ${meterValue}"
	
	def aboveThresholdValue = aboveThreshold as int
	def belowThresholdValue = belowThreshold as int
	def belowThresholdTimesValue = belowThresholdTimes as int
	
    if(atomicState.active) {
		log.debug "device is already detected as active"
	} else if (meterValue > aboveThresholdValue) {
		atomicState.active = true
		log.debug "device is detected as being active (above threshold ${aboveThresholdValue})"
	} else {
		log.debug "device is not in use"
	}	
	
	if(atomicState.active && meterValue < belowThresholdValue) {
		atomicState.belowThresholdTimes += 1
		log.debug "belowThresholdTimes is ${atomicState.belowThresholdTimes}"
		
		if(atomicState.belowThresholdTimes >= belowThresholdTimesValue) {
			
			sendMessage(notificationText)
			atomicState.belowThresholdTimes = 0
			atomicState.active = false
		}
	} else {
		atomicState.belowThresholdTimes = 0
	}
}

def sendMessage(msg) {
	log.debug "Sending notification: ${msg}"
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        /*if (sms) {
            sendSms(sms, msg)
        }*/
        if (pushNotification) {
            sendPush(msg)
        }
    }
}
