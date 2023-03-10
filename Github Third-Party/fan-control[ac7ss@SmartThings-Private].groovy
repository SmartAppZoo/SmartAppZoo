/**
 *  Fan Control
 *
 *  Copyright 2016 Glenn Brockett
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
 *
 *	Things I want to do:
 *		Add option for using thermostat settings for setting and if in cooling mode.
 *			Optional Themostat device, if defined, will use both setpoint and if in cooling mode.
 *
 */
definition(
    name: "Fan Control",
    namespace: "ac7ss",
    author: "Glenn Brockett",
    description: "Turn on fan when outside temperature is lower than inside.",
    category: "Green Living",
    iconUrl: "https://iconalone.com/sites/default/files/styles/frontx48x48/public/Fan.svg_0.png",
    iconX2Url: "http://downloadicons.net/sites/default/files/fan-icon-46129.png",
    iconX3Url: "http://downloadicons.net/sites/default/files/fan-icon-46129.png",
    oauth: true)


preferences {
	section("Device(s) under control:") {
    	input "ThisSwitch","capability.switch", title: "Switched Device", required: true, multiple: true
	}
    section("Temperature sensors:") {
    	input "InsideTemp", "capability.temperatureMeasurement", title: "Inside Thermometer", required: true
       	input "OutsideTemp", "capability.temperatureMeasurement", title: "Outside Thermometer", required: true
    }
    section ("Temperature settings:") {
    	input "Delta", "number", title: "Temperature Delta", required: true
    	input "MinimumTemp", "number", title: "Inside Minimum Temp", required: true
        input "MaxTemp", "number", title: "Always Run If Hotter Than", required: true
        input "Override", "bool", default: true, title: "Override off time setting on always run setting?"
    }
    section ("Time Constraints") {
    	input "TimeOff","time", title: "Turn off at:", required: false
        input "TimeOn","time", title: "Start program at:", required: true
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
    if ("on"==ThisSwitch.currentswitch){
    	state.fanRunning = true;
    }
	state.runtime = true
	if (TimeOff) {
		schedule(TimeOff, "stopCallback")
    }
   	schedule(TimeOn, "startCallback")
    
	subscribe(InsideTemp, "temperature", myHandler);
    subscribe(OutsideTemp, "temperature", myHandler);
    subscribe(ThisSwitch, "currentswitch", myHandler);
}

// TODO: implement event handlers

def stopCallback() {
	state.runtime = false;
    state.fanRunning = false;
    ThisSwitch.off();
}

def startCallback() {
	state.runtime = true;
    state.fanRunning = false;
}

def myHandler(evt) {
	def Inside=settings.InsideTemp.currentValue('temperature')
    def Outside=settings.OutsideTemp.currentValue('temperature')
    log.debug "Variables: $Inside $Outside $Delta $MinimumTemp $state.fanRunning"
    if (state.runtime) {
	    if(Inside > MinimumTemp && ((Inside > (Outside+Delta))||(Inside > MaxTemp)) && !state.fanRunning ) {
    		log.debug "Turn on"
        	ThisSwitch.on();
		    state.fanRunning=true;    
	    } else if ((Inside < MinimumTemp || (Inside < Outside && Inside < MaxTemp)) && state.fanRunning) {
    		log.debug "Turn off"
    	    ThisSwitch.off();
        	state.fanRunning = false;
    	}
    }else if (!state.runtime) {
    	if(Inside > MaxTemp) {
    		log.debug "Offtime Turn on"
        	ThisSwitch.on();
		    state.fanRunning=true;            	
        }else if (Inside < MaxTemp){
	    	log.debug "Offtime Turn off"
    		ThisSwitch.off();
        	state.fanRunning = false;
        }
    }
}
