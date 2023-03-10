/**
 *  Has Barkley Been Fed
 *
 *  Author: SmartThings
 */
preferences {
	section("Choose your pet feeder...") {
		input "feeder1", "capability.contactSensor", title: "Where?"
	}
	section("Feed my pet at...") {
		input "time1", "time", title: "When?"
	}
	section("Text me if I forget...") {
		input "phone1", "phone", title: "Phone number?"
	}
}

def installed()
{
	schedule(time1, "scheduleCheck")
}

def updated()
{
	unsubscribe() //TODO no longer subscribe like we used to - clean this up after all apps updated
	unschedule()
	schedule(time1, "scheduleCheck")
}

def scheduleCheck()
{
	log.trace "scheduledCheck"

	def midnight = (new Date()).clearTime()
	def now = new Date()
	def feederEvents = feeder1.eventsBetween(midnight, now)
	log.trace "Found ${feederEvents?.size() ?: 0} feeder events since $midnight"
	def feederOpened = feederEvents.count { it.value && it.value == "open" } > 0

	if (feederOpened) {
		log.debug "Feeder was opened since $midnight, no SMS required"
	} else {
		log.debug "Feeder was not opened since $midnight, texting $phone1"
		sendSms(phone1, "No one has fed the dog")
	}
}
