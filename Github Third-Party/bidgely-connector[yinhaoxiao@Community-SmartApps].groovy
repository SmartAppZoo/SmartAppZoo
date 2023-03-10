/**
 *  Bidgely Connector
 *
 *  Copyright 2015 Michael Kurtz
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
    name: "Bidgely Connector",
    namespace: "bigpunk6",
    author: "Michael Kurtz",
    description: "Upload Power Meter data to Bidgely",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("House Power Meter") {
		input "housePower", "capability.powerMeter", title: "House Power Meters", required:false, multiple: true
	}
    section("Idividule circuit Power Meters") {
		input "circuitPower", "capability.powerMeter", title: "Idividule circuit Power Meters", required:false, multiple: true
	}
    section ("Bidgely API") {
		input "apiUrl", "text", title: "API URL", required:true
        input "uploadCount", "number", title: "Upload after this many events", required:true
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
    state.body = []
	subscribe(housePower, "power", powerEvent)
    subscribe(housePower, "energy", powerEvent)
    subscribe(circuitPower, "power", circuitPowerEvent)
}

def powerEvent(evt) {
	sendEvent(evt, 0)
}

def circuitPowerEvent(evt) {
	sendEvent(evt, 10) { it.toString() }
}

private sendEvent(evt, meterType) {
    def streamType
    def streamUnit
    def streamDescription
    def timeStamp = now().toString() [0..9]
    
    if (evt.name == "energy") {
        streamType = "CurrentSummationDelivered"
        streamUnit = "kWh"
        streamDescription = "Billing Stream"
        log.debug "$evt.displayName $evt.name is $evt.value$streamUnit Type:$meterType"
	    def postApi = [
    	    uri: apiUrl,
            headers: ['Content-Type': 'application/xml'],
    		body:'<upload version="1.0">'+
                 '<meters>'+
                    '<meter id="' + evt.deviceId + '" model="API" type="' + meterType + '" description="' + evt.displayName + '">'+
                       '<streams>'+
                          '<stream id="' + streamType + '" unit="' + streamUnit + '" description="' + streamDescription + '">'+
                             '<data time="' + timeStamp + '" value="' + evt.value + '" />'+
                          '</stream>'+
                       '</streams>'+
                    '</meter>'+
                 '</meters>'+
              '</upload>'
            ]
        
        httpPost(postApi) { response ->
        log.info "httpPost responce:${response.status}"
	    }
    } else if (evt.name == "power") {
        streamType = "InstantaneousDemand"
        streamUnit = "W"
        streamDescription = "Real-Time Demand"
        log.info "$evt.displayName $evt.name is $evt.value$streamUnit Type:$meterType"
        
        state.body << '<data time="' + timeStamp + '" value="' + evt.value + '" />'
        log.debug state.body.size()
        if (state.body.size() >= uploadCount) {
            def postBody = state.body.collect { it }.join()
            //log.debug postBody
            log.info "Posting last ${uploadCount} events to ${apiUrl}"
            state.body = []
            def postApi = [
    	    uri: apiUrl,
            headers: ['Content-Type': 'application/xml'],
	    	body:'<upload version="1.0">'+
                 '<meters>'+
                    '<meter id="' + evt.deviceId + '" model="API" type="' + meterType + '" description="' + evt.displayName + '">'+
                       '<streams>'+
                          '<stream id="' + streamType + '" unit="' + streamUnit + '" description="' + streamDescription + '">'+
                             postBody +
                          '</stream>'+
                       '</streams>'+
                    '</meter>'+
                 '</meters>'+
              '</upload>'
            ]
            httpPost(postApi) { response ->
            log.info "httpPost responce:${response.status}"
	        }
        }
    }
}