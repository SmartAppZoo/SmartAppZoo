/**
 *  smartEntry
 *
 *  Author: Mike Maxwell
 */
definition(
    name: "Smart Entry Lights",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "Turn somethings on when someone arrives, only if its dark out.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet@2x.png"
)

preferences {
	section("When these people arrive..."){
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
	section("Turn on these lights..."){
		input "switches", "capability.switch", multiple: true
	}
    section("Using this light sensor."){
		input "lightSensor", "capability.illuminanceMeasurement", required: true
	}
    section("When it's below this LUX level...") {
		input "lux", "enum", title: "LUX?", options:["10","50","100","200"], required: true
	}
	section("For this amount of time...") {
		input "minutes", "enum", title: "Minutes?", options:["5","10","15","20","25","30","60"], required: true
	}
}

def installed()
{
	subscribe(presence1, "presence", presenceHandler)
 }

def updated()
{
	unsubscribe()
	subscribe(presence1, "presence", presenceHandler)
}

def presenceHandler(evt)
{
	//log.debug "presenceHandler $evt.name: $evt.value"
	def current = presence1.currentValue("presence")
	log.debug current
	def presenceValue = presence1.find{it.currentPresence == "present"}
	//log.debug "presenceValue = $presenceValue"
	if (presenceValue && enabled()) {
        def s = minutes.toInteger() * 60
        //log.debug "home ds:${s}"   
		switches.on()
        runIn(s,sOff)
		
	}

}
def sOff() {
	switches.each {
    	it.off()
    }
}

private enabled() {
	def result
	result = lightSensor.currentIlluminance < lux.toInteger()
	return result
}
