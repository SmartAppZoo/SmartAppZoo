/**
 *  QuietCool Fan Control
 *
 *  Copyright 2017 Coding Panda
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
    name: "QuietCool Fan Control",
    namespace: "7100SW",
    author: "Coding Panda",
    description: "Automatically turn on fan when temperature reaches threshold temperature",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Turn on when temperature exceeds:") {
        input "theMercury", "capability.temperatureMeasurement", required: true, title: "Sensor?"
    	}

	section("Temperature threshold:") {
        input "threshold", "number", required: true, title: "Fahrenheit?"
    	}

	section("Turn on this fan") {
            input "theFanSwitch", "capability.switch", required: true
        }
}

def installed() {
	log.debug "Installed with settings: ${settings}";
	initialize();
}

def updated() {
	log.debug "Updated with settings: ${settings}";
	unsubscribe();
	initialize();
}

def initialize() {
    subscribe(theMercury, "temperature", temperatureChangeHandler);
}

def temperatureChangeHandler(evt) {
  
  if(evt.value > threshold)
  {
	theFanSwitch.on();  
  }
}

