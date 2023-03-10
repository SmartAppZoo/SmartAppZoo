/**
 *  Testing App
 *
 *  Copyright 2017 Rob Leonard
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
    name: "Testing App",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "jhgfds",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "AdditionalSettingsPage")
    page(name: "InputTypePage")
    page(name: "DevicesPage")
    page(name: "CapabilitiesPage")
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
}

def AdditionalSettingsPage() {
	dynamicPage(name: "AdditionalSettingsPage", title: "Additional Options",nextPage: "InputTypePage", install: false, uninstall: true) {
    	// Time Settings
        section() {
        	input "startingX", "enum", title: "Only between these times", options: ["A specific time", "Sunrise", "Sunset"], submitOnChange: true, required: false
            if(startingX == "A specific time") 
            	input "starting", "time", title: "Start time", required: true
            else if(startingX == "Sunrise") 
                input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
            else if(startingX == "Sunset") 
            	input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
        	if(startingX != null) {
            	input "endingX", "enum", title: "Ending at", options: ["A specific time", "Sunrise", "Sunset"], submitOnChange: true, required: true
           		if(endingX == "A specific time") 
            		input "ending", "time", title: "End time", required: true
            	else if(endingX == "Sunrise") 
            		input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
            	else if (endingX == "Sunset") 
            		input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0 
        	}
		}
        // 
        section() {
        	input "modes", "mode", title: "Only during these mode(s)", multiple: true, required: false
        }
  

	}
}
def InputTypePage() {
	 dynamicPage(name: "InputTypePage", title: "Input Types", nextPage: "DevicesPage", uninstall: true, install: false) {
     	// Input Types
        section() {
			label(name: "TextInput", title: "Your Text", required: false)
            input "TextPrompt", "text", title: "Text?", required: false
			input "boolPrompt", "bool", title: "Yes / No ?", required: false
			input "decPrompt", "decimal", title: "decimal?", defaultValue: 1.5, required: false
            input "numPrompt", "number", title: "number 1-100", range: "1..100", defaultValue: 1, required: false
            input "emailPrompt", "email", title: "Email Address(s)", required: false
			input "enumPrompt", "enum", title: "Pick a choice(s)", options:["yes", "no"], multiple: true , required: false
            input "hubPrompt", "hub", title: "Hub(s)", multiple: true, required: false
            input "iconPrompt", "icon", title: "Icon?", defaultValue: "st.Food & Dining.dining16", required: false 
            input "phonePrompt", "phone", title: "phone?", description: "Enter phone", required: false
            input "timePrompt", "time", title: "time?", description: "Enter time", required: false
            input "passwordPrompt", "password", title: "password", description: "Enter password", required: false
        }
     }
}
def DevicesPage() {
	 dynamicPage(name: "DevicesPage", title: "Devices", nextPage: "CapabilitiesPage", uninstall: true, install: false) {
     	section() {
        	input "JoinNotifiers", "device.JoinNotifier",title: "JoinNotifier", multiple: true, hideWhenEmpty: true
        }
     }
}
def CapabilitiesPage () {
    dynamicPage(name: "CapabilitiesPage", title: "Capabilities", uninstall: true, install: true) {
    	// Capibilities
        // started from http://docs.smartthings.com/en/latest/capabilities-reference.html
        section() {
        	input "1", "capability.accelerationSensor",title: "accelerationSensor", multiple: true, hideWhenEmpty: true
            input "2", "capability.actuator",title: "actuator", multiple: true, hideWhenEmpty: true
            input "3", "capability.alarm",title: "alarm", multiple: true, hideWhenEmpty: true
            input "4", "capability.battery",title: "battery", multiple: true, hideWhenEmpty: true
            input "5", "capability.beacon",title: "beacon", multiple: true, hideWhenEmpty: true
            input "6", "capability.bridge",title: "bridge", multiple: true, hideWhenEmpty: true
            input "7", "capability.bulb",title: "bulb", multiple: true, hideWhenEmpty: true
            input "8", "capability.button",title: "button", multiple: true, hideWhenEmpty: true
            input "9", "capability.carbonDioxideMeasurement",title: "carbonDioxideMeasurement", multiple: true, hideWhenEmpty: true
            input "10", "capability.carbonMonoxideDetector",title: "carbonMonoxideDetector", multiple: true, hideWhenEmpty: true
            input "11", "capability.colorControl",title: "colorControl", multiple: true, hideWhenEmpty: true
            input "12", "capability.colorTemperature",title: "colorTemperature", multiple: true, hideWhenEmpty: true
            input "13", "capability.configuration",title: "configuration", multiple: true, hideWhenEmpty: true
            input "14", "capability.consumable",title: "consumable", multiple: true, hideWhenEmpty: true
            input "15", "capability.contactSensor",title: "contactSensor", multiple: true, hideWhenEmpty: true
            input "16", "capability.doorControl",title: "doorControl", multiple: true, hideWhenEmpty: true
            input "17", "capability.estimatedTimeOfArrival",title: "estimatedTimeOfArrival", multiple: true, hideWhenEmpty: true
            input "18", "capability.garageDoorControl",title: "garageDoorControl", multiple: true, hideWhenEmpty: true
            input "19", "capability.holdableButton",title: "holdableButton", multiple: true, hideWhenEmpty: true
            input "20", "capability.illuminanceMeasurement",title: "illuminanceMeasurement", multiple: true, hideWhenEmpty: true
            input "21", "capability.imageCapture",title: "imageCapture", multiple: true, hideWhenEmpty: true
            input "22", "capability.indicator",title: "indicator", multiple: true, hideWhenEmpty: true
            input "23", "capability.infraredLevel",title: "infraredLevel", multiple: true, hideWhenEmpty: true 
            input "24", "capability.light",title: "light", multiple: true, hideWhenEmpty: true
            input "25", "capability.lock",title: "lock", multiple: true, hideWhenEmpty: true
            input "26", "capability.lockOnly",title: "lockOnly", multiple: true, hideWhenEmpty: true
            input "27", "capability.mediaController",title: "mediaController", multiple: true, hideWhenEmpty: true 
            input "28", "capability.accelerationSensor",title: "holdableButton", multiple: true, hideWhenEmpty: true
            input "29", "capability.accelerationSensor",title: "holdableButton", multiple: true, hideWhenEmpty: true
            input "30", "capability.accelerationSensor",title: "holdableButton", multiple: true, hideWhenEmpty: true
            input "31", "capability.accelerationSensor",title: "holdableButton", multiple: true, hideWhenEmpty: true 
            input "32", "capability.accelerationSensor",title: "holdableButton", multiple: true, hideWhenEmpty: true
            input "33", "capability.accelerationSensor",title: "holdableButton", multiple: true, hideWhenEmpty: true
            input "34", "capability.accelerationSensor",title: "holdableButton", multiple: true, hideWhenEmpty: true
            input "35", "capability.accelerationSensor",title: "holdableButton", multiple: true, hideWhenEmpty: true 
        }
    }
}

private timeToRun() {

	log.debug "Running timeToRun"
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
	def start = null
	def stop = null
    
    if(startingX =="A specific time" && starting!= null)
    	start = timeToday(starting,location.timeZone)
    if(endingX == "A specific time" && ending!= null)
        stop = timeToday(ending,location.timeZone)
        
    if(startingX == "Sunrise")
    	start = s.sunrise
     if(startingX == "Sunset")
    	start = s.sunset
     if(endingX == "Sunrise")  
      	stop = s.sunrise
     if(endingX == "Sunset")
     	stop = s.sunset
	
    if(start == null || stop == null)
    	return true
    
     if(stop < start) 
     	stop = stop + 1
    
    log.debug "start: ${start} | stop: ${stop}"
    return timeOfDayIsBetween(start, stop, (new Date()), location.timeZone)
}
private modeOk() {
	log.debug "Running modeOk"
	if(modes == null) return true
	def result = !modes || modes.contains(location.mode)
	return result
}