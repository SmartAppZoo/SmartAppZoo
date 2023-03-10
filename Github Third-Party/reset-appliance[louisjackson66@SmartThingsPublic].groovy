/**
 *  Reset Appliance 
 *
 *  Copyright 2016 Louis Jackson
 *
 *  Version 1.0.2   29 Jan 2016
 *
 *	Version History
 *
 *	1.0.2   31 Jan 2016		Added version number to the bottom of the input screen
 *	1.0.1	29 Jan 2016		Allows for multiple Reoccurring time options
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
    name: "Reset Appliance",
    namespace: "lojack66",
    author: "Louis Jackson",
    description: "At a specific time, turn a device/appliance OFF and then back ON after a given number of seconds.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png")

preferences {
    page(name: "page1", uninstall: true, install: true)
}

def page1() {
    dynamicPage(name: "page1") {
    	section("Reset Thing(s) (Off/On)") 
        {
    		input "switch1", "capability.switch", title: "Using switch(s)", multiple: true, required: true
        
        	input "bReoccurring", "bool", title: "Reoccurring Reset?", required: false, defaultValue:false, submitOnChange: true
            if(!bReoccurring) { input "time1", "time", title: "At this time of day", required: false }
            else
            {
                if(!b30Min && !b1Hr   && !b3Hr) input "b10Min", "bool", title: "        - Every 10 mins", required: false, defaultValue:false, submitOnChange: true
            	if(!b10Min && !b1Hr   && !b3Hr) input "b30Min", "bool", title: "        - Every 30 mins", required: false, defaultValue:false, submitOnChange: true
                if(!b10Min && !b30Min && !b3Hr) input "b1Hr",   "bool", title: "        - Every 1 hr",    required: false, defaultValue:false, submitOnChange: true
                if(!b10Min && !b30Min && !b1Hr) input "b3Hr",   "bool", title: "        - Every 3 hrs",   required: false, defaultValue:false,  submitOnChange: true
            }

        	input "seconds1", "number", title: "Turn on after (default 30) seconds", defaultValue:30, required: false
        }
        
		section("Advanced Options") 
        {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
    	}
        
        section ("Version 1.0.2") {}
    }
}

def installed() 
{
	log.trace "(0A) ${app.label} - installed() - settings: ${settings}"
	initialize()
}

def updated() 
{
	log.info "(0B) ${app.label} - updated()"
    unschedule()
	initialize()
}

def initialize() 
{
    if (bReoccurring) 
    {
    	log.info "(0C) ${app.label} - initialize() - Run Reoccurring - Can schedule? ${canSchedule()}"
        
        if (b10Min) runEvery15Minutes(handlerMethod) 
        else if (b30Min) runEvery30Minutes(handlerMethod)
        else if (b1Hr) runEvery1Hour(handlerMethod)
        else runEvery3Hours(handlerMethod)
        
        handlerMethod()                   //runs this SmartApp after inital setup
    } else
    {
    	log.info "(0D) ${app.label} - initialize() - Run as scheduled: ${time1} - Can schedule? ${canSchedule()}"
        schedule(time1, handlerMethod)
    }    
}

def handlerMethod() {
	log.trace "(0E) - ${app.label} - ${settings}"
    
    for (SwitchDeviceOff in switch1) 
    {
        log.info "(0F) - Turning OFF ${SwitchDeviceOff.label}"
		SwitchDeviceOff.off()
    }
    
    runIn(seconds1, turnOnSwitch)
}

def turnOnSwitch() {
	def strMsg = ""
    
    for (SwitchDeviceOn in switch1) 
    {
        log.info "(10) - Turning ON ${SwitchDeviceOn.label}"
		SwitchDeviceOn.on()
        strMsg += "[${SwitchDeviceOn.label}]"
    }
    
     sendNotificationEvent("${app.label} completed reset of ${strMsg}")
     log.trace "(11) - ${app.label} completed reset of ${strMsg}"
}
