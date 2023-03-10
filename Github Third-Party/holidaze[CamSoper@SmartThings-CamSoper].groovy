/**
 *  Holidaze
 *
 *  Copyright 2017 Cam Soper
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
    name: "Holidaze",
    namespace: "CamSoper",
    author: "Cam Soper",
    description: "Remembers the colors of a collection of RGBW bulbs, switches to incandescent, and back again.  Great for outdoor RGBW bulbs you want to use for decoration AND useful lighting!",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Hardware") {
    		input "bulbs", "capability.colorControl", required: true, multiple: true, title: "RGBW Bulbs" 
		input "virtualSwitch", "capability.switch", required: true, multiple: false, title: "Virtual Switch" 
		input "physicalSwitch", "capability.switch", required: true, multiple: false, title: "Physical Switch" 
	}
    section("Behaviors") {
		input "powerCycle", "bool", required: true, multiple: false, title: "Power-cycle the bulbs after restoring color"
        	input "enabled", "bool", required: true, multiple: false, title: "Change bulbs to warm light when virtual switch turned on", defaultValue: true
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
	subscribe virtualSwitch, "switch.on", onVirtualOn
	subscribe virtualSwitch, "switch.off", onVirtualOff
	subscribe physicalSwitch, "switch.on", onPhysicalOn
	subscribe physicalSwitch, "switch.off", onPhysicalOff
}

def onVirtualOn(evt) {
	log.debug "Virtual switch on"
	def hadToTurnOnPower = (physicalSwitch.currentSwitch == "off")
	
	if(hadToTurnOnPower) {
    	log.debug "Physical switch is off. Turning on."
        physicalSwitch.on()
    }
    else {
	rememberTheColors()
    }
    if(enabled) {
	setWarmWhite()
    }
    
    atomicState.hadToTurnOnPower = hadToTurnOnPower
}

def onVirtualOff(evt) {
	log.debug "Virtual switch off"
    if(enabled) {
    	restoreTheColors()
    }
    
    if(atomicState.hadToTurnOnPower) {
    	physicalSwitch.off()
    }
    else if (powerCycle) {
    	runIn(10, powerCyclePhysicalSwitch)
    }
}

def powerCyclePhysicalSwitch() {
	log.debug "Cycling physical switch"
	physicalSwitch.off()
	physicalSwitch.on()
}

def onPhysicalOn(evt) {
	log.debug "Physical switch on"
	if(virtualSwitch.currentSwitch == "off") {
		runIn(5, restoreTheColors)
	}
}

def onPhysicalOff(evt) {
	log.debug "Physical switch off"
	atomicState.lastPhysicalState = "off"
	virtualSwitch.off()
}

def rememberTheColors() {
	log.debug "Storing color info."
	def colorInfo = []

	for(def b in bulbs) {
		log.debug "Recording values - name: ${b.label}, deviceId: ${b.deviceNetworkId}, sat: ${b.currentSaturation}, hue: ${b.currentHue}, level: ${b.currentLevel}"
		colorInfo.add([deviceId: b.deviceNetworkId, hue: b.currentHue, saturation: b.currentSaturation, level: b.currentLevel])
	}


	atomicState.colorInfo = colorInfo
}

def restoreTheColors(){
	log.debug "Retrieving color info"
	def colorInfo = atomicState.colorInfo
	for(def b in bulbs) {
		def currColorInfo = colorInfo.find { ci -> ci.deviceId == b.deviceNetworkId }

		log.debug "Setting values - name: ${b.label}, deviceId: ${currColorInfo.deviceId}, sat: ${currColorInfo.saturation}, hue: ${currColorInfo.hue}, level: ${currColorInfo.level}"
		def colorMap = [hue:currColorInfo.hue.toInteger(),saturation:currColorInfo.saturation.toInteger(),level:currColorInfo.level.toInteger()]
		b.setColor(colorMap)
		//Apparently level isn't sent in setColor().
		if(currColorInfo.level.toInteger() < 100) {
			b.setLevel(currColorInfo.level.toInteger())
		}
	}
}

def setWarmWhite() {
	log.debug "Setting color temp."
	bulbs.setColorTemperature(2700)
	bulbs.setLevel(100)
}
