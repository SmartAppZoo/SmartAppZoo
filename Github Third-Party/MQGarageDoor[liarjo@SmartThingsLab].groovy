/**
 *  MQGarageDoor
 *
 *  Copyright 2020 Juan Pablo Garcia
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
    name: "MQGarageDoor",
    namespace: "jpgg",
    author: "Juan Pablo Garcia",
    description: "Open and Close Garage door using myQ and Custome API in Azure",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "APIHOST"
    appSetting "code"    
    appSetting "clientId"    
    appSetting "serialNumber"


}


preferences {
     section("Select your Garage switch") {
     	input "theSwitch", "capability.switch", title: "Virtual Switch", required: true
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
	//1. Sync Switch status
    SyncSwitchStatus()
    //2. subscribe switch events.
    subscribe(theSwitch, "switch", switchHandler)
}

def switchHandler(evt) {
	def currentState=updateCurrentDoorState()
    if (evt.value == "on") {
    	if (currentState=="closed") {
        	//Open Garage
            OpenDoor()
            //Sync Status after 5 minutes
            runIn(60*5, updated)
        }
    } else {
    	//event off
        if (currentState=="open") {
        	//Open Garage
            CloseDoor()
            //Sync Status after 5 minutes
            runIn(60*5, updated)
        }
    }
  log.debug "Event: $evt last state $currentState new state $theSwitch.currentSwitch"
}



def SyncSwitchStatus() {
    def currentState=updateCurrentDoorState()
    if (currentState=="closed"){
    	theSwitch.off()
    } else if (currentState=="open") {
    	theSwitch.on()
    }
    log.debug "Switch synced, Timer Current State $currentState and Switch current state $theSwitch.currentSwitch"
}

def updateCurrentDoorState() {
    def params = [
        uri: appSettings.APIHOST,
        path: "/api/DoorStatus",
        query: [
            "serialNumber": appSettings.serialNumber,
            "code":appSettings.code,
			"clientId":appSettings.clientId            
        ]
    ]
    return HTTPGet(params).data.deviceState
}

def OpenDoor() {
    def params = [
        uri: appSettings.APIHOST,
        path: "/api/DoorOpen",
        query: [
            "serialNumber": appSettings.serialNumber,
            "code":appSettings.code,
			"clientId":appSettings.clientId            
        ]
    ]
    HTTPGet(params)
}

def CloseDoor() {
    def params = [
        uri: appSettings.APIHOST,
        path: "/api/DoorClose",
        query: [
            "serialNumber": appSettings.serialNumber,
            "code":appSettings.code,
			"clientId":appSettings.clientId            
        ]
    ]
   HTTPGet(params)
}

def HTTPGet(params) {
	 try {
        httpGet(params)  { resp ->
        	return resp
    	}
    } catch (e) {
        log.error "something went wrong: $e"
        log.error params
    }
}