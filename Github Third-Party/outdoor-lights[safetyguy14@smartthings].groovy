/**
 *  Outdoor_Lights
 *
 *  Copyright 2018 JOSHUA MASON
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
    name: "Outdoor_Lights",
    namespace: "safetyguy14",
    author: "JOSHUA MASON",
    description: "SmartApp to control outdoor lighting",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "Devices", title: "Select your Inputs/Outputs", 
         install: true, uninstall: true, hideWhenEmpty: true) {
            section("Motion Sensors") {
            input "motions_1", "capability.motionSensor", title: "Motion Sensor(s) in Zone 1", multiple: true, required: false
            input "motions_2", "capability.motionSensor", title: "Motion Sensor(s) in Zone 2", multiple: true, required: false
        }
        section("Lights") {
            input "switches_1", "capability.switch", title: "Light(s) in Zone 1", multiple: true, required: false
            input "switches_2", "capability.switch", title: "Light(s) in Zone 2", multiple: true, required: false
        }
        section("Door(s)") {
            //input "garageDoorControls", "capability.garageDoorControl", title: "Garage Door(s)", multiple: true, required: false
            //input "doorControls", "capability.doorControl", title: "Door(s)", multiple: true, required: false
            input "contactSensors_1", "capability.contactSensor", title: "Door(s) in Zone 1", multiple: true, required: false
            input "contactSensors_2", "capability.contactSensor", title: "Door(s) in Zone 2", multiple: true, required: false
        }
        section("Not Present debounce timer [default=5 minutes]") {
            input "notPresentThreshold", "number", title: "Time in minutes after door closes/motion no longer detected to turn off", required: false
        }
        section("Zone Control Method") {
        	input "controlMode_1", "enum", title: "Zone 1: 0 = Time, 1 = Time+Motion, 2 = Time+Door, 3 = Time+Door+Motion", options: [0, 1, 2, 3], required: false
            input "controlMode_2", "enum", title: "Zone 2: 0 = Time, 1 = Time+Motion, 2 = Time+Door, 3 = Time+Door+Motion", options: [0, 1, 2, 3], required: false
        }
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Would you like push notifications", metadata: [values: ["Yes", "No"]], required: false
		}        
        section("want to turn on mega-debugging?") {
            input "debugMode", "bool", title: "Debug Mode?", required: false
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
	initialize()
}

def initialize() {
	// using sunrise and sunset to trigger actions
    subscribe(location, "sunsetTime", sunsetTimeHandler)
    subscribe(location, "sunriseTime", sunriseTimeHandler)
    
    // schedule it to run the day you install it/update it
    scheduleTurnOn(location.currentValue("sunsetTime"))
    scheduleTurnOff(location.currentValue("sunriseTime"))
    
    // if motion or door is selected for a zone, schedule a turn off
    
    // using motion sensors to detect presence
    // but only if installed
    if (controlMode_1 == 1 || controlMode_1 == 3) {
        if (motions_1 != null && motions_1 != "") {
            subscribe(motions_1, "motion", motion1EvtHandler)
        }
    }
    if (controlMode_2 == 1 || controlMode_2 == 3) {
        if (motions_2 != null && motions_2 != "") {
            subscribe(motions_2, "motion", motion2EvtHandler)
        }
    }
    // using contact sensors on doors
    // as a method of presence
    if (controlMode_1 == 2 || controlMode_1 == 3) {
        if (contactSensors_1 != null && contactSensors_1 != "") {
            subscribe(contactSensors_1, "contact", contact1EvtHandler)
        }
    }
    if (controlMode_2 == 2 || controlMode_2 == 3) {
        if (contactSensors_2 != null && contactSensors_2 != "") {
            subscribe(contactSensors_2, "contact", contact2EvtHandler)
        }
    }
}

// event handler for sunset time
// when I find out the sunset time, schedule the lights to turn on with an offset
def sunsetTimeHandler(evt) {
    if(debugMode) {
        log.debug "$evt.name: $evt.value"
    }
    scheduleTurnOn(evt.value)
}

// event handler for sunrise time
// when I find out the sunrise time, schedule the lights to turn off
def sunriseTimeHandler(evt) {
    if(debugMode) {
        log.debug "$evt.name: $evt.value"
    }
    scheduleTurnOff(evt.value)
}

// event handler for motion sensor events
def motion1EvtHandler(evt) {
	if (evt.value == "active") {
		if(debugMode) {
        	log.debug "Motion in Zone 1... ${evt.device}"
		}
	}
}

def scheduleTurnOn(sunsetString) {
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    def offset = 30 //minutes
    
    //calculate the offset
    def timeBeforeSunset = new Date(sunsetTime.time - (offset * 60 * 1000))

    log.debug "Scheduling for: $timeBeforeSunset (sunset is $sunsetTime)"

    //schedule this to run one time
    runOnce(timeBeforeSunset, turnOnLights)
}

def scheduleTurnOff(sunriseString) {
    //get the Date value for the string
    def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)
    def offset = 30 //minutes
    
    //calculate the offset
    def timeAfterSunrise = new Date(sunriseTime.time + (offset * 60 * 1000))
    if(debugMode) {
   		log.debug "Scheduling for: $timeAfterSunrise (sunrise is $sunriseTime)"
	}
    //schedule this to run one time
    runOnce(timeAfterSunrise, turnOffLights)
}

def turnOffLights() {
	switches_1.each{
        if(it.currentSwitch == "off") {
            if(debugMode) {	
                log.debug "${it.device} lights already off"
            }
        }
        else { 
            it.off()
            def msg = "Turned off the ${it.device} lights"
            log.info msg
            if (sendPushMessage != "No") {
            	sendPush(msg)
            }
        }
    }
	switches_2.each{
        if(it.currentSwitch == "off") {
            if(debugMode) {	
                log.debug "${it.device} lights already off"
            }
        }
        else { 
            it.off()
            def msg = "Turned off the ${it.device} lights"
            log.info msg
            if (sendPushMessage != "No") {
            	sendPush(msg)
            }
        }
    }
}

def turnOnLights() {
	switches_1.each{
        if(it.currentSwitch == "on") {
            if(debugMode) {	
                log.debug "${it.device} lights already on"
            }
        }
        else {
        	it.on()
            def msg = "Turning on the ${it.device} lights"
            log.info msg
            if (sendPushMessage != "No") {
            	sendPush(msg)
            }
        }
    }
	switches_2.each{
        if(it.currentSwitch == "on") {
            if(debugMode) {	
                log.debug "${it.device} lights already on"
            }
        }
        else {
        	it.on()
            def msg = "Turning on the ${it.device} lights"
            log.info msg
            if (sendPushMessage != "No") {
            	sendPush(msg)
            }
        }
    }
}