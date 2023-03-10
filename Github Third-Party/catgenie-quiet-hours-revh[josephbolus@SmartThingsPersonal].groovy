/**
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain a
 *  copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License  for the specific language governing permissions and limitations
 *  under the License.
 *
 *  The latest version of this file can be found at:
 *  https://github.com/statusbits/smartthings/blob/master/Pollster/Pollster.app.groovy
 *
 *  Revision History
 *  ----------------
 *
 *  2015-04-6: Version: H
 *  Stopped polling, just using the scheduling. 
 *
 */

definition(
    name: "Catgenie Quiet Hours RevH",
    namespace: "keithcroshaw",
    author: "keithcroshaw",
    description: "Checks time and power consumption of a device periodically for selected device.",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Bath.bath5-icn?displaySize=2x",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Bath.bath5-icn?displaySize=2x")

preferences {

  	section ("Disable?") {
    	input "disableLogic", "bool", title: "Disable Logic?"
  	}
    section ("Debug?") {
    	input "debugMessages", "bool", title: "Debug Messages?"
  	}
    section("Load up the triggers..") {
    	input "motion", "capability.motionSensor", title: "Which Motion?", required: false, multiple: true
		input "temp", "capability.temperatureMeasurement", title: "Temp Sensor", required: false, multiple: true
        input "hum", "capability.relativeHumidityMeasurement", title: "Humidity Sensor(s)", required: false, multiple: true
        input "lightSensor", "capability.illuminanceMeasurement", required: false, multiple: true
    }   
	section ("When this device stops drawing power") {
    	input "meter", "capability.powerMeter", multiple: false, required: true
        input "DeviceNotRunning", "number", title: "Device not running when power drops below (W)", description: "8", required: true
        input "timeBegin", "time", title: "Time of Day to start"
        input "timeEnd", "time", title: "Time of Day to stop"
 	}
    section("Turn off these switches..."){
		input "switches", "capability.switch", multiple: true
	}
}

def installed() {
	subscribe(motion, "motion.active", eventHandler)
    subscribe(meter, "power", eventHandler)
    subscribe(temp, "temperature", eventHandler)
    subscribe(hum, "humidity", eventHandler)
    subscribe(lightSensor, "illuminance", eventHandler)
    
}

def updated() {
    unschedule()
    unsubscribe()
	subscribe(motion, "motion.active", eventHandler)
    subscribe(meter, "power", eventHandler)
    subscribe(temp, "temperature", eventHandler)
    subscribe(hum, "humidity", eventHandler)
    subscribe(lightSensor, "illuminance", eventHandler)
}

def eventHandler(evt) {

	if (!disableLogic) {   
    
    	def latestPower = meter.currentValue("power")
    	//log.debug "Power: ${latestPower}W"
    
    	if (latestPower <= DeviceNotRunning){
    		state.deviceInStandby = 1
    	} else {
    		state.deviceInStandby = 0
    	}
    	//log.debug "state.deviceInStandby: ${state.deviceInStandby}"

    

	def now = new Date()
    def startCheck = timeToday(timeBegin)
    def stopCheck = timeToday(timeEnd)
    
    //log.debug "now: ${now}"
    //log.debug "startCheck: ${startCheck}"
    //log.debug "stopCheck: ${stopCheck}"
    
    def between = timeOfDayIsBetween(startCheck, stopCheck, now, location.timeZone)
    //log.debug "between: ${between}"
        
        
    if (state.deviceInStandby==1 && between){
    	switches?.off()
        log.debug "ST Turned off the ${switches}"
        
        if (debugMessages) {
        	sendNotificationEvent("ST Turned off the ${switches}")
        }
        
    } else {
    	if (state.deviceInStandby == 0) {
        	log.debug "Device was not turned off because: Not in standby"
            if (debugMessages) {
            	sendNotificationEvent("Device was not turned off because: Not in standby")
            }
        }
        if (!between) {
        	log.debug "Device was not turned off because: Not between quiet hours"
            if (debugMessages) {
        		sendNotificationEvent("Device was not turned off because: Not between quiet hours")
            }
        }
    }
  }
}