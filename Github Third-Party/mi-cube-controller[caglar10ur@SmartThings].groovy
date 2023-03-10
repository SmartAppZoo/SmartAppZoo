/**
 *  Mi Cube Controller - Based on https://github.com/mattjfrank/ZWN-SC7-Enerwave-7-Button-Scene-Controller/blob/master/ZWN-SC7.SmartApp.groovy
 *
 *  Copyright 2017 S.Çağlar Onur
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
    name: "Mi Cube Controller",
    namespace: "caglar10ur",
    author: "S.Çağlar Onur",
    description: "Mi Cube Controller - Based on Matt Frank's ZWN-SC7 Button Controller",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "cube")
    page(name: "shake")
    page(name: "flip90")
    page(name: "flip180")
    page(name: "slide")
    page(name: "knock")
    page(name: "rotateright")
    page(name: "rotateleft")
}

def cube() {
    dynamicPage(name: "cube", title: "First, select which Cube", nextPage: "shake", uninstall: configured()) {
        section {
            input "cube", "capability.button", title: "Controller", multiple: false, required: true
        }

        section([mobileOnly: true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }

        section("More options", hideable: true, hidden: true) {
            input "debounce", "number", title: "Debounce time in milliseconds", required: true, value: 3000
        }
    }
}

def shake() {
    dynamicPage(name: "shake", title: "Shake, what do you want it to do?", nextPage: "flip90", uninstall: configured(), getSections(1))
}

def flip90() {
    dynamicPage(name: "flip90", title: "Flip 90, what do you want it to do?", nextPage: "flip180", uninstall: configured(), getSections(2))
}

def flip180() {
    dynamicPage(name: "flip180", title: "Flip 180, what do you want it to do?", nextPage: "slide", uninstall: configured(), getSections(3))
}

def slide() {
    dynamicPage(name: "slide", title: "Slide, what do you want it to do?", nextPage: "knock", uninstall: configured(), getSections(4))
}

def knock() {
    dynamicPage(name: "knock", title: "Knock, what do you want it to do?", nextPage: "rotateright", uninstall: configured(), getSections(5))
}

def rotateright() {
    dynamicPage(name: "rotateright", title: "Rotate Right, what do you want it to do?", nextPage: "rotateleft", uninstall: configured(), getSections(6))
}

def rotateleft() {
    dynamicPage(name: "rotateleft", title: "Rotate Left, what do you want it to do?", install: true, uninstall: true, getSections(7))
}

def getSections(buttonNumber) {
    return {
        section(title: "Toggle these...") {
            input "lights_${buttonNumber}_toggle", "capability.switch", title: "Switches:", multiple: true, required: false
            input "sonos_${buttonNumber}_toggle", "capability.musicPlayer", title: "Music players:", multiple: true, required: false
            input "locks_${buttonNumber}_toggle", "capability.lock", title: "Locks:", multiple: true, required: false
        }

        section(title: "Turn on these...") {
            input "lights_${buttonNumber}_on", "capability.switch", title: "Switches:", multiple: true, required: false
            input "sonos_${buttonNumber}_on", "capability.musicPlayer", title: "Music players:", multiple: true, required: false
        }

        section(title: "Turn off these...") {
            input "lights_${buttonNumber}_off", "capability.switch", title: "Switches:", multiple: true, required: false
            input "sonos_${buttonNumber}_off", "capability.musicPlayer", title: "Music players:", multiple: true, required: false
        }

        section(title: "Locks:") {
            input "locks_${buttonNumber}_unlock", "capability.lock", title: "Unlock these locks:", multiple: true, required: false
            input "locks_${buttonNumber}_lock", "capability.lock", title: "Lock these locks:", multiple: true, required: false
        }

        section(title: "Dimmers:") {
            input "dimmers_${buttonNumber}_dim", "capability.switchLevel", title: "Dim these dimmers:", multiple: true, required: false
            input "dimmers_${buttonNumber}_level", "enum", options: [
                [10: "10%"],
                [20: "20%"],
                [30: "30%"],
                [40: "40%"],
                [50: "50%"],
                [60: "60%"],
                [70: "70%"],
                [80: "80%"],
                [90: "90%"],
                [100: "100%"]
            ], title: "Set dimmers to this level", multiple: false, required: false
        }

        section(title: "Temprature:") {
            input "temps_${buttonNumber}_color", "capability.colorTemperature", title: "Change temprature of these dimmers:", multiple: true, required: false
            input "temps_${buttonNumber}_level", "enum", options: [
                [2700: "Soft White"],
                [4100: "Moonlight"],
                [5000: "Cool White"],
                [6500: "Daylight"]
            ], title: "Set temprature to this level", multiple: false, required: false
        }

        section("More options", hideable: true, hidden: true) {
            input "mode_${buttonNumber}_on", "mode", title: "Activate this mode:", required: false

            def routines = location.helloHome?.getPhrases()*.label
            if (routines) {
                routines.sort()
                input "routine_${buttonNumber}_on", "enum", title: "Activate this routine:", required: false, options: routines
            }
        }
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
    subscribe(cube, "button", buttonEvent)
}

def configured() {
    return cube || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4) || buttonConfigured(5) || buttonConfigured(6) || buttonConfigured(7)
}

def buttonConfigured(idx) {
    return settings["lights_$idx_toggle"] ||
        settings["sonos_$idx_toggle"] ||
        settings["locks_$idx_toggle"] ||
        settings["lights_$idx_on"] ||
        settings["sonos_$idx_on"] ||
        settings["mode_$idx_on"] ||
        settings["routine_$idx_on"] ||
        settings["lights_$idx_off"] ||
        settings["sonos_$idx_off"] ||
        settings["locks_$idx_on"] ||
        settings["locks_$idx_off"] ||
        settings["dimmers_$idx_dim"] ||
        settings["dimmers_$idx_level"] ||
        settings["temps_$idx_color"] ||
        settings["temps_$idx_level"]
}

def buttonEvent(evt) {
    log.debug "buttonEvent"

    if (!allOk) {
        return
    }

    def buttonNumber = evt.jsonData.buttonNumber
    def firstEventId = 0
    def value = evt.value

    log.debug "button: $buttonNumber, value: $value"
    def recentEvents = cube.eventsSince(new Date(now() - debounce)).findAll {
        it.value == evt.value && it.data == evt.data
    }
    log.debug "Found ${recentEvents.size()?:0} events in past ${debounce/1000} seconds"
    if (recentEvents.size() != 0) {
        log.debug "First Event ID: ${recentEvents[0].id}"
        firstEventId = recentEvents[0].id
    } else {
        firstEventId = 0
    }

    log.debug "This Event ID: ${evt.id}"

    if (firstEventId == evt.id) {
        switch (buttonNumber) {
            case ~/.*1.*/:
                executeHandlers(1)
                break
            case ~/.*2.*/:
                executeHandlers(2)
                break
            case ~/.*3.*/:
                executeHandlers(3)
                break
            case ~/.*4.*/:
                executeHandlers(4)
                break
            case ~/.*5.*/:
                executeHandlers(5)
                break
            case ~/.*6.*/:
                executeHandlers(6)
                break
            case ~/.*7.*/:
                executeHandlers(7)
                break
        }
    } else if (firstEventId == 0) {
        log.debug "No events found. Possible SmartThings latency"
    } else {
        log.debug "Duplicate button press found. Not executing handlers"
    }
}

def executeHandlers(buttonNumber) {
    log.debug "executeHandlers: $buttonNumber"

    def lights = find('lights', buttonNumber, "toggle")
    if (lights) toggle(lights)

    def sonos = find('sonos', buttonNumber, "toggle")
    if (sonos) toggle(sonos)

    def locks = find('locks', buttonNumber, "toggle")
    if (locks) toggle(locks)

    lights = find('lights', buttonNumber, "on")
    if (lights) flip(lights, "on")

    sonos = find('sonos', buttonNumber, "on")
    if (sonos) flip(sonos, "on")

    lights = find('lights', buttonNumber, "off")
    if (lights) flip(lights, "off")

    sonos = find('sonos', buttonNumber, "off")
    if (sonos) flip(sonos, "off")

    locks = find('locks', buttonNumber, "lock")
    if (locks) flip(locks, "lock")

    locks = find('locks', buttonNumber, "unlock")
    if (locks) flip(locks, "unlock")

    def temps = find('temps', buttonNumber, "color")
    if (temps) {
        def level = find('temps', buttonNumber, "level")
        if (level) temp(temps, level)
    }

    def dimmers = find('dimmers', buttonNumber, "dim")
    if (dimmers) {
        def level = find('dimmers', buttonNumber, "level")
        if (level) dim(dimmers, level)
    }

    def mode = find('mode', buttonNumber, "on")
    if (mode) changeMode(mode)

    def routine = find('routine', buttonNumber, "on")
    if (routine) location.helloHome.execute(routine)
}

def find(type, buttonNumber, value) {
    def preferenceName = type + "_" + buttonNumber + "_" + value
    def pref = settings[preferenceName]
    if (pref) {
        log.debug "Found: $pref for $preferenceName"
    }
    return pref
}

def flip(devices, newState) {
    log.debug "flip: $devices = ${devices*.currentValue('switch')}"

    if (newState == "off") {
        devices.off()
    } else if (newState == "on") {
        devices.on()
    } else if (newState == "unlock") {
        devices.unlock()
    } else if (newState == "lock") {
        devices.lock()
    }
}

def toggle(devices) {
    log.debug "toggle: $devices = ${devices*.currentValue('switch')}"

    if (devices*.currentValue('switch').contains('on')) {
        devices.off()
    } else if (devices*.currentValue('switch').contains('off')) {
        devices.on()
    } else if (devices*.currentValue('lock').contains('locked')) {
        devices.unlock()
    } else if (devices*.currentValue('lock').contains('unlocked')) {
        devices.lock()
    } else {
        devices.on()
    }
}

def dim(devices, level) {
    log.debug "dim: $devices = ${devices*.currentValue('switch')}"
    log.debug "dim: level = $level"

    def l = level as Integer
    devices.setLevel(l)
}

def temp(devices, level) {
    log.debug "dim: $devices = ${devices*.currentValue('switch')}"
    log.debug "dim: level = $level"

    def l = level as Integer
    devices.setColorTemperature(l)
}

def changeMode(mode) {
    log.debug "changeMode: $mode, location.mode = $location.mode, location.modes = $location.modes"

    if (location.mode != mode && location.modes?.find {
            it.name == mode
        }) {
        setLocationMode(mode)
    }
}

// execution filter methods
private getAllOk() {
    modeOk
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    log.debug "modeOk = $result"

    result
}