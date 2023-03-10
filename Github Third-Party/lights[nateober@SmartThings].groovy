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
 *  Turn off lights at night.
 *
 *  Author: SmartThings
 */

definition(
    name: "Turn off lights at night.",
    namespace: "smartthings",
    author: "Nate Ober",
    description: "Save energy by turning off the lights after a configurable time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Turn off these lights light(s)..."){
		input "switches", "capability.switch", multiple: true
	}
	section("When they've been on for..."){
		input "minutes1", "number", title: "Minutes?"
	}
    	section("Between..."){
		input "begin1", "time", title: "beginning?"
        input "end1", "time", title: "ending?"
	}
}

def installed() {
	subscribe(switches, "switch", switchesHandler)
}

def updated() {
	unsubscribe()
	subscribe(switches, "switch", switchesHandler)
}

def switchesHandler(evt) {
    log.debug "$evt.name: $evt.value"
    def data = parseJson(evt.data)
    log.debug "event data: ${data}"
    
     if (evt.value == "on") {
        log.debug "switch turned on!"
        //runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
    } else if (evt.value == "off") {
        log.debug "switch turned off!"
    }
}

//def switchesHandler(evt) {
//	log.debug "$evt.name: $evt.value"
//	if (evt.value == "active") {
//		log.debug "turning on lights"
//		switches.on()
//	} else if (evt.value == "inactive") {
//		runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
//	}
//}

//def scheduleCheck() {
//	log.debug "schedule check"
//	def motionState = motion1.currentState("motion")
//   if (motionState.value == "inactive") {
//        def elapsed = now() - motionState.rawDateCreated.time
//    	def threshold = 1000 * 60 * minutes1 - 1000
//    	if (elapsed >= threshold) {
//            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning lights off"
//            switches.off()
//    	} else {
//        	log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
//        }
//    } else {
//    	log.debug "Motion is active, do nothing and wait for inactive"
//    }
//}