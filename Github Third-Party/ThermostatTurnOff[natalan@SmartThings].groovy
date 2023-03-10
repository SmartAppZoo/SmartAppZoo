/**
 *  Turn off thermostat when mode changed to X
 *
 *  Copyright 2014 skp19
 *
 */
definition(
        name: "Thermostat turn off",
        namespace: "belmass@gmail.com",
        author: "Andrei Zharov",
        description: "Turn off thermostat when mode changes to the selected mode.",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("When the mode changes to...") {
        input "alarmMode", "mode", multiple: true
    }
    section("Turn off these thermostats") {
        input "thermostats", "capability.thermostat", multiple: true
        input "notify", "bool", title: "Notify?"
    }
    section("Only between these times...") {
        input "startTime", "time", title: "Start Time", required: false
        input "endTime", "time", title: "End Time", required: false
    }
}

def installed() {
    subscribe(location, checkTime)
}

def updated() {
    unsubscribe()
    subscribe(location, checkTime)
}

def turnOffSelectedThermostats(evt) {
    if (evt.value in alarmMode) {
        log.trace "Mode changed to ${evt.value}. Turning off thermostats"
        thermostats?.off()
        sendMessage("Selected thermostats have been turned off")
    }
}

def checkTime(evt) {
    if(startTime && endTime) {
        def currentTime = new Date()
        def startUTC = timeToday(startTime)
        def endUTC = timeToday(endTime)
        if((currentTime > startUTC && currentTime < endUTC && startUTC < endUTC) || (currentTime > startUTC && startUTC > endUTC) || (currentTime < endUTC && endUTC < startUTC)) {
            turnOffSelectedThermostats(evt)
        }
    }
    else {
        turnOffSelectedThermostats(evt)
    }
}

def sendMessage(msg) {
    if (notify) {
        sendPush msg
    }
}
