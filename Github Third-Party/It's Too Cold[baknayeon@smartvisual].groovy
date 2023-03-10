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
 *  It's Too Cold
 *
 *  Author: SmartThings
 */
definition(
    name: "It's Too Cold",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Monitor the temperature and when it drops below your setting get a text and/or turn on a heater or additional appliance.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("When the temperature drops below...") {
		input "temperature1", "number", title: "Temperature?"
	}
	    section( "Notifications" ) {
		input("recipients", "contact", title: "Send notifications to") {
		    input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
		    input "phone1", "phone", title: "Send a Text Message?", required: false
		}
	    }
	section("Turn on a heater...") {
		input "switch1", "capability.switch", required: false
	}
}

def installed() {
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def updated() {
	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {

	def tooCold = temperature1
	def mySwitch = settings.switch1

	if (evt.doubleValue <= tooCold) {
		def deltaMinutes = 10 // TODO: Ask for "retry interval" in prefs?
		def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
		def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
		def alreadySentSms = recentEvents.count { it.doubleValue <= tooCold } > 1

		if (alreadySentSms) {
			log.debug "SMS already sent within the last $deltaMinutes minutes"
		} else {
			def tempScale = location.temperatureScale ?: "F"
			send("${temperatureSensor1.displayName} is too cold, reporting a temperature of ${evt.value}${evt.unit?:tempScale}")
			switch1?.on()
		}
	}
}

private send(msg) {
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") 
             sendPush(msg)

        if (phone1) {
            sendSms(phone1, msg)
        
	    }
	}
}

