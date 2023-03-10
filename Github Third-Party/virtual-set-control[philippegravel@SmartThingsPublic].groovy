/**
 *  Push to Max
 *
 *  Copyright 2016 Philippe Gravel
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
    name: "Virtual Set Control",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Virtual Set Control",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png")


preferences {
	section("Switch to be set") {
		input "theSwitch", "capability.switch", title: "Switch to start?", required: true
		input "theScene", "capability.switch", title: "Switch to control Scene?", required: true
		input "theLevel", "number", title: "Number on Control Scene?", required: true
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
	subscribe(theSwitch, "switch.on", onHandler)
}

def onHandler(evt) {
	log.debug "Events: " + evt.displayName

	theScene.setLevel(theLevel)
    theSwitch
}