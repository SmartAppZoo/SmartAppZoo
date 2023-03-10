/**
 *  Light Timer Management
 *
 *  Copyright 2016 Philippe Gravel
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
    name: "Light Timer Management",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Open / Close Light at time",
    category: "Family",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png"
)

preferences {
	input "lights", "capability.switch", title: "Lights", required: true, multiple: true
    
	section("Time to and close the light(s)") {
    	input "openTime", "time", title: "Open at", required: true
        input "closeTime", "time", title: "Close at", required: true
	}
    
    section("Active/Inactive") {
    	input "active", "boolean", title: "Timer Active?", required: true
    }
    
	section("Send Notifications?") {
		input("recipients", "contact", title: "Send notifications to", multiple: true, required: false)
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	log.debug "$app.label Initialize"
    
    if (active) {

        def timeZone = location.timeZone
        def currentTime = timeToday(openTime, timeZone)
        def timeOpenHour = currentTime.format("H", timeZone)
        def timeOpenMinute = currentTime.format("m", timeZone)
        log.debug "Open: $timeOpenHour:$timeOpenMinute"
        schedule("0 $timeOpenMinute $timeOpenHour ? * * *", openHandler)

        currentTime = timeToday(closeTime, timeZone)
        def timeCloseHour = currentTime.format('H', timeZone)
        def timeCloseMinute = currentTime.format('m', timeZone)
        log.debug "Close: $timeCloseHour:$timeCloseMinute"
        schedule("0 $timeCloseMinute $timeCloseHour ? * * *", closeHandler)

        sendNotificationToContacts("$app.label\nOpen at $timeOpenHour:$timeOpenMinute\nClose at $timeCloseHour:$timeCloseMinute", recipients)
	}
}

def openHandler() {
	sendNotificationToContacts("$app.label Open!", recipients)
	lights.on()
}

def closeHandler() {
	sendNotificationToContacts("$app.label Close!", recipients)
	lights.off()
}