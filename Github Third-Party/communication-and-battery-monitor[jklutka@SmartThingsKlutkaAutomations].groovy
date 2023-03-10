/**
 *  Sensor Radio Silence Detection
 *
 *  Copyright 2016 Justin Klutka
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
    name: "Communication and Battery Monitor",
    namespace: "klutka",
    author: "Justin Klutka",
    description: "The purpose of this app is to determine if battery operated sensors have not reported an event in a set amount of time.  I was finding my multi-sensors were running out of battery and there was no way to know unless I check each sensor via the mobile app.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Please select sensors to monitor:") {
		input "batteryPoweredSensors", "capability.battery", title: "Battery Powered Sensors:", required: true, multiple: true 
        input "thresholdPreference", "number", title: "Specify the threshold, in minutes, before report a problem.", required: true, description: "240 is a good start...", multiple: false
	}
    
    section("Send a push alert with every successful inspection?") {
    	input name: "pushSuccessfulInspections", type: "enum", title: "Set your preference:", options: ["Yes", "No"], description: "Set your preference:", required: true

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

	log.debug "Initialize app"
	
    runEvery3Hours(inspectDeviceSets)
        
}

def getThreshold() {
	return 1000 * 60 * thresholdPreference - 1000
}

def inspectDeviceSets() {
	
    log.debug "Starting Device Inspection."
    
    def reportCount = 0
        
    batteryPoweredSensors.each { 
    
    	def recentEvents = it.events([max: 1])            
        
        if (recentEvents.size > 0) {
        	log.debug "There were ${recentEvents.size} found for device ${it.displayName} with the last activity occuring at ${recentEvents[0].date}."         
        
            def elapseTime = now() - recentEvents[0].date.time
            
            if (elapseTime >= getThreshold()) {

				log.debug "Computed elapse time of ${elapseTime} with a threshold of ${getThreshold()}."      

                ReportOnDevice(it, elapseTime)
                reportCount++
            }        
       	}
        else { 
        	SendNoActivityWarning(it)
            reportCount++
        }        
    }
              
    //determine if a success report needs to be sent
    if (reportCount == 0 && pushSuccessfulInspections == "Yes") {
    	log.debug "Sending a success report based on user preference."
        sendPush("All devices were inspected and passed!")
    }
        
    log.debug "Device inspection is complete at ${state.lastCheckTime}"
    
}

private ReportOnDevice(device, deltaMilliseconds) {

	def convertedMinutes = deltaMilliseconds / 1000 / 60
    
    log.debug "The device ${device.displayName} has not reported activity in ${convertedMinutes.intValue()} minutes."
    
    sendPush("The device ${device.displayName} has not reported activity in ${convertedMinutes.intValue()} minutes.  Please check the device.")
}

private SendNoActivityWarning(device) {

	log.debug "The device ${device.displayName} reported no activity -- warn about possible dead device."
    
    sendPush("Warning! The device ${device.displayName} reported no activity.  It may no longer be functioning.")

}