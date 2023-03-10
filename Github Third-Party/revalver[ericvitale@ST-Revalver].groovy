/**
 *  Revalver
 *
 *  Version 1.0.2 = 08/22/16
 *   -- Added: Ability to turn a valve on / off based on a temperature & time period.
 *  Version 1.0.1 - 08/19/16
 *   -- Added: Schedule Trigger
 *   -- Added: Toggle Trigger
 *  Version 1.0.0 - 08/17/16
 *   -- Initial Build
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
 *  You can find this smart app @ https://github.com/ericvitale/ST-Revalver
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */
 
definition(
    name: "${appName()}",
    namespace: "ericvitale",
    author: "Eric Vitale",
    description: "Manage your valves with SmartThings like a boss.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/ev-public/st-images/revalver-icon.png", 
    iconX2Url: "https://s3.amazonaws.com/ev-public/st-images/revalver-icon-2x.png", 
    iconX3Url: "https://s3.amazonaws.com/ev-public/st-images/revalver-icon-3x.png",
    singleInstance: true)

preferences {
    page(name: "startPage")
    page(name: "parentPage")
    page(name: "childStartPage")
}

def startPage() {
    if (parent) {
        childStartPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: false, uninstall: true) {
        section("Create a new child app.") {
            app(name: "childApps", appName: appName(), namespace: "ericvitale", title: "New Valve Automation", multiple: true)
        }
    }
}
 
def childStartPage() {
	return dynamicPage(name: "childStartPage", title: "", install: true, uninstall: true) {
        
        section("Trigger Type") {
        	input "triggerType", "enum", title: "Trigger Type", required: true, multiple: false, options: ["Contact", "Schedule", "Switch", "Temperature", "Toggle", "Water Sensor"], submitOnChange: true
        }
        
        if(triggerType == "Water Sensor") {
            section("Water Sensor") {
                input "waterSensors", "capability.waterSensor", title: "Which?", required: false, multiple: true
            }
        } else if(triggerType == "Switch") {
            section("Trigger") {
                input "switches", "capability.switch", title: "Switches", multiple: true, required: false, submitOnChange: true
                if(switches != null) {
                    input "closeEvent", "enum", title: "Close Valve When?", required: true, multiple: false, options: ["On", "Off", "Never", "Timer"], defaultValue: "On", submitOnChange: true
                    if(closeEvent == "Timer") {
                    	input "closeTimerLength", "number", title: "Close After?", required: true, range: "1..*"
                        input "closeTimerUnit", "enum", title: "Unit?", required: true, options: getTimeUnits(), defaultValue: "Minutes"
                    }
                    input "openEvent", "enum", title: "Open Valve When?", required: true, multiple: false, options: ["On", "Off", "Never", "Timer"], defaultValue: "Never", submitOnChange: true
                    if(openEvent == "Timer") {
                    	input "openTimerLength", "number", title: "Open After?", required: true, range: "1..*"
                        input "openTimerUnit", "enum", title: "Unit?", required: true, options: getTimeUnits(), defaultValue: "Minutes"
                    }
                }
            }
        } else if(triggerType == "Schedule") {
        	section("Schedule") {
            	input "closeTime", "time", title: "Close daily at?", required: false
                input "openTime", "time", title: "Open daily at?", required: false
            }
        } else if(triggerType == "Contact") {
        	section("Contact Sensor Subscriptions") {
            	input "contacts", "capability.contactSensor", title: "Which?", required: false, multiple: true, submitOnChange: true
                if(contacts != null) {
            		input "closeEvent", "enum", title: "Close Valve When?", required: true, multiple: false, options: ["Open", "Closed", "Never", "Timer"], defaultValue: "Open"
                	input "openEvent", "enum", title: "Open Valve When?", required: true, multiple: false, options: ["Open", "Closed", "Never", "Timer"], defaultValue: "Never"
                }
            }
        } else if(triggerType == "Toggle") {
        	section("Toggle") {
        		input "toggleTimerLength", "number", title: "Toggle Every?", required: true, range: "1..*"
            	input "toggleTimerUnit", "enum", title: "Unit?", required: true, options: getToggleTimeUnits(), defaultValue: "Minutes"
            }
        } else if(triggerType == "Temperature") {
        	section("Temperature") {
            	input "temperatureSensors", "capability.temperatureMeasurement", title: "Which?", required: true, multiple: true, submitOnChange: true
                if(temperatureSensors != null) {
                	input "closeEvent", "enum", title: "Close Valve When?", required: true, multiple: false, options: getTemperatureEventList(), submitOnChange: true
                    if(closeEvent in getTemperatureEventList(false)) {
                    	input "closeTemperature", "number", title: "Close When ${closeEvent}", required: true, range: "*..*"
                    }
                    input "openEvent", "enum", title: "Open Valve When?", required: true, multiple: false, options: getTemperatureEventList(), submitOnChange: true
					if(openEvent in getTemperatureEventList(false)) {
                    	input "openTemperature", "number", title: "Open When ${openEvent}", required: true, range: "*..*"
                    }
                }
            }
        }
        
        section("Valves") {
        	input "valves", "capability.valve", title: "Valves?", required: false, multiple: true
        }
        
        section("Lights") {
            input "useLights", "bool", title: "Turn on Lights?", required: true, defaultValue: false, submitOnChange: true
            if(useLights) {
				input "lights", "capability.switch", title: "Which?", required: false, multiple: true
            }
        }
        
        section("Electronic Notifications") {
        	input "push", "bool", title: "Send Push Notifications?", required: true, defaultValue: true
        }
        
        section("Test This Automation") {
        	input "testAutomation", "bool", title: "Test This Automation?", required: true, defaultValue: false, submitOnChange: true
            if(testAutomation) {
            	input "testLights", "capability.switch", title: "Which Light?", required: false, multiple: true
            }
        }
        
        section("Time Range") {
            input "useTimeRange", "bool", title: "Use Time Range?", required: true, defaultValue: false, submitOnChange: true
        	if(useTimeRange) {
                input "startTimeSetting", "enum", title: "Start Time", required: true, defaultValue: "None", options: ["None", "Sunrise", "Sunset", "Custom"], submitOnChange: true
                if(startTimeSetting == "Custom") {
                    input "startTimeInput", "time", title: "Custom Start Time", required: true
                } else if(startTimeSetting == "Sunrise" || startTimeSetting == "Sunset") {
                    input "startTimeOffset", "number", title: "Offset ${startTimeSetting} by (Mins)...", range: "*..*"
                }
                input "endTimeSetting", "enum", title: "End Time", required: true, defaultValue: "None", options: ["None", "Sunrise", "Sunset", "Custom"], submitOnChange: true
                if(endTimeSetting == "Custom") {
                    input "endTimeInput", "time", title: "Custom End Time", required: true
                } else if(endTimeSetting == "Sunrise" || endTimeSetting == "Sunset") {
                    input "endTimeOffset", "number", title: "Offset ${endTimeSetting} by (Mins)...", range: "*..*"
                }
            }
        }
        
        section("Settings") {
        	label(title: "Assign a name", required: false)
            input "active", "bool", title: "Rules Active?", required: true, defaultValue: true
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
        }
	}
}

private def appName() { return "${parent ? "Valve Automation" : "Revalver"}" }

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "Revalver -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "Revalver -- Invalid Log Setting of ${type}"
        }
    }
}

def installed() {
	log("Begin installed.", "DEBUG")
	initialization() 
    log("End installed.", "DEBUG")
}

def updated() {
	log("Begin updated().", "DEBUG")
	unsubscribe()
    unschedule()
	initialization()
    log("End updated().", "DEBUG")
}

def initialization() {
	log("Begin initialization().", "DEBUG")
    
    log("triggerType = ${triggerType}.", "INFO")
    if(closeEvent != null) {
    	log("closeEvent = ${closeEvent}.", "INFO")
        if(closeEvent == "Timer") {
       		log("closeTimerLength = ${closeTimerLength}.", "INFO")
            log("closeTimerUnit = ${closeTimerUnit}.", "INFO")
        }
        if(openEvent == "Timer") {
        	log("openTimerLength = ${openTimerLength}.", "INFO")
            log("openTimerUnit = ${openTimerUnit}.", "INFO")
        }
        
        if(lights != null) {
        	lights.each { it->
            	log("Light: it.label selected to turn on with valve action.", "INFO")
            }
        } else {
        	log("No lights selected.", "INFO")
        }
        
        if(push) {
        	log("Push notifications are enabled.", "INFO")
        } else {
	        log("Push notifications are disabled.", "INFO")
        }
        
        if(triggerType == "Schedule") {
        	log("closeTime = ${closeTime}.", "INFO")
            log("openTime = ${openTime}.", "INFO")
        }
        
        if(testAutomation) {
        	log("Automation testing is enabled.", "INFO")
            if(testLights != null) {
            	log("The following lights are selected for testing this automation.", "INFO")
            	testLights.each { it->
                	log("${it.label}", "INFO")
                }
            } else {
            	log("No lights selected for testing.", "WARN")
            }
        }
    }
    
    if(parent) {
    	if(triggerType == "Water Sensor") {
        	initWaterSensorChild()
        } else if(triggerType == "Switch") {
        	initSwitchChild()
        } else if(triggerType == "Schedule") {
        	initScheduleChild()
        } else if(triggerType == "Contact") {
        	initContactChild()
        } else if(triggerType == "Toggle") {
        	initToggleChild()
        } else if(triggerType == "Temperature") {
        	initTemperatureChild()
        }
    } else {
    	initParent() 
    }
    
    log("End initialization().", "DEBUG")
}

def initParent() {
	log("initParent()", "DEBUG")
}

def initWaterSensorChild() {
	log("Begin initWaterSensorChild().", "DEBUG")
    log("active = ${active}.", "INFO")
    log("triggerType = ${triggerType}.", "INFO")
    
    unsubscribe()
    
    if(!active) {
        log("Application is not active, ignoring further initialization tasks.", "INFO")
        log("End initialization().", "DEBUG")
        return
	}
    
    if(waterSensors != null) {
    	waterSensors.each { it->
        	log("Water Sensor ${it.label} selected.", "INFO")
        }
    	subscribe(waterSensors, "water.wet", waterHandler)
        log("Subscribed to water sensor events.", "INFO")
    }
    
    if(useTimeRange) {
    	setupTimes()
    }
    
    initValves()

    log("End initWaterSensorChild().", "DEBUG")
}

def initTemperatureChild() {
	log("Begin initTemperatureChild().", "DEBUG")
    log("active = ${active}.", "INFO")
    log("triggerType = ${triggerType}.", "INFO")
    
    unsubscribe()
    
    if(!active) {
        log("Application is not active, ignoring further initialization tasks.", "INFO")
        log("End initialization().", "DEBUG")
        return
	}
    
    if(triggerType == "Temperature") {
        log("Temperature sensors selected: ...", "INFO")
        if(temperatureSensors != null) {
            def averageTemp = 0.0
            temperatureSensors.each { it->
            log("Temperature Sensor: ${it.label} is selected and is currently reading ${it.currentValue("temperature")}.", "INFO")
                averageTemp += it.currentValue("temperature")
            }

            averageTemp = (averageTemp / temperatureSensors.size())
            log("closeEvent = ${closeEvent}.", "INFO")
            log("closeTemperature = ${closeTemperature}.", "INFO")
            log("openEvent = ${openEvent}.", "INFO")
            log("openTemperature = ${openTemperature}.", "INFO")
            log("Current average temperature is ${averageTemp}.", "INFO")
        }
    }
    
    if(useTimeRange) {
    	setupTimes()
    }
    
    initValves()

    log("End initTemperatureChild().", "DEBUG")
}

def initSwitchChild() {
	log("Begin initSwitchChild().", "DEBUG")
    log("active = ${active}.", "INFO")
    
    unsubscribe()
    
    if(!active) {
        log("Application is not active, ignoring further initialization tasks.", "INFO")
        log("End initialization().", "DEBUG")
        return
	}
    
    if(closeEvent == openEvent) {
    	log("You cannot set the close event and the open event to the same value. Defaulting to close when switch is on and never open valve.", "WARN")
        closeEvent = "On"
        openEvent = "Never"
    }
    
    if(switches != null) {
        switches.each { it->
        	log("Switch ${it.label} selected.", "INFO")
        }
    	subscribe(switches, "switch", switchHandler)
        log("Subscribed to switch events.", "INFO")
    }
    
    if(useTimeRange) {
    	setupTimes()
    }
    
    initValves()
    
    log("End initSwitchChild().", "DEBUG")
}

def initScheduleChild() {
	log("Begin initScheduleChild().", "DEBUG")
    log("active = ${active}.", "INFO")
    unsubscribe()
    unschedule()
    
    if(!active) {
        log("Application is not active, ignoring further initialization tasks.", "INFO")
        log("End initialization().", "DEBUG")
        return
	}
    
    if(useTimeRange) {
    	setupTimes()
    }
    
    initValves()
    
    if(closeTime != null) {
    	if(testAutomation) {
        	schedule(closeTime, testLightsOn)
        } else {
        	schedule(closeTime, closeValves)
        }
    }
    
    if(openTime != null) {
    	if(testAutomation) {
        	schedule(openTime, testLightsOff)
        } else {
        	schedule(openTime, openValves)
        }
    }
    
    log("End initScheduleChild().", "DEBUG")
}

def initContactChild() {
	log("Begin initContactChild().", "DEBUG")
    log("active = ${active}.", "INFO")
    unsubscribe()
    
    if(!active) {
        log("Application is not active, ignoring further initialization tasks.", "INFO")
        log("End initialization().", "DEBUG")
        return
	}
    
    if(closeEvent == openEvent) {
    	log("You cannot set the close event and the open event to the same value. Defaulting to close when contact is opened and never open valve.", "WARN")
        closeEvent = "Open"
        openEvent = "Never"
    }
    
    if(contacts != null) {
    	contacts.each { it->
        	log("Contact Sensor ${it.label} selected.", "INFO")
        }
    	subscribe(contacts, "contact", contactHandler)
        log("Subscribed to contact events.", "INFO")
    }
    
    if(useTimeRange) {
    	setupTimes()
    }
    
    initValves()
    
    log("End initContactChild().", "DEBUG")
}

def initToggleChild() {
	log("Begin initTogglechild().", "DEBUG")
    log("active = ${active}.", "INFO")
    log("triggerType = ${triggerType}.", "INFO")
    
    unschedule()
    unsubscribe()
    
    if(!active) {
        log("Application is not active, ignoring further initialization tasks.", "INFO")
        log("End initialization().", "DEBUG")
        return
	}
    
    if(useTimeRange) {
    	setupTimes()
    }
    
	initValves()
    
    log("CRON String = ${getCron(toggleTimerLength, toggleTimerUnit)}.", "DEBUG")
    schedule(getCron(toggleTimerLength, toggleTimerUnit), toggleValves)

    log("End initTogglechild().", "DEBUG")
}

def initValves() {
	log("Begin initValves", "DEBUG")
    
    if(valves != null) {
    	valves.each {it->
        	log("Valve selected: ${it.label}.", "INFO")
        }
        
        subscribe(valves, "contact", valveHandler)
        log("Subscribed to valve events.", "INFO")
    } else {
    	log("No valves selected.", "INFO")
    }
    
    log("End initValves", "DEBUG")
}

def switchHandler(evt) {
	log("Manual switch (${evt.device.label}) -- ${evt.value}.", "INFO")
    
    if(outOfRange()) {
    	log("Current time is out of range, ignoring.", "INFO")
        return
    }
    
    if(evt.value == closeEvent.toLowerCase()) {
    	if(testAutomation) {
        	log("Testing Automation: turning test lights on, simulating a valve closing.", "INFO")
        	testLightsOn()
        } else {
	        closeValves()
        }
        
        if(useLights) {
        	lightsOn()
        }
    } else if(evt.value == openEvent.toLowerCase()) {
    	if(testAutomation) {
        	log("Testing Automation: turning test lights off, simulating a valve opening.", "INFO")
        	testLightsOff()
        } else {
	        openValves()
        }
    }
}

def waterHandler(evt) {
	log("Water detected by (${evt.device.label}) -- ${evt.value}.", "INFO")
    log("Event = ${evt.descriptionText}.", "DEBUG")
    
    if(outOfRange()) {
    	log("Current time is out of range, ignoring.", "INFO")
        return
    }
    
    if(testAutomation) {
        log("Testing Automation: turning test lights on.", "INFO")
        testLightsOn()
    } else {
		closeValves()
    }
    
	if(useLights) {
    	lightsOn()
    }
}

def temperatureHandler(evt) {
	log("Temperature event detected for (${evt.device.label}) -- ${evt.value}.", "INFO")
    log("Event = ${evt.descriptionText}.", "DEBUG")
    
    if(outOfRange()) {
    	log("Current time is out of range, ignoring.", "INFO")
        return
    }
    
	evaluateTemperatures()
}

def contactHandler(evt) {
	log("Contact event by (${evt.device.label}) -- ${evt.value}.", "INFO")
    
    if(outOfRange()) {
    	log("Current time is out of range, ignoring.", "INFO")
        return
    }
    
    if(evt.value == closeEvent.toLowerCase()) {
    	if(testAutomation) {
        	log("Testing Automation: turning test lights on, simulating a valve closing.", "INFO")
        	testLightsOn()
        } else {
	        closeValves()
        }
        
        if(useLights) {
        	lightsOn()
        }
    } else if(evt.value == openEvent.toLowerCase()) {
    	if(testAutomation) {
        	log("Testing Automation: turning test lights off, simulating a valve opening.", "INFO")
        	testLightsOff()
        } else {
	        openValves()
        }
    }
}

def valveHandler(evt) {
	log("Valve event by (${evt.device.label}).", "INFO")
    log("${evt.descriptionText}.", "INFO")
    if(push) {
    	state.phrase = "${evt.descriptionText}"
    	asyncSendPushNotification()
    } 
}

def evaluateTemperatures() {
    if(temperatureSensors != null) {
    	def averageTemp = 0.0
        def belowClose = false
        def aboveClose = false
        def belowOpen = false
        def aboveOpen = false
        temperatureSensors.each { it->
	        log("Temperature Sensor: ${it.label} is selected and is currently reading ${it.currentValue("temperature")}.", "INFO")
        	averageTemp += it.currentValue("temperature")
            
            /*if(it.currentValue("temperature") > closeTemperature) {
            	aboveClose = true
            } else if(it.currentValue("temperature") <= closeTemperature) {
            	belowClose = true	
            }
            
            if(it.currentValue("temperature") > openTemperature) {
            	aboveOpen = true
            } else if(it.currentValue("temperature") <= openTemperature) {
            	belowOpen = true	
            }*/
		}
                
        averageTemp = (averageTemp / temperatureSensors.size())
        log("closeEvent = ${closeEvent}.", "DEBUG")
        log("closeTemperature = ${closeTemperature}.", "DEBUG")
        log("openEvent = ${openEvent}.", "DEBUG")
        log("openTemperature = ${openTemperature}.", "DEBUG")
        log("Current average temperature is ${averageTemp}.", "DEBUG")
	}
        
	if(useAverageTemp) {
    
    	aboveClose = false
        belowClose = false
        
    	if(averageTemp >= closeTemperature) {
        	aboveClose = true
        } else {
        	belowClose = true
        }
    }
    
    if(useAverageTemp) {
    
        aboveOpen = false
        belowOpen = false
        
        if(averageTemp >= openTemperature) {
        	aboveOpen = true
        } else {
        	belowOpen = true
        }
    }
    
    if(closeEvent == "Above") {
    	if(aboveClose == true) {
			if(testAutomation) {
            	testLightsOn()
            } else {
	            closeValves()
            }
        }
    } else if(closeEvent == "Below") {
    	if(belowClose == true) {
			if(testAutomation) {
            	testLightsOn()
            } else {
	            closeValves()
            }
        }
    }
    
    if(openEvent == "Above") {
    	if(aboveOpen == true) {
			if(testAutomation) {
            	testLightsOff()
            } else {
	            openValves()
            }
        }
    } else if(openEvent == "Below") {
    	if(belowOpen == true) {
			if(testAutomation) {
            	testLightsOff()
            } else {
	            openValves()
            }
        }
    }
}

def closeValves() {
	log("Closing valves.", "INFO")
    valves.each { it->
        if(it.currentValue("contact") == "open") {
        	it.close()
        }
        log("Closing ${it.label} now...", "INFO")
        if(push) {
        	state.phrase = "${it.label} is closing"
        	asyncSendPushNotification()
        }
        
        if(openEvent == "Timer") {
        	log("Scheduling the valve to open in ${openTimerLength} ${openTimerUnit}.", "INFO")
        	runIn(getDuration(openTimerLength, openTimerUnit), openValves)
        }
    }
}

def openValves() {
	log("Opening valves.", "INFO")
    valves.each { it->
        if(it.currentValue("contact") == "closed") {
	        it.open()
        }
        log("Opening ${it.label} now...", "INFO")
        if(push) {
        	state.phrase = "${it.label} is opening"
        	asyncSendPushNotification()
        }
        
        if(closeEvent == "Timer") {
        	log("Scheduling the valve to close in ${closeTimerLength} ${closeTimerUnit}.", "INFO")
        	runIn(getDuration(closeTimerLength, closeTimerUnit), closeValves)
        }
    }
}

def toggleValves() {
	log("Toggling valves.", "INFO")
    if(testAutomation) {
    	testLights.each { it->
        	if(it.currentValue("switch").toLowerCase() == "on") {
            	it.off()
            } else if(it.currentValue("switch").toLowerCase() == "off") {
            	it.on()
            }
        }
    } else {
    	valves.each { it->
        	if(it.currentValue("contact").toLowerCase() == "open") {
            	closeValves()
            } else if(it.currentValue("contact").toLowerCase() == "closed") {
            	openValves()
            }
        }
    }
}

def asyncSendPushNotification() {
	sendPush(state.phrase)
}

def lightsOn() {
	if(useLights) {
    	if(lights != null) {
        	lights.each { it->
            	it.on()
            }
        }
    }
}

def lightsOff() {
	if(useLights) {
    	if(lights != null) {
        	lights.each { it->
            	it.off()
            }
        }
    }
}

def testLightsOff() {
    if(testAutomation) {
    	if(testLights != null) {
        	log("Turning test lights off.", "INFO")
        	testLights.each { it->
            	it.off()
            }
        }
        if(push) {
        	state.phrase = "Testing Automation Valve Opening"
        	asyncSendPushNotification()
        }
        
        if(closeEvent == "Timer") {
        	log("Scheduling the test lights to turn on in ${openTimerLength} ${openTimerUnit} to simulate valve closing.", "INFO")
        	runIn(getDuration(closeTimerLength, closeTimerUnit), testLightsOn)
        }
    }
}

def testLightsOn() {
    if(testAutomation) {
    	if(testLights != null) {
        	testLights.each { it->
            	it.on()
            }
        }
        if(push) {
        	state.phrase = "Testing Automation Valve Closing"
        	asyncSendPushNotification()
        }
        
        if(openEvent == "Timer") {
        	log("Scheduling the test lights to turn off in ${openTimerLength} ${openTimerUnit}.", "INFO")
        	runIn(getDuration(openTimerLength, openTimerUnit), testLightsOff)
        }
    }
}

def getOpenEvents() {
	return ["On", "Off", "Never", "Timer"]
}

def getCloseEvents() {
	return ["On", "Off", "Never", "Timer"]
}

def getTimeUnits() {
	return ["Seconds", "Minutes", "Hours", "Days", "Weeks"]
}

def getToggleTimeUnits() {
	return ["Minutes", "Hours", "Days"]
}

def getTemperatureEventList() {
	return getTemperatureEventList(true)
}

def getTemperatureEventList(val) {
	if(val == true) {
    	return ["Above", "Below", "Never"]
    } else {
    	return ["Above", "Below"]
    }
}

def getDuration(val, unit) {
	switch(unit) {
    	case "Seconds":
        	return 1 * val
        case "Minutes":
        	return 60 * val
        case "Hours":
        	return 3600 * val
        case "Days":
        	return 86400 * val
        case "Weeks":
        	return 604800 * val
        default:
        	return 60 * val
    }
}

def getCron(val, unit) {
	switch(unit) {
        case "Minutes":
        	return "0 0/${val} * 1/1 * ?"
        case "Hours":
        	return "0 0 0/${val} 1/1 * ?"
        case "Days":
        	return "0 0 0 1/${val} * ?"
        default:
        	return "0 0/${val} * 1/1 * ?"
    }
}

//// Begin Time Getters / Setters ////
def getStartTime() {
	return state.theStartTime
}

def setStartTime(val) {
	state.theStartTime = val
}

def getEndTime() {
	return state.theEndTime
}

def setEndTime(val) {
	state.theEndTime = val
}

def getStartTimeType() {
    return state.theStartTimeSetting
}

def setStartTimeType(val) {
    state.theStartTimeSetting = val
}

def getEndTimeType() {
    return state.theEndTimeSetting
}

def setEndTimeType(val) {
    state.theEndTimeSetting = val
}

def getStartTimeOffset() {
	if(state.theStartTimeOffset == null) {
    	return 0
    } else {
    	return state.theStartTimeOffset
    }
}

def setStartTimeOffset(val) {
	if(val == null) {
    	state.theStartTimeOffset = 0
    } else {
    	state.theStartTimeOffset = val
    }
}

def getEndTimeOffset() {
	if(state.theEndTimeOffset == null) {
    	return 0
    } else {
    	return state.theEndTimeOffset
    }
}

def setEndTimeOffset(val) {
	if(val == null) {
    	state.theEndTimeOffset = 0
    } else {
    	state.theEndTimeOffset = val
    }
}

def getUseStartTime() {
	return state.UsingStartTime
}

def getUseEndTime() {
	return state.UsingEndTime
}

def setUseStartTime(val) {
	state.UsingStartTime = val
}

def setUseEndTime(val) {
	state.UsingEndTime = val
}	

def getCalculatedStartTime() {
	if(getStartTimeType() == "Custom") {
    	return inputDateToDate(getStartTime())
    } else if(getStartTimeType() == "Sunset") {
    	return getSunset(getStartTimeOffset())
    } else if(getStartTimeType() == "Sunrise") {
    	return getSunrise(getStartTimeOffset())
    }
}

def getCalculatedEndTime() {
	if(getEndTimeType() == "Custom") {
    	return inputDateToDate(getEndTime())
    } else if(getEndTimeType() == "Sunset") {
    	return getSunset(getEndTimeOffset())
    } else if(getEndTimeType() == "Sunrise") {
    	return getSunrise(getEndTimeOffset())
    }	
}

//// End Time Getters / Setters ////

/////// Begin Time / Date Methods ///////////////////////////////////////////////////////////

def outOfRange() {
    if(getUseStartTime() && getUseEndTime() && useTimeRange) {
        if(isBetween(getCalculatedStartTime(), getCalculatedEndTime(), getNow())) {
       		return false
        } else {
	        return true
        }
    } else {
    	return false
    }
}


def setupTimes() {
	setStartTimeType(startTimeSetting)
    setEndTimeType(endTimeSetting)
    
    if(getStartTimeType() == "None") {
        setUseStartTime(false)
    } else if (getStartTimeType() == "Sunrise") {
    	setUseStartTime(true)
        setStartTimeOffset(startTimeOffset)
    } else if (getStartTimeType() == "Sunset") {
    	setUseStartTime(true)
        setStartTimeOffset(startTimeOffset)
    } else if (getStartTimeType() == "Custom") {
    	setUseStartTime(true)
        setStartTime(startTimeInput)
    }
    
    if(endTimeType == "None") {
    	setUseEndTime(false)
    } else if (endTimeType == "Sunrise") {
    	setUseEndTime(true)
        setEndTimeOffset(endTimeOffset)
    } else if (endTimeType == "Sunset") {
    	setUseEndTime(true)
        setEndTimeOffset(endTimeOffset)
    } else if (endTimeType == "Custom") {
    	setUseEndTime(true)
        setEndTime(endTimeInput)
    }
}

def getNow() {
	return new Date()
}

def minutesBetween(time1, time2) {
	return (time1.getTime() - time2.getTime())/1000/60
}

def isBefore(time1, time2) {
	if(minutesBetween(time1, time2) <= 0) {
    	return true
    } else {
    	return false
    }
}

def isAfter(time1, time2) {
	if(minutesBetween(time1, time2) > 0) {
    	return true
    } else {
    	return false
    }
}

def isBetween(time1, time2, time3) {
	if(isAfter(time1, time2)) {
        time2 = time2 + 1
    }
    
    if(isAfter(time3, time1) && isBefore(time3, time2)) {
    	return true
    } else {
    	return false
    }
}

def getSunset() {
	return getSunset(0)
}

def getSunrise() {
	return getSunrise(0)
}

def getSunset(offset) {
	def offsetString = getOffsetString(offset)
	return getSunriseAndSunset(sunsetOffset: offsetString).sunset
}

def getSunrise(offset) {
	def offsetString = getOffsetString(offset)
	return getSunriseAndSunset(sunriseOffset: "${offsetString}").sunrise
}

def getOffsetString(offsetMinutes) {
	int hours = Math.abs(offsetMinutes) / 60; //since both are ints, you get an int
	int minutes = Math.abs(offsetMinutes) % 60;
    def sign = (offsetMinutes >= 0) ? "" : "-"
	def offsetString = "${sign}${hours.toString().padLeft(2, "0")}:${minutes.toString().padLeft(2, "0")}"
	return offsetString
}

def inputDateToDate(val) {
	return Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", val)
}

def dateToString(val) {
	log("val = ${val}.", "DEBUG")
	return val.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}

/////// End Time / Date Methods ///////////////////////////////////////////////////////////