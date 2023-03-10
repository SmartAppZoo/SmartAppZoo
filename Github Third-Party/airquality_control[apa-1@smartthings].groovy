/**
 *  Foobot Air quality controls
 *  Controls Holmes Wemo Air Purifiers, Keen Smart Vents, Thermostat Fans and optionally a switch for a dehumidifier based on data from a Foobot
 */
definition(
        name: "Air Quality Controls for Foobot",
        namespace: "apa-1",
        author: "alex",
        description: "Controls wemo air purifier based on foobot conditions",
        category: "My Apps",
        iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn.png",
        iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png"
)

preferences {
    section("Foobot") {
        input("foobot", "capability.sensor", title: "Select a Foobot to monitor:", required: true, multiple: false)
    }


    section("Air Purifiers") {
        input("airpurifiers", "capability.switch", title: "Select Wemo Air Purifiers", required: true, multiple: true)
        input("air_great_mode", "enum", title: "Select mode for 'Great' GPI", required: true, options: ["Off", "Low", "Med", "High", "Auto"], defaultValue: "Off")
        input("air_good_mode", "enum", title: "Select mode for 'Good' GPI", required: true, options: ["Off", "Low", "Med", "High", "Auto"], defaultValue: "Low")
        input("air_fair_mode", "enum", title: "Select mode for 'Fair' GPI", required: true, options: ["Off", "Low", "Med", "High", "Auto"], defaultValue: "Med")
        input("air_poor_mode", "enum", title: "Select mode for 'Poor' GPI", required: true, options: ["Off", "Low", "Med", "High", "Auto"], defaultValue: "High")
    }

}

def installed() {
    initialized()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialized()
}

def initialized() {
    subscribe(foobot, "polling", airHandler)
    subscribe(foobot, "refresh", airHandler)
    subscribe(foobot, "GPIState", airHandler)
}

def get_mode(gpistate) {
    if (gpistate == "great") {
        return air_great_mode
    }
    if (gpistate == "good") {
        return air_good_mode
    }
    if (gpistate == "fair") {
        return air_fair_mode
    }
    if (gpistate == "poor") {
        return air_poor_mode
    }
}

def airHandler(evt) {
    def gpistate = foobot.currentValue("GPIState")
    log.debug("Current GPI State is: $gpistate")

    //setting purifiers
    for (purifier in airpurifiers) {
        log.debug "Checking $purifier"
        def currentMode = purifier.latestState('mode').stringValue
        log.debug("State of $purifier: $currentMode")
        def desiredMode = get_mode(gpistate)
        log.debug("Desired State of $purifier: $desiredMode")
        if (currentMode.toLowerCase() != desiredMode.toLowerCase()) {
            log.debug("Setting $purifier")
            switch (desiredMode.toLowerCase()) {
                case "off":
                    purifier.fanOff()
                    break
                case "low":
                    purifier.fanLow()
                    break
                case "med":
                    purifier.fanMed()
                    break
                case "high":
                    purifier.fanHigh()
                    break
                case "auto":
                    purifier.fanAuto()
                    break
            }
        } else {
            log.debug("Purifier Mode is correct not changing")
        }
    }

}

