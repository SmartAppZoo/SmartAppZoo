/**
 *  Door Alert After Dark
 *
 *  Author: Matt Rogers
 *
 *  Date: 2015-10-17
 */
definition(
    name: "Door Alert After Dark",
    namespace: "rogersmj",
    author: "Matt Rogers",
    description: "Sends notification when door is open during sunset times.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
    section("Select door(s)") {
		input "doors", "capability.contactSensor", title: "Which doors?", multiple: true
	}
	section ("Sunset offset (optional)") {
    	paragraph "How long before or after sunset do you want the check to happen?"
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Zip code") {
    	paragraph "Optional, defaults to location coordinates."
		input "zipCode", "text", title: "Zip Code", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
	}
}

def installed() {
	initialize()
}

def updated() {
	unschedule()
	initialize()
}

def initialize() {
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	log.trace "sunriseSunsetTimeHandler()"
	astroCheck()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunsetOffset: sunsetOffset)

	def now = new Date()
	def setTime = s.sunset
	log.debug "setTime: $setTime"

	if (state.setTime != setTime.time) {
		unschedule("sunsetHandler")

	    if(setTime.before(now)) {
		    setTime = setTime.next()
	    }

		state.setTime = setTime.time

		log.info "scheduling sunset handler for $setTime"
	    schedule(setTime, sunsetHandler)
	}
}

def sunsetHandler() {
	log.info "Executing sunset handler"
    doors?.each { door ->
    	def doorName = door.displayName
        def doorOpen = checkDoor(door)
        log.debug("Door $doorName: $doorOpen")
        if (doorOpen == "open") {	
            send("Alert: It's sunset and $doorName is open.")
        }
    }
}


private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	log.debug msg
}

def checkDoor(door) {
	def latestValue = door.currentValue("contact")
}

private getLabel() {
	app.label ?: "SmartThings"
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}