/**
 *  Setpoint
 *
 *  Copyright 2015 Eric Roberts
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
    name: "Setpoint",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "The child of Thermostat Setpoint Manager",
    category: "Green Living",
    parent: "baldeagle072:Thermostat Setpoint Manager",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page name:"mainPage"
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
	if (trigger == "Mode") {
    	initMode()
    }
    if (trigger == "Time") {
    	initTime()
    }
    if (trigger == "Switch") {
    	initSwitch()
    }
}

def mainPage() {
	dynamicPage(name: "mainPage", title:"Setpoint", install:true, uninstall: true) {
		thermostatAndSetpointInputs()
		triggerInput()
        actionInputs()
        section([title:"Options", mobileOnly:true]) {
           	label title:"Assign a name", required:false
        }
    }
}

def thermostatAndSetpointInputs() {
	section("Thermostat and Setpoints") {
    	input "thermostat", "capability.thermostat", title: "Thermostat", required: true
        input "heatingSetpoint", "number", title: "Heting Setpoint", required: false
        input "coolingSetpoint", "number", title: "Cooling Setpoint", required: false
        input "tstatMode", "enum", title: "Thermostat Mode", options:["Off", "Heat", "Cool", "Auto"], required: false
    }
}

def triggerInput() {
	section("How do you want to trigger the setpoint?") {
    	input "trigger", "enum", title: "Trigger", options: ["Mode", "Time", "Switch"], submitOnChange: true, required: true, multiple: false
    }
}

def actionInputs() {
	modeInputs()
    timeInputs()
    switchInputs()
}

def modeInputs() {
	if (trigger == "Mode") {
    	section {
        	input "triggerModes", "mode", title: "Which mode(s)?", required: true, multiple: true
        }
    }
}

def initMode() {
	subscribe(location, "mode", locationHandler)
}

def locationHandler(evt) {
	if (triggerModes.find{it == evt.value}) {
    	changeSetpoint(evt)
    }
}

def timeInputs() {
	if (trigger == "Time") {
    	section {
        	input "triggerTime", "time", title: "What time?", required: true
        }
    }
}

def initTime() {
	schedule(triggerTime, changeSetpoint)
}

def switchInputs() {
	if (trigger == "Switch") {
    	section {
        	input "triggerSwitch", "capability.switch", title: "Which switch?", required: true, multiple: true
            input "triggerSwitchState", "enum", title: "When turned on or off?", options:["On", "Off", "Both"], required: true, mutiple: false
        }
    }
}

def initSwitch() {
	if (triggerSwitchState == "On") {
    	subscribe(triggerSwitch, "switch.on", changeSetpoint)
    } else if (triggerSwitchState == "Off") {
    	subscribe(triggerSwitch, "switch.off", changeSetpoint)
    } else {
    	subscribe(triggerSwitch, "switch", changeSetpoint)
    }
}

def changeSetpoint(evt) {
	log.debug("Changing")
    if (evt) { log.debug(evt.value) } else { log.debug(now()) }
	if (heatingSetpoint) {
    	log.debug "New Heat Setpoint $heatingSetpoint"
    	thermostat.setHeatingSetpoint(heatingSetpoint)
    }
    if (coolingSetpoint) {
    	log.debug "New Cool Setpoint $coolingSetpoint"
    	thermostat.setCoolingSetpoint(coolingSetpoint)
    }
    switch (tstatMode) {
    	case "Off":
        	log.debug "New Mode - Off"
            thermostat.off()
            break
        case "Heat":
        	log.debug "New Mode - Heat"
            thermostat.heat()
            break
        case "Cool":
        	log.debug "New mode - Cool"
            thermostat.cool()
            break
        case "Auto":
        	log.debug "New mode - Auto"
            thermostat.auto()
            break
        default:
        	log.debug "Not changing tstat mode"
            break
    }
}