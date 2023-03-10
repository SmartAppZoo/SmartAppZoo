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
    name: "Close after time",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Close light after amount of seconds",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png")


preferences {
	section("Switch to be set") {
		input "theSwitch", "capability.switch", title: "Switch?", required: true
 	   	input "delaySeconds", "number", title:"Number of seconds?", required: true
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
    
   	def fireTime = new Date(new Date().time + (delaySeconds * 1000))
	runOnce(fireTime, turnOffAfterDelay, [overwrite: true])
}

def turnOffAfterDelay() {
	log.debug "Turn Off after delay: " + theSwitch.displayName

	theSwitch.off()
}