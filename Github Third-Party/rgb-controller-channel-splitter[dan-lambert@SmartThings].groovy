/**
 *  RGB Controller Channel Splitter
 *
 *  Copyright 2016 Dan Lambert
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
    name: "RGB Controller Channel Splitter",
    namespace: "dan-lambert",
    author: "Dan Lambert",
    description: "Allows each channel of an RGB controller to be controlled independently by a virtual dimmer.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Physical Device") {
    	input "rgbController", "capability.colorControl", title: "RGB Controller", required: true 
	}
    section("Virtual Dimmers") {
    	input "rSwitch", "capability.switchLevel", title: "Red Channel", required: true
        input "gSwitch", "capability.switchLevel", title: "Green Channel", required: true
        input "bSwitch", "capability.switchLevel", title: "Blue Channel", required: true
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
	subscribe(rgbController, "color", rgbControllerColorHandler)
    subscribeToCommand(rSwitch, "switch", switchHandler)
    subscribeToCommand(gSwitch, "switch", switchHandler)
    subscribeToCommand(bSwitch, "switch", switchHandler)
    subscribeToCommand(rSwitch, "level", switchHandler)
    subscribeToCommand(gSwitch, "level", switchHandler)
    subscribeToCommand(bSwitch, "level", switchHandler)
}

def rgbControllerColorHandler(evt) {
    log.debug "rgbControllerColorHandler called with event: deviceId ${evt.deviceId} name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}"
    if (!evt.isStateChange()) {
        return;
    }
    def hexColor = evt.value;
    updateSwitchLevel(rSwitch, hexColor.substring(1, 3))
    updateSwitchLevel(gSwitch, hexColor.substring(3, 5))
    updateSwitchLevel(bSwitch, hexColor.substring(5, 7))
}

def switchHandler(evt) {
    log.debug "switchHandler called with event: deviceId ${evt.deviceId} name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}"
    if (!evt.isStateChange()) {
        return;
    }
	def r = getSwitchLevelHexString(rSwitch)
    def g = getSwitchLevelHexString(gSwitch)
    def b = getSwitchLevelHexString(bSwitch)    
    def newHexColorString = "#${r}${g}${b}"
    def currentHexColorString = rgbController.currentValue("color").toUpperCase();
    log.debug "Current color is ${currentColor}"
    if (newHexColorString != currentHexColorString) {
        log.debug "Updating color to ${newHexColorString}"
        def colorMap = [:]
        colorMap = [hex: newHexColorString]
    	rgbController.setColor(colorMap)
    }
}

def getSwitchLevelHexString(device) {
    def hexString = "00"
	if (device.currentState("switch").value == "on") {
        def level = device.currentState("level").value as Integer
        hexString = new BigInteger(Math.round((level/100) * 255).toString()).toString(16)
        if (1 == hexString.size()) {
            hexString = "0" + hexString
        }
    }
    hexString.toUpperCase()
}

def updateSwitchLevel(device, levelHexString) {
    def newLevel = Math.round(Integer.parseInt(levelHexString, 16) * 100 / 255)
    def currentLevel = device.currentValue("level")
    /* Be tolerant of rounding errors introduced by converting back and forth between 0-100 and 0-255 ranges
     * and update to new level only if current level is more than 1 out.
     */
    if (newLevel < currentLevel - 1 || newLevel > currentLevel + 1) {
        device.setLevel(newLevel)
    }
}
