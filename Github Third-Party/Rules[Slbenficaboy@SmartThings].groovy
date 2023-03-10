/**
 *  Rule
 *
 *  Copyright 2015 Bruce Ravenel
 *
 *  Version 1.108  16 Nov 2015
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
    name: "Rule",
    namespace: "bravenel",
    author: "Bruce Ravenel",
    description: "Rule",
    category: "Convenience",
    parent: "bravenel:Rule Machine",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
	page(name: "selectRule")
	page(name: "selectConditions")
	page(name: "defineRule")
	page(name: "certainTime")
	page(name: "atCertainTime")
	page(name: "selectActionsTrue")
	page(name: "selectActionsFalse")
	page(name: "selectMsgTrue")
	page(name: "selectMsgFalse")
}

def selectRule() {
	dynamicPage(name: "selectRule", title: "Select Conditions, Rule and Results", uninstall: true, install: true) {
		section() {     
			label title: "Name the Rule", required: true
			def condLabel = conditionLabel()
			if (condLabel) condLabel = condLabel[0..-2]
			href "selectConditions", title: "Define Conditions", description: condLabel ? (condLabel) : "Tap to set", required: true, state: condLabel ? "complete" : null
			href "defineRule", title: "Define the Rule", description: state.str ? (state.str) : "Tap to set", state: state.str ? "complete" : null
			href "selectActionsTrue", title: "Select the Actions for True", description: state.actsTrue ? state.actsTrue : "Tap to set", state: state.actsTrue ? "complete" : null
			href "selectActionsFalse", title: "Select the Actions for False", description: state.actsFalse ? state.actsFalse : "Tap to set", state: state.actsFalse ? "complete" : null
		}
		section() {mode(title: "Restrict to specific mode(s)")}
	}
}

def selectConditions() {
    def ct = settings.findAll{it.key.startsWith("rCapab")}
    state.howMany = ct.size() + 1
    def howMany = state.howMany
	dynamicPage(name: "selectConditions", title: "Select Conditions", uninstall: false) {
		if(howMany) {
			for (int i = 1; i <= howMany; i++) {
				def thisCapab = "rCapab$i"
				section("Condition #$i") {
					getCapab(thisCapab)
					def myCapab = settings.find {it.key == thisCapab}
					if(myCapab) {
						def xCapab = myCapab.value
						if(!(xCapab in ["Time of day", "Certain Time", "Days of week", "Mode"])) {
							def thisDev = "rDev$i"
							getDevs(xCapab, thisDev)
							def myDev = settings.find {it.key == thisDev}
							if(myDev) if(myDev.value.size() > 1) getAnyAll(thisDev)
							if(xCapab in ["Temperature", "Humidity", "Illuminance", "Dimmer level", "Energy meter", "Power meter", "Battery"]) getRelational(thisDev)
						}
						getState(xCapab, i)
					}
				}
			}
		}
	}
}

def stripBrackets(str) {
	def i = str.indexOf('[')
	def j = str.indexOf(']')
	def result = str.substring(0, i) + str.substring(i + 1, j) + str.substring(j + 1)
	return result
}

def setActTrue(dev, str) {
	if(dev) state.actsTrue = state.actsTrue + stripBrackets("$str") + "\n"
}

def addToActTrue(str) {
	state.actsTrue = state.actsTrue + str + "\n"
}

def setActFalse(dev, str) {
	if(dev) state.actsFalse = state.actsFalse + stripBrackets("$str") + "\n"
}

def addToActFalse(str) {
	state.actsFalse = state.actsFalse + str + "\n"
}

def selectActionsTrue() {
	dynamicPage(name: "selectActionsTrue", title: "Select Actions for True", uninstall: false) {
		state.actsTrue = ""
		section("") {
			input "onSwitchTrue", "capability.switch", title: "Turn on these switches", multiple: true, required: false, submitOnChange: true
			setActTrue(onSwitchTrue, "On: $onSwitchTrue")
			input "offSwitchTrue", "capability.switch", title: "Turn off these switches", multiple: true, required: false, submitOnChange: true
			setActTrue(offSwitchTrue, "Off: $offSwitchTrue")
			input "delayedOffTrue", "capability.switch", title: "Turn off these switches after a delay", multiple: true, required: false, submitOnChange: true
			if(delayedOffTrue) input "delayMinutesTrue", "number", title: "Minutes of delay", required: true, range: "1..*", submitOnChange: true
			if(delayMinutesTrue) {
				def delayStr = "Delayed Off: $delayedOffTrue: $delayMinutesTrue minute"
				if(delayMinutesTrue > 1) delayStr = delayStr + "s"
				setActTrue(delayedOffTrue, delayStr)
			}
			input "pendedOffTrue", "capability.switch", title: "Turn off these switches after a delay, pending cancellation", multiple: true, required: false, submitOnChange: true
			if(pendedOffTrue) input "pendMinutesTrue", "number", title: "Minutes of delay", required: true, range: "1..*", submitOnChange: true
			if(pendMinutesTrue) {
				def pendStr = "Pending Off: $pendedOffTrue: $pendMinutesTrue minute"
				if(pendMinutesTrue > 1) pendStr = pendStr + "s"
				setActTrue(pendedOffTrue, pendStr)
			}
			input "dimATrue", "capability.switchLevel", title: "Set these dimmers", multiple: true, submitOnChange: true, required: false
			if(dimATrue) input "dimLATrue", "number", title: "To this level", range: "0..100", required: true, submitOnChange: true
			if(dimLATrue) setActTrue(dimATrue, "Dim: $dimATrue: $dimLATrue")
			input "dimBTrue", "capability.switchLevel", title: "Set these other dimmers", multiple: true, submitOnChange: true, required: false
			if(dimBTrue) input "dimLBTrue", "number", title: "To this level", range: "0..100", required: true, submitOnChange: true
			if(dimLBTrue) setActTrue(dimBTrue, "Dim: $dimBTrue: $dimLBTrue")
			input "lockTrue", "capability.lock", title: "Lock these locks", multiple: true, required: false, submitOnChange: true
			setActTrue(lockTrue, "Lock: $lockTrue")
			input "unlockTrue", "capability.lock", title: "Unlock these locks", multiple: true, required: false, submitOnChange: true
			setActTrue(unlockTrue, "Unlock: $unlockTrue")
			input "modeTrue", "mode", title: "Set the mode", multiple: false, required: false, submitOnChange: true
			if(modeTrue) addToActTrue("Mode: $modeTrue")
			def phrases = location.helloHome?.getPhrases()*.label
			input "myPhraseTrue", "enum", title: "Routine to run", required: false, options: phrases.sort(), submitOnChange: true
			if(myPhraseTrue) addToActTrue("Routine: $myPhraseTrue")
			href "selectMsgTrue", title: "Send message", description: state.msgTrue ? state.msgTrue : "Tap to set", state: state.msgTrue ? "complete" : null
			if(state.msgTrue) addToActTrue(state.msgTrue)
            input "delayTrue", "number", title: "Delay the effect of this rule by this many minutes", required: false, submitOnChange: true
            if(delayTrue) {
            	def delayStr = "Delay Rule: $delayTrue minute"
                if(delayTrue > 1) delayStr = delayStr + "s"
            	addToActTrue(delayStr)
            }
			
			//My Code
			input "onDoorOpen", "capability.garageDoorControl", title: "Open these doors", multiple: true, required: false, submitOnChange: true
			setActTrue(onDoorOpen, "Open: $onDoorOpen")
			input "onDoorClose", "capability.garageDoorControl", title: "Close these doors", multiple: true, required: false, submitOnChange: true
			setActTrue(onDoorClose, "Close: $onDoorClose")
			input "pendedCloseTrue", "capability.garageDoorControl", title: "Close these doors after a delay", multiple: true, required: false, submitOnChange: true
			if(pendedCloseTrue) input "pendDoorMinutesTrue", "number", title: "Minutes of delay", required: true, range: "1..*", submitOnChange: true
			if(pendDoorMinutesTrue) {
				def pendDoorStr = "Pending Off: $pendedCloseTrue: $pendDoorMinutesTrue minute"
				if(pendDoorMinutesTrue > 1) pendDoorStr = pendDoorStr + "s"
				setActTrue(pendedCloseTrue, pendDoorStr)
			}
		}
        if(state.actsTrue) state.actsTrue = state.actsTrue[0..-2]
	}
}

def selectActionsFalse() {
	dynamicPage(name: "selectActionsFalse", title: "Select Actions for False", uninstall: false) {
		state.actsFalse = ""
		section("") {
			input "onSwitchFalse", "capability.switch", title: "Turn on these switches", multiple: true, required: false, submitOnChange: true
			setActFalse(onSwitchFalse, "On: $onSwitchFalse")
			input "offSwitchFalse", "capability.switch", title: "Turn off these switches", multiple: true, required: false, submitOnChange: true
			setActFalse(offSwitchFalse, "Off: $offSwitchFalse")
			input "delayedOffFalse", "capability.switch", title: "Turn off these switches after a delay", multiple: true, required: false, submitOnChange: true
			if(delayedOffFalse) input "delayMinutesFalse", "number", title: "Minutes of delay", required: true, range: "1..*", submitOnChange: true
			if(delayMinutesFalse) {
				def delayStr = "Delayed Off: $delayedOffFalse: $delayMinutesFalse minute"
				if(delayMinutesFalse > 1) delayStr = delayStr + "s"
				setActFalse(delayedOffFalse, delayStr)
			}
			input "pendedOffFalse", "capability.switch", title: "Turn off these switches after a delay, pending cancellation", multiple: true, required: false, submitOnChange: true
			if(pendedOffFalse) input "pendMinutesFalse", "number", title: "Minutes of delay", required: true, range: "1..*", submitOnChange: true
			if(pendMinutesFalse) {
				def pendStr = "Pending Off: $pendedOffFalse: $pendMinutesFalse minute"
				if(pendMinutesFalse > 1) pendStr = pendStr + "s"
				setActFalse(pendedOffFalse, pendStr)
			}
			input "dimAFalse", "capability.switchLevel", title: "Set these dimmers", multiple: true, submitOnChange: true, required: false
			if(dimAFalse) input "dimLAFalse", "number", title: "To this level", range: "0..100", required: true, submitOnChange: true
			if(dimLAFalse) setActFalse(dimAFalse, "Dim: $dimAFalse: $dimLAFalse")
			input "dimBFalse", "capability.switchLevel", title: "Set these other dimmers", multiple: true, submitOnChange: true, required: false
			if(dimBFalse) input "dimLBFalse", "number", title: "To this level", range: "0..100", required: true, submitOnChange: true
			if(dimLBFalse) setActFalse(dimBFalse, "Dim: $dimBFalse: $dimLBFalse")
			input "lockFalse", "capability.lock", title: "Lock these locks", multiple: true, required: false, submitOnChange: true
			setActFalse(lockFalse, "Lock: $lockFalse")
			input "unlockFalse", "capability.lock", title: "Unlock these locks", multiple: true, required: false, submitOnChange: true
			setActFalse(unlockFalse, "Unlock: $unlockFalse")
			input "modeFalse", "mode", title: "Set the mode", multiple: false, required: false, submitOnChange: true
			if(modeFalse) addToActFalse("Mode: $modeFalse")
			def phrases = location.helloHome?.getPhrases()*.label
			input "myPhraseFalse", "enum", title: "Routine to run", required: false, options: phrases.sort(), submitOnChange: true
			if(myPhraseFalse) addToActFalse("Routine: $myPhraseFalse")
			href "selectMsgFalse", title: "Send message", description: state.msgFalse ? state.msgFalse : "Tap to set", state: state.msgFalse ? "complete" : null
			if(state.msgFalse) addToActFalse(state.msgFalse)
            input "delayFalse", "number", title: "Delay the effect of this rule by this many minutes", required: false, submitOnChange: true
            if(delayFalse) {
            	def delayStr = "Delay Rule: $delayFalse minute"
                if(delayFalse > 1) delayStr = delayStr + "s"
            	addToActFalse(delayStr)
            }
		}
        if(state.actsFalse) state.actsFalse = state.actsFalse[0..-2]
	}
}

def selectMsgTrue() {
	dynamicPage(name: "selectMsgTrue", title: "Select Message and Destination", uninstall: false) {
		state.msgTrue = ""
		section("") {
			input "pushTrue", "bool", title: "Send Push Notification?", required: false, submitOnChange: true
			if(pushTrue) state.msgTrue = state.msgTrue + "Push"
			input "msgTrue", "text", title: "Custom message to send", required: false, submitOnChange: true
			if(msgTrue) state.msgTrue = state.msgTrue + (state.msgTrue ? (state.msgTrue + " '$msgTrue'") : "Send '$msgTrue'")
			input "phoneTrue", "phone", title: "Phone number for SMS", required: false, submitOnChange: true
			if(phoneTrue) state.msgTrue = state.msgTrue + " to $phoneTrue"
		}
	}
}

def selectMsgFalse() {
	dynamicPage(name: "selectMsgFalse", title: "Select Message and Destination", uninstall: false) {
		state.msgFalse = ""
		section("") {
			input "pushFalse", "bool", title: "Send Push Notification?", required: false, submitOnChange: true
			if(pushFalse) state.msgFalse = state.msgFalse + "Push"
			input "msgFalse", "text", title: "Custom message to send", required: false, submitOnChange: true
			if(msgFalse) state.msgFalse = state.msgFalse + state.msgFalse ? state.msgFalse + " '$msgFalse'" : "Send '$msgFalse'"
			input "phoneFalse", "phone", title: "Phone number for SMS", required: false, submitOnChange: true
			if(phoneFalse) state.msgFalse = state.msgFalse + " to $phoneFalse"
		}
	}
}

def defineRule() {
	dynamicPage(name: "defineRule", title: "Define the Rule", uninstall: false) {
		state.n = 0
		state.str = ""
		state.eval = []
		section() {
		inputLeftAndRight(false)
		}
	}
}

def certainTime() {
	dynamicPage(name: "certainTime", title: "Only during a certain time", uninstall: false) {
		section() {
			input "startingX", "enum", title: "Starting at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
			if(startingX in [null, "A specific time"]) input "starting", "time", title: "Start time", required: false
			else {
				if(startingX == "Sunrise") input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
				else if(startingX == "Sunset") input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
			}
		}
		
		section() {
			input "endingX", "enum", title: "Ending at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
			if(endingX in [null, "A specific time"]) input "ending", "time", title: "End time", required: false
			else {
				if(endingX == "Sunrise") input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
				else if(endingX == "Sunset") input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
			}
		}
	}
}

def atCertainTime() {
	dynamicPage(name: "atCertainTime", title: "At a certain time", uninstall: false) {
		section() {
			input "timeX", "enum", title: "At this time", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
			if(timeX in [null, "A specific time"]) input "atTime", "time", title: "At this time", required: false
			else {
				if(timeX == "Sunrise") input "atSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
				else if(timeX == "Sunset") input "atSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
			}            
		}
	}
}

def getDevs(myCapab, dev) {
    def thisName = ""
    def thisCapab = ""
	switch(myCapab) {
		case "Switch":
			thisName = "Switches"
			thisCapab = "switch"
			break
		case "Motion":
			thisName = "Motion sensors"
			thisCapab = "motionSensor"
			break
		case "Acceleration":
			thisName = "Acceleration sensors"
			thisCapab = "accelerationSensor"
			break        
		case "Contact":
			thisName = "Contact sensors"
			thisCapab = "contactSensor"
			break
		case "Presence":
			thisName = "Presence sensors"
			thisCapab = "presenceSensor"
			break
		case "Lock":
			thisName = "Locks"
			thisCapab = "lock"
			break
		case "Dimmer level":
			thisName = "Dimmers"
			thisCapab = "switchLevel"
			break
		case "Temperature":
			thisName = "Temperature sensors"
			thisCapab = "temperatureMeasurement"
			break
		case "Humidity":
			thisName = "Humidity sensors"
			thisCapab = "relativeHumidityMeasurement"
			break
		case "Illuminance":
			thisName = "Illuminance sensors"
			thisCapab = "illuminanceMeasurement"
			break
		case "Energy meter":
			thisName = "Energy meters"
			thisCapab = "energyMeter"
			break
		case "Power meter":
			thisName = "Power meters"
			thisCapab = "powerMeter"
			break
		case "Battery":
			thisName = "Batteries"
			thisCapab = "battery"
	}
	def result = input dev, "capability.$thisCapab", title: thisName, required: true, multiple: true, submitOnChange: true
}

def getAnyAll(myDev) {
	def result = input "All$myDev", "bool", title: "All of these?", defaultValue: false
}

def getRelational(myDev) {
	def result = input "Rel$myDev", "enum", title: "Choose comparison", required: true, options: ["=", "!=", "<", ">", "<=", ">="]
}

def getCapab(myCapab) {  
	def myOptions = ["Switch", "Motion", "Acceleration", "Contact", "Presence", "Lock", "Temperature", "Humidity", "Illuminance", "Time of day", "Certain Time", 
    	"Days of week", "Mode", "Dimmer level", "Energy meter", "Power meter", "Battery"]
	def result = input myCapab, "enum", title: "Select capability", required: false, options: myOptions.sort(), submitOnChange: true
}

def getState(myCapab, n) {
	def result = null
	def days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
	if     (myCapab == "Switch") 		result = input "state$n", "enum", title: "Switch state", options: ["on", "off"]
	else if(myCapab == "Motion") 		result = input "state$n", "enum", title: "Motion state", options: ["active", "inactive"], defaultValue: "active"
	else if(myCapab == "Acceleration")	result = input "state$n", "enum", title: "Acceleration state", options: ["active", "inactive"]
	else if(myCapab == "Contact") 		result = input "state$n", "enum", title: "Contact state", options: ["open", "closed"]
	else if(myCapab == "Presence") 		result = input "state$n", "enum", title: "Presence state", options: ["present", "not present"], defaultValue: "present"
	else if(myCapab == "Lock") 			result = input "state$n", "enum", title: "Lock state", options: ["locked", "unlocked"]
	else if(myCapab == "Dimmer level")	result = input "state$n", "number", title: "Dimmer level", range: "0..100"
	else if(myCapab == "Temperature") 	result = input "state$n", "number", title: "Temperature"
	else if(myCapab == "Humidity") 		result = input "state$n", "number", title: "Humidity", range: "0..100"
	else if(myCapab == "Illuminance") 	result = input "state$n", "number", title: "Illuminance"
	else if(myCapab == "Energy meter") 	result = input "state$n", "number", title: "Energy level"
	else if(myCapab == "Power meter") 	result = input "state$n", "number", title: "Power level"
	else if(myCapab == "Battery") 		result = input "state$n", "number", title: "Battery level"
	else if(myCapab == "Mode") 			result = input "modes", "mode", title: "When mode is", multiple: true, required: false
	else if(myCapab == "Days of week") 	result = input "days", "enum", title: "On certain days of the week", multiple: true, required: false, options: days
	else if(myCapab == "Time of day") {
		def timeLabel = timeIntervalLabel()
		href "certainTime", title: "During a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
	} else if(myCapab == "Certain Time") {
		def atTimeLabel = atTimeLabel()
		href "atCertainTime", title: "At a certain time", description: atTimeLabel ?: "Tap to set", state: atTimeLabel ? "complete" : null
	}
}

def inputLeft(sub) {
	def howMany = state.howMany - 1
	def conds = []
	for (int i = 1; i <= howMany; i++) conds << conditionLabelN(i)
	input "subCondL$state.n", "bool", title: "Enter subrule for left?", submitOnChange: true
	if(settings["subCondL$state.n"]) {
		state.str = state.str + "("
		state.eval << "("
		paragraph(state.str)
		inputLeftAndRight(true)
		input "moreConds$state.n", "bool", title: "More conditions on left?", submitOnChange: true
		if(settings["moreConds$state.n"]) inputRight(sub)
	} else {
		input "condL$state.n", "enum", title: "Which condition?", options: conds, submitOnChange: true
		if(settings["condL$state.n"]) {
			state.str = state.str + settings["condL$state.n"]
			def myCond = 0
			for (int i = 1; i <= howMany; i++) if(conditionLabelN(i) == settings["condL$state.n"]) myCond = i
			state.eval << myCond
			paragraph(state.str)
		}
	}
}

def inputRight(sub) {
	def howMany = state.howMany - 1
	state.n = state.n + 1
	input "operator$state.n", "enum", title: "Choose: AND,  OR,  Done", options: ["AND", "OR", "Done"], submitOnChange: true
	if(settings["operator$state.n"]) if(settings["operator$state.n"] != "Done") {
		state.str = state.str + " " + settings["operator$state.n"] + " "
		state.eval << settings["operator$state.n"]
		paragraph(state.str)
		def conds = []
		for (int i = 1; i <= howMany; i++) conds << conditionLabelN(i)
		input "subCondR$state.n", "bool", title: "Enter subrule for right?", submitOnChange: true
		if(settings["subCondR$state.n"]) {
			state.str = state.str + "("
			state.eval << "("
			paragraph(state.str)
			inputLeftAndRight(true)
			input "moreConds$state.n", "bool", title: "More conditions on right?", submitOnChange: true
			if(settings["moreConds$state.n"]) inputRight(sub)
		} else {
			input "condR$state.n", "enum", title: "Which condition?", options: conds, submitOnChange: true
			if(settings["condR$state.n"]) {
				state.str = state.str + settings["condR$state.n"]
				def myCond = 0
				for (int i = 1; i <= howMany; i++) if(conditionLabelN(i) == settings["condR$state.n"]) myCond = i
				state.eval << myCond
				paragraph(state.str)
			}
			if(sub) {
				input "endOfSub$state.n", "bool", title: "End of sub-rule?", submitOnChange: true
				if(settings["endOfSub$state.n"]) {
					state.str = state.str + ")"
					state.eval << ")"
					paragraph(state.str)
					return
				}
			}
			input "moreConds$state.n", "bool", title: "More conditions on right?", submitOnChange: true
			if(settings["moreConds$state.n"]) inputRight()
		}
	} 
}

def inputLeftAndRight(sub) {
	state.n = state.n + 1
	inputLeft(sub)
	inputRight(sub)
}

// initialization code below

def scheduleTimeOfDay() {
	def start = null
	def stop = null
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
	if(startingX == "Sunrise") start = s.sunrise.time
	else if(startingX == "Sunset") start = s.sunset.time
	else if(starting) start = timeToday(starting,location.timeZone).time
	s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: endSunriseOffset, sunsetOffset: endSunsetOffset)
	if(endingX == "Sunrise") stop = s.sunrise.time
	else if(endingX == "Sunset") stop = s.sunset.time
	else if(ending) stop = timeToday(ending,location.timeZone).time
	schedule(start, "startHandler")
	schedule(stop, "stopHandler")
	if(startingX in ["Sunrise", "Sunset"] || endingX in ["Sunrise", "Sunset"])
		schedule("2015-01-09T00:00:29.000-0700", "scheduleTimeOfDay") // in case sunset/sunrise; change daily
}

def scheduleAtTime() {
	def myTime = null
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: atSunriseOffset, sunsetOffset: atSunsetOffset)
	if(timeX == "Sunrise") myTime = s.sunrise.time
	else if(timeX == "Sunset") myTime = s.sunset.time
	else myTime = timeToday(atTime, location.timeZone).time
	schedule(myTime, "timeHandler")
	if(timeX in ["Sunrise", "Sunset"]) schedule("2015-01-09T00:00:29.000-0700", "scheduleAtTime") // in case sunset/sunrise; change daily
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
	def howMany = state.howMany - 1
	for (int i = 1; i <= howMany; i++) {
		def capab = (settings.find {it.key == "rCapab$i"}).value
		if     (capab == "Mode") subscribe(location, "mode", allHandler)
		else if(capab == "Time of day") scheduleTimeOfDay()
		else if(capab == "Certain Time") scheduleAtTime()
		else if(capab == "Days of week") schedule("2015-01-09T00:00:10.000-0700", "runRule")
		else if(capab == "Dimmer level") subscribe((settings.find{it.key == "rDev$i"}).value, "level", allHandler)
		else if(capab == "Energy meter") subscribe((settings.find{it.key == "rDev$i"}).value, "energy", allHandler)
		else if(capab == "Power meter") subscribe((settings.find{it.key == "rDev$i"}).value, "power", allHandler)
		else subscribe((settings.find{it.key == "rDev$i"}).value, capab.toLowerCase(), allHandler)
	}
	state.success = null
	runRule(false)
}

// Main rule evaluation code follows

def compare(a, rel, b) {
	def result = true
	if     (rel == "=") 	result = a == b
	else if(rel == "!=") 	result = a != b
	else if(rel == ">") 	result = a > b
	else if(rel == "<") 	result = a < b
	else if(rel == ">=") 	result = a >= b
	else if(rel == "<=") 	result = a <= b
	return result
}

def checkCondAny(dev, state, cap, rel) {
	def result = false
	if     (cap == "Temperature") 	dev.currentTemperature.each {result = result || compare(it, rel, state)}
	else if(cap == "Humidity") 		dev.currentHumidity.each    {result = result || compare(it, rel, state)}
	else if(cap == "Illuminance") 	dev.currentIlluminance.each {result = result || compare(it, rel, state)}
	else if(cap == "Dimmer level")	dev.currentLevel.each		{result = result || compare(it, rel, state)}
	else if(cap == "Energy meter")	dev.currentEnergy.each		{result = result || compare(it, rel, state)}
	else if(cap == "Power meter")	dev.currentPower.each		{result = result || compare(it, rel, state)}
	else if(cap == "Battery")		dev.currentBattery.each		{result = result || compare(it, rel, state)}
	else if(cap == "Switch") 		result = state in dev.currentSwitch
	else if(cap == "Motion") 		result = state in dev.currentMotion
	else if(cap == "Acceleration") 	result = state in dev.currentAcceleration
	else if(cap == "Contact") 		result = state in dev.currentContact
	else if(cap == "Presence") 		result = state in dev.currentPresence
	else if(cap == "Lock") 			result = state in dev.currentLock
//	log.debug "CheckAny $cap $result"
	return result
}

def checkCondAll(dev, state, cap, rel) {
	def flip = ["on": "off",
				"off": "on",
                "active": "inactive",
                "inactive": "active",
                "open": "closed",
                "closed": "open",
                "present": "not present",
                "not present": "present",
                "locked": "unlocked",
                "unlocked": "locked"]
	def result = true
	if     (cap == "Temperature") 	dev.currentTemperature.each {result = result && compare(it, rel, state)}
	else if(cap == "Humidity") 		dev.currentHumidity.each    {result = result && compare(it, rel, state)}
	else if(cap == "Illuminance") 	dev.currentIlluminance.each {result = result && compare(it, rel, state)}
	else if(cap == "Dimmer level")	dev.currentLevel.each		{result = result && compare(it, rel, state)}
	else if(cap == "Energy meter")	dev.currentEnergy.each		{result = result && compare(it, rel, state)}
	else if(cap == "Power meter")	dev.currentPower.each		{result = result && compare(it, rel, state)}
	else if(cap == "Battery")		dev.currentBattery.each		{result = result && compare(it, rel, state)}
	else if(cap == "Switch") 		result = !(flip[state] in dev.currentSwitch)
	else if(cap == "Motion") 		result = !(flip[state] in dev.currentMotion)
	else if(cap == "Acceleration") 	result = !(flip[state] in dev.currentAcceleration)
	else if(cap == "Contact") 		result = !(flip[state] in dev.currentContact)
	else if(cap == "Presence") 		result = !(flip[state] in dev.currentPresence)
	else if(cap == "Lock") 			result = !(flip[state] in dev.currentLock)
//	log.debug "CheckAll $cap $result"
	return result
}

def getOperand(i) {
	def result = true
	def capab = (settings.find {it.key == "rCapab$i"}).value
	if     (capab == "Mode") result = modeOk
	else if(capab == "Time of day") result = timeOk
	else if(capab == "Certain Time") result = atTimeOk
	else if(capab == "Days of week") result = daysOk
	else {
		def myDev = 	settings.find {it.key == "rDev$i"}
		def myState = 	settings.find {it.key == "state$i"}
		def myRel = 	settings.find {it.key == "RelrDev$i"}
		def myAll = 	settings.find {it.key == "AllrDev$i"}
		if(myAll) {
			if(myAll.value) result = checkCondAll(myDev.value, myState.value, capab, myRel ? myRel.value : 0)
			else result = checkCondAny(myDev.value, myState.value, capab, myRel ? myRel.value : 0)
		} else result = checkCondAny(myDev.value, myState.value, capab, myRel ? myRel.value : 0)
	}
//    log.debug "operand is $result"
	return result
}

def findRParen() {
	def noMatch = true
	while(noMatch) {
		if(state.eval[state.token] == ")") {
			if(state.parenLev == 0) return
			else state.parenLev = state.parenLev - 1
		} else if(state.eval[state.token] == "(") state.parenLev = state.parenLev + 1
		state.token = state.token + 1
		if(state.token >= state.eval.size) return
	}
}

def disEval() {
    if(state.eval[state.token] == "(") {
    	state.parenLev = 0
        findRParen()
    }
    if(state.token >= state.eval.size) return
    state.token = state.token + 1
}

def evalTerm() {
	def result = true
	def thisTok = state.eval[state.token]
//    log.debug "evalTerm tok is $thisTok"
	if (thisTok == "(") {
		state.token = state.token + 1
		result = eval()
	} else result = getOperand(thisTok)
	state.token = state.token + 1
//    log.debug "evalTerm is $result"
	return result
}

def eval() {
	def result = evalTerm()
	while(true) {
		if(state.token >= state.eval.size) return result
		def thisTok = state.eval[state.token]
//        log.debug "eval: $thisTok"
		if (thisTok == "OR") {
			if(result) {
				disEval()
				return true
			} 
		} else if (thisTok == "AND") {
			if(!result) {
				disEval()
				return false
			} 
		} else if (thisTok == ")") return result
		state.token = state.token + 1
		result = evalTerm()
	}
}

def doDelay(time, success) {
	runIn(time * 60, delayRule)
    def delayStr = "minute"
    if(time > 1) delayStr = delayStr + 1
    log.info (success ? "$app.label is True, but delayed by $time $delayStr" : "$app.label is False, but delayed by $time $delayStr")
    state.success = success
}

def runRule(delay) {
	state.token = 0
	def success = eval()
	if((success != state.success) || delay) {
		unschedule(delayRule)
		if(delayTrue && !delay)		doDelay(delayTrue, success)
		else if(delayFalse && !delay)	doDelay(delayFalse, success)
		else if(success) {
			if(onSwitchTrue) 	onSwitchTrue.on()
			if(offSwitchTrue) 	offSwitchTrue.off()
			if(delayedOffTrue)	runIn(delayMinutesTrue * 60, delayOffTrue)
			if(pendedOffTrue)	runIn(pendMinutesTrue * 60, pendingOffTrue)
			if(pendedOffFalse)	unschedule(pendingOffFalse)
			if(dimATrue) 		dimATrue.setLevel(dimLATrue)
			if(dimBTrue) 		dimBTrue.setLevel(dimLBTrue)
			if(lockTrue) 		lockTrue.lock()
			if(unlockTrue) 		unlockTrue.unlock()
			if(modeTrue) 		setLocationMode(modeTrue)
			if(pushTrue)		sendPush(msgTrue ?: "Rule $app.label True")
			if(phoneTrue)		sendSms(phoneTrue, msgTrue ?: "Rule $app.label True")
				
			//My Code
			if(pendedCloseTrue)	runIn(pendDoorMinutesTrue * 60, pendingCloseTrue)
			if(onDoorOpen) 		onDoorOpen.open()
			if(onDoorClose) 	onDoorClose.close()
			//End My COde
		
			if(myPhraseTrue)	location.helloHome.execute(myPhraseTrue)
		} else {
			if(onSwitchFalse) 	onSwitchFalse.on()
			if(offSwitchFalse) 	offSwitchFalse.off()
			if(delayedOffFalse)	runIn(delayMinutesFalse * 60, delayOffFalse)
			if(pendedOffFalse)	runIn(pendMinutesFalse * 60, pendingOffFalse)
			if(pendedOffTrue)	unschedule(pendingOffTrue)
			if(dimAFalse) 		dimAFalse.setLevel(dimLAFalse)
			if(dimBFalse) 		dimBFalse.setLevel(dimLBFalse)
			if(lockFalse) 		lockFalse.lock()
			if(unlockFalse) 	unlockFalse.unlock()
			if(modeFalse) 		setLocationMode(modeFalse)
			if(myPhraseFalse) 	location.helloHome.execute(myPhraseFalse)
			if(pushFalse)		sendPush(msgFalse ?: "Rule $app.label False")
			if(phoneFalse)		sendSms(phoneFalse, msgFalse ?: "Rule $app.label False")
		}
		state.success = success
		log.info (success ? "$app.label is True" : "$app.label is False")
	}
}

def allHandler(evt) {
	log.info "$app.label: $evt.displayName $evt.name $evt.value"
	runRule(false)
}

def startHandler() {
	runRule(false)
}

def stopHandler() {
	runRule(false)
}

def timeHandler() {
	runRule(false)
}

def delayOffTrue() {
	delayedOffTrue.off()
}

def pendingOffTrue() {
	pendedOffTrue.off()
}

def delayOffFalse() {
	delayedOffFalse.off()
}

def pendingOffFalse() {
	pendedOffFalse.off()
}

def delayRule() {
	runRule(true)
}

//My Code
def pendingCloseTrue() {
	pendedCloseTrue.close()
}

//  private execution filter methods below
private conditionLabel() {
	def howMany = state.howMany
	def result = ""
	if(howMany) {
		for (int i = 1; i <= howMany; i++) {
			result = result + conditionLabelN(i)
			if((i + 1) <= howMany) result = result + "\n"
		}
    }
	return result
}

private conditionLabelN(i) {
	def result = ""
        def thisCapab = settings.find {it.key == "rCapab$i"}
        if(!thisCapab) return result
        if(thisCapab.value == "Time of day") result = "Time between " + timeIntervalLabel()
        else if(thisCapab.value == "Certain Time") result = "Time is " + atTimeLabel()
        else if(thisCapab.value == "Days of week") result = "Day i" + (days.size() > 1 ? "n " + days : "s " + days[0])
        else if(thisCapab.value == "Mode") result = "Mode i" + (modes.size() > 1 ? "n " + modes : "s " + modes[0])
        else {
		def thisDev = settings.find {it.key == "rDev$i"}
		if(!thisDev) return result
		def thisAll = settings.find {it.key == "AllrDev$i"}
		def myAny = thisAll ? "any " : ""
		if(thisCapab.value == "Temperature") 		result = "Temperature of "
		else if(thisCapab.value == "Humidity") 		result = "Humidity of "
		else if(thisCapab.value == "Illuminance")	result = "Illuminance of "
		else if(thisCapab.value == "Dimmer level")	result = "Dimmer level of " 
		else if(thisCapab.value == "Energy meter")	result = "Energy level of " 
		else if(thisCapab.value == "Power meter")	result = "Power level of " 
		else if(thisCapab.value == "Battery")		result = "Battery level of " 
		result = result + (myAny ? thisDev.value : thisDev.value[0]) + " " + ((thisAll ? thisAll.value : false) ? "all " : myAny)
		def thisRel = settings.find {it.key == "RelrDev$i"}
		if(thisCapab.value in ["Temperature", "Humidity", "Illuminance", "Dimmer level", "Energy meter", "Power meter", "Battery"]) result = result + " " + thisRel.value + " "
		def thisState = settings.find {it.key == "state$i"}
		result = result + thisState.value
        }
	return result
}

private msgLabel() {
	def result = state.msgTrue
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private offset(value) {
	def result = value ? ((value > 0 ? "+" : "") + value + " min") : ""
}

private timeIntervalLabel() {
	def result = ""
	if (startingX == "Sunrise" && endingX == "Sunrise") result = "Sunrise" + offset(startSunriseOffset) + " and Sunrise" + offset(endSunriseOffset)
	else if (startingX == "Sunrise" && endingX == "Sunset") result = "Sunrise" + offset(startSunriseOffset) + " and Sunset" + offset(endSunsetOffset)
	else if (startingX == "Sunset" && endingX == "Sunrise") result = "Sunset" + offset(startSunsetOffset) + " and Sunrise" + offset(endSunriseOffset)
	else if (startingX == "Sunset" && endingX == "Sunset") result = "Sunset" + offset(startSunsetOffset) + " and Sunset" + offset(endSunsetOffset)
	else if (startingX == "Sunrise" && ending) result = "Sunrise" + offset(startSunriseOffset) + " and " + hhmm(ending, "h:mm a z")
	else if (startingX == "Sunset" && ending) result = "Sunset" + offset(startSunsetOffset) + " and " + hhmm(ending, "h:mm a z")
	else if (starting && endingX == "Sunrise") result = hhmm(starting) + " and Sunrise" + offset(endSunriseOffset)
	else if (starting && endingX == "Sunset") result = hhmm(starting) + " and Sunset" + offset(endSunsetOffset)
	else if (starting && ending) result = hhmm(starting) + " and " + hhmm(ending, "h:mm a z")
}

private atTimeLabel() {
	def result = ''
	if     (timeX == "Sunrise") result = "Sunrise" + offset(atSunriseOffset)
	else if(timeX == "Sunset")  result = "Sunset" + offset(atSunsetOffset)
	else if(atTime) result = hhmm(atTime)
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
//	log.trace "modeOk = $result"
	return result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) df.setTimeZone(location.timeZone)
		else df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		def day = df.format(new Date())
		result = days.contains(day)
	}
//	log.trace "daysOk = $result"
	return result
}

private getTimeOk() {
	def result = true
	if((starting && ending) ||
	(starting && endingX in ["Sunrise", "Sunset"]) ||
	(startingX in ["Sunrise", "Sunset"] && ending) ||
	(startingX in ["Sunrise", "Sunset"] && endingX in ["Sunrise", "Sunset"])) {
		def currTime = now()
		def start = null
		def stop = null
		def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
		if(startingX == "Sunrise") start = s.sunrise.time
		else if(startingX == "Sunset") start = s.sunset.time
		else if(starting) start = timeToday(starting, location.timeZone).time
		s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: endSunriseOffset, sunsetOffset: endSunsetOffset)
		if(endingX == "Sunrise") stop = s.sunrise.time
		else if(endingX == "Sunset") stop = s.sunset.time
		else if(ending) stop = timeToday(ending,location.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
//	log.trace "getTimeOk = $result"
	return result
}

private getAtTimeOk() {
	def result = true
	if(atTime || timex) {
		def currTime =  now()
		def myTime = null
		def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: atSunriseOffset, sunsetOffset: atSunsetOffset)
		if(timeX == "Sunrise") myTime = s.sunrise.time
		else if(timeX == "Sunset") myTime = s.sunset.time
		else myTime = timeToday(atTime, location.timeZone).time
		def mt2 = (myTime / 100000).toInteger()
		def ct2 = (currTime / 100000).toInteger()
		result = mt2 == ct2
	}
//	log.trace "getAtTimeOk = $result"
	return result
}
