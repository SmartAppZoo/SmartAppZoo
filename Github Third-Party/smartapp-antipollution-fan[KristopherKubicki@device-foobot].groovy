/**
 *  Anti Pollution Fan
 *
 */
definition(
    name: "Anti Pollution Fan",
    namespace: "KristopherKubicki",
    author: "kristopher@acm.org",
    description: "Turns on central fans if pollution is detected",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/nest.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/nest@2x.png"
)


preferences {
	section("Pollution Sensors"){
		input "ppmeter", "capability.sensor", title: "Which", required: true, multiple: false
	}
    section("Thermostat Fans"){
		input "ttfans", "capability.thermostat", title: "Which?", required: true, multiple: true
	}
}

def installed() {
   initialized()
}

def updated() {
	unsubscribe()
    initialized()
}

def initialized() {
	subscribe(ppmeter, "polling", pHandler)
    subscribe(ppmeter, "refresh", pHandler)
    subscribe(ppmeter, "co2", pHandler)
    subscribe(ppmeter, "voc", pHandler)
    subscribe(ppmeter, "particle", pHandler)
    subscribe(ppmeter, "pollution", pHandler)
}

def pHandler(evt) {
	log.debug "$evt.name: $evt.value"

	if(ppmeter.currentValue("co2") > 1300 || ppmeter.currentValue("voc") > 300 || ppmeter.currentValue("particle") > 25 || ppmeter.currentValue("pollution") > 50) {
    	for(tfan in ttfans) { 
        	if(tfan.currentValue("thermostatFanMode") != "on") { 
    			log.debug "Turning Fan on: $plevel Watts"
    			tfan.fanOn()
            }
        }
    }
    else {
    	for(tfan in ttfans) { 
        	if(tfan.currentValue("thermostatFanMode") != "auto") { 
    			log.debug "Turning Fan off: $plevel Watts"
    			runIn(60,tfan.fanAuto())
            }
        }
    }
}
