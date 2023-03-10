/**
 *  Automatic Thermostat Fan Control
 *
 *  Copyright 2015 Doug Dale
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
    name: "Automatic Thermostat Fan Control",
    namespace: "dougdale",
    author: "Doug Dale",
    description: "Controls on/auto fan setting on a thermostat based on temperature information from remote sensors (meaning, other than the thermostat itself). The fan will be switched from 'auto' to 'on' if the temperature at any one of those sensors goes above or below an absolute setting or the delta between the measured temperature and the current setpoint is greater than a specified value.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Choose thermostat") {
 		input "thermostat", "capability.thermostat"
    }
    section("Choose temperature sensor(s)") {
    	input "tempsensors", "capability.temperatureMeasurement", multiple: true
    }
    section("Specify maximum temperature (default 80)") {
        input "maxtemp", "number", required: true, title: "Maximum temperature?", defaultValue: 80
    }
    section("Specify minimum temperature (default 55)") {
        input "mintemp", "number", required: true, title: "Minimum temperature?", defaultValue: 55
    }
    section("Specify maximum difference between temperature and setpoint (default 5)") {
        input "tempdelta", "number", required: true, title: "Maximum delta?", defaultValue: 5
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
	// Kick off temperature checking on one minute.
	runIn(60, temperatureCheck)
}

// Event handler
def temperatureCheck() {
	def tempInfo = tempsensors.currentTemperature
    def setpoint = thermostat.currentThermostatSetpoint
    def fanMode = "auto"
    
    log.debug "Setpoint ${setpoint}"
    
    // Check the temp at each sensor. If any of the sensors are past the thresholds or the delta
    // from the thermostat setpooint is to big, set the thermostat state to "on".
    tempInfo.each { temp -> 
        log.debug "Temp ${temp}"
        
    	if (temp > maxtemp || temp < mintemp || ((int)temp - (int)setpoint).abs() > tempdelta) {
        	log.info "Sensor temp of ${temp} (setpoint ${setpoint}). Turning fan on."
            fanMode = "on"
        }
    }
    
    log.debug "Fan mode ${fanMode}"
    
    if (thermostat.currentFanMode != fanMode) {
    	log.debug "Fan mode changed"
    	thermostat.setThermostatFanMode(fanMode)
    }

	// Do this check every 30 minutes
	runIn(30*60, temperatureCheck)
}

