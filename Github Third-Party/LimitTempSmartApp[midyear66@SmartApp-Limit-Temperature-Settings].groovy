/**
 *  Monitor Max/Min Temperature Setting
 *
 *  Copyright 2014 Bob Sanford
 * 
 *  Visit Home Page for more information:
 *  http://goo.gl/i5yzuU
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
    name: "Monitor Max Heat/Min Cool Thermostat Settings",
    namespace: "midyear66",
    author: "Bob Sanford",
    description: "SmartApp to regulate max/min temperature setting of any connected thermostat",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("About") {
        paragraph "This SmartApp is a process that monitors the temperature setting " +
            "for the selected thermostat.  If a user attempts to raise the temperature" +
            "above or below a selected value, the SmartApp will adjust " +
            "the setting to a designated Max/Min value after a given time." +  
            "This replaces telling my daughter; " +
            "Remember, a house does not warm up any faster if you set the temperature really high ;)!"
        paragraph "Version 1.0\nCopyright (c) 2014 ssetco.com"
    }
    
	section("Thermostat") {
		// TODO: put inputs here
        input "device", "capability.thermostat", title:"Select thermostat to be monitored", multiple:false, required:false
        input "nor_cool", "number", title:"Set normal cooling value", defaultValue:74
	input "min_cool", "number", title:"Set minimum cooling value", defaultValue:72
	input "nor_heat", "number", title:"Set normal heating value", defaultValue:70
        input "max_heat", "number", title:"Set maximum heating value", defaultValue:72
        input "length", "number", title:"How long to allow HVAC to reach Max Temp (in minutes)", defaultValue:10
	}
}

def installed() {
	DEBUG("Installed with settings: ${settings}")

	initialize()
}

def updated() {
	DEBUG("Updated with settings: ${settings}")
	
	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
	def heatingSetpoint = settings.device.latestValue("heatingSetpoint")
	def coolingSetpoint = settings.device.latestValue("coolingSetpoint")
    
	DEBUG("Current Heating Set Point: ${heatingSetpoint}")
	DEBUG("Current Cooling Set Point: ${coolingSetpoint}")
	DEBUG("Heating Max Point: ${max_heat}")
	DEBUG("Cooling Min Point: ${min_cool}")
	DEBUG("Heating Normal Point: ${nor_heat}")
	DEBUG("Cooling Normal Point: ${nor_cool}")
	DEBUG("Length of Allowance: ${length}")

	subscribe(settings.device, "heatingSetpoint", heatingEventHandler)
	subscribe(settings.device, "coolingSetpoint", coolingEventHandler)
}

// TODO: implement event handlers

def coolingEventHandler(evt){
	DEBUG("coolingEventHandler: ${evt.value}: ${evt}, ${settings}")
	def minutes = settings.length.toInteger()
	def seconds = minutes*60
	if(evt.value.toInteger() <= max_cool.toInteger()){
		runIn(seconds, raiseAC)
    }
}

def heatingEventHandler(evt){
	DEBUG("heatingEventHandler: ${evt.value}: ${evt}, ${settings}")
	def minutes = settings.length.toInteger()
	def seconds = minutes*60
	if(evt.value.toInteger() >= max_heat.toInteger()){
		runIn(seconds, lowerHeat)
    }
}

def lowerHeat(){
	DEBUG("Changing heatingSetpoint to ${nor_heat}")
        settings.device.setHeatingSetpoint(nor_heat)
}

def raiseAC(){
    	DEBUG("Changing coolingSetpoint to ${nor_cool}")
        settings.device.setCoolingSetpoint(nor_cool)
}

private def DEBUG(message) {
//	log.debug message
}
