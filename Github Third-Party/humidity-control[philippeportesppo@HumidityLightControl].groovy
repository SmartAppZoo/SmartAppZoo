/**
 *  Humidity Control with light sensor
 *
 *  Philippe Portes based on Copyright 2015 Vikash Varma
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AScurrent IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Humidity Control",
    namespace: "philippeportesppo",
    author: "Philippe Portes",
    description: "Maintain humidity in a room by connecting humidity sensor with humidifier and dehumidifier if the light is below a given level",
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
            input "luxSensor", "capability.IlluminanceMeasurement", title: "Using lux sensor"

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
                    input "luxLow", "number", title: "When light falls below", description:"10"
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
    state.Lux = 30
    switch (opt) {
    	case "Smart humidifier" :
        	subscribe(humiditySensor, "humidity", smartHumidifier)
            subscribe(luxSensor, "illuminance", luxManager)
            break
        case "Smart dehumidifier" :
        	subscribe(humiditySensor, "humidity", smartDehumidifier)
            subscribe(luxSensor, "illuminance", luxManager)
            break
        case "To maintain humidity" :
        	subscribe(humiditySensor, "humidity", maintainHumidity)
            subscribe(luxSensor, "illuminance", luxManager)
            break
    }
}

def checkDelta(evt) {
	def delta = 0
    state.currentHumidity = Double.parseDouble(evt.value.replace("%", ""))
	//delta = state.lastHumidity - state.currentHumidity
    //if (delta < 0 ) {
    //	delta = delta * -1
    //}
    log.debug "checkDelta"
    log.trace "currentHumidity = ${state.currentHumidity} | humidityLow = ${humidityLow} | Illumination = ${state.currentIllumination} | luxLow = ${luxLow}"
    //if (delta > 1) {
    	state.lastHumidity = state.currentHumidity
    	return true
    //} else {
    //	return false
    //}
}
def luxManager(evt) {
	log.debug "luxManager"
    
	if (state.currentIllumination == null)
    	state.currentIllumination = 100.0
	state.currentIllumination = evt.doubleValue
    def luxLimit=luxLow*1.0
    

    log.trace "luxManager"

    log.trace "currentHumidity = ${state.currentHumidity} | humidityLow = ${humidityLow} | Illumination = ${state.currentIllumination} | luxLow = ${luxLimit}"

    if (state.currentHumidity <= humidityLow && state.currentIllumination <= luxLimit) {
        turnOn(humidiferSwitch)
    } else {
        turnOff(humidiferSwitch)
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
	log.debug "smartHumidifier"

	if (checkDelta(evt) ) {
    	def luxLimit=luxLow*1.0
		if (state.currentIllumination == null)
        	state.currentIllumination = 100.0
    	log.trace "currentHumidity = ${state.currentHumidity} | humidityLow = ${humidityLow} | Illumination = ${state.currentIllumination} | luxLow = ${luxLimit}"
    	if (state.currentHumidity <= humidityLow && state.currentIllumination <= luxLimit) {
			log.debug "turnOn(humidiferSwitch)"
            turnOn(humidiferSwitch)
        } else {
        	log.debug "turnOff(humidiferSwitch)"
            turnOff(humidiferSwitch)
        }
    }
}

def maintainHumidity(evt) {

    def luxLimit=luxLow*1.0
    if (state.currentIllumination == null)
    state.currentIllumination = 100.0
    log.trace "currentHumidity = ${state.currentHumidity} | humidityLow = ${humidityLow} | Illumination = ${state.currentIllumination} | luxLow = ${luxLimit}"
    
	if (checkDelta(evt) ) {	
        if (state.currentHumidity > humidityHigh) {
            // run dehumidifier: 
            turnOff(humidiferSwitch)
            turnOn(dehumidiferSwitch)
            }
        if (state.currentHumidity <= humidityLow && state.currentIllumination <= luxLimit) {
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
