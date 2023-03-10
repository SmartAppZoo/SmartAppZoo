/*
 *  Galaxy Home Time of Day Auto Brightness Service SmartApp
 *  
*/
definition(
    name: "Galaxy Home Auto Brightness Service",
    namespace: "cosmicc",
    author: "Ian Perry",
    description: "Galaxy Home Time of Day Auto Brightness Service SmartApp",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true) {
    appSetting "token"
}


preferences {
	section("Title") {
		// TODO: put inputs here
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
    subscribe(location, "sunset", sunsetHandler)
    subscribe(location, "sunrise", sunriseHandler)
    schedule("0 0 23 * * ?", todnight)
    schedule("0 0 1 * * ?", todlate)
    sunriseTurnOn(location.currentValue("sunriseTime"))
    //sunsetTurnOn(location.currentValue("sunsetTime"))
}

void sendautobrite(brite) {
 try {
      httpPost (uri: "https://api.particle.io/v1/devices/events",
        body: [access_token: appSettings.token,
        name: "ghmcmd",
        private: false,
        data: "T${brite}" ] ) {response -> log.debug "Auto Brightness change sent: T${brite}, Response: ${response.data}" }
        }
     catch (e) {
   		log.error "error: $e"
    }
}


def sunsetHandler(evt) {
 log.trace "Running sunsetHandler - data: ${evt}"
    sunsetTurnOn(evt.value)
}

def sunriseHandler(evt) {
log.trace "Running sunriseHandler - data: ${evt}"
    sunriseTurnOn(evt.value)
}

def sunriseTurnOn(sunriseString) {
    //get the Date value for the string
    def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)
    //calculate the offset                             //v minutes
    def timeAfterSunrise = new Date(sunriseTime.time + (180 * 60 * 1000))
    def tenAfterSunrise = new Date(sunriseTime.time + (30 * 60 * 1000))    
    //log.debug "Scheduling for: $timeBeforeSunset (sunset is $sunsetTime)"
    //schedule this to run one time
    log.trace "Running sunriseturnOn - data: ${sunriseTime}"
    runOnce(timeAfterSunrise, todday)
    runOnce(tenAfterSunrise, todsunrise)
}

def sunsetTurnOn(sunsetString) {
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    //calculate the offset                          //v minutes
    def timeAfterSunset = new Date(sunsetTime.time + (90 * 60 * 1000))
    //log.debug "Scheduling for: $timeBeforeSunset (sunset is $sunsetTime)"
    //schedule this to run one time
     log.trace "Running sunsetturnOn - data: ${sunsetTime}"
    runOnce(timeAfterSunset, todeve)
}

def todsunrise() {
 sendautobrite(50)
}

def todday() {
 sendautobrite(100)
}

def todeve() {
 sendautobrite(75)
}

def todnight() {
 sendautobrite(50)
}

def todlate() {
 sendautobrite(20)
}