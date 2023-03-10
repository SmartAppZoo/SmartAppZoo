/**
 *  ScheduleRoomTempControl
 *
 *  Copyright Yves Racine
 *  LinkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/
 *
 *  Developer retains all right, title, copyright, andF interest, including all copyright, patent rights, trade secret 
 *  in the Background technology. May be subject to consulting fees under the Agreement between the Developer and the Customer. 
 *  Developer grants a non exclusive perpetual license to use the Background technology in the Software developed for and delivered 
 *  to Customer under this Agreement. However, the Customer shall make no commercial use of the Background technology without
 *  Developer's written consent.
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *
 *  Software Distribution is restricted and shall be done only with Developer's written approval.
 */
import groovy.transform.Field

definition(
	name: "${get_APP_NAME()}",
	namespace: "acrosscable12814",
	author: "Yves Racine (adapted by Dieter R)",
	description: "Enable better temperature control in rooms based on Smart Vents",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

def get_APP_VERSION() {return "5.1.4d"}

preferences {

	page(name: "dashboardPage")
	page(name: "generalSetupPage")
	page(name: "roomsSetupPage")
	page(name: "zonesSetupPage")
	page(name: "schedulesSetupPage")
	page(name: "configDisplayPage")
	page(name: "NotificationsPage")
	page(name: "roomsSetup")
	page(name: "zonesSetup")
	page(name: "schedulesSetup")
	page(name: "ventSettingsSetup")
}

def dashboardPage() {
	state?.scale= getTemperatureScale()
	def scale = state?.scale    
	dynamicPage(name: "dashboardPage", title: "Dashboard", uninstall: true, nextPage: generalSetupPage,submitOnChange: true) {
		section("Tap Running Schedule(s) Config for latest info\nPress Next (upper right) for initial Setup") {
			if (roomsCount && zonesCount && schedulesCount) {
				paragraph image: "${getCustomImagePath()}office7.png", "location mode: $location.mode" +
					"\nLast Running Schedule: ${state?.lastScheduleName}" +
					"\nIncludedZone(s): ${state?.activeZones}"
				if (state?.avgTempDiff)  { 
					paragraph "AvgTempDiffInZone: ${state?.avgTempDiff}$scale"                   
				}
				if (thermostat) {                	
					def currentTempAtTstat = thermostat.currentTemperature.toFloat().round(1) 
					String mode = thermostat?.currentThermostatMode
					def operatingState=thermostat.currentThermostatOperatingState                
					def heatingSetpoint,coolingSetpoint
					switch (mode) { 
 						case 'cool':
							coolingSetpoint = thermostat.currentValue('coolingSetpoint')
						break                            
 						case 'auto': 
						case 'off': 
						case 'eco': 
							try {                    
		 						coolingSetpoint = thermostat?.currentValue('coolingSetpoint')
							} catch (e) {
								traceEvent(settings.logFilter,"dashboardPage>not able to get coolingSetpoint from $thermostat,exception $e",
									settings.detailedNotif,GLOBAL_LOG_INFO)                                
							}                        
							coolingSetpoint=  (coolingSetpoint)? coolingSetpoint: (scale=='C')?23:73                        
						case 'heat':
						case 'emergency heat':
						case 'auto': 
						case 'eco': 
							try {                    
		 						heatingSetpoint = thermostat?.currentValue('heatingSetpoint')
							} catch (e) {
								traceEvent(settings.logFilter,"dashboardPage>not able to get heatingSetpoint from $thermostat,exception $e",
									settings.detailedNotif,GLOBAL_LOG_INFO)                                
							}                        
							heatingSetpoint=  (heatingSetpoint)? heatingSetpoint: (scale=='C')?21:72                        
						break
						default:
							log.warn "dashboardPage>invalid mode $mode"
						break                        
					}        
					def dParagraph= "TstatMode: $mode" +
						"\nTstatOperatingState: $operatingState" +
						"\nTstatCurrentTemp: ${currentTempAtTstat}$scale"                
					if (coolingSetpoint)  { 
						dParagraph = dParagraph + "\nCoolingSetpoint: ${coolingSetpoint}$scale"
					}     
					if (heatingSetpoint)  { 
						dParagraph = dParagraph + "\nHeatingSetpoint: ${heatingSetpoint}$scale" 
					}     
					paragraph image: "${getCustomImagePath()}home1.png", dParagraph 
				}                        
				if ((state?.closedVentsCount) || (state?.openVentsCount)) {
					paragraph "    ** SMART VENTS SUMMARY **\n              For Active Zone(s)\n" 
					String dPar = "OpenVentsCount: ${state?.openVentsCount}" +                    
						"\nMaxOpenLevel: ${state?.maxOpenLevel}%" +
						"\nMinOpenLevel: ${state?.minOpenLevel}%" +
						"\nAvgVentLevel: ${state?.avgVentLevel}%" 
					if (state?.minTempInVents) {
						dPar=dPar +  "\nMinVentTemp: ${state?.minTempInVents}${scale}" +                    
						"\nMaxVentTemp: ${state?.maxTempInVents}${scale}" +
						"\nAvgVentTemp: ${state?.avgTempInVents}${scale}"
					}
					paragraph image: "${getCustomImagePath()}ventopen.png",dPar                    
					if (state?.totalVents) {
						paragraph image: "${getCustomImagePath()}ventclosed.png","ClosedVentsInZone: ${state?.closedVentsCount}" +
						 "\nClosedVentsTotal: ${state?.totalClosedVents}" +
						"\nRatioClosedVents: ${state?.ratioClosedVents}%" +
						"\nVentsTotal: ${state?.totalVents}" 
					}
				}                
				href(name: "toConfigurationDisplayPage", title: "Running Schedule(s) Config", page: "configDisplayPage") 
			}
		} /* end section dashboard */
		section("ABOUT") {
			paragraph image:"${getCustomImagePath()}ecohouse.jpg","${get_APP_NAME()}, the smartapp that enables better temperature control in rooms based on Smart Vents"
			paragraph "Version ${get_APP_VERSION()}"
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.me/ecomatiqhomes",
					title:"Paypal donation..."
			paragraph "Copyright©2015-2020 Yves Racine"
				href url:"https://www.maisonsecomatiq.com/#!home/mainPage", style:"embedded", required:false, title:"More information..."  
					description: "https://www.maisonsecomatiq.com/#!home/mainPage"
		} /* end section about  */
	}
}

def generalSetupPage() {
	dynamicPage(name: "generalSetupPage", title: "General Setup",submitOnChange: true, uninstall:false, nextPage: roomsSetupPage,
		refreshAfterSelection:true) {
		section ("") {
			paragraph "Warning: don't hit the back button, use the links to navigate back to a page\n"
		}
        
		section("Main thermostat at home (used for vent adjustment) [optional]") {
			input (image: "${getCustomImagePath()}home1.png", name:"thermostat", type: "capability.thermostat", title: "Which main thermostat?",required:false)
		}
		section("Rooms count") {
			input (name:"roomsCount", title: "Rooms count (max=${get_MAX_ROOMS()})?", type: "number", range: "1..${get_MAX_ROOMS()}")
		}
		section("Zones count") {
			input (name:"zonesCount", title: "Zones count (max=${get_MAX_ZONES()})?", type:"number",  range: "1..${get_MAX_ZONES()}")
		}
		section("Schedules count") {
			input (name:"schedulesCount", title: "Schedules count (max=${get_MAX_SCHEDULES()})?", type: "number",  range: "1..${get_MAX_SCHEDULES()}")
		}
		section("Links to other setup pages") {        
			href(name: "toRoomPage", title: "Rooms Setup", page: "roomsSetupPage", description: "Tap to configure", image: "${getCustomImagePath()}room.png")
			href(name: "toZonePage", title: "Zones Setup", page: "zonesSetupPage",  description: "Tap to configure",image: "${getCustomImagePath()}zoning.jpg")
			href(name: "toSchedulePage", title: "Schedules Setup", page: "schedulesSetupPage",  description: "Tap to configure",image: "${getCustomImagePath()}office7.png")
			href(name: "toNotificationsPage", title: "Notification & Options Setup", page: "NotificationsPage",  description: "Tap to configure", image: "${getCustomImagePath()}notification.png")
		}      
        
		section("Optional Vent settings [optional, default=false]") {
			input (name:"delayInSecForVentSettings", title: "Delay in seconds after HVAC is running (heating, cooling, fan only) for vent settings (10..120)?", type:"number",
				range: "10..120", description:"no explicit delay",required:false)
		}
		section("Enable Contact Sensors to be used for vent adjustments [optional, default=false]") {
			input (name:"setVentAdjustmentContactFlag", title: "Enable vent adjustment set in schedules based on contact sensors?", type:"bool",
				description:" if true and contact open=>vent(s) closed in schedules",required:false)
		}
        
		section("Disable or Modify the safeguards [default=some safeguards are implemented to avoid damaging your HVAC by closing too many vents]") {
			input (name:"fullyCloseVentsFlag", title: "Bypass all safeguards & allow closing the vents totally?", type:"bool",required:false)
			input (name:"minVentLevelInZone", title: "Safeguard's Minimum Vent Level", type:"number", required: false, description: "[default=10%]")
			input (name:"minVentLevelOutZone", title: "Safeguard's Minimum Vent Level Outside of the Zone", type:"number", required: false, description: "[default=25%]")
			input (name:"maxVentTemp", title: "Safeguard's Maximum Vent Temp", type:"number", required: false, description: "[default= 131F/55C]")
			input (name:"minVentTemp", title: "Safeguard's Minimum Vent Temp", type:"number", required: false, description: "[default= 45F/7C]")
			input (name:"maxPressureOffsetInPa", title: "Safeguard's Max Vent Pressure Offset with room's pressure baseline [unit: Pa]", type:"decimal", required: false, description: "[default=124.54Pa/0.5'' of water]")
		}       
		section("What do I use for the Master on/off switch to enable/disable smartapp processing? [optional]") {
			input (name:"powerSwitch", type:"capability.switch", required: false,description: "Optional")
		}
		section {
			href(name: "toDashboardPage", title: "Back to Dashboard Page", page: "dashboardPage")
		}
	}
}

def roomsSetupPage() {

	dynamicPage(name: "roomsSetupPage", title: "Rooms Setup", uninstall: false, nextPage: zonesSetupPage) {
		section("Press each room slot below to complete setup") {
			for (int i = 1; ((i <= settings.roomsCount) && (i <= get_MAX_ROOMS())); i++) {
				href(name: "toRoomPage$i", page: "roomsSetup", params: [indiceRoom: i], required:false, description: roomHrefDescription(i), 
					title: roomHrefTitle(i), state: roomPageState(i),image: "${getCustomImagePath()}room.png" )
			}
		}            
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}        

def roomPageState(i) {

	if (settings."roomName${i}" != null) {
		return 'complete'
	} else {
		return 'incomplete'
	}
  
}

def roomHrefTitle(i) {
	def title = "Room ${i}"
	return title
}

def roomHrefDescription(i) {
	def description ="Room #${i} "

	if (settings."roomName${i}" !=null) {
		description += settings."roomName${i}"		    	
	}
	return description
}

def roomsSetup(params) {
	def indiceRoom=1   
	// Assign params to indiceRoom.  Sometimes parameters are double nested.
	if (params?.indiceRoom) {
		indiceRoom = params.indiceRoom
		state?.params=params        
	} else if (state?.params?.indiceRoom) {    
		indiceRoom = state?.params.indiceRoom
	}    
 
	indiceRoom=indiceRoom.intValue()

	dynamicPage(name: "roomsSetup", title: "Rooms Setup", uninstall: false, nextPage: zonesSetupPage) {

		section("Room ${indiceRoom} Setup") {
			input "roomName${indiceRoom}", title: "Room Name", type: "text",image: "${getCustomImagePath()}room.png"
		}
		section("Room ${indiceRoom}-Temp Sensor [optional]") {
			input image: "${getCustomImagePath()}IndoorTempSensor.png", "tempSensor${indiceRoom}", title: "Temp sensor for better temp adjustment", "capability.temperatureMeasurement", 
				required: false, description: "Optional"

		}
		section("Room ${indiceRoom}-Contact Sensors [optional]") {
			input image: "${getCustomImagePath()}contactSensor.png", "contactSensor${indiceRoom}", title: "Contact sensor(s) for better vent/temp adjustment", "capability.contactSensor", 
				required: false, multiple:true, description: "Optional, contact open=>vent is closed"
			input "contactClosedLogicFlag${indiceRoom}", title: "Inverse temp/vent logic,contact open=>vent is open [default=false]", "bool",  
				required: false, description: "Optional"

		}
        
		section("Room ${indiceRoom}-Vents Setup [optional]")  {
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				input image: "${getCustomImagePath()}ventclosed.png","ventSwitch${j}${indiceRoom}", title: "Vent switch #${j} in room", "capability.switch", 
					required: false, description: "Optional"
				input "ventLevel${j}${indiceRoom}", title: "set vent #${j}'s level in room [optional, range 0-100]", "number", range: "0..100",
						required: false, description: "blank:calculated by smartapp"
			}           
		}          
		section("Room ${indiceRoom}-Pressure Sensor [optional]") {
			input image: "${getCustomImagePath()}pressure.png", "pressureSensor${indiceRoom}", title: "Pressure sensor used for HVAC safeguard", "capability.sensor", 
				required: false, description: "Optional"

		}
		section("Room ${indiceRoom}-Motion Detection parameters [optional]") {
			input image: "${getCustomImagePath()}MotionSensor.png","motionSensor${indiceRoom}", title: "Motion sensor (if any) to detect if room is occupied", "capability.motionSensor", 
				required: false, description: "Optional"
			input "needOccupiedFlag${indiceRoom}", title: "Will do vent adjustement only when Occupied [default=false]", "bool",  
				required: false, description: "Optional"
			input "residentsQuietThreshold${indiceRoom}", title: "Threshold in minutes for motion detection [default=15 min]", "number", 
				required: false, description: "Optional"
			input "occupiedMotionOccNeeded${indiceRoom}", title: "Motion counter for positive detection [default=1 occurence]", "number", 
				required: false, description: "Optional"
		}
		section("Room ${indiceRoom}-Make this room inactive in all zones & schedules?") {
			input "inactiveRoomFlag${indiceRoom}", title: "Inactive?", "bool", 
				required: false, description: "false"
		}
		section {
			href(name: "toRoomsSetupPage", title: "Back to Rooms Setup Page", page: "roomsSetupPage")
		}
	}
}


def configDisplayPage() {
	def key 
	def fullyCloseVents = (settings.fullyCloseVentsFlag) ?: false 	
    
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone) 
	float desiredTemp, median
	def scale = (state?.scale) ?: getTemperatureScale()
	def currTime = now()	 
	boolean foundSchedule=false 
	String bypassSafeguardsString= (fullyCloseVents)?'true':'false'                             
	String setpointFlagString= (noSetPoints=='false')?'true':'false'                             
	float currentTempAtTstat =(scale=='C')?21:72 	// set a default value 
	String mode='auto', operatingState
	int nbClosedVents=0, nbOpenVents=0, totalVents=0, nbRooms=0 
	int min_open_level=100, max_open_level=0, total_level_vents=0     
	float min_temp_in_vents=200, max_temp_in_vents=0, total_temp_diff=0
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	def MIN_OPEN_LEVEL_OUT_ZONE=(minVentLevelOutZone!=null)?((minVentLevelOutZone>=0 && minVentLevelOutZone <100)?minVentLevelOutZone:25):25
	def MAX_TEMP_VENT_SWITCH = (settings.maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (settings.minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch
	def MAX_PRESSURE_OFFSET = (settings.maxPressureOffsetInPa)?:124.54     //Translate to  0.5 inches of water in Pa

	def heatingSetpoint,coolingSetpoint
	def desiredCool,desiredHeat
	if (thermostat) { 
		currentTempAtTstat = thermostat.currentTemperature.toFloat().round(1) 
		mode = thermostat.currentThermostatMode
		operatingState=thermostat.currentThermostatOperatingState
	}         

	traceEvent(settings.logFilter,"configDisplayPage>About to display Running Schedule(s) Configuration",settings.detailedNotif)
	dynamicPage(name: "configDisplayPage", title: "Running Schedule(s) Config", nextPage: generalSetupPage,submitOnChange: true) {
		section {
			href(name: "toDashboardPage", title: "Back to Dashboard Page", page: "dashboardPage")
		}
        
		def detailedNotifString=(settings.detailedNotif)?'true':'false'			            
		def askAlexaString=(settings.askAlexaFlag)?'true':'false'			            
		section("General") {
			paragraph image: "${getCustomImagePath()}notification.png", "Notifications" +
					"\n  >Detailed Notification: $detailedNotifString" +
					"\n  >AskAlexa Notifications: $askAlexaString"             
			paragraph image: "${getCustomImagePath()}home1.png", "location: $location.mode" 
			if (thermostat) {                	
				switch (mode) { 
					case 'cool':
 						coolingSetpoint = thermostat.currentValue('coolingSetpoint')
					break                        
					case 'auto': 
					case 'off': 
					case 'eco': 
						try {                    
		 					coolingSetpoint = thermostat?.currentValue('coolingSetpoint')
						} catch (e) {
							traceEvent(settings.logFilter,"dashboardPage>not able to get coolingSetpoint from $thermostat,exception $e",
								settings.detailedNotif,GLOBAL_LOG_INFO)                                
						}                        
						coolingSetpoint=  (coolingSetpoint)?: (scale=='C')?23:73                        
					case 'heat':
					case 'emergency heat':
					case 'auto': 
					case 'eco': 
						try {                    
		 					heatingSetpoint = thermostat?.currentValue('heatingSetpoint')
						} catch (e) {
							traceEvent(settings.logFilter,"ConfigDisplayPage>not able to get heatingSetpoint from $thermostat, exception $e",
								settings.detailedNotif, GLOBAL_LOG_INFO)                            
						}                        
						heatingSetpoint=  (heatingSetpoint)?: (scale=='C')?21:72   
					break                        
					default:
						log.warn "ConfigDisplayPage>invalid mode $mode"
					break                        
				}                        
				def setVentAdjustmentContactString=(settings.setVentAdjustmentContactFlag)?'true':'false'
				paragraph "  >TstatMode: $mode" +
						"\n  >TstatOperatingState: $operatingState" +
						"\n  >TstatCurrentTemp: ${currentTempAtTstat}$scale"                
				if (coolingSetpoint)  { 
					paragraph "  >TstatCoolingSetpoint: ${coolingSetpoint}$scale"
				}                        
				if (heatingSetpoint)  { 
					paragraph "  >TstatHeatingSetpoint: ${heatingSetpoint}$scale"
				}    
			}                
			if (state?.avgTempDiff)  {   
				paragraph " >AvgTempDiffInZone: ${state?.avgTempDiff.toFloat().round(1)}$scale"                     
			}  
			paragraph image: "${getCustomImagePath()}safeguards.jpg","Safeguards"
 			paragraph "  >BypassSafeguards: ${bypassSafeguardsString}" +
					"\n  >MinVentLevelInZone: ${MIN_OPEN_LEVEL_IN_ZONE}%" +
					"\n  >MinVentLevelOutZone: ${MIN_OPEN_LEVEL_OUT_ZONE}%" +
					"\n  >MinVentTemp: ${MIN_TEMP_VENT_SWITCH}${scale}" +
					"\n  >MaxVentTemp: ${MAX_TEMP_VENT_SWITCH}${scale}" +
					"\n  >MaxPressureOffset: ${MAX_PRESSURE_OFFSET} Pa" 
  		}
		for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
        
			key = "selectedMode$i"
			def selectedModes = settings[key]
			key = "scheduleName$i"
			def scheduleName = settings[key]
			traceEvent(settings.logFilter,"configDisplayPage>looping thru schedules, now at $scheduleName",settings.detailedNotif)
			boolean foundMode=selectedModes.find{it == (location.mode as String)} 
			if ((selectedModes != null) && (!foundMode)) {
        
				traceEvent(settings.logFilter,"configDisplayPage>schedule=${scheduleName} does not apply,location.mode= $location.mode, selectedModes=${selectedModes},foundMode=${foundMode}, continue",
					settings.detailedNotif)                
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
				traceEvent(settings.logFilter,"configDisplayPage>schedule ${scheduleName}, subtracted - 1 day, new startTime=${startTimeToday.time}",
					settings.detailedNotif)                
			}            
			if ((currTime > endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
				endTimeToday = endTimeToday +1        
				traceEvent(settings.logFilter,"configDisplayPage>schedule ${scheduleName}, added + 1 day, new endTime=${endTimeToday.time}",settings.detailedNotif)
			}   
            
			String startInLocalTime = startTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
			String endInLocalTime = endTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
            
			if ((currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange(i))) {
                
				traceEvent(settings.logFilter,"configDisplayPage>$scheduleName is good to go..",settings.detailedNotif)
				key = "givenClimate${i}"
				def climate = settings[key]
                
				key = "includedZones$i"
				def zones = settings[key]
				key = "desiredCool${i}"
				def desiredCoolTemp = (settings[key])?: ((scale=='C') ? 23:75)
				key = "desiredHeat${i}"
				def desiredHeatTemp = (settings[key])?: ((scale=='C') ? 21:72)
				key = "adjustVentsEveryCycleFlag${i}"
				String adjustVentsEveryCycleString = (settings[key])?'true':'false'
				key = "openVentsFanOnlyFlag${i}"                
				def openVentsWhenFanOnlyString = (settings[key])?'true':'false'                
				key = "setVentLevel${i}"
				def setLevel = settings[key]
				key = "resetLevelOverrideFlag${i}"
				def resetLevelOverrideString=(settings[key])?'true':'false'
  				key = "inactiveScheduleFlag${i}"                
				def activeScheduleString = (settings[key])?'false':'true'                
				if (activeScheduleString=='true') {
					foundSchedule=true				                
				}                    
				def ventDelayInSecsString=(settings.delayInSecForVentSettings)?:0		            

				traceEvent(settings.logFilter,"configDisplayPage>about to display schedule $scheduleName..",settings.detailedNotif)
				                
				section("Running Schedule(s)") {
					paragraph image: "${getCustomImagePath()}office7.png","Schedule $scheduleName"  +
						"\n >ActiveSchedule: $activeScheduleString" +                    
						"\n >StartTime: $startInLocalTime" +                    
						"\n >EndTime: $endInLocalTime"                   
					if (setLevel) {
						paragraph " >DefaultSetLevelForAllVentsInZone(s): ${setLevel}%"
					}                        
					paragraph " >BypassSetLevelOverrideinZone(s): ${resetLevelOverrideString}" +
						"\n >AdjustVentsEveryCycle: $adjustVentsEveryCycleString" + 
						"\n >VentLevelSetDelay: ${ventDelayInSecsString} second(s)" +
						"\n >OpenVentsWhenFanOnly: $openVentsWhenFanOnlyString"                        
					key = "noSetpointsFlag$i"
					def noSetpointInSchedule = settings[key]?: false
					def setpointsAtThermostat = (noSetpointInSchedule==true)?'false':'true'                    
					paragraph " >SetpointsAtThermostat: $setpointsAtThermostat"  
					if (!noSetpointInSchedule) {
						if (climate) {
							paragraph " >EcobeeProgramSet: $climate" 
						} else {
							if (desiredCoolTemp) {                            
								paragraph " >DesiredCool: ${desiredCoolTemp}$scale" 
							}                                
							if (desiredHeatTemp) {                            
								paragraph " >DesiredHeat: ${desiredHeatTemp}$scale"
							}
						}                                
					}                            
                    
					if (selectedModes) {                    
						paragraph " >LocationModes: $selectedModes"
					}                        
					paragraph " >Includes: $zones" 
				}
				state?.activeZones = zones // save the zones for the dashboard                
				for (zone in zones) {
					def zoneDetails=zone.split(':') 
 					def indiceZone = zoneDetails[0] 
					def zoneName = zoneDetails[1] 
					key = "desiredCoolTemp${indiceZone}"
					desiredCool = settings[key]
					key = "desiredHeatTemp${indiceZone}"
					desiredHeat = settings[key]
					key = "inactiveZoneFlag${indiceZone}"                
					def activeZoneString = (settings[key])?'false':'true'                
					key  = "desiredHeatDeltaTemp$indiceZone"
					def desiredHeatDelta =  (state?."desiredHeatTempDelta$indiceZone")? state?."desiredHeatTempDelta$indiceZone".toFloat(): settings[key]
					key  = "desiredCoolDeltaTemp$indiceZone"
					def desiredCoolDelta = (state?."desiredCoolTempDelta$indiceZone")? state?."desiredCoolTempDelta$indiceZone".toFloat(): settings[key]
                        
					key = "includedRooms$indiceZone" 
					def rooms = settings[key] 
					if ((mode == 'cool') || ((mode in ['auto', 'off']) && (currentTempAtTstat > median))) { 
						
						if (desiredCool) { 
							desiredTemp= desiredCool.toFloat() + (desiredCoolDelta?:0)
						} else { 
							desiredTemp = ((coolingSetpoint)?:(scale=='C')?23:75) + (desiredCoolDelta?:0)
 						}                 
					}                           
					if ((mode.contains('heat')) || ((mode in ['auto', 'off', 'eco']) && (currentTempAtTstat < median))) { 
 
						if (desiredHeat) { 
							desiredTemp= desiredHeat.toFloat() + (desiredHeatDelta?:0)
						} else {
							desiredTemp = ((heatingSetpoint)?:(scale=='C')?21:72) + (desiredHeatDelta?:0)
						}                 
					} 
					section("Active Zone(s) in Schedule $scheduleName") { 
						paragraph image: "${getCustomImagePath()}zoning.jpg", "Zone $zoneName" +
							"\n >ActiveZone: $activeZoneString" 
						if (desiredTemp) {                         
							paragraph " >TempThresholdForVents: ${desiredTemp}$scale"  
						}   
						if (desiredCoolDelta) {                         
							paragraph " >DesiredCoolDeltaSP: ${desiredCoolDelta}$scale"  
						}   
						if (desiredHeatDelta) {                         
							paragraph " >DesiredHeatDeltaSP: ${desiredHeatDelta}$scale"  
						}   
                            
						paragraph " >Includes: $rooms" 
					} 
					for (room in rooms) { 
						def roomDetails=room.split(':') 
						def indiceRoom = roomDetails[0] 
						def roomName = roomDetails[1] 
						key = "needOccupiedFlag$indiceRoom" 
						def needOccupied = (settings[key]) ?: false 
						traceEvent(settings.logFilter,"configDisplayPage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
							settings.detailedNotif)                            
						key = "motionSensor${indiceRoom}" 
						def motionSensor = (settings[key])  
						key = "contactSensor${indiceRoom}"
						def contactSensor = settings[key]
						key = "tempSensor${indiceRoom}" 
						def tempSensor = (settings[key])  
						def tempAtSensor =getSensorTemperature(indiceRoom)			 
						if (tempAtSensor == null) { 
							tempAtSensor= currentTempAtTstat				             
						} 
						key = "inactiveRoomFlag${indiceRoom}"                
						def activeRoomString = (settings[key])?'false':'true'                
						key = "pressureSensor$indiceRoom"
						def pressureSensor = settings[key]
                            
						section("Room(s) in Zone $zoneName") { 
 							nbRooms++                         
							paragraph image: "${getCustomImagePath()}room.png","$roomName" +
								"\n >ActiveRoom: $activeRoomString" 
							if (tempSensor) {                            
								paragraph image: "${getCustomImagePath()}IndoorTempSensor.png", "TempSensor: $tempSensor" 
							}                                
							if (tempAtSensor) { 
								if (desiredTemp) {                             
 									float temp_diff = (tempAtSensor.toFloat() - desiredTemp).round(1)  
									paragraph " >CurrentTempOffSet: ${temp_diff.round(1)} $scale"  
									total_temp_diff = total_temp_diff + temp_diff                                     
								}                                     
								paragraph " >CurrentTempInRoom: ${tempAtSensor}$scale" 
 							}                                 
							if (contactSensor) {      
								key = "contactClosedLogicFlag$indiceRoom" 
								def contactClosedLogicString=(settings[key])?'true':'false'                            
								if (any_contact_open(contactSensor)) {
									paragraph image: "${getCustomImagePath()}contactSensor.png", " ContactSensor: $contactSensor" + 
										"\n >ContactState: open" + 
										"\n >ContactOpenForVentOpen: $contactClosedLogicString" 
								} else {
									paragraph image: "${getCustomImagePath()}contactSensor.png", " ContactSensor: $contactSensor" + 
										"\n >ContactState: all closed" +
										"\n >ContactOpenForOpenVent: $contactClosedLogicString" 
								}
							}
							def baselinePressure
							if (pressureSensor) {
								baselinePressure= pressureSensor.currentValue("pressure")								                            
								paragraph image: "${getCustomImagePath()}pressure.png", " PressureSensor: $pressureSensor" + 
									"\n >BaselinePressure: ${baselinePressure} Pa"                                
							}                              
							if (motionSensor) {      
								def countActiveMotion=isRoomOccupied(motionSensor, indiceRoom)
								String needOccupiedString= (needOccupied)?'true':'false'
								if (!needOccupied) {                                
										paragraph " >MotionSensor: $motionSensor" +
										"\n ->NeedToBeOccupied: ${needOccupiedString}" 
								} else {                                        
									key = "residentsQuietThreshold${indiceRoom}"
									def threshold = (settings[key]) ?: 15 // By default, the delay is 15 minutes 
									String thresholdString = threshold   
									key = "occupiedMotionOccNeeded${indiceRoom}"
									def occupiedMotionOccNeeded= (settings[key]) ?:1
									key = "occupiedMotionTimestamp${indiceRoom}"
									def lastMotionTimestamp = (state[key])
									String lastMotionInLocalTime                                     
									def isRoomOccupiedString=(countActiveMotion>=occupiedMotionOccNeeded)?'true':'false'                                
									if (lastMotionTimestamp) {                                    
										lastMotionInLocalTime= new Date(lastMotionTimestamp).format("yyyy-MM-dd HH:mm", location.timeZone)
									}						                                    
                                    
									paragraph image: "${getCustomImagePath()}MotionSensor.png", "MotionSensor: $motionSensor" 
									paragraph "  >IsRoomOccupiedNow: ${isRoomOccupiedString}" + 
										"\n  >NeedToBeOccupied: ${needOccupiedString}" + 
										"\n  >OccupiedThreshold: ${thresholdString} minutes"+ 
										"\n  >MotionCountNeeded: ${occupiedMotionOccNeeded}" + 
										"\n  >OccupiedMotionCounter: ${countActiveMotion}" +
										"\n  >LastMotionTime: ${lastMotionInLocalTime}"
								}
							}                                
							paragraph "** VENTS in $roomName **" 
							float total_temp_in_vents=0                            
							for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
							key = "ventSwitch${j}$indiceRoom"
							def ventSwitch = settings[key]
							if (ventSwitch != null) {
								float temp_in_vent=getTemperatureInVent(ventSwitch)                               
								// compile some stats for the dashboard                    
								if (temp_in_vent) {                                   
									min_temp_in_vents=(temp_in_vent < min_temp_in_vents)? temp_in_vent.toFloat().round(1) : min_temp_in_vents
									max_temp_in_vents=(temp_in_vent > max_temp_in_vents)? temp_in_vent.toFloat().round(1) : max_temp_in_vents
									total_temp_in_vents=total_temp_in_vents + temp_in_vent
								}                                        
								def switchLevel = getCurrentVentLevel(ventSwitch)							                        
								totalVents++                                    
								def ventPressure=ventSwitch.currentValue("pressure")
								if (baselinePressure) {                            
									float offsetPressure=(ventPressure.toFloat() - baselinePressure.toFloat()).round(2)                                     
									paragraph image: "${getCustomImagePath()}ventopen.png","$ventSwitch"
									paragraph " >CurrentVentLevel: ${switchLevel}%" +
										"\n >CurrentVentStatus: ${ventSwitch.currentValue("switch")}" +                                     
										"\n >VentPressure: ${ventPressure} Pa" +                                      
										"\n >BaseOffsetPressure: ${offsetPressure} Pa"     
								} else {                                            
									paragraph image: "${getCustomImagePath()}ventopen.png","$ventSwitch"
									paragraph " >CurrentVentLevel: ${switchLevel}%" +
										"\n >CurrentVentStatus: ${ventSwitch.currentValue("switch")}" +                                     
										"\n >VentPressure: ${ventPressure} Pa"                                       
								}                                            
								if (switchLevel) {                                    
									// compile some stats for the dashboard                    
									min_open_level=(switchLevel.toInteger() < min_open_level)? switchLevel.toInteger() : min_open_level
									max_open_level=(switchLevel.toInteger() > max_open_level)? switchLevel.toInteger() : max_open_level
									total_level_vents=total_level_vents + switchLevel.toInteger()                                    
									if (switchLevel > MIN_OPEN_LEVEL_IN_ZONE) {
										nbOpenVents++                                    
									} else {
										nbClosedVents++                                    
									}                                        
								}                                        
                            
								input "ventLevel${j}${indiceRoom}", title: "  >override vent level [Optional,0-100]", "number", range: "0..100",
									required: false, description: "  blank:calculated by smartapp"
								}                            
							}  
						} /* end section rooms */
					} /* end for rooms */
				} /* end for zones */
			} /* end if current schedule */ 
		} /* end for schedules */
		state?.closedVentsCount= nbClosedVents                                  
		state?.openVentsCount= nbOpenVents         
		state?.minOpenLevel= min_open_level
		state?.maxOpenLevel= max_open_level
		state?.minTempInVents=min_temp_in_vents
		state?.maxTempInVents=max_temp_in_vents
		traceEvent(settings.logFilter,"configDisplayPage>foundSchedule=$foundSchedule",settings.detailedNotif)
		if (total_temp_in_vents) {
			state?.avgTempInVents= (total_temp_in_vents/totalVents).toFloat().round(1)
		}		        
		if (total_level_vents) {    
			state?.avgVentLevel= (total_level_vents/totalVents).toFloat().round(1)
		}		        
		nbClosedVents=0        
		nbOpenVents=0    
		totalVents=0        
		// Loop thru all smart vents to get the total count of vents (open,closed)
		for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					totalVents++                
					def switchLevel = getCurrentVentLevel(ventSwitch)							                        
					if ((switchLevel!=null) && (switchLevel <= MIN_OPEN_LEVEL_IN_ZONE)) {
						nbClosedVents++                                    
					}                                        
				} /* end if ventSwitch != null */
			} /* end for switches null */
		} /* end for vent rooms */

		// More stats for dashboard
		if (total_temp_diff ) {
			state?.avgTempDiff = (total_temp_diff/nbRooms).round(1)			        
		}            
		state?.totalVents=totalVents
		state?.totalClosedVents=nbClosedVents
		if (nbClosedVents) {
			float ratioClosedVents=((nbClosedVents/state?.totalVents).toFloat()*100)
			state?.ratioClosedVents=ratioClosedVents.round(1)
		} else {
			state?.ratioClosedVents=0
		}
		if (!foundSchedule) {         
			section {
				paragraph "\n\nNo Schedule running at this time $nowInLocalTime" 
			}	                
		}
		section {
			href(name: "toDashboardPage", title: "Back to Dashboard Page", page: "dashboardPage")
		}
	} /* end dynamic page */                
}


def zoneHrefDescription(i) {
	def description ="Zone #${i} "

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

	dynamicPage(name: "zonesSetupPage", title: "Zones Setup", uninstall: false, nextPage: schedulesSetupPage) {
		section("Press each zone slot below to complete setup") {
			for (int i = 1; ((i <= settings.zonesCount) && (i<= get_MAX_ZONES())); i++) {
				href(name: "toZonePage$i", page: "zonesSetup", params: [indiceZone: i], required:false, description: zoneHrefDescription(i), 
					title: zoneHrefTitle(i), state: zonePageState(i),  image: "${getCustomImagePath()}zoning.jpg" )
			}
		}            
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}        

def zonesSetup(params) {

	def rooms = []
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		def key = "roomName$indiceRoom"
		def room = "${indiceRoom}:${settings[key]}"
		rooms = rooms + room
	}
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
	dynamicPage(name: "zonesSetup", uninstall: false, title: "Zones Setup") {
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
		section("Zone ${indiceZone}-Static Cool Temp threshold in the zone (below it, when cooling, the vents are -partially- closed)") {
			input (name:"desiredCoolTemp${indiceZone}", type:"decimal", title: "Cool Temp Threshold [if blank, then dynamic threshold is used with thermostat only]", 
				required: false,defaultValue:settings."desiredCoolTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Static Heat Temp threshold in the zone (above it, when heating, the vents are -partially- closed)") {
			input (name:"desiredHeatTemp${indiceZone}", type:"decimal", title: "Heat Temp Threshold [if blank, then dynamic threshold is used with thermostat only]", 
				required: false, defaultValue:settings."desiredHeatTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Dynamic Cool Temp threshold based on the coolSP at thermostat (above it, when cooling, the vents are -partially- closed)") {
			input (name:"desiredCoolDeltaTemp${indiceZone}", type:"decimal", range: "*..*", title: "Dynamic Cool Temp Threshold [default = +/-0F or +/-0C]", 
				required: false, defaultValue:settings."desiredCoolDeltaTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Dynamic Heat Temp threshold based on the heatSP at thermostat (above it, when heating, the vents are -partially- closed)") {
			input (name:"desiredHeatDeltaTemp${indiceZone}", type:"decimal", range: "*..*", title: "Dynamic Heat Temp Threshold [default = +/-0F or +/-0C]", 
				required: false, defaultValue:settings."desiredHeatDeltaTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Create a virtual device for controlling the zone?") {
			input "virtualZoneFlag${indiceZone}", title: "Virtual Device for the zone?", "bool", 
				required: false, description: "false"
		}
		section("Zone ${indiceZone}-Make this zone inactive in all schedules?") {
			input "inactiveZoneFlag${indiceZone}", title: "Inactive?", "bool", 
				required: false, description: "false"
		}
		section {
			href(name: "toZonesSetupPage", title: "Back to Zones Setup Page", page: "zonesSetupPage")
		}
	}            
}

def scheduleHrefDescription(i) {
	def description ="Schedule #${i} " 
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
	dynamicPage(name: "schedulesSetupPage", title: "Schedules Setup", uninstall: false, nextPage: NotificationsPage) {
		section("Press each schedule slot below to complete setup") {
			for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
				href(name: "toSchedulePage$i", page: "schedulesSetup", params: [indiceSchedule: i],required:false, description: scheduleHrefDescription(i), 
					title: scheduleHrefTitle(i), state: schedulePageState(i),image: "${getCustomImagePath()}office7.png" )
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
		traceEvent(settings.logFilter,"Not able to get the list of climates (ecobee)",settings.detailedNotif)
	}    
    
    
	traceEvent(settings.logFilter,"programs: $ecobeePrograms",settings.detailedNotif)

	def zones = []
    
	for (int i = 1; ((i <= settings.zonesCount) && (i<= get_MAX_ZONES())); i++) {
		def key = "zoneName$i"
		def zoneName =  "${i}:${settings[key]}"   
		zones = zones + zoneName
	}

	
	def enumModes=location.modes.collect{ it.name }
	def indiceSchedule=1
	// Assign params to indiceSchedule.  Sometimes parameters are double nested.
	if (params?.indiceSchedule) {
		indiceSchedule = params.indiceSchedule
		state?.params=params        
	} else if (state?.params?.indiceSchedule) {    
		indiceSchedule = state?.params.indiceSchedule
	}    
	indiceSchedule=indiceSchedule.intValue()
	dynamicPage(name: "schedulesSetup", title: "Schedule Setup",uninstall: false) {
		section("Schedule ${indiceSchedule} Setup") {
			input (name:"scheduleName${indiceSchedule}", title: "Schedule Name", type: "text",
				defaultValue:settings."scheduleName${indiceSchedule}", image: "${getCustomImagePath()}office7.png" )
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
			    options: [
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
				])
			input (name:"begintime${indiceSchedule}", type: "time", title: "Beginning time to trigger the zoned heating/cooling settings",
				defaultValue:settings."begintime${indiceSchedule}")
			input (name:"endtime${indiceSchedule}", type: "time", title: "End time",
				defaultValue:settings."endtime${indiceSchedule}")
		}
		section("Schedule ${indiceSchedule}-Select the program/climate at ecobee thermostat to be applied [optional,for ecobee only]") {
			input (name:"givenClimate${indiceSchedule}", type:"enum", title: "Which ecobee program? ", options: ecobeePrograms, 
				required: false, defaultValue:settings."givenClimate${indiceSchedule}", description: "Optional")
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Cooling setpoint during the schedule [when no ecobee climate is specified]") {
			input (name:"desiredCool${indiceSchedule}", type:"decimal", title: "Cooling Setpoint, default = 75F/23C", 
				required: false,defaultValue:settings."desiredCool${indiceSchedule}", description: "Not Optional for non ecobee tstats")			                
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Heating setpoint during the schedule [when no ecobee climate is specified]") {
			input (name:"desiredHeat${indiceSchedule}", type:"decimal", title: "Heating Setpoint, default=72F/21C", 
				required: false, defaultValue:settings."desiredHeat${indiceSchedule}", description: "Not Optional for non ecobee tstats")			                
		}
		section("Schedule ${indiceSchedule}-Vent Settings for the Schedule") {
			href(name: "toVentSettingsSetup", page: "ventSettingsSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: ventSettingsHrefTitle(indiceSchedule), image: "${getCustomImagePath()}ventopen.png" ) 
		}
		section("Schedule ${indiceSchedule}-Run only during specific mode(s) [default=all]")  {
			input (name:"selectedMode${indiceSchedule}", type:"enum", title: "Choose Mode", options: enumModes, 
				required: false, multiple:true,defaultValue:settings."selectedMode${indiceSchedule}", description: "Optional")
		}
		section("Do not set the thermostat setpoints in this schedule [optional, default=The thermostat setpoints are set]") {
			input (name:"noSetpointsFlag${indiceSchedule}", title: "Do not set the thermostat setpoints?", type:"bool", 
				required:false, defaultValue:settings."noSetpointsFlag${indiceSchedule}")
		}
		section("Schedule ${indiceSchedule}-Make the schedule inactive?") {
			input "inactiveScheduleFlag${indiceSchedule}", title: "Inactive?", "bool", 
				required: false, description: "false"
		}
		section {
			href(name: "toSchedulesSetupPage", title: "Back to Schedules Setup Page", page: "schedulesSetupPage")
		}
	}        
}

def ventSettingsSetup(params) {
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
    
	dynamicPage(name: "ventSettingsSetup", title: "Vent Settings for schedule " + settings."scheduleName${indiceSchedule}", uninstall: false, 
		nextPage: "schedulesSetupPage") {
		section("Schedule ${indiceSchedule}-Vent Settings for the Schedule [optional]") {
			input (name: "setVentLevel${indiceSchedule}", type:"number",  title: "Set all Vents in Zone(s) to a specific Level during the Schedule [range 0-100]", 
				required: false, defaultValue:settings."setVentLevel${indiceSchedule}", range: "0..100", description: "blank: calculated by smartapp")
			input (name: "resetLevelOverrideFlag${indiceSchedule}", type:"bool",  title: "Bypass all vents overrides in zone(s) during the Schedule (default=false)?", 
				required: false, defaultValue:settings."resetLevelOverrideFlag${indiceSchedule}", description: "Optional")
			input (name: "adjustVentsEveryCycleFlag${indiceSchedule}", type:"bool",  title: "Adjust vent settings every 5 minutes (default=only when heating/cooling/fan running)?", 
				required: false, defaultValue:settings."adjustVentsEveryCycleFlag${indiceSchedule}", description: "Optional")
			input (name: "openVentsFanOnlyFlag${indiceSchedule}", type:"bool", title: "Open all vents when HVAC's OperatingState is Fan only",
				required: false, defaultValue:settings."openVentsFanOnlyFlag${indiceSchedule}", description: "Optional")
		}
		section {
			href(name: "toSchedulePage${indiceSchedule}", title: "Back to Schedule #${indiceSchedule} - Setup Page", page: "schedulesSetup", params: [indiceSchedule: indiceSchedule])
		}
	}    
}   

def ventSettingsHrefTitle(i) {
	def title = "Vent Settings for Schedule ${i}"
	return title
}


def NotificationsPage() {
	dynamicPage(name: "NotificationsPage", title: "Other Options", install: true) {
		if (isST()) {        
			section("Notifications") {
				input "sendPushMessage", "enum", title: "Send a push notification?", options:["Yes", "No"], required:
		    		false
				input "phoneNumber", "phone", title: "Send a text message?", required: false
            }				          
			section("Enable Amazon Echo/Ask Alexa Notifications for events logging (optional)") {
	    		input (name:"askAlexaFlag", title: "Ask Alexa verbal Notifications [default=false]?", type:"bool",
		    		description:"optional",required:false)
    			input (name:"listOfMQs",  type:"enum", title: "List of the Ask Alexa Message Queues (default=Primary)", options: state?.askAlexaMQ, multiple: true, required: false,
	    			description:"optional")            
    			input "AskAlexaExpiresInDays", "number", title: "Ask Alexa's messages expiration in days (optional,default=2 days)?", required: false
			}
		}
		section("Logging") {
			input "detailedNotif", "bool", title: "Detailed Logging?", required:false
			input "logFilter", "enum", title: "log filtering [Level 1=ERROR only,2=<Level 1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>]?",required:false, options:[1,2,3,4,5]
				          
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
	state?.closedVentsCount= 0
	state?.openVentsCount=0
	state?.totalVents=0
	state?.ratioClosedVents=0
	state?.avgTempDiff=0.0
	state?.activeZones=[]
	initialize()
}
def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("detailedNotif",[value:"false",type:"bool"])
} 
boolean isST() { 
    return (getHub() == "SmartThings") 
}

private getHub() {
    def result = "SmartThings"
    if(state?.hub == null) {
        try { [value: "value"]?.encodeAsJson(); } catch (e) { result = "Hubitat" }
        state?.hub = result
    }
//  log.debug "hubPlatform: (${state?.hub})"
    return state?.hub
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
    if (!isST()) {    
        if (detailedNotif) runIn(900,"logsOff")    
    }        
}

def offHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value",settings.detailedNotif)
}

def onHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value",settings.detailedNotif)
	setZoneSettings()    
	rescheduleIfNeeded(evt)   // Call rescheduleIfNeeded to work around ST scheduling issues
}



def ventTemperatureHandler(evt) {
	traceEvent(settings.logFilter,"vent temperature: $evt.value",settings.detailedNotif)
	float ventTemp = evt.value.toFloat()
	def scale = (state?.scale) ?: getTemperatureScale()
	def MAX_TEMP_VENT_SWITCH = (maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch
	String currentHVACMode = thermostat?.currentThermostatMode
	currentHVACMode=(currentHVACMode)?:'auto'	// set auto by default

	if ((currentHVACMode in ['heat','auto','emergency heat', 'eco']) && (ventTemp >= MAX_TEMP_VENT_SWITCH)) {
		if (fullyCloseVentsFlag) {
			// Safeguards are not implemented as requested     
			traceEvent(settings.logFilter, "ventTemperatureHandler>vent temperature is not within range ($evt.value>$MAX_TEMP_VENT_SWITCH) ,but safeguards are not implemented as requested",
				true,GLOBAL_LOG_WARN,true)        
			return    
		}    
    
		// Open all vents just to be safe
		open_all_vents()
		traceEvent(settings.logFilter,"current HVAC mode is ${currentHVACMode}, found one of the vents' value too hot (${evt.value}), opening all vents to avoid any damage", 
			true,GLOBAL_LOG_ERROR,true)        
        
	} /* if too hot */           
	if ((currentHVACMode in ['cool','auto', 'eco']) && (ventTemp <= MIN_TEMP_VENT_SWITCH)) {
		if (fullyCloseVentsFlag) {
			// Safeguards are not implemented as requested     
			traceEvent(settings.logFilter, "ventTemperatureHandler>vent temperature is not within range, ($evt.value<$MIN_TEMP_VENT_SWITCH) but safeguards are not implemented as requested",
				true,GLOBAL_LOG_WARN,true)        
			return    
		}    
		// Open all vents just to be safe
		open_all_vents()
		traceEvent(settings.logFilter,"current HVAC mode is ${currentHVACMode}, found one of the vents' value too cold (${evt.value}), opening all vents to avoid any damage",
			true,GLOBAL_LOG_ERROR,true)        
	} /* if too cold */ 
}


def thermostatOperatingHandler(evt) {
	traceEvent(settings.logFilter,"Thermostat Operating now: $evt.value",settings.detailedNotif)
	state?.operatingState=evt.value    
	def setVentSettings = (setVentSettingsFlag) ?: false
	if ((setVentSettings) && (settings.delayInSecForVentSettings)) {  // if delay has been asked, then call runIn  
			traceEvent(settings.logFilter,"thermostatOperatingHandler>calling setZoneSettings with delay of $delayInSecForVentSettings seconds",
				settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
			runIn(delayInSecForVentSettings, "setZoneSettings")
	} else {
    
		traceEvent(settings.logFilter,"thermostatOperatingHandler> calling setZoneSettings",
			settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
		setZoneSettings()        
	}
	rescheduleIfNeeded(evt)   // Call rescheduleIfNeeded to work around ST scheduling issues
}

def heatingSetpointHandler(evt) {
	traceEvent(settings.logFilter,"heating Setpoint now: $evt.value",settings.detailedNotif)
	checkOverrideHandler(evt)    
}
def coolingSetpointHandler(evt) {
	traceEvent(settings.logFilter,"cooling Setpoint now: $evt.value",settings.detailedNotif)
	checkOverrideHandler(evt)    
}
def changeModeHandler(evt) {
	traceEvent(settings.logFilter,"Changed mode, $evt.name: $evt.value",settings.detailedNotif)
	rescheduleIfNeeded(evt)   // Call rescheduleIfNeeded to work around ST scheduling issues
	state?.lastScheduleName=null    
	setZoneSettings()    
}



def ventEvtRoomHandler1(evt) {
	int i=1
	ventRoomHandler(evt,i)
    
}


def ventEvtRoomHandler2(evt) {
	int i=2
	ventRoomHandler(evt,i)
    
}


def ventEvtRoomHandler3(evt) {
	int i=3
	ventRoomHandler(evt,i)
    
}

def ventEvtRoomHandler4(evt) {
	int i=4
	ventRoomHandler(evt,i)
    
}

def ventEvtRoomHandler5(evt) {
	int i=5
	ventRoomHandler(evt)
    
}


def ventEvtRoomHandler6(evt) {
	int i=6
	ventRoomHandler(evt)
    
}

def ventEvtRoomHandler7(evt) {
	int i=7
	ventRoomHandler(evt)
    
}


def ventEvtRoomHandler8(evt) {
	int i=8
	ventRoomHandler(evt)
    
}

def ventEvtRoomHandler9(evt) {
	int i=9
	ventRoomHandler(evt)
    
}

def ventEvtRoomHandler10(evt) {
	int i=10
	ventRoomHandler(evt)
    
}


def ventEvtRoomHandler11(evt) {
	int i=11
	ventRoomHandler(evt)
    
}


def ventEvtRoomHandler12(evt) {
	int i=12
	ventRoomHandler(evt)
    
}


def ventEvtRoomHandler13(evt) {
	int i=13
	ventRoomHandler(evt)
    
}

def ventEvtRoomHandler14(evt) {
	int i=14
	ventRoomHandler(evt)
    
}

def ventEvtRoomHandler15(evt) {
	int i=15
	ventRoomHandler(evt)
    
}


def ventEvtRoomHandler16(evt) {
	int i=16
	ventRoomHandler(evt)
    
}


private def ventRoomHandler(evt, indiceRoom=0) {
	def key= "roomName${indiceRoom}"    
	def roomName= settings[key]
	traceEvent(settings.logFilter,"ventRoomHandler>in room $roomName, $evt.name: $evt.value",settings.detailedNotif)
	for (int indiceZone = 1;
		((indiceZone <= settings.zonesCount) && (indiceZone <= get_MAX_ZONES())); indiceZone++) { 		// look up for the room in the includedRooms for each zone
		key = "zoneName$indiceZone"
		def zoneName = settings[key]
		if (!zoneName || zoneName=='null') {
			continue
		}
		key = "inactiveZoneFlag$indiceZone"
		boolean inactiveZone=(state?."inactiveZone$indiceZone"!= null)? state?."inactiveZone$indiceZone".toBoolean() :settings[key]
		if (inactiveZone) {
			traceEvent(settings.logFilter,"ventRoomHandler>zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
			continue
		}
		traceEvent(settings.logFilter,"ventRoomHandler>zone=${zoneName},about to scan zone for room",settings.detailedNotif)
		key = "virtualZoneFlag${indiceZone}"
		def virtualCreateZoneFlag = (settings[key])?:false

		if (!virtualCreateZoneFlag) {   // if no virtual zone is created, just continue
			traceEvent(settings.logFilter,"ventRoomHandler>zone $zoneName doesn't have a virtual zone associated, just continue",
					settings.detailedNotif, GLOBAL_LOG_INFO)
			continue        
		}
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		if (rooms.toString().contains(roomName.toString().trim())) {
			traceEvent(settings.logFilter,"ventRoomHandler>found $roomName in zone $zoneName",settings.detailedNotif)
			def dni = "My Virtual Zone.${zoneName}.$indiceZone"  
			def d = getChildDevice(dni)
			if (d) {  
				def ventState=d.currentValue("allVentsState")      
				def isChange = d.isStateChange(d, "allVentsState", evt.value)	//check of state change
				traceEvent(settings.logFilter,"ventRoomHandler>found $roomName in zone $zoneName, about to set corresponding virtual zone ($dni) to $evt.value if isChange ($isChange) is true (current Vent State=$ventState)",settings.detailedNotif)
				if (isChange) {                
					d.sendEvent(name:"allVentsState", value: evt.value, isDisplayed: true, isStateChange: isChange)       
				}                    
			} else {
				traceEvent(settings.logFilter,"ventRoomHandler>didnt't find virtual $dni, not able to update virtual zone with ${evt.value}",
					settings.detailedNotif, GLOBAL_LOG_WARN)
			} 
		} else {
			traceEvent(settings.logFilter,"ventRoomHandler>$roomName not in zone $zoneName",settings.detailedNotif)
		}        
	} /* end for each zone */
}

def contactEvtHandler1(evt) {
	int i=1
	contactEvtHandler(evt,i)    
}

def contactEvtHandler2(evt) {
	int i=2
	contactEvtHandler(evt,i)    
}

def contactEvtHandler3(evt) {
	int i=3
	contactEvtHandler(evt,i)    
}

def contactEvtHandler4(evt) {
	int i=4
	contactEvtHandler(evt,i)    
}

def contactEvtHandler5(evt) {
	int i=5
	contactEvtHandler(evt,i)    
}

def contactEvtHandler6(evt) {
	int i=6
	contactEvtHandler(evt,i)    
}

def contactEvtHandler7(evt) {
	int i=7
	contactEvtHandler(evt,i)    
}

def contactEvtHandler8(evt) {
	int i=8
	contactEvtHandler(evt,i)    
}

def contactEvtHandler9(evt) {
	int i=9
	contactEvtHandler(evt,i)    
}

def contactEvtHandler10(evt) {
	int i=10
	contactEvtHandler(evt,i)    
}

def contactEvtHandler11(evt) {
	int i=11
	contactEvtHandler(evt,i)    
}

def contactEvtHandler12(evt) {
	int i=12
	contactEvtHandler(evt,i)    
}

def contactEvtHandler13(evt) {
	int i=13
	contactEvtHandler(evt,i)    
}

def contactEvtHandler14(evt) {
	int i=14
	contactEvtHandler(evt,i)    
}

def contactEvtHandler15(evt) {
	int i=15
	contactEvtHandler(evt,i)    
}

def contactEvtHandler16(evt) {
	int i=16
	contactEvtHandler(evt,i)    
}


private def contactEvtHandler(evt, indiceRoom=0) {
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	traceEvent(settings.logFilter,"contactEvtHandler>$evt.name: $evt.value",settings.detailedNotif)
	def adjustmentBasedOnContact=(settings.setVentAdjustmentContactFlag)?: false
	def switchLevel =null 
	def key= "roomName${indiceRoom}"    
	def roomName= settings[key]
    
	if (adjustmentBasedOnContact) { 
		key = "contactSensor$indiceRoom"
		def contactSensor = settings[key]
		traceEvent(settings.logFilter,"contactEvtHandler>contactSensor=${contactSensor}",settings.detailedNotif)
		if (contactSensor !=null) {
			key = "contactClosedLogicFlag${indiceRoom}"            
			boolean closedContactLogicFlag= (settings[key])?:false            
			boolean isContactOpen = any_contact_open(contactSensor)            
			if ((!closedContactLogicFlag) && isContactOpen ) {
				switchLevel=((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE)					                
				traceEvent(settings.logFilter,"contactEvtHandler>a contact in ${contactSensor} is open, the vent(s) in ${roomName} set to mininum level=${switchLevel}",
					settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)                        
			} else if (closedContactLogicFlag && (!isContactOpen)) {
				switchLevel=((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE)					                
				traceEvent(settings.logFilter,"contactEvtHandler>contact(s) in ${contactSensor} closed, the vent(s) in ${roomName} set to mininum level=${switchLevel}",
					settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)                        
			} else {
				switchLevel=100            	
			} 
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)
				}                    
			} /* end for ventSwitch */                
		}            
	}            

}

private def motionEvtHandler(evt, indice) {
	traceEvent(settings.logFilter,"motionEvtHandler>$evt.name: $evt.value",settings.detailedNotif)
	def key= "roomName${indice}"    
	if (evt.value == "active") {
		def roomName= settings[key]
		key = "occupiedMotionTimestamp${indice}"       
		state[key]= now()        
		traceEvent(settings.logFilter,"Motion at home in ${roomName},occupiedMotionTimestamp=${state[key]}",settings.detailedNotif, 
			GLOBAL_LOG_INFO,settings.detailedNotif)
   
	}
	for (int indiceZone = 1;
		((indiceZone <= settings.zonesCount) && (indiceZone <= get_MAX_ZONES())); indiceZone++) { 		// look up for the room in the includedRooms for each zone
		key = "zoneName$indiceZone"
		def zoneName = settings[key]
		if (!zoneName || zoneName=='null') {
			continue
		}
		key = "inactiveZoneFlag$indiceZone"
		boolean inactiveZone=(state?."inactiveZone$indiceZone"!= null)? state?."inactiveZone$indiceZone".toBoolean() :settings[key]
		if (inactiveZone) {
			traceEvent(settings.logFilter,"motionEvtHandler>zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
			continue
		}
		traceEvent(settings.logFilter,"motionEvtHandler>zone=${zoneName},about to scan zone for room",settings.detailedNotif)
		key = "virtualZoneFlag${indiceZone}"
		def virtualCreateZoneFlag = (settings[key])?:false

		if (!virtualCreateZoneFlag) {   // if no virtual zone is created, just continue
			traceEvent(settings.logFilter,"motionEvtHandler>zone $zoneName doesn't have a virtual zone associated, just continue",
				settings.detailedNotif, GLOBAL_LOG_INFO)
			continue        
		}
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		if (rooms.toString().contains(roomName.toString().trim())) {
			traceEvent(settings.logFilter,"motionEvtHandler>found $roomName in zone $zoneName",settings.detailedNotif)
			def dni = "My Virtual Zone.${zoneName}.$indiceZone"  
			def d = getChildDevice(dni)
			if (d) {  
				d.sendEvent(name:"motion", value: evt.value , isDisplayed:true, isStateChange:true)
			} else {
				traceEvent(settings.logFilter,"motionEvtHandler>didnt't find virtual $dni, not able to update contact with ${evt.value}",
					settings.detailedNotif, GLOBAL_LOG_WARN)
			} 
		} else {
			traceEvent(settings.logFilter,"motionEvtHandler>$roomName not in zone $zoneName",settings.detailedNotif)
		}   
	}        
  
}    


def motionEvtHandler1(evt) {
	int i=1
	motionEvtHandler(evt,i)    
}

def motionEvtHandler2(evt) {
	int i=2
	motionEvtHandler(evt,i)    
}

def motionEvtHandler3(evt) {
	int i=3
	motionEvtHandler(evt,i)    
}

def motionEvtHandler4(evt) {
	int i=4
	motionEvtHandler(evt,i)    
}

def motionEvtHandler5(evt) {
	int i=5
	motionEvtHandler(evt,i)    
}

def motionEvtHandler6(evt) {
	int i=6
	motionEvtHandler(evt,i)    
}

def motionEvtHandler7(evt) {
	int i=7
	motionEvtHandler(evt,i)    
}

def motionEvtHandler8(evt) {
	int i=8
	motionEvtHandler(evt,i)    
}

def motionEvtHandler9(evt) {
	int i=9
	motionEvtHandler(evt,i)    
}

def motionEvtHandler10(evt) {
	int i=10
	motionEvtHandler(evt,i)    
}

def motionEvtHandler11(evt) {
	int i=11
	motionEvtHandler(evt,i)    
}

def motionEvtHandler12(evt) {
	int i=12
	motionEvtHandler(evt,i)    
}

def motionEvtHandler13(evt) {
	int i=13
	motionEvtHandler(evt,i)    
}

def motionEvtHandler14(evt) {
	int i=14
	motionEvtHandler(evt,i)    
}

def motionEvtHandler15(evt) {
	int i=15
	motionEvtHandler(evt,i)    
}

def motionEvtHandler16(evt) {
	int i=16
	motionEvtHandler(evt,i)    
}


def checkOverrideHandler(evt) {
	traceEvent(settings.logFilter,"checkOverrideHandler>evt=${evt.value}",settings.detailedNotif)
	def heatSP = thermostat.currentHeatingSetpoint
	def coolSP = thermostat.currentCoolingSetpoint
    
	String currentMode= thermostat.currentThermostatMode    
	if ((evt.value.contains('hold')) || (evt.isPhysical())) { // if a temporary hold is set, then change the baselines
		if ((currentMode in ['cool', 'auto', 'off', 'eco']) && (state?.scheduleCoolSetpoint != coolSP)) {
			save_new_cool_baseline_value(coolSP)		        
			traceEvent(settings.logFilter,"checkOverrideHandler>new cooling baseline=$coolSP, manual hold set",settings.detailedNotif, GLOBAL_LOG_INFO)
		}    
		if ((currentMode in ['heat', 'auto', 'off', 'eco']) && (state?.scheduleHeatSetpoint != heatSP)) {
			save_new_heat_baseline_value(heatSP)		        
			traceEvent(settings.logFilter,"checkOverrideHandler>new heating baseline=$heatSP, manual hold set",settings.detailedNotif, GLOBAL_LOG_INFO)
		}   
	}
	if (evt.value.contains('_auto')) { // if climateRef auto is set, then change the baselines
		if ((currentMode in ['cool', 'auto', 'off', 'eco']) && (state?.scheduleCoolSetpoint != coolSP)) {
			save_new_cool_baseline_value(coolSP)		        
			traceEvent(settings.logFilter,"checkOverrideHandler>new cooling baseline=$coolSP, climateRef= ${evt.value} set",settings.detailedNotif, GLOBAL_LOG_INFO)
			        
		}    
		if ((currentMode in ['heat', 'auto', 'off', 'eco']) && (state?.scheduleHeatSetpoint != heatSP)) {
			save_new_heat_baseline_value(heatSP)		        
			traceEvent(settings.logFilter,"checkOverrideHandler>new heating baseline=$heatSP, climateRef= ${evt.value} set",settings.detailedNotif, GLOBAL_LOG_INFO)
		}   
	}    
}    

private void save_new_cool_baseline_value(coolSP) {
	state?.scheduleCoolSetpoint= coolSP
	traceEvent(settings.logFilter,"save_new_cool_baseline_value>new cooling baseline=$coolSP",settings.detailedNotif, GLOBAL_LOG_INFO)
}

private void save_new_heat_baseline_value(heatSP) {
	state?.scheduleHeatSetpoint= heatSP
	traceEvent(settings.logFilter,"save_new_heat_baseline_value>new heating baseline=$heatSP",settings.detailedNotif, GLOBAL_LOG_INFO)
}


def subscribe_all_events() {

	if (powerSwitch) {
		subscribe(powerSwitch, "switch.off", offHandler, [filterEvents: false])
		subscribe(powerSwitch, "switch.on", onHandler, [filterEvents: false])
	}
	subscribe(location, "mode", changeModeHandler)
	if (thermostat) {  
		subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)    
		subscribe(thermostat, "coolingSetpoint", coolingSetpointHandler)
		subscribe(thermostat, "thermostatOperatingState", thermostatOperatingHandler)
    
    
		if (thermostat.hasCommand("resumeThisTstat")) {
			subscribe(thermostat, "programScheduleName", checkOverrideHandler)
		}        
	}        
    
	subscribe(app, appTouch)

	// subscribe all vents to check their temperature on a regular basis
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
			if (vent) {
				subscribe(vent, "temperature", ventTemperatureHandler)
				subscribe(vent, "switch.off", "ventEvtRoomHandler${indiceRoom}", [filterEvents: false]) 
				subscribe(vent, "switch.on", "ventEvtRoomHandler${indiceRoom}", [filterEvents: false])
			} /* end if vent != null */
		} /* end for vent switches */
	} /* end for rooms */

	// subscribe all motion sensors to check for active motion in rooms
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		def key    
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
			if (vent != null) {
				subscribe(vent, "temperature", ventTemperatureHandler)
			} /* end if vent != null */
		} /* end for vent switches */
		key = "occupiedMotionCounter${indiceRoom}"       
		state[key]=0	 // initalize the motion counter to zero		                

		key = "motionSensor${indiceRoom}"
		def motionSensor = settings[key]
        
		if (motionSensor) {
			// associate the motionHandler to the list of motionSensors in rooms   	 
			subscribe(motionSensor, "motion", "motionEvtHandler${indiceRoom}", [filterEvents: false])
		}            
		key ="contactSensor${indiceRoom}"
		def contactSensor = settings[key]
       
		if (contactSensor) {
			// associate the contactHandler to the list of contactSensors in rooms   	 
			subscribe(contactSensor, "contact.closed", "contactEvtHandler${indiceRoom}", [filterEvents: false])
			subscribe(contactSensor, "contact.open", "contactEvtHandler${indiceRoom}", [filterEvents: false])
		}            

	} /* end for rooms */
	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "sunrise", rescheduleIfNeeded)
	subscribe(location, "sunset", rescheduleIfNeeded)
	subscribe(location, "sunriseTime", rescheduleIfNeeded)
	subscribe(location, "sunsetTime", rescheduleIfNeeded)

	subscribe(location, "askAlexaMQ", askAlexaMQHandler)
	runIn(30,"create_zone_devices")     
	rescheduleIfNeeded()   
}

def initialize() {

	// Initialize state variables
	state?.lastScheduleName=""
	state.lastStartTime=null 
	state.scheduleHeatSetpoint=0  
	state.scheduleCoolSetpoint=0    
	state.operatingState=""
	state?.scale=getTemperatureScale()    
    
	state?.poll = [ last: 0, rescheduled: now() ]

	Integer delay =5 				// wake up every 5 minutes to apply zone settings if any
	traceEvent(settings.logFilter,"initialize>scheduling setZoneSettings every ${delay} minutes to check for zone settings to be applied",settings.detailedNotif,
		GLOBAL_LOG_INFO)

	state?.scale=getTemperatureScale()    
	subscribe_all_events()
}

private void create_zone_devices() {

	def zoneDevices = getChildDevices()
	traceEvent(settings.logFilter,"create_zone_devices>found $zoneDevices, about to save zone settings in state variables & create virtual zones", detailedNotif)
	int countNewChildDevices=0, countDeletedDevices=0                
	
	for (int i = 1;
		((i <= settings.zonesCount) && (i <= get_MAX_ZONES())); i++) {
		def key  = "desiredHeatDeltaTemp$i"
		state?."desiredHeatTempDelta$i"=settings[key]           
		key  = "desiredCoolTempDelta$i"
		state?."desiredCoolTempDelta$i"=settings[key]           
		key = "zoneName${i}"
		def zoneName = settings[key]
		key = "virtualZoneFlag${i}"
		def virtualCreateZoneFlag = (settings[key])?:false
		def dni = "My Virtual Zone.${zoneName}.$i"
		def d=zoneDevices.find {
				((it.device.deviceNetworkId.contains(dni)))        
		}
		traceEvent(settings.logFilter,"create_zone_devices>for zoneName $zoneName, virtualZoneFlag=$virtualCreateZoneFlag, found d=${d?.name}", detailedNotif)
		if (virtualCreateZoneFlag && (!d)) {
			def labelName="My Zone $zoneName"        
			traceEvent(settings.logFilter,"create_zone_devices>about to create child device with id $dni,labelName=  ${labelName}", detailedNotif)
			def newZone            
			if (isST()) {                            
 				newZone = addChildDevice(	getSTChildNamespace(), getVirtualZoneChildName(), dni, null, [label: "${labelName}"])
			} else {
				newZone = addChildDevice(	getChildNamespace(), getVirtualZoneChildName(), dni,   [label: "${labelName}"])
			}                    
			traceEvent(settings.logFilter,"create_zone_devices>created ${newZone.displayName} with id $dni", detailedNotif)
			countNewChildDevices++     
			subscribe(d,"tempDelta", tempDeltaHandler)
			subscribe(d,"activeZone", activeZoneHandler)
		} else if (virtualCreateZoneFlag && d) {
			traceEvent(settings.logFilter,"create_zone_devices>found ${d.displayName} with id $dni already exists", detailedNotif)
			subscribe(d,"tempDelta", tempDeltaHandler)
			subscribe(d,"activeZone", activeZoneHandler)
		}

		if (!virtualCreateZoneFlag && d) {
			def delete = zoneDevices.findAll {
				((it.device.deviceNetworkId.contains(getVirtualZoneChildName())) && ((it.device.deviceNetworkId.contains("${zoneName}."))))		
			}
			traceEvent(settings.logFilter,"create_zone_devices>found $delete devices to be deleted", detailedNotif)

                
			delete.each {
				try {    
					deleteChildDevice(it.deviceNetworkId)
					state?."inactiveZone${i}"=null                
					state?."desiredCoolTempDelta${i}"=null                
					state?."desiredHeatTempDelta${i}"=null           
					countDeletedDevices++                    
				} catch(e) {
					traceEvent(settings.logFilter,"create_zone_devices>exception $e while trying to delete Virtual Zone ${it.name}", detailedNotif, GLOBAL_LOG_ERROR)
				}        
			}
  			      
		}
	} /* end for */
	traceEvent(settings.logFilter,"create_zone_devices>created $countNewChildDevices zones, deleted $countDeletedDevices zones", detailedNotif)
    
}    
def askAlexaMQHandler(evt) {
	if (!evt) return
	switch (evt.value) {
		case "refresh":
		state?.askAlexaMQ = evt.jsonData && evt.jsonData?.queues ? evt.jsonData.queues : []
		traceEvent(settings.logFilter,"askAlexaMQHandler>new refresh value=$evt.jsonData?.queues", detailedNotif, GLOBAL_LOG_INFO)
		break
	}
}

def activeZoneHandler(evt) {
	def d = evt.device	
	def virtual_info = d.deviceNetworkId.tokenize('.')
	def indiceZone=virtual_info.last()
	def key="zoneName$indiceZone"
	def zoneName=settings[key]    
	traceEvent(settings.logFilter,"activeZoneHandler>for $d device, receive new active value ${evt.value} in zone $zoneName",settings.detailedNotif)
	boolean newValue=(evt.value=="true")? false :true
	state?."inactiveZone$indiceZone"=newValue // set new value
	def settingKey= "inactiveZoneFlag$indiceZone"   
	if (isST()) {    
		app.updateSetting(settingKey, newValue)        
	} else {
		app.updateSetting(settingKey, [value: newValue, type:"bool"])        
	}  
	traceEvent(settings.logFilter,"activeZoneHandler>for $d device, inactiveZoneFlag=${newValue} in zone $zoneName",settings.detailedNotif)
}

def tempDeltaHandler(evt) {
	def d = evt.device	
	String mode='auto' // By default, set to auto
    
	if (thermostat) mode=thermostat?.currentThermostatMode   
    
	def virtual_info = d.deviceNetworkId.tokenize('.')
	def indiceZone=virtual_info.last()
	def key="zoneName$indiceZone"
	def zoneName=settings[key]    
	traceEvent(settings.logFilter,"tempDeltaHandler>for $d device, receive new temp delta ${evt.value} in zone $zoneName",settings.detailedNotif)
	def tempDelta=evt.value    
	if (mode in ['heat', 'off','auto', 'eco'] && tempDelta!=null) {
		traceEvent(settings.logFilter,"tempDeltaHandler>About to refresh desiredHeatTempDelta at $indiceZone with $tempDelta",settings.detailedNotif)
		save_new_heat_delta_value(tempDelta, indiceZone)    
	}
	if (mode in ['cool', 'off','auto', 'eco'] && tempDelta!=null)  {
		traceEvent(settings.logFilter,"tempDeltaHandler>About to refresh desiredCoolTempDelta at $indiceZone with $tempDelta",settings.detailedNotif)
		save_new_cool_delta_value(tempDelta, indiceZone)    
	}
    
}


private void save_new_heat_delta_value(newDeltaValue, indiceZone) {
	def scale = (state?.scale)?: getTemperatureScale()	
	float MAX_DELTA=(scale=='C')?1:2
	def originalDelta = (state?."desiredHeatTempDelta${indiceZone}") ?:0
	float offsetDelta=originalDelta?.toFloat() - newDeltaValue?.toFloat()
	if (offsetDelta.abs() > MAX_DELTA) {
		traceEvent(settings.logFilter,"save_new_heat_delta_value>hasn't saved desiredHeatTempDelta at $indiceZone with $newDeltaValue as offset too big vs. original delta $originalDelta",settings.detailedNotif, GLOBAL_LOG_WARN)
		return            
	}        
	def key ="desiredHeatTemp$indiceZone"
	def staticHeatTemp=settings[key]	
	if (staticHeatTemp) {
		float new_static_value=staticHeatTemp.toFloat() + newDeltaValue?.toFloat()
    	def settingKey= "desiredHeatDeltaTemp${indiceZone}"   
		if (isST()) {    
			app.updateSetting(key, new_static_value)        
		} else {
			app.updateSetting(key, [value: new_static_value, type:"int"])        
		}  
		traceEvent(settings.logFilter,"save_new_heat_delta_value>Saved $newDeltaValue as calculated static heat temp (before= $staticHeatTemp, new=$new_static_value)",settings.detailedNotif)
		runIn(10,"setZoneSettings", [overwrite:true]) 
		return        
	}    
	state?."desiredHeatTempDelta${indiceZone}"=newDeltaValue // save the new temp delta at virtual zone and store it in state variable
	def settingKey= "desiredHeatDeltaTemp${indiceZone}"   
	if (isST()) {    
		app.updateSetting(settingKey,  newDeltaValue)        
	} else {
		app.updateSetting(settingKey, [value: newDeltaValue, type:"int"])        
	}  
//	settings."desiredHeatDeltaTemp${indiceZone}"=newDeltaValue         
	traceEvent(settings.logFilter,"save_new_heat_delta_value>saved desiredHeatTempDelta at $indiceZone with $newDeltaValue",settings.detailedNotif, GLOBAL_LOG_INFO)
	runIn(10,"setZoneSettings",[overwrite:true])                 
}

private void save_new_cool_delta_value(newDeltaValue, indiceZone) {
	def scale = (state?.scale)?: getTemperatureScale()	
	float MAX_DELTA=(scale=='C')?2:4
	def originalDelta = (state?."desiredCoolTempDelta${indiceZone}") ?:0
	float offsetDelta=originalDelta?.toFloat() - newDeltaValue?.toFloat()
	if (offsetDelta.abs() > MAX_DELTA) {
		traceEvent(settings.logFilter,"save_new_cool_delta_value>hasn't saved desiredCoolTempDelta at $indiceZone with $newDeltaValue as offset too big vs. original delta $originalDelta",settings.detailedNotif, GLOBAL_LOG_WARN)
		return            
	}        
	def key ="desiredCoolTemp$indiceZone"
	def staticCoolTemp=settings[key]	
	if (staticCoolTemp) {
		float new_static_value=staticCoolTemp.toFloat() + newDeltaValue?.toFloat()
		if (isST()) {    
			app.updateSetting(key, new_static_value)        
		} else {
			app.updateSetting(key, [value: new_static_value, type:"int"])        
		}  
		traceEvent(settings.logFilter,"save_new_heat_delta_value>Saved $newDeltaValue as calculated static cool temp (before= $staticCoolTemp, new=$new_static_value)",settings.detailedNotif)
		runIn(10,"setZoneSettings", [overwrite:true]) 
		return        
	}    
	state?."desiredCoolTempDelta${indiceZone}"=newDeltaValue // save the new temp delta at virtual zone and and store it in state variable
//	settings."desiredCoolDeltaTemp${indiceZone}"=newDeltaValue         
	def settingKey= "desiredCoolDeltaTemp${indiceZone}"   
	if (isST()) {    
		app.updateSetting(settingKey,  newDeltaValue)        
	} else {
		app.updateSetting(settingKey, [value: newDeltaValue, type:"int"])        
	}  
	traceEvent(settings.logFilter,"save_new_cool_delta_value>saved desiredCoolTempDelta at $indiceZone with $newDeltaValue",settings.detailedNotif, GLOBAL_LOG_INFO)
	runIn(10,"setZoneSettings", [overwrite:true])      
}

def rescheduleIfNeeded(evt) {
	if (evt) traceEvent(settings.logFilter,"rescheduleIfNeeded>$evt.name=$evt.value",settings.detailedNotif)
	Integer delay = 5 // By default, schedule SetZoneSettings() every 5 min.
	BigDecimal currentTime = now()    
	BigDecimal lastPollTime = (currentTime - (state?.poll["last"]?:0))  
	if (lastPollTime != currentTime) {    
		Double lastPollTimeInMinutes = (lastPollTime/60000).toDouble().round(1)      
		traceEvent(settings.logFilter, "rescheduleIfNeeded>last poll was  ${lastPollTimeInMinutes.toString()} minutes ago",settings.detailedNotif)
	}
	if (((state?.poll["last"]?:0) + (delay * 60000) < currentTime)) {
		traceEvent(settings.logFilter, "setZoneSettings>scheduling rescheduleIfNeeded() in ${delay} minutes..",settings.detailedNotif, GLOBAL_LOG_INFO)
		try {        
			runEvery5Minutes(setZoneSettings)
		} catch (e) {
 			traceEvent(settings.logFilter,"rescheduleIfNeeded>exception $e while rescheduling",settings.detailedNotif, GLOBAL_LOG_ERROR,true)        
		}
		setZoneSettings()    
	}
    
    
	// Update rescheduled state
    
	if (!evt) state.poll["rescheduled"] = now()
}


def appTouch(evt) {
	state?.lastScheduleName=""	// force reset of the zone settings
	state.lastStartTime=null    
	setZoneSettings()
	rescheduleIfNeeded()    
}

void close_vents_in_zone(indiceZone) {
	def key= "zoneName$indiceZone"    
	def zoneName= settings[key]
	traceEvent(settings.logFilter,"close_vents_in_zone>closing all vents in $zoneName",settings.detailedNotif, GLOBAL_LOG_INFO)
	def ventSwitches=set_vent_switches_in_zone(indiceZone,0)
}

void open_vents_in_zone(indiceZone) {
	def key= "zoneName$indiceZone"    
	def zoneName= settings[key]
	traceEvent(settings.logFilter,"open_vents_in_zone>opening all vents in $zoneName",settings.detailedNotif, GLOBAL_LOG_INFO)
	def ventSwitches=set_vent_switches_in_zone(indiceZone,100)
}

private def set_vent_switches_in_zone(indiceZone, switchLevel) {
	def ventSwitchSet=[]	
	def key = "zoneName$indiceZone"
	def zoneName = settings[key]
	if (!zoneName || zoneName=='null') {
		return ventSwitchSet
	}
	key = "includedRooms$indiceZone"
	def rooms = settings[key]
    
	for (room in rooms) {
		def roomDetails=room.split(':')
		def indiceRoom = roomDetails[0]
		def roomName = roomDetails[1]

		if (!roomName || roomName=='null') {
			continue
		}
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			if (ventSwitch != null) {
				ventSwitchSet.add(ventSwitch)
				setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)
			}
		} 
	} /* end for rooms */
	return ventSwitchSet
}


def setZoneSettings() {

	traceEvent(settings.logFilter,"Begin of setZoneSettings Fcn",settings.detailedNotif, GLOBAL_LOG_TRACE)
	def todayDay = new Date().format("dd",location.timeZone)
	if ((!state?.today) || (todayDay != state?.today)) {
		state?.exceptionCount=0   
		state?.sendExceptionCount=0        
		state?.today=todayDay        
	}   
	Integer delay = 5 // By default, schedule SetZoneSettings() every 5 min.

	//schedule the rescheduleIfNeeded() function
	state?.poll["last"] = now()
    
	if (((state?.poll["rescheduled"]?:0) + (delay * 60000)) < now()) {
		traceEvent(settings.logFilter, "setZoneSettings>scheduling rescheduleIfNeeded() in ${delay} minutes..",settings.detailedNotif, GLOBAL_LOG_INFO)
		schedule("0 0/${delay} * * * ?", rescheduleIfNeeded)
		// Update rescheduled state
		state?.poll["rescheduled"] = now()
	}
	if (powerSwitch?.currentSwitch == "off") {
		traceEvent(settings.logFilter, "${powerSwitch.name} is off, schedule processing on hold...",true, GLOBAL_LOG_INFO)
		return
	}

	def currTime = now()
	boolean initialScheduleSetup=false        
	boolean foundSchedule=false

	if (thermostat) {
		/* Poll or refresh the thermostat to get latest values */
		if  (thermostat?.hasCapability("Polling")) {
			try {        
				thermostat.poll()
			} catch (e) {
				traceEvent(settings.logFilter,"setZoneSettings>not able to do a poll() on ${thermostat}, exception ${e}",settings.detailedNotif,GLOBAL_LOG_ERROR)
			}                    
		}  else if  (thermostat?.hasCapability("Refresh")) {
			try {        
				thermostat.refresh()
			} catch (e) {
				traceEvent(settings.logFilter,"setZoneSettings>not able to do a refresh() on ${thermostat}, exception ${e}",settings.detailedNotif,GLOBAL_LOG_ERROR)
			}                
		}                    
	}                    

	def ventSwitchesOn = []
    
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
	for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
        
		def key = "scheduleName$i"
		def scheduleName = settings[key]
		if (!scheduleName) {
			continue
		}
		key = "inactiveScheduleFlag$i"
		boolean inactiveSchedule=settings[key]
        
		if (inactiveSchedule) {
			traceEvent(settings.logFilter,"setZoneSettings>found schedule=${scheduleName}, inactive:$inactiveSchedule", settings.detailedNotif)
			continue
		}
		key = "selectedMode$i"
		def selectedModes = settings[key]
        
		key = "noSetpointsFlag$i"
		def noSetpointInSchedule = settings[key]?: false
        
		boolean foundMode=selectedModes.find{it == (location.currentMode as String)} 
		if ((selectedModes != null) && (!foundMode)) {
			traceEvent(settings.logFilter,"setZoneSettings>schedule=${scheduleName} does not apply,location.mode= $location.mode, selectedModes=${selectedModes},foundMode=${foundMode}, continue",
				settings.detailedNotif)
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
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName}, subtracted - 1 day, new startTime=${startTimeToday.time}",
				settings.detailedNotif)
		}            
		if ((currTime > endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
			endTimeToday = endTimeToday +1        
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName}, added + 1 day, new endTime=${endTimeToday.time}",settings.detailedNotif)

		}        
		String startInLocalTime = startTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
		String endInLocalTime = endTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)

		traceEvent(settings.logFilter,"setZoneSettings>found schedule ${scheduleName},original startTime=$startTime,original endTime=$endTime,nowInLocalTime= ${nowInLocalTime},startInLocalTime=${startInLocalTime},endInLocalTime=${endInLocalTime}," +
			"currTime=${currTime},begintime=${startTimeToday.time},endTime=${endTimeToday.time},lastScheduleName=${state?.lastScheduleName}, lastStartTime=${state.lastStartTime}",
			settings.detailedNotif)
		def ventSwitchesZoneSet = []        
		if ((currTime >= startTimeToday.time) && (currTime <= endTimeToday.time)  && (IsRightDayForChange(i)) && (lastScheduleName !=scheduleName)) {
        
			// let's set the given schedule
			initialScheduleSetup=true
			foundSchedule=true

			traceEvent(settings.logFilter,"setZoneSettings>new schedule ${scheduleName},foundSchedule=$foundSchedule, currTime= ${currTime}, current date & time OK for execution", detailedNotif)
            
			if ((thermostat) && (!noSetpointInSchedule)){
				traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},about to set the thermostat setpoint", settings.detailedNotif)
 				set_thermostat_setpoint_in_zone(i)
			}            
			// set the zoned vent switches to 'on' and adjust them according to the ambient temperature
               
			ventSwitchesZoneSet= adjust_vent_settings_in_zone(i)
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},list of Vents turned 'on'= ${ventSwitchesZoneSet}",settings.detailedNotif)

 			ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet              
			state?.lastScheduleName = scheduleName
			state?.lastStartTime = startTimeToday.time
		}
		else if ((state?.lastScheduleName == scheduleName) && (currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange(i))) {
			// We're in the middle of a schedule run
        
			foundSchedule=true
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},foundSchedule=$foundSchedule,currTime= ${currTime}, current time is OK for execution, we're in the middle of a schedule run",
				settings.detailedNotif)
			// let's adjust the vent settings according to desired Temp only if thermostat is not idle or was not idle at the last run
			key = "adjustVentsEveryCycleFlag$i"
			def adjustVentSettings = (settings[key]) ?: false
			traceEvent(settings.logFilter,"setZoneSettings>adjustVentsEveryCycleFlag=$adjustVentSettings",settings.detailedNotif)
			
			if (thermostat) {
				// Check the operating State before adjusting the vents again.
				String operatingState = thermostat.currentThermostatOperatingState           
				if ((adjustVentSettings) || ((operatingState?.toUpperCase() !='IDLE') ||
					((state?.operatingState) && (state?.operatingState.toUpperCase() =='HEATING') || (state?.operatingState.toUpperCase() =='COOLING'))))
				{            
					traceEvent(settings.logFilter,"setZoneSettings>thermostat ${thermostat}'s Operating State is ${operatingState} or was just recently " +
						"${state?.operatingState}, adjusting the vents for schedule ${scheduleName}",settings.detailedNotif, GLOBAL_LOG_INFO)
					ventSwitchesZoneSet=adjust_vent_settings_in_zone(i)
					ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet     
				}                    
				state?.operatingState =operatingState            
			}  else if (adjustVentSettings) {
				ventSwitchesZoneSet=adjust_vent_settings_in_zone(i)
				ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet     
            
			}   
		} else {
			traceEvent(settings.logFilter,"schedule: ${scheduleName},change not scheduled at this time ${nowInLocalTime}...",settings.detailedNotif)
		}

	} /* end for */
    
	if (((setVentSettings) && (ventSwitchesOn !=[])) || (initialScheduleSetup && (ventSwitchesOn !=[]))) {
		traceEvent(settings.logFilter,"setZoneSettings>list of Vents turned on= ${ventSwitchesOn}",settings.detailedNotif)
		turn_off_all_other_vents(ventSwitchesOn)
	}
	if (!foundSchedule) {
		traceEvent(settings.logFilter, "No schedule applicable at this time ${nowInLocalTime}",settings.detailedNotif,GLOBAL_LOG_INFO)
	} 
}


private def isRoomOccupied(sensor, indiceRoom) {
	def key ="occupiedMotionOccNeeded${indiceRoom}"
	def nbMotionNeeded = (settings[key]) ?: 1
    // If mode is Night, then consider the room occupied.
	key = "roomName$indiceRoom"
	def roomName = settings[key]
    
	if (location.mode == "Night") {
		traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} is considered occupied, location mode ($location.mode) == Night",settings.detailedNotif)
		return nbMotionNeeded
    
	}    
	if (thermostat) {
		try {    
			String currentProgName = thermostat.currentSetClimate
			if (currentProgName?.toUpperCase().contains('SLEEP')) { 
				// Rooms are considered occupied when the ecobee program is set to 'SLEEP'    
				traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} is considered occupied, ecobee ($currentProgName) == Sleep",settings.detailedNotif)
				return nbMotionNeeded
			} 
		} catch (any) {
			traceEvent(settings.logFilter,"isRoomOccupied>not an ecobee thermostat, continue",settings.detailedNotif)           
		}        
	}    
   
	key = "residentsQuietThreshold$indiceRoom"
	def threshold = (settings[key]) ?: 15 // By default, the delay is 15 minutes 

	def t0 = new Date(now() - (threshold * 60 * 1000))
	def recentStates = sensor.statesSince("motion", t0)
	def countActive =recentStates.count {it.value == "active"}
 	if (countActive>0) {
		traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} has been occupied, motion was detected at sensor ${sensor} in the last ${threshold} minutes",settings.detailedNotif)
		traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName}, is motion counter (${countActive}) for the room >= motion occurence needed (${nbMotionNeeded})?",settings.detailedNotif)
		if (countActive >= nbMotionNeeded) {
			return countActive
		}            
 	}
	return 0
}

private def any_contact_open(contactSet) {

	int contactSize=(contactSet)? contactSet.size() :0
	for (i in 0..contactSize -1) {
		def contactState = contactSet[i].currentState("contact")
		if (contactState.value == "open") {
			traceEvent(settings.logFilter,"any_contact_open>contact ${contactSet[i]} is open",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                        
			return true 
		}
	}            
	return false    
}


private def getSensorTemperature(indiceRoom, refreshSensor=true) {
	def key = "tempSensor$indiceRoom"
	def currentTemp=null
	    
	def tempSensor = settings[key]
	if (tempSensor != null) {
		traceEvent(settings.logFilter,"getSensorTemperature>found sensor ${tempSensor}",settings.detailedNotif)
		if ((refreshSensor) && (tempSensor.hasCapability("Refresh"))) {
			// do a refresh to get the latest temp value
			try {        
				tempSensor.refresh()
			} catch (e) {
				traceEvent(settings.logFilter,"getSensorTemperature>not able to do a refresh() on $tempSensor",settings.detailedNotif, GLOBAL_LOG_INFO)
			}                
		}        
		currentTemp = tempSensor.currentTemperature?.toFloat().round(1)
	}
	return currentTemp
}


private def set_thermostat_setpoint_in_zone(indiceSchedule) {
	def scale = (state?.scale) ?: getTemperatureScale()
	float desiredHeat, desiredCool
	def currentHeatingSetpoint, currentCoolingSetpoint    

	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	key = "includedZones$indiceSchedule"
	def zones = settings[key]
	key = "givenClimate$indiceSchedule"
	def climateName = settings[key]

	float currentTemp = thermostat?.currentTemperature.toFloat().round(1)
	String mode = thermostat?.currentThermostatMode
	currentHeatingSetpoint=thermostat?.currentHeatingSetpoint
	currentCoolingSetpoint=thermostat?.currentCoolingSetpoint
    
	if (mode in ['heat', 'auto', 'emergency heat']) {
		if ((climateName) && (thermostat.hasCommand("setThisTstatClimate"))) {
			try {
				thermostat.setThisTstatClimate(climateName)
				thermostat.refresh() // to get the latest setpoints
			} catch (any) {
				traceEvent(settings.logFilter,"schedule ${scheduleName}:not able to set climate ${climateName} for heating at the thermostat ${thermostat}",
					true, GLOBAL_LOG_ERROR,true)                
			}                
			desiredHeat = thermostat?.currentHeatingSetpoint
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredHeat=${desiredHeat}",
					settings.detailedNotif)                
            
		} else {
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for heatingSetpoint",
					settings.detailedNotif)                
			key = "desiredHeat$indiceSchedule"
			def heatTemp = settings[key]
			if (!heatTemp) {
				traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName}:about to apply default heat settings",
					settings.detailedNotif)                
                
				desiredHeat = (scale=='C') ? 21:72 					// by default, 21C/72F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredHeat=${desiredHeat}",
					settings.detailedNotif)                

			if (desiredHeat != currentHeatingSetpoint) thermostat?.setHeatingSetpoint(desiredHeat)
		} 
		traceEvent(settings.logFilter,"schedule ${scheduleName},in zones=${zones},heating setPoint now =${desiredHeat}",
			settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
		if (scheduleName != state?.lastScheduleName) {
			state.scheduleHeatSetpoint=desiredHeat 
		}  
	}        
	if (mode in ['cool', 'auto']) {
		if ((climateName) && (thermostat.hasCommand("setThisTstatClimate"))) {
			try {
				thermostat?.setThisTstatClimate(climateName)
				thermostat.refresh() // to get the latest setpoints
			} catch (any) {
				traceEvent(settings.logFilter,"schedule ${scheduleName},not able to set climate ${climateName} for cooling at the thermostat(s) ${thermostat}",
					true, GLOBAL_LOG_ERROR,true)                
			}                
			desiredCool = thermostat?.currentCoolingSetpoint
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredCool=${desiredCool}",
					settings.detailedNotif)                
            
		} else {
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for coolingSetpoint",
					settings.detailedNotif)                

			key = "desiredCool$indiceSchedule"
			def coolTemp = settings[key]
			if (!coolTemp) {
				traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},about to apply default cool settings",
					settings.detailedNotif)                
                
				desiredCool = (scale=='C') ? 23:75					// by default, 23C/75F is the target cool temp
			} else {
				desiredCool = coolTemp.toFloat()
			}
            
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredCool=${desiredCool}",
					settings.detailedNotif)                
            
		} 
		traceEvent(settings.logFilter,"schedule ${scheduleName}, in zones=${zones},cooling setPoint now =${desiredCool}",
			settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
		if (scheduleName != state?.lastScheduleName) {
			state.scheduleCoolSetpoint=desiredCool 
		}        
		if (desiredCool != currentCoolingSetpoint) thermostat?.setCoolingSetpoint(desiredCool)
	} /* else if mode == 'cool' */

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
		traceEvent(settings.logFilter,"getTempSensorForAverage>found sensor ${tempSensor}",settings.detailedNotif)
		currentTemp = tempSensor.currentTemperature?.toFloat().round(1)
	}
	return currentTemp
}

private void refresh_virtual_zone_values(indiceZone,refreshSensors=false, activeInSchedule='on') {
	def tempAtSensor
	def key = "zoneName$indiceZone"
	def zoneName= settings[key]
	String mode = 'auto'
	if (thermostat) mode=thermostat.currentThermostatMode    
	def adjustmentBasedOnContact=(settings.setTempAdjustmentContactFlag)?:false
	String motionValue="inactive"
	def indoorTemps = []
	key = "includedRooms$indiceZone"
	def rooms = settings[key]
	boolean isContactOpen=false  
	for (room in rooms) {

		def roomDetails=room.split(':')
		def indiceRoom = roomDetails[0]
		def roomName = roomDetails[1]

		if (!roomName || roomName=='null') {
			continue
		}                
		key = "inactiveRoomFlag$indiceRoom"
		boolean inactiveRoom = settings[key]
		if (inactiveRoom) {
			traceEvent(settings.logFilter,"refresh_virtual_zone_values>in zone $zoneName, room=${roomName},inactive:$inactiveRoom",settings.detailedNotif)
			continue
		}                
		
		key = "contactSensor$indiceRoom"
		def contactSensor = settings[key]
		if (contactSensor) {
			isContactOpen = any_contact_open(contactSensor)            
			if (adjustmentBasedOnContact) {			
				key = "contactClosedLogicFlag${indiceRoom}"            
				boolean closedContactLogicFlag= (settings[key])?:false            
				if ((!closedContactLogicFlag) && isContactOpen ) {
					continue  // do not use the temp inside the room as the associated contact is open
				} else if (closedContactLogicFlag && (!isContactOpen)) {
					continue			                
				}                
			}                
		}            
		key = "needOccupiedFlag$indiceRoom"
		def needOccupied = (settings[key]) ?: false
		traceEvent(settings.logFilter,"refresh_virtual_zone_values>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
			settings.detailedNotif)        

		key = "tempSensor$indiceRoom"
		def tempSensor = settings[key]

		if ((refreshSensors) && ((tempSensor) && (tempSensor.hasCapability("Refresh")))) {
			// do a refresh to get the latest temp value
			try {        
				tempSensor.refresh()
			} catch (e) {
				traceEvent(settings.logFilter,"refresh_virtual_zone_values>not able to do a refresh() on $tempSensor",settings.detailedNotif, GLOBAL_LOG_INFO)
			}                
		}        
		key = "motionSensor$indiceRoom"
		def motionSensor = settings[key]
		if (motionSensor != null) {

			if ((refreshSensors) && ((motionSensor != tempSensor) && (motionSensor.hasCapability("Refresh")))) {
				// do a refresh to get the motion value if motionSensor != tempSensor
				try {        
					motionSensor.refresh()
				} catch (e) {
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>not able to do a refresh() on $motionSensor",settings.detailedNotif, GLOBAL_LOG_INFO)
				}                
			}
			def isRoomOccupied=isRoomOccupied(motionSensor, indiceRoom)            
			if ((isRoomOccupied) || (!needOccupied)) {
				tempAtSensor = getSensorTempForAverage(indiceRoom)
				if (tempAtSensor != null) {
					indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
						traceEvent(settings.logFilter,"refresh_virtual_zone_values>added ${tempAtSensor.toString()} due to occupied room ${roomName} based on ${motionSensor}",
							settings.detailedNotif)
				}
				tempAtSensor = getSensorTempForAverage(indiceRoom,'roomTstat')
				if (tempAtSensor != null) {
					indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>added ${tempAtSensor.toString()} due to occupied room ${roomName} based on ${motionSensor}",
						settings.detailedNotif)                        
				}
				if (isRoomOccupied) motionValue="active"    
			}
                
		} else {
			tempAtSensor = getSensorTempForAverage(indiceRoom)
			if (tempAtSensor != null) {
				traceEvent(settings.logFilter,"refresh_virtual_zone_values>added ${tempAtSensor.toString()} in room ${roomName}",settings.detailedNotif)
				indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
			}
			tempAtSensor = getSensorTempForAverage(indiceRoom,'roomTstat')
			if (tempAtSensor != null) {
				indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>added ${tempAtSensor.toString()} in room ${roomName}",settings.detailedNotif)
			}
		}
	} /* end for rooms */

// Refresh My Virtual zone device if needed    
	key = "virtualZoneFlag${indiceZone}"
	def MAX_DELTA=20
	def virtualCreateZoneFlag = (settings[key]) ?:false
	def scale = (state?.scale) ?: getTemperatureScale()
	if (virtualCreateZoneFlag) {
		def dni = "My Virtual Zone.${zoneName}.$indiceZone"  
		def d = getChildDevice(dni)
		if (d) {  
			double avg_temp_in_zone
			String tempValueString            
			if (indoorTemps != []) {
				avg_temp_in_zone=  (indoorTemps.sum()/indoorTemps.size()).toDouble().round(1)
				if (scale == "C") {
					tempValueString = String.format('%2.1f', avg_temp_in_zone)
				} else {
					avg_temp_in_zone=avg_temp_in_zone.round()            
					tempValueString = String.format('%2d', avg_temp_in_zone.intValue())            
				}
				def isChange = d.isStateChange(d, "temperature", tempValueString)	//check of state change
				d.sendEvent(name:"temperature", value: tempValueString, isDisplayed:true, isStateChange:isChange)
			}                
			def isChange = d.isStateChange(d, "motion", motionValue)	//check of state change
			d.sendEvent(name:"motion", value: motionValue, isDisplayed:true, isStateChange:isChange)
			def contactValue=((isContactOpen)?"open":"closed")             
			isChange = d.isStateChange(d, "contact", contactValue)	//check of state change
			d.sendEvent(name:"contact", value: contactValue, isDisplayed:true, isStateChange:isChange)
			isChange = d.isStateChange(d, "mode", mode)	//check of state change
			d.sendEvent(name:"mode", value:mode, isDisplayed:true, isStateChange:isChange)            
			isChange = d.isStateChange(d, "activeInSchedule", activeInSchedule)	//check of state change
			d.sendEvent(name:"activeInSchedule", value: activeInSchedule, isDisplayed: true, isStateChange: isChange)          
			traceEvent(settings.logFilter,"refresh_virtual_zone_values>$dni found, refreshed temp value with $avg_temp_in_zone, motion=$motionDetected",settings.detailedNotif)
			double thermostatSetpoint            
			def coolSP, heatSP, thermostatSP, isChangeMainSP=false
			def tempDelta=d.currentValue("tempDelta")
			tempDelta=(tempDelta==null)? tempDelta=0 : tempDelta.toDouble()            
			key ="desiredCoolTemp$indiceZone"
			def staticCoolTemp=settings[key]			            
			key ="desiredHeatTemp$indiceZone"
			def staticHeatTemp=settings[key]			            
			if (staticCoolTemp && (mode in ['cool', 'off','auto'])) {
				if (scale == "C") {
					thermostatSetpoint=staticCoolTemp.toDouble().round(1)
					tempValueString = String.format('%2.1f', thermostatSetpoint)
				} else {
					thermostatSetpoint=staticCoolTemp.toDouble().round()            
					tempValueString = String.format('%2d', thermostatSetpoint.intValue())            
				}
				isChangeMainSP = d.isStateChange(d, "baselineSetpoint",tempValueString)	//check of state change            
				d.sendEvent(name:"baselineSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
				traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating baselineSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				d.sendEvent(name:"tempDelta", value: "0", isDisplayed:false) // reset the delta
			} else if (staticHeatTemp && (mode in ['heat', 'off','auto'])) {
				if (scale == "C") {
					thermostatSetpoint=staticHeatTemp.toDouble().round(1)
					tempValueString = String.format('%2.1f', thermostatSetpoint)
				} else {
					thermostatSetpoint=staticHeatTemp.toDouble().round()            
					tempValueString = String.format('%2d', thermostatSetpoint.intValue())            
				}
				isChangeMainSP = d.isStateChange(d, "baselineSetpoint",tempValueString)	//check of state change            
				d.sendEvent(name:"baselineSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
				d.sendEvent(name:"tempDelta", value: "0", isDisplayed:false) // reset the delta
			} else if (thermostat) {            
				try {
					thermostatSP=thermostat?.currentThermostatSetpoint
				} catch (e) {
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>no thermostat setpoint... exception $e",settings.detailedNotif)
				}            
				if (!thermostatSP) {
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>no thermostat setpoint... will calculate it",settings.detailedNotif)
					if (mode in ['heat', 'auto', 'off', 'eco']) {                
						try {
							heatSP=(thermostat?.currentHeatingSetpoint)?:(scale=='C')?21:72
							thermostatSetpoint=heatSP.toDouble()                        
						} catch (e) {
							traceEvent(settings.logFilter,"refresh_virtual_zone_values>no thermostat heatingSetpoint... exceptio $e",settings.detailedNotif)
						}
					}                    
					if (mode in ['cool', 'auto', 'off', 'eco']) {                
						try {
							coolSP=(thermostat?.currentCoolingSetpoint)?:(scale=='C')?23:73
							thermostatSetpoint=coolSP.toDouble()                        
						} catch (e) {
							traceEvent(settings.logFilter,"refresh_virtual_zone_values>no thermostat cooling Setpoint... exceptio $e",settings.detailedNotif)
						}
					}                    
					if (mode in ['auto', 'off', 'eco']) {                
						thermostatSetpoint=((coolSP + heatSP)/2).toDouble().round(1)                
					}                
				} else {
					thermostatSetpoint=thermostatSP.toDouble()            
				}            
				if (scale == "C") {
					tempValueString = String.format('%2.1f', thermostatSetpoint)
				} else {
					thermostatSetpoint=thermostatSetpoint.round()            
					tempValueString = String.format('%2d', thermostatSetpoint.intValue())            
				}
                
				isChangeMainSP = d.isStateChange(d, "baselineSetpoint",tempValueString)	//check of state change            
				d.sendEvent(name:"baselineSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
                               
                
			} /* end if thermostat */
			def desiredHeatDelta, desiredCoolDelta
			key  = "desiredHeatDeltaTemp$indiceZone"
			desiredHeatDelta =  settings[key]
			key  = "desiredCoolDeltaTemp$indiceZone"
			desiredCoolDelta = settings[key]
			// the delta in the app takes precedence over the ones set in the virtual zones
            
			if (staticHeatTemp && (mode in ['heat', 'off','auto'])) {
				traceEvent(settings.logFilter,"refresh_virtual_zone_values>about to refresh thermostatSetpoint based on static desiredHeat temp ($staticHeatTemp)",settings.detailedNotif)
				if (scale == 'C') {
					double newThermostatSetpoint= staticHeatTemp.toDouble().round(1)
					tempValueString = String.format('%2.1f', newThermostatSetpoint)
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>reset the delta to zero",settings.detailedNotif)
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else {
					double newThermostatSetpoint= staticHeatTemp.toDouble().round()
					tempValueString = String.format('%2d', newThermostatSetpoint.intValue())           
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>reset the delta to zero",settings.detailedNotif)
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange,unit:scale)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}
			}            
			if (staticCoolTemp && (mode in ['cool', 'off','auto'])) {
				traceEvent(settings.logFilter,"refresh_virtual_zone_values>about to refresh thermostatSetpoint based on static desiredCool temp ($staticCoolTemp)",settings.detailedNotif)
				if (scale == 'C') {
					double newThermostatSetpoint= staticCoolTemp.toDouble().round(1)
					tempValueString = String.format('%2.1f', newThermostatSetpoint)
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange,unit:scale)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else {
					double newThermostatSetpoint= staticCoolTemp.toDouble().round()
					tempValueString = String.format('%2d', newThermostatSetpoint.intValue())           
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange,unit:scale)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}
			}            
			if ((desiredHeatDelta != tempDelta) && (mode in ['heat', 'off','auto']) && (!staticHeatTemp)) {
				if (desiredHeatDelta) tempDelta=desiredHeatDelta.toDouble()
				traceEvent(settings.logFilter,"refresh_virtual_zone_values>about to refresh tempDelta & thermostatSetpoint based on tempDelta $tempDelta and desiredHeatDelta ($desiredHeatDelta)",settings.detailedNotif)
				if (tempDelta.toDouble().abs() <= MAX_DELTA) {            
					d.sendEvent(name:"tempDelta", value:tempDelta, isDisplayed:true, isStateChange:true)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating tempDelta to $tempDelta for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else { // reset the delta
					d.sendEvent(name:"tempDelta", value:MAX_DELTA, isDisplayed:true, isStateChange:true)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>MAX delta is reached ($MAX_DELTA)",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}
				if (thermostat) {
					if (scale == 'C') {
						double newThermostatSetpoint=( thermostatSetpoint+ tempDelta.toDouble()).round(1)
						tempValueString = String.format('%2.1f', newThermostatSetpoint)
						isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
						d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange,unit:scale)
						traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
					} else {
						double newThermostatSetpoint=( thermostatSetpoint + tempDelta.toDouble()).round()       
						tempValueString = String.format('%2d', newThermostatSetpoint.intValue())           
						isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
						d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange,unit:scale)
						traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
					}
				}                    
			}  
			// the delta in the app takes precedence over the ones set in the virtual zones
			if ((desiredCoolDelta != tempDelta) && (mode in ['cool', 'off','auto']) && (!staticCoolTemp)) {
				if (desiredCoolDelta) tempDelta=desiredCoolDelta.toDouble()
				traceEvent(settings.logFilter,"refresh_virtual_zone_values>about to refresh tempDelta & thermostatSetpoint based on tempDelta $tempDelta and desiredCoolDelta ($desiredCoolDelta)",settings.detailedNotif)
				if (tempDelta.toDouble().abs() <= MAX_DELTA) {            
					d.sendEvent(name:"tempDelta", value:tempDelta, isDisplayed:true, isStateChange:true)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating tempDelta to $tempDelta for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else { // reset the delta
					d.sendEvent(name:"tempDelta", value:MAX_DELTA, isDisplayed:true, isStateChange:true)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>MAX delta is reached ($MAX_DELTA)",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}
				if (thermostat) {                
					if (scale == 'C') {
						double newThermostatSetpoint=( thermostatSetpoint+ tempDelta.toDouble()).round(1)
						tempValueString = String.format('%2.1f', newThermostatSetpoint)
						isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
						d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange,unit:scale)
						traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
					} else {
						double newThermostatSetpoint=( thermostatSetpoint + tempDelta.toDouble()).round()       
						tempValueString = String.format('%2d', newThermostatSetpoint.intValue())           
						isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
						d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange,unit:scale)
						traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
					}
				}                    
			}                
			if (isChangeMainSP && thermostat && (!staticHeatTemp && !staticCoolTemp)) {  // to change the thermosatSetpoint if there are no static values inputted and baseline setpoint has changed
				if (scale == 'C') {
					double newThermostatSetpoint=( thermostatSetpoint + tempDelta.toDouble()).round(1)
					tempValueString = String.format('%2.1f', newThermostatSetpoint)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>about to refresh thermostatSetpoint based on new baselineSetpoint ($tempValueString)",settings.detailedNotif)
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName} following changes at baseline",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else {
 					double newThermostatSetpoint=( thermostatSetpoint + tempDelta.toDouble()).round()       
					tempValueString = String.format('%2d', newThermostatSetpoint.intValue())           
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>about to refresh thermostatSetpoint based on new baselineSetpoint ($tempValueString)",settings.detailedNotif)
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"refresh_virtual_zone_values>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName} following changes at baseline",settings.detailedNotif, GLOBAL_LOG_INFO, true)
 				}               
			}           
		} else {
			traceEvent(settings.logFilter,"refresh_virtual_zone_values>$dni not found, not able to update values",settings.detailedNotif)
		}
		            
	}
    
}


private void set_virtual_zones_inactive_in_schedule(indiceSchedule) {   
	def	key= "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	def zoneName
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		key = "roomName$indiceRoom"
		def roomName = settings[key]

		boolean found_room_in_zone = false
		key = "includedZones$indiceSchedule"
		def zones = settings[key]
		for (zone in zones) {		// look up for the room in the includedRooms for each zone
			def zoneDetails=zone.split(':')
			def indiceZone = zoneDetails[0]
			zoneName = zoneDetails[1]
			if (!zoneName || zoneName=='null') {
				continue
			} 
/*            
			key = "inactiveZoneFlag$indiceZone"
			boolean inactiveZone=(state?."inactiveZone$indiceZone"!= null)? state?."inactiveZone$indiceZone".toBoolean() :settings[key]
			if (inactiveZone) {
				traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule $scheduleName, zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
				continue
			}            
*/
			key = "includedRooms$indiceZone"
			def rooms = settings[key]
			if (rooms.toString().contains(roomName.toString().trim())) {
				traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule $scheduleName, found $roomName in zone $zoneName",settings.detailedNotif)
				found_room_in_zone=true
				break                
			} 
		} /* end for each zone */
		if (!found_room_in_zone) { 	// find the zone(s) associated to the excluded room	    
			// now get the associated zone to turn off the activeInSchedule switch in virtual zone (if any) 
			for (int z =1; ((z <= settings.zonesCount) && (z <= get_MAX_ZONES())); z++) {
				key = "zoneName$z"
				zoneName = settings[key]
				key = "includedRooms$z"
				def rooms = settings[key]
				if (rooms.toString().contains(roomName.toString().trim())) {
					traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule $scheduleName, found $roomName in zone $zoneName, about to check virtual zone flag",settings.detailedNotif)
					// Refresh My Virtual zone device if needed    
					key = "virtualZoneFlag${z}"
					def virtualCreateZoneFlag = (settings[key]) ?:false
					if (virtualCreateZoneFlag) {
						refresh_virtual_zone_values(z, true, "off")                    
						traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule $scheduleName, ${zoneName}'s activeSchedule set to off, outside of scheduled zones",settings.detailedNotif)
/*                    
						def dni = "My Virtual Zone.${zoneName}.$z"  
						def d = getChildDevice(dni)
0/*                    
						def dni = "My Virtual Zone.${zoneName}.$z"  
						def d = getChildDevice(dni)
						if (d) {
							def isChange = d.isStateChange(d, "activeInSchedule", "off")	//check of state change
							traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule $scheduleName, found $roomName in zone $zoneName, about to set corresponding virtual zone ($dni) to off",settings.detailedNotif)
							d.sendEvent(name:"activeInSchedule", value: "off", isDisplayed: true, isStateChange: isChange)          
						}
*/                        
					}                         
				} /* end if room is part of the rooms in the zone. */                        
			} /* end for all zones */
		} /* end if room not in active zones */       
	} /* end for each room */
}



private def adjust_vent_settings_in_zone(indiceSchedule) {
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	float desiredTemp,total_temp_in_vents=0
	def indiceRoom
	boolean closedAllVentsInZone=true
	int nbVents=0, openVentsCount=0,closedVentsCount=0,total_level_vents=0
	def switchLevel    
	def ventSwitchesOnSet=[]
	def scale = (state?.scale) ?: getTemperatureScale()
	def currentHeatingSetpoint,currentCoolingSetpoint
    
	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key= "openVentsFanOnlyFlag$indiceSchedule"
	def openVentsWhenFanOnly = (settings[key])?:false
	String operatingState = thermostat?.currentThermostatOperatingState           

	set_virtual_zones_inactive_in_schedule(indiceSchedule)  // set the inactive zones flag in schedule
    
	if ((thermostat) && (openVentsWhenFanOnly) && (operatingState.toUpperCase().contains("FAN ONLY"))) { 
 		// If fan only and the corresponding flag is true, then set all vents to 100% and finish the processing
 		if ((settings.delayInSecForVentSettings)) {  // if delay has been asked, then call runIn  
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>${scheduleName}:set all vents to 100% with delay of $delayInSecForVentSettings seconds,exiting",
			 	settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
			runIn(delayInSecForVentSettings, "open_all_vents")
		} else  {            
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>${scheduleName}:set all vents to 100% in fan only mode,exiting",
			 	settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
			open_all_vents()        
		}
		return ventSwitchesOnSet      
	}
    
	key = "includedZones$indiceSchedule"
	def zones = settings[key]
 
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}: zones= ${zones}",
		settings.detailedNotif)     

	float currentTempAtTstat =(scale=='C')?21:72
	String mode='auto' // By default, set to auto
	if (thermostat) {
		currentTempAtTstat = thermostat.currentTemperature.toFloat().round(1)
 		mode = thermostat.currentThermostatMode
		try {        
			currentHeatingSetpoint=thermostat?.currentHeatingSetpoint        
		} catch (any) {
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>not able to get current heatingSetpoint from $thermostat, mode is $mode",
				settings.detailedNotif, GLOBAL_LOG_WARN)     
		}
		try {        
			currentCoolingSetpoint=thermostat?.currentCoolingSetpoint        
		} catch (any) { 
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>not able to get current coolingSetpoint from $thermostat, mode is $mode",
				settings.detailedNotif, GLOBAL_LOG_WARN)     
		}
	}        
	key = "setVentLevel${indiceSchedule}"
	def defaultSetLevel = settings[key]
	key = "resetLevelOverrideFlag${indiceSchedule}"	
	boolean resetLevelOverrideFlag = settings[key]
	def fullyCloseVents = (fullyCloseVentsFlag) ?: false
	state?.activeZones=zones
	def min_open_level=100, max_open_level=0, nbRooms=0    
	float min_temp_in_vents=200, max_temp_in_vents=0, total_temp_diff=0, median    
	def adjustmentBasedOnContact=(settings.setVentAdjustmentContactFlag)?:false
	state?.activeZones = zones // save the zones for the dashboard                
  	
	for (zone in zones) {

		def zoneDetails=zone.split(':')
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		if (!zoneName || (zoneName=='null')) {
			continue
		}
		key = "inactiveZoneFlag$indiceZone"
		boolean inactiveZone=settings[key]
		if (inactiveZone) {
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
			continue
		}
		refresh_virtual_zone_values(indiceZone)        
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		if (mode=='cool') {
			key = "desiredCoolTemp$indiceZone"
			def desiredCool= settings[key]
			if (!desiredCool) {            
				desiredCool = (currentCoolingSetpoint?:(scale=='C')?23:75)
			}                
			key  = "desiredCoolDeltaTemp$indiceZone"
			def desiredCoolDelta =  settings[key]           
			desiredTemp= desiredCool.toFloat() + (desiredCoolDelta?:0)                
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredCoolDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
		} else if (mode in ['auto', 'off', 'eco']) {
			key = "desiredCoolTemp$indiceZone"
			def desiredCool= settings[key]
			if (!desiredCool) {            
				desiredCool = (currentCoolingSetpoint?:(scale=='C')?23:75)
			}                
			key = "desiredHeatTemp$indiceZone"
			def desiredHeat= settings[key]
			if (!desiredHeat) {            
				desiredHeat = (currentHeatingSetpoint?:(scale=='C')?21:72)
			}                
			key  = "desiredHeatDeltaTemp$indiceZone"
			def desiredHeatDelta =  (state?."desiredHeatTempDelta$indiceZone")? state?."desiredHeatTempDelta$indiceZone".toFloat(): settings[key]
			key  = "desiredCoolDeltaTemp$indiceZone"
			def desiredCoolDelta =  (state?."desiredCoolTempDelta$indiceZone")? state?."desiredCoolTempDelta$indiceZone".toFloat(): settings[key]
			median = ((desiredHeat + desiredCool)/2).toFloat().round(1)
			if (currentTempAtTstat > median) {
				desiredTemp =desiredCool.toFloat().round(1) + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)          
			} else {
				desiredTemp =desiredHeat.toFloat().round(1)  + ((desiredHeatDelta)?desiredHeatDelta.toFloat():0)              
			}                        
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredHeatDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredCoolDelta=${desiredCoolDelta}",			
				settings.detailedNotif)     
		} else {
			key = "desiredHeatTemp$indiceZone"
			def desiredHeat= settings[key]
			if (!desiredHeat) {            
				desiredHeat = (currentHeatingSetpoint?:(scale=='C')?21:72)
			}       
			key  = "desiredHeatDeltaTemp$indiceZone"
			def desiredHeatDelta =  settings[key]           
			desiredTemp= desiredHeat.toFloat() +  (desiredHeatDelta?:0)
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredHeatDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
            
		}
		for (room in rooms) {
        
			nbRooms++        
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredTemp=${desiredTemp}",			
				settings.detailedNotif)     


			switchLevel =null	// initially set to null for check later
			def roomDetails=room.split(':')
			indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]
			if (!roomName || (roomNam =='null')) {
				continue
			}
			key = "inactiveRoomFlag$indiceRoom"
			boolean inactiveRoom = settings[key]
			if (inactiveRoom) {
				traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule $scheduleName, room=${roomName},inactive:$inactiveRoom",settings.detailedNotif)
				continue
			}                
         
			key = "needOccupiedFlag$indiceRoom"
			def needOccupied = (settings[key]) ?: false
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
				settings.detailedNotif)     
            

			if (needOccupied) {
				key = "motionSensor$indiceRoom"
				def motionSensor = settings[key]
				if (motionSensor != null) {
					if (!isRoomOccupied(motionSensor, indiceRoom)) {
						switchLevel = (fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE // setLevel at a minimum as the room is not occupied.
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, ${roomName} is not occupied,vents set to mininum level=${switchLevel}",
							settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)     
                        
					}
				}
			} 
            
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>AdjustmentBasedOnContact=${adjustmentBasedOnContact}",settings.detailedNotif)
            
			if (adjustmentBasedOnContact) { 
				key = "contactSensor$indiceRoom"
				def contactSensor = settings[key]
				traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>contactSensor=${contactSensor}",settings.detailedNotif)
				if (contactSensor !=null) {
					key = "contactClosedLogicFlag${indiceRoom}"            
					boolean closedContactLogicFlag= (settings[key])?:false            
					boolean isContactOpen = any_contact_open(contactSensor)            
					if (!closedContactLogicFlag && isContactOpen ) {
						switchLevel=((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE)					                
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, a contact ${contactSensor} is open, the vent(s) in ${roomName} set to mininum level=${switchLevel}",
							settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)                        
					} else if (closedContactLogicFlag && (!isContactOpen)) {
						switchLevel=((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE)					                
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, contact(s) ${contactSensor} closed, the vent(s) in ${roomName} set to mininum level=${switchLevel}",
							settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)                        
					}                
				}            
			}            
	           
			if (switchLevel ==null) {
				def tempAtSensor =getSensorTemperature(indiceRoom)			
				if (tempAtSensor == null) {
					tempAtSensor= currentTempAtTstat				            
				}
                
				float temp_diff_at_sensor = (tempAtSensor - desiredTemp).toFloat().round(1)
				total_temp_diff =  total_temp_diff + temp_diff_at_sensor                
				traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>thermostat mode = ${mode}, schedule ${scheduleName}, in zone ${zoneName}, room ${roomName}, temp_diff_at_sensor=${temp_diff_at_sensor}",
					settings.detailedNotif)     
                
				if ((mode=='cool') || ((mode in ['auto', 'off', 'eco']) && (tempAtSensor> median)))  {
					switchLevel=(temp_diff_at_sensor <=0)? ((fullyCloseVents) ? 0: MIN_OPEN_LEVEL_IN_ZONE): 100
				} else  {
					switchLevel=(temp_diff_at_sensor >=0)? ((fullyCloseVents) ? 0: MIN_OPEN_LEVEL_IN_ZONE): 100
				}                
			} 
                
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, room ${roomName},switchLevel to be set=${switchLevel}",
						settings.detailedNotif)     
                    
					float temp_in_vent=getTemperatureInVent(ventSwitch)     
					if (temp_in_vent) {
						// compile some stats for the dashboard                    
						min_temp_in_vents=(temp_in_vent < min_temp_in_vents)? temp_in_vent.toFloat().round(1): min_temp_in_vents
						max_temp_in_vents=(temp_in_vent > max_temp_in_vents)? temp_in_vent.toFloat().round(1): max_temp_in_vents
						total_temp_in_vents=total_temp_in_vents + temp_in_vent
					}                        
					def switchOverrideLevel=null                 
					nbVents++
					if (!resetLevelOverrideFlag) {
						key = "ventLevel${j}$indiceRoom"
						switchOverrideLevel = settings[key]
					}                        
					if (switchOverrideLevel != null) {                
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} to switchOverrideLevel =${switchOverrideLevel}%",
							settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)     
						switchLevel = ((switchOverrideLevel >= 0) && (switchOverrideLevel<= 100))? switchOverrideLevel:switchLevel                     
					} else if (defaultSetLevel != null)  {
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} to defaultSetLevel =${defaultSetLevel}%",
							settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)     
						switchLevel = ((defaultSetLevel >= 0) && (defaultSetLevel<= 100))? defaultSetLevel:switchLevel                     
					}
					setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)                    
					// compile some stats for the dashboard                   
					min_open_level=(switchLevel.toInteger() < min_open_level)? switchLevel.toInteger() : min_open_level
					max_open_level=(switchLevel.toInteger() > max_open_level)? switchLevel.toInteger() : max_open_level
					total_level_vents=total_level_vents + switchLevel.toInteger()
					if (switchLevel > MIN_OPEN_LEVEL_IN_ZONE) {      // make sure that the vents are set to a minimum level, otherwise they are considered to be closed              
						ventSwitchesOnSet.add(ventSwitch)
						closedAllVentsInZone=false
						openVentsCount++    
					} else {
						closedVentsCount++                    
					}                    
				}                
			} /* end for ventSwitch */                             
		} /* end for rooms */
	} /* end for zones */

	if ((!fullyCloseVents) && (closedAllVentsInZone) && (nbVents)) {
		    	
		switchLevel= MIN_OPEN_LEVEL_IN_ZONE
		ventSwitchesOnSet=control_vent_switches_in_zone(indiceSchedule, switchLevel)		    
		traceEvent(settings.logFilter,"schedule ${scheduleName}, safeguards on: set all ventSwitches to ${switchLevel}% to avoid closing all of them",
			settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)       
	}    
	// Save the stats for the dashboard
    
	state?.openVentsCount=openVentsCount
	state?.closedVentsCount=closedVentsCount
	state?.maxOpenLevel=max_open_level
	state?.minOpenLevel=min_open_level
	state?.minTempInVents=min_temp_in_vents
	state?.maxTempInVents=max_temp_in_vents
	if (total_temp_in_vents) {
		state?.avgTempInVents= (total_temp_in_vents/nbVents).toFloat().round(1)
	}		        
	if (total_level_vents) {    
		state?.avgVentLevel= (total_level_vents/nbVents).toFloat().round(1)
	}		        
	if (total_temp_diff) {
		state?.avgTempDiff = (total_temp_diff/ nbRooms).toFloat().round(1)    
	}		        
	return ventSwitchesOnSet    
}

private def turn_off_all_other_vents(ventSwitchesOnSet) {
	def foundVentSwitch
	int nbClosedVents=0, totalVents=0
	float MAX_RATIO_CLOSED_VENTS=50 // not more than 50% of the smart vents should be closed at once
	def MIN_OPEN_LEVEL_SMALL=(minVentLevelOutZone!=null)?((minVentLevelOutZone>=0 && minVentLevelOutZone <100)?minVentLevelOutZone:25):25
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	def closedVentsSet=[]
	def fullyCloseVents = (fullyCloseVentsFlag) ?: false
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			if (ventSwitch != null) {
				totalVents++
				foundVentSwitch = ventSwitchesOnSet.find{it == ventSwitch}
				if (foundVentSwitch ==null) {
					nbClosedVents++ 
					closedVentsSet.add(ventSwitch)                        
				} else {
					def ventLevel= getCurrentVentLevel(ventSwitch)
					if ((ventLevel!=null) && (ventLevel <= MIN_OPEN_LEVEL_IN_ZONE)) { // below minimum level is considered as closed.
						nbClosedVents++ 
						closedVentsSet.add(ventSwitch)                        
						traceEvent(settings.logFilter,"turn_off_all_other_vents>${ventSwitch}'s level=${ventLevel} is lesser or equal to minimum level ${MIN_OPEN_LEVEL_IN_ZONE}",
						 	settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
 					}                        
				} /* else if foundSwitch==null */                    
			}   /* end if ventSwitch */                  
		}  /* end for ventSwitch */         
	} /* end for rooms */
	state?.totalClosedVents= nbClosedVents                     
	state?.openVentsCount= totalVents - nbClosedVents                     
	state?.totalVents=totalVents
	state?.ratioClosedVents =0   
	if (totalVents >0) {    
		float ratioClosedVents=((nbClosedVents/totalVents).toFloat()*100)
		state?.ratioClosedVents=ratioClosedVents.round(1)
		if ((!fullyCloseVents) && (ratioClosedVents > MAX_RATIO_CLOSED_VENTS)) {
			traceEvent(settings.logFilter,"ratio of closed vents is too high (${ratioClosedVents.round()}%), opening ${closedVentsSet} at minimum level of ${MIN_OPEN_LEVEL_SMALL}%",
				settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)            
		} /* end if ratioCloseVents is ratioClosedVents > MAX_RATIO_CLOSED_VENTS */            
		if (!fullyCloseVents) {
			closedVentsSet.each {
				setVentSwitchLevel(null, it, MIN_OPEN_LEVEL_SMALL)
			}                
			traceEvent(settings.logFilter,"turn_off_all_other_vents>closing ${closedVentsSet} using the safeguards as requested to create the desired zone(s)",
				settings.detailedNotif, GLOBAL_LOG_INFO)
		}            
		if (fullyCloseVents) {
			closedVentsSet.each {
				setVentSwitchLevel(null, it, 0)
			traceEvent(settings.logFilter,"turn_off_all_other_vents>closing ${closedVentsSet} totally as requested to create the desired zone(s)",settings.detailedNotif,
				GLOBAL_LOG_INFO)            
			}                
		}        
	} /* if totalVents >0) */        
}

private def open_all_vents() {
	int nbOpenVents=0
    
	// Turn on all vents        
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
			if (vent != null) {
				setVentSwitchLevel(null, vent, 100)
				nbOpenVents++                
			} /* end if vent != null */
		} /* end for vent switches */
	} /* end for rooms */
	state?.openVentsCount=nbOpenVents   
	state?.closedVentsCount=0    
	state?.totalClosedVents=0    
}

// @ventSwitch vent switch to be used to get temperature
private def getTemperatureInVent(ventSwitch) {
	def temp=null
	try {
		temp = ventSwitch.currentValue("temperature")       
	} catch (any) {
		traceEvent(settings.logFilter,"getTemperatureInVent>error, not able to get current Temperature from ${ventSwitch}",settings.detailedNotif, 
			GLOBAL_LOG_WARN,settings.detailedNotif)
	}    
	return temp    
}

// @ventSwitch	vent switch to be used to get level
private def getCurrentVentLevel(ventSwitch) {
	def ventLevel=null
    
	def currentSwitchValue=ventSwitch.currentValue("switch")    
	try {
		ventLevel=((currentSwitchValue) && (currentSwitchValue=='off'))? 0 : ventSwitch.currentValue("level")	
	} catch (any) {
		traceEvent(settings.logFilter,"getCurrentVentLevel>error,not able to get current vent level from ${ventSwitch}",settings.detailedNotif,
			GLOBAL_LOG_WARN,settings.detailedNotif)
	}    
	return ventLevel   
}
private def check_pressure_in_vent(ventSwitch, pressureSensor) {
	float pressureInVent, pressureBaseline
	float MAX_OFFSET_VENT_PRESSURE=124.54  // translate to 0.5 inches of water
    
	float max_pressure_offset=(settings.maxPressureOffsetInPa)?: MAX_OFFSET_VENT_PRESSURE 
	try {
		pressureInVent = ventSwitch.currentValue("pressure").toFloat()       
	} catch (any) {
		traceEvent(settings.logFilter,"check_pressure_in_vent>error, not able to get current pressure from ${ventSwitch}",settings.detailedNotif, 
			GLOBAL_LOG_WARN,settings.detailedNotif)
		return true       
	}    
	    
	try {
		pressureBaseline = pressureSensor.currentValue("pressure").toFloat()       
	} catch (any) {
		traceEvent(settings.logFilter,"check_pressure_in_vent>error, not able to get current pressure from ${pressureSensor}",settings.detailedNotif, 
			GLOBAL_LOG_WARN,settings.detailedNotif)
		return true       
	}    
	float current_pressure_offset =  (pressureInVent - pressureBaseline).round(2) 
	traceEvent(settings.logFilter,
			"check_pressure_in_vent>checking vent pressure=${pressureInVent} in ${ventSwitch}, pressure baseline=${pressureBaseline} based on ${pressureSensor}",
			settings.detailedNotif)

	if (current_pressure_offset > max_pressure_offset) {
		traceEvent(settings.logFilter,
			"check_pressure_in_vent>calculated pressure offset of ${current_pressure_offset} is greater than ${max_pressure_offset} in ${ventSwitch}: vent pressure=${pressureInVent}, pressure baseline=${pressureBaseline}, need to open the vent",
			settings.detailedNotif, GLOBAL_LOG_ERROR,true)
		return false            
    
	}    
	return true    
}
private def setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel=100) {
	def roomName
	int MAX_LEVEL_DELTA=5
	def key
    
	if (indiceRoom) {
		key = "roomName$indiceRoom"
		roomName = settings[key]
	}
	def currentLevel=getCurrentVentLevel(ventSwitch)?.toInteger()
	if ((currentLevel >= (switchLevel - MAX_LEVEL_DELTA)) && (currentLevel <= (switchLevel + MAX_LEVEL_DELTA))) {
		traceEvent(settings.logFilter, "setVentSwitchLevel>not changing the level to ${switchLevel} as currentLevel ($currentLevel) is within $MAX_LEVEL_DELTA % range",
			settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)           
		return true          
	}    
	try {
//		if (switchValue=='off' && switchLevel) ventSwitch.on()    
		ventSwitch.setLevel(switchLevel)
		if (roomName) {       
			traceEvent(settings.logFilter,"set ${ventSwitch} to level ${switchLevel} in room ${roomName} to reach desired temperature",settings.detailedNotif,
				GLOBAL_LOG_INFO)
		}            
	} catch (e) {
		if (switchLevel >0) {
			ventSwitch.off() // alternate off/on to clear potential obstruction        
			ventSwitch.on()        
			traceEvent(settings.logFilter, "setVentSwitchLevel>not able to set ${ventSwitch} to ${switchLevel} (exception $e), trying to turn it on",
				true, GLOBAL_LOG_WARN,settings.detailedNotif)  
			return false                
		} else {
			ventSwitch.on() // alternate on/off to clear potential obstruction             
			ventSwitch.off()        
			traceEvent(settings.logFilter, "setVentSwitchLevel>not able to set ${ventSwitch} to ${switchLevel} (exception $e), trying to turn it off",
				true, GLOBAL_LOG_WARN,settings.detailedNotif)           
			return false                
		}
	}    
	if (roomName) {    
		key = "pressureSensor$indiceRoom"
		def pressureSensor = settings[key]
		if (pressureSensor) {
			traceEvent(settings.logFilter,"setVentSwitchLevel>found pressureSensor ${pressureSensor} in room ${roomName}, about to check pressure offset vs. vent",settings.detailedNotif)
			if (!check_pressure_in_vent(ventSwitch, pressureSensor)) {
				ventSwitch.on()             
				return false        
			}
		}            
	}                    
	currentLevel=ventSwitch.currentValue("level")    
	def currentStatus=ventSwitch.currentValue("switch")    
	if (currentStatus=="obstructed") {
		traceEvent(settings.logFilter, "setVentSwitchLevel>error while trying to send setLevel command, switch ${ventSwitch} is obstructed",
			true, GLOBAL_LOG_WARN,settings.detailedNotif)            
		if (ventSwitch.hasCommand("clearObstruction")) {
			traceEvent(settings.logFilter, "setVentSwitchLevel>error while trying to send setLevel command, sending switch ${ventSwitch} clearObstruction command",
				true, GLOBAL_LOG_INFO,settings.detailedNotif)            
			ventSwitch.clearObstruction()
		}            
		ventSwitch.off() // alternate off/on to clear obstruction        
		ventSwitch.on()  
		return false   
	}    
	if ((currentLevel.toInteger() < (switchLevel - MAX_LEVEL_DELTA).toInteger()) ||  (currentLevel.toInteger() > (switchLevel + MAX_LEVEL_DELTA).toInteger())) {
		traceEvent(settings.logFilter, "setVentSwitchLevel>not able to set ${ventSwitch} to ${switchLevel}, maybe due to some command processing delay",
			settings.detailedNotif, GLOBAL_LOG_TRACE,settings.detailedNotif)           
		return false           
	}    
    
	return true    
}
private def control_vent_switches_in_zone(indiceSchedule, switchLevel=100) {

	def key = "includedZones$indiceSchedule"
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


			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
	                
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
    
	String currentDay = new Date().format("E", location.timeZone)
    

	traceEvent(settings.logFilter, "IsRightDayForChange>schedule $scheduleName, currentDayOfWeek=$currentDay vs. dayOfWeek from Schedule $indiceSchedule=$dayOfWeek", 
		settings.detailedNotif, GLOBAL_LOG_TRACE)  
	        
	// Check the condition under which we want this to run now
	// This set allows the most flexibility.
	if (dayOfWeek == 'All Week') {
		makeChange = true
	} else if ((dayOfWeek == 'Monday' || dayOfWeek == 'Monday to Friday') && currentDay == 'Mon') {
		makeChange = true
	} else if ((dayOfWeek == 'Tuesday' || dayOfWeek == 'Monday to Friday') && currentDay == 'Tue') {
		makeChange = true
	} else if ((dayOfWeek == 'Wednesday' || dayOfWeek == 'Monday to Friday') && currentDay == 'Wed') {
		makeChange = true
	} else if ((dayOfWeek == 'Thursday' || dayOfWeek == 'Monday to Friday') && currentDay == 'Thu') {
		makeChange = true
	} else if ((dayOfWeek == 'Friday' || dayOfWeek == 'Monday to Friday') &&  currentDay == 'Fri') {
		makeChange = true
	} else if ((dayOfWeek == 'Saturday' || dayOfWeek == 'Saturday & Sunday') && currentDay == 'Sat') {
		makeChange = true
	} else if ((dayOfWeek == 'Sunday' || dayOfWeek == 'Saturday & Sunday') && currentDay == 'Sun' ) {
		makeChange = true
	}

	traceEvent(settings.logFilter, "IsRightDayForChange>schedule $scheduleName, DayOfWeek=$dayOfWeek vs. currentDayOfWeek=$currentDay, makeChange=$makeChange",
		settings.detailedNotif, GLOBAL_LOG_TRACE)  
	return makeChange
    
}




private def get_MAX_SCHEDULES() {
	return 12
}


private def get_MAX_ZONES() {
	return 8
}

private def get_MAX_ROOMS() {
	return 16
}

private def get_MAX_VENTS() {
	return 5
}

def getCustomImagePath() {
	return "https://raw.githubusercontent.com/yracine/device-type.myecobee/master/icons/"
}    

private def getStandardImagePath() {
	return "http://cdn.device-icons.smartthings.com"
}


@Field int GLOBAL_LOG_ERROR=1
@Field int GLOBAL_LOG_WARN= 2
@Field int GLOBAL_LOG_INFO=3
@Field int GLOBAL_LOG_DEBUG=4
@Field int GLOBAL_LOG_TRACE=5


def traceEvent(filterLog, message, displayEvent=false, traceLevel=GLOBAL_LOG_DEBUG, sendMessage=false) {
	int filterLevel=(filterLog)?filterLog.toInteger():GLOBAL_LOG_WARN


	if (filterLevel >= traceLevel) {
		if (displayEvent) {    
			switch (traceLevel) {
				case GLOBAL_LOG_ERROR:
					log.error "${message}"
				break
				case GLOBAL_LOG_WARN:
					log.warn "${message}"
				break
				case GLOBAL_LOG_INFO:
					log.info "${message}"
				break
				case GLOBAL_LOG_TRACE:
					log.trace "${message}"
				break
				case GLOBAL_LOG_DEBUG:
				default:            
					log.debug "${message}"
				break
			}                
		}			                
		if (sendMessage) send (message,settings.askAlexaFlag) //send message only when true
	}        
}

private send(msg, askAlexa=false) {
	int MAX_EXCEPTION_MSG_SEND=5

	// will not send exception msg when the maximum number of send notifications has been reached
	if ((msg.contains("exception")) || (msg.contains("error"))) {
		state?.sendExceptionCount=state?.sendExceptionCount+1         
		traceEvent(settings.logFilter,"checking sendExceptionCount=${state?.sendExceptionCount} vs. max=${MAX_EXCEPTION_MSG_SEND}", detailedNotif)
		if (state?.sendExceptionCount >= MAX_EXCEPTION_MSG_SEND) {
			traceEvent(settings.logFilter,"send>reached $MAX_EXCEPTION_MSG_SEND exceptions, exiting", detailedNotif)
			return        
		}        
	}    
	def message = "${get_APP_NAME()}>${msg}"

	if (sendPushMessage == "Yes") {
		traceEvent(settings.logFilter,"contact book not enabled", false, GLOBAL_LOG_INFO)
		sendPush(message)
	}
	if (askAlexa) {
		def expiresInDays=(AskAlexaExpiresInDays)?:2    
		sendLocationEvent(
			name: "AskAlexaMsgQueue", 
			value: "${get_APP_NAME()}", 
			isStateChange: true, 
			descriptionText: msg, 
			data:[
				queues: listOfMQs,
				expires: (expiresInDays*24*60*60)  /* Expires after 2 days by default */
			]
		)
	} /* End if Ask Alexa notifications*/
		
	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, message)
	}
}

def getSTChildNamespace() {
	"acrosscable12814" //"fabricacross64399"
}

def getChildNamespace() {
	"acrosscable12814" //"yracine"
}
def getVirtualZoneChildName() {
	"My Virtual Zone V2"
}

private def get_APP_NAME() {
	return "Schedule Room Temp Control V2"
}