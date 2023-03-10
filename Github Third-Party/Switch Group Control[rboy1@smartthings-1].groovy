/**
 *
 *  Turns a group of switches on/off when any of the switches in the group are turned on/off.
 *
 *  Allows you to specify if it responds to physical triggers, virtual triggers, or both.
 *
 *
 *  Copyright 2015 uncleskippy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

definition(
	name: "Switch Group Control",
	namespace: "uncleskippy",
	author: "UncleSkippy",
	description: "Turn on/off switches in a group when one of them is turned on/off.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("Switches") {
		input "switches", "capability.switch", required: true, multiple: true, title: "Switches in the group?"
		input "actionTypes", "enum", title: "What type(s) of actions will trigger the group?", required: true, multiple: true, options: ["Physical Trigger","Virtual Trigger"]
	}
}

def installed()
{
	subscribe(switches, "switch", switchHandler, [filterEvents: false])
}

def updated()
{
	unsubscribe()
	subscribe(switches, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) {
	// Check if we should process this event
	def processEvent = false
	for (t in actionTypes) {
		if (evt.physical && t == "Physical Trigger" || !evt.physical && t == "Virtual Trigger") {
			processEvent = true;
			break;
		}
	}
	if (processEvent) {
		if (evt.value == "on") {
			for (sw in switches) {
				if (sw.currentValue("switch") == "off") {
					sw.on();
			   	}
			}
		} else if (evt.value == "off") {
			for (sw in switches) {
				if (sw.currentValue("switch") == "on") {
					sw.off();
			   	}
			}
		}
	}

	state.handlingEvents = false
}
