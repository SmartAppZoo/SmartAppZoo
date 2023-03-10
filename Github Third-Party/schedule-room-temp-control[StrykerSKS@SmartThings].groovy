/**
 *  ScheduleRoomTempControl
 *
 *  Copyright 2015 Yves Racine
 *  linkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/
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
	name: "Schedule Room Temp Control",
	namespace: "eco-community",
	author: "Yves Racine",
	description: "Enable better temp control in rooms based on Smart Vents",
	category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)



preferences {

	page(name: "generalSetupPage")
	page(name: "roomsSetupPage")
	page(name: "zonesSetupPage")
	page(name: "schedulesSetupPage")
	page(name: "NotificationsPage")
	page(name: "roomsSetup")
	page(name: "zonesSetup")
	page(name: "schedulesSetup")
}

def generalSetupPage() {

	dynamicPage(name: "generalSetupPage", uninstall: true, nextPage: roomsSetupPage) {
		section("About") {
			paragraph "ScheduleRoomTempControl, the smartapp that enables better temp control in rooms based on Smart Vents"
			paragraph "Version 1.3.1" 
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=yracine%40yahoo%2ecom&lc=US&item_name=Maisons%20ecomatiq&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest",
					title:"Paypal donation..."
			paragraph "Copyright©2015 Yves Racine"
				href url:"http://github.com/yracine/device-type.myecobee", style:"embedded", required:false, title:"More information..."  
 					description: "http://github.com/yracine/device-type.myecobee/blob/master/README.md"
		}
		section("Main thermostat at home (used for vent adjustment)") {
			input (name:"thermostat", type: "capability.thermostat", title: "Which main thermostat?")
		}
		section("Rooms count") {
			input (name:"roomsCount", title: "Rooms count (max=16)?", type: "number",refreshAfterSelection: true)
		}
		section("Zones count") {
			input (name:"zonesCount", title: "Zones count (max=8)?", type:"number",refreshAfterSelection: true)
		}
		section("Schedules count") {
			input (name:"schedulesCount", title: "Schedules count (max=12)?", type: "number",refreshAfterSelection: true)
		}
		section("Do not set the thermostat setpoints in schedules [optional, default=The thermostat setpoints are set]") {
			input (name:"noSetpointsFlag", title: "Do not set the thermostat setpoints?", type:"bool",required:false)
		}
        
		section("What do I use for the Master on/off switch to enable/disable smartapp processing? [optional]") {
			input (name:"powerSwitch", type:"capability.switch", required: false,description: "Optional")
		}
		if (thermostat) {
			section {
				href(name: "toRoomPage", title: "Room Setup", page: "roomsSetupPage")
				href(name: "toZonePage", title: "Zone Setup", page: "zonesSetupPage")
				href(name: "toSchedulePage", title: "Schedule Setup", page: "schedulesSetupPage")
				href(name: "toNotificationsPage", title: "Notifications Setup", page: "NotificationsPage")
			}                
		}
	}
}






def roomsSetupPage() {

	dynamicPage(name: "roomsSetup", title: "Rooms Setup", uninstall: true, nextPage: zonesSetupPage) {

		for (int indiceRoom = 1;
			((indiceRoom <= settings.roomsCount) && (indiceRoom <= 16)); indiceRoom++) {
            
			section("Room ${indiceRoom} Setup") {
				input "roomName${indiceRoom}", title: "Room Name", "string"
			}
			section("Room ${indiceRoom}-TempSensor [optional]") {
				input "tempSensor${indiceRoom}", title: "Temp sensor for better temp adjustment", "capability.temperatureMeasurement", 
					required: false, description: "Optional"

			}
			section("Room ${indiceRoom}-Vents Setup [optional]")  {
				for (int j = 1;(j <= 5); j++)  {
					input "ventSwitch${j}${indiceRoom}", title: "Vent switch no ${j} in room", "capability.switch", 
						required: false, description: "Optional"
				}           
			}           
			section("Room ${indiceRoom}-MotionSensor [optional]") {
				input "motionSensor${indiceRoom}", title: "Motion sensor (if any) to detect if room is occupied", "capability.motionSensor", 
                			required: false, description: "Optional"

			}
			section("Room ${indiceRoom}-Do vent adjustment when occupied room only [optional, vent will be partially closed otherwise]") {
				input "needOccupiedFlag${indiceRoom}", title: "Will do vent adjustement only when Occupied [default=false]", "bool",  
                			required: false, description: "Optional"

			}
			section("Room ${indiceRoom}-Do vent adjustment with this occupied's threshold [optional]") {
				input "residentsQuietThreshold${indiceRoom}", title: "Threshold in minutes for motion detection [default=15 min]", "number", 
               				required: false, description: "Optional"

			}
			section() {
				paragraph "**** DONE FOR ROOM ${indiceRoom} **** "

			}                
		} /* end for */
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}

	}

}

def zoneHrefDescription(i) {
	def description ="Zone no ${i} "

	if (settings."zoneName${i}" !=null) {
		description += settings."zoneName${i}"		    	
	}
	return description
}

def zonePageState(i) {

	if (settings."zoneName${i}" != null) {
		return 'complete'
	} else {
		return 'incomplete'
	}
  
}

def zoneHrefTitle(i) {
	def title = "Zone ${i}"
	return title
}

def zonesSetupPage() {

	dynamicPage(name: "zonesSetupPage", title: "Zones Setup", nextPage: schedulesSetupPage) {
		section("Zones") {
			for (int i = 1; i <= settings.zonesCount; i++) {
				href(name: "toZonePage$i", page: "zonesSetup", params: [indiceZone: i], required:false, description: zoneHrefDescription(i), title: zoneHrefTitle(i), state: zonePageState(i) )
			}
		}            
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}        

def zonesSetup(params) {

	def rooms = []
	for (i in 1..settings.roomsCount) {
		def key = "roomName$i"
		def room = "${i}:${settings[key]}"
		rooms = rooms + room
	}
	log.debug "rooms: $rooms"
	def indiceZone=0    

	// Assign params to indiceZone.  Sometimes parameters are double nested.
	if (params?.indiceZone || params?.params?.indiceZone) {

		if (params.indiceZone) {
			indiceZone = params.indiceZone
		} else {
			indiceZone = params.params.indiceZone
		}
	}    
	indiceZone=indiceZone.intValue()
	log.debug "zonesSetup> indiceZone=${indiceZone}"
	dynamicPage(name: "zonesSetup", title: "Zones Setup") {
		section("Zone ${indiceZone} Setup") {
			input (name:"zoneName${indiceZone}", title: "Zone Name", type: "text",
				defaultValue:settings."zoneName${indiceZone}")
		}
		section("Zone ${indiceZone}-Included rooms") {
			input (name:"includedRooms${indiceZone}", title: "Rooms included in the zone", type: "enum",
				options: rooms,
				multiple: true,
				defaultValue:settings."includedRooms${indiceZone}")
		}
		section("Zone ${indiceZone}-Cool Temp threshold in the zone (below it, when cooling, the vents are -partially- closed)") {
			input (name:"desiredCoolTemp${indiceZone}", type:"decimal", title: "Cool Temp Threshold", 
				required: true,defaultValue:settings."desiredCoolTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Heat Temp threshold in the zone (above it, when heating, the vents are -partially- closed)") {
			input (name:"desiredHeatTemp${indiceZone}", type:"decimal", title: "Heat Temp", 
				required: true, defaultValue:settings."desiredHeatTemp${indiceZone}")			                
		}
		section {
			href(name: "toZonesSetupPage", title: "Back to Zones Setup Page", page: "zonesSetupPage")
		}
	}            
}

def scheduleHrefDescription(i) {
	def description ="Schedule no ${i} " 
	if (settings."scheduleName${i}" !=null) {
		description += settings."scheduleName${i}"		    
    }
	return description
}

def schedulePageState(i) {

	if (settings."scheduleName${i}"  != null) {
		return 'complete'
	} else {
		return 'incomplete'
	}	
    
}

def scheduleHrefTitle(i) {
	def title = "Schedule ${i}"
	return title
}

def schedulesSetupPage() {
	dynamicPage(name: "schedulesSetupPage", title: "Schedule Setup", nextPage: NotificationsPage) {
		section("Schedules") {
			for (int i = 1; i <= settings.schedulesCount; i++) {
				href(name: "toSchedulePage$i", page: "schedulesSetup", params: [indiceSchedule: i],required:false, description: scheduleHrefDescription(i), title: scheduleHrefTitle(i), state: schedulePageState(i) )
			}
		}            
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}        

def schedulesSetup(params) {
    
	def ecobeePrograms=[]
	// try to get the thermostat programs list (ecobee)
	try {
		ecobeePrograms = thermostat?.currentClimateList.toString().minus('[').minus(']').tokenize(',')
		ecobeePrograms.sort()        
	} catch (any) {
		log.debug("Not able to get the list of climates (ecobee)")    	
	}    
    
    
	log.debug "programs: $ecobeePrograms"

	def zones = []
    
	for (i in 1..settings.zonesCount) {
		def key = "zoneName$i"
		def zoneName =  "${i}:${settings[key]}"   
		zones = zones + zoneName
	}
	log.debug "zones: $zones"

	
	def enumModes=[]
	location.modes.each {
		enumModes << it.name
	}    
	def indiceSchedule=1
	// Assign params to indiceSchedule.  Sometimes parameters are double nested.
	if (params?.indiceSchedule || params?.params?.indiceSchedule) {

		if (params.indiceSchedule) {
			indiceSchedule = params.indiceSchedule
		} else {
			indiceSchedule = params.params.indiceSchedule
		}
	}    
	indiceSchedule=indiceSchedule.intValue()
	log.debug "scheduleSetup> indiceSchedule=${indiceSchedule}"

	dynamicPage(name: "schedulesSetup", title: "Schedule Setup") {
		section("Schedule ${indiceSchedule} Setup") {
			input (name:"scheduleName${indiceSchedule}", title: "Schedule Name", type: "text",
				defaultValue:settings."scheduleName${indiceSchedule}")
		}
		section("Schedule ${indiceSchedule}-Included zones") {
			input (name:"includedZones${indiceSchedule}", title: "Zones included in this schedule", type: "enum",
				defaultValue:settings."includedZones${indiceSchedule}",
				options: zones,
 				multiple: true)
		}
		section("Schedule ${indiceSchedule}- Day & Time of the desired Heating/Cooling settings for the selected zone(s)") {
			input (name:"dayOfWeek${indiceSchedule}", type: "enum",
				title: "Which day of the week to trigger the zoned heating/cooling settings?",
				defaultValue:settings."dayOfWeek${indiceSchedule}",                 
				multiple: false,
				metadata: [
					values: [
						'All Week',
						'Monday to Friday',
						'Saturday & Sunday',
						'Monday',
						'Tuesday',
						'Wednesday',
						'Thursday',
						'Friday',
						'Saturday',
						'Sunday'
					]
				])
			input (name:"begintime${indiceSchedule}", type: "time", title: "Beginning time to trigger the zoned heating/cooling settings",
				defaultValue:settings."begintime${indiceSchedule}")
			input (name:"endtime${indiceSchedule}", type: "time", title: "End time",
				defaultValue:settings."endtime${indiceSchedule}")
		}
		section("Schedule ${indiceSchedule}-Select the program/climate at ecobee thermostat to be applied [optional,for ecobee]") {
			input (name:"givenClimate${indiceSchedule}", type:"enum", title: "Which ecobee program? ", options: ecobeePrograms, 
				required: false, defaultValue:settings."givenClimate${indiceSchedule}", description: "Optional")
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Cooling setpoint during the schedule [optional, when no ecobee climate is specified]") {
			input (name:"desiredCool${indiceSchedule}", type:"decimal", title: "Cooling Setpoint, default = 75°F/23°C", 
				required: false,defaultValue:settings."desiredCool${indiceSchedule}", description: "Optional")			                
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Heating setpoint during the schedule [optional, when no ecobee climate is specified]") {
			input (name:"desiredHeat${indiceSchedule}", type:"decimal", title: "Heating Setpoint, default=72°F/21°C", 
				required: false, defaultValue:settings."desiredHeat${indiceSchedule}", description: "Optional")			                
		}
		section("Schedule ${indiceSchedule}-Adjust vent settings every 5 minutes [optional]") {
			input (name: "adjustVentsEveryCycleFlag${indiceSchedule}", type:"bool",  title: "Adjust vent settings every 5 minutes (default=only when heating/cooling/fan running)?", 
				required: false, defaultValue:settings."adjustVentsEveryCycleFlag${indiceSchedule}", description: "Optional")
		}
		section("Schedule ${indiceSchedule}-Set for specific mode(s) [default=all]")  {
			input (name:"selectedMode${indiceSchedule}", type:"enum", title: "Choose Mode", options: enumModes, 
				required: false, multiple:true,defaultValue:settings."selectedMode${indiceSchedule}", description: "Optional")
		}
		section {
			href(name: "toSchedulesSetupPage", title: "Back to Schedules Setup Page", page: "schedulesSetupPage")
		}
	}        
}

def NotificationsPage() {
	dynamicPage(name: "NotificationsPage", title: "Other Options", install: true) {
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
			input "phone", "phone", title: "Send a Text Message?", required: false
		}
		section("Detailed Notifications") {
			input "detailedNotif", "bool", title: "Detailed Notifications?", required:
				false
		}
		section([mobileOnly: true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}



def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def offHandler(evt) {
	log.debug "$evt.name: $evt.value"
}

def onHandler(evt) {
	log.debug "$evt.name: $evt.value"
	setZoneSettings()
}

def ventTemperatureHandler(evt) {
	log.debug "vent temperature: $evt.value"
	float ventTemp = evt.value.toFloat()
	def scale = getTemperatureScale()
	def MAX_TEMP_VENT_SWITCH = (scale=='C')?49:121 //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (scale=='C')?7:45 //Min temperature inside a ventSwitch
	String currentHVACMode = thermostat.currentThermostatMode.toString()
    
	if (((currentHVACMode=='heat') || (currentHVACMode == 'auto')) && (ventTemp >= MAX_TEMP_VENT_SWITCH)) {
		// Open all vents just to be safe
        open_all_vents()
		send("ScheduleRoomTempControl>current HVAC mode is ${currentHVACMode}, found one of the vents' value too hot (${evt.value}°), opening all vents to avoid any damage")
	} /* if too hot */           
	if (((currentHVACMode=='cool') || (currentHVACMode == 'auto')) && (ventTemp <= MIN_TEMP_VENT_SWITCH)) {
		// Open all vents just to be safe
		open_all_vents()
		send("ScheduleRoomTempControl>current HVAC mode is ${currentHVACMode}, found one of the vents' value too cold (${evt.value}°), opening all vents to avoid any damage")
	} /* if too cold */ 
}

def thermostatOperatingHandler(evt) {
	log.debug "Thermostat Operating now: $evt.value"
	state?.operatingState=evt.value    
	setZoneSettings()      
}

def heatingSetpointHandler(evt) {
	log.debug "heating Setpoint now: $evt.value"
}
def coolingSetpointHandler(evt) {
	log.debug "cooling Setpoint now: $evt.value"
}

def changeModeHandler(evt) {
	log.debug "Changed mode, $evt.name: $evt.value"
	setZoneSettings()  
}

def motionEvtHandler(evt) {
	if (evt.value == "active") {
		log.debug "Motion at home..."

		if (state?.setPresentOrAway == 'Away') {
			set_main_tstat_to_AwayOrPresent('present')
		}        
	}
}

def initialize() {

	if (powerSwitch) {
		subscribe(powerSwitch, "switch.off", offHandler, [filterEvents: false])
		subscribe(powerSwitch, "switch.on", onHandler, [filterEvents: false])
	}
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)    
	subscribe(thermostat, "coolingSetpoint", coolingSetpointHandler)
	subscribe(thermostat, "thermostatOperatingState", thermostatOperatingHandler)
    
	subscribe(location, changeModeHandler)

	// Initialize state variables
	state.lastScheduleLastName=""
	state.lastStartTime=null 
	state.scheduleHeatSetpoint=0  
	state.scheduleCoolSetpoint=0    
	state.setPresentOrAway=''
	state.programSetTime = ""
	state.programSetTimestamp = null
	state.operatingState=""
    
	subscribe(app, appTouch)

	// subscribe all vents to check their temperature on a regular basis
    
	for (indiceRoom in 1..roomsCount) {
		for (int j = 1;(j <= 5); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
			if (vent) {
				subscribe(vent, "temperature", ventTemperatureHandler)
			} /* end if vent != null */
		} /* end for vent switches */
	} /* end for rooms */

	// subscribe all motion sensors to check for active motion in rooms
    
	def motionSensors =[]   	 
	for (int i = 1;
		((i <= settings.roomsCount) && (i <= 16)); i++) {
		def key = "motionSensor${i}"
		def motionSensor = settings[key]
        
		if (motionSensor) {
			motionSensors.add(motionSensor)    
		}            
	}        
	// associate the motionHandler to the list of motionSensors in rooms   	 
	subscribe(motionSensors, "motion", motionEvtHandler, [filterEvents: false])
    
	state?.poll = [ last: 0, rescheduled: now() ]

	Integer delay =5 				// wake up every 5 minutes to apply zone settings if any
	log.debug "initialize>scheduling setZoneSettings every ${delay} minutes to check for zone settings to be applied"

	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "sunrise", rescheduleIfNeeded)
	subscribe(location, "sunset", rescheduleIfNeeded)
	subscribe(location, "mode", rescheduleIfNeeded)
	subscribe(location, "sunriseTime", rescheduleIfNeeded)
	subscribe(location, "sunsetTime", rescheduleIfNeeded)

	rescheduleIfNeeded()   
}

def rescheduleIfNeeded(evt) {
	if (evt) log.debug("rescheduleIfNeeded>$evt.name=$evt.value")
	Integer delay = 5 // By default, schedule SetZoneSettings() every 5 min.
	BigDecimal currentTime = now()    
	BigDecimal lastPollTime = (currentTime - (state?.poll["last"]?:0))  
	if (lastPollTime != currentTime) {    
		Double lastPollTimeInMinutes = (lastPollTime/60000).toDouble().round(1)      
		log.info "rescheduleIfNeeded>last poll was  ${lastPollTimeInMinutes.toString()} minutes ago"
	}
	if (((state?.poll["last"]?:0) + (delay * 60000) < currentTime) && canSchedule()) {
		log.info "recheduleIfNeeded>scheduling setZoneSettings in ${delay} minutes.."
		runEvery5Minutes(setZoneSettings)
	}
    
	setZoneSettings()
    
	// Update rescheduled state
    
	if (!evt) state.poll["rescheduled"] = now()
}

def appTouch(evt) {
	setZoneSettings()
}

def setZoneSettings() {

	log.debug "Begin of setZoneSettings Fcn"
	Integer delay = 5 // By default, schedule SetZoneSettings() every 5 min.

	//schedule the rescheduleIfNeeded() function
	state?.poll["last"] = now()
    
	if (((state?.poll["rescheduled"]?:0) + (delay * 60000)) < now()) {
		log.info "setZoneSettings>Scheduling rescheduleIfNeeded() in ${delay} minutes.."
		schedule("0 0/${delay} * * * ?", rescheduleIfNeeded)
		// Update rescheduled state
		state?.poll["rescheduled"] = now()
	}
	if (powerSwitch?.currentSwitch == "off") {
		if (detailedNotif) {
			send("ScheduleRoomTempControl>${powerSwitch.name} is off, schedule processing on hold...")
		}
		return
	}

	def currTime = now()
	boolean initialScheduleSetup=false        
	boolean foundSchedule=false

	/* Poll or refresh the thermostat to get latest values */
	if  (thermostat.hasCapability("Polling")) {
		try {        
			thermostat.poll()
		} catch (e) {
			log.debug("setZoneSettings>not able to do a poll() on ${thermostat}, exception ${e}")
		}                    
	}  else if  (thermostat.hasCapability("Refresh")) {
		try {        
			thermostat.refresh()
		} catch (e) {
			log.debug("setZoneSettings>not able to do a refresh() on ${thermostat}, exception ${e}")
		}                    
	}                    

	def ventSwitchesOn = []
    
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
	for (int i = 1;((i <= settings.schedulesCount) && (i <= 12)); i++) {
        
		def key = "selectedMode$i"
		def selectedModes = settings[key]
		key = "scheduleName$i"
		def scheduleName = settings[key]

		boolean foundMode=false        
		selectedModes.each {
        
			if (it==location.mode) {
				foundMode=true            
			}            
		}        
        
		if ((selectedModes != null) && (!foundMode)) {
        
			log.debug "setZoneSettings>schedule=${scheduleName} does not apply,location.mode= $location.mode, selectedModes=${selectedModes},foundMode=${foundMode}, continue"
			continue			
		}
		key = "begintime$i"
		def startTime = settings[key]
		if (startTime == null) {
        		continue
		}
		def startTimeToday = timeToday(startTime,location.timeZone)
		key = "endtime$i"
		def endTime = settings[key]
		def endTimeToday = timeToday(endTime,location.timeZone)
		if ((currTime < endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
			startTimeToday = startTimeToday -1        
			log.debug "setZoneSettings>schedule ${scheduleName}, subtracted - 1 day, new startTime=${startTimeToday.time}"
		}            
		if ((currTime > endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
			endTimeToday = endTimeToday +1        
			log.debug "setZoneSettings>schedule ${scheduleName} added + 1 day, new endTime=${endTimeToday.time}"
		}        
		String startInLocalTime = startTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
		String endInLocalTime = endTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)

		log.debug "setZoneSettings>found schedule ${scheduleName},original startTime=$startTime,original endTime=$endTime,nowInLocalTime= ${nowInLocalTime},startInLocalTime=${startInLocalTime},endInLocalTime=${endInLocalTime}," +
        		"currTime=${currTime},begintime=${startTimeToday.time},endTime=${endTimeToday.time},lastScheduleName=$state.lastScheduleName, lastStartTime=$state.lastStartTime"
        
		def ventSwitchesZoneSet = []        
		if ((currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (state.lastStartTime != startTimeToday.time) && (IsRightDayForChange(i))) {
        
			// let's set the given schedule
			initialScheduleSetup=true
			foundSchedule=true

			log.debug "setZoneSettings>schedule ${scheduleName},currTime= ${currTime}, current date & time OK for execution"
			if (detailedNotif) {
				send("ScheduleRoomTempControl>now running schedule ${scheduleName},about to set zone settings as requested")
			}
            
			if (!noSetpointsFlag) {
				log.debug "setZoneSettings>schedule ${scheduleName},about to set the thermostat setpoint"
				if (detailedNotif) {
					send("setZoneSettings>schedule ${scheduleName},about to set the thermostat setpoint")
				}
 				set_thermostat_setpoint_in_zone(i)
			}            
			// set the zoned vent switches to 'on' and adjust them according to the ambient temperature
               
			ventSwitchesZoneSet= adjust_vent_settings_in_zone(i)
			log.debug "setZoneSettings>schedule ${scheduleName},list of Vents turned 'on'= ${ventSwitchesZoneSet}"
 			ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet              
			state.lastScheduleName = scheduleName
			state?.lastStartTime = startTimeToday.time
		}
		else if ((state.lastScheduleName == scheduleName) && (currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange)) {
			// We're in the middle of a schedule run
        
			log.debug "setZoneSettings>schedule ${scheduleName},currTime= ${currTime}, current time is OK for execution, we're in the middle of a schedule run"
			foundSchedule=true
			// Check the operating State before adjusting the vents again.
			String operatingState = thermostat.currentThermostatOperatingState           
			// let's adjust the vent settings according to desired Temp only if thermostat is not idle or was not idle at the last run


			key = "adjustVentsEveryCycleFlag$i"
			def setVentSettings = (settings[key]) ?: false
			log.debug "setZoneSettings>adjustVentsEveryCycleFlag=$setVentSettings"

			if ((setVentSettings) || ((operatingState.toUpperCase() !='IDLE') ||
				((state?.operatingState.toUpperCase() =='HEATING') || (state?.operatingState.toUpperCase() =='COOLING'))))
			{            
				log.debug "setZoneSettings>thermostat ${thermostat}'s Operating State is ${operatingState} or was just recently " +
					"${state?.operatingState}, adjusting the vents for schedule ${scheduleName}"
				ventSwitchesZoneSet=adjust_vent_settings_in_zone(i)
				ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet     
                
			}   
			state?.operatingState =operatingState            
		} else {
			if (detailedNotif) {
				send("ScheduleRoomTempControl>schedule: ${scheduleName},change not scheduled at this time ${nowInLocalTime}...")
			}
		}

	} /* end for */
    
	if ((ventSwitchesOn !=[]) || (initialScheduleSetup)) {
		log.debug "setZoneSettings>list of Vents turned on= ${ventSwitchesOn}"
		turn_off_all_other_vents(ventSwitchesOn)
	}
	if (!foundSchedule) {
		if (detailedNotif) {
			send "ScheduleRoomTempControl>No schedule applicable at this time ${nowInLocalTime}"
		}
		log.debug "setZoneSettings>No schedule applicable at this time ${nowInLocalTime}"
	} 
        
	log.debug "End of setZoneSettings Fcn"
}



private def isRoomOccupied(sensor, indiceRoom) {
	def key = "residentsQuietThreshold$indiceRoom"
	def threshold = (settings[key]) ?: 15 // By default, the delay is 15 minutes 

	key = "roomName$indiceRoom"
	def roomName = settings[key]

	def result = false
	def t0 = new Date(now() - (threshold * 60 * 1000))
	def recentStates = sensor.statesSince("motion", t0)
	if (recentStates.find {it.value == "active"}) {
		log.debug "isRoomOccupied>room ${roomName} has been occupied, motion was detected at sensor ${sensor} in the last ${threshold} minutes"
		result = true
	}
	return result
}

private def verify_presence_based_on_motion_in_rooms() {

	def result=false
	for (i in 1..roomsCount) {

		def key = "roomName$i"
		def roomName = settings[key]
		key = "motionSensor$i"
		def motionSensor = settings[key]
		if (motionSensor != null) {

			if (isRoomOccupied(motionSensor,i)) {
				log.debug("verify_presence_based_on_motion>in ${roomName},presence detected, return true")
				return true
			}                
		}
	} /* end for */        
	return result
}

private def getSensorTempForAverage(indiceRoom, typeSensor='tempSensor') {
	def key 
	def currentTemp=null
	    
	if (typeSensor == 'tempSensor') {
		key = "tempSensor$indiceRoom"
	} else {
		key = "roomTstat$indiceRoom"
	}
	def tempSensor = settings[key]
	if (tempSensor != null) {
		log.debug("getTempSensorForAverage>found sensor ${tempSensor}")
		if (tempSensor.hasCapability("Refresh")) {
			// do a refresh to get the latest temp value
			try {        
				tempSensor.refresh()
			} catch (e) {
				log.debug("getSensorTempForAverage>not able to do a refresh() on $tempSensor")
			}                
		}        
		currentTemp = tempSensor.currentTemperature.toFloat().round(1)
	}
	return currentTemp
}



private def getAllTempsForAverage(indiceZone) {
	def tempAtSensor

	def indoorTemps = []
	def key = "includedRooms$indiceZone"
	def rooms = settings[key]
	for (room in rooms) {

		def roomDetails=room.split(':')
		def indiceRoom = roomDetails[0]
		def roomName = roomDetails[1]

		key = "needOccupiedFlag$indiceRoom"
		def needOccupied = (settings[key]) ?: false
		log.debug("getAllTempsForAverage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}")

		if (needOccupied) {

			key = "motionSensor$indiceRoom"
			def motionSensor = settings[key]
			if (motionSensor != null) {

				if (isRoomOccupied(motionSensor, indiceRoom)) {

					tempAtSensor = getSensorTempForAverage(indiceRoom)
					if (tempAtSensor != null) {
						indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
						log.debug("getAllTempsForAverage>added ${tempAtSensor.toString()} due to occupied room ${roomName} based on ${motionSensor}")
					}
					tempAtSensor = getSensorTempForAverage(indiceRoom,'roomTstat')
					if (tempAtSensor != null) {
						indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
						log.debug("getAllTempsForAverage>added ${tempAtSensor.toString()} due to occupied room ${roomName} based on ${motionSensor}")
					}
				}
			}
		} else {
			tempAtSensor = getSensorTempForAverage(indiceRoom)
			if (tempAtSensor != null) {
				log.debug("getAllTempsForAverage>added ${tempAtSensor.toString()} in room ${roomName}")
				indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
			}
			tempAtSensor = getSensorTempForAverage(indiceRoom,'roomTstat')
			if (tempAtSensor != null) {
				indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
 				log.debug("getAllTempsForAverage>added ${tempAtSensor.toString()} in room ${roomName}")
			}
		}
	} /* end for */
	return indoorTemps

}

private def set_thermostat_setpoint_in_zone(indiceSchedule) {
	def scale = getTemperatureScale()
	float desiredHeat, desiredCool

	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	key = "includedZones$indiceSchedule"
	def zones = settings[key]
	key = "givenClimate$indiceSchedule"
	def climateName = settings[key]

	float currentTemp = thermostat?.currentTemperature.toFloat().round(1)
	String mode = thermostat?.currentThermostatMode.toString()
	if (mode == 'heat') {
		if ((climateName) && (thermostat.hasCommand("setClimate"))) {
			try {
				thermostat.setClimate("", climateName)
				thermostat.poll() // to get the latest setpoints
			} catch (any) {
				if (detailedNotif) {
					send("ScheduleRoomTempControl>schedule ${scheduleName}:not able to set climate ${climateName} for heating at the thermostat ${thermostat}")
				}
				log.debug("adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}:not able to set climate  ${climateName} for heating at the thermostat ${thermostat}")
			}                
			desiredHeat = thermostat.currentHeatingSetpoint
			log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredHeat=${desiredHeat}")
		} else {
			log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for heatingSetpoint")
			key = "desiredHeat$indiceSchedule"
			def heatTemp = settings[key]
			if (!heatTemp) {
				log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName}:about to apply default heat settings")
				desiredHeat = (scale=='C') ? 21:72 					// by default, 21°C/72°F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredHeat=${desiredHeat}")
			thermostat?.setHeatingSetpoint(desiredHeat)
		} 
		if (detailedNotif) {
			send("ScheduleRoomTempControl>schedule ${scheduleName},in zones=${zones},heating setPoint now =${desiredHeat}°")
		}
		if (scheduleName != state.lastScheduleLastName) {
			state.scheduleHeatSetpoint=desiredHeat 
		}        
	} else if (mode == 'cool') {
		if ((climateName) && (thermostat.hasCommand("setClimate"))) {
			try {
				thermostat?.setClimate("", climateName)
				thermostat.poll() // to get the latest setpoints
			} catch (any) {
				if (detailedNotif) {
					send("ScheduleRoomTempControl>schedule ${scheduleName},not able to set climate ${climateName} for cooling at the thermostat(s) ${thermostat}")
				}
				log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName},not able to set climate ${climateName} associated for cooling at the thermostat ${thermostat}")
			}                
			desiredCool = thermostat.currentCoolingSetpoint
			log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredCool=${desiredCool}")
		} else {
			log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for coolingSetpoint")
			key = "desiredCool$indiceSchedule"
			def coolTemp = settings[key]
			if (!coolTemp) {
				log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName},about to apply default cool settings")
				desiredCool = (scale=='C') ? 23:75					// by default, 23°C/75°F is the target cool temp
			} else {
				desiredCool = coolTemp.toFloat()
			}
            
			log.debug("set_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredCool=${desiredCool}")
		} 
		if (detailedNotif) {
			send("ScheduleTstatZones>schedule ${scheduleName}, in zones=${zones},cooling setPoint now =${desiredCool}°")
		}            
		if (scheduleName != state.lastScheduleLastName) {
			state.scheduleCoolSetpoint=desiredCool 
		}        
		thermostat?.setCoolingSetpoint(desiredCool)
	} /* else if mode == 'cool' */

}


private def adjust_vent_settings_in_zone(indiceSchedule) {
	def MIN_OPEN_LEVEL_SMALL=25
	def MIN_OPEN_LEVEL_BIG=35
	float desiredTemp
	def indiceRoom
	boolean closedAllVentsInZone=true
	int nbVents=0
	def switchLevel    
	def ventSwitchesOnSet=[]

	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key = "includedZones$indiceSchedule"
	def zones = settings[key]
 
	log.debug("adjust_vent_settings_in_zone>schedule ${scheduleName}: zones= ${zones}")


	float currentTempAtTstat = thermostat?.currentTemperature.toFloat().round(1)
	String mode = thermostat.currentThermostatMode.toString()

	for (zone in zones) {

		def zoneDetails=zone.split(':')
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		if (mode=='cool') {
			key = "desiredCoolTemp$indiceZone"
			def desiredCool= settings[key]
			desiredTemp= desiredCool.toFloat()                
		} else {
			key = "desiredHeatTemp$indiceZone"
			def desiredHeat= settings[key]
			desiredTemp= desiredHeat.toFloat()
		}
		for (room in rooms) {
        
			log.debug("adjust_vent_settings_in_zone>schedule ${scheduleName}, desiredTemp=${desiredTemp}")

			switchLevel =0	// initially set at zero for check later
			def roomDetails=room.split(':')
			indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]
			key = "needOccupiedFlag$indiceRoom"
			def needOccupied = (settings[key]) ?: false
			log.debug("adjust_vent_settings_in_zone>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}")

			if (needOccupied) {
				key = "motionSensor$indiceRoom"
				def motionSensor = settings[key]
				if (motionSensor != null) {
					if (!isRoomOccupied(motionSensor, indiceRoom)) {
						switchLevel =MIN_OPEN_LEVEL_SMALL // setLevel at a minimum as the room is not occupied.
						log.debug("adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, room = ${roomName},not occupied,vents set to mininum level=${switchLevel}")
					}
				}
			} 
			if (!switchLevel) {
				def tempAtSensor =getSensorTempForAverage(indiceRoom)			
				if (tempAtSensor == null) {
					tempAtSensor= currentTempAtTstat				            
				}
				float temp_diff_at_sensor = tempAtSensor.toFloat().round(1) - desiredTemp 
				log.debug("adjust_vent_settings_in_zone>thermostat mode = ${mode}, schedule ${scheduleName}, in zone ${zoneName}, room ${roomName}, temp_diff_at_sensor=${temp_diff_at_sensor}")
				if (mode=='heat') {
					switchLevel=(temp_diff_at_sensor >=0)? MIN_OPEN_LEVEL_SMALL: 100
				} else if (mode =='cool') {
					switchLevel=(temp_diff_at_sensor <=0)? MIN_OPEN_LEVEL_SMALL: 100
				}                
			} 
			if (switchLevel >=10) {	
				closedAllVentsInZone=false
			}              
                
			log.debug("adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, room ${roomName},switchLevel to be set=${switchLevel}")
			for (int j = 1;(j <= 5); j++)  {
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)                
					log.debug "adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} at switchLevel =${switchLevel}%"
					ventSwitchesOnSet.add(ventSwitch)
					nbVents++                    
				}
			} /* end for ventSwitch */                             
		} /* end for rooms */
	} /* end for zones */

	if ((closedAllVentsInZone) && (nbVents)) {
		    	
		switchLevel=(nbVents>2)? MIN_OPEN_LEVEL_SMALL:MIN_OPEN_LEVEL_BIG        
		ventSwitchesOnSet=control_vent_switches_in_zone(indiceSchedule, switchLevel)		    
		log.debug "adjust_vent_settings_in_zone>schedule ${scheduleName}, set all ventSwitches at ${switchLevel}% to avoid closing all of them"
		if (detailedNotif) {
			send("ScheduleRoomTempControl>schedule ${scheduleName},set all ventSwitches at ${switchLevel}% to avoid closing all of them")
		}
	}    
	return ventSwitchesOnSet    
}

private def turn_off_all_other_vents(ventSwitchesOnSet) {
	def foundVentSwitch
	int nbClosedVents=0, totalVents=0
	float MAX_RATIO_CLOSED_VENTS=50 // not more than 50% of the smart vents should be closed at once
	def MIN_OPEN_LEVEL=25  
	def closedVentsSet=[]
    
	for (indiceRoom in 1..roomsCount) {
		for (int j = 1;(j <= 5); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			if (ventSwitch != null) {
				totalVents++
				log.debug "turn_off_all_other_vents>found=${ventSwitch}"

				foundVentSwitch = ventSwitchesOnSet.find{it == ventSwitch}
				if (foundVentSwitch ==null) {
					nbClosedVents++ 
					closedVentsSet.add(ventSwitch)                        
					log.debug("turn_off_all_other_vents>about to turn off ${ventSwitch} as requested to create the desired zone(s)")
				}                    
			}   /* end if ventSwitch */                  
		}  /* end for ventSwitch */         
	} /* end for rooms */
	float ratioClosedVents=(nbClosedVents/totalVents*100)
    
	if (ratioClosedVents > MAX_RATIO_CLOSED_VENTS) {
		log.debug("turn_off_all_other_vents>ratio of closed vents is too high (${ratioClosedVents.round()}%), opening ${closedVentsSet} at minimum level of ${MIN_OPEN_LEVEL}%")
		if (detailedNotif) {
			send("ScheduleRoomTempControl>ratio of closed vents is too high (${ratioClosedVents.round()}%), opening ${closedVentsSet} at minimum level of ${MIN_OPEN_LEVEL}%")
		}
		closedVentsSet.each {
			setVentSwitchLevel(null, it, MIN_OPEN_LEVEL)
		}        
	} else {
		closedVentsSet.each {
			it.off()
		}        
    
	}        
}


private def open_all_vents() {
	// Turn on all vents        
	for (indiceRoom in 1..roomsCount) {
		for (int j = 1;(j <= 5); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
				if (vent != null) {
					vent.on()	
				} /* end if vent != null */
		} /* end for vent switches */
	} /* end for rooms */
}

private def getTemperatureInVent(ventSwitch) {
	def temp=null
	try {
		temp = ventSwitch.currentTemperature
	} catch (any) {
		log.debug("getTemperatureInVent>Not able to current Temperature from ${ventSwitch}")
	}    
	return temp    
}

private def setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel=100) {
	def roomName
    
	if (indiceRoom) {
		def key = "roomName$indiceRoom"
		roomName = settings[key]
	}
	try {
		ventSwitch.setLevel(switchLevel)
		if (roomName) {       
			log.debug("setVentSwitchLevel>set ${ventSwitch} at level ${switchLevel} in room ${roomName} to reach desired temperature")
			if (detailedNotif) {
				send("ScheduleRoomTempControl>set ${ventSwitch} at level ${switchLevel} in room ${roomName} to reach desired temperature")
			}
		}            
	} catch (e) {
		if (switchLevel >0) {
			ventSwitch.on()        
			log.error "setVentSwitchLevel>not able to set ${ventSwitch} at ${switchLevel} (exception $e), trying to turn it on"
		} else {
			ventSwitch.off()        
			log.error "setVentSwitchLevel>not able to set ${ventSwitch} at ${switchLevel} (exception $e), trying to turn it off"
		}
	}
    
}

private def control_vent_switches_in_zone(indiceSchedule, switchLevel=100) {
	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key = "includedZones$indiceSchedule"
	def zones = settings[key]
	def ventSwitchesOnSet=[]
    
	for (zone in zones) {

		def zoneDetails=zone.split(':')
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
    
		for (room in rooms) {
			def roomDetails=room.split(':')
			def indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]

			for (int j = 1;(j <= 5); j++)  {
	                
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					ventSwitchesOnSet.add(ventSwitch)
					setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)
				}
			} /* end for ventSwitch */
		} /* end for rooms */
	} /* end for zones */
	return ventSwitchesOnSet
}


def IsRightDayForChange(indiceSchedule) {

	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key ="dayOfWeek$indiceSchedule"
	def dayOfWeek = settings[key]
    
	def makeChange = false
	Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
	int currentDayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK);

	// Check the condition under which we want this to run now
	// This set allows the most flexibility.
	if (dayOfWeek == 'All Week') {
		makeChange = true
	} else if ((dayOfWeek == 'Monday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.MONDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Tuesday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.TUESDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Wednesday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.WEDNESDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Thursday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.THURSDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Friday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.FRIDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Saturday' || dayOfWeek == 'Saturday & Sunday') && currentDayOfWeek == Calendar.instance.SATURDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Sunday' || dayOfWeek == 'Saturday & Sunday') && currentDayOfWeek == Calendar.instance.SUNDAY) {
		makeChange = true
	}

	log.debug "IsRightDayforChange>schedule ${scheduleName}, makeChange=${makeChange},Calendar DOW= ${currentDayOfWeek}, dayOfWeek=${dayOfWeek}"

	return makeChange
}


private send(msg) {
	if (sendPushMessage != "No") {
		sendPush(msg)
	}

	if (phone) {
		log.debug("sending text message")
		sendSms(phone, msg)
	}
	log.debug msg
}
