/**
 *  Averaging Thermostat Rev A
 *
 *  Copyright 2015 Keith Croshaw
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
    name: "Averaging Thermostat Rev A",
    namespace: "keithcroshaw",
    author: "Keith Croshaw",
    description: "Averaging Thermostat",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences() {
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Temperature Setpoint when thermostat mode is Heat and SmartThings mode is Home..." ) {
		input "heatingHomeSetpoint", "decimal", title: "Degrees"
	}
	section("Temperature Setpoint when thermostat mode is Heat and SmartThings mode is Sleep..." ) {
		input "heatingSleepSetpoint", "decimal", title: "Degrees"
	}
    section("Temperature Setpoint when thermostat mode is Heat and SmartThings mode is Away..." ) {
		input "heatingAwaySetpoint", "decimal", title: "Degrees"
	}
	section("Temperature Setpoint when thermostat mode is Cooling and SmartThings mode is Home...") {
		input "coolingHomeSetpoint", "decimal", title: "Degrees"
	}
    section("Temperature Setpoint when thermostat mode is Cooling and SmartThings mode is Sleep...") {
		input "coolingSleepSetpoint", "decimal", title: "Degrees"
	}
	section("Temperature Setpoint when thermostat mode is Cooling and SmartThings mode is Away...") {
		input "coolingAwaySetpoint", "decimal", title: "Degrees"
	}    
	section("Temperature sensor in Living space 1") {
		input "liveSpace1", "capability.temperatureMeasurement", title: "Temp Sensor", required: true, multiple: false
        input "liveSpace1PosOffset", "bool", title: "Positive offset?"
        input "liveSpace1PosOffsetValue", "decimal", title: "Degrees", required: false
        input "liveSpace1NegOffset", "bool", title: "Negative offset?"
        input "liveSpace1NegOffsetValue", "decimal", title: "Degrees", required: false
	}
    section("Temperature sensor in Living space 2") {
		input "liveSpace2", "capability.temperatureMeasurement", title: "Temp Sensor", required: true, multiple: false
        input "liveSpace2PosOffset", "bool", title: "Positive offset?"
        input "liveSpace2PosOffsetValue", "decimal", title: "Degrees", required: false
        input "liveSpace2NegOffset", "bool", title: "Negative offset?"
        input "liveSpace2NegOffsetValue", "decimal", title: "Degrees", required: false
	}
    section("Temperature sensor in Bedroom 1") {
		input "bedroom1", "capability.temperatureMeasurement", title: "Temp Sensor", required: true, multiple: false
        input "bedroom1PosOffset", "bool", title: "Positive offset?"
        input "bedroom1PosOffsetValue", "decimal", title: "Degrees", required: false
        input "bedroom1NegOffset", "bool", title: "Negative offset?"
        input "bedroom1NegOffsetValue", "decimal", title: "Degrees", required: false
	}    
    section("Temperature sensor in Bedroom 2") {
		input "bedroom2", "capability.temperatureMeasurement", title: "Temp Sensor", required: true, multiple: false
        input "bedroom2PosOffset", "bool", title: "Positive offset?"
        input "bedroom2PosOffsetValue", "decimal", title: "Degrees", required: false
        input "bedroom2NegOffset", "bool", title: "Negative offset?"
        input "bedroom2NegOffsetValue", "decimal", title: "Degrees", required: false
	}
}

def installed()
{
	log.debug "enter installed, state: $state"
        
	subscribeToEvents()
}

def updated()
{
	log.debug "enter updated, state: $state"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents()
{
	subscribe(location, changedLocationMode)
	subscribe(thermostat, "temperature", temperatureHandler)
	subscribe(thermostat, "thermostatMode", temperatureHandler)
    subscribe(liveSpace1, "temperature", temperatureHandler)
    subscribe(liveSpace2, "temperature", temperatureHandler)
    subscribe(bedroom1, "temperature", temperatureHandler)
    subscribe(bedroom2, "temperature", temperatureHandler)
	evaluate()
}

def changedLocationMode(evt)
{
	log.debug "changedLocationMode mode: $evt.value, heat: $heat, cool: $cool"
	evaluate()
}

def temperatureHandler(evt)
{
	evaluate()
}

def evaluate()
{

        //Init all spaces
        def livSpace1Final = 0
        def livSpace2Final = 0
        def bedroom1Final = 0
        def bedroom2Final = 0
        //log.debug "first actual reading1: $liveSpace1.currentTemperature"
        //log.debug "after init: $livSpace1Final"
        //log.debug "2after init: $livSpace2Final"

        //Set living space 1's value with offset
        if (liveSpace1PosOffset) {
        	livSpace1Final = liveSpace1.currentTemperature + liveSpace1PosOffsetValue
        }
      	if (liveSpace1NegOffset) {
        	livSpace1Final = liveSpace1.currentTemperature - liveSpace1NegOffsetValue
        }
        if (!liveSpace1PosOffset && !liveSpace1NegOffset) {
        	livSpace1Final = liveSpace1.currentTemperature
        }
        log.debug "Livespace1 after offset: $livSpace1Final"
        //Set living space 2's value with offset
        if (liveSpace2PosOffset) {
        	livSpace2Final = liveSpace2.currentTemperature + liveSpace2PosOffsetValue
        }
        if (liveSpace2NegOffset) {
        	livSpace2Final = liveSpace2.currentTemperature - liveSpace2NegOffsetValue
        }
        if (!liveSpace2PosOffset && !liveSpace2NegOffset) {
        	livSpace2Final = liveSpace2.currentTemperature
        }
        
        //Set bedroom 1's value with offset
        if (bedroom1PosOffset) {
        	bedroom1Final = bedroom1.currentTemperature + bedroom1PosOffsetValue
        }
      	if (bedroom1NegOffset) {
        	bedroom1Final = bedroom1.currentTemperature - bedroom1NegOffsetValue
        }
        if (!bedroom1PosOffset && !bedroom1NegOffset) {
        	bedroom1Final = bedroom1.currentTemperature
        }
        
        //Set bedroom 2's value with offset
        if (bedroom2PosOffset) {
        	bedroom2Final = bedroom2.currentTemperature + bedroom2PosOffsetValue
        }
      	if (bedroom2NegOffset) {
        	bedroom2Final = bedroom2.currentTemperature - bedroom2NegOffsetValue
        }
        if (!bedroom2PosOffset && !bedroom2NegOffset) {
        	bedroom2Final = bedroom2.currentTemperature
        }
        
		//log.debug "after offset, before eval: $livSpace1Final"
        //log.debug "2after offset, before eval: $livSpace2Final"

        def ct = thermostat.currentTemperature
		def tm = thermostat.currentThermostatMode
        //def csp = thermostat.currentHeatingSetpoint
        //def hsp = thermostat.currentHeatingSetpoint
        def cm = location.mode

		if (tm in ["cool","auto"] && cm == "Home") {
			// air conditioner in Home Mode
			if ((livSpace1Final + livSpace2Final)/2 > coolingHomeSetpoint) {
				thermostat.setCoolingSetpoint(ct - 2)
				log.debug "Cool / Home thermostat.setCoolingSetpoint(${ct - 2}), ON"
			} else {
            	thermostat.setCoolingSetpoint(ct + 2)
            }
		}
		if (tm in ["cool","auto"] && cm == "Bedtime") {
			// air conditioner in Bedtime Mode
			if ((bedroom1Final + bedroom2Final)/2 > coolingSleepSetpoint) {
				thermostat.setCoolingSetpoint(ct - 2)
				log.debug "Cool Bedtime thermostat.setCoolingSetpoint(${ct - 2}), ON"
			} else {
            	thermostat.setCoolingSetpoint(ct + 2)
            }
		}
		if (tm in ["cool","auto"] && cm == "Away") {
			// air conditioner in Away Mode (No averaging)
				thermostat.setCoolingSetpoint(coolingAwaySetpoint)
                log.debug "Cool Away thermostat.setCoolingSetpoint($awayCoolingSetpoint)"
		}
		if (tm in ["heat","emergency heat","auto"] && cm == "Home") {
			// heater in Home Mode
            log.debug "Val1 before averaging $livSpace1Final"
            log.debug "Val2 before averaging $livSpace2Final"
            log.debug "hsp for comp: $hsp"
			if ((livSpace1Final + livSpace2Final)/2 < heatingHomeSetpoint) {
				thermostat.setHeatingSetpoint(ct + 2)
				log.debug "Heat / Home thermostat.setHeatingSetpoint(${ct + 2}), ON"
			} else {
            log.debug "Set the heat to the current temp to stop it: ${ct - 2}"
            thermostat.setHeatingSetpoint(ct - 2)
            }
		}
		if (tm in ["heat","emergency heat","auto"] && cm == "Bedtime") {
			// heater in Bedtime Mode
			if ((bedroom1Final + bedroom2Final)/2 < heatingSleepSetpoint) {
				thermostat.setHeatingSetpoint(ct + 2)
				log.debug "Heat / Bedtime thermostat.setHeatingSetpoint(${ct + 2}), ON"
			} else {
            thermostat.setHeatingSetpoint(ct - 2)
            }
		}
		if (tm in ["heat","emergency heat","auto"] && cm == "Away") {
			// heater in Away Mode (No averaging)
				thermostat.setHeatingSetpoint(heatingAwaySetpoint)
                log.debug "Heat / Away thermostat.setHeatingSetpoint($awayHeatingSetpoint)"
		}
}


// for backward compatibility with existing subscriptions
def coolingSetpointHandler(evt) {
	log.debug "coolingSetpointHandler()"
}
def heatingSetpointHandler (evt) {
	log.debug "heatingSetpointHandler ()"
}
