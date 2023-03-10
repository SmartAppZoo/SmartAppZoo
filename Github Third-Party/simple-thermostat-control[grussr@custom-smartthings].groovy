definition(
    name: "Simple Thermostat Control",
    namespace: "thermostatcontrol/automations",
    author: "Ryan Gruss",
    description: "A simple app to control thermostat automations. This is a child app.",
    category: "My Apps",

    // the parent option allows you to specify the parent app in the form <namespace>/<app name>
    parent: "thermostatcontrol/parent:Smart Thermostat Control",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "Smart Thermostat Control", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "Smart Thermostat Control", install: true, uninstall: true
}

def mainPage() {
	dynamicPage(name: "mainPage") {
    	section("Choose thermostat (s)") {
			input "thermostats", "capability.thermostat", required: true, multiple: true
		}

    	section("Set mode temperatures") {
        	input "opHeatSet", "decimal", title: "When Heating", description: "Heating temperature for mode", required: false
        	input "opCoolSet", "decimal", title: "When Cooling", description: "Cooling temperature for mode", required: false
    	}
        
        section("Mode Selection") {
        	mode(title: "Set for specific mode(s)")
		}
    }
}

def namePage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

def defaultLabel() {
    def thermosLabel = settings.thermostats.size() == 1 ? thermostats[0].displayName : thermostats[0].displayName + ", etc..."
    def modesLabel = settings.modes.join(",")
    def tempLabelArray = []
    if (opHeatSet) {
    	tempLabelArray.add(" heat to $opHeatSet°")
    }
    if (opCoolSet) {
    	tempLabelArray.add(" cool to $opCoolSet°")
    }
    def tempLabel = tempLabelArray.join(",")
    "Set$tempLabel on $thermosLabel when $modesLabel"
}

def installed() {
	subscribeToEvents()
}

def updated() {
    unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
    subscribe(location, "mode", modeChangeHandler)
}

// Handle mode changes, reinitialize the current temperature and timers after a mode change
def modeChangeHandler(evt) {
    if (opHeatSet) {
    	thermostats.setHeatingSetpoint(opHeatSet)
	    log.info "Set $thermostats Heat $opHeatSet° on $evt.value mode"
    	sendNotificationEvent("Set $thermostats Heat $opHeatSet° on $evt.value mode")
    }
    if (opCoolSet) {
    	thermostats.setCoolingSetpoint(opCoolSet)
	    log.info "Set $thermostats Cool $opCoolSet° on $evt.value mode"
    	sendNotificationEvent("Set $thermostats Cool $opCoolSet° on $evt.value mode")
    }
}
