/**
 *  Humidifier Controller
 *
 *  Copyright 2019 Matt
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
    name: "Humidifier",
    namespace: "smartthings",
    author: "Matt",
    description: "Monitors thermostat actions, indoor humidity and outdoor temperature and turns a switch on to the humidifier in the event humidity needs raising.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png")
    
preferences {
    section("Choose the thermostat to read values from and the humidifier switch to control.") {
        input "thermostat", "device.myEcobeeDevice", required: true, multiple: false, title: "Select the thermostat."
        input "humidifier", "capability.switch", required: true, multiple: false, title: "Select the humidifier switch."
        input "humidifier_runtime", "enum", required: true, title: "Select how long to run the humidifier (in minutes) during each call for heat.", options: [1, 2, 3, 4, 5]
    }
}

def installed() {
    subscribe(thermostat, "thermostatOperatingState", thermostatOperatingHandler)
    // A fail safe to prevent the humidifier from being left running
    runEvery5Minutes("humidifierSwitchHandler", [data: [failsafe: true]])
    // Reset the humidifier run time each day at midnight
    schedule("0 0 0 * * ?", "humidifierRunTimeReset")
    humidifierRunTimeReset()
}

def updated() {
    unsubscribe()
    unschedule()
    subscribe(thermostat, "thermostatOperatingState", thermostatOperatingHandler)
    
    // A fail safe to prevent the humidifier from being left running
    runEvery5Minutes("humidifierSwitchHandler", [data: [failsafe: true]])
    // Reset the humidifier run time each day at midnight
    schedule("0 0 0 * * ?", "humidifierRunTimeReset")
    humidifierRunTimeReset()
}

def thermostatOperatingHandler(evt) {
	// A map of outdoor temperature : recommended indoor relative humidity
	def humidity_map = [:]
	humidity_map = [
		"40" : "45",
		"30" : "40",
		"20" : "35",
		"10" : "30",
		"0" : "25",
		"-10" : "20",
		"-20" : "15"
	]
	if (evt.value.toString() == "heating") {
    	def current_temperature = thermostat.currentValue("weatherTemperatureDisplay").toInteger()
        def current_humidity = thermostat.currentValue("remoteSensorAvgHumidity").toInteger()
        
        sendNotificationEvent("[HUMIDIFIER] The furnace has called for heat. Outdoor temperature: ${current_temperature}. Indoor humidity: ${current_humidity}%.")
        
        def target_humidity = null
        humidity_map.any { temperature, humidity ->
            if (current_temperature <= temperature.toInteger()) {
                target_humidity = humidity.toInteger()
            } else {
            	return true
            }
        }
        
        if (target_humidity == null) {
            sendNotificationEvent("[HUMIDIFIER] staying off. The current outdoor temperature of ${current_temperature} is too high to run the humidifier.")
        } else {
            if (current_humidity <= target_humidity) {
                try {
                    if (humidifier.currentValue("switch") == "off") {
                        // Log the humidifier run time
                        state.humidifier_on = new Date().getTime() / 1000
                        // Turn the humidifer switch ON
                        humidifier.on()
                        // Run it for the selected time period
                        // Use runOnce() instead of runIn() which does not seem very reliable
                        def now = new Date()
                        def runTime = new Date(now.getTime() + ((humidifier_runtime.toInteger() * 60) * 1000))
                        runOnce(runTime, humidifierSwitchHandler, [data: [failsafe: false]])

                        sendNotificationEvent("[HUMIDIFIER] turning ON for ${humidifier_runtime} minutes.")
                    }
                } catch(e) {
                    sendNotificationEvent("[HUMIDIFIER] thermostatOperatingHandler ERROR: ${e}")
                }
            } else {
                sendNotificationEvent("[HUMIDIFIER] staying off. The current humidity of ${current_humidity} exceeds the target of ${target_humidity} at outdoor temperature ${current_temperature}.")
            }
        }
    } else if (evt.value.toString() == "idle") {
    	// If the humidifier is still running, turn it off
        humidifier.off()
    }
}

def humidifierSwitchHandler(data) {
    try {
    	if (state.humidifier_on != null) {
            def now = new Date()
            def lastRunTime = (now.getTime() / 1000) - state.humidifier_on
            
            if (humidifier.currentValue("switch") == "on") {
                if (data.failsafe) {
                	// If this is the failsafe that is called periodically, don't interrupt a valid runtime. 
                    // Only shut the humidifier off if humidifier_runtime has been exceeded by a factor of 1.25 (to account for small variance in job execution)
                	if (lastRunTime <= (humidifier_runtime.toInteger() * 60 * 1.25)) {
                    	return false
                    }
                }
                // Log the humidifier run time
                def total_runtime = (state.humidifier_total_runtime == null ? 0 : state.humidifier_total_runtime.toInteger()) + ((new Date().getTime() / 1000) - state.humidifier_on)
                state.humidifier_total_runtime = total_runtime
                
                def msg = "turning OFF"
                if (data.failsafe) {
                    msg = msg + " due to failsafe"
                }
                
                sendNotificationEvent("[HUMIDIFIER] ${msg}. Total runtime today: ${(state.humidifier_total_runtime / 60)} minutes.")
                
                // Turn the humidifier switch OFF
                humidifier.off()
            }
        }
    } catch(e) {
        sendNotificationEvent("[HUMIDIFIER] humidifierSwitchHandler ERROR: ${e}")
    }
}

def humidifierRunTimeReset() {
    // Reset the humidifier run time
    state.humidifier_total_runtime = 0
}