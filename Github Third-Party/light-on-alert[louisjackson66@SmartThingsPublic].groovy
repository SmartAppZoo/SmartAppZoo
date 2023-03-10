/**
 *  Light ON Alert
 *
 *  Copyright 2016 Louis Jackson
 *
 *  Version 1.0.1   31 Jan 2016
 *
 *	Version History
 *
 *	1.0.1   31 Jan 2016		Added version number to the bottom of the input screen
 *	1.0.0	28 Jan 2016		Added to GitHub
 *	1.0.0	27 Jan 2016		Creation
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
    name: "Light ON Alert",
    namespace: "lojack66",
    author: "Louis Jackson",
    description: "Sends a message when a Smart Light bulb is on.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png")

preferences {
	section("Select Things to Monitor:") {
		input "switches", "capability.switch", title: "Select Lights...", multiple: true, required:true }
    
    section("Via push notification and/or a SMS message") 
    {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false }
    }
    
    section ("Version 1.0.1") {}
}

def installed() {
	log.trace "(0A) ${app.label} - settings: ${settings}"
	initialize()
}

def updated() {
	log.info "(0B) ${app.label} - updated()"
	unsubscribe()
	initialize()
}

def initialize() {
	log.info "(0C) ${app.label} - initialize()"
	state.bSendMsg = true //This is used to prevent double message from being send, since the event is triggered twice.
    subscribe(switches, "switch", onHandler)
}

def onHandler(evt) {
    log.info "(0D)  ${app.label} - onHandler - state.bSendMsg = ${state.bSendMsg}"

    switches.each 
    {
    	log.trace "(0E) ${app.label} - Checking ${it.label} - Before state:${it.latestState("switch").value}"
        
		if(state.bSendMsg && (it.latestState("switch").value in ["on", "setLevel"])) 
    	{
    		log.trace "(0F)  ${app.label} - onHandler - light attribute changed to ${it.latestState("switch").value}"
			send("${app.label} detected ${it.label} is on")
		}
    }
    
    state.bSendMsg = !state.bSendMsg
    log.trace "(10) ${app.label} - onHandler - state.bSendMsg = ${state.bSendMsg}"
}

private send(msg) {
    log.info "(01) sending message ${msg}"

    if (location.contactBookEnabled) {
    	log.trace "(02) send to contact ${recipients}"
        sendNotificationToContacts(msg, recipients)
    } 
    else if (phone)
    {
        log.trace "(03) send to contact ${phone}"
        sendSms(phone, msg)
    }
}
