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
    name: "ThermostatMonitor",
    namespace: "itnting",
    author: "itnting",
    description: "Monitor the thermostat mode and tursn on/off switches when needed.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Monitor mode of which thermostat of...") {
		input "temperatureSensor", "capability.thermostat"
	}
    section("Minimum time") {
    	input name:"minTime", type:number, title: "Minimum time", description: "Minimum time between changes", required: false, defaultValue:10	 
    }
	section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }
	section("Turn on things...") {
		input "conSwitches", "capability.switch", required: false, multiple:true
	}
}

def installed() {
	subscribe(temperatureSensor, "thermostatMode", thermostatModeHandler)
    subscribe(temperatureSensor, "temprature", tempratureHandler)
}

def updated() {
	unsubscribe()
    subscribe(temperatureSensor, "thermostatMode", thermostatModeHandler)
    subscribe(temperatureSensor, "temprature", tempratureHandler)
}

def tempratureHandler(evt) {
	def msMinTime = minTime.toInteger() * 60 * 1000
	def difTime =new Date().time - state.lastModeChange
	log.debug "${minTime} ${difTime} ${msMinTime} ${state.lastTempChange}"
	log.trace "${tempratureSensor.currentvalue("thermostatMode")}"
    // Only do anything if this has happened after the minTime
    
    if (!state.lastTempChange || ( difTime > msMinTime) ) 
    {
		log.debug "${evt.displayName} temp changed to $evt.value" 	
    	checkMode(evt)
        state.lastTempChange = new Date().time
    }
    else {
    	log.debug "${evt.displayName} temp changed but don't to do anything yet"
    }
    
}

def thermostatModeHandler(evt) {

    // Only do anything if this has happened after the minTime
    def msMinTime = minTime.toInteger() * 60 * 1000
    def difTime =new Date().time - state.lastModeChange
	log.debug "${minTime} ${difTime} ${msMinTime} ${state.lastModeChange} ${( difTime > msMinTime)}"
    if (!state.lastModeChange || ( difTime > msMinTime) ) 
    {
		log.debug "${evt.displayName} mode changed to ${evt.value}" 	
    	checkMode(evt)
        state.lastModeChange = new Date().time
    }
    else {
    	log.debug "${evt.displayName} mode changed but don't to do anything yet"
    }
    
}

private checkMode(evt) {
	if (evt.value == "heat") {
        def currSwitches = conSwitches.currentSwitch
        def offSwitches = currSwitches.findAll { it == "off" ? true : false }

        if (offSwitches.size() != 0) {
            log.debug "${evt.displayName}, ${evt.value}, activating ${conSwitches}"
            for (s in conSwitches) { s.on() }		
            send("${evt.displayName}, ${evt.value}, activating ${conSwitches}")
         }
     }

    else if (evt.value == "off") {
        def currSwitches = conSwitches.currentSwitch
        def onSwitches = currSwitches.findAll { it == "on" ? true : false  }

        if (onSwitches.size() != 0) {
            log.debug "${evt.displayName}, ${evt.value}, de-activating ${conSwitches}"
            for (s in conSwitches) { s.off() }
            send("${evt.displayName}, ${evt.value}, de-activating ${conSwitches}")
        }
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
