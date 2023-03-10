/**
 *  Porch Light
 *
 *  Author: Jim Dusseau
 *  Date: 2014-01-05
 */
preferences {
	section("When any of these people arrive") {
    input "people", "capability.presenceSensor", multiple: true
	}

  section("Turn on these lights") {
		input "switches", "capability.switch", multiple: true
  }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	people.each {
		subscribe(it.presence)
	}
}

//---------

def presence(evt)
{
	log.debug "evt.name: $evt.value"
	def threshold = falseAlarmThreshold != null ? (falseAlarmThreshold * 60 * 1000) as Long : 10 * 60 * 1000L

	if (location.mode != newMode) {

		def t0 = new Date(now() - threshold)
		if (evt.value == "present") {

			def person = getPerson(evt)
			def recentNotPresent = person.statesSince("presence", t0).find{it.value == "not present"}
			if (recentNotPresent) {
				log.debug "skipping notification of arrival of ${person.displayName} because last departure was only ${now() - recentNotPresent.date.time} msec ago"
			}
			else {
				def message = "${person.displayName} arrived at home."
				log.info message
          turnOnLight()

			}
		}
	}
	else {
		log.debug "mode is the same, not evaluating"
	}
}

private getPerson(evt) {
	people.find{evt.deviceId == it.id}
}

private turnOnLight() {
    def sunRiseAndSunset = getSunriseAndSunset(sunsetOffset: -30)
    def timenow = now()
    if (timenow > sunRiseAndSunset.sunset.time  || timenow < sunRiseAndSunset.sunrise.time) {
    	log.debug "$timenow is not between sunrise: $sunRiseAndSunset.sunrise.time and sunset: $sunRiseAndSunset.sunset.time . Turning on lights!"
		def message = "Turning on porch light because someone's home and it's dark"
        log.info message
        //sendPush(message)

        for (aSwitch in switches) {
        	log.debug "Turning on ${aSwitch.displayName}"
        	aSwitch.on()
        }
    }
    else {
    	log.debug "$timenow is between sunrise: $sunRiseAndSunset.sunrise.time and sunset: $sunRiseAndSunset.sunset.time - Ignorning"
    }
}
