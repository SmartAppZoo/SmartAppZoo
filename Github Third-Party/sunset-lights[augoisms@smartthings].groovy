definition(
    name: "Sunset Lights",
    namespace: "augoisms",
    author: "augoisms",
    description: "Turn on one or more switches at sunset.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png"
)

preferences {
    section("Lights") {
        input "switches", "capability.switch", title: "Which lights to turn on?", multiple: true
        input "offset", "number", title: "Turn on this many minutes before sunset"
    }
    section("When to turn off") {
        input "timeOff", "time", title: "Turn off at this time", required: true
        input "modes", "mode", title: "select a mode(s)", multiple: true, required: true
	}
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    subscribe(location, "sunsetTime", sunsetTimeHandler)

    //schedule it to run today too
    scheduleTurnOn(location.currentValue("sunsetTime"))
    
    // schedule to turn off
    schedule(timeOff, "timeOffCallback")
}

def sunsetTimeHandler(evt) {
    //when I find out the sunset time, schedule the lights to turn on with an offset
    scheduleTurnOn(evt.value)
}

def scheduleTurnOn(sunsetString) {
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)

    //calculate the offset
    def timeBeforeSunset = new Date(sunsetTime.time - (offset * 60 * 1000))

    log.debug "Scheduling for: $timeBeforeSunset (sunset is $sunsetTime)"

    //schedule this to run one time
    runOnce(timeBeforeSunset, turnOn)
}

def turnOn() {
    log.debug "turning on lights"
    switches.on()
}

def timeOffCallback() {
    // only proceed if in vacation mode
    if(modes.contains(location.currentMode)) {
        switches.off()
    }
}