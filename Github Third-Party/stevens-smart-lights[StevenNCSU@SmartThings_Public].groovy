definition(
    name: "Steven's Smart Lights",
    namespace: "StevenNCSU/automations",
    author: "Steven Stewart",
    description: "A simple app to control basic lighting automations. This is a child app.",
    category: "My Apps",

    // the parent option allows you to specify the parent app in the form <namespace>/<app name>
    parent: "StevenNCSU/parent:Steven's Smart Lighting",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "Automate Lights & Switches", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "Automate Lights & Switches", install: true, uninstall: true
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unschedule()
    initialize()
}

def initialize() {
    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }
    // schedule the turn on and turn off handlers
    if (enableAutomation) {
    	schedule(turnOnTime, turnOnHandler)
    	schedule(turnOffTime, turnOffHandler)
    }
}

// main page to select lights, the action, and turn on/off times
def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            lightInputs()
            actionInputs()
        }
        timeInputs()
        enableAutomation()
    }
}

// page for allowing the user to give the automation a custom name
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

// inputs to select the lights
def lightInputs() {
    input "lights", "capability.switch", title: "Which lights do you want to control?", multiple: true, submitOnChange: true
}

// inputs to control what to do with the lights (turn on, turn on and set color, turn on
// and set level)
def actionInputs() {
    if (lights) {
        input "action", "enum", title: "What do you want to do?", options: actionOptions(), required: true, submitOnChange: true
        if (action == "color") {
            input "color", "enum", title: "Color", required: true, multiple:false, options: [
                ["Soft White":"Soft White - Default"],
                ["White":"White - Concentrate"],
                ["Daylight":"Daylight - Energize"],
                ["Warm White":"Warm White - Relax"],
                "Red","Green","Blue","Yellow","Orange","Purple","Pink"]

        }
        if (action == "level" || action == "color") {
            input "level", "enum", title: "Dimmer Level", options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: "80"
        }
    }
}

// utility method to get a map of available actions for the selected switches
def actionMap() {
    def map = [on: "Turn On", off: "Turn Off"]
    if (lights.find{it.hasCommand('setLevel')} != null) {
        map.level = "Turn On & Set Level"
    }
    if (lights.find{it.hasCommand('setColor')} != null) {
        map.color = "Turn On & Set Color"
    }
    map
}

// utility method to collect the action map entries into maps for the input
def actionOptions() {
    actionMap().collect{[(it.key): it.value]}
}

// inputs for selecting on and off time
def timeInputs() {
    if (settings.action) {
        section {
            input "turnOnTime", "time", title: "Time to turn lights on", required: true
            input "turnOffTime", "time", title: "Time to turn lights off", required: true
        }
    }
}

// inputs for selecting on and off time
def enableAutomation() {
    if (settings.action) {
        section {
            input "enableAutomation", "bool", title: "Enable Automation", defaultValue: "true", required: false
        }
    }
}

// a method that will set the default label of the automation.
// It uses the lights selected and action to create the automation label
def defaultLabel() {
    def lightsLabel = settings.lights.size() == 1 ? lights[0].displayName : lights[0].displayName + ", etc..."

    if (action == "color") {
        "Turn on and set color of $lightsLabel"
    } else if (action == "level") {
        "Turn on and set level of $lightsLabel"
    } else {
        "Turn $action $lightsLabel"
    }
}

// the handler method that turns the lights on and sets level and color if specified
def turnOnHandler() {
    // switch on the selected action
    switch(action) {
        case "level":
            lights.each {
                // check to ensure the switch does have the setLevel command
                if (it.hasCommand('setLevel')) {
                    log.debug("Not So Smart Lighting: $it.displayName setLevel($level)")
                    it.setLevel(level as Integer)
                }
                it.on()
            }
            break
        case "on":
            log.debug "on()"
            lights.on()
            break
        case "color":
            setColor()
            break
        }
}

// set the color and level as specified, if the user selected to set color.
def setColor() {

    def hueColor = 0
    def saturation = 100

    switch(color) {
            case "White":
            hueColor = 52
            saturation = 19
            break;
        case "Daylight":
            hueColor = 53
            saturation = 91
            break;
        case "Soft White":
            hueColor = 23
            saturation = 56
            break;
        case "Warm White":
            hueColor = 20
            saturation = 80
            break;
        case "Blue":
            hueColor = 70
            break;
        case "Green":
            hueColor = 39
            break;
        case "Yellow":
            hueColor = 25
            break;
        case "Orange":
            hueColor = 10
            break;
        case "Purple":
            hueColor = 75
            break;
        case "Pink":
            hueColor = 83
            break;
        case "Red":
            hueColor = 100
            break;
    }

    def value = [switch: "on", hue: hueColor, saturation: saturation, level: level as Integer ?: 100]
    log.debug "color = $value"

    lights.each {
        if (it.hasCommand('setColor')) {
            log.debug "$it.displayName, setColor($value)"
            it.setColor(value)
        } else if (it.hasCommand('setLevel')) {
            log.debug "$it.displayName, setLevel($value)"
            it.setLevel(level as Integer ?: 100)
        } else {
            log.debug "$it.displayName, on()"
            it.on()
        }
    }
}

// simple turn off lights handler
def turnOffHandler() {
    lights.off()
}