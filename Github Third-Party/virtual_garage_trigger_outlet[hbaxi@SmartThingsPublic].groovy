/**
 *  Virtual Garage Triggers Outlet
 *
 *  Author: chrisb
 */

// Automatically generated. Make future change here.
definition(
    name: "Virtual Garage triggers outlet",
    namespace: " ",
    author: "ChrisB",
    description: "Tapping a SmartSense Virtual Garage Sensor Button triggers and outlet connected to a relay to open/close a garage door, and then shuts off the outlet after 4 seconds.",
    category: "My Apps",
    iconUrl: "http://drive.google.com/uc?export=view&id=0B9oYaOR7L7rmdTkyU2pvX0doaVk",
    iconX2Url: "http://drive.google.com/uc?export=view&id=0B9oYaOR7L7rmdTkyU2pvX0doaVk",
    oauth: true)

preferences {
	section("When a Virtual Garage Door is tapped..."){
		input "GarageSensor1", "capability.contactSensor", title: "Which?"
	}
	section("Trigger which outlet?"){
		input "switches", "capability.switch"
	}
}


def installed()
{
	subscribe(GarageSensor1, "buttonpress.true", contactOpenHandler)
}

def updated()
{
	unsubscribe()
	subscribe(GarageSensor1, "buttonpress.true", contactOpenHandler)
}

def contactOpenHandler(evt) {
	log.debug "$evt.value: $evt, $settings"
	log.trace "Turning on switches: $switches"
	switches.on()
}