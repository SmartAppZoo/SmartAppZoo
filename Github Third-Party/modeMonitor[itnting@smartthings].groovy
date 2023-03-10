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
 *
 *  Thermostat Monitor
 *  Author: doIHaveTo
 */
definition(
    name: "modeMonitor",
    namespace: "doIHaveTo",
    author: "doIHaveTo",
    description: "Monitor the mode and makes temprature setPointChanges. Uses hoseTemps device handler for inputs ",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Monitor...") {
		input "thermostats", "capability.thermostat", required: true, multiple:true
	}
	
	section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
	}
}

def installed() {
    subscribe(location, "mode", modeChangeHandler)
}

def updated() {
    unsubscribe()
    subscribe(location, "mode", modeChangeHandler)
}


def modeChangeHandler(evt) {
	def locMode = evt.value
	log.debug "Mode has changed $locMode"
    for (t in thermostats) {
    	log.debug "Set ${t} locModeTemp"
        t.setToLocModeTemp()
        send("Location mode has changed, setting ${t} to ${locMode} temp")
    }
}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phone1) {
            log.debug("sending text message")
            sendSms(phone1, msg)
        }
    }

    log.debug msg
}
