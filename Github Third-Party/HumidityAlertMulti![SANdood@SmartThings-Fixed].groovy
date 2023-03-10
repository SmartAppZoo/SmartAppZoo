/**
 *  Its too humid!
 *
 *  Copyright 2014 Brian Critchlow
 *  Copyright 2018 Barry A. Burke (multi-sensor version)
 *
 *  Based on Its too cold code by SmartThings
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	1.8	 10/03/18 -	Added support for multiple sensors, revamped multi-message suppression logic
 */
def getVersionNum() { return "1.8" }
private def getVersionLabel() { return "Humidity Alert Multi!, version ${getVersionNum()}" }

definition(
    name: "Humidity Alert Multi!",
    namespace: "sandood",
    author: "Barry A. Burke (storageanarchy@gmail.com)",
    description: "Notify me when the humidity rises above or falls below the given threshold. It will turn on a switch when it rises above the first threshold and off when it falls below the second threshold.",
    category: "Convenience",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather9-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather9-icn?displaySize=2x"
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "${getVersionLabel()}", uninstall: true, install: true) {

        section("Monitor the humidity of:") {
            input(name: "humiditySensors", type: "capability.relativeHumidityMeasurement", multiple: true, required: true, submitOnChange: true)
            def humidityNow
            if (settings.humiditySensors?.size() > 1) {
                input "multiMode", "enum", title: "Multiple sensors mode", description: 'Tap to choose...', options:['average','any-on/all-off','all-on/any-off','all-on/all-off'], defaultValue: 'average', 
                		multiple: false, required: true, submitOnChange: true
                if (settings.multiMode == 'average') {
                    def total = 0
                    humiditySensors.each {
                        total += Double.parseDouble(it.currentHumidity.toString().replace("%", ""))
                    }
                    humidityNow = String.format('%.1f', total / humiditySensors.size())+'%'
                } else {
                    humiditySensors.each {
                        humidityNow = humidityNow ? humidityNow + ', ' + it.currentHumidity.toString().replace("%", "")+'%' : it.currentHumidity.toString().replace("%", "")+'%'
                    }
                }
            } else {
                humidityNow = humiditySensors[0].currentHumidity.toString().replace("%", "") + '%'
            }
            if (humidityNow) paragraph("Current humidity is ${humidityNow}")
        }
        section("(ON) When humidity rises above:") {
            input "humidityHigh", "number", title: "Percentage ?"
        }
        section("(OFF) When the humidity falls below:") {
            input "humidityLow", "number", title: "Percentage ?"
        }
        section( "Notifications" ) {
            input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:true, defaultValue: "No"
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
        section("Control this switch:") {
            input "theSwitch", "capability.switch", required: false
            if (settings.theSwitch) {
            	paragraph( "This switch is currently ${theSwitch.currentSwitch}" )
            }
        }
    }
}

def installed() {
    atomicState.sentSMSLow = 0
    atomicState.sentSMSHigh = 0
    initialize()
}

def updated() {
	unsubscribe()
    initialize()
}

def initialize() {
	log.info(getVersionLabel())
   	subscribe(humiditySensors, "humidity", humidityHandler)
        
    def humidityNow
    if (settings.humiditySensors?.size() > 1) {
        if (settings.multiMode == 'average') {
            def total = 0
            humiditySensors.each {
                total += Double.parseDouble(it.currentHumidity.toString().replace("%", ""))
            }
            humidityNow = String.format('%.1f', total / humiditySensors.size())+'%'
        } else {
            humiditySensors.each {
                humidityNow = humidityNow ? humidityNow + ', ' + it.currentHumidity.toString().replace("%", "")+'%' : it.currentHumidity.toString().replace("%", "")+'%'
            }
        }
    } else {
        humidityNow = humiditySensors[0].currentHumidity.toString().replace("%", "") + '%'
    }
    if (humidityNow) log.info "Current humidity is: ${humidityNow}"
    if (theSwitch) log.info "${theSwitch.displayName} is currently ${theSwitch.currentSwitch}"
    log.info "Waiting for a change in reported humidity..."
}

def humidityHandler(evt) {
	log.trace "humidity: ${evt.value}"
    log.trace "set point: ${humidityHigh}"

	def currentHumidity = Double.parseDouble(evt.stringValue.replace("%", ""))
    
	// ignore bad data from sensor
	if (currentHumidity > 100.0) {
    	if (theSwitch) {
			theSwitch.off()
			log.info "Humidity at ${evt.device.displayName} exceeds 100%: turning off ${theSwitch}"
        } else {
        	log.info "Humidity at ${evt.device.displayName} exceeds 100%, ignoring"
        }
		return
	}
    
    def multiHumidity = currentHumidity
	def humidistat = evt.device.displayName
    
    if (humiditySensors.size() > 1) {
    	switch (multiMode) {
        	case 'average': 
            	def total = 0
                humiditySensors.each {
                	total += Double.parseDouble(it.currentHumidity.toString().replace("%", ""))
                }
                multiHumidity = total / humiditySensors.size()
                humidistat = 'average'
            	break;
                
            case 'any-on/all-off':
            	// Highest reading has to be lower than humidityLow to turn it off (all-off)
                // Any value higher than humidityHigh turns it on (any-on)
            	def highest = currentHumidity
                humiditySensors.each {
                	def val = Double.parseDouble(it.currentHumidity.toString().replace("%", ""))
                	if ( val > highest ) {
                    	highest = val
                        humidistat = it.displayName
                    }
                }
                multiHumidity = highest
            	break;
                
            case 'all-on/any-off':
            	// Lowest reading has to be higher than humidityHigh to turn on the switch (all-on)
                // Any value lower than humidityLow turns it off (any-off)
            	def lowest = currentHumidity
                humiditySensors.each {
                	def val = Double.parseDouble(it.currentHumidity.toString().replace("%", ""))
                    if ( val < lowest ) {
                    	lowest = val
                        humidistat = it.displayName
                    }
                }
                multiHumidity = lowest
            	break;
                
            case 'all-on/all-off':
            	def val = Double.parseDouble(it.currentHumidity.replace("%", ""))
				if (theSwitch.currentSwitch == 'on') {
                	// we want to turn it off if ALL of the current values are less than humidityLow, so find the highest value
                	if ( val > highest ) {
                    	highest = val
                        humidistat = it.displayName
                    }
                    multiHumidity = highest
                } else {
                	// switch is OFF, turn it ON if ALL of the current values are higher than humidityHigh, so find the lowest value
                	if ( val < lowest ) {
                    	lowest = val
                    	humidistat = it.displayName
                	}
                	multiHumidity = lowest
                }
            	break;
        } 
    }
	
	def deltaMinutes = 10 
    
    def timeAgo = now() - (1000 * 60 * deltaMinutes).toLong()
	if ((theSwitch.currentSwitch == 'off') && (multiHumidity >= humidityHigh)) {
		log.debug "Checking how long the humidity sensor(s) have been reporting >= ${humidityHigh}"

		// Don't send a continuous stream of text messages
		def alreadySentSMS = (atomicState.sentSMSHigh >= timeAgo)
        
		if (alreadySentSMS) {
			log.info "ON Notification already sent within the last ${deltaMinutes} minutes"
            if (theSwitch?.currentSwitch != 'on') { log.warn "${theSwitch.displayName} should already be on, but it isn't"; theSwitch.on()}
		} else {
        	if (humidistat == 'average') {
            	log.info "Average humidity rose above ${humidityHigh}, sending SMS and activating ${theSwitch.displayName}"
                send("Average humidity rose to ${multiHumidity}, activating ${theSwitch.displayName}")
            } else {
				log.debug "Humidity at ${humidistat} rose above ${humidityHigh}: sending SMS and activating ${theSwitch.displayName}"
                send("${humidistat} sensed high humidity level of ${evt.value}, activating ${theSwitch.displayName}")
            }
			theSwitch?.on()
            atomicState.sentSMSHigh = now()
		}
	} else if ((theSwitch.currentSwitch == 'on') &&(multiHumidity <= humidityLow)) {
		log.debug "Checking how long the humidity sensor(s) have been reporting <= ${humidityLow}"

		// Don't send a continuous stream of text messages
		def alreadySentSMS = (atomicState.sentSMSLow >= timeAgo)

		if (alreadySentSMS) {
			log.debug "OFF Notification already sent within the last ${deltaMinutes} minutes"
			if (theSwitch?.currentSwitch != 'off') { log.warn "${theSwitch.displayName} should already be off, but it isn't"; theSwitch.off()}
		} else {
        	if (humidistat == 'average') {
            	log.info "Average humidity fell below ${humidityLow}, sending SMS and deactivating ${theSwitch.displayName}"
                send("Average humidity fell to ${multiHumidity}, deactivating ${theSwitch.displayName}")
            } else {
				log.info "Humidity fell below ${humidityLow}: sending SMS and deactivating ${theSwitch.displayName}"
				send("${humidistat} sensed low humidity level of ${evt.value}, deactivating ${theSwitch.displayName}")
            }
			theSwitch.off()
            atomicState.sentSMSLow = now()
		}
	}
}

private send(msg) {
	def message = app.label + ': ' + msg
    if ( sendPushMessage && (sendPushMessage == "Yes" )) {
        log.debug( "sending push message" )
        sendPush( message )
    }
    if ( phone1 ) {
        log.debug( "sending text message" )
        sendSms( phone1, message )
    }

    log.debug "msg: " + msg
}
