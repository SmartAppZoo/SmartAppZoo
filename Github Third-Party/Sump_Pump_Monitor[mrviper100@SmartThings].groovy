/**
 *  Sump Pump Monitor
 *
 *  Copyright 2015 Pat Heideman
 *  Thanks to bmmiller's smartapp.laundrymonitor.  I changed code to alert when wattage is "above" a set point for X amount of time.
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
 
import groovy.time.* 
 
definition(
    name: "Sump Pump Monitor",
    namespace: "mrviper100",
    author: "Pat Heideman",
    description: "Sump Pump Monitor SmartApp. It tells you if pump has been running to long. It monitors the wattage draw from an Aeon Smart Energy Meter.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Tell me when the Sump Pump is stuck on!"){
		input "sensor1", "capability.powerMeter"
	}
    
    section("Notifications") {
		input "sendPushMessage", "bool", title: "Push Notifications?"
		input "phone", "phone", title: "Send a text message?", required: false      
	}

	section("System Variables"){
    	input "maxWattage", "decimal", title: "Set below pumps running wattage", required: false, defaultValue: 50
        input "maxOnTime", "decimal", title: "Max amount of time the pump is on (secs)", required: false, defaultValue: 60
        input "message", "text", title: "Notification message", description: "Sump Pump is stuck on!", required: true
	}
	
	section ("Additionally", hidden: hideOptionsSection(), hideable: true) {
        input "switches", "capability.switch", title: "Turn on these switches?", required:false, multiple:true
	    input "speech", "capability.speechSynthesis", title:"Speak message via: ", multiple: true, required: false
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
	subscribe(sensor1, "power", powerInputHandler)
}

def powerInputHandler(evt) {
	def latestPower = sensor1.currentValue("power")
    log.trace "Power: ${latestPower}W"
    
    if (!atomicState.isRunning && latestPower < maxWattage) {
    	atomicState.isRunning = true
		atomicState.startedAt = now()
        atomicState.stoppedAt = null
        atomicState.midCycleCheck = null
        log.trace "Cycle started."
    } else if (atomicState.isRunning && latestPower > maxWattage) {
    	if (atomicState.midCycleCheck == null)
        {
        	atomicState.midCycleCheck = true
            atomicState.midCycleTime = now()
        }
        else if (atomicState.midCycleCheck == true)
        {
        	// Time between first check and now  
            if ((now() - atomicState.midCycleTime)/1000 > maxOnTime)
            {
            	atomicState.isRunning = false
                atomicState.stoppedAt = now()  
                log.debug "startedAt: ${atomicState.startedAt}, stoppedAt: ${atomicState.stoppedAt}"                    
                log.info message

                if (phone) {
                    sendSms phone, message
                } else {
                    sendPush message
                }
				
                if (switches) {
          			switches*.on()
      			}               
                if (speech) { 
                    speech.speak(message) 
                }          
            }
        }             	
    }
}

private hideOptionsSection() {
  (phone || switches) ? false : true
}
