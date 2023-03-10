/**
 *  Run circulate in HVAC
 *
 *  Copyright 2014 Bob Sanford
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
    name: "smartapp-circulate-hvac",
    namespace: "midyear66",
    author: "Bob Sanford",
    description: "Run circulate every X minutes if AC or heat has not been on",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")


preferences {
	section("Title") {
		paragraph "Run circulate in HVAC"
	}
	section("About") {
        	paragraph "Run circulate every X minutes if AC or heat has not been on"
            paragraph "Additional setpoint can be used to determine minimum run temperature."
        	paragraph "Version 1.1\nCopyright (c) 2014 ssetco.com"
    	}
    	section("Thermostat") {
        	input "thermostat", "capability.thermostat", title:"Select thermostat to be controlled"
        	input "interval", "number", title:"Set time between circulation cycles (in minutes)", defaultValue:30
        	input "length", "number", title:"Set of length of circulation cycle (in minutes)", defaultValue:5
		}
        section("Choose a temperature sensor... "){
			input "sensor", "capability.temperatureMeasurement", title: "Temperature Sensor used to establish minimum run temperature"
		}
		section("Operation") {
			input "runTemp", "number", title:"Choose a temperature to set the minimum run temperature.", defaultValue:70
			input "onoff", "capability.switch", title:"Select switch to control operation.  Typically a virtual switch created in the IDE" 
        }
}

def installed() {
	DEBUG("Installed with settings: ${settings}")
    initialize()
}

def updated() {
	DEBUG("Updated with settings: ${settings}")
	unsubscribe()
	unschedule()
   	initialize()

}

def onHandler(evt) {
	DEBUG(evt.value)
	LOG("Running Switch On Event")
    scheduler()
}

def offHandler(evt) {
	DEBUG(evt.value)
	LOG("Running Switch Off Event")
    unschedule()
    thermostat.fanAuto()
}

def scheduler(){
	DEBUG ("scheduler()")
    thermostat.fanAuto()
	def interval = settings.interval.toInteger() * 60
	def length = settings.length.toInteger() * 60
	DEBUG("Interval in seconds: ${interval}, Length in seconds: ${length}")
	runIn(interval, start_circulate)
	runIn(interval+length, scheduler)
}
        
def start_circulate(){
	DEBUG("start_circulate()")
	if (sensor.currentValue("temperature") >= runtemp)
	{	DEBUG ("into start_circulate() if statement")
		thermostat.fanOn()
   	}
}

def initialize() {
	DEBUG("initialize()")
    subscribe(onoff, "switch.on", onHandler)
    subscribe(onoff, "switch.off", offHandler)
    subscribe(device, "thermostatOperatingState", eventHandler)
    DEBUG ("running_state: ${thermostat.currentValue("thermostatOperatingState")}")
    DEBUG ("On/Off Switch: ${onoff.currentswitch}")
	if(thermostat.currentValue("thermostatOperatingState") == "idle" && onoff.currentSwitch == "on"){
		scheduler()
    }
}
    
// TODO: implement event handlers

def eventHandler(evt){
	DEBUG("eventHandler: ${evt.value}: ${evt}, ${settings}")
	if(evt.value == "idle"){
    	LOG("idle - running scheduler()")
		scheduler()
	}
	if(evt.value == "heating"|| evt.value == "cooling"){
		LOG("not idle - running unschedule()")
    		unschedule()
    		thermostat.fanAuto()
   }
}

private def LOG(message){
	log.info message
}

private def DEBUG(message){
	//log.debug message
}
