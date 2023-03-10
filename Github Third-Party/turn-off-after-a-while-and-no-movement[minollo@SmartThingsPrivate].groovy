/**
 */

// Automatically generated. Make future change here.
definition(
    name: "Turn off after a while and no movement",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Turn off after a while and no movement",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
	section("Manage light..."){
		input "motionSensors", "capability.motionSensor", title: "Motion sensor", multiple: true
		input "minutes", "number", title: "Minutes timeout"
        input "lightSwitch", "capability.switch", title: "Light switch"
        input "plugs", "capability.switch", title: "Other plugs", required: false, multiple: true
        input "onWhenMovement", "enum", title: "Turn on when movement?", required: false, metadata:[values:['true','false']]
	}
	section("Away mode") {
		input "awayMode", "mode", title: "Away mode"

	}
}

def installed()
{
	log.debug "[Turn off timeout/move] Installed app"
    runIn(60, initialize)
}

def updated()
{
	log.debug "[Turn off timeout/move] Updated app"
	unsubscribe()
    runIn(60, initialize)
}

def initialize() {
    subscribe(motionSensors, "motion", motionHandler)
    subscribe(lightSwitch, "switch", lightSwitchHandler, [filterEvents: false])
    subscribe(plugs, "switch", plugsHandler)
    subscribe(location, modeChangeHandler)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
    state.keepAliveLatest = now()
    schedule("0 0 0/1 * * ?", keepAlive)	//poll every hour to keep the sensors active
    updateState(location.mode)
}

def pollerEvent(evt) {
    log.debug "[PollerEvent]"
    log.debug "[PollerEvent] keepAliveLatest == ${state.keepAliveLatest}; turnOffLightLatest == ${state.turnOffLightLatest}; now() == ${now()}"
    if (state.keepAliveLatest && now() - state.keepAliveLatest > 3720000) {
        log.error "Waking up keepAlive timer"
        keepAlive()
        schedule("0 0 0/1 * * ?", keepAlive)	//poll every hour to keep the sensors active
    }
    if (state.turnOffLightLatest && now() - state.turnOffLightLatest > (minutes + 2) * 60 * 1000) {
        log.error "Waking up turnOffLight timer"
        turnOffLight()
    }
}

def modeChangeHandler(evt)
{
	if (evt.value == awayMode) {
		log.debug "[Turn off timeout/move] Location mode is away; shut down everything"
        state.turnOffLightLatest = null
    	try {unschedule(turnOffLight)} catch(e) {log.error "Ignoring exception: ${e}"}
        lightSwitch.off()
        plugs?.off()
        state.switchOn = "false"
        state.plugsOn = "false"
        state.checkMovement = "false"
    } else {
		log.debug "[Turn off timeout/move] Location mode is no more away"
    	updateState(evt.value)
    }
}

private updateState(newMode)
{
	log.debug "[Turn off timeout/move] Update state"
    if (newMode == awayMode) {
		log.debug "[Turn off timeout/move] Location mode is away; shut down everything"
        state.turnOffLightLatest = null
    	try {unschedule(turnOffLight)} catch(e) {log.error "Ignoring exception: ${e}"}
        lightSwitch.off()
        plugs?.off()
        state.switchOn = "false"
        state.plugsOn = "false"
	}
}

def motionHandler(evt)
{
	if (location.mode == awayMode) {
		log.debug "[Turn off timeout/move] Location mode is away; do nothing"
    } else {
    	if (evt.value == "active") {
            log.debug "[Turn off timeout/move] Motion is active"
            state.turnOffLightLatest = null
            try {unschedule(turnOffLight)} catch(e) {log.error "Ignoring exception: ${e}"}
            if (onWhenMovement && onWhenMovement == "true") {
            	if (state.manualOffAt && (now() - state.manualOffAt) < (10 * 1000)) {	// after manual off, don't turn on on movement for 10 seconds
                	log.debug "Movement but too close to manual off"
                } else {
                    lightSwitch.on()
                    plugs?.on()
                    state.switchOn = "true"
                    state.plugsOn = "true"
                }
            } else {
            	if (state.checkMovement && state.checkMovement == "true") {
                	log.info "Turn lights back on: movement detected in within grace period"
                	state.checkMovement = "false"
                    if (state.switchOn && state.switchOn == "true") {
                		lightSwitch.on()
                    }
                    if (state.plugsOn && state.plugsOn == "true") {
                    	plugs?.on()
                    }
                    state.turnOffLightLatest = now()
                    runIn(minutes * 60, turnOffLight)
                    try {unschedule(stopCheckMovement)} catch(e) {log.error "Ignoring exception: ${e}"}
                }
            }
        } else if (evt.value == "inactive" && allMotionInactive()) {
            log.debug "[Turn off timeout/move] Motion is inactive"
            state.turnOffLightLatest = now()
            runIn(minutes * 60, turnOffLight)
        }
	}
}

private allMotionInactive() {
	def result = true
	for (motion in motionSensors) {
		if (motion.currentMotion != "inactive") {
			result = false
			break
		}
	}
	log.debug "[Turn off timeout/move]: allMotionInactive() == $result"
	return result
}

def lightSwitchHandler(evt)
{
    log.debug "[Turn off timeout/move] lightSwitchHandler(${evt.value}), currentValue(${lightSwitch.currentValue("switch")}), latestState = ${state.latestState}, state change = ${evt.isStateChange()}" 
    if (evt.isStateChange()) {
    	state.latestState = evt.value
        log.info "Light switch state changed, physical == ${evt.isPhysical()}"
        if (lightSwitch.currentValue("switch") == "off" && evt.isPhysical()) {
            log.info "Light switch is off"
            state.manualOffAt = now()
            if (state.checkMovement && state.checkMovement == "false") {
                state.switchOn = "false"
                try {unschedule(stopCheckMovement)} catch(e) {log.error "Ignoring exception: ${e}"}
            }
        } else if (lightSwitch.currentValue("switch") == "on") {
            log.info "Light switch is on"
            state.switchOn = "true"
            state.turnOffLightLatest = now()
            runIn(minutes * 60, turnOffLight)
            state.checkMovement = "false"
            try {unschedule(stopCheckMovement)} catch(e) {log.error "Ignoring exception: ${e}"}
        }
    } else {
        log.info "Light switch pressed but no state change"
        if (evt.value == "off") {
            log.info "Switch already off; turn plugs off"
            plugs?.off()
            state.plugsOn = "false"
            state.turnOffLightLatest = null
            try {unschedule(turnOffLight)} catch(e) {log.error "Ignoring exception: ${e}"}
        } else if (evt.value == "on") {
            log.info "Switch already on; turn plugs on"
            plugs?.on()
            state.plugsOn = "true"
            state.turnOffLightLatest = now()
            runIn(minutes * 60, turnOffLight)
        }
        state.checkMovement = "false"
        try {unschedule(stopCheckMovement)} catch(e) {log.error "Ignoring exception: ${e}"}
    }
}

def plugsHandler(evt) {
}

def turnOffLight()
{
    log.debug "[Turn off timeout/move] Scheduled turn off light"
    state.turnOffLightLatest = null
    if (lightSwitch.currentValue("switch") == "on" || (state.plugsOn && state.plugsOn == "true")) {
        lightSwitch.off()
        plugs?.off()
        log.info "Starting grace period monitoring movement"
        state.checkMovement = "true"
        runIn(60, stopCheckMovement)
    }
}

def stopCheckMovement()
{
    state.checkMovement = "false"
    state.plugsOn = "false"
    state.switchOn = "false"
    log.info "Grace period for turning lights back on has expired"
}


def keepAlive()
{
    log.debug "keepAlive"
    state.keepAliveLatest = now()
    for (sensor in motionSensors) {
        for (capability in sensor.capabilities) {
            if (capability.name == "Polling") {
                log.info "Polling ${sensor.label}"
                sensor.poll()
                break;
            }
        }
    }
}

