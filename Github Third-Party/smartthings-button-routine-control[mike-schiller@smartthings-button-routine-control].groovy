definition(
    name: "Button Routine Control",
    namespace: "mike-schiller",
    author: "Mike Schiller",
    description: "Allows you to select one or more buttons to cycle through a set of routines. Typically the only " +
                 "action the routine performs is to activate a single scene. However, this is not a strict " +
                 "requirement.",
    category: "Convenience",
    /* TODO these are dummy icons that one of the sample apps uses, change these icons */
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png",
    pausable: true
)

preferences {
    page(name: "selectButtonsAndRoutines")
    page(name: "setRoutineOrder")
}

def selectButtonsAndRoutines() {
    dynamicPage(name: "selectButtonsAndRoutines",
                title: "Button(s) & Routines",
                nextPage: "setRoutineOrder",
                uninstall: uninstallable()) {
        section("Button Selection") {
            input "selectedButtons",
                "capability.button",
                title: "Select one or more buttons that you wish to be able to use to cycle through routines.",
                multiple: true,
                required: true
        }

        def allRoutines = location.helloHome?.getPhrases()*.label
        /* TODO figure out how to error if there are no routines */
        section("Routine Selection") {
            input "selectedRoutines",
                "enum", 
                title: "Select all routines you wish to use. You'll choose the cycling order of these routines on " +
                       "subsequent screens.",
            options: allRoutines,
            multiple: true,
            required: true
        }
        /* TODO figure out how to handle changes to the targets of actions from outside sources (e.g. Alexa). */
    }
}

def setRoutineOrder() {
    dynamicPage(name: "setRoutineOrder",
                title: "Choose Your Routine Order",
                install: savable(),
                uninstall: uninstallable()) {
        def numRoutines = selectedRoutines.size()
        def unmappedRoutines = getUnmappedRoutines()
        if (numRoutines < 2){
            section("Error") {
                paragraph "You must select at least 2 routines, typically at least 3 are selected." +
                          "Return to the previous page and update your routine selection."
            }
        } else {
           def inputVarName = ""
           def sectionName = ""
           def title = ""
           for (int i = 0; i < numRoutines; i++){
                inputVarName = "stateRoutine${i}"
                def options = unmappedRoutines
                /* should show the current selected value as an option if it exists */
                /* not sure why I had to use != null instead of containsKey() */
                if (settings[inputVarName] != null){
                    def routineName = settings[inputVarName]
                    if (selectedRoutines.contains(routineName)) {
                        options = unmappedRoutines + [settings[inputVarName]]
                    }
                }
                if (i == 0){
                    sectionName = "State ${i} / Off State Routine"
                    title = "Select the routine to execute in the 0th state. This is the routine that is executed " +
                            "when a button is double pressed. Typically the routine triggers a scene that " +
                            "turns everything off"
                } else if (i < (numRoutines - 1)) {
                    sectionName = "State / Routine ${i}"
                    title = "Select the routine to execute in state ${i}. This state occurs ${i} (short) button " +
                            "presses after the 0 / Off State."
                } else {
                   sectionName = "State ${i} / All On Routine"
                   title = "Select the routine to execute in state ${i}. This is the last state before a (short) " +
                           "button press returns to the 0 / Off State Routine. This is also the state that " +
                           "immediately occurs if you hold (long press) a button. The routine typically executes " +
                           "a scene that turns everything on or to a maximum setting." 
                }
                section(sectionName) {
                    input inputVarName,
                        "enum",
                        title: title,
                        options: options,
                        multiple: false,
                        required: true,
                        submitOnChange: true
                }
            }
        }
    }
}

def getUnmappedRoutines(){
    def mappedRoutines = getMappedRoutines()["byName"].keySet() as List
    return (mappedRoutines + selectedRoutines) - mappedRoutines.intersect(selectedRoutines)
}

def getMappedRoutines(){
    def mappedRoutines = ["byName":[:], "byState":[:]]
    def prefix = "stateRoutine"
    def stateNumber
    settings.each{ k, v ->
        if (k.startsWith(prefix)){
            stateNumber = k.split(prefix)[1].toInteger()
            if (stateNumber < selectedRoutines.size()){
                mappedRoutines.byName[settings[k]] = stateNumber
                mappedRoutines.byState[stateNumber] = settings[k]
            }
        }
    }
    return mappedRoutines
}
/* The callback called when the user taps save and the app was not already installed" */
def installed() {
    setup()
    state.installed = true
}

/* The callback called when the user taps save and the app was already installed" */
def updated() {
    log.debug "unsubscribing"
    unsubscribe()
    setup()
}

def savable() {
  /* TODO implement real logic here */
  return true
}

def uninstallable(){
  return state.containsKey("installed")
}

def setup() {
    /* subscribe to all button events from the selected buttons (this excludes things like temperature events that
       may be capabilities of some buttons). */
    selectedButtons.each(){
        log.debug "subscribing to deviceId: ${it.getId()}"
        subscribe(it, "button", buttonEvent)
    }
    selectedRoutines.each(){
        log.debug "action: ${it}"
    }
    state.state = 0
}

def buttonEvent(evt){
    def value = evt.value
    def eventDevice = evt.getDevice()
    def recentEvents = eventDevice.eventsSince(new Date(now() - 400)).findAll{it.value == evt.value && 
                                                                               it.data == evt.data}
    log.debug "Found ${recentEvents.size()?:0} events in past 400ms"

    if(recentEvents.size <= 1){
            if (value == "held") {
                state.state = selectedRoutines.size() - 1
            } else if (value == "double") {
                state.state = 0
            } else {
                state.state = state.state + 1
                if (state.state > selectedRoutines.size() -1) {
                    state.state = 0
                }
            }
            log.debug("state is now: ${state.state}")
            log.debug(getMappedRoutines())
            def routine = getMappedRoutines().byState[state.state]
            log.debug("executing routine: ${routine}")
            location.helloHome?.execute(routine)
        } else {
            log.debug "400ms DEBOUNCE! for ${eventDevice.getLabel()} ${eventDevice.getId()}"
        }
}
