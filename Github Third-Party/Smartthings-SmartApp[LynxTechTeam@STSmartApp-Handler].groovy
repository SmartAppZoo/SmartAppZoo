/**
 *  VirtualKEY Access Control
 *
 *  Copyright 2017 VirtualKEY
 *
 *  Version is v4.7.3
 */
definition(
	name: "VirtualKEY Access Control System",
	namespace: "com.virtualkey.smartthings.smartapp",
	author: "VirtualKEY",
	description: "VirtualKEY Access Control System",
	category: "Safety & Security",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

  section ("Allow external service to control these things...") {
	input "locks", "capability.lock", multiple: true, required: true
	input (name:"thermostatDevice", type: "capability.thermostatMode", title: "Select Thermostats", required: false, multiple: true)
	input (name:"waterLeakageSensors", type: "capability.waterSensor", title: "Select Water Leak Sensors", required: false, multiple: true)
	input (name:"doorsensors", type: "capability.contactSensor", title: "Select Door Sensors", required: false, multiple: true)
	input (name:"doorControl", type: "capability.doorControl", title: "Select Door Opener", required: false, multiple: true)

  }
  

}
import physicalgraph.zwave.commands.doorlockv1.*
import physicalgraph.zwave.commands.usercodev1.*

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
	
	subscribe(locks, "lock", lockHandler)
	subscribe(locks, "codeChanged", lockHandler)
	subscribe(locks, "codeReport", lockHandler)
	subscribe(locks, "tamper", lockHandler)
	subscribe(locks, "battery", batteryHandler)
	subscribe(locks, "reportAllCodes", lockHandler)
	subscribe(waterLeakageSensors,"water",waterLeakSensorHandler)
	subscribe(thermostatDevice,"thermostat",thermostatDeviceHandler)
	subscribe(doorsensors, "status", doorSensorHandler)
	subscribe(doorsensors, "contact", doorSensorHandler)
	subscribe(doorControl, "door", doorControlHandler)

}

include 'asynchttp_v1'

def thermostatDeviceHandler(thermostatEvent){
	log.debug "thermostatDeviceHandler is invoke";
	def evntDevice = thermostatEvent.getDevice();

	log.debug "Thermostats events belongs to Device id is : ${evntDevice.getId()}";
	
}


def waterLeakSensorHandler(evt){
	 log.debug "waterLeakSensorHandler is invoke";
	def evntDevice = evt.getDevice();

	log.debug "waterLeakSensorHandler Device id is : ${evntDevice.getId()}";
	log.debug "waterLeakSensorHandler Hub name is : ${evntDevice.hub.name}"
	
	log.debug "waterLeakSensorHandler Hub id is : ${evntDevice.hub.id}"
	log.debug "waterLeakSensorHandler The device id for this event: ${evt.deviceId}"
	
	log.debug "waterLeakSensorHandler event display name: ${evt.displayName}"
	
	log.debug "waterLeakSensorHandler This event name is ${evt.name}"
	log.debug "waterLeakSensorHandler The value of this event is different from its previous value: ${evt.isStateChange()}"
	log.debug "waterLeakSensorHandler Date: ${evt.date}";
	log.debug("waterLeakSensorHandler event current water status ${evntDevice.currentWater}")
	log.debug("waterLeakSensorHandler event current battery level ${evntDevice.currentBattery}")
	def water = evntDevice.currentWater
	def battery = evntDevice.currentBattery
	def temperature = evntDevice.currentTemperature
	def lock_id=evntDevice.getId();
	log.debug "waterLeakSensorHandler Event happened on water leak sensor $lock_id"
	
	log.debug("waterLeakSensorHandler event data ${evt.data}")
	def jsonData = '{"water":"'+water+'","battery":'+battery+',"temperature":'+temperature+',"eventTypeId":158}'
	
	/*
	def paramsEvent = [
		uri: 'https://admin.getlynx.co/lock/webhook_smartthing_events_handler',
		query: [lockId: lock_id, descriptionText: evt.descriptionText, date: evt.date, name: evt.name, source: evt.source, id: String.valueOf(evt.id), data: jsonData],
		contentType: 'application/json'
	]
	log.debug "webhook call ${paramsEvent}"
	try{
		asynchttp_v1.get('responseHandlerMethod', paramsEvent)
	  } catch (e) {
		log.debug "something went wrong: $e"
	}
	*/


	def paramsEvent2 = [
		uri: 'https://api.getlynx.co/ProdV1.1/webhook/smartthings',
		query: [lockId: lock_id, descriptionText: evt.descriptionText, date: evt.date, name: evt.name, source: evt.source, id: String.valueOf(evt.id), data: jsonData],
		contentType: 'application/json'
	]
	log.debug "webhook call for sqs endpoint ${paramsEvent2}"
	try{
		asynchttp_v1.get('responseHandlerMethod', paramsEvent2)
	} catch (e) {
		log.debug "something went wrong: $e"
	}

}

def batteryHandler(evt){
	
	log.debug "batteryHandler is invoke";
	def evntDevice = evt.getDevice();

	log.debug "Device id is : ${evntDevice.getId()}";
	log.debug "Hub name is : ${evntDevice.hub.name}"
	
	log.debug "Hub id is : ${evntDevice.hub.id}"
	log.debug "The device id for this event: ${evt.deviceId}"
	
	log.debug "event display name: ${evt.displayName}"
	
	log.debug "This event name is ${evt.name}"
	log.debug "The value of this event is different from its previous value: ${evt.isStateChange()}"
	log.debug "Date: ${evt.date}";
	
	def lock_id=evntDevice.getId();
	log.debug "Event happened on lock $lock_id"
	
	log.debug("battery handler event data ${evt.data}")
	def slurper = new groovy.json.JsonSlurper()
	 def result = slurper.parseText(evt.data)
	log.debug("battery handler vk data ${result.vkData}")
	if(result != null){
		if(result.vkData != null){
			def eventData = groovy.json.JsonOutput.toJson(result.vkData)

/*
			def paramsEvent = [
				uri: 'https://admin.getlynx.co/lock/webhook_smartthing_events_handler',
				query: [lockId: evt.deviceId, descriptionText: evt.descriptionText, date: evt.date, name: evt.name, source: evt.source, id: String.valueOf(evt.id), data: eventData],
				contentType: 'application/json'
			]

			//def eventData = groovy.json.JsonOutput.toJson(evt.data)

			log.debug "webhook call ${paramsEvent}"
			try{
				asynchttp_v1.get('responseHandlerMethod', paramsEvent)
			  } catch (e) {
				log.debug "something went wrong: $e"
			}
*/
			def paramsEvent2 = [
				uri: 'https://api.getlynx.co/ProdV1.1/webhook/smartthings',
				query: [lockId: evt.deviceId, descriptionText: evt.descriptionText, date: evt.date, name: evt.name, source: evt.source, id: String.valueOf(evt.id), data: eventData],
				contentType: 'application/json'
			]
			log.debug "webhook call for sqs endpoint ${paramsEvent2}"
			try{
				asynchttp_v1.get('responseHandlerMethod', paramsEvent2)
			} catch (e) {
				log.debug "something went wrong: $e"
			}

		}
	}
}

// TODO: implement event handlers
def lockHandler(evt) {
	log.debug "lockHandler is invoke";
	def evntDevice = evt.getDevice();

	log.debug "Device id is : ${evntDevice.getId()}";
	log.debug "Hub name is : ${evntDevice.hub.name}"
	
	log.debug "Hub id is : ${evntDevice.hub.id}"
	log.debug "The device id for this event: ${evt.deviceId}"
	
	log.debug "event display name: ${evt.displayName}"
	
	log.debug "This event name is ${evt.name}"
	log.debug "The value of this event is different from its previous value: ${evt.isStateChange()}"
	log.debug "Date: ${evt.date}";
	
	def lock_id=evntDevice.getId();
	log.debug "Event happened on lock $lock_id"

/*
	def paramsEvent = [
		uri: 'https://admin.getlynx.co/lock/webhook_smartthing_events_handler',
		query: [lockId: evt.deviceId, descriptionText: evt.descriptionText, date: evt.date, name: evt.name, source: evt.source, id: String.valueOf(evt.id), data: evt.data],
		contentType: 'application/json'
	]
	log.debug "webhook call ${paramsEvent}"
	try{
		asynchttp_v1.get('responseHandlerMethod', paramsEvent)
	  } catch (e) {
		log.debug "something went wrong: $e"
	}
*/

	def paramsEvent2 = [
		uri: 'https://api.getlynx.co/ProdV1.1/webhook/smartthings',
		query: [lockId: evt.deviceId, descriptionText: evt.descriptionText, date: evt.date, name: evt.name, source: evt.source, id: String.valueOf(evt.id), data: evt.data],
		contentType: 'application/json'
	]
	log.debug "webhook call for sqs endpoint ${paramsEvent2}"
	try{
		asynchttp_v1.get('responseHandlerMethod', paramsEvent2)
	  } catch (e) {
		log.debug "something went wrong: $e"
	}

}

def responseHandlerMethod(response, data) {
	log.debug "got response data: ${response.getData()}"
	log.debug "data map passed to handler method is: $data"
}


mappings {

	path("/getLock/:lockid") {
		action: [
			GET: "getLock"
		]
	}
	
	path("/lock/:lockid") {
		action: [
			GET: "lock"
		]
	}
	
	path("/unlock/:lockid") {
		action: [
			GET: "unlock"
		]
	}
	
	path("/setCode/:lockid/:pin/:confirmation") {
		action: [
			GET: "setCode"
		]
	}
	
	path("/deleteCode/:lockid/:slotNumber/:confirmation/") {
		action: [
			GET: "deleteCode"
		]
	}
	
	path("/requestCode/:lockid/:slotnumber") {
		action: [
			GET: "requestCode"
		]
	}

	path("/hubList") {
		action: [
			GET: "hubList"
		]
	}
	
	path("/locks") {
		action: [
			GET: "locks"
		]
	}
	
	path("/refresh/:lockid") {
		action: [
			GET: "refresh"
		]
	}
	
	path("/getEvents/:lockid/:startDate/:endDate") {
		action: [
			GET: "getEvents"
		]
	}
	
	path("/setScheduleEntryLockCmd/:lockId/:slotNumber") {
		action: [
			POST: "setScheduleEntryLockCmd"
		]
	}
	
	path("/getBatteryFromLock/:lockId"){
		action: [
			GET: "getBatteryFromLock"
		]
	}
	
	path("/setThermostatCoolingPoint/:deviceId/:temperatureinF"){
		action: [
			GET: "setThermostatCoolingPoint"
		]  
	}

	path("/setThermostatHeatingPoint/:deviceId/:temperatureinF"){
		action: [
			GET: "setThermostatHeatingPoint"
		]  
	}

	path("/getThermostatCoolingPoint/:deviceId"){
		action: [
			GET: "getThermostatCoolingPoint"
		]  
	}

	path("/getThermostatHeatingPoint/:deviceId"){
		action: [
			GET: "getThermostatHeatingPoint"
		]  
	}

	path("/setThermostatMode/:deviceId/:thermostatMode"){
		action: [
			GET: "setThermostatMode"
		]  
	}

	path("/getThermostatMode/:deviceId/:thermostatMode"){
		action: [
			GET: "getThermostatMode"
		]  
	}


	path("/getThermostatDevices"){
		action: [
			GET: "getThermostatDevices"
		]  
	}

	path("/getThermostatDeviceStatus/:deviceId"){
		action: [
			GET: "getThermostatDeviceStatus"
		]  
	}

	path("/setThermostatToVacantMode/:deviceId/:coolingTemperatureInF/:heatingTemperatureInF"){
		action: [
			GET: "setThermostatToVacantMode"
		]  
	}
	
	path("/setTimeParameters/:deviceId"){
		action: [
			GET: "setTimeParameters"
		]  
	}
	
	path("/setScheduleTimeOffset/:deviceId"){
		action: [
			GET: "setScheduleTimeOffset"
		]  
	}
	
	
	path("/setConfigParameters/:deviceId/:parameterNumber/:parameterValue"){
		action: [
			GET: "setConfigParameters"
		]  
	}
	path("/deleteAllCodes/:deviceId"){
		action: [
			GET: "deleteAllCodes"
		]  
	}
	
	path("/getMSRFromLock/:lockId"){
		action: [
			GET: "getMSRFromLock"
		]
	}

	path("/getVersionsFromLock/:lockId"){
		action: [
			GET: "getVersionsFromLock"
		]
	}

	path("/getMaxSlotsFromLock/:lockId"){
		action: [
			GET: "getMaxSlotsFromLock"
		]
	}
	
	path("/getWaterLeakSensors") {
		action: [
			GET: "getWaterLeakSensors"
		]
	}
	
	path("/getWaterLeakSensorStatus/:deviceId") {
		action: [
			GET: "getWaterLeakSensorStatus"
		]
	}
	
	path("/clearYearDaySchedule/:lockId/:slotNumber") {
		action: [
			GET: "clearYearDaySchedule"
		]
	}
		
	path("/clearAllYearDaySchedule/:lockId") {
		action: [
			GET: "clearAllYearDaySchedule"
		]
	}
	
	path("/getYearDaySchedule/:lockId/:slotNumber") {
		action: [
			GET: "getYearDaySchedule"
		]
	}

	path("/getDoorSensors"){
		action: [
			GET: "getDoorSensors"
		]  
	}

	path("/getDoorSensorStatus/:deviceId") {
		action: [
			GET: "getDoorSensorStatus"
		]
	}

	path("/getDoorControlDevices"){
		action: [
			GET: "getDoorControlDevices"
		]  
	}

	path("/getDoorControlDeviceStatus/:deviceId") {
		action: [
			GET: "getDoorControlDeviceStatus"
		]
	}

	path("/openDoorControl/:deviceId") {
		action: [
			GET: "openDoorControl"
		]
	}
	
	path("/closeDoorControl/:deviceId") {
		action: [
			GET: "closeDoorControl"
		]
	}

	path("/refreshDoorControlDevice/:deviceId") {
		action: [
			GET: "refreshDoorControlDevice"
		]
	}


}


def getThermostatDevices(){
	
	def resp = [];
	def hub;
	def thermostatList = [];
	
	thermostatDevice.each{
		def deviceId = String.valueOf(it.id);
		log.debug "Display name is $it.displayName"
		log.debug "Display name is $deviceId "

		def deviceStatus = it.getStatus();
		def deviceModelName = it.getModelName()
		def deviceManufacturer = it.getManufacturerName();
		def deviceLastActivity = it.getLastActivity();
		def deivceLabel = it.getLabel();
		
		def thermostat = 	it.currentThermostat
		def coolingSetpoint = 	it.currentCoolingSetpoint
		def supportedThermostatFanModes =	it.currentSupportedThermostatFanModes
		def maxHeatingSetpoint =	it.currentMaxHeatingSetpoint
		//def thermostatSetpointRange =	it.currentThermostatSetpointRange
		def thermostatSetpointRange = ''
		def thermostatSetpoint =	it.currentThermostatSetpoint
		def heatingSetpoint =	it.currentHeatingSetpoint
		def maxCoolingSetpoint =	it.currentMaxCoolingSetpoint
		def minHeatingSetpoint =	it.currentMinHeatingSetpoint
		def checkInterval =	it.currentCheckInterval
		def healthStatus =	it.currentHealthStatus
		def thermostatOperatingState =	it.currentThermostatOperatingState
		def minCoolingSetpoint =	it.currentMinCoolingSetpoint
		def supportedThermostatModes =	it.currentSupportedThermostatModes
		def schedule =	it.currentSchedule
		def thermostatMode =	it.currentThermostatMode
		//def coolingSetpointRange =	it.currentCoolingSetpointRange
		def coolingSetpointRange =	''
		def temperature =	it.currentTemperature
		def supportedCaps = it.capabilities
		def batteryLevel = it.currentBattery
		
		log.debug "Device : thermostat $thermostat"
		log.debug "Device : coolingSetpoint $coolingSetpoint"
		log.debug "Device : supportedThermostatFanModes $supportedThermostatFanModes"
		log.debug "Device : maxHeatingSetpoint $maxHeatingSetpoint"
		log.debug "Device : thermostatSetpointRange $thermostatSetpointRange"
		log.debug "Device : thermostatSetpoint $thermostatSetpoint"
		log.debug "Device : heatingSetpoint $heatingSetpoint"
		log.debug "Device : maxCoolingSetpoint $maxCoolingSetpoint"
		log.debug "Device : minHeatingSetpoint $minHeatingSetpoint"
		log.debug "Device : checkInterval $checkInterval"
		log.debug "Device : healthStatus $healthStatus"
		log.debug "Device : thermostatOperatingState $thermostatOperatingState"
		log.debug "Device : minCoolingSetpoint $minCoolingSetpoint"
		log.debug "Device : supportedThermostatModes $supportedThermostatModes"
		log.debug "Device : schedule $schedule"
		log.debug "Device : thermostatMode $thermostatMode"
		log.debug "Device : coolingSetpointRange $coolingSetpointRange"
		log.debug "Device : temperature $temperature"
		log.debug "Device : battery level $batteryLevel"
		
		
		log.debug "Current device status is : $deviceStatus";
			
		thermostatList << [name: it.displayName, deviceId: deviceId,deviceStatus:deviceStatus,deviceLastActivity:deviceLastActivity,deviceManufacturer:deviceManufacturer,deviceModelName:deviceModelName,deivceLabel:deivceLabel,locationTemperatureScale:location.temperatureScale,coolingSetpoint:coolingSetpoint,heatingSetpoint:heatingSetpoint,thermostatMode:thermostatMode,thermostatSetpoint:thermostatSetpoint,temperature:temperature,thermostatOperatingState:thermostatOperatingState, batteryLevel: batteryLevel ];
	}
	
	resp << [status:true,thermostatList:thermostatList];
	return resp;
}

def getThermostatDeviceStatus(){
	log.debug "Getting the Thermostat status for device $params.deviceId";
	def resp = [status: false];
	def thermostatStatus = {};

	String deviceId = params.deviceId.toString();
	
	if(deviceId == null){
		log.debug "Device Id not provided, return status as false"
		return resp;
	}

	thermostatDevice.each{

		if(it.getId() == deviceId){

			log.debug "Display name is $it.displayName"
			log.debug "Display name is $deviceId "
	
			def deviceStatus = it.getStatus();
			def deviceModelName = it.getModelName()
			def deviceManufacturer = it.getManufacturerName();
			def deviceLastActivity = it.getLastActivity();
			def deviceLabel = it.getLabel();
			
			def thermostat = 	it.currentThermostat
			def coolingSetpoint = 	it.currentCoolingSetpoint
			def supportedThermostatFanModes =	it.currentSupportedThermostatFanModes
			def maxHeatingSetpoint =	it.currentMaxHeatingSetpoint
			//def thermostatSetpointRange =	it.currentThermostatSetpointRange
			def thermostatSetpointRange = ''
			def thermostatSetpoint =	it.currentThermostatSetpoint
			def heatingSetpoint =	it.currentHeatingSetpoint
			def maxCoolingSetpoint =	it.currentMaxCoolingSetpoint
			def minHeatingSetpoint =	it.currentMinHeatingSetpoint
			def checkInterval =	it.currentCheckInterval
			def healthStatus =	it.currentHealthStatus
			def thermostatOperatingState =	it.currentThermostatOperatingState
			def minCoolingSetpoint =	it.currentMinCoolingSetpoint
			def supportedThermostatModes =	it.currentSupportedThermostatModes
			def schedule =	it.currentSchedule
			def thermostatMode =	it.currentThermostatMode
			//def coolingSetpointRange =	it.currentCoolingSetpointRange
			def coolingSetpointRange =	''
			def temperature =	it.currentTemperature
			def supportedCaps = it.capabilities
			def humidity = it.currentHumidity
			def batteryLevel = it.currentBattery
			
			log.debug "Device : humidity $humidity"
			log.debug "Device : thermostat $thermostat"
			log.debug "Device : coolingSetpoint $coolingSetpoint"
			log.debug "Device : supportedThermostatFanModes $supportedThermostatFanModes"
			log.debug "Device : maxHeatingSetpoint $maxHeatingSetpoint"
			log.debug "Device : thermostatSetpointRange $thermostatSetpointRange"
			log.debug "Device : thermostatSetpoint $thermostatSetpoint"
			log.debug "Device : heatingSetpoint $heatingSetpoint"
			log.debug "Device : maxCoolingSetpoint $maxCoolingSetpoint"
			log.debug "Device : minHeatingSetpoint $minHeatingSetpoint"
			log.debug "Device : checkInterval $checkInterval"
			log.debug "Device : healthStatus $healthStatus"
			log.debug "Device : thermostatOperatingState $thermostatOperatingState"
			log.debug "Device : minCoolingSetpoint $minCoolingSetpoint"
			log.debug "Device : supportedThermostatModes $supportedThermostatModes"
			log.debug "Device : schedule $schedule"
			log.debug "Device : thermostatMode $thermostatMode"
			log.debug "Device : coolingSetpointRange $coolingSetpointRange"
			log.debug "Device : temperature $temperature"
			log.debug "Current device status is : $deviceStatus";
			log.debug "Device : battery level $batteryLevel"
				
			thermostatStatus =  [name: it.displayName, deviceId: deviceId,deviceStatus:deviceStatus,deviceLastActivity:deviceLastActivity,deviceManufacturer:deviceManufacturer,deviceModelName:deviceModelName,deviceLabel:deviceLabel,locationTemperatureScale:location.temperatureScale,coolingSetpoint:coolingSetpoint,heatingSetpoint:heatingSetpoint,thermostatMode:thermostatMode,thermostatSetpoint:thermostatSetpoint,temperature:temperature,thermostatOperatingState:thermostatOperatingState, humidity:humidity, supportedThermostatModes: supportedThermostatModes , supportedThermostatFanModes: supportedThermostatFanModes, maxHeatingSetpoint: maxHeatingSetpoint, minHeatingSetpoint: minHeatingSetpoint, maxCoolingSetpoint: maxCoolingSetpoint, minCoolingSetpoint: minCoolingSetpoint, batteryLevel: batteryLevel ];
			log.debug "thermostatStatus : $thermostatStatus"
			
			return resp << [status: true, thermostatStatus: thermostatStatus];
		}
	}
	return resp;
}

//Set cooling set point for a particular thermostat
def setThermostatCoolingPoint(){
	def resp = [status: false];
	
	String deviceId = params.deviceId.toString();
	float coolingPointInF = Float.parseFloat(params.temperatureinF)//27.0f
	float coolingPoint=0.0f;
	def scale = location.temperatureScale;
	log.debug "Current temp scale is $scale"
	
	if(scale == "C"){
		log.debug "Converting to celcius"
		coolingPoint = ((float)coolingPointInF - 32.0f) * (5.0f/9.0f)
	}else if(scale=="F"){
		log.debug "Current temp scale is F so no change"
		coolingPoint = coolingPointInF;
	}
	
	log.debug "After conversion cooling point value is $coolingPoint "
	if(deviceId == null){
		return resp;
	}
	
	thermostatDevice.each{
		if(it.getId() == deviceId){
			log.debug "Setting cooling point value is $coolingPoint"
			if(coolingPoint != 0.0f){
				it.setCoolingSetpoint(coolingPoint);
				resp << [status:true,]
			}
		}
	}
	
	return resp;
}



def getThermostatCoolingPoint(){
	def resp = [status: false];
   
	String deviceId = params.deviceId.toString();
	
	if(deviceId == null){
		return resp;
	}
	thermostatDevice.each{
		if(it.getId() == deviceId){
			def coolingSetPointLatest = it.latestValue("coolingSetpoint")
			def coolingSetPointCurrent = it.currentValue("coolingSetpoint")
			log.debug "Latest value is $coolingSetPointLatest"
			log.debug "Current value is $coolingSetPointCurrent"
			resp <<  [status: true,coolingSetPointLatest:coolingSetPointLatest,coolingSetPointCurrent:coolingSetPointCurrent];
			return resp;
		}
	}

	return resp;
}

def setThermostatHeatingPoint(){
	def resp = [status: false];
	
	String deviceId = params.deviceId.toString();
	
	float heatingPointInF = Float.parseFloat(params.temperatureinF)//27.0f
	float heatingPoint=0.0f;
	def scale = location.temperatureScale;
	log.debug "Current temp scale is $scale"
	if(scale == "C"){
			log.debug "Converting to celcius"
			heatingPoint = ((float)heatingPointInF - 32.0f) * (5.0f/9.0f)
	}else if(scale=="F"){
			log.debug "Current temp scale is F so no change"
			heatingPoint=heatingPointInF;
	}
	log.debug "After conversion heating point value is $heatingPoint "
	if(deviceId == null){
		return resp;
	}
	
	thermostatDevice.each{
		if(it.getId() == deviceId){
			log.debug "Setting heating point value is $heatingPoint for $deviceId"
			if(heatingPoint != 0.0f){
				it.setHeatingSetpoint(heatingPoint);
				resp << [status:true]
				return resp;
			}
		   }
	}
	return resp;
}

def getThermostatHeatingPoint(){
	def resp = [status: false];
	
	String deviceId = params.deviceId.toString();
	
	if(deviceId == null){
		return resp;
	}
	
	thermostatDevice.each{
		if(it.getId() == deviceId){
			def heatingSetpointLatest = it.latestValue("heatingSetpoint")
			def heatingSetpointCurrent = it.currentValue("heatingSetpoint")
			log.debug "Latest value is $heatingSetpointLatest"
			log.debug "Current value is $heatingSetpointCurrent"
			resp <<  [status: true,heatingSetpointLatest:heatingSetpointLatest,heatingSetpointCurrent:heatingSetpointCurrent];
			return resp;
		}
	}

	return resp;
}

def setThermostatMode(){
	def resp = [status: false];
	String thermostatMode = params.thermostatMode.toString();
	String deviceId = params.deviceId.toString();
	
	if(deviceId == null || thermostatMode == null  ){
		return resp;
	}
	
	thermostatDevice.each{
		if(it.getId() == deviceId){
			if (thermostatMode == 'auto') {
				it.auto()
			} else if (thermostatMode == 'cool') {
				it.cool()
			} else if (thermostatMode == 'heat') {
				it.heat()
			} else if (thermostatMode == 'off') {
				it.off()
			}

			log.debug "$thermostat mode change to ${thermostatMode}"

			// Adding a pause of 5 seconds before retrieving the latest status of the thermostat
			pause(5000);

			def thermostatModelatest = it.latestValue("thermostatMode")
			def thermostatModeCurrent = it.currentValue("thermostatMode")
			if(thermostatModelatest == thermostatMode){
				resp <<  [status: true,thermostatModelatest:thermostatModelatest,thermostatModeCurrent:thermostatModeCurrent];
			}else{
				resp <<  [status: false,thermostatModelatest:thermostatModelatest,thermostatModeCurrent:thermostatModeCurrent];
			}

			log.debug("response: ${resp}");

			return resp;
		}
	}
	
	return resp;
}

def getThermostatMode(){
	def resp = [status: false];
	
	String deviceId = params.deviceId.toString();
	
	if(deviceId == null){
		return resp;
	}
	
	thermostatDevice.each{
		if(it.getId() == deviceId){
			def thermostatModelatest = it.latestValue("thermostatMode")
			def thermostatModeCurrent = it.currentValue("thermostatMode")
			resp <<  [status: true,thermostatModelatest:thermostatModelatest,thermostatModeCurrent:thermostatModeCurrent];
			return resp;
		}
	}
	
	return resp;
}

def setThermostatToVacantMode(){
	def resp = [status: false];
	
	float coolingTemperatureInF = Float.parseFloat(params.coolingTemperatureInF)
	float heatingTemperatureInF = Float.parseFloat(params.heatingTemperatureInF)
	def scale = location.temperatureScale;
	float coolingTemperature=null;
	float heatingTemperature=null;
	
	String deviceId = params.deviceId.toString();
	
	if(deviceId == null){
		return resp;
	}
	
	if(scale == "C"){
		coolingTemperature = ((float)coolingTemperatureInF - 32.0f) * (5.0f/9.0f)
		heatingTemperature = ((float)heatingTemperatureInF - 32.0f) * (5.0f/9.0f)
	}else if(scale=="F"){
		coolingTemperature = coolingTemperatureInF;
		heatingTemperature = heatingTemperatureInF;
	}
	
	thermostatDevice.each{
		if(it.getId() == deviceId){
			it.auto();
			
			log.debug "Cooling point is $coolingTemperature"
			it.setCoolingSetpoint(coolingTemperature);
			log.debug "Heating point is $heatingTemperature"
			it.setHeatingSetpoint(heatingTemperature);
			it.fanAuto();
			resp << [status:true];
			return resp;
		}
	}

}


def getWaterLeakSensors(){
	def resp = [];
	def hub;
	def waterLeakSensorsList = [];
	
	waterLeakageSensors.each{
		def deviceId = String.valueOf(it.id);
		def deviceStatus = it.getStatus();
		def deviceModelName = it.getModelName()
		def deviceManufacturer = it.getManufacturerName();
		def deviceLastActivity = it.getLastActivity();
		def deivceLabel = it.getLabel();
		log.debug "Display name is $it.displayName"
		log.debug "Display name is $deviceId "
		
		def water = it.currentWater
		def temperature = it.currentTemperature
		def battery = it.currentBattery
		def checkInterval = it.currentCheckInterval
		
		log.debug "Device : currentWater $water"
		log.debug "Device : currentTemperature $temperature"
		log.debug "Device : currentBattery $battery"
		log.debug "Device : currentCheckInterval $checkInterval"
			
		waterLeakSensorsList << [name: it.displayName, deviceId: deviceId, deviceStatus:deviceStatus,deviceLastActivity:deviceLastActivity,deviceManufacturer:deviceManufacturer,deviceModelName:deviceModelName,deivceLabel:deivceLabel, water:water, temperature:temperature, battery:battery, checkInterval:checkInterval];
	}
	
	resp << [status:true,waterLeakSensorsList:waterLeakSensorsList];
	return resp;
}

def getWaterLeakSensorStatus(){
	def resp = [];
	def hub;
	def waterLeakSensorStatus = [];
	String deviceIdParam = params.deviceId.toString();
	
	waterLeakageSensors.each{
		def deviceId = String.valueOf(it.id);
		log.debug "Display id is $deviceId "
		log.debug "Display id from patams is $deviceIdParam "
		if(deviceIdParam == deviceId){
			def water = it.currentWater
			def level = it.currentLevel
			def battery = it.currentBattery
			def checkInterval = it.currentCheckInterval
			def deviceLastActivity = it.getLastActivity();
			def deviceStatus = it.getStatus();
			
			log.debug "Display status is $deviceStatus"
			log.debug "Display id is $deviceId "

			log.debug "Device : currentWater $water"
			log.debug "Device : currentLevel $level"
			log.debug "Device : currentBattery $battery"
			log.debug "Device : currentCheckInterval $checkInterval"
			waterLeakSensorStatus << [deviceId: deviceId, deviceStatus:deviceStatus, deviceLastActivity:deviceLastActivity, water:water, battery:battery, checkInterval:checkInterval];
		}
	}
	
	resp << [status:true,waterLeakSensorStatus:waterLeakSensorStatus];
	return resp;
}

def getMSRFromLock() {
	String lockId = params.lockId.toString();
	
	def resp = [status: false];
	
	log.debug "Getting MSR Value for lock: $lockId";
	locks.each{
		if(it.getId() == lockId) {
			log.debug "Matching lock is $lockId"
			
			it.getMSRFromLock();
			resp << [status: true];
		}
	}
	return resp;
}

def getVersionsFromLock() {
	String lockId = params.lockId.toString();
	
	def resp = [status: false];
	
	log.debug "Getting Versions Value for lock: $lockId";
	locks.each{
		if(it.getId() == lockId) {
			log.debug "Matching lock is $lockId"
			
			it.getVersionFromLock();
			resp << [status: true];
		}
	}
	return resp;
}

def getMaxSlotsFromLock() {
	String lockId = params.lockId.toString();
	
	def resp = [status: false];
	
	log.debug "Getting Max slots supported Value for lock: $lockId";
	locks.each{
		if(it.getId() == lockId) {
			log.debug "Matching lock is $lockId"
			
			it.getMaxSlotsFromLock();
			resp << [status: true];
		}
	}
	return resp;
}

def getBatteryFromLock() {
	String lockId = params.lockId.toString();
	
	def resp = [status: false];
	
	log.debug "Getting battery Value for lock: $lockId";
	locks.each{
		if(it.getId() == lockId) {
			log.debug "Matching lock is $lockId"
			
			it.getBatteryLevelFromLock();
			resp << [status: true];
		}
	}
	return resp;
}

import static java.util.Calendar.*
def setScheduleEntryLockCmd(){
	log.debug "setScheduleEntryLockCmd executed "
	
	def resp = [status: false];
	
	String slotNumber = params.slotNumber.toString();
	String lockId = params.lockId.toString();
	short slotNumberInt = slotNumber.toShort();
	
	String startDateString = request.JSON?.startDate
	String endDateString = request.JSON?.endDate
	
	log.debug "setScheduleEntryLockCmd From json: $request.JSON";
	log.debug "setScheduleEntryLockCmd From json start date : $startDateString end date is : $endDateString"
	log.debug "Parameters are accessCode: $accessCode Slot Number: $slotNumberInt LockId : $lockId";
	
	locks.each{
		if(it.getId()==lockId){
			log.debug "Matching lock is $lockId"
			
			def startDate = new Date(Long.valueOf(startDateString) * 1000L)
			def endDate = new Date(Long.valueOf(endDateString) * 1000L)
   
			log.debug "StartDateDay: $startDate[DAY_OF_MONTH]"
			log.debug "StartDateHour: $startDate[HOUR_OF_DAY]"
			log.debug "StartDateMinute: $startDate[MINUTE]"
			log.debug "StartDateMonth: $startDate[MONTH]+1"
			log.debug "StartDateYear: $startDate[YEAR]%100"
			log.debug "EndDateDay: $endDate[DAY_OF_MONTH]"
			log.debug "EndDateHour: $endDate[HOUR_OF_DAY]"
			log.debug "EndDateMinute: $endDate[MINUTE]"
			log.debug "EndDateMonth: $endDate[MONTH]+1"
			log.debug "EndDateYear: $endDate[YEAR]%100"

			it.setYearDayScheduleAPI(
			slotNumberInt,//Identifier
			1,//slot
			startDate[DAY_OF_MONTH],//Start Day
			startDate[HOUR_OF_DAY],//Start Hour
			startDate[MINUTE],//Start Minute
			startDate[MONTH]+1,//Start Month
			startDate[YEAR]%100,//Start Year
			endDate[DAY_OF_MONTH],//Stop Day
			endDate[HOUR_OF_DAY],//Stop Hour
			endDate[MINUTE],//Stop Minute
			endDate[MONTH]+1,//Stop Month
			endDate[YEAR]%100//Stop Year
			);
			
			resp << [status: true];
		}//if lock match end
	
	}//locks iteration end
	return resp;
}


def getEvents(){
	def resp = [];
	
	locks.each{
		if(it.getId()==params.lockid){
			log.debug "Getting events for the lock $params.lockid"

			Date eventsStartDate = new Date ((Long.valueOf(params.startDate) * 1000L));
			Date eventsEndDate = new Date ((Long.valueOf(params.endDate) * 1000L));
			log.debug "Getting events between $eventsStartDate and $eventsEndDate"
			log.debug "Getting events for the lock ${getEventsOfDevice(it, eventsStartDate, eventsEndDate)}";
			resp << [events: getEventsOfDevice(it, eventsStartDate, eventsEndDate)];
		}
	}

	thermostatDevice.each{
		if(it.getId()==params.lockid){
			log.debug "Getting events for the lock $params.lockid"

			Date eventsStartDate = new Date ((Long.valueOf(params.startDate) * 1000L));
			Date eventsEndDate = new Date ((Long.valueOf(params.endDate) * 1000L));
			log.debug "Getting events between $eventsStartDate and $eventsEndDate"
			log.debug "Getting events for the lock ${getEventsOfDevice(it, eventsStartDate, eventsEndDate)}";
			resp << [events: getEventsOfDevice(it, eventsStartDate, eventsEndDate)];
		}
	}
	
	return resp;
}


def getEventsOfDevice(device, eventsStartDate, eventsEndDate) {
	log.debug "Returning events for lock $device.id between $eventsStartDate and $eventsEndDate"
	
	return device.eventsBetween(eventsStartDate, eventsEndDate, [max: 1000])?.collect{[description: it.description, descriptionText: it.descriptionText, displayName: it.displayName, date: it.date, name: it.name, unit: it.unit, source: it.source, value: it.value,id: String.valueOf(it.id), data: it.data]}
}

def refresh(){
	def statusExecution = false;
	log.debug "refresh invoked for is $params.lockid"
	
	locks.each{
		if(it.getId()==params.lockid){
			log.debug "invoking refresh for the lock $params.lockid"
			it.refresh();
			statusExecution = true;    
		}
	}
	return [status: statusExecution];
}

def setCodeScheduler(lockid,slotNumberString,codeString){
	String code = codeString.toString();
	int slotNumber = Integer.valueOf(slotNumberString.toString());
	
	log.debug "setCodeScheduler invoked with $lockid & $slotNumber & $code"
	
	locks.each{
		if(it.getId()==lockid){
			log.debug "setting code $code in slot $slotNumber for the lock $lockid"
			it.setCode(slotNumber,code);//slotNumber,code  
		}
	}
}

def deleteCodeHandler(lockid,slotNumberString){
	int slotNumber = Integer.valueOf(slotNumberString.toString());
	log.debug "deleteCodeHandler invoked with $lockid & $slotNumber & $code"
	locks.each{
		if(it.getId()==lockid){
			log.debug "deleting code $slotNumber for lock $lockid"
			it.deleteCode(slotNumber);  
			
		}  
	}
}

def getHubListForLocation(){
	return location.getHubs();
}



def hubList(){
	def hub;
	def hubList=[];
	
	for (int x=0; x<location.hubs.size(); x++){
	
		hub = location.hubs[x];
		log.debug "id: ${hub.id}"
		log.debug "zigbeeId: ${hub.zigbeeId}"
		log.debug "zigbeeEui: ${hub.zigbeeEui}"
	
		// PHYSICAL or VIRTUAL
		log.debug "type: ${hub.type}"
	
		log.debug "name: ${hub.name}"
		
		log.debug "firmwareVersionString: ${hub.firmwareVersionString}"
		log.debug "localIP: ${hub.localIP}"
		log.debug "localSrvPortTCP: ${hub.localSrvPortTCP}"
		hubList << [name: hub.name, id: hub.id,locationId: location.id,locationName:location.name,locationTemperatureScale:location.temperatureScale ]
	}
	
	def resp = [status:true,hubList:hubList]
	return resp
}


def setTimeParameters(){
	def resp = [status: false];
	log.debug "setTimeParameters invoked"
	String deviceId = params.deviceId.toString();
	if(deviceId == null){
		return resp;
	}
	locks.each{
		if(it.getId()==deviceId){
			it.setTimeParameters();
			resp = [status: true];
		}
	}
	return resp;
}

def setConfigParameters(){
	def resp = [status: false];
	log.debug "setConfigParameters invoked"
	String deviceId = params.deviceId.toString();
	String parameterNumberString = params.parameterNumber.toString();
	String parameterValueString = params.parameterValue.toString();
	int parameterNumber = Integer.valueOf(parameterNumberString);
	int parameterValue = Integer.valueOf(parameterValueString);
	log.debug "Parameter Number is $parameterNumber"
	log.debug "Parameter Value is $parameterValue"
	if(deviceId == null){
		return resp;
	}
	if(parameterNumber == null){
		return resp;
	}
	if(parameterValue == null){
		return resp;
	}
	locks.each{
		if(it.getId()==deviceId){
			it.setConfigParameters(parameterNumber, parameterValue);
			resp = [status: true];
		}
	}
	return resp;
}

def setScheduleTimeOffset(){
	def resp = [status: false];
	log.debug "setScheduleTimeOffset invoked"
	String deviceId = params.deviceId.toString();
	if(deviceId == null){
		return resp;
	}
	locks.each{
		if(it.getId()==deviceId){
			it.setScheduleTimeOffset();
			resp = [status: true];
		}
	}
	return resp;
}

def deleteAllCodes(){
	def resp = [status: false];
	log.debug "deleteAllCodes invoked"
	String deviceId = params.deviceId.toString();
	if(deviceId == null){
		return resp;
	}
	locks.each{
		if(it.getId()==deviceId){
			it.deleteAllCodes();
			resp = [status: true];
		}
	}
	return resp;
}

def locks(){
	def resp = [];
	locks.each{
		resp << [name: it.displayName, hubname: it.hub.name, lockid: it.getId(),hubid: it.hub.id ];
	}
	return resp;
}

def requestCode(){
	int slotNumber = Integer.valueOf(params.slotnumber.toString());
	
	log.debug "lock id received is $params.lockid"
	locks.each{
		if(it.getId()==params.lockid){
		log.debug "locking the lock $params.lockid"
		
		it.requestCode(slotNumber);    
		}
	}
}



def setCode() {
	def statusExecution = false;
	log.debug "Set Code called with $params.slotnumber and code is $params.pin"
	
	String code = params.pin.toString();
	int slotNumber = Integer.valueOf(params.confirmation.toString());

	log.debug "Slotnumber called with $slotNumber"

	if (slotNumber == 0){
		log.debug "Slotnumber is zero. Returning false as Set code should not be called for slot zero";
	} else {
		if(code instanceof String) {
			log.debug "code is instance of string";
			locks.each{
				if(it.getId()==params.lockid) {
					log.debug "setting code for the lock $params.lockid"
					pause(2000);
					it.setCode(slotNumber,code);//slotNumber,code  
					statusExecution = true;    
				}
			}
		} else {
			log.debug "code is not instance of string";
		}
	}

	return [status: statusExecution];
}

def deleteCode() {
	def statusExecution = false;
	log.debug "delete Code called with $params.slotNumber and confirmation is $params.confirmation"

	int slotNumber = Integer.valueOf(params.slotNumber.toString());//4;//Integer.valueOf(params.slotnumber.toString());

	log.debug "Slotnumber called with $slotNumber"

	if (slotNumber == 0){
		log.debug "Slotnumber is zero. Returning false as Set code should not be called for slot zero";
	} else {
		log.debug "Slotnumber is non zero. Calling delete for the lock $params.lockid on slot number $slotNumber";
		locks.each{
			if(it.getId()==params.lockid) {
				log.debug "deleting code for lock $params.lockid"
				pause(2000);
				it.deleteCode(slotNumber);  
				statusExecution = true;   
			}  
		}
	}
	
	return [status: statusExecution];  
}

def getLock(){
	log.debug "lock id received is $params.lockid"
	def statusExecution = false;
	def isConnected = "";
	def batteryStatus = "";
	def lockStatus = "";
	def lastActivityAt = "";

	locks.each{
		if(it.getId()==params.lockid){
			log.debug "Getting status for the lock $params.lockid"
			batteryStatus=it.currentValue("battery");
			lockStatus=it.latestValue("lock");//currentValue("lock");
			lastActivityAt=it.getLastActivity();
			isConnected=it.getStatus(); 

			// Adding a check to get the hub status and return lock status as HUB_DISCONNECTED if the hub is not active
			log.debug "Hub Id " + it.hub.id
			log.debug "Hub status " + it.hub.status

			if(it.hub.status != 'ACTIVE')
				isConnected = 'HUB_DISCONNECTED';

			log.debug "Lock connectivity staus is $isConnected"
			log.debug "Lock staus is $lockStatus"
		
			statusExecution = true; 
		}    
	}
	
	return [status: statusExecution,isConnected:isConnected,batteryStatus:batteryStatus,lockStatus:lockStatus,lastActivityAt:lastActivityAt ];
}


def lock(){
	log.debug "lock id received is $params.lockid"
	def statusExecution = false;
	locks.each{
		if(it.getId()==params.lockid){
			log.debug "locking the lock $params.lockid"
			it.lockViaApi()
			statusExecution = true; 
		}    
	}
	return [status: statusExecution];
}


def unlock(){
	def statusExecution = false;
	log.debug "lock id received is $params.lockid"
	locks.each{
		if(it.getId()==params.lockid){
			log.debug "unlocking the lock $params.lockid"
			it.unlockViaApi()
			statusExecution = true; 
		}
	}
	return [status: statusExecution];
}


def clearYearDaySchedule(){
	def resp = [status: false];
	log.debug("Start of clearYearDaySchedule")
	int slotNumber = Integer.valueOf(params.slotNumber.toString());
	int scheduleSlot = 1; //Integer.valueOf(params.scheduleSlot.toString()); Setting it to only slot 1
		
	log.debug "lock id received is $params.lockId"
	locks.each{
		if(it.getId()==params.lockId){
			log.debug "Clearing Year End Scheduling on lock :$params.lockId and slot number: $slotNumber and schedule Slot: $scheduleSlot"
		
			it.clearYearDaySchedule(slotNumber, scheduleSlot);
			resp << [status: true];
		}
	}
	log.debug("End of clearYearDaySchedule")
	return resp;
}

def clearAllYearDaySchedule(){
	def resp = [status: false];
	log.debug("Start of clearAllYearDaySchedule")
	int scheduleSlot = 1; //Integer.valueOf(params.scheduleSlot.toString());  Setting it to only slot 1
	
	log.debug "lock id received is $params.lockId"
	locks.each{
		if(it.getId()==params.lockId){
			log.debug "Clearing Year End Scheduling on lock :$params.lockId and schedule slot number: $scheduleSlot"
		
			it.clearAllYearDaySchedule(scheduleSlot);
			resp << [status: true];
		}
	}
	log.debug("End of clearAllYearDaySchedule")
	return resp;
}



def getYearDaySchedule(){
	def resp = [status: false];
	log.debug("Start of getYearDaySchedule")
	int slotNumber = Integer.valueOf(params.slotNumber.toString());
	int scheduleSlot = 1; //Integer.valueOf(params.scheduleSlot.toString()); Setting it to only slot 1
		
	log.debug "lock id received is $params.lockId"
	locks.each{
		if(it.getId()==params.lockId){
			log.debug "Getting Year End Scheduling on lock :$params.lockId and slot number: $slotNumber and schedule Slot: $scheduleSlot"
		
			it.getYearDaySchedule(slotNumber, scheduleSlot);
			resp << [status: true];
		}
	}
	log.debug("End of getYearDaySchedule")
	return resp;
}

def doorSensorHandler(evt){
 log.debug "doorSensorHandler is invoke";
	def evntDevice = evt.getDevice();

	log.debug "doorSensorHandler Device id is : ${evntDevice.getId()}";
	log.debug "doorSensorHandler Hub name is : ${evntDevice.hub.name}"
	
	log.debug "doorSensorHandler Hub id is : ${evntDevice.hub.id}"
	log.debug "doorSensorHandler The device id for this event: ${evt.deviceId}"
	
	log.debug "doorSensorHandler event display name: ${evt.displayName}"
	
	log.debug "doorSensorHandler This event name is ${evt.name}"
	log.debug "doorSensorHandler The value of this event is different from its previous value: ${evt.isStateChange()}"
	log.debug "doorSensorHandler Date: ${evt.date}";
	
	def deviceId = evntDevice.getId();
	log.debug "doorSensorHandler Event happened on sensor $deviceId"
	
	log.debug("doorSensorHandler event data ${evt.data}")

	def paramsEvent = [
		uri: 'https://api.getlynx.co/ProdV1.1/stwebhooks/handleDoorSensorEvent',
		query: [deviceId: evt.deviceId, date: evt.date, name: evt.name, value: evt.value, id: String.valueOf(evt.id), displayName: evt.displayName],
		contentType: 'application/json'
	]
	log.debug "webhook call ${paramsEvent}"
	try{
		asynchttp_v1.get('responseHandlerMethod', paramsEvent)
	  } catch (e) {
		log.debug "something went wrong: $e"
	}
	
}


def getDoorSensors(){
	
	def resp = [];
	def hub;
	def doorSensorList = [];
	
	doorsensors.each{
		def deviceId = String.valueOf(it.id);
		def deviceStatus = it.getStatus();
		def deviceModelName = it.getModelName()
		def deviceManufacturer = it.getManufacturerName();
		def deviceLastActivity = it.getLastActivity();
		def deivceLabel = it.getLabel();
		log.debug "Display name is $it.displayName"
		log.debug "Display name is $deviceId "
		
		def operatingStatus = it.currentContact
		def temperature = it.currentTemperature
		def batteryLevel = it.currentBattery
		def acceleration = it.currentAcceleration
		def locationId = location.id
		
		
		log.debug "Device : locationId: $locationId"
		log.debug "Device : doorsensorStatus $operatingStatus"
		log.debug "Device : currentTemperature $temperature"
		log.debug "Device : currentBatteryLevel $batteryLevel"
		log.debug "Device : doorsensorStatus $acceleration"
			
		doorSensorList << [name: it.displayName, deviceId: deviceId, deviceStatus:deviceStatus,deviceLastActivity:deviceLastActivity,deviceManufacturer:deviceManufacturer,deviceModelName:deviceModelName,deivceLabel:deivceLabel, operatingStatus:operatingStatus, temperature:temperature, batteryLevel:batteryLevel, acceleration:acceleration, locationId:locationId];
	}
	
	resp << [status:true,doorSensorList:doorSensorList];
	return resp;
}

def getDoorSensorStatus(){
	log.debug "Device id received is $params.deviceId"
	def resp = [status: false];
	def doorSensorStatus = {};

	doorsensors.each{
		if(it.getId()==params.deviceId){
			log.debug "Getting status for the Door Sensor $params.deviceId"

			def deviceId = String.valueOf(it.id);
			def deviceStatus = it.getStatus();
			def deviceModelName = it.getModelName()
			def deviceManufacturer = it.getManufacturerName();
			def deviceLastActivity = it.getLastActivity();
			def deviceLabel = it.getLabel();
			log.debug "Display name is $it.displayName"
			log.debug "Display name is $deviceId "
		
			def operatingStatus = it.currentContact
			def temperature = it.currentTemperature
			def batteryLevel = it.currentBattery
			def acceleration = it.currentAcceleration
		
			log.debug "Device : doorsensorStatus $operatingStatus"
			log.debug "Device : currentTemperature $temperature"
			log.debug "Device : currentBatteryLevel $batteryLevel"
			log.debug "Device : acceleration $acceleration"

			doorSensorStatus =  [displayName: it.displayName, deviceId: deviceId, deviceStatus:deviceStatus,deviceLastActivity:deviceLastActivity,deviceManufacturer:deviceManufacturer,deviceModelName:deviceModelName, operatingStatus:operatingStatus, temperature:temperature, batteryLevel:batteryLevel, acceleration:acceleration];
			log.debug "doorSensorStatus : $doorSensorStatus"
			
			return resp << [status: true, doorSensorStatus: doorSensorStatus];
		}    
	}

	return resp;
}


/**
*	Functions for Garage Door opener capabilities 
*/

def doorControlHandler(evt){
	log.debug "doorControlHandler is invoke";

	log.debug "Webhook event data $evt";

	log.debug "The device id for this event: ${evt.deviceId}"
	
	log.debug "data: ${evt.data}"
	
	log.debug "date ${evt.date}"

	log.debug "DateValue ${evt.dateValue}"
	

	log.debug "Description: ${evt.description}"
	
	log.debug "descriptionText ${evt.descriptionText}"

	log.debug "Device ${evt.device}"
	

	log.debug "event display name: ${evt.displayName}"
	
	log.debug "deviceId ${evt.deviceId}"

	log.debug "id ${evt.id}"

	try {
		log.debug "The jsonValue of this event is ${evt.jsonValue}"
	} catch (e) {
		log.debug("Trying to get the jsonValue for ${evt.name} threw an exception", e)
	}

	log.debug "locationId ${evt.locationId}"

	log.debug "name ${evt.name}"

	log.debug "source ${evt.source}"

	log.debug "stringValue ${evt.stringValue}"

	log.debug "value ${evt.value}"


/*
	def paramsEvent = [
		uri: 'https://api.getlynx.co/ProdV1.1/stwebhooks/handleDoorSensorEvent',
		query: [deviceId: evt.deviceId, date: evt.date, name: evt.name, value: evt.value, id: String.valueOf(evt.id), displayName: evt.displayName],
		contentType: 'application/json'
	]
	log.debug "webhook call ${paramsEvent}"
	try{
		asynchttp_v1.get('responseHandlerMethod', paramsEvent)
	  } catch (e) {
		log.debug "something went wrong: $e"
	}
*/	
}


def getDoorControlDevices(){
	
	def resp = [];
	def hub;
	def doorControlDeviceList = [];
	
	doorControl.each{
		def deviceSerialNumber = String.valueOf(it.id);

		log.debug "Door Control Device details $it";

		def deviceStatus = it.getStatus();
		def deviceLastActivity = it.getLastActivity();
		def deviceLabel = it.getLabel();

		log.debug "Device status $deviceStatus"
		log.debug "deviceLastActivity is $deviceLastActivity "
		log.debug "deviceLabel is $deviceLabel "
		
		def contactSensorStatus = it.currentContact
		def doorSensorStatus = it.currentDoor
		def locationId = location.id
		def locationName = location.name
		def hubId = it.hub.id
		def hubName = it.hub.name
		def hubStatus = it.hub.status
		
		log.debug "operatingStatus is $operatingStatus "
		log.debug "locationId is $locationId "

		
		doorControlDeviceList << [name: it.displayName, deviceLabel:deviceLabel, deviceSerialNumber: deviceSerialNumber,  locationId:locationId, locationName:locationName,  hubSerialNumber: hubId, hubName:hubName, hubStatus: hubStatus, deviceStatus:deviceStatus, deviceLastActivity:deviceLastActivity, contactSensorStatus: contactSensorStatus, doorSensorStatus: doorSensorStatus];

	}
	
	resp << [status:true,doorControlDeviceList:doorControlDeviceList];
	return resp;
}

def getDoorControlDeviceStatus(){
	log.debug "Door Control Device id received is $params.deviceId"
	def resp = [status: false];
	def doorControlDeviceStatus = {};

	doorControl.each{
		if(it.getId()==params.deviceId){
			def deviceSerialNumber = String.valueOf(it.id);

			log.debug "Door Control Device details $it";

			def deviceStatus = it.getStatus();
			def deviceLastActivity = it.getLastActivity();
			def deviceLabel = it.getLabel();

			log.debug "Device status $deviceStatus"
			log.debug "deviceLastActivity is $deviceLastActivity "
			log.debug "deviceLabel is $deviceLabel "

			def contactSensorStatus = it.currentContact
			def doorSensorStatus = it.currentDoor
			def locationId = location.id
			def locationName = location.name
			def hubId = it.hub.id
			def hubName = it.hub.name
			def hubStatus = it.hub.status

			log.debug "operatingStatus is $operatingStatus "
			log.debug "locationId is $locationId "

			doorControlDeviceStatus =  [name: it.displayName, deviceLabel:deviceLabel, deviceSerialNumber: deviceSerialNumber, locationId:locationId, locationName:locationName, hubSerialNumber: hubId, hubStatus: hubStatus, hubName: hubName, deviceStatus:deviceStatus,  deviceLastActivity:deviceLastActivity, contactSensorStatus: contactSensorStatus, doorSensorStatus: doorSensorStatus];
			log.debug "doorSensorStatus : $doorControlDeviceStatus"
			
			return resp << [status: true, doorControlDeviceStatus: doorControlDeviceStatus];
		}
	}

	return resp;
}

def openDoorControl(){
	log.debug "Door Control Device id received for open operation is $params.deviceId"
	def statusExecution = false;
	doorControl.each{
		if(it.getId()==params.deviceId){
			log.debug "Opening the Device $params.deviceId";
			it.open();
			statusExecution = true; 
		}    
	}
	return [status: statusExecution];
}


def closeDoorControl(){
	log.debug "Door Control Device id received for close operation is $params.deviceId"
	def statusExecution = false;
	doorControl.each{
		if(it.getId()==params.deviceId){
			log.debug "Closing the Device $params.deviceId";
			it.close();
			statusExecution = true; 
		}
	}
	return [status: statusExecution];
}


/**
* This function refreshes the status of the device and also updates the lastACtivityAt time for the device
*/

def refreshDoorControlDevice() {
	log.debug "Door Control Device id received for refresh is $params.deviceId"
	def statusExecution = [];

	doorControl.each{
		if(it.getId()==params.deviceId){
			log.debug "Refreshing device status for $params.deviceId"
			it.refresh();
			statusExecution = true; 
		}
	}
	return [status: statusExecution];
}