/**
 *  Humidifier_Control
 *
 *  Copyright 2019 Steven Rudolph
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
    name: "Humidifier_Control",
    namespace: "RudeDog69",
    author: "Steven Rudolph",
    description: "Control Humidifier based on high low set points.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Monitor the humidity of:") {
		input "humiditySensor1", "capability.relativeHumidityMeasurement"
	}
	section("When the humidity rises above:") {
		input "humidityHigh", "number", title: "Percentage ?"
	}
    section("When the humidity falls below:") {
		input "humidityLow", "number", title: "Percentage ?"
	}
    section( "Notifications" ) {
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
    }
	section("Control this switch:") {
		input "switch1", "capability.switch", required: false
	}
}

def installed() {
	subscribe(humiditySensor1, "humidity", humidityHandler)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	subscribe(humiditySensor1, "humidity", humidityHandler)
	unsubscribe()
	initialize()
}

def humidityHandler(evt) {
	log.trace "humidity: ${evt.value}"
    log.trace "set point: ${humidity1}"

	def currentHumidity = Double.parseDouble(evt.value.replace("%", ""))
	def tooHumid = humidityHigh
    def notHumidEnough = humidityLow
	def mySwitch = settings.switch1
	def deltaMinutes = 10 
    
    def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
	def recentEvents = humiditySensor1.eventsSince(timeAgo)
	log.trace "Found ${recentEvents?.size() ?: 0} events in the last ${deltaMinutes} minutes"
	def alreadySentSms = recentEvents.count { Double.parseDouble(it.value.replace("%", "")) >= tooHumid } > 1 || recentEvents.count { Double.parseDouble(it.value.replace("%", "")) <= notHumidEnough } > 1
    
	if (currentHumidity >= tooHumid) {
		log.debug "Checking how long the humidity sensor has been reporting >= ${tooHumid}"

		// Don't send a continuous stream of text messages
		
		if (alreadySentSms) {
			log.debug "Notification already sent within the last ${deltaMinutes} minutes"
			
		} else {
			log.debug "Humidity Rose Above ${tooHumid}:  sending SMS and turning off ${mySwitch}"
			send("${humiditySensor1.label} sensed high humidity level of ${evt.value}")
			switch1?.off()
		}
	}

    if (currentHumidity <= notHumidEnough) {
		log.debug "Checking how long the humidity sensor has been reporting <= ${notHumidEnough}"

		if (alreadySentSms) {
			log.debug "Notification already sent within the last ${deltaMinutes} minutes"
			
		} else {
			log.debug "Humidity Fell Below ${notHumidEnough}:  sending SMS and turning on ${mySwitch}"
			send("${humiditySensor1.label} sensed low humidity level of ${evt.value}")
			switch1?.on()
		}
	}
}
private send(msg) {
    if ( sendPushMessage != "No" ) {
        log.debug( "sending push message" )
        sendPush( msg )
    }

    log.debug msg
}