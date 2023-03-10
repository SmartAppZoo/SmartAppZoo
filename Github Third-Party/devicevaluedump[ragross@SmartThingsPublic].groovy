/**
 *  deviceValueDump
 *
 *  Copyright 2015 Mike Maxwell
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
    name: "deviceValueDump",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "Displays device values to live logging when triggered by a switch. ",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Trigger:") {
		input(
            	name		: "switches"
                ,title		: "Export values when these switches are activated..."
                ,multiple	: true
                ,required	: false
                ,type		: "capability.switch"
            )
	}
	section ("Sensors:"){
            input(
            	name		: "luxMeters"
                ,title		: "Poll these illuminance sensors:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.illuminanceMeasurement"
            )
            input(
            	name		: "tempMeters"
                ,title		: "Poll these temperature sensors:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.temperatureMeasurement"
            )
            input(
            	name		: "batMeters"
                ,title		: "Poll these battery values:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.battery"
            )
            input(
            	name		: "humMeters"
                ,title		: "Poll these humidity sensors:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.relativeHumidityMeasurement"
            )
            input(
            	name		: "levelMeters"
                ,title		: "Poll these dimmer levels:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.switchLevel"
            )
            input(
            	name		: "powerMeters"
                ,title		: "Poll these power meters:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.powerMeter"
            )
            input(
            	name		: "contactSensors"
                ,title		: "Poll these Contact Sensors:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.contactSensor"
            )
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
	subscribe(switches, "switch.on", dump)
    subscribe(app, dump)
}

def dump(evt) {
	if (luxMeters) {
    	log.debug "^^^^^^^^^^^ illuminance ^^^^^^^^^^^"
		luxMeters.each() { m ->
    		log.info "${m.displayName} [${m.name}] lux:${m.currentValue("illuminance")}"
    	}
    }
    if (tempMeters) {
    	log.debug "^^^^^^^^^^^ temperature ^^^^^^^^^^^"
    	tempMeters.each() { m ->
    		log.info "${m.displayName} [${m.name}] temp:${m.currentValue("temperature")}"
    	}
	}
    if (batMeters) {
    	log.debug "^^^^^^^^^^^ battery ^^^^^^^^^^^"
    	batMeters.each() { m ->
    		log.info "${m.displayName} [${m.name}] charge:${m.currentValue("battery")}%"
    	}
	}
    if (humMeters) {
    	log.debug "^^^^^^^^^^^ humidity ^^^^^^^^^^^"
    	humMeters.each() { m ->
    		log.info "${m.displayName} [${m.name}] rh:${m.currentValue("humidity")}%"
    	}
	}
    if (levelMeters) {
    	log.debug "^^^^^^^^^^^ dimmer level ^^^^^^^^^^^"
    	levelMeters.each() { m ->
    		log.info "${m.displayName} [${m.name}] level:${m.currentValue("level")}%"
    	}
	}
    if (powerMeters) {
    	log.debug "^^^^^^^^^^^ power ^^^^^^^^^^^"
    	powerMeters.each() { m ->
    		log.info "${m.displayName} [${m.name}] watts:${m.currentValue("power")}"
    	}
	}
    if (contactSensors) {
    	log.debug "^^^^^^^^^^^ contact ^^^^^^^^^^^"
    	contactSensors.each() { m ->
    		log.info "${m.displayName} [${m.name}] state:${m.currentValue("contact")}"
    	}
	}
}