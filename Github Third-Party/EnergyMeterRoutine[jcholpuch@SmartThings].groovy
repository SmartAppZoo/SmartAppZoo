/**
 *  Energy Meter Routine
 *
 *  Copyright 2015 Jason Holpuch
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
    name: "Energy Meter Routine",
    namespace: "jcholpuch",
    author: "Jason Holpuch",
	description: "Run an ON/OFF Routine based on energy threshold.  Used to trigger a lighting scene based on A/V equipment power state.",
	category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png")

preferences {
	page(name: "getPref")
}
	
def getPref() {    
    dynamicPage(name: "getPref", title: "Choose Meter, Threshold, and Routines", install:true, uninstall: true) {

	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
       	input(name: "threshold", type: "number", title: "Energy Meter Threshold...", required: true, description: "in either watts or kw.")
	}

    def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
        	phrases.sort()
			section("Perform the following phrase when...") {
				log.trace phrases
				input "phrase_on", "enum", title: "Power is Above Threshold", required: true, options: phrases
				input "phrase_off", "enum", title: "Power is Below Threshold", required: false, options: phrases
			}
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
	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {

	def meterValue = evt.value as double
    if (!atomicState.lastValue) {
    	atomicState.lastValue = meterValue
    }

    def lastValue = atomicState.lastValue as double
    atomicState.lastValue = meterValue

    def aboveThresholdValue = threshold as int
    if (meterValue > aboveThresholdValue) {
    	if (lastValue < aboveThresholdValue) { // only trigger routine when crossing above the threshold
	    log.debug "${meter} reported energy consumption above ${threshold}. Run Routine ${phrase_on} ."
        location.helloHome.execute(settings.phrase_on)
        } else {
//        	log.debug "not triggering routine for ${evt.description} because the threshold (${aboveThreshold}) has already been crossed"
        }
    }

    def belowThresholdValue = threshold as int
    if (meterValue < belowThresholdValue) {
    	if (lastValue > belowThresholdValue) { // only trigger routine when crossing below the threshold
		    log.debug "${meter} reported energy consumption below ${threshold}. Run Routine ${phrase_off} ."
    	    location.helloHome.execute(settings.phrase_off)
	} 
    else {
//        	log.debug "not sending triggering routine for ${evt.description} because the threshold (${belowThreshold}) has already been crossed"
        }
    }
}
