/**
 *  Auto Garage Close
 *
 *  Author: Jim Mulholland (@mully)
 *  Collaborators: @mully
 *  Date: 2015-04-29
 *  URL: 
 *
 * Copyright (C) 2015 Jim Mulholland.
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions: The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

definition(
    name: "Auto Garage Close",
    namespace: "smartthings",
    author: "Ted Reimer",
    description: "Close an open garage door after X minutes.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
preferences
{	
	section("Garage door") {
		input "door", "capability.doorControl", title: "Which garage door controller?"
		input "openThreshold", "number", title: "Close when open longer than ",description: "Number of minutes", required: true
	}
}

def installed()
{
	log.trace "installed()"
    subscribe()
}

def updated()
{
	log.trace "updated()"
	unsubscribe()
	subscribe()
    log.debug "Auto Garage Close updated."
}

def subscribe() {
    log.debug "Settings: ${settings}"
	subscribe(door, "door", garageDoorState)
}


def garageDoorState(evt)
{
	log.info "garageDoorState, $evt.name: $evt.value"
	if (evt.value == "open") {
		schedule("0 * * * * ?", "doorOpenCheck")
        log.debug "Schedule Set"
	}
	else {
		unschedule("doorOpenCheck")
        log.debug "Unscheduled"
	}
}

def doorOpenCheck()
{
	final thresholdMinutes = openThreshold
	log.debug "doorOpenCheck -- ${currentState?.value}; thresholdMinutes: ${thresholdMinutes}"
	if (thresholdMinutes) {
		def currentState = door.doorState
		if (currentState?.value == "open") {
			log.debug "open for ${(now() - currentState.date.time)/60000} minutes"
			if (now() - currentState.date.time > thresholdMinutes * 60 *1000) {
				def msg = "${door.displayName} has been open for ${thresholdMinutes} minutes"
				log.info msg
				door.close()
			}
		}
	}
}