/**
 *  Presence Machine 2
 *
 *  Copyright 2015 Keith Croshaw
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
    name: "Presence Machine Rev B",
    namespace: "keithcroshaw",
    author: "Keith Croshaw",
    description: "Presence Machine Rev B",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.presence.tile.presence-default?displaySize=2x",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.presence.tile.presence-default?displaySize=2x",
    iconX3Url: "https://graph.api.smartthings.com/api/devices/icons/st.presence.tile.presence-default?displaySize=2x")


preferences {
	section("Select Person1 Switchs to Monitor"){
		input "person1Precense", "capability.switch", multiple: false, required: true
	}
    section("Select Person2 Switchs to Monitor"){
		input "person2Precense", "capability.switch", multiple: false, required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(person1Precense, "switch.On", person1onHandler)
    subscribe(person1Precense, "switch.Off", person1offHandler)
    subscribe(person2Precense, "switch.On", person1onHandler)
    subscribe(person2Precense, "switch.Off", person1offHandler)
    state.peopleHome = 0
    if (person1Precense=="off" && person2Precense=="off") {
    	state.peopleHome = 0
        setLocationMode("Away")
    }
    if (person1Precense=="on" && person2Precense=="off") {
    	state.peopleHome = 1
        setLocationMode("Home")
    }
    if (person1Precense=="off" && person2Precense=="on") {
    	state.peopleHome = 1
        setLocationMode("Home")
    }
    if (person1Precense=="on" && person2Precense=="on") {
    	state.peopleHome = 2
        setLocationMode("Home")
    }
    log.debug "int is: ${state.peopleHome}"
}

def person1onHandler(evt) {
	state.peopleHome = state.peopleHome + 1
    
    if (state.peopleHome > 0) {
    	setLocationMode("Away")
    } else {
    	setLocationMode("Home")
    }
    log.debug "int is: ${state.peopleHome}"
}

def person1offHandler(evt) {
	state.peopleHome = state.peopleHome - 1
    
    if (state.peopleHome > 0) {
    	setLocationMode("Away")
    } else {
    	setLocationMode("Home")
    }
    log.debug "int is: ${state.peopleHome}"
}

def person2onHandler(evt) {
	state.peopleHome = state.peopleHome + 1
    
    if (state.peopleHome > 0) {
    	setLocationMode("Away")
    } else {
    	setLocationMode("Home")
    }
    log.debug "int is: ${state.peopleHome}"
}

def person2offHandler(evt) {
	state.peopleHome = state.peopleHome - 1
    
    if (state.peopleHome > 0) {
    	setLocationMode("Away")
    } else {
    	setLocationMode("Home")
    }
    log.debug "int is: ${state.peopleHome}"
}