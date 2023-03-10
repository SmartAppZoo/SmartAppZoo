/**
 *  Motion in meeting rooms
 *
 *  Author: SmartThings
 */
definition(
    name: "Room Motion Monitor",
    namespace: "OSIOFFICES",
    author: "SmartThings",
    description: "Log motion to the database for usage tracking.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/intruder_motion.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/intruder_motion@2x.png"
)



preferences {
	section("When there's movement..."){
		input "motion1", "capability.motionSensor", title: "Where?"
	}
    section("Monitor the temperature...") {
        input "temperatureSensor1", "capability.temperatureMeasurement"
    }
	
}

def installed()
{
log.debug "Started install "
	subscribe(motion1, "motion.active", motionActiveHandler)
     subscribe(temperatureSensor1, "temperature", temperatureHandler)
    
}

def updated()
{
log.debug "Updated room motion monitor "


	unsubscribe()
	subscribe(motion1, "motion.active", motionActiveHandler)
     subscribe(temperatureSensor1, "temperature", temperatureHandler)
    
}
def temperatureHandler(evt) {
    log.debug "door sensor temperature event: $evt.value, $temperatureSensor1.displayName"

    def tooHot = temperature1
    def mySwitch = settings.switch1

    // TODO: Replace event checks with internal state (the most reliable way to know if an SMS has been sent recently or not).
    if (evt.doubleValue >= tooHot) {
        log.debug "Checking how long the temperature sensor has been reporting <= $tooHot"

        // Don't send a continuous stream of text messages
        def deltaMinutes = 5 // TODO: Ask for "retry interval" in prefs?
        def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
        def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
        log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
        def alreadySentSms = recentEvents.count { it.doubleValue >= tooHot } > 1

        if (alreadySentSms) {
            log.debug "SMS already sent to $phone1 within the last $deltaMinutes minutes"
            // TODO: Send "Temperature back to normal" SMS, turn switch off
        } else {
            log.debug "Temperature rose above $tooHot:  sending SMS to $phone1 and activating $mySwitch"
            def tempScale = location.temperatureScale ?: "F"
            //	send("${temperatureSensor1.displayName} is too hot, reporting a temperature of ${evt.value}${evt.unit?:tempScale}")
            switch1?.on()
            
            def params = [
                uri: "https://osiitservices.com/osiportal/pub/rest/monitorcapture.xhtml?room=${temperatureSensor1.displayName}&temp=${evt.value}",
  
            ]
              def params2 = [
      uri: "https://osiitservices.com/osiportal/pub/rest/monitorcapture.xhtml?room=${temperatureSensor1.displayName}&battery=${motion1.currentBattery}",
    
            ]

            try {
                httpPost(params) { resp ->
                    // iterate all the headers
                    // each header has a name and a value
                    resp.headers.each {
                        log.debug "${it.name} : ${it.value}"
                    }

                    // get an array of all headers with the specified key
                    def theHeaders = resp.getHeaders("Content-Length")

                    // get the contentType of the response
                    log.debug "response contentType: ${resp.contentType}"

                    // get the status code of the response
                    log.debug "response status code: ${resp.status}"

                    // get the data from the response body
                    log.debug "response data: ${resp.data}"
                }
                httpPost(params2) { resp ->
                    // iterate all the headers
                    // each header has a name and a value
                    resp.headers.each {
                        log.debug "${it.name} : ${it.value}"
                    }

                    // get an array of all headers with the specified key
                    def theHeaders = resp.getHeaders("Content-Length")

                    // get the contentType of the response
                    log.debug "response contentType: ${resp.contentType}"

                    // get the status code of the response
                    log.debug "response status code: ${resp.status}"

                    // get the data from the response body
                    log.debug "response data: ${resp.data}"
                }
            } catch (e) {
                log.error "something went wrong: $e"
            }
            
        }
    }
}
def motionActiveHandler(evt) {
	//log.trace "$evt.value: $evt, $settings"
	log.debug "${motion1.displayName} detected motion in room "
    def params = [
    uri: "https://osiitservices.com/osiportal/pub/rest/monitorcapture.xhtml?room=${motion1.displayName}&motion=yes",
    
]

try {
    httpPost(params) { resp ->
        // iterate all the headers
        // each header has a name and a value
        resp.headers.each {
       //    log.debug "${it.name} : ${it.value}"
        }

        // get an array of all headers with the specified key
        def theHeaders = resp.getHeaders("Content-Length")

        // get the contentType of the response
        log.debug "response contentType: ${resp.contentType}"

        // get the status code of the response
       log.debug "response status code: ${resp.status}"

        // get the data from the response body
        log.debug "response data: ${resp.data}"
    }
} catch (e) {
    log.error "something went wrong: $e"
}

//	sendSms(phone1, "${motion1.label ?: motion1.name} detected motion")
}