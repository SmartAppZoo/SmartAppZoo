/**
 *  ADT Mode Change
 *
 *  Copyright 2018 CRAIG KING
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
 /**
 10/27/2018
 Added ability to turn on and off the ability to keep the ADT Status and the Location Alam state in the same status.
 Updated test to better describe certain settings.
 
 8/06/18 1.0.1
 Added ability to control location alarm status in Smartthings. This will allow more integration with SHM type components
 Added the ability to create a delay when activating the location alarm state. This will be used to enable delayed arming 
 when using non monitored sensors. Will be required for ADT Any Sensor app to function.
 */
 
definition(
    name: "ADT Mode Change",
    namespace: "Mavrrick",
    author: "CRAIG KING",
    description: "ADT Child app to change modes.",
    category: "Safety & Security",
    parent: "Mavrrick:ADT Tools",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select what button you want for each mode..."){
        input "myDisarmButton", "capability.momentary", title: "What Button will disarm the alarm?", required: false, multiple: false
        input "myArmStay", "capability.momentary", title: "What button will put the alarm in Armed/Stay?", required: false, multiple: false
        input "myArmAway", "capability.momentary", title: "What button will put the alarm in Armed/Away?", required: false, multiple: false
	}
   section("Smartthings location alarm state setup. These must be configured to use the Any Sensory Child App."){
   		input "locAlarmSync", "bool", title: "Maintain synchronization between Smartthings ADT alarm panel and location clound alarm state", description: "This switch will tell ADT Tools if it needs to kep the ADT Alarm and the Smarthings location alarm status in sync.", defaultValue: false, required: true, multiple: false
		input "delay", "number", range: "1..120", title: "Please specify your Alarm Delay", required: true, defaultValue: 0
	}
    
section("Select your ADT Smart Panel..."){
		input "panel", "capability.battery", title: "ADT Panel?", required: true
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
    subscribe(myDisarmButton, "momentary.pushed", disarmHandler)
    subscribe(myArmStay, "momentary.pushed", armstayHandler)
    subscribe(myArmAway, "momentary.pushed", armawayHandler)
    if (settings.locAlarmSync) 
		{
    	subscribe(location, "securitySystemStatus", alarmModeHandler)
        }
}



def disarmHandler(evt) {
      log.debug "Disarming alarm"
      panel?.disarm()
	}

def armstayHandler(evt) {
       log.debug "Changeing alarm to Alarm/Stay"
       def alarmState = panel.currentSecuritySystemStatus
        if (alarmState == "armedAway") {
        	log.debug "Current alarm mode: ${alarmState}. Alarm must be in Disarmed state before changeing state"
        }
        else {       
        panel?.armStay(armedStay)

        }
	}
    
def armawayHandler(evt) {
       	log.debug "Changeing alarm to Alarm/Away"
        def alarmState = panel.currentSecuritySystemStatus
        if (alarmState == "armedStay") {
        	log.debug "Current alarm mode: ${alarmState}. Alarm must be in Disarmed state before changeing state"
        }
        else {
      	panel?.armAway(armedAway)}
	   }
       
def alarmModeHandler(evt) {
	switch (evt.value)
        	{
            	case "armedAway":
        			runIn(delay, armawaySHMHandler)
                    break
                case "armedStay":
                	log.debug "Attempting change of Hub alarm Mode"
                    runIn(delay, armstaySHMHandler)
                    break
                case "disarmed" :
                    sendLocationEvent(name: "alarmSystemStatus", value: "off")
                    break
                default:
					log.debug "Ignoring unexpected alarmtype mode."
        			log.debug "Unexpected value for Alarm status"
                    break
                    }
         }

def armstaySHMHandler() {
       	log.debug "Changeing HUB alarm state to Armed/Stay"
        sendLocationEvent(name: "alarmSystemStatus", value: "stay")
	   }
       
def armawaySHMHandler() {
       	log.debug "Changeing HUB alarm state to Alarm/Away"
        sendLocationEvent(name: "alarmSystemStatus", value: "away")
	   } 
 