/**
 *  Mode change camera
 *
 *  Version: 1.0
 *
 *  Copyright 2015 Rob Landry
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
	name:		"Mode change camera",
	namespace:	"roblandry",
	author:		"Rob Landry",
	description:	"Enable camera motion based on mode",
	category:	"Mode Magic",
	iconUrl:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches.png",
	iconX2Url:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png",
	iconX3Url:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png")

preferences {
	section("Info") {
		paragraph "Author:  Rob Landry"
		paragraph "Version: 1.0"
		paragraph "Date:    3/25/2015"
	}
	section("Devices") {
		input ("camera", "capability.imageCapture", required: true, multiple: true)
	}
	section("Preferences") {
		input (name: "detection", type: "enum", title: "Motion or PIR", required: true, multiple: false, options: ["Motion","PIR"])
		input ("awayMode", "mode", title: "Away Modes", required: true, multiple: true)
		input ("backMode", "mode", title: "Back Modes", required: true, multiple: true)
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
	subscribe(location, modeChangeHandler)
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
	if (evt.value in awayMode) {
		if (detection == "Motion") {
			camera.motionDetectionOn()
			log.info "Camera Motion Enabled"
		} else {
			camera.pirDetectionOn()
			log.info "Camera PIR Enabled"
		}
	} else if (evt.value in backMode) {
		if (detection == "Motion") {
			camera.motionDetectionOff()
			log.info "Camera Motion Disabled"
		} else {
			camera.pirDetectionOff()
			log.info "Camera PIR Disabled"
		}
	}
}