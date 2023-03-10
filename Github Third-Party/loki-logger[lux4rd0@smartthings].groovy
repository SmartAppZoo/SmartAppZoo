/**
 * HTTP Event Logger For Grafana Loki
 *

 * Copyright 2015 Brian Keifer
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 * 
 */

/**
 * Original source via Brian Keifer:
 *  https://raw.githubusercontent.com/bkeifer/smartthings/master/Logstash%20Event%20Logger/LogstashEventLogger.groovy
 *
 * Modifications from: Nic Jansma:
 *  https://github.com/nicjansma/smart-things
 *
 * This was originally created from the Splunk logger from Brian Keifer and Jason Hamilton.
 * I modified it to work as a Grafana Loki Event push.
 *
 * Changes:
 * 2021-06-27 Modified the code to publish to Grafana Loki
 * 2021-07-05 Added illuminence and tamper alerts
 */

definition(
  name: "Grafana Loki Event Logger",
  namespace: "lux4rd0",
  author: "Dave Schmid",
  description: "Log SmartThings events to a Grafana Loki Log Aggrigation server",
  category: "Convenience",
  iconUrl: "https://labs.lux4rd0.com/wp-content/uploads/2021/06/Grafana_Loki_Icon.png",
  iconX2Url: "https://labs.lux4rd0.com/wp-content/uploads/2021/06/Grafana_Loki_Icon.png",
  iconX3Url: "https://labs.lux4rd0.com/wp-content/uploads/2021/06/Grafana_Loki_Icon.png")

preferences {
  section("Log these CO Detectors:") {input "codetectors", "capability.carbonMonoxideDetector", multiple: true, required: false}
  section("Log these Acceleration Sensors:") {input "accelerations", "capability.accelerationSensor", multiple: true, required: false}
  section("Log these Alarms:") {input "alarms", "capability.alarm", multiple: true, required: false}
  section("Log these Batteries:") {input "batteries", "capability.battery", multiple: true, required: false}
  section("Log these Buttons:") {input "button", "capability.button", multiple: true, required: false}
  section("Log these Contact Sensors:") {input "contacts", "capability.contactSensor", multiple: true, required: false}
  section("Log these Doors Controllers:") {input "doorcontrollers", "capability.doorControl", multiple: true, required: false}
  section("Log these Energy Meters:") {input "energymeters", "capability.energyMeter", multiple: true, required: false}
  section("Log these Humidity Sensors:") {input "humidities", "capability.relativeHumidityMeasurement", multiple: true, required: false}
  section("Log these Illuminance Sensors:") {input "illuminances", "capability.illuminanceMeasurement", multiple: true, required: false}
  section("Log these Indicators:") {input "indicators", "capability.indicator", multiple: true, required: false}
  section("Log these Locks:") {input "lockDevice", "capability.lock", multiple: true, required: false}
  section("Log these Motion Sensors:") {input "motions", "capability.motionSensor", multiple: true, required: false}
  section("Log these Music Players:") {input "musicplayer", "capability.musicPlayer", multiple: true, required: false}
  section("Log these Power Meters:") {input "powermeters", "capability.powerMeter", multiple: true, required: false}
  section("Log these Presence Sensors:") {input "presences", "capability.presenceSensor", multiple: true, required: false}
  section("Log these Smoke Detectors:") {input "smokedetectors", "capability.smokeDetector", multiple: true, required: false}
  section("Log these Switch Levels:") {input "levels", "capability.switchLevel", multiple: true, required: false}
  section("Log these Switches:") {input "switches", "capability.switch", multiple: true, required: false}
  section("Log these Tamper Alerts:") {input "tamperAlert", "capability.tamperAlert", multiple: true, required: false}
  section("Log these Temperature Sensors:") {input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false}
  section("Log these Three-axis (Orientation) Sensors:") {input "threeAxis", "capability.threeAxis", multiple: true, required: false}
  section("Log these Thermostats:") {input "thermostats", "capability.thermostat", multiple: true, required: false}
  section("Log these Voltages:") {input "voltage", "capability.voltageMeasurement", multiple: true, required: false}
  section("Log these Water Detectors:") {input "waterdetectors", "capability.waterSensor", multiple: true, required: false}

  section("Loki Server") {
    input "use_local", "boolean", title: "Local Server?", required: true
    input "loki_host", "text", title: "Loki Hostname/IP", required: true
    input "use_ssl", "boolean", title: "Use SSL?", required: true
    input "loki_port", "number", title: "Loki Port", required: true
    input "loki_user", "text", title: "Loki User", required: false
    input "loki_api_key", "text", title: "Loki API Key", required: false
    input "loki_path", "text", title: "Loki Path", required: true
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
  subscribe(accelerations, "acceleration", accelerationHandler)
  subscribe(alarms, "alarm", alarmHandler)
  subscribe(batteries, "battery", batteryHandler)
  subscribe(button, "button", buttonHandler)
  subscribe(codetectors, "carbonMonoxideDetector", coHandler)
  subscribe(contacts, "contact", contactHandler)
  subscribe(doorcontrollers, "door", doorcontrollerHandler)
  subscribe(energymeters, "energy", energyHandler)
  subscribe(humidities, "humidity", humidityHandler)
  subscribe(indicators, "indicator", indicatorHandler)
  subscribe(levels, "level", levelHandler)
  subscribe(lightSensor, "illuminance", illuminanceHandler)
  subscribe(location, "location", locationHandler)
  subscribe(lockDevice, "lock", lockHandler)
  subscribe(modes, "locationMode", modeHandler)
  subscribe(motions, "motion", motionHandler)
  subscribe(musicplayers, "music", musicHandler)
  subscribe(powermeters, "power", powerHandler)
  subscribe(powermeters, "voltage", powerHandler)
  subscribe(powermeters, "current", powerHandler)
  subscribe(powermeters, "powerFactor", powerHandler)
  subscribe(presences, "presence", presenceHandler)
  subscribe(relays, "relaySwitch", relayHandler)
  subscribe(smokedetectors, "smokeDetector", smokeHandler)
  subscribe(switches, "switch", switchHandler)
  subscribe(tamperAlerts, "tamperAlert", tamperAlertHandler)
  subscribe(temperatures, "temperature", temperatureHandler)
  subscribe(thermostats, "temperature", thermostatHandler)
  subscribe(thermostats, "heatingSetpoint", thermostatHandler)
  subscribe(thermostats, "coolingSetpoint", thermostatHandler)
  subscribe(thermostats, "thermostatSetpoint", thermostatHandler)
  subscribe(thermostats, "thermostatMode", thermostatHandler)
  subscribe(thermostats, "thermostatFanMode", thermostatHandler)
  subscribe(thermostats, "thermostatOperatingState", thermostatHandler)
  subscribe(thermostats, "thermostatSetpointMode", thermostatHandler)
  subscribe(thermostats, "scheduledSetpoint", thermostatHandler)
  subscribe(thermostats, "optimisation", thermostatHandler)
  subscribe(thermostats, "windowFunction", thermostatHandler)
  subscribe(threeAxis, "threeAxis", threeaxisHandler)
  subscribe(voltageMeasurement, "voltage", voltageHandler)
  subscribe(waterdetectors, "water", waterHandler)
}

def genericHandler(evt) {
  /*
  log.debug("------------------------------")
  log.debug("date: ${evt.date}")
  log.debug("type: ${evt.name}")
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

  def startTime = new Date().time

  def json = ""
  json += "{\"streams\": [{\"stream\": {\"app\": \"smartthings\", \"location\": \"${evt.location}\", \"type\": \"${evt.name}\", \"device\": \"${evt.device}\"},\"values\": ["
  json += "[ \"${startTime}000000\", \""
  json += "{\\\"type\\\":\\\"${evt.name}\\\","
  json += "\\\"displayName\\\":\\\"${evt.displayName}\\\","
  json += "\\\"device\\\":\\\"${evt.device}\\\","
  json += "\\\"deviceId\\\":\\\"${evt.deviceId}\\\","
  json += "\\\"value\\\":\\\"${evt.value}\\\","
  json += "\\\"isStateChange\\\":\\\"${evt.isStateChange()}\\\","
  json += "\\\"id\\\":\\\"${evt.id}\\\","
  json += "\\\"description\\\":\\\"${evt.description}\\\","
  json += "\\\"descriptionText\\\":\\\"${evt.descriptionText}\\\","
  json += "\\\"installedSmartAppId\\\":\\\"${evt.installedSmartAppId}\\\","
  json += "\\\"isoDate\\\":\\\"${evt.isoDate}\\\","
  json += "\\\"isDigital\\\":\\\"${evt.isDigital()}\\\","
  json += "\\\"isPhysical\\\":\\\"${evt.isPhysical()}\\\","
  json += "\\\"location\\\":\\\"${evt.location}\\\","
  json += "\\\"locationId\\\":\\\"${evt.locationId}\\\","
  json += "\\\"unit\\\":\\\"${evt.unit}\\\","
  json += "\\\"source\\\":\\\"${evt.source}\\\"}"
  json += "\" ]"
  json += "]}]}"
  //log.debug("JSON: ${json}")
  def ssl = use_ssl.toBoolean()
  def local = use_local.toBoolean()
  def http_protocol
  def loki_server = "${loki_host}:${loki_port}"
  def length = json.getBytes().size().toString()
  def msg = parseLanMessage(description)
  def body = msg.body
  def status = msg.status

  if (local == true) {
    //sendHubCommand(new physicalgraph.device.HubAction([
    def result = (new physicalgraph.device.HubAction([
      method: "POST",
      path: "${loki_path}",
      headers: [
        "Content-Length": "${length}",
        HOST: "${loki_server}",
        "Content-Type": "application/json",
        "Accept-Encoding": "gzip,deflate"
      ],
      body: json
    ]))
 //   log.debug result
    sendHubCommand(result);
    return result
  } else {
    //log.debug "Use Remote"
    //log.debug "Current SSL Value ${use_ssl}"
    if (ssl == true) {
      //log.debug "Using SSL"
      http_protocol = "https"
    } else {
      //log.debug "Not Using SSL"
      http_protocol = "http"
    }

    def params = [
      uri: "${http_protocol}://${loki_user}:${loki_api_key}@${loki_host}${loki_path}",
      headers: [
        'Content-Type': "application/json"
      ],
      body: json
    ]
    log.debug params
    try {
      httpPostJson(params)
    } catch (groovyx.net.http.HttpResponseException ex) {
      log.debug "Unexpected response error: ${ex.statusCode}"
    }
  }
}

def accelerationHandler(evt) {genericHandler(evt)}
def alarmHandler(evt) {genericHandler(evt)}
def batteryHandler(evt) {genericHandler(evt)}
def buttonHandler(evt) {genericHandler(evt)}
def coHandler(evt) {genericHandler(evt)}
def contactHandler(evt) {genericHandler(evt)}
def doorcontrollerHandler(evt) {genericHandler(evt)}
def energyHandler(evt) {genericHandler(evt)}
def humidityHandler(evt) {genericHandler(evt)}
def illuminanceHandler(evt) {genericHandler(evt)}
def indicatorHandler(evt) {genericHandler(evt)}
def levelHandler(evt) {genericHandler(evt)}
def locationHandler(evt) {genericHandler(evt)}
def lockHandler(evt) {genericHandler(evt)}
def modeHandler(evt) {genericHandler(evt)}
def motionHandler(evt) {genericHandler(evt)}
def musicHandler(evt) {genericHandler(evt)}
def powerHandler(evt) {genericHandler(evt)}
def presenceHandler(evt) {genericHandler(evt)}
def relayHandler(evt) {genericHandler(evt)}
def smokeHandler(evt) {genericHandler(evt)}
def switchHandler(evt) {genericHandler(evt)}
def tamperAlertHandler(evt) {genericHandler(evt)}
def temperatureHandler(evt) {genericHandler(evt)}
def thermostatHandler(evt) {genericHandler(evt)}
def threeaxisHandler(evt) {genericHandler(evt)}
def voltageHandler(evt) {genericHandler(evt)}
def waterHandler(evt) {genericHandler(evt)}
