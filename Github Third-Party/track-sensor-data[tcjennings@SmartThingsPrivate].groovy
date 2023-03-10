/**
 *  Track Sensor Data
 *
 *  Copyright 2016 Toby Jennings
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
    name: "Track Sensor Data",
    namespace: "tcjennings",
    author: "Toby Jennings",
    description: "This smart app subscribes to sensors and pushes the updates to a local (to the hub) Elasticsearch database.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
) {
	appSetting "dbHost"
	appSetting "dbIndex"
	appSetting "dbType"
    appSetting "dbPort"
}


preferences {
    section("Select Temperature Sensors") {
        input "sensors", "capability.temperatureMeasurement", required: true,  multiple: true, title: "Temperature Sensors:"
    }
    section("Select Humidity Sensors") {
        input "humsensors", "capability.relativeHumidityMeasurement", required: true,  multiple: true, title: "Humidity Sensors:"
    }

    section("Select Switches") {
        input "switches", "capability.switch", required: true,  multiple: true, title: "Switches:"
    }

    section("Select Contact") {
            input "contact", "capability.contactSensor", title: "pick a contact sensor", multiple: true
    }

	section("Select Acceleration"){
	    input "accelerationSensor", "capability.accelerationSensor", multiple:true, title: "Sensors:"
    }
	section("Select Battery"){
	    input "battery", "capability.battery", multiple:true, title: "Sensors with Battery:"
    }
	section("Select Power Meters"){
	    input "power", "capability.powerMeter", multiple:true, title: "Power Meter Sensors:"
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
	subscribe(sensors, "temperature", eventHandler)
    subscribe(humsensors, "humidity", eventHandler)
    subscribe(switches, "switch", eventHandler)
    subscribe(accelerationSensor, "acceleration", eventHandler)
    subscribe(contact, "contact", eventHandler)
    subscribe(battery, "battery", eventHandler)
    subscribe(power, "power", eventHandler)

}

def issueLocalCommand(command) {
  sendHubCommand(command)
  return command
}

def sendInflux(data) {
	def headers = [:]
    headers.put("HOST", "${appSettings.dbHost}:8086")
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    
    def body = ""
    body = body.concat(data.event.sensor)
    body = body.concat(",name=").concat(data.name.replace(" ","\\ "))
    if (data.event.unit) {
    	body = body.concat(",units=").concat(data.event.unit)
    }
    body = body.concat(",source=").concat("${data.source}")
    body = body.concat(" ")
    body = body.concat("value=").concat( data.event.binValue == null ? "${data.event.value}" : "${data.event.binValue}" )
    body = body.concat(" ")
    body = body.concat("${data.date}")
    log.debug "${body}"
    
    def result = new physicalgraph.device.HubAction(
    	method: "POST",
        path: "/write?db=${appSettings.dbIndex}&precision=ms",
        headers: headers,
        body: body
    )
    
    return( issueLocalCommand(result) )
}

def eventHandler(evt) {

	def evtValue
    def binValue = null
	try{
		evtValue = evt.doubleValue
	} catch (e) {
		evtValue =  evt.stringValue
	}
    
    switch(evtValue){
    	case ["open","inactive","off"]:
        	binValue = 0.0
            break
        case ["closed","active","on"]:
        	binValue = 1.0
            break
    }
    //log.debug "${binValue} : ${evtValue}"

	try {
		def data = [
        	date: evt.date.getTime(),
            name: evt.displayName,
            source: evt.source,
            event: [
            	sensor: evt.name,
                value: evtValue,
                binValue: binValue,
                unit: evt.unit
            ]
        ]
        //log.debug "${data}"
        
        sendInflux(data)
    } catch (e) {
       log.debug "Trying to build data for ${evt.name} threw an exception: $e"
    }
}