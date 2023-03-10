/**
 *  DoorSensor
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
    name: "DoorSensor",
    namespace: "osimanager",
    author: "manager",
    description: "Monitors doors",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
//"access_token": "a152937c-e811-475d-acd0-692beab9c6ab"
// https://graph.api.smartthings.com/api/smartapps/installations/5e54ad14-2cff-4512-8403-f6c402ca456c
//curl -H "Authorization: Bearer a152937c-e811-475d-acd0-692beab9c6ab" "https://graph.api.smartthings.com/api/smartapps/installations/5e54ad14-2cff-4512-8403-f6c402ca456c/getbattery"


preferences {
    section("Door Sensor ") {
        // TODO: put inputs here
    }
    section("Monitor this door or window") {
        input "contact", "capability.contactSensor"
    }
    section("Monitor the temperature...") {
        input "temperatureSensor1", "capability.temperatureMeasurement"
    }
}

def installed() {
    log.debug "Door monitor Installed with settings: ${settings}"
    
    initialize()
}

def updated() {
    log.debug "Door sensor Updated with settings: ${settings}"
    unsubscribe()
    
    initialize()
}
def doorHandler(evt) {
    log.debug "doorevent : $evt.value, $evt"
    def params = [
        uri: "https://osiitservices.com/osiportal/pub/rest/monitorcapture.xhtml?room=${contact.displayName}&door=${evt.value}",
    
    ]

    try {
        httpPost(params) { resp ->
            // iterate all the headers
            // each header has a name and a value
            resp.headers.each {
                //    log.debug "${it.name} : ${it.value}"
            }

            // get an array of all headers with the specified key
            def theHeaders = resp.getHeaders("Content-Length")

            // get the contentType of the response
            //    log.debug "response contentType: ${resp.contentType}"

            // get the status code of the response
            //  log.debug "response status code: ${resp.status}"

            // get the data from the response body
            log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }

}
def temperatureHandler(evt) {
    log.debug "door sensor temperature event: $evt.value, $temperatureSensor1.displayName"

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
                uri: "http://osiitservices.com/osi/faces/pub/rest/monitorcapture.xhtml?room=${temperatureSensor1.displayName}&temp=${evt.value}",
    
            ]
            def params2 = [
      uri: "http://osiitservices.com/osi/faces/pub/rest/monitorcapture.xhtml?room=${temperatureSensor1.displayName}&battery=${contact.currentBattery}",
    
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
                httpPost(params2) { resp ->
                    // iterate all the headers
                    // each header has a name and a value
                    resp.headers.each {
                        log.debug "${it.name} : ${it.value}"
                    }

                    
                }
            } catch (e) {
                log.error "something went wrong: $e"
            }
            
        }
    }
}
def batteryHandler(evt) {
    log.debug "Battery Event value: ${evt.value}%"
    log.debug "Battery Event device: ${evt.device}"
    
     def params = [
    uri: "http://osiitservices.com/osi/faces/pub/rest/monitorcapture.xhtml?room=${motion1.displayName}&battery=${evt.value}",    
]
try {
    httpPost(params) { resp ->
        // iterate all the headers
        // each header has a name and a value
        resp.headers.each {
       //    log.debug "${it.name} : ${it.value}"
        }

        // get an array of all headers with the specified key
        def theHeaders = resp.getHeaders("Content-Length")

    
    }
} catch (e) {
    log.error "something went wrong: $e"
}

    
    
}
def initialize() {
    log.debug "Initializing doorsensor for device ${temperatureSensor1.displayName}"
     subscribe(temperatureSensor1, "temperature", temperatureHandler)
    subscribe(contact, "contact.open", doorHandler)
    subscribe(contact, "contact.closed", doorHandler)
     subscribe(contact, "contact.closed", doorHandler)
      subscribe(motion1, "battery", batteryHandler)
    // TODO: subscribe to attributes, devices, locations, etc.
}

mappings {
    path("/gettemp") {
        log.debug "calling get temp"
        action: [
            GET: "showTemp"
        ]
    }
    path("/getopen") {
        action: [
            GET: "showOpen"
        ]
    }
    path("/getbattery") {
        action: [
            GET: "showBattery"
        ]
    }
}
def showBattery() {
log.debug "trying to get last battery level ${temperatureSensor1.currentBattery}"
def battery=temperatureSensor1.currentBattery

return [data: battery]
}
def showTemp() {

    def tempState = temperatureSensor1.temperatureState
    log.debug "trying to get last temp ${temperatureSensor1.displayName}"
    // a map is serialized to JSON and returned on the response
    return [tempState]
}
def showOpen() {

    def doorState = contact.contactState
    log.debug "trying to get door state ${doorState}"
    // a map is serialized to JSON and returned on the response
    return [data: doorState]
}
// TODO: imp