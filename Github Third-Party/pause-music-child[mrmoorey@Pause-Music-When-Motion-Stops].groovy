/**
 *  Pause Music When No Motion Child
 *
 *  Copyright 2018 Alan Moore
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
    name: "Pause Music Child",
    namespace: "mrmoorey",
    author: "Alan Moore",
    description: "Pause music when no motion detected. This is the child SmartApp",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics16-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics16-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics16-icn@2x.png")


preferences {
    page(name: "mainPage", title: "Install Music Player", install: true, uninstall:true) {
		section("Select Music Players, Motion Sensors and Wait Time") {
			input "player", "capability.musicPlayer", title: "Player", required: true, multiple: true
			input "motions", "capability.motionSensor", title: "Motion Sensor", required: true, multiple: true
			input "minutes", "number", title: "Minutes", required: true
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
	log.debug "initialize"
	
	app.updateLabel(defaultLabel())
	subscribe(motions, "motion.active", activeHandler)
	subscribe(motions, "motion.inactive", inactiveHandler)
	state.pending = !("active" in motions.currentMotion)
	/* Ensure music stops after the allotted time even if the motion sensor is already in the inactive state */
	if(minutes) runIn(minutes*60, pause)
}

def activeHandler(evt) {
	log.debug "activeHandler"
	state.pending = false
}

def inactiveHandler(evt) {
	log.debug "inactiveHandler"
	state.pending = !("active" in motions.currentMotion)
	if(state.pending) {if(minutes) runIn(minutes*60, pause) else pause()}
}

def pause() {
	log.debug "pause ${player}"
	if(state.pending) { 
		player.pause()
		/* Get the current state again. If there's still no motion we can set another timer */
		/* If the motion sensor is triggered, it will override the current setting */
		state.pending = !("active" in motions.currentMotion)
		/* Ensure music stops after the allotted time even if the motion sensor is already in the inactive state */
		if(minutes) runIn(minutes*60, pause)
	}
}

// a method that will set the default label of the automation.
// It uses the selected speakers and motion detectors
def defaultLabel() {
	def playerLabel = settings.player.size() == 1 ? player[0].displayName : player[0].displayName + ", etc..."
	def motionLabel = settings.motions.size() == 1 ? motions[0].displayName : motions[0].displayName + ", etc..."

	"Pause $playerLabel after $settings.minutes mins when no motion on $motionLabel"
}