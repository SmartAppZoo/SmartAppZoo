/**
 *  Copyright 2017 Simon Cross
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
 *  Thermostat Scheduler
 *
 *  Author: Simon Cross
 */
definition(
    name: "Thermostat Scheduler",
    namespace: "sicross",
    author: "Simon Cross",
    description: "Enable or disable a thermostat at certain times of the day",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat", required: true
	}
    
  section("Schedule") {
    input "time_on", "time", title: "Turn on at...", required: true
    input "time_off", "time", title: "Turn off at...", required: true
  }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}

def initialize() {
	log.debug("Scheduling")
	schedule(time_on, changeModeOn)
    schedule(time_off, changeModeOff)
}

def changeModeOn() {
	log.debug("Set thermostat mode to HEAT")
    //sendPush("Thermostat On")
    thermostat.heat()
}

def changeModeOff() {
	log.debug("Set thermostat mode to OFF")
    //sendPush("Thermostat Off")
    thermostat.off()
}

