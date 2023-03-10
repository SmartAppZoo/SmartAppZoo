/**
 *  Off Without Motion
 *
 *  Concept: Dan Lieberman
 *  Author: Justin J. Novack
 *  Date: 2013-08-21
 *  URL: http://www.github.com/smartthings-users/smartapp.off-without-motion
 *
 * Copyright (C) 2013 Justin J. Novack
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

preferences
{
    section("Detect movement on...") {
        input "motion_detector", "capability.motionSensor", title: "Where?"
    }
    section("When there has been no movement for...") {
        input "minutesLater", "number", title: "Minutes?"
    }
    section("Turn off the following...") {
        input "switches", "capability.switch", multiple: true
    }
}

def installed()
{
    log.debug "Off Without Motion installed. (URL: http://www.github.com/smartthings-users/smartapp.off-without-motion)"
    initialize()
}

def updated()
{
    unsubscribe()
    unschedule()
    log.debug "Off Without Motion updated."
    initialize()
}

def initialize()
{
    log.debug "Settings: ${settings}"
    subscribe(motion_detector, "motion", motionHandler)
}

def turnOff()
{
    log.debug "Turning switches off.  I'll miss you."
    switches.off()
}

def motionHandler(evt)
{
    log.debug "${evt.name} is ${evt.value}."

    if (evt.value == "active") {                // If there is movement then...
        log.debug "Cancelling previous turn off task..."
        unschedule( turnOff )                   // ...we don't need to turn it off.
    }
    else {                                      // If there is no movement then...
        def delay = minutesLater * 60           // runIn uses seconds
        log.debug "Turning off switches in ${minutesLater} minutes (${delay}s)."
        runIn( delay, turnOff )                 // ...schedule to turn off in x minutes.
    }
}
