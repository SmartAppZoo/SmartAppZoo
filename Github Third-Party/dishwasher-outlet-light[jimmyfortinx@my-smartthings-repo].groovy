definition(
    name: "Dishwasher Outlet Light",
    namespace: "jimmyfortinx",
    author: "Jimmy Fortin",
	description: "Synchronize a ligth with a dishwasher power meter to know its state.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "Dishwasher Power Meter", required: true, multiple: false, description: null)
        input(name: "openThreshold", type: "number", title: "Consider Open Above...", required: true, description: "in either watts or kw.")
        input(name: "runningThreshold", type: "number", title: "Consider Running Above...", required: true, description: "in either watts or kw.")
	}
    section {
    	input(name: "switches", type: "capability.switch", title: "Control These Switches", required: true, multiple: true, description: null)
        input(name: "openLevel", type: "number", range: "0..100", title: "Level when open", defaultValue: 100, description: null, required: true, multiple: false)
        input(name: "runningLevel", type: "number", range: "0..100", title: "Lvel when running", defaultValue: 50, description: null, required: true, multiple: false)
    }
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
	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {
    def meterValue = evt.value as double
    if (meterValue > runningThreshold as int) {
        dishwasherRunning()
    } else if (meterValue > openThreshold as int) {
        dishwasherOpened()
    } else {
        dishwasherClosed()
    }
}

def dishwasherRunning() {
    if (state.previousState == "running") 
        return;

    state.previousState = "running"

    log.debug "Dishwasher is running."
    switches.setLevel(runningLevel)
}

def dishwasherOpened() {
    if (state.previousState == "opened") 
        return;

    state.previousState = "opened"

    log.debug "Dishwasher is opened."
    switches.setLevel(openLevel)
}

def dishwasherClosed() {
    if (state.previousState == "closed") 
        return;

    state.previousState = "closed"

    log.debug "Dishwasher is closed."
    switches.off()
}