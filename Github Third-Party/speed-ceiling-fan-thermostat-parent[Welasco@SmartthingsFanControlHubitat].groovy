/*
   Virtual Thermostat for 3 Speed Ceiling Fan Control
   Copyright 2020 Hubitat, Victor Santana
   
   This smartapp provides automatic control of Low, Medium, High speeds of a ceiling fan using 
   any temperature sensor with optional motion override. 
   It requires two hardware devices; any temperature sensor and a dimmer type smart fan controller
   such as the GE 12730 or Leviton VRF01-1LX. Incorporates contributions from:
 
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at: www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.
  
 */
definition(
    name: "Speed Ceiling Fan Thermostat",
    namespace: "Speed Ceiling Fan Thermostat",
    author: "Victor Santana",
    description: "Automatic control for Speed Ceiling Fan using Low, Medium, High speeds with any temperature sensor.",
    category: "My Apps",
    singleInstance: true,
	iconUrl: "https://raw.githubusercontent.com/Welasco/SmartthingsFanControlHubitat/master/3scft125x125.png", 
   	iconX2Url: "https://raw.githubusercontent.com/Welasco/SmartthingsFanControlHubitat/master/3scft250x250.png",
	iconX3Url: "https://raw.githubusercontent.com/Welasco/SmartthingsFanControlHubitat/master/3scft250x250.png"
)

preferences {
    page(name: "parentPage")
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: true, uninstall: true) {
        section("Create a new fan automation.") {
            app(name: "childApps", appName: "SpeedCeilingFanThermostatChild", namespace: "Speed Ceiling Fan Thermostat", title: "New Fan Automation", multiple: true)
        }
    }
}

