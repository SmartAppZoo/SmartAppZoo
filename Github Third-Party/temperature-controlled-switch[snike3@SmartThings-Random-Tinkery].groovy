/**
 *  Copyright 2015 SmartThings
 *  Copyright 2016 Tim Polehna
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
 *  Temperature Controlled Switch
 *
 *  Author: SmartThings
 *  Author: Tim Polehna
 */
definition(
    name: "Temperature Controlled Switch",
    namespace: "polehna",
    author: "Tim Polehna",
    description: "Monitor the temperature and when it drops below or rises above your setting turn on/off a switch. Set a threshold to perform the opposite operation when the temperature rises back above or drops back below your settings. Optionally get notified when the state changes.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("When the temperature drops below...") {
		input "temperatureLow", "number", title: "Temperature?"
		input "temperatureLowThreshold", "number", title: "Rising Threshold?", defaultValue: "10"
	}
	section("When the temperature rises above...") {
		input "temperatureHigh", "number", title: "Temperature?"
		input "temperatureHighThreshold", "number", title: "Dropping Threshold?", defaultValue: "10"
	}
	section("Switches to control...") {
		input "switch1", "capability.switch", multiple: true, required: false
        input "switchNormally", "enum", title: "Normal state of switch (between temps)", multiple: false, required: true,
				options: ["On", "Off"], defaultValue: "On"
        input "switchInterval", "number", title: "Minimum Switch Interval (Minutes)", defaultValue: "30"
	}
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }
}

def installed() {
    state.tempLocation = -2
    state.lastChange = now()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
    
    // cannot do a refresh on the accuweather sensor
    // temperatureSensor1.refresh()
}

def updated() {
	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
    
    // cannot do a refresh on the accuweather sensor
    // temperatureSensor1.refresh()
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def tooCold = temperatureLow
    def rise = temperatureLowThreshold
    def tooHot = temperatureHigh
    def drop = temperatureHighThreshold
	def mySwitch = settings.switch1
    
    def nextState = state.tempLocation
    
    def currentTemp = evt.doubleValue
    def deltaMinutes = switchInterval
    
    switch (nextState)
    {
    	// this is the just installed case...
    	case -2:
        nextState = 0
        deltaMinutes = 0
        break
        
        case -1:
        if (currentTemp >= (tooCold + rise))
        {
        	nextState = 0;
        }
        break
        
    	case 0:
        if (currentTemp <= tooCold)
        {
        	nextState = -1
        }
        else (currentTemp >= tooHot)
        {
        	nextState = 1
        }
        break
        
        case 1:
        if (currentTemp <= (tooHot - drop))
        {
        	nextState = 0
        }
        break
        
        default:
        log.debug "state.tempLocation is out of range"
    }

	if (state.tempLocation != nextState)
    {
		log.debug "Checking how long since the last time we changed states"

		def currentTime = now()
    	def sinceLastChange = currentTime - state.lastChange
        
        if (sinceLastChange < (deltaMinutes * 1000 * 60).toLong())
        {
			log.debug "Msg already sent to $phone1 within the last $deltaMinutes minutes"
        }
        else 
        {
    		def setTo = (nextState == 0) ? switchNormally : ((switchNormally == "On") ? "Off" : "On")
            
            state.tempLocation = nextState
            state.lastChange = currentTime
   
			log.debug "Temperature $currentTemp: sending msg to $phone1 and turning $setTo $mySwitch"
			send("${temperatureSensor1.displayName} reporting ${evt.value}${evt.unit?:"F"}, turning $setTo switches")
            
   			if (setTo == "On")
            {
            	switch1*.on()
            }
            else
            {
            	switch1*.off()
            }
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