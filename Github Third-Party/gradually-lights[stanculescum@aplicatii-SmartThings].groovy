/**
 *  Gradually lights SmartApp for SmartThings
 *
 *  Copyright (c) 2019
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
 *  Overview
 *  ----------------
 *  This SmartApp will turn on the lights gradually.
 *
 *  Install Steps
 *  ----------------
 *  1. Create new SmartApps at https://graph.api.smartthings.com/ide/apps.
 *  2. Install the newly created SmartApp in the SmartThings mobile application.
 *  3. Configure the inputs to the SmartApp as prompted.
 *  4. Tap done.
 *  5. Enjoy...
 *
 *  Revision History
 *  ----------------
 *  2019-11-12  v1.0  Initial release
 *
 */

definition(
	name: "Gradually lights",
	namespace: "mST",
	author: "Mihail Stanculescu",
	description: "This SmartApp will turn on the lights gradually.",
	category: "My Apps",
	iconUrl: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/gradually-lights.png",
	iconX2Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/gradually-lights@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/gradually-lights@2x.png"
)

preferences {
	page(name: "mainPage", title: "Adjust the level of your bulbs", nextPage: "secondPage", uninstall: true) {
		section("Control with switch..."){
			input "mySwitch", "capability.switch", title: "Switch?", required: true, multiple: false
		}
		section("Control these bulbs...") {
			input "bulb", "capability.switchLevel", title: "Which Bulbs?", required:false, multiple:true
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
    page(name: "secondPage", title: "Adjust the level", install: true) {
        section("Options...") {
            input "levelSec", "number", title: "Seconds between change?", required: true, defaultValue:5
        }
        section("Options...") {
			input "levelStep", "number", title: "Number of level steps per interval?", required: true, defaultValue:1
        }
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(mySwitch, "switch.on",  eventHandlerOn)
	subscribe(mySwitch, "switch.off", eventHandlerOff)
	subscribe(bulb,     "switch.on",  eventHandlerOn)
	subscribe(bulb,     "switch.off", eventHandlerOff)
    if (mySwitch.switch == "on") {
    	myRunIn(1, "nextLevel")
    }
}

def eventHandlerOn(evt) {
	log.trace "eventHandlerOn($evt.name: $evt.value)"
	bulb?.on()
    //nextLevel
    myRunIn(1, "nextLevel")
}

def eventHandlerOff(evt) {
	log.trace "eventHandlerOff($evt.name: $evt.value)"
	unschedule()
    bulb?.off()
}

def nextLevel() {
	if (settings.levelStep < 1) { settings.levelStep = 1 }
	if (settings.levelSec  < 1) { settings.levelSec  = 1 }
	
	//Loop through level (1-100)
    def previousLevelBulb = state.levelBulb as Integer
    def levelBulb = 1
    if (previousLevelBulb == null) {
    	levelBulb = 1
    } else if (previousLevelBulb >= 0 && previousLevelBulb < 100) {
    	levelBulb = (previousLevelBulb as Integer) + (settings.levelStep as Integer)
    } else {
    	levelBulb = 1
    }
	if (levelBulb > 100) { levelBulb = 1 }
	state.levelBulb = levelBulb

	bulb*.setLevel(levelBulb)
    myRunIn(1, "nextLevel")
}

private def myRunIn(delay_s, func) {
    if (delay_s > 0) {
        def tms = now() + (delay_s * 100)
        def date = new Date(tms)
        runOnce(date, func)
        log.trace("runOnce() scheduled for ${date}")
    }
}