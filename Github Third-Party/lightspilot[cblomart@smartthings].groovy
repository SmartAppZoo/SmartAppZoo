/**
 *  LightsPilot
 *
 *  Copyright 2017 C&eacute;dric Blomart
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "LightsPilot",
    namespace: "cblomart",
    author: "Cédric Blomart",
    description: "Pilot different switches in function of motion, doors, switches and time.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name:"sensors", title:"Sensors",  nextPage: "light", uninstall: true, install: false, hideWhenEmpty: true) {
    	section {
        	paragraph "Select motions detectors or contact sensors. None can be selected then lighting can only be controled by light and/or time."
    	}
 		section(hideWhenEmpty: true, "Turn on when motion detected:") {
       		input "themotions", "capability.motionSensor", multiple: true, required: false, title: "Motion where?", hideWhenEmpty: true
   		}
 		section(hideWhenEmpty: true, "Turn on when contact opened:") {
       		input "thecontacts", "capability.contactSensor", multiple: true, required: false, title: "Which contact?", hideWhenEmpty: true
   		}
    }
    page(name:"light", title:"Illuminance",  nextPage: "time", uninstall: true, install: false, hideWhenEmpty: true) {
    	section {
        	paragraph "Indicate if illuminance should be checked and the lux treshold. None can be selected then lighting can only be controled by sensors and/or time."
    	}
    	section(hideWhenEmpty: true, "Turn on if illuminance bellow treshold in:") {
    	    input "theilluminances", "capability.illuminanceMeasurement", multiple: true, required: false, title: "Which illuminance?", hideWhenEmpty: true
    	}
    	section(hideWhenEmpty: true, "Trun illuminance treshhold:") {
    	    input "thelux", "decimal", required: false, title: "Lux level?", hideWhenEmpty: theilluminances, range: "0..100"
    	}
    }
    page(name:"time", title:"Time", nextPage: "switch", uninstall: true, install: false) {
    	section {
        	paragraph "Indicate if a timer. None can be selected then lighting will not shut off."
    	}
    	section("Turn on for how long:") {
    	    input "theseconds", "decimal", required: false, title: "How many seconds?"
    	}
    }
    page(name:"switch", title:"Switches", uninstall: true, install: true) {
    	section("Turn on these lights") {
       		input "theswitches", "capability.switch", multiple: true, required: true
    	}
        section() {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
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
    if (themotions) {
	    subscribe(themotions, "motion.active", motionDetectedHandler)
    }
    if (thecontacts) {
	    subscribe(thecontacts, "contact.open", contactDetectedHandler)
    }
    if (theswitches) {
    	subscribe(theswitches, "switch.on", switchOnHandler)
    }
    if (theilluminances) {
    	subscribe(theilluminances, "illuminance", illuminanceHandler)
    }
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    on()
}

def contactDetectedHandler(evt) {
    log.debug "contactDetectedHandler called: $evt"
    on()
}

def switchOnHandler(evt) {
    log.debug "swhitchOnHandler called: $evt"
    if(theseconds) {
        log.debug("setting a switch off timer")
	    runIn(theseconds,off)
    }
}

def illuminanceHandler(evt) {
    log.debug "illuminanceHandler called: $evt"
    // don't switch on or of with a timer movement or contact sensor
    if (!theseconds || !thecontacts || !themotions) {
    	if (checkIlluminance()) {
        	on()
        } else {
            off()
        }
    }
}

def illuminance() {
    def lux = null
    log.debug "checking minimum illuminance"
    if (theilluminances) {
        theilluminances.each() {
       		if (lux) {
        		if (it.illuminance < lux) {
                	lux = it.currentIlluminance
       			}
            } else {
               lux = it.currentIlluminance
            }
        }
    }
    log.debug "mininmum illuminance: ${lux}"
    lux
}

def checkIlluminance() {
    if (theilluminances && thelux) {
	    def lux = illuminance()
    	if (lux) {
    		if (lux < thelux) {
               log.debug "luminance bellow treshold ( ${lux} < ${thelux} )"
               true
            } else {
               log.debug "luminance over treshold ( ${lux} >= ${thelux} )"
               false
            }
        } else {
            log.debug "luminance not found!"
            false
        }
    }
    log.debug "no luminance check needed"
    true
}

def on() {
    log.debug "switching on switches"
    if (checkIlluminance()) {
	    theswitches.each() {
    	   log.debug "turning on ${it}"
       	it.on()
    	}
    	//if (theseconds)  {
	    //	runIn(theseconds,off)
    	//}
    }
}

def off() {
    log.debug "switching off switches"
    theswitches.each() {
    	log.debug "turning off ${it}"
        it.off()
    }
}