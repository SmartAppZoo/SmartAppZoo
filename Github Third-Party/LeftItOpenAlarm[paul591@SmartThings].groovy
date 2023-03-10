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
 *  Left It Open
 *
 *  Author: SmartThings
 *  Make Alarm Version: JohnS.
 * 
 *  Date: 2013-05-09
 */
definition(
    name: "Left It Open Alarm",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Notifies you via Alarm when you have left a door or window open longer that a specified amount of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {

	section("Monitor this door or window") {
		input "contact", "capability.contactSensor"
	}
	section("And notify me if it's open for more than this many minutes (default 1)") {
		input "openThreshold", "number", description: "Number of minutes", required: false
	}
	section("Sound this Alarm unil its closed") {
		input "alarm", "capability.alarm"
	}
	section("Alarm only (no Strobe)") {
		input "alarmOnly", "boolean", description: "Don't use the strobe"
	}
}

def installed() {
	log.trace "installed()"
    state.alarm = false;
	subscribe()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
	subscribe()
    state.alarm = false;
}

def subscribe() {
	subscribe(contact, "contact.open", doorOpen)
	subscribe(contact, "contact.closed", doorClosed)
}

def doorOpen(evt)
{
	log.trace "doorOpen($evt.name: $evt.value)"
	def t0 = now()
    // delay 1 min by default
	def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 60
	runIn(delay, doorOpenTooLong, [overwrite: true])
	log.debug "scheduled doorOpenTooLong in ${now() - t0} msec"
}

def doorClosed(evt)
{
	log.trace "doorClosed($evt.name: $evt.value)"
    if (state.alarm) {
    	state.alarm = false;
    	alarm.off()
    }
    // kill any pending request
    runIn(1, doNothing, [overwrite: true]);
}

def doNothing() {
	// just here to kill the pending open request on close
	log.warn "door closed before timeout:  doing nothing"
}

def doorOpenTooLong() {
	def contactState = contact.currentState("contact")
    
    // if it's closed now, just ignore the event
	if (contactState.value == "open") {
    	state.alarm = true
    	if (alarmOnly) {
        	log.trace "left open too long - alarm only!"
        	alarm.siren()
        } else {
        	log.trace "left open too long - alarm and strobe!"
	    	alarm.both()
        }
	} else {
		log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
	}
}
