/**
 *  Random Hue
 *
 *  Copyright 2017-2018 Omen Wild
 *  https://github.com/OmenWild/SmartThings-RandomHue
 *
 **************************************************
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 **************************************************
 *  Sunset/Sunrise/Time handling originally based on:
 *  https://github.com/imnotbob/vacation-lighting-director/blob/master/smartapps/imnotbob/vacation-lighting-director.src/vacation-lighting-director.groovy
 **************************************************
 */
 
definition(
    name: "Random Hue",
    namespace: "OmenWild",
    author: "Omen Wild",
    description: "An app to cycle the hue of color changing lights.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/lighting-wizard.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/lighting-wizard@2x.png",
    //iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/lighting-wizard@2x.png",
    )


preferences {
	page(name: "pageSettings")
	page(name: "pageColorsList")
    page(name: "pageTimeInterval")
}
    
// Show setup page
def pageSettings() {

	def pageProperties = [
		name:		"pageSettings",
		title:		"Setup",
		install:	true,
		uninstall:	true
	]

    dynamicPage(pageProperties) {
    	if (! location?.timeZone) {
            section("Time Zone") {
                paragraph "Please set your location's time zone, this app cannot run without it"
                return
            }
        }
    
        section("Choose lights you wish to control") {
            input "whichLights", "capability.colorControl", title: "Which color changing lights?", multiple: true, required: true
            input "brightness", "number", title: "Brightness (1-100)?", required: false, defaultValue: 100, range: "0..100"
            input "honorOffLights","bool", title: "Ignore lights manually turned off?", required: false, defaultValue: true
        }

		section {
	        href "pageTimeInterval", title: "Set start and stop time", description: timeIntervalLabel() //, state: greyedOutSettings()
        }

        section("Choose cycle time between color changes") {
            input "cycleTime", "enum", title: "Cycle time?" , options: [
                "1 minute",
                "5 minutes",
                "10 minutes", 
                "15 minutes", 
                "30 minutes", 
                "1 hour", 
                "3 hours"
            ], required: true, defaultValue: "5 minutes"
        }
  
        section("Colors") {
            input "groupControl","bool", title: "Control colors as a group?", required: false, defaultValue: true
            input "colorMode", "enum", title: "Color selection mode?" , options: [
                "Random",
                "From list", 
                "Incremental", 
            ], required: true, defaultValue: "Incremental", submitOnChange: true
        }
    
    	if (colorMode == "From list") {
	        section {
    	        href "pageColorsList", title: "Which colors to enable", description: ""
        	}
        }
    
        section ("Send notification?") {
            input "notification", "bool", title: "Post a notification to the activity feed?", required: false, defaultValue: false
        }

        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
	}
}

def pageColorsList() {
    def colors = [
        [name: 'Red', hue: 0, saturation: 100, default: true],
        [name: 'Brick Red', hue: 4, saturation: 100, default: true],
        [name: 'Safety Orange', hue: 7, saturation: 100, default: true],
        [name: 'Orange', hue: 10, saturation:  100, default: true],
        [name: 'Amber', hue: 13, saturation:  100, default: true],
        [name: 'Yellow', hue: 17, saturation:  100, default: true],
        [name: 'Green', hue: 33, saturation:  100, default: true],
        [name: 'Turquoise', hue: 47, saturation:  100, default: true],
        [name: 'Aqua', hue: 47, saturation:  100, default: true],
        [name: 'Navy Blue', hue: 61, saturation:  100, default: true],
        [name: 'Blue', hue: 65, saturation:  100, default: true],
        [name: 'Indigo', hue: 73, saturation:  100, default: true],
        [name: 'Purple', hue: 82, saturation:  100, default: true],
        [name: 'Pink', hue: 90, saturation:  67, default: true],
        [name: 'Raspberry', hue: 94, saturation:  100, default: true],
        [name: 'White', hue: 0, saturation: 0, default: false],
        [name: 'Warm White', hue: 20, saturation:  80, default: false],
    ]

    dynamicPage(name: "pageColorsList", title: "Which colors to enable") {
        section {
            for (color in colors) {
                String name = sprintf("colorEnabled_%d_%d_%s", color['hue'], color['saturation'], color['name'])
                String title = sprintf("Enable %s?", color['name'])
                input name, "bool", title: title, required: false, defaultValue: color['default']    
            }
        }
    }
}


def pageTimeInterval() {
	dynamicPage(name: "pageTimeInterval", title: "Start and stop time") {
		section {
			input "startTimeType", "enum", title: "Starting at", options: 
                [["Time": "A specific time"], 
                 ["Sunrise": "Sunrise"], 
                 ["Sunset": "Sunset"]], 
                required: true,
                submitOnChange:true
                
            if (startTimeType in ["Sunrise", "Sunset"]) {
				input "startTimeOffset", "number", title: "Offset in minutes (+/-)", range: "*..*", required: true, defaultValue: 0
			} else if (startTimeType == "Time") {
				input "startTime", "time", title: "Start time", required: true
			}
		}
        
		section {
            input "stopTimeType", "enum", title: "Ending at", options: 
                [["Time": "A specific time"], 
                 ["Sunrise": "Sunrise"], 
                 ["Sunset": "Sunset"]],
                 required: true,
                 submitOnChange:true

            if (stopTimeType in ["Sunrise", "Sunset"]) {
                input "stopTimeOffset", "number", title: "Offset in minutes (+/-)", range: "*..*", required: true, defaultValue: 0
			} else if (stopTimeType == "Time") {
                input "stopTime", "time", title: "End time", required: true
			}
		}
	}
}


private timeIntervalLabelStr(TimeType, TimeOffset) {
    def fmt = TimeType
    if (TimeOffset) {
        if (TimeOffset > 0) {
            fmt += "+"
        }
        fmt += "${TimeOffset} min"
    }
    
    return fmt
}

private timeIntervalLabel() {
	def start = ""
	switch (settings.startTimeType) {
		case "Time":
                start = hhmm(settings.startTime)
			break
		case "Sunrise":
		case "Sunset":
			start = timeIntervalLabelStr(settings.startTimeType, settings.startTimeOffset)
			break
	}

	def finish = ""
	switch (settings.stopTimeType) {
		case "Time":
				finish = hhmm(settings.stopTime)
			break
		case "Sunrise":
		case "Sunset":
        	finish = timeIntervalLabelStr(settings.stopTimeType, settings.stopTimeOffset)
			break
	}
    
	if (start && finish) {
    	return "${start} to ${finish}"
    } else {
    	return ""
    }
}

private hhmm(time, fmt = "HH:mm") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone)
	return f.format(t)
}


def buildColors() {
    def enabledColorArray = []
    settings.each {key, val ->
    	// log.debug "buildColors(key, val: $key, $val"
        if (key.startsWith('colorEnabled_') && val) {
        	def values = key.split('_')
        	enabledColorArray << [name: values[3], hue: values[1].toInteger(), saturation: values[2].toInteger()]
        }
	}

	if (enabledColorArray.size() == 0) {
		sendPush("WARNING: no colors enabled from the list, forcing red.\n")
        enabledColorArray << [name: 'FORCED RED', hue: 0, saturation: 100]
	}

	state.enabledColorArray = enabledColorArray
	// log.debug "buildColors(): ${state.enabledColorArray}"
}


def validateBrightness() {
    if (! settings.brightness || settings.brightness < 1 || settings.brightness > 100) {
    	state.brightness = 100
    }
    //log.debug "validateBrightness() -> ${state.brightness}"
}


def installed() {
	//log.trace "installed(${settings})"
    log.debug "installed()"

	initialize() 
}

def updated() {
	//log.debug "updated(${settings})"
    log.debug "updated()"

	unsubscribe()
    unschedule()
	initialize()
}


def initialize() {
	log.debug "initialize()"

    if (settings.colorMode == 'From list') {
    	buildColors()
    }

	validateBrightness()

	if (inRuntime('initialize')) {
    	// In the groove, turn it on!
        log.debug "initialize(): in a runtime, calling onHandler()"
        onHandler()
    } else {
        // Make sure they are off and schedule next run.
        log.debug "initialize(): NOT in a runtime, calling offHandler()"
        offHandler()
	}
}


private when(whence, TimeType, TimeOffset = 0, Time) {
	def result = null
    
	if (TimeType in ["Sunrise", "Sunset"]) {
		result = location.currentState("${TimeType.toLowerCase()}Time").dateValue
        result = new Date(result.time + Math.round(TimeOffset * 60000))
	} else {
        result = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Time)
	}
    
	log.debug "${whence}->when(${TimeType}, ${TimeOffset}, ${Time}) => ${result.format('YYYY-MM-dd h:mm a', location.timeZone)}"
	return result
}

private timeStart(whence) {
	return when(whence + '->timeStart', settings.startTimeType, settings.startTimeOffset, settings.startTime)
}

private timeStop(whence) {
	return when(whence + '->timeStop', settings.stopTimeType, settings.stopTimeOffset, settings.stopTime)
}


def inRuntime(whence) {
    return timeOfDayIsBetween(timeStart(whence), timeStop(whence), new Date(), location.timeZone)
}


// implement event handlers
def offHandler(evt = null) {
	settings.whichLights.off()

    log.debug "offHandler()"
    unschedule(cycleHandler)

	// schedule to turn on tomorrow
    runOnce(timeStart('offHandler->runOnce'), onHandler)
}


def onHandler() {
	log.debug "onHandler()"
    
    // Schedule turning off
    runOnce(timeStop('onHandler'), offHandler)
    
    // Switch the colors on start
    cycleHandler(true)
    
	switch (settings.cycleTime) {
    	case "5 minute1":
            log.debug "Switching color every 1 minute"
            runEvery1Minute(cycleHandler)
     	break;

    	case "5 minutes":
            log.debug "Switching color every 5 minutes"
            runEvery5Minutes(cycleHandler)
     	break;

     	case "10 minutes":
     		log.debug "Switching color every 10 minutes"
     		runEvery10Minutes(cycleHandler)
     	break;
    
		case "15 minutes":
             log.debug "Switching color every 15 minutes"
             runEvery15Minutes(cycleHandler)
    	break;
    
        case "30 minutes":
            log.debug "Switching color every 30 minutes"
            runEvery30Minutes(cycleHandler)
        break;
    
	    case "1 hour":
            log.debug "Switching color every hour"
            runEvery1Hour(cycleHandler)
	    break;
    
    	case  "3 hours":
            log.debug "Switching color every 3 hours"
            runEvery3Hours(cycleHandler)
     	break;
      
	 	default:
     		log.debug "Switching color every 5 minutes (default)"
     		runEvery5Minutes(cycleHandler)
     	break;
	}
}


def cycleHandler(turningOn = false) {
    String id
    String msg = ''
    def devices = []
    
    if (! inRuntime('cycleHandler')) {
    	offHandler()
        return
    }
    
    def color = getColor()
   
	for (device in settings.whichLights) {
        if (turningOn == false && settings.honorOffLights && device.currentSwitch == "off") {
            log.debug "cycleHandler(${device}): is off, skipping (turningOn: ${turningOn} settings.honorOffLights: ${settings.honorOffLights})"
        	continue
        }

        if (settings.groupControl == false) {
            color = getColor(device)
            msg += "$device -> ${color._name}, "
        } else {
        	msg += "$device, "
        }

        color['hex'] = colorUtil.hslToHex(color['hue'], color['saturation'])
        device.setColor(color)
        // log.trace "cycleHandler($device): $color"
    }
    
    if (msg?.size() > 2) {
        msg = msg[0..-2] // trim extra comma-space
        if (settings.groupControl) {
            msg += ": -> ${color._name}"
        }
        
        log.info "$msg"
        if (settings.notification) {
	        sendNotificationEvent("$msg\n")
        }
	}
}



def getColor(device = null) {
	if (! device) {
    	device = [id: 'AllDevices']
	}
    
    Integer hue
    Integer saturation
    String name

	def random = new Random();

    if (settings.colorMode == 'From list') {
        def chosen
        
        if (state.enabledColorArray.size() == 1) {
			chosen = 0
        } else {
        	chosen = -1
            while (chosen == state["chosen_${device.id}"] || chosen == -1) {
                chosen = random.nextInt(state.enabledColorArray.size())
                log.trace "chosen = ${chosen}, state.enabledColorArray.size() = ${state.enabledColorArray.size()}"
            }
    	    // Store the chosen value for next pass to force a change
	        state["chosen_${device.id}"] = chosen
        }

        chosen = state.enabledColorArray[chosen]
        hue = chosen['hue']
        saturation = chosen['saturation']
        name = chosen['name']
    } else if (settings.colorMode == 'Random') {
        hue = random.nextInt(100) + 1
        saturation = 75 + random.nextInt(25) + 1
        state["chosen_${device.id}"] = [hue, saturation]
        name = sprintf("Random (%d%%/%d%%)", hue, saturation)
    } else { // Must be incremental
        if (state["chosen_${device.id}"]) {
        	state["chosen_${device.id}"] = state["chosen_${device.id}"] + 1
            if (state["chosen_${device.id}"] > 100) {
            	state["chosen_${device.id}"] = 1
            }
        } else {
        	// First run, start from a random place
        	state["chosen_${device.id}"] = random.nextInt(100) + 1
        }
        
        hue = state["chosen_${device.id}"]
        saturation = 100
        name = sprintf("Incremental (%d)", hue)
    }

    return [_name: name, hue: hue, saturation: saturation, level: state.brightness]
}
