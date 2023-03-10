/**
 * Presence and Motion based Automation
 *
 * Author: A. Trent Foley, Sr. (trentfoley64)
 * Forked: 12/15/2015
 * Credit to: Lights Off with No Motion and Presence
 * Original Author: Bruce Adelsman
 *
 the logic:
 	set state.motionReported=false
 	subscribe to motion sensor activity
    when inactive reported:
    	set state.motionReported=false
    	schedule motion check for specified minutes
    when active reported:
        set state.motionReported=true
	scheduled motion check:
    	if !state.motionReported
        	if presence checks
        		devices.off
 *
 todo:
 	expand to turn devices on based on motion and presence, including dimming levels
 */

definition(
    name: "Presence and Motion based Automation",
    namespace: "trentfoley64",
    author: "trentfoley64",
    description: "Executes automations based on presence and motion changes with additional conditions.",
    category: "My Apps",
    iconUrl: "http://www.trentfoley.com/ST/icons/presence-and-motion.png",
    iconX2Url: "http://www.trentfoley.com/ST/icons/presence-and-motion@2X.png",
    iconX3Url: "http://www.trentfoley.com/ST/icons/presence-and-motion@3X.png"
)

preferences {
	page name: "mainPage", title: "New Presence and Motion based Automation", install: false, uninstall: true, nextPage: "namePage"
	page name: "namePage", title: "New Presence and Motion based Automation", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {
		// Let user specify devices to control
		section("Devices to turn off") {
			input "devices", "capability.switch", title: "Choose devices", multiple: true
		}
		// Let user specify motion sensor to monitor
		section("Motion Sensor to monitor") {
			input "motionSensor", "capability.motionSensor", title: "Choose motion sensor", multiple: false
			input "motionDelayMins", "number", title: "Minutes since inactivity before valid?"
		}
		// Let user specify presence rules
		section( "Presences", hideable: true, hidden: !(anyMustBePresent||allMustBePresent||anyMustBeAbsent||allMustBeAbsent)) {
			input "anyMustBePresent", "capability.presenceSensor", title: "At least one must be present", multiple: true, required: false
			input "allMustBePresent", "capability.presenceSensor", title: "All must be present", multiple: true, required: false
			input "anyMustBeAbsent", "capability.presenceSensor", title: "At least one must be absent", multiple: true, required: false
			input "allMustBeAbsent", "capability.presenceSensor", title: "All must be absent", multiple: true, required: false
		}
	}
}

// page for allowing the user to give the automation a custom name
def namePage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

def defaultLabel() {
    def msg=""
    if (anyMustBePresent) {
    	msg=msg?"$msg, and ":"" + "any of ${anyMustBePresent} are present"
	}
    if (allMustBePresent) {
    	msg=msg?"$msg, and ":"" + "all of ${allMustBePresent} are present"
    }
    if (anyMustBeAbsent) {
    	msg=msg?"$msg, and ":"" + "any of ${anyMustBeAbsent} are absent"
    }
    if (allMustBeAbsent) {
    	msg=msg?"$msg, and ":"" + "all of ${allMustBeAbsent} are absent"
    }
    "Turn off $devices when no motion $motionDelayMins min" + msg?:" when $msg"
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    state.motionReported=false
	subscribe(motionSensor, "motion", motionHandler)
}

def motionHandler(evt) {
	log.debug "motionHandler $evt.name: $evt.value"
    // If motion has been reported,
    if (evt.value=="active") {
    	// set flag so that we know to
        state.motionReported=true
    }
    // now that no motion has been reported
	else if (evt.value=="inactive") {
        // start the clock waiting for any motion
    	state.motionReported=false
        // schedule motion check, cancelling any prior unexecuted schedules
        runIn(motionDelayMins*60, scheduledMotionCheck)
	}
}

private scheduledMotionCheck() {
	log.debug "scheduledMotionCheck: state.motionReported=${state.motionReported}"
    // If scheduled motion check goes off and no motion has been reported
	if (state.motionReported==false) {
    	// and if everyone is where they are supposed to be...
    	if (checkPresences()==true) { 
        	// turn off all devices
        	devices.off	
        }
    }
}

private checkPresences() {
	// If defined, check anyMustBePresent
	if (anyMustBePresent) {
		// If anyMustBePresent does not contain anyone present, do not change thermostats
		if (!anyMustBePresent.currentValue('presence').contains('present')) {
			return false
		}
	}
	// If defined, check allMustBePresent
	if (allMustBePresent) {
		// If allMustBePresent contains anyone not present, do not change thermostats
		if (allMustBePresent.currentValue('presence').contains('not present')) {
			return false
		}
	}
	// If defined, check anyMustBeAbsent
	if (anyMustBeAbsent) {
		// If anyMustBeAbsent does not contain anyone not present, do not change thermostats
		if (!anyMustBeAbsent.currentValue('presence').contains('not present')) {
			return false
		}
	}
	// If defined, check allMustBeAbsent
	if (allMustBeAbsent) {
		// If allMustBeAbsent contains anyone present, do not change thermostats
		if (allMustBeAbsent.currentValue('presence').contains('present')) {
			return false
		}
	}
    // If we've gotten to here, all checks have passed
    return true
} 

def isActivePresence() {
	// check all the presence sensors, make sure none are present
	def noPresence=presenceSensors.find{it.currentPresence=="present"}==null
	!noPresence		
}