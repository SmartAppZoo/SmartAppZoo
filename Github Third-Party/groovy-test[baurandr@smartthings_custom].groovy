/**
 *  Author: Baur
 Test GitHub
 */

definition(
    name: "Groovy Test",
    namespace: "baurandr",
    author: "A. Baur",
    description: "Turn your lights on to set level when motion is detected and then off again once the motion stops for a set period of time.",
    category: "Convenience",
    parent: "baurandr:Baur Lighting",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {/*
	section("Turn on when there's movement..."){
		input "motion1", "capability.motionSensor", title: "Where?"
	}
            section("Sun State Device"){
				input "sunStateDevice", "capability.sensor", title: "?"
			}*/
}

def installed() {
	subscribe(motion1, "motion", motionHandler)
    def params = [
        uri: "http://baurfam.com/exportBoilerTemps"
    ]

    try {
        httpGet(params) { resp ->
            resp.headers.each {
            log.debug "${it.name} : ${it.value}"
        }
        log.debug "response contentType: ${resp.contentType}"
        log.debug "response data: ${resp.data}"
        def maxBoilerOut = resp.data.maxBoilerOut
		log.debug "maxBoilerOut: ${maxBoilerOut}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

def updated() {
	unsubscribe()
	subscribe(motion1, "motion", motionHandler)
}

def motionHandler(evt) {
changeToNight()
}

def changeToDusk() {
    log.debug "Changing to dusk mode"
	sunStateDevice.setSunState("Dusk")
}
def changeToDawn() {
    log.debug "Change to dawn mode"
    sunStateDevice.setSunState("Dawn")
}
def changeToDay() {
    log.debug "Changing to day mode"
    sunStateDevice.setSunState("Day")
}
def changeToNight() {
    log.debug "Change to night mode"
    sunStateDevice.setSunState("Night")
}