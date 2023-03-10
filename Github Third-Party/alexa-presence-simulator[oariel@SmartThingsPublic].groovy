definition(
	name: "Alexa Presence Simulator",
	namespace: "oariel",
	author: "oariel",
	description: "Simulate presence change when arriving as motion.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/areas-active.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/areas-active@2x.png"
)

preferences {
	section("When this presence is on") {
		input "presence1", "capability.presenceSensor", multiple: false, required: true
	}
	section("Simulate motion") {
		input "motion1", "capability.motionSensor", multiple: false, required: true
	}
}    

def installed()
{   
	subscribe(presence1, "presence", onHandler)
}

def updated()
{
	unsubscribe()
	subscribe(presence1, "presence", onHandler)
}

def deactivateMotion() {
    motion1.inactive()
}

def onHandler(evt) {
	log.debug evt.value
    if (evt.value == "present") {
	   log.debug "Simulating motion: $motion1"
	   motion1.active()
       runIn(30, "deactivateMotion")
    }
}