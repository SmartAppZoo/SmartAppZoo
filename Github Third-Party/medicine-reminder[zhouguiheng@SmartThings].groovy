/**
 *  Lock Door after Closed with Retries
 *
 *  Copyright 2018 Vincent
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
    name: "Medicine Reminder",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Remind taking medicine",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Select the medicine motion sensor:") {
		input "medicineSensor", "capability.motionSensor", required: true
	}
    section("Select the location motion sensor:") {
    	input "locationSensor", "capability.motionSensor", required: true
    }
    section("Select the presence sensor:") {
    	input "presenceSensor", "capability.presenceSensor", required: true
    }
    section("Select the tone device:") {
    	input "toneDevice", "capability.tone", required: true
    }
    section("How to play sound:") {
        input "playType", "enum", title: "Type", options: ["beep", "playSound"], required: true
        input "soundNumber", "number", title: "Sound number for playSound", required: false, defaultValue: 1
        input "doneSoundNumber", "number", title: "Sound number for playSound when motion detected", required: false
    }
    section("Between what time?") {
    	input "fromTime", "time", title: "From", required: true
        input "toTime", "time", title: "To", required: true
    }
    section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
    section("Time to remind regardless location / presence sensor:") {
    	input "finalTime", "time", title: "Time to remind", required: true
    }
    section("Debug logging") {
        input "enableLog", "bool", title: "Enable?", defaultValue: false
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(medicineSensor, "motion.active", medicineTaken)
	subscribe(locationSensor, "motion.active", nearMedicine)
    schedule(finalTime, maybeRemind)
    state.lastMedicineTakenTime = 0
}

def hoursSinceLastMedicineTaken() {
	state.lastMedicineTakenTime == null ? 100 : (now() - state.lastMedicineTakenTime) / 1000 / 3600
}

def medicineTaken(evt) {
	logDebug "medicineTaken() at ${now()}"
	state.lastMedicineTakenTime = now()
    if (playType != "beep" && doneSoundNumber != null) {
        toneDevice.playSound(doneSoundNumber)
    }
}

def nearMedicine(evt) {
	logDebug "nearMedicine()"
    if (presenceSensor.latestValue("presence") != "present") return
	maybeRemind(evt)
}

def maybeRemind(evt) {
	logDebug "maybeRemind(), hours: ${hoursSinceLastMedicineTaken()}"
	if (hoursSinceLastMedicineTaken() < 6) return
    logDebug "More than 6 hours"
    if (!timeOfDayIsBetween(toDateTime(fromTime), toDateTime(toTime), new Date(), location.timeZone)) return
    logDebug "Within given time"
    
    def df = new java.text.SimpleDateFormat("EEEE")
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    if (!days.contains(day)) return
    logDebug "In given days"

    if (playType == "beep") {
        toneDevice.beep()
    } else {
        toneDevice.playSound(soundNumber)
    }
}

def logDebug(msg) {
    if (enableLog) log.debug msg
}