/**
 *  GE Smart Switch with Motion Mode Change
 *
 *  Copyright 2019 Steven Stewart
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
    name: "GE Smart Switch with Motion Operating Mode Change",
    namespace: "StevenNCSU",
    author: "Steven Stewart",
    description: "Automatically changes the operating mode of a GE Smart Switch with Motion from one of the three operating modes (Manual, Occupancy, Vacancy) to another.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("GE Smart Motion Switch") {
    	input "motion1", "capability.motionSensor", title: "GE Smart Switch with Motion", multiple: false, required: true
    }
	section("At this time every day") {
		input "time", "time", title: "Time of Day", required: true
	}
	section("Change to this operating mode") {
		input (name: "operationMode", title: "Operating Mode",
                type: "enum",
                options: [
                    "1" : "Manual (no auto-on/no auto-off)",
                    "2" : "Vacancy (no auto-on/auto-off)",
                    "3" : "Occupancy (auto-on/auto-off)"
                ],
                required: true
            )
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
	schedule(time, changeOperatingMode)
}

def changeOperatingMode() {
	log.debug "Entered changeOperatingMode method with ${operationMode}" 
	if ("1" == operationMode) {
    	log.debug("Setting Operating Mode to Manual")
        motion1.off()
		motion1.manual()
	}
    if ("2" == operationMode) {
    	log.debug("Setting Operating Mode to Vacancy")
        motion1.off()
		motion1.vacancy()
	}
    if ("3" == operationMode) {
    	log.debug("Setting Operating Mode to Occupancy")
		motion1.occupancy()
	}
}
