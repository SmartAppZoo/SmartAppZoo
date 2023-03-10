/**
 *  Notify Me When My Hub Goes Offline
 *
 *  Author: SmartThings
 */
preferences {
	page(name: "root", title: "Hub Notification", install: true) {
	    section("Notification method") {
	        input "pushNotification", "bool", title: "Push notification", description: "Receive a push notification", required: false
	        input "phone", "phone", title: "Text message at", description: "Tap to enter phone number", required: false
	    }
	    section("Choose Hub to Monitor") {
			input "hub", "hub", multiple: true, title: "Select a hub"
		}
	}
}

def installed()
{
    subscribe(hub, "activity", processEvent, [filterEvents: false])
}

def updated()
{
	unsubscribe()
    subscribe(hub, "activity", processEvent, [filterEvents: false])
}

def processEvent(evt) {
    if (evt.value.contains("inactive")) {
		def msg = evt.description
		log.debug "A Hub is inactive, sending message: '$msg', push:$pushNotification, phone:$phone"

		if (pushNotification) {
			sendPush(msg)
		}
		if (phone) {
			sendSms(phone, msg)
		}
    }
}
