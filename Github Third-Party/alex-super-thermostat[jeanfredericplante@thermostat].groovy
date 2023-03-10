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
 *  Virtual Thermostat
 *
 *  Author: JF after SmartThings
 */

definition(
    name: "Alex Super Thermostat",
    namespace: "jeanfredericplante",
    author: "JF Plante",
    description: "Control Alex's realm",
    category: "Green Living",
    iconUrl: "https://cdn2.iconfinder.com/data/icons/modern-future-technology/128/smart-thermostat-128.png",
    iconX2Url: "https://cdn2.iconfinder.com/data/icons/modern-future-technology/128/smart-thermostat-512.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
    section("Choose a motion sensor... "){
		input "motion", "capability.motionSensor", title: "Motion", required: false
	}
	section("Select the heater or air conditioner outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Set the desired temperature (F)..."){
		input "setpoint", "decimal", title: "Set Temp"
	}
	section("Set the desired threshold (F)..."){
		input "temperature_threshold", "decimal", title: "Set Threshold before it toggles"
	}
    section("Refresh rate (min)") {
    	input "refresh_rate_min", "number", title: "check temperature every x minutes", required: false
    }
	section("Select 'heat' for a heater and 'cool' for an air conditioner..."){
		input "mode", "enum", title: "Heating or cooling?", options: ["heat","cool"]
	}
}

def installed()
{
	subscribe(sensor, "temperature", temperatureHandler)

	if (motion) {
		subscribe(motion, "motion", motionHandler)
   }
}

def updated()
{
	unsubscribe()
	subscribe(sensor, "temperature", temperatureHandler)

	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}
    checkTemp()
    setRecurringCheck()
}



def temperatureHandler(evt)
{
    log.debug "temperature change detected"
    checkTemp()
}

def motionHandler(evt)
{
        log.debug "motion detected"
        checkTemp()
}

def setRecurringCheck()
{
		def refresh_rate = 5
        if (refresh_rate_min) {
        	refresh_rate = refresh_rate_min
        }
//        log.degug "setting schedule every $refresh_rate"
        schedule("0 0/$refresh_rate * * * ?", checkTemp)

}

def checkTemp()
{
        log.debug "scheduled event handler"
		def lastTemp = sensor.currentTemperature

		if (lastTemp != null) {
			evaluate(lastTemp, setpoint)
		}
}

private evaluate(currentTemp, desiredTemp)
{
	def threshold = 1.0
    if (temperature_threshold) {
      threshold = temperature_threshold
    }
    log.debug "EVALUATE($currentTemp, $desiredTemp) with threshold: $threshold"

	if (mode == "cool") {
		// air conditioner
		if (currentTemp - desiredTemp >= threshold) {
			outlets.on()
		}
		else if (desiredTemp - currentTemp >= threshold) {
			outlets.off()
		}
	}
	else {
		// heater
		if (desiredTemp - currentTemp >= threshold) {
			outlets.on()
		}
		else if (currentTemp - desiredTemp >= threshold) {
			outlets.off()
		}
	}
}
