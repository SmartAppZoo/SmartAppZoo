/**
 *  StrangerThingsApp
 *
 *  Copyright 2016 Austin Fonacier
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
    name: "StrangerThingsApp",
    namespace: "austinrfnd",
    author: "Austin Fonacier",
    description: "Personal stranger things app",
    category: "Fun & Social",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		input "motion_sensor", "capability.motionSensor", title: "Motion Sensor", required: true
        input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: false
        input "christmas_light", "capability.switch", title: "Christmas Lights", required: true
        input "dimmed_light", "capability.switchLevel", title: "Outdoor Porch Lights", required: true
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
	subscribe(motion_sensor, "motion", motionHandler)
}

// TODO: implement event handlers

def motionHandler(evt) {
    if("active" == evt.value) {
		sonos.playTrackAtVolume("https://dl.dropboxusercontent.com/u/64874/NETFLIX-%20Stranger%20Things%20-%20IntroOpening%20Credits.mp3", 60)

        // dim the lights
        dimmed_light.setLevel(15)
        //eventually play stranger things sound

        runIn(1, turnOffLightsA)
        runIn(2, turnOnLightsA)
        runIn(4, turnOffLightsB)
        runIn(5, turnOnLightsB)
        runIn(6, turnOffLightsC)
        runIn(7, turnOnLightsC)
        runIn(10, turnOffLightsD)
        runIn(11, turnOnLightsD)
        runIn(14, turnOffLightsE)
        runIn(15, turnOnLightsE)
        runIn(20, resetEverything)
     }
}
def turnOffLightsA() {
	log.debug "Lights Off"
	christmas_light.off()
}

def turnOnLightsA() {
	log.debug "Lights On"
	christmas_light.on()
}
def turnOffLightsB() {
	log.debug "Lights Off"
	christmas_light.off()
}
def turnOffLightsC() {
	log.debug "Lights Off"
	christmas_light.off()
}

def turnOnLightsC() {
	log.debug "Lights On"
	christmas_light.on()
}
def turnOffLightsD() {
	log.debug "Lights Off"
	christmas_light.off()
}

def turnOnLightsD() {
	log.debug "Lights On"
	christmas_light.on()
}

def turnOnLightsB() {
	log.debug "Lights On"
	christmas_light.on()
}
def resetEverything(){
	log.debug "Reset"
    sonos.stop()
	christmas_light.off()
 	dimmed_light.setLevel(70)
}
