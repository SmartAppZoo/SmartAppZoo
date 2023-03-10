/**
 *  Severe Weather Alert
 *
 *  Author: SmartThings
 *  Date: 2013-03-04
 */
preferences {
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
	schedule("0 */10 * * * ?", "checkForSevereWeather") //Check at top and half-past of every hour
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
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
			if (!oldKeys.contains(alert.type + alert.date_epoch) && !alert.description.contains("Special") && !alert.description.contains("Statement")) {
				def msg = "Weather Alert! ${alert.description} from ${alert.date} until ${alert.expires}"
				send(msg)
			}
		}
	}
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
}