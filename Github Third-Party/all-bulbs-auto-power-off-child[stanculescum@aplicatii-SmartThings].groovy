/**
 *  All Bulbs Auto Power Off!-child
 *  Copyright 2019
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  
 *  About: ZigBee bulbs are automatically off when the main power supply voltage is restored and bulbs will be on, but no one is home.
 *
 *  Version: v1.0 / 2019-12-12 - Initial Release
 *  Author: Mihail Stanculescu
 */
definition(
    name: "All Bulbs Auto Power Off!-child",
    namespace: "mST/child",
    author: "Mihail Stanculescu",
    description: "All bulbs are turned off automatically if no one is home after a set time",
    category: "My Apps",
    parent: "mST/parent:All Bulbs Auto Power Off!",
    iconUrl: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/bulb-zigbee-icon.png",
    iconX2Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/bulb-zigbee-icon@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/bulb-zigbee-icon@2x.png"
)

preferences {
    section("The following bulbs...") {
		input "bulb", "capability.switch", title: " ", multiple: true, required: true
	}
    section("Turn off all light bulbs after ... minutes when no one is home and the main power supply is restored") {
		input "lockTime", "number", title: "Auto off time (seconds)", description: "Number of minutes", required: true, defaultValue: "10"
	}
}

def installed() {
	subscribe(bulb, "switch", bulbHandler)
}

def updated() {
	unsubscribe()
	subscribe(bulb, "switch", bulbHandler)
}

def bulbHandler(evt) {
	log.debug "$evt.value"
	if (evt.value == "on") {
    	def MinuteDelay = (lockTime)
    	runIn(MinuteDelay, turnOffSwitch)
    	log.debug "The lights were turned off!"
    }
}

def turnOffSwitch() {
	bulb.off()
}
