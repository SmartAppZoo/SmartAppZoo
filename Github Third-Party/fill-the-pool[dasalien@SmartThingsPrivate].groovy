/**
 *  Fill the Pool
 *
 *  Copyright 2018 Dieter Rothhardt
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
    name: "Fill the Pool",
    namespace: "dasalien",
    author: "Dieter Rothhardt",
    description: "Automatically fill the pool when the water level is too low.\r\nAssumes a sensor that can indicate 'dry', and a Rachio sprinkler system with one zone dedicated to filling the pool",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When no water is sensed...") {
		input "sensor", "capability.waterSensor", title: "Pool Level Sensor?", required: true, multiple: false
	}
	section("Activate Sprinkler Zone...") {
		input "valve", "capability.valve", title: "Which?", required: true, multiple: false
	}
	section("For this amount of time...") {
		input "length", "decimal", title: "Minutes", required: true
	}    
	section("Send this message (optional, sends standard status message if not specified)"){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("Via a push notification and/or an SMS message"){
		input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
		input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes","No"]
	}
	section("Minimum time between messages (optional)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}    
}

def installed() {
	log.debug "Installed"
 	state.dryCount = 0
    state.splashTimeout = 0
    state.poolRunning = 0
	subscribe(sensor, "water", waterHandler)
}

def updated() {
	log.debug "Updated"
    state.dryCount = 0
    state.splashTimeout = 0
    state.poolRunning = 0
    unsubscribe()
 	subscribe(sensor, "water", waterHandler)
}

def waterHandler(evt) {	
	log.debug "Sensor says ${evt.value}"
    
    if (state.dryCount == null) { state.dryCount = 0 }
    if (state.splashTimeout == null) { state.splashTimeout = 0 }
    if (state.poolRunning == null) { state.poolRunning = 0 }
    
	if (evt.value == "wet") {
    	//There is water, reset the count
    	log.debug "Water sensed."
        state.dryCount = 0
        state.splashTimeout = 0

        if(state.poolRunning > 0) {
        	state.poolRunning = 0
    		closeValve()	
       	}
    }
    if (evt.value == "dry") {
    	log.debug "poolRunning ${state.poolRunning}"
    	log.debug "splashTimeout ${state.splashTimeout}"
        log.debug "dryCount ${state.dryCount}"
        
    	//Don't act if pool is already filling
        if(state.poolRunning == 0) {
        	log.debug "Pool zone is not running"
        	//Avoid fals alarms due to water movement
        	if(state.splashTimeout == 0) {
            	log.debug "No Splash Timeout"
                //If more than five cycles were all dry, fill the pool
            	if(state.dryCount > 0) {
                	log.debug "dryCount > 0"
                    fillPool()
                } else {                	
                    log.debug "dryCount == 0"
                    state.splashTimeout = 1
                    state.dryCount = 1
                    state.poolRunning = 0                    
                    log.debug "Increasing Dry Count (${state.dryCount})"
            		log.debug "Waiting 60 seconds"
                    runIn(60, splashTimer)
               }
           }	    	
        }
	}

}

def fillPool() {
	log.debug "Starting Pool Zone"
    valve.open()
    state.poolRunning = 1
    runIn(60, closeValve)
    
    //Notify
	if (frequency) {
		def lastTime = state.lastTime
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
        	sendMessage(evt)
		}
	}
	else {
		sendMessage(evt)
	}                        
}

def closeValve() {
    log.debug "Stopping Pool Zone"
    state.poolRunning = 0
    valve.close()
}

def splashTimer() {
    log.debug "Splash Guard Timer Expired"
    log.debug "Dry Count (${state.dryCount})"
    state.splashTimeout = 0
    if(state.dryCount > 0) {
    	log.debug "calling fillPool"
        fillPool()
    }
}

private sendMessage(evt) {
	def msg = messageText ?: "We triggered the pool zone as water level dropped under the threshold"
	//log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	if (!phone || pushAndPhone != "No") {
		log.debug "sending push"
		sendPush(msg)
	}
	if (phone) {
		log.debug "sending SMS"
		sendSms(phone, msg)
	}
	if (frequency) {
		state.lastTime = now()
	}
}