/**
 *  Whole House Fan
 *
 *  Copyright 2014 Brian Steere
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * Version 1.0 Original from Brian Steere
 * Version 1.1 Added run time of the day so that once the house fan turns off between the no run time, it won't turn back on.
 */
definition(
    name: "Whole House Fan App",
    namespace: "jgagnon5541",
    author: "Brian Steere",
    description: "Toggle a whole house fan (switch) when: Outside is cooler than inside, Inside is above x temp, Thermostat is off",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan%402x.png"
)


preferences {
	section("Outdoor") {
		input "outTemp", "capability.temperatureMeasurement", title: "Outdoor Thermometer", required: true
	}
    
    section("Indoor") {
    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", required: true
        input "minTemp", "number", title: "Minimum Indoor Temperature"
        input "fans", "capability.switch", title: "Vent Fan", multiple: true, required: true
    }
    
    section("Thermostat") {
    	input "thermostat", "capability.thermostat", title: "Thermostat"
    }
    
    section("Windows/Doors") {
    	paragraph "[Optional] Only turn on the fan if at least one of these is open"
        input "checkContacts", "enum", title: "Check windows/doors", options: ['Yes', 'No'], required: true 
    	input "contacts", "capability.contactSensor", title: "Windows/Doors", multiple: true, required: false
    }
    section("Run Time of Day") {
    	input "timeBegin", "time", title: "Time of Day to start"
    	input "timeEnd", "time", title: "Time of Day to stop"
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
	state.fanRunning = false;
    
    subscribe(outTemp, "temperature", "checkThings");
    subscribe(inTemp, "temperature", "checkThings");
    subscribe(thermostat, "thermostatMode", "checkThings");
    subscribe(contacts, "contact", "checkThings");
}

def checkThings(evt) {
	def outsideTemp = settings.outTemp.currentTemperature
    def insideTemp = settings.inTemp.currentTemperature
    def thermostatMode = settings.thermostat.currentThermostatMode
    def somethingOpen = settings.checkContacts == 'No' || settings.contacts?.find { it.currentContact == 'open' }
    log.debug "Inside: $insideTemp, Outside: $outsideTemp, Thermostat: $thermostatMode, Something Open: $somethingOpen"
    
    def now = new Date()
    def startCheck = timeToday(timeBegin)
    def stopCheck = timeToday(timeEnd)
    
    log.debug "now: ${now}"
    log.debug "startCheck: ${startCheck}"
    log.debug "stopCheck: ${stopCheck}"
    
    def between = timeOfDayIsBetween(startCheck, stopCheck, now, location.timeZone)
    log.debug "between: ${between}"
    def shouldRun = true
    def minoutsideTemp = outsideTemp + 3
    log.debug "minoutsideTemp: ${minoutsideTemp}"
    
    if((insideTemp <= minoutsideTemp) && !state.fanRunning) {
    	log.debug "Not running due to insideTemp: ${insideTemp} <= outsideTemp ${outsideTemp}+ 3 = ${minoutsideTemp}"
    	shouldRun = false;
    }
    
    if((insideTemp <= outsideTemp) && state.fanRunning) {
    	log.debug "Not running due to insideTemp: ${insideTemp} <= outdoorTemp: ${minoutsideTemp}"
    	shouldRun = false;
    }
    
    /* if(insideTemp <= minoutsideTemp) {
    	log.debug "Not running due to insideTemp: ${insideTemp} < outdoorTemp: ${minoutsideTemp}"
    	shouldRun = false;
    } */
    
    if(insideTemp < settings.minTemp) {
    	log.debug "Not running due to insideTemp < minTemp"
    	shouldRun = false;
    }
    
    if(!somethingOpen) {
    	log.debug "Not running due to nothing open"
        shouldRun = false
    }
    if(thermostatMode != 'off' && shouldRun) {
    	log.debug "Setting thermostat to OFF"
    	thermostat.off()
    }
    
    if(between && shouldRun && !state.fanRunning) {
    	fans.on();
        state.fanRunning = true;
    } else if(!shouldRun && state.fanRunning) {
    	fans.off();
        state.fanRunning = false;
        /* if(!somethingOpen){
        	if( outsideTemp > settings.minTemp) {
        		log.debug "Setting thermostat to cool"
        		thermostat.cool()
      		} else {
        		log.debug "Setting thermostat to auto"
        		thermostat.auto()
        		}
     		} */
        
    }
}