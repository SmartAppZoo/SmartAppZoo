/**
 *  Lights On
 *
 *  Copyright 2015 Michael Moore
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
    name: "Lights On",
    namespace: "shikkie",
    author: "Michael Moore",
    description: "Turn lights on automatically through sensors and motion.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Trigger lights when") {
		input "MotionSensor", "capability.motionSensor", title: "Motion Detected", multiple: true, required: false
        input "OpenSensor", "capability.contactSensor", title: "Contact Opens", multiple: true, required: false
        input "LightSensor", "capability.illuminanceMeasurement", title: "Only When Dark", required: false
	}
    section("Lights to turn on")
    {
    	input "Lights", "capability.switch", title: "Which?", required: true, multiple: true
    }
    section("Turning Off")
    {
    	input "OffDelayMinutes", "number", title: "Turn off after x minutes", require: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.j
    state.LightsOn = false;
    subscribe(OpenSensor, "contact", triggerEvent);
    subscribe(MotionSensor, "motion", triggerEvent);
}

// TODO: implement event handlers

def triggerEvent(evt)
{
	log.debug "triggerEvent $evt.name - $evt.value";
    log.debug "state.LightsOn = $state.LightsOn";
    
    def delay = 60
    //for physical use parameters
    if(evt.isPhysical())
    	delay = 60 * OffDelayMinutes
        
    def action = ""
    
    if(evt.name == "motion")
    {
    	if(evt.value == "active")
        	action = "on"
        else
        	action = "off"
    }
    else if(evt.name == "contact")
    {
    	if(evt.value == "open")
        	action = "on"
        else
        	action = "off"
    }
   	
    if(action == "on")
    {
    	if(state.LightsOn)
    		unschedule(); //cancel pending off events if lights were already on
       else  
      // if(!state.LightsOn)
        {
        	Lights?.on();
            state.LightsOn = true;
        }
    }
    else
    {
    	if(state.LightsOn)
        {
        	log.debug "scheduling off callback in $delay sec";
        	runIn(delay, "lightsOffCallback");
        }
    }
    
    
}

def lightsOffCallback()
{
	log.debug "lightsOffCallback running";
    //TODO make sure all selected contact and motion sensors are clear before actually going lights off.
    
    
    /*
    def onSwitches = currSwitches.findAll { switchVal ->
        switchVal == "on" ? true : false
    }
    (*/
    
    def openSensors = OpenSensor.findAll { s -> s.contactState.value == "open" ? true : false }
    
    def activeMotion = MotionSensor.findAll { m -> m.motionState.value == "active" ? true : false }
    
    log.debug "active sensors ; motion = ${openSensors.size()} ; ${activeMotion.size()}";
    
    if(openSensors.size() == 0 && activeMotion.size() == 0)
    {
        Lights?.off();
        state.LightsOn = false;
    }
}
