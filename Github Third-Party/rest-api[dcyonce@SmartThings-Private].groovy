/**
 *  REST API
 *
 *  Copyright 2017 DON YONCE
 *
 *  https://graph.api.smartthings.com/api/smartapps/installations/32aa705e-fd19-4176-836a-db1a319194db/lights
 *  https://graph.api.smartthings.com/api/smartapps/installations/32aa705e-fd19-4176-836a-db1a319194db/lights/<lamp name>/on
 *  https://graph.api.smartthings.com/api/smartapps/installations/32aa705e-fd19-4176-836a-db1a319194db/lights/<lamp name>/off
 *
 *  https://graph.api.smartthings.com/api/smartapps/installations/32aa705e-fd19-4176-836a-db1a319194db/probes
 *  https://graph.api.smartthings.com/api/smartapps/installations/32aa705e-fd19-4176-836a-db1a319194db/family
 *  https://graph.api.smartthings.com/api/smartapps/installations/32aa705e-fd19-4176-836a-db1a319194db/sensors
 */
definition(
    name: "REST API",
    namespace: "dcyonce",
    author: "DON YONCE",
    description: "REST access to SmartThings devices",
    category: "My Apps",
    iconUrl: "http://www.yonce.com/images/32x32/Modules.png",
    iconX2Url: "http://www.yonce.com/images/64x64/Modules.png",
    iconX3Url: "http://www.yonce.com/images/64x64/Modules.png",
	oauth: [displayName: "REST App", displayLink: "http://localhost:4567"])

preferences {
  section ("Report via REST on these things...") {
    input "switches", "capability.switch", multiple: true, title: "Lights"
    input "probes", "capability.temperatureMeasurement", multiple: true, title: "Sensors"
    input "weatherStations", "capability.waterSensor", multiple: true, title: "Weather Stations"
    input "people", "capability.presenceSensor", multiple: true, title: "People"
  }
}

mappings {
  path("/lights") {
    action: [
      GET: "listLights"
    ]
  }
  path("/probes") {
    action: [
      GET: "listProbes"
    ]
  }
  path("/sensors") {
    action: [
      GET: "listProbes"
    ]
  }
  path("/family") {
    action: [
      GET: "listFamily"
    ]
  }
  path("/weather") {
    action: [
      GET: "listWeatherStations"
    ]
  }
  // Turn lights On/Off
  path("/lights/:name/:command") {
    action: [
      GET: "lightsOnOff"
    ]
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
}

// 	returns a list of lights
// 	[{"name":"Lamp on right","switch":"off","level":100,"hue":null,"colorTemperature":null,"saturation":null,"power":null},
//	 {"name":"Don's Bedside Table","switch":"on","level":100,"hue":null,"colorTemperature":null,"saturation":null,"power":null},
//	 {"name":"Office Overhead 2","switch":"on","level":0,"hue":225,"colorTemperature":4292,"saturation":12,"power":null}]
def listLights() {
    def resp = []
    switches.each {
    	log.debug("switch ${it}")
      	resp << denull(
        		[	id: it.id,
            		deviceType: it.typeName,
        			name: it.displayName, 
        			switch: it.currentValue("switch"), 
                    level: it.currentValue("level"), 
                    hue: it.currentValue("hue"), 
                    colorTemperature: it.currentValue("colorTemperature"), 
                    saturation: it.currentValue("saturation"),
                    power: it.currentValue("power"),
      				checkInterval: it.currentValue("checkInterval"),
                    lastActivity: it.getLastActivity(),
                    manufacturer: it.getManufacturerName(),
                    model: it.getModelName(),
                    status: it.getStatus(),                    
                 ])
    }
    return resp
}

// 	returns a list of devices that support temperature measurement
//	[{"name":"On/Off Cube","temperature":74,"battery":100,"water":null,"contact":"open","motion":null}]
def listProbes() {
    log.debug("getprobes")
    def resp = []
    probes.each {
    //log.debug("probe ${it}")
      resp << denull(
      		[	id: it.id,
            	deviceType: it.typeName,
             	name: it.displayName, 
      			temperature: it.currentValue("temperature"),
                humidity: it.currentValue("humidity"),
      			battery: it.currentValue("battery"),
      			water: it.currentValue("water"),
      			contact: it.currentValue("contact"),
      			threeAxis: it.currentValue("threeAxis"),
      			acceleration: it.currentValue("acceleration"),
      			checkInterval: it.currentValue("checkInterval"),
      			motion: it.currentValue("motion"),
                illuminance: it.currentValue("illuminance"),
                tamper: it.currentValue("tamper"),
                lastActivity: it.getLastActivity(),
                manufacturer: it.getManufacturerName(),
                model: it.getModelName(),
                status: it.getStatus(),                    
                ])
    }
    return resp
}

// 	returns a list of devices that support temperature measurement
//	[{"name":"On/Off Cube","temperature":74,"battery":100,"water":null,"contact":"open","motion":null}]
def listWeatherStations() {
    log.debug("weatherStations")
    def resp = []
    weatherStations.each {
    //log.debug("probe ${it}")
      resp << denull(
      		[	id: it.id,
            	deviceType: it.typeName,
             	name: it.displayName, 
      			temperature: it.currentValue("temperature"),
                humidity: it.currentValue("humidity"),
      			feels_like: it.currentValue("feels_like"),
      			water: it.currentValue("water"),
      			forecast: it.currentValue("forecast"),
      			wind_speed: it.currentValue("wind_speed"),
      			wind_direction: it.currentValue("wind_direction"),
      			uv_index: it.currentValue("uv_index"),
      			location: it.currentValue("location"),
                precipitation: it.currentValue("precipitation"),
                lastActivity: it.getLastActivity(),
                manufacturer: it.getManufacturerName(),
                model: it.getModelName(),
                status: it.getStatus(),                    
                ])
    }
    return resp
}

//	returns a list of people
//	[{"name":"Dondo","presence":"present"},{"name":"Sheila","presence":"present"}]
def listFamily() {
    def resp = []
    people.each {
    	//	log.debug("switch ${it}")
      	resp << denull(
        		[	id: it.id,        			
            		deviceType: it.typeName,
                	name: it.displayName, 
        			presence: it.currentValue("presence"), 
                    lastActivity: it.getLastActivity(),
                    manufacturer: it.getManufacturerName(),
                    model: it.getModelName(),
                    status: it.getStatus(),                    
                ])
    }
    return resp
}

//	Turn Lights On/Off
void lightsOnOff() {
    // the lamp "name" and "command"
    def command = params.command
    def name = params.name
    def lights

	//	log.debug("name=${name}, command=${command}, switches=${switches}")
    // 	Look for a Lamp with the "name"
	lights= switches.find{it.displayName==name}    	
	log.debug("lights=${lights}")

	if (lights)
    {
        switch(command) {
            case "on":
                log.debug "turning ${lights} on"
                lights.on()
                break
            case "off":
                log.debug "turning ${lights} off"
                lights.off()
                break
            default:
                httpError(400, "$command is not a valid command for all switches specified")
        }
    }
}

def denull(obj) {
  if(obj instanceof Map) {
    obj.collectEntries {k, v ->
      if(v) [(k): denull(v)] else [:]
    }
  } else if(obj instanceof List) {
    obj.collect { denull(it) }.findAll { it != null }
  } else {
    obj
  }
}