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
 *  Control The Lights based on Motion Sensor Allow for Dimmer and Switch Control
 *
 *  Author: Phil Bianco 
 */
definition(
    name: "Light - Based on Motion Switch / Dimmer",
    namespace: "",
    author: "Phil Bianco",
    description: "Switch / Dimmer Control Based on Motion",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet-luminance@2x.png"
)

preferences {

    section ("When there is motion...") {
    	input "motion1", "capability.motionSensor", title: "Where?"
    }
    section("Delay after motions stops to turn off lights...") {
        input name: "delayMinutes", title: "Delay in Minutes..", type: "number", required: false
	}
	section("Turn on a light...") {
		input "switch1", "capability.switch", multiple: true, required: false
	}
    section("Turn on a dimmer...") {
		input "dimmer1", "capability.switchLevel", multiple: true, required: false
	}
    
    section("Dimmer Value") {
	    input name: "dimmerValue", title: "dimmer Value 10% - 100%?", type: "number", defaultValue: 50
	}
}

def installed()
{
    subscribe(motion1, "motion", contactMotionHandler)
}

def updated()
{
    unsubscribe()
    subscribe(motion1, "motion", contactMotionHandler)
}

def contactMotionHandler(evt) {

    def lastStatusDimmer= state.dimmerLastStatus
    def lastStatusSwitch= state.switchLastStatus
   
    if (evt.value == "active") {
       log.debug "There is motion"
       state.motionStopTime = now()
       if (dimmer1 != null && state.dimmerLastStatus != "on") {
          log.trace "dimmerValue = ${dimmerValue}"
          log.trace "Turning Dimmer on"
          dimmer1.setLevel(dimmerValue)
          state.dimmerLastStatus = "on"
       }
       if (switch1 != null && state.switchLastStatus != "on") {
          log.trace "Turning Switch On"
          switch1.on()
          state.switchLastStatus = "on"
       }
	}
    else { if (evt.value == "inactive") {
       log.debug "There is no motion"
       state.motionStopTime = now()
       if ( delayMinutes != null ) {
          runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
       }
       else {
          turnOffMotionNoDelay ()
       }
       }
    }
}

def turnOffMotionAfterDelay () {
   
   log.debug "turnOffMotionAfterDelay has been called"
   log.trace "state.motionStopTime= ${state.motionStopTime}"
   log.trace "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
   
   if (state.motionStopTime && state.dimmerLastStatus != "off" && dimmer1 != null) {
		def elapsed = now() - state.motionStopTime
        def delayTime = ((delayMinutes * 60000L) - 2000)
        log.trace "delayTime= $delayTime"
        log.trace "elapsed = $elapsed"
		if (elapsed >= delayTime) {
        	log.debug "Turning off lights"
			dimmer1.setLevel(0)
			state.dimmerLastStatus = "off"
		}
	}
    if (state.motionStopTime && state.switchLastStatus != "off" && switch1 != null) {
		def elapsed = now() - state.motionStopTime
        def delayTime = ((delayMinutes * 60000L) - 2000)
        log.trace "elapsed = $elapsed"
        log.trace "delayTime= $delayTime"
		if (elapsed >= delayTime) {
        	log.debug "Turning off lights"
			switch1.off()
			state.switchLastStatus = "off"
		}
	}
}

def turnOffMotionNoDelay () {

	log.debug "Turn off Motion No Delay"

	if ( dimmer1 != null ) {
       dimmer1.setLevel ( 0 )
       state.dimmerLastStatus = "off"
    }
    if ( switch1 != null ) {
       switch1.off () 
       state.switchLastStatus = "off"
    }
}