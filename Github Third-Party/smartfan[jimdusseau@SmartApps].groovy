/**
 *  Smartfan
 *
 *  Author: Jim Dusseau
 *  Date: 2014-04-07
 *
 *  Thanks to Kris Linquist and his original Keep It Cool script
 */

definition(
    name: "Smart Fan",
    namespace: "",
    author: "Jim Dusseau",
    description: "Turns on a fan when it's colder outside and the inside temp is higher than set.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
  section("Choose a temperature sensor... ") {
    input "sensor", "capability.temperatureMeasurement", title: "Sensor"
  }

  section("Choose your fans...") {
    input "switches", "capability.switch", multiple: true
  }

  section("What is your zip code?") {
    input "zipcode", "text", title: "Zipcode?"
  }

  section("Turn fan off when it's colder than ") {
    input "lowTemp", "number", title: "Degrees F"
  }
}

def installed() {
  initialize()
  state.fanState = -1
}

def updated() {
  unschedule()
  initialize()
}

def initialize() {
  schedule("*/10 * * * * ?", scheduleCheck)
}

def scheduleCheck() {
  def data = getWeatherFeature( "conditions", zipcode )
  log.debug "Outside temp = $data.current_observation.temp_f  and internal temp = $sensor.currentTemperature"

  def internalTemp = sensor.currentTemperature

  def warmerOutside = data.current_observation.temp_f > internalTemp
  log.debug "warmer outside: $warmerOutside Internal temp: $internalTemp low temp: $lowTemp"

  if(!warmerOutside && internalTemp > lowTemp) {
  	log.debug "Fans should be on"
    if(state.fanState != 1) {
      log.debug "Turning fans on"
      switches.on()
      state.fanState = 1;
    }
    else {
      log.debug "Fans are already on"
    }
  }
  else {
	log.debug "Fans should be off"
  	if(state.fanState != 0) {
      log.debug "Turning fans off"
      switches.off()
      state.fanState = 0
    }
    else {
      log.debug "Fans are already off"
    }

  }
}
