/**
 *  Copyright 2017 Derek Brooks
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
 *  Notify Me With Color
 *
 *  Based off of the old SmartThings "Notify with Hue" smart app
 *
 *  Author: Derek Brooks
 *  Date: 2017-04-12
 */
 
// TODO:
// - add "current level" to bulb level dropdown options to retain brightness
// - allow setting a certain brightness if notifying a bulb that is off

definition(
    name: "Notify Me With Color",
    namespace: "broox",
    author: "Derek Brooks",
    description: "Temporarily changes the color and brightness of lightbulbs when any of a variety of SmartThings actions occur.  Supports motion, contact, acceleration, moisture and presence sensors as well as modes, schedules, buttons and switches.",
    category: "SmartThings Labs",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png"
)

preferences {
    section("Control these bulbs...") {
        input "bulbs", "capability.colorControl", title: "Which bulbs?", required:true, multiple:true
    }

    section("Choose one or more, when..."){
        input "motion", "capability.motionSensor", title: "Motion is detected", required: false, multiple: true
        input "contact", "capability.contactSensor", title: "Sensor opens", required: false, multiple: true
        input "contactClosed", "capability.contactSensor", title: "Sensor closes", required: false, multiple: true
        input "acceleration", "capability.accelerationSensor", title: "Acceleration is detected", required: false, multiple: true
        input "button1", "capability.button", title: "Button is pressed", required:false, multiple:true
        input "mySwitch", "capability.switch", title: "Switch is turned on", required: false, multiple: true
        input "mySwitchOff", "capability.switch", title: "Switch is turned off", required: false, multiple: true
        input "arrivalPresence", "capability.presenceSensor", title: "Someone arrives", required: false, multiple: true
        input "departurePresence", "capability.presenceSensor", title: "Someone departs", required: false, multiple: true
        input "smoke", "capability.smokeDetector", title: "Smoke is detected", required: false, multiple: true
        input "water", "capability.waterSensor", title: "Water is detected", required: false, multiple: true
        input "triggerModes", "mode", title: "System changes mode", description: "Select mode(s)", required: false, multiple: true
        input "timeOfDay", "time", title: "The time is", required: false
    }

    section("Choose bulb effects...") {
        input "inputColor", "enum", title: "Color", options: [[100:"Red"],[39:"Green"],[70:"Blue"],[25:"Yellow"],[10:"Orange"],[75:"Purple"],[83:"Pink"]], defaultValue: 100, required: true
        input "inputLevel", "enum", title: "Brightness", options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: 50, required: true
        input "duration", "number", title: "Number of seconds", defaultValue: "2", required: true
        input "turnOn", "enum", title: "Turn on when off?", options: ["Yes", "No"], defaultValue: "No", required: true
    }

    section("Minimum time between notifications (optional, defaults to every notification)") {
        input "frequency", "decimal", title: "Minutes", required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribeToEvents()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    unschedule()
    subscribeToEvents()
}

def subscribeToEvents() {
    subscribe(app, appTouchHandler)
    subscribe(contact, "contact.open", eventHandler)
    subscribe(contactClosed, "contact.closed", eventHandler)
    subscribe(acceleration, "acceleration.active", eventHandler)
    subscribe(motion, "motion.active", eventHandler)
    subscribe(mySwitch, "switch.on", eventHandler)
    subscribe(mySwitchOff, "switch.off", eventHandler)
    subscribe(arrivalPresence, "presence.present", eventHandler)
    subscribe(departurePresence, "presence.not present", eventHandler)
    subscribe(smoke, "smoke.detected", eventHandler)
    subscribe(smoke, "smoke.tested", eventHandler)
    subscribe(smoke, "carbonMonoxide.detected", eventHandler)
    subscribe(water, "water.wet", eventHandler)
    subscribe(button1, "button.pushed", eventHandler)

    if (triggerModes) {
        subscribe(location, modeChangeHandler)
    }

    if (timeOfDay) {
        schedule(timeOfDay, scheduledTimeHandler)
    }
}

def eventHandler(evt) {
    if (frequency) {
        def lastTime = state[evt.deviceId]
        if (lastTime == null || now() - lastTime >= frequency * 60000) {
            notifyBulbs(evt)
        }
    }
    else {
        notifyBulbs(evt)
    }
}

def modeChangeHandler(evt) {
    log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
    if (evt.value in triggerModes) {
        eventHandler(evt)
    }
}

def scheduledTimeHandler() {
    eventHandler(null)
}

def appTouchHandler(evt) {
    notifyBulbs(evt)
}

def getDesiredBulbs() {    
    if (turnOn == "Yes") {
        return bulbs
    } else {
        return bulbs.findAll({ it.currentValue("switch") == "on" })
    }
}

private notifyBulbs(evt) {
    if (frequency) {
        state[evt.deviceId] = now()
    }

    state.previous = [:]

    def bulbsToNotify = getDesiredBulbs()
    bulbsToNotify.each {
        state.previous[it.id] = [
            "switch": it.currentValue("switch"),
            "level" : it.currentValue("level"),
            "hue": it.currentValue("hue"),
            "saturation": it.currentValue("saturation"),
            "color": it.currentValue("color")            
        ]
    }

    log.debug "current values = $state.previous"

    def newValue = [hue: inputColor as Integer ?: 100, saturation: 100, level: (inputLevel as Integer) ?: 100]
    log.debug "new value = $newValue"

    bulbsToNotify*.setColor(newValue)
    setTimer()
}

def setTimer() {
    if(!duration) {
        duration = 5
    }

    if(duration < 10) {
        log.debug "pause $duration seconds"
        pause(duration * 1000)
        resetBulbs()
    } else {
        log.debug "runIn $duration seconds, resetBulbs"
        runIn(duration, "resetBulbs", [overwrite: false])
    }
}

def resetBulbs() {
    def bulbsToReset = getDesiredBulbs()
    bulbsToReset.each {
        log.debug "reset bulb to $state.previous[t.id]"
        if(state.previous[it.id].switch == "off") {
            it.off()
        }
        it.setColor(state.previous[it.id])
    }
}