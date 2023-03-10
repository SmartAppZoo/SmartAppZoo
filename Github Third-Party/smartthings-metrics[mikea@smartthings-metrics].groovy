/**
 *  smartthings-metrics
 *
 *  Copyright 2017 Mike Aizatsky
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
    name: "SmartThings Metrics",
    namespace: "mikea",
    author: "Mike Aizatsky",
    description: "Collect measurements from devices and publish to the cloud.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
   section("Collect Metrics") {
        input "co2_devices", "capability.carbonDioxideMeasurement", required: false, title: "Carbon Dioxide", multiple: true
        input "i_devices", "capability.illuminanceMeasurement", required: false, title: "Illuminance", multiple: true
        input "ph_devices", "capability.phMeasurement", required: false, title: "pH", multiple: true
        input "rh_devices", "capability.relativeHumidityMeasurement", required: false, title: "Relative Humidity", multiple: true
        input "t_devices", "capability.temperatureMeasurement", required: false, title: "Temperature", multiple: true
        input "v_devices", "capability.voltageMeasurement", required: false, title: "Voltage", multiple: true
   }
   
   section("Publish Metrics") {
        input "datadrop_bin", type: "text", required: true, title: "Wolfram Data Drop Bin ID"
        //input "period", type: "number", required: true, title: "Period, min", defaultValue: 15
   }
}

def installed() {
	log.debug "[metrics] installed settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "[metrics] updated settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "[metrics] initializing"
	log.debug "[metrics] unscheduling..."
	unschedule(publishMetrics)
	log.debug "[metrics] scheduling..."
    schedule("0 0/15 * 1/1 * ? *", publishMetrics)
    log.debug "[metrics] initialized, force publishing"
    publishMetrics()
}

def updateParams(query, device, parameterName) {
	query[device.displayName] = device.currentValue(parameterName)
}

def publishMetrics() {
	log.debug "[metrics] publishMetrics binid: ${datadrop_bin}"
    
    if (datadrop_bin == "") {
    	log.error "[metrics] error: datadrop bin not set"
        return
    }
    
    def query = ["bin": datadrop_bin]

	co2_devices.each{ d -> updateParams(query, d, "carbonDioxide") }
	i_devices.each{ d -> updateParams(query, d, "illuminance") }
	ph_devices.each{ d -> updateParams(query, d, "pH") }
	rh_devices.each{ d -> updateParams(query, d, "humidity") }
	t_devices.each{ d -> updateParams(query, d, "temperature") }
    v_devices.each{ d -> updateParams(query, d, "voltage") }
	query["bin"] = datadrop_bin

    def params = [
        uri:  'https://datadrop.wolframcloud.com/api/v1.0/Add',
        query: query
    ]
    
    log.debug "[metrics] publishing ${params}"

    try {
        httpGet(params) { resp ->
            log.debug "[metrics] response status: ${resp.status}"
        }
    } catch (e) {
        log.error "[metrics] exception: $e"
    }

}
