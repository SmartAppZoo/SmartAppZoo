/**
 *  SmartThings attributes into OSIsoft PI via PIWebAPI
 *
 *  Copyright 2019 Michael Horrocks
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
    name: "ST-PIWebAPI",
    namespace: "LiveMike78",
    author: "Michael Horrocks",
    description: "Push Event Values to OSIsoft PI",
    category: "My Apps",
    iconUrl: "https://106c4.wpc.azureedge.net/80106C4/Gallery-Prod/cdn/2015-02-24/prod20161101-microsoft-windowsazure-gallery/osisoft.6df9de04-c074-4e96-9f8d-1ae95fce1115.1.0.4/Icon/large.png",
    iconX2Url: "https://106c4.wpc.azureedge.net/80106C4/Gallery-Prod/cdn/2015-02-24/prod20161101-microsoft-windowsazure-gallery/osisoft.6df9de04-c074-4e96-9f8d-1ae95fce1115.1.0.4/Icon/large.png",
    iconX3Url: "https://106c4.wpc.azureedge.net/80106C4/Gallery-Prod/cdn/2015-02-24/prod20161101-microsoft-windowsazure-gallery/osisoft.6df9de04-c074-4e96-9f8d-1ae95fce1115.1.0.4/Icon/large.png")


preferences {

	// included all capabilities not marked as dead 2019-03-26
	// this includes proposed and deprecated
	
	section("Devices To Monitor:") {
		input "accelerationSensors", "capability.accelerationSensor", title: "Acceleration Sensors", multiple: true, required: false
		input "airConditionerModes", "capability.airConditionerMode", title: "Air Conditioner Modes", multiple: true, required: false
		input "airQualitySensors", "capability.airQualitySensor", title: "Air Quality Sensors", multiple: true, required: false
		input "alarms", "capability.alarm", title: "Alarms", multiple: true, required: false
		input "audioMutes", "capability.audioMute", title: "Audio Mutes", multiple: true, required: false
		input "audioTrackDatas", "capability.audioTrackData", title: "Audio Track Datas", multiple: true, required: false
		input "audioVolumes", "capability.audioVolume", title: "Audio Volumes", multiple: true, required: false
		input "batteries", "capability.battery", title: "Batteries", multiple: true, required: false
		input "beacons", "capability.beacon", title: "Beacons", multiple: true, required: false
		input "carbonDioxideMeasurements", "capability.carbonDioxideMeasurement", title: "Carbon Dioxide Measurements", multiple: true, required: false
		input "carbonMonoxideDetectors", "capability.carbonMonoxideDetector", title: "Carbon Monoxide Detectors", multiple: true, required: false
		input "colorControls", "capability.colorControl", title: "Color Controls", multiple: true, required: false
		input "colorTemperatures", "capability.colorTemperature", title: "Color Temperatures", multiple: true, required: false
		input "colors", "capability.color", title: "Colors", multiple: true, required: false
		input "colorModes", "capability.colorMode", title: "Color Modes", multiple: true, required: false
		input "consumables", "capability.consumable", title: "Consumables", multiple: true, required: false
		input "contactSensors", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
		input "demandResponseLoadControls", "capability.demandResponseLoadControl", title: "Demand Response Load Controls", multiple: true, required: false
		input "dishwasherModes", "capability.dishwasherMode", title: "Dishwasher Modes", multiple: true, required: false
		input "dishwasherOperatingStates", "capability.dishwasherOperatingState", title: "Dishwasher Operating States", multiple: true, required: false
		input "doorControls", "capability.doorControl", title: "Door Controls", multiple: true, required: false
		input "dryerModes", "capability.dryerMode", title: "Dryer Modes", multiple: true, required: false
		input "dryerOperatingStates", "capability.dryerOperatingState", title: "Dryer Operating States", multiple: true, required: false
		input "dustSensors", "capability.dustSensor", title: "Dust Sensors", multiple: true, required: false
		input "energyMeters", "capability.energyMeter", title: "Energy Meters", multiple: true, required: false
		input "estimatedTimeOfArrivals", "capability.estimatedTimeOfArrival", title: "Estimated Time Of Arrivals", multiple: true, required: false
		input "executes", "capability.execute", title: "Executes", multiple: true, required: false
		input "fanSpeeds", "capability.fanSpeed", title: "Fan Speeds", multiple: true, required: false
		input "filterStatus", "capability.filterStatus", title: "Filter Status", multiple: true, required: false
		input "garageDoorControls", "capability.garageDoorControl", title: "Garage Door Controls", multiple: true, required: false
		input "geolocations", "capability.geolocation", title: "Geolocations", multiple: true, required: false
		input "holdableButtons", "capability.holdableButton", title: "Holdable Buttons", multiple: true, required: false
		input "illuminanceMeasurements", "capability.illuminanceMeasurement", title: "Illuminance Measurements", multiple: true, required: false
		input "imageCaptures", "capability.imageCapture", title: "Image Captures", multiple: true, required: false
		input "indicators", "capability.indicator", title: "Indicators", multiple: true, required: false
		input "infraredLevels", "capability.infraredLevel", title: "Infrared Levels", multiple: true, required: false
		input "lights", "capability.light", title: "Lights", multiple: true, required: false
		input "lockOnlies", "capability.lockOnly", title: "Lock Onlies", multiple: true, required: false
		input "locks", "capability.lock", title: "Locks", multiple: true, required: false
		input "mediaInputSources", "capability.mediaInputSource", title: "Media Input Sources", multiple: true, required: false
		input "mediaPlaybackRepeats", "capability.mediaPlaybackRepeat", title: "Media Playback Repeats", multiple: true, required: false
		input "mediaPlaybackShuffles", "capability.mediaPlaybackShuffle", title: "Media Playback Shuffles", multiple: true, required: false
		input "mediaPlaybacks", "capability.mediaPlayback", title: "Media Playbacks", multiple: true, required: false
		input "mediaPresets", "capability.mediaPresets", title: "Media Presets", multiple: true, required: false
		input "motionSensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
		input "musicPlayers", "capability.musicPlayer", title: "Music Players", multiple: true, required: false
		input "odorSensors", "capability.odorSensor", title: "Odor Sensors", multiple: true, required: false
		input "outlets", "capability.outlet", title: "Outlets", multiple: true, required: false
		input "ovenModes", "capability.ovenMode", title: "Oven Modes", multiple: true, required: false
		input "ovenOperatingStates", "capability.ovenOperatingState", title: "Oven Operating States", multiple: true, required: false
		input "ovenSetpoints", "capability.ovenSetpoint", title: "Oven Setpoints", multiple: true, required: false
		input "pHMeasurements", "capability.pHMeasurement", title: "pH Measurements", multiple: true, required: false
		input "powerConsumptionReports", "capability.powerConsumptionReport", title: "Power Consumption Reports", multiple: true, required: false
		input "powerMeters", "capability.powerMeter", title: "Power Meters", multiple: true, required: false
		input "powerSources", "capability.powerSource", title: "Power Sources", multiple: true, required: false
		input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false
		input "rapidCoolings", "capability.rapidCooling", title: "Rapid Coolings", multiple: true, required: false
		input "refrigerationSetpoints", "capability.refrigerationSetpoint", title: "Refrigeration Setpoints", multiple: true, required: false
		input "relativeHumidityMeasurements", "capability.relativeHumidityMeasurement", title: "Relative Humidity Measurements", multiple: true, required: false
		input "relaySwitches", "capability.relaySwitch", title: "Relay Switches", multiple: true, required: false
		input "robotCleanerCleaningModes", "capability.robotCleanerCleaningMode", title: "Robot Cleaner Cleaning Modes", multiple: true, required: false
		input "robotCleanerMovements", "capability.robotCleanerMovement", title: "Robot Cleaner Movements", multiple: true, required: false
		input "robotCleanerTurboModes", "capability.robotCleanerTurboMode", title: "Robot Cleaner Turbo Modes", multiple: true, required: false
		input "shockSensors", "capability.shockSensor", title: "Shock Sensors", multiple: true, required: false
		input "signalStrengths", "capability.signalStrength", title: "Signal Strengths", multiple: true, required: false
		input "sleepSensors", "capability.sleepSensor", title: "Sleep Sensors", multiple: true, required: false
		input "smokeDetectors", "capability.smokeDetector", title: "Smoke Detectors", multiple: true, required: false
		input "soundPressureLevels", "capability.soundPressureLevel", title: "Sound Pressure Levels", multiple: true, required: false
		input "soundSensors", "capability.soundSensor", title: "Sound Sensors", multiple: true, required: false
		input "speechRecognitions", "capability.speechRecognition", title: "Speech Recognitions", multiple: true, required: false
		input "stepSensors", "capability.stepSensor", title: "Step Sensors", multiple: true, required: false
		input "switchLevels", "capability.switchLevel", title: "Switch Levels", multiple: true, required: false
		input "switches", "capability.switch", title: "Switches", multiple: true, required: false
		input "tamperAlerts", "capability.tamperAlert", title: "Tamper Alerts", multiple: true, required: false
		input "temperatureMeasurements", "capability.temperatureMeasurement", title: "Temperature Measurements", multiple: true, required: false
		input "thermostatCoolingSetpoints", "capability.thermostatCoolingSetpoint", title: "Thermostat Cooling Setpoints", multiple: true, required: false
		input "thermostatFanModes", "capability.thermostatFanMode", title: "Thermostat Fan Modes", multiple: true, required: false
		input "thermostatHeatingSetpoints", "capability.thermostatHeatingSetpoint", title: "Thermostat Heating Setpoints", multiple: true, required: false
		input "thermostatModes", "capability.thermostatMode", title: "Thermostat Modes", multiple: true, required: false
		input "thermostatOperatingStates", "capability.thermostatOperatingState", title: "Thermostat Operating States", multiple: true, required: false
		input "thermostatSetpoints", "capability.thermostatSetpoint", title: "Thermostat Setpoints", multiple: true, required: false
		input "thermostats", "capability.thermostat", title: "Thermostats", multiple: true, required: false
		input "threeAxis", "capability.threeAxis", title: "Three Axis", multiple: true, required: false
		input "timedSessions", "capability.timedSession", title: "Timed Sessions", multiple: true, required: false
		input "touchSensors", "capability.touchSensor", title: "Touch Sensors", multiple: true, required: false
		input "tvChannels", "capability.tvChannel", title: "Tv Channels", multiple: true, required: false
		input "ultravioletIndexs", "capability.ultravioletIndex", title: "Ultraviolet Indexs", multiple: true, required: false
		input "valves", "capability.valve", title: "Valves", multiple: true, required: false
		input "videoClips", "capability.videoClips", title: "Video Clips", multiple: true, required: false
		input "videoStreams", "capability.videoStream", title: "Video Streams", multiple: true, required: false
		input "voltageMeasurements", "capability.voltageMeasurement", title: "Voltage Measurements", multiple: true, required: false
		input "washerModes", "capability.washerMode", title: "Washer Modes", multiple: true, required: false
		input "washerOperatingStates", "capability.washerOperatingState", title: "Washer Operating States", multiple: true, required: false
		input "waterSensors", "capability.waterSensor", title: "Water Sensors", multiple: true, required: false
		input "windowShades", "capability.windowShade", title: "Window Shades", multiple: true, required: false
    	}

	section("OSIsoft PIWebAPI") {
		// Enter the PI Data Archive Name, PI Web API URL and encode64 username and password

		input "pi", "text", required: true, multiple: false, title: "PI Server Name?"
        	input "piwebapi", "text", required: true, multiple: false, title: "PI Web API Url?"
        	input "piid", "text", required: true, multiple: false, title: "PI Web API Id?"
                
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

	//// TODO: subscribe to attributes, devices, locations, etc.
	subscribe(accelerationSensors, "acceleration", evtHandler)
	subscribe(airConditionerModes, "airConditionerMode", evtHandler)
	subscribe(airQualitySensors, "airQuality", evtHandler)
	subscribe(alarms, "alarm", evtHandler)
	subscribe(audioMutes, "mute", evtHandler)
	subscribe(audioTrackDatas, "audioTrackData", evtHandler)
	subscribe(audioVolumes, "volume", evtHandler)
	subscribe(batteries, "battery", evtHandler)
	subscribe(beacons, "presence", evtHandler)
	subscribe(carbonDioxideMeasurements, "carbonDioxide", evtHandler)
	subscribe(carbonMonoxideDetectors, "carbonMonoxide", evtHandler)
	subscribe(colorControls, "color", evtHandler)
	subscribe(colorControls, "hue", evtHandler)
	subscribe(colorControls, "saturation", evtHandler)
	subscribe(colorTemperatures, "colorTemperature", evtHandler)
	subscribe(colors, "colorValue", evtHandler)
	subscribe(colorModes, "colorMode", evtHandler)
	subscribe(consumables, "consumableStatus", evtHandler)
	subscribe(contactSensors, "contact", evtHandler)
	subscribe(demandResponseLoadControls, "drlcStatus", evtHandler)
	subscribe(dishwasherModes, "dishwasherMode", evtHandler)
	subscribe(dishwasherOperatingStates, "machineState", evtHandler)
	subscribe(dishwasherOperatingStates, "supportedMachineStates", evtHandler)
	subscribe(dishwasherOperatingStates, "dishwasherJobState", evtHandler)
	subscribe(dishwasherOperatingStates, "completionTime", evtHandler)
	subscribe(doorControls, "door", evtHandler)
	subscribe(dryerModes, "dryerMode", evtHandler)
	subscribe(dryerOperatingStates, "machineState", evtHandler)
	subscribe(dryerOperatingStates, "supportedMachineStates", evtHandler)
	subscribe(dryerOperatingStates, "dryerJobState", evtHandler)
	subscribe(dryerOperatingStates, "completionTime", evtHandler)
	subscribe(dustSensors, "fineDustLevel", evtHandler)
	subscribe(dustSensors, "dustLevel", evtHandler)
	subscribe(energyMeters, "energy", evtHandler)
	subscribe(estimatedTimeOfArrivals, "eta", evtHandler)
	subscribe(executes, "data", evtHandler)
	subscribe(fanSpeeds, "fanSpeed", evtHandler)
	subscribe(filterStatus, "filterStatus", evtHandler)
	subscribe(garageDoorControls, "door", evtHandler)
	subscribe(geolocations, "latitude", evtHandler)
	subscribe(geolocations, "longitude", evtHandler)
	subscribe(geolocations, "method", evtHandler)
	subscribe(geolocations, "accuracy", evtHandler)
	subscribe(geolocations, "altitudeAccuracy", evtHandler)
	subscribe(geolocations, "heading", evtHandler)
	subscribe(geolocations, "speed", evtHandler)
	subscribe(geolocations, "lastUpdateTime", evtHandler)
	subscribe(holdableButtons, "button", evtHandler)
	subscribe(holdableButtons, "numberOfButtons", evtHandler)
	subscribe(illuminanceMeasurements, "illuminance", evtHandler)
	subscribe(imageCaptures, "image", evtHandler)
	subscribe(indicators, "indicatorStatus", evtHandler)
	subscribe(infraredLevels, "infraredLevel", evtHandler)
	subscribe(lights, "switch", evtHandler)
	subscribe(lockOnlies, "lock", evtHandler)
	subscribe(locks, "lock", evtHandler)
	subscribe(mediaInputSources, "inputSource", evtHandler)
	subscribe(mediaInputSources, "supportedInputSources", evtHandler)
	subscribe(mediaPlaybackRepeats, "playbackRepeatMode", evtHandler)
	subscribe(mediaPlaybackShuffles, "playbackShuffle", evtHandler)
	subscribe(mediaPlaybacks, "level", evtHandler)
	subscribe(mediaPlaybacks, "playbackStatus", evtHandler)
	subscribe(mediaPresets, "presets", evtHandler)
	subscribe(motionSensors, "motion", evtHandler)
	subscribe(musicPlayers, "level", evtHandler)
	subscribe(musicPlayers, "mute", evtHandler)
	subscribe(musicPlayers, "status", evtHandler)
	subscribe(musicPlayers, "trackData", evtHandler)
	subscribe(musicPlayers, "trackDescription", evtHandler)
	subscribe(odorSensors, "odorLevel", evtHandler)
	subscribe(outlets, "switch", evtHandler)
	subscribe(ovenModes, "ovenMode", evtHandler)
	subscribe(ovenOperatingStates, "machineState", evtHandler)
	subscribe(ovenOperatingStates, "supportedMachineStates", evtHandler)
	subscribe(ovenOperatingStates, "ovenJobState", evtHandler)
	subscribe(ovenOperatingStates, "completionTime", evtHandler)
	subscribe(ovenOperatingStates, "operationTime", evtHandler)
	subscribe(ovenSetpoints, "ovenSetpoint", evtHandler)
	subscribe(pHMeasurements, "pH", evtHandler)
	subscribe(powerConsumptionReports, "powerConsumption", evtHandler)
	subscribe(powerMeters, "power", evtHandler)
	subscribe(powerSources, "powerSource", evtHandler)
	subscribe(presenceSensors, "presence", evtHandler)
	subscribe(rapidCoolings, "rapidCooling", evtHandler)
	subscribe(refrigerationSetpoints, "refrigerationSetpoint", evtHandler)
	subscribe(relativeHumidityMeasurements, "humidity", evtHandler)
	subscribe(relaySwitches, "switch", evtHandler)
	subscribe(robotCleanerCleaningModes, "robotCleanerCleaningMode", evtHandler)
	subscribe(robotCleanerMovements, "robotCleanerMovement", evtHandler)
	subscribe(robotCleanerTurboModes, "robotCleanerTurboMode", evtHandler)
	subscribe(shockSensors, "shock", evtHandler)
	subscribe(signalStrengths, "lqi", evtHandler)
	subscribe(signalStrengths, "rssi", evtHandler)
	subscribe(sleepSensors, "sleeping", evtHandler)
	subscribe(smokeDetectors, "smoke", evtHandler)
	subscribe(soundPressureLevels, "soundPressureLevel", evtHandler)
	subscribe(soundSensors, "sound", evtHandler)
	subscribe(speechRecognitions, "phraseSpoken", evtHandler)
	subscribe(stepSensors, "goal", evtHandler)
	subscribe(stepSensors, "steps", evtHandler)
	subscribe(switchLevels, "level", evtHandler)
	subscribe(switches, "switch", evtHandler)
	subscribe(tamperAlerts, "tamper", evtHandler)
	subscribe(temperatureMeasurements, "temperature", evtHandler)
	subscribe(thermostatCoolingSetpoints, "coolingSetpoint", evtHandler)
	subscribe(thermostatFanModes, "thermostatFanMode", evtHandler)
	subscribe(thermostatFanModes, "supportedThermostatFanModes", evtHandler)
	subscribe(thermostatHeatingSetpoints, "heatingSetpoint", evtHandler)
	subscribe(thermostatModes, "thermostatMode", evtHandler)
	subscribe(thermostatModes, "supportedThermostatModes", evtHandler)
	subscribe(thermostatOperatingStates, "thermostatOperatingState", evtHandler)
	subscribe(thermostatSetpoints, "thermostatSetpoint", evtHandler)
	subscribe(thermostats, "coolingSetpoint", evtHandler)
	subscribe(thermostats, "coolingSetpointRange", evtHandler)
	subscribe(thermostats, "heatingSetpoint", evtHandler)
	subscribe(thermostats, "heatingSetpointRange", evtHandler)
	subscribe(thermostats, "schedule", evtHandler)
	subscribe(thermostats, "temperature", evtHandler)
	subscribe(thermostats, "thermostatFanMode", evtHandler)
	subscribe(thermostats, "supportedThermostatFanModes", evtHandler)
	subscribe(thermostats, "thermostatMode", evtHandler)
	subscribe(thermostats, "supportedThermostatModes", evtHandler)
	subscribe(thermostats, "thermostatOperatingState", evtHandler)
	subscribe(thermostats, "thermostatSetpoint", evtHandler)
	subscribe(threeAxis, "threeAxis", evtHandler)
	subscribe(timedSessions, "sessionStatus", evtHandler)
	subscribe(timedSessions, "completionTime", evtHandler)
	subscribe(touchSensors, "touch", evtHandler)
	subscribe(tvChannels, "tvChannel", evtHandler)
	subscribe(ultravioletIndexs, "ultravioletIndex", evtHandler)
	subscribe(valves, "valve", evtHandler)
	subscribe(videoClips, "videoClip", evtHandler)
	subscribe(videoStreams, "stream", evtHandler)
	subscribe(voltageMeasurements, "voltage", evtHandler)
	subscribe(washerModes, "washerMode", evtHandler)
	subscribe(washerOperatingStates, "machineState", evtHandler)
	subscribe(washerOperatingStates, "supportedMachineStates", evtHandler)
	subscribe(washerOperatingStates, "washerJobState", evtHandler)
	subscribe(washerOperatingStates, "completionTime", evtHandler)
	subscribe(waterSensors, "water", evtHandler)
	subscribe(windowShades, "windowShade", evtHandler)
 
    
}

// TODO: implement event handlers

def evtHandler(evt) {

	// tag name will be the <device name>.<event name> - for example lamp.switch
	def evtName = "${evt.name}"

	def devName = evt.getDevice().getName()
	def tagName = "$devName"+"."+"$evtName"
	def evtValue = evt.value
	def evtTime = "${evt.isoDate}"
    
	log.debug "$tagname : $evtTime : $evtValue"
	piWriter(tagName, evtValue, evtTime)
}

def getWebId(tag) {
	
    	// get WebId of tag    
   	log.debug "getWebID: trying to find WebId for $tag"
    
   	// path is \\<pi server>\<tag>
   	def ptPath = "path="+URLEncoder.encode("\\\\$pi\\$tag")
	def webId = ""
    
	def params = [
    		uri: "$piwebapi/points?$ptPath",
        	headers: [contenttype: "application/json", authorization: "basic $piid"],        
		]
    
    	try {
    		httpGet(params) { response ->
        		webId = response.data.WebId
        	}
    	}
	catch (e) {
    		log.debug "exception: $e"
	}	
    
    	log.debug "$webId"
	
	return webId    
}
    
def piWriter(tag, val, ts) {
    
    	// get the WebId for the PI Tag
	def webId = getWebId(tag)
    
    	// create the value message; (note; use digital PI Points when states are being recorded)
    	def evtJson = new groovy.json.JsonOutput().toJson(
    		[Value: "$val",
        		Timestamp: "$ts"]
        )

	def params = [
    		uri: "$piwebapi/streams/$webId/Value",
 		headers: [authorization: "basic $piid"],
        	body: evtJson
	]
    
    	// post to the PI Web API
	try {
    		httpPostJson(params) { resp ->
        		resp.headers.each {
            			log.debug "${it.name} : ${it.value}"
        		}
        		log.debug "posted $tag : $val"
    		}
	}
    	catch (e) {
    		log.debug "exception: $e"
	}	
}
