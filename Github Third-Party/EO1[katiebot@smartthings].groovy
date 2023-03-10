/**
 *  Electric Objects EO1
 *
 *	Version: 1.0 - Initial Version
 */
definition(
    name: "EO1 Controller",
    namespace: "katiebot",
    author: "Katie Graham",
    description: "Controls the Electric Objects EO1",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Turn EO1 on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn EO1 off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
    }
    section("EO1 device ID") {
        input "deviceid", "number", required: true, title: "EO1 device ID?"
    }
    section("Remember user token") {
        input "rememberusertoken", "text", required: true, title: "Remember user token?"
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
}

private toggleEO1(String backlightstate) {
    def params = [
        uri: "https://www.electricobjects.com/api/v6/devices/${deviceid}?backlight_state=${backlightstate}",
        headers: ["Cookie": "remember_user_token=${rememberusertoken}"],
        tlsVersion: "TLSv1.1"
    ]

    try {
        httpPutJson(params) { resp ->
            resp.headers.each {
            log.debug "${it.name} : ${it.value}"
        }
        log.debug "response contentType: ${resp.contentType}"
        log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    toggleEO1("true")
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    runIn(60 * minutes, checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"

    def motionState = themotion.currentState("motion")

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            toggleEO1("false")
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
            runIn(60 * minutes, checkMotion)
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Motion is active, do nothing and wait for inactive"
    }
}
