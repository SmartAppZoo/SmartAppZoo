/**
 *  Humidity Control
 *
 *  Copyright 2015 Vikash Varma
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
    name: "Humidity Control",
    namespace: "varma",
    author: "Vikash Varma",
    description: "Maintain humidity in a room by connecting humidity sensor with humidifier and dehumidifier",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather12-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather12-icn?displaySize=2x"
)


preferences {
	page (name: "mainPage", title: "Set Preference", nextPage: "page2") {
        section("Settings") {
            input "opt", "enum", title: "I want", metadata: [values: ["Smart humidifier", "Smart dehumidifier", "To maintain humidity"]], multiple:false
            input "humiditySensor", "capability.relativeHumidityMeasurement", title: "Using humidity sensor"
            input "notify", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
            input "sms", "phone", title: "Send a Text Message?", required: false
        }
    }
	
    page (name: "page2", nextPage: "Settings",  title: "Settings", install: true, uninstall: true)	
}

def page2() {
 	dynamicPage(name: "page2") {
        section("Settings") {
       		switch (opt) {
    			case "Smart humidifier" :
        			input "humidiferSwitch", "capability.switch", title: " Turn humidifier switch on"
            		input "humidityLow", "number", title: "When humidity falls below", description:"40"
                	break
        		case "Smart dehumidifier" :
        			input "dehumidiferSwitch", "capability.switch", title: "Turn dehumidifier switch on"
            		input "humidityHigh", "number", title: "When humidity goes above", description:"50"
                	break
       	 		case "To maintain humidity" :
        			input "humidiferSwitch", "capability.switch", title: " Turn humidifier switch on"
            		input "humidityLow", "number", title: "When humidity falls below", description:"40"
                	input "dehumidiferSwitch", "capability.switch", title: "Turn dehumidifier switch on"
            		input "humidityHigh", "number", title: "When humidity goes above", description:"50"
                	break
        	}
         }
 	}
}
def installed() {
    initialize()
}

def updated() {
	unsubscribe()
    initialize()
}

def initialize() {
	state.lastHumidity = 0
    switch (opt) {
    	case "Smart humidifier" :
        	subscribe(humiditySensor, "humidity", smartHumidifier)
            break
        case "Smart dehumidifier" :
        	subscribe(humiditySensor, "humidity", smartDehumidifier)
            break
        case "To maintain humidity" :
        	subscribe(humiditySensor, "humidity", maintainHumidity)
            break
    }
}

def checkDelta(evt) {
	def delta = 0
    state.currentHumidity = Double.parseDouble(evt.value.replace("%", ""))
	delta = state.lastHumidity - state.currentHumidity
    if (delta < 0 ) {
    	delta = delta * -1
    }
    log.trace "humidity: $evt.value | delta = $delta"
    if (delta > 5) {
    	state.lastHumidity = state.currentHumidity
    	return true
    } else {
    	return false
    }
}

def smartDehumidifier(evt) {
	if (checkDelta(evt) ) {
    	if (state.currentHumidity >= humidityHigh) {
            turnOn(dehumidiferSwitch)
        } else {
            turnOff(dehumidiferSwitch)
        }
    }
}
def smartHumidifier(evt) {
	if (checkDelta(evt) ) {
    	log.trace "currentHumidity = ${state.currentHumidity} | humidityLow = $humidityLow"
        if (state.currentHumidity <= humidityLow) {
            turnOn(humidiferSwitch)
        } else {
            turnOff(humidiferSwitch)
        }
    }
}

def maintainHumidity(evt) {
	if (checkDelta(evt) ) {	
        if (state.currentHumidity > humidityHigh) {
            // run dehumidifier: 
            turnOff(humidiferSwitch)
            turnOn(dehumidiferSwitch)
        } else if (state.currentHumidity < humidityLow) {
            turnOff(dehumidiferSwitch)
            turnOn(humidiferSwitch)
        } else {
            turnOff(humidiferSwitch)
            turnOff(dehumidiferSwitch)
        }
    }
}

def turnOn(switchName) {
 if (switchName.currentSwitch != "on" ) {
 	switchName.on()
    send("Turned on $switchName since humidity is ${state.currentHumidity}")
  } 
}

def turnOff(switchName) {
 if (switchName.currentSwitch != "off" ) {
 	switchName.off()
    send("Turned off $switchName since humidity is ${state.currentHumidity}")
  }
}

private send(msg) {
    if ( notify == "Yes" ) {
        log.debug( "sending push message" )
        sendPushMessage( msg )
    }

    if ( sms ) {
        log.debug( "sending text message to $phone 1" )
        sendSms( phone1, msg )
    }
}
