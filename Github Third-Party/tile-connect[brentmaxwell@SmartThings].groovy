/**
 *  Tile (Connect)
 *
 *  Copyright 2019 Brent Maxwell
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
    name: "Tile (Connect)",
    namespace: "thebrent",
    author: "Brent Maxwell",
    description: "Connects to Tile Bluetooth trackers",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  page(name: "auth", title: "Tile (Connect)", nextPage:"", content:"authPage", uninstall: true, install:true)
  page(name: "deviceDiscovery", title:"Device Setup", content:"deviceDiscovery", refreshTimeout:5);
}

def authPage() {
  return dynamicPage(name: "auth", title: "Login", nextPage: "deviceDiscovery", uninstall:uninstallAllowed) {
    section(){
      paragraph "Enter your email and password for Tile"
      input "email", "text", title: "Email", required: true
      input "password", "password", title: "Password", required: true
    }
  }
}

def deviceDiscovery() {

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
	// TODO: subscribe to attributes, devices, locations, etc.
}

def apiUrl() { "https://production.tile-api.com/api/v1" }
def apiApplicationId() { "ios-tile-production" }
def apiAppVersion() { "2.31.0" }
def apiAppLocale() { "en-US" }
def clientId() { "f7d7d792-57f0-4bc4-a28d-2120744a5856" }

def getDevices() {

}