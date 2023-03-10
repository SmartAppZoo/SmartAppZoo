/**
 *  Harmony Switch Service Manager
 *
 *  Copyright 2015 Bill Napier
 *
 */
definition(
    name: "Harmony Switch Service Manager",
    namespace: "billnapier",
    author: "Bill Napier",
    description: "Harmony Switch Service Manager",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name: "configPage")
}

def configPage() {
    dynamicPage(name: "configPage", title: "", install: true, uninstall: true) {

        section {
            input(name: "harmony", type: "capability.refresh",// type: "device.logitechHarmonyHubC2C", 
                  title: "Harmony Hub", description: null, required: true, submitOnChange: true)
        }

        if (harmony) {
            def activities = getActivities()
            def activityNames = activities.findAll {it.id != "off"}.collect { it.name }
            section {
                input(name: "onActivityName", type: "enum", title: "Pick activity to use for \"ON\"", 
                      required: true, options: activityNames)
            }
        }
    }
}

private getActivities() {
  return new groovy.json.JsonSlurper().parseText(harmony.currentActivities)
}


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    harmony.refresh()

    addDevice()
}

private addDevice() {
    log.debug "Going to add a new device"
    // TODO(napier): figure out a better scheme here, in case we have multiple switches installed.
    def dni = "billnapier:harmony"
    def d = getChildDevice(dni)
    if (!d) {
        d = addChildDevice("billnapier", "Harmony Virtual Switch", dni, null, [label:"Harmony Virtual Switch"])
    }
}

def getSwitchStatus() {
  harmony.refresh()
  if (harmony.currentCurrentActivity == "--") {
    log.debug "Switch Status: off"
    return "off";
  }
  log.debug "Switch Status: on"
  return "on";
}

def off() {
  harmony.alloff()
}

def on() {
  // Find activity id
  def activityId = getActivities().findResult{it.name == onActivityName}
  log.debug "BILL"
  log.debug activityId
  if (activityId) {
    harmony.startActivity(Integer.toString(activityId))
  }
}
// TODO: implement event handlers