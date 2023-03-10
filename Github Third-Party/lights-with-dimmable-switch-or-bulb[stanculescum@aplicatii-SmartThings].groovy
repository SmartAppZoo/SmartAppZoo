/**
 *  Lights with dimmable switch or bulb-parent SmartApp for SmartThings
 *
 *  Copyright 2020
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
 *  v1.0 / 2020-08-21 - Initial Release
 */
definition(
    name: "Lights with dimmable switch or bulb",
    namespace: "mST/parent",
    author: "Mihail Stanculescu",
    description: "Turn on your dimmable bulb or switch, dimmed to a level you set at sunset and increase to full brightness when someone arrives or some other action is triggered. After a number of minutes the light will dim back to its original level. Optionally, set the light to turn off at a specified time while still turning on when someone arrives. This app runs from sunset to sunrise.\n\nAdditional triggers include motion detection, door knock, door open and app button.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/stanculescum/aplicatii-SmartThings/master/pictures/lamp.png",
	iconX2Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-SmartThings/master/pictures/lamp@2x.png",
	iconX3Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-SmartThings/master/pictures/lamp@2x.png"
)

preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Content", install: true, uninstall: true, submitOnChange: true) {
        section {
            app(name: "Lights with dimmable switch or bulb", appName: "Lights with dimmable switch or bulb-child", namespace: "mST/child", title: "Create a new automation", multiple: true, image: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/start-button.png")
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
	return paragraph (image: "https://raw.githubusercontent.com/stanculescum/aplicatii-SmartThings/master/pictures/lamp.png", "${textVersion()}")
}

private def textVersion() {
    def text = "Turn on your dimmable bulb or switch, dimmed to a level you set at sunset and increase to full brightness when someone arrives or some other action is triggered. After a number of minutes the light will dim back to its original level. Optionally, set the light to turn off at a specified time while still turning on when someone arrives. This app runs from sunset to sunrise.\n\nAdditional triggers include motion detection, door knock, door open and app button.\nVersion: v1.0 / 2020-08-21"
}