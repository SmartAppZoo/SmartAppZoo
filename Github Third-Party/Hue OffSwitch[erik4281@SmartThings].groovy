/**
 *  Hue OffSwitch
 *
 *  Copyright 2015 Erik Vennink
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

/************
 * Metadata *
 ************/

definition(
    name: "Hue OffSwitch",
    namespace: "evennink",
    author: "Erik Vennink",
    description: "Switch off Hue lights based on an (virtual) switch.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

/**********
 * Setup  *
 **********/

preferences {
    page(name: "lightSelectPage", title: "Turn off these lights:", install: true, params: [sceneId:sceneId], uninstall: true) 
}

def lightSelectPage() {
	dynamicPage(name: "lightSelectPage") {
        section("Use this (virtual) switch"){
            input "inputSwitch", "capability.switch", title: "Switches", required: true, multiple: true
        }

		section("To switch off these lights (when the switch is OFF)") {
			input "lights", "capability.colorControl", multiple: true, required: false, title: "Lights, switches & dimmers"
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
		}
    }
}

/*************************
 * Installation & update *
 *************************/

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(app, appTouchHandler)
	subscribe(inputSwitch, "switch", switchHandler)
}

/******************
 * Event handlers *
 ******************/

def appTouchHandler(evt) {
	log.info "app started manually"
    deactivateHue()
}

def switchHandler(evt) {
	log.trace "switchHandler()"
	def current = inputSwitch.currentValue('switch')
	def switchValue = inputSwitch.find{it.currentSwitch == "on"}
    def waitMode = 2500
	if (switchValue) {
    	log.info "Wrong mode to activate anything"
    }
	else {
        pause(waitMode)
        deactivateHue()
        pause(waitMode)
        deactivateHue()
        pause(waitMode)
        deactivateHue()
    }
}

/******************
 * Helper methods *
 ******************/

private deactivateHue() {
	log.trace "Deactivating!"
	state.lastStatus = "off"
    def wait = 250
    log.debug wait

	lights.each {light ->
        light.off()
        pause(wait)
        light.off()
        pause(wait)
        light.off()
    }
}
