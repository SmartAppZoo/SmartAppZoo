/**
 *  Report To Firebase
 *
 *  Copyright 2017 Duy Nguyen
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
    name: "Report Sensor Values to Firebase",
    namespace: "com.aduyng",
    author: "Duy Nguyen",
    description: "Report the status of my devices to firebase",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"){
    appSetting "firebaseUrl"
}


preferences {
	section("Sensors") {
		input("energyMeter", "capability.energyMeter", title: "Which energy meter(s)?", required: true)
	}
}

def installed() {
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	def powerValue = energyMeter.currentValue("power")
    log.debug "current power value: $powerValue"
    reportPowerValueToFirebase(powerValue);
	subscribe(energyMeter, "power", energyMeterPowerHandler)
}

def energyMeterPowerHandler(event){
	log.debug "current value: $event.value"
    reportPowerValueToFirebase(event.value)
}

def reportPowerValueToFirebase(powerValue){
	def path = new Date(now()).format("yyyy/MM/dd/HH/mm", location.timeZone)

    def url = "$appSettings.firebaseUrl/db/power/$path"
	log.debug "url: $url, power: $powerValue"
    
    try {
        httpPutJson(url, [power: powerValue]) { resp ->
            resp.headers.each {
               log.debug "${it.name} : ${it.value}"
            }
            log.debug "response contentType: ${resp.contentType}"
            log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}