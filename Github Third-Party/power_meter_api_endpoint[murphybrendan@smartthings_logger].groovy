/**
 *  Power Meter API Endpoint
 *
 *  Copyright 2015 Brendan Murphy
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
    name: "Power Meter API Endpoint",
    namespace: "bmurphy592",
    author: "Brendan Murphy",
    description: "Exposes endpoints for subscribing to power information",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {

    /*section ("Power meters that you want to keep track of:") {
        input "meters", "capability.powerMeter", multiple: true, required: true
    }*/
    section ("Switches that you want to keep track of:") {
        input "switches", "capability.switch", multiple: true, required: true
    }
}

def installed() {
    log.trace "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.trace "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

/**
 *	Initialize the script
 *
 *	Create the scheduled event subscribe to the power event
 */
def initialize() {
    state.debug = true
    subscribe(switches, "switch", switchHandler)
}

mappings {
  path("/subscribe") {
    action: [
      PUT: "subscribeToMeters"
    ]
  }
}

def subscribeToMeters() {
	log.debug "adding loggeruri: ${params.loggeruri}"
	state.loggeruri = params.loggeruri
}

def switchHandler(evt) {
	if (state.debug) {
        log.debug "switch evt: ${evt}"
        log.debug "state: ${state}"
    }
    
    if (state.loggeruri != null) {
    	def params = [
    		uri: state.loggeruri,
        	body: [
	        	time: evt.date.toString(),
    	        id: evt.deviceId,
        	    state: evt.value,
        	]
    	]
    
    	try {
    		httpPostJson(params) { resp ->
        		resp.headers.each {
            		log.debug "${it.name} : ${it.value}"
        		}
        		log.debug "response contentType: ${resp.    contentType}"
    		}
		} catch (e) {
    		log.debug "something went wrong: $e"
		}
    }
}

/**
 *	Power event handler
 *
 *	Called when there is a change in the power value.
 *
 *	evt		The power event
 */
def powerHandler(evt) {
    if (state.debug) {
        log.debug "power evt: ${evt}"
        log.debug "state: ${state}"
    }

    def currPower = meter.currentValue("power")
    log.trace "Power: ${currPower}W"
    
    def params = [
    	uri: state.loggeruri,
        body: [
        	time: evt.date.getTime(),
            id: evt.deviceId,
            state: currPower,
        ]
    ]
    
    try {
    	httpPostJson(params) { resp ->
        	resp.headers.each {
            	log.debug "${it.name} : ${it.value}"
        	}
        	log.debug "response contentType: ${resp.    contentType}"
    	}
	} catch (e) {
    	log.debug "something went wrong: $e"
	}
}