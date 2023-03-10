/**
 *  Switch Off After Delay
 *
 *  Copyright 2015 Doug Dale
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
    name: "Switch Off After Delay",
    namespace: "dougdale",
    author: "Doug Dale",
    description: "Simply turns a switch off X minutes after it is turned on.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section ("Turn off a switch...") {
		input "switch1", "capability.switch"
	}
    section ("After this long...") {
    	input "delay", "number", title: "Delay (minutes)"
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
	// Make sure the delay is at least 1 minute.
    if (delay < 1) delay = 1;
    
    // Subscribe to the handler to detect when the switch is turned on.
    subscribe(switch1, "switch.on", switchOnHandler);
}

def switchOnHandler(evt)
{
	// When the switch is turned on, schedule the turn off event.
	runIn(60*delay, offHandler);
}

def offHandler()
{
	switch1.off();
}
