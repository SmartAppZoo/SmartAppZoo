/**
 *  Logstash Event Logger
 *
 *  Copyright 2016 Chris Liebman
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
    name: "LogStash Event Logger",
    namespace: "liebman",
    author: "Chris Liebman",
    description: "Log events to logstash",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    input "logger", "capability.switch", title: "LogDevice:", required: true
    section("Log these Sensors") {
        input "sensors", "capability.sensor", multiple: true, required: false
    }

    section("Log these Actuators") {
        input "actuators", "capability.actuator", multiple: true, required: false
    }

}

def installed() {
	logger "debug", "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	logger "debug", "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(location, 		"position", 				eventHandler, [filterEvents: false])
	subscribe(location, 		"sunset", 					eventHandler, [filterEvents: false])
	subscribe(location, 		"sunrise", 					eventHandler, [filterEvents: false])
    subscribe(location, 		null, 						eventHandler, [filterEvents: false])

    subscribeAll(actuators)
    subscribeAll(sensors)
}

def subscribeAll(dev) {
    subscribe(dev, "acceleration",              eventHandler, [filterEvents: false])
    subscribe(dev, "alarm",                     eventHandler, [filterEvents: false])
    subscribe(dev, "apiStatus",                 eventHandler, [filterEvents: false])
    subscribe(dev, "battery",                   eventHandler, [filterEvents: false])
    subscribe(dev, "carbonMonoxideDetector",    eventHandler, [filterEvents: false])
    subscribe(dev, "comfortDewpointExceeded",   eventHandler, [filterEvents: false])
    subscribe(dev, "comfortDewpointMax",        eventHandler, [filterEvents: false])
    subscribe(dev, "comfortHumidityMax",        eventHandler, [filterEvents: false])
    subscribe(dev, "comfortHumidtyExceeded",    eventHandler, [filterEvents: false])
    subscribe(dev, "contact",                   eventHandler, [filterEvents: false])
    subscribe(dev, "dewpoint",                  eventHandler, [filterEvents: false])
    subscribe(dev, "feelsLike",                 eventHandler, [filterEvents: false])
    subscribe(dev, "hasLeaf",                   eventHandler, [filterEvents: false])
    subscribe(dev, "humidity",                  eventHandler, [filterEvents: false])
    subscribe(dev, "illuminance",               eventHandler, [filterEvents: false])
    subscribe(dev, "indicator",                 eventHandler, [filterEvents: false])
    subscribe(dev, "lastConnection",            eventHandler, [filterEvents: false])
    subscribe(dev, "level",                     eventHandler, [filterEvents: false])
    subscribe(dev, "locationMode",              eventHandler, [filterEvents: false])
    subscribe(dev, "lockedTempMax",             eventHandler, [filterEvents: false])
    subscribe(dev, "lockedTempMin",             eventHandler, [filterEvents: false])
    subscribe(dev, "motion",                    eventHandler, [filterEvents: false])
    subscribe(dev, "nestPresence",              eventHandler, [filterEvents: false])
    subscribe(dev, "nestReportData",            eventHandler, [filterEvents: false])
    subscribe(dev, "nestThermostatMode",        eventHandler, [filterEvents: false])
    subscribe(dev, "onlineStatus",              eventHandler, [filterEvents: false])
    subscribe(dev, "percentPrecip",             eventHandler, [filterEvents: false])
    subscribe(dev, "power",                     eventHandler, [filterEvents: false])
    subscribe(dev, "presence",                  eventHandler, [filterEvents: false])
    subscribe(dev, "relaySwitch",               eventHandler, [filterEvents: false])
    subscribe(dev, "safetyHumidityMin",         eventHandler, [filterEvents: false])
    subscribe(dev, "safetyTempExceeded",        eventHandler, [filterEvents: false])
    subscribe(dev, "safetyTempMax",             eventHandler, [filterEvents: false])
    subscribe(dev, "safetyTempMin",             eventHandler, [filterEvents: false])
    subscribe(dev, "smokeDetector",             eventHandler, [filterEvents: false])
    subscribe(dev, "sunlightCorrectionActive",  eventHandler, [filterEvents: false])
    subscribe(dev, "sunlightCorrectionEnabled", eventHandler, [filterEvents: false])
    subscribe(dev, "switch",                    eventHandler, [filterEvents: false])
    subscribe(dev, "targetTemp",                eventHandler, [filterEvents: false])
    subscribe(dev, "tempLockOn",                eventHandler, [filterEvents: false])
    subscribe(dev, "temperature",               eventHandler, [filterEvents: false])
    subscribe(dev, "thermostat",                eventHandler, [filterEvents: false])
    subscribe(dev, "thermostatOperatingState",  eventHandler, [filterEvents: false])
    subscribe(dev, "timeToTarget",              eventHandler, [filterEvents: false])
    subscribe(dev, "ultravioletIndex",          eventHandler, [filterEvents: false])
    subscribe(dev, "visibility",                eventHandler, [filterEvents: false])
    subscribe(dev, "water",                     eventHandler, [filterEvents: false])
    subscribe(dev, "weather",                   eventHandler, [filterEvents: false])
    subscribe(dev, "wind",                      eventHandler, [filterEvents: false])
    subscribe(dev, "windDir",                   eventHandler, [filterEvents: false])
    subscribe(dev, "windGust",                  eventHandler, [filterEvents: false])
}

def locationEvent(evt) {
    log.debug "locationEvent()!"
    if (logger) {
	    logger.log("debug", app.name, evt.description)
	    logger.log(evt)
    }
}

def eventHandler(evt) {
    if (logger) {
        logger.log(evt)
    }
}

def logger(level, message) {
    if (logger) {
        logger.log(level, app.label, message)
    }
    switch(level) {
    	case "debug": log.debug(message); break;
    	case "info":  log.info(message);  break;
    	case "warn":  log.warn(message);  break;
    	case "error": log.error(message); break;
    	default:      log.info(message);  break;
    }
}