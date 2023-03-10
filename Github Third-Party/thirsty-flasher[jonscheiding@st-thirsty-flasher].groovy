/**
 *  Thirsty Flasher
 *
 *  Copyright 2017 Jon Scheiding
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
    name: "Thirsty Flasher",
    namespace: "jonscheiding",
    author: "Jon Scheiding",
    description: "SmartApp that flashes a light when a water sensor goes dry.",
    category: "My Apps",
    pausable: true,
    iconUrl: "https://github.com/material-icons/material-icons-png/raw/master/png/black/invert_colors/baseline.png",
    iconX2Url: "https://github.com/material-icons/material-icons-png/raw/master/png/black/invert_colors/baseline-2x.png",
    iconX3Url: "https://github.com/material-icons/material-icons-png/raw/master/png/black/invert_colors/baseline-4x.png")


preferences {
    section("Water Sensor") {
        input "sensor", "capability.waterSensor", title: "Sensor", required: true
    }
    section("Switch To Flash") {
        input "lights", "capability.switch", title: "Switches", multiple: true, required: true
    }
    section("Notifications") {
        input "sendNotifications", "bool", title: "Send Notifications", required: true
    }
}

def shouldBeFlashing() {
    if (sensor.currentWater != "dry") {
    	log.debug("No need to flash, sensor is ${sensor.currentWater}.")
        return false
    }

    log.debug("Flash light because sensor is ${sensor.currentWater} and mode is ${location.currentMode}.")

    return true
}

def startFlashingIfNecessary(e) {
    if (!shouldBeFlashing()) {
        return
    }

	captureInitialLightState()

    notifyIfNecessary()
    
    flashIfNecessary()
}

def captureInitialLightState() {
    log.debug("Capturing previous light state before starting flashing.")
    state.lights = [:]
    lights.each {
        log.debug("Light ${it.id} is ${it.currentSwitch}.")
        state.lights[it.id] = it.currentSwitch
    }
}

def restoreInitialLightState() {
    log.debug("Restoring previous light state.")
    state.lights = state.lights ?: [:]
    lights.each {
        log.debug("Light ${it.id} was ${state.lights[it.id]}.")
    	switch(state.lights[it.id]) {
            case "on": it.on(); break
            case "off": it.off(); break
        }
    }
    state.lights = [:]
}

def notifyIfNecessary() {
    def msg = "${app.label} is thirsty!"
	log.info msg

    if(!sendNotifications) {
        return
    }

    sendPush msg
}

def flashIfNecessary() {
    if (!shouldBeFlashing()) {
        restoreInitialLightState()
        return
    }
    
    lights.each { it.on() }

    flipSwitch()
    runIn(2, flipSwitch)
    runIn(4, flashIfNecessary)
}

def flipSwitch() {
    lights.each { 
	    if(it.currentSwitch == "off") {
        	log.debug("Turn ${it.displayName} on.")
	        it.on()
        } else {
        	log.debug("Turn ${it.displayName} off.")
        	it.off()
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    subscribe(sensor, "water.dry", startFlashingIfNecessary)
    subscribe(location, "mode", startFlashingIfNecessary)
    startFlashingIfNecessary()
}
