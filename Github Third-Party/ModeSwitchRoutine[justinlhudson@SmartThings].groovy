definition(
  name: "Mode Switch Routine",
  namespace: "Operations",
  author: "justinlhudson",
  description: "When mode changes activate routine",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  page(name: "getPref")
}
  
def getPref() { 
    dynamicPage(name: "getPref", install:true, uninstall: true) {
    section("Choose Mode to use...") {
      input "mode", "mode", title: "Mode Activate", required: true
    }

    // get the available actions
            def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
            // sort them alphabetically
            actions.sort()
                    section("Routine") {
                            log.trace actions
                // use the actions as the options for an enum input
                input "action", "enum", title: "Select an action to execute", options: actions
                    }
            }
    section([mobileOnly:true], "Options") {
      label(title: "Assign a name", required: false)
    }
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  unschedule()

  initialize()
}

def routineHandler(evt) {
  log.debug "${evt.displayName}"
  state.currentRoutine = evt.displayName
}

def modeHandler(evt)
{
  if (state.currentRoutine != settings.action)
  {
    log.debug "${state.currentRoutine} != ${settings.action}"
    if (state.currentMode != evt.value) {
      log.debug "${state.currentMode} != ${evt.value}"
      if(evt.value == settings.mode) {
        location.helloHome?.execute(settings.action)
      }
    }
  }
  log.debug "${evt.value}"
  state.currentMode = evt.value
}

private def initialize() {
  subscribe(location, "mode", modeHandler)
  subscribe(location, "routineExecuted", routineHandler)
}

/*
def changeMode(newMode) {
  if (location.mode != newMode) {
    if (location.modes?.find{it.name == newMode}) {
      setLocationMode(newMode)
    } else {
      log.debug "Unable to change to undefined mode '${newMode}'"
    }
  }
}
*/
