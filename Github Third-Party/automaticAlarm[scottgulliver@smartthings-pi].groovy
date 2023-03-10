/**
 *  Automatic Alarm
 *
 *  Copyright (c) 2017 Scott Gulliver
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:

 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.

 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 *
 *
 *
 *  TODO:
 *     1. Change the mode to AWAY and back to HOME automatically
 */
definition(
    name: "Automatic Alarm",
    namespace: "scottg1989",
    author: "Scott Gulliver",
    description: "Automatically set and disarm the alarm..",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("These people..."){
        input "people", "capability.presenceSensor", multiple: true
    }
    section("When all leave, perform these actions...") {
        input "setAlarmOnLeaveEnabled", "bool", title: "Set the alarm", defaultValue: true
        input "leavePushMessageEnabled", "bool", title: "Send a message", defaultValue: true
        input "leavePushMessage", "text", title: "Message", defaultValue: "The alarm has been set.", required: false
    }
    section("When one arrives, perform these actions...") {
        input "disarmAlarmOnArriveEnabled", "bool", title: "Disarm the alarm", defaultValue: true
        input "arrivePushMessageEnabled", "bool", title: "Send a message", defaultValue: true
        input "arrivePushMessage", "text", title: "Message", defaultValue: "The alarm has been disarmed.", required: false
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
	subscribe(people, "presence", presence)
}

def presence(evt) {
    if (evt.value == "present" && onlyPersonPresent(evt.getDevice())) {
        log.debug "First person back!"
        
        if (disarmAlarmOnArriveEnabled && getAlarmState() != "off") {
            setAlarmState("off")

            if (arrivePushMessageEnabled) {
                sendPush(arrivePushMessage)
            }
        }
    }
    else if (everyoneIsAway()) {
        log.debug "All gone!"
        
        if (setAlarmOnLeaveEnabled && getAlarmState() != "away") {
            setAlarmState("away")
            
            if (leavePushMessageEnabled) {
                sendPush(leavePushMessage)
            }
        }
    }
}

private onlyPersonPresent(checkPerson) {
    if (checkPerson.currentPresence != "present") {
        return false
    }

    for (person in people) {
        if (person.getId() != checkPerson.getId() && person.currentPresence == "present") {
            return false
        }
    }

    return true
}

private everyoneIsAway() {
    def result = true
    
    for (person in people) {
        if (person.currentPresence == "present") {
            result = false
            break
        }
    }
    return result
}

private getAlarmState() {
	return location.currentState("alarmSystemStatus")?.value
}

private setAlarmState(state) {
    sendLocationEvent(name: "alarmSystemStatus", value: state)
}
