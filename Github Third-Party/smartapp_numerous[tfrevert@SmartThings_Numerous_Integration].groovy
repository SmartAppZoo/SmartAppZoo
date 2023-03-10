/**
 *  Numerous App Integration
 *
 *	http://numerousapp.com/
 *
 *  Copyright 2015 TJ Frevert
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
    name: "Numerous App Integration",
    namespace: "tfrevert",
    author: "TJ Frevert",
    description: "Integrate SmartThings sensor values with the Numerous app.",
    category: "My Apps",
    iconUrl: "http://i.imgur.com/ef3zHOC.png",
    iconX2Url: "http://i.imgur.com/ef3zHOC.png",
    iconX3Url: "http://i.imgur.com/ef3zHOC.png")


preferences {
	section("Select a sensor") {
        input "meter", "capability.powerMeter", title: "Power Meter", required: true, multiple: false
    }
    section("Configure your Numerous App Settings") {
        input "API_Key", "text", title: "API Key", required: true
        input "Metric_ID", "text", title: "Metric ID", required: true
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
	subscribe(meter, "power", updateCurrentValue)
}

def updateCurrentValue(evt) {

	log.debug "Updating Numerous Value: ${meter} to ${evt.value}"

    def params = [
        uri: "https://" + API_Key + "@api.numerousapp.com",
        path: "/v2/metrics/" + Metric_ID + "/events",
        body: ["value": evt.value]
    ]

    try {
        httpPostJson(params) { resp ->
            log.debug "response data: ${resp.data}"
            log.debug "response contentType: ${resp.contentType}"
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }

} 
