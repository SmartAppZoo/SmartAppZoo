/**
 *  Lights Controller 2
 *
 *  Copyright 2017 Nick Yantikov
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
    name: "Lights Controller 2",
    namespace: "ovonick",
    author: "Nick Yantikov",
    description: "Control lights and dimmers with motion, door open",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Events that trigger lights on") {
        input "switchesInput",    "capability.switch",        title: "Switch Devices", required: true,  multiple: true
        input "motionsInput",     "capability.motionSensor",  title: "Motion Sensors", required: false, multiple: true
        input "doorSensorsInput", "capability.contactSensor", title: "Door Sensors",   required: false, multiple: true
    }

    section("Lights that are being controlled") {
        input "switchesControlled", "capability.switch",      title: "Switch Devices",  required: false,  multiple: true
        input "dimmersControlled",  "capability.switchLevel", title: "Dimmers Devices", required: false,  multiple: true
    }
    
    section("Parameters") {
        input "isTimeRestricted",             "bool",    title: "React on motion/door open only between Sunset and Sunrise?", required: true, defaultValue: true
        input "sunriseSunsetOffset",          "number",  title: "Sunrise/Sunset Offset"
        input "motionActiveAction",           "enum",    title: "When motion detected do the following",                      required: true, defaultValue: "Turn on and maintain lights on", options: ["Turn on and maintain lights on", "Maintain lights on"]
        input "turnOffIntervalPhysicalEvent", "number",  title: "Minutes to turn off after pressing on a switch",             required: true, defaultValue: 30, range: "1..*"
        input "turnOffIntervalSensorEvent",   "number",  title: "Minutes to turn off after motion/door open",                 required: true, defaultValue: 10, range: "1..*"
        input "dimmToLevel",                  "number",  title: "Dimmers level 30 seconds before turning off"
        input "isKeepOnWhileDoorIsOpen",      "bool",    title: "Keep on while door is open?",                                required: true, defaultValue: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "updated(); settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(motionsInput,     "motion.active",   motionActiveHandler)
    subscribe(switchesInput,    "switch.on",       switchOnHandler)
    subscribe(switchesInput,    "switch.off",      switchOffHandler)
    subscribe(doorSensorsInput, "contact.open",    contactOpenHandler)
}

def logEvent(event) {
    log.debug "logEvent(); device: ${event?.device?.label}, event: ${event}, event.value: ${event?.value}, event.physical: ${event?.physical}, event.digital: ${event?.digital}, event.source: ${event?.source}, event.data: ${event?.data}"
}

def switchOnHandler(event) {
	logEvent(event)
    
    if (!event.physical) {
        return
    }
    
    scheduleTurnOff(turnOffIntervalPhysicalEvent)
}

def switchOffHandler(event) {
	logEvent(event)

    if (!event.physical) {
        return
    }
    
    atomicState.turnOnAfter = now() + 10 * 1000 // 10 seconds silent period where no event will turn switches on after physically turning off
    atomicState.turnOffAfter = 0;
}

def motionActiveHandler(event) {
	logEvent(event)
    
    //dimmersControlled?.getSupportedAttributes().each {log.debug "${it.name}"}
    //dimmersControlled?.getProperties().each {log.debug "${it}"}
    //log.debug "name: ${dimmersControlled?.name}, id: ${dimmersControlled?.id}"
    //dimmersControlled?.each {log.debug "${it.switchState.value}"}
    
    if ("Maintain lights on".equals(motionActiveAction)) {
    	maintainOn()
    } else {
		turnOn()
    }
}

def contactOpenHandler(event) {
	logEvent(event)
    
    turnOn()
}

def turnOn() {
	if (!isShouldTurnOn()) {
    	 return
    }

    restoreSavedDimmerLevels()
    dimmersControlled*.on()
    switchesControlled*.on()
    
    scheduleTurnOff(turnOffIntervalSensorEvent)
    
    resetSavedDimmerLevels()
}

def maintainOn() {
	if (!isAnyLightOn()) {
    	return
    }
    
    restoreSavedDimmerLevels()
    
    scheduleTurnOff(turnOffIntervalSensorEvent)
    
    resetSavedDimmerLevels()
}

def isAnyLightOn() {
	def isAnyLightOn = getDevicesWithSwitchState(switchesControlled, "on") || getDevicesWithSwitchState(dimmersControlled, "on")
    
    log.debug "isAnyLightOn(); isAnyLightOn: ${isAnyLightOn}"
    
    return isAnyLightOn
}

def getDevicesWithSwitchState(devices, stateValue) {
	return devices?.findAll { it.switchState.value == stateValue }
}

def restoreSavedDimmerLevels() {
	def savedDimmerLevels = atomicState.savedDimmerLevels;

    log.debug "restoreSavedDimmerLevels(); restoring saved dimmer levels: ${savedDimmerLevels}"

    if (savedDimmerLevels) {
    	dimmersControlled.each {
            def savedLevel = savedDimmerLevels[it.id]
            log.debug "restoreSavedDimmerLevels(); restoring saved dimmer level: ${savedLevel} for dimmer id ${it.id}"
            if (savedLevel) {
	        	it.setLevel(savedLevel)
            }
        }
    }
}

def saveDimmerLevels(dimmers) {
	def savedDimmerLevels = dimmers.collectEntries {[it.id, it.currentLevel]}
    
    log.debug "saveDimmerLevels(); saving dimmer levels: ${savedDimmerLevels}"
    
    atomicState.savedDimmerLevels = savedDimmerLevels
}

def resetSavedDimmerLevels() {
	atomicState.savedDimmerLevels = null;
}

def isShouldTurnOn() {
	log.debug "isShouldTurnOn(); isTimeRestricted: ${isTimeRestricted}"
    
	return (!isTimeRestricted || isBetweenSunsetAndSunrise()) && (now() > atomicState.turnOnAfter)
}

def isBetweenSunsetAndSunrise() {
	def sunriseAndSunsetWithOffset = getSunriseAndSunset(sunriseOffset: "00:${sunriseSunsetOffset}", sunsetOffset: "-00:${sunriseSunsetOffset}")

	def isTimeOfDayIsBetweenSunsetAndSunrise = timeOfDayIsBetween(sunriseAndSunsetWithOffset.sunset, sunriseAndSunsetWithOffset.sunrise, new Date(), location.timeZone)
    
    log.debug "isBetweenSunsetAndSunrise(); isTimeOfDayIsBetweenSunsetAndSunrise: ${isTimeOfDayIsBetweenSunsetAndSunrise}"
    
    return isTimeOfDayIsBetweenSunsetAndSunrise
}

def scheduleTurnOff(turnOffInterval) {
    def turnOffIntervalSeconds = turnOffInterval * 60

	def currentTurnOffAfter = atomicState.turnOffAfter?:0;
	def newTurnOffAfter = now() + (turnOffIntervalSeconds * 1000)

	log.debug "scheduleTurnOff(); turnOffIntervalSeconds: ${turnOffIntervalSeconds}, currentTurnOffAfter: ${currentTurnOffAfter}, newTurnOffAfter: ${newTurnOffAfter}, difference ${(currentTurnOffAfter - newTurnOffAfter) / 1000} "

	if (newTurnOffAfter > currentTurnOffAfter) {
    	atomicState.turnOffAfter = newTurnOffAfter;
        
		def runInSeconds = Math.max(60, turnOffIntervalSeconds) + 10; // adding 10 seconds so that when switchesOffOrDimHandler runs now() would be past atomicState.turnOffAfter

		log.debug "scheduleTurnOff(); scheduling switchesOffOrDimHandler to runInSeconds: ${runInSeconds}"
		runIn(runInSeconds, switchesOffOrDimHandler)
    }
}

def switchesOffOrDimHandler(event) {
	// Some motion sensors don't fire "motion.active" events for 4-5 minutes after first motion is sensed
    // In this cases we need to check if motion sensors are still in "motion.active" mode then
    // schedule this event one more time
    if (motionsInput?.any {it.currentMotion == 'active'}) {
        log.debug "switchesOffOrDimHandler(); Rescheduling turn off handler because there are motion sensors still in motion.active mode"
        scheduleTurnOff(turnOffIntervalSensorEvent)
        return
    }

	if (isKeepOnWhileDoorIsOpen && (doorSensorsInput.any {it.currentContact == 'open'})) {
        log.debug "switchesOffOrDimHandler(); Rescheduling turn off handler because there are contact sensors in contact.open mode"
        scheduleTurnOff(turnOffIntervalSensorEvent)
        return
    }
    
	def currentTurnOffAfter = atomicState.turnOffAfter;
    def now = now();
    
	if (now < currentTurnOffAfter) {
        log.debug "switchesOffOrDimHandler(); Rescheduling turn off handler because currentTurnOffAfter: ${currentTurnOffAfter} is in the future. now: ${now}"
        scheduleTurnOff(turnOffIntervalSensorEvent)
        return;
    }

	def dimmersThatAreOn = getDevicesWithSwitchState(dimmersControlled, "on")
    
    if (dimmersThatAreOn) {
    	saveDimmerLevels(dimmersThatAreOn)
    	dimmersThatAreOn*.setLevel(dimmToLevel)
        runIn(30, dimmersFullyOffHandler)
    }
    
    getDevicesWithSwitchState(switchesControlled, "on")*.off()
}

def dimmersFullyOffHandler() {
	def currentTurnOffAfter = atomicState.turnOffAfter;
    def now = now();
    
    log.debug "dimmersFullyOffHandler(); currentTurnOffAfter: ${currentTurnOffAfter}, now: ${now}"

	if (now < currentTurnOffAfter)
	    return

	def dimmersThatAreOn = getDevicesWithSwitchState(dimmersControlled, "on")

	restoreSavedDimmerLevels()
	dimmersThatAreOn*.off()
    
    resetSavedDimmerLevels()
}