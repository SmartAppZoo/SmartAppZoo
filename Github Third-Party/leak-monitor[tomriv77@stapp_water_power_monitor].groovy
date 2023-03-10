/**
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
    name: "Leak Monitor",
    namespace: "tomriv77",
    author: "tomriv77",
    description: "Close a selected valve if moisture is detected, shut off power to select devices, and get notified by SMS and push notification.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot@2x.png"
)

preferences {
	page (name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", install: true, uninstall: true) {
        section("Configuration") {
            input "sensor", "capability.waterSensor", title: "Select Water Sensor(s)", required: true, multiple: true
            input "valve", "capability.valve", title: "Select Valve(s)", required: true, multiple: true
            input "offSwitches", "capability.switch", title: "Optional: Select Switch(es) to turn off", multiple: true, required: false
            input "messageText", "text", title: "Optional: Custom message (standard status message sent if not specified)", required: false
            input "frequency", "number", title: "Minimum time between messages (in minutes)", required: true, defaultValue: "10"
        }
    
        section {
            input(name: "sendSMS", title: "Push notification is always sent by default. Send SMS notification?", type: "bool", required: false, defaultValue: false, submitOnChange: true)
        }
    
        if(sendSMS != null && sendSMS) {
        	section("SMS settings") {
                input("recipients", "contact", title: "Send notifications to") {
                    input "phone", "phone", title: "Phone Number for SMS", required: "$sendSMS"
                }
            }
        }
	}
}

def installed() {
 	subscribe(sensor, "water", waterHandler)
}

def updated() {
	unsubscribe()
 	subscribe(sensor, "water", waterHandler)
}

def waterHandler(evt) {
	log.debug "Sensor $evt.displayName says ${evt.value}"
	if (evt.value == "wet") {
		valve.close()
	}
	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
			sendMessage(evt)
		}
	}
	else {
		sendMessage(evt)
	}    
}

private sendMessage(evt) {
	def msg = messageText ?: "$evt.displayName detected water, valve(s) closed."
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	log.debug "sending push"
	sendPush(msg)
    
    if (location.contactBookEnabled && recipients) {
    	log.debug "sending SMS"
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (phone) {
        	log.debug "sending SMS"
            sendSms(phone, msg)
        }
    }

	if (frequency) {
		state[evt.deviceId] = now()
	}
}