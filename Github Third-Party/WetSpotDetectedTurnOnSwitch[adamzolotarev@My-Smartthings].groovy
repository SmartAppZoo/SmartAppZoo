/**
 *  Turn Off Lights After
 *
 *  Copyright 2015 Adam Zolotarev
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
    name: "Wet Spot - Turn Switch",
    namespace: "smartthings",
    author: "Adam Zolotarev",
    description: "Permanently turns switch on when moisture sensor detects mosture.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot@2x.png"
)

preferences {
	section("When water is sensed...") {
		input "sensor", "capability.waterSensor", title: "Where?", required: true
	}
	section("Turn on a switch") {
		input "outlet", "capability.switch", title: "Which?", required: true
	}
}

def installed() {
	subscribe(sensor, "water.wet", waterHandler)
}

def updated() {
	unsubscribe()
	subscribe(sensor, "water.wet", waterHandler)
}

def waterHandler(evt) {
	log.debug "Sensor says ${evt.value}"
	if (evt.value == "wet") {
		outlet.on()
	}
}