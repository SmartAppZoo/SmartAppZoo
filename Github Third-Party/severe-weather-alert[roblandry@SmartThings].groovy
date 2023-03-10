/**
 *  Severe Weather Alert
 *
 *  Author: SmartThings / dome
 *  Date: 2014-03-03
 */

// Automatically generated. Make future change here.
definition(
    name: "Severe Weather Alert",
    namespace: "roblandry",
    author: "Rob Landry",
    description: "Severe weather alerts by push notification as well as by flashing LED strip. Requires Smart Room Controller - http://build.smartthings.com/projects/smartkitchen/",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather@2x.png"
)

preferences {

    section("Issue notifications with a Smart Room Controller") {
    	//input "Ledstrip", "device.SmartRoomController", required: false, multiple: true
	input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:false, multiple:true
       	input "color", "enum", title: "What color?", required: true, multiple: false, options: ["White", "Red", "Green", "Blue", "Orange", "Purple", "Yellow"]
    	input "speed", "enum", title: "What flash pattern?", required: true, multiple: false, options: ["Fade", "Flash", "Strobe", "Persist", "Alert"]
   	//input "audible", "enum", title: "What (optional) sound?", required: false, multiple: false, options: ["Beep1", "Beep2", "Siren1", "Siren2"]
   }
    

	section ("In addition to push notifications, send text alerts to...") {
		input "phone1", "phone", title: "Phone Number 1", required: false
		input "phone2", "phone", title: "Phone Number 2", required: false
		input "phone3", "phone", title: "Phone Number 3", required: false
	}

	section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipcode", "text", title: "Zip Code", required: false
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
    subscribe(app, appTouchHandler)
	schedule("0 */10 * * * ?", "checkForSevereWeather") //Check at top and half-past of every hour

}

def checkForSevereWeather() {
	def alerts
	if(locationIsDefined()) {
		if(zipcodeIsValid()) {
			alerts = getWeatherFeature("alerts", zipcode)?.alerts
		} else {
			log.warn "Severe Weather Alert: Invalid zipcode entered, defaulting to location's zipcode"
			alerts = getWeatherFeature("alerts")?.alerts
		}
	} else {
		log.warn "Severe Weather Alert: Location is not defined"
	}

	def newKeys = alerts?.collect{it.type + it.date_epoch} ?: []
	log.debug "Severe Weather Alert: newKeys: $newKeys"

	def oldKeys = state.alertKeys ?: []
	log.debug "Severe Weather Alert: oldKeys: $oldKeys"

	if (newKeys != oldKeys) {

		state.alertKeys = newKeys

		alerts.each {alert ->
			if (!oldKeys.contains(alert.type + alert.date_epoch)) {
				def msg = "Weather Alert! ${alert.description} from ${alert.date} until ${alert.expires}"
				send(msg)
			}
		}
	}
    //send(msg)
}


def locationIsDefined() {
	zipcodeIsValid() || location.zipCode || ( location.latitude && location.longitude )
}

def zipcodeIsValid() {
	zipcode && zipcode.isNumber() && zipcode.size() == 5
}

private send(message) {
	sendPush message
	if (settings.phone1) {
		sendSms phone1, message
	}
	if (settings.phone2) {
		sendSms phone2, message
	}
	if (settings.phone3) {
		sendSms phone3, message
	}
    alert()
}

def appTouchHandler(evt) {
	alert()
}

def alert() {
    
	def hueColor = 0
	def saturation = 100

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 //83
			break;
		case "Blue":
			hueColor = 70
			break;
		case "Green":
			hueColor = 39
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
		case "Red":
			hueColor = 100
			break;
	}

	def duration = 1
	def loop = 0
	switch(speed) {
		case "Fade":
			duration = 1 * 1000
			loop = 1
			break;
		case "Flash":
			duration = 1 * 500
			loop = 5
			break;
		case "Strobe":
			duration = 1 * 100
			loop = 5
			break;
		case "Persist":
			break;
		case "Alert":
			duration = 1 * 100
			loop = 10
			hueColor = 100
			break;
	}

	state.previous = [:]

	hues.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation")
		]
	}


	def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]

	// change color - for persistent notification
	log.debug "Change color to $newValue"
	hues*.setColor(newValue)

	for (def i = 0; i < loop; i++) {

		// change color
		log.debug "Change color to $newValue"
		hues*.setColor(newValue)

		// wait
		log.debug "Wait $duration seconds"
		setTimer(duration)

		// reset
		log.debug "Reset color"
		resetHue()

		// wait
		log.debug "Wait $duration seconds"
		setTimer(duration)

	}
}

def setTimer(duration) {
	log.debug "pause $duration"
	pause(duration)
}


def resetHue() {
	hues.each {
		it.setColor(state.previous[it.id])
		log.debug "New hue value = $state.previous"
	}
}