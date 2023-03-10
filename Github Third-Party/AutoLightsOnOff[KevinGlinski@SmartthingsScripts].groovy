definition(
    name: "Auto Lights On Off",
    namespace: "kevinglinski",
    author: "Kevin Glinski",
    description: "Turn lights on and off automatically. If after lights are turned on, they are manually dimmed or turned off, then they won't automatically be turned off.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
    section("Turn on when there's movement..."){
        input "motion1", "capability.motionSensor", title: "Where?"
    }
    section("And off when there's been no movement for..."){
        input "minutes1", "number", title: "Minutes?"
    }
    section("Turn on/off light(s)..."){
        input "switches", "capability.switchLevel", multiple: true
    }
}

def installed() {
    initialize()

}

def updated() {
    unsubscribe()
    initialize()
}

def initialize(){
    state.autoTurnedOn = false
    subscribe(motion1, "motion", motionHandler)
}

def motionHandler(evt) {
    log.debug "$evt.name: $evt.value"
    if (evt.value == "active") {
        def currSwitches = switches.currentSwitch

        def onSwitches = switches.findAll { switchVal ->
        	log.debug "Switch state $switchVal.currentSwitch"
            log.debug "Switch level $switchVal.currentLevel"
            switchVal.currentSwitch == "on" ? true : false
        }

        if(onSwitches.size() == 0){
            log.debug "turning on lights"
           // switches.on()
            switches.setLevel(90)
            state.autoTurnedOn = true
        }

    } else if (evt.value == "inactive" && state.autoTurnedOn) {
        runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
    }
}

def scheduleCheck() {
    log.debug "schedule check"
    def motionState = motion1.currentState("motion")
    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.rawDateCreated.time
        def threshold = 1000 * 60 * minutes1 - 1000
        if (elapsed >= threshold) {
            state.autoTurnedOn = false

            def switchLevelChanged = switches.findAll { switchVal ->
                log.debug "Switch state $switchVal.currentSwitch"
                log.debug "Switch level $switchVal.currentLevel"
                switchVal.currentSwitch != "on" || switchVal.currentLevel != 90
            }

            if(switchLevelChanged.size() == 0){
                log.debug "Switch hasn't changed turning lights off"
                switches.off()
            }


        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
        log.debug "Motion is active, do nothing and wait for inactive"
    }
}
