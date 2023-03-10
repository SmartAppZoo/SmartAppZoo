/**
 *  toPIOMF
 *
 *  Copyright 2020 Joey Langan
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
    name: "toPIOMF",
    namespace: "langanjp",
    author: "Joey Langan",
    description: "Sends from SmartThings to PI OMF",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section ("Sensors that should go to PI") {
		input "batteries", "capability.battery", title: "Batteries", required: false, multiple: true
		input "motions", "capability.motionSensor", title: "Motion Sensors", required: false, multiple: true
		input "presences", "capability.presenceSensor", title: "Presence Sensors", required: false, multiple: true
		input "switches", "capability.switch", title: "Switches", required: false, multiple: true
		input "switchLevels", "capability.switchLevel", title: "Switch Levels", required: false, multiple: true        
		input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required: false, multiple: true
		input "waterSensors", "capability.waterSensor", title: "Water Sensors", required: false, multiple: true
	}
    section ("PI OMF Details") {
		input "omfendpoint", "text", title: "PI Web API OMF Endpoint (https://piwebapiserver/piwebapi/omf)", required: true
		input "basicauth", "text", title: "base64 encoded username password", require: true 
        input "typeprefix", "text", title: "Prefix for OMF type (include delimiter)", require: false
        input "streamprefix", "text", title: "Prefix for OMF container (include delimiter)", require: false
	}
}

def installed() {
//	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
//	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {

//	subscribe(batteries, "battery", batteryHandler)
//	subscribe(motions, "motion", motionHandler)
//	subscribe(presences, "presence", presenceHandler)
//	subscribe(switches, "switch", switchHandler)
//	subscribe(switchLevels, "switchLevel", switchLevelHandler)
//  subscribe(temperatures, "temperature", temperatureHandler)
//  subscribe(waterSensors, "water", waterHandler)

//  initdevice(sensor, capattr, numtype, numformat)
	initdevice(batteries, "battery", "integer", "int32")
	initdevice(motions, "motion", "integer", "int32")
	initdevice(presences, "presence", "integer", "int32")
	initdevice(switches, "switch", "integer", "int32")
	initdevice(switchLevels, "switchLevel", "integer", "int32")
    initdevice(temperatures, "temperature", "number", "float32")
	initdevice(waterSensors, "water", "integer", "int32")		

	log.debug "Initialized with settings: ${settings}"
}


def initdevice(sensor, capattr, numtype, numformat) {
	def location = location.name
//	def containerid = ""
    def handlername = capattr + "Handler"
    
	subscribe(sensor, capattr, handlername)
	createomftype(numtype, numformat)
    createomfcontainer(sensor, capattr, numtype, numformat)
}

def createomftype(numtype, numformat) {
	def obody = []
	def typeid = "$typeprefix$numtype.$numformat" 

	obody = new groovy.json.JsonOutput().toJson(
		[[
		    "id": typeid,
		    "classification": "dynamic",
		    "type": "object",
		    "properties": [
		        "Timestamp": [
		            "type": "string",
		            "format": "date-time",
		            "isindex": true
		        ],
		        "value": [
		            "type": [numtype, "null"],
		            "format": numformat
		        ]
			]
		]]
	)

	log.debug obody
	if(state[typeid] >= 1) { 
		log.debug "omf type already created"
	} else{ 
		piomfWriter("create", "type", obody, typeid)
	}
}

def createomfcontainer(sensor, capattr, numtype, numformat) {
	def obody = []
	def typeid = "$typeprefix$numtype.$numformat" 
	sensor.each {sen ->	
		def containerid = "$streamprefix$location.$sen.$capattr"
		//log.debug containerid      
        obody = new groovy.json.JsonOutput().toJson(
			[[
		    	"id": containerid,
		    	"typeid": typeid
			]]            
		)
		log.debug obody
		log.debug state[containerid]
		if(state[containerid] >= 1) { 
			log.debug "omf container already created"
		} else { 
			piomfWriter("create", "container", obody, containerid)
		}
	}
}

// Event Handlers
def noHandler(evt) {
	log.debug "no Handler"
}

// for individual handlers, when passing to the genericHandler be sure to pass the data type ("real" (real number), "int" (integer), "dig" (digital state or string)) and the conversion to be used

def batteryHandler(evt) {
	genericHandler(evt,"int") {it.toInteger()}
}

def motionHandler(evt) {
	genericHandler(evt,"int") {it == "active" ? 1 : 0}
}

def presenceHandler(evt) {
	genericHandler(evt,"int") {it == "present" ? 1 : 0}
}

def switchHandler(evt) {
	genericHandler(evt,"int") {it == "on" ? 1 : 0}
}

def switchLevelHandler(evt) {
	genericHandler(evt,"int") {it.toInteger()}
}

def temperatureHandler(evt) {
	genericHandler(evt,"real") {it.toFloat()}
}

def waterHandler(evt) {
	genericHandler(evt,"int") {it == "wet" ? 1 : 0}
}

def genericHandler(evt,valtype,Closure convert) {
	log.debug "genericHandler"
    
	def location = location.name
	def capattr = evt.name
    def sen = evt.displayName
	def containerid = "$streamprefix$location.$sen.$capattr"	

	def evtValue = convert(evt.value)
    def evtTime = evt.isoDate // evt.date.format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")).toString()



// Get into UFL input format: tagname, timestamp, real value, integer value, digital state or string value, status, questionable
	def datarecord = tagName + "," + evtTime + "," + (valtype == "real" ? evtValue : "") + "," + (valtype == "int" ? evtValue : "") + "," + (valtype == "dig" ? evtValue : "") + "," + "," 
	log.debug containerid
    def obody = new groovy.json.JsonOutput().toJson(
		[[
		    "containerid": containerid,
		    "Values": [[
                "Timestamp": evtTime,
                "value": evtValue
            ]]
		]]            

	)
// send to PI OMF
	log.debug obody
    piomfWriter("create", "data", obody, null)
}

def piomfWriter(oaction, omessagetype, obody, ostate) {

    def params = [
		uri: omfendpoint,
//		contentType: "application/json",
		contentType: "text",
		headers: [
    		Authorization: "Basic " + "$basicauth",
            "Content-Type": "application/json",
			omfversion: "1.1",
			action: oaction,
			messageformat: "json",
			messagetype: omessagetype
		],
		body: obody
   ]

    try {
		httpPost(params) { resp ->
			log.debug resp.status
            log.debug resp.getData().getText()
            if (resp.status == 201) {state[ostate] = 1}
		}
    } catch (e) {
        log.debug "something went wrong: $e"
    }  
  
}


// minuteHandler not used for now
def minuteHandler() {
	log.debug "minute handler"
	def cs = batteries.currentState("battery")

	def evtDate = Date.parse("E MMM dd H:m:s z yyyy", cs.date.join(','))
	def evt = [name: cs.name.join(','), displayName: cs.displayName.join(','), location: cs.location.join(','), value: cs.value.join(','), date: evtDate]
    
//	batteryHandler(evt)
} 
