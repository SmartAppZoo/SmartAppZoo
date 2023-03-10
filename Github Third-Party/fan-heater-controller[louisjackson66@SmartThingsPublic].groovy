/**
 *  Fan & Heater Controller
 *
 *  Copyright 2016 Louis Jackson
 *
 *  Version 1.0.1   31 Jan 2016
 *
 *	Version History
 *
 *	1.0.1   31 Jan 2016		Added version number to the bottom of the input screen
 *	1.0.0	28 Jan 2016		Added to GitHub
 *	1.0.0	27 Jan 2016		Creation
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
    name: "Fan & Heater Controller",
    namespace: "lojack66",
    author: "Louis Jackson",
    description: "Turns Fans & Heaters (Electric Blankets) ON\\OFF based on Temperature",
    category: "My Apps",
    iconUrl:   "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png")


preferences {
    page(name: "page1", uninstall: true, install: true)
}

def page1() {
    dynamicPage(name: "page1") {
    	section("Set Fan or Heater controls:") {
            input "theThermostat", "capability.temperatureMeasurement", title: "Thing with thermostat:", multiple: false,   required: true
    		input "switches", "capability.switch", title: "Switch to control...", multiple: true, required: true
            
            input "bFan", "bool", title: "Is this a Fan or AC?", required: false, defaultValue:false, submitOnChange: true
			if(bFan) 
            { 
            	input "maxThreshold", "number", title: "On if above... (default 77°)", defaultValue:77,  required: false
            	input "minThreshold", "number", title: "Off if below... (default 74°)", defaultValue:74,   required: false
            } else
            {
				input "maxThreshold", "number", title: "Off if above... (default 77°)", defaultValue:77,  required: false
				input "minThreshold", "number", title: "On if below... (default 74°)", defaultValue:74,   required: false
            }
    	}
        
        section("Advanced Options") 
        {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
    	}
        
        section ("Version 1.0.1a") {}
    }
}


def installed() 
{
	log.info "(0A) ${app.label} - installed() - settings: ${settings}"
	initialize()
}

def updated() 
{
	log.trace "(0B) ${app.label} - updated()"
    unsubscribe()
    initialize()
}

def initialize() 
{
	log.trace "(0C) ${app.label} - initialize()"
    subscribe(theThermostat, "temperature", doTempCheck)
}

def doTempCheck(evt) 
{
	def strMsg = ""
	log.info "(0D) ${app.label} - ${theThermostat.label} is at: ${evt.doubleValue}° - ${settings}"

    switches.each 
    {
    	log.trace "(0E) ${app.label} - Checking ${it.label} - Before state:${it.latestState("switch").value}"
        
        if (bFan)
        {
        	if ( settings.maxThreshold.toInteger() != null && evt.doubleValue >= settings.maxThreshold.toInteger() ) {strMsg = "ON"}
			if ( settings.minThreshold.toInteger() != null && evt.doubleValue <= settings.minThreshold.toInteger() ) {strMsg = "OFF"}
        } else
		{
        	if ( settings.maxThreshold.toInteger() != null && evt.doubleValue >= settings.maxThreshold.toInteger() ) {strMsg = "OFF"}
			if ( settings.minThreshold.toInteger() != null && evt.doubleValue <= settings.minThreshold.toInteger() ) {strMsg = "ON"}
        }
        
        // Keeps from turning on or off a switch unnessesarily
        if (((it.latestState("switch").value in ["on", "ON"]) && strMsg == "OFF") ||
            ((it.latestState("switch").value in ["off", "OFF"]) && strMsg == "ON")
            )
        {
        	strMsg == "ON" ? it.on() : it.off()
			log.trace "(0F) ${app.label} turned ${it.label} ${strMsg} because ${theThermostat.label} is at ${evt.doubleValue}° - ${bFan}"
			sendNotificationEvent("${app.label} turned ${it.label} ${strMsg} because ${theThermostat.label} is at ${evt.doubleValue}°")
        }
        //The after state has to be checked manually.  This code runs faster than the lights will be adjusted.
    }
}