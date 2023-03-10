/**
 *  ecobeeChangeMode
 *  Andrei Zharov
 *
 *  Change Ecobee Mode based on ST Mode change
 */
definition(
        name: "ecobeeChangeMode",
        namespace: "belmass@gmail.com",
        author: "Andrei Zharov",
        description: "Change the mode automatically at the ecobee thermostat(s)",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png"
)

preferences {
    section("Turn off these thermostats") {
        input "thermostats", "capability.thermostat", multiple: true
        input "notify", "bool", title: "Notify?"
    }
}


def installed() {
    subscribe(location, changeMode)
}

def updated() {
    unsubscribe()
    subscribe(location, changeMode)
}


def changeMode(evt) {
    def message
    def newMode = evt.value.trim().toUpperCase()
    def givenMode

    log.debug "New mode is $newMode"
    if (newMode == "AWAY") {
        givenMode = "Away"
        sendEvent(name: "presence", value: "not present")
    } else if (newMode == "HOME" || newMode == "MORNING") {
        givenMode = "Home"
        sendEvent(name: "presence", value: "present")
    } else if (newMode == "NIGHT") {
        givenMode = "Sleep"
    }
    message = "Setting thermostat(s) to $givenMode"
    log.debug message
    thermostats?.setThisTstatClimate(givenMode)
    sendMessage(message)
}

def sendMessage(msg) {
    if (notify) {
        sendPush msg
    }
}
