/**
 *  Dump Temperature In SQL Database
 *
 *  Copyright 2016 Philippe PORTES
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
    name: "Dump Temperature In SQL Database",
    namespace: "philippeportesppo",
    author: "Philippe PORTES",
    description: "Collect temperature from available sensors and store them into a NAS hosted mySQL database for future use.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: false)


preferences {
	section("Select sensors") {
		input "temp", "capability.temperatureMeasurement", title: "Temperature", required: true, multiple: true
}
	section("Configure your NAS MySQL server and credentials") {
    	input "serverURL", "text", title:"Server URL", required: true;
        input "serverPort", "text", title:"Server port", required: false
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

	log.debug "initialize"

 // execute once then wait every 10mins
 updateCurrentInformation()
 runEvery3Hours(updateCurrentInformation)
}


def updateCurrentInformation() {

temp.each { eswitch ->
		
    log.debug eswitch.currentTemperature
        

	// Example: http://server:port/add_smartthing_record.php

    def params = [
        uri: "${serverURL}:${serverPort}",
        path: "/add_smartthing_record.php",
        query: [
                "sensorID": eswitch.displayName,
                "temp": eswitch.currentTemperature,
                "batt": eswitch.currentBattery]
    ]

    log.debug params

    try {
        httpGet(params) { resp ->   
            log.debug "response data: ${resp.data}"
        }
    } 
    catch (e) {
        log.error "something went wrong: $e"
	}
  }
}
