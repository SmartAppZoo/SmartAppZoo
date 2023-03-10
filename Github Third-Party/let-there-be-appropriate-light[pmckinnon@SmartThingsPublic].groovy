/**
 * Work in Progress.
 * Stolen from Todd Wackford:
 *   https://github.com/twack/smarthings-apps/blob/master/dim-with-me.app.groovy
 */


// Automatically generated. Make future change here.
definition(
    name: "Time of Day Dimmer",
    namespace: "pmckinnon",
    author: "mail@patrickmckinnon.com",
    description: "Set's slave dimmer level appropriate for time of day when switch is turned on",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    section("When this...") {
        input "masters", "capability.switch",
            multiple: false,
            title: "Master Switch...",
            required: true
    }

    section("And these will follow with appropriate dimming level...") {
        input "slaves", "capability.switchLevel",
            multiple: true,
            title: "Slave Dimmer Switch(es)...",
            required: true
    }
}

def installed()
{
    subscribe(masters, "switch.on", switchOnHandler)
    subscribe(masters, "switch.off", switchOffHandler)
        def sun = getSunriseAndSunset()
    def sunset = sun.sunset
    log.debug "sun: ${sun.sunset.toCalendar().get(Calendar.HOUR_OF_DAY);}"
    log.debug "now: ${new Date(now()).toCalendar().get(Calendar.HOUR_OF_DAY);}"
}

def updated()
{
    unsubscribe()
    subscribe(masters, "switch.on", switchOnHandler)
    subscribe(masters, "switch.off", switchOffHandler)
    log.info "subscribed to all of switches events"
}

def switchOffHandler(evt) {
    log.info "switchoffHandler Event: ${evt.value}"
    slaves?.off()
}

def getLevelFromSun() {
    def sun = getSunriseAndSunset()
    def sunset = sun.sunset
    log.debug "sun: ${sun.sunset.toCalendar().get(Calendar.HOUR_OF_DAY);}"
    log.debug "now: ${new Date(now()).toCalendar().get(Calendar.HOUR_OF_DAY)}, ${sun.sunrise.toCalendar().get(Calendar.HOUR_OF_DAY)}, ${sun.sunset.toCalendar().get(Calendar.HOUR_OF_DAY)}"
}

def switchOnHandler(evt) {
    log.info "switchOnHandler Event: ${evt.value}"
    getLevelFromSun();
    def level = 50
        log.info "Setting slave level: ${level}"
    slaves?.on()
        slaves?.setLevel(level)
}
