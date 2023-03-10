/**
 *  Auto Lock After Sunset and at Sunset
 *  Copyright 2016 Patrick Cartwright
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
    name: "Auto Lock After Sunset and at Sunset",
    namespace: "pcartwright81",
    author: "Patrick Cartwright",
    description: "This app auto locks the door after sunset and additionally locks the door at sunset.  An offset can be provided.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-locks.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-locks@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-locks@2x.png")


preferences {
	section ("Auto-Lock...")
    	{
		input "contact0", "capability.contactSensor", title: "Which door?"
        	input "lock0","capability.lock", title: "Which lock?"
        	input "autolock_delay", "number", title: "Delay for auto-Lock after door is closed? (Seconds)"
        	input "relock_delay", "number", title: "Delay for re-lock w/o opening door? (Seconds)"
        	input "leftopen_delay", "number", title: "Notify if door open for X seconds."
            input "offset", "number", title: "Lock this many minutes after sunset"
            input "push_enabled", "enum", title: "Enable NORMAL push notifications?", metadata: [values: ["Yes","No"]]
            input "debug_notify", "enum", title: "Enable DEBUG push notifications?", metadata: [values: ["Yes","No"]]
            input "phone_debug_enabled", "enum", title: "Enable DEBUG push to phone?", metadata: [values: ["Yes","No"]]
            input "phone", "phone", title: "Send a text message (enter tel. #)?", required: false
    	} 
}

def installed() {
  debug_handler("Installed")
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
	debug_handler("Initializing")
	scheduleLock()
	setautolock()
}

def setautolock(){
    subscribe(lock0, "lock", door_handler, [filterEvents: false])
    subscribe(lock0, "unlock", door_handler, [filterEvents: false])  
    subscribe(contact0, "contact.open", door_handler)
	subscribe(contact0, "contact.closed", door_handler)
}

def updated(settings) {
    debug_handler("Updated")
    unschedule()
  	unsubscribe()
    initialize()
}

def timeToLockDoor() {
    debug_handler("Locking ${lock0.displayName} due to scheduled lock.")
 	lock_door()
    scheduleLock()
}

def scheduleLock() {
	debug_handler("The zip code for this location: ${location.zipCode}")
    
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.currentValue("sunsetTime"))

    //calculate the offset
    def timeaftersunset = new Date(sunsetTime.time + (offset * 60 * 1000))
	debug_handler("Scheduling for: $timeaftersunset")

    //schedule this to run one time
    runOnce(timeaftersunset, timeToLockDoor)
}

def door_handler(evt)
{	
    def sunsetWithOffset = new Date(getSunriseAndSunset().sunset.time + (offset * 60 * 1000))
    def d = new Date()
    if(!d.after(sunsetWithOffset) && !d.before(getSunriseAndSunset().sunrise.time))
    { 
        debug_handler("Not Locking Door It Is Not Time")
        return;
    }
	if(evt.value == "closed")
    {
		unschedule( lock_door )
    	unschedule( notify_door_left_open )
        state.lockattempts = 0
        
        if(autolock_delay == 0)
        {
        	debug_handler("$contact0 closed, locking IMMEDIATELY.")
        	lock_door()
        }
        else
        {
        	debug_handler("$contact0 closed, locking after $autolock_delay seconds.")
			runIn(autolock_delay, "lock_door")
        }
	}
	if(evt.value == "open")
	{
		unschedule( lock_door )
        unschedule( notify_door_left_open )
        unschedule( check_door_actually_locked )
        state.lockattempts = 0 // reset the counter due to door being opened
        debug_handler("$contact0 has been opened.")
	 	runIn(leftopen_delay, "notify_door_left_open")
	}
    
	if(evt.value == "unlocked")
	{
    	unschedule( lock_door )
        unschedule( check_door_actually_locked )
        state.lockattempts = 0 // reset the counter due to manual unlock
    	debug_handler("$lock0 was unlocked.")
        debug_handler("Re-locking in $relock_delay seconds, assuming door remains closed.")
        runIn(relock_delay, "lock_door")
	}
	if(evt.value == "locked") // since the lock is reporting LOCKED, action stops.
	{
    	unschedule( lock_door )
    	debug_handler("$lock0 reports: LOCKED")
	}
}

def lock_door() // auto-lock specific
{
	if (contact0.latestValue("contact") == "closed")
	{
		lock0.lock()
    	debug_handler("Sending lock command to $lock0.")
        pause(10000)
        check_door_actually_locked()     // wait 10 seconds and check thet status of the lock
	}
	else
	{
    	unschedule( lock_door )
    	debug_handler("$contact0 is still open, trying to lock $lock0 again in 30 seconds")
        runIn(30, "lock_door")
	}
}

def check_door_actually_locked() // if locked, reset lock-attempt counter. If unlocked, try once, then notify the user
{
	if (lock0.latestValue("lock") == "locked")
    {
    	state.lockattempts = 0
    	debug_handler("Double-checking $lock0: LOCKED")
        unschedule( lock_door )
        unschedule( check_door_actually_locked )
        if(state.lockstatus == "failed")
        {
        	message_handler("$lock0 has recovered and is now locked!")
            state.lockstatus = "okay"
        }
    }
    else // if the door doesn't show locked, try again
    {
    	if (contact0.latestValue("contact") == "closed") // just a double-check, since the door can be opened quickly.
        {
            state.lockattempts = state.lockattempts + 1
            if ( state.lockattempts < 2 )
            {
                unschedule( lock_door )
                debug_handler("$lock0 lock attempt #$state.lockattempts of 2 failed.")
                runIn(15, "lock_door")
            }
            else
            {
                message_handler("ALL Locking attempts FAILED! Check out $lock0 immediately!")
                state.lockstatus = "failed"
                unschedule( lock_door )
                unschedule( check_door_actually_locked )
            }
        }
	}
}

def notify_door_left_open()
{
	message_handler("$contact0 has been left open for $leftopen_delay seconds.")
}

def debug_handler(msg)
{
	log.debug msg
	if(debug_notify == "Yes")
    {
    	sendPush msg	
    }
    if(phone_debug_enabled == "Yes")
    {
    	if (phone) {
      		sendSms phone, msg
  		}
    }    
}

def message_handler(msg)
{
    log.debug msg
	if(push_enabled == "Yes")
    {
    	sendPush msg
    }
	if (phone) {
      sendSms phone, msg
  }
}
