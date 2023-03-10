/**
 *  Test2
 *
 *  Copyright 2018 Bill Brazeal
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
    name: "Test2",
    namespace: "billbrazeal",
    author: "Bill Brazeal",
    description: "Test for URL call",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

    def hub = location.hubs[0]

    log.debug "id: ${hub.id}"
    log.debug "zigbeeId: ${hub.zigbeeId}"
    log.debug "zigbeeEui: ${hub.zigbeeEui}"

    // PHYSICAL or VIRTUAL
    log.debug "type: ${hub.type}"

    log.debug "name: ${hub.name}"
    log.debug "firmwareVersionString: ${hub.firmwareVersionString}"
    log.debug "localIP: ${hub.localIP}"
    log.debug "localSrvPortTCP: ${hub.localSrvPortTCP}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	MakeURLcall()
}

def MakeURLcall(){
    def params = [
        uri: "http://24.197.52.218:8099",
        path: "/api/HomeAutomation.HomeGenie/Automation/Programs.Run/1061"
    ]
    
    try {
    	log.debug "Parameters: ${params}"
        httpGet(params) { resp ->
            resp.headers.each {
               log.debug "${it.name} : ${it.value}"
            }
 //           log.debug "response contentType: ${resp.contentType}"
            log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

def MakeURLcall2() {
    def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: "/api/HomeAutomation.HomeGenie/Automation/Programs.Run/1061",
    headers: [
        HOST: "192.168.0.210"
    ])

    log.debug hubAction
 //   result C0A800D2:50

    return hubAction
}