/**
 *  Garage Door Checker
 *
 *  Copyright 2019 Indu Prakash
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
	name: "Garage Door Checker",
	namespace: "induprakash",
	author: "Indu Prakash",
	description: "Sends notification if the garage door is open at the scheduled time.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot@2x.png")

preferences {
	input "door", "capability.garageDoorControl", title: "Which garage door to check?", required: true
	input "executeTime", "time", title: "When to check?", required: true
}

def installed() {
	initialize()
}

def updated() {
	log.debug "updated()"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	log.debug "initialize() ${executeTime}"
	log.debug "location.getTimeZone() value is: ${location.getTimeZone()}"
	//log.debug "timeZone() for the preference time input value is: ${timeZone(executeTime)}"
	if (executeTime && door) {
		schedule(executeTime, handler)	//run daily
	}
}

def handler() {
	log.debug "handler executed at ${new Date()}"
	if (door.currentValue("door") == "open"){
		sendNotification("The ${door} is open.")
	}
}