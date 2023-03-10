/**
 *  Harmony Stop Only Active Activity
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
    name: "Harmony Stop Only Active Activity",
    namespace: "sethaniel",
    author: "Seth Munroe",
    description: "When this SmartApp is triggered, it will go through the chosen switches (these should be Harmony Activities). For each Activity, it will refresh it&#39;s state and then if it is on, it will turn it off. This ensures that Off is only called if an Activity is On. It will also return it&#39;s trigger switch to the ON state so that it can be turned off again.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/harmony.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/harmony%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Trigger switch"){
    	paragraph "The values set here relate to the switch that will be watched to trigger shutting off the active Harmony activity."
        
    	input(name: "triggerSwitch", type: "capability.switch", required: true, title: "Select trigger switch:")
        paragraph "This is usually a Simulated Switch."
        
        input(name: "triggerSwitchState", type: "enum", required: true, defaultValue: "OFF", title: "trigger state:", options: ["OFF", "ON"])
        paragraph "Select [OFF] if you want the active Harmony activity to be shut off when the trigger switch is turned off."
        
        input(name: "triggerSwitchReset", type: "enum", required: true, defaultValue: "YES", title: "reset trigger after timer:", options: ["YES","NO"])
        paragraph "Select [YES] if you want the trigger switch to be reset itself so that it can be used trigger again."
	}
    section("Harmony Activities"){
    	input(name: "harmonyActivities", type: "capability.switch", required: true, title: "Select only Harmony Activitis (other than All Off):", multiple:true)
        paragraph "Select only switches that are Harmony Activities. Do not select a Harmony Activity that shuts off all activities."
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
    subscribe(triggerSwitch, "switch.${triggerSwitchState.toLowerCase()}", triggerSwitchHandler)
    log.debug "subscribed to ${triggerSwitch.name}.${triggerSwitchState.toLowerCase()}"
}

def triggerSwitchHandler(evt) {
    log.debug "event handler: ${evt.displayName}.${evt.description}"

    harmonyActivities.refresh()
    
    log.debug "activities refreshed"
    
    runIn(1,checkAndTurnOff)
}

def checkAndTurnOff(){
    harmonyActivities.each { oneActivity ->
    	log.debug "${oneActivity.label}.switch = [${oneActivity.currentValue("switch")}]."
    	if (oneActivity.currentValue("switch") == "on"){
            log.debug "Turning off the ${oneActivity.label} activity."
            oneActivity.off()
        } else {
        	log.debug "${oneActivity.label} is already off."
        }
    }

    if (triggerSwitchReset == "YES") {
        if (triggerSwitchReset == "ON") {
            triggerSwitch.off()
        } else {
            triggerSwitch.on()
        }
    }
}