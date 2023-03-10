/**
 *  Security Lights
 *
 *  Author: mwwalker@gmail.com
 *  Date: 2014-01-19
 */
preferences {

	section("Master Switch") {
    	input "masterSwitch", "capability.switch"
    }
	section("Devices to turn on/off...") {
		input "switchDevice", "capability.switch", title: "Switch?", required: false, multiple: true
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
	subscribe(masterSwitch, "switch", eventHandler)
	schedule("0 0 0 * * ?", scheduleSecurityLights)
    scheduleSecurityLights()
    eventHandler()
}

def scheduleSecurityLights() {

  def sunInfo = getSunriseAndSunset()
  
  log.info("Sunrise: ${sunInfo.sunrise}")
  log.info("Sunset: ${sunInfo.sunset}")
  
  runOnce(sunInfo.sunrise, sunriseEvent)
  runOnce(sunInfo.sunset, sunsetEvent)
  
}

def eventHandler(evt) {

	log.trace "$evt?.name: $evt?.value"
    
    def sunInfo = getSunriseAndSunset()
    def currentDate = new Date()
    
    log.debug("Sunrise: ${sunInfo.sunrise}")
    log.debug("Sunset: ${sunInfo.sunset}")
    log.debug("Current DateTime: ${sunInfo.sunset}")
    
    if(currentDate.after(sunInfo.sunrise) && currentDate.before(sunInfo.sunset)) {
    	switchDevice?.off()
    }
    else {
    	switchDevice?.on()
    }
    
}

def sunriseEvent() {
	if(enabled()){
		switchDevice?.off()
    }
}

def sunsetEvent() {
	if(enabled()){
		switchDevice?.on()
    }
}

def enabled() {
	if(masterSwitch.latestState("switch").value == "on") {
    	return true
    }
    return false
}
