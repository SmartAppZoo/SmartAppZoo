/**
 *  Attic Fan Control
 *  Based on Whole House Fan by Brian Steere
 *
 *  Copyright 2017 Bryan Li
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
    name: "Attic Fan Control",
    namespace: "joyfulhouse",
    author: "Bryan Li",
    description: "Toggle an attic fan (switch) based on temperature readings",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan%402x.png"
)

preferences {
    section("Outdoor") {
        input "outTemp", "capability.temperatureMeasurement", title: "Outdoor Thermometer", required: true
        input "minTempDiff", "number", title: "Minimum Temperature Difference", required: true
    }
    
    section("Indoor") {
    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", required: true
        input "minTemp", "number", title: "Minimum Indoor Temperature", required: true
        input "maxTemp", "number", title: "Maximum Indoor Temperature", required: true
        input "fan", "capability.switch", title: "Attic Fan", required: true
    }
    
    section("Override Switch") {
        paragraph "[Optional] Only turn on fan if override switch is off. This behavior is reversed by enabling 'Invert On/Off'."
        input "checkSwitch", "bool", title: "Enable Override Switch", required: true
        input "invertCheckSwitch", "bool", title: "Invert On/Off", required: false
        input "overrideSwitch", "capability.switch", title: "Override Switch", required: false
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
    state.fanRunning = false
    state.overrideOn = false
    
    subscribe(fan, "switch", "switchHandler")
    subscribe(outTemp, "temperature", "checkThings")
    subscribe(inTemp, "temperature", "checkThings")
    subscribe(overrideSwitch, "switch", "overrideSwitch")
}

// Main function for Attic Fan Control
def checkThings(evt) {
    // Only operate if override switch is on/off or not checking the override switch
    if(!state.overrideOn || !settings.checkSwitch){
        // Current Temperatures
        def outsideTemp = settings.outTemp.currentTemperature
        def insideTemp = settings.inTemp.currentTemperature    

        // Begin Logic
        def shouldRun = true

        // Check Temperatures
        if(insideTemp < outsideTemp) {
            shouldRun = false
        }
        
        // Check Temperature Difference
        if (insideTemp - outsideTemp < settings.minTempDiff) {
            shouldRun = false
        }

        // Check if Minimum Temperature is set
        if(insideTemp < settings.minTemp) {
            shouldRun = false
        }

        // Override checks if Maximum Temperature reached
        if(insideTemp > settings.maxTemp) {
            shouldRun = true
        }

        // Turn fan on or off
        if(shouldRun) {
            if(settings.fan.currentValue("switch") == 'off') {
                fan.on()
                state.fanRunning = true
            }
        } else if(!shouldRun) {
            if(settings.fan.currentValue("switch") == 'on') {
                fan.off()
                state.fanRunning = false
            }
        }
    }
}

// Helper Functions
// Override Switch
def overrideSwitch(evt){
    if(!settings.invertCheckSwitch) {
      if(evt.value == "on"){
        state.overrideOn = true
      } else {
        state.overrideOn = false
      }
    } else {
      if(evt.value == "on"){
        state.overrideOn = false
      } else {
        state.overrideOn = true
      }
    }
    
    if(state.overrideOn) {
      fan.off()
    }
    
    checkThings(evt)
}

// Fan On/Off handlers
def switchHandler(evt){
    if(evt.value == "on")
        state.fanRunning = true
    else if(evt.value == "off")
        state.fanRunning = false
        
    checkThings(evt)
}
