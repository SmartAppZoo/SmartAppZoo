/**
 *  Garage Door Monitor V3
 *
 *  Copyright 2017 Joel Jayson Clark
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
    name: "Garage Door Monitor V3",
    namespace: "jc214809",
    author: "Joel Jayson Clark",
    description: "Will shut garage door ",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Doors to Monitor") {
        input "doors", "capability.contactSensor", title: "Which doors?", multiple: true
    }

    section("Monitoring Type") {
        paragraph "Do you want to monitor the garage door at specific times of the day, or based on the sunset?"
        input(name: "monitoringType", type: "enum", title: "Monitoring Type", options: ["Time of Day", "Sunset"])
    }

    section("Time of Day Monitoring") {
        paragraph "If you selected 'Time of Day' monitoring, what times do you want to start/stop monitoring?"
        input "startMonitoring", "time", title: "Start Monitoring (hh:mm 24h)", required: false
        input "stopMonitoring", "time", title: "Stop Monitoring (hh:mm 24h)", required: false
    }

    section("Sunset Monitoring") {
        paragraph "If you selected 'Sunset' Monitoring, how long before or after sunset do you want the check to happen? (optional)"
        input "sunsetOffsetValue", "text", title: "HH:MM", required: false
        input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before", "After"]]
    }

    section("Zipcode (optional)") {
        paragraph "Zip code for 'Sunset' Monitoring"
        input "zipCode", "text", title: "Zip Code", required: false
    }

    section("Alert Thresholds") {
        paragraph "How many minutes should the door be open in the before an alert is fired?  Note doors are only checked every 5 minutes, so you should select a multiple of 5 (0, 5, 10, etc)"
        input "threshold", "number", title: "Minutes (use multiples of 5)", defaultValue: 5, required: true
    }

    section("Notifications") {
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
    }
}

//
// Functions
//
def installed() {
    initialize()
}

def updated() {
    unschedule("sunsetHandler")
    unschedule("sunriseHandler")

    initialize()
}

def initialize() {
    state.monitoring = false
    state.opened = [:]
    state.threshold = 0
    state.timeToClose = null;

    doors?.each { door ->
            state.opened.put(door.displayName, false)
    }

    // only hook into sunrise/sunset if we're doing Sunset monitoring
    if (monitoringType == "Sunset") {
        subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
        astroCheck()
    }

    runEvery5Minutes(checkDoors)
}

def sunriseSunsetTimeHandler(evt) {
    log.trace "sunriseSunsetTimeHandler()"

    astroCheck()
}

def astroCheck() {
    def s

    if (monitoringType != "Sunset") {
        return
    }

    if (zipCode) {
        s = getSunriseAndSunset(zipCode: zipCode, sunsetOffset: sunsetOffset)
    } else {
        s = getSunriseAndSunset(sunsetOffset: sunsetOffset)
    }

    def now = new Date()
    def sunsetTime = s.sunset
    def sunriseTime = s.sunrise

    log.debug "sunsetTime: $sunsetTime, sunriseTime: $sunriseTime"

    if (state.sunsetTime != sunsetTime.time || state.sunriseTime != sunriseTime.time) {
        unschedule("sunsetHandler")
        unschedule("sunriseHandler")

        // change to the next sunset
        if (sunsetTime.before(now)) {
            log.info "After sunset, starting monitoring"

            state.monitoring = true

            sunsetTime = sunsetTime.next()
        }

        if (sunriseTime.before(now)) {
            log.info "Before sunrise, starting monitoring"

            state.monitoring = true

            sunriseTime = sunriseTime.next()
        }

        state.sunsetTime = sunsetTime.time
        state.sunriseTime = sunriseTime.time

        log.info "Scheduling sunset handler for $sunsetTime, sunrise for $sunriseTime"

        schedule(sunsetTime, sunsetHandler)
        schedule(sunriseTime, sunriseHandler)
    }
}

def sunriseHandler() {
    log.info "Sunrise, stopping monitoring"
    state.monitoring = false
}

def sunsetHandler() {
    log.info "Sunset, starting monitoring"
    state.monitoring = true
}

def checkDoors() {
    log.debug("checkDoors: What is timeToClose? ${state.timeToClose}")
    // if we're doing Time of Day monitoring, see if this is within the times they specified
    if (monitoringType == "Time of Day") {
        def currTime = now()
        def eveningStartTime = timeToday(startMonitoring)
        def morningEndTime = timeToday(stopMonitoring)

        state.monitoring = currTime >= eveningStartTime.time || currTime <= morningEndTime.time
    }

    log.info "checkDoors: Should we check? $state.monitoring"

    if (!state.monitoring) {
        return
    }
    doors?.each{ door ->
        def doorName = door.displayName
        def doorOpen = checkDoor(door)
        def readableNowTime = new Date(now()).format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("EST"));
        log.debug("checkDoors: readableNowTime: ${readableNowTime}")

        if (doorOpen == "open") {
            log.debug("checkDoors: Should times be set? ${state.timeToClose == null}")
            if (state.timeToClose == null) {
                //state.timeToClose = new Date(now() + 9*60*1000);
                state.timeToClose = new Date(now() + 5*60*1000).format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("EST"));
                //def state.timeToClose = new Date(state.timeToClose).format("", TimeZone.getTimeZone("UTC")); 
            }
            // previously closed, now open
            log.debug("checkDoors: When to close ${state.timeToClose}")
            log.debug("checkDoors: state.timeToClose: ${state.timeToClose}")
            log.debug("checkDoors: Compare is ${timeToday(state.timeToClose, location.timeZone).time > timeToday(readableNowTime, location.timeZone).time}")
            if (timeToday(state.timeToClose, location.timeZone).time < timeToday(readableNowTime, location.timeZone).time) {
                log.debug("checkDoors: About to close door")
                door.close()
                log.debug("checkDoors: Door closing")
                log.debug("checkDoors: The Door object is $door")
                runIn(60, resetDoor());
            } else {
                send("Alert: The $doorName is open")
            }
        } else if (doorOpen == "closed") {
            // Door closed before threshold, reset threshold
            state.timeToClose = null;
            log.debug("checkDoors: Door is Closed. Time to close is ${state.timeToClose}")
        }
        log.debug("checkDoors: End " + state.opened[doorName])
    }
}

private send(msg) {
    if (sendPushMessage != "No") {
        sendPush(msg)
    }
    log.debug msg
}

def checkDoor(door) {
    def latestValue = door.currentValue("contact")
}

def resetDoor() {
    doors?.each { door ->
        if (checkDoor(door)) {
    	    log.debug("checkDoors: Door closing")
            send("Alert: The $door.displayName has been open for too long and is now closing")
            state.timeToClose = null; 
        }
    }
    //log.debug("checkDoors: The Door object in resetDoor is $door")
    //def value = checkDoor(door)
    //log.debug("checkDoors: The door is $value")
}

private getLabel() {
    app.label ?: "SmartThings"
}

private getSunsetOffset() {
    sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}