/**
 *  Door lighting
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
    name: "Door lighting",
    namespace: "tybo27",
    author: "tybo27",
    description: "Increases lighting while door is open",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn@2x.png"
)

/* ******************************************************************************************
* Preferences: Input door sensor, lights to control, and time for lighting    	 	    *
*********************************************************************************************/
preferences {
 
    section("Turn on these things") {
        input "contact", "capability.contactSensor", multiple: false, title: "Door Switch", required: true
        input "switches", "capability.switch", multiple: true, title: "On/Off Devices", required: false
        input "dimmers", "capability.switchLevel", multiple: true, title: "Dimmer Devices", required: false
        input "dimmerLevel",  "number", title: "dimmerLevel", required: true
        input "timeDelay",  "number", title: "Time Delay", required: true
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
* Initialize: set switchstate and dimmerLevel to 0 and subscribe to sensor                  *
*********************************************************************************************/
def initialize() {
 
    atomicState.dimmerLevels = [:]
    atomicState.switchStates = [:]
    atomicState.delayIsOn= 0

    // Capture intial state
    for (aSwitch in switches) {
         log.debug "Initializing ${aSwitch.displayName} which is currently ${aSwitch.currentSwitch}"
         atomicState.switchStates[aSwitch.displayName] = aSwitch.currentSwitch
    }
    for (aDimmer in dimmers) {
        log.debug "Initializing ${aDimmer.displayName} which is currently ${aDimmer.currentSwitch} and ${aDimmer.currentLevel}"
        atomicState.switchStates[aDimmer.displayName] = aDimmer.currentSwitch
        atomicState.dimmerLevels[aDimmer.displayName] = aDimmer.currentLevel
    }

    subscribe(contact, "contact.open", doorOpenHandler)
    subscribe(contact, "contact.closed", doorClosedHandler)

}

/* ******************************************************************************************
* doorOpenHandler: Turn on switches / ramp up dimmers if door is opened	                    *
**********************************	*********************************************************/
def doorOpenHandler(evt) {
    log.debug "${evt.displayName} has triggered door *${evt.value}*"
    
    def eventDevice = evt.device
    def delayIsOn = atomicState.delayIsOn
    def curSwitchStates = [:]
    def curDimmerLevels = [:]

    // Check to see if in delay window, if so attempt to cancel return, if not capture current settings and set
    if (delayIsOn==1) {
        // remove the scheduled execution
        log.debug "Removing scheduled execution"
        unschedule(returnState)
    } else {
        
        for (aSwitch in switches) {
            log.debug "${aSwitch.displayName} is currently *${aSwitch.currentSwitch}*, commanding on"
            //log.debug "Switch Info: ${aSwitch}"
            curSwitchStates[aSwitch.displayName] = aSwitch.currentSwitch
            aSwitch.on()
        }
        for (aDimmer in dimmers) {
            
            curSwitchStates[aDimmer.displayName] = aDimmer.currentSwitch
            curDimmerLevels[aDimmer.displayName] = aDimmer.currentLevel

            // Only set new level if higher than current, but make sure to turn on either way
            if (aDimmer.currentLevel < dimmerLevel) {
                log.debug "${aDimmer.displayName} is currently *${aDimmer.currentSwitch}* and ${aDimmer.currentLevel} < ${dimmerLevel} so setting level"
                aDimmer.setLevel (dimmerLevel)//, dimmerRampRate)
            } else {
                log.debug "${aDimmer.displayName} is currently *${aDimmer.currentSwitch}* and ${aDimmer.currentLevel} >= ${dimmerLevel} so turning on"
                aDimmer.setLevel (aDimmer.currentLevel)
            }
        }
        log.debug "Switches were: ${curSwitchStates}"
        log.debug "Dimmers were: ${curDimmerLevels}"
    
        // Save states
        atomicState.dimmerLevels = curDimmerLevels
        atomicState.switchStates = curSwitchStates
    }
}

/* ******************************************************************************************
* doorClosedHandler: schedule return to initial state after door is close                   *
*********************************************************************************************/
def doorClosedHandler(evt) {
    log.debug "${evt.displayName} has triggered door *${evt.value}*, scheduling return to previous in ${timeDelay}s"
    def eventDevice = evt.device

    atomicState.delayIsOn = 1
    runIn(timeDelay, returnState)
}

/* ******************************************************************************************
* returnState: return to intial state                                                       *
*********************************************************************************************/
def returnState() {
    log.debug "Running returnState()"
    atomicState.delayIsOn = 0
    def prevDimmerLevels = atomicState.dimmerLevels
    def prevSwitchStates = atomicState.switchStates
    
    //Return all dimmers to previous
    for (aDimmer in dimmers) {
        log.debug "Returning ${aDimmer.displayName} to ${prevDimmerLevels[aDimmer.displayName]}"
        aDimmer.setLevel (prevDimmerLevels[aDimmer.displayName])
        //, dimmerRampRate)
    }
    // If the switch was previously off, turn it back off
    for (aSwitch in switches) {
        if (prevSwitchStates[aSwitch.displayName] == "off") {
            log.debug "${aSwitch.displayName} was previously ${prevSwitchStates[aSwitch.displayName]}, commanding off"
            aSwitch.off()
        } else {
            log.debug "${aSwitch.displayName} was previously ${prevSwitchStates[aSwitch.displayName]}, taking no action"
        }
    }
}