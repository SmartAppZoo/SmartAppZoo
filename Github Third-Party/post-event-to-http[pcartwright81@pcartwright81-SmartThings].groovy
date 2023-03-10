/**
 *  Post Event to Http
 *  Uses local c# client parser to send events to sql database
 *  Copyright 2017 Patrick Cartwright
 *  Based on bkeifer's Logstash Event Logger & jt55401's SmartThings Data Logger
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
    name: "Post Event to Http",
    namespace: "pcartwright81",
    author: "Patrick Cartwright",
    description: "Log SmartThings events to an http server",
    category: "Convenience",
    iconUrl: "http://valinor.net/images/logstash-logo-square.png",
    iconX2Url: "http://valinor.net/images/logstash-logo-square.png",
    iconX3Url: "http://valinor.net/images/logstash-logo-square.png")


preferences {
    section("Log these presence sensors:") {
        input "presences", "capability.presenceSensor", multiple: true, required: false
    }
 	section("Log these switches:") {
    	input "switches", "capability.switch", multiple: true, required: false
    }
 	section("Log these switch levels:") {
    	input "levels", "capability.switchLevel", multiple: true, required: false
    }
	section("Log these motion sensors:") {
    	input "motions", "capability.motionSensor", multiple: true, required: false
    }
	section("Log these temperature sensors:") {
    	input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false
    }
    section("Log these humidity sensors:") {
    	input "humidities", "capability.relativeHumidityMeasurement", multiple: true, required: false
    }
    section("Log these contact sensors:") {
    	input "contacts", "capability.contactSensor", multiple: true, required: false
    }
    section("Log these alarms:") {
		input "alarms", "capability.alarm", multiple: true, required: false
	}
    section("Log these indicators:") {
    	input "indicators", "capability.indicator", multiple: true, required: false
    }
    section("Log these CO detectors:") {
    	input "codetectors", "capability.carbonMonoxideDetector", multiple: true, required: false
    }
    section("Log these smoke detectors:") {
    	input "smokedetectors", "capability.smokeDetector", multiple: true, required: false
    }
    section("Log these water detectors:") {
    	input "waterdetectors", "capability.waterSensor", multiple: true, required: false
    }
    section("Log these acceleration sensors:") {
    	input "accelerations", "capability.accelerationSensor", multiple: true, required: false
    }
    section("Log these energy meters:") {
        input "energymeters", "capability.energyMeter", multiple: true, required: false
    }
    
    section("Log these power meters:") {
        input "powermeters", "capability.powerMeter", multiple: true, required: false
    }
    
    section("Log these thermostats:") {
    input "thermostats", "capability.thermostat", title: "Thermostats", required:false, multiple:true
    }
    
    section("Log these locks:") {
    input "locks", "capability.lock", title: "Locks", required:false, multiple:true
    }
        

    section ("Server") {
        input "httpserver_host", "text", title: "Hostname/IP"
        input "httpserver_port", "number", title: "Port"
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
    //state.body = []
	doSubscriptions()  
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
    subscribe(powermeters,      "power",                    powerHandler)
    subscribe(energymeters,     "energy", 					energyEventHandler)
    subscribe(thermostats, 		"thermostatOperatingState", thermostatOperatingHandler)
    subscribe(thermostats,		"thermostatMode", 			thermostatModeHandler)
    subscribe(thermostats,		"heatingSetpoint", 			heatingSetPointHandler)
    subscribe(thermostats,		"coolingSetpoint", 			coolingSetPointHandler)
    subscribe(locks, 			"lock", 					lockEventHandler)
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
}
def energyEventHandler(evt) {
	genericHandler(evt)
}
def thermostatOperatingHandler(evt) {
	genericHandler(evt)
}

def thermostatModeHandler(evt) {
	genericHandler(evt)
}

def heatingSetPointHandler(evt) {
	genericHandler(evt)
}

def coolingSetPointHandler(evt) {
	genericHandler(evt)
}

def lockEventHandler(evt) {
	genericHandler(evt)
}

def genericHandler(evt) {
	if(state.body == null){
    	state.body = []
    }   
    def json = "{\"event\":{"
    json += "\"date\":\"${evt.date}\","
    json += "\"name\":\"${evt.name}\","
    json += "\"displayName\":\"${evt.displayName}\","
    json += "\"device\":\"${evt.device}\","
    json += "\"deviceId\":\"${evt.deviceId}\","
    json += "\"value\":\"${evt.value}\","
    json += "\"valuegraph\":\"${getgraphvalue(evt)}\","
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
    json += "\"source\":\"${evt.source}\""
    json += "}}"
    //log.debug("JSON: ${json}")     
	state.body << json
    //log.info state.body.size()
    if (state.body.size()  >= 3) {    	
        try{
            def jsonout = "{" + "\"events\":[" + state.body.collect { it }.join(',') + "]}"
            //log.debug jsonout
			//def msg = parseLanMessage(jsonout)
			//def body = msg.body
			//def status = msg.status
            //log.debug status
        	def length = jsonout.getBytes().size().toString()
            def result = (new physicalgraph.device.HubAction([
            method: "POST",
            path: "/house.metric",
            headers: [    
            "Content-Length":"${length}",
            HOST: "${httpserver_host}:${httpserver_port}",
            "Content-Type":"application/json",
            "Accept-Encoding":"gzip,deflate"
            ],
            body:jsonout
            ]))
            //log.debug jsonout
            //log.debug result
            sendHubCommand(result);            
            
        }catch (Exception ex)
        {
        	log.error ex
            return;
        } 
        state.body = []
    }
    
}

def lanResponseHandler(evt){
	//log.debug("Lan Response: ${evt.description}")
    def msg = parseLanMessage(evt.description)
	def body = msg.body
	def status = msg.status
    log.debug "Lan message response:" + "${status}"
}

def getgraphvalue(evt){
	if(evt.name  == "alarm")
	{
		if(evt.value == "active")
    	{
    		return 1
    	}
    	if(evt.value == "inactive")
    	{
    		return 0
    	}
	}
	
	if(evt.name  == "carbonMonoxideDetector")
	{
		if(evt.value == "active")
    	{
    		return 1
    	}
    	if(evt.value == "inactive")
    	{
    		return 0
    	}
	}
	
    if(evt.name  == "motion")
    {	
    	if(evt.value == "active")
    	{
    		return 1
    	}
    	if(evt.value == "inactive")
    	{
    		return 0
    	}
    }
	
	if(evt.name  == "heatingSetpoint")
    {	
    	return evt.value.toInteger()
    }
	
	if(evt.name  == "coolingSetpoint")
    {	
    	return evt.value.toInteger()
    }
	
	if(evt.name  == "thermostatOperatingState")
    {	
    	if(evt.value == "aux")
    	{
    		return 4
    	}
		if(evt.value == "heating")
    	{
    		return 3
    	}
    	if(evt.value == "cooling")
    	{
    		return 2
    	}
        
        if(evt.value == "fan only")
    	{
    		return 1
    	}
		
		if(evt.value == "idle")
    	{
    		return 0
    	}
    }
	
	if(evt.name  == "thermostatMode")
    {	
    	if(evt.value == "heat")
    	{
    		return 2
    	}
    	if(evt.value == "cool")
    	{
    		return 1
    	}
		if(evt.value == "off")
    	{
    		return 1
    	}
    }
    
    if(evt.name  == "power")
    {	
    	return evt.value.toBigDecimal()
    }
    
    if(evt.name  == "energy")
    {	
    	//log.debug "${evt.value}" + " energy"
    	return evt.value.toBigDecimal()
    }
    
    if(evt.name  == "level")
    {	
    	return evt.value.toInteger()
    }
    
    if(evt.name  == "temperature")
    {	
    	return evt.value.toInteger()
    }
    
    if(evt.name == "switch")
    {
    	if(evt.value == "on")
    	{
    		return 1
    	}
    	if(evt.value == "off")
    	{
    		return 0
    	}   
    }
    
    if(evt.name == "contact")
    {
    	if(evt.value == "open")
    	{
    		return 1
    	}
    	if(evt.value == "closed")
    	{
    		return 0
    	}   
    }
    
    if(evt.name == "lock")
    {
    	if(evt.value == "unlocked")
    	{
    		return 1
    	}
    	if(evt.value == "locked")
    	{
    		return 0
    	}   
    }
     
   	return -1
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