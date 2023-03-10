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
    name: "Turn Off Outlet After",
    namespace: "smartthings",
    author: "Adam Zolotarev",
    description: "Turns off an outlet after it's been turned of for a period of time",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {	
	section("Monitor these outlet(s)") {
		input name: "outlet", title: "Which?", type: "capability.switch", multiple: false, required: true
	}
	section("Turn off after") {
		input "minutes", "number", title: "Minutes", required: true
	}
}

def installed() {
	subscribeToEvents()
}

def updated() {
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(outlet, "switch.on", turnOffAfterMinutes)
}

def turnOffAfterMinutes(evt) {
	def delay = minutes * 60
	runIn(delay, "scheduledTurnOff")
}

def scheduledTurnOff() {
	outlet.off()
}