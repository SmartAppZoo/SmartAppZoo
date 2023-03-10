definition(
    name: "Datalogger",
    namespace: "mwren",
    author: "Matt Wren",
    description: "Get a push notification or text message when any of a variety of SmartThings is activated.  Supports motion, contact, acceleration, moisture and presence sensors as well as switches.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png"
)

preferences {
    section("Monitor one or more...") {
        input "motion",       "capability.motionSensor",           title: "Motion", required: false, multiple: true
        input "contact",      "capability.contactSensor",          title: "Contact", required: false, multiple: true
        input "acceleration", "capability.accelerationSensor",     title: "Acceleration", required: false, multiple: true
        input "mySwitch",     "capability.switch",                 title: "Switch", required: false, multiple: true
        input "presence",     "capability.presenceSensor",         title: "Presence", required: false, multiple: true
        input "smoke",        "capability.smokeDetector",          title: "Smoke", required: false, multiple: true
        input "water",        "capability.waterSensor",            title: "Water", required: false, multiple: true
        input "temperature",  "capability.temperatureMeasurement", title: "Temperature", required: false, multiple: true
        input "hygro",        "capability.relativeHumidityMeasurement", title: "Hygrometer", required: false, multiple: true
        input "luminance",    "capability.illuminanceMeasurement", title: "Luminance", required: false, multiple: true
        
        input "alarm",    "capability.alarm", title: "Alarm", required: false, multiple: true
        input "lock",    "capability.lock", title: "Lock", required: false, multiple: true
        input "music",    "capability.musicPlayer", title: "Music", required: false, multiple: true
        input "location",    "capability.locationMode", title: "Location Mode", required: false, multiple: true
        
        input "battery",      "capability.battery",                title: "Battery", required: false, multiple: true
        
        input "switchLevel",  "capability.switchLevel",            title: "Dimmer", required: false, multiple: true
        input "powerMeter",   "capability.powerMeter",             title: "Power", required: false, multiple: true
        input "energyMeter",  "capability.energyMeter",            title: "Energy", required: false, multiple: true
        input "thermostat",   "capability.thermostat",             title: "Thermostat", required: false, multiple: true
    }
    section("Send to...") {
        input "host",  "text",                                     title: "Host", required: true, multiple: false
        input "port", "number",                                    title: "Port", required: true, multiple: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribeToEvents()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  subscribeToEvents()
}

def subscribeToEvents() {
  subscribe(contact, "contact", eventHandler)
    subscribe(acceleration, "acceleration", eventHandler)
    subscribe(motion, "motion", eventHandler)
    
    subscribe(mySwitch, "switch", eventHandler)
    subscribe(presence, "presence", eventHandler)
    subscribe(smoke, "smoke", eventHandler)
    subscribe(water, "water", eventHandler)
    subscribe(temperature, "temperature", eventHandler)
    subscribe(hygro, "humidity", eventHandler)
    subscribe(luminance, "illuminance", eventHandler)
    
    subscribe(alarm, "alarm", eventHandler)
    subscribe(lock, "lock", eventHandler)
    subscribe(music, "status", eventHandler)
    subscribe(music, "level", eventHandler)
    subscribe(music, "mute", eventHandler)
    subscribe(location, "mode", eventHandler)
    
    subscribe(battery, "battery", eventHandler)  
    //

    subscribe(switchLevel, "level", eventHandler)
    subscribe(powerMeter, "power", eventHandler)
    subscribe(energyMeter, "energy", eventHandler)
    
    subscribe(thermostat, "heatingSetpoint", eventHandler)
    subscribe(thermostat, "coolingSetpoint", eventHandler)
    subscribe(thermostat, "thermostatMode", eventHandler)
    subscribe(thermostat, "thermostatFanMode", eventHandler)
    subscribe(thermostat, "thermostatOperatingState", eventHandler)

}

def eventHandler(evt) {
  sendMessage(evt)
}

private sendMessage(evt) {
  def headers = [:]
    headers.put("HOST", "$host:$port")
    headers.put("Content-Type", "application/json")
    def body = [event: [deviceId: evt.deviceId, name: evt.name, isPhysical: evt.isPhysical(), 
      value: evt.value, description: evt.description, date: evt.date, unit: evt.unit, 
        location: evt.locationId, displayName: evt.displayName]]
  def command = new physicalgraph.device.HubAction(
        method: "POST",
        path: "/st",
        headers: headers,
        body: body
    )
    sendHubCommand(command)
}

