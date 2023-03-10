/**
 *  SimpliSafe Alarm State revision 5
 *  02-15-2016
 *
 *  Copyright 2015 Ben Fox with modifications by Jim Reardon
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
 *  When you change your Smart Home Monitor security setting -- Arm(away), Arm(home), Off -- your SimpliSafe
 *  system will be set to away, home, or off.
 *
 *  For use with SimpliSafe device type found here: https://community.smartthings.com/t/simplisafe-alarm-integration-cloud-to-cloud/8473
 *
 *  Based heavily on https://github.com/foxxyben/smartthings/blob/master/smartapps/foxxyben/simpliSafeAlarmState.src/simpliSafeAlarmState.groovy
 */
definition(
	name: "SimpliSafe Alarm State",
	namespace: "eviljim",
	author: "Jim Reardon, based on Ben Fox",
	description: "Automatically sets the SimpliSafe alarm state based on the Smartthings smart home monitor state.",
	category: "My Apps",
	iconUrl: "https://pbs.twimg.com/profile_images/594250179215241217/LOjVA4Yf.jpg",
	iconX2Url: "https://pbs.twimg.com/profile_images/594250179215241217/LOjVA4Yf.jpg"
)

preferences {
	page(name: "selectProgram", title: "SimpliSafe Alarm State", install: true, uninstall: true) {
		section("Use this Alarm...") {
			input "simpliSafeAlarm", "capability.alarm", multiple: false, required: true
		}
	}
}

def installed() {
    subscribe(location, "alarmSystemStatus", changeMode)
    subscribe(simpliSafeAlarm, "presence", changeAlarm)
    subscribe(simpliSafeAlarm, "alarm", changeAlarm)
}

def updated() {
    unsubscribe()
    installed()
}

def changeAlarm(evt) {
    def alarmState = simpliSafeAlarm.currentState("alarm").value.toLowerCase()
    def monitorState = location.currentState("alarmSystemStatus").value.toLowerCase()
    def desiredMonitorState

	if (alarmState == "off") {
      desiredMonitorState = "off"
    } else if (alarmState == "home") {
      desiredMonitorState = "stay"
    } else if (presenceState == "away") {
      desiredMonitorState = "away"
    } else {
      log.error "Something went wrong: confused by SS state ${alarmState}"
      return
    }

    log.debug "Smart Home Monitor state: ${monitorState}"
    log.debug "SimpliSafe current alarm state: ${alarmState}"
    
    if (monitorState != desiredMonitorState) {
      log.info "Updating home monitor state to ${desiredMonitorState} from ${monitorState}"
      sendLocationEvent(name: "alarmSystemStatus", value: desiredMonitorState)
    } else {
      log.info "No home monitor state update needed - SS: ${alarmState}, SHM: ${monitorState} / ${desiredMonitorState}"
    }
}

def changeMode(evt) {
    if (evt == null || evt.value == null) {
      log.info "Null event received."
      return
    }

    def currentState = simpliSafeAlarm.currentState("alarm").value.toLowerCase()
    def alarmState = evt.value.toLowerCase()
    
    log.debug "Mode change triggered is: ${alarmState}"
    log.debug "Current SimpliSafe state is: ${currentState}"
    
    if (alarmState == "stay") {
    	if (currentState == "home") {
        	log.info "No state change needed."
        } else {
	        simpliSafeAlarm.home()
    	    log.debug "Setting SimliSafe to HOME"
        }
    } else if (currentState == alarmState) {
    	log.info "No state change needed."
    } else if (alarmState == "away") {
        simpliSafeAlarm.away()
        log.debug "Setting SimliSafe to AWAY"
    } else if (alarmState == "off") {
        simpliSafeAlarm.off()
        log.debug "Setting SimliSafe to OFF"
    } else {
		log.error "Something broke and system status was not changed!"
    }
}
