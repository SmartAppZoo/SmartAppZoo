/**
 *  Copyright 2015 SmartThings
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
 *  Keep Me Cozy II
 *
 *  Author: SmartThings
 */

definition(
    name: "Maintain heat temp",
    namespace: "kearygriffin",
    author: "kearygriffin",
    description: "Keep one room the same temp as another",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences() {
	section("Choose Room Temperature... ") {
		input "roomTemperature", "capability.temperatureMeasurement"
	}
	section("Choose Base Temperature... ") {
		input "baseTemperature", "capability.temperatureMeasurement"
	}
	section("Switch to toggle..." ) {
		input "outlet", "capability.switch"
	}
	section("Maximum temp..."){
		input "maxTemp", "decimal", title: "Set Temp"
	}    
}

def installed()
{
	log.debug "enter installed, state: $state"
	subscribeToEvents()
}

def updated()
{
	log.debug "enter updated, state: $state"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents()
{
	subscribe(roomTemperature, "temperature", temperatureHandler)
	subscribe(baseTemperature, "temperature", temperatureHandler)
    temperatureHandler()
}

def temperatureHandler(evt)
{
        if ((roomTemperature.currentTemperature < baseTemperature.currentTemperature) && roomTemperature.currentTemperature < maxTemp) {
            log.debug "Turning heater on."
            outlet.on()
        } else {
            log.debug "Turning heater off"
            outlet.off()
        }  
}