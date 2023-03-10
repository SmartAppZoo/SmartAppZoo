/**
 *  Copyright 2019 Josh Johnson
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
 *  Turn Light On When Door Opens at Night
 *  When a contact sensor is opened, a switch will be turned on for a period of time if it is dark outside and the switch is not already on
 *
 *  Author: Josh Johnson
 */
definition(
    name: "Turn Light On When Door Opens at Night",
    namespace: "joshjohnson",
    author: "Josh Johnson",
    description: "When a contact sensor is opened, a switch will be turned on for a period of time if it is dark outside and the switch is not already on",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section("When it opens..."){
		input "contact1", "capability.contactSensor"
	}
	section("Turn on a switch..."){
		input "switch1", "capability.switch"
	}
    section("For how many seconds..."){
		input "secondsDelay", "number"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(contact1, "contact.open", contactOpenHandler)
    def currentValue = contact1.currentValue("contact")
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe(contact1, "contact.open", contactOpenHandler)
}

def contactOpenHandler(evt) {
	log.debug("Contact opened")

	def currentValue = switch1.currentValue("switch")
    log.debug("Switch state: $currentValue")
    
    def now = new Date()
	def sunTime = getSunriseAndSunset();
    
	log.debug "nowTime: $now"
	log.debug "riseTime: $sunTime.sunrise"
    log.debug "setTime: $sunTime.sunset"
	
    if (now > sunTime.sunset || now < sunTime.sunRise) {
    	log.debug "It's dark outside"
        
        if (currentValue == "off") {
            switch1.on()
            runIn(secondsDelay, turnOffSwitch)
        }
    }
}

def turnOffSwitch() {
	log.debug("Turning switch off")
	switch1.off()
}