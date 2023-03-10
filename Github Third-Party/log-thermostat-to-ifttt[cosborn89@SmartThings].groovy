/**
 *  IFTTT Thermostat Logger
 *  Logs current state, current temp, and current setpoint.
 *	Current triggers are state change, temp change, setpoint change
 */


definition(
    name: "Log Thermostat to IFTTT",
    namespace: "tierneykev",
    author: "tierneykev@gmail.com",
    description: "Log thermostat changes to IFTTT",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Monitor This Thermostat") {	
             input "thermostat", "capability.thermostat", title: "Which thermostat?", multiple: false
	} 
    
    //You must set up the Maker channel on IFTTT https://ifttt.com/maker
    //The channel has a single API Key per account
    //Each recipe requires a distinct event name    
    section("Send Pushover alert (optional)"){
        input "apiKey", "text", title: "IFTTT API Key", required: true
        input "makerEvent","text",title: "IFTTT Event Name", required: true
    } 
} 

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"    
	initialize()
}


def initialize() {
    subscribe(thermostat,"temperature",thermostatHandler)
    subscribe(thermostat,"thermostatOperatingState",thermostatHandler)
    subscribe(thermostat,"thermostatSetpoint",thermostatHandler)
}
def thermostatHandler(evt){
    log.debug "Event Name: ${evt.name}, Event Value:  ${evt.value}" 
    
    //IFTTT Maker Channel URL Format is https://maker.ifttt.com/trigger/{event}/with/key/{key}
    def apiURL = "https://maker.ifttt.com/trigger/${makerEvent}/with/key/${apiKey}"
  	
    //IFTTT supports up to 3 variables - value1, value2, value3
    //For this app Value 1 = Thermostat State, Value 2 = Current Temp, Value 3 = Current Setpoint
  	try {
        httpPost(apiURL, 
        "value1=${thermostat.currentValue("thermostatOperatingState")}&value2=${thermostat.currentValue("temperature")}&value3=${thermostat.currentValue("thermostatSetpoint")}") { resp ->
            log.debug "response data: ${resp.data}"
            log.debug "response contentType: ${resp.contentType}"
        }
	} catch (e) {
    	log.debug "something went wrong: $e"
	}
}
  
