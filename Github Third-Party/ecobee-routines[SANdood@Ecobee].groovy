/**
 *  ecobee Routines
 *
 *  Copyright 2015 Sean Kendall Schneyer
 *
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
 *
 * 0.1.4 - Fix Custom Mode Handling
 * 0.1.5 - SendNotificationMessage so that the action shows up in Notification log after the mode/routine notices
 * 0.1.6 - Logic tweaks, fixed bad state.variableNames
 * 0.1.7 - Extended to support the inverse: Ecobee Program change (including Vacation) can change ST Mode or run Routine
 * 1.0.0 - Final preparation for General Release
 * 1.0.1 - Updated LOG and setup for consistency
 */
def getVersionNum() { return "1.0.1" }
private def getVersionLabel() { return "ecobee Routines Version ${getVersionNum()}" }


definition(
	name: "ecobee Routines",
	namespace: "smartthings",
	author: "Sean Kendall Schneyer (smartthings at linuxbox dot org)",
	description: "Change ecobee Programs based on SmartThings Routine execution or Mode changes, OR change Mode/run Routine based on Ecobee Program/Vacation changes",
	category: "Convenience",
	parent: "smartthings:Ecobee (Connect)",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	dynamicPage(name: "mainPage", title: "Setup Routines", uninstall: true, install: true) {
    	section(title: "Name for Mode/Routine/Program Handler") {
        	label title: "Name this Handler", required: true, defaultValue: "Mode|Routine|Program Handler"
        }
        
        section(title: "Select Thermostats") {
        	if(settings.tempDisable == true) {
            	paragraph "WARNING: Temporarily Disabled as requested. Turn back on to activate handler."
            } else {
        		input ("myThermostats", "capability.Thermostat", title: "Pick Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)            
			}
        }
            
        section(title: "Select ST Mode or Routine, or Ecobee Program") {
        	// Settings option for using Mode or Routine
            input(name: "modeOrRoutine", title: "Use Mode Change, Routine Execution or Ecobee Program: ", type: "enum", required: true, multiple: false, description: "Tap to choose...", metadata:[values:["Mode", "Routine", "Ecobee Program"]], submitOnChange: true)
		}
        
	        if (myThermostats?.size() > 0) {
            	if(settings.modeOrRoutine == "Mode") {
	    	    	// Start defining which Modes(s) to allow the SmartApp to execute in
                    // TODO: Need to run in all modes now and then filter on which modes were selected!!!
    	            //mode(title: "When Hello Mode(s) changes to: ", required: true)
                    section(title: "Modes") {
                    	input(name: "modes", type: "mode", title: "When Hello Mode(s) change to: ", description: "Tap to choose Modes...", required: true, multiple: true)
					}
                } else if(settings.modeOrRoutine == "Routine") {
                	// Routine based inputs
                    def actions = location.helloHome?.getPhrases()*.label
					if (actions) {
            			// sort them alphabetically
            			actions.sort()
						LOG("Actions found: ${actions}", 4)
						// use the actions as the options for an enum input
                        section(title: "Routines") {
							input(name:"action", type:"enum", title: "When these Routines execute: ", description: "Tap to choose Routines...", options: actions, required: true, multiple: true)
                        }
					} // End if (actions)
                } else if(settings.modeOrRoutine == "Ecobee Program") {
                	def programs = getEcobeePrograms()
                    programs = programs + ["Vacation"]
                    LOG("Found the following programs: ${programs}", 4) 
                    section(title: "Programs") {
                    	def n = myThermostats?.size()
                    	if (n > 1) paragraph("NOTE: It is recommended (but not required) to select only one thermostat when using Ecobee Programs to control SmartThings Modes or Routines")
                    	input(name: "ctrlProgram", title: "When Ecobee${n>1?'s':''} switch to Program: ", type: "enum", description: "Tap to choose Programs...", options: programs, required: true, multiple: true)
                    }
                }

				section(title: "Actions") {
                	if (settings.modeOrRoutine != "Ecobee Program") {
                		def programs = getEcobeePrograms()
                   		programs = programs + ["Resume Program"]
                		LOG("Found the following programs: ${programs}", 4)
                    
	               		input(name: "whichProgram", title: "Switch to this Ecobee Program: ", type: "enum", required: true, multiple:false, description: "Tap to choose...", options: programs, submitOnChange: true)
    	       	    	input(name: "fanMode", title: "Select a Fan Mode to use\n(Optional) ", type: "enum", required: false, multiple: false, description: "Tap to choose...", metadata:[values:["On", "Auto", "default"]], submitOnChange: true)
        	       		if(settings.whichProgram != "Resume Program") input(name: "holdType", title: "Select the Hold Type to use\n(Optional) ", type: "enum", required: false, multiple: false, description: "Tap to choose...", metadata:[values:["Until I Change", "Until Next Program", "default"]], submitOnChange: true)
            	   		input(name: "useSunriseSunset", title: "Also at Sunrise or Sunset?\n(Optional) ", type: "enum", required: false, multiple: true, description: "Tap to choose...", metadata:[values:["Sunrise", "Sunset"]], submitOnChange: true)                
                	} else {
                    	input(name: "runModeOrRoutine", title: "Change Mode or Execute Routine: ", type: "enum", required: true, multiple: false, description: "Tap to choose...", metadata:[values:["Mode", "Routine"]], submitOnChange: true)
                        if (settings.runModeOrRoutine == "Mode") {
    	                	input(name: "runMode", type: "mode", title: "Change Hello Mode to: ", required: true, multiple: false)
                		} else if (settings.runModeOrRoutine == "Routine") {
                			// Routine based inputs
                    		def actions = location.helloHome?.getPhrases()*.label
							if (actions) {
            					// sort them alphabetically
            					actions.sort()
								LOG("Actions found: ${actions}", 4)
								// use the actions as the options for an enum input
								input(name:"runAction", type:"enum", title: "Execute this Routine: ", options: actions, required: true, multiple: false)
							} // End if (actions)
                        } // End if (Routine)
                    } // End else Program --> Mode/Routine
                } // End of "Actions" section
            } // End if myThermostats size
            section(title: "Temporarily Disable?") {
            	input(name: "tempDisable", title: "Temporarily Disable Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        	}
        
        section (getVersionLabel())
    }
}

// Main functions
def installed() {
	LOG("installed() entered", 5)
	initialize()  
}

def updated() {
	LOG("updated() entered", 5)
	unsubscribe()
    initialize()
}

def initialize() {
	LOG("initialize() entered")
    if(tempDisable == true) {
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }
	
	if (settings.modeOrRoutine == "Routine") {
    	subscribe(location, "routineExecuted", changeProgramHandler)
    } else if (settings.modeOrRoutine == "Mode") {
    	subscribe(location, "mode", changeProgramHandler)
    } else { // has to be "Ecobee Program"
    	subscribe(myThermostats, "currentProgramName", changeSTHandler)
    }
   
    if(useSunriseSunset?.size() > 0) {
		// Setup subscriptions for sunrise and/or sunset as well
        if( useSunriseSunset.contains("Sunrise") ) subscribe(location, "sunrise", changeProgramHandler)
        if( useSunriseSunset.contains("Sunset") ) subscribe(location, "sunset", changeProgramHandler)
    }
    
	// Normalize settings data
    normalizeSettings()
    LOG("initialize() exiting")
}

// get the combined set of Ecobee Programs applicable for these thermostats
private def getEcobeePrograms() {
	def programs

	if (myThermostats?.size() > 0) {
		myThermostats.each { stat ->
        	def DNI = stat.device.deviceNetworkId
            LOG("Getting list of programs for stat (${stat}) with DNI (${DNI})", 4)
        	if (!programs) {
            	LOG("No programs yet, adding to the list", 5)
                programs = parent.getAvailablePrograms(stat)
            } else {
            	LOG("Already have some programs, need to create the set of overlapping", 5)
                programs = programs.intersect(parent.getAvailablePrograms(stat))
            }
        }
	} 
    LOG("getEcobeePrograms: returning ${programs}", 4)
    return programs
}

private def normalizeSettings() {
	if (settings.modeOrRoutine == "Ecobee Program") return		// no normalization required
    
	// whichProgram
	state.programParam = ""
	if (whichProgram != null && whichProgram != "") {
    	if (whichProgram == "Resume Program") {
        	state.doResumeProgram = true
        } else {        	
    		state.programParam = whichProgram
    	}
	}
    
    // fanMode
    state.fanCommand = ""
    if (fanMode != null && fanMode != "") {
    	if (fanMode == "On") {
        	state.fanCommand = "fanOn"
        } else if (fanMode == "Auto") {
        	state.fanCommand = "fanAuto"
        } else {
        	state.fanCommand = ""
        }
    }
    
    // holdType
    state.holdTypeParam = null
    if (holdType != null && holdType != "") {
    	if (holdType == "Until I Change") {
        	state.holdTypeParam = "indefinite"
        } else if (holdType == "Until Next Program") {
        	state.holdTypeParam = "nextTransition"
        } else {
        	state.holdTypeParam = null
        }
    }
    
	if (settings.modeOrRoutine == "Routine") {
    	state.expectedEvent = settings.action
    } else {
    	state.expectedEvent = settings.modes
    }
	LOG("state.expectedEvent set to ${state.expectedEvent}", 4)
}

def changeSTHandler(evt) {
	LOG("changeSTHandler() entered with evt: ${evt.name}: ${evt.value}", 5)
    
    // adjust because currentProgramName carries some informational baggage
    String newProgram = evt.value
    if (newProgram.startsWith("Hold: ")) { 
    	newProgram = newProgram.drop(6)
    } else if (newProgram.startsWith("Away ")) {
    	newProgram = newProgram.drop(5)
    }
    
    if (settings.ctrlProgram.contains(newProgram)) {
    	if (!state.ecobeeThatChanged) {
        	state.ecobeeThatChanged = evt.displayName
            state.ecobeeNewProgram = evt.value
        }
    	if (settings.runModeOrRoutine == "Mode") {
        	LOG("Changing Mode to ${settings.runMode} because ${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}",2,null,'info')
        	if (settings.myThermostats.size() == 1) { 
            	changeMode() 
            } else { 
            	// trick to avoid multiple calls if more than 1 thermostat - they could change simultaneously or within the next pollCycle
                // to avoid this delayed response, assign to only one thermostat
                parent.poll()
            	runIn(15, changeMode, [overwrite: true]) 
            }	
        } else {
        	LOG("Executing Routine ${settings.runAction} because ${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}",2,null,'info')
        	if (settings.myThermostats.size() == 1) { 
            	runRoutine() 
            } else {
            	parent.poll()
            	runIn(15, runRoutine, [overwrite: true]) 
            }
        }
    }
}

def changeMode() {
	if (settings.runMode != location.mode) { 	// only if we aren't already in the specified Mode
		if (state.ecobeeThatChanged) sendNotificationEvent("Changing Mode to ${settings.runMode} because ${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}")
    	location.mode(settings.runMode)
    }
    state.ecobeeThatChanged = null
}

def runRoutine() {
	if (state.ecobeeThatChanged) sendNotificationEvent("Executing Routine ${settings.runAction} because ${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}")
    state.ecobeeThatChanged = null
	location.helloHome?.execute(settings.runAction)
}

def changeProgramHandler(evt) {
	LOG("changeProgramHander() entered with evt: ${evt.name}: ${evt.value}", 5)
	
    def gotEvent 
    if (settings.modeOrRoutine == "Routine") {
    	gotEvent = evt.displayName?.toLowerCase()
    } else {
    	gotEvent = evt.value?.toLowerCase()
    }
    LOG("Event name received (in lowercase): ${gotEvent}  and current expected: ${state.expectedEvent}", 5)

    if ( !state.expectedEvent*.toLowerCase().contains(gotEvent) ) {
    	LOG("Received an mode/routine that we aren't watching. Nothing to do.", 4)
        return true
    }
    
    settings.myThermostats.each { stat ->
    	LOG("In each loop: Working on stat: ${stat}", 4, null, 'trace')
    	// First let's change the Thermostat Program
        if(state.doResumeProgram == true) {
        	LOG("Resuming Program for ${stat}", 4, null, 'trace')
            if (stat.currentValue("thermostatHold") == 'hold') {
            	def scheduledProgram = stat.currentValue("scheduledProgram")
        		stat.resumeProgram(true) // resumeAll to get back to the schedules program
				sendNotificationEvent("And I resumed the scheduled ${scheduledProgram} program on ${stat}.")
            }
        } else {
        	LOG("Setting Thermostat Program to programParam: ${state.programParam} and holdType: ${state.holdTypeParam}", 4, null, 'trace')
            
            boolean done = false
            def thermostatHold = stat.currentValue('thermostatHold')
            if (stat.currentValue('currentProgram') == state.programParam) {
            	if (thermostatHold == "") {
                	sendNotificationEvent("And I verified that ${stat} is already in the ${state.programParam} program.")
                    done = true
                }
            } else if (thermostatHold == 'hold') {
                if (stat.currentValue('scheduledProgram') == state.programParam) {
                    if (state.programParam == 'nextTransition') {
                    	stat.resumeProgram(true)	// resumeAll to get back to the originally scheduled program
                        sendNotificationEvent("And I resumed the scheduled ${state.programParam} on ${stat}.")
                        done = true
                    }
                }
            }
            if (!done) {    
        		stat.setThermostatProgram(state.programParam, state.holdTypeParam)
				sendNotificationEvent("And I set ${stat} to the ${state.programParam} program${(state.holdTypeParam!='nextTransition') ? ' indefinitely.' : '.'}")
            }
		}
        if (state.fanCommand != "" && state.fanCommand != null) stat."${state.fanCommand}"()
    }
    return true
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}
