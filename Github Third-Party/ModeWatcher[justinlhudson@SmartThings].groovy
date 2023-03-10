definition(
    name: "Mode Watcher",
    namespace: "Operations",
    author: "justinlhudson",
    description: "Changing mode on zones (i.e. Zone 1 -> Zone 2 => New Mode).",
    category:  "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Zones Mode") {
        for (int n = 1; n <= 2; n++) {
            input "zone_${n}", "capability.motionSensor", title:"Zone ${n}", multiple:true, required:true
        }
        input "revertMode", "mode", title:"Mode Revert", multiple:false, required:true
        input "setMode", "mode", title:"Mode Change", multiple:false, required:true
        input "window", "number", title:"Window period (seconds)", defaultValue:10
    }
    /*
    section("During this time window") {
        input "startTime", "time", title: "Start Time?", required:true
        input "endTime", "time", title: "End Time?", required:true
    }
    */
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()

    initialize()
}

def modeHandler(evt)
{
    if(state.currentMode != evt.value) {
      state.prevMode = state.currentMode
      state.currentMode = evt.value

      log.debug "Prev. Mode: ${state.prevMode}" 
      log.debug "Current Mode: ${state.currentMode}"
    }
/*
    if (location.mode == "Away") {

    }
    else if (location.mode == "Night") {

    }
    else {

    }
*/
}

def zone_2_Handler(evt)
{
    log.debug "Z2: ${evt.value}"
    if(evt.value == "active") {
      state.zone_2 = state.zone_2 + 1
    }
    else if(state.zone_1 <= 0 && evt.value == "inactive") {
      reset()
    }
}

def zone_1_Handler(evt)
{
    log.debug "Z1: ${evt.value}"
    if(state.zone_2 <= 0 && evt.value == "active") {
        if(state.currentMode == settings.revertMode) { // before any changes make sure in mode we care about 
            state.zone_1 = state.zone_1 + 1
            
            if(state.currentMode != settings.setMode) {
                setLocationMode(settings.setMode)
                log.debug "Set Mode: ${settings.setMode}"
            }
        }
    }
    else if (evt.value == "inactive") {
        def runTime = new Date((new Date()).getTime() + (settings.window * 1000))
        runOnce(runTime, operation, [overwrite: true])
    }
}

def operation()
{
    if(state.zone_1 > 0 && state.zone_2 <= 0) {
        if(state.prevMode == settings.revertMode) {  // if prev mode is mode we want
            if(state.currentMode != settings.revertMode) {
                setLocationMode(settings.revertMode)
                log.debug "Revert Mode: ${settings.revertMode}"  //note: mode not change instantly so false reading
                //sendNotificationEvent "Revert Mode: ${settings.revertMode}"
            }
        }
    }

    reset()
}

private def reset()
{
    state.zone_1 = 0
    state.zone_2 = 0

    //modeHandler(location.mode)
}

private def initialize() {
    reset()

    subscribe(location, "mode", modeHandler)

    subscribe(zone_1, "motion", zone_1_Handler)
    subscribe(zone_2, "motion", zone_2_Handler)
}
