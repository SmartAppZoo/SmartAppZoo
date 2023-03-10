/** CatGenie Monitor Version 2.1
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
 *  Sends a message and (optionally) turns on or blinks a light to indicate that laundry is done.
 *
 *  Date: 2013-02-21
 */

definition(
	name: "CatGenie Monitor",
	namespace: "jgagnon5541",
	author: "Jonathan Gagnon",
	description: "Turn on a fan while a Cat Genie is running.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-HotTubTuner.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-HotTubTuner%402x.png"
)

preferences {
	section("Select an icon"){
    	icon(title: "Icon:", required: false)}
	section("Tell me when this CatGenie is running..."){
		input "sensor1", "capability.accelerationSensor", required: true, multiple: true, title: "Which CatGenie?"
	}
	section("And by turning on this fans") {
		input "fan", "capability.switch", required: false, multiple: true, title: "Which Fan?"
	}
	section("Time thresholds (in minutes, optional)"){
		input "cycleTime", "decimal", title: "Minimum fan Runtime", required: false, defaultValue: 50
		input "startDelay", "decimal", title: "Fan Start Delay", required: false, defaultValue: 1
	}
}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
	initialize()
}

def initialize() {

	state.fanRunning = false;
    state.isRunning = false;
    state.stoppedAt = null;
    subscribe(sensor1, "acceleration.active", accelerationActiveHandler)
	subscribe(sensor1, "acceleration.inactive", accelerationInactiveHandler)
}

def accelerationActiveHandler(evt) {
	log.trace "Detected vibration from CatGenie"
	if (!state.isRunning) {
		log.info "Arming detector"
		state.isRunning = true;
		state.startedAt = now()
        
	}
	
}

def accelerationInactiveHandler(evt) {
	log.trace "no vibration, isRunning: $state.isRunning"
    
	if (state.isRunning) {
		log.debug "startedAt: ${state.startedAt}, stoppedAt: ${state.stoppedAt}"
		if (!state.stoppedAt) {
			state.stoppedAt = now()
            def delay = Math.floor(startDelay * 60).toInteger()
            
            
			runIn(delay, checkRunning, [overwrite: false])
            log.debug "RunIn Delay: ${delay}"
		}
	def cycledelay = Math.floor(cycleTime * 60).toInteger()
    log.debug "Turning off delay: ${cycledelay}"
    runIn(cycledelay, checkRunning, [overwrite: false])
    }
   
}

def checkRunning() {
	log.trace "checkRunning()"
	if (state.isRunning) {
		def startDelayMsec = startDelay ? startDelay * 60000 : 300000
		def sensorStates = sensor1.statesSince("acceleration", new Date((now() - startDelayMsec) as Long))
		
	

			def cycleTimeMsec = cycleTime ? cycleTime * 60000 : 600000
			def duration = now() - state.startedAt
			log.debug "duration: ${duration}"
            log.debug "startDelayMsec: ${startDelayMsec}"
            log.debug "FanRunning: ${state.fanRunning}"
            def msg1 = "CatGenie is on and turning fan on"
            if (duration > startDelayMsec) {
				if(!state.fanRunning) {
               		log.debug "Sending notification"
                    // sendPush msg1
					fan.on()
                    state.fanRunning = true;
                    
                }
					
				
			} else {
				log.debug "Not sending notification because the CatGenie wasn't running long enough $duration versus $startDelayMsec msec"
			}
            
            if (duration > cycleTimeMsec) {
				log.debug "Sending notification"

				def msg = "CatGenie is finished and fan is shutting off"
				log.info msg
				
                if (fan) {
					
						state.isRunning = false;
						log.info "Turning off the fan"
                        fan.off()
                        state.fanRunning = false;
                        // sendPush msg
					 
				}
			} else {
				log.debug "Not sending notification because fan wasn't running long enough $duration versus $cycleTimeMsec msec"
			}
			
		
	}
	else {
		log.debug "machine no longer running"
	}
state.stoppedAt = null
}

