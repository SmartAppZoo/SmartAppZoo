/**
 *  Text Me When It Opens - Two Numbers 
 *
 *  Author: Phil Bianco 
 */


preferences {
	section("When the door opens or closes ...") {
		input "contact1", "capability.contactSensor", title: "Where?"
	}
    section("Turn on a light ...") {
    	input "lights", "capability.switch", multiple: true
    }
	section("Text me at...") {
		input "phone1", "phone1", title: "Phone number?", required: true
        input "phone2", "phone2", title: "Phone number?", required: false
	}
}

def installed()
{
	subscribe(contact1, "contact.open", contactOpenHandler)
    subscribe(contact1, "contact.closed", contactCloseHandler)
}

def updated()
{
	unsubscribe()
	subscribe(contact1, "contact.open", contactOpenHandler)
    subscribe(contact1, "contact.closed", contactCloseHandler)
}

def contactOpenHandler(evt) {
	log.trace "$evt.value: $evt, $settings"
	if (phone1) {
    log.debug "$contact1 was opened, texting $phone1"
    sendSms(phone1, "Your ${contact1.label ?: contact1.name} was opened")
    }
    if (phone2) {
    log.debug "$contact1 was opened, texting $phone2"
    sendSms(phone2, "Your ${contact1.label ?: contact1.name} was opened")
    log.debug "Turning lights on"
    }
    lights.on()
}

def contactCloseHandler(evt) {
	log.trace"$evt.value: $evt, $settings"
    if (phone1) {
    log.debug "$contact1 was closed, texting $phone1"
    sendSms(phone1, "Your ${contact1.label ?: contact1.name} was closed")
    log.debug "Turning lights off"
    lights.off()
    }
}