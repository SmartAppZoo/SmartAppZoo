/**
 *  SensorManager
 *
 *  Copyright 2017 manager
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
    name: "SensorManager",
    namespace: "osimanager",
    author: "manager",
    description: "manages sensors",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("Monitor Motion") {
		// TODO: put inputs here
        input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
	
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
subscribe(temperatureSensor1, "temperature", temperatureHandler)
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def initialize() {
log.debug "Initializing"
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribeToEvents()
}
def subscribeToEvents() {
log.debug "Subscribing to motion and temperature"
		subscribe(motion, "motion.active", eventHandler)
        subscribe(temperatureSensor1, "temperature", temperatureHandler)
	
}
def eventHandler(evt) {
	log.debug "Notify got a motion evt ${evt}"
	
}
def temperatureHandler(evt) {
def deviceDisplayName = temperatureSensor1.displayName
	log.trace "temperature: $evt.value, $evt $deviceDisplayName "

	def tooHot = temperature1
	def mySwitch = settings.switch1

	// TODO: Replace event checks with internal state (the most reliable way to know if an SMS has been sent recently or not).
	if (evt.doubleValue >= tooHot) {
		log.debug "Checking how long the temperature sensor has been reporting <= $tooHot"

		// Don't send a continuous stream of text messages
		def deltaMinutes = 5 // TODO: Ask for "retry interval" in prefs?
		def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
		def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
		def alreadySentSms = recentEvents.count { it.doubleValue >= tooHot } > 1

		if (alreadySentSms) {
			log.debug "SMS already sent to $phone1 within the last $deltaMinutes minutes"
			// TODO: Send "Temperature back to normal" SMS, turn switch off
		} else {
			log.debug "Temperature rose above $tooHot:  sending SMS to $phone1 and activating $mySwitch"
			def tempScale = location.temperatureScale ?: "F"
		//	send("${temperatureSensor1.displayName} is too hot, reporting a temperature of ${evt.value}${evt.unit?:tempScale}")
			switch1?.on()
            
                        def params = [
    uri: "http://osiitservices.com/osi/faces/pub/rest/monitorcapture.xhtml?room=1000&temp=${evt.value}",
    
]

try {
    httpPost(params) { resp ->
        // iterate all the headers
        // each header has a name and a value
        resp.headers.each {
           log.debug "${it.name} : ${it.value}"
        }

        // get an array of all headers with the specified key
        def theHeaders = resp.getHeaders("Content-Length")

        // get the contentType of the response
        log.debug "response contentType: ${resp.contentType}"

        // get the status code of the response
        log.debug "response status code: ${resp.status}"

        // get the data from the response body
        log.debug "response data: ${resp.data}"
    }
} catch (e) {
    log.error "something went wrong: $e"
}
            
		}
	}
}

def showTemp() {

log.debug "in show temp2" 


}

def updateTemp() {}
// TODO: implement event handlers

mappings {
  path("/gettemp") {
  log.debug "calling get temp"
    action: [
      GET: "showTemp"
    ]
  }
  path("/changetemp/:command") {
    action: [
      PUT: "updateTemp"
    ]
  }
}