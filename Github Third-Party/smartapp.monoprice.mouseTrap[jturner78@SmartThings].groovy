/**
 *  Mouse Trap Alert
 *
 *  Author: Justin Eltoft
 *  Date: 2015-01-24
 *
 *  "The light is green, the trap is clean"
 *  https://www.youtube.com/watch?v=aLwKMkdVMnQ
 *
 */
definition(
    name: "Mouse Trap Alert",
    namespace: "jdeltoft",
    author: "Justin Eltoft",
    description: "Get a push notification or text message when you caught a mouse.",
    category: "",
    
    iconUrl: "https://raw.githubusercontent.com/jdeltoft/SmartThings/master/stIcons/myIcons/moustTrap.png",
    iconX2Url: "https://raw.githubusercontent.com/jdeltoft/SmartThings/master/stIcons/myIcons/moustTrap@2x.png"
    
    //iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-UndeadEarlyWarning.png",
    //iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-UndeadEarlyWarning@2x.png"
)


preferences {
	section("Choose one or more, when..."){
		input "trapDead", "capability.sensor", title: "Dead Mouse", required: false, multiple: false
	}
	section("Send this message (optional, sends standard status message if not specified)"){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("Via a push notification and/or an SMS message"){
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
            input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
        }
	}
	section("Minimum time between messages (optional, defaults to every message)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}
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
	subscribe(trapDead, "sensor.dead", eventHandler)
}

def eventHandler(evt) {
	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
			sendMessage(evt)
		}
	}
	else {
		sendMessage(evt)
	}
}

private sendMessage(evt) {
	def msg = messageText ?: defaultText(evt)
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {

        if (!phone || pushAndPhone != "No") {
            log.debug "sending push"
            sendPush(msg)
        }
        if (phone) {
            log.debug "sending SMS"
            sendSms(phone, msg)
        }
    }
	if (frequency) {
		state[evt.deviceId] = now()
	}
}

private defaultText(evt) {
	if (evt.name == "presence") {
		if (evt.value == "present") {
			if (includeArticle) {
				"$evt.linkText has arrived at the $location.name"
			}
			else {
				"$evt.linkText has arrived at $location.name"
			}
		}
		else {
			if (includeArticle) {
				"$evt.linkText has left the $location.name"
			}
			else {
				"$evt.linkText has left $location.name"
			}
		}
	}
	else {
		evt.descriptionText
	}
}

private getIncludeArticle() {
	def name = location.name.toLowerCase()
	def segs = name.split(" ")
	!(["work","home"].contains(name) || (segs.size() > 1 && (["the","my","a","an"].contains(segs[0]) || segs[0].endsWith("'s"))))
}
