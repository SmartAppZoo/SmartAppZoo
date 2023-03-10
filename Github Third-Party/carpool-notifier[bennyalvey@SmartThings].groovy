/**
 * title: Carpool Notifier
 *
 * description:
 * Do you carpool to work with your spouse? Do you pick your children up from school? Have they been waiting in doors for you? Let them know you've arrived with Carpool Notifier.
 *
 * This SmartApp is designed to send notifications to your carpooling buddies when you arrive to pick them up. What separates this SmartApp from other notification SmartApps is that it will only send a notification if your carpool buddy is not with you.
 *
 * category: Family

 * icon:		https://s3.amazonaws.com/smartapp-icons/Family/App-IMadeIt.png
 * icon2X:	https://s3.amazonaws.com/smartapp-icons/Family/App-IMadeIt%402x.png
 *
 *  Author: steve
 *  Date: 2013-11-19
 */

preferences {
	section() {
		input(name: "driver", type: "capability.presenceSensor", required: true, multiple: false, title: "When this person arrives", description: "Who's driving?")
		input(name: "phoneNumber", type: "phone", required: true, multiple: false, title: "Send a text to", description: "Phone number")
		input(name: "message", type: "text", required: false, multiple: false, title: "With the message:", description: "Your ride is here!")
		input(name: "rider", type: "capability.presenceSensor", required: true, multiple: false, title: "But only when this person is not with you", description: "Who are you picking up?")
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
	subscribe(driver, "presence.present", presence)
}

def presence(evt) {

	if (evt.value == "present" && riderIsHome())
	{
//        	log.debug "Rider Is Home; Send A Text"
		sendText()
	}

}

def riderIsHome() {

//	log.debug "rider presence: ${rider.currentPresence}"

	if (rider.currentPresence != "present")
	{
		return false
	}

	def riderState = rider.currentState("presence")
//	log.debug "riderState: ${riderState}"
	if (!riderState)
	{
		return true
	}

	def latestState = rider.latestState("presence")

	def now = new Date()
	def minusFive = new Date(minutes: now.minutes - 5)


	if (minusFive > latestState.date)
	{
		return true
	}

	return false
}

def sendText() {
	sendSms(phoneNumber, message ?: "Your ride is here!")
}
