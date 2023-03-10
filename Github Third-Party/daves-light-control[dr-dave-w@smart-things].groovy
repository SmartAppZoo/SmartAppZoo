/**
*  Dave's Light Control
*
*  Copyright 2016 Dave Watson
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
definition(
    name: "Dave's light control",
    namespace: "drdavew.co.uk",
    author: "Dave Watson",
    description: "Motion sensor light control",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "setting1"
}


preferences {
    page(name: "firstPage")
}

def firstPage() {
    dynamicPage(name: "firstPage", title: "", install: true, uninstall: true) {
        section() {
            input "themotion2", "capability.motionSensor", required: true, multiple: true, title: "Which sensors?"
            input "theswitch", "capability.switchLevel", required: true, title: "Turn on what?"
        }
        section() {
            input "thelevel", "number", required: true, title: "Dim level?"
            input "dimmerLater", "enum", options: ["No", "Yes"], required: true, defaultValue: 0,  title: "Dimmer after midnight?"
        }
        section() {
            input "minutes", "decimal", required: true, title: "Turn off after no motion for how many minutes?"
        }
        section() {
            input "when", "enum", options: ["Always", "Sunset to sunrise", "Between specific times"], required: true, submitOnChange: true, defaultValue: 0,  title: "When to control the light"
            if ((when == "Between specific times") || (when == "2")) {
                input "fromTime", "time", title: "From", required: true
                input "toTime", "time", title: "To", required: true
            }
            input "override", "enum", options: ["No", "Yes"], required: true, defaultValue: 0,  title: "Override other people?"

        }
    }
}


def installed() {
    log.debug "Installed with settings: ${settings}"
    log.debug "Installed with app settings: ${appSettings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings, ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(themotion2, "motion.active", motionDetectedHandler)
    subscribe(themotion2, "motion.inactive", motionStoppedHandler)
    subscribe(theswitch, "switch.on", switchOnHandler)
    subscribe(theswitch, "switch.off", switchOffHandler)
    subscribe(theswitch, "level", switchLevelHandler)
    log.debug("Subscribed.")
    log.debug("Second sensor, ${themotion2}.")
    if (themotion2 == null){
        log.debug("No sensors.")
    } //else {
    //log.debug("Number of sensors, ${themotion2.size()}.")
    //}
    // Reset the flags.
    atomicState.wasItMe = false
    atomicState.switchOnRequested = false
    atomicState.switchLevelRequested = false
    if ((override == null) || (override == "No")) {
        atomicState.override = false
    } else {
        atomicState.override = true
    }
    if ((dimmerLater == null) || (dimmerLater == "No")) {
        atomicState.dimmerLater = false
    } else {
        atomicState.dimmerLater = true
    }
    if ((when == null) || (when == "Always") || (when == "0")) {
        atomicState.when = "0" // Always (default)
    } else if ((when == "Sunset to sunrise") || (when == "1")) {
        atomicState.when = "1"
    } else if ((when == "Between specific times") || (when == "2")) {
        atomicState.when = "2"
    }
    log.debug "Override? ${atomicState.override}."
    log.debug "Dimmer latter? ${atomicState.dimmerLater}."
    log.debug "When? ${atomicState.when}."
    log.debug "Settings.themotion2 ${settings.themotion2}"
    log.debug "themotion2 ${themotion2}"
    log.debug "List? ${themotion2 instanceof List}"
    log.debug "Sunrise: ${location.currentValue("sunriseTime")}"
    log.debug "Sunset: ${location.currentValue("sunsetTime")}"

}

def amIOperating() {
    // Default to "no".
    def operating = false
    log.debug "when = ${atomicState.when}"
    if (atomicState.when == "0") { // Always running.
        operating = true
    } else if (atomicState.when == "1") { // Between sunset and sunrise.
        // Get sunrise and sunset times.
        def sunTimes = getSunriseAndSunset()
        def between = timeOfDayIsBetween(sunTimes.sunset, sunTimes.sunrise, new Date(), location.timeZone)
        if (between) {
            operating = true
        }
    } else if (atomicState.when == "2") { // Running between particular times.
        def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
        if (between) {
            operating = true
        }
    }
    log.debug("amIOperating() ${operating}.")
    return operating;
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    log.debug "Device: $evt.device"

    // Always cancel a check, even if we are not operating.
    log.debug "Cancelling check."
    unschedule(checkMotion)

    // Only work between the set times.
    if (amIOperating()) {    
        //def currentLevel = theswitch.currentState("level")
        //log.debug "Level: $currentLevel.value"

        // Check if the light is already on.
        def switchState = theswitch.currentState("switch")

        // Am I overriding?
        log.debug "Override = ${atomicState.override}."

        log.debug "Switch is $switchState.value"
        if ((switchState.value == "off") || atomicState.override) {
            log.debug "Light is off (or we are overriding), so switch it on."
            atomicState.wasItMe = true
            atomicState.switchOnRequested = true
            atomicState.switchLevelRequested = true
            //theswitch.on()
            def requiredLevel = thelevel
            if (atomicState.dimmerLater) {
                def between = timeOfDayIsBetween("00:00", "04:00", new Date(), location.timeZone)
                log.debug "After midnight? ${between}"
                if (between) {
                    Float thelevelHalved = thelevel/2
                    log.debug "thelevelHalved ${thelevelHalved}"
                    Integer thelevelRounded = thelevelHalved.round()
                    log.debug "thelevelRounded ${thelevelRounded}"
                    requiredLevel = thelevelRounded
                }
            }
            log.debug "Setting level to ${requiredLevel}."
            theswitch.setLevel(requiredLevel)
        } else {
            log.debug "Light is already on, leave it alone."
            atomicState.switchOnRequested = false
            atomicState.switchLevelRequested = false
        }
    } else {
        log.debug("Not operating.")
    }
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    log.debug "Device: $evt.device"

    // Check if I turned the light on.
    log.debug "Was it me? $atomicState.wasItMe"
    // If it was me that turned it on, wait and see if we need to turn it off.
    if (atomicState.wasItMe) {
        // Check all motion sensors are off.
        def noActive = 0
        def theListOfSensors
        if (themotion2 instanceof List) {
            theListOfSensors = themotion2
        } else {
            theListOfSensors = [ themotion2 ]
        }
        for (def i = 0; i < theListOfSensors.size(); i++) {
            def currentState = theListOfSensors[i].currentState("motion")
            log.debug("Sensor ${i}, ${currentState.value}.")
            if (currentState.value == "active") {
                log.debug("This one is ACTIVE.")
                noActive++
                    }
        }
        log.debug("Number of active sensors, ${noActive}.")
        // If there no active sensors, start the clock.
        if (noActive == 0) {
            log.debug "All sensors are inactive."
            log.debug "Wait and see if we need to turn it off."
            runIn(60 * minutes, checkMotion)
        } else {
            log.debug "Not doing anything yet, ${noActive} active sensor(s)."
        }
    } else {
        log.debug "Nothing to do with me."
    }

}

def switchLevelHandler(evt) {
    if (atomicState.switchLevelRequested) {
        log.debug "Level changed because of me."
        // Turn this flag off, request has been met.
        atomicState.switchLevelRequested = false
    } else {
        log.debug "Level changed by someone else."
        if (atomicState.override == false) {
            atomicState.wasItMe = false
        }
    }
}
def switchOnHandler(evt) {
    log.debug "Switch turned on."
    log.debug "Was it me? $atomicState.wasItMe"
    log.debug "Did I request this? $atomicState.switchOnRequested"
    if (atomicState.switchOnRequested) {
        log.debug "Switched on because of me."
        // Turn this flag off, request has been met.
        atomicState.switchOnRequested = false
    } else {
        log.debug "Switched on by someone else."
        if (atomicState.override == false) {
            atomicState.wasItMe = false
        }
    }    
}

def switchOffHandler(evt) {
    log.debug "Switch turned off."
    log.debug "Cancelling check."
    if (atomicState.override == false) {
        unschedule(checkMotion)
    }
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"

    // Check statuses of the motion sensors.
    def noActive = 0
    def theListOfSensors
    if (themotion2 instanceof List) {
        theListOfSensors = themotion2
    } else {
        theListOfSensors = [ themotion2 ]
    }
    for (def i = 0; i < theListOfSensors.size(); i++) {
        def currentState = theListOfSensors[i].currentState("motion")
        log.debug("Sensor ${i}, ${currentState.value}.")
        if (currentState.value == "active") {
            log.debug("This one is ACTIVE.")
            noActive++
                }
    }
    log.debug("Number of active sensors, ${noActive}.")

    // Are all the sensors off?
    if (noActive == 0) {
        // Yes.
        // Check they have been inactive long enough.
        def threshold = 1000 * 60 * minutes
        log.debug("Need motion sensor to have been inactive for $threshold ms")
        def noNotEnough = 0
        for (def i = 0; i < theListOfSensors.size(); i++) {
            def currentState = theListOfSensors[i].currentState("motion")
            log.debug("Sensor ${i}, ${currentState.date.time}.")
            def elapsed = now() - currentState.date.time
            // Add 10 seconds on, to correct for processing delays.
            elapsed = elapsed + 10000

            // Has the sensor been off for the whole required period?
            if (elapsed >= threshold) {
                log.debug "Motion sensor has been inactive long enough ($elapsed ms)."
            } else {
                log.debug "Motion has not stayed inactive long enough ($elapsed ms)."
                noNotEnough++
                    }
        }    

        // Have all the sensors been off long enough?
        if (noNotEnough == 0) {
            // Yes! Turn the switch off.
            theswitch.setLevel(0)
        } else {
            log.debug "Not doing anything yet, ${noNotEnough} sensor(s) not inactive long enough."
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Not doing anything, ${noActive} active sensor(s)."
    }
}
