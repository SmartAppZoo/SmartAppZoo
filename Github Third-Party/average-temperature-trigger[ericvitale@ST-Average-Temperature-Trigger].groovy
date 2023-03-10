/**
 *  Average Temperature Trigger
 *
 *  Version 1.0.2 - 08/04/14
 *   -- Added the ability to just control an HVAC fan based on average temperature.
 *   -- Now a parent child app for multiple automations.
 *  Version 1.0.1 - 07/24/16
 *   -- Added proper logging to make the app less verbose.
 *   -- Added the active setting.
 *   -- Renamed to Average Temperature Trigger
 *
 *  Version 1.0.0 - 07/05/16
 *   -- Initial Build
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
 *  You can find this SmartApp @ https://github.com/ericvitale/ST-Average-Temperature-Trigger
 *  Don't forget the Settable Temperature Measurement virtual device in the same repository as the SmartApp.
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */
 
definition(
    name: "${appName()}",
    namespace: "ericvitale",
    author: "Eric Vitale",
    description: "Control a thermostat or update a virtual temperature reporting device based on the average temperature of temperature sensors.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "startPage")
    page(name: "parentPage")
    page(name: "childStartPage")
}

def startPage() {
    if (parent) {
        childStartPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: false, uninstall: true) {
        section("Create a new child app.") {
            app(name: "childApps", appName: appName(), namespace: "ericvitale", title: "New Temperature Automation", multiple: true)
        }
    }
}

def childStartPage() {
	return dynamicPage(name: "childStartPage", title: "", install: true, uninstall: true) {
        section("Settable Sensor") {
            input "settableSensor", "capability.temperatureMeasurement", title: "Virtual Settable Temperature Sensor", multiple: false, required: false
            input "setVirtualTemp", "bool", title: "Set virtual temp based on average temp?", required: true, defaultValue: false
        }

        section("Select your thermostat.") {
            input "thermostat", "capability.thermostat", multiple:false, title: "Thermostat", required: false
            input "setThermostat", "bool", title: "Set your thermostate temperature based on average temperature?", required: true, defaultValue: false
            input "controlFan", "bool", title: "Turn on your HVAC fan based on an average temperature?", required: true, defaultValue: false
        }

        section("Select your temperature sensors.") {
            input "temperatureSensors", "capability.temperatureMeasurement", multiple: true
        }

        section("Select the temperature at which you want to begin cooling.") {
            input "maxTemp", "decimal", title: "Max Temperature", range: "*", required: false
        }

        section("Select the temperature at which you want to cool to.") {
            input "coolingSetpoint", "decimal", title: "Cooling Setpoint", range: "*", required: false
        }

        section("Select the temperature at which you want to begin heating.") {
            input "minTemp", "decimal", title: "Min Temperature", range: "*", required: false
        }

        section("Select the temperature at which you want to heat to.") {
            input "heatingSetpoint", "decimal", title: "Heating Setpoint", range: "*", required: false
        }

        section("Setting") {
        	label(title: "Assign a name", required: false)
            input "active", "bool", title: "Rules Active?", required: true, defaultValue: true
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "DEBUG", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
        }
    }
}

private def appName() { return "${parent ? "Temperature Automation" : "Average Temperature Trigger"}" }

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "ATT -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "ATT -- Invalid Log Setting"
        }
    }
}

def installed() {
	log("Installed with settings: ${settings}", "INFO")
	initialization()
}

def updated() {
	log("Updated with settings: ${settings}", "INFO")
	unsubscribe()
	initialization()
}

def initialization() {
	log.debug "Begin initialization()."
    
    if(parent) { 
    	initChild() 
    } else {
    	initParent() 
    }
    
    log.debug "End initialization()."
}

def initParent() {
	log.debug "initParent()"
}

def initChild() {
	if(active) {
		log("App is active.", "INFO")
        subscribe(temperatureSensors, "temperature", temperatureHandler)
        
        if(controlFan && setThermostat) {
        	log("You cannot control a thermostat and an HVAC fan with the same automation. Install another. Defaulting to thermostat.", "WARN")
            controlFan = false
        }
        
	    updateTemp()
    } else {
    	log("App is not active.", "INFO")
    }

    log("Initialization complete.", "INFO")
}

def temperatureHandler(evt) {
	log("Temperature event ${evt.descriptionText} and value: ${evt.doubleValue}.", "INFO")
    updateTemp()
}

def updateTemp() {
    def averageTemp = 0.0
    def currentState
    
    temperatureSensors.each() {
    	log("${it.displayName} Temp: ${it.currentValue("temperature")}.", "TRACE")	
        
        currentState = it.currentState("temperature")
        
        log("currentState.integerValue: ${currentState.integerValue}.", "TRACE")

        try {
            averageTemp += currentState.integerValue
        } catch(e) {
        	log("ERROR -- ${e}", "ERROR")
        }
    }
   	
    try {
    	averageTemp = averageTemp / temperatureSensors.size()
    } catch(e) {
    	log("ERROR -- ${e}", "ERROR")
    }
    
    if(setThermostat) {
    	log("Evaluating thermostat rules...", "INFO")
        if(averageTemp > maxTemp) {
        	log("Begin cooling to ${coolingSetpoint}.", "INFO")
            beginCooling(coolingSetpoint)
        } else if(averageTemp < minTemp) {
	        log("Begin heating to ${heatingSetpoint}.", "INFO")
            beginHeating(heatingSetpoint)
        } else {
            log("Temperature is just right.", "INFO")
        }
    }
    
    if(controlFan){
    	log("Evaluating fan rules...", "INFO")
        
        if(thermostat.thermostatMode == "auto" || thermostat.thermostatMode == "off") {
        
            if(averageTemp > maxTemp) {
                log("Turning on fan in order to cool to: ${coolingSetpoint}.", "INFO")
                turnFanOn()
            } else if(averageTemp < minTemp) {
                log("Turning on fan in order to heat to: ${heatingSetpoint}.", "INFO")
                turnFanOn()
            } else {
                if(thermostat.thermostatFanMode == "on") {
                    turnFanAuto()
                    log("Turning fan to auto.", "INFO")
                }

                log("Temperature is just right.", "INFO")
            }
            
    	} else {
        	log("You are already in a running mode of ${thermostat.thermostatMode}, ignoring your fan control request.", "INFO")
        }
    }
    
    if(setVirtualTemp) {
        log("Updating ${settableSensor.label} to ${Math.round(averageTemp * 100) / 100}.", "INFO")
    	settableSensor.setTemperature((Math.round(averageTemp * 100) / 100).toString())
    }
}

def beginCooling(val) {
	log("Setting coolingSetpoint to: ${val}.", "DEBUG")
    thermostat.setCoolingSetpoint(val)
}

def beginHeating(val) {
	log("Setting heatingSetpoint to: ${val}.", "DEBUG")
	thermostat.setHeatingSetpoint(val)
}

def turnFanOn() {
	thermostat.fanOn()
}

def turnFanAuto() {
	thermostat.fanAuto()
}