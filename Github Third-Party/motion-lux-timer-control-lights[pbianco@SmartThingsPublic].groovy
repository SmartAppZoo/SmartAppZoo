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
 *  Control The Lights
 *
 *  Author: Phil Bianco 
 */
definition(
    name: "Motion / Lux / Timer - Control Lights",
    namespace: "",
    author: "Phil Bianco",
    description: "Turn your lights on when motion, delay off and the space is dark (Configurable Lux).",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet-luminance@2x.png"
)

preferences {

    section ("When is the motion...") {
    	input "motion1", "capability.motionSensor", title: "Where?", multiple: true
    }
     section("Delay after motions stops to turn off lights...") {
		input name: "delayMinutes", title: "Delay in Minutes?", type: "number", require: true 
	}
	section("And it's dark...") {
		input "lightSensor1", "capability.illuminanceMeasurement", title: "Where?", required: true
	}
    section("Lux Value") {
		input name: "luxValue", title: "Lux Value 0 - 1000?", type: "number", range: "0..1000", defaultValue: 500
	}
	section("Turn on a light...") {
		input "switch1", "capability.switch", multiple: true, required: false
	}
    section("Turn on a dimmer...") {
		input "dimmer1", "capability.switchLevel", multiple: true, required: false
	}
    section("Dimmer Value") {
		input name: "dimmerValue", title: "dimmer Value 10% - 100%?", type: "number", range: "10..100", defaultValue: 50
	}
}

def installed()
{
    subscribe(motion1, "motion", contactMotionHandler)
    subscribe(lightSensor1, "illuminance", lumHandler)
}

def updated()
{
    unsubscribe()
    subscribe(motion1, "motion", contactMotionHandler)
    subscribe(lightSensor1, "illuminance", lumHandler)
}

def lumHandler(evt) {
   if ( lightSensor1 != null) { 
      state.luminance = evt.integerValue
   }
   else {
      state.luminance = 0
   }
}

def contactMotionHandler(evt) {
	def lightSensorState = state.luminance
    def lastStatus = state.lastStatus
   
	log.debug "SENSOR = $lightSensorState"
    log.debug "luxValue=$luxValue"
    if (evt.value == "active" && lightSensorState < luxValue) {
       log.debug "There is motion"
       state.motionStopTime = now()
       if (dimmer1 != null && state.lastStatus != "on") {
       log.trace "dimmerValue = ${dimmerValue}"
          dimmer1.setLevel(dimmerValue)
          state.dimmerLastStatus = "on"
       }
       if (switch1 != null && state.lastStatus != "on") {
          log.trace "light.on() ... [luminance: ${lightSensorState}]"
          switch1.on()
          state.switchLastStatus = "on"
       }
	}
    else { if (evt.value == "inactive" && state.lastStatus != "off") {
       log.debug "There is no motion"
       state.motionStopTime = now()
       if (delayMinutes) {
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
        def delayTime=((delayMinutes * 60000L) - 2000)
        log.trace "elapsed = ${elapsed} delayTime= ${delayTime}"
		if (elapsed >= delayTime) {
        	log.debug "Turning off lights"
			dimmer1.setLevel(0)
			state.dimmerLastStatus = "off"
		}
	}
    if (state.motionStopTime && state.switchLastStatus != "off" && switch1 != null) {
		def elapsed = now() - state.motionStopTime
        def delayTime=((delayMinutes * 60000L) - 2000)
        log.trace "elapsed = ${elapsed} delayTime= ${delayTime}"
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