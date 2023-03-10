/**
 *  Author: Baur
 */

definition(
    name: "Speaker Notifier",
    namespace: "baurandr",
    author: "A. Baur",
    description: "Monitor your sensors and get speaker notifications",
    category: "Safety & Security",
    parent: "baurandr:Baur Speaker Automation",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Select contacts...") {
		input("contact1", "capability.contactSensor", title: "Which contact sensor?", multiple: true, required: false)
        input(name: "openOrClosed", type: "enum", title: "Notify when open or closed?", options: ["open","closed"], required: false)
    }
	section("Select motion sensors..."){
		input "motion1", "capability.motionSensor", title: "Which motion sensor(s)?", multiple: true, required: false
	}
    section("Notify on google home:") {
        input("speaker1", "capability.actuator", title: "Which speaker?", multiple: false)
    }
    section("Announce between what times? Both absolute and sun event based times must be true to turn lights on.") {
        input "fromTime", "time", title: "Start of allowed time window", required: false
        input "toTime", "time", title: "End of allowed time window", required: false
        input "onOffset", "number", title: "Start of allowed time based on Sunset offset (+ = after, - = before)", required: false
        input "offOffset", "number", title: "End of allowed time based on Sunrise offset (+ = after, - = before)", required: false
	}
	section("Announce during what modes?") {
    	input "modesTurnOnAllowed", "mode", title: "select a mode(s)", multiple: true, required: false
    }
}

def installed(){
	setupSubscribes()
}

def updated(){
	unsubscribe()
    setupSubscribes()
}

def setupSubscribes(){
	subscribe(contact1, "contact", eventHandler)
    subscribe(motion1, "motion", eventHandler)
}

def eventHandler(evt) {
	log.debug "$evt.device $evt.name: $evt.value"
    if (evt.value == "active" || evt.value == openOrClosed){ //Motion has started or contact open/closed
            //check time
            def timeOK = true
            if(fromTime && toTime){ 
                timeOK = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
            }
            if(onOffset && offOffset && timeOK){ 
                def sunTimes = getSunriseAndSunset()
                def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunTimes.sunset)
                def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunTimes.sunrise)

                //calculate the offset
                def timeAfterSunset = new Date(sunsetTime.time + (onOffset * 60 * 1000))
                def timeAfterSunrise = new Date(sunriseTime.time + (offOffset * 60 * 1000))

                timeOK = timeOfDayIsBetween(timeAfterSunset, timeAfterSunrise, new Date(), location.timeZone)
            }

            //check mode
            def curMode = location.currentMode
            def modeOK = !modesTurnOnAllowed || modesTurnOnAllowed.contains(curMode)

            log.debug "Current Mode: ${curMode}, Turn on Mode OK: ${modeOK}, Turn On time frame OK: ${timeOK}"

            if (timeOK & modeOK) {
                log.debug "Sending Announcement"
                def msg = "${evt.device} is ${evt.value}"
               	speaker1.customBroadcast(msg)
                //speaker1.customCommand("what is the weather", "baur", true)
            }
    }
}