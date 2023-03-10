/**
 *  Copyright 2016 Ryan Haack
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
 *  Turn It On When It's Active
 *
 *  Author: Ryan Haack
 */
definition(
    name: "Turn It On When It's Active",
    namespace: "haackr",
    author: "Ryan Haack",
    description: "Turn a switch on when an accelerometer is active.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png"
)

preferences {
	section("When the sensor is active..."){
		input "accelerationSensor", "capability.accelerationSensor", title: "Where?"
	}
	section("Turn on a light..."){
		input "switches", "capability.switch", multiple: true
	}
    section("Turn it back off after (optional)..."){
    	input "offDelay", "number", title: "Minutes", required: false
    }
}


def installed()
{
	subscribe(accelerationSensor, "acceleration.active", accelerationActiveHandler)
}

def updated()
{
	unsubscribe()
	subscribe(accelerationSensor, "acceleration.active", accelerationActiveHandler)
}

def accelerationActiveHandler(evt) {
	log.debug "$evt.value: $evt, $settings"
	log.trace "Turning on switches: $switches"
	switches.on()
    if(offDelay){
    	runIn(offDelay*60, "turnSwitchesOff")
    }
}

def turnSwitchesOff(){
	log.trace "Turning off switches: $switches"
    switches.off()
}