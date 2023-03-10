/**
 *  Extended Routines
 *
 *  Copyright 2017 Michael Robertson
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
    name: "Extended Routines",
    namespace: "mjr9804",
    author: "Michael Robertson",
    description: "Runs a custom set of actions after an automation routine has run.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "selectRoutines", nextPage: "pageTwo")
    page(name: "pageTwo", title: "Perform these actions...", install: true, uninstall: true) {
        section("Alarms") {
            input "alarmMode", "enum", title: "Set alarm mode to...", options: ["0": "Disabled", "1": "Activity", "2": "Tamper", "3": "Forced Entry"], required: false
            input "alarmSensitivity", "enum", title: "With a sensitivity of...", options: ["1": "High Sensitivity", "2": "Medium-High Sensitivity", "3": "Medium Sensitivity", "4": "Medium-Low Sensitivity", "5": "Low Sensitivity"], required: false
            input "locks", "capability.lock", title: "On these devices...", required: false, multiple: true
        }
    }
}

def selectRoutines() {
    dynamicPage(name: "selectRoutines", nextPage: "pageTwo", install: false, uninstall: true) {    
        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
            actions.sort()
            section() {
                // note: this doesn't work in the IDE simulator, it stores the index instead of the routine name
                input "routine", "enum", title: "When this routine runs...", options: actions
            }
        }
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
    subscribe(location, "routineExecuted", handler)
}

def handler(evt) {
    log.debug "${evt.displayName} routine has run"
    if (evt.displayName == settings.routine) {
        if (alarmMode && locks) {
            setAlarmMode()
        }
        if (alarmSensitivity && locks) {
            setAlarmSensitivity()
        }
    }
}

def setAlarmMode() {
    def newMode = 0x0
    log.debug "alarmMode="+alarmMode
    switch (alarmMode) {
        case "0":
            newMode = 0x0
            break
        case "1":
            newMode = 0x1
            break
        case "2":
            newMode = 0x2
            break
        case "3":
            newMode = 0x3
            break
    }
    locks.setAlarmMode(newMode)
}

def setAlarmSensitivity() {
    def newValue = 0x0
    switch (alarmSensitivity) {
        case "1":
            newValue = 0x5
            break
        case "2":
            newValue = 0x4
            break
        case "3":
            newValue = 0x3
            break
        case "4":
            newValue = 0x2
            break
        case "5":
            newValue = 0x1
            break
    }
    locks.setAlarmSensitivity(newValue)
}