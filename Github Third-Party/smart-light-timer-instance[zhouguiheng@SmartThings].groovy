/**
 *  Smart Light Timer for Vincent (Child app)
 *
 *  Copyright 2016 Vincent
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
    name: "Smart Light Timer Instance",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Turn on/off the lights based on sensors.",
    category: "My Apps",
	parent: "zhouguiheng:Smart Light Timer",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
		section("Turn on/off light..."){
			input "myswitch", "capability.switch", title: "Select Light", submitOnChange: true
			if (myswitch && myswitch.hasAttribute("level")) {
	        	input "level", "number", title: "Set dimmer level instead", required: false, range: "0..100"
			}
		}
		section("Turn on when...", hideWhenEmpty: true) {
			input "motions", "capability.motionSensor", multiple: true, title: "Motion detected", required: false, submitOnChange: true
			input "contacts", "capability.contactSensor", multiple: true, title: "Contacts open", required: false
		}
		section("Turn off after...") {
			if (motions && motions.size() > 0) {
				input "motionMinutes", "number", title: "Minutes to turn off after motion stops", defaultValue: "1"
			}
			input "minutes1", "number", title: "Minutes to turn off after no other triggers", defaultValue: "10"
		}
	    section("The switch to prevent the light from being turned off") {
			input "holder", "capability.switch", title: "Select Light Holder", required: false
		}
		section("Enable the app only between two times?") {
			input "fromTimeType", "enum", title: "From time?", required: false, options: ["Sunrise", "Sunset", "Specific time"], submitOnChange: true
			if (fromTimeType == "Specific time") {
				input "fromTime", "time", title: "Specific time?", required: false
			} else if (fromTimeType) {
				input "fromOffset", "number", title: "Offset?", required: false, range: "*..*"
			}
			input "toTimeType", "enum", title: "To time?", required: false, options: ["Sunrise", "Sunset", "Specific time"], submitOnChange: true
			if (toTimeType == "Specific time") {
				input "toTime", "time", title: "Specific time?", required: false
			} else if (toTimeType) {
				input "toOffset", "number", title: "Offset?", required: false, range: "*..*"
			}
		}
		section("Enable the app only when any of these motion sensors are active") {
			input "activeMotions", "capability.motionSensor", multiple: true, required: false
		}
		section("Enable the app only when all these switches are off") {
			input "offSwitches", "capability.switch", multiple: true, required: false
		}
		section("Enable debug logging") {
			input "debuglog", "bool", title: "Enabled?", defaultValue: "false"
		}
		section {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}

def installed()
{
	subscribe(myswitch, "switch", lightHandler)
	subscribe(motions, "motion", motionHandler)
	subscribe(contacts, "contact", contactHandler)
	if (holder != null) subscribe(holder, "switch", holderHandler)
}

def updated()
{
	unsubscribe()
    installed()
}

def lightHandler(evt) {
	if (!shouldEnable()) return
	if (debuglog) log.debug "lightHandler from $evt.source: $evt.name: $evt.value"
    if (evt.value == "on") {
  		scheduleTurnOff(minutes1)
    } else if (evt.value == "off") {
    	unschedule(turnOff)
    }
}

def contactHandler(evt) {
	if (!shouldEnable()) return
	if (debuglog) log.debug "contactHandler: $evt.name: $evt.value"
    if (evt.value == "open") {
    	turnOn(minutes1)
    }
}

def motionHandler(evt) {
	if (!shouldEnable()) return
	if (debuglog) log.debug "motionHandler: $evt.name: $evt.value"
    if (evt.value == "active") {
    	turnOn(minutes1)
    } else if (evt.value == "inactive") {
    	scheduleTurnOff(motionMinutes) 
    }
}

def holderHandler(evt) {
	if (!shouldEnable()) return
	if (debuglog) log.debug "holderHandler: $evt.name: $evt.value"
    if (evt.value == "on") {
    	turnOn(-1)
    } else {
    	turnOff()
    }
}

def turnOn(minutes) {
	if (debuglog) log.debug "turnOn: " + myswitch.latestValue("switch")
    unschedule(turnOff)
	if (myswitch.latestValue("switch") == "off") {
        if (level != null) {
            myswitch.setLevel(level)
        } else {
            myswitch.on()
        }
    }
    if (minutes > 0) scheduleTurnOff(minutes)
}

def scheduleTurnOff(minutes) {
	if (debuglog) log.debug "scheduleTurnOff: $minutes minutes"
    if (holder == null || holder.latestValue("switch") != "on") {
        if (minutes > 0) {
          runIn(minutes * 60, turnOff)
        } else {
          unschedule(turnOff)
          turnOff()
        }
    }
}

def turnOff() {
	if (debuglog) log.debug "turnOff: " + myswitch.latestValue("switch")
	if (myswitch.latestValue("switch") == "on") {
    	if (motions.find { it.latestValue("motion") == "active" } == null) {
			myswitch.off()
        } else {
        	// Re-check after 1 minute.
        	scheduleTurnOff(1)
        }
    }
}

def shouldEnable() {
	return anyActiveMotionSensorsAreActive() && allOffSwitchesAreOff() && isBetweenSpecifiedTimes()
}

def anyActiveMotionSensorsAreActive() {
	return activeMotions == null || activeMotions.find { it.latestValue("motion") == "active" } != null
}

def allOffSwitchesAreOff() {
	return offSwitches == null || offSwitches.find { it.latestValue("switch") != "off" } == null
}

def isBetweenSpecifiedTimes() {
	if (!fromTimeType || !toTimeType) {
		return true
	}
	def from
	switch (fromTimeType) {
		case "Sunrise":
			from = getSunrise(fromOffset)
			break
		case "Sunset":
			from = getSunset(fromOffset)
			break
		default:
			from = toDateTime(fromTime)
	}
	def to
	switch (toTimeType) {
		case "Sunrise":
			to = getSunrise(toOffset)
			break
		case "Sunset":
			to = getSunset(toOffset)
			break
		default:
			to = toDateTime(toTime)
	}
	def now = new Date()
	if (to.before(from)) {
		if (now.before(from)) {
			from = from.previous()
		} else {
			to = to.next()
		}
	}
	if (debuglog) log.debug "$from -> $to"
	return timeOfDayIsBetween(from, to, new Date(), location.timeZone);
}

def getSunrise(offset) {
	return getSunriseAndSunset(sunriseOffset: offset).sunrise
}

def getSunset(offset) {
	return getSunriseAndSunset(sunsetOffset: offset).sunset
}