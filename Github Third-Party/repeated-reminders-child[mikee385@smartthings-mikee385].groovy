/**
 *  Repeated Reminders
 *
 *  Copyright 2019 Michael Pierce
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
    name: "Repeated Reminders - Child",
    namespace: "mikee385",
    author: "Michael Pierce",
    parent: "mikee385:Repeated Reminders",
    description: "Child app for Repeated Reminders.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Family/App-QualityTimeTracker.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Family/App-QualityTimeTracker@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Family/App-QualityTimeTracker@2x.png")

def getTurnedOn() {
    return "Turned On"
}

def getTurnedOff() {
    return "Turned Off"
}

preferences {
    page(name: "settings")
}

def settings() {
    dynamicPage(name: "settings", title: "", install: true, uninstall: true) {
        section("Reminder Switch") {
            input "reminderSwitch", "capability.switch", title: "Which switch should start and stop the reminder?", required: true, submitOnChange: true
            if (reminderSwitch) input "reminderStart", "enum", title: "Start reminder when switch is:", options: [turnedOn, turnedOff], required: true, submitOnChange: true
            if (reminderSwitch && reminderStart) {
                if (reminderStart == turnedOn) {
                    paragraph "Reminder will start when ${reminderSwitch} is turned on and will stop when ${reminderSwitch} is turned off."
                } else {
                    paragraph "Reminder will start when ${reminderSwitch} is turned off and will stop when ${reminderSwitch} is turned on."
                }
            }
        }
        if (reminderSwitch) {
            section("Pause Switch") {
                input "pauseSwitch", "capability.switch", title: "Which switch should pause and resume the reminder? (optional)", required: false, submitOnChange: true
                if (pauseSwitch) input "reminderPause", "enum", title: "Pause reminder when switch is:", options: [turnedOn, turnedOff], required: true, submitOnChange: true
                if (pauseSwitch && reminderPause) {
                    if (reminderPause == turnedOn) {
                        paragraph "Reminder will pause when ${pauseSwitch} is turned on and will resume when ${pauseSwitch} is turned off."
                    } else {
                        paragraph "Reminder will pause when ${pauseSwitch} is turned off and will resume when ${pauseSwitch} is turned on."
                    }
                }
            }
            section("Delay") {
                input "initialDuration", "number", title: "How long to wait before repeated messages start (in seconds)?", required: true, defaultValue: 0
            }
            section("Repeated Messages") {
                input "repeatedMessage", "text", title: "What message should be shown while the reminder is running?", required: true
                input "includeDurationInMessage", "boolean", title: "Should the running time be appended to the message?", required: true, defaultValue: false, submitOnChange: true
                if (includeDurationInMessage) input "durationResolution", "enum", title: "Minumum resolution for the running time:", options: ["Days", "Hours", "Minutes", "Seconds"], required: true, defaultValue: "Seconds"
                input "repeatedDuration", "number", title: "How frequently should messages be shown (in seconds)?", required: true, defaultValue: 300
            }
            section("Optional Messages") {
                input "startMessage", "text", title: "Message shown when the reminder starts:", required: false
                input "stopMessage", "text", title: "Message shown when the reminder stops:", required: false
                input "pauseMessage", "text", title: "Message shown when the reminder pauses:", required: false
                input "resumeMessage", "text", title: "Message shown when the reminder resumes:", required: false
            }        
            section() {
                label title: "Assign a name", required: true
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(reminderSwitch, "switch.on", reminderSwitchOnHandler)
    subscribe(reminderSwitch, "switch.off", reminderSwitchOffHandler)
    subscribe(pauseSwitch, "switch.on", pauseSwitchOnHandler)
    subscribe(pauseSwitch, "switch.off", pauseSwitchOffHandler)
    
    state.startTime = now()
}

def reminderSwitchOnHandler(evt) {
    log.debug "${evt.displayName}'s ${evt.name} is ${evt.value}"
    
    if (reminderStart == turnedOn) {
        start()
    } else {
        stop()
    }
}

def reminderSwitchOffHandler(evt) {
    log.debug "${evt.displayName}'s ${evt.name} is ${evt.value}"
    
    if (reminderStart == turnedOn) {
        stop()
    } else {
        start()
    }
}

def pauseSwitchOnHandler(evt) {
    log.debug "${evt.displayName}'s ${evt.name} is ${evt.value}"
    
    if (reminderPause == turnedOn) {
        pause()
    } else {
        resume()
    }
}

def pauseSwitchOffHandler(evt) {
    log.debug "${evt.displayName}'s ${evt.name} is ${evt.value}"
    
    if (reminderPause == turnedOn) {
        resume()
    } else {
        pause()
    }
}

def start() {
    if (!isPaused) {
        log.debug "Reminder has started"
        
        if (startMessage) {
            sendPush(startMessage)
        }
        begin()
    } else {
        log.debug "Reminder was previously paused"
    }
}

def stop() {
    log.debug "Reminder has stopped"
    
    if (stopMessage) {
        sendPush(stopMessage)
    }
    end()
}

def pause() {
    if (isStarted) {
        log.debug "Reminder has been paused"
        
        if (pauseMessage) {
            sendPush(pauseMessage)
        }
        end()
    } else {
        log.debug "Reminder was not previously started"
    }  
}

def resume() {
    if (isStarted) {
        log.debug "Reminder has been resumed"
        
        if (resumeMessage) {
            sendPush(resumeMessage)
        }
        begin()
    } else {
        log.debug "Reminder was not previously started"
    }    
}

def begin() {
    state.startTime = now()
    
    if (initialDuration > 0) {
        runIn(initialDuration, repeat)
    } else {
        repeat()
    }
}

def end() {
    unschedule()
}

def repeat() {
    log.debug "Reminder has been running for ${now() - state.startTime} milliseconds"

    def message = repeatedMessage
    if (includeDurationInMessage) {    
        def totalMilliseconds = now() - state.startTime        
        def milliseconds = (int)(totalMilliseconds % 1000)
        
        def totalSeconds = (int)Math.floor((totalMilliseconds - milliseconds) / 1000)
        def seconds = totalSeconds % 60
        def totalMinutes = (int)Math.floor((totalSeconds - seconds) / 60)
        def minutes = totalMinutes % 60
        def totalHours = (int)Math.floor((totalMinutes - minutes) / 60)
        def hours = totalHours % 24
        def days = (int)Math.floor((totalHours - hours) / 24)
        
        def durationString = ""
        if (days || durationResolution == "Days") {
            durationString += "${days} day"
            if (days != 1) {
                durationString += "s"
            }
        }
        if (durationResolution != "Days") {
            if (hours || durationResolution == "Hours" ) {
                if (durationString) {
                    durationString += ", "
                }
                durationString += "${hours} hour"
                if (hours != 1) {
                    durationString += "s"
                }
            }
            if (durationResolution != "Hours") {
                if (minutes || durationResolution == "Minutes") {
                    if (durationString) {
                        durationString += ", "
                    }
                    durationString += "${minutes} minute"
                    if (minutes != 1) {
                        durationString += "s"
                    }
                }
                if (durationResolution != "Minutes") {
                    if (seconds || durationResolution == "Seconds") {
                        if (durationString) {
                            durationString += ", "
                        }
                        durationString += "${seconds} second"
                        if (seconds != 1) {
                            durationString += "s"
                        }
                    }
                }
            }
        }
        
        if (durationString) {
            message = message + " (" + durationString + ")"
        }
    }
    
    sendPush(message)
    runIn(repeatedDuration, repeat)
}

def getIsStarted() {
    if (reminderStart == turnedOn) {
        return reminderSwitch.currentSwitch == "on"
    } else {
        return reminderSwitch.currentSwitch == "off"
    }
}

def getIsPaused() {
    if (pauseSwitch) {
        if (reminderPause == turnedOn) {
            return pauseSwitch.currentSwitch == "on"
        } else {
            return pauseSwitch.currentSwitch == "off"
        }
    } else {
        return false
    }
}