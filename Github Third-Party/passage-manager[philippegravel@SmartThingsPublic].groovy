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
 *  Smart Nightlight
 *
 *  Author: SmartThings
 *
 */
definition(
    name: "Passage Manager",
    namespace: "philippegravel",
    author: "Philippe Gravel, using code from SmartThings",
    description: "Turns on lights when it's dark and motion is detected. Turns lights off when it becomes light or some time after motion ceases. And push to max level if not from the motion",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	page(name: "Passage")
}

def Passage () {
    dynamicPage(name: "Passage", title: "Passage Manager", install: true, uninstall: true) {
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
        }
        section("Control these lights..."){
            input "light", "capability.switch", required: true
        }
        section("Turning on when there's movement..."){
            input "motionSensor", "capability.motionSensor", title: "Where (sensor)?", required: true
        }
        section("And then off when it's light or there's been no movement for..."){
            input "delaySeconds", "number", title: "Seconds?", required: true
        }
        section("Percent of light when motion...") {
            input "percentLightLow", "number", title:"Light Percent?", required: true
            input "percentLightHigh", "number", title:"Default light open?", required: true, defaultValue: 100
        }
        
        section ("Time to stop") {
            input "timeStopOpen", "time", title: "Time to Stop open?", required: true
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

    def stopOpenAtTime = timeToday(timeStopOpen, location.timeZone)
    
	subscribe(motionSensor, "motion", motionHandler)
    subscribe(light, "switch", switchHandler)
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
  
    atomicState.inProcess = true
   
    if (evt.value == "active" ) {
    	log.debug "Passage Manager: Current mode: $location.mode, Current Switch: $light.currentSwitch"
        
    	if (light.currentSwitch == "off") {

			if (location.mode == "Evening") {
				log.debug "Passage Manager: turning on lights due to motion (at $percentLightHigh%)"

                atomicState.lastStatus = "on"
				light.setLevel(percentLightHigh)
                atomicState.motionStopTime = null
        	} else if (location.mode == "Night") {
            	
                def now = new Date()	
                def sunTime = getSunriseAndSunset()
                
                if ((now > sunTime.sunset) || (now < stopOpenAtTime)){
                	atomicState.lastStatus = "on"
                    light.setLevel(percentLightLow)
	                atomicState.motionStopTime = null
                }
        	}        
        }
    } else if (atomicState.lastStatus == "on") {
        log.debug "Passage Manager $evt.name: $evt.value, $evt.displayName"

        atomicState.motionStopTime = now()
        if(delaySeconds) {
            def fireTime = new Date(new Date().time + (delaySeconds * 1000))
            runOnce(fireTime, turnOffMotionAfterDelay, [overwrite: true])
        } else {
            turnOffMotionAfterDelay()
        }
    }        

    atomicState.inProcess = false
}

def turnOffMotionAfterDelay() {
	log.trace "Passage Manager: In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
    
    if (!atomicState.inProcess) {

		if (atomicState.motionStopTime && atomicState.lastStatus == "on") {
            log.debug "Turning off lights"

            light.off()
            atomicState.lastStatus = "off"
        }        
	}
}

def switchHandler(evt) {

    log.debug "Passage Manager: $evt.name: $evt.value (Last Status: $state.lastStatus)"
    
    if (evt.value == "on") {
    
        if (atomicState.lastStatus == "off") {
            log.debug "Passage Manager: Change percent to $percentLightDefault"
            light.setLevel(percentLightDefault) 
        }           
    } else if (evt.value == "off") {
        atomicState.lastStatus = "off"
    }
}
