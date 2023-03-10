/**
 *  Copyright 2016 Chad Lee
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
 *	AsusWRT Wifi Presence
 *
 *	Author: Chad Lee, Based on work from Stuart Buchanan, Based on original work by midyear66 with thanks
 *
 *	2016-07-13: Initial Release
 */
definition(
	name: "AsusWRT Wifi Presence",
	namespace: "chadly",
	author: "Chad Lee",
	description: "Triggers Wifi Presence Status when HTTP GET Request is recieved",
	category: "My Apps",
	iconUrl: "http://icons.iconarchive.com/icons/icons8/windows-8/256/Network-Wifi-icon.png",
	iconX2Url: "http://icons.iconarchive.com/icons/icons8/windows-8/256/Network-Wifi-icon.png"
)

preferences {
	section(title: "Select Devices") {
		input "presence", "capability.presenceSensor", title: "Select Virtual Presence Sensor", required: true, multiple:false
		input "timeout", "number", title:"Number of Minutes WIFI says Away Before Actually Marked Away", defaultValue:1
	}
}

def installed() {
	createAccessToken()
	getToken()
}

def updated() {
}

mappings {
	path("/home") {
		action: [
			POST: "updatePresent"
		]
	}
	path("/away") {
		action: [
			POST:"updateNotPresentAfterTime"
		]
	}
}

def updatePresent() {
	if (presence.currentPresence == "present") {
		//cancel the pending depart
		log.debug "Arrived; sensor already present, unscheduling pending depart"
		unschedule();
	} else {
		log.debug "Arrived; Marking sensor present"
		presence.arrived();
	}
}

def updateNotPresentAfterTime() {
	if (timeout < 1) {
		updateNotPresent();
	} else {
		runIn(timeout * 60, updateNotPresent);
	}
}

def updateNotPresent() {
	log.debug "departed"
	presence.departed();
}

def getToken() {
	if (!state.accessToken) {
		getAccessToken()
		log.debug "Creating new Access Token: $state.accessToken"
	}
}