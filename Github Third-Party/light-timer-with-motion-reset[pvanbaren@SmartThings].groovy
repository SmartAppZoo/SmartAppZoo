/**
 *  Light Timer With Motion Reset
 *
 *  Turns off one or more lights after a timeout, unless motion occurs on one or more sensors.
 *  The timer is extended whenever there is any motion.
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
	name: "Light Timer With Motion Reset",
	namespace: "pvanbaren",
	author: "Philip Van Baren",
	description: "Turns off a light after the timer expires, unless there is motion",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/smart-light-timer.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/smart-light-timer@2x.png")

preferences
{
	section("Turn off light(s)...")
	{
		input "theswitch", "capability.switch", multiple: true
	}
	section("after what length of time...")
	{
		input "thetime", "number", title: "Minutes?"
	}
	section("unless there is movement...")
	{
		input "themotion", "capability.motionSensor", title: "Where? (optional)", multiple: true, required: false
	}
}

def installed()
{
	subscribe(themotion, "motion.active", motionDetectedHandler)
	subscribe(themotion, "motion.inactive", motionStoppedHandler)
	subscribe(theswitch, "switch.on", switchOnHandler)
	
	// Start the timeout in case the light is currently on
	resetTimeout()
}

def updated()
{
	unsubscribe()
	installed()
}

def switchOnHandler(evt)
{
	log.debug "$evt.name: $evt.value"
	log.debug "setting timeout"
	resetTimeout()
}

def motionDetectedHandler(evt)
{
	log.debug "$evt.name: $evt.value"
	log.debug "extending timeout"
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
        def motionState = themotion.currentState("motion").value

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
                log.debug "Motion inactive for $elapsed sec: turning switch off"
                theswitch.off()
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
    	log.debug "No motion configured, turning switch off"
    	theswitch.off()
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
    	nextCheck = 20
    }
	runIn(nextCheck, checkMotion)
}
