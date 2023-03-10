/**
 *  Wake On Lan
 *
 *  Copyright 2018 Mihai Floares
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License. *
 */
 
definition(
    name: "WOLan wtex_i",
    namespace: "psecto",
    author: "Mihai Floares",
    description: "Wake On Lan with Text Input for Samsung Smarthings Hub",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Set Device") {
		input "MyVButton", "capability.momentary", required: true, title: "Add button Trigger"
		input "macSet", "text", title: "Input MAC", required: true
	}
}

def installed () {
	log.debug "installed with settings: ${settings}"
	initialize()
}

def updated () {
	log:debut "updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize () {
	subscribeToCommand(MyVButton, "push", WakeOnLan)
}

def WakeOnLan(evt) {
	sendHubCommand(new physicalgraph.device.HubAction (
		"wake on lan $macSet",
		physicalgraph.device.Protocol.LAN,
		null,
		[:]
))
}

//  \o/ for to the god of wol