/**
 *  SHMHelper
 *
 *  Copyright 2016 JojanAntony
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
    name: "SHMHelper",
    namespace: "jojanantony",
    author: "JojanAntony",
    description: "Enable run routines when user arms/disarms",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  page(name: "selectRoutines")
}

def selectRoutines() {
	dynamicPage(name: "selectRoutines", title: "When Smart Home Monitor change modes", install: true, uninstall: true) {
  
    	def actions = location.getHelloHome()?.getPhrases()*.label
    	actions?.sort()

		section("Execute Routines?") {
    		input(name: "armRoutine", title: "Arm/Away routine", type: "enum", options: actions, required: false)
      		input(name: "stayRoutine", title: "Arm/Stay routine", type: "enum", options: actions, required: false)
      		input(name: "disarmRoutine", title: "Disarm routine", type: "enum", options: actions, required: false)
    	}
    	section("Send Notifications?") {
        	input "pushNotify", "bool", required: false, title: "Send Push Notification?"
           	input "phone", "phone", title: "Send text message?", description: "Phone Number", required: false    
  		}
  	}
}

def installed() {
	log.debug "SHMHelper Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "SHMHelper Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(location, "alarmSystemStatus", alarmStatusHandler, [filterEvents:false])
}

// TODO: implement event handlers
def alarmStatusHandler(evt) {
	log.debug "SHMHelper  alarm status changed to: ${evt.value}"
  	if (evt.value == 'away') {
    	location.helloHome?.execute(settings.armRoutine)
        sendMsg("${location.name} is Armed-Away")
  	} else if (evt.value == 'stay') {
    	location.helloHome?.execute(settings.stayRoutine)
        sendMsg("${location.name} is Armed-Stay")
  	} else if (evt.value == 'off') {
    	location.helloHome?.execute(settings.disarmRoutine)
        sendMsg("${location.name} is Disarmed")
  	}	
}

def sendMsg(message) {
	if (pushNotify) {
    	sendPush(message)
    }
    if (phone) {
		sendSms(phone, message)
    }
}
