preferences() {
    section("Motion"){
        input "MotionSensor", "capability.motionSensor", multiple: false
    }
}

def someEventHandler(evt) {
    if (someCondition) {
        theSwitch.on()
    } else {
        theSwitch.off()
    }

    // logs either "switch is on" or "switch is off"
    log.debug "switch is ${theSwitch.currentSwitch}"
}
