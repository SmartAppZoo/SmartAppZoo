/**
 *  Color Coded Alerts
 *
 *  Copyright 2020 Matt
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
	name: "Color Coded Alerts",
	namespace: "smartthings",
	author: "Matt",
	description: "Monitor selected sensors/switches and notify of any changes or alerts using specific color changes in Philips Hue bulbs.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/smart-light-timer.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/smart-light-timer@2x.png"
)

preferences {
	section("Which sensors monitor the washer and dryer?") {
		input "laundry_devices", "capability.powerMeter", required: true, multiple: true, title: "Choose the washer and dryer power meters"
	}
    section("Choose the virtual switches to turn on/off when voltage changes are detected in the laundry room.") {
    	input "virtual_laundry_devices", "capability.switch", required: true, multiple: true, title: "Choose the virtual washer and dryer switches"
    }
    section("Configure how the garage door should be monitored.") {
        input "garage_sensor", "capability.sensor", required: true, multiple: false, title: "Choose the garage door sensor to monitor"
        input "garage_door_check_time", "time", required: true, title: "Choose a time to check to see if the garage door is open each day"
    }
    
	section("Which lights should change color?") {
		input "lights", "capability.colorControl", required: true, multiple: true, title: "Choose the color changing lights"
	}
}

def installed() {
	log.debug laundry_devices.capabilities
	log.debug ("Current switch status: ${lights.currentValue("switch")}")
	log.debug ("Current level: ${lights.currentValue("level")}")
	log.debug ("Current color saturation: ${lights.currentValue("saturation")}")
	log.debug ("Current hue: ${lights.currentValue("hue")}")
	log.debug ("Current color temperature: ${lights.currentValue("colorTemperature")}")
 	
    initColorCodedAlerts()
}

def updated() {
	unsubscribe()
    unschedule(garageDoorCheckHandler)
    initColorCodedAlerts()
}

def initColorCodedAlerts() {
	// Initialize monitoring for washer and dryer
	state.laundry_devices = [:]
	state.laundry_devices["dryer power"] = ["last_run": null, "started": null, "stopped": null]
    state.laundry_devices["washer power"] = ["last_run": null, "started": null, "stopped": null]
    virtual_laundry_devices.each { object ->
    	object.off()
    }
	subscribe(laundry_devices, "power", powerChangeHandler)
    // Initialize monitoring for garage door
    schedule(garage_door_check_time, garageDoorCheckHandler)
}

def powerChangeHandler (evt) {
	if (evt.value.toFloat() > 5) {
    	if (state.laundry_devices[evt.device.getLabel().toLowerCase()]["started"] == null) {
			sendNotificationEvent("[EVENT] ALERT: The ${evt.device.getLabel().toLowerCase().replaceAll(" power", "")} has started.")
 			virtual_laundry_devices.each { object ->
            	if ("${object.toString().toLowerCase()} power" == evt.device.getLabel().toLowerCase()) {
                	object.on()
                }
            }
        	state.laundry_devices[evt.device.getLabel().toLowerCase()]["started"] = new Date().getTime() / 1000
            state.laundry_devices[evt.device.getLabel().toLowerCase()]["last_run"] = new Date().getTime() / 1000
        }
        state.laundry_devices[evt.device.getLabel().toLowerCase()]["stopped"] = null
	} else {
		if (state.laundry_devices[evt.device.getLabel().toLowerCase()]["started"] != null) {
            // The device is outputting < 5 watts. Has it really finished?
            if (state.laundry_devices[evt.device.getLabel().toLowerCase()]["stopped"] != null) {
                def current_date = new Date().getTime() / 1000
                // Check to make sure at least 45 seconds have passed (to avoid fluke fluctuations in reported voltage)
                if ((current_date - state.laundry_devices[evt.device.getLabel().toLowerCase()]["stopped"]) >= 45) {
                    virtual_laundry_devices.each { object ->
                        if ("${object.toString().toLowerCase()} power" == evt.device.getLabel().toLowerCase()) {
                            object.off()
                        }
                    }					
                    // If the stopped time is within 15 minutes of the start time, there may be a problem or the machine may have been stopped manually.
                    if ((current_date - state.laundry_devices[evt.device.getLabel().toLowerCase()]["started"]) <= 900) {
                        sendNotificationEvent("[EVENT] ALERT: The ${evt.device.getLabel().toLowerCase().replaceAll(" power", "")} has stopped sooner than expected.")
                        sendPush("The ${evt.device.getLabel().toLowerCase().replaceAll(" power", "")} has stopped sooner than expected. Check to make sure there's not a problem.")
                    } else {
                        sendNotificationEvent("[EVENT] ALERT: The ${evt.device.getLabel().toLowerCase().replaceAll(" power", "")} has finished.")
                        sendPush("The ${evt.device.getLabel().toLowerCase().replaceAll(" power", "")} has finished!")
                        def lights_on = false
                        lights.any { object ->
                            if (object.currentValue("switch") == "on") {
                                lights_on = true
                                return true
                            }
                        }
                        if (lights_on) {
                            runIn(5, initLightSequence, [overwrite: false, data: ["machine": evt.device.getLabel().toLowerCase()]])
                            runIn(8, backToNormal, [overwrite: false])
                        }
                    }
                    // Reset the stopped and started dates to NULL to prepare for the next event
                    state.laundry_devices[evt.device.getLabel().toLowerCase()]["started"] = null
                    state.laundry_devices[evt.device.getLabel().toLowerCase()]["stopped"] = null
                    // If the washer has stopped, run an event in 30 minutes to see if the dryer has started to avoid loads that may have been forgotten
                    if (evt.device.getLabel().toLowerCase() == "washer power") {
                        runIn(1800, dryerCheckHandler)
                    }
                }
            } else {
                state.laundry_devices[evt.device.getLabel().toLowerCase()]["stopped"] = new Date().getTime() / 1000
                log.trace ("Target voltage value has been met. Setting 1 minute timer (${state.laundry_devices[evt.device.getLabel().toLowerCase()]["stopped"]})")
            }
		}
	}
}

def dryerCheckHandler() {
    // Check to see whether the dryer has been run in the time since the washer last stopped
    def current_date = new Date().getTime() / 1000
    if (current_date - state.laundry_devices["dryer power"]["last_run"] >= 1800) {
        sendNotificationEvent("[EVENT] ALERT: The dryer hasn't started yet. Was a load of clothes forgotten in the washer?")
        sendPush("The dryer hasn't started yet. Was a load of clothes forgotten in the washer?")
        def lights_on = false
        lights.any { object ->
            if (object.currentValue("switch") == "on") {
                lights_on = true
                return true
            }
        }
        if (lights_on) {
            runIn(5, initLightSequence, [overwrite: false, data: ["machine": "washer power"]])
            runIn(8, backToNormal, [overwrite: false])
        }
    }
}

def garageDoorCheckHandler() {
    // Check the garage door sensor to see if it's open
    def door_status = garage_sensor.currentValue("contact").toString()
    if (door_status == "open") {
        // Send an alert and flash the lights (if they're on) if the door is still open at the garage_door_check_time
        sendNotificationEvent("[EVENT] ALERT: The garage door is open.")
        sendPush("The garage door is open!")
        def lights_on = false
        lights.any { object ->
            if (object.currentValue("switch") == "on") {
                lights_on = true
                return true
            }
        }
        if (lights_on) {
            runIn(5, initLightSequence, [overwrite: false, data: ["machine": "garage door"]])
            runIn(8, backToNormal, [overwrite: false])
            // If the lights are on, repeat this routine every 5 minutes until the garage door is closed
            runIn(300, garageDoorCheckHandler)
        }
    }
}

def initLightSequence(data) {
    def newValue = [:]
    switch(data["machine"]) {
        case "dryer power":
            // The color code for the dryer finishing is GREEN
		    newValue = [hue: 37, saturation: 100, level: 100, temperature: 6500]
            break
        case "washer power":
            // The color code for the washer finishing is BLUE
		    newValue = [hue: 68, saturation: 100, level: 100, temperature: 6500]
            break
        case "garage door":
            // The color code for the garage door left open is RED
		    newValue = [hue: 99, saturation: 100, level: 100, temperature: 6500]
            break
        default:
        	return
    }
	lights.setColor(newValue)
}

// Return the lights to the default hue/temperature
def backToNormal() {
	def newValue = [hue: 13, saturation: 55, level: 100, temperature: 2732]
	lights.setColor(newValue)
}