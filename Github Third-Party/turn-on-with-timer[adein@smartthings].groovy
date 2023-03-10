/**
 *  Copyright 2017 Adein
 *
 *  Turn On With Timer
 *  Turn on an outlet when a switch triggers, and then turn it back off after a delay.
 *
 *  Turning off the switch will immediately turn off the outlet.
 */
definition(
    name: "Turn On With Timer",
    namespace: "adein",
    author: "adeinpublic@gmail.com",
    description: "When a switch turns on another switch will be turned on, and then turned off after a delay.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
    section("Use this switch as the trigger...") {
        input "trigger", "capability.switch", title: "Switch", multiple: false, required: true
    }
    section("Control this device..."){
        input "outlet", "capability.switch", title: "Outlet", multiple: false, required: true
    }
    section("Turn off after this many minutes..."){
        input "minutes", "number", title: "Minutes", required: true
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(trigger, "switch.on", onHandler)
    subscribe(trigger, "switch.off", offHandler)
}

def updated(settings) {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(trigger, "switch.on", onHandler)
    subscribe(trigger, "switch.off", offHandler)
}

def turnOffSwitch() {
    outlet.off()
    trigger.off()
}

def onHandler(evt) {
    outlet.on()
    def delay = minutes * 60
    runIn(delay, turnOffSwitch)
}

def offHandler(evt) {
    outlet.off()
}

