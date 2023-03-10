definition(
    name: "Home / Away Lights Manager",
    namespace: "petermajor",
    author: "Peter Major",
    description: "Turn on outside lights when I'm away, but only if it's dark.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Lights") {
        input "insideLights", "capability.switch", title: "Which inside lights?", required: false, multiple: true
        input "outsideLights", "capability.switch", title: "Which outside lights?", required: false, multiple: true
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
    subscribe(location, "mode", onModeChanged)
    subscribe(location, "sunrise", onSunrise)
    subscribe(location, "sunset", onSunset)

    state.lastMode = location.mode

    log.debug "current mode is ${state.lastMode}"
}

def isDark() {
    def time = now()
    log.debug "now: " + time
    
    def times = getSunriseAndSunset()
    log.debug "times: ${times}"
    log.debug "sunrise: ${times.sunrise}, ${times.sunrise.time}"
    log.debug "sunset: ${times.sunset}, ${times.sunset.time}"
    
    def result = time < times.sunrise.time || time > times.sunset.time
    log.debug "isDark = $result"
    result
}

def onModeChanged(evt) {
    log.debug "modeChanged: $evt"

    log.debug "previous mode: ${state.lastMode}"

    // value will be the name of the mode
    // e.g., "Home" or "Away"
    log.debug "new mode: ${evt.value}"
    
    if(evt.value == "Away" && state.lastMode == "Home") {
        log.debug "Mode changed from Home to Away. Checking if it's dark..."
        if(isDark()) {
            log.debug "... it's dark, turning on outside lights"
            outsideLights.on()
        }

        unschedule(onFiveMinsAfterModeChangedToHome)
    }

    if(evt.value == "Home" && state.lastMode == "Away") {

        log.debug "Mode changed from Away to Home. Checking if it's dark..."
        if(isDark()) {
            log.debug "... it's dark, turning on inside lights"
            insideLights.on()
        }

        runIn(60*5, onFiveMinsAfterModeChangedToHome)
    }    
    state.lastMode = evt.value
}

def onFiveMinsAfterModeChangedToHome() {
    outsideLights.off()
}

def onSunset(evt) {
    log.debug "It's sunset..."

    if(location.mode == "Away") {
        log.debug "... and you're away, so turning on outside lights"
        outsideLights.on()
    }
}

def onSunrise(evt) {
    log.debug "It's sunrise..."

    if(location.mode == "Away") {
        log.debug "... and you're away, so turning off outside lights"
        outsideLights.off()
    }
}