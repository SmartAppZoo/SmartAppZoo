
definition(
    name: "setTimeout",
    namespace: "cramforce",
    author: "Malte",
    description: "Debounce switches based on switches, contact, and motion sensors.",
    category: "Convenience",
    iconUrl: "https://png.pngtree.com/svg/20170719/react_1353128.png",
    iconX2Url: "https://png.pngtree.com/svg/20170719/react_1353128.png"
)

preferences {
    section("Control switch") {
        // If this switch is off, the switches cannot be turned on.
        input "controlSwitch", "capability.switch", title: "Control switch (optional)", required: false
    }
    section("Debounce on motion") {
        input "motion", "capability.motionSensor", title: "Motion sensors (optional)", multiple: true, required: false
    }
    section("Debounce on door open") {
        input "contact", "capability.contactSensor", title: "Contact sensors (optional)", multiple: true, required: false
    }
    section("And off after minutes...") {
        input "minutes", "number", title: "Minutes?"
    }
    section("Turn on/off light(s)...") {
        input "switches", "capability.switch", multiple: true
    }
}

def installed() {
    subscribe(motion, "motion", stateChanged)
    subscribe(contact, "contact", stateChanged)
    subscribe(switches, "switch", stateChanged)
    if (controlSwitch) {
        subscribe(controlSwitch, "switch", controlChanged)
    }
}

def updated() {
    unsubscribe()
    installed()
}

def stateChanged(evt) {
    log.debug "$evt.name: $evt.value"
    if (evt.value == "on") {
        log.info "Switches turned on: ${evt.description}"
        maybeDebounce();
        return;
    }
    if (evt.value == "off") {
        log.info "Switches turned off: ${evt.description}"
        // Ignore the next event if light was manually turned off.
        state.ignoreNext = true;
        return;
    }
    if (evt.value == "active") {
        log.info "Motion active: ${evt.description}"
        maybeDebounce();
        return;
    }
    if (evt.value == "open") {
        log.info "Door open: ${evt.description}"
        maybeDebounce();
        return;
    }
}

def controlChanged(evt) {
    log.debug "$evt.name: $evt.value"
    if (evt.value == "on") {
        log.info "Control turned on: ${evt.description}"
        maybeDebounce();
        return;
    }
    if (evt.value == "off") {
        log.info "Control turned off: ${evt.description}"
        switches.off();
        return;
    }
}

def maybeDebounce() {
    log.info "Debounce."
    state.ignoreNext = false;
    if (controlSwitch?.currentValue("switch") == "off") {
        log.info "Debounce. Control switch is off. No action."
        return;
    }
    log.info "Debounce. Turning on lights. Scheduling timer."
    switches.on();
    runIn(minutes * 60, timerHandler)
}

def timerHandler() {
    if (state.ignoreNext) {
        log.info "Ignoring debounce"
        state.ignoreNext = false;
        return;
    }
    log.info "Timer fired"
    if (motion.currentValue("motion").any { log.debug it; it == "active" }) {
        log.info "Motion still active"
        maybeDebounce();
        return
    }
    if (contact.currentValue("contact").any { log.debug it; it == "open" }) {
        log.info "Door still open"
        maybeDebounce();
        return
    }
    log.info "No debounce condition met. Turning off lights"
    switches.off();
}
