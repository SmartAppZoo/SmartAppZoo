/**
 *  Lighting Director
 *
 *  Version - 1.3
 *  Version - 1.30.1 Modification by Michael Struck - Fixed syntax of help text and titles of scenarios, along with a new icon
 *  Version - 1.40.0 Modification by Michael Struck - Code optimization and added door contact sensor capability		
 *  Version - 1.41.0 Modification by Michael Struck - Code optimization and added time restrictions to each scenario
 *  Version - 2.0  Tim Slagle - Moved to only have 4 slots.  Code was to heavy and needed to be trimmed.
 *  Version - 2.1  Tim Slagle - Moved time interval inputs inline with STs design.
 *  Version - 2.2  Michael Struck - Added the ability to activate switches via the status locks and fixed some syntax issues
 *  Version - 2.5  Michael Struck - Changed the way the app unschedules re-triggered events
 *  Version - 2.5.1 Tim Slagle - Fixed Time Logic
 *  Version - 2.6 Michael Struck - Added the additional restriction of running triggers once per day and misc cleanup of code
 *  Version - 2.7 Michael Struck - Added feature that turns off triggering if the physical switch is pressed.
 *  Version - 2.81 Michael Struck - Fixed an issue with dimmers not stopping light action
 *  Version - 2.9 Michael Struck - Fixed issue where button presses outside of the time restrictions prevent the triggers from firing and code optimization 
 *  Version - 2.9.1 Tim Slagle - Further enhanced time interval logic.  
 *  Version - 2.9.2 Brandon Gordon - Added support for acceleration sensors.
 *  Version - 2.9.3 Brandon Gordon - Added mode change subscriptions.
 *  Version - 2.9.4 Michael Struck - Code Optimization when triggers are tripped
 *  Version - 3.0.0 Michael Struck - Added presence sensors to the triggers to allow for smart outdoor lighting when people arrive and the option to disable mode change subscriptions (from 2.9.3)
 * 
 *  Version - mb1.0 Michael Black - changed minutes to seconds, changed dimmer level to any number and removed a few features I didnt need
 *
 *  Copyright 2015 Tim Slagle & Michael Struck
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *	The original licensing applies, with the following exceptions:
 *		1.	These modifications may NOT be used without freely distributing all these modifications freely
 *			and without limitation, in source form.	 The distribution may be met with a link to source code
 *			with these modifications.
 *		2.	These modifications may NOT be used, directly or indirectly, for the purpose of any type of
 *			monetary gain.	These modifications may not be used in a larger entity which is being sold,
 *			leased, or anything other than freely given.
 *		3.	To clarify 1 and 2 above, if you use these modifications, it must be a free project, and
 *			available to anyone with "no strings attached."	 (You may require a free registration on
 *			a free website or portal in order to distribute the modifications.)
 *		4.	The above listed exceptions to the original licensing do not apply to the holder of the
 *			copyright of the original work.	 The original copyright holder can use the modifications
 *			to hopefully improve their original work.  In that event, this author transfers all claim
 *			and ownership of the modifications to "SmartThings."
 *
 *	Original Copyright information:
 *
 *	Copyright 2014 SmartThings
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */
 
definition(
    name: "Modified Lighting Director",
    namespace: "mblack01",
    author: "Tim Slagle & Michael Struck",
    description: "Control up to 4 sets (scenarios) of lights based on various inputs.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/MichaelStruck/SmartThings/master/Other-SmartApps/Lighting-Director/LightingDirector.png",
    iconX2Url: "https://raw.githubusercontent.com/MichaelStruck/SmartThings/master/Other-SmartApps/Lighting-Director/LightingDirector@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/MichaelStruck/SmartThings/master/Other-SmartApps/Lighting-Director/LightingDirector@2x.png")

preferences {
    page name:"pageSetup"
    page name:"pageSetupScenarioA"
    page name:"pageSetupScenarioB"
    page name:"pageSetupScenarioC"
    page name:"pageSetupScenarioD"
}

// Show setup page
def pageSetup() {

    def pageProperties = [
        name:       "pageSetup",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

	return dynamicPage(pageProperties) {
        section("Setup Menu") {
            href "pageSetupScenarioA", title: getTitle(settings.ScenarioNameA), description: getDesc(settings.ScenarioNameA), state: greyOut(settings.ScenarioNameA)
            href "pageSetupScenarioB", title: getTitle(settings.ScenarioNameB), description: getDesc(settings.ScenarioNameB), state: greyOut(settings.ScenarioNameB)
            href "pageSetupScenarioC", title: getTitle(settings.ScenarioNameC), description: getDesc(settings.ScenarioNameC), state: greyOut(settings.ScenarioNameC)
			href "pageSetupScenarioD", title: getTitle(settings.ScenarioNameD), description: getDesc(settings.ScenarioNameD), state: greyOut(settings.ScenarioNameD)
            }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

// Show "pageSetupScenarioA" page
def pageSetupScenarioA() {

    def inputLightsA = [
        name:       "A_switches",
        type:       "capability.switch",
        title:      "Control the following switches...",
        multiple:   true,
        required:   false
    ]
    def inputDimmersA = [
        name:       "A_dimmers",
        type:       "capability.switchLevel",
        title:      "Dim the following...",
        multiple:   true,
        required:   false
    ]

    def inputMotionA = [
        name:       "A_motion",
        type:       "capability.motionSensor",
        title:      "Using these motion sensors...",
        multiple:   true,
        required:   false
    ]
    
    def inputPresenceA = [
        name:       "A_presence",
        type:       "capability.presenceSensor",
        title:      "Or using these presence sensors...",
        multiple:   true,
        required:   false
    ]
    
	def inputAccelerationA = [
		name:       "A_acceleration",
		type:       "capability.accelerationSensor",
		title:      "Or using these acceleration sensors...",
		multiple:   true,
		required:   false
	]
    def inputContactA = [
        name:       "A_contact",
        type:       "capability.contactSensor",
        title:      "Or using these contact sensors...",
        multiple:   true,
        required:   false
    ]
    
    def inputSwitchDisableA = [
    	name:       "A_switchDisable",
        type:       "bool",
        title:      "Stop triggering if physical switches/dimmers are turned off...",
        defaultValue:true
    ]
    
    def inputUseModeA = [
    	name:       "A_useMode",
        type:       "bool",
        title:      "Allow mode changes to trigger event...",
        defaultValue:false
    ]
    
    def inputLockA = [
        name:       "A_lock",
        type:       "capability.lock",
        title:      "Or using these locks...",
        multiple:   true,
        required:   false
    ]
    
    def inputModeA = [
        name:       "A_mode",
        type:       "mode",
        title:      "Only during the following modes...",
        multiple:   true,
        required:   false
    ]
    
    def inputDayA = [
        name:       "A_day",
        type:       "enum",
        options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
        title:      "Only on certain days of the week...",
        multiple:   true,
        required:   false
    ]
    
    
    def inputLevelA = [
        name:       "A_level",
        type:       "number",
        title:      "Set dimmers to this level",
        multiple:   false,
        required:   false
    ]
    
    def inputTurnOnLuxA = [
        name:       "A_turnOnLux",
        type:       "number",
        title:      "Only run this scenario if lux is below...",
        multiple:   false,
        required:   false
    ]
    
    def inputLuxSensorsA = [
        name:       "A_luxSensors",
        type:       "capability.illuminanceMeasurement",
        title:      "On these lux sensors",
        multiple:   false,
        required:   false
    ]
    
    def inputTurnOffA = [
        name:       "A_turnOff",
        type:       "number",
        title:      "Turn off this scenario after motion stops or doors close/lock (seconds)...",
        multiple:   false,
        required:   false
    ]
    
    def inputScenarioNameA = [
        name:       "ScenarioNameA",
        type:       "text",
        title:      "Scenario Name",
        multiple:   false,
        required:   false,
        defaultValue: empty
    ]
    
    def pageProperties = [
        name:       "pageSetupScenarioA",
    ]

    return dynamicPage(pageProperties) {
section("Name your scenario") {
            input inputScenarioNameA
        }

section("Devices included in the scenario") {
            input inputMotionA
			input inputAccelerationA
            input inputPresenceA
            input inputContactA
            input inputLockA
            input inputLightsA
            input inputDimmersA
            }

section("Scenario settings") {
            input inputLevelA
            input inputTurnOnLuxA
            input inputLuxSensorsA
            input inputTurnOffA
            }
            
section("Scenario restrictions") {            
            input inputSwitchDisableA
            href "timeIntervalInputA", title: "Only during a certain time...", description: getTimeLabel(A_timeStart, A_timeEnd), state: greyedOutTime(A_timeStart, A_timeEnd), refreshAfterSelection:true
            input inputDayA
            input inputModeA
            input inputUseModeA
            }

section("Help") {
            paragraph helpText()
            }
    }
    
}

def pageSetupScenarioB() {

    def inputLightsB = [
        name:       "B_switches",
        type:       "capability.switch",
        title:      "Control the following switches...",
        multiple:   true,
        required:   false
    ]
    def inputDimmersB = [
        name:       "B_dimmers",
        type:       "capability.switchLevel",
        title:      "Dim the following...",
        multiple:   true,
        required:   false
    ]
    
    def inputTurnOnLuxB = [
        name:       "B_turnOnLux",
        type:       "number",
        title:      "Only run this scenario if lux is below...",
        multiple:   false,
        required:   false
    ]
    
    def inputLuxSensorsB = [
        name:       "B_luxSensors",
        type:       "capability.illuminanceMeasurement",
        title:      "On these lux sensors",
        multiple:   false,
        required:   false
    ]

    def inputMotionB = [
        name:       "B_motion",
        type:       "capability.motionSensor",
        title:      "Using these motion sensors...",
        multiple:   true,
        required:   false
    ]
    
    def inputPresenceB = [
        name:       "B_presence",
        type:       "capability.presenceSensor",
        title:      "Or using these presence sensors...",
        multiple:   true,
        required:   false
    ]
    
	def inputAccelerationB = [
		name:       "B_acceleration",
		type:       "capability.accelerationSensor",
		title:      "Or using these acceleration sensors...",
		multiple:   true,
		required:   false
	]
    def inputContactB = [
        name:       "B_contact",
        type:       "capability.contactSensor",
        title:      "Or using these contact sensors...",
        multiple:   true,
        required:   false
    ]
    
    def inputUseModeB = [
    	name:       "B_useMode",
        type:       "bool",
        title:      "Allow mode changes to trigger event...",
        defaultValue:false
    ]
    
    def inputSwitchDisableB = [
    	name:       "B_switchDisable",
        type:       "bool",
        title:      "Stop triggering if physical switches/dimmers are turned off...",
        defaultValue:true
    ]
    
    def inputLockB = [
        name:       "B_lock",
        type:       "capability.lock",
        title:      "Or using these locks...",
        multiple:   true,
        required:   false
    ]
    
    def inputModeB = [
        name:       "B_mode",
        type:       "mode",
        title:      "Only during the following modes...",
        multiple:   true,
        required:   false
    ]
    
    def inputDayB = [
        name:       "B_day",
        type:       "enum",
        options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
        title:      "Only on certain days of the week...",
        multiple:   true,
        required:   false
    ]
    
    def inputLevelB = [
        name:       "B_level",
	type:       "number",
        title:      "Set dimmers to this level",
        multiple:   false,
        required:   false
    ]
    
    def inputTurnOffB = [
        name:       "B_turnOff",
        type:       "number",
        title:      "Turn off this scenario after motion stops or doors close/lock (seconds)...",
        multiple:   false,
        required:   false
    ]
    
    def inputScenarioNameB = [
        name:       "ScenarioNameB",
        type:       "text",
        title:      "Scenario Name",
        multiple:   false,
        required:   false,
        defaultValue: empty
    ]
    
    def pageProperties = [
        name:       "pageSetupScenarioB",
    ]

    return dynamicPage(pageProperties) {
section("Name your scenario") {
            input inputScenarioNameB
        }

section("Devices included in the scenario") {
            input inputMotionB
			input inputAccelerationB
            input inputPresenceB
			input inputContactB
            input inputLockB
            input inputLightsB
            input inputDimmersB
            }

section("Scenario settings") {
            input inputLevelB
            input inputTurnOnLuxB
            input inputLuxSensorsB
            input inputTurnOffB
            }
            
section("Scenario restrictions") {    
            input inputSwitchDisableB
            href "timeIntervalInputB", title: "Only during a certain time...", description: getTimeLabel(B_timeStart, B_timeEnd), state: greyedOutTime(B_timeStart, B_timeEnd), refreshAfterSelection:true
            input inputDayB
            input inputModeB
            input inputUseModeB
            }

section("Help") {
            paragraph helpText()
            }
    }
}

def pageSetupScenarioC() {

    def inputLightsC = [
        name:       "C_switches",
        type:       "capability.switch",
        title:      "Control the following switches...",
        multiple:   true,
        required:   false
    ]
    def inputDimmersC = [
        name:       "C_dimmers",
        type:       "capability.switchLevel",
        title:      "Dim the following...",
        multiple:   true,
        required:   false
    ]

    def inputMotionC = [
        name:       "C_motion",
        type:       "capability.motionSensor",
        title:      "Using these motion sensors...",
        multiple:   true,
        required:   false
    ]
    
    def inputPresenceC = [
        name:       "C_presence",
        type:       "capability.presenceSensor",
        title:      "Or using these presence sensors...",
        multiple:   true,
        required:   false
    ]
    
	def inputAccelerationC = [
		name:       "C_acceleration",
		type:       "capability.accelerationSensor",
		title:      "Or using these acceleration sensors...",
		multiple:   true,
		required:   false
	]
    def inputContactC = [
        name:       "C_contact",
        type:       "capability.contactSensor",
        title:      "Or using these contact sensors...",
        multiple:   true,
        required:   false
    ]
    
    def inputUseModeC = [
    	name:       "C_useMode",
        type:       "bool",
        title:      "Allow mode changes to trigger event...",
        defaultValue:false
    ]
    
    def inputSwitchDisableC = [
    	name:       "C_switchDisable",
        type:       "bool",
        title:      "Stop triggering if physical switches/dimmers are turned off...",
        defaultValue:true
    ]
    
    def inputLockC = [
        name:       "C_lock",
        type:       "capability.lock",
        title:      "Or using these locks...",
        multiple:   true,
        required:   false
    ]
    
    def inputModeC = [
        name:       "C_mode",
        type:       "mode",
        title:      "Only during the following modes...",
        multiple:   true,
        required:   false
    ]
    
    def inputDayC = [
        name:       "C_day",
        type:       "enum",
        options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
        title:      "Only on certain days of the week...",
        multiple:   true,
        required:   false
    ]
    
    def inputLevelC = [
        name:       "C_level",
	type:       "number",
        title:      "Set dimmers to this level",
        multiple:   false,
        required:   false
    ]
    
    def inputTurnOffC = [
        name:       "C_turnOff",
        type:       "number",
        title:      "Turn off this scenario after motion stops or doors close/lock (seconds)...",
        multiple:   false,
        required:   false
    ]
    
    def inputScenarioNameC = [
        name:       "ScenarioNameC",
        type:       "text",
        title:      "Scenario Name",
        multiple:   false,
        required:   false,
        defaultValue: empty
    ]
    
    def inputTurnOnLuxC = [
        name:       "C_turnOnLux",
        type:       "number",
        title:      "Only run this scenario if lux is below...",
        multiple:   false,
        required:   false
    ]
    
    def inputLuxSensorsC = [
        name:       "C_luxSensors",
        type:       "capability.illuminanceMeasurement",
        title:      "On these lux sensors",
        multiple:   false,
        required:   false
    ]
    
    def pageProperties = [
        name:       "pageSetupScenarioC",
    ]

    return dynamicPage(pageProperties) {
        section("Name your scenario") {
            input inputScenarioNameC
        }

section("Devices included in the scenario") {
            input inputMotionC
	    input inputAccelerationC
            input inputPresenceC
            input inputContactC
            input inputLockC
            input inputLightsC
            input inputDimmersC
            }

section("Scenario settings") {
            input inputLevelC
            input inputTurnOnLuxC
            input inputLuxSensorsC
            input inputTurnOffC
			}
            
section("Scenario restrictions") { 
            input inputSwitchDisableC
            href "timeIntervalInputC", title: "Only during a certain time...", description: getTimeLabel(C_timeStart, C_timeEnd), state: greyedOutTime(C_timeStart, C_timeEnd), refreshAfterSelection:true
            input inputDayC
            input inputModeC
            input inputUseModeC
            }

section("Help") {
            paragraph helpText()
            }
    }
}

def pageSetupScenarioD() {

    def inputLightsD = [
        name:       "D_switches",
        type:       "capability.switch",
        title:      "Control the following switches...",
        multiple:   true,
        required:   false
    ]
    def inputDimmersD = [
        name:       "D_dimmers",
        type:       "capability.switchLevel",
        title:      "Dim the following...",
        multiple:   true,
        required:   false
    ]

    def inputMotionD = [
        name:       "D_motion",
        type:       "capability.motionSensor",
        title:      "Using these motion sensors...",
        multiple:   true,
        required:   false
    ]
    
    def inputPresenceD = [
        name:       "D_presence",
        type:       "capability.presenceSensor",
        title:      "Or using these presence sensors...",
        multiple:   true,
        required:   false
    ]
    
	def inputAccelerationD = [
		name:       "D_acceleration",
		type:       "capability.accelerationSensor",
		title:      "Or using these acceleration sensors...",
		multiple:   true,
		required:   false
	]
    def inputContactD = [
        name:       "D_contact",
        type:       "capability.contactSensor",
        title:      "Or using these contact sensors...",
        multiple:   true,
        required:   false
    ]
    
    def inputLockD = [
        name:       "D_lock",
        type:       "capability.lock",
        title:      "Or using these locks...",
        multiple:   true,
        required:   false
    ]
    
    def inputModeD = [
        name:       "D_mode",
        type:       "mode",
        title:      "Only during the following modes...",
        multiple:   true,
        required:   false
    ]
    
    def inputUseModeD = [
    	name:       "D_useMode",
        type:       "bool",
        title:      "Allow mode changes to trigger event...",
        defaultValue:false
    ]
    
    def inputSwitchDisableD = [
    	name:       "D_switchDisable",
        type:       "bool",
        title:      "Stop triggering if physical switches/dimmers are turned off...",
        defaultValue:true
    ]
    
    def inputDayD = [
        name:       "D_day",
        type:       "enum",
        options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
        title:      "Only on certain days of the week...",
        multiple:   true,
        required:   false
    ]
    
    
    def inputLevelD = [
        name:       "D_level",
	type:       "number",
        title:      "Set dimmers to this level",
        multiple:   false,
        required:   false
    ]
    
    def inputTurnOffD = [
        name:       "D_turnOff",
        type:       "number",
        title:      "Turn off this scenario after motion stops, doors close or close/lock (seconds)...",
        multiple:   false,
        required:   false
    ]
    
    def inputScenarioNameD = [
        name:       "ScenarioNameD",
        type:       "text",
        title:      "Scenario Name",
        multiple:   false,
        required:   false,
        defaultValue: empty
    ]
    
    def inputTurnOnLuxD = [
        name:       "D_turnOnLux",
        type:       "number",
        title:      "Only run this scenario if lux is below...",
        multiple:   false,
        required:   false
    ]
    
    def inputLuxSensorsD = [
        name:       "D_luxSensors",
        type:       "capability.illuminanceMeasurement",
        title:      "On these lux sensors",
        multiple:   false,
        required:   false
    ]

    def pageProperties = [
        name:       "pageSetupScenarioD",
    ]

    return dynamicPage(pageProperties) {
        section("Name your scenario") {
            input inputScenarioNameD
        }

section("Devices included in the scenario") {
            input inputMotionD
			input inputAccelerationD
            input inputPresenceD
          	input inputContactD
            input inputLockD
            input inputLightsD
            input inputDimmersD
            }

section("Scenario settings") {
            input inputLevelD
            input inputTurnOnLuxD
            input inputLuxSensorsD
            input inputTurnOffD
			}
            
section("Scenario restrictions") {    

            input inputSwitchDisableD
            href "timeIntervalInputD", title: "Only during a certain time", description: getTimeLabel(D_timeStart, D_timeEnd), state: greyedOutTime(D_timeStart, D_timeEnd), refreshAfterSelection:true
            input inputDayD
            input inputModeD
            input inputUseModeD
            }

section("Help") {
            paragraph helpText()
            }
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {

//to keep track of on presses
state.myCounter=0
state.isLightOn=false
state.onByThisApp=false

midNightReset()

if(A_motion) {
	subscribe(settings.A_motion, "motion", onEventA)
}

if(A_presence){
	subscribe(settings.A_presence, "presence", onEventA)
}

if(A_acceleration) {
	subscribe(settings.A_acceleration, "acceleration", onEventA)
}

if(A_contact) {
	subscribe(settings.A_contact, "contact", onEventA)
}

if(A_lock) {
	subscribe(settings.A_lock, "lock", onEventA)
}

if(A_switchDisable) {
	subscribe(A_switches, "switch", onPressA)
        subscribe(A_dimmers, "switch", onPressA)

}

if(A_mode && A_useMode) {
    subscribe(location, onEventA)
}

if(B_motion) {
	subscribe(settings.B_motion, "motion", onEventB)
}

if(B_presence){
	subscribe(settings.B_presence, "presence", onEventB)
}

if(B_acceleration) {
	subscribe(settings.B_acceleration, "acceleration", onEventB)
}

if(B_contact) {
	subscribe(settings.B_contact, "contact", onEventB)
}

if(B_lock) {
	subscribe(settings.B_lock, "lock", onEventB)
}

if(B_switchDisable) {
	subscribe(B_switches, "switch", onPressB)
	subscribe(B_dimmers, "switch", onPressB)
            
}

if(B_mode && B_useMode) {
    subscribe(location, onEventB)
}

if(C_motion) {
	subscribe(settings.C_motion, "motion", onEventC)
}

if(C_presence){
	subscribe(settings.C_presence, "presence", onEventC)
}

if(C_acceleration) {
	subscribe(settings.C_acceleration, "acceleration", onEventC)
}

if(C_contact) {
	subscribe(settings.C_contact, "contact", onEventC)
}

if(C_lock) {
	subscribe(settings.C_lock, "lock", onEventC)
}

if(C_switchDisable) {
	subscribe(C_switches, "switch", onPressC)
        subscribe(C_dimmers, "switch", onPressC)
}

if(C_mode && C_useMode) {
    subscribe(location, onEventC)
}

if(D_motion) {
	subscribe(settings.D_motion, "motion", onEventD)
}

if(D_presence){
	subscribe(settings.D_presence, "presence", onEventD)
}

if(D_acceleration) {
	subscribe(settings.D_acceleration, "acceleration", onEventD)
}

if(D_contact) {
	subscribe(settings.D_contact, "contact", onEventD)
}

if(D_lock) {
	subscribe(settings.D_lock, "lock", onEventD)
}

if(D_switchDisable) {
	subscribe(D_switches, "switch", onPressD)
        subscribe(D_dimmers, "switch", onPressD)
}

if(D_mode && D_useMode) {
    subscribe(location, onEventD)
}

}

def resetMyCounter() {
  log.info("Reseting state.myCounter to 0")
  state.myCounter=0
  }

def onEventA(evt) {

if (!state.isLightOn && (!A_switchDisable || (A_switchDisable && !state.A_triggered))) {
//log.debug("A.triggered is $state.A_triggered")


if ((!A_mode || A_mode.contains(location.mode)) && getTimeOk (A_timeStart, A_timeEnd) && getDayOk(A_day)) {

log.debug("IN EVENT A - CHECK :: is the light already on by another app? $state.isLightOn")

if ((!A_luxSensors) || (A_luxSensors.latestValue("illuminance") <= A_turnOnLux)){
def A_levelOn = A_level as Integer

if (getInputOk(A_motion, A_contact, A_lock, A_acceleration) || evt.value == "present") {
            log.debug("Presence, Motion, Door Open or Unlock Detected Running '${ScenarioNameA}'")
        	
            
            settings.A_dimmers?.setLevel(A_levelOn)
            settings.A_switches?.on()
            state.onByThisApp=true
            
            
            log.debug("TURNED ON $settings.A_dimmers and set state.onByThisApp to $state.onByThisApp")

        	if (state.A_timerStart){
        	log.debug("UNSCHEDULED TIMER")
            	unschedule(delayTurnOffA)
            	state.A_timerStart = false
        	}
}
else {
    	if (settings.A_turnOff) {
    		log.debug("STARTING TURN OFF TIME OF $A_turnOff")
		runIn(A_turnOff, "delayTurnOffA")
		state.A_timerStart = true
        }
        else {
		settings.A_switches?.off()
		settings.A_dimmers?.setLevel(0)
		state.onByThisApp=false
        }
}
}
}

}
}

def delayTurnOffA(){
	log.debug("TURNING OFF SWITCH IN DELAY METHOD A")
	settings.A_switches?.off()
	settings.A_dimmers?.setLevel(0)
	state.A_timerStart = false
	state.onByThisApp=false
}

def onPressA(evt) {

	if ((!A_mode || A_mode.contains(location.mode)) && getTimeOk (A_timeStart, A_timeEnd) && getDayOk(A_day)) {
	
	        
	        if (evt.value == "on" && !state.onByThisApp) {
	        	state.isLightOn=true
	        	log.info("The light status was change to ON")
	        	}
	        	
	         else   if (evt.value == "off" && !state.onByThisApp) {
			state.isLightOn=false
			log.info("The light status was change to OFF")
        	}
        	
		if ((!A_luxSensors) || (A_luxSensors.latestValue("illuminance") <= A_turnOnLux)){   
		
	log.info("In onPressA")
		
		    if (evt.type == "physical" ) {

			    // get the value of this event, e.g., "on" or "off"
			    log.debug ("A - The value of this event is ${evt.value}")

			    // get the value description of this event
			    log.debug ("A - The value of this event description is ${evt.description}")

			    // get the value descriptionText of this event
			    log.debug ("A - The value of this event text is ${evt.descriptionText}")

			    // get the value for type of this event
			    log.debug ("A - The value of this event TYPE is ${evt.type}")

			    // get the value description of this event
			    //log.debug ("A - The value of this event linkText is ${evt.linkText}")

			    state.myCounter = state.myCounter + 1

			    log.info("Value of state.myCounter is $state.myCounter")

			    runIn(7, "resetMyCounter")

			    if (state.myCounter == 2) {

				log.info("CHANGING a_triggered state because on button was pressed twice")

				if (!state.A_triggered) {
					state.A_triggered = true
					unschedule(delayTurnOffA)
					log.debug "Physical switch in '${ScenarioNameA}' pressed. Triggers for this scenario DISABLED."
				}

				else if (state.A_triggered) {
					log.info("CHANGING a_triggered state because on button was pressed twice")
					state.A_triggered = false
					runIn(A_turnOff, "delayTurnOffA")
					log.debug "Physical switch in '${ScenarioNameA}' pressed. Triggers for this scenario ENABLED."
				}

			    }

		     }
	    }
    }
}

def onEventB(evt) {

if (!state.isLightOn && (!B_switchDisable || (B_switchDisable && !state.B_triggered))) {
//log.debug("B.triggered is $state.B_triggered")


if ((!B_mode || B_mode.contains(location.mode)) && getTimeOk (B_timeStart, B_timeEnd) && getDayOk(B_day)) {

log.debug("IN EVENT B - CHECK :: is the light already on by another app? $state.isLightOn")

if ((!B_luxSensors) || (B_luxSensors.latestValue("illuminance") <= B_turnOnLux)){
def B_levelOn = B_level as Integer

if (getInputOk(B_motion, B_contact, B_lock, B_acceleration) || evt.value == "present") {
            log.debug("Presence, Motion, Door Open or Unlock Detected Running '${ScenarioNameB}'")
        	
            
            settings.B_dimmers?.setLevel(B_levelOn)
            settings.B_switches?.on()
            state.onByThisApp=true
            
            
            log.debug("TURNED ON $settings.B_dimmers and set state.onByThisApp to $state.onByThisApp")

        	if (state.B_timerStart){
        	log.debug("UNSCHEDULED TIMER")
            	unschedule(delayTurnOffB)
            	state.B_timerStart = false
        	}
}
else {
    	if (settings.B_turnOff) {
    		log.debug("STARTING TURN OFF TIME OF $B_turnOff")
		runIn(B_turnOff, "delayTurnOffB")
		state.B_timerStart = true
        }
        else {
		settings.B_switches?.off()
		settings.B_dimmers?.setLevel(0)
		state.onByThisApp=false
        }
}
}
}

}
}

def delayTurnOffB(){
	log.debug("TURNING OFF SWITCH IN DELAY METHOD B")
	settings.B_switches?.off()
	settings.B_dimmers?.setLevel(0)
	state.B_timerStart = false
	state.onByThisApp=false
}

def onPressB(evt) {

	if ((!B_mode || B_mode.contains(location.mode)) && getTimeOk (B_timeStart, B_timeEnd) && getDayOk(B_day)) {
	
	        
	        if (evt.value == "on" && !state.onByThisApp) {
	        	state.isLightOn=true
	        	log.info("The light status was change to ON")
	        	}
	        	
	         else   if (evt.value == "off" && !state.onByThisApp) {
			state.isLightOn=false
			log.info("The light status was change to OFF")
        	}
        	
		if ((!B_luxSensors) || (B_luxSensors.latestValue("illuminance") <= B_turnOnLux)){   
		
	log.info("In onPressB")
		
		    if (evt.type == "physical" ) {

			    // get the value of this event, e.g., "on" or "off"
			    log.debug ("B - The value of this event is ${evt.value}")

			    // get the value description of this event
			    log.debug ("B - The value of this event description is ${evt.description}")

			    // get the value descriptionText of this event
			    log.debug ("B - The value of this event text is ${evt.descriptionText}")

			    // get the value for type of this event
			    log.debug ("B - The value of this event TYPE is ${evt.type}")

			    // get the value description of this event
			    //log.debug ("B - The value of this event linkText is ${evt.linkText}")

			    state.myCounter = state.myCounter + 1

			    log.info("Value of state.myCounter is $state.myCounter")

			    runIn(7, "resetMyCounter")

			    if (state.myCounter == 2) {

				log.info("CHANGING a_triggered state because on button was pressed twice")

				if (!state.B_triggered) {
					state.B_triggered = true
					unschedule(delayTurnOffB)
					log.debug "Physical switch in '${ScenarioNameB}' pressed. Triggers for this scenario DISABLED."
				}

				else if (state.B_triggered) {
					log.info("CHANGING B_triggered state because on button was pressed twice")
					state.B_triggered = false
					runIn(B_turnOff, "delayTurnOffB")
					log.debug "Physical switch in '${ScenarioNameB}' pressed. Triggers for this scenario ENABLED."
				}

			    }

		     }
	    }
    }
}

def onEventC(evt) {

if (!state.isLightOn && (!C_switchDisable || (C_switchDisable && !state.C_triggered))) {
//log.debug("C.triggered is $state.C_triggered")


if ((!C_mode || C_mode.contains(location.mode)) && getTimeOk (C_timeStart, C_timeEnd) && getDayOk(C_day)) {

log.debug("IN EVENT C - CHECK :: is the light already on by another app? $state.isLightOn")

if ((!C_luxSensors) || (C_luxSensors.latestValue("illuminance") <= C_turnOnLux)){
def C_levelOn = C_level as Integer

if (getInputOk(C_motion, C_contact, C_lock, C_acceleration) || evt.value == "present") {
            log.debug("Presence, Motion, Door Open or Unlock Detected Running '${ScenarioNameC}'")
        	
            
            settings.C_dimmers?.setLevel(C_levelOn)
            settings.C_switches?.on()
            state.onByThisApp=true
            
            
            log.debug("TURNED ON $settings.C_dimmers and set state.onByThisApp to $state.onByThisApp")

        	if (state.C_timerStart){
        	log.debug("UNSCHEDULED TIMER")
            	unschedule(delayTurnOffC)
            	state.C_timerStart = false
        	}
}
else {
    	if (settings.C_turnOff) {
    		log.debug("STARTING TURN OFF TIME OF $C_turnOff")
		runIn(C_turnOff, "delayTurnOffC")
		state.C_timerStart = true
        }
        else {
		settings.C_switches?.off()
		settings.C_dimmers?.setLevel(0)
		state.onByThisApp=false
        }
}
}
}

}
}

def delayTurnOffC(){
	log.debug("TURNING OFF SWITCH IN DELAY METHOD C")
	settings.C_switches?.off()
	settings.C_dimmers?.setLevel(0)
	state.C_timerStart = false
	state.onByThisApp=false
}

def onPressC(evt) {


	if ((!C_mode || C_mode.contains(location.mode)) && getTimeOk (C_timeStart, C_timeEnd) && getDayOk(C_day)) {
	        
	        if (evt.value == "on" && !state.onByThisApp) {
	        	state.isLightOn=true
	        	log.info("The light status was change to ON")
	        	}
	        	
	         else   if (evt.value == "off" && !state.onByThisApp) {
			state.isLightOn=false
			log.info("The light status was change to OFF")
        	}
        	
		if ((!C_luxSensors) || (C_luxSensors.latestValue("illuminance") <= C_turnOnLux)){   
		
	log.info("In onPressC")
		
		    if (evt.type == "physical" ) {

			    // get the value of this event, e.g., "on" or "off"
			    log.debug ("C - The value of this event is ${evt.value}")

			    // get the value description of this event
			    log.debug ("C - The value of this event description is ${evt.description}")

			    // get the value descriptionText of this event
			    log.debug ("C - The value of this event text is ${evt.descriptionText}")

			    // get the value for type of this event
			    log.debug ("C - The value of this event TYPE is ${evt.type}")

			    // get the value description of this event
			    //log.debug ("C - The value of this event linkText is ${evt.linkText}")

			    state.myCounter = state.myCounter + 1

			    log.info("Value of state.myCounter is $state.myCounter")

			    runIn(7, "resetMyCounter")

			    if (state.myCounter == 2) {

				log.info("CHANGING a_triggered state because on button was pressed twice")

				if (!state.C_triggered) {
					state.C_triggered = true
					unschedule(delayTurnOffC)
					log.debug "Physical switch in '${ScenarioNameC}' pressed. Triggers for this scenario DISABLED."
				}

				else if (state.C_triggered) {
					log.info("CHANGING C_triggered state because on button was pressed twice")
					state.C_triggered = false
					runIn(C_turnOff, "delayTurnOffC")
					log.debug "Physical switch in '${ScenarioNameC}' pressed. Triggers for this scenario ENABLED."
				}

			    }

		     }
	    }
    }
}

def onEventD(evt) {


if (!state.isLightOn && (!D_switchDisable || (D_switchDisable && !state.D_triggered))) {
//log.debug("D.triggered is $state.D_triggered")


if ((!D_mode || D_mode.contains(location.mode)) && getTimeOk (D_timeStart, D_timeEnd) && getDayOk(D_day)) {

log.debug("IN EVENT D - CHECK :: is the light already on by another app? $state.isLightOn")

if ((!D_luxSensors) || (D_luxSensors.latestValue("illuminance") <= D_turnOnLux)){
def D_levelOn = D_level as Integer

if (getInputOk(D_motion, D_contact, D_lock, D_acceleration) || evt.value == "present") {
            log.debug("Presence, Motion, Door Open or Unlock Detected Running '${ScenarioNameD}'")
        	
            
            settings.D_dimmers?.setLevel(D_levelOn)
            settings.D_switches?.on()
            state.onByThisApp=true
            
            
            log.debug("TURNED ON $settings.D_dimmers and set state.onByThisApp to $state.onByThisApp")

        	if (state.D_timerStart){
        	log.debug("UNSCHEDULED TIMER")
            	unschedule(delayTurnOffD)
            	state.D_timerStart = false
        	}
}
else {
    	if (settings.D_turnOff) {
    		log.debug("STARTING TURN OFF TIME OF $D_turnOff")
		runIn(D_turnOff, "delayTurnOffD")
		state.D_timerStart = true
        }
        else {
		settings.D_switches?.off()
		settings.D_dimmers?.setLevel(0)
		state.onByThisApp=false
        }
}
}
}

}
}

def delayTurnOffD(){
	log.debug("TURNING OFF SWITCH IN DELAY METHOD D")
	settings.D_switches?.off()
	settings.D_dimmers?.setLevel(0)
	state.D_timerStart = false
	state.onByThisApp=false
}

def onPressD(evt) {

	if ((!D_mode || D_mode.contains(location.mode)) && getTimeOk (D_timeStart, D_timeEnd) && getDayOk(D_day)) {
	        
	        if (evt.value == "on" && !state.onByThisApp) {
	        	state.isLightOn=true
	        	log.info("The light status was change to ON")
	        	}
	        	
	         else   if (evt.value == "off" && !state.onByThisApp) {
			state.isLightOn=false
			log.info("The light status was change to OFF")
        	}
        	
		if ((!D_luxSensors) || (D_luxSensors.latestValue("illuminance") <= D_turnOnLux)){   
		
	log.info("In onPressD")
		
			    if (evt.type == "physical" ) {
			    		
				    // get the value of this event, e.g., "on" or "off"
				    log.debug ("D - The value of this event is ${evt.value}")

				    // get the value description of this event
				    log.debug ("D - The value of this event description is ${evt.description}")

				    // get the value descriptionText of this event
				    log.debug ("D - The value of this event text is ${evt.descriptionText}")

				    // get the value for type of this event
				    log.debug ("D - The value of this event TYPE is ${evt.type}")

				    // get the value description of this event
				    //log.debug ("D - The value of this event linkText is ${evt.linkText}")

				    state.myCounter = state.myCounter + 1

				    log.info("Value of state.myCounter is $state.myCounter")

				    runIn(7, "resetMyCounter")

				    if (state.myCounter == 2) {

					log.info("CHANGING a_triggered state because on button was pressed twice")

					if (!state.D_triggered) {
						state.D_triggered = true
						unschedule(delayTurnOffD)
						log.debug "Physical switch in '${ScenarioNameD}' pressed. Triggers for this scenario DISABLED."
					}

					else if (state.D_triggered) {
						log.info("CHANGING D_triggered state because on button was pressed twice")
						state.D_triggered = false
						runIn(D_turnOff, "delayTurnOffD")
						log.debug "Physical switch in '${ScenarioNameD}' pressed. Triggers for this scenario ENABLED."
					}

				    }

			     }
		    }
	    }
}

//Common Methods

def midNightReset() {
    state.A_triggered = false
    state.B_triggered = false
    state.C_triggered = false
    state.D_triggered = false
}

private def helpText() {
	def text =
		"Select locks, presence, motion, acceleration or contact sensors to control a set of lights. " +
        "Each scenario can control dimmers and switches but can also be " +
        "restricted to modes or between certain times and turned off after " +
        "motion stops, doors close or lock. Scenarios can also be limited to  " +
        "running once or to stop running if the physical switches are turned off."
	text
}

def greyOut(scenario){
	def result = ""
    if (scenario) {
    	result = "complete"	
    }
    result
}

def greyedOutTime(start, end){
	def result = ""
    if (start || end) {
    	result = "complete"	
    }
    result
}

def getTitle(scenario) {
	def title = "Empty"
	if (scenario) {
		title = scenario
    }
	title
}

def getDesc(scenario) {
	def desc = "Tap to create a scenario"
	if (scenario) {
		desc = "Tap to edit scenario"
    }
	desc	
}

def getMidnight() {
	def midnightToday = timeToday("2000-01-01T23:59:59.999-0000", location.timeZone)
	midnightToday
}

private getInputOk(motion, contact, lock, acceleration) {

def motionDetected = false
def accelerationDetected = false
def contactDetected = false
def unlockDetected = false
def result = false

if (motion) {
	if (motion.latestValue("motion").contains("active")) {
		motionDetected = true
	}
}

if (acceleration) {
	if (acceleration.latestValue("acceleration").contains("active")) {
		accelerationDetected = true
	}
}

if (contact) {
	if (contact.latestValue("contact").contains("open")) {
		contactDetected = true
	}
}

if (lock) {
	if (lock.latestValue("lock").contains("unlocked")) {
		unlockDetected = true
	}
}

result = motionDetected || contactDetected || unlockDetected || accelerationDetected
result

}

private getTimeOk(starting, ending) {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
    
    else if (starting){
    	result = currTime >= start
    }
    else if (ending){
    	result = currTime <= stop
    }
    
	log.trace "timeOk = $result"
	result
}

def getTimeLabel(start, end){
	def timeLabel = "Tap to set"
	
    if(start && end){
    	timeLabel = "Between" + " " + hhmm(start) + " "  + "and" + " " +  hhmm(end)
    }
    else if (start) {
		timeLabel = "Start at" + " " + hhmm(start)
    }
    else if(end){
    timeLabel = "End at" + hhmm(end)
    }
	timeLabel	
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private getDayOk(dayList) {
	def result = true
    if (dayList) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = dayList.contains(day)
	}
    result
}


page(name: "timeIntervalInputA", title: "Only during a certain time", refreshAfterSelection:true) {
		section {
			input "A_timeStart", "time", title: "Starting", required: false, refreshAfterSelection:true
			input "A_timeEnd", "time", title: "Ending", required: false, refreshAfterSelection:true
		}
        }  
page(name: "timeIntervalInputB", title: "Only during a certain time", refreshAfterSelection:true) {
		section {
			input "B_timeStart", "time", title: "Starting", required: false, refreshAfterSelection:true
			input "B_timeEnd", "time", title: "Ending", required: false, refreshAfterSelection:true
		}
        }  
page(name: "timeIntervalInputC", title: "Only during a certain time", refreshAfterSelection:true) {
		section {
			input "C_timeStart", "time", title: "Starting", required: false, refreshAfterSelection:true
			input "C_timeEnd", "time", title: "Ending", required: false, refreshAfterSelection:true
		}
        }         
page(name: "timeIntervalInputD", title: "Only during a certain time", refreshAfterSelection:true) {
		section {
			input "D_timeStart", "time", title: "Starting", required: false, refreshAfterSelection:true
			input "D_timeEnd", "time", title: "Ending", required: false, refreshAfterSelection:true
		}
        }          


