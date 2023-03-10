/**
 *  Away Lights
 *
 *  Copyright 2016 Chris Liebman
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
    name: "Away Lights",
    namespace: "liebman",
    author: "Chris Liebman",
    description: "Randomly control lights when away",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "Setup", title: "Setup", install: true, uninstall: true) {
        section("Settings") {
            input "switches", "capability.switch", title: "Control these switches...", multiple: true
            input "modes", "mode", title: "In these modes...", multiple: true
            input "modeDelay", "number", title: "Delay in minutes from mode change to first light change"
            input "active", "number", title: "Active switch count"
            input "interval", "number", title: "Minutes between changes"
            input "intervalDelay", "number", title: "increase interval by up to this randomly"
            input "starting", "time", title: "Start time"
            input "startingDelay", "number", title: "random delay for start time"
            input "ending", "time", title: "End time"
            input "endingDelay", "number", title: "random delay for end time"
            input "debugEvents", "bool", title: "send debug messages as events", defaultValue: false
            input "logger", "capability.switch", title: "LogDevice:", required: false
        }
    }
}

def getVersion() {
   return "0.9"
}

def installed() {
	logit "trace", "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	logit "trace", "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
    logit "trace", "initialize() version:${version}"

    if (modes) {
        logit "debug", "subscribing to mode changes"
        subscribe(location, "mode", modeChangeHandler)
    }

    if (starting) {
        logit "debug", "scheduling starting time: ${starting}"
        schedule(starting, startTimeHandler)
        if (ending) {
            logit "debug", "scheduling ending time: ${ending}"
            schedule(ending, endTimeHandler)
        }
    }
    doActivity()
}

def debug(name, value) {
    logit "debug", "${name}: ${value}"
    if (debugEvents) {
	    sendEvent(linkText:app.label, name:name, value:value, eventType:"SOLUTION_EVENT", displayed: true)
    }
}

// called when the mode changes
def modeChangeHandler(evt) {
    debug("modeChangeHandler", evt.value)
    if (shouldBeActive()) {
        logit "debug", "should be active so making it so!"
        if (modeDelay) {
            logit "debug", "scheduling intervalHandler to run in ${modeDelay} minutes"
            runIn(modeDelay*60, intervalHandler)
        } else {
            doActivity()
        }
    }
}

// called when time window starts
def startTimeHandler(evt) {
    debug("startTimeHandler", "called")
    def delay = getRandomDelay(startingDelay)
    debug("startTimeHandler", "delaying ${delay} minutes")
    runIn(delay*60, doActivity())
}

// called when time window ends
def endTimeHandler(evt) {
    debug("endTimeHandler", "called")
    def delay = getRandomDelay(startingDelay)
    debug("endTimeHandler", "delaying ${delay} minutes")
    runIn(getRandomDelay(endingDelay)*60, endTimeActivity)
}

def endTimeActivity() {
    debug("endTimeHandler", "called")
    // if we are still in the mode, turn all the managed lights off.
    if (inMode()) {
        debug("endTimeHandler", "managed lights off")
        switches.off()
    }
}

// called each interval to change lights
def intervalHandler(evt) {
    debug("intervalHandler",  "called")
    doActivity()
}

def doActivity() {
    if (shouldBeActive()) {
        randomLights()
        scheduleInterval()
    }
}

def randomLights() {
    debug("randomLights", "called")
    def off = switches
    logit "debug", "switches: ${off}"
    def on = getRandomElements(off, active, true)
    logit "debug", "on switches: ${on}"
    on.each { it.on() }
    logit "debug", "off switches: ${off}"
    off.each { it.off() }
}

def scheduleInterval() {
    def delay = computeNextInterval(interval, intervalDelay)
    debug("scheduleInterval", "${delay} minutes")
    // must use runIn() as cron scheduling only works for smaller values
    runIn(delay*60, intervalHandler)
}

// compute a value with random delay
def computeNextInterval(value, delay) {
    debug("computeNextInterval", "${value}, ${delay}")
    def random = getRandomDelay(delay)
    def result = value + random
    return result
}

def getRandomDelay(maxDelay) {
    return new Random().nextInt(maxDelay)
}

// fetch and maybe remove <number> random items from <delagate>
def getRandomElements(delegate, number, remove) {
    def rnd = new Random()

    if(number < 1) {
        number = 1
    }

    def tempList = []
    def counter = 0

    // if asking for all or more than we have, return all
    if (number >= delegate.size()) {
        tmpList.addAll(delegate)
        delegate.removeAll()
        return tmpList
    }


    while(counter < number) {
        def index = rnd.nextInt(delegate.size())
        tempList.add(delegate[index])
        if (remove) {
            delegate.remove(index)
        }
        counter++
    }
    return tempList
}

def shouldBeActive() {
    def result = inMode() && inTimeWindow()
    return result
}

def inMode() {
    def result = true
    if (modes) {
        result = modes.contains(location.mode)
    }
    return result
}

def inTimeWindow() {
    def now = now()
    def result = true
    if (starting && ending) {
        result = now >= startTime &&
                 now < endTime
    }
	return result
}

def getStartTime() {
    def date = timeToday(starting, location.timeZone)
    return date.time
}

def getEndTime() {
    def date = timeTodayAfter(starting, ending, location.timeZone)
    return date.time
}

def logit(level, message) {
    if (logger) {
        logger.log(level, app.label, message)
    }
    
    switch(level) {
        case "trace": log.trace(message); break;
        case "debug": log.debug(message); break;
        case "info":  log.info(message);  break;
        case "warn":  log.warn(message);  break;
        case "error": log.error(message); break;
        default:      log.info(message);  break;
    }
}

