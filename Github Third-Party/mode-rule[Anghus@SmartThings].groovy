/**
 *  Mode Rule
 *
 *  Copyright 2016 Jerry Honeycutt
 *
 *  Version 0.1   8 Dec 2016
 *
 *	Version History
 *
 *  0.1		08 Dec 2016		Initial version
 *  0.2		17 Mar 2017		Beefed up motion rules
 *  0.3		31 Jul 2018		Added delay to astro events
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
    name: "Mode Rule",
    namespace: "Anghus",
    author: "Jerry Honeycutt",
    description: "Change modes based upon presence devices, motion sensors, and time of day.",
    category: "My Apps",

    parent: "Anghus:Home Rules",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@3x.png")

/**/

preferences {
	page(name: "schedulePage")
    page(name: "presencePage")
	page(name: "motionPage")
	page(name: "switchPage")
    page(name: "actionsPage")
    page(name: "extrasPage")
	page(name: "notifyPage")
	page(name: "installPage")
}

def schedulePage() {
	dynamicPage(name: "schedulePage", nextPage: "presencePage", uninstall: true) {
    	section("START TIME") {
    		input name: "startType", type: "enum", title: "Time", options: ["Custom", "Sunrise", "Sunset"], submitOnChange: true, required: true
			switch(startType) {
            	case "Custom":
                	input name: "startTime", type: "time", title: "Custom time", required: true
                	break;
				case "Sunrise":
                case "Sunset":
					def sun = getSunriseAndSunset(zipCode: location.zipCode)
                    if(startType == "Sunrise")
						paragraph "Sunrise is ${sun.sunrise.format('hh:mm a', location.timeZone)} today."
					else
						paragraph "Sunset is ${sun.sunset.format('hh:mm a', location.timeZone)} today."
                	input name: "startOffset", type: "number", title: "Offset (+/- minutes)", range: "-60..60", defaultValue: 0, required: true                    
                    break;
            }
        }
    	section("FINISH TIME") {
    		input name: "finishType", type: "enum", title: "Time", options: ["Custom", "Sunrise", "Sunset"], submitOnChange: true, required: true
			switch(finishType) {
            	case "Custom":
                	input name: "finishTime", type: "time", title: "Custom time", required: true
                	break;
				case "Sunrise":
                case "Sunset":
					def sun = getSunriseAndSunset(zipCode: location.zipCode)
                    if(finishType == "Sunrise")
						paragraph "Sunrise is ${sun.sunrise.format('hh:mm a', location.timeZone)} today."
					else
						paragraph "Sunset is ${sun.sunset.format('hh:mm a', location.timeZone)} today."
                	input name: "finishOffset", type: "number", title: "Offset (+/- minutes)", range: "-60..60", defaultValue: 0, required: true
                    break;
            }
        }
        section("DELAY") {
            input name: "scheduleDelay", type: "number", title: "Delay (minutes)", defaultValue: 0, required: true
            paragraph "Evaluate rule after the specified time, canceling the rule if conditions change."
        }
    	section() {
			paragraph "This rule will be evaluated only between the start and finish times. Both are required."
		}
    }
}

def presencePage() {
	dynamicPage(name: "presencePage", nextPage: motionPage, uninstall: true) {
    	section("PRESENCE") {
			input name: "presenceSensors", type: "capability.presenceSensor", title: "People", multiple: true, submitOnChange: true, required: false
        	paragraph "Optionally, add presence sensors to the rule and define how they will affect its evaluation."
        }
		if(presenceSensors) {
            section("RULE") {
                input name: "presenceScope", type: "enum", title: "Scope", options: ["Any", "All"], defaultValue: "Any", required: true
                input name: "presenceComparison", type: "enum", title: "Comparison", options: ["Are", "Are not"], defaultValue: "Are", required: true
                input name: "presenceValue", type: "enum", title: "Presence", options: ["Present", "Not present"], defaultValue: "Present", required: true
			}
            section("DELAY") {
				input name: "presenceDelay", type: "number", title: "Delay (minutes)", defaultValue: 0, required: true
                paragraph "Evaluate presence sensors after the specified time, canceling the rule if conditions change."
            }
        }
	}
}

def motionPage() {
	dynamicPage(name: "motionPage", nextPage: "switchPage", uninstall: true) {
    	section("MOTION") {
			input name: "motionSensors", type: "capability.motionSensor", title: "Motion sensors", multiple: true, submitOnChange: true, required: false
        	paragraph "Optionally, add motion sensors to the rule and define how they will affect its evaluation."
        }
        if(motionSensors) {
        	section("RULE") {
                input name: "motionScope", type: "enum", title: "Scope", options: ["Any", "All"], defaultValue: "Any", required: true
                input name: "motionComparison", type: "enum", title: "Comparison", options: ["Are", "Are not"], defaultValue: "Are", required: true
                input name: "motionValue", type: "enum", title: "Motion", options: ["Active", "Inactive"], defaultValue: "Active", required: true
            	input name: "motionTimeout", type: "number", title: "Effective time (seconds)", defaultValue: 30, required: true
                paragraph "After activity, sensors will continue to indicate activity for the duration that Effective Time specifies."
            }
            section("DELAY") {
				input name: "motionDelay", type: "number", title: "Delay (minutes)", defaultValue: 0, required: true
                paragraph "Evaluate motion sensors after the specified time, canceling the rule if conditions change."
            }
        }
	}
}

def switchPage() {
	dynamicPage(name: "switchPage", nextPage: "actionsPage", uninstall: true) {
    	section("SWITCHES") {
			input name: "switches", type: "capability.switch", title: "Switches", multiple: true, submitOnChange: true, required: false
        	paragraph "Optionally, add switches to the rule and define how they will affect its evaluation."
        }
        if(switches) {
        	section("RULE") {
                input name: "switchScope", type: "enum", title: "Scope", options: ["Any", "All"], defaultValue: "Any", required: true
                input name: "switchComparison", type: "enum", title: "Comparison", options: ["Are", "Are not"], defaultValue: "Are", required: true
                input name: "switchValue", type: "enum", title: "Switch", options: ["On", "Off"], defaultValue: "Off", required: true
            }
            section("DELAY") {
				input name: "switchDelay", type: "number", title: "Delay (minutes)", defaultValue: 0, required: true
                paragraph "Evaluate switches after the specified time, canceling the rule if conditions change."
            }
        }
	}
}

def actionsPage() {
	dynamicPage(name: "actionsPage", nextPage: "notifyPage", uninstall: true) {
    	section("ACTIONS") {
        	input name: "targetMode", type: "mode", title: "Change mode to", required: true
            def actions = location.helloHome?.getPhrases()*.label
            if(actions) {
            	actions.sort()
            	input name: "targetRoutines", type: "enum", title: "Run these routines", options: actions, multiple: true, required: false
			}
            href page: "extrasPage", title: "Extra actions", description: "Configure lights, locks, and thermostats."
        }
    	section("MODES") {
        	input name: "restrictModes", type: "bool", title: "Limit to certain modes?", defaultvalue: false, submitOnChange: true, required: false
            if(restrictModes)
        		input name: "modeList", type: "mode", title: "Choose modes", multiple: true, required: true
				paragraph "Limiting these actions to certain modes is useful to control the order in which modes change (e.g., only setting the mode to Sleep if the current mode is Home Night)."
        }
        section("MESSAGES") {
            input name: "notifyOnRun", type: "bool", title: "Notify on run?", defaultValue: false, submitOnChange: true, required: true
            if(notifyOnRun) {
                input name: "actionsList", type: "enum", title: "Which actions?", options: ["mode", "routine"], defaultValue: ["mode", "routine"], multiple: true, required: true
                input name: "useCustomMessages", type: "bool", title: "Custom messages?", defaultValue: false, submitOnChange: true, required: true
                if (useCustomMessages) {
                    input name: "modeMessage", type: "text", title: "Mode message", defaultValue: "Changing mode.", required: true
                    input name: "routineMessage", type: "text", title: "Routine message", defaultValue: "Executing routine.", required: true
                }
            }
        }
    }
}

def extrasPage() {
	dynamicPage(name: "extrasPage", uninstall: false) {
        section("LIGHTS") {
        	input name: "lightsOn", type: "capability.switch", title: "Turn on these lights", multiple: true, required: false
            input name: "lightsOff", type: "capability.switch", title: "Turn off these lights", multiple: true, required: false
        }
        section("LOCKS") {
        	input name: "doorsToLock", type: "capability.lock", title: "Lock these doors", multiple: true, required: false
            input name: "doorsToUnlock", type: "capability.lock", title: "Unlock these doors", multiple: true, required: false
        }
        section("THERMOSTAT") {
        	input name: "thermostats", type: "capability.thermostat", title: "Set these thermostats", multiple: true, submitOnChange: true, required: false
            if(thermostats) {
            	input name: "thermostatMode", type: "enum", title: "Mode?", options: ["auto", "cool", "heat", "off"], required: false
            	input name: "heatingTemp", type: "number", title: "When heating", range: "60..80", required: false
            	input name: "coolingTemp", type: "number", title: "When cooling", range: "65..85", required: false
            }
        }
    }
}

def notifyPage() {
	dynamicPage(name: "notifyPage", nextPage: "installPage", uninstall: true) {
    	section("NOTIFICATIONS") {
            input name: "sendPush", type: "bool", title: "Send a push notification?", defaultValue: false, required: true
        }
        section("TEXT MESSAGES") {
        	input name: "sendText", type: "bool", title: "Send a text message?", defaultValue: false, submitOnChange: true, required: true
            if (sendText) {
            	input name: "phoneNumber", type: "phone", title: "Phone number", required: true
			}
        }
    }
}

def installPage() {
	dynamicPage(name: "installPage", uninstall: true, install: true) {
    	section("NAME") {
        	label title: "Rule name", defaultValue: targetMode, required: false
        }
        section("DEBUG") {
        	input name: "debug", type: "bool", title: "Debug rule?", defaultValue: false, required: true
        }
    }
}

/**/

def installed() {
	trace("installed()")
	initialize()
}

def updated() {
	trace("updated()")
    unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	trace("initialize()")
	setupSchedule()
	if(presenceSensors) subscribe(presenceSensors, "presence", presenceEvent)
	if(motionSensors) subscribe(motionSensors, "motion", motionEvent)
	if(switches) subscribe(switches, "switch", switchEvent)
    
    state.motionScheduled = false
    state.presenceScheduled = false
    state.switchScheduled = false
}

def setupSchedule() {
	trace("setupSchedule()")

	// Setup custom schedules, if specified.

	if(startType == "Custom")  {
    	schedule(startTime, startCallback);
        state.startTime = startTime
        debug("Scheduling the custom start time $state.startTime")
	}

	if(finishType == "Custom") {
        state.finishTime = finishTime
        debug("Scheduling the custom finish time $state.finishTime")
    }

	// Schedule astronomical events daily, and
    // go ahead and pick up any missed events.

    schedule("0 0 0 1/1 * ? *", astroCheck)
	astroCheck()
}

/**/

def startCallback()  {
	trace("startCallback()")
    debug("Evaluation runs in $scheduleDelay minutes")
    runIn(scheduleDelay * 60, evaluateRule, [overwrite: true])
}

def finishCallback() {
	trace("finishCallback()")
    debug("Evaluation runs in $scheduleDelay minutes")
    runIn(scheduleDelay * 60, evaluateRule, [overwrite: true])
}

def astroCheck() {
	trace("astroCheck()")

    def now = new Date()
	def sun = getSunriseAndSunset(zipCode: location.zipCode)
    def sunrise = sun.sunrise
    def sunset = sun.sunset

	// Missed today? Schedule tomorrow's sunrise/sunset events.

	if(sunrise.before(now)) sunrise = sunrise.next()
	if(sunset.before(now)) sunset = sunset.next()

	// Schedule the starting astro event for the schedule, if any.

	if(startType == "Sunrise") {
    	def timeWithOffset = new Date(sunrise.time + (startOffset * 60000))
    	runOnce(timeWithOffset, evaluateRule, [overwrite: false])
        state.startTime = timeWithOffset.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
        debug("Scheduling a sunrise start time at $state.startTime")
    }
    else
    	if(startType == "Sunset") {
            def timeWithOffset = new Date(sunset.time + (startOffset * 60000))
            runOnce(timeWithOffset, evaluateRule, [overwrite: false])
            state.startTime = timeWithOffset.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
            debug("Scheduling a sunset start time at $state.startTime")
        }

	// Schedule the finishing astro event for the schedule, if any.

	if(finishType == "Sunrise") {
    	def timeWithOffset = new Date(sunrise.time + (finishOffset * 60000))
        state.finishTime = timeWithOffset.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
        debug("Scheduling a sunrise finish time at $state.finishTime")
    }
    else
    	if(finishType == "Sunset") {
            def timeWithOffset = new Date(sunset.time + (finishOffset * 60000))
            state.finishTime = timeWithOffset.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
            debug("Scheduling a sunset finish time at $state.finishTime")
        }
}

def evaluateSchedule() {
	def now = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
	return timeOfDayIsBetween(state.startTime, state.finishTime, now, location.timeZone)
}

/**/

def presenceEvent(evt) {
	trace("presenceEvent($evt.value})")

	if(evt.isStateChange && evaluateSchedule() && isNotChanged()) {

		// Don't bother if we're not in the rule's schedule.

    	if(evaluatePresence()) {
            debug("Presence evaluation runs in $presenceDelay minutes")
			runIn(presenceDelay * 60, handlePresence, [overwrite: true])
            state.presenceScheduled = true
		}
        else {
        	if(presenceScheduled) {
                debug("Presence evaluation unscheduled")
                unschedule(handlePresence)
                state.presenceScheduled = false
            }
		}
    }
}

def handlePresence() {
	trace("handlePresence()")
	state.presenceScheduled = false
    evaluateRule()
}

def evaluatePresence() {
	trace("evaluatePresence()")

	def result = true
	if(presenceSensors) {
        def all = true
        def any = false

        presenceSensors.each {

            // Any matching value will change $any to true.
			// Any non-matching value will change $all to false.

            any |= (it.currentValue("presence") == presenceValue.toLowerCase())
            all &= (it.currentValue("presence") == presenceValue.toLowerCase())
        }

		// Based on the defined rule, pick the results we need.

        if(presenceScope == "All")
            result = (presenceComparison == "Are") ? all : !all
        else
            result = (presenceComparison == "Are") ? any : !any

		debug("Presence rule '${presenceScope.toLowerCase()} ${presenceComparison.toLowerCase()} ${presenceValue.toLowerCase()}' is $result")
	}

	return result
}

/**/

def motionEvent(evt) {
	trace("motionEvent($evt.value})")

    // Don't bother if we're not in the rule's schedule.

	if(evt.isStateChange && evaluateSchedule() && isNotChanged())
		scheduleMotion()
}

def scheduleMotion() {
	trace("scheduleMotion()")

    if(evaluateMotion()) {
        if(!state.motionScheduled) {
            debug("Motion evaluation runs in $motionDelay minutes")
            runIn(motionDelay * 60, handleMotion, [overwrite: true])
            state.motionScheduled = true
        }
    }
    else {

        // Don't let a brief moment of inactivity disrupt activity detection
        // by completely unscheduling the evaluation. It's OK to interrupt
        // inactivity detection by unscheduling the evaluation after activity.

        if(state.motionScheduled && motionValue.toLowerCase() == "inactive") {
            debug("Motion evaluation unscheduled")
            unschedule(handleMotion)
            state.motionScheduled = false
        }
    }
}

def handleMotion() {
	trace("handleMotion()")
	state.motionScheduled = false
	evaluateRule()
}

def evaluateMotion() {
	trace("evaluateMotion()")

	def result = true
	if(motionSensors) {
        def all = true
        def any = false

        motionSensors.each {

			def events = it.eventsSince(new Date(now() - motionTimeout * 1000))
			def motionEvents = events?.findAll {it.value == "active"}.size() > 0
			def recentMotion = motionEvents ? "active" : "inactive"

			if(motionEvents && it.currentValue("motion") == "inactive") {

				// Handle the scenario where motion stopped but there is recent motion.
				// If motion just stopped, we want to have a look in a bit to check again.

                runIn(motionTimeout + 1, scheduleMotion, [overwrite: true])
				debug("$it.label was recently active but just changed states.")
				debug("Evaluating $it.label again after $motionTimeout seconds.")
			}

            // Any matching value will change $any to true.
			// Any non-matching value will change $all to false.

            any |= (recentMotion == motionValue.toLowerCase())
            all &= (recentMotion == motionValue.toLowerCase())
        }

		// Based on the defined rule, pick the results we need.

        if(motionScope == "All")
            result = (motionComparison == "Are") ? all : !all
        else
            result = (motionComparison == "Are") ? any : !any

		debug("Motion rule '${motionScope.toLowerCase()} ${motionComparison.toLowerCase()} ${motionValue.toLowerCase()}' is $result")
	}

	return result
}

/**/

def switchEvent(evt) {
	trace("switchEvent($evt.value})")

	if(evt.isStateChange && evaluateSchedule() && isNotChanged()) {

		// Don't bother if we're not in the rule's schedule.

    	if(evaluateSwitch()) {
            debug("Switch evaluation runs in $switchDelay minutes")
			runIn(switchDelay * 60, handleSwitch, [overwrite: true])
            state.switchScheduled = true
		}
        else {
        	if(state.switchScheduled) {
                debug("Switch evaluation unscheduled")
                unschedule(handleSwitch)
                state.switchScheduled = false
            }
		}
    }
}

def handleSwitch() {
	trace("handleSwitch()")
	state.switchScheduled = false
    evaluateRule()
}

def evaluateSwitch() {
	trace("evaluateSwitch()")

	def result = true
	if(switches) {
        def all = true
        def any = false

        switches.each {

            // Any matching value will change $any to true.
			// Any non-matching value will change $all to false.

            any |= (it.currentValue("switch") == switchValue.toLowerCase())
            all &= (it.currentValue("switch") == switchValue.toLowerCase())
        }

		// Based on the defined rule, pick the results we need.

        if(switchScope == "All")
            result = (switchComparison == "Are") ? all : !all
        else
            result = (switchComparison == "Are") ? any : !any

		debug("Switch rule '${switchScope.toLowerCase()} ${switchComparison.toLowerCase()} ${switchValue.toLowerCase()}' is $result")
	}

	return result
}

/**/

def evaluateRule() {
	trace("evaluateRule()")

	def now = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)

	if(evaluateSchedule()) {
    	if(evaluatePresence()) {
            if(evaluateMotion()) {
            	if(evaluateSwitch()) {
                    if(!restrictModes || location.mode in modeList) {

                        // The current time is within our schedule.
                        // Modes aren't restricted or we're in a chosen mode.
                        // Therefore, run the actions this rule defines.

                        if(targetMode) changeMode()
                        if(targetRoutines) execRoutines()
                        execExtraActions()
                    }
                    else
                        debug("Mode $location.mode is not in $modeList")
				}
                else
                	debug("The switch rule prevented execution")
            }
            else
                debug("The motion rule prevented execution")
		}
        else
        	debug("The presence rule prevented execution")
    }
    else
    	debug("Time is not between start and finish")
}

def changeMode() {
	trace("changeMode()")

    if(isNotChanged()) {
        if(location.modes?.find{it.name == targetMode}) {

            // Our target mode is valid and not already set,
            // so change the mode and notify the user.

            setLocationMode(targetMode)
            if("mode" in actionsList)
            	notify(notifyOnRun, useCustomMessages, modeMessage, "Setting mode to $targetMode.")                
        }
        else
            debug("Mode '$targetMode' is not defined")
    }
    else
        debug("Mode is already '$targetMode'")
}

def execRoutines() {
	trace("execRoutines()")

    targetRoutines.each {
        location.helloHome?.execute(it)
        if("routine" in actionsList)
			notify(notifyOnRun, useCustomMessages, routineMessage, "Executing the routine $it.label.")                
    }
}

def execExtraActions() {
	trace("execExtraActions()")

	if(lightsOn)      { debug("Turning on $lightsOn");     lightsOn.each { it.on() }}
	if(lightsOff)     { debug("Turning off $lightsOff");   lightsOff.each { it.off() }}
	if(doorsToLock)   { debug("Locking $doorsToLock");     doorsToLock.each { it.lock() }}
    if(doorsToUnlock) { debug("Unlocking $doorsToUnlock"); doorsToUnlock.each { it.unlock() }}
    
    thermostats.each {
    	debug("Setting $it.label")
    	if(thermostatMode) { it.setThermostatMode(thermostatMode); debug("Mode is $thermostatMode") }
        if(heatingTemp)    { it.setHeatingSetpoint(heatingTemp);   debug("Heating temp is $heatingTemp") }
        if(coolingTemp)    { it.setCoolingSetpoint(coolingTemp);   debug("Cooling temp is $coolingTemp") }
    }
}

/**/

def isNotChanged() {
	return targetMode && targetMode != location.mode
}

/**/

private notify(enabled, useCustom, customText, defaultText) {
	if(enabled) {
    	def message = useCustom ? customText : defaultText
		if(sendPush) sendPush(message)
		if(sendText) sendSms(phoneNumber, message)
    }
    log.info(defaultText)
}

private debug(message) {
	if(true)
    	log.debug(message)
}

private trace(function) {
	if(true)
    	log.trace(function)
}
