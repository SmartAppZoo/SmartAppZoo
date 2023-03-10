/**
 *  Sprinklers
 *
 *  Copyright 2014 Carlo Innocenti
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
    name: "Sprinklers",
    namespace: "",
    author: "Carlo Innocenti",
    description: "Sprinklers",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
	section("Sprinklers") {
        input "sprinklersEnabled", "bool", title: "Enabled?", required: true
        input "globalSwitch", "capability.switch", title: "Global Switch", multiple: false, required: false
		input "sprinkler1", "capability.switch", title: "Sprinkler 1", multiple: false, required: true
        input "time1", "number", title: "Time sprinkler 1", required: true
		input "sprinkler2", "capability.switch", title: "Sprinkler 2", multiple: false, required: true
        input "time2", "number", title: "Time sprinkler 2", required: true
		input "sprinkler3", "capability.switch", title: "Sprinkler 3", multiple: false, required: false
        input "time3", "number", title: "Time sprinkler 3", required: false
		input "sprinkler4", "capability.switch", title: "Sprinkler 4", multiple: false, required: false
        input "time4", "number", title: "Time sprinkler 4", required: false
	}
    
    section("Starting Times") {
    	input "startTime1", "time", title: "Start time morning", required: true
        input "enabledStartTime1", "bool", title: "Morning enabled?", required: true
    	input "startTime2", "time", title: "Start time afternoon", required: false
        input "enabledStartTime2", "bool", title: "Afternoon enabled?", required: false
    	input "startTime3", "time", title: "Start time evening", required: false
        input "enabledStartTime3", "bool", title: "Evening enabled?", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

    state.running = false
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    state.running = false
    sprinkler1?.off()
    sprinkler2?.off()
    sprinkler3?.off()
    sprinkler4?.off()
	initialize()
}

def initialize() {
	unschedule()
	//schedule("0 0/5 * * * ?", timeMonitor)    
    //runEvery5Minutes(timeMonitor)
    runIn(300, timeMonitor)
    if (globalSwitch) {
    	subscribe(globalSwitch, "switch.on", globalSwitchOn, [filterEvents: false])
    	subscribe(globalSwitch, "switch.off", globalSwitchOff)
    }
	subscribe(sprinkler1, "switch.off", startSprinkler2)
	subscribe(sprinkler2, "switch.off", startSprinkler3)
	subscribe(sprinkler3, "switch.off", startSprinkler4)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
	subscribe(app, appTouch)
}

def pollerEvent(evt) {
	log.debug "[PollerEvent]"
    if (state.keepAliveLatest && now() - state.keepAliveLatest > 450000) {
    	log.error "Waking up timer"
    	timeMonitor()
    }
}

def timeMonitor() {
	log.debug "timeMonitor"
    runIn(300, timeMonitor)
    state.keepAliveLatest = now()
//	shouldWater()
    def nowTime = now()
	if (sprinklersEnabled && state.running == false) {
    	def startTime
        def startDate
        if (enabledStartTime1) {
        	startDate = timeToday(startTime1, location.timeZone)
        	startTime = startDate.getTime()
            if (nowTime > startTime && nowTime - startTime < 10 * 60 * 1000) {	// < 10 minutes past required start time
            	log.info "Activating sprinklers, start time 1: ${startDate}"
            	startSprinkler1(false)
            } else if (startTime2 && enabledStartTime2) {
	        	startDate = timeToday(startTime2, location.timeZone)
                startTime = startDate.getTime()
	            if (nowTime > startTime && nowTime - startTime < 10 * 60 * 1000) {	// < 10 minutes past required start time
	            	log.info "Activating sprinklers, start time 2: ${startDate}"
                    startSprinkler1(false)
	            } else if (startTime3 && enabledStartTime3) {
		        	startDate = timeToday(startTime3, location.timeZone)
                	startTime = startDate.getTime()
		            if (nowTime > startTime && nowTime - startTime < 10 * 60 * 1000) {	// < 10 minutes past required start time
		            	log.info "Activating sprinklers, start time 3: ${startDate}"
                    	startSprinkler1(false)
                    }
                }
            }
        }
	} else {
    	log.debug "sprinklersEnabled = ${sprinklersEnabled}; state.running = ${state.running}"
        if (state.running && (state.lastRunTime == null || nowTime - state.lastRunTime > 120 * 60 * 1000)) {	//state.running seems off; reset it and make sure everything is off
        	log.warn "state.running seems off; resetting it and making sure everything is off"
            state.running = false
            sprinkler1?.off()
            sprinkler2?.off()
            sprinkler3?.off()
            sprinkler4?.off()
        }
    }
}


def waitForUpdate(device, endState) {
	log.debug "About to check $device for $endState"
    pause(1000)
	if (device && endState) {
    	for (def i = 0; i < 5 ; i++ ) {
            if (device.currentValue("switch").toString() == endState) {
            	log.debug "Device ${device} is indeed ${endState}"
            	return true
            } else {
            	log.debug "Checking #$i, expected $device to be $endState but was ${device.currentValue("switch")}"
                if (endState == "on")
                	device.on()
                else
                	device.off()
            	pause(2000)
            }
        }
    }
    return false
}

def startSprinkler1(bForce) {
	if (bForce || shouldWater()) {
        log.info "Turning on Sprinker 1 for ${time1} minutes"
        state.running = true
        sprinkler1.on()
        waitForUpdate(sprinkler1, "on")
        state.lastRunTime = now()
        def nowTime = now()
        def theDate = new Date(nowTime + (time1 * 60 * 1000))
        runOnce(theDate, stopSprinkler1)
    } else {
		state.running = false
    	globalSwitch.off()
    	log.info "Sprinkler activation requested and declined"
    }
}

def stopSprinkler1() {
	log.info "Stopping Sprinkler 1"
    state.running = true
	sprinkler1.off()
    waitForUpdate(sprinkler1, "off")
}

def startSprinkler2(evt) {
	if (state.running == true) {
        log.info "Turning on Sprinker 2 for ${time2} minutes"
	    try {unschedule(stopSprinkler1)} catch(e) {log.error "Ignoring error: ${e}"}
        sprinkler2.on()
        waitForUpdate(sprinkler2, "on")
        def nowTime = now()
        def theDate = new Date(nowTime + (time2 * 60 * 1000))
        runOnce(theDate, stopSprinkler2)
    }
}

def stopSprinkler2() {
	log.info "Stopping Sprinkler 2"
    state.running = true
	sprinkler2.off()
    waitForUpdate(sprinkler2, "off")
}

def startSprinkler3(evt) {
	try {unschedule(stopSprinkler2)} catch(e) {log.error "Ignoring error: ${e}"}
	if (state.running == true && sprinkler3) {
		log.info "Turning on Sprinker 3 for ${time3} minutes"
        sprinkler3.on()
	    waitForUpdate(sprinkler3, "on")
        def nowTime = now()
        def theDate = new Date(nowTime + (time3 * 60 * 1000))
        runOnce(theDate, stopSprinkler3)
    } else {
    	state.running = false
        globalSwitch.off()
    }
}

def stopSprinkler3() {
	log.info "Stopping Sprinkler 3"
	if (sprinkler3) {
	    state.running = true
		sprinkler3.off()
	    waitForUpdate(sprinkler3, "off")
    } else {
    	state.running = false
        globalSwitch.off()
    }
}

def startSprinkler4(evt) {
	try {unschedule(stopSprinkler3)} catch(e) {log.error "Ignoring error: ${e}"}
	if (state.running == true && sprinkler4) {
		log.info "Turning on Sprinker 4 for ${time4} minutes"
        sprinkler4.on()
	    waitForUpdate(sprinkler4, "on")
        def nowTime = now()
        def theDate = new Date(nowTime + (time4 * 60 * 1000))
        runOnce(theDate, stopSprinkler4)
    } else {
    	state.running = false
        globalSwitch.off()
    }
}

def stopSprinkler4() {
	log.info "Stopping Sprinkler 4"
	if (sprinkler4) {
	    state.running = true
		sprinkler4.off()
	    waitForUpdate(sprinkler4, "off")
    }
    state.running = false
    globalSwitch.off()
}

def shouldWater() {
	def enoughWater = true
	try {
        def rainYesterdayStr = getWeatherFeature("yesterday").history.dailysummary.precipm[0]
        def rainYesterday = (rainYesterdayStr == "T") ? 0.1 : rainYesterdayStr.toDouble()
        def rainTodayStr = getWeatherFeature("conditions").current_observation.precip_today_metric
        def rainToday = (rainTodayStr == "T") ? 0.1 : rainTodayStr.toDouble()
        def forecastData = getWeatherFeature("forecast").forecast.simpleforecast.forecastday
        def rainForecastToday = (forecastData.qpf_day.mm[0] != null ? forecastData.qpf_day.mm[0] : 0) + (forecastData.qpf_night.mm[0] != null ? forecastData.qpf_night.mm[0] : 0)
        def rainForecastTomorrow = (forecastData.qpf_day.mm[1] != null ? forecastData.qpf_day.mm[1] : 0) + (forecastData.qpf_night.mm[1] != null ? forecastData.qpf_night.mm[1] : 0)
        enoughWater = 	rainToday >= 1 ||
                        rainToday + rainForecastToday >= 2 ||
        				rainYesterday + rainToday > 3 ||
                        rainToday + rainForecastToday + rainForecastTomorrow > 3 ||
                        rainYesterday + rainToday + rainForecastToday + rainForecastTomorrow > 4
        def logInfo = "Rain condition: yesterday ${rainYesterday}, today ${rainToday}, today next ${rainForecastToday}, tomorrow ${rainForecastTomorrow}, need water ${!enoughWater}"
        log.info logInfo
        sendNotification(logInfo)
    } catch(e1) {
    	def errorInfo = "Error computing rain heuristics: ${e1}"
		log.error errorInfo
        sendNotification(errorInfo)
    }
    return !enoughWater
}


def globalSwitchOff(evt) {
	log.debug "globalSwitchOff: $evt, $settings"
	state.running = false
    sprinkler1?.off()
    sprinkler2?.off()
    sprinkler3?.off()
    sprinkler4?.off()
}

def globalSwitchOn(evt) {
	log.debug "globalSwitchOn: $evt, $settings"
    if (state.running) {
    	if (sprinkler1.currentSwitch == "on") {
        	log.info "Manual shut down of sprinkler 1"
            try {unschedule(stopSprinkler1)} catch(e) {log.error "Ignoring error: ${e}"}
        	stopSprinkler1()
        } else if (sprinkler2.currentSwitch == "on") {
        	log.info "Manual shut down of sprinkler 2"
            try {unschedule(stopSprinkler2)} catch(e) {log.error "Ignoring error: ${e}"}
	    	stopSprinkler2()
        } else if (sprinkler3 && sprinkler3.currentSwitch == "on") {
        	log.info "Manual shut down of sprinkler 3"
            try {unschedule(stopSprinkler3)} catch(e) {log.error "Ignoring error: ${e}"}
	    	stopSprinkler3()
        } else if (sprinkler4 && sprinkler4.currentSwitch == "on") {
        	log.info "Manual shut down of sprinkler 4"
            try {unschedule(stopSprinkler4)} catch(e) {log.error "Ignoring error: ${e}"}
        	stopSprinkler4()
        } else {
        	log.error "State is flagged as running, but no switches are reported on; timing issue?"
			state.running = false
            globalSwitch.off()
        }
    } else {
    	startSprinkler1(true)
    }
}

def appTouch(evt)
{
	log.debug "appTouch: $evt, $settings"
    globalSwitch.on()
}


