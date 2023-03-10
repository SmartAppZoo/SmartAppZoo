/**
 *  Copyright 2015 SmartThings
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
 *  Ceiling fan control (based on virtual thermostat)
 *  -fix issues with initializing for first time.
 *  -mode enum for cooling = 1 instead of cool.
 *  Author: SmartThings,modified  by Brian Russell
 *
 *  NOTE: when the selected mode that is running under changes to something else, the outputs will remain in the last state!  make sure turn these
 *  off in the other mode state logic if needed.
 */
definition(
    name: "Virtual Thermostat2",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Control a space heater or window air conditioner in conjunction with any temperature sensor, like a SmartSense Multi.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Select the ceiling fan outlet or switch."){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Set the desired setpoint..."){
		input "setpoint", "decimal", title: "Setpoint", required: true
	}
	section("Set the desired differential temperature (2 deg default)"){
		input "differential", "decimal", title: "Differential"
	}
	section("Select fan mode."){
		input "fanMode", "enum", title: "Heating or cooling?", options: ["heat","cool"]
	}
    section("Enable control."){
		input "enable", "bool", title: "Enable?", required: true
	}
/*    section("Modes to run in.  Outputs will turn OFF when not in these modes."){
        input "modes", "mode", title: "only when mode is", multiple: true, required: false
        }*/
}

def installed()
{
	subscribe(sensor, "temperature", temperatureHandler)
    subscribe(location, "mode", modeChangeHandler)
/*	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}*/
    initialize()
}

def updated()
{
	unsubscribe()
    if(enable)
    {
		subscribe(sensor, "temperature", temperatureHandler)
        subscribe(location, "mode", modeChangeHandler)
        initialize()
    }
/*	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}
*/
}

def initialize()
{   
    log.debug("initialize")
    state.enabled = enabled
   	state.coolingCall= false
    state.heatingCall = false
	def lastTemp = sensor.currentTemperature
    log.debug("initial temp = $lastTemp, modes = $location.modes, mode=$location.mode")
	if (lastTemp != null) {
		handleTemperature(lastTemp)
	}
}

def modeChangeHandler(evt) {
    //state.enabled = getModeOk()
    log.debug "mode changed to ${evt.value}"
/*    if(!state.enabled)
   	{
	       log.debug "Outlets OFF - not in selected mode(s)."
           outlets.off()
    } */
}

def handleTemperature(currentTemp)
{
	if (enable) {
		evaluate(currentTemp, setpoint , differential)
	}
}

def temperatureHandler(evt)
{
        handleTemperature(evt.doubleValue)
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private evaluate(currentTemp, desiredTemp, differentialTemp)
{
	def threshold = 2.0
    if (differentialTemp > threshold)
    { 
        threshold = differential
    }
        
    log.debug "EVALUATE($currentTemp, $desiredTemp, $threshold) mode=$fanMode"
	if (fanMode == "1" || fanMode == "cool") {
		// cooling
		if (currentTemp >= desiredTemp) {
	       log.debug "Cooling Outlets ON"
           outlets.on()
           state.coolingCall = true
		}
		else if (state.coolingCall && (currentTemp < desiredTemp - threshold)){
	       log.debug "Cooling Outlets OFF"
			outlets.off()
            state.coolingCall = false
		}
	}
	else {
		// heating mode
		if (currentTemp <= desiredTemp) {
	       log.debug "Heating Outlets ON"
           outlets.on()
           state.heatingCall = true
           }
		else if (currentTemp > desiredTemp + threshold){
	       log.debug "Heating Outlets OFF"
           outlets.off()
           state.heatingCall = false
           }
	}
}

/*def motionHandler(evt)
{
	if (evt.value == "active") {
		def lastTemp = sensor.currentTemperature
		if (lastTemp != null) {
			evaluate(lastTemp, setpoint)
		}
	} else if (evt.value == "inactive") {
		def isActive = hasBeenRecentMotion()
		log.debug "INACTIVE($isActive)"
		if (isActive || emergencySetpoint) {
			def lastTemp = sensor.currentTemperature
			if (lastTemp != null) {
				evaluate(lastTemp, isActive ? setpoint : emergencySetpoint)
			}
		}
		else {
			outlets.off()
		}
	}
}
*/


/* private hasBeenRecentMotion()
{
	def isActive = false
	if (motion && minutes) {
		def deltaMinutes = minutes as Long
		if (deltaMinutes) {
			def motionEvents = motion.eventsSince(new Date(now() - (60000 * deltaMinutes)))
			log.trace "Found ${motionEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
			if (motionEvents.find { it.value == "active" }) {
				isActive = true
			}
		}
	}
	else {
		isActive = true
	}
	isActive
}
*/