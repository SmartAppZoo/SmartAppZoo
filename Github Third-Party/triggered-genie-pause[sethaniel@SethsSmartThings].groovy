/**
 *  Triggered Genie Pause
 *
 *  Copyright 2016 Seth Munroe
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
    name: "Triggered Genie Pause",
    namespace: "sethaniel",
    author: "Seth Munroe",
    description: "This will send a pause signal to a Genie DVR when the trigger switch is turned on. It will also reset the trigger.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Receiver Information") {
		input(name: "receiverIp", type: "string", title: "What's the IP address of the Master Genie?", required: true)
		input(name: "receiverPort", type: "string", title: "What port is the Master Genie using for control?", defaultValue: "8080", required: true)
		input(name: "receiverMac", type: "string", title: "What's the MAC address of the target Genie (if not master)", required: false)
	}
    
    section("Trigger Switch") {
    	input(name: "triggerSwitch", type: "capability.switch", title: "Select the trigger switch that will pause the Genie", required: true)
        input(name: "triggerReset", type: "bool", title: "Should the switch be switched back automatically so that the trigger can be used again?", required: true)
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
	subscribe(triggerSwitch, "switch.on", triggerThrown)
}

def triggerThrown(evt) {

	log.debug "sending"
    
    sendHubCommand(
		new physicalgraph.device.HubAction(
			headers: [
				HOST: "${receiverIp}:${receiverPort}"
			],
			method : "GET",
			path   : "/remote/processKey",
			query  : [
            	key: "pause",
				clientAddr: (receiverMac ?: "0").replace(":", "").toUpperCase()
			]
		)
    )
    
    log.debug "sent"

    
    if (triggerReset) {
		log.debug "resetting."
        triggerSwitch.off()
    }
}
