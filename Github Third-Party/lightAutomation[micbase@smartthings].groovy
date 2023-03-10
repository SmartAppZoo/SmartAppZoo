definition(
    name: "Lights Automation",
    namespace: "miccrun",
    author: "Michael Chang",
    description: "Customized lights automation",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)

preferences {
    page(name: "configurations", install: true, uninstall: true)
}

def configurations() {
    dynamicPage(name: "configurations", title: "Configurations") {
        section(title: "Select lights") {
            input "lights", "capability.switch", title: "Lights", multiple: true, required: false
        }

        section(title: "Select dimmers") {
            input "dimmers", "capability.switchLevel", title: "Dimmers", multiple: true, required: false
            input "dimmerLevel", "number", title: "How bright", required: false, description: "0% to 100%"
        }

        section("Select motion sensor") {
            input "motionSensor", "capability.motionSensor", title: "Motion Sensor", required: true
        }

        section("Select light sensor") {
            input "lightSensor", "capability.illuminanceMeasurement",title: "Light Sensor", required: true
            input "luxLevel", "number", title: "Illuminance threshold (default 20 lux)", defaultValue: "20", required: false
        }

        section("Delay Time") {
            input "delayMinutes", "number", title: "Minutes", required: false
        }

        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
            actions.sort()
            section("Routine Bindings") {
                input "goodMorningRoutine", "enum", title: "Routine to change mode to normal", options: actions, required: false
                input "goodNightRoutine", "enum", title: "Routine to change mode to sleep", options: actions, required: false
                input "movieTimeRoutine", "enum", title: "Routine to change mode to movie", options: actions, required: false
                input "manualRoutine", "enum", title: "Routine to change mode to manual", options: actions, required: false
            }
        }

        section ("Assign a name") {
            label title: "Assign a name", required: false
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}."
    initialize()
    state.mode = "normal"
    state.dark = true
}

def updated() {
    log.debug "Updated with settings: ${settings}."
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(motionSensor, "motion", motionHandler)
    subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
    subscribe(location, "routineExecuted", routineChanged)
}

def motionHandler(evt)
{
    def lastState = state.motion
    if (evt.value == "active") {
        state.motion = true
    }
    else if (evt.value == "inactive") {
        state.motion = false
    }
    if (lastState != state.motion) {
        log.debug("motion changed to: ${state.motion}")
        process()
    }
}

def illuminanceHandler(evt)
{
    def lastState = state.dark
    if (evt.integerValue > ((luxLevel != null && luxLevel != "") ? luxLevel : 20)) {
        state.dark = false
    } else {
        state.dark = true
    }
    if (lastState != state.dark) {
        log.debug("dark changed to: ${state.dark}")
        process()
    }
}

def routineChanged(evt) {
    def lastState = state.mode
    if (evt.displayName == goodMorningRoutine) {
        state.mode = "normal"
    }
    else if (evt.displayName == goodNightRoutine) {
        state.mode = "sleep"
    }
    else if (evt.displayName == movieTimeRoutine) {
        state.mode = "movie"
    }
    else if (evt.displayName == manualRoutine) {
        state.mode = "manual"
    }
    if (lastState != state.mode) {
        log.debug("mode changed to: ${state.mode}")
        process()
    }
}

def turnOffLights()
{
    log.debug("lights off")
    lights?.off()
}

def turnOnLights()
{
    log.debug("lights on")
    unschedule(turnOffLights)
    lights?.on()
}

def dimDimmers()
{
    log.debug("dimmers dimmed")
    dimmers?.setLevel(dimmerLevel)
}

def turnOffDimmers()
{
    log.debug("dimmers off")
    dimmers?.off()
}

def turnOnDimmers()
{
    log.debug("dimmers on")
    unschedule(turnOffDimmers)
    dimmers?.setLevel("100")
}

def process() {
    log.debug("motion: ${state.motion}, dark: ${state.dark}, mode: ${state.mode}")
    if (state.mode == "normal") {
        if (state.dark) {
            if (state.motion) {
                log.debug("turn on lights & dimmers")
                turnOnLights()
                turnOnDimmers()
            } else {
                if (delayMinutes) {
                    def delay = delayMinutes * 60
                    log.debug("turn off lights & dimmers in ${delayMinutes} minutes")
                    runIn(delay, turnOffLights)
                    runIn(delay, turnOffDimmers)
                } else {
                    log.debug("turn off lights & dimmers")
                    turnOffLights()
                    turnOffDimmers()
                }
            }
        } else {
            log.debug("turn off lights & dimmers")
            turnOffLights()
            turnOffDimmers()
        }
    } else if (state.mode == "sleep") {
        log.debug("turn off lights & dimmers, sleep mode")
        turnOffLights()
        turnOffDimmers()
    } else if (state.mode == "movie") {
        log.debug("turn off lights, movie mode")
        turnOffLights()
        if (state.dark) {
            log.debug("dim dimmers, movie mode")
            dimDimmers()
        } else {
            log.debug("turn off dimmers, movie mode")
            turnOffDimmers()
        }
    }
}
