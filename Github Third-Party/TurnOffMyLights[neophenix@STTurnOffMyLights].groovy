/**
 *  Turn Off My Lights
 *
 *  Copyright 2017 Brian Clapper
 *
 */
 
// return true here to turn on the entries to log.debug 
def isDebug() {
    return false
}

definition(
    name: "Turn Off My Lights",
    namespace: "neophenix",
    author: "Brian Clapper",
    description: "Apparently it is hard to turn off the lights when there isn't motion, so this aims to fix that.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    // WHY do none of my lights have the light or bulb capability, is this, just a thing?
    section("Lights") {
        input "lights", "capability.switch", multiple: true, required: true
    }
    section("Motion Sensors") {
        input "sensors", "capability.motionSensor", multiple: true, required: true
    }
    section("Wait how many minutes?") {
        input "minutes", "number", title: "Minutes", required: true
    }
}

def checkMotion() {
    // First we are going to grab the current value, because if we haven't had a state change in the
    // X minutes we specified for the setting, we won't get anything back and won't know whats going on
    def motion = "inactive"
    sensors.find{
        if (it.currentValue("motion") == "active") {
            motion = "active"
            return true
        }
    }

    // if we have current active motion, just "bail"
    debug("current motion: ${motion}")
    if (motion == "inactive") {
        // if we don't have motion, we want to figure out when the last time we had motion was, if it was
        // within our threshold, then we can skip this run.
        
        // Use a Calendar object to then convert to Date so we can just grab the last X mins instead of days
        def cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, -1 * minutes)
        def since = cal.getTime()  // convert cal to date
        def lastMotion = 0
        
        sensors.each {
            def states = it.statesBetween("motion", since, new Date())

            // Look at all the returned states and if we see an active one, set our lastMotion to that to compare later
            states.find {
                motion = it.stringValue
                def epoch = it.date.getTime()
                debug("${it.displayName} motion was ${motion} on ${epoch}")
                if (motion == "active") {
                    if (epoch > lastMotion) {
                        lastMotion = epoch
                    }
                    return true // breaks out of the find, we don't need to go any further
                }
            }
        }
        debug("last activity on ${lastMotion}")
        
        // grab a new cal instance to get our epoch to compare to our minutes setting
        cal = Calendar.getInstance()
        def diffMins = (cal.getTimeInMillis() - lastMotion) / 60000
        debug("minutes since motion ${diffMins}")
        if (diffMins >= minutes) {
            // look at each light, if it is on, turn it off
            lights.each {
                if (it.currentValue("switch") == "on") {
                    debug("turning off ${it.displayName}")
                    it.off()
                }
            }
        }
    }
}

def installed() {
    debug("Installed with settings: ${settings}")
    initialize()
}

def updated() {
    debug("Updated with settings: ${settings}")
    unsubscribe()
    initialize()
}

def initialize() {
    schedule("0 * * * * ?", checkMotion)
}

def debug(str) {
    if (isDebug()) {
        log.debug(str)
    }
}
