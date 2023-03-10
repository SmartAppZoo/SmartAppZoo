/**
 *  Copyright 2016 Nicholas Wilde
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
 *	MAC Address Monitor
 *
 *	Author: Nicholas Wilde, Based on original works by Stuart Buchanan & midyear66 with thanks
 *	This app recieves a HHTP Get request from a Linux client when a network device connects & disconnects from the Wifi network
 *	Date: 2016-02-03 v1.0 Initial Release
 */
definition(
    name: "MAC Address Monitor",
    namespace: "nicholaswilde",
    author: "Nicholas Wilde",
    description: "Triggers Virtual Switch when HTTP GET Request is recieved",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics18-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics18-icn@2x.png")

preferences {
    section(title: "Select Devices") {
        input "virtualSwitch",
        "capability.Switch",
        title: "Select Virtual Switch",
        required: true,
        multiple:false
    }
}

def installed() {
	createAccessToken()
	getToken()
	DEBUG("Installed Phone with rest api: $app.id")
    DEBUG("Installed Phone with token: $state.accessToken")
    DEBUG("Installed with settings: $virtualSwitch.name")
}

def updated() {
	DEBUG("Updated Phone with rest api: $app.id")
    DEBUG("Updated Phone with token: $state.accessToken")
}

mappings {
  path("/device/on") {
    action: [
      GET: "switchOn"
    ]
  }
  path("/device/off") {
    action: [
      GET:"switchOff"
    ]
  }
}


// Callback functions
def getSwitch() {
    // This returns the current state of the switch in JSON
    return virtualSwitch.currentState("switch")
}

def switchOn() {
	DEBUG("on")
	virtualSwitch.on();
}

def switchOff() {
	DEBUG("off")
	virtualSwitch.off();
}

def getToken(){
if (!state.accessToken) {
		try {
			getAccessToken()
			DEBUG("Creating new Access Token: $state.accessToken")
		} catch (ex) {
			DEBUG("Did you forget to enable OAuth in SmartApp IDE settings")
            DEBUG(ex)
		}
	}
}

private def DEBUG(message){
	log.debug message
}