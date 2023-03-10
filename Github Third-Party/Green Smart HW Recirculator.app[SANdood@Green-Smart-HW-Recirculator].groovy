/**
 *  Green Smart HW Recirculator
 *
 *  Copyright 2014 Barry A. Burke
 *
 *
 * For usage information & change log: https://github.com/SANdood/Green-Smart-HW-Recirculator
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * CHANGE LOG
 * **********
 * 2018.03.06 -	Fixed timed off (argument missing in call to secondsPast())
 * 2018.11.28 - Added configurable "minimum time between turning on"
 * 2018.11.28a- Fixed type conversion anomoly
 *
 */
def getVersionNum() { return "2018.11.29" }
private def getVersionLabel() { return "Green Smart Hot Water Recirculator, v${getVersionNum()}" }
def getDebug() { false }
 
definition(
	name:		"Green Smart HW Recirculator",
	namespace: 	"sandood",
	author: 	"Barry A. Burke",
	description: "Intelligent event-driven optimizer for whole house Hot Water recirculation system.",
	category: 	"Green Living",
	iconUrl: 	"https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
	iconX2Url:	"https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png"
)

preferences {
	page( name: "setupApp" )
}

def setupApp() {
	dynamicPage(name: "setupApp", title: versionLabel, install: true, uninstall: true) {

		section("HW Recirculator") {
			input name: "recircSwitch", type: "capability.switch", title: "Recirculator switch?", multiple: false, required: true
			input name: "recircMomentary", type: "bool", title: "Is this a momentary switch?", required: true, defaultValue: true, refreshAfterSelection: true, submitOnChange: true
			if (!recircMomentary) {
				input name: "timedOff", type: "bool", title: "Timed off?", defaultValue: false, required: true, refreshAfterSelection: true, submitOnChange: true
				if (timedOff) {
					input name: "offAfterMinutes", type: "number", title: "On for how many minutes (1-60)?", required: true, defaultValue: 1, range: "1..60", multiple: false
				}
			}
            input name: "minTimeBetween", type: "number", title: "Minimum minutes between activations (1-120)?", required: true, defaultValue: 1, range: "1..120", multiple: false
		}

		section("Recirculator Activation events:") {

			input name: "useTargetTemp", type: "bool", title: "On using target temperature?", required: true, defaultValue: false, refreshAfterSelection: true, submitOnChange: true
			if (useTargetTemp) {
				input name: "targetThermometer", type: "capability.temperatureMeasurement", title: "Use this thermometer", multiple: false, required: true
				input name: "targetTemperature", type: "number", title: "Target temperature", defaultValue: 105, required: true
				input name: "targetOff", type: "bool", title: "Off at target temp?", defaultValue: true
				input name: "targetOn", type: "bool", title: "On when below target?", defaultValue: false, refreshAfterSelection: true, submitOnChange: true
				if (!targetOff && !targetOn) { settings.useTargetTemp = false }
				if (targetOn) {
					input name: "targetSwing", type: "number", title: "Below by this many degrees:", defaultValue: 5, required: true
				}
			}
            
            paragraph ""
			input name: "useTimer", type: "bool", title: "On using a schedule?", defaultValue: false, refreshAfterSelection: true, submitOnChange: true
			if (useTimer) {
				input name: "onEvery", type: "number", title: "On every XX minutes", defaultValue: 15, required: true
			}
			
            paragraph ""
			input name: "motionActive", type: "capability.motionSensor", title: "On when motion is detected here", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.motionActivate && !settings.recircMomentary) {
				input name: "motionInactive", type: "bool", title: "Off when motion stops?", defaultValue: true
			}

			paragraph ""
			input name: "contactOpens", type: "capability.contactSensor", title: "On when any of these things open", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.contactOpens && !settings.recircMomentary) {
				input name: "openCloses", type: "bool", title: "Off when they re-close?", defaultValue: false
			}
			
			paragraph ""
			input name: "contactCloses", type: "capability.contactSensor", title: "On when any of these things close", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.contactCloses && !settings.recircMomentary) {
				input name: "closedOpens", type: "bool", title: "Off when they re-open?", defaultValue: false
			}

			paragraph ""
			input name: "switchedOn", type: "capability.switch", title: "On when any switch is turned on", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.switchedOn && !settings.recircMomentary) {
				input name: "onSwitchedOff", type: "bool", title: "Off when turned off?", defaultValue: false
			}
			
			paragraph ""
			input name: "switchedOff", type: "capability.switch", title: "On when any switch is turned off", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.switchedOff && !settings.recircMopomentary) {
				input name: "offSwitchedOn", type: "bool", title: "Off when turned on?", defaultValue: false
			}
			
			paragraph ""
			input name: "somethingMoved", type: "capability.accelerationSensor", title: "On when any of these things move", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.somethingMoved && !settings.recircMomentary) {
				input name: "stoppedMoving", type: "bool", title: "Off when they stop?",  defaultValue: false
			}
            
            if ( true ) { // settings.recircMomentary || settings.timedOff) {			// we don't have an "off" condition for powerMeters
            	paragraph ""
            	input name: "powerChanged", type: "capability.powerMeter", title: "On when power changes on any of these", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
                if (settings.powerChanged) {
                	input name: "minPower", type: "decimal", title: "Minimum power level?", defaultValue: 0, required: false
                    input name: "maxPower", type: "decimal", title: "Maximum power level?", defaultValue: 1000, required: false
                }
			}
            
			paragraph ""
			input name: "modeOn",  type: "mode", title: "Enable only in specific mode(s)?", multiple: true, required: false
		}
		
		section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
//			mode title: "Set for specific mode(s)", required: false
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	log.debug "Initializing"

	atomicState.keepOffNow = false 
    atomicState.lastOnTime = 0

	if (modeOn) {
    	subscribe( location, locationModeHandler)
        if (location.currentMode in modeOn) {
        	atomicState.keepOffNow == false
        }
        else { atomicState.keepOffNow = true }
	}
    
	if (useTargetTemp) { subscribe( targetThermometer, "temperature", tempHandler) }

	if (motionActive) {
		subscribe( motionActive, "motion.active", onHandler)
		if (motionInactive) { subscribe( motionActive, "motion.inactive", offHandler) }
	}

	if (contactOpens) {
		subscribe( contactOpens, "contact.open", onHandler)
		if (openCloses) { subscribe( contactOpens, "contact.close", offHandler ) }
	}

	if (contactCloses) {
		subscribe( contactCloses, "contact.close", onHandler)
		if (closedOpens) { subscribe( contactCloses, "contact.open", offHandler ) }
	}

	if (switchedOn) {
		subscribe( switchedOn, "switch.on", onHandler)
		if (onSwitchedOff) { subscribe( switchedOn, "switch.off", offHandler ) }
	}
	
	if (switchedOff) {
		subscribe( switchedOff, "switch.off", onHandler)
		if (offSwitchedOn) { subscribe( switchedOff, "switch.on", onHandler ) }
	}
	
	if (somethingMoved) {
		subscribe( somethingMoved, "acceleration.active", onHandler)
		if (stoppedMoving) { subscribe( somethingMoved, "acceleration.inactive", offHandler) }
	}
    
    if (powerChanged) {
    	subscribe( powerChanged, "power", powerHandler )
        if (settings.minPower > settings.maxPower) {
        	settings.minPower = 0
            settings.maxPower = 1000
        }
    }

    if ( !state.keepOffNow) {
    	if ( useTimer ) { 
    		schedule("0 */${onEvery} * * * ?", "turnItOn")
    	}
    	turnItOn()
	}
	else {
		turnItOff()
	}
}

def powerHandler(evt) {
	log.trace "powerHandler() ${evt.device?.label} ${evt.name}: ${evt.value}"
    
    def newPower = 0.0
    if (evt.value.isNumber()) {
	    try {
    		newPower = evt.numberValue
    	} catch (e) {
    		newPower = 0.0
    	}
    }
    if (settings.minPower?.isNumber() && (settings.minPower >= 0.0)) {
    	if (newPower > settings.minPower) {
        	if (settings.maxPower?.isNumber() && (settings.maxPower > 0.0)) {
            	if (newPower <= settings.maxPower) {
                	if (debug) log.trace "${minPower} <= newPower <= ${maxPower}"
        			turnItOn()
            	} else {
                	if (debug) log.trace "newPower > ${maxPower}, skipping..."
                }
            } else {
            	// No max power, just turn it on
                if (debug) log.trace "${minPower} <= newPower"
                turnItOn()
            }
        } else {
        	if (debug) log.trace "newPower < ${minPower}, skipping..."
        }
    } else {
    	// No power constraints, just turn it on
        if (debug) log.trace "No power constraints"
    	turnItOn()
    }
}

def tempHandler(evt) {
	log.trace "tempHandler() ${evt.device?.label} ${evt.name}: ${evt.value}"
    
    if (targetOff) {
    	if (evt.integerValue >= targetTemperature) { turnItOff() }
    }
    
    if (targetOn) {
    	if ( evt.integerValue < targetTemperature) { turnItOn() }
    }
}

def onHandler(evt) {
	log.trace "onHandler() ${evt.device?.label} ${evt.name}: ${evt.value}"

	turnItOn()
}
         
def turnItOn() { 
    if (atomicState.keepOffNow) { return }				// we're not supposed to turn it on right now
    
    def minSeconds = minTimeBetween?.isNumber() ? (minTimeBetween.toInteger() * 60) : 60
    def turnOn = secondsPast( atomicState.lastOnTime, minSeconds )  // limit sending On commands to 1 per minute max (reduces network loads)
    if (turnOn && timedOff) {
    	turnOn = secondsPast( atomicState.lastOnTime, (offAfterMinutes * 60) )	// Wait longer if we are using timedOff
    }
    
    if (turnOn && useTargetTemp) {							// only turn it on if not hot enough yet
    	if (targetThermometer.currentTemperature >= targetTemperature) { turnOn = false }
    }
    
    if (turnOn) {
    	log.info "Turning on"
		if (!recircMomentary) {
			if (recircSwitch.currentSwitch != "on") { recircSwitch.on() }
		}
    	else { recircSwitch.on() }

		atomicState.lastOnTime = new Date().time
    
    	if (!recircMomentary) {
    		if (timedOff) {
    			runIn((offAfterMinutes * 60), "turnItOff", [overwrite: true])
        	} else {
            	runIn(2, "turnItOff", [overwrite: true])
            }
        }
    } else {
    	log.info "Skipping..."
    }
}

def offHandler(evt) {
	log.trace "offHandler() ${evt.device?.label} ${evt.name}: ${evt.value}"

    turnItOff()
}

def turnItOff() {

	def turnOff = true
    if (useTargetTemp) {						// only turn it off if it's hot enough
    	if (targetThermometer.currentTemperature < targetTemperature) { turnOff = false }
    }

	if (turnOff) {
        if (!recircMomentary) { unschedule( "turnItOff" ) }				// delete any other pending off schedules
		if (recircSwitch.currentSwitch != "off" ) {
        	log.info "Turning off"
        	recircSwitch.off() // avoid superfluous off()s
        }
    }
}

def locationModeHandler(evt) {
	log.trace "locationModeHandler() ${evt.name}: ${evt.value}"
    
	if (modeOn) {
        if (evt.value in modeOn) {
        	atomicState.keepOffNow = false
            atomicState.lastOnTime = 0
        	log.trace "Enabling GSHWR"
        	sendNotificationEvent ( "Plus, I enabled ${recircSwitch.displayName}" )

    		if (useTimer) {
    			unschedule( "turnItOn" )											// stop any lingering schedules
        		schedule("0 */${onEvery} * * * ?", "turnItOn")        	// schedule onHandler every $onEvery minutes 
    		}
            turnItOn()													// and turn it on to start the day!
            runIn( 63, "turnItOn", [overwrite: true])					// belt & suspenders - atomicState isn't always "atomic"
		}
        else {
			log.trace "Disabling GSHWR"
            sendNotificationEvent ( "Plus, I disabled ${recircSwitch.displayName}" )
        	if (useTimer) { unschedule( "turnItOn" ) }					// stop timed on schedules
    		if (!recircMomentary) { unschedule( "turnItOff" ) }					// delete any pending off schedules
			turnItOff()													// Send one final turn-off 
            atomicState.keepOffNow = true								// make sure nobody turns it on again
		}
    }
}

//check last message so thermostat poll doesn't happen all the time
private Boolean secondsPast(timestamp, seconds=1) {
	if (!(timestamp instanceof Number)) {
		if (timestamp instanceof Date) {
			timestamp = timestamp.time
		} else if ((timestamp instanceof String) && timestamp.isNumber()) {
			timestamp = timestamp.toLong()
		} else {
			return true
		}
	}
	return (new Date().time - timestamp) > (seconds * 1000)
}
