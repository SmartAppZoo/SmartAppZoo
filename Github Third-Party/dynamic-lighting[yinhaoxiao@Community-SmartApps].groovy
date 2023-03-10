/**
 *  Dynamic Lighting
 *
 *  Copyright 2015 Tracy Hale
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
    name: "Dynamic Lighting",
    namespace: "thale",
    author: "Tracy Hale",
    description: "Updates the brightness of the given lightbulb based on how bright it is outside",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/switches/light/on.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/switches/light/on@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/switches/light/on@2x.png")


preferences {
	section("Triggers") {
		input "switchButton", "capability.momentary", title: "When this switch is pushed:", required:false
        input "motion", "capability.motionSensor", title: "When motion is detected here:", required:false
	}
	section("Settings") {
		input "lights", "capability.switchLevel", title: "Light(s) to control", required: true, multiple: true
        input "weatherStation", "capability.illuminanceMeasurement", title: "Weather station to use", required: true, multiple: false
        input "toggleMode", "bool", title: "Work in toggle mode? If enabled, will turn lights on/off from triggers. If disabled, it will only turn on"
	}
    section(hideable:true, hidden:true, "Advanced Light Values") {
        input "lowBrightness", "number", title: "Low outside light bulb brightness", defaultValue:100, required:true
        input "lowLuxThreshold", "number", title: "Low outside light top lux value", defaultValue:800, required:true
        input "medBrightness", "number", title: "Medium outside light bulb brightness", defaultValue:60, required:true
        input "medLuxThreshold", "number", title: "Medium outside light top lux value", defaultValue:2000, required:true
        input "highBrightness", "number", title: "High outside light bulb brightness", defaultValue:10, required:true
        input "lightThreshold", "number", title: "Don't turn on lights if outside brightness is more than X lux", defaultValue: 10001, required:true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
	subscribe(switchButton, "momentary.pushed", appHandler)
    subscribe(motion, "motion", appHandler)
    
}

def appHandler(evt) {
	def lux = weatherStation.currentIlluminance
    def lightSwitches = lights?.currentSwitch
    def lightLevels = lights?.currentLevel
    
    log.debug "Light switch is $lightSwitches and the level is set to $lightLevels. Illuminance is currently $lux"
    
    //check if all lights are on. If a combination of on and off, we want to default to turning on.
    if(lightSwitches.toString().contains("off")) {
    	// only turn on lights if the outside light is less than the threshold
        if(lux < lightThreshold) {
            log.debug "At least one light is currently off, so turn them all on"
            lights?.on()

            //set the light value (defaults below)
            if(lux > 0 && lux <= lowLuxThreshold) {
                lights?.setLevel(lowBrightness)
            }
            else if(lux > lowLuxThreshold && lux <= medLuxThreshold) {
                lights?.setLevel(medBrightness)
            }
            else {
                lights?.setLevel(highBrightness)
            }

            log.debug "Light is ${lights?.currentSwitches} and set to ${lights?.currentLevels}"
		}
    }
    else if(toggleMode == true) {
     	log.debug "All lights are currently on, so turn all of them off"
        lights?.off()
    }
}