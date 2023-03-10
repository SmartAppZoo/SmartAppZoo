/**
 *  Heat Boost
 *
 *  Copyright 2016 Keary Griffin
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
    name: "Heat Boost",
    namespace: "kearygriffin",
    author: "Keary Griffin",
    description: "Heat boost",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Which Thermostat") {
    input "thermostat", "capability.thermostat", title: "Where?", multiple: false, required: true
	}
	section("When the temperature difference is too low") {
    input "temperatureSensor", "capability.temperatureMeasurement", title: "Where?", multiple: false, required: true
	}
    section("By a difference of") {
    input "difference", "decimal", title: "Degrees", required: true
    }
    section("maximum") {
    input "maxtemp", "decimal", title: "max temp Degrees", required: false
    }
    section("Turn on which heater outlet") {
    input "outlet", "capability.switch", title: "Which?", multiple: false, required: true
	}
    section("Enable only in these modes") {
    input "modes", "mode", title: "select enabled mode(s)", multiple: true
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

  subscribe(temperatureSensor, "temperature", executeCheck)
  subscribe(thermostat, "temperature", executeCheck)
  subscribe(thermostat, "thermostatMode", executeCheck)
  subscribe(thermostat, "presence", executeCheck)
  subscribe(thermostat, "thermostatOperatingState", executeCheck)
  subscribe(thermostat, "heatingSetpoint", executeCheck)
  subscribe(location, "mode", executeCheck)
temperatureCheck()
}

def executeCheck(evt) {
	temperatureCheck()
	
}

def temperatureCheck() {
	def presence = thermostat.currentValue("presence");
    def mode = thermostat.currentValue("thermostatMode");
    def opState = thermostat.currentValue("thermostatOperatingState");
    def thermTemp = thermostat.currentValue("temperature")
    def heatPoint = thermostat.currentValue("heatingSetpoint")
    def sensorTemp = temperatureSensor.currentValue("temperature");
    def locationMode = location.currentMode
    log.debug("Mode: ${locationMode}");
    def isActiveMode = (modes.size() == 0 || modes.contains(locationMode));
    log.debug("IsAnActiveMode: ${isActiveMode}");
	log.debug("ThermPresence: ${presence}");
	log.debug("ThermMode: ${mode}");
    log.debug("ThermState: ${opState}");
    log.debug("ThermTemp: ${thermTemp}");
	log.debug("SensorTemp: ${sensorTemp}");
	log.debug("HeatSetPoint: ${heatPoint}");
    
    def desiredTemp = Math.min(thermTemp, heatPoint);
    def actualDifference = desiredTemp - sensorTemp;
    log.debug("Desired: ${desiredTemp}");
	log.debug("Diff: ${actualDifference}");
    
    def turnOn = true;
    if (!isActiveMode)
    	turnOn = false;
    else if (mode != 'heat' && mode != 'auto')
    	turnOn = false;
    else if (presence != 'present')
    	turnOn = false;
    else if (opState != 'idle' && opState != 'fan only')
    	turnOn = false;
    else if (actualDifference <= 2)
    	turnOn = false;
    else if (sensorTemp >= maxtemp)
    	turnOn = false;
        
    log.debug("Set boost state: ${turnOn}");
    if (turnOn) {
    	outlet.on();
        //thermostat.fanOn();
    } else {
    	if (opState == 'heating') {
        	log.debug("Heat is on, turning on heater outlet");
        	outlet.on();
        }
        else {
	    	log.debug("Heat is off, turning off heater outlet");
        	outlet.off();
        }    
        //thermostat.fanAuto();
    }
}