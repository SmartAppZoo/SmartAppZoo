  
/**
 *  Smart Light
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
 *  Use Cases:
 *    1. Security - Turn on lights when its dark. Turn off when there is light.
 *    2. Sensor - Turn on / off based on sensors
 *    3. SecurityOptimized - if I am home, turn on/off based on sensor. if i am away, turn on when its drak and turn off when there is light.
 *
 */
 
 /*
during sunset motion was on, however light did not turn on since motion event did not fire. Fix: created schedule job to call motion handler at set time
 */
definition(
    name: "Smart Light",
    namespace: "vvarma",
    author: "Vikash Varma",
    description: "Light and switch automation using home mode, motion, contact, and lock sensors.",
    category: "My Apps",
    iconUrl: "http://ecx.images-amazon.com/images/I/31zw0IKIkbL.jpg",
    iconX2Url: "http://ecx.images-amazon.com/images/I/31zw0IKIkbL._AA160_.jpg"
)

preferences {
	   page(name: "mainPage", title: "Control lights and switches",  uninstall: true, install: true)
}

def mainPage() {
	dynamicPage (name: "mainPage" ) {
        section("Control these lights..."){
            input "lights", "capability.switch", multiple: true
            input ("pref", "enum", title: "Preference", metadata: [values: ["Security", "Sensor" , "SecurityOptimized"]], multiple:false,submitOnChange: true)  
        }
        if ( pref == "Sensor" || pref == "SecurityOptimized" ) {
        	section ("Sensors to control lights and switches..") {
                    input "motionSensors", "capability.motionSensor", title:"Motion sensors?", multiple: true, required: false
                    input "doorSensors", "capability.contactSensor", title: "Door sensors", multiple:true, required: false
                    input "lock1", "capability.lock", title:"Which Lock?", required: false
                    input "delayMinutes", "number", title: "Turn off after (Minutes)?", description:"5"
            }
        } 
        
        
        if (pref == "SecurityOptimized" ) {
        	section ("Away modes..") {
        		input "onModes", "mode", title: "Away Modes", required: true, multiple: true
            }
        }
        
        section("Using either light sensor or the local sunrise and sunset"){
			input "lightSensor", "capability.illuminanceMeasurement", required: false
        }
        section ("Sunrise offset (optional)...") {
            input "sunriseOffsetValue", "number", title: "Offset Minutes", required: false, description:"0"
            input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
        }
        section ("Sunset offset (optional)...") {
            input "sunsetOffsetValue", "number", title: "Offset Minutes", required: false, description:"0"
            input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
        }
        

        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
    subscribe(location, "position", locationPositionChange)       
    // subsscribe(lights, "switch.on", tryme)
    // subsscribe(lights, "switch.off", tryme)

    switch (pref) {
        case "Sensor":
            subscribe(motionSensors, "motion", motionHandler)
            subscribe(doorSensors, "contact.open", sensorOpenHandler)
            subscribe(doorSensors, "contact.closed", sensorCloseHandler)
            subscribe(lock1, "lock.unlocked", sensorOpenHandler)
            subscribe(lock1, "lock.locked", sensorCloseHandler)
            break
        case "Security" :
            if (lightSensor) {
                subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
            } 
            break        
    }
    state.motionActive = false
    state.lastStatus = "ini"
    state.reason = "initialize"
    if (isDark() ) {
        trunLightsOn()  
    } else {
        trunLightsOff()
    }
    astroCheck()
    schedule ("0 0 3 * * ?" , astroCheck) //once a day at 3 AM 


}

def tryme(evt) {
log.debug "tryme $evt.name $evt.value"
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}



def modeHandler(evt) {
    state.homeMode = evt.value 
    state.reason = "home mode changed to $state.homeMode"
    unschedule
 /*   
    if (onModes && onModes.contains (state.homeMode) ) {    	
        schedule(state.riseTime,trunLightsOff);
		schedule(state.setTime,trunLightsOn);
       // sendPush("secheduling $lights off at $state.riseTime and on at $state.setTime" );
       if (enabled()) {
        	trunLightsOn()  
       } else {
       		trunLightsOff()  
       }
    } else {
        trunLightsOff()
    }
    */
}

def motionHandler(evt) {
	state.motionActive = false
    for(sensor in motionSensors) {
    	if( sensor.currentValue('motion') == "active") {
			state.motionActive = true
        }
    } 
    if (state.motionActive) {
			sensorOpenHandler(evt)
	} else {
        	sensorCloseHandler(evt) 
    }
	
}

def sensorOpenHandler(evt) {
	 if ( isDark() ) {
     	state.reason = "$evt.displayName $evt.name is $evt.value"	
    	unschedule
		trunLightsOn()
     }
}

def sensorCloseHandler(evt) {
	state.reason = "$evt.displayName $evt.name is $evt.value"
    log.info "sensorCloseHandler: scheduling trunLightsOff since $state.reason"
//    sendEvent(name:"NightLight", value:"sensorCloseHandler", descriptionText:"sensorCloseHandler scheduling trunLightsOff since $state.reason")
    runIn(delayMinutes*60, trunLightsOff)
}

def trunLightsOn() {
	if (state.lastStatus != "on") {
		log.info "trunLightsOn: turning $lights on since $state.reason"
        sendEvent(name:"Light", value:"trunLightsOn", descriptionText:"trunLightsOn: turning $lights on since $state.reason")
 		lights.on()	
        state.lastStatus = "on"
	} else {
     	log.info "trunLightsOn: not turning $lights on since $state.reason"
        sendEvent(name:"Light", value:"trunLightsOn", descriptionText:"trunLightsOn: $state.reason. Lights already on or its not dark yet")
    }
}

def trunLightsOff() {
	if (state.lastStatus != "off") {	
    	if (state.motionActive == false  ) {
			state.lastStatus = "off"
        	lights.off()
        	log.info "trunLightsOff: turning $lights off since $state.reason"
        	sendEvent(name:"NightLight", value:"trunLightsOff", descriptionText:"trunLightsOff: turning $lights off since $state.reason")	
   		} 
    } else {
        log.info "trunLightsOff: not turning $lights off since $state.reason"
        sendEvent(name:"NightLight", value:"trunLightsOff", descriptionText:"trunLightsOff: not turning $lights off since $state.reason")
		
   }
	
}

/*
* TODO: Rewrite
*/

def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	if (state.lastStatus != "off" ) {
		lights.off()
		state.lastStatus = "off"
	}
	else if (state.motionStopTime) {
		if (lastStatus != "off") {
			def elapsed = now() - state.motionStopTime
			if (elapsed >= (delayMinutes ?: 0) * 60000L) {
				turnLightsOff
			}
		}
	}
	else if (lastStatus != "on" && evt.value < 30){
		lights.on()
		state.lastStatus = "on"
	}
}


def astroCheck() {
	def s = getSunriseAndSunset(sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
	log.debug "rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime)"
    if (pref == "Security") {
    	unschedule
        state.reason = "astroCheck"
        schedule(state.riseTime,trunLightsOff);
        schedule(state.setTime,trunLightsOn);
    } else {
    	 schedule(state.setTime,motionHandler);
    }
}

private isAway() {
	if (onModes.contains (state.homeMode) ) {
    	return true
    } else {
    	return false
    }
}

private isDark() {
	def result
	if (lightSensor) {
		result = lightSensor.currentIlluminance < 30
	}
	else {
		def t = now()
		result = t < state.riseTime || t > state.setTime
	}
    
    //log.debug "isDark: $result now: rise: ${new Date(t)} | ${new Date(state.riseTime)} | set: ${new Date(state.setTime)}"
    log.debug "isDark: $result"
    
	result
}


private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}
