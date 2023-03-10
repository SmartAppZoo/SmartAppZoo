/**
 *  Auto Lock
 *
 *  Copyright 2020 Anthony H. Berkemeyer
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
    name: "Auto Lock",
    namespace: "ahb",
    author: "Anthony H. Berkemeyer",
    description: "Automatically lock doors when unlocked for XX minutes.",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/aberkeme/AHB_SmartThings_SmartApps_AutoLock/master/icons/AHB_AutoLock.png",
    iconX2Url: "https://raw.githubusercontent.com/aberkeme/AHB_SmartThings_SmartApps_AutoLock/master/icons/AHB_AutoLock%402x.png",
    iconX3Url: "https://raw.githubusercontent.com/aberkeme/AHB_SmartThings_SmartApps_AutoLock/master/icons/AHB_AutoLock%402x.png")

/* Set preferences for Auto Lock: select the lock and specify the number of minutes to wait before activating the lock.
*/
preferences {
	section("Auto lock when door is unlocked:") {
		input "theLock", "capability.lock", required: true, title: "Which lock?"
	}
    section("Auto lock after a certain amount of time:") {
        input name: "minutesLater", title: "Minutes?", type: "number", multiple: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(theLock, "lock.unlocked", unlockedDetectedHandler)
}

def unlockedDetectedHandler(evt) {
    log.debug "unlockedDetectedHandler called: $evt"
    def delay = minutesLater * 60
    log.debug "Turning off in ${minutesLater} minutes (${delay}seconds)"
    runIn(delay, lockDoor)
}

def lockDoor() {
    theLock.lock()
}
