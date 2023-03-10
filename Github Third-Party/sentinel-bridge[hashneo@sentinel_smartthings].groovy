/**
 *  sentinel-bridge
 *
 *  Copyright 2020 Steven Taylor
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

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.Base64

definition(
    name: "Sentinel-Bridge",
    namespace: "hashneo",
    author: "Steven Taylor",
    description: "Sentinel Bridge",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true
)

preferences {
	page(name: "page1")
}

def page1() {
  dynamicPage(name: "page1", install: true, uninstall: true) {
    section("SmartThings Hub") {
      input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
    }
    section("General:") {
        //input "prefDebugMode", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: true
        input (
        	name: "configLoggingLevelIDE",
        	title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
        	type: "enum",
        	options: [
        	    "0" : "None",
        	    "1" : "Error",
        	    "2" : "Warning",
        	    "3" : "Info",
        	    "4" : "Debug",
        	    "5" : "Trace"
        	],
        	defaultValue: "3",
            displayDuringSetup: true,
        	required: false
        )
    }
    section("Sentinel:") {
      input "emailAddress", "text", title: "Email Address", description: "", required: true
      input "password", "password", title: "Password", description: "", required: true
    }
    section("System Monitoring:") {
        input "prefLogModeEvents", "bool", title:"Log Mode Events?", defaultValue: true, required: true
        input "prefLogHubProperties", "bool", title:"Log Hub Properties?", defaultValue: true, required: true
        input "prefLogLocationProperties", "bool", title:"Log Location Properties?", defaultValue: true, required: true
    }

    section("Devices To Monitor:") {
        input "accelerometers", "capability.accelerationSensor", title: "Accelerometers", multiple: true, required: false
        input "alarms", "capability.alarm", title: "Alarms", multiple: true, required: false
        input "batteries", "capability.battery", title: "Batteries", multiple: true, required: false
        input "beacons", "capability.beacon", title: "Beacons", multiple: true, required: false
        input "buttons", "capability.button", title: "Buttons", multiple: true, required: false
        input "cos", "capability.carbonMonoxideDetector", title: "Carbon Monoxide Detectors", multiple: true, required: false
        input "co2s", "capability.carbonDioxideMeasurement", title: "Carbon Dioxide Detectors", multiple: true, required: false
        input "colors", "capability.colorControl", title: "Color Controllers", multiple: true, required: false
        input "consumables", "capability.consumable", title: "Consumables", multiple: true, required: false
        input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
        input "doorsControllers", "capability.doorControl", title: "Door Controllers", multiple: true, required: false
        input "energyMeters", "capability.energyMeter", title: "Energy Meters", multiple: true, required: false
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity Meters", multiple: true, required: false
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminance Meters", multiple: true, required: false
        input "locks", "capability.lock", title: "Locks", multiple: true, required: false
        input "motions", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
        input "musicPlayers", "capability.musicPlayer", title: "Music Players", multiple: true, required: false
        input "peds", "capability.stepSensor", title: "Pedometers", multiple: true, required: false
        input "phMeters", "capability.pHMeasurement", title: "pH Meters", multiple: true, required: false
        input "powerMeters", "capability.powerMeter", title: "Power Meters", multiple: true, required: false
        input "presences", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false
        input "pressures", "capability.sensor", title: "Pressure Sensors", multiple: true, required: false
        input "shockSensors", "capability.shockSensor", title: "Shock Sensors", multiple: true, required: false
        input "signalStrengthMeters", "capability.signalStrength", title: "Signal Strength Meters", multiple: true, required: false
        input "sleepSensors", "capability.sleepSensor", title: "Sleep Sensors", multiple: true, required: false
        input "smokeDetectors", "capability.smokeDetector", title: "Smoke Detectors", multiple: true, required: false
        input "soundSensors", "capability.soundSensor", title: "Sound Sensors", multiple: true, required: false
		input "spls", "capability.soundPressureLevel", title: "Sound Pressure Level Sensors", multiple: true, required: false
		input "switches", "capability.switch", title: "Switches", multiple: true, required: false
        input "switchLevels", "capability.switchLevel", title: "Switch Levels", multiple: true, required: false
        input "tamperAlerts", "capability.tamperAlert", title: "Tamper Alerts", multiple: true, required: false
        input "temperatures", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: false
        input "thermostats", "capability.thermostat", title: "Thermostats", multiple: true, required: false
        input "threeAxis", "capability.threeAxis", title: "Three-axis (Orientation) Sensors", multiple: true, required: false
        input "touchs", "capability.touchSensor", title: "Touch Sensors", multiple: true, required: false
        input "uvs", "capability.ultravioletIndex", title: "UV Sensors", multiple: true, required: false
        input "valves", "capability.valve", title: "Valves", multiple: true, required: false
        input "volts", "capability.voltageMeasurement", title: "Voltage Meters", multiple: true, required: false
        input "waterSensors", "capability.waterSensor", title: "Water Sensors", multiple: true, required: false
        input "windowShades", "capability.windowShade", title: "Window Shades", multiple: true, required: false
    }

  }
}

mappings {
  path("/devices") {
    action: [
      GET: "getDevices"
    ]
  }
  path("/device/:id/status") {
    action: [
      GET: "getDeviceStatus"
    ]
  }
  path("/device/:id/command/:name") {
    action: [
        POST: "deviceCommand"
    ]
  }
  path("/resetChildDevices") {
    action: [
        GET: "deleteChildDevices"
    ]
  }

}

def deleteChildDevices() {
	removeChildDevices()
    return ["ok"]
}

def installed() {
	updated()
}

def updated() {
    logger("updated(): Updated with settings: ${settings}","debug")

	// Update internal state:
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3

	initialize()
}

def initialize() {

    // Build array of device collections and the attributes we want to report on for that collection:
    //  Note, the collection names are stored as strings. Adding references to the actual collection
    //  objects causes major issues (possibly memory issues?).
    state.deviceAttributes = []

    state.deviceAttributes << [ devices: 'accelerometers', attributes: ['acceleration']]
    state.deviceAttributes << [ devices: 'alarms', attributes: ['alarm']]
    state.deviceAttributes << [ devices: 'batteries', attributes: ['battery']]
    state.deviceAttributes << [ devices: 'beacons', attributes: ['presence']]
    state.deviceAttributes << [ devices: 'buttons', attributes: ['button']]
    state.deviceAttributes << [ devices: 'cos', attributes: ['carbonMonoxide']]
    state.deviceAttributes << [ devices: 'co2s', attributes: ['carbonDioxide']]
    state.deviceAttributes << [ devices: 'colors', attributes: ['hue','saturation','color']]
    state.deviceAttributes << [ devices: 'consumables', attributes: ['consumableStatus']]
    state.deviceAttributes << [ devices: 'contacts', attributes: ['contact']]
    state.deviceAttributes << [ devices: 'doorsControllers', attributes: ['door']]
    state.deviceAttributes << [ devices: 'energyMeters', attributes: ['energy']]
    state.deviceAttributes << [ devices: 'humidities', attributes: ['humidity']]
    state.deviceAttributes << [ devices: 'illuminances', attributes: ['illuminance']]
    state.deviceAttributes << [ devices: 'locks', attributes: ['lock']]
    state.deviceAttributes << [ devices: 'motions', attributes: ['motion']]
    state.deviceAttributes << [ devices: 'musicPlayers', attributes: ['status','level','trackDescription','trackData','mute']]
    state.deviceAttributes << [ devices: 'peds', attributes: ['steps','goal']]
    state.deviceAttributes << [ devices: 'phMeters', attributes: ['pH']]
    state.deviceAttributes << [ devices: 'powerMeters', attributes: ['power','voltage','current','powerFactor']]
    state.deviceAttributes << [ devices: 'presences', attributes: ['presence']]
    state.deviceAttributes << [ devices: 'pressures', attributes: ['pressure']]
    state.deviceAttributes << [ devices: 'shockSensors', attributes: ['shock']]
    state.deviceAttributes << [ devices: 'signalStrengthMeters', attributes: ['lqi','rssi']]
    state.deviceAttributes << [ devices: 'sleepSensors', attributes: ['sleeping']]
    state.deviceAttributes << [ devices: 'smokeDetectors', attributes: ['smoke']]
    state.deviceAttributes << [ devices: 'soundSensors', attributes: ['sound']]
	state.deviceAttributes << [ devices: 'spls', attributes: ['soundPressureLevel']]
	state.deviceAttributes << [ devices: 'switches', attributes: ['switch']]
    state.deviceAttributes << [ devices: 'switchLevels', attributes: ['level']]
    state.deviceAttributes << [ devices: 'tamperAlerts', attributes: ['tamper']]
    state.deviceAttributes << [ devices: 'temperatures', attributes: ['temperature']]
    state.deviceAttributes << [ devices: 'thermostats', attributes: ['temperature','heatingSetpoint','coolingSetpoint','thermostatSetpoint','thermostatMode','thermostatFanMode','thermostatOperatingState','thermostatSetpointMode','scheduledSetpoint','optimisation','windowFunction']]
    state.deviceAttributes << [ devices: 'threeAxis', attributes: ['threeAxis']]
    state.deviceAttributes << [ devices: 'touchs', attributes: ['touch']]
    state.deviceAttributes << [ devices: 'uvs', attributes: ['ultravioletIndex']]
    state.deviceAttributes << [ devices: 'valves', attributes: ['valve']]
    state.deviceAttributes << [ devices: 'volts', attributes: ['voltage']]
    state.deviceAttributes << [ devices: 'waterSensors', attributes: ['water']]
    state.deviceAttributes << [ devices: 'windowShades', attributes: ['windowShade']]

    subscribeToEvents()

    setWebhook()

    loadDevices()

}

def subscribeToEvents() {

	// Unsubscribe:
    unsubscribe()

	subscribe(location, null, lanResponseHandler, [filterEvents:false])

    // Subscribe to App Touch events:
    //subscribe(app,handleAppTouch)

    // Subscribe to mode events:
    //if (prefLogModeEvents) subscribe(location, "mode", handleModeEvent)

    // Subscribe to device attributes (iterate over each attribute for each device collection in state.deviceAttributes):
    def devs // dynamic variable holding device collection.

	def devices = []

	state.deviceAttributes.each { da ->
        devs = settings."${da.devices}"

        if (devs && (da.attributes)) {

            da.attributes.each { attr ->
                logger("manageSubscriptions(): Subscribing to attribute: ${attr}, for devices: ${da.devices}","info")
                // There is no need to check if all devices in the collection have the attribute.
                try{
                	subscribe(devs, attr, handleDeviceEvent)
                } catch (e) {
		      		logger("subscribe(): error subscribing: $e", "error")
  				}

            }

         }
    }
}

def deviceDetails(device) {

  def supportedAttributes = []
  device.supportedAttributes.each {
    supportedAttributes << it.name
  }

  def supportedCommands = [:]
  device.supportedCommands.each {
    def arguments = []
    it.arguments.each { arg ->
      arguments << "${arg}"
    }
    supportedCommands."${it.name}" = arguments

  }

  return [
      deviceId: device.id,
      type: device.typeName,
      label: device.label,
      manufacturerName: device.manufacturerName,
      modelName: device.modelName,
      name: device.name,
      displayName: device.displayName,
      components : [
      	capabilities: supportedAttributes.unique{ c1, c2 ->
    		c1 <=> c2
    	},
      	commands: supportedCommands
      ]
  ]
}

def getAvailableDevices(){

	def devices = []

    def attrs = [:]

    state.deviceAttributes.each { da ->
        def devs = settings."${da.devices}"
		if (devs) {

			devices += devs

            try {
                devs.each{ dev ->

					def k = dev.id

                    if ( !attrs.containsKey(k) ){
                        attrs."${k}" = []
                    }

                    attrs."${k}".addAll( da.attributes )

                }
            } catch (e) {
		      logger("getAvailableDevices(): error: $e", "error")
  			}
        }
    }

    devices = devices.unique{ dev1, dev2 ->
    	dev1.id <=> dev2.id
    }

    return [
    	devices : devices,
    	attributes : attrs
    ]
}

def getDevices() {

    logger("getDevices(): Getting a list of devices","info")

    def resp = []

	def ad = getAvailableDevices()
	def devices = ad.devices
    def attrs = ad.attributes

    devices.each { dev ->
        def networkId = dev.deviceNetworkId

        def childDevice = getChildDevice(networkId)

        if (!childDevice){
        	def details = deviceDetails(dev)
            def states = [:]
            def props = details.components.capabilities

            props.each { name ->
                def state = dev.currentState(name)
                if ( state )

                	states."${state.name}" = [
                        				value : state.value,
                    					timestamp : state.date
                                    ]
            }

            details.components.states = states

        	resp << details
        }
	}

    return resp
}

def getDeviceStatus(){

    logger("getDeviceStatus(): Getting the status of device id => ${params.id}","info")

    def resp = []

	def r = getAvailableDevices()

	def devices = r.devices
    def attrs = r.attributes

	def device = devices.find {element -> element.id == params.id}

    if (!device){
    	return httpError(404, "Not Found")
    }

    def props = attrs[params.id]

    props.each { name ->
        def state = device.currentState(name)
    	resp << state
    }

    return resp
}

def getDeviceById(id) {
	def ad = getAvailableDevices()
	def devices = ad.devices

  	return devices.find { it.id == id }
}

def deviceCommand() {
    def device = getDeviceById(params.id)
    def name = params.name

    def args = request.JSON?.args

    if (args == null) {
        args = []
    }

	logger("deviceCommand(): ${name} ${args}", "debug")

    switch(args.size()) {
        case 0:
        device."$name"()
        break;
        case 1:
        device."$name"(args.get(0))
        break;
        case 2:
        device."$name"(args.get(0),args.get(1))
        break;
        default:
            throw new Exception("Unhandled number of args")
    }

    return ["ok"]
}

def handleDeviceEvent(evt){

  try{

    def d = evt.device

    def deviceId = evt.deviceId
    def networkId = d.deviceNetworkId

    logger("handleDeviceEvent(): $evt.displayName($evt.deviceId => $evt.name:$evt.unit) $evt.value", "info")

    def childDevice = getChildDevice(networkId)

    if ( childDevice ) {
      logger("handleDeviceEvent(): device id => ${networkId}, belongs to me, skipping", "debug")
    } else {
      logger("handleDeviceEvent(): Sending update to Sentinel for device id => ${deviceId}", "debug")

      postUpdate( [
                    source : "smartthings",
                    type : "device.event",
                    payload : [
                                eventId : UUID.randomUUID().toString(),
                                attribute : evt.name,
                                deviceId : evt.deviceId,
                                capability : evt.name,
                                value : evt.value
                              ]
                            ]
                 )
	}

  } catch (e) {
      logger("handleDeviceEvent(evt): error: $e", "error")
  }

}

def lanResponseHandler(evt) {
    def map = stringToMap(evt.stringValue)

    def headers = getHttpHeaders(map.headers)
    def body = getJsonFromBase64(map.body)

    if ( headers['x-security-key'] != state.securityKey ){
        logger("lanResponseHandler(evt): ${headers['x-security-key']} is an invalid security key", "info")
        return;
    }

    logger("lanResponseHandler(evt): event body: ${body}", "trace")

    processEvent(body)
}

private processEvent(evt) {

	def deviceId = evt.payload?.id

    if ( deviceId ){

		logger("processEvent(evt): ${evt.type} for device id => ${deviceId}", "trace")

        def childDevice = getChildDevice(deviceId)

		if ( childDevice ) {

            switch (evt.type){
                case 'device.update':
                	childDevice.updateStatus( evt.payload?.value );
                break
            }
        } else {
        	logger("processEvent(evt): Could not find child device id => ${deviceId}", "trace")
        }
   	}
}

private getEmail(){
	return settings.emailAddress
}

private getPassword(){
    return settings.password
}

private getToken(){
	return "Bearer ${state.token}";
}

private login(force = false){

    try {

		if (!force){
            if ( state?.token_exp != null ) {

                long timeDiff
                def now = new Date()
                def end =  Date.parse("yyy-MM-dd'T'HH:mm:ssZ","${state.token_exp}".replace("+00:00","+0000"))

                long unxNow = now.getTime()
                long unxEnd = end.getTime()

                unxNow = unxNow/1000
                unxEnd = unxEnd/1000

                timeDiff = Math.abs(unxNow-unxEnd)
                timeDiff = Math.round(timeDiff/60)

				logger("login(force): expiration of token in ${timeDiff} minutes", "debug")

                // No need to reauth if < 6 hours
                if ( timeDiff > (6*60) )
                    return
            }
		}

        def params = [
          uri: "https://home.steventaylor.me/api/auth/login",
          body: [
            email: getEmail(),
            password: getPassword()
          ]
        ]

        httpPostJson(params) { resp ->

			logger("login(force): response: ${resp}", "trace")

            resp.headers.each {
				logger("login(force): Header => ${it.name} : ${it.value}", "debug")
            }

            state.token = resp.data.data.token

            String jwtToken = state.token
            String[] split_string = jwtToken.split("\\.")
            String base64EncodedHeader = split_string[0]
            String base64EncodedBody = split_string[1]
            String base64EncodedSignature = split_string[2]

            Base64 base64Url = new Base64(true)

			def obj = getJsonFromBase64(base64EncodedBody)

            state.token_exp = new Date( (obj.exp as long) * 1000 ).format("yyy-MM-dd'T'HH:mm:ssZ")

			logger("login(force): token => ${state.token}", "debug")
            logger("login(force): token expires => " + state.token_exp, "debug")
        }
    } catch (e) {
    	logger("login(force): error: $e", "error")
    }
}

private setWebhook(){

    login()

    try {

        state.securityKey = UUID.randomUUID().toString()

        def params = [
          uri: "https://home.steventaylor.me/api/eventbus/notifications/register",
          headers: [ Authorization : getToken() ],
          body: [
            url : getNotifyAddress(),
            method : "NOTIFY",
            securityKey : state.securityKey
          ]
        ]

        httpPostJson(params) { resp ->

			logger("setWebhook(): Response: ${resp}", "debug")

            resp.headers.each {
               logger("setWebhook(): Header => ${it.name} : ${it.value}", "debug")
            }
        }

    } catch (e) {
      	logger("setWebhook(): error: $e", "error")
    }
}

private postUpdate(body){

    login()

    try {

        def params = [
          uri: "https://home.steventaylor.me/api/eventbus/notify",
          headers: [ Authorization : getToken() ],
          body: body
        ]

        httpPostJson(params) { resp ->

			logger("postUpdate(body): Response: ${resp}", "debug")

            resp.headers.each {
               logger("postUpdate(body): Header => ${it.name} : ${it.value}", "debug")
            }

        }

    } catch (e) {
      	logger("postUpdate(body): error: $e", "error")
    }
}

private loadDevices(){

    login(true)

    try {
        def params = [
          uri: "https://home.steventaylor.me/api/devices",
          headers: [ Authorization : getToken() ]
        ]

        httpGet(params) { resp ->

   			logger("loadDevices(): Response: ${resp}", "debug")

            resp.headers.each {
                logger("loadDevices(): Header => ${it.name} : ${it.value}", "debug")
            }

            logger("loadDevices(): Content-Type: ${resp.contentType}", "debug")
            logger("loadDevices(): Data: ${resp.data}", "debug")

            createChildDevices(resp.data)
        }
    } catch (e) {
    	logger("loadDevices(): error: $e", "error")
    }

}

private createChildDevices(data){

	def hostHub = location.hubs[0]

    //removeChildDevices();

    data.devices.each {
        def device = it;

        def deviceId = device.id

        if ( device.plugin.name != "smartthings" && !getChildDevice(device.id)) {

			def newDevice = null

            switch ( device.type ){
                case "alarm.panel":
                      newDevice = addChildDevice("hashneo", "alarm-panel", deviceId, hostHub.id, [name: device.name, label: device.name, completedSetup: true])
                break;
                case "sensor.motion":
                      newDevice = addChildDevice("hashneo", "sensor-motion", deviceId, hostHub.id, [name: device.name, label: device.name, completedSetup: true])
                break;
                case "sensor.contact":
                      newDevice = addChildDevice("hashneo", "sensor-contact", deviceId, hostHub.id, [name: device.name, label: device.name, completedSetup: true])
                break;
                case "sensor.smoke":
                      newDevice = addChildDevice("hashneo", "sensor-smoke", deviceId, hostHub.id, [name: device.name, label: device.name, completedSetup: true])
                break;
                case "sensor.co2":
                      newDevice = addChildDevice("hashneo", "sensor-co2", deviceId, hostHub.id, [name: device.name, label: device.name, completedSetup: true])
                break;
            }

			if ( newDevice != null ){
                logger("createChildDevices(data): Created new ${device.type} device => ${device.name}", "debug")
            }
        }

    }

}

private getHttpHeaders(headers) {
  def obj = [:]

  def data = headers.decodeBase64();

  if (data){
      new String(data).split("\r\n").each { param ->
          def nameAndValue = param.split(":")
          obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
      }
  }
  return obj
}

private getJsonFromBase64(base64Data) {
  if (base64Data) {
  	return getJsonFromText( new String(base64Data.decodeBase64()) )
  }
  return null;
}

private getJsonFromText(text) {
  def obj = null
  if (text) {
    def slurper = new JsonSlurper()
    obj = slurper.parseText(text)
  }
  return obj
}

private getNotifyAddress() {
	def hostHub = location.hubs[0]
  	return "http://" + hostHub.localIP + ":" + hostHub.localSrvPortTCP + "/notify"
}

private String convertIPtoHex(ipAddress) {
  return ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
}

private String convertPortToHex(port) {
  return port.toString().format( '%04x', port.toInteger() ).toUpperCase()
}

private removeChildDevices() {

  getAllChildDevices().each { device ->

  	try{
      	logger("removeChildDevices():${device.id} -> ${device.name} : ${device.type}", "debug")
		deleteChildDevice(device.deviceNetworkId)
    }
    catch(e){
    	logger("removeChildDevices(): error: $e", "error")
    }
  }
}

public logger(msg, level = "debug") {

   switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }

}
