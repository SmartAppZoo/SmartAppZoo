/**
 *	Copyright 2017 SmartThings
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *	Auto Lock Door
 *
 *	Author: TechWithJake, NateBentley, SmartThings
 *	Date: 2017 - 05 - 12
 *
 * Icon Licensed from Neurovit under Creative Commons (Attribution-Share Alike 3.0 Unported)
 *     https://www.iconfinder.com/icons/48787/alert_attention_warning_icon
 *
 * 	Change Log:
 *	1. Creating Auto Door Lock based on Enhanced Auto Door Lock
 *	1.2 Removing Unlock options
 *	1.3 Adding Lock option based on Unlock time
 *	1.4 Adding icons to warning paragraphs
 */

definition(
    name: "Auto Lock Door",
    namespace: "techwithjake",
    author: "Arnaud, TechWithJake, NateBentley",
    description: "Automatically locks a specific door after X minutes when closed.",
    category: "My Apps",
    iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
    iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg",
)

preferences{
	page(name: "appSetup", install: true, uninstall: true)
}

def appSetup() {
	dynamicPage(name: "appSetup", title: "Auto Lock Setup") {
		section("Select the door lock:") {
			input "lock1", "capability.lock", required: true
		}
		section("Automatically lock the door when unlocked...") {
			input "minutesLater1", "number", title: "Delay (in minutes):", required: true
		}
			if (minutesLater1 == 0) {
				section ("Warning!") {
					paragraph image: "https://raw.githubusercontent.com/techwithjake/smartthings-custom/master/smartapps/techwithjake/auto-door-lock.src/warning-icon.png",
					title: "A delay of 0 Minutes is not recommended.",
					"If you set this value to zero, the door will re-lock itself whenever you try to unlock it."
				}
			}
		section("Select the door contact sensor:") {
			input "contact", "capability.contactSensor", required: false, submitOnChange: true
		}
			if (contact != null) {
				section("Automatically lock the door when closed...") {
					input "minutesLater2", "number", title: "Delay (in minutes):", required: true
				}
			}
				if (minutesLater2 == 0) {
					section ("Warning!") {
						paragraph image: "https://raw.githubusercontent.com/techwithjake/smartthings-custom/master/smartapps/techwithjake/auto-door-lock.src/warning-icon.png",
						title: "A delay of 0 Minutes is not recommended.",
						"If you set this value to zero, the door will re-lock itself whenever you try to unlock and open it."
					}
				}
		section("Notifications") {
			input("recipients", "contact", title: "Send notifications to", required: false) {
				input "phoneNumber", "phone", title: "Send Text Message to", description: "Phone Number", required: false
				input "pushNotification", "bool", title: "Send Push Notification", defaultValue: "false", required: false
			}
		}
		section("Modes"){
			mode(title: "Select Mode(s) to run in:", multiple: true, required: false)
		}
	}
}

def installed(){
    initialize()
}

def updated(){
    unsubscribe()
    unschedule()
    initialize()
}

def initialize(){
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", doorHandler, [filterEvents: false])
    subscribe(contact, "contact.closed", doorHandler)
}

def lockDoor(){
    log.debug "Locking the door."
    lock1.lock()
    if(location.contactBookEnabled) {
        if ( recipients ) {
            log.debug ( "Sending Push Notification..." )
            sendNotificationToContacts( "${lock1} LOCKED after ${contact} was closed for ${minutesLater2} minutes or it was unlocked for ${minutesLater1} minutes!", recipients)
        }
    }
    if (phoneNumber) {
        log.debug("Sending text message...")
        sendSms( phoneNumber, "${lock1} LOCKED after ${contact} was closed for ${minutesLater2} minutes or it was unlocked for ${minutesLater1} minutes!")
    }
    if (pushNotification) {
        log.debug("Sending push notification...")
        sendPush("${lock1} LOCKED after ${contact} was closed for ${minutesLater2} minutes or it was unlocked for ${minutesLater1} minutes!")
    }
	else {
		log.debug 'Sending nothing'
		options.method = 'none'
	}
}

def doorHandler(evt){
    if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "unlocked")) { // If a person unlocks a locked door...
		//def delay = (minutesLater * 60) // runIn uses seconds
		runIn( (minutesLater1 * 60), lockDoor ) // ...schedule (in minutes) to lock.
    }
    else if ((lock1.latestValue("lock") == "locked") && (evt.value == "locked")) { // If a person manually locks it then...
		unschedule( lockDoor ) // ...we don't need to lock it later.
    }
    if ((contact.latestValue("contact") == "closed") && (evt.value == "locked")) { // If the door is closed and a person manually locks it then...
		unschedule( lockDoor ) // ...we don't need to lock it later.
    }
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "unlocked")) { // If the door is closed and a person unlocks it then...
		//def delay = (minutesLater * 60) // runIn uses seconds
		runIn( (minutesLater2 * 60), lockDoor ) // ...schedule (in minutes) to lock.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "closed")) { // If a person closes an unlocked door...
		//def delay = (minutesLater * 60) // runIn uses seconds
		runIn( (minutesLater2 * 60), lockDoor ) // ...schedule (in minutes) to lock.
    }
}
