/**
 *  Close the Garage
 *
 */
definition(
    name: "Close the Garage",
    namespace: "KristopherKubicki",
    author: "Kristopher Kubicki",
    description: "Closes the garage door if no motion detected",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png")



preferences {
	section("Close the garage door if there's no motion..."){
		input "motions", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("After how many minutes..."){
		input "minutes", "number", title: "Minutes?"
	}
	section("Close this door..."){
		input "doors", "capability.doorControl"
	}
}


def installed() {
   initialized()
}

def updated() {
	unsubscribe()
    initialized()
}

def initialized() {
    subscribe(motions, "motion", motionHandler)
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"

	if (evt.value == "inactive") {
		runIn(minutes*60, "scheduleCheck", [overwrite: true])
	}
}

def scheduleCheck() {
    // better way to do this is to check the inactivity time of each device
    	def success = 1
        for (sensor in settings.motions) { 
			if(sensor.currentValue("motion") == "active") { 
				success = 0
			}
		}
        
		if (success > 0 && doors.currentValue("door") != "closed") {
			log.debug "closing door"
			doors.close()
		}
}
