/**
 *  Weather Alert Monitor
 *
 *  Author: bob
 *
 *  EXAMPLE NOT CURRENTLY PUBLISHED
 *
 *  Date: 2013-04-30
 */
preferences {
	section("Weather service..") {
		input "weather", "device.SmartweatherStationTile"
	}
	section ("In addition to push notifications, send text alerts to...") {
		input "phone1", "phone", title: "Phone Number 1", required: false
		input "phone2", "phone", title: "Phone Number 2", required: false
		input "phone3", "phone", title: "Phone Number 3", required: false
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
	subscribe(weather, "alert", alertHandler)
}

def alertHandler(evt) {
	if (evt.value != "no current weather alerts") {
		def msg = "Weather Alert! $evt.descriptionText"
		log.debug msg

		sendPush msg

		if (settings.phone1) {
			sendSms phone1, msg
		}
		if (settings.phone2) {
			sendSms phone2, msg
		}
		if (settings.phone3) {
			sendSms phone3, msg
		}
	}
}