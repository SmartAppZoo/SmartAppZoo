import groovy.json.JsonBuilder

// Device List: http://docs.smartthings.com/en/latest/capabilities-reference.html

definition (
    name: "Device Logger",
    namespace: "Operations",
    author: "justinlhudson",
    description: "Logger",
    category: "Convenience",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Electronics.electronics13-icn?displaySize",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Electronics.electronics13-icn?displaySize=2x")

preferences {
    section("Log devices...") {
        input "presences", "capability.presenceSensor", title: "Presence", required: false, multiple: true
        input "powers", "capability.powerMeter", title: "Power", required: false, multiple: true
        input "energies", "capability.energyMeter", title: "Energy", required: false, multiple: true
        input "contacts", "capability.contactSensor", title: "Contact", required: false, multiple: true
        input "switches", "capability.switch", title: "Switch", required: false, multiple: true
        input "motions", "capability.motionSensor", title: "Motion", required: false, multiple: true
        input "batteries", "capability.battery", title: "Battery", required: false, multiple: true
        input "temperatures", "capability.temperatureMeasurement", title: "Temperature", required:false, multiple: true
        input "thermostats", "capability.thermostat", title: "Thermostat", required:false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity", required: false, multiple: true
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminance", required:false, multiple: true
        input "waters", "capability.waterSensor", title: "Water", required:false, multiple: true
        input "valves", "capability.valve", title: "Valve", required:false, multiple: true
        input "detectors", "capability.smokeDetector", title: "Detectors", required:false, multiple: true
    }

    section ("API (GET request query") {
    input("ip", "string", title:"IP Address", description: "IP Address", required: true, displayDuringSetup: true)
    input("port", "string", title:"Port", description: "Port", defaultValue: 3000 , required: true, displayDuringSetup: true)
    input("path", "string", title:"Path", description: "Path", defaultValue: "/api/log" , required: true, displayDuringSetup: true)
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
    subscribe(location, "mode", modeHandler)
    subscribe(location, "routineExecuted", routineHandler)
    
    subscribe(presences, "presence", presenceHandler)
    subscribe(temperatures, "temperature", temperatureHandler)
    subscribe(thermostats, "thermostatOperatingState", thermostatHandler)
    subscribe(humidities, "humidity", humidityHandler)
    subscribe(illuminances, "illuminance", illuminanceHandler)
    subscribe(batteries, "battery", batteryHandler)
    subscribe(contacts, "contact", contactHandler)
    subscribe(powers, "power", powerHandler)
    subscribe(energies, "energy", energyHandler)
    subscribe(motions, "motion", motionHandler)
    subscribe(switches, "switch", switchHandler)
    subscribe(waters, "water", waterHandler)
    subscribe(valves, "contact", valveHandler)
    subscribe(detectors, "smoke", detectorHandler)
    subscribe(detectors, "carbonMonoxide", detectorHandler)
}

def routineHandler(evt) {
  logField("routine",evt) { 1 }//it.toString() }
}

def modeHandler(evt)
{
  logField("mode",evt) { 1 } //it.toString() }
}

def presenceHandler(evt)
{
  logField("presence",evt) { (it == "present" ? 1 : 0).toString() }
}

def illuminanceHandler(evt) {
  logField("illuminance",evt) { it.toString() }
}

def thermostatHandler(evt) {
  logField("thermostat",evt) { ((it == "heating" ? 1 : 0) || (it == "cooling" ? 1 : 0)).toString() }
}

def powerHandler(evt) {
  logField("power",evt) { it.toString() }
}

def energyHandler(evt) {
  logField("energy",evt) { it.toString() }
}

def batteryHandler(evt) {
  logField("battery",evt) { it.toString() }
}

def humidityHandler(evt) {
  logField("humidity",evt) { it.toString() }
}

def temperatureHandler(evt) {
  logField("temperature",evt) { it.toString() }
}

def contactHandler(evt) {
  logField("contact",evt) { it == "open" ? "1" : "0" }
}

def motionHandler(evt) {
  logField("motion",evt) { it == "active" ? "1" : "0" }
}

def switchHandler(evt) {
  logField("switch",evt) { it == "on" ? "1" : "0" }
}

def waterHandler(evt) {
  logField("water",evt) { it == "wet" ? "1" : "0" }
}

def valveHandler(evt) {
  logField("valve",evt) { it == "open" ? "1" : "0" }
}

def detectorHandler(evt) {
  logField("detector",evt) { it == "detected" ? "1" : "0" }
}

private logField(type, evt, Closure c) {
  def name = evt.name.trim()
  try {
    name = evt.displayName.trim()
  } catch (ex) { }

  // Note: value is of type int32, for web service used
  def value = c(evt.value)

  log.debug "Logging: ${type}, ${name}, ${value}"

  def method = "POST"

  def headers = [:] 
  headers.put("HOST", getHostAddress())
  headers.put("Content-Type", "application/json")

    def json = new JsonBuilder()
    json.call("type":"${type}","name":"${name}","value":"${value}")

  def result = new physicalgraph.device.HubAction(
      method: method,
      path: "${settings.path}",
      body: json.toString(),
      headers: headers
  )
/*
  try {
    httpPost("${path}", "type=${type}&name=${name}&value=${value}") { resp ->
    log.debug "response data: ${resp.data}"
    log.debug "response contentType: ${resp.contentType}"
  } catch (ex) {
      log.debug "something went wrong: $ex"
  }
*/
  log.debug "${result}"
  sendHubCommand(result)
}

private getHostAddress() {
  return "${ip}:${port}"
}