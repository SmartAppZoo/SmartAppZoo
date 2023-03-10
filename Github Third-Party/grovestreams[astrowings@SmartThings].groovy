/**
 *  GroveStreams
 *
 *  Copyright © 2016 Phil Maynard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0												*/
 	       private urlApache() { return "http://www.apache.org/licenses/LICENSE-2.0" }			/*
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Adapted from example written by Jason Steele with usage instructions and code found at
 *    https://www.grovestreams.com/developers/getting_started_smartthings.html
 *  
 * 
 *	VERSION HISTORY										*/
 	 private versionNum() { return "version 2.10" }
     private versionDate() { return "17-Nov-2016" }		/*
 * 
 *    v2.10 (17-Nov-2016) - added logging of 'thermostatSetpoint' attribute
 *    v2.00 (15-Nov-2016) - code improvement: store images on GitHub, use getAppImg() to display app images
 *                        - added option to disable icons
 *                        - added option to disable multi-level logging
 *						  - moved 'About' to its own page
 *						  - added link to readme file
 *    v1.21 (07-Nov-2016) - bug fix: modify thermostat logging to output true/false instead of heating/idle
 *    v1.20 (06-Nov-2016) - added device type 'thermostat' and logging of 'thermostatOperatingState' attribute
 *    v1.12 (04-Nov-2016) - update href state & images
 *    v1.11 (02-Nov-2016) - add link for Apache license
 *    v1.10 (02-Nov-2016) - implement multi-level debug logging function
 *    v1.02 (01-Nov-2016) - code improvement: standardize pages layout
 *	  v1.01 (01-Nov-2016) - code improvement: standardize section headers
 *    v1.00 (30-Oct-2016) - copied code from example (https://www.grovestreams.com/developers/getting_started_smartthings.html)
 *
*/
definition(
    name: "GroveStreams",
    namespace: "astrowings",
    author: "Jason Steele",
    description: "Log to GroveStreams",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office8-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office8-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office8-icn@3x.png")


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	page(name: "pageMain")
    page(name: "pageSensors")
    page(name: "pageSettings")
    page(name: "pageLogOptions")
    page(name: "pageAbout")
    page(name: "pageUninstall")
}


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

private		appImgPath()			{ return "https://raw.githubusercontent.com/astrowings/SmartThings/master/images/" }
private		readmeLink()			{ return "https://github.com/astrowings/SmartThings/blob/master/smartapps/astrowings/grovestreams.src/readme.md" }


//   -----------------------------
//   ***   PAGES DEFINITIONS   ***

def pageMain() {
    dynamicPage(name: "pageMain", install: true, uninstall: false) {
    	section(){
        	paragraph "", title: "This SmartApp logs events from selected sensors to the GroveStreams data analytics platform"
        }
		section() {
            href "pageSensors", title: "Sensors", description: sensorDesc, image: getAppImg("home30-icn.png"), required: true, state: sensorsOk ? "complete" : null
		}
		section() {
            href "pageSettings", title: "App settings", description: "", image: getAppImg("configure_icon.png"), required: false
            href "pageAbout", title: "About", description: "", image: getAppImg("info-icn.png"), required: false
		}
    }
}

def pageSensors() {
	dynamicPage(name: "pageSensors", install: false, uninstall: false) {
        section("Log devices...") {
            input "presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
            input "thermostats", "capability.thermostat", title: "Thermostats", required: false, multiple: true
            input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
            input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required: false, multiple: true
            input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
            input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
            input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
            input "switches", "capability.switch", title: "Switches", required: false, multiple: true
            input "waterSensors", "capability.waterSensor", title: "Water sensors", required: false, multiple: true
            input "powers", "capability.powerMeter", title: "Power Meters", required:false, multiple: true
            input "energies", "capability.energyMeter", title: "Energy Meters", required:false, multiple: true
            input "batteries", "capability.battery", title: "Batteries", required:false, multiple: true
        }
    }
}

def pageSettings() {
	dynamicPage(name: "pageSettings", install: false, uninstall: false) {
        section ("GroveStreams Feed PUT API key") {
            input "apiKey", "text", title: "Enter API key"
        }
   		section() {
			label title: "Assign a name", defaultValue: "${app.name}", required: false
            href "pageUninstall", title: "", description: "Uninstall this SmartApp", image: getAppImg("trash-circle-red-512.png"), state: null, required: true
		}
        section("Debugging Options", hideable: true, hidden: true) {
            input "noAppIcons", "bool", title: "Disable App Icons", description: "Do not display icons in the configuration pages", image: getAppImg("disable_icon.png"), defaultValue: false, required: false, submitOnChange: true
            href "pageLogOptions", title: "IDE Logging Options", description: "Adjust how logs are displayed in the SmartThings IDE", image: getAppImg("office8-icn.png"), required: true, state: "complete"
        }
    }
}

def pageAbout() {
	dynamicPage(name: "pageAbout", title: "About this SmartApp", install: false, uninstall: false) { //with 'install: false', clicking 'Done' goes back to previous page
		section() {
        	href url: readmeLink(), title: app.name, description: "Copyright ©2016 Phil Maynard\n${versionNum()}", image: getAppImg("readme-icn.png")
            href url: urlApache(), title: "License", description: "View Apache license", image: getAppImg("license-icn.png")
		}
    }
}

def pageLogOptions() {
	dynamicPage(name: "pageLogOptions", title: "IDE Logging Options", install: false, uninstall: false) {
        section() {
	        input "debugging", "bool", title: "Enable debugging", description: "Display the logs in the IDE", defaultValue: false, required: false, submitOnChange: true 
        }
        if (debugging) {
            section("Select log types to display") {
                input "log#info", "bool", title: "Log info messages", defaultValue: true, required: false 
                input "log#trace", "bool", title: "Log trace messages", defaultValue: true, required: false 
                input "log#debug", "bool", title: "Log debug messages", defaultValue: true, required: false 
                input "log#warn", "bool", title: "Log warning messages", defaultValue: true, required: false 
                input "log#error", "bool", title: "Log error messages", defaultValue: true, required: false 
			}
            section() {
                input "setMultiLevelLog", "bool", title: "Enable Multi-level Logging", defaultValue: true, required: false,
                    description: "Multi-level logging prefixes log entries with special characters to visually " +
                        "represent the hierarchy of events and facilitate the interpretation of logs in the IDE"
            }
        }
    }
}

def pageUninstall() {
	dynamicPage(name: "pageUninstall", title: "Uninstall", install: false, uninstall: true) {
		section() {
        	paragraph "CAUTION: You are about to completely remove the SmartApp '${app.name}'. This action is irreversible. If you want to proceed, tap on the 'Remove' button below.",
                required: true, state: null
        }
	}
}


//   ----------------------------
//   ***   APP INSTALLATION   ***

def installed() {
	debug "installed with settings: ${settings}", "trace"
    initialize()
}
 
def updated() {
    debug "updated with settings ${settings}", "trace"
	unsubscribe()
    initialize()
}
 
def uninstalled() {
    state.debugLevel = 0
    debug "application uninstalled", "trace"
}

def initialize() {
    state.debugLevel = 0
    debug "initializing", "trace", 1
    subscribeToEvents()
    debug "initialization complete", "trace", -1
}

def subscribeToEvents() {
    debug "subscribing to events", "trace", 1
    subscribe(temperatures, "temperature", handleTemperatureEvent)
    subscribe(waterSensors, "water", handleWaterEvent)
    subscribe(humidities, "humidity", handleHumidityEvent)
    subscribe(contacts, "contact", handleContactEvent)
    subscribe(accelerations, "acceleration", handleAccelerationEvent)
    subscribe(motions, "motion", handleMotionEvent)
    subscribe(presence, "presence", handlePresenceEvent)
    subscribe(switches, "switch", handleSwitchEvent)
    subscribe(batteries, "battery", handleBatteryEvent)
    subscribe(powers, "power", handlePowerEvent)
    subscribe(energies, "energy", handleEnergyEvent)
    subscribe(thermostats, "thermostatOperatingState", handleThermostatStateEvent)
    subscribe(thermostats, "thermostatSetpoint", handleThermostatSetpointEvent)
    subscribe(thermostats, "heatingSetpoint", handleHeatingSetpointEvent)
    debug "subscriptions complete", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def handleTemperatureEvent(evt) {
    sendValue(evt) { it.toString() }
}
 
def handleWaterEvent(evt) {
    sendValue(evt) { it == "wet" ? "true" : "false" }
}
 
def handleHumidityEvent(evt) {
    sendValue(evt) { it.toString() }
}
 
def handleContactEvent(evt) {
    sendValue(evt) { it == "open" ? "true" : "false" }
}
 
def handleAccelerationEvent(evt) {
    sendValue(evt) { it == "active" ? "true" : "false" }
}
 
def handleMotionEvent(evt) {
    sendValue(evt) { it == "active" ? "true" : "false" }
}
 
def handlePresenceEvent(evt) {
    sendValue(evt) { it == "present" ? "true" : "false" }
}
 
def handleSwitchEvent(evt) {
    sendValue(evt) { it == "on" ? "true" : "false" }
}
 
def handleBatteryEvent(evt) {
    sendValue(evt) { it.toString() }
}
 
def handlePowerEvent(evt) {
    sendValue(evt) { it.toString() }
}
 
def handleEnergyEvent(evt) {
    sendValue(evt) { it.toString() }
}

def handleThermostatStateEvent(evt) {
    sendValue(evt) { it == "heating" ? "true" : "false" }
}

def handleThermostatSetpointEvent(evt) {
    sendValue(evt) { it.toString() }
}

def handleHeatingSetpointEvent(evt) {
    //sendValue(evt) { it.toString() }
}


//   -------------------
//   ***   METHODS   ***

private sendValue(evt, Closure convert) {
    def compId = URLEncoder.encode(evt.displayName.trim())
    def streamId = evt.name
    def value = convert(evt.value)

    debug "logging to GroveStreams ${compId}, ${streamId} = ${value}", "info"

    def url = "https://grovestreams.com/api/feed?api_key=${apiKey}&compId=${compId}&${streamId}=${value}"

    //Make the actual device the origin of the message to avoid exceeding 12 calls within 2 minutes rule:
    //http://forum.grovestreams.com/topic/155/10-second-feed-put-limit-algorithm-change/
    def header = ["X-Forwarded-For": evt.deviceId]

    try {
        def putParams = [
            uri: url,
            header: header,
            body: []]

        httpPut(putParams) { response ->
            if (response.status != 200 ) {
                debug "GroveStreams logging failed, status = ${response.status}", "error"
            }
        }
    
    } catch (groovyx.net.http.ResponseParseException e) {
        // ignore error 200, bogus exception
        if (e.statusCode != 200) {
            debug "Grovestreams exception: ${e}", "error"
        }
    } catch (Exception e) {
        debug "Grovestreams exception:: ${e}", "error"
    }

}


//   -------------------------
//   ***   APP FUNCTIONS   ***

def getSensorDesc() {
	def result
	if (temperatures || humidities || contacts || accelerations || motions || presence || switches || waterSensors || batteries || powers || energies || thermostats) {
        def numSensors =
        	(temperatures?.size() ?: 0) +
            (humidities?.size() ?: 0) +
            (contacts?.size() ?: 0) +
            (accelerations?.size() ?: 0) +
            (motions?.size() ?: 0) +
            (presence?.size() ?: 0) +
            (switches?.size() ?: 0) +
            (waterSensors?.size() ?: 0) +
            (batteries?.size() ?: 0) +
            (powers?.size() ?: 0) +
            (thermostats?.size() ?: 0) +
            (energies?.size() ?: 0)
        debug ">> numSensors : $numSensors"
        result = "$numSensors sensors selected"
    } else {
    	result = "Select the sensors for which to log events"
    }
    debug ">> sensorDesc : $result"
    return result
}

def getSensorsOk() {
	def result = (temperatures || humidities || contacts || accelerations || motions || presence || switches || waterSensors || batteries || powers || thermostats || energies)
    debug ">> sensorsOk : $result"
    return result
}


//   ------------------------
//   ***   COMMON UTILS   ***

def getAppImg(imgName, forceIcon = null) {
	def imgPath = appImgPath()
    return (!noAppIcons || forceIcon) ? "$imgPath/$imgName" : ""
}

def debug(message, lvl = null, shift = null, err = null) {
	
    def debugging = settings.debugging
	if (!debugging) {
		return
	}
    
    lvl = lvl ?: "debug"
	if (!settings["log#$lvl"]) {
		return
	}
	
    def multiEnable = (settings.setMultiLevelLog == false ? false : true) //set to true by default
    def maxLevel = 4
	def level = state.debugLevel ?: 0
	def levelDelta = 0
	def prefix = "║"
	def pad = "░"
	
    //shift is:
	//	 0 - initialize level, level set to 1
	//	 1 - start of routine, level up
	//	-1 - end of routine, level down
	//	 anything else - nothing happens
	
    switch (shift) {
		case 0:
			level = 0
			prefix = ""
			break
		case 1:
			level += 1
			prefix = "╚"
			pad = "═"
			break
		case -1:
			levelDelta = -(level > 0 ? 1 : 0)
			pad = "═"
			prefix = "╔"
			break
	}

	if (level > 0) {
		prefix = prefix.padLeft(level, "║").padRight(maxLevel, pad)
	}

	level += levelDelta
	state.debugLevel = level

	if (multiEnable) {
		prefix += " "
	} else {
		prefix = ""
	}

    if (lvl == "info") {
    	def leftPad = (multiEnable ? ": :" : "")
        log.info "$leftPad$prefix$message", err
	} else if (lvl == "trace") {
    	def leftPad = (multiEnable ? "::" : "")
        log.trace "$leftPad$prefix$message", err
	} else if (lvl == "warn") {
    	def leftPad = (multiEnable ? "::" : "")
		log.warn "$leftPad$prefix$message", err
	} else if (lvl == "error") {
    	def leftPad = (multiEnable ? "::" : "")
		log.error "$leftPad$prefix$message", err
	} else {
		log.debug "$prefix$message", err
	}
}