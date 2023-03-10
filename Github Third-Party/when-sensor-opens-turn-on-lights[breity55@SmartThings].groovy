definition(
	name: "When Sensor Opens Turn On Lights",
	namespace: "breity55",
	author: "Alex Breitenstein",
	description: "When a sensor is opened between sunset and sunrise then turn on selected lights.",
	category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet@2x.png"
)

preferences {
	
	section("When Sensor Opens...") {
		input "sensorInput", "capability.contactSensor", title: "Which Sensor?", required: true
	}
	section("Turn On Lights...") {
		input "lightInputs", "capability.switch", title: "Which Light(s)?", required: true, multiple: true
	}
    section("Time and Place...") {
        input "zipCodeInput", "text", title: "Zip Code?", required: true
    	input "sunsetOffsetValueInput", "text", title: "Offset sunset by? (HH:MM)", required: false
        input "sunriseOffsetValueInput", "text", title: "Offset surise by? (HH:MM)", required: false
    }
}

def installed() {
   	subscribeEvents()
}

def updated() {
	unsubscribe()
	subscribeEvents()
}

def subscribeEvents() {
	subscribe(sensorInput, "contact.open", handleEvent)
}

def handleEvent(event) {
    def currentTime = new Date()
	def sunriseAndSunset = getSunriseAndSunset(zipCode: zipCodeInput, sunsetOffset: sunsetOffsetValueInput, sunriseOffset: sunriseOffsetValueInput)

	if(currentTime < sunriseAndSunset.sunrise || currentTime > sunriseAndSunset.sunset) {
		lightInputs.on()
	}
}