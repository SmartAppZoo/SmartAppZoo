/**
 *  Slack
 *
 *  Copyright 2019 Brent Maxwell
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
  name: "Slack",
  namespace: "thebrent",
  author: "Brent Maxwell",
  description: "Slack integration",
  category: "Fun & Social",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
{
  appSetting "token"
  appSetting "botToken"
}

preferences {
  section (title:"Select Channel"){
		input "channel", "text", title: "Channel", required: true, multiple:true
	}

  section("Choose what events you want to trigger"){
    for(cap in getCapabilities()) {
      input "${cap.value.id}Pref", "capability.${cap.value.id}",
        title: "${cap.value.name}",
        required:false,
        multiple: true,
        hideWhenEmpty: true
    }
  }
}

private getSelectedDevices() {
	def devices = []
	getCapabilities()?.each {	
		try {
			if (it.cap && settings?."${it.cap}Pref") {
				devices << settings?."${it.cap}Pref"
			}
		}
		catch (e) {
			logWarn "Error while getting selected devices for capability ${it}: ${e.message}"
		}
	}	
	return devices?.flatten()?.unique { it.displayName }
}

mappings {
  path("/devices") {
    action: [
      GET: "listDevices"
    ]
  }
  path("/device/:id") {
    action: [
      GET: "deviceDetails"
    ]
  }
  path("/device/:id/attribute/:name") {
    action: [
      GET: "deviceGetAttributeValue"
    ]
  }
  path("/device/:id/command/:name") {
    action: [
      POST: "deviceCommand"
    ]
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  subscribeToEvents()
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  unschedule()
  subscribeToEvents()
  initialize()
}

def initialize() {
  // TODO: subscribe to attributes, devices, locations, etc.
}

def listDevices() {
  def resp = []
  devices.each {
    resp << [
        id: it.id,
        label: it.label,
        manufacturerName: it.manufacturerName,
        modelName: it.modelName,
        name: it.name,
        displayName: it.displayName
    ]
  }
  return resp
}

def deviceDetails() {
  def device = getDeviceById(params.id)

  def supportedAttributes = []
  device.supportedAttributes.each {
    supportedAttributes << it.name
  }

  def supportedCommands = []
  device.supportedCommands.each {
    def arguments = []
    it.arguments.each { arg ->
      arguments << "" + arg
    }
    supportedCommands << [
        name: it.name,
        arguments: arguments
    ]
  }

  return [
      id: device.id,
      label: device.label,
      manufacturerName: device.manufacturerName,
      modelName: device.modelName,
      name: device.name,
      displayName: device.displayName,
      supportedAttributes: supportedAttributes,
      supportedCommands: supportedCommands
  ]
}

def deviceGetAttributeValue() {
  def device = getDeviceById(params.id)
  def name = params.name
  def value = device.currentValue(name);
  return [
      value: value
  ]
}

def deviceCommand() {
  def device = getDeviceById(params.id)
  def name = params.name
  def args = params.arg
  if (args == null) {
    args = []
  } else if (args instanceof String) {
    args = [args]
  }
  log.debug "device command: ${name} ${args}"
  switch(args.size) {
    case 0:
      device."$name"()
      break;
    case 1:
      device."$name"(args[0])
      break;
    case 2:
      device."$name"(args[0], args[1])
      break;
    default:
      throw new Exception("Unhandled number of args")
  }
}

def getDeviceById(id) {
  return devices.find { it.id == id }
}

def mainPage() {
  dynamicPage(name: "mainPage") {
		def anythingSet = anythingSet()
    def notificationMessage = defaultNotificationMessage();
    log.debug "set $anythingSet"
		if (anythingSet) {
			section("Show message when"){
				ifSet "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true , submitOnChange:true
				ifSet "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true, submitOnChange:true
				ifSet "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true, submitOnChange:true
				ifSet "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true, submitOnChange:true
				ifSet "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true, submitOnChange:true
				ifSet "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true, submitOnChange:true
				ifSet "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true, submitOnChange:true
				ifSet "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true, submitOnChange:true
				ifSet "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true, submitOnChange:true
				ifSet "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true, submitOnChange:true
				ifSet "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
				ifSet "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true, submitOnChange:true
				ifSet "timeOfDay", "time", title: "At a Scheduled Time", required: false, submitOnChange:true
			}
		}
		def hideable = anythingSet || app.installationState == "COMPLETE"
		def sectionTitle = anythingSet ? "Select additional triggers" : "Show message when..."

		section(sectionTitle, hideable: hideable, hidden: true){
			ifUnset "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true, submitOnChange:true
			ifUnset "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true, submitOnChange:true
			ifUnset "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true, submitOnChange:true
			ifUnset "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true, submitOnChange:true
			ifUnset "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true, submitOnChange:true
			ifUnset "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true, submitOnChange:true
			ifUnset "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true, submitOnChange:true
			ifUnset "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true, submitOnChange:true
			ifUnset "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true, submitOnChange:true
			ifUnset "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true, submitOnChange:true
			ifUnset "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
			ifUnset "triggerModes", "mode", title: "System Changes Mode", description: "Select mode(s)", required: false, multiple: true, submitOnChange:true
			ifUnset "timeOfDay", "time", title: "At a Scheduled Time", required: false, submitOnChange:true
		}

		section (title:"Select Channel"){
			input "channel", "text", title: "Channel", required: true, multiple:true, submitOnChange:true
		}
    section (title: "Configure message"){
      input "defaultMessage", "bool", title: "Use Default Text:\n\"$notificationMessage\"", required: false, defaultValue: true, submitOnChange:true
		  def showMessageInput = (settings["defaultMessage"] == null || settings["defaultMessage"] == true) ? false : true;
			if (showMessageInput) {
        input "customMessage","text",title:"Use Custom Text", defaultValue:"", required:true, multiple: false
      }
    }
		section("More options", hideable: true, hidden: true) {
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
		}
	}
}

private anythingSet() {
	for (it in controlToAttributeMap) {
    log.debug ("key ${it.key} value ${settings[it.key]} ${settings[it.key]?true:false}")
    if (settings[it.key]) {
      log.debug constructMessageFor(it.value, settings[it.key])
			return true
		}
	}
	return false
}

def defaultNotificationMessage(){
	def message = "";
	for (it in controlToAttributeMap)  {
		if (settings[it.key]) {
      message = constructMessageFor(it.value, settings[it.key])
      break;
		}
	}
	return message;
}

def constructMessageFor(group, device) {
	log.debug ("$group $device")
	def message = "";
  def firstDevice;
  if (device instanceof List) {
    firstDevice = device[0];
  } else {
    firstDevice = device;
  }
  switch(group) {
    case "motion.active":
      message = "Motion detected by $firstDevice.displayName at $location.name"
      break;
    case "contact.open":
      message = "Openning detected by $firstDevice.displayName at $location.name"
      break;
    case "contact.closed":
      message = "Closing detected by $firstDevice.displayName at $location.name"
      break;
    case "acceleration.active":
      message = "Acceleration detected by $firstDevice.displayName at $location.name"
      break;
    case "switch.on":
      message = "$firstDevice.displayName turned on at $location.name"
      break;
    case "switch.off":
      message = "$firstDevice.displayName turned off at $location.name"
      break;
    case "presence.present":
      message = "$firstDevice.displayName detected arrival at $location.name"
      break;
    case "presence.not present":
      message = "$firstDevice.displayName detected departure at $location.name"
      break;
    case "smoke.detected":
      message = "Smoke detected by $firstDevice.displayName at $location.name"
        break;
    case "smoke.tested":
      message = "Smoke tested by $firstDevice.displayName at $location.name"
      break;
    case "water.wet":
      message = "Dampness detected by $firstDevice.displayName at $location.name"
      break;
    case "button.pushed":
      message = "$firstDevice.displayName pushed at $location.name"
      break;
    case "time":
    case "time.":
      message = "Scheduled notification"
      break;
    case "mode":
      message = "Mode changed at $location.name"
      break;
  }
  
  for (mode in location.modes) {
    if ("mode.$mode" == group) {
      message = "Mode changed to $location.currentMode at $location.name";
      break;
    }
  }
  return message;
}

private ifUnset(Map options, String name, String capability) {
	if (!settings[name]) {
		input(options, name, capability)
	}
}

private ifSet(Map options, String name, String capability) {
	if (settings[name]) {
		input(options, name, capability)
	}
}

def subscribeToEvents() {
  subscribe(app, eventHandler)
  for(cap in getCapabilities()) {
  	if(settings."${cap.key}Pref" != null) {
      if(cap.value.attributes != null) {
        for(attr in cap.value.attributes) {
          log.debug attr
    	  def deviceList = settings."${cap.key}Pref"
          subscribe(deviceList, "${attr.value}", eventHandler)
        }
      }
    }
  }
	if (triggerModes) {
		subscribe(location, modeChangeHandler)
	}

	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}
}

def eventHandler(evt) {
	log.trace "eventHandler(${evt?.name}: ${evt?.value})"
  def name = evt?.name;
  def value = evt?.value;
  takeAction(evt)
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
	if (evt?.value in triggerModes) {
		eventHandler(evt)
	}
}

def scheduledTimeHandler() {
  def evt = [name:"time", value:"", device : ""];
	eventHandler(evt)
}

def appTouchHandler(evt) {
	takeAction(evt)
}

def slackConversationsListUrl() { "https://slack.com/api/conversations.list" }
def slackChatPostMessageUrl() { "https://slack.com/api/chat.postMessage" }

def getChannels() {
  httpGet([
    uri: slackChannelsListUrl(),
  ])
}

def takeAction(evt) {
  if(evt == null) {
    log.debug "NPE in takeAction"
    return;
  }
	def messageToShow
  if (defaultMessage) {
    messageToShow = constructMessageFor("${evt.name}.${evt.value}", evt.device);
  } else {
    messageToShow = customMessage;
  }
	if (messageToShow) {
    log.debug "text ${messageToShow}"
    def notification = [
      token: appSettings.botToken,
      channel: channel,
      text: messageToShow,
    ];
    httpPost([
      uri: slackChatPostMessageUrl(),
      contentType: "application/json",
      body: notification
    ])
  } else {
    log.debug "No message to show"
  }
	log.trace "Exiting takeAction()"
}

Map getCapabilities() {
  [
    accelerationSensor: [
      id: "accelerationSensor",
      name: "Acceleration Sensor",
      attributes: [
        "acceleration"  
      ]
    ],
    airConditionerMode: [
      id: "airConditionerMode",
      name: "Air Conditioner Mode",
      attributes: [
        "airConditionerMode"
      ]
    ],
    airQualitySensor: [
      id: "airQualitySensor",
      name: "Air Quality Sensor",
      attributes: [
        "airQuality"
      ]
    ],
    alarm: [
      id: "alarm",
      name: "Alarm",
      attributes: [
        "alarm"
      ]
    ],
    audioMute: [
      id: "audioMute",
      name: "Audio Mute",
      attributes: [
        "mute"
      ]
    ],
    audioTrackData: [
      id: "audioTrackData",
      name: "Audio Track Data",
      attributes: [
        "audioTrackData"
      ]
    ],
    audioVolume: [
      id: "audioVolume",
      name: "Audio Volume",
      attributes: [
        "volume"
      ]
    ],
    battery: [
      id: "battery",
      name: "Battery",
      attributes: [
        "battery"
      ]
    ],
    beacon: [
      id: "beacon",
      name: "Beacon",
      attributes: [
        "presence"
      ]
    ],
    button: [
      id: "button",
      name: "Button",
      attributes: [
        "button"
      ]
    ],
    carbonDioxideMeasurement: [
      id: "carbonDioxideMeasurement",
      name: "Carbon Dioxide Measurement",
      attributes: [
        "carbonDioxide"
      ]
    ],
    carbonMonoxideDetector: [
      id: "carbonMonoxideDetector",
      name: "Carbon Monoxide Detector",
      attributes: [
        "carbonMonoxide"
      ]
    ],
    colorControl: [
      id: "colorControl",
      name: "Color Control",
      attributes: [
        "color",
        "hue",
        "saturation"
      ]
    ],
    colorTemperature: [
      id: "colorTemperature",
      name: "Color Temperature",
      attributes: [
        "colorTemperature"
      ]
    ],
    color: [
      id: "color",
      name: "Color",
      attributes: [
        "colorValue"
      ]
    ],
    colorMode: [
      id: "colorMode",
      name: "Color Mode",
      attributes: [
        "colorMode"
      ]
    ],
    consumable: [
      id: "consumable",
      name: "Consumable",
      attributes: [
        "consumableStatus"
      ]
    ],
    contactSensor: [
      id: "contactSensor",
      name: "Contact Sensor",
      attributes: [
        "contact"
      ]
    ],
    demandResponseLoadControl: [
      id: "demandResponseLoadControl",
      name: "Demand Response Load Control",
      attributes: [
        "drlcStatus"
      ]
    ],
    dishwasherMode: [
      id: "dishwasherMode",
      name: "Dishwasher Mode",
      attributes: [
        "dishwasherMode"
      ]
    ],
    dishwasherOperatingState: [
      id: "dishwasherOperatingState",
      name: "DishwasherOperatingState",
      attributes: [
        "machineState",
        "supportedMachineStates",
        "dishwasherJobState",
        "completionTime",
        
      ]
    ],
    doorControl: [
      id: "doorControl",
      name: "Door Control",
      attributes: [
        "door"
      ]
    ],
    dryerMode: [
      id: "dryerMode",
      name: "Dryer Mode",
      attributes: [
        "dryerMode"
      ]
    ],
    dryerOperatingState: [
      id: "dryerOperatingState",
      name: "Dryer Operating State",
      attributes: [
        "machineState",
        "supportedMachineStates",
        "dryerJobState",
        "completionTime"
      ]
    ],
    dustSensor: [
      id: "dustSensor",
      name: "DustSensor",
      attributes: [
        "fineDustLevel",
        "dustLevel"
      ]
    ],
    energyMeter: [
      id: "energyMeter",
      name: "Energy Meter",
      attributes: [
        "energy"
      ]
    ],
    estimatedTimeOfArrival: [
      id: "estimatedTimeOfArrival",
      name: "Estimated Time Of Arrival",
      attributes: [
        "eta"      
      ]
    ],
    execute: [
      id: "execute",
      name: "Execute",
      attributes: [
        "data"
      ]
    ],
    fanSpeed: [
      id: "fanSpeed",
      name: "Fan Speed",
      attributes: [
        "fanSpeed"
      ]
    ],
    filterStatus: [
      id: "filterStatus",
      name: "Filter Status",
      attributes: [
        "filterStatus"
      ]
    ],
    garageDoorControl: [
      id: "garageDoorControl",
      name: "Garage Door Control",
      attributes: [
        "door"
      ]
    ],
    geolocation: [
      id: "geolocation",
      name: "Geolocation",
      attributes: [
        "latitude",
        "longitude",
        "method",
        "accuracy",
        "altitudeAccuracy",
        "heading",
        "speed",
        "lastUpdateTime"
      ]
    ],
    holdableButton: [
      id: "holdableButton",
      name: "Holdable Button",
      attributes: [
        "button",
        "numberOfButtons"
      ]
    ],
    illuminanceMeasurement: [
      id: "illuminanceMeasurement",
      name: "Illuminance Measurement",
      attributes: [
        "illuminance"
      ]
    ],
    imageCapture: [
      id: "imageCapture",
      name: "Image Capture",
      attributes: [
        "image"
      ]
    ],
    indicator: [
      id: "indicator",
      name: "Indicator",
      attributes: [
        "indicatorStatus"
      ]
    ],
    infraredLevel: [
      id: "infraredLevel",
      name: "Infrared Level",
      attributes: [
        "infraredLevel"
      ]
    ],
    light: [
      id: "light",
      name: "Light",
      attributes: [
        "switch"
      ]
    ],
    lockOnly: [
      id: "lockOnly",
      name: "Lock Only",
      attributes: [
        "lock"
      ]
    ],
    lock: [
      id: "lock",
      name: "Lock",
      attributes: [
        "lock"
      ]
    ],
    mediaController: [
      id: "mediaController",
      name: "Media Controller",
      attributes: [
        "activities",
        "currentActivity"
      ]
    ],
    mediaInputSource: [
      id: "mediaInputSource",
      name: "Media Input Source",
      attributes: [
        "inputSource"
      ]
    ],
    mediaPlaybackRepeat: [
      id: "mediaPlaybackRepeat",
      name: "Media Playback Repeat",
      attributes: [
        "playbackRepeatMode"
      ]
    ],
    mediaPlaybackShuffle: [
      id: "mediaPlaybackShuffle",
      name: "Media Playback Shuffle",
      attributes: [
        "playbackShuffle"
      ]
    ],
    mediaPlayback: [
      id: "mediaPlayback",
      name: "Media Playback",
      attributes: [
        "level",
        "playbackStatus"
      ]
    ],
    mediaPresets: [
      id: "mediaPresets",
      name: "Media Presets",
      attributes: [
        "presets"
      ]
    ],
    mediaTrackControl: [
      id: "mediaTrackControl",
      name: "Media Track Control",
      attributes: []
    ],
    momentary: [
      id: "momentary",
      name: "Momentary",
      attributes: []
    ],
    motionSensor: [
      id: "motionSensor",
      name: "Motion Sensor",
      attributes: [
        "motion"
      ]
    ],
    musicPlayer: [
      id: "musicPlayer",
      name: "Music Player",
      attributes: [
        "level",
        "mute",
        "status",
        "trackData",
        "trackDescription"
      ]
    ],
    notification: [
      id: "notification",
      name: "Notification",
      attributes: []
    ],
    odorSensor: [
      id: "odorSensor",
      name: "Odor Sensor",
      attributes: [
        "odorLevel"
      ]
    ],
    outlet: [
      id: "outlet",
      name: "Outlet",
      attributes: [
        "switch"
      ]
    ],
    ovenMode: [
      id: "ovenMode",
      name: "Oven Mode",
      attributes: [
        "ovenMode"
      ]
    ],
    ovenOperatingState: [
      id: "ovenOperatingState",
      name: "Oven Operating State",
      attributes: [
        "machineState",
        "supportedMachineStates",
        "ovenJobState",
        "completionTime",
        "operationTime",
        "progress"
      ]
    ],
    ovenSetpoint: [
      id: "ovenSetpoint",
      name: "Oven Setpoint",
      attributes: [
        "ovenSetpoint"
      ]
    ],
    pHMeasurement: [
      id: "pHMeasurement",
      name: "pH Measurement",
      attributes: [
        "pH"
      ]
    ],
    powerConsumptionReport: [
      id: "powerConsumptionReport",
      name: "Power Consumption Report",
      attributes: [
        "powerConsumption"
      ]
    ],
    powerMeter: [
      id: "powerMeter",
      name: "Power Meter",
      attributes: [
        "power"
      ]
    ],
    powerSource: [
      id: "powerSource",
      name: "Power Source",
      attributes: [
        "powerSource"
      ]
    ],
    presenceSensor: [
      id: "presenceSensor",
      name: "Presence Sensor",
      attributes: [
        "presence"
      ]
    ],
    rapidCooling: [
      id: "rapidCooling",
      name: "Rapid Cooling",
      attributes: [
        "rapidCooling"
      ]
    ],
    refrigerationSetpoint: [
      id: "refrigerationSetpoint",
      name: "Refrigeration Setpoint",
      attributes: [
        "refrigerationSetpoint"
      ]
    ],
    relativeHumidityMeasurement: [
      id: "relativeHumidityMeasurement",
      name: "Relative Humidity Measurement",
      attributes: [
        "humidity"
      ]
    ],
    relaySwitch: [
      id: "relaySwitch",
      name: "Relay Switch",
      attributes: [
        "switch"
      ]
    ],
    robotCleanerCleaningMode: [
      id: "robotCleanerCleaningMode",
      name: "Robot Cleaner Cleaning Mode",
      attributes: [
        "robotCleanerCleaningMode"
      ]
    ],
    robotCleanerMovement: [
      id: "robotCleanerMovement",
      name: "Robot Cleaner Movement",
      attributes: [
        "robotCleanerMovement"
      ]
    ],
    robotCleanerTurboMode: [
      id: "robotCleanerTurboMode",
      name: "Robot Cleaner Turbo Mode",
      attributes: [
        "robotCleanerTurboMode"
      ]
    ],
    shockSensor: [
      id: "shockSensor",
      name: "Shock Sensor",
      attributes: [
        "shock"
      ]
    ],
    signalStrength: [
      id: "signalStrength",
      name: "Signal Strength",
      attributes: [
        "lqi",
        "rssi"
      ]
    ],
    sleepSensor: [
      id: "sleepSensor",
      name: "Sleep Sensor",
      attributes: [
        "sleeping"
      ]
    ],
    smokeDetector: [
      id: "smokeDetector",
      name: "Smoke Detector",
      attributes: [
        "smoke"
      ]
    ],
    soundPressureLevel: [
      id: "soundPressureLevel",
      name: "Sound Pressure Level",
      attributes: [
        "soundPressureLevel"
      ]
    ],
    soundSensor: [
      id: "soundSensor",
      name: "Sound Sensor",
      attributes: [
        "sound"
      ]
    ],
    speechRecognition: [
      id: "speechRecognition",
      name: "Speech Recognition",
      attributes: [
        "phraseSpoken"
      ]
    ],
    speechSynthesis: [
      id: "speechSynthesis",
      name: "Speech Synthesis",
      attributes: []
    ],
    stepSensor: [
      id: "stepSensor",
      name: "Step Sensor",
      attributes: [
        "goal",
        "steps"
      ]
    ],
    switchLevel: [
      id: "switchLevel",
      name: "Switch Level",
      attributes: [
        "level"
      ]
    ],
    _switch: [
      id: "switch",
      name: "Switch",
      attributes: [
        "switch"
      ] 
    ],
    tamperAlert: [
      id: "tamperAlert",
      name: "Tamper Alert",
      attributes: [
        "tamper"
      ]
    ],
    temperatureMeasurement: [
      id: "temperatureMeasurement",
      name: "Temperature Measurement",
      attributes: [
        "temperature"
      ]
    ],
    thermostatCoolingSetpoint: [
      id: "thermostatCoolingSetpoint",
      name: "Thermostat Cooling Setpoint",
      attributes: [
        "coolingSetpoint"
      ]
    ],
    thermostatFanMode: [
      id: "thermostatFanMode",
      name: "Thermostat Fan Mode",
      attributes: [
        "thermostatFanMode"
      ]
    ],
    thermostatHeatingSetpoint: [
      id: "thermostatHeatingSetpoint",
      name: "Thermostat Heating Setpoint",
      attributes: [
        "heatingSetpoint"
      ]
    ],
    thermostatMode: [
      id: "thermostatMode",
      name: "Thermostat Mode",
      attributes: [
        "thermostatMode"
      ]
    ],
    thermostatOperatingState: [
      id: "thermostatOperatingState",
      name: "Thermostat Operating State",
      attributes: [
        "thermostatOperatingState"
      ]
    ],
    thermostatSetpoint: [
      id: "thermostatSetpoint",
      name: "Thermostat Setpoint",
      attributes: [
        "thermostatSetpoint"
      ]
    ],
    thermostat: [
      id: "thermostat",
      name: "Thermostat",
      attributes: [
        "coolingSetpoint",
        "coolingSetpointRange",
        "heatingSetpoint",
        "heatingSetpointRange",
        "schedule",
        "temperature",
        "thermostatFanMode",
        "thermostatMode",
        "thermostatOperatingState",
        "thermostatSetpoint",
        "thermostatSetpointRange"
      ]
    ],
    threeAxis: [
      id: "threeAxis",
      name: "Three Axis",
      attributes: [
        "threeAxis"
      ]
    ],
    timedSession: [
      id: "timedSession",
      name: "Timed Session",
      attributes: [
        "sessionStatus",
        "completionTime"
      ]
    ],
    tone: [
      id: "tone",
      name: "Tone",
      attributes: []
    ],
    touchSensor: [
      id: "touchSensor",
      name: "Touch Sensor",
      attributes: [
        "touch"
      ]
    ],
    tvChannel: [
      id: "tvChannel",
      name: "TV Channel",
      attributes: [
        "tvChannel"
      ]
    ],
    ultravioletIndex: [
      id: "ultravioletIndex",
      name: "Ultraviolet Index",
      attributes: [
        "ultravioletIndex"
      ]
    ],
    valve: [
      id: "valve",
      name: "Valve",
      attributes: [
        "valve"
      ]
    ],
    videoClips: [
      id: "videoClips",
      name: "Video Clips",
      attributes: [
        "videoClip"
      ]
    ],
    videoStream: [
      id: "videoStream",
      name: "Video Stream",
      attributes: [
        "stream"
      ]
    ],
    voltageMeasurement: [
      id: "voltageMeasurement",
      name: "Voltage Measurement",
      attributes: [
        "voltage"
      ]
    ],
    washerMode: [
      id: "washerMode",
      name: "Washer Mode",
      attributes: [
        "washerMode"
      ]
    ],
    washerOperatingState: [
      id: "washerOperatingState",
      name: "Washer Operating State",
      attributes: [
        "machineState",
        "washerJobState",
        "completionTime"
      ]
    ],
    waterSensor: [
      id: "waterSensor",
      name: "Water Sensor",
      attributes: [
        "water"
      ]
    ],
    windowShade: [
      id: "windowShade",
      name: "Window Shade",
      attributes: [
        "windowShade"
      ]
    ]
  ]
}