/**
 *  Lock Door after Closed with Retries
 *
 *  Copyright 2017 Vincent
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
    name: "Lock Door after Closed",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Automatically lock the door after it's closed. Specially designed for Schlage Connect",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Select the door lock:") {
		input "thelock", "capability.lock", required: true
	}
    section("Select the door contact sensor:") {
    	input "thesensor", "capability.contactSensor", required: true
    }
	section("Select notification devices after all locking attempts fail:") {
		input "notifications", "capability.notification", multiple: true, required: false
		input "message", "string", title: "Custom Message", required: false
	}
    section("Enable debug logging:") {
        input "enableDebug", "bool", title: "Enable?", required: false
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(thesensor, "contact.closed", closeHandler)
	subscribe(thesensor, "contact.open", openHandler)
	subscribe(thelock, "lock.unlocked", unlockHandler)
	subscribe(thelock, "lock.locked", lockHandler)
    state.canLock = true
}

def closeHandler(evt) {
	lockTheDoor()
	scheduleLockingTasks();
}

def openHandler(evt) {
	unscheduleLockingTasks()
}

def unlockHandler(evt) {
	state.canLock = false

	// It requires at least 10 seconds for Schlage Connect to accept lock command after unlocked.
	runIn(11, allowLockAndLockTheDoor)
	if (thesensor.latestValue("contact") == "closed") {
		scheduleLockingTasks()
	}
}

def lockHandler(evt) {
	unschedule(lockTheDoor)
	unschedule(refreshLock)
	unschedule(notifyOnFailure)
}

def allowLockAndLockTheDoor() {
	if (enableDebug) log.debug("Lock allowed")
	state.canLock = true
    lockTheDoor()
}

def scheduleLockingTasks() {
	runIn(3 * 60, refreshLock)
	runIn(6 * 60, notifyOnFailure)
	for (delay in [15, 30, 60, 2 * 60, 4 * 60, 5 * 60]) {
		runIn(delay, lockTheDoor, [overwrite: false])
	}
}

def unscheduleLockingTasks() {
	unschedule(refreshLock)
	unschedule(notifyOnFailure)
	unschedule(lockTheDoor)
}

def lockTheDoor() {
	if (enableDebug) log.debug("Checking the lock and contact states")
	if (!state.canLock || thesensor.latestValue("contact") != "closed") return
	if (enableDebug) log.debug("Sending lock command")
	thelock.lock()
}

def refreshLock() {
	if (enableDebug) log.debug("Refreshing the lock")
	thelock.refresh()
}

def notifyOnFailure() {
	if (notifications == null) return
	def msg = message ? message : "Failed to lock ${thelock.label ? thelock.label : thelock.name}"
	notifications.deviceNotification(msg)
}