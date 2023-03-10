/**
 *  Run Routine on Time
 *
 *  Author: skyjedi@gmail.com
 *  Date: 11/2/16
 */

definition(
  name: "Run Routine on Time",
  namespace: "SkyJedi",
  author: "skyjedi@gmail.com",
  description: "Run a Routine at defined time",
  category: "My Apps",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    page(name: "configure")
}
def configure() {
    dynamicPage(name: "configure", title: "Configure Time and Phrase", install: true, uninstall: true) 
    {
  	section("Trigger Time") {
    input "theTime", "time", title: "Time to execute every day", required: true
	}
	def actions = location.helloHome?.getPhrases()*.label
    if (actions) {
            actions.sort()
                    section("Routines") {
                            log.trace actions
                	input "routine", "enum", title: "Routine to Execute", options: actions, required: true
                    					}
				}
     section("Options") {
     label title: "Assign a name", required: false
     input "modes", "mode", title: "Set for specific mode(s)", multiple: true //set to execute only in these modes
        }	
	}
    
}

def installed() {
  	schedule(theTime, timedRoutine)
    log.debug "timedRoutine Scheduled at ${theTime}"
}

def updated() {
	unschedule()
	schedule(theTime, timedRoutine)
    log.debug "timedRoutine Scheduled at ${theTime}"
}

// called every day at the time specified by the user
def timedRoutine() {
    log.debug "settings.modes: $allowedModes  Current Mode: $currMode"
    
    //If the current mode is not in the subset of allowed Modes the smartapp will not execute the routine
   	if (settings.modes.contains(location.currentMode)) {
	location.helloHome?.execute(settings.routine)
    log.debug "timedRoutine called at ${new Date()}"
    
	} else {
    log.debug "timedRoutine not called due to Mode ${settings.modes}"
    }
}