/**
 *  Delayed On Timer With Motion Reset
 *
 *  Turn on one or more switches after the timer expires
 *  with the timer reset whenever motion is detected
 *
 *  Copyright 2016 Philip Van Baren
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
	name: "Delayed On Timer With Motion Reset",
	namespace: "pvanbaren",
	author: "Philip Van Baren",
	description: "Turns a switch back on after a timer expires, unless motion is present",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/switches.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/switches@2x.png")

preferences
{
	section("Turn on switch(es)...")
	{
		input "theswitch", "capability.switch", title: "Which?", multiple: true
	}
	section("after movement ceases for...")
	{
		input "thetime", "number", title: "Minutes?"
	}
	section("unless there is movement...")
	{
		input "themotion", "capability.motionSensor", title: "Where?", multiple: true, required:false
	}
}

def installed()
{
	subscribe(themotion, "motion.active", motionDetectedHandler)
	subscribe(themotion, "motion.inactive", motionStoppedHandler)
	subscribe(theswitch, "switch.off", switchOffHandler)
}

def updated()
{
	unsubscribe()
	installed()
}

def switchOffHandler(evt)
{
	log.debug "$evt.name: $evt.value"
	log.debug "setting timeout"
	resetTimeout()
}

def motionDetectedHandler(evt)
{
	log.debug "$evt.name: $evt.value"
	log.debug "resetting timeout"
	resetTimeout()
}

def motionStoppedHandler(evt)
{
	log.debug "$evt.name: $evt.value"
	log.debug "setting timeout"
	resetTimeout()
}

def checkMotion()
{
	if (themotion)
    {
		def motionState = themotion.currentMotion

		log.debug "motionState is $motionState"

		// Check if any motion sensor is active
		if (motionState.contains("active"))
		{
			// Motion active; just log it and do nothing
			log.debug "Motion is active yet, resetting the timer"
			resetTimeout()
		}
		else
		{
			// Get the time elapsed between now and when the motion reported inactive
			// The elapsed time is in milliseconds, but comparisons are in seconds
			def elapsed = (now() - state.lastMotion) / 1000

			def threshold = 60 * thetime

			if (elapsed >= (threshold - 1))
			{
				log.debug "Motion inactive for $elapsed sec: turning switch on"
				theswitch.on()
			}
			else
			{
				log.debug "Motion inactive for $elapsed sec"
				scheduleCheckMotion(threshold - elapsed)
            }
		}
	}
    else
    {
		log.debug "No motion detection configured, turning switch on"
		theswitch.on()
    }
}

private resetTimeout()
{
	state.lastMotion = now()
	scheduleCheckMotion(60 * thetime)
}

private scheduleCheckMotion(def inSeconds)
{
	// Try to turn off the lights after the timeout
	// but no sooner than 20 seconds from now
	def nextCheck = inSeconds;
    if (nextCheck < 20)
    {
    	nextCheck = 20;
    }
	runIn(nextCheck, checkMotion)
}