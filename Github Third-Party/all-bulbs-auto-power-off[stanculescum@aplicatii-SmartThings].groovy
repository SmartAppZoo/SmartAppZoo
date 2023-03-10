/**
 *  All Bulbs Auto Power Off! - Parent SmartApp for SmartThings
 *
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
    name: "All Bulbs Auto Power Off!",
    namespace: "mST/parent",
    author: "Mihail Stanculescu",
    description: "All bulbs are auto power off when the main power supply voltage is restored and bulbs will on, but no one is home.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/bulb-zigbee-icon.png",
    iconX2Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/bulb-zigbee-icon@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/bulb-zigbee-icon@2x.png"
)

preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Content", install: true, uninstall: true, submitOnChange: true) {
        section {
            app(name: "Auto Power Off!", appName: "All Bulbs Auto Power Off!-child", namespace: "mST/child", title: "Create a new automation", multiple: true, image: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/start-button.png")
		}
        section("About") { 
			headerSECTION()
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
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
}

def headerSECTION() {
	return paragraph (image: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/bulb-zigbee-icon.png", "${textVersion()}")
}

private def textVersion() {
    def text = "This application turns off the lights when no one is home.\nVersion: v1.0 / 2019-10-15"
}