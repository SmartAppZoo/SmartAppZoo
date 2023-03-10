/**
 *  Thermostat state tracking
 *
 *  Copyright 2018 Richard Letts
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
    name: "Thermostat heating state tracking",
    namespace: "slashrjl",
    author: "Richard Letts",
    description: "Turn On/Off switch depending on the heating state of the thermostat. This allows for a thermostat to control a switch based on the heating state.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",  

    )

preferences {
	section("Monitor the Thermostat Operating State of:") {
		input "thermostatSensor1", "capability.thermostat"
	}
    section("Control Switch:") {
		input "switch1", "capability.switch"
	}

}

def installed() {
	subscribe(thermostatSensor1, "thermostatOperatingState", thermostatHandler)
}

def updated() {
	unsubscribe()
	subscribe(thermostatSensor1, "thermostatOperatingState", thermostatHandler)
}

def thermostatHandler(evt) {
	log.trace "thermostatOperatingState: $evt.value"

	def currentState = evt.value
 
	def mySwitch = settings.switch1

	if (currentState == "idle" ) {
    	log.debug "$currentState: turning switch off"
        switch1.off()
	}
    else if (currentState == "heating" ) {
		log.debug "$currentState: turning switch on"
        switch1.on()
	}
    else {
    	log.debug "$currentState: turning switch off"
        switch1.off()
    }
}

