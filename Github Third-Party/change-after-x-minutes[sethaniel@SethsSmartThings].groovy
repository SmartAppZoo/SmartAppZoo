/**
 *  Copyright 2015 SmartThings
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
 *  Turn It On For 5 Minutes
 *  Turn on a switch when a contact sensor opens and then turn it back off 5 minutes later.
 *
 *  Author: SmartThings
 */
definition(
    name: "Change after x minutes",
    namespace: "sethaniel",
    author: "Seth Munroe",
    description: "Uses a trigger switch to start. When the selected state happens on the trigger, a timer will start and at the end the target switch will be changed to the selected state. The target switch can have a state set at the beginning of the timer too, but it's not required.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png"
)

preferences {
	section("Timer"){
    	input "timer", "decimal", required: true, title: "Number of minutes on timer"
    }
	section("Trigger switch"){
		input(name: "triggerswitch", type: "capability.switch", required: true, title: "Select trigger switch:")
        input(name: "triggerswitchstate", type: "enum", required: true, defaultValue: "ON", title: "trigger state:", options: ["ON","OFF"])
        input(name: "triggerswitchreset", type: "enum", required: true, defaultValue: "YES", title: "reset trigger after timer:", options: ["YES","NO"])
	}
	section("Target switch:"){
		input(name: "targetswitch", type: "capability.switch", required: true, title: "Select target switch:")
        input(name: "targetswitchstart", type: "enum", required: true, defaultValue: "ON", title: "Set state when triggered:", options: ["ON","OFF","No CHANGE"])
        input(name: "targetswitchend", type: "enum", required: true, defaultValue: "OFF", title: "Set state after timer:", options: ["ON","OFF"])
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    
    if (triggerswitchstate == "ON") {
		subscribe(triggerswitch, "switch.on", triggerSwitchHandler)
    } else {
		subscribe(triggerswitch, "switch.off", triggerSwitchHandler)
    }
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    
    if (triggerswitchstate == "ON") {
		subscribe(triggerswitch, "switch.on", triggerSwitchHandler)
    } else {
		subscribe(triggerswitch, "switch.off", triggerSwitchHandler)
    }
}

def triggerSwitchHandler(evt) {
	unsubscribe(targetswitch)
    
	if (targetswitchstart == "ON") {
		targetswitch.on()
    } else if (targetswitchstart == "OFF") {
    	targetswitch.off()
    }
    
	def delayTime = 60 * timer
	runIn(delayTime, changeTarget)
    
	if (targetswitchend == "ON") {
    	subscribe(targetswitch, "switch.on", targetSwitchHandler)
    } else {
        subscribe(targetswitch, "switch.off", targetSwitchHandler)
    }
}

def targetSwitchHandler(evt) {
	unschedule()
    unsubscribe(targetswitch)
    
    if (triggerswitchreset == "YES") {
        if (triggerswitchstate == "ON") {
            triggerswitch.off()
        } else {
            triggerswitch.on()
        }
    }
}

def changeTarget() {
	unsubscribe(targetswitch)
    
	if (targetswitchend == "ON") {
		targetswitch.on()
    } else {
    	targetswitch.off()
    }
    
    if (triggerswitchreset == "YES") {
        if (triggerswitchstate == "ON") {
            triggerswitch.off()
        } else {
            triggerswitch.on()
        }
    }
}