/**
 *  Good Morning (weekend) + Motion detector
 *
 *  Copyright 2016 Diego
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
    name: "Good Morning (weekend) - Motion detector",
    namespace: "DiegoAntonino",
    author: "Diego",
    description: "Set Home mode to 'HOME' there are movement for 'X' Seconds.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MindYourHome.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MindYourHome@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MindYourHome@2x.png"
)

preferences {
    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?", multiple: false
    }
    section("Set Mode when there's been  movement for") {
        input "ActiveMinutes", "number", required: true, title: "Minutes?"
    }
    section("Turn On switch..."){
		input "switchOn", "capability.switch", required: false, multiple: true
	}
    section("Turn Off switch..."){
		input "switchOff", "capability.switch", required: false, multiple: true
	}
    section ("Change home to this mode"){
    	input "ModeToSet", "mode", title: "select a mode"
    }
    section ("Run When Mode is..."){
    	input "CurrentMode", "mode", title: "select a mode"
    }
    section("Select the operating mode time and days (optional)") {
		input "startTime", "time", title: "Start Time", required: false
		input "endTime", "time", title: "End Time", required: false
        input "dayOfWeek", "enum",
                title: "Which day of the week?",
                required: false,
                multiple: true,
                options: [
                        'Monday',
                        'Tuesday',
                        'Wednesday',
                        'Thursday',
                        'Friday',
                        'Saturday',
                        'Sunday'
                    ]
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
    subscribe(themotion, "motion.active", motionDetectedHandler)
    
}

def motionDetectedHandler(evt) {

    log.debug("Active motion detected from ${evt?.displayName}")
    //def curMode = location.mode
    if (location.mode == CurrentMode && operation_time() && day_week()) {    	
        	runIn(ActiveMinutes*60, checkMotion)
        }
}

def day_week() {        
     // Check the condition under which we want this to run now
    // This set allows the most flexibility.
    def doChange = false
    def calendar = Calendar.getInstance(location.timeZone);
    calendar.setTimeZone(location.timeZone)
	def currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    if((dayOfWeek.contains('Monday')) && currentDayOfWeek == Calendar.instance.MONDAY) {
            doChange = true
    }
    else if((dayOfWeek.contains('Tuesday')) && currentDayOfWeek == Calendar.instance.TUESDAY) {
            doChange = true
    }
    else if((dayOfWeek.contains('Wednesday')) && currentDayOfWeek == Calendar.instance.WEDNESDAY) {
            doChange = true
    }
    else if((dayOfWeek.contains('Thursday')) && currentDayOfWeek == Calendar.instance.THURSDAY) {
            doChange = true
    }
    else if((dayOfWeek.contains('Friday')) && currentDayOfWeek == Calendar.instance.FRIDAY) {
            doChange = true
    }
    else if((dayOfWeek.contains('Saturday')) && currentDayOfWeek == Calendar.instance.SATURDAY) {
            doChange = true
    }
    else if((dayOfWeek.contains('Sunday')) && currentDayOfWeek == Calendar.instance.SUNDAY) {
            doChange = true
    }
    
    if (doChange) {
    	log.info("Inside operating day of week")
    } else { 
    	log.info("Outside operating day of week") 
        log.debug("Operating DOW(s): $dayOfWeek")
        log.debug("Current Day of the week: $currentDayOfWeek")
        }
    return doChange
}
        

def operation_time() {
    def answer = false
    if (startTime != null && endTime != null) {
    	def currentTime = now()
        def scheduledStart = timeToday(startTime, location.timeZone).time
        def scheduledEnd = timeToday(endTime, location.timeZone).time

        if (currentTime < scheduledStart || currentTime > scheduledEnd) {
            log.info("Outside operating schedule")
            log.info("Operating StartTime ${(new Date(scheduledStart)).format("HH:mm z", location.timeZone)}, endTime ${(new Date(scheduledEnd)).format("HH:mm z", location.timeZone)}")
			log.info("Current Time:${(new Date(currentTime)).format("HH:mm z", location.timeZone)}")
        } else {
            log.info("Inside operating schedule")
            answer = true
          }
    }
    return answer
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"
    def motionState = themotion.currentState("motion")
    
    log.debug "motionState: "+motionState.value
	if (motionState.value == "active") {
    	// elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * ActiveMinutes
        // get the time elapsed between now and when the motion reported inactive
    	def elapsed = now() - motionState.date.time + 1000
        if (elapsed >= threshold) {
            log.debug "Motion has stayed active long enough since last check ($elapsed ms)"
            sendLocationEvent(name: "alarmSystemStatus", value: "off") //  Set alarm status to "Disarm" state.
            setHomeStatus(curMode,ModeToSet) // Set Home to Home Mode
            switchOn?.on() //Turn Switch on
            switchOff?.off() //Turn Switch off

        } else {
            log.debug "Motion has not stayed active long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {log.debug "Motion is INACTIVE"}
}

def setHomeStatus(currentMode,ModeToBeSet) {
    if (currentMode != ModeToBeSet) {
        if (location.modes?.find{it.name == ModeToBeSet}) {
            setLocationMode(ModeToBeSet)
            log.debug "setHomeTo: $ModeToBeSet"
        }  
        else {
            log.warn "Tried to change to undefined mode $ModeToBeSet"
        }
    }
}