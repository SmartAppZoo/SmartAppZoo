/**
 *  Insteon Device Manager Smart App
 *  
 *  Copyright 2018 Hugo Bonilla
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

def clientVersion() { return "01.00" }

definition (
	name: "Insteon Device Manager",
	namespace: "HBOdR",
	author: "Hugo Bonilla",
	description: "Used to install and manage Insteon devices. Currently work with switches only.",
    category: "Convenience",
	singleInstance: true,
    iconUrl: "http://www.insteon.com.au/img/site/badge-large.png",
	iconX2Url: "http://www.insteon.com.au/img/site/badge-large.png",
	iconX3Url: "http://www.insteon.com.au/img/site/badge-large.png",
)

preferences {
	page(name: "pageMain")
	page(name: "switchSettings")
}

def pageMain() {
	def installed = app.installationState == "COMPLETE"
    
	dynamicPage(name: "pageMain", title: "", install: true, uninstall: true) {
	
		section("SmartThings Hub") {
			input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
		}

		section("Insteon Settings") {
			input "InsteonIP", "text", title: "Hub IP Address", description: "Insteon Hub IP Address", required: true, displayDuringSetup: true
			input "InsteonPort", "text", title: "Hub Port", description: "Insteon Hub port", required: true, defaultValue: "25105", displayDuringSetup: true
			input "InsteonHubUsername", "text", title: "Username", description: "Insteon Hub username (found in app)", required: true, displayDuringSetup: true
			input "InsteonHubPassword", "password", title: "Password", description: "Insteon Hub password (found in app)", required: true, displayDuringSetup: true
		}

		if (installed) {
			section("Insteon Switch") {
				href "switchSettings", title: "Switches", description: "Tap here to add or manage Insteon Switches", 
					image: "http://www.freeiconspng.com/uploads/light-switch-png-icon-1.png", required: false, page: "switchSettings"
			}
		}
	}
}

def switchSettings() {
	dynamicPage(name: "switchSettings", title: "Insteon Switches", install: false, uninstall: false) {
		section(){
			app(name: "childSwitches", appName: "Insteon Switch Child", namespace: "HBOdR", title: "Add a new switch...", 
			image: "http://www.freeiconspng.com/uploads/light-switch-png-icon-1.png", multiple: true)
		}
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
}
