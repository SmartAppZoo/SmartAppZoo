/**
 *  Internet Access By MAC
 *
 *  Copyright 2017 Patrick Powell
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
    name: "Block Internet Access By MAC",
    namespace: "patrickkpowell",
    author: "Patrick Powell",
    description: "Block Internet access by MAC address",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Block Internet Access") {
		// TODO: put inputs here
        //input "MAC File", "text", "title": "MAC Address File", multiple: false, required: true
        input "switches", "capability.switch", multiple: false
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
    // TODO: implement event handlers
    subscribe(switches, "switch", switchesHandler)
}

def switchesHandler(evt) {
	log.debug "Current Switch: " + switches.currentValue("switch")
    def mypath
    if ( switches.currentValue("switch") == "on" ) {
    	//log.debug "If worked!"
        mypath = "/cgi-bin/block.cgi"
    }
    if ( switches.currentValue("switch") == "off" ) {
    	//log.debug "If worked!"
        mypath = "/cgi-bin/allow.cgi"
    }
    log.debug "one of the configured switches changed states - calling " + mypath
    	def params = [
    	uri: "http://server.powellcompanies.com",
    	path: mypath,
        headers: [
              'Accept': '*/*',
              'DNT': '1',
              'Accept-Encoding': 'plain',
              'Cache-Control': 'max-age=0',
              'Accept-Language': 'en-US,en,q=0.8',
              'Connection': 'keep-alive',
              'Host': 'smartthings.powellcompanies.com',
              'Referer': 'https://smartthings.powellcompanies.com',
              'Connection-Control': 'AlsaireaLAERILeaeAejkejIAjrieOJAOEIroaOIRRIAOIfhuefhiuaeiufaieufuUHUIAHUHUEIUHAEFUHIFUFHEaufUHifuahUAEhfiUfhaefiIFUhUfahfjhfhsvgejf',
              'User-Agent': 'SmartThings/1.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
              ]
	]

	try {
    	httpGet(params) { resp ->
        	// iterate all the headers
        	// each header has a name and a value
        	resp.headers.each {
    	       log.debug "${it.name} : ${it.value}"
	        }

    	    // get an array of all headers with the specified key
	        def theHeaders = resp.getHeaders("Content-Length")

    	    // get the contentType of the response
	        log.debug "response contentType: ${resp.contentType}"

        	// get the status code of the response
        	log.debug "response status code: ${resp.status}"

    	    // get the data from the response body
	        log.debug "response data: ${resp.data}"
    	}
	} catch (e) {
    	log.error "something went wrong: $e"
	}
}