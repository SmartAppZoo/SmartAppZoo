definition(
		name: "MySQL Event Logger",
		namespace: "ndroo",
		author: "Andrew McGrath",
		description: "Log SmartThings events to a REST endpoint so they can be pushed into a MySQL databases server",
		category: "Convenience",
		iconUrl: "http://icons.iconarchive.com/icons/graphics-vibe/developer/256/mysql-icon.png",
		iconX2Url: "http://icons.iconarchive.com/icons/graphics-vibe/developer/256/mysql-icon.png",
		iconX3Url: "http://icons.iconarchive.com/icons/graphics-vibe/developer/256/mysql-icon.png")


preferences {
	section("Log these presence sensors:") {
		input "presences", "capability.presenceSensor", multiple: true, required: false
	}
	section("Log these switches:") {
		input "switches", "capability.switch", multiple: true, required: false
	}
	section("Log these switch levels:") {
		input "levels", "capability.switchLevel", multiple: true, required: false
	}
	section("Log these motion sensors:") {
		input "motions", "capability.motionSensor", multiple: true, required: false
	}
	section("Log these temperature sensors:") {
		input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false
	}
	section("Log these humidity sensors:") {
		input "humidities", "capability.relativeHumidityMeasurement", multiple: true, required: false
	}
	section("Log these contact sensors:") {
		input "contacts", "capability.contactSensor", multiple: true, required: false
	}
	section("Log these alarms:") {
		input "alarms", "capability.alarm", multiple: true, required: false
	}
	section("Log these indicators:") {
		input "indicators", "capability.indicator", multiple: true, required: false
	}
	section("Log these CO detectors:") {
		input "codetectors", "capability.carbonMonoxideDetector", multiple: true, required: false
	}
	section("Log these smoke detectors:") {
		input "smokedetectors", "capability.smokeDetector", multiple: true, required: false
	}
	section("Log these water detectors:") {
		input "waterdetectors", "capability.waterSensor", multiple: true, required: false
	}
	section("Log these acceleration sensors:") {
		input "accelerations", "capability.accelerationSensor", multiple: true, required: false
	}
	section("Log these power meters:") {
		input "energymeters", "capability.powerMeter", multiple: true, required: false
	}
	section("Log these door locks:") {
		input "locks", "capability.lock", multiple: true, required: false
	}

	section ("REST Server") {
		input "rest_url", "text", title: "API host / IP"
			input "rest_key", "text", title: "Self generated authentication key"
	}

}

def installed() {
	log.debug "Installed: ${settings}"
		initialize()
}

def updated() {
	log.debug "Updated: ${settings}"

		unsubscribe()
		initialize()
}

def initialize() {
	doSubscriptions()
}

def doSubscriptions() {
	subscribe(alarms,			"alarm",					genericHandler)
		subscribe(codetectors,		"carbonMonoxideDetector",	genericHandler)
		subscribe(contacts,			"contact",      			genericHandler)
		subscribe(indicators,		"indicator",    			genericHandler)
		subscribe(modes,			"locationMode", 			genericHandler)
		subscribe(motions,			"motion",       			genericHandler)
		subscribe(presences,		"presence",     			genericHandler)
		subscribe(relays,			"relaySwitch",  			genericHandler)
		subscribe(smokedetectors,	"smokeDetector",			genericHandler)
		subscribe(switches,			"switch",       			genericHandler)
		subscribe(levels,			"level",					genericHandler)
		subscribe(temperatures,		"temperature",  			genericHandler)
		subscribe(waterdetectors,	"water",					genericHandler)
		subscribe(location,			"location",					genericHandler)
		subscribe(accelerations,    "acceleration",             genericHandler)
		subscribe(energymeters,     "power",               		genericHandler)
		subscribe(locks,     		"lock",               		genericHandler)
}

def genericHandler(evt) {
	def json = "{"
		json += "\"date\":\"${evt.date}\","
		json += "\"name\":\"${evt.name}\","
		json += "\"displayName\":\"${evt.displayName}\","
		json += "\"device\":\"${evt.device}\","
		json += "\"deviceId\":\"${evt.deviceId}\","
		json += "\"value\":\"${evt.value}\","
		json += "\"isStateChange\":\"${evt.isStateChange()}\","
		json += "\"id\":\"${evt.id}\","
		json += "\"description\":\"${evt.description}\","
		json += "\"descriptionText\":\"${evt.descriptionText}\","
		json += "\"installedSmartAppId\":\"${evt.installedSmartAppId}\","
		json += "\"isoDate\":\"${evt.isoDate}\","
		json += "\"isDigital\":\"${evt.isDigital()}\","
		json += "\"isPhysical\":\"${evt.isPhysical()}\","
		json += "\"location\":\"${evt.location}\","
		json += "\"locationId\":\"${evt.locationId}\","
		json += "\"unit\":\"${evt.unit}\","
		json += "\"source\":\"${evt.source}\","
		json += "\"program\":\"SmartThings\""
		json += "}"

		def params = [
		uri: "http://${rest_url}?key=${rest_key}",
		body: json
		]
		try {
			httpPostJson(params)
		} catch ( groovyx.net.http.HttpResponseException ex ) {
			log.debug "Unexpected error: ${ex.statusCode}"
		}
}

