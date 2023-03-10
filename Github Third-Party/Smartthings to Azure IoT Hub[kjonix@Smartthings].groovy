/**
 *  Azure Event Hub
 *
 *  Copyright 2020 Pål-André Kjøniksen
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
    name: "Smartthings to Azure IoT Hub",
    namespace: "KjoniX",
    author: "Pål-André Kjøniksen", 
    description: "Smartthings Azure IoT Hub Integration",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://user-images.githubusercontent.com/9644737/79095740-7e23e900-7d5b-11ea-9624-c78be27b2292.png",
    iconX3Url: "https://user-images.githubusercontent.com/9644737/79095785-9dbb1180-7d5b-11ea-9e93-85d9e4aba2b2.png") {
     appSetting "AzureIoTHubUri"
    appSetting "SAStoken"
   
}


preferences {
    section("Power Meter") {
        input "powers", "capability.powerMeter", title: "Power Sensor", multiple: true, required: false
    }
    section("Environment") {
        input "temperatures", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: false
    }
    section("Security Sensors") {
        input "motions", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
        }
    section("Switches") {
        input "switches", "capability.switch", title: "Switches", multiple: true, required: false
    }
     section("Acceleration Sensors") {
        input "acceleration sensors", "capability.accelerationSensor", title: "Acceleration Sensors", multiple: true, required: false
    }
    section("Contact Sensors") {
        input "contact sensors", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
    }
    section("Buttons") {
        input "buttons", "capability.button", title: "Buttons", multiple: true, required: false
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
    subscribe(powers, "power", powerHandler)
    subscribe(temperatures, "temperature", temperatureHandler)
    subscribe(motions, "motion", motionHandler)
    subscribe(contacts, "contact", contactHandler)
    subscribe(switches, "switch", switchHandler)
}

def sendEvent(sensorId, sensorName, sensorType, value) {
    log.debug "sending ${sensorId} at ${value}"
    def cleanedSensorId = sensorId.replace(" ", "")
    def params = [
        uri: "${appSettings.AzureIoTHubUri}",
        body: "{ sensorId : \"${cleanedSensorId}\", sensorName : \"${sensorName}\", sensorType : \"${sensorType}\", value : \"${value}\" }",
      
        headers: 
        ["Authorization": "${appSettings.SAStoken}",
        "Content-Type": "application/json",
        "Content-Encoding": "utf-8"]
    ]

	try {
        httpPost(params) { resp -> 
            log.debug "response message ${resp}"
        }
    } catch (e) {
        //For some reason SmartThings treats 200 as an error response, so we need to comment this out to avoid errors. Uncomment the line below to debug errors 
        log.error "something went wrong: $e"
    }
}

def powerHandler(evt) {
    sendEvent('powerMeter', evt.displayName, 'power', evt.value)
}

def temperatureHandler(evt) {    
    sendEvent(evt.displayName + 'temp', evt.displayName, 'temperature', evt.value)
}

def motionHandler(evt) {
    if (evt.value == 'active') {
        sendEvent(evt.displayName + 'motion', evt.displayName, 'motion', 'motion detected')
    }
    if (evt.value == 'inactive') {
        sendEvent(evt.displayName + 'motion', evt.displayName, 'motion', 'no motion detected')
    }
}

def contactHandler(evt) {
    if (evt.value == 'open') {
        sendEvent(evt.displayName + 'contact', evt.displayName, 'doorOpen', 'open')
    }
    if (evt.value == 'closed') {
        sendEvent(evt.displayName + 'contact', evt.displayName, 'doorOpen', 'closed')
    }
}

def switchHandler(evt) {
    if (evt.value == "on") {
        sendEvent(evt.displayName + 'switch', evt.displayName, 'switch', 'on')
    } else if (evt.value == "off") {
        sendEvent(evt.displayName + 'switch', evt.displayName, 'switch', 'off')
    }
}
