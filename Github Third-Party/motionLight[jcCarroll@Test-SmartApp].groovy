/**
 *  testSmartApp
 *
 *  Copyright 2017 Jacob Carroll
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
 
// Every SmartApp must have a definition method call
// Provides metadata about the SmartApp itself
definition(
	
    name: "testSmartApp",
    namespace: "jcCarroll",
    author: "Jacob Carroll",
    description: "This is just my first attempt at a SmartThings SmartApp.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

// This is where the app defines what information is needed from the user
// It is the configuration section seen after installing the app
preferences {
	// Defines the section and gives it a title
	section("Turn on when motion detected:") {
    	// The device name and what it does
        // Required does just that, needs input to continue
        // Title is just the title for the input box the user sees
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn off when there's been no movement for") {
    	// This input is used to gather information to turn the light off after a set amount of time
    	input "minutes", "number", repuired: true, title: "Minutes?"
    }
    section("Turn on/off this light") {
    	// Sets the device that is activated by the event you are setting up
        input "theswitch", "capability.switch", required: true
    }
}

// Run after app installation
def installed() {
	// Just a log like console.log. Shows what is going on
	log.debug "Installed with settings: ${settings}"
	
    // Method called after filling out the preferences to get things going
	initialize()
}

// Called when user updates their selection 
// Change motion or use different light
def updated() {
	log.debug "Updated with settings: ${settings}"

	// The app needs to "disconnect" from devices
	unsubscribe()
    // Then run the initialize method to attach to the new devices
	initialize()
}

def initialize() {
	// The subscribe method accepts three parameters
    // The thing we want to subscribe to
    // The specific attribute and its state we care about
    // The name of the method that should be called when this event happens
	subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    // Turns the switch on
    theswitch.on()
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    // runIn is used to schedule the checkMotion() method
    runIn(60 * minutes, checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"

    // get the current state object for the motion sensor
    def motionState = themotion.currentState("motion")

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            theswitch.off()
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
            // Motion active; just log it and do nothing
            log.debug "Motion is active, do nothing and wait for inactive"
    }
}
