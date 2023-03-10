/**
 *  Activate fake motion detector when virtual switch is flipped
 *
 *  Copyright 2015 Matt Habermehl
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
    name: "A switch is a motion detector",
    namespace: "matthabermehl",
    author: "Matt Habermehl",
    description: "This app requires the custom device type \"Fake Motion Detector\" available here: https://goo.gl/54Y906",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "page1", title: "Select your switch and fake motion detector", nextPage: "page2", uninstall: true) {
        section("Please select the switch that will trigger your motion detector's 'active' state.") {
            input "triggerSwitch", "capability.switch", title: "Select Switch"
        }
        section("Please select your custom fake motion detector (this won't work with hardware motion detectors because they typically don't accept commands).") {
            input "fakeMotion", "capability.motionSensor", title: "Select Fake Motion Detector"
        }
        section("Motion will time out after this many seconds:") {
            input "seconds", "number", title: "Seconds", required: true
        }
	}
     page(name: "page2", title: "Optional Settings", install: true, uninstall: true)
}

def page2() {
    dynamicPage(name: "page2") {
       	if ( fakeMotion != null && fakeMotion.hasCommand("activate") ){
        	section("Optional settings"){
                mode(name: "modeMultiple",
                     title: "Only when mode is... (optional)",
                     required: false)
                label(name: "label",
                  title: "Name this app installation (optional)",
                  required: false,
                  multiple: false)
            }
		} else {
            section("Error"){
                paragraph "This app requires the custom device type \"Fake Motion Detector\" available here: https://goo.gl/54Y906"
        	}
        }
     }
}


def installed() {
	log.debug "Installed with settings: ${settings}"
    if ( fakeMotion.hasCommand("deactivate") ){
   		fakeMotion.deactivate()
        log.debug "deactivated ${fakeMotion.name}"
    } else {
    	log.debug "${fakeMotion.name} doesn't support the command 'deactivate'"
    }
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(triggerSwitch, "switch.on", onHandler)
}

def onHandler(evt) {
	log.debug evt.value
    if ( fakeMotion.hasCommand("activate") ){
    	unschedule("deactivate")
    	fakeMotion.activate()
        log.debug "activated ${fakeMotion.name}. Will deactivate in about ${seconds} seconds."
		runIn( seconds, deactivate )
    } else {
    	log.debug "${fakeMotion.name} doesn't support the command 'activate'."
    }
    runIn ( 10, turnOffTriggerSwitch )
     
}

def deactivate(){
    if ( fakeMotion.hasCommand("deactivate") ){
    	triggerSwitch.off() // just in case it didn't get turned off before
   		fakeMotion.deactivate()
        log.debug "deactivated ${fakeMotion.name}"
    } else {
    	log.debug "${fakeMotion.name} doesn't support the command 'deactivate'"
    }
}

def turnOffTriggerSwitch(){
	log.debug "Turned off switch"
	triggerSwitch.off() // get ready for the next switch
}