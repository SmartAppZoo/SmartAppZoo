/**
 *  Data Logger
 *
 *  See: https://raw.githubusercontent.com/TheFuzz4/SmartThingsSplunkLogger/master/smartapps/thefuzz4/splunk-http-event-logger.src/splunk-http-event-logger.groovy
 *
 *  Author: Jarrod Stenberg
 *  Date: 2018-12-12
 *
 *  Log data to a HTTP POST.
 *
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
	name: "JSON Logger",
	namespace: "slonob",
	author: "Jarrod Stenberg",
	description: "Log device events as JSON to Fluentd endpoint.",
	category: "Convenience",
	iconUrl: "https://img.icons8.com/ios/50/000000/event-log.png",
	iconX2Url: "https://img.icons8.com/ios/50/000000/event-log.png",
	iconX3Url: "https://img.icons8.com/ios/50/000000/event-log.png")

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
  section("Log these music players:") {
    input "musicplayer", "capability.musicPlayer", multiple: true, required: false
  }
  section("Log these power meters:") {
    input "powermeters", "capability.powerMeter", multiple: true, required: false
  }
  section("Log these illuminance sensors:") {
    input "illuminances", "capability.illuminanceMeasurement", multiple: true, required: false
  }
  section("Log these batteries:") {
    input "batteries", "capability.battery", multiple: true, required: false
  }
  section("Log these buttons:") {
    input "button", "capability.button", multiple: true, required: false
  }
  section("Log these voltages:") {
    input "voltages", "capability.voltageMeasurement", multiple: true, required: false
  }
  section("Log these locks:") {
    input "locks", "capability.lock", multiple: true, required: false
  }
  section("Log these thermostats:") {
    input "thermostats", "capability.thermostat", multiple: true, required: false
    input "thermostats", "capability.thermostatOperatingState", multiple: true, required: false
  }

  section ("Fluentd Server") {
    input "fluentd_host", "text", title: "Fluentd Hostname/IP", required: true
    input "fluentd_port", "number", title: "Fluentd Port", required: true, defaultValue: 9880
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
  subscribe(alarms,	"alarm",	alarmHandler)
  subscribe(codetectors,	"carbonMonoxideDetector",	coHandler)
  subscribe(contacts,	"contact", contactHandler)
  subscribe(indicators,	"indicator", indicatorHandler)
  subscribe(modes,	"locationMode", modeHandler)
  subscribe(motions,	"motion", motionHandler)
  subscribe(presences,	"presence", presenceHandler)
  subscribe(relays,	"relaySwitch", relayHandler)
  subscribe(smokedetectors,	"smokeDetector",	smokeHandler)
  subscribe(switches,	"switch", switchHandler)
  subscribe(levels,	"level",	levelHandler)
  subscribe(temperatures,	"temperature", temperatureHandler)
  subscribe(waterdetectors,	"water",	waterHandler)
  subscribe(location,	"location",	locationHandler)
  subscribe(accelerations, "acceleration", accelerationHandler)
  subscribe(energymeters, "energy", energyHandler)
  subscribe(musicplayers, "music", musicHandler)
  subscribe(lightSensor,	"illuminance",	illuminanceHandler)
  subscribe(powermeters,	"power",	powerHandler)
  subscribe(batteries,	"battery", batteryHandler)
  subscribe(button, "button", buttonHandler)
  subscribe(voltageMeasurement, "voltage", voltageHandler)
  subscribe(locks, "lock", lockHandler)
  subscribe(humidities, "humidity", humidityHandler)
  subscribe(thermostats, "thermostat", thermostatHandler)
  subscribe(thermostats, "thermostatMode", thermostatHandler)
  subscribe(thermostats, "thermostatSetpoint", thermostatHandler)
  subscribe(thermostats, "thermostatCoolingSetpoint", thermostatHandler)
  subscribe(thermostats, "thermostatHeatingSetpoint", thermostatHandler)
  subscribe(thermostats, "thermostatOperatingState", thermostatHandler)
  subscribe(thermostats, "thermostatFanMode", thermostatHandler)
}

def genericHandler(evt) {
  logField(evt)
}

private logField(evt) {
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
  //log.debug("timezone: ${location.timeZone}")
  def rawValue = evt.value.toString() // Should check if number and handle accordingly
	def value = evt.value
	def metric = evt.name.toString()
    switch(metric) {
        case "motion":
        	value = (rawValue == 'active') ? 1 : -1;
			break
		case "switch":
        	value = (rawValue == 'on') ? 1 : -1;
			break
		case "relay":
        	value = (rawValue == 'closed') ? 1 : -1;
			break
		case "presence":
        	value = (rawValue == 'present') ? 1 : -1;
			break
		case "lock":
        	value = (rawValue == 'locked') ? 1 : -1;
			break
		case "acceleration":
        	value = (rawValue == 'active') ? 1 : -1;
			break
	}


  def unit = evt.unit.toString()
  def deviceName = evt.displayName.trim().toLowerCase().replaceAll(' ','_').replaceAll('/','_')
	def nowTime = new Date().format( "yyyy-MM-dd hh:mm:ss.SSS",location.timeZone)
  def timestamp = new Date().getTime();
	def isStateChange = evt.isStateChange.toString()
  def deviceId = evt.deviceId.toString()
  //def isoDate = evt.isoDate.toString()
  def body = """json={"deviceId":"$deviceId","deviceName":"$deviceName","metric":"$metric","rawValue":"$rawValue","value":$value,"unit":"$unit","isStateChange":$isStateChange,"datetime":"$nowTime"}"""
  log.debug("${body}")
	postapi(body)
}

private postapi(command) {
	def length = command.getBytes().size().toString()
    sendHubCommand(new physicalgraph.device.HubAction("""POST /home_things HTTP/1.1\r\nHOST: ${fluentd_host}:${fluentd_port}\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: ${length}\r\nAccept:*/*\r\n\r\n${command}""", physicalgraph.device.Protocol.LAN, "0x0A026432:22B8"))
    //sendHubCommand(new physicalgraph.device.HubAction("""PUT HTTP/1.1\r\nHOST: 10.2.100.50:8888\r\nContent-Type: text/json\r\nContent-Length: ${length}\r\nAccept:*/*\r\n\r\n${command}""", physicalgraph.device.Protocol.LAN, "0x0A026432:22B8"))
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

def energyHandler(evt) {
  genericHandler(evt)
}

def musicHandler(evt) {
  genericHandler(evt)
}
def illuminanceHandler(evt) {
  genericHandler(evt)
}

def powerHandler(evt) {
  genericHandler(evt)
}

def humidityHandler(evt) {
  genericHandler(evt)
}

def batteryHandler(evt) {
  genericHandler(evt)
}

def buttonHandler(evt) {
  genericHandler(evt)
}

def voltageHandler(evt) {
  genericHandler(evt)
}

def lockHandler(evt) {
  genericHandler(evt)
}

def thermostatHandler(evt) {
  log.debug("Thermostat event captured: ${evt}")

  genericHandler(evt)
}
def lanResponseHandler(evt){
	log.debug("Lan Response: ${evt.description}")
}
