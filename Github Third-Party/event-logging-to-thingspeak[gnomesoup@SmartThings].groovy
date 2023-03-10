/**
 *  Event Logging to ThingSpeak
 *
 *  Copyright 2015 Michael Pfammatter
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
    name: "Event Logging to ThingSpeak",
    namespace: "GnomeSoup",
    author: "Michael Pfammatter",
    description: "Log as many events as possible to ThingSpeak",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Log the following...") {
		input "temp1", "capability.temperatureMeasurement", title: "Temperature"
        input "humi1", "capability.relativeHumidityMeasurement", title: "Humidity"
        input "lux1", "capability.illuminanceMeasurement", title: "Illuminance"
	}

        section( "Enter your ThingSpeak API Key..." ) {
		input "thingSpeakApiKey", "text"
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
	subscribe( temp1, "temperature", handleTemperatureEvent );
}

def handleTemperatureEvent( evt ) {
	def httpGetUrl = "http://api.thingspeak.com/update?key=${thingSpeakApiKey}&field1=${evt.value}"
        httpGet( httpGetUrl ) { response -> 
    	if ( response.status != 200 ) {
        	log.debug( "ThingSpeak Logging Failed: status was ${response.status}" )
        } else {
        	log.debug( "ThingSpeak Logging Successful" )
        }
    }
}