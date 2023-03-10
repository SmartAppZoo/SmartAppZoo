/**
 *  SplunkLogging
 *
 *  Copyright 2017 Home
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
import groovy.json.*

definition(
    name: "SplunkLogging",
    namespace: "alacercogitatus",
    author: "Home",
    description: "Send data to Splunk HEC.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

	section("Splunk HEC Settings") {
    	input "splunk_hec_url", "text", title: "Splunk HEC URI"
        input "splunk_hec_token", "password", title: "HEC Token"
    }
    section("Wink Settings") {
    	input "wink_username", "text", title: "Wink Username", required: false
        input "wink_password", "password", title: "Wink Password", required: false
    }
    section("Log these Devices:") {
        input "consume_wink", "bool", title: "Wink Hub", required: false
        input "presences", "capability.presenceSensor", title:"Presence", multiple: true, required: false
    	input "switches", "capability.switch", title: "Switches", multiple: true, required: false
   		input "levels", "capability.switchLevel", title: "Levels", multiple: true, required: false
    	input "motions", "capability.motionSensor", title: "Motions", multiple: true, required: false
    	input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", multiple: true, required: false
    	input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", multiple: true, required: false
    	input "contacts", "capability.contactSensor", title: "Contacts", multiple: true, required: false
    	input "alarms", "capability.alarm", title: "Alarms", multiple: true, required: false
		input "indicators", "capability.indicator", title: "Indicators", multiple: true, required: false
    	input "codetectors", "capability.carbonMonoxideDetector", title: "CO Detectors", multiple: true, required: false
    	input "smokedetectors", "capability.smokeDetector", title: "Smoke Detectors", multiple: true, required: false
    	input "waterdetectors", "capability.waterSensor", title:"Water Sensors", multiple: true, required: false
        input "energymeters", "capability.energyMeter", title:"Energy Meters", multiple: true, required: false
        input "battery", "capability.battery", title:"Batteries", multiple: true, required: false
        input "contactsensor", "capability.contactSensor", title:"Contact Sensors", multiple: true, required: false
        input "acceleration", "capability.accelerationSensor", title:"Acceleration Sensors", multiple: true, required: false
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
	doSubscriptions()
    // Get Device Every 1 Minutes.
    runEvery1Minute(evtHandler)   
    runEvery1Minute(gatherWink)
}

def sendWink(dvc){
	dvc["data_type"] = "wink"
    def cdvc = new JsonBuilder(dvc).toPrettyString()
	def json2 = "{ \"sourcetype\": \"wink_via_smartthings\",\"event\" : ${cdvc} }"
    def json = json2.replaceAll(/null/, "\"\"")
    json = json.replaceAll(/true/, "\"true\"")
    json = json.replaceAll(/false/, "\"false\"")
    log.debug "Single Device to Splunk: ${json}"
    send_to_splunk(json)
}

def gatherWink(){
	log.info "starting wink collection" 
    def wink_api = "https://api.wink.com/users/me/wink_devices"
    def client_id = "<get_one>"
    def client_secret = "<get_one>"
    def url_to_auth = "https://api.wink.com/oauth2/authorize?client_id=${client_id}&redirect_uri=http://127.0.0.1&response_type=code&state=mystate"
    def token = "<gotta_auth>"
    def access_token = ""
    def refresh_token = ""
    def params  = [
    		uri: wink_api,
            headers: [ "Content-Type": "application/json", 
            		   "Authorization": "Bearer ${access_token}"]
        ]
        try {
            httpGet(params) { resp ->
            resp.headers.each {
                log.info "${it.name} : ${it.value}"
            }
            log.info "wink response contentType: ${resp.contentType}"
            def myWinkData = resp.data.get("data")
            log.info "Data : ${myWinkData}"
            myWinkData.each{ wDvc -> sendWink(wDvc) }
            }
        } catch ( groovyx.net.http.HttpResponseException ex ) {
        	log.info "URI: ${wink_api}"
            log.warn "Unexpected response error: ${ex.statusCode}"
            log.error "${ex}"
            send_to_splunk("{\"sourcetype\": \"wink_via_smartthings\", \"event\": { \"data\": \"${ex}\" } }")
        }
}

def grabLocation(it){
	def myD = [:]
    myD["name"] = it.name
    myD["mode"] = it.currentMode
    myD["id"] = it.id
    myD["latitude"] = it.latitude
    myD["longitude"] = it.longitude
    myD["zip_code"] = it.zipCode
    myD["data_source"] = "SmartThings_location"
    myD["data_type"] = "location"
    return myD
}

def grabEvents(it){
	def myD = [:]
    myD["name"] = it.name
    myD["displayName"] = it.displayName
    myD["device"] = it.device
    myD["uuid"] = it.id
    myD["status"] = it.status
    myD["model_name"] = it.modelName
    myD["device_manufacturer"] = it.manufacturerName
    myD["data_source"] = "SmartThings_grabEvents"
    myD["data_type"] = "device"
    myD["capabilities"] = []
    myD["value"] = it.currentValue
    it.capabilities.each {cap ->
    	myD["capabilities"].push("\"${cap.name}\"")
	}
    myD["attributes"] = []
    def theAtts = it.supportedAttributes
    theAtts.each{ att ->
        def cS = it.currentState("${att.name}")
        def val = (cS!=null) ? cS.value : "null" 
        myD["attributes"].push("\"${att.name}\"")
        myD[att.name] = val
    }
    myD["hub"] = it.hub
    myD["is_subscribe"] = "true"
    return myD
}
def evtHandler() {
	def myMap = [:]
    def myDevices = []
	//alarms + codetectors + contacts + indicators + modes + motions + presences + relays + smokedetectors + switches + levels + temperatures + waterdetectors + location + accelerations + energymeters
	// Collect switch values
	//myMap.temperatures = temperatures.collect{ [ id: it.id, name: "${it.displayName}", value: it.currentState("temperature").value] } 
     alarms.each{ myDevices.push(grabEvents(it)) } 
     codetectors.each{ myDevices.push(grabEvents(it)) } 
     contacts.each{ myDevices.push(grabEvents(it)) } 
     indicators.each{ myDevices.push(grabEvents(it)) } 
     modes.each{ myDevices.push(grabEvents(it)) } 
     motions.each{ myDevices.push(grabEvents(it)) } 
     presences.each{ myDevices.push(grabEvents(it)) } 
     relays.each{ myDevices.push(grabEvents(it)) } 
     smokedetectors.each{ myDevices.push(grabEvents(it)) } 
     switches.each{ myDevices.push(grabEvents(it)) } 
     levels.each{ myDevices.push(grabEvents(it)) } 
     temperatures.each{ myDevices.push(grabEvents(it)) } 
     waterdetectors.each{ myDevices.push(grabEvents(it)) } 
     location.each{ myDevices.push(grabLocation(it)) } 
     accelerations.each{ myDevices.push(grabEvents(it)) } 
     energymeters.each{ myDevices.push(grabEvents(it)) } 
     battery.each{ myDevices.push(grabEvents(it)) }
     
     myDevices.each{ log_to_Splunk(it) }
}

def log_to_Splunk(dvc){
	def json = "{ \"sourcetype\": \"smartthings\",\"event\" : { "
    dvc.each{ k, v ->
       switch(k){
         case "capabilities":
         case "attributes":
		    json += "\"${k}\": ${v},"        
         break
         
         default:
       		json += "\"${k}\":\"${v}\","
         break
         }
    }
    json += "\"done\": \"completed_output\""
    json += "} }"
    send_to_splunk(json)
}
def send_to_splunk(json){
    def params = [
            uri: "http://${splunk_hec_url}/services/collector",
            body: json,
            headers: [ "Authorization": "Splunk ${splunk_hec_token}" ]
        ]
        try {
            httpPostJson(params) { resp ->
            resp.headers.each {
                //log.debug "${it.name} : ${it.value}"
            }
            //log.debug "response contentType: ${resp.contentType}"
            }
        } catch ( groovyx.net.http.HttpResponseException ex ) {
        	log.debug "URI : http://${splunk_hec_url}/services/collector"
            log.debug "Send to Splunk Unexpected response error: ${ex.statusCode}"
            log.debug "${ex}"
            def resp = ex.getResponse()
            log.debug "${resp}"
        }        
}

def doSubscriptions() {
	subscribe(alarms,			"alarm",					alarmHandler)
    subscribe(codetectors,		"carbonMonoxideDetector",	coHandler)
	subscribe(contacts,			"contact",      			contactHandler)
    subscribe(indicators,		"indicator",    			indicatorHandler)
    subscribe(modes,			"locationMode", 			modeHandler)
    subscribe(motions,			"motion",       			motionHandler)
   	subscribe(presences,		"presence",     			presenceHandler)
    subscribe(relays,			"relaySwitch",  			relayHandler)
	subscribe(smokedetectors,	"smokeDetector",			smokeHandler)
	subscribe(switches,			"switch",       			switchHandler)
    subscribe(levels,			"level",					levelHandler)
	subscribe(temperatures,		"temperature",  			temperatureHandler)
	subscribe(waterdetectors,	"water",					waterHandler)
    subscribe(location,			"location",					locationHandler)
    subscribe(accelerations,    "acceleration",             accelerationHandler)
    subscribe(energymeters,     "power",                    powerHandler)
    
}

def genericHandler(evt) {
    def json = "{ \"event\" : { "
    json += "\"date\":\"${evt.date}\","
    json += "\"name\":\"${evt.name}\","
    json += "\"displayName\":\"${evt.displayName}\","
    json += "\"device\":\"${evt.device}\","
    json += "\"deviceId\":\"${evt.deviceId}\","
    json += "\"value\":\"${evt.value}\","
    json += "\"isStateChange\":\"${evt.isStateChange()}\","
    json += "\"id\":\"${evt.id}\","
    json += "\"description\":\"${evt.description}\","
    json += "\"descriptionText\":\"${evt.descriptionText}\","
    json += "\"installedSmartAppId\":\"${evt.installedSmartAppId}\","
    json += "\"isoDate\":\"${evt.isoDate}\","
    json += "\"isDigital\":\"${evt.isDigital()}\","
    json += "\"isPhysical\":\"${evt.isPhysical()}\","
    json += "\"location\":\"${evt.location}\","
    json += "\"locationId\":\"${evt.locationId}\","
    json += "\"unit\":\"${evt.unit}\","
    json += "\"source\":\"${evt.source}\","
    json += "\"is_subscribe\":\"true\","
    json += "\"data_source\":\"SmartThings_genericHandler\", \"data\": ${evt.data} "
    json += "} }"
    log.debug("JSON: ${json}")

    send_to_splunk(json)
}

def alarmHandler(evt) {
	genericHandler(evt)
}

def coHandler(evt) {
	genericHandler(evt)
}

def indicatorHandler(evt) {
	genericHandler(evt)
}

def presenceHandler(evt) {
	genericHandler(evt)
}

def switchHandler(evt) {
	genericHandler(evt)
}

def smokeHandler(evt) {
	genericHandler(evt)
}

def levelHandler(evt) {
    genericHandler(evt)
}

def contactHandler(evt) {
	genericHandler(evt)
}

def temperatureHandler(evt) {
	genericHandler(evt)
}

def motionHandler(evt) {
	genericHandler(evt)
}

def modeHandler(evt) {
	genericHandler(evt)
}

def relayHandler(evt) {
	genericHandler(evt)
}

def waterHandler(evt) {
	genericHandler(evt)
}

def locationHandler(evt) {
	genericHandler(evt)
}

def accelerationHandler(evt) {
	genericHandler(evt)
}

def powerHandler(evt) {
    genericHandler(evt)
}
