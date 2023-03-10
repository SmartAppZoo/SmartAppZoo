/**
 *  Phone Talks when Door Opens
 *
 *  Author: John Gorsica
 */
definition(
    name: "Talking Door",
    namespace: "jgorsica",
    author: "John Gorsica",
    description: "Phones Talk when Door Opens",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section ("When the door opens...") {
		input "contact1", "capability.contactSensor", title: "Where?", multiple:true
	}
	section ("Turn on a light...") {
		input "hues", "capability.colorControl", multiple: true, required: false
	}
    section ("Make Phone Talk...") {
    	input "phones", "capability.speechSynthesis", multiple: true, required: false
        input "message", "text", title: "Notification message", description: "Garage Door Opening", required: false
        input "timeOfDay", "time", title: "After what time?", required: false
    }
}

def installed()
{
	subscribe(contact1, "contact.open", contactHandler)
}

def updated()
{
	unsubscribe()
	subscribe(contact1, "contact.open", contactHandler)
    if(!message) message="Door is opening"
}

def contactHandler(evt) {
	if(true){
    	hues*.on()
        if (correctTime()){
            phones*.speak(message)
        }
    }
}

private correctTime() {
	def t0 = now()
	def modeStartTime = new Date()
	def startTime = timeTodayAfter(modeStartTime, timeOfDay, location.timeZone)
    def stopTime = timeTodayAfter(modeStartTime, '01:00', location.timeZone)
	if (t0 >= startTime.time || t0 <= stopTime.time) {
		true
	} else {
		log.debug "The current time of day (${new Date(t0)}), is not in the correct time window ($startTime):  doing nothing"
		false
	}
}
