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
 *  The Flasher
 *
 *  Author: bob
 *  Date: 2013-02-06
 */
definition(
    name: "Alarm Delay",
    namespace: "buzzkc",
    author: "BuzzKc",
    description: "Triggers alarm after set delay if alarm mode is still armed",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-contact%402x.png"
)

preferences {
	section("When any of the following devices trigger..."){
		input "mySwitch", "capability.switch", title: "Switch?", required: true
	}
	section("Then trigger..."){
		input "alarms", "capability.alarm", title: "Alarms", required: true, multiple: true
	}
	section("Time delay in seconds..."){
		input "alarmdelay", "number", title: "Seconds to delay alarm", required: true

	}
}

def switchState = false

def installed() {
	log.debug "Installed with settings: ${settings}"

	subscribe()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	subscribe()
}

def subscribe() {

	state.switch = false
	if (mySwitch) {
		subscribe(mySwitch, "switch.on", switchOnHandler)
        subscribe(mySwitch, "switch.off", switchOffHandler)
	}
}

def switchOnHandler(evt) {
	log.debug "Switch $evt.value";
    state.switch = true;
	flashStrobe();
}

def switchOffHandler(evt) {
	log.debug "Switch $evt.value";
	state.switch = false;
    soundAlarm()
}

def flashStrobe() {
	log.debug "Strobe warning";
        alarms?.strobe();
        runIn(alarmdelay, soundAlarm);
}

def soundAlarm() {
	if (state.switch == true) {
        log.debug "Sound Alarm";
        alarms?.both();
    } else {
    	log.debug "Alarm Canceled"
        alarms?.off();
    }
}