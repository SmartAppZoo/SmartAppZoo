/**
 *  Water Shutoff on Leak
 *
 *  Copyright 2015 EcoNet Controls
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
    name: "Water Shutoff When Leak Detected",
    namespace: "dadlersan",
    author: "Petar Dozet",
    description: "Turns a valve or switch to the off/closed position",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Outdoor/outdoor16-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Outdoor/outdoor16-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Outdoor/outdoor16-icn@3x.png")
    

preferences {
	section("When water is sensed...") {
		input "waterSensor", "capability.waterSensor", title: "Where? (sensors)", required: false, multiple: true
		input "contactSensor", "capability.contactSensor", title: "Where? (switches)", required: false, multiple: true
	}
	section("Close a valve...") {
		input "valve", "capability.switch", title: "Which?", required: true
	}
	section("Push notification alerts?"){
        input(name: "pushNotification", type: "bool", title: "Send a Push-notification Alert", description: null, defaultValue: true)
	}
	section("SMS message alerts?"){
        input(name: "sms", type: "phone", title: "Send a Text Alert To:", description: null, required: false)
	}

}

def initialize() {
    state.defaultWaterAlertMsg = "Water Leak Alert!"
    state.alertStart = false
	subscribe(waterSensor, "water.dry", waterHandler)
	subscribe(waterSensor, "water.wet", waterHandler)
    subscribe(valve, "switch.on", valveHandler) 
    subscribe(valve, "switch.off", valveHandler) 
    subscribe(contactSensor, "contact.closed", contactHandler)
    subscribe(contactSensor, "contact.open", contactHandler)
    
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

private sendAlert(msg) {
	log.debug "pushNotification: ${pushNotification}"
	log.debug "sms: ${sms}"

    if (sms)
    {
        sendSms(sms, msg)
        log.debug "sending SMS: ${msg}"
    }
    if (pushNotification) 
    {
        sendPush(msg)
        log.debug "sending Push: ${msg}"
    }
}

def contactHandler(evt) {
	log.debug "contactHandler event created at: ${evt.date}"
	def msg = "${contactSensor.displayName}: ${state.defaultWaterAlertMsg}"
	log.debug "alertMessage: ${msg}"
	if (evt.value == "open") {
		sendAlert(msg)
		valve.off()
        state.alertStart = true
        runIn(15, checkValve)
	} 
}

def waterHandler(evt) {
	//log.debug "waterHandler event created at: ${evt.date}"
	def msg = "${waterSensor.displayName}: ${state.defaultWaterAlertMsg}"
	log.debug "alertMessage: ${msg}"
    
	if (evt.value == "wet") {
    	sendAlert(msg)
		valve.off()
        state.alertStart = true
        runIn(15, checkValve)
	} 
}

def valveHandler(evt) {

	def msg
    if (evt.value == "on") msg = "${valve.displayName}: OPEN"
    else msg = "${valve.displayName}: CLOSE"
	log.debug "alertMessage: ${msg}"
    sendAlert(msg)
    state.alertStart = false
}

def checkValve() {
	log.debug "valve.currentValue: ${valve.currentValue("switch")}"
	if (state.alertStart) {
    	def msg = "${valve.displayName}: not responding!"
    	if (valve.currentValue("switch") == "off") msg = "${valve.displayName}: CLOSE!"
        sendAlert(msg)
    }
}