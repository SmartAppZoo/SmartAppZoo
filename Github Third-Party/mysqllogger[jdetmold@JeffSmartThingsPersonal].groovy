/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Full credit to Charles Schwer as this code is adapted from his Log to Google Sheets app
 */

definition(
		name: "MySQL Logger",
		namespace: "jdetmold",
		author: "Jeff Detmold",
		description: "log data to mysql",
		category: "My Apps",
		iconUrl: "https://s3-us-west-2.amazonaws.com/jdetmold-smarthings/MySQLLogger/img/mysql.png",
		iconX2Url: "https://s3-us-west-2.amazonaws.com/jdetmold-smarthings/MySQLLogger/img/mysql@2x.png",
		iconX3Url: "https://s3-us-west-2.amazonaws.com/jdetmold-smarthings/MySQLLogger/img/mysql@3x.png")

preferences {
    section("Contact Sensors to Log") {
		input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
        input "contactLogType", "enum", title: "Value to log", options: ["open/closed", "true/false", "1/0"], defaultValue: "open/closed", required: true, multiple: false
	}
    
    section("Motion Sensors to Log") {
        input "motions", "capability.motionSensor", title: "Motion Sensors", required: false, multiple: true
        input "motionLogType", "enum", title: "Value to log", options: ["active/inactive", "true/false", "1/0"], defaultValue: "active/inactive", required: true, multiple: false
    }
    
    section("Thermostat Settings") {
        input "heatingSetPoints", "capability.thermostat", title: "Heating Setpoints", required: false, multiple: true
        input "coolingSetPoints", "capability.thermostat", title: "Cooling Setpoints", required: false, multiple: true
        input "thermOperatingStates", "capability.thermostat", title: "Operating States", required: false, multiple: true
    }
	
    section("Locks to Log") {
    	input "locks", "capability.lock", title: "Locks", multiple: true, required: false
        input "lockLogType", "enum", title: "Value to log", options: ["locked/unlocked", "true/false", "1/0"], defaultValue: "locked/unlocked", required: true, multiple: false
	}
    
    section("Log Other Devices") {
    	input "batteries", "capability.battery", title: "Batteries", multiple: true, required: false
		input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
        input "energyMeters", "capability.energyMeter", title: "Energy Meters", required: false, multiple: true
        input "powerMeters", "capability.powerMeter", title: "Power Meters", required: false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity Sensors", required: false, multiple: true
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminance Sensors", required: false, multiple: true
        input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", required: false, multiple: true
        input "switches", "capability.switch", title: "Switches", required: false, multiple: true
        input "sensors", "capability.sensor", title: "Sensors", required: false, multiple: true
        input "sensorAttributes", "text", title: "Sensor Attributes (comma delimited)", required: false
	}

	section ("mySql Logger php location") {
		input "urlKey", "text", title: "URL key"
	}
    
    section ("Technical settings") {
        input "queueTime", "enum", title:"Time to queue events before pushing to databas (in minutes)", options: ["0"], defaultValue:"0" //options: ["0", "5", "10", "15"]
        input "resetVals", "enum", title:"Reset the state values (queue, schedule, etc)", options: ["yes", "no"], defaultValue: "no"
    }
}

def installed() {
	setOriginalState()
	initialize()
}

def updated() {
	log.debug "Updated"
	unsubscribe()
	initialize()
    if(settings.resetVals == "yes") {
    	setOriginalState()
        settings.resetVals = "no"
    }
}

def initialize() {
	log.debug "Initialized"
    subscribe(locks, "lock", handleLockEvent)
	subscribe(batteries, "battery", handleNumberEvent)
	subscribe(contacts, "contact", handleContactEvent)
	subscribe(motions, "motion", handleMotionEvent)
	subscribe(heatingSetPoints, "heatingSetpoint", handleNumberEvent)
	subscribe(coolingSetPoints, "coolingSetpoint", handleNumberEvent)
	subscribe(thermOperatingStates, "thermostatOperatingState", handleStringEvent)
	subscribe(temperatures, "temperature", handleNumberEvent)
	subscribe(energyMeters, "energy", handleNumberEvent)
	subscribe(powerMeters, "power", handleNumberEvent)
	subscribe(humidities, "humidity", handleNumberEvent)
	subscribe(illuminances, "illuminance", handleNumberEvent)
	subscribe(presenceSensors, "presence", handleStringEvent)
	subscribe(switches, "switch", handleStringEvent)
	if (sensors != null && sensorAttributes != null) {
		sensorAttributes.tokenize(',').each {
			subscribe(sensors, it, handleStringEvent)
			}
	}
}

def setOriginalState() {
	log.debug "Set original state"
	unschedule()
	atomicState.queue = [:]
    atomicState.failureCount=0
    atomicState.scheduled=false
    atomicState.lastSchedule=0
}

def handleStringEvent(evt) {
log.debug "handling string event ${evt}"
	if(settings.queueTime.toInteger() > 0) {
    	queueValue(evt) { it }
    } else {
    	sendValue(evt) { it }
    }
}

def handleNumberEvent(evt) {
	if(settings.queueTime.toInteger() > 0) {
    	queueValue(evt) { it.toString() }
    } else {
    	sendValue(evt) { it.toString() }
    }
}

def handleContactEvent(evt) {
	// default to open/close, the value of the event
    def convertClosure = { it }
    if (contactLogType == "true/false")
    	convertClosure = { it == "open" ? "true" : "false" }
    else if ( contactLogType == "1/0")
    	convertClosure = { it == "open" ? "1" : "0" }

	if(settings.queueTime.toInteger() > 0) {
		queueValue(evt, convertClosure)
    } else {
		sendValue(evt, convertClosure)
    }
}

def handleMotionEvent(evt) {
	// default to active/inactive, the value of the event
    def convertClosure = { it }
    if (motionLogType == "true/false")
    	convertClosure = { it == "active" ? "true" : "false" }
    else if (motionLogType == "1/0")
    	convertClosure = { it == "active" ? "1" : "0" }

	if(settings.queueTime.toInteger() > 0) {
    	queueValue(evt, convertClosure)
    } else {
		sendValue(evt, convertClosure)
    }
}

def handleLockEvent(evt) {
	// default to locked/unlocked, the value of the event
    def convertClosure = { it }
    if (lockLogType == "true/false") {
		convertClosure = { it == "locked" ? "true" : "false" }
    }else if (lockLogType == "1/0") {
    	convertClosure = { it == "locked" ? "1" : "0" }
	}
	if(settings.queueTime.toInteger() > 0) {
    	queueValue(evt, convertClosure)
    } else {
		sendValue(evt, convertClosure)
    }
}


private sendValue(evt, Closure convert) {
	def keyId = URLEncoder.encode(evt.displayName.trim()+ " " +evt.name)
	def value = URLEncoder.encode(convert(evt.value))
    def eventTime = URLEncoder.encode(evt.date.format( 'yyyy-M-d HH:mm:ss', location.timeZone ))
	log.debug "Logging event to mySql ${keyId} = ${value} at Time = ${eventTime}"
	def url = "${urlKey}?time=${eventTime}&${keyId}=${value}"
    log.debug "${url}"
    
	def putParams = [
		uri: url]

	httpGet(putParams) { response ->
    	log.debug(response.status)
		if (response.status != 200 ) {
			log.debug "Database logging failed, status = ${response.status}"
		}
	}
}

private queueValue(evt, Closure convert) {
	checkAndProcessQueue()
	if( evt?.value ) {
    	
    	def keyId = URLEncoder.encode(evt.displayName.trim()+ " " +evt.name)
		def value = URLEncoder.encode(convert(evt.value))
        
    	def eventTime = URLEncoder.encode(evt.date.format( 'yyyy-M-d HH:mm:ss', location.timeZone ))
//    	log.debug "Logging to queue ${keyId} = ${value} at ${eventTime}"
        
//		if( atomicState.queue == [:] ) {
//		    def eventTime = URLEncoder.encode(evt.date.format( 'yyyy-M-d HH:mm:ss', location.timeZone ))
//          addToQueue("time", eventTime)
//    	}

		log.debug "Logging to queue ${keyId} = ${value} at ${eventTime}"

		addToQueue(keyId, value, eventTime)
        
        log.debug(atomicState.queue)

    	scheduleQueue()
	}
}

/*
 * atomicState acts differently from state, so we have to get the map, put the new item and copy the map back to the atomicState
 */
private addToQueue(key, value, eventTime) {
	def queue = atomicState.queue
    def keyT = "${eventTime}&${key}"
	log.debug "adding to cueue ${keyT} ${value}"
	queue.put(keyT, value)
	atomicState.queue = queue
}

private checkAndProcessQueue() {
    if (atomicState.scheduled && ((now() - atomicState.lastSchedule) > (settings.queueTime.toInteger()*120000))) {
		// if event has been queued for twice the amount of time it should be, then we are probably stuck
        sendEvent(name: "scheduleFailure", value: now())
        unschedule()
    	processQueue()
    }
}

def scheduleQueue() {
	if(atomicState.failureCount >= 3) {
	    log.debug "Too many failures, clearing queue"
        sendEvent(name: "queueFailure", value: now())
        resetState()
    }
	
    if(!atomicState.scheduled) {
    	runIn(settings.queueTime.toInteger() * 60, processQueue)
        atomicState.scheduled=true
        atomicState.lastSchedule=now()
    } 
}


private resetState() {
    atomicState.queue = [:]
    atomicState.failureCount=0
    atomicState.scheduled=false
}

def processQueue() {
    atomicState.scheduled=false
    log.debug "Processing Queue"
    if (atomicState.queue != [:]) {
        def url = "${urlKey}"
        for ( e in atomicState.queue ) { url+="?time=${e.key}=${e.value}&" }
        url = url[0..-2]
        log.debug(url)
        try {
            def putParams = [
                uri: url]

            httpGet(putParams) { response ->
                log.debug(response.status)
                if (response.status != 200 ) {
                    log.debug "Database logging failed, status = ${response.status}"
                    atomicState.failureCount = atomicState.failureCount+1
                    scheduleQueue()
                } else {
                    log.debug "Database accepted event(s)"
                    resetState()
                }
            }
            atomicState.queue = [:]
            atomicState.failureCount=0
            atomicState.scheduled=false
        } catch(e) {
            def errorInfo = "Error sending value: ${e}"
            log.error errorInfo
        }
    }
}