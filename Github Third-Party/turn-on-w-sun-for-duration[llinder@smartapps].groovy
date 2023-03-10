/**
 *  Turn on w/Sun for Duration
 *
 *  Author: lance@buddho.io
 *  Date: 2015-08-23
 */

definition(
    name: "Turn on w/Sun for Duration",
    namespace: "buddho",
    author: "lance@buddho.io",
    description: "Turns on selected device(s) at sun rise or sun set on selected days with specified offset for set duration.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
    section("Turn on which device?"){
        input "switchs", "capability.switch", title: "Select Light", required: true, multiple: true
    }
    section("On which Days?") {
        input "days", "enum", title:"Select Days", required: true, multiple:true, metadata: [values: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']]
    }
    section("Minutes before sunset?") {
        input "offset", "number", title: "Minutes Before Sun", multiple: false, required: false
    }
    section("For how long?") {
        input "duration", "number", title: "Number of minutes", required: false
    }
    
    section("Sun Action?") {
        input "sunAction", "enum", title:"Sunrise or Sunset", required: true, multiple:false, metadata: [values: ['Sunrise','Sunset']]
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
    subscribe(location, action(), sunsetTimeHandler)
    //schedule it to run today too
    scheduleTurnOn(location.currentValue(action()), true)
}

def sunsetTimeHandler(evt) {
    scheduleTurnOn(evt.value, false)
}

def scheduleTurnOn(sunDateString, initialize) {
	def theOffset = (offset > 0) ? offset : 0
    //get the Date value for the string
    def sunDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunDateString)
    //calculate the offset
    def sunOffset = sunDate.time - (theOffset * 60 * 1000)
    if (!initialize) {
    	// schedule turn on for tomorrow if this is not an initial schedule
        sunOffset += (24 * 60 * 60 * 1000)
    }
    sunOffset = new Date(sunOffset)
    log.debug "Scheduling for: $sunOffset ($sunAction is $sunDate)"
    //schedule this to run one time
    runOnce(sunOffset, turnOn)
}


def turnOn() {
    log.debug "Turning on"
    
    def sunDateString = location.currentValue(action())
    def dayOfWeek = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunDateString).format("EEE")
	def matchesToday = days.contains(dayOfWeek)
	if(matchesToday) {
		switchs?.on()
		def delay = duration * 60
		runIn(delay, "turnOff")
    }
}

def action() { return (sunAction == "Sunset") ? "sunsetTime" : "sunriseTime" }

def turnOff() {
	switchs?.off()
}