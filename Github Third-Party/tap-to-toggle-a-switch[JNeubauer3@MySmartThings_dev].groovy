/**
 *  tap to toggle a switch
 *
 *  Copyright 2017 Mike Wang
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
    name: "Tap to toggle a switch",
    namespace: "q13975",
    author: "Mike Wang",
    description: "Double tap a switch to toggle another switch",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

def appVersion() { "1.0.0" }
def appVerDate() { "2-9-2017" }

preferences {
	section("Double Tap this switch") {
		input name: "master", type: "capability.switch", title: "Master Switch?", required: true
	}
	section("to toggle this switch") {
		input name: "slave", type: "capability.switch", title: "Slave Switch?", required: true
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	state.nextTime = 0
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) {
	if(evt.isPhysical() && (evt.value == "on" || evt.value == "off")) {
		if(!evt.isStateChange()) {
			def eventTime = evt.date.getTime()
			if(state.nextTime < eventTime) {	// first tap
				// set time fence for second tap
				state.nextTime = eventTime + 5000
			} else {				// second tap
				toggleSwitch(slave)
				state.nextTime = 0	
			}
		} else if(state.nextTime) {
			state.nextTime = 0	
		}
	}
}

private toggleSwitch(sw) {
	if(sw) {
		if(sw.currentSwitch == "on") {
			sw.off()
		} else {
			sw.on()
		}
	}
}
