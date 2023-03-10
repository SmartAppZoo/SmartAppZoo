/**
 *  Auto Alarms
 *
 *  Copyright 2016 tybo27
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
 
/* ******************************************************************************************
* Definition: Name, namespace, author, description, category, icon						 	*
*********************************************************************************************/
definition(
    name: "Auto Alarms",
    namespace: "tybo27",
    author: "tybo27",
    description: "Automates switches and dimmers based on unison of two virtual counting switches",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn@2x.png")

/* ******************************************************************************************
* Preferences: Input presences, reset times, and switches/dimmers to turn on			 	*
*********************************************************************************************/
preferences {   
    section("Person1") {
		input "presence1", "capability.presenceSensor", multiple: false, title: "Using whose presence"
        //input "alarm1", "device.countingSwitch", multiple: false, title: "Using which Alarm"
        input "alarm1", "capability.switch", multiple: false, title: "Using which Alarm"
        input "mode1", "mode", title: "Mode to transition into when person 1 is present", multiple: false, required: false
	}
    section("Person2") {
		input "presence2", "capability.presenceSensor", multiple: false, title: "Using whose presence"
        //input "alarm2", "device.countingSwitch", multiple: false, title: "Using which Alarm"
        input "alarm2", "capability.switch", multiple: false, title: "Using which Alarm"
        input "mode2", "mode", title: "Mode to transition into when person 2 is present", multiple: false, required: false
	}
    section("General") {
    	input "resetTime", "time", multiple: false, title: "Scheduled time for alarm count reset"
        input "jointMode", "mode", title: "Mode to transition into when both people are present", multiple: false, required: false
    }
    section("Turn on these things") {
    	input "switches", "capability.switch", multiple: true, title: "On/Off Devices", required: false
        input "dimmers", "capability.switchLevel", multiple: true, title: "Dimmer Devices", required: false
        input "dimmerInterval",  "number", title: "Dimmer Interval", required: false
		input "dimmerMax",  "number", multiple: false, title: "Dimmer Max Level", required: false
		input "dimmerRampRate", "number", multiple: false, title: "Dimmer Ramp Rate", required: false
		//input actions after N 
    } 
}

/* ******************************************************************************************
* Installed: Initialize	smartApp														 	*
*********************************************************************************************/
def installed() {
	
    log.debug "Installed with settings: ${settings}"
	initialize()
}

/* ******************************************************************************************
* Updated: Unsubscribe and reinitialize													 	*
*********************************************************************************************/
def updated() {
	
    log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

/* ******************************************************************************************
* Initialize: set atomicstates to 0, and subscribe to events							 	*
*********************************************************************************************/
def initialize() {
  	atomicState.jointCount = 0  
    atomicState.dimmerLevel = 0
    atomicState.presence1 = "present"
    atomicState.presence2 = "present"

    subscribe(alarm1, "switch.on", switchOnHandler)
    subscribe(alarm2, "switch.on", switchOnHandler)
    schedule (resetTime, reset)// Consider subscribing to mode change to "Night" for reset
    subscribe(alarm1, "resetState.reset", switchResetHandler)
    subscribe(alarm2, "resetState.reset", switchResetHandler)
}

/* ******************************************************************************************
* switchOnHandler: Turn on switches / ramp dimmers if presence meets correct criteria	 	*
*********************************************************************************************/
def switchOnHandler (evt) {
	log.debug "${evt.displayName} has triggered turned on! ${evt.value}"
    def eventDevice = evt.device
    
    def curJointCount = atomicState.jointCount
    def person1 = presence1.currentPresence //atomicState.presence1
    def person2 = presence2.currentPresence//atomicState.presence2
    def newMode = jointMode
    
    log.debug "${presence1.displayName} is ${person1}"
    log.debug "${presence2.displayName} is ${person2}"
    
    // Handle both people present case
    if (person1 == "present" && person2 == "present") {
    	log.debug "Both people confirmed present"
        def alarm1Count = alarm1.currentCount
        def alarm2Count = alarm2.currentCount
    	log.debug "${alarm1.displayName} count = ${alarm1Count}"
    	log.debug "${alarm2.displayName} count = ${alarm2Count}"
    	
        if (alarm1Count > 0 && alarm2Count > 0) {
            curJointCount++
            log.debug "Both alarms>0, jointCount=${curJointCount}"
        }
        newMode = jointMode
    } else if  (person1 == "present") {
        curJointCount = alarm1.currentCount
        log.debug "Only ${presence1.displayName} present, jointCount=${curJointCount}"
        newMode = mode1
    } else if (person2 == "present") {
        curJointCount = alarm2.currentCount
        log.debug "Only ${presence2.displayName} present, jointCount=${curJointCount}"
        newMode = mode2
    }
    
    log.debug "Joint Count = ${curJointCount}"
    atomicState.jointCount = curJointCount
    
    // Perform Action if joint Count >=1
    if (curJointCount == 1) {
    	log.debug "Determined Joint Count = 1, turning on first switches"
        for (aSwitch in switches) {
        	log.debug "Turning on ${aSwitch.displayName}"
            aSwitch.on()
        }
        
        log.debug "Changing Modes as applicable"
        if (location.modes?.find{it.name == newMode}) {
            setLocationMode(newMode)
        }  else {
            log.warn "Tried to change to undefined mode '${newMode}'"
        }
    }
    
    // Two thoughts - and in changed via evt.displayname == personX.displayName/ andcurLevel= dimmerInterval*curJointCount
    if  (curJointCount >0) {
    	def curLevel = atomicState.dimmerLevel
    	log.debug "Determined Joint Count > 0, Dimmers are currently set to ${curLevel}, dimmerMax = ${dimmerMax}"
    
    	if (curLevel < dimmerMax) {
    		for (aDimmer in dimmers) {
        		def newLevel = dimmerInterval * curJointCount //curLevel + dimmerInterval
            	if (newLevel > dimmerMax) {
            		newLevel=dimmerMax
            	}
                atomicState.dimmerLevel = newLevel
            	log.debug "Setting ${aDimmer.name} to ${newLevel}"
            	if (curLevel != newLevel) {aDimmer.setLevel (newLevel, dimmerRampRate)}
        	}
    	}
   	}
    log.debug "Turning off evt.device: $eventDevice"
    eventDevice.off()
}

/* ******************************************************************************************
* Reset: Reset all counters to 0														 	*
*********************************************************************************************/
def reset () {
	
    log.debug "Resetting on scehdule, starting at jointCount = ${atomicState.jointCount}, dimmerLevel = ${atomicState.dimmerLevel}"
	atomicState.jointCount = 0
    atomicState.dimmerLevel = 0
    alarm1.reset()
    alarm2.reset()
    log.debug "Reset Complete, jointCount = ${atomicState.jointCount}, dimmerLevel = ${atomicState.dimmerLevel}"
    
}

/* ******************************************************************************************
* swtichRestHandler: Reset jointCount and dimmerLever is one of the alarms is reset		 	*
*********************************************************************************************/
def switchResetHandler (evt) {
   	
    atomicState.jointCount = 0
   	atomicState.dimmerLevel = 0
    log.debug "Resetting because ${evt.displayName} alarm reset, jointCount = ${atomicState.jointCount}, dimmerLevel = ${atomicState.dimmerLevel}"

}