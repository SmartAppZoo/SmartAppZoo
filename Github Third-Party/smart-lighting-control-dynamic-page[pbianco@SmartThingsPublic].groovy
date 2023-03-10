/**
 *  Smart Lighting Control (Dynamic Page)
 *
 *  Copyright 2015 Phil Bianco
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
    name: "Smart Lighting Control (Dynamic Page)",
    namespace: "",
    author: "Phil Bianco",
    description: "App to control lights / dimmers based on lux sensor and motion detectors",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
   page(name: "pageOne", title: "Select Sensors, Switches and Dimmers", nextPage: "pageTwo", uninstall: true)
   page(name: "pageTwo", title: "Configuration Options / Settings", nextPage: "pageThree", uninstall: true)
   page(name: "pageThree", title: "Done please install", install: true, uninstall: true){
      section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
   }
}

def pageOne() {
	dynamicPage(name: "pageOne"){
	section {            
            input(name: "motionSensor", type: "capability.motionSensor",
            	title: "Motion sensor(s) used to active lights", description: null, multiple: true,
                required: true)            
            input(name: "switch1", type: "capability.switch", title: "Switches to turn on/off",
            	description: null, multiple: true, required: false)
            input(name: "dimmer1", type: "capability.switchLevel", title: "Dimmers to turn on/off",
            	description: null, multiple: true, required: false)
            input(name: "luxAnswer", type: "enum", title: "Lux Sensor (Yes/No)?",
            	options: ["No","Yes"])
            input(name: "delayAnswer", type: "enum", title: "Set off delay when motion stops (Yes/No)?",
            	options: ["No","Yes"])
        }
    }
}

def pageTwo() {
	dynamicPage(name: "pageTwo") {
    
    	if (luxAnswer == "Yes") { 
        section ("Lux Settings"){
        	input(name: "luxSensor", type: "capability.illuminanceMeasurement", title: "Lux Sensor")
            input(name: "luxLevel", type: "number", defaultValue: 500, title: "Lux Level to turn lights on",
            	range: "10..1000")
        }
        }
        if (delayAnswer == "Yes"){
        section ("Motion Settings") { 
        	input(name: "delayMinutes", type: "number", title: "Delay after motion stops", defaultValue: 1)
        }
        }
        if (dimmer1) {
        section("Dimmer Settings") {
            input(name: "dimmerLevel", type: "number", title: "Dimmer level you would like dimmer(s) to turn on", 
            	description: "Range 5 - 100 ... Default 50", defaultValue: 50, range: "5..100")
        }
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    
    subscribe(motionSensor, "motion", motionHandler)
    subscribe(luxSensor,"illuminance",luxHandler)
}

def motionHandler (evt) { 
// This handle events from the motion sensors

	def lightSensorState = state.luminance
    def lastStatus = state.lastStatus
    
    log.debug "luxAnswer = ${luxAnswer}"
    log.debug "SENSOR = $lightSensorState"
    log.debug "luxLevel=$luxLevel"
    
    if (evt.value == "active" && (lightSensorState < luxLevel || luxAnswer == "No")) {
       log.debug "There is motion"
       state.motionStopTime = now()
       if (dimmer1 != null && state.dimmerLastStatus != "on") {
       log.trace "dimmerLevel = ${dimmerLevel}"
          dimmer1.setLevel(dimmerLevel)
          state.dimmerLastStatus = "on"
          state.lastStatus = "on"
       }
       if (switch1 != null && state.swirchLastStatus != "on") {
          log.trace "light.on() ... [luminance: ${lightSensorState}]"
          switch1.on()
          state.switchLastStatus = "on"
          state.lastStatus = "on"
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

def turnOffMotionNoDelay() {
// This module executes to turn of lights with no delay
	log.debug "Turn off Motion No Delay"

	if ( dimmer1 != null ) {
       dimmer1.setLevel ( 0 )
       state.dimmerLastStatus = "off"
       state.lastStatus = "off"
    }
    if ( switch1 != null ) {
       switch1.off () 
       state.switchLastStatus = "off"
       state.lastStatus = "off"
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
            state.lastStatus = "off"
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
            state.lastStatus = "off"
		}
	}
}

def luxHandler (evt) {
     if ( luxSensor != null) { 
             state.luminance = evt.integerValue
     }
   else {
      state.luminance = 0
   }
}
