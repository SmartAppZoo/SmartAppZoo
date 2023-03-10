/**
 * SmartThings webhooks
 *
 *  Author: Harper Reed (@harper - harper@nata2.org)
 *  URL: https://github.com/harperreed/SmartThings-webhook
 */
 
/** 
 * Let's define this dude
 *
 */
definition(
  name: "Webhooks",
  author: "Harper Reed (@harper - harper@nata2.org)",
  description: "Send Smartthings events to a webhook",
  category: "My Apps",
  iconUrl: "https://s3.amazonaws.com/kinlane-productions/bw-icons/webhooks.png",
  iconX2Url: "https://s3.amazonaws.com/kinlane-productions/bw-icons/webhooks.png"
)

preferences {
  section("Webhook URL") {
    input "url", "text", title: "Webhook URL", description: "Your webhook URL", required: true
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

def installed() {
  log.debug "Installed with settings: ${settings}"
  subscribeToEvents()
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  subscribeToEvents()
  initialize()
}

def initialize() {
  subscribe(app, eventHandler)
}

def subscribeToEvents() {
  for(cap in getCapabilities()) {
  	if(settings."${cap.key}Pref" != null) {
      if(cap.value.attributes != null) {
        for(attr in cap.value.attributes) {
    	    def deviceList = settings."${cap.key}Pref"
          subscribe(deviceList, "${attr.value}", eventHandler)
        }
      }
    }
  }
}

def eventHandler(evt) {
  def state_changed = evt.isStateChange()
  def json_body = [
    id: evt.deviceId, 
    date: evt.isoDate,
    value: evt.value, 
    name: evt.name, 
    display_name: evt.displayName, 
    description: evt.descriptionText,
    source: evt.source, 
    state_changed: evt.isStateChange(),
    physical: evt.isPhysical(),
    location_id: evt.locationId,
    hub_id: evt.hubId, 
    smartapp_id: evt.installedSmartAppId
  ] 

  def json_params = [
    uri: settings.url,
    success: successClosure,
    body: json_body
  ]
    
  try {
    log.debug json_params
    //httpPostJson(json_params)
  } catch (e) {
    log.error "http post failed: $e"
  }
}

/**
 * Called when a successful webhook post happens!
 */
def successClosure = { response ->
  log.debug "Request was successful, $response"
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