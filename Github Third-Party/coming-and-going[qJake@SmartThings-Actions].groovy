// Automatically generated. Make future change here.
definition(
    name: "Coming and Going",
    namespace: "qJake",
    author: "Jake Burgy",
    description: "Get a notification when someone comes or goes between a specific time.",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Categories/presence.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Categories/presence@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Categories/presence@2x.png")

preferences {
	section("When..."){
		input "arrivalPresence", "capability.presenceSensor", title: "One of these people arrives:", required: false, multiple: true
		input "departurePresence", "capability.presenceSensor", title: "One of these people departs:", required: false, multiple: true
	}
    section("Between..."){
    	input "from", "time", title: "This time:", required: true
        input "to", "time", title: "And this time:", required: true
    }
	section("Send this message..."){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("To these people..."){
        input "phone", "phone", title: "Phone Number:", required: false
        paragraph "Or, leave this blank to send yourself a push notification via the SmartThings app instead."
	}
}

def installed() {
	subscribeToEvents()
}

def updated() {
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
}

def eventHandler(evt) {   
    def now = new Date()
    def fromDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", from)
    def toDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", to)
    
    // Reset date to current date
    fromDate.date = now.date
    fromDate.month = now.month
    fromDate.year = now.year
    toDate.date = now.date
    toDate.month = now.month
    toDate.year = now.year    

    if(now >= fromDate && now <= toDate) {
        if (location.contactBookEnabled) {
            sendNotificationToContacts(messageText, recipients)
        }
        else {
            if (!phone) {
                sendPush(messageText)
            } else {
                sendSms(phone, messageText)
            }
        }	
    }
}