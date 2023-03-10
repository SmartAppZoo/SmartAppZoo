/**
 *	Enhanced Switch On Open
 *
 *	Oddities and notes:
 *
 *	If the switch that this app turns on is "messed with", it cancels the turn-off event.  In other words,
 *	if opening the door causes "switch1" to be turned on, and then press the physical "on" switch for
 *  "switch1", it won't turn off.  This allows a person to override the automatic function.  HOWEVER, this
 *  doesn't seem to work all the time.  Not all physical switches send an "on" event if they are already
 *  on, and when using 3-way switches, the auxillary switches might not send an "on" event even if the
 *  main switch does. 
 *
 *  In testing with GE/Jasco standard on/off switches, the main switch ALWAYS sends an 'on' event.  However,
 *  an auxillary switch in a 3-way configuration will never send an 'on' event (if the switch is already on.)
 *
 *  Somewhere along the line, ST changed things up so that the on/off switches weren't always sending on/off event
 *  even when filtering was turned off.  Not sure when that happened, but it did break this code badly...
 *
 *	The original licensing applies, with the following exceptions:
 *		1.	These modifications may NOT be used without freely distributing all these modifications freely
 *			and without limitation, in source form.	 The distribution may be met with a link to source code
 *			with these modifications.
 *		2.	These modifications may NOT be used, directly or indirectly, for the purpose of any type of
 *			monetary gain.	These modifications may not be used in a larger entity which is being sold,
 *			leased, or anything other than freely given.
 *		3.	To clarify 1 and 2 above, if you use these modifications, it must be a free project, and
 *			available to anyone with "no strings attached."	 (You may require a free registration on
 *			a free website or portal in order to distribute the modifications.)
 *
 *	Copyright 2015 by "garyd9"
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "Enhanced Switch On Open",
	namespace: "garyd9",
	author: "Gary D",
	description: "Allows turning on a light when opening a door.  Enhanced to provide more scheduling options (Sunrise/sunset), allow canceling the timer that turns the light back off, and doesn't fire the action if the light was ALREADY on.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")



 preferences
 {
	 section("Nighttime light on door open")
	 {
		input "contact1", "capability.contactSensor", title: "When this door opens...", required: true
		input "switch1", "capability.switch", title: "Turn on this light...", multiple: false, required: true
		input "howLong", "number", title: "For this many minutes...", description: "default 3 minutes", required: false
        input name: "nightOnly", type: "bool", title: "Only between sunset and sunrise?"
	 }
}



def installed()
{
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated()
{
	log.debug "Updated with settings: ${settings}"
        
	unsubscribe()
	initialize()

}

def initialize()
{
	// obviously need to know when the contact is opened...
	subscribe(contact1, "contact.open", contactOpenHandler)
	// we need to know if the switch was messed with in any way (it cancels a turnoff schedule)
    
    // functionality to detect any switch movement disabled for debugging
    
//	subscribe(switch1, "switch", switchHandler, [filterEvents: false])
	subscribe(switch1, "switch.off", switchHandler)
    if (nightOnly)
    {
		// force updating the sunrise/sunset data
		retrieveSunData(true)
    }
}


def contactOpenHandler(evt)
{
	def timer = howLong ?: 3000

	// no longer working in UTC.  Everything is converted to "location.timeZone".  This was needed
    // because sometimes the sunset time passed to "timeToday" actually was in the past.  That was
    // unexpected.  At least when converting to "local time", I can force the sunset to be "after noon today"
	def bIsValidTime = !nightOnly
    if (!bIsValidTime)
    {
		// possibly update the sunrise/sunset data. (don't force the update)
		retrieveSunData(false)
        // make sure the default tz is set for the local timezone.  
		TimeZone.setDefault(location.timeZone)
		// get the current time
		def curTime = new Date(now())

		// when is/was sunrise TODAY after midnight local. 
		def dtSunrise = timeTodayAfter("0:00", state.sunriseTime, location.timeZone)
        // when is/was sunset TODAY after high noon local.
        def dtSunset = timeTodayAfter("12:00", state.sunsetTime, location.timeZone)

// debug block
		// I was experiencing some edge cases where lights were going on when they shouldn't (due to daytime), etc.  So,
        // dump the variables used for figuring out sunrise/sunset times for debugging...
        log.debug "state.sunriseTime: ${state.sunriseTime} dtSunrise: ${dtSunrise.inspect()}"
        log.debug "state.sunsetTime: ${state.sunsetTime} dtSunrise: ${dtSunset.inspect()}"
        log.debug "curTime: ${curTime.inspect()}"
// end debug block

        // then check if the local time is before sunrise (it must be early AM) or after sunset (it must be late PM.) 
    	bIsValidTime = ((curTime.getTime() < dtSunrise.time) ||  (curTime.getTime() > dtSunset.time))
    }
	
    if (bIsValidTime)
	{
    	if ((switch1.switchState.value == "off") || (state.active == true))
		{
			// do we already have an active handler?  if so, cancel it so
			// it can be reset.
			if (state.active == true)
			{
				log.debug "Cancelling existing schedule to reset."
				unschedule()
				state.active = false
			}
			log.trace ("Turning on switch: $switch1")
			switch1.on()
			// set the active state AFTER turning on the switch.
			state.active = true
			runIn(60 * timer, turnOffSwitch)
		}
        else
		{
			log.debug "Switch was already on; not doing anything"
		}
	}
	else
	{
		log.debug "$contact1 opened, but it's daytime"
	}
}


def switchHandler(evt)
{
	if (state.active == true)
	{
		// cancel the runIn
		log.debug "Switch event detected.  Canceling scheduled turn off"
		unschedule()
		state.active = false
	}
//    else
//    {
//    	log.debug "ignoring switch event from $switch1"
//    }
}

def turnOffSwitch()
{
	log.debug "timeout expired - turning off $switch1"
	state.active = false
	switch1.off()
}


def retrieveSunData(forceIt)
{
	if ((true == forceIt) || (now() > state.nextSunCheck))
	{
		state.nextSunCheck = now() + (1000 * (60 * 60 *12)) // every 12 hours
		log.debug "Updating sunrise/sunset data"

	/* instead of absolute timedate stamps for sunrise and sunset, use just hours/minutes.	The reason
	   is that if we miss updating the sunrise/sunset data for a day or two, at least the times will be
	   within a few minutes.  Using "timeToday" or "timeTodayAfter", the hours:minutes can be converted
       to the current day.. (this won't work when transitioning into or out of DST) */

		TimeZone.setDefault(location.timeZone)
		def sunData = getSunriseAndSunset(zipcode : location.zipCode)
        
        // tzOffset should actually end up being "0", assuming the proper TZ is configured.  However,
        // I've seen ST come back with dates in Pacific time and UTC.. so do the work to find 
        // the "local" tzOffet for adding to the returned sunrise/sunset data.
        def tzOffset = location.timeZone.getOffset(sunData.sunrise.getTime()) + (sunData.sunrise.getTimezoneOffset() * 60000)

        def newDate = new Date(sunData.sunrise.getTime() + tzOffset)
		state.sunriseTime = newDate.hours + ':' + newDate.minutes

        newDate = new Date(sunData.sunset.getTime() + tzOffset)
		state.sunsetTime = newDate.hours + ':' + newDate.minutes
        
		log.debug "Sunrise time: ${state.sunriseTime} (sunData.sunrise: ${sunData.sunrise.inspect()})"
		log.debug "Sunset time: ${state.sunsetTime} (sunData.sunset: ${sunData.sunset.inspect()}) "
	}
}
