/**
 *  Insteon Status Grabber
 *
 *  Copyright 2017 Brandt Barretto
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
 * VERSION HISTORY
 * 2017-09-19 : 1.0 BETA : Initial Version
 * 2017-09-27 : 1.1 BETA : First version published on GitHub
 * 2017-09-29 : 1.2 : Code cleanup.  Removed hash map construct, so user will now have to put actual ST device name in REST call in Indigo trigger.
 * 
 */
definition(
    name: "Insteon Status Grabber",
    namespace: "brbarret",
    author: "Brandt Barretto",
    description: "Exposing REST endpoint so that an server running Indigo Domotics can send REST calls with device status to SmartThings.  This will keep the status of any devices managed in both places in sync",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
        section("Insteon Interface") {
            // TODO: put inputs here

            input "theSwitches", "capability.switch", title: "Switches", required: true, multiple: true 
            //input "theDimmer", "capability.switchLevel", title: "Dimmer Switches", required: true, multiple: true 
	}
}

mappings {
  path("/statusreport") {
    action: [
      GET: "showAllStates"
    ]
  }
  path("/statusreport/:device/:state") {
    action: [
      GET: "showState"
    ]
  }
  path("/insteonControl/:device/:state") {
  	action: [
    	GET: "controlDevice"
    ]
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
	// TODO: subscribe to attributes, devices, locations, etc.
	//state.myDevices = [:]
    
}

// TODO: implement event handlers

def showAllStates () {
	def resp = []
    theSwitches.each {
      resp << [name: it.displayName, value: it]
    }
    return resp}

def showState() {
    
    //log.debug("Device parameter: ${params.device}, deviceMap -> ${deviceMap()}")
    // get the device from the list
    def thisDevice = theSwitches.find{it.name == params.device} 
    def deviceState = params.state
    log.debug("Device info: ${thisDevice?.getDeviceNetworkId()}")
    log.debug("The device ${thisDevice} is ${deviceState}")
    
    thisDevice.refresh("${deviceState}")
}

def controlDevice() {}

def manageCallback() { return true }