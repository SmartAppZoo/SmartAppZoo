/**
 *  Event Hub
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
    name: "Event Hub",
    namespace: "summitbri",
    author: "summitbri",
    description: "Sends iot stream to Event Hub",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "EventHubSecret"
    appSetting "EventHubURL"
}

preferences {
	section("About") {
		paragraph "AzureStream, the smartapp that sends your device states to Azure for data correlation"
	}
    
	section("Log devices...") {
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required: false, multiple: true
		input "thermostats", "capability.thermostat", title: "Thermostats", required: false, multiple: true
		input "ecobees", "device.myEcobeeDevice", title: "Ecobees", required: false, multiple: true
		input "automatic", "capability.presenceSensor", title: "Automatic Connected Device(s)", required: false, multiple: true
		input "detectors", "capability.smokeDetector", title: "Smoke/CO Detectors", required: false, multiple: true
		input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity sensors", required: false, multiple: true
		input "waters", "capability.waterSensor", title: "Water sensors", required: false, multiple: true
		input "illuminances", "capability.IlluminanceMeasurement", title: "Illuminance sensor", required: false, multiple: true
		input "locks", "capability.lock", title: "Locks", required: false, multiple: true
		input "contacts", "capability.contactSensor", title: "Sensor open/close", required: false, multiple: true
		input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
		input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
		input "presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
		input "switches", "capability.switch", title: "Switches", required: false, multiple: true
		input "dimmerSwitches", "capability.switchLevel", title: "Dimmer Switches", required: false, multiple: true
		input "batteries", "capability.battery", title: "Battery-powered devices", required: false, multiple: true
		input "powers", "capability.powerMeter", title: "Power Meters", required: false, multiple: true
		input "energys", "capability.energyMeter", title: "Energy Meters", required: false, multiple: true
        input "voltages", "capability.voltageMeasurement", title: "Voltage Measurement", required: false, multiple: true
        input "garages", "capability.garageDoorControl", title: "Garage Door Status", required: false, multiple: true
	}


	section("Sending data at which interval in minutes (default=5)?") {
		input "givenInterval", "number", title: 'Send Data Interval', required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
}

def initialize() {
	subscribe(temperatures, "temperature", handleTemperatureEvent)
	subscribe(humidities, "humidity", handleHumidityEvent)
    subscribe(waters, "water", handleWaterEvent)
	subscribe(detectors, "smoke", handleSmokeEvent)
    subscribe(detectors, "carbonMonoxide", handleCarbonMonoxideEvent)
	subscribe(illuminances, "illuminance", handleIlluminanceEvent)
	subscribe(contacts, "contact", handleContactEvent)
	subscribe(locks, "lock", handleLockEvent)
	subscribe(accelerations, "acceleration", handleAccelerationEvent)
	subscribe(motions, "motion", handleMotionEvent)
	subscribe(presence, "presence", handlePresenceEvent)
	subscribe(switches, "switch", handleSwitchEvent)
	subscribe(dimmerSwitches, "switch", handleSwitchEvent)
	subscribe(dimmerSwitches, "level", handleSetLevelEvent)
	subscribe(batteries, "battery", handleBatteryEvent)
	subscribe(powers, "power", handlePowerEvent)
//    subscribe(voltages, "voltageMeasurement", handleVoltageEvent)
	subscribe(energys, "energy", handleEnergyEvent)
	subscribe(energys, "cost", handleCostEvent)
	subscribe(thermostats, "heatingSetpoint", handleHeatingSetpointEvent)
	subscribe(thermostats, "coolingSetpoint", handleCoolingSetpointEvent)
	subscribe(thermostats, "thermostatMode", handleThermostatModeEvent)
	subscribe(thermostats, "fanMode", handleFanModeEvent)
	subscribe(thermostats, "thermostatOperatingState", handleThermostatOperatingStateEvent)
//    subscribe(garageDoorControls, "garageDoorControl", handleGarageDoorEvent)

	def queue = []
	atomicState.queue=queue
    
	if (atomicState.queue==null) {
		atomicState.queue = []
	}    
	atomicState?.poll = [ last: 0, rescheduled: now() ]

	Integer delay  = givenInterval ?: 5 // By default, schedule processQueue every 5 min.
	log.debug "initialize>scheduling processQueue every ${delay} minutes"

	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "sunrise", rescheduleIfNeeded)
	subscribe(location, "sunset", rescheduleIfNeeded)
	subscribe(location, "mode", rescheduleIfNeeded)
	subscribe(location, "sunriseTime", rescheduleIfNeeded)
	subscribe(location, "sunsetTime", rescheduleIfNeeded)
	subscribe(app, appTouch)

	rescheduleIfNeeded()   
}

def appTouch(evt) {
	rescheduleIfNeeded()
	processQueue()
	def queue = []
	atomicState.queue=queue
}


def rescheduleIfNeeded(evt) {
	if (evt) log.debug("rescheduleIfNeeded>$evt.name=$evt.value")
	Integer delay  = givenInterval ?: 5 // By default, schedule processQueue every 5 min.
	BigDecimal currentTime = now()    
	BigDecimal lastPollTime = (currentTime - (atomicState?.poll["last"]?:0))  
	if (lastPollTime != currentTime) {    
		Double lastPollTimeInMinutes = (lastPollTime/60000).toDouble().round(1)      
		log.info "rescheduleIfNeeded>last poll was  ${lastPollTimeInMinutes.toString()} minutes ago"
	}
	if (((atomicState?.poll["last"]?:0) + (delay * 60000) < currentTime) && canSchedule()) {
		log.info "rescheduleIfNeeded>scheduling processQueue in ${delay} minutes.."
		unschedule        
		schedule("0 0/${delay} * * * ?", processQueue)
	}
	// Update rescheduled state
    
	if (!evt) {
		atomicState.poll["rescheduled"] = now()    
	}        
}    

def handleTemperatureEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleHumidityEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleHeatingSetpointEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleCoolingSetpointEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleThermostatModeEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleFanModeEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleHumidifierModeEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleHumidifierLevelEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleDehumidifierModeEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleDehumidifierLevelEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleVentilatorModeEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleFanMinOnTimeEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleVentilatorMinOnTimeEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleThermostatOperatingStateEvent(evt) {
	queueValue(evt) {
		it == "idle" ? 0 : (it == 'fan only') ? 1 : (it == 'heating') ? 2 : 3
	}

}
def handleDailyStats(evt) {
	queueValue(evt) {
		it.toString()
	}

}
def handleEquipmentStatusEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleProgramNameEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleWaterEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleSmokeEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}
def handleCarbonMonoxideEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleIlluminanceEvent(evt) {
	log.debug ("handleIlluminanceEvent> $evt.name= $evt.value")
	queueValue(evt) {
		it.toString()
	}
}

def handleLockEvent(evt) {
	queueValue(evt) {
		it == "locked" ? 1 : 0
	}
}

def handleBatteryEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handleContactEvent(evt) {
	queueValue(evt) {
		it == "open" ? 1 : 0
	}
}

def handleAccelerationEvent(evt) {
	queueValue(evt) {
		it == "active" ? 1 : 0
	}
}

def handleMotionEvent(evt) {
	log.debug "motion event handling"
    queueValue(evt) {
		it == "active" ? 1 : 0
	}
}

def handlePresenceEvent(evt) {
	queueValue(evt) {
		it == "present" ? 1 : 0
	}
}

def handleSwitchEvent(evt) {
	queueValue(evt) {
		it == "on" ? 1 : 0
	}
}

def handleSetLevelEvent(evt) {
	queueValue(evt) {
		it.toString()
	}
}

def handlePowerEvent(evt) {
	if (evt.value) {
		queueValue(evt) {
			it.toString()
		}
	}
}

def handleEnergyEvent(evt) {
	if (evt.value) {
		queueValue(evt) {
			it.toString()
		}
	}
}

def handleCostEvent(evt) {
	if (evt.value) {
		queueValue(evt) {
			it.toString()
		}
	}
}

//def handleVoltageEvent(evt) {
//	if (evt.value) {
//		queueValue(evt) {
//			it.toString()
//		}
//	}
//}


private queueValue(evt, Closure convert) {
	def MAX_QUEUE_SIZE=95000
	def jsonPayload = "{sensorId:\"${evt.displayName}\",streamId:\"${evt.name}\",data:${convert(evt.value)},timestamp:${now()}}"

    def queue

	queue = atomicState.queue
	queue << jsonPayload
	atomicState.queue = queue    
	def queue_size = queue.toString().length()
	def last_item_in_queue = queue[queue.size() -1]    
	log.debug "queueValue>queue size in chars=${queue_size}, appending json payload - ${jsonPayload} to queue, last item in queue= $last_item_in_queue"
	if (queue_size >  MAX_QUEUE_SIZE) {
		processQueue()
	}
}

def processQueue() {
	Integer delay  = givenInterval ?: 5 // By default, schedule processQueue every 5 min.
	atomicState?.poll["last"] = now()

	if (((atomicState?.poll["rescheduled"]?:0) + (delay * 60000)) < now()) {
		log.info "processQueue>scheduling rescheduleIfNeeded() in ${delay} minutes.."
		schedule("0 0/${delay} * * * ?", rescheduleIfNeeded)
		// Update rescheduled state
		atomicState?.poll["rescheduled"] = now()
	}

	def queue = atomicState.queue
    
    def params = [
        uri: "${appSettings.EventHubURL}",
        body: queue,
        contentType: "application/xml; charset=utf-8",
        requestContentType: "application/atom+xml;type=entry;charset=utf-8",
        headers: ["Authorization": "${appSettings.EventHubSecret}"],
    ]
    log.debug "processQueue"
    if (queue != []) {
		log.debug "Events to be sent to event hub: ${queue}"

   try {


    	httpPostJson(params)        
        { resp -> 
            if (response.status != 200) {
				log.debug "event hub logging failed, status = ${response.status}"
			} else {
				log.debug "event hub accepted event(s)"
				// reset the queue 
				queue =[]                         
				atomicState.queue = queuelog.debug "response message ${resp}"
            }
        }   
    } catch (groovyx.net.http.ResponseParseException e) {
			// ignore response code 201
			if (e.statusCode != 201) {
				log.error "Posting error: ${e}"
			} else {
				log.debug "Event Hub accepted event(s)"
			}
			// reset the queue 
			queue =[]                         
			atomicState.queue = queue                      
            
		} catch (e) {
			def errorInfo = "Error sending value: ${e}"
			log.error errorInfo
			// reset the queue 
			queue =[]                         
			atomicState.queue = queue                        
		}
	}
}
