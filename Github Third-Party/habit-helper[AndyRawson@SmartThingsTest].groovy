/**
 *  Habit Helper
 *  Every day at a specific time, get a text reminding you about your habit
 *
 *  Author: SmartThings
 */
definition(
    name: "Habit Helper",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Add something you want to be reminded about each day and get a text message to help you form positive habits.",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/text.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png"
)

preferences {
	section("Remind me about..."){
		input "message1", "text", title: "What?"
	}
	section("At what time?"){
		input "time1", "time", title: "When?"
	}
	section("Text me at..."){
        input("recipients", "contact", title: "Send notifications to") {
            input "phone1", "phone", title: "Phone number?"
        }
	}
}

def installed()
{
	schedule(time1, "scheduleCheck")
}

def updated()
{
	unschedule()
	schedule(time1, "scheduleCheck")
}

def scheduleCheck()
{
	log.trace "scheduledCheck"

	def message = message1 ?: "SmartThings - Habit Helper Reminder!"

    if (location.contactBookEnabled) {
        log.debug "Texting reminder: ($message) to contacts:${recipients?.size()}"
        sendNotificationToContacts(message, recipients)
    }
    else {

        log.debug "Texting reminder: ($message) to $phone1"
        sendSms(phone1, message)
    }
}
