/**
 *  Door Rule
 *
 *  Copyright 2016 Jerry Honeycutt
 *
 *  Version 0.3   20 Mar 2017
 *
 *	Version History
 *
 *  0.1		08 Dec 2016		Initial version
 *  0.2		17 Mar 2017		Beefed up motion rules
 *  0.3		20 Mar 2017		Made lights off less annoying
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
    name: "Door Rule",
    namespace: "Anghus",
    author: "Jerry Honeycutt",
    description: "Manage aspects of a door, including locks, knock sensors, contact sensors, and doorbells.",
    category: "My Apps",

    parent: "Anghus:Home Rules",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@3x.png")

/**/

preferences {
	page(name: "doorPage")
	page(name: "presencePage")
    page(name: "knockPage")
    page(name: "doorbellPage")
    page(name: "lightsPage")
    page(name: "notifyPage")
    page(name: "installPage")
}

def doorPage() {
	dynamicPage(name: "doorPage", nextPage: "presencePage", uninstall: true) {
    	section("DOOR") {
			input name: "contactSensor", type: "capability.contactSensor", title: "Contact", submitOnChange: true, required: false
            if(contactSensor) {
    	    	input name: "doorLock", type: "capability.lock", title: "Door lock", submitOnChange: true, required: false
                if(doorLock) {
                    input name: "lockDelay", type: "number", title: "Delay (minutes)", defaultValue: 15, required: true
                    paragraph "The door lock engages after the specified delay only if the contact sensor is closed."
                }
            }
		}
        if(doorLock) {
            section("MOTION") {
                input name: "motionSensors", type: "capability.motionSensor", title: "Motion sensors", multiple: true, submitOnChange: true, required: false
                if(motionSensors) {
            		input name: "motionTimeout", type: "number", title: "Effective time (seconds)", defaultValue: 30, required: true
				}
                paragraph "If you specify one or more motion sensors, the door lock only engages after the sensors are inactive for the specified delay. After activity, sensors will continue to indicate activity for the duration that Effective Time specifies."
            }
        }
        section("MESSAGES") {
            input name: "notifyOnDoorEvents", type: "bool", title: "Notify on change?", defaultValue: false, submitOnChange: true, required: true
            if(notifyOnDoorEvents) {
                input name: "doorEvents", type: "enum", title: "Which events?", options: ["open", "closed", "locked", "unlocked"], defaultValue: ["open", "closed", "locked", "unlocked"], multiple: true, required: true
                input name: "useCustomDoorMessages", type: "bool", title: "Custom messages?", defaultValue: false, submitOnChange: true, required: true
                if (useCustomDoorMessages) {
                    input name: "openMessage", type: "text", title: "Open message", defaultValue: "The door is open.", required: true
                    input name: "closeMessage", type: "text", title: "Close message", defaultValue: "The door is closed.", required: true
                    input name: "lockMessage", type: "text", title: "Lock message", defaultValue: "The door is locked.", required: true
                    input name: "unlockMessage", type: "text", title: "Unlock message", defaultValue: "The door is unlocked.", required: true
                }
            }
        }
	}
}

def presencePage() {
	dynamicPage(name: "presencePage", nextPage: "knockPage", uninstall: true) {
		section("LEAVING") {
            input name: "people", type: "capability.presenceSensor", title: "People", multiple: true, submitOnChange: true, required: false
            paragraph "Lock door when everyone specified leaves."
        }
        if(people) {
            section("ARRIVAL") {
                input name: "arriveUnlocksDoor", type: "bool", title: "Unlock door when someone arrives?", defaultValue: false, submitOnChange: true, required: true
                if(arriveUnlocksDoor) {
                    input name: "presenceDelay", type: "number", title: "Minimum time away (minutes)", defaultValue: 10, required: true
                	paragraph "When someone returns after the specified delay, the door unlocks. The delay prevents false alarms caused by a wandering GPS."
                }
            }
        }
    }
}

def knockPage() {
	dynamicPage(name: "knockPage", nextPage: "doorbellPage", uninstall: true) {
		section("KNOCK") {
	        input name: "knockSensor", type: "capability.accelerationSensor", title: "Acceleration sensor", submitOnChange: true, required: false
            if(knockSensor) {
                input name: "knockDelay", type: "number", title: "Knock delay (seconds)", defaultValue:5, required: true
                paragraph "The knock delay prevents false alarms by giving the door a chance to open, lock, or unlock before reporting the knock."
            }
        }
        if(knockSensor) {
            section("MOTION") {
                input name: "outsideSensor", type: "capability.motionSensor", title: "Motion sensor", submitOnChange: true, required: false
                if(outsideSensor) {
            		input name: "outsideTimeout", type: "number", title: "Effective time (seconds)", defaultValue: 30, required: true
				}
                paragraph "If there was no recent activity on this motion sensor, the knock is probably a false alarm and is ignored. After activity, sensors will continue to indicate activity for the duration that Effective Time specifies."
            }
            section("MESSAGES") {
                input name: "notifyKnock", type: "bool", title: "Notify on knock?", defaultValue: false, required: true
                input name: "useCustomKnockMessage", type: "bool", title: "Custom message?", defaultValue: false, submitOnChange: true, required: true
                if (useCustomKnockMessage) {
                    input name: "knockMessage", type: "text", title: "Knock message", defaultValue: "Someone is knocking the door.", required: true
                }
            }
        }
    }
}

def doorbellPage() {
	dynamicPage(name: "doorbellPage", nextPage: "lightsPage", uninstall: true) {
    	section("DOORBELL") {
        	input name: "doorbellContact", type: "capability.contactSensor", title: "Doorbell contact", submitOnChange: true, required: false
        }
        if(doorbellContact) {
            section("MESSAGES") {
                input name: "notifyDoorbell", type: "bool", title: "Notify on ring?", defaultValue: false, required: true
                input name: "useCustomDoorbellMessage", type: "bool", title: "Custom message?", defaultValue: false, submitOnChange: true, required: true
                if (useCustomDoorbellMessage) {
                    input name: "doorbellMessage", type: "text", title: "Doorbell message", defaultValue: "Someone is ringing the doorbell.", required: true
                }
            }
        }
    }
}

def lightsPage() {
	dynamicPage(name: "lightsPage", nextPage: "notifyPage", uninstall: true) {
		section("LIGHTS") {
            input name: "lights", type: "capability.switch", title: "Light switches", multiple: true, submitOnChange: true, required: false
            if(lights)
            	input name: "lightTimeout", type: "number", title: "Turn off after (minutes)", defaultValue: 5, required: true
        	paragraph "Turn on lights when the door changes state."
        }
        if(lights) {
            section("EVENTS") {
                input name: "limitLightEvents", type: "bool", title: "Choose events?", defaultValue: false, submitOnChange: true, required: true
                if(limitLightEvents) {
                    input name: "lightEvents", type: "enum", title: "Which events?", options: ["open", "closed", "locked", "unlocked", "doorbell", "knock", "motion"], defaultValue: ["open", "unlocked", "doorbell", "knock", "motion"], multiple: true, required: true
                    input name: "lightModes", type: "mode", title: "During which modes?", multiple: true, required: true
                }
            	paragraph "Optionally, choose events that turn on lights."
            }
        }
    }
}

def notifyPage() {
	dynamicPage(name: "notifyPage", nextPage: "installPage", uninstall: true) {
    	section("NOTIFICATIONS") {
            input name: "sendPush", type: "bool", title: "Send a push notification?", defaultValue: false, required: true
        }
        section("TEXT MESSAGES") {
        	input name: "sendText", type: "bool", title: "Send a text message?", defaultValue: false, submitOnChange: true, required: true
            if (sendText) {
            	input name: "phoneNumber", type: "phone", title: "Phone number", required: true
			}
        }
    }
}

def installPage() {
	dynamicPage(name: "installPage", uninstall: true, install: true) {
    	section("NAME") {
        	label title: "Door name", defaultValue: contactSensor.label, required: false
        }
        section("DEBUG") {
        	input name: "debug", type: "bool", title: "Debug rule?", defaultValue: false, required: true
        }
    }
}

/**/

def installed() {
	trace("installed()")
	initialize()
}

def updated() {
	trace("updated()")
	unsubscribe()
	initialize()
}

def initialize() {
	trace("initialize()")

	state.lockScheduled = false
    state.lightsScheduled = false
    state.whenUnlocked = 0
	state.lastLeave = 0

	// Lock events:
    
	if(doorLock) subscribe(doorLock, "lock", lockEvent)
	if(contactSensor) subscribe(contactSensor, "contact", contactEvent)
    if(motionSensors) subscribe(motionSensors, "motion", motionEvent)
	if(presenceEvent) subscribe(people, "presence", presenceEvent)

	// Knock events:
    
	if(knockSensor) subscribe(knockSensor, "acceleration.active", knockEvent)
 	if(outsideSensor) subscribe(outsideSensor, "motion.active", outsideEvent)
    
    // Doorbell events:
    
    if(doorbellContact) subscribe(doorbellContact, "contact.closed", doorbellEvent)
}

/**/

def lockEvent(evt)	{
	trace("lockEvent($evt.value)")

	if(evt.isStateChange) {
        def customMsg = (evt.value == "locked") ? lockMessage : unlockMessage
        def defaultMsg = "The ${getApp().label.toLowerCase()} was $evt.value."

        state.whenUnlocked = now()
        notify(notifyOnDoorEvents && evt.value in doorEvents, useCustomDoorMessages, customMsg, defaultMsg)
        if(!limitLightEvents || (limitLightEvents && evt.value in lightEvents))
            turnOnLights()

        evaluateLock()
    }
}

def motionEvent(evt) {
	trace("motionEvent($evt.value)")

	if(evt.isStateChange) {
        evaluateLock()
    }
}

def contactEvent(evt) {
	trace("contactEvent($evt.value)")

	if(evt.isStateChange) {
        def customMsg = (evt.value == "open") ? openMessage : closeMessage
        def defaultMsg = "The ${getApp().label.toLowerCase()} was ${(evt.value == "open") ? "opened" : evt.value}."

        notify(notifyOnDoorEvents && evt.value in doorEvents, useCustomDoorMessages, customMsg, defaultMsg)
        if(!limitLightEvents || (limitLightEvents && evt.value in lightEvents))
            turnOnLights()

        evaluateLock()
    }
}

def presenceEvent(evt) {
	trace("presenceEvent($evt.value)")

    if(true) {
    	if(evt.value == "present" && arriveUnlocksDoor) {

			// Someone came home. If timed out, unlock the door.

			if((now() - state.lastLeave) > presenceDelay * 60000) {
            	unlockDoor()
                debug("Unlocking the ${getApp().label.toLowerCase()} after someone has arrived")
			}
        }
        else {

			// If everyone is away, lock the door.

        	def allGone = true
        	people.each {
            	// Any present person changes AllGone from True to False.
            	allGone &= !(it.currentValue("presence") == "present")
            }
            if(allGone) {
            	lockDoor()
                debug("Locking the ${getApp().label.toLowerCase()} after everyone has left")
			}
        	state.lastLeave = now()
        }
    }
}

def evaluateLock() {
	trace("evaluateLock()")

	def doorLocked = (doorLock == null) ? true : doorLock.currentValue("lock") == "locked"
    def doorClosed = (contactSensor == null) ? true : contactSensor.currentValue("contact") == "closed"
	def motionActive = false
    motionSensors.each {
		def events = it.eventsSince(new Date(now() - motionTimeout * 1000))
		motionActive |= events?.findAll {it.value == "active"}.size() > 0
        debug("Motion ${motionActive ? "is" : "is not"} detected on $it.label")

		if(motionActive && it.currentValue("motion") == "inactive") {

			// Handle the scenario where motion stopped but there is recent motion.
            // If motion just stopped, we want to have another look in a bit to see
            // if it's time to lock the door.

			runIn(motionTimeout + 1, evaluateLock, [overwrite: true])
            debug("$it.label was recently active but just changed states.")
            debug("Evaluating $it.label again after $motionTimeout seconds.")
		}
	}

	debug("${getApp().label} is ${doorLocked ? "locked" : "unlocked"}, ${doorClosed ? "closed" : "open"}, and ${motionActive ? "active" : "inactive"}")

	if(!doorLocked && doorClosed && !motionActive) {

		// Schedule the door lock if it's closed, unlocked, and no motion detected.

		runIn((lockDelay ?: 15)*60, lockDoor, [overwrite: true])
        debug("Scheduling the ${getApp().label.toLowerCase()} to lock in $lockDelay minutes")
        state.lockScheduled = true
    }
    else {

		// Otherwise, if the door lock was previously scheduled, remove it from the queue.
        // This would only occur if the door was manually locked, closed, or motion stopped.

		if(state.lockScheduled) {
            unschedule(lockDoor)
            debug("Removing the ${getApp().label.toLowerCase()} lock from the schedule")
            state.lockScheduled = false
        }
    }
}

def lockDoor() {
	trace("lockDoor()")

	doorLock.lock()
	debug("Locking the ${getApp().label.toLowerCase()} after ${Math.round((now() - state.whenUnlocked)/60000)} minutes")
}

def unlockDoor() {
	trace("unlockDoor()")
	doorLock.unlock()
}


/**/

def knockEvent(evt) {
	trace("knockEvent($evt.value)")

	if(evt.isStateChange) {
        runIn(knockDelay ?: 5, evaluateKnock, [overwrite: true])
        debug("Scheduling knock handler for ${getApp().label.toLowerCase()} to run in $knockDelay seconds");
    }
}

def outsideEvent(evt) {
	trace("outsideEvent($evt.value)")

	if(evt.isStateChange) {
        if(!limitLightEvents || (limitLightEvents && "motion" in lightEvents))
            turnOnLights()
    }
}

def evaluateKnock() {
	trace("evaluateKnock()")

	def defaultMsg = "Someone is knocking at the ${getApp().label.toLowerCase()}."

	// Check recent contact, lock, and sensor events.

	def events = contactSensor?.eventsSince(new Date(now() - knockDelay * 1000))
	def recentContact = events?.findAll {it.value == "closed"}.size() > 0 || events?.findAll {it.value == "open"}.size() > 0

	events = doorLock?.eventsSince(new Date(now() - knockDelay * 1000))
	def recentLock = events?.findAll {it.value == "locked"}.size() > 0 || events?.findAll {it.value == "unlocked"}.size() > 0

	events = outsideSensor?.eventsSince(new Date(now() - outsideTimeout * 1000))
	def recentMotion = events?.findAll {it.value == "active"}.size() > 0

	if(!recentContact && !recentLock && recentMotion) {

		// If contact and lock events are't recent, send notification and log event.
        // Ignore the event if the door is open (i.e., door was answered or left open).
        
        if(contactSensor.currentValue("contact") == "closed") {
			notify(notifyKnock, useCustomKnockMessage, knockMessage, defaultMsg)
            if(!limitLightEvents || (limitLightEvents && "knock" in lightEvents))
                turnOnLights()
		}
	}
	else {
    
    	// If any of the sensors have recent activity, log the false alarm.
        
    	debug("Recent knock on the ${getApp().label.toLowerCase()} was just noise")
	}
}

/**/

def doorbellEvent(evt) {
	trace("doorbellEvent($evt.value)")

	if(evt.isStateChange) {
        def defaultMsg = "Someone is ringing the ${getApp().label.toLowerCase()} doorbell."
        notify(notifyDoorbell, useCustomDoorbellMessage, doorbellMessage, defaultMsg)
        if(!limitLightEvents || (limitLightEvents && "doorbell" in lightEvents))
            turnOnLights()
    }
}

/**/

def turnOnLights() {
	trace("turnOnLights()")

	if(lights) {
    	if(!lightModes || (lightModes && location.mode in lightModes)) {
        	def scheduleLag = 0
            lights.each {
            	if(it.currentValue("switch") == "off") {

					// If the light is off, turn it on and schedule it to turn off.
                    // Lag each schedule by 5 seconds to ensure separate schedules.
                    // If the light is already on, don't mess with it. Don't want to
                    // turn off any lights that were manually turned on.

                    it.on()
					runIn(lightTimeout*60+scheduleLag, turnOffLight, [overwrite: false, data: [id: it.id]])
            		debug("Turning on $it.label for $lightTimeout minutes")
                    scheduleLag += 5
                }
                else
            		debug("Skipping light $it.label; it's already on")
			}
        }
        else
        	debug("$location.mode is not in $lightModes")
    }
}

def turnOffLight(data) {
	trace("turnOffLight($data.id)")

	// Check the area around the door for activity.

	def motionActive = false
    motionSensors.each {
		def events = it.eventsSince(new Date(now() - lightTimeout*60000))
		motionActive |= events?.findAll {it.value == "active"}.size() > 0
        debug("Motion ${motionActive ? "is" : "is not"} detected on $it.label")
	}

	if(!motionActive) {

		// There is no recent motion near the door, so turn off the light.

		def light = lights.find{ it.id == data.id }; light.off()
    	debug("Turning off $light.label after $lightTimeout minutes")
    }
    else {

		// There was recent motion near the door, so let's have another look
        // in a bit. This has the effect of a rolling window of time defined
        // by lightTimeout for which motion must be inactive.

			runIn(motionTimeout + 1, turnOffLight,[overwrite: false, data: [id: data.id]])
            debug("${getApp().label} was recently active but just changed states.")
            debug("Evaluating ${getApp().label} again after $motionTimeout seconds.")
    }
}

/**/

private notify(enabled, useCustom, customText, defaultText) {
	if(enabled) {
    	def message = useCustom ? customText : defaultText
		if(sendPush) sendPush(message)
		if(sendText) sendSms(phoneNumber, message)
    }
    log.info(defaultText)
}

private debug(message) {
	if(debug)
    	log.debug(message)
}

private trace(function) {
	if(debug)
    	log.trace(function)
}