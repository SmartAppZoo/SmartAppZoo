/**
 *  Hidden Door
 *
 *  Author: SmartThings
 */

//Not in catalog, only for Ben E.

preferences {
	section("When the secret knock happens ...") {
		input "globe", "capability.contactSensor", title: "Where?"
	}
	section("Open the hidden door...") {
		input "door", "device.doorShield", title: "Which?"
	}
}

def installed()
{
	subscribe(globe, "contact.closed", contactClosedHandler)
}

def updated()
{
	unsubscribe()
	subscribe(globe, "contact.closed", contactClosedHandler)
}

def contactClosedHandler(evt)
{
	log.debug "Globe value: $evt.value, $evt"
	log.debug "Opening door"
	door.open()
}
