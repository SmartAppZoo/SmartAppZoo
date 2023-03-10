/**
 *  Aidi Home
 *
 *  Copyright 2017 Xiaojing Ma
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

import grails.converters.JSON

definition(
    name: "Automatic",
    namespace: "randymxj",
    author: "Xiaojing Ma",
    description: "Webhook Endpoint of Automatic",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences {
	section("Allow these things to be exposed via JSON...") {
	}
}

mappings {
  path("/automatic") {
    action: [
      POST: "updateAutomatic"
    ]
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
    subscribe(presences, "presence", dataChangeHandler)
}

/* Update Automatic Device */
def updateAutomatic() {
	def deviceNetworkId = request.JSON?.deviceNetworkId
    def type = request.JSON?.type
        
    def thePresence = presences.find { it.deviceNetworkId == deviceNetworkId }
    if (!thePresence) {
    	return httpError(404, "Can't find device with deviceNetworkId $deviceNetworkId")
    }
        
    thePresence.updatePresence(request.JSON) 
}

/* Event data handler */
def dataChangeHandler(evt) {

}
