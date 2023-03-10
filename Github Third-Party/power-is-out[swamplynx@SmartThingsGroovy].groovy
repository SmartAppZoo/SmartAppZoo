/**
 *  Power Is Out
 *
 *  Copyright 2014 Jesse Ziegler
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
    name: "Power Is Out",
    namespace: "swamplynx",
    author: "Chris Marganian",
    description: "Alert me of power loss.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
	section("When there is wired-power loss on...") {
			input "motion1", "capability.motionSensor", title: "Where?"
	}
	section("Via a push notification and a text message(optional)"){
    	input "pushAndPhone", "enum", title: "Send Text?", required: false, metadata: [values: ["Yes","No"]]
		input "phone1", "phone", title: "Phone Number (for Text, optional)", required: false
	}
    section("Make changes to the following when powered is restored..."){
    	input "offSwitches", "capability.switch", title: "Turn these off", required: false, multiple: true
    	input "onSwitches", "capability.switch", title: "Turn these on if after sunset", required: false, multiple: true
    }
}

def installed() {
	init()
}

def updated() {
	unsubscribe()
	init()
}

def init() {
	subscribe(motion1, "powerSource.battery", powerOff)
    subscribe(motion1, "powerSource.powered", powerOn)
}

def powerOff(evt) {
	def msg = "Power Outage!"
    
	log.debug "Sending push for power is out.."
	sendPush(msg)
    
    if ( phone1 && pushAndPhone ) {
    	log.debug "sending SMS to ${phone1}"
   		sendSms(phone1, msg)
	}
}

def powerOn(evt) {
	def msg = "Power Restored!"
    
	log.debug "Sending push for power is back on.."
	sendPush(msg)
    
    if ( phone1 && pushAndPhone ) {
    	log.debug "sending SMS to ${phone1}"
    	sendSms(phone1, msg)
	}
    
    if ( offSwitches ) {
    	log.debug "killing Hues"
    	offSwitches.off()
	}
    
    if ( onSwitches ) {
    	log.debug "restoring Hues"
        def ss = getSunriseAndSunset()
        def now = new Date()
		def dark = ss.sunset
        if ( dark.before(now) ) {
    		onSwitches.on()
        }    
	}
}