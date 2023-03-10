/**
 *  Event Logger
 *
 *  Copyright 2015 Brian Keifer
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
 * based on code from Brian Keifer - https://github.com/bkeifer/smartthings/blob/master/Logstash%20Event%20Logger/LogstashEventLogger.groovy
 */
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

definition(
    name: "Logstash Event Logger",
    namespace: "r3dey3",
    author: "Kenny Keslar",
    description: "Log SmartThings events to a Logstash server",
    category: "Convenience",
    iconUrl: "http://valinor.net/images/logstash-logo-square.png",
    iconX2Url: "http://valinor.net/images/logstash-logo-square.png",
    iconX3Url: "http://valinor.net/images/logstash-logo-square.png")


preferences {
	section("Log these Things:") {
    	input "accelerations", "capability.accelerationSensor", title:"Accelerometers", multiple: true, required: false
        input "alarms", "capability.alarm", title: "Alarms", multiple: true, required: false
        input "batteries", "capability.battery", title: "Batteries", multiple: true, required: false
        input "beacons", "capability.beacon", title: "Beacons", multiple: true, required: false
        input "codetectors", "capability.carbonMonoxideDetector", title: "Carbon  Monoxide Detectors", multiple: true, required: false
        input "colors", "capability.colorControl", title: "Color Controllers", multiple: true, required: false
        input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
        input "doorsControllers", "capability.doorControl", title: "Door Controllers", multiple: true, required: false
        input "energymeters", "capability.energyMeter", title: "Energy Meters", multiple: true, required: false
        input "indicators", "capability.indicator", title: "Indicators", multiple: true, required: false
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminance Meters", multiple: true, required: false
        input "locks", "capability.lock", title: "Locks", multiple: true, required: false
        input "motions", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
        input "musicPlayers", "capability.musicPlayer", title: "Music Players", multiple: true, required: false
        input "powerMeters", "capability.powerMeter", title: "Power Meters", multiple: true, required: false
        input "presences", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity Meters", multiple: true, required: false
        input "relaySwitches", "capability.relaySwitch", title: "Relay Switches", multiple: true, required: false
        input "sleepSensors", "capability.sleepSensor", title: "Sleep Sensors", multiple: true, required: false
        input "smokeDetectors", "capability.smokeDetector", title: "Smoke Detectors", multiple: true, required: false
        input "peds", "capability.stepSensor", title: "Pedometers", multiple: true, required: false
        input "switches", "capability.switch", title: "Switches", multiple: true, required: false
        input "levels", "capability.switchLevel", title: "Switch Levels", multiple: true, required: false
        input "temperatures", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: false
        input "thermostats", "capability.thermostat", title: "Thermostats", multiple: true, required: false
        input "valves", "capability.valve", title: "Valves", multiple: true, required: false
        input "waterdetectors", "capability.waterSensor", title: "Water Sensors", multiple: true, required: false
    }

    section ("Logstash Server") {
        input "logstash_host", "text", title: "Logstash Hostname/IP"
        input "logstash_port", "number", title: "Logstash Port"
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
	// TODO: subscribe to attributes, devices, locations, etc.
	def eventBuffer = atomicState.eventBuffer ?: []
	atomicState.eventBuffer = eventBuffer
    unschedule()
    schedule("0 * * * * ?", sendEvents)
	doSubscriptions()
}

def doSubscriptions() {
	if (accelerometers != null) {
		subscribe(accelerometers, "acceleration", genericHandler)
	}
	if (alarms != null) {
		subscribe(alarms, "alarm", genericHandler)
	}
	if (batteries != null) {
		subscribe(batteries, "battery", genericHandler)
	}
	if (beacons != null) {
		subscribe(beacons, "presence", genericHandler)
	}

	if (codetectors != null) {
		subscribe(codetectors, "carbonMonoxide", genericHandler)
	}
	if (colors != null) {
		subscribe(colors, "hue", genericHandler)
		subscribe(colors, "saturation", genericHandler)
		subscribe(colors, "color", genericHandler)
	}
	if (contacts != null) {
		subscribe(contacts, "contact", genericHandler)
	}
	if (energymeters != null) {
		subscribe(energymeters, "energy", genericHandler)
	}
	if (indicators != null) {
		subscribe(indicators, "indicator", genericHandler)
	}
	if (illuminances != null) {
		subscribe(illuminances, "illuminance", genericHandler)
	}
	if (locks != null) {
		subscribe(locks, "lock", genericHandler)
	}
	if (motions != null) {
		subscribe(motions, "motion", genericHandler)
	}
	if (musicPlayers != null) {
		subscribe(musicPlayers, "status", genericHandler)
		subscribe(musicPlayers, "level", genericHandler)
		subscribe(musicPlayers, "trackDescription", genericHandler)
		subscribe(musicPlayers, "trackData", genericHandler)
		subscribe(musicPlayers, "mute", genericHandler)
	}
	if (powerMeters != null) {
		subscribe(powerMeters, "power", genericHandler)
	}
	if (presences != null) {
		subscribe(presences, "presence", genericHandler)
	}
	if (humidities != null) {
		subscribe(humidities, "humidity", genericHandler)
	}
	if (relaySwitches != null) {
		subscribe(relaySwitches, "switch", genericHandler)
	}
	if (sleepSensors != null) {
		subscribe(sleepSensors, "sleeping", genericHandler)
	}
	if (smokeDetectors != null) {
		subscribe(smokeDetectors, "smoke", genericHandler)
	}
	if (peds != null) {
		subscribe(peds, "steps", genericHandler)
		subscribe(peds, "goal", genericHandler)
	}
	if (switches != null) {
		subscribe(switches, "switch", genericHandler)
	}
	if (levels != null) {
		subscribe(levels, "level", genericHandler)
	}
	if (temperatures != null) {
		subscribe(temperatures, "temperature", genericHandler)
	}
	if (thermostats != null) {
		subscribe(thermostats, "temperature", genericHandler)
		subscribe(thermostats, "heatingSetpoint", genericHandler)
		subscribe(thermostats, "coolingSetpoint", genericHandler)
		subscribe(thermostats, "thermostatSetpoint", genericHandler)
		subscribe(thermostats, "thermostatMode", genericHandler)
		subscribe(thermostats, "thermostatFanMode", genericHandler)
		subscribe(thermostats, "thermostatOperatingState", genericHandler)
	}
	if (valves != null) {
		subscribe(valves, "contact", genericHandler)
	}
	if (waterdetectors != null) {
		subscribe(waterdetectors, "water", genericHandler)
	}
    
    
    subscribe(location,			"location",					genericHandler)
    subscribe(location,			"mode",					genericHandler)
    subscribe(location,			"sunset",					genericHandler)
    subscribe(location,			"sunrise",					genericHandler)
}

def genericHandler(evt) {
    //def data = [:]
    def data = new HashMap() 
    
    try {
        if (evt.value == null) {
            return;
        }
    } catch (e) {
    	return
    }
    
    
   	try {
        data.isDigital = evt.isDigital()
        data.isPhysical = evt.isPhysical()
        data.isStateChange = evt.isStateChange()
        data.id = "${evt.id}"
        data.epoch = now()
        data.device = ""
        data.value = "${evt.value}"
        
        data.extra_data = "${evt.data}"
        //if (evt.description) data.description = evt.description
        data.descriptionText = evt.descriptionText
        //if (evt.device) data.device = evt.device
        data.displayName = evt.displayName
        data.deviceId = "${evt.deviceId}"
        data.hubId = "${evt.hubId}"
        //if (evt.installedSmartAppId) data.installedSmartAppId = evt.installedSmartAppId
        data.isoDate = "${evt.isoDate}"
        data.locationId = "${evt.locationId}"
        data.name = evt.name
        data.source = "${evt.source}"
        if (evt.unit != null) {
        	data.unit = evt.unit
        }
        if (data.unit != null)  {
        	data.device = "${data.displayName} (${data.name}.${data.unit})"
        } else {
        	data.device = "${data.displayName} (${data.name})"
        }
        
        
        if (location.id == evt.locationId) {
            data.locationName = "${location.name}"
            data.locationLat = location.latitude
            data.locationLong = location.longitude
            data.currentMode = "${location.currentMode}"
            data.timeZone = "${location.timeZone.getID()}"
            data.timeZoneName = "${location.timeZone.getDisplayName()}"
        }
        
	} catch (e) {
        log.debug "Trying to get the data for ${evt.name} threw an exception: $e"
    }
    
    try {
        if (evt.integerValue != null) 
    		data.integerValue = evt.integerValue
    } catch (e) {}
    try {
        if (evt.floatValue != null) 
	    	data.floatValue = evt.floatValue
    } catch (e) {}
    try {
        if (evt.xyzValue != null) 
	    	data.xyzValue = evt.xyzValue
    } catch (e) {}
    
    switch (data.value) {
        case 'on':
        case 'open':
        case 'active':
	        data.integerValue = 1
    	    break
        case 'off':
        case 'closed':
        case 'inactive':
        	data.integerValue = 0
        	break
    }
   	log.trace "genericHandler(${data})"
    
    
    if (data.name == 'thermostatOperatingState') {
        thermostats.each { dev ->
            if (dev.id == data.deviceId) {
                switch (data.value) {
                    case 'cooling':
                    	data.integerValue = dev.getCurrentValue("coolingSetPoint")
                    	break
                    case 'heating':
                    	data.integerValue = dev.getCurrentValue("heatingSetPoint")
                    	break
                    case 'idle':
                    	data.integerValue = 0
                    	break
                    default:
                        data.integerValue = -1
                    	break
                }
            }
        }
    }
    
    try {
    	def eventBuffer = atomicState.eventBuffer ?: []
        eventBuffer << data
    	atomicState.eventBuffer = eventBuffer
    } catch (e) {
        log.debug "Trying to save the data for ${evt.name} threw an exception: $e"
    }
    
}


def sendEvents() {
	def eventBuffer = atomicState.eventBuffer ?: []
	if (eventBuffer.size() >= 1) {
		// Clear eventBuffer right away to reduce the risk of missing events
        // since we've already pulled it off of atomicState 
		atomicState.eventBuffer = []
        try {
        	def data = new groovy.json.JsonOutput().toJson(eventBuffer)
            def hubAction = new physicalgraph.device.HubAction(
                method: "PUT",
                path: "/smartthings",
                body: eventBuffer,
                headers: [
                	Host: "${logstash_host}:${logstash_port}",
					'Content-Type': "application/json"
                ]
            )
            sendHubCommand(hubAction)
        } catch ( e ) {
            log.debug "Trying to sendhubcmd post the data for ${evt.name} threw an exception: $e"
        }
	} 
}
