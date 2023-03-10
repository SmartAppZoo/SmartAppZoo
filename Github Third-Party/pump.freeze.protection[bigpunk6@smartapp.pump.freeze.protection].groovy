/**
 *  Turn on Pool Pump when Frezzing
 *
 *  Author: mkurtzjr@live.com
 *  Date: 2013-10-28
 */
preferences {
	section("Monitor the temperature with local sensor (optional, uses Weather Underground if not specified)") {
		input "temperatureSensor1", "capability.temperatureMeasurement", required: false
	}
	section("When the temperature drops below...") {
		input "temperature1", "number", title: "Temperature?"
	}
    section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipcode", "text", title: "Zip Code", required: false
	}
	section("Turn on Pool Pump") {
		input "switch1", "device.poolswitch"
	}
    section("Switch number") {
		input "instance", "number"
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
	unsubscribe()
    unschedule()
    initialize()
}

def initialize()
{
    log.debug "Settings: ${settings}"
    if (temperatureSensor1) {
        subscribe(temperatureSensor1, "temperature", temperatureHandler)
    } else {
        schedule("0 */10 * * * ?", "checkTemp")
    }
}

def checkTemp() {
    def conditions
	if(locationIsDefined()) {
		if(zipcodeIsValid()) {
			conditions = getWeatherFeature("conditions", zipcode)
            log.debug "Current Temperature $conditions.current_observation.temp_f"
		} else {
			log.warn "Pool Frezze Protection: Invalid zipcode entered, defaulting to location's zipcode"
			conditions = getWeatherFeature("conditions")
            log.debug "Current Temperature $conditions.current_observation.temp_f"
		}
        if (conditions.current_observation.temp_f <= temperature1) {
            log.debug "Temperature dropped below $temperature1:  activating $settings.switch1"
            switch1.on(instance)
	    } else {
            log.debug "Temperature above $temperature1:  deactivating $settings.switch1"
            switch1.off(instance)
        }
	} else {
		log.warn "Pool Frezze Protection: Location is not defined"
	}
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"
	def tooCold = temperature1
	def mySwitch = settings.switch1
	if (evt.doubleValue <= tooCold) {
        log.debug "Temperature dropped below $tooCold:  activating $mySwitch"
        switch1.on(instance)
	} else {
    log.debug "Temperature above $tooCold:  deactivating $mySwitch"
    switch1.off(instance)
    }
}

def locationIsDefined() {
	zipcodeIsValid() || location.zipCode || ( location.latitude && location.longitude )
}

def zipcodeIsValid() {
	zipcode && zipcode.isNumber() && zipcode.size() == 5
}
