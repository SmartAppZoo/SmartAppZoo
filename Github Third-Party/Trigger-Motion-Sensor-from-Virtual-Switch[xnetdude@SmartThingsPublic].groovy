/**
 *  Trigger Motion Sensor from Virtual Switch
 *
 *  Copyright 2015 James Houghton
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
 *  Description
 *  -----------
 *  This simple SmartApp is designed to take an activation of a (Simulated/Virtual) Switch (for example, triggered by IFTTT) and create 
 *  an event similar to that of a motion sensor. My first very quick and dirty app (a SmartThings version of Hello World - without 
 *  the actual message!) which could be used in the "use case" below.
 * 
 *  To Do
 *  -----
 *  Because we are using a binary switch and not a momentary switch the state needs to be set to "off" after the initial trigger 
 *  otherwise it will remain in the "on" state, so perhaps change the switch type although this works fine.
 *
 *  Use Case Requirement: Trigger Smart Home Monitor when email arrives in inbox.
 *  -----------------------------------------------------------------------------
 *  An email is sent from a lagacy/non ST compatible (TrendNet) motion sensing camera (that has been powered-on by a SmartThings 
 *  outlet when a SmartThings "Routine" is enabled eg. "Away" or "GoodNight") to a dedicated Gmail account.  
 *
 *  An IFTTT rule is listening for new mail to arrive in the Gmail account and when it does, it will then "Switch On" the
 *  SmartThings virtual switch in this SmartApp.  
 *
 *  Once the switch's event handler is called it will then send a message to a "Simulated Motion Sensor" that activity has been sensed
 *  (and then reset state for switch and sensor).  This short burst of "activity" is enough to trigger the Smart Home Monitor into 
 *  the alarmed state.
 */
  
definition(
    name: "Motion Sensor Activation",
    namespace: "xnetdude",
    author: "James Houghton",
    description: "Trigger Motion Sensor from Virtual Switch",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Which Switch will cause Motion Sensor to Activate:") {
		input "theswitch", "capability.switch", required:true, title: "Which?"
	}

	section("Which Motion Sensor will be activated:") {
		input "themotion", "capability.motionSensor", required:true, title: "Where?"
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
	log.debug "subscribe"
    	subscribe(theswitch, "switch.on", onHandler)
}

def onHandler(evt){
	log.debug "onHandler called: $evt"

	// call active method on themotion object
    	themotion.active()
    
    // reset state for each object, ready for next trigger
    	theswitch.off()
    	themotion.inactive()
}

