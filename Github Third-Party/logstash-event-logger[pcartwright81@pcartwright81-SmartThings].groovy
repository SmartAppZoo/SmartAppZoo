/*This smartapp has been modified
/**
 *  Event Logger
 *
 *  Copyright 2015 Brian Keifer
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
    name: "Logstash Event Logger",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Log SmartThings events to a Logstash server",
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
        

    section ("Logstash Server") {
        input "logstash_host", "text", title: "Logstash Hostname/IP"
        input "logstash_port", "number", title: "Logstash Port"
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
    /*
    log.debug("------------------------------")
	log.debug("date: ${evt.date}")
	log.debug("name: ${evt.name}")
    log.debug("displayName: ${evt.displayName}")
    log.debug("device: ${evt.device}")
    log.debug("deviceId: ${evt.deviceId}")
    log.debug("value: ${evt.value}")
    log.debug("isStateChange: ${evt.isStateChange()}")
	log.debug("id: ${evt.id}")
    log.debug("description: ${evt.description}")
    log.debug("descriptionText: ${evt.descriptionText}")
    log.debug("installedSmartAppId: ${evt.installedSmartAppId}")
    log.debug("isoDate: ${evt.isoDate}")
    log.debug("isDigital: ${evt.isDigital()}")
    log.debug("isPhysical: ${evt.isPhysical()}")
    log.debug("location: ${evt.location}")
    log.debug("locationId: ${evt.locationId}")
    log.debug("source: ${evt.source}")
    log.debug("unit: ${evt.unit}")
    */
    def json = "{"
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
    json += "}"
    //log.debug("JSON: ${json}")
	state.body << json
    log.info state.body.size()
    if (state.body.size()  >= 10) {
    	//def body = state.body.collect { it }.join(',')
    	//def params = [
    	//uri: "http://${logstash_host}:${logstash_port}",
        //body: body
    	//]
    	//try {
       // 	httpPostJson(params)            
    	//} catch ( groovyx.net.http.HttpResponseException ex ) {
       	//	log.debug "Unexpected response error: ${ex.statusCode}"
        //    return;
    	//}
        try{
            def body = state.body.collect { it }.join(',')
        	postapi(body);
        }catch (Exception ex)
        {
        	log.Error(ex)
            return;
        }
        state.body = []
    }
    
}
private postapi(command) {
	def length = command.getBytes().size().toString()
	sendHubCommand(new physicalgraph.device.HubAction("""POST /house.metric HTTP/1.1\r\nHOST: ${logstash_host}:${logstash_port}\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: ${length}\r\nAccept:*/*\r\n\r\n${command}""", physicalgraph.device.Protocol.LAN, "XXXXXXXX:PPP"))
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