/**
 *  ScheduleTstatZones
 *
 *  Copyright 2015-2020 Yves Racine
 *  LinkedIn profile: ca.linkedin.com/pub/yves-racine-sc-a/0/406/4b/
 *
 *  Developer retains all right, title, copyright, and interest, including all copyright, patent rights, trade secret 
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
	description: "Enable Zoned Heating/Cooling for thermostats coupled with smart vents (optional) for better temp settings control throughout your home",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

def get_APP_VERSION() {return "9.1.1"}

preferences {

	page(name: "dashboardPage")
	page(name: "generalSetupPage")
	page(name: "roomsSetupPage")
	page(name: "roomVentSettingsSetup")
	page(name: "zonesSetupPage")
	page(name: "schedulesSetupPage")
	page(name: "configDisplayPage")
	page(name: "NotificationsPage")
	page(name: "roomsSetup")
	page(name: "zonesSetup")
	page(name: "schedulesSetup")
 	page(name: "fanSettingsSetup")
	page(name: "outdoorThresholdsSetup")
	page(name: "ventSettingsSetup")
	page(name: "alternativeCoolingSetup")
}


def dashboardPage() {
	state?.scale= getTemperatureScale()
	def scale= state?.scale
	dynamicPage(name: "dashboardPage", title: "Dashboard", uninstall: true, nextPage: generalSetupPage) {
		section("Tap Running Schedule(s) Config for latest info\nPress Next (upper right) for initial Setup") {
			if (roomsCount && zonesCount && schedulesCount) {
				paragraph image: "${getCustomImagePath()}office7.png", "ST hello mode: $location.mode" +
					"\nLast Running Schedule: ${state?.lastScheduleName}" +
					"\nIncludedZone(s): ${state?.activeZones}"
				def currentTemp = thermostat?.currentTemperature
				String mode =thermostat?.currentThermostatMode   
				def operatingState=thermostat?.currentThermostatOperatingState                
				def heatingSetpoint,coolingSetpoint
				switch (mode) { 
					case 'cool':
						coolingSetpoint = thermostat?.currentValue('coolingSetpoint')
					break                        
 					case 'auto': 
					case 'off':  
					case 'eco':  
						try {                    
							coolingSetpoint = thermostat.currentValue('coolingSetpoint')
						} catch (e) {
							traceEvent(settings.logFilter,"ConfigDisplayPage>not able to get coolingSetpoint from $thermostat, exception $e",settings.detailedNotif,
								GLOBAL_LOG_WARN,settings.detailedNotif)                        
						} 
						coolingSetpoint=  (coolingSetpoint)? coolingSetpoint: (scale=='C')?23:73                        
                        
					case 'heat':
					case 'emergency heat':
					case 'auto': 
					case 'eco':
						try {                    
	 						heatingSetpoint = thermostat?.currentValue('heatingSetpoint')
						} catch (e) {
							traceEvent(settings.logFilter,"dashboardPage>not able to get heatingSetpoint from $thermostat,exception $e",settings.detailedNotif)                      
						}                        
						heatingSetpoint=  (heatingSetpoint)? heatingSetpoint: (scale=='C')?21:72                        
					break
					default:
						log.warn "dashboardPage>invalid mode $mode"
					break                        
                    
				}        
				def dParagraph = "TstatMode: $mode" +
						"\nTstatOperatingState: $operatingState" +
						"\nTstatCurrentTemp: ${currentTemp}$scale" 
				if (coolingSetpoint)  { 
					 dParagraph = dParagraph + "\nCoolingSetpoint: ${coolingSetpoint}$scale"
				}     
				if (heatingSetpoint)  { 
					dParagraph = dParagraph + "\nHeatingSetpoint: ${heatingSetpoint}$scale" 
				}     
				if (state?.zoned_min_indoor_temp != null) {
					dParagraph = dParagraph + "\n\nZonedRoomsMinTemp: ${state?.zoned_min_indoor_temp}$scale" 
					dParagraph = dParagraph + "\nZonedRoomsMaxTemp: ${state?.zoned_max_indoor_temp}$scale" 
					dParagraph = dParagraph + "\nZonedRoomsMedianTemp: ${state?.zoned_med_indoor_temp}$scale" 
					dParagraph = dParagraph + "\nZonedRoomsAverageTemp: ${state?.zoned_avg_indoor_temp}$scale" 
					dParagraph = dParagraph + "\nZonedRoomsTempCount: ${state?.zoned_rooms_count}" 
				}				                
                
				if (state?.avgTempDiff)  { 
					dParagraph =  dParagraph + "\nAvgTempDiffInZone: ${state?.avgTempDiff}$scale\n"                   
				}
				paragraph image: "${getCustomImagePath()}home1.png", dParagraph 
				if ((state?.closedVentsCount) || (state?.openVentsCount)) {
					String dPar = "    ** SMART VENTS SUMMARY **\n              For Active Zone(s)\n\n" 
					dPar = dPar + "OpenVentsCount: ${state?.openVentsCount}" +                    
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
			paragraph image:"${getCustomImagePath()}ecohouse.jpg","${get_APP_NAME()}, the smartapp that enables Zoned Heating/Cooling at selected thermostat(s) coupled with smart vents (optional) for better temp settings control throughout your home"
			paragraph "Version ${get_APP_VERSION()}" 
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.me/ecomatiqhomes",
					title:"Paypal donation..."
			paragraph "Copyright@2015-2020 Yves Racine"
				href url:"https://www.maisonsecomatiq.com/#!home/mainPage", style:"embedded", required:false, title:"More information..."  
 				description: "https://www.maisonsecomatiq.com/#!home/mainPage"
		} /* end section about */
	}
}

def generalSetupPage() {

	dynamicPage(name: "generalSetupPage", title: "General Setup",nextPage: roomsSetupPage,uninstall: false,refreshAfterSelection:true) {

		section ("") {
			paragraph "Warning: don't hit the back button, use the links to navigate back to a page\n"
		}
		section(image: "${getCustomImagePath()}home1.png", "Main thermostat at home (used for temp/vent adjustment)") {
			input (name:"thermostat", type: "capability.thermostat", title: "Which main thermostat?")
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
		if (thermostat) {
			section {
				href(name: "toRoomPage", title: "Rooms Setup", page: "roomsSetupPage", description: "Tap to configure", image: "${getCustomImagePath()}room.png")
				href(name: "toZonePage", title: "Zones Setup", page: "zonesSetupPage",  description: "Tap to configure",image: "${getCustomImagePath()}zoning.jpg")
				href(name: "toSchedulePage", title: "Schedules Setup", page: "schedulesSetupPage",  description: "Tap to configure",image: "${getCustomImagePath()}office7.png")
				href(name: "toNotificationsPage", title: "Notification & Options Setup", page: "NotificationsPage",  description: "Tap to configure", image: "${getCustomImagePath()}notification.png")
			}                
		}
		section("Set your main thermostat to [Away,Present] based on all Room Motion Sensors [default=false] ") {
			input (name:"setAwayOrPresentFlag", title: "Set Main thermostat to [Away,Present]?", type:"bool",required:false)
		}
		section("Outdoor temp Sensor used for adjustment or alternative cooling [optional]") {
			input (name:"outTempSensor", type:"capability.temperatureMeasurement", required: false,
				description:"Optional")
		}
		section("Vent Control settings [optional]") {
			input (name:"setVentSettingsFlag", title: "Set Vent Settings - Enable Vent Control  [optional, default=false]?", type:"bool",
				description:"optional",required:false)
			input (name:"delayInSecForVentSettings", title: "Delay in seconds after HVAC is running (heating, cooling, fan only) for vent settings (10..120)?", type:"number",
				range: "10..120", description:"no explicit delay",required:false)
		}
		section("Enable mode/temp adjustment based on outdoor temp sensor [optional, default=false]") {
			input (name:"setAdjustmentOutdoorTempFlag", title: "Enable mode/temp adjustment based on outdoor sensor?", type:"bool",required:false)
		}
		section("Enable temp adjustment at main thermostat based on indoor temp/motion sensor(s) [optional, default=false]") {
			input (name:"setAdjustmentTempFlag", title: "Enable temp adjustment based on collected temps at indoor sensor(s)?", type:"bool",
				description:"optional",required:false)
			input (name:"adjustmentTempMethod", title: "Calculated method to be used for setpoints adjustment", type:"enum",
				description:"optional [default=calculated avg of all sensors' temps]",required:false, options:["avg", "med", "min","max", "heat min/cool max"], 
				default: "avg")
		}
		section("Enable fan adjustment based on indoor/outdoor temp sensors [optional, default=false]") {
			input (name:"setAdjustmentFanFlag", title: "Enable fan adjustment set in schedules based on sensors?", type:"bool",required:false)
		}
		section("Enable Contact Sensors to be used for vent/temp adjustments [optional, default=false]") {
			input (name:"setVentAdjustmentContactFlag", title: "Enable vent adjustment set in schedules based on contact sensors?", type:"bool",
				description:" if true and contact open=>vent(s) closed in schedules",required:false)
			input (name:"setTempAdjustmentContactFlag", title: "Enable temp adjustment set in schedules based on contact sensors?", type:"bool",
				description:"optional, true and contact open=>no temp reading in schedules",required:false)
		}
        
		section("Efficient Use of evaporative cooler/Big Fan/Damper Switch(es) for cooling/air circulation based on indoor/outdoor sensor readings [optional]") {
			input (name:"evaporativeCoolerSwitch", title: "Evaporative Cooler/Big Fan/Damper Switch(es) to be turned on/off?",
				type:"capability.switch", required: false, multiple:true, description: "Optional")
			input (name:"doNotUseHumTableFlag", title: "For alternative cooling, use it only when the outdoor temp is below the lessCoolThreshold in schedule [default=use of ideal humidity/temp table]?", 
				type:"bool",description:"optional",required:false)
			input (name:"switchToTurnOnWhenFanDiffFlag", title: "Turn on switch(es) when indoor temp differential (set in Schedules) is too big in the zones?", 
				type:"bool",description:"optional",required:false)
 		}
		section("Disable or Modify the safeguards [default=some safeguards are implemented to avoid damaging your HVAC by closing too many vents]") {
			input (name:"fullyCloseVentsFlag", title: "Bypass all safeguards & allow closing the vents totally?", type:"bool",required:false)
			input (name:"minVentLevelInZone", title: "Safeguard's Minimum Vent Level in Zone", type:"number", required: false, description: "[default=10%]")
			input (name:"minVentLevelOutZone", title: "Safeguard's Minimum Vent Level Outside of the Zone", type:"number", required: false, description: "[default=25%]")
			input (name:"maxVentTemp", title: "Safeguard's Maximum Vent Temp", type:"number", required: false, description: "[default= 131F/55C]")
			input (name:"minVentTemp", title: "Safeguard's Minimum Vent Temp", type:"number", required: false, description: "[default= 45F/7C]")
			input (name:"maxPressureOffsetInPa", title: "Safeguard's Max Vent Pressure Offset with room's pressure baseline [unit: Pa]", type:"decimal", required: false, description: "[default=124.54Pa/0.5'' of water]")
		}  
		section("Room Thermostat Options for heaters/coolers/eTRVs or slave thermostats [optional]") {
			input (name:"turnOffRoomTstatsFlag", title: "Set room tstats to eco/away/off mode when outside scheduled zones(s) or rooms are not occupied", type:"bool",required:false) 
			input (name:"useRoomTstatSetpointFlag", title: "use of the room tstat's setpoints to adjust zones", type:"bool",required:false) 
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

	dynamicPage(name: "roomsSetup", title: "Rooms Setup",  uninstall: false, nextPage: zonesSetupPage) {

		section("Room ${indiceRoom} Setup") {
			input "roomName${indiceRoom}", "text" , title: "Room Name",image: "${getCustomImagePath()}room.png"
		}
		section("Room ${indiceRoom}-Temp Sensor [optional]") {
			input image: "${getCustomImagePath()}IndoorTempSensor.png", "tempSensor${indiceRoom}", title: "Temp sensor for better temp adjustment", "capability.temperatureMeasurement", 
				required: false, description: "Optional"

		}
		section("Room ${indiceRoom}-Vent Settings for the room [optional]") {
			href(name: "toRoomVentSettingsSetup", page: "roomVentSettingsSetup", params: [indiceRoom: indiceRoom],required:false,  description: "Optional",
				title: roomVentSettingsHrefTitle(indiceRoom), image: "${getCustomImagePath()}ventopen.png" ) 
		}
		section("Room ${indiceRoom}-Contact Sensors [optional]") {
			input image: "${getCustomImagePath()}contactSensor.png", "contactSensor${indiceRoom}", title: "Contact sensor(s) for better vent/temp adjustment", "capability.contactSensor", 
				required: false, multiple:true, description: "Optional, contact open=>vent is closed"
			input "contactClosedLogicFlag${indiceRoom}", title: "Inverse temp/vent logic,contact open=>vent is open [default=false]", "bool",  
				required: false, description: "Optional"
		}
        
		section("Room ${indiceRoom}-Room Thermostat for a fireplace, baseboards, eTRV,window AC, etc.  [optional]") {
			input image: "${getCustomImagePath()}home1.png", "roomTstat${indiceRoom}", title: "Thermostat for better room comfort", "capability.thermostatMode", 
				required: false, description: "Optional"
		}
		section("Room ${indiceRoom}-Flair's Puck (or other pressure sensor) for better temp & HVAC static pressure control [optional]") {
			input image: "${getCustomImagePath()}pressure.png", "pressureSensor${indiceRoom}", title: "Puck or pressure sensor to be used", "capability.acrosscable12814.flairPressure", //capability.sensor
				required: false, description: "Optional"
		}
		section("Room ${indiceRoom}-Motion Detection parameters [optional]") {
			input image: "${getCustomImagePath()}MotionSensor.png","motionSensor${indiceRoom}", title: "Motion sensor (if any) to detect if room is occupied", "capability.motionSensor", 
				required: false, description: "Optional"
			input "needOccupiedFlag${indiceRoom}", title: "Will do temp/vent adjustement only when Occupied [default=false]", "bool",  
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

def roomVentSettingsSetup(params) {
	def indiceRoom=1
	// Assign params to indiceSchedule.  Sometimes parameters are double nested.
	if (params?.indiceRoom || params?.params?.indiceRoom) {

		if (params.indiceRoom) {
			indiceRoom = params.indiceRoom
		} else {
			indiceRoom = params.params.indiceRoom
		}
	}    
	indiceRoom=indiceRoom.intValue()
    
	dynamicPage(name: "roomVentSettingsSetup", title: "Vent Settings for Room " + settings."roomName${indiceRoom}", uninstall: false, 
		nextPage: "roomsSetupPage") {
		section("Room ${indiceRoom}-Vent Settings for the Room [optional]") {
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				input image: "${getCustomImagePath()}ventclosed.png","ventSwitch${j}${indiceRoom}", title: "Vent switch #${j} in room", "capability.acrosscable12814.flairPressure", //capability.switch
					required: false, description: "Optional"
				input "ventLevel${j}${indiceRoom}", title: "set vent  #${j}'s level in room [optional, range 0-100]", "number", range: "0..100",
						required: false, description: "blank:calculated by smartapp"
			}  

		}
		section {
			href(name: "toRoomPage${indiceRoom}", title: "Back to Room ${indiceRoom} - Setup Page", page: "roomsSetup", params: [indiceRoom: indiceRoom])
		}
	}    
}   


def roomVentSettingsHrefTitle(i) {
	def title = "Vent Settings for Room ${i}"
	return title
}

def configDisplayPage() {
	def fullyCloseVents = (settings.fullyCloseVentsFlag) ?: false
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
	String mode = thermostat?.currentThermostatMode
	def operatingState=thermostat.currentThermostatOperatingState                
	float desiredTemp, total_temp_in_vents=0, median
	def key
	def scale = (state?.scale)?: getTemperatureScale()
	def currTime = now()	
	boolean foundSchedule=false
	String bypassSafeguardsString= (fullyCloseVents)?'true':'false'                            
	String setAwayOrPresentString= (setAwayOrPresentFlag)?'true':'false'                            
	String setAdjustmentTempString= (setAdjustmentTempFlag)?'true':'false'                            
	String setAdjustmentOutdoorTempString= (setAdjustmentOutdoorTempFlag)?'true':'false'                            
	String setAdjustmentFanString= (setAdjustmentFanFlag)?'true':'false'                            
	String setVentSettingsString = (setVentSettingsFlag)?'true':'false'    
	int nbClosedVents=0, nbOpenVents=0, totalVents=0,  nbRooms=0
	int min_open_level=100, max_open_level=0,total_level_vents=0       
	float min_temp_in_vents=200, max_temp_in_vents=0, total_temp_diff=0, target_temp=0 
	float currentTempAtTstat = thermostat?.currentTemperature.toFloat().round(1)
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	def MIN_OPEN_LEVEL_OUT_ZONE=(minVentLevelOutZone!=null)?((minVentLevelOutZone>=0 && minVentLevelOutZone <100)?minVentLevelOutZone:25):25
	def MAX_TEMP_VENT_SWITCH = (settings.maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (settings.minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch
	def MAX_PRESSURE_OFFSET = (settings.maxPressureOffsetInPa)?:124.54     //Translate to  0.5 inches of water in Pa

	traceEvent(settings.logFilter,"configDisplayPage>About to display Running Schedule(s) Configuration",settings.detailedNotif)
	dynamicPage(name: "configDisplayPage", title: "Running Schedule(s) Config", nextPage: generalSetupPage,submitOnChange: true) {
		section {
			href(name: "toDashboardPage", title: "Back to Dashboard Page", page: "dashboardPage")
		}
		section("General") {
			def heatingSetpoint,coolingSetpoint
			switch (mode) { 
				case 'cool':
					coolingSetpoint = thermostat.currentValue('coolingSetpoint')
					target_temp=coolingSetpoint.toFloat()                       
				break                    
	 			case 'auto': 
				case 'off':  
				case 'eco':  
					try {                    
						coolingSetpoint = thermostat.currentValue('coolingSetpoint')
					} catch (e) {
						traceEvent(settings.logFilter,"ConfigDisplayPage>not able to get coolingSetpoint from $thermostat, exception $e",settings.detailedNotif,
							GLOBAL_LOG_WARN,settings.detailedNotif)                        
					}   
					coolingSetpoint=  (coolingSetpoint)? coolingSetpoint: (scale=='C')?23:73                        
                    
				case 'heat':
				case 'emergency heat':
				case 'auto': 
				case 'eco':  
					try {                    
	 					heatingSetpoint = thermostat?.currentValue('heatingSetpoint')
					} catch (e) {
						traceEvent(settings.logFilter,"ConfigDisplayPage>not able to get heatingSetpoint from $thermostat, exception $e",settings.detailedNotif,
							GLOBAL_LOG_WARN,settings.detailedNotif)                        
					}   
					heatingSetpoint=  (heatingSetpoint)? heatingSetpoint: (scale=='C')?21:72                        
					if (mode in ['auto', 'off', 'eco']) {
						median= ((coolingSetpoint + heatingSetpoint)/2).toFloat().round(1)
						if (currentTempAtTstat > median) {
							target_temp =coolingSetpoint.toFloat()                   
						} else {
							target_temp =heatingSetpoint.toFloat()                   
						}                        
					} else {                         
						target_temp =heatingSetpoint.toFloat()                   
					}   
				break
				default:
					log.warn "ConfigDisplayPage>invalid mode $mode"
				break                        
                
			}      
            
			traceEvent(settings.logFilter,"configDisplayPage>About to loop thru schedules, target_temp=$target_temp,thermostatMode=$mode",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)
			def detailedNotifString=(settings.detailedNotif)?'true':'false'			            
			def askAlexaString=(settings.askAlexaFlag)?'true':'false'			            
			def setVentAdjustmentContactString=(settings.setVentAdjustmentContactFlag)?'true':'false'
			def setTempAdjustmentContactString=(settings.setTempAdjustmentContactFlag)?'true':'false'
			def setAdjustmentTempMethod=(settings.adjustmentTempMethod)?:"avg"
			def turnOffRoomTstatsString=(settings.turnOffRoomTstatsFlag)?'true':'false'			            
			def ventDelayInSecsString=(settings.delayInSecForVentSettings)?:0		            
			paragraph image: "${getCustomImagePath()}notification.png", "Notifications" 
			paragraph "  >Detailed Notification: $detailedNotifString" +
					"\n  >AskAlexa Notifications: $askAlexaString"             
			paragraph image: "${getCustomImagePath()}home1.png", "ST hello mode: $location.mode" +
					"\nTstatMode: $mode\nTstatOperatingState: $operatingState" +
					"\nTstatCurrentTemp: ${currentTempAtTstat}$scale" 
			if (coolingSetpoint)  { 
				paragraph " >TstatCoolingSetpoint: ${coolingSetpoint}$scale"
			}                        
			if (heatingSetpoint)  { 
				paragraph " >TstatHeatingSetpoint: ${heatingSetpoint}$scale"
			}
            
            
			paragraph " >SetVentSettings: ${setVentSettingsString}" +
					"\n >VentLevelSetDelay: ${ventDelayInSecsString} second(s)" +
					"\n >SetAwayOrPresentFlag: ${setAwayOrPresentString}" +
					"\n >SetAwayOrPresentNow: ${state?.setPresentOrAway}" + 
					"\n >AdjustTstatVs.indoorAvgTemp: ${setAdjustmentTempString}" +
					"\n >AdjustTstatTempCalcMethod: ${setAdjustmentTempMethod}" +
					"\n >AdjustTempBasedOnContact: ${setTempAdjustmentContactString}" +
					"\n >AdjustVentBasedOnContact: ${setVentAdjustmentContactString}" +
					"\n >TurnOffRoomTstatsOutZone: ${turnOffRoomTstatsString}" 

			paragraph image: "${getCustomImagePath()}safeguards.jpg","Safeguards"
 			paragraph "  >BypassSafeguards: ${bypassSafeguardsString}" +
					"\n  >MinVentLevelInZone: ${MIN_OPEN_LEVEL_IN_ZONE}%" +
					"\n  >MinVentLevelOutZone: ${MIN_OPEN_LEVEL_OUT_ZONE}%" +
					"\n  >MinVentTemp: ${MIN_TEMP_VENT_SWITCH}${scale}" +
					"\n  >MaxVentTemp: ${MAX_TEMP_VENT_SWITCH}${scale}" +
					"\n  >MaxPressureOffset: ${MAX_PRESSURE_OFFSET} Pa" 
                    
			if (outTempSensor) {
				paragraph image: "${getCustomImagePath()}WeatherStation.jpg", "OutdoorTempSensor: $outTempSensor" 
				paragraph " >AdjustTstatVs.OutdoorTemp: ${setAdjustmentOutdoorTempString}"  +                         
					"\n >AdjustFanVs.OutdoorTemp: ${setAdjustmentFanString}"                            
			}				
		}
		for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
        
			key = "selectedMode$i"
			def selectedModes = settings[key]
			key = "scheduleName$i"
			def scheduleName = settings[key]
			traceEvent(settings.logFilter,"configDisplayPage>looping thru schedules, now at $scheduleName",settings.detailedNotif)
			boolean foundMode=selectedModes.find{it == (location.currentMode as String)} 
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
				traceEvent(settings.logFilter,"configDisplayPage>schedule ${scheduleName}, subtracted - 1 day, new startTime=${startTimeToday.time}",settings.detailedNotif)
			}            
			if ((currTime > endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
				endTimeToday = endTimeToday +1        
				traceEvent(settings.logFilter,"configDisplayPage>schedule ${scheduleName}, added + 1 day, new endTime=${endTimeToday.time}",settings.detailedNotif)
			}        
			String startInLocalTime = startTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
			String endInLocalTime = endTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
			traceEvent(settings.logFilter,"configDisplayPage>$scheduleName is good to go..",settings.detailedNotif)
			if ((currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange(i))) {
                
				key = "givenClimate${i}"
				def climate = settings[key]
                
				key = "includedZones$i"
				def zones = settings[key]
				key = "heatModeThreshold${i}"
				def heatModeThreshold=settings[key]                
				key = "coolModeThreshold${i}"
				def coolModeThreshold=settings[key]                
				key = "moreHeatThreshold$i"
				def moreHeatThreshold= settings[key]
				key = "moreCoolThreshold$i"
				def moreCoolThreshold= settings[key]
				key = "givenMaxTempDiff${i}"
				def givenMaxTempDiff= (settings[key]!=null) ?settings[key]: (scale=='C')? 2: 5 // 2C/5F temp differential is applied by default
				key = "fanMode${i}"
				def fanMode = settings[key]
				key ="moreFanThreshold${i}"
				def moreFanThreshold = settings[key]
				key = "fanModeForThresholdOnlyFlag${i}"                
				def fanModeForThresholdOnlyString = (settings[key])?'true':'false'
				key = "fanDiffCalcExcludeMasterFlag${i}"
				String fanDiffCalcExcludeMasterString = (settings[key])?'true':'false'
				key = "setRoomThermostatsOnlyFlag${i}"
				String setRoomThermostatsOnlyString = (settings[key])?'true':'false'
				key = "useMasterTstatSetpointsFlag${i}"
				String useMasterTstatSetpointsString = (settings[key])?'true':'false'
				key = "desiredCoolTemp${i}"
				def desiredCoolTemp = (settings[key])?: ((scale=='C') ? 23:75)
				key = "desiredHeatTemp${i}"
				def desiredHeatTemp = (settings[key])?: ((scale=='C') ? 21:72)
				key = "adjustVentsEveryCycleFlag${i}"
				def adjustVentsEveryCycleString = (settings[key])?'true':'false'
				key = "setVentLevel${i}"
				def setLevel = settings[key]
				key = "resetLevelOverrideFlag${i}"
				def resetLevelOverrideString=(settings[key])?'true':'false'
				key = "useEvaporativeCoolerFlag${i}"                
				def useAlternativeCoolingString = (settings[key])?'true':'false'
				key = "useAlternativeWhenCoolingFlag${i}"                
				def useAlternativeWhenCoolingString = (settings[key])?'true':'false'
				key = "openVentsFanOnlyFlag${i}"                
				def openVentsWhenFanOnlyString = (settings[key])?'true':'false'                
				def doNotUseHumTableString = (doNotUseHumTableFlag)?'false':'true'
				key = "inactiveScheduleFlag${i}"                
				def activeScheduleString = (settings[key])?'false':'true'
				key = "ecoWhenAway${i}"
				def ecoWhenAwayString = (settings[key]) ?'true':'false' 
				if (activeScheduleString=='true') {
					foundSchedule=true				                
				}                    
				section("Running Schedule(s)") {
					paragraph image: "${getCustomImagePath()}office7.png","Schedule $scheduleName"  +
						"\n >ActiveSchedule: $activeScheduleString" +                    
						"\n >StartTime: $startInLocalTime" +                    
						"\n >EndTime: $endInLocalTime"  +                
						"\n >SetToEcoWhenAway: $ecoWhenAwayString"                  
                    
					if (climate) {                    
						paragraph " >EcobeeProgramSet: $climate" 
					} else {
						if (desiredCoolTemp) {
							paragraph " >DesiredCoolTemp: ${desiredCoolTemp}$scale"
						}    
						if (desiredHeatTemp) {
							paragraph " >DesiredHeatTemp: ${desiredHeatTemp}$scale"
						}    
					}                    
					if (fanMode) {
						paragraph " >SetFanMode: $fanMode"
					}                    
					if (moreFanThreshold) {
						paragraph " >MoreFanThreshold: ${moreFanThreshold}$scale"
					}                    
					if (fanModeForThresholdOnlyString=='true') {
						paragraph " >AdjustFanWhenThresholdMetOnly: $fanModeForThresholdOnlyString"
					}
					if (fanDiffCalcExcludeMasterString=='true') {
						paragraph " >FanDiffCalcExcludeMasterTemp: $fanDiffCalcExcludeMasterString"
					}
					if (heatModeThreshold != null) {
						paragraph " >HeatModeThreshold: ${heatModeThreshold}$scale"
					}                    
					if (coolModeThreshold != null) {
						paragraph " >CoolModeThreshold: ${coolModeThreshold}$scale"
					}                    
					if (moreHeatThreshold != null) {
						paragraph " >MoreHeatThreshold: ${moreHeatThreshold}$scale"
					}                    
					if (moreCoolThreshold != null) {
						paragraph " >MoreCoolThreshold: ${moreCoolThreshold}$scale"
					}                    
					if (setRoomThermostatsOnlyString=='true') {
						paragraph " >SetRoomThermostatOnly: $setRoomThermostatsOnlyString"
						if (desiredCoolTemp) {
							paragraph " >DesiredCoolTempForRoomTstat: ${desiredCoolTemp}$scale"
						}    
						if (desiredHeatTemp) {
							paragraph " >DesiredHeatTempForRoomTstat: ${desiredHeatTemp}$scale"
						}    
					}                    
					if (useMasterTstatSetpointsString=='true') {
						paragraph " >UseMasterTstatSetpoints: $useMasterTstatSetpointsString"
					}                    
					if (setLevel) {
						paragraph " >DefaultSetLevelForAllVentsInZone(s): ${setLevel}%"
					}                        
					paragraph " >BypassSetLevelOverrideinZone(s): ${resetLevelOverrideString}" +
						"\n >AdjustVentsEveryCycle: $adjustVentsEveryCycleString" + 
						"\n >OpenVentsWhenFanOnly: $openVentsWhenFanOnlyString" +
						"\n >MaxTempAdjustment: ${givenMaxTempDiff}${scale}"                        
                        
					paragraph image: "${getCustomImagePath()}altenergy.jpg", "UseAlternativeCooling: $useAlternativeCoolingString"
        
					if (useAlternativeCoolingString=='true') {                    
						key = "diffDesiredTemp${i}"
						def diffDesiredTemp = (settings[key])?: (scale=='F')?5:2             
						key = "diffToBeUsedFlag${i}"
						def diffToBeUsedString = (settings[key])? 'true':'false'
						paragraph " >UseAlternativeWhenCooling: $useAlternativeWhenCoolingString" +
						"\n >UseHumidityTempTable: $doNotUseHumTableString" +
						"\n >DiffToBeUsedForCooling: $diffToBeUsedString" +
						"\n >DiffToDesiredTemp: $diffDesiredTemp${scale}"
					}                    
					if (selectedModes) {                    
						paragraph " >STHelloModes: $selectedModes"
					}                        
					paragraph " >Includes: $zones" 
				}
				state?.activeZones = zones // save the zones for the dashboard                
				for (zone in zones) {
					float targetTemp=target_temp                
					def zoneDetails=zone.split(':')
					def indiceZone = zoneDetails[0]
					def zoneName = zoneDetails[1]
					key = "includedRooms$indiceZone"
					def rooms = settings[key]
					key  = "desiredHeatDeltaTemp$indiceZone"
					def desiredHeatDelta =  (state?."desiredHeatTempDelta$indiceZone")? state?."desiredHeatTempDelta$indiceZone".toFloat(): settings[key]
					key  = "desiredCoolDeltaTemp$indiceZone"
					def desiredCoolDelta = (state?."desiredCoolTempDelta$indiceZone")? state?."desiredCoolTempDelta$indiceZone".toFloat(): settings[key]
					key = "inactiveZoneFlag$indiceZone"
					boolean inactiveZone=(state?."inactiveZone$indiceZone"!= null)? state?."inactiveZone$indiceZone".toBoolean() :settings[key]
					def activeZoneString = inactiveZone ?'false':'true'                
 					section("Zone(s) in Schedule $scheduleName") {
						paragraph image: "${getCustomImagePath()}zoning.jpg", "Zone $zoneName" +
							"\n >ActiveZone: $activeZoneString" 
						paragraph " >Includes: $rooms" 
						if ((desiredCoolDelta) && ((mode in ['cool']) ||
							((mode in ['auto','off', 'eco']) && (currentTempAtTstat > median)))) {                         
							paragraph " >DesiredCoolDeltaSP: ${desiredCoolDelta}$scale" 
							targetTemp = targetTemp+ desiredCoolDelta                            
						} else if ((desiredHeatDelta) && (mode in ['heat','emergency heat', 'off', 'auto', 'eco'])) {                         
							paragraph " >DesiredHeatDeltaSP: ${desiredHeatDelta}$scale"  
							targetTemp = targetTemp + desiredHeatDelta                            
						}                            
					}
					for (room in rooms) {
						def roomDetails=room.split(':')
						def indiceRoom = roomDetails[0]
						def roomName = roomDetails[1]
						if (!roomName || roomName=='null')  {
							continue                        
						}
                        
						key = "needOccupiedFlag$indiceRoom"
						def needOccupied = (settings[key]) ?: false
						traceEvent(settings.logFilter,"configDisplayPage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
							settings.detailedNotif)                        
						key = "motionSensor${indiceRoom}"
						def motionSensor = settings[key] 
						key = "tempSensor${indiceRoom}"
						def tempSensor = settings[key]
						key = "contactSensor${indiceRoom}"
						def contactSensor = settings[key]
						key = "roomTstat${indiceRoom}"
						def roomTstat = settings[key] 
						def tempAtSensor =getSensorTempForAverage(indiceRoom)			
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
								float temp_diff = (tempAtSensor- targetTemp).toFloat().round(1) 
								paragraph " >CurrentTempInRoom: ${tempAtSensor}$scale" +	
									"\n >TempOffsetVs.TargetTemp: ${temp_diff.round(1)}$scale"
								total_temp_diff = total_temp_diff + temp_diff    
							}   
							if (contactSensor) {      
								key = "contactClosedLogicFlag$indiceRoom" 
								def contactClosedLogicString=(settings[key])?'true':'false'                            
								if (any_contact_open(contactSensor)) {
									paragraph image: "${getCustomImagePath()}contactSensor.png", " ContactSensor: $contactSensor" + 
										"\n >ContactState: open" + 
										"\n >ContactOpenForOpenVent: $contactClosedLogicString" 
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
                            
							if (roomTstat) {      
								paragraph image: "${getCustomImagePath()}home1.png", " RoomTstat: $roomTstat" 
							}                            
							if (motionSensor) {      
								def countActiveMotion=isRoomOccupied(motionSensor, indiceRoom)
								String needOccupiedString= (needOccupied)?'true':'false'
								if (!needOccupied) {                                
									paragraph " >MotionSensor: $motionSensor" +
										"\n  ->NeedToBeOccupied: ${needOccupiedString}" 
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
                                    
									paragraph "  >IsRoomOccupiedNow: ${isRoomOccupiedString}" + 
										"\n  >NeedToBeOccupied: ${needOccupiedString}" + 
										"\n  >OccupiedThreshold: ${thresholdString} minutes"+ 
										"\n  >MotionCountNeeded: ${occupiedMotionOccNeeded}" + 
										"\n  >OccupiedMotionCounter: ${countActiveMotion}" +
										"\n  >LastMotionTime: ${lastMotionInLocalTime}"
								}
							}                                
							paragraph "** VENTS in $roomName **" 
							for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
								key = "ventSwitch${j}$indiceRoom"
								def ventSwitch = settings[key]
								if (ventSwitch != null) {
									def temp_in_vent=getTemperatureInVent(ventSwitch)                                
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
							} /* end for ventSwitch */                             
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
		if (total_temp_diff) {
			state?.avgTempDiff = (total_temp_diff/ nbRooms).toFloat().round(1)    
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

	dynamicPage(name: "zonesSetupPage", title: "Zones Setup",  uninstall: false,nextPage: schedulesSetupPage) {
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
	dynamicPage(name: "zonesSetup", title: "Zones Setup", uninstall: false) {
		section("Zone ${indiceZone} Setup") {
			input "zoneName${indiceZone}", "text", title: "Zone Name", 
				defaultValue:settings."zoneName${indiceZone}"
		}
		section("Zone ${indiceZone}-Included rooms") {
			input (name:"includedRooms${indiceZone}", title: "Rooms included in the zone", type: "enum",
				options: rooms,
				multiple: true,
				defaultValue:settings."includedRooms${indiceZone}")
		}
		section("Zone ${indiceZone}-Dynamic Cool Temp Adjustment for Vents/Zone Tstats based on the coolSP in Schedule - to make the zone cooler or warmer") {
			input (name:"desiredCoolDeltaTemp${indiceZone}", type:"decimal", range: "*..*", title: "Dynamic Cool Temp Adjustment for the zone [default = +/-0F or +/-0C]", 
				required: false, defaultValue:settings."desiredCoolDeltaTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Dynamic Heat Temp Adjustment for Vents/Zone Tstats based on the heatSP in Schedule- to make the zone cooler or warmer") {
			input (name:"desiredHeatDeltaTemp${indiceZone}", type:"decimal", range: "*..*", title: "Dynamic Heat Temp Adjustment for the zone [default = +/-0F or +/-0C]", 
				required: false, defaultValue:settings."desiredHeatDeltaTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Create a virtual device for controlling the zone?") {
			input "virtualZoneFlag${indiceZone}", title: "Virtual Device for the zone?", "bool", 
				required: false, description: "false"
		}
        
		section("Zone ${indiceZone}-Make this zone inactive in all schedules?") {
			input "inactiveZoneFlag${indiceZone}", title: "Inactive?", "bool", 
				required: false, description: "false",defaultValue:settings."inactiveZoneFlag${indiceZone}"
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

	dynamicPage(name: "schedulesSetup", title: "Schedule Setup", uninstall: false) {
		section("Schedule ${indiceSchedule} Setup") {
			input "scheduleName${indiceSchedule}",  "text",title: "Schedule Name",
				defaultValue:settings."scheduleName${indiceSchedule}"
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
				required: false, defaultValue:settings."givenClimate${indiceSchedule}", description: "Optional, for ecobee thermostats only")
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Cooling setpoint in the selected zone(s) [when no ecobee program/climate available]") {
			input (name:"desiredCoolTemp${indiceSchedule}", type:"decimal", title: "Cooling setpoint, default = 75F/23C", 
				required: false,defaultValue:settings."desiredCoolTemp${indiceSchedule}", description: "Not optional for non ecobee tstats")			                
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Heating setpoint [when no ecobee program/climate available]") {
			input (name:"desiredHeatTemp${indiceSchedule}", type:"decimal", title: "Heating setpoint, default=72F/21C", 
				required: false, defaultValue:settings."desiredHeatTemp${indiceSchedule}", description: "Not optional for non ecobee tstats")			                
		}
		section("Schedule ${indiceSchedule}-Set thermostat(s) to 'eco' mode")  {
			input (name:"ecoWhenAway${indiceSchedule}", type:"bool", title: "set thermostat mode to 'eco' when Away and back to last mode when Home?",  
				required: false, defaultValue:settings."ecoWhenAway${indiceSchedule}", description: "optional")
		}
		section("Schedule ${indiceSchedule}-Outdoor Thresholds Setup for switching thermostat mode (heat/cool/auto) or more heating/cooling [optional]") {
			href(name: "toOutdoorThresholdsSetup", page: "outdoorThresholdsSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: outdoorThresholdsHrefTitle(indiceSchedule), image: getCustomImagePath() + "WeatherStation.jpg"  ) 
		}
		section("Schedule ${indiceSchedule}-Max Temp Adjustment Allowed for the active zone(s)") {
			input (name:"givenMaxTempDiff${indiceSchedule}", type:"decimal", title: "Max Temp adjustment to setpoints", required: false,
				defaultValue:settings."givenMaxTempDiff${indiceSchedule}", description: " [default= +/-5F/2C]")
		}        
		section("Schedule ${indiceSchedule}-Set Fan Mode [optional]") {
			href(name: "toFanSettingsSetup", page: "fanSettingsSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: fanSettingsHrefTitle(indiceSchedule), image: getCustomImagePath() + "Fan.png") 
		}	
		section("Schedule ${indiceSchedule}-Alternative Cooling Setup [optional]") {
			href(name: "toAlternativeCoolingSetup", page: "alternativeCoolingSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: alternativeCoolingHrefTitle(indiceSchedule),image: getCustomImagePath() + "altenergy.jpg" ) 
		}
		section("Schedule ${indiceSchedule}-Zone/Room Thermostats settings [optional]") {
			input (name:"setRoomThermostatsOnlyFlag${indiceSchedule}", type:"bool", title: "Set room thermostats only [default=false,main & room thermostats setpoints are set]", 
				required: false, defaultValue:settings."setRoomThermostatsOnlyFlag${indiceSchedule}")
			input (name:"useMasterTstatSetpointsFlag${indiceSchedule}", type:"bool", title: "Set room thermostats' setpoints based on master tstat setpoints [default=false,setpoints are set in schedule]", 
				required: false, defaultValue:settings."useMasterTstatSetpointsFlag${indiceSchedule}")
		}
		section("Schedule ${indiceSchedule}-Vent Settings for the Schedule") {
			href(name: "toVentSettingsSetup", page: "ventSettingsSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: ventSettingsHrefTitle(indiceSchedule), image: "${getCustomImagePath()}ventopen.png" ) 
		}
		section("Schedule ${indiceSchedule}-Set for specific ST location mode(s) [default=all]")  {
			input (name:"selectedMode${indiceSchedule}", type:"enum", title: "Choose Mode", options: enumModes, 
				required: false, multiple:true,defaultValue:settings."selectedMode${indiceSchedule}", description: "Optional")
		}
		section("Schedule ${indiceSchedule}-Make the schedule inactive?") {
			input "inactiveScheduleFlag${indiceSchedule}", title: "Inactive?", "bool", 
				required: false, description: "false",defaultValue:settings."inactiveScheduleFlag${indiceSchedule}"
		}
		section {
			href(name: "toSchedulesSetupPage", title: "Back to Schedules Setup Page", page: "schedulesSetupPage")
		}
	}        
}


def fanSettingsSetup(params) {
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
    
	dynamicPage(name: "fanSettingsSetup", title: "Fan Settings Setup for schedule " + settings."scheduleName${indiceSchedule}", uninstall: false, 
		nextPage: "schedulesSetupPage") {
		section("Schedule ${indiceSchedule}-Set Fan Mode [optional]") {
			input (name:"fanMode${indiceSchedule}", type:"enum", title: "Set Fan Mode ['on', 'auto', 'circulate']", options: ["on", "auto", "circulate"], required: false,
				defaultValue:settings."fanMode${indiceSchedule}", description: "Optional")
			input (name:"moreFanThreshold${indiceSchedule}", type:"decimal", title: "Outdoor temp's threshold for Fan Mode", required: false,
				defaultValue:settings."moreFanThreshold${indiceSchedule}", description: "Optional")			                
			input (name:"givenMaxFanDiff${indiceSchedule}", type:"decimal", title: "Max Temp Differential in the active zone(s) to trigger Fan mode", required: false,
				defaultValue:settings."givenMaxFanDiff${indiceSchedule}", description: "[default= +/-5F/2C]", range:"0.5..20")
			input (name:"fanModeForThresholdOnlyFlag${indiceSchedule}", type:"bool",  title: "Override Fan Mode only when Outdoor Threshold or Indoor Temp differential is reached(default=false)", 
				required: false, defaultValue:settings."fanModeForThresholdOnlyFlag${indiceSchedule}")
			input (name:"fanDiffCalcExcludeMasterFlag${indiceSchedule}", type:"bool",  title: "Fan temp diff calculation excludes master's ambient temp & includes only the active zones' sensors(default=false)", 
				required: false, defaultValue:settings."fanDiffCalcExcludeMasterFlag${indiceSchedule}")
		}	
		section {
			href(name: "toSchedulePage${indiceSchedule}", title: "Back to Schedule #${indiceSchedule} - Setup Page", page: "schedulesSetup", params: [indiceSchedule: indiceSchedule])
		}
	}    
}   


def fanSettingsHrefTitle(i) {
	def title = "Fan Settings for Schedule ${i}"
	return title
}


def outdoorThresholdsSetup(params) {
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
    
	dynamicPage(name: "outdoorThresholdsSetup", title: "Outdoor Thresholds for schedule " + settings."scheduleName${indiceSchedule}", uninstall: false,
		nextPage: "schedulesSetupPage") {
		section("Schedule ${indiceSchedule}-Switch thermostat mode (auto/cool/heat) based on this outdoor temp range [optional]") {
			input (name:"heatModeThreshold${indiceSchedule}", type:"decimal", title: "Heat mode threshold", 
				required: false, defaultValue:settings."heatModeThreshold${indiceSchedule}", description: "Optional")			                
			input (name:"coolModeThreshold${indiceSchedule}", type:"decimal", title: "Cool mode threshold", 
				required: false, defaultValue:settings."coolModeThreshold${indiceSchedule}", description: "Optional")			               
		}			
		section("Schedule ${indiceSchedule}-More Heat/Cool Threshold in the selected zone(s) based on outdoor temp Sensor [optional]") {
			input (name:"moreHeatThreshold${indiceSchedule}", type:"decimal", title: "Outdoor temp's threshold for more heating", 
				required: false, defaultValue:settings."moreHeatThreshold${indiceSchedule}", description: "Optional")			                
			input (name:"moreCoolThreshold${indiceSchedule}", type:"decimal", title: "Outdoor temp's threshold for more cooling",
				required: false,defaultValue:settings."moreCoolThreshold${indiceSchedule}", description: "Optional")
		}                
		section {
			href(name: "toSchedulePage${indiceSchedule}", title: "Back to Schedule #${indiceSchedule} - Setup Page", page: "schedulesSetup", params: [indiceSchedule: indiceSchedule])
		}
	}    
}   

def outdoorThresholdsHrefTitle(i) {
	def title = "Outdoor Thresholds for Schedule ${i}"
	return title
}

def alternativeCoolingSetup(params) {
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
    
	dynamicPage(name: "alternativeCoolingSetup", title: "Alternative Cooling for schedule " + settings."scheduleName${indiceSchedule}" + "-switch(es) in General Setup required", uninstall: false,
		nextPage: "schedulesSetupPage") {
		section("Schedule ${indiceSchedule}-Use of Evaporative Cooler/Big Fan/Damper For alternative cooling based on outdoor sensor's temp and humidity readings [optional]") {
			input (name:"useEvaporativeCoolerFlag${indiceSchedule}", type:"bool", title: "Use of evaporative cooler/Big Fan/Damper? [default=false]", 
				required: false, defaultValue:settings."useEvaporativeCoolerFlag${indiceSchedule}")
			input (name:"useAlternativeWhenCoolingFlag${indiceSchedule}", type:"bool", title: "Alternative cooling in conjunction with cooling? [default=false]", 
				required: false, defaultValue:settings."useAlternativeWhenCoolingFlag${indiceSchedule}")
			input (name:"coolModeThreshold${indiceSchedule}", type:"decimal", title: "Outdoor temp's threshold for alternative cooling, run when temp <= threshold [Required when not using humidity/temp table", 
				required: false, defaultValue:settings."coolModeThreshold${indiceSchedule}", description: "Optional")			               
			input (name:"diffToBeUsedFlag${indiceSchedule}", type:"bool", title: "Use of an offset value against the desired Temp for switching to cool [default=false]", 
				required: false, defaultValue:settings."diffToBeUsedFlag${indiceSchedule}")
			input (name:"diffDesiredTemp${indiceSchedule}", type:"decimal", title: "Temp Offset/Differential value vs. desired Cooling Temp", required: false,
				defaultValue:settings."diffDesiredTemp${indiceSchedule}", description: "[default= +/-5F/2C]")
		}                
		section {
			href(name: "toSchedulePage${indiceSchedule}", title: "Back to Schedule #${indiceSchedule} - Setup Page", page: "schedulesSetup", params: [indiceSchedule: indiceSchedule])
		}
	}    
}   

def alternativeCoolingHrefTitle(i) {
	def title = "Alternative Cooling Setup for Schedule ${i}"
	return title
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
			input "logFilter", "enum", title: "log filtering [Level 1=ERROR only,2=<Level 1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>]?",required:false, options: [1,2,3,4,5]
				          
		}
		section([mobileOnly: true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}

private boolean is_alternative_cooling_efficient(outdoorTemp, outdoorHum) {
	def scale = (state?.scale)?: getTemperatureScale()
	int outdoorTempInF= (scale=='C') ? cToF(outdoorTemp):outdoorTemp
	traceEvent(settings.logFilter,"is_alternative_cooling_efficient>outdoorTemp In Farenheit=$outdoorTempInF",settings.detailedNotif)
    
	switch (outdoorTempInF) {
    	case 75..79:
			outdoorTempInF =75        
		break            
    	case 80..84:
			outdoorTempInF =80        
		break
    	case 85..89:
			outdoorTempInF =85        
		break
    	case 90..94:
			outdoorTempInF =90        
		break
    	case 95..99:
			outdoorTempInF =95        
		break
    	case 100..104:
			outdoorTempInF =100        
		break
    	case 105..109:
			outdoorTempInF =105        
		break
    	case 110..114:
			outdoorTempInF =110        
		break
		default:
			outdoorTempInF =0        
		break        
	}        
	def temp_hum_range_table = [
		'75': '70,75,80,',
		'80': '50,55,60,65,',
		'85': '35,40,45,50,',
		'90': '20,25,30,',
		'95': '10,15,20,',
		'100': '5,10,',
		'105': '2,5,',
		'110': '2,'
	]    
	if (outdoorTempInF >= 75) {
		def max_hum_range
		try {
			max_hum_range = temp_hum_range_table.getAt(outdoorTempInF.toString())
		} catch (any) {
			traceEvent(settings.logFilter,"not able to get max humidity for temperature $outdoorTemp",settings.detailedNotif)
			return false        
		}
		def humidities  = max_hum_range.tokenize(',')
		def max_hum = humidities.last()
		traceEvent(settings.logFilter, "Max humidity $max_hum % found for temperature $outdoorTemp according to table",settings.detailedNotif)    
		    
		if ((outdoorHum) && (outdoorHum <= max_hum.toInteger())) {
			return true
		}
	} else if (outdoorTempInF <75) {
		return true    
	}
	return false
}


def check_use_alternative_cooling(data) {
	def indiceSchedule = data.indiceSchedule
	def scale = (state?.scale)?: getTemperatureScale()
	def key = "scheduleName${indiceSchedule}"
	def scheduleName=settings[key]
	def setVentSettings = (setVentSettingsFlag) ?: false
	def desiredCoolTemp= thermostat.currentCoolingSetpoint
    
	def outdoorTemp = outTempSensor?.currentTemperature
	def outdoorHum = outTempSensor?.currentHumidity
	def currentTemp = thermostat?.currentTemperature
	String currentMode = thermostat.latestValue("thermostatMode")
	String currentFanMode = thermostat.latestValue("thermostatFanMode")
    
	traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName, outdoorTemp=$outdoorTemp, outdoorHumidity=$outdoorHum,current mode=$currentMode, desiredCoolTemp=$desiredCoolTemp, currentTemp=$currentTemp",
		settings.detailedNotif)    
	if (evaporativeCoolerSwitch==null) {
		return false    
	}    

	def adjustmentFanFlag = (settings.setAdjustmentFanFlag)?: false
	key = "useAlternativeWhenCoolingFlag${indiceSchedule}"
	def useAlternativeWhenCooling=settings[key]
	traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName, doNotUseHumTable= $settings.doNotUseHumTableFlag, useAlternativeWhenCooling=$useAlternativeWhenCooling",
		settings.detailedNotif)    
	if (settings.doNotUseHumTableFlag) {    
		key = "coolModeThreshold$indiceSchedule"
		def lessCoolThreshold = settings[key]
		if (!lessCoolThreshold) { // if no threshold value is set, return false
			return false        
		}        
		if ((currentMode in ['cool','off', 'auto', 'eco']) && ((outdoorTemp) &&
			(outdoorTemp.toFloat() <= lessCoolThreshold.toFloat())) && 
			(currentTemp.toFloat() > desiredCoolTemp.toFloat())) {
			evaporativeCoolerSwitch.on()
			if (setVentSettings) open_all_vents()            
			if ((!useAlternativeWhenCooling) && (!(currentMode in ['off', 'eco']))) {                
				traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn off the thermostat $thermostat, saving the current thermostat's mode=$currentMode",settings.detailedNotif,
					GLOBAL_LOG_WARN,settings.detailedNotif)            
				state?.lastThermostatMode= currentMode            
				thermostat.off()
			} else if ((useAlternativeWhenCooling) && (currentMode in ['off', 'eco'])) {
				traceEvent(settings.logFilter,"check_use_alternative_cooling>useAlternativeWhenCooling= $useAlternativeWhenCooling,restoring $thermostat to ${state?.lastThermostatMode} mode ",
					settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)            
				if (!state?.lastThermostatMode) { // by default, set it to cool
					thermostat.cool()                
				} else {                
					restore_thermostat_mode()  // set the thermostat back to the mode it was before using alternative cooling           
				}                    
			}            
			if (adjustmentFanFlag) {             
				if (currentFanMode != 'auto') {            
					traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn on the thermostat's fan, saving the current Fan Mode=${currentFanMode}",settings.detailedNotif)            
					if (!state?.lastThermostatFanMode) { // save the fan mode for later
						state?.lastThermostatFanMode=  currentFanMode
					}
				}
				traceEvent(settings.logFilter,"check_use_alternative_cooling>turning on the fan",settings.detailedNotif, GLOBAL_LOG_INFO,true)            
				set_fan_mode(indiceSchedule,true,'on')
			}            
                
			traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName: alternative cooling (w/o HumTempTable); switch (${evaporativeCoolerSwitch}) is on",
				settings.detailedNotif, GLOBAL_LOG_INFO, true)
			return true				                
		} else {
			evaporativeCoolerSwitch.off()
			if (adjustmentFanFlag) {             
				traceEvent(settings.logFilter,"check_use_alternative_cooling>turning off the fan",settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)            
				set_fan_mode(indiceSchedule,true,'off')
			}  
			key = "diffDesiredTemp${indiceSchedule}"
			def diffDesiredTemp = (settings[key])?: (scale=='F')?5:2             
			key = "diffToBeUsedFlag${indiceSchedule}"
			def diffToBeUsed = (settings[key])?:false
			float desiredTemp = (diffToBeUsed)? (desiredCoolTemp.toFloat() - diffDesiredTemp.toFloat()) : desiredCoolTemp.toFloat()            
			if ((currentTemp.toFloat() > desiredTemp) && (currentMode in ['off', 'eco'])) {        
				traceEvent(settings.logFilter,"check_use_alternative_cooling>diffToBeUsed=$diffToBeUsed, currentTemp ($currentTemp) > desiredTemp in schedule ($desiredTemp), switching $thermostat to cool mode",settings.detailedNotif,
					GLOBAL_LOG_INFO,settings.detailedNotif)           
				if (!state?.lastThermostatMode) { // by default, set it to cool
					thermostat.cool()                
				} else {                
					restore_thermostat_mode()  // set the thermostat back to the mode it was before using alternative cooling           
				}                    
			}    
			traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName: alternative cooling (w/o HumTempTable); switch (${evaporativeCoolerSwitch}) is off",
				settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)
		}            
	} else if (currentMode in ['cool','off', 'auto', 'eco']) {    
		if (is_alternative_cooling_efficient(outdoorTemp,outdoorHum)) {
			if (currentTemp.toFloat() > desiredCoolTemp.toFloat()) {        
				traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn on the alternative cooling Switch (${evaporativeCoolerSwitch})",settings.detailedNotif)            
				if ((!useAlternativeWhenCooling) && (!(currentMode in ['off', 'eco']))) {                
					traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn off the thermostat $thermostat",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                        
					state?.lastThermostatMode= currentMode             
					thermostat.off()
				} else if ((useAlternativeWhenCooling) && (currentMode in ['off', 'eco'])) {
    	        
					traceEvent(settings.logFilter,"check_use_alternative_cooling>useAlternativeWhenCooling= $useAlternativeWhenCooling,restoring $thermostat to ${state?.lastThermostatMode} mode ",settings.detailedNotif,
						settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)            
					if (!state?.lastThermostatMode) { // by default, set it to cool
						thermostat.cool()                
					} else {                
						restore_thermostat_mode()  // set the thermostat back to the mode it was before using alternative cooling           
					}            
				}                    
				evaporativeCoolerSwitch.on()	
				// set all vent levels to 100% 
				if (setVentSettings) open_all_vents()                
				traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName: turned on the alternative cooling switch (${evaporativeCoolerSwitch})",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
				if (adjustmentFanFlag) {             
					if (currentFanMode != 'auto') {            
						traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn on the thermostat's fan, saving the current Fan Mode=${currentFanMode}",
							settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                                    
						if (!state?.lastThermostatFanMode) { // save the fan mode for later
							state?.lastThermostatFanMode=  currentFanMode
						}
					}
					traceEvent(settings.logFilter,"check_use_alternative_cooling>turning on the fan",settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)            
					set_fan_mode(indiceSchedule,true,'on')
				}            
				                
				return true            
			} else { /* current temp < desiredCoolTemp */
				traceEvent(settings.logFilter,"check_use_alternative_cooling>currentTemp ($currentTemp) <= desiredCoolTemp in schedule ($desiredCoolTemp), turning off alternative cooling ($evaporativeCoolerSwitch)",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)            
				if (adjustmentFanFlag) {  
					traceEvent(settings.logFilter,"check_use_alternative_cooling>turning off the fan",settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)            
					set_fan_mode(indiceSchedule,true,'off')
				}  
				evaporativeCoolerSwitch.off()	
			}
		} else {
			evaporativeCoolerSwitch.off()		        
			traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName: alternative cooling not efficient, switch (${evaporativeCoolerSwitch}) is off",
				settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
			if (adjustmentFanFlag) {             
				traceEvent(settings.logFilter,"check_use_alternative_cooling>turning off the fan",settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)            
				set_fan_mode(indiceSchedule,true,'off')
			}  
			key = "diffDesiredTemp${indiceSchedule}"
			def diffDesiredTemp = (settings[key])?: (scale=='F')?5:2             
			key = "diffToBeUsedFlag${indiceSchedule}"
			def diffToBeUsed = (settings[key])?:false
			float desiredTemp = (diffToBeUsed)? (desiredCoolTemp.toFloat() - diffDesiredTemp.toFloat()) : desiredCoolTemp.toFloat()            
			if ((currentTemp.toFloat() > desiredTemp) && (currentMode in ['off', 'eco'])) {        
				traceEvent(settings.logFilter,"check_use_alternative_cooling>diffToBeUsed=$diffToBeUsed,currentTemp ($currentTemp) > desiredCoolTemp in schedule ($desiredCoolTemp), switching $thermostat to ${state?.lastThermostatMode} mode",            
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
				if (!state?.lastThermostatMode) { // by default, set it to cool
					thermostat.cool()                
				} else {                
					restore_thermostat_mode()  // set the thermostat back to the mode it was before using alternative cooling           
				}                    
			}    
		} /* end if alternative_cooling efficient */            
	} /* end if settings.doNotUseHumTableFlag */
	return false    
} 


def installed() {
	state?.closedVentsCount= 0
	state?.openVentsCount=0
	state?.totalVents=0
	state?.ratioClosedVents=0
	state?.activeZones=[]
	state?.avgTempDiff= 0.0
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
//    log.debug "hubPlatform: (${state?.hub})"
    return state?.hub
}


def updated() {
	unsubscribe()
	try {
		unschedule()
	} catch (e) {	
		traceEvent(settings.logFilter,"updated>exception $e while calling unschedule()",settings.detailedNotif, GLOBAL_LOG_ERROR)
	}
	initialize()
	// when updated, save the current thermostat modes for restoring them later
	if (!state?.lastThermostatMode) {
		state?.lastThermostatMode= thermostat.latestValue("thermostatMode")    
		state?.lastThermostatFanMode= thermostat.latestValue("thermostatFanMode")   
	}				        
	if (!isST()) {    
        	if (detailedNotif) runIn(900,"logsoff")    
	} 
}

def offHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value",settings.detailedNotif)
	if (evaporativeCoolerSwitch) {
		evaporativeCoolerSwitch.off() // Turn off the alternative cooling for the running schedule 
		restore_thermostat_mode()
    }
}

def onHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value",settings.detailedNotif)
	setZoneSettings()    
	rescheduleIfNeeded(evt)   // Call rescheduleIfNeeded to work around ST scheduling issues
}

def ventTemperatureHandler(evt) {
	traceEvent(settings.logFilter,"vent temperature: $evt.value",settings.detailedNotif)
	float ventTemp = evt.value.toFloat()
	def scale = (state?.scale)?: getTemperatureScale()
	def MAX_TEMP_VENT_SWITCH = (maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch
	String currentHVACMode = thermostat.currentValue("thermostatMode")

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
        
	} /* end if too hot */           
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
	} /* end if too cold */ 
}

def thermostatOperatingHandler(evt) {
	log.debug("thermostatOperatingHandler: ${evt}")
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
	traceEvent(settings.logFilter,"changeModeHandler>$evt.name: $evt.value",settings.detailedNotif)

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
            def appName = getVirtualZoneChildName()
			def dni = "${appName}.${zoneName}.$indiceZone"  
			def d = getChildDevice(dni)
            log.debug("zone: ${zoneName}, dni: ${dni}, d: {d}")
			if (d) {  
				def ventState=d.currentValue("allVentsState")      
				def isChange = d.isStateChange(d, "allVentsState", evt.value)	//check of state change
				traceEvent(settings.logFilter,"ventRoomHandler>found $roomName in zone $zoneName, about to set corresponding virtual zone ($dni) to $evt.value if isChange ($isChange) is true (current Vent State=$ventState)",settings.detailedNotif)
				if (isChange) {                
					d.sendEvent(name:"allVentsState", value: evt.value, isDisplayed: true, isStateChange: isChange)
                    
                    log.debug("zoneAllVents ${evt.value}")
                    if(evt.value == "on") {
                    	d.sendEvent(name:"zoneAllVents", value: "Active")
					} else {
                    	d.sendEvent(name:"zoneAllVents", value: "Off")
                    }
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
	boolean isContactOpen=false    
	key = "contactSensor$indiceRoom"
	def contactSensor = settings[key]
	traceEvent(settings.logFilter,"contactEvtHandler>contactSensor=${contactSensor}",settings.detailedNotif)
	if (contactSensor !=null) {
		isContactOpen = any_contact_open(contactSensor)            
	}        
	if (adjustmentBasedOnContact) { 
		key = "contactClosedLogicFlag${indiceRoom}"            
		boolean closedContactLogicFlag= (settings[key])?:false            
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
			} /* end for ventSwitch */                
		}            
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
			traceEvent(settings.logFilter,"contactEvtHandler>zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
			continue
		}
		traceEvent(settings.logFilter,"contactEvtHandler>zone=${zoneName},about to scan zone for room",settings.detailedNotif)
		key = "virtualZoneFlag${indiceZone}"
		def virtualCreateZoneFlag = (settings[key])?:false

		if (!virtualCreateZoneFlag) {   // if no virtual zone is created, just continue
			traceEvent(settings.logFilter,"contactEvtHandler>zone $zoneName doesn't have a virtual zone associated, just continue",
					settings.detailedNotif, GLOBAL_LOG_INFO)
			continue        
		}
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		if (rooms.toString().contains(roomName.toString().trim())) {
			traceEvent(settings.logFilter,"contactEvtHandler>found $roomName in zone $zoneName",settings.detailedNotif)
            def appName = getVirtualZoneChildName()
			def dni = "${appName}.${zoneName}.$indiceZone"  
			def d = getChildDevice(dni)
			if (d) {  
				d.sendEvent(name:"contact", value: ((isContactOpen)?"open":"closed") , isDisplayed:true, isStateChange:true)
			} else {
				traceEvent(settings.logFilter,"contactEvtHandler>didnt't find virtual $dni, not able to update contact with ${evt.value}",
					settings.detailedNotif, GLOBAL_LOG_WARN)
			} 
		} else {
			traceEvent(settings.logFilter,"contactEvtHandler>$roomName not in zone $zoneName",settings.detailedNotif)
		}        
	} /* end for each zone */
}

private def motionEvtHandler(evt, indice) {
	traceEvent(settings.logFilter,"motionEvtHandler>$evt.name: $evt.value",settings.detailedNotif)
	def key= "roomName${indice}"    
	def roomName= settings[key]
	if (evt.value == "active") {
		key = "occupiedMotionTimestamp${indice}"       
		state[key]= now()        
		traceEvent(settings.logFilter,"Motion at home in ${roomName},occupiedMotionTimestamp=${state[key]}",settings.detailedNotif, GLOBAL_LOG_INFO)
		if (state?.setPresentOrAway == 'Away') {
			set_main_tstat_to_AwayOrPresent('present')
		}        
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
			def appName = getVirtualZoneChildName()
            def dni = "${appName}.${zoneName}.$indiceZone"  
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


def roomTstatEvtHandler1(evt) {
	int i=1
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler2(evt) {
	int i=2
	roomTstatHandler(evt,i)    
}
def roomTstatEvtHandler3(evt) {
	int i=3
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler4(evt) {
	int i=4
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler5(evt) {
	int i=5
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler6(evt) {
	int i=6
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler7(evt) {
	int i=7
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler8(evt) {
	int i=8
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler9(evt) {
	int i=9
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler10(evt) {
	int i=10
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler11(evt) {
	int i=11
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler12(evt) {
	int i=12
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler13(evt) {
	int i=13
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler14(evt) {
	int i=14
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler15(evt) {
	int i=15
	roomTstatHandler(evt,i)    
}

def roomTstatEvtHandler16(evt) {
	int i=16
	roomTstatHandler(evt,i)    
}




private def roomTstatHandler(evt, indice) {
	traceEvent(settings.logFilter,"roomTstatHandler>$evt.name now: $evt.value",settings.detailedNotif)
	float MAX_DELTA=2    
	def scale = (state?.scale) ?: getTemperatureScale()

	def currentMainThermostatBaseline=thermostat.currentThermostatSetpoint
	def key ="roomTstat${indice}"
	def roomTstat=settings[key]    
	def currentSlaveThermostatSetpoint=evt.value
	traceEvent(settings.logFilter,"roomTstatHandler>at $roomTstat: value of currentSlaveThermostatSetpoint = $currentSlaveThermostatSetpoint, currentMainThermostatBaseline=$currentMainThermostatBaseline", settings.detailedNotif, GLOBAL_LOG_INFO)
	if ((!currentSlaveThermostatSetpoint) || (!currentMainThermostatBaseline)) {
		traceEvent(settings.logFilter,"roomTstatHandler>value of currentSlaveThermostatSetpoint or currentMainThermostatBaseline is null, exiting", 
			settings.detailedNotif, GLOBAL_LOG_WARN)
		return        
	}

	def mode=thermostat.currentThermostatMode    

	key= "roomName${indice}"    
	def roomName= settings[key]
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
			traceEvent(settings.logFilter,"roomTstatHandler>zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
			continue
		}
		traceEvent(settings.logFilter,"roomTstatHandler>looking for $roomTstat, zone=${zoneName},about to scan zone for room",settings.detailedNotif)
		key = "virtualZoneFlag${indiceZone}"
		def virtualCreateZoneFlag = (settings[key])?:false

		if (!virtualCreateZoneFlag) {   // if no virtual zone is created, just continue
			traceEvent(settings.logFilter,"roomTstatHandler>zone $zoneName doesn't have a virtual zone associated, just continue",
				settings.detailedNotif, GLOBAL_LOG_INFO)
			continue        
		}
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		if (rooms.toString().contains(roomName.toString().trim())) {
			traceEvent(settings.logFilter,"roomTstatHandler>for $roomTstat, found $roomName in zone $zoneName",settings.detailedNotif)
			def appName = getVirtualZoneChildName()
            def dni = "${appName}.${zoneName}.$indiceZone"  
			def d = getChildDevice(dni)
			def originalTempDelta=(d.currentValue("tempDelta")) ?:0          
			def originalBaseline=(d.currentValue("baselineSetpoint")) ?: (state?.scale=='C') ?21:70          
			if (d) {  
				float tempDelta=  currentSlaveThermostatSetpoint.toFloat() -  originalBaseline.toFloat()  
				float offsetDelta=tempDelta - originalTempDelta.toFloat()                
				if (offsetDelta.abs() <= MAX_DELTA) {            
					d.sendEvent(name:"tempDelta", value:tempDelta, isDisplayed:true, isStateChange:true)
					d.sendEvent(name:"thermostatSetpoint", value:currentSlaveThermostatSetpoint, isDisplayed:true, isStateChange:true, unit:scale)
                    d.sendEvent(name:"zoneTemperatureDelta", value:tempDelta, unit:scale)
                    log.debug("1 - ${tempDelta}")
				}                    
			} else {
				traceEvent(settings.logFilter,"motionEvtHandler>didnt't find virtual $dni, not able to update contact with ${evt.value}",
					settings.detailedNotif, GLOBAL_LOG_WARN)
			
 			
            
				if (mode in ['heat', 'off','auto']){
					traceEvent(settings.logFilter,"roomTstatHandler>About to refresh desiredHeatTempDelta at $indiceZone with $tempDelta",settings.detailedNotif)
					save_new_heat_delta_value(tempDelta, indiceZone)    
				}
				if (mode in ['cool', 'off','auto']) {
					traceEvent(settings.logFilter,"roomTstatHandler>About to refresh desiredCoolTempDelta at $indiceZone with $tempDelta",settings.detailedNotif)
					save_new_cool_delta_value(tempDelta, indiceZone)    
				}
			}                
                    
		} else {
			traceEvent(settings.logFilter,"roomTstatHandler>$roomName not in zone $zoneName",settings.detailedNotif)
		}        
	} 
} 


private void restore_thermostat_mode() {

	if (state?.lastThermostatMode) {
		if (state?.lastThermostatMode == 'cool') {
			if (thermostat.hasCommand('cool')) thermostat.cool()
			else thermostat.setThermostatMode('cool')                
		} else if (state?.lastThermostatMode.contains('heat')) {
			if (thermostat.hasCommand('heat')) thermostat.heat()
			else thermostat.setThermostatMode('heat')                
		} else if (state?.lastThermostatMode  == 'auto') {
			if (thermostat.hasCommand('auto')) thermostat.auto()
			else thermostat.setThermostatMode('auto')                
		} else if (state?.lastThermostatMode  == 'eco') {
			if (thermostat.hasCommand('eco')) thermostat.eco()
			else thermostat.setThermostatMode('eco')                
		} else if (state?.lastThermostatMode  == 'dry') {
			if (thermostat.hasCommand('dry')) thermostat.dry()
			else thermostat.setThermostatMode('dry')                
		} else if (state?.lastThermostatMode  == 'off') {
			if (thermostat.hasCommand('off')) thermostat.off()
			else thermostat.setThermostatMode('off')                
		}            
		traceEvent(settings.logFilter, "thermostat ${thermostat}'s mode is now set back to ${state?.lastThermostatMode}",settings.detailedNotif, 
			GLOBAL_LOG_INFO,settings.detailedNotif)
		state?.lastThermostatMode=null        
	}        
	if (state?.lastThermostatFanMode) {
		if (state?.lastThermostatFanMode == 'on') {
			if (thermostat.hasCommand('fanOn')) thermostat.fanOn()
			else thermostat.setThermostatFanMode('on')                
		} else if (state?.lastThermostatFanMode  == 'auto') {
			if (thermostat.hasCommand('fanAuto')) thermostat.fanAuto()
			else thermostat.setThermostatFanMode('auto')                
		} else if (state?.lastThermostatFanMode  == 'off') {
			if (thermostat.hasCommand('fanOff')) thermostat.fanOff()
			else thermostat.setThermostatFanMode('off')                
		} else if (state?.lastThermostatFanMode  == 'circulate') {
			if (thermostat.hasCommand('fanCirculate')) thermostat.fanCirculate()
			else thermostat.setThermostatFanMode('circulate')                
		}            
		traceEvent(settings.logFilter, "thermostat ${thermostat}'s fan mode is now set back to ${state?.lastThermostatFanMode}", settings.detailedNotif, GLOBAL_LOG_INFO,true)
		state?.lastThermostatFanMode=null 
	}        
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
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)    
	subscribe(thermostat, "coolingSetpoint", coolingSetpointHandler)
	subscribe(thermostat, "thermostatOperatingState", thermostatOperatingHandler)
	subscribe(thermostat, "thermostatMode", changeModeHandler)
    
	subscribe(location, "mode", changeModeHandler)

	if (thermostat.hasCommand("resumeThisTstat")) {
		subscribe(thermostat, "programScheduleName", checkOverrideHandler)
	}        
	subscribe(app, appTouch)

	// subscribe all vents to check their temperature on a regular basis
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		def key    
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
			if (vent != null) {
				subscribe(vent, "temperature", ventTemperatureHandler)
				subscribe(vent, "switch.off", "ventEvtRoomHandler${indiceRoom}", [filterEvents: false]) 
				subscribe(vent, "switch.on", "ventEvtRoomHandler${indiceRoom}", [filterEvents: false])
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
		if (useRoomTstatSetpointFlag) {      
			key = "roomTstat${indiceRoom}"
			def roomTstat = settings[key]
			if (roomTstat) {
				traceEvent(settings.logFilter,"initialize>subscribed to thermostatSetpoint events in $roomTstat",settings.detailedNotif)

				subscribe(roomTstat, "thermostatSetpoint", "roomTstatEvtHandler${indiceRoom}", [filterEvents: false])
			}
		}            

	} /* end for rooms */


	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "sunrise", rescheduleIfNeeded)
	subscribe(location, "sunset", rescheduleIfNeeded)
	subscribe(location, "mode", rescheduleIfNeeded)
	subscribe(location, "sunriseTime", rescheduleIfNeeded)
	subscribe(location, "sunsetTime", rescheduleIfNeeded)
    
	subscribe(location, "askAlexaMQ", askAlexaMQHandler)
	runIn(30,"create_zone_devices")     
	rescheduleIfNeeded()   

}

def initialize() {

	state?.lastThermostatMode= ""
	state?.lastThermostatFanMode= ""
    state?.operatingState= ""
      
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
		state?."desiredHeatDeltaTemp$i"=settings[key]           
		key  = "desiredCoolDeltaTemp$i"
		state?."desiredCoolDeltaTemp$i"=settings[key]           
		key = "zoneName${i}"
		def zoneName = settings[key]
		key = "virtualZoneFlag${i}"
		def virtualCreateZoneFlag = (settings[key])?:false
		def appName = getVirtualZoneChildName()
        def dni = "${appName}.${zoneName}.$i"
		def d=zoneDevices.find {
				((it.device.deviceNetworkId.contains(dni)))        
		}
		traceEvent(settings.logFilter,"create_zone_devices>for zoneName $zoneName, virtualZoneFlag=$virtualCreateZoneFlag, found d=${d?.name}", detailedNotif)
		if (virtualCreateZoneFlag && (!d)) {
			def labelName="My Zone $zoneName"        
			traceEvent(settings.logFilter,"create_zone_devices>about to create child device with id $dni,labelName=  ${labelName}", detailedNotif)
			def newZone 
			if (isST()) {
                		newZone=addChildDevice(	getSTChildNamespace(), getVirtualZoneChildName(), dni, null,[label: "${labelName}"])
            		} else {
                		newZone=addChildDevice(	getChildNamespace(), getVirtualZoneChildName(), dni,[label: "${labelName}"])
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
	def mode=thermostat.currentThermostatMode    
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
	state?."desiredHeatTempDelta${indiceZone}"=newDeltaValue // save the new temp delta at virtual zone and store it in state variable
	def settingKey= "desiredHeatDeltaTemp${indiceZone}"   
	if (isST()) {    
        	app.updateSetting(settingKey,  newDeltaValue)        
	} else {
        	app.updateSetting(settingKey, [value: newDeltaValue, type:"int"])        
	}  
//	settings."desiredHeatDeltaTemp${indiceZone}"=newDeltaValue         
	traceEvent(settings.logFilter,"save_new_heat_delta_value>saved desiredHeatTempDelta at $indiceZone with $newDeltaValue",settings.detailedNotif, GLOBAL_LOG_INFO)
	runIn(10,"setZoneSettings")                 
}

private void save_new_cool_delta_value(newDeltaValue, indiceZone) {
	def scale = (state?.scale)?: getTemperatureScale()	
	float MAX_DELTA=(scale=='C')?1:2
	def originalDelta = (state?."desiredCoolTempDelta${indiceZone}") ?:0
	float offsetDelta=originalDelta?.toFloat() - newDeltaValue?.toFloat()
	if (offsetDelta.abs() > MAX_DELTA) {
		traceEvent(settings.logFilter,"save_new_heat_delta_value>hasn't saved desiredCoolTempDelta at $indiceZone with $newDeltaValue as offset too big vs. original delta $originalDelta",settings.detailedNotif, GLOBAL_LOG_WARN)
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
	runIn(10,"setZoneSettings")      
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
		traceEvent(settings.logFilter, "rescheduleIfNeeded>scheduling takeAction in ${delay} minutes..", settings.detailedNotif,GLOBAL_LOG_INFO)
		try {        
			runEvery5Minutes(setZoneSettings)
		} catch (e) {
 			traceEvent(settings.logFilter,"rescheduleIfNeeded>exception $e while rescheduling",settings.detailedNotif, GLOBAL_LOG_ERROR,true)        
		}
		setZoneSettings()    
	}
    
    
	// Update rescheduled state
    
	if (!evt) state?.poll["rescheduled"] = now()
}


def appTouch(evt) {
	state?.lastScheduleName=""	// force reset of the zone settings
	state?.lastStartTime=null 
	state?.scheduleCoolSetpoint=null
	state?.scheduleHeatSetpoint=null
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
	boolean isResidentPresent=true
	traceEvent(settings.logFilter,"Begin of setZoneSettings Fcn",settings.detailedNotif, GLOBAL_LOG_TRACE)
	def todayDay = new Date().format("dd",location.timeZone)
	if ((!state?.today) || (todayDay != state?.today)) {
		state?.exceptionCount=0   
		state?.sendExceptionCount=0        
		state?.today=todayDay        
	}   
    
	traceEvent(settings.logFilter,"setZoneSettings>setVentSettingsFlag=$setVentSettingsFlag,setAdjustmentTempFlag=$setAdjustmentTempFlag," +
		"setAdjustmentOutdoorTempFlag=$setAdjustmentOutdoorTempFlag,\n setAdjustmentFanFlag=$setAdjustmentFanFlag",settings.detailedNotif)
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

	/* Poll or refresh the thermostat to get latest values */
	if  (thermostat.hasCapability("Polling")) {
		try {        
			thermostat.poll()
		} catch (e) {
			traceEvent(settings.logFilter,"setZoneSettings>not able to do a poll() on ${thermostat}, exception ${e}", settings.detailedNotif, GLOBAL_LOG_ERROR)
		}                    
	}  else if  (thermostat.hasCapability("Refresh")) {
		try {        
			thermostat.refresh()
		} catch (e) {
			traceEvent(settings.logFilter,"setZoneSettings>not able to do a refresh() on ${thermostat}, exception ${e}",settings.detailedNotif, GLOBAL_LOG_ERROR)
		}                    
	}                    
/* Commented out to avoid any "offline" issues on some sensors following some ST platform changes. */

	if ((outTempSensor) && ((outTempSensor.hasCapability("Refresh")) || (outTempSensor.hasCapability("Polling")))) {

		// do a refresh to get latest temp value
		try {        
			outTempSensor.refresh()
		} catch (e) {
			traceEvent(settings.logFilter,"setZoneSettings>not able to do a refresh() on ${outTempSensor}, exception ${e}",settings.detailedNotif, GLOBAL_LOG_INFO)
		}                    
	}
    
	def ventSwitchesOn = []
	def mode =thermostat.latestValue("thermostatMode")                 
	def setVentSettings = (setVentSettingsFlag) ?: false
	def adjustmentOutdoorTempFlag = (setAdjustmentOutdoorTempFlag)?: false
	def adjustmentTempFlag = (setAdjustmentTempFlag)?: false
	def adjustmentFanFlag = (setAdjustmentFanFlag)?: false
    
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
	for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
        
		def key = "scheduleName$i"
		def scheduleName = settings[key]
		if (!scheduleName || scheduleName=='null') {
        
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
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName} added + 1 day, new endTime=${endTimeToday.time}",settings.detailedNotif)            

		}        
		String startInLocalTime = startTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
		String endInLocalTime = endTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)

		traceEvent(settings.logFilter,"setZoneSettings>found schedule ${scheduleName},original startTime=$startTime,original endTime=$endTime,nowInLocalTime= ${nowInLocalTime},startInLocalTime=${startInLocalTime},endInLocalTime=${endInLocalTime}," +
			"currTime=${currTime},begintime=${startTimeToday.time},endTime=${endTimeToday.time},lastScheduleName=${state?.lastScheduleName}, lastStartTime=${state?.lastStartTime}",
				settings.detailedNotif)            
		def ventSwitchesZoneSet = []        
		if ((currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (state.lastScheduleName !=scheduleName) && (IsRightDayForChange(i))) {
        
			// let's set the given schedule
			initialScheduleSetup=true
			foundSchedule=true

			traceEvent(settings.logFilter,"setZoneSettings>new schedule ${scheduleName},currTime= ${currTime}, foundSchedule=$foundSchedule,current date & time OK for execution", detailedNotif)
			if (adjustmentFanFlag) {                
				set_fan_mode(i)
			}  
            
			traceEvent(settings.logFilter,"setZoneSettings>About to call adjust_thermostat_setpoints",detailedNotif)
			runIn(30,"adjust_thermostat_setpoints", [data: [indiceSchedule:i]])
			key = "useEvaporativeCoolerFlag${i}"                
			def useAlternativeCooling = (settings[key]) ?: false
			if ((useAlternativeCooling) && (mode in ['cool','off', 'auto'])) {
				traceEvent(settings.logFilter,"setZoneSettings>about to call check_use_alternative_cooling()",settings.detailedNotif)
				// save the current thermostat modes for restoring them later
				if (!state?.lastThermostatMode) {
					state?.lastThermostatMode= thermostat.latestValue("thermostatMode")    
					state?.lastThermostatFanMode= thermostat.latestValue("thermostatFanMode")   
				}				        
				runIn(60,"check_use_alternative_cooling", [data: [indiceSchedule:i]])
			} else {
				if (evaporativeCoolerSwitch) {
					evaporativeCoolerSwitch.off() // Turn off the alternative cooling for the running schedule 
					restore_thermostat_mode()
				}        
			}            
			if (setVentSettings) {
				ventSwitchesZoneSet= adjust_vent_settings_in_zone(i)
				traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},list of Vents turned 'on'= ${ventSwitchesZoneSet}",settings.detailedNotif)
			}
			state.lastStartTime = startTimeToday.time		  
			state.lastScheduleName = scheduleName
 			ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet              
		}
		else if ((state?.lastScheduleName == scheduleName) && (currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange(i))) {
			// We're in the middle of a schedule run
        
			foundSchedule=true
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},currTime= ${currTime},foundSchedule=$foundSchedule,current time is OK for execution, we're in the middle of a schedule run",
				settings.detailedNotif)            
			def setAwayOrPresent = (setAwayOrPresentFlag)?:false
            
			if (setAwayOrPresent) {
	            
				isResidentPresent=verify_presence_based_on_motion_in_rooms()
				if (isResidentPresent) {            

					if (state?.setPresentOrAway != 'present') {
						set_main_tstat_to_AwayOrPresent('present')
					}
				} else {
					if (state?.setPresentOrAway != 'away') {
						set_main_tstat_to_AwayOrPresent('away')
					}                
				}
			}            
			if (adjustmentFanFlag) {                
				// will override the fan settings if required (ex. more Fan Threshold is set)
				set_fan_mode(i)
			}                    
			if (isResidentPresent) {
            
				runIn(30,"adjust_thermostat_setpoints", [data: [indiceSchedule:i]])
				if (adjustmentOutdoorTempFlag) {
					// let's adjust the thermostat setpoints according to outdoor temperature
					traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},about to call adjust_tstat_for_more_less_heat_cool", settings.detailedNotif, GLOBAL_LOG_INFO)
					runIn(60,"adjust_tstat_for_more_less_heat_cool", [data: [indiceSchedule:i]])
				}                    
 			}
            
           
			key = "useEvaporativeCoolerFlag${i}"                
			def useAlternativeCooling = (settings[key]) ?: false
			if ((useAlternativeCooling) && (mode in ['cool','off', 'auto'])) {
				traceEvent(settings.logFilter,"setZoneSettings>about to call check_use_alternative_cooling()",settings.detailedNotif)
				runIn(60,"check_use_alternative_cooling", [data: [indiceSchedule:i]])
			}            
			String operatingState = thermostat.currentThermostatOperatingState
            log.debug("operatingState: ${operatingState}")
            
            log.debug("state: ${state}")
			if (setVentSettings) {            

				key = "adjustVentsEveryCycleFlag$i"
				def adjustVentSettings = (settings[key]) ?: false
				traceEvent(settings.logFilter,"setZoneSettings>adjustVentsEveryCycleFlag=$adjustVentSettings",settings.detailedNotif)
				// Check the operating State before adjusting the vents again.
				// let's adjust the vent settings according to desired Temp only if thermostat is not idle or was not idle at the last run

				if (state?.operatingState== null) {
                	state?.operatingState= ""
                    log.debug("state?.operatingState=  NULL")
                }
				if ((adjustVentSettings) || ((operatingState.toUpperCase() !='IDLE') ||
					((state?.operatingState) && (state?.operatingState.toUpperCase() =='HEATING') || (state?.operatingState.toUpperCase() =='COOLING'))))
				{            
					traceEvent(settings.logFilter,"setZoneSettings>thermostat ${thermostat}'s Operating State is ${operatingState} or was just recently " +
							"${state?.operatingState}, adjusting the vents for schedule ${scheduleName}",settings.detailedNotif, GLOBAL_LOG_INFO)
					ventSwitchesZoneSet=adjust_vent_settings_in_zone(i)
					ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet     
				}   
			                
			}        
            log.debug("operatingState: ${operatingState}")
			state?.operatingState =operatingState            
		}

	} /* end for */
    
	if (((setVentSettings) && (ventSwitchesOn !=[])) || (initialScheduleSetup && (ventSwitchesOn !=[]))) {
		traceEvent(settings.logFilter,"setZoneSettings>list of Vents turned on= ${ventSwitchesOn}",settings.detailedNotif)
		turn_off_all_other_vents(ventSwitchesOn)
	}
	if (!foundSchedule) {
		traceEvent(settings.logFilter,"setZoneSettings>No schedule applicable at this time ${nowInLocalTime}",settings.detailedNotif, GLOBAL_LOG_INFO)
		if (evaporativeCoolerSwitch) {
			traceEvent(settings.logFilter,"setZoneSettings>about to turn off switch(es)",settings.detailedNotif, GLOBAL_LOG_INFO)
			evaporativeCoolerSwitch.off() // Turn off the alternative cooling for the running schedule 
			restore_thermostat_mode()
		}
	} 
        
}


private def isRoomOccupied(sensor, indiceRoom) {
	def key ="occupiedMotionOccNeeded${indiceRoom}"
	def nbMotionNeeded = (settings[key]) ?: 1
	key = "roomName$indiceRoom"
	def roomName = settings[key]

  	if (location.mode == "Night") { 
		// Rooms are considered occupied when the ST hello mode is "Night"  
		traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} is considered occupied, ST hello mode ($location.mode) == Night",settings.detailedNotif,
			GLOBAL_LOG_INFO)        
		return nbMotionNeeded
	} 
    
	if (thermostat) {
		try {    
			String currentProgName = thermostat.currentSetClimate
			if (currentProgName?.toUpperCase().contains('SLEEP')) { 
				traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} is considered occupied, ecobee ($currentProgName) == Sleep",settings.detailedNotif, GLOBAL_LOG_INFO)
				// Rooms are considered occupied when the ecobee program is set to 'SLEEP'    
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


private def verify_presence_based_on_motion_in_rooms() {
	boolean foundSensor=false
    
	def result=false
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		def key = "roomName$indiceRoom"
		def roomName = settings[key]
		key = "motionSensor$indiceRoom"
		def motionSensor = settings[key]
		if (motionSensor != null) {

			if (isRoomOccupied(motionSensor,indiceRoom)) {
				traceEvent(settings.logFilter,"verify_presence_based_on_motion>in ${roomName},presence detected, return true",settings.detailedNotif)
				return true
			} 
			foundSensor=true            
		}
	} /* end for */   
	if (foundSensor) return result
	return true // by default, returns true
}
private def set_main_tstat_to_AwayOrPresent(mode) {

	def currentMode=thermostat.currentThermostatMode
    
	try {
		if ((mode == 'eco') && (thermostat.hasCommand("eco"))) {
			thermostat.eco()
		} else if ((mode == 'off') && (thermostat.hasCommand("off"))) {
			thermostat.off()
		} else if ((mode == 'away') && (thermostat.hasCommand("away"))) {
			thermostat.away()
		} else if ((mode == 'present') && (thermostat.hasCommand("present"))) {	
			thermostat.present()
		} else {
			traceEvent(settings.logFilter,"error, not able to set main thermostat ${thermostat} to ${mode} as this mode is not supported" ,settings.detailedNotif,
				GLOBAL_LOG_WARN,settings.detailedNotif)
			return                
		}        
		traceEvent(settings.logFilter,"set main thermostat ${thermostat} to ${mode} mode based on motion in all rooms && currentLocationMode (${location.currentMode})" ,settings.detailedNotif,
			GLOBAL_LOG_INFO,settings.detailedNotif)
		state?.setPresentOrAway=mode    // set a state for further checking later
	 	state?.programSetTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
 		state?.programSetTimestamp = now()
	}    
	catch (e) {
		traceEvent(settings.logFilter,"set_tstat_to_AwayOrPresent>not able to set thermostat ${thermostat} to ${mode} mode (exception $e)",true, GLOBAL_LOG_ERROR,true)
	}

}




private def any_contact_open(contactSet) {

	if (!contactSet) {
		return false
	}        
	int contactSize=(contactSet)? contactSet.size() :0
	for (i in 0..contactSize -1) {
		def contactState = contactSet[i].currentState("contact")
		if (contactState.value == "open") {
			traceEvent(settings.logFilter,"any_contact_open>contact ${contactSet[i]} is open",
					settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)                        
			return true 
		}
	}            
	return false    
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

private def setRoomPuckSettings(indiceSchedule,indiceZone, indiceRoom) {

	def scale = (state?.scale)?: getTemperatureScale()
	float desiredHeat, desiredCool
	boolean setClimate = false
	def key = "zoneName$indiceZone"
	def zoneName = settings[key]

	key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	key = "pressureSensor$indiceRoom"
	def roomPuck = settings[key]
	key = "roomName$indiceRoom"
	def roomName = settings[key]
	key  = "desiredHeatDeltaTemp$indiceZone"
	def desiredHeatDelta =  (state?."desiredHeatTempDelta$indiceZone")? state?."desiredHeatTempDelta$indiceZone".toFloat(): settings[key]
	key  = "desiredCoolDeltaTemp$indiceZone"
	def desiredCoolDelta =  (state?."desiredCoolTempDelta$indiceZone")? state?."desiredCoolTempDelta$indiceZone".toFloat(): settings[key]
    

	traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName},about to apply puck's temp settings at ${roomPuck}",settings.detailedNotif)
	String mode = thermostat?.currentValue("thermostatMode") // get the mode at the main thermostat
	if (mode.contains('heat')) {
		key = "desiredHeatTemp$indiceSchedule"
		def heatTemp = settings[key]
		if (!heatTemp) {
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName},about to apply default heat settings",settings.detailedNotif)
			desiredHeat = (scale=='C') ? 21:72				// by default, 21C/72F is the target heat temp
		} else {
			desiredHeat = heatTemp.toFloat()
		}
		desiredHeat =desiredHeat + ((desiredHeatDelta)?desiredHeatDelta.toFloat():0)                    
		def currentHeatSetpoint=roomPuck?.currentDesiredTemperature
		if (currentHeatSetpoint != desiredHeat) {            
			roomPuck.setDesiredTemp(desiredHeat)
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName},in room ${roomName},${roomPuck}'s desiredHeat=${desiredHeat} set in ${mode} mode",
				settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
		} else {
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName},in room ${roomName},${roomPuck}'s desiredHeat=${desiredHeat} already set in ${mode} mode",
				settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
		} 
	} else if (mode == 'cool') {
		key = "desiredCoolTemp$indiceSchedule"
		def coolTemp = settings[key]
		if (!coolTemp) {
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName},about to apply default cool settings",settings.detailedNotif)
			desiredCool = (scale=='C') ? 23:75				// by default, 23C/75F is the target cool temp
		} else {
			desiredCool = coolTemp.toFloat()
		}
		desiredCool =desiredCool + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)                    
		def currentCoolSetpoint=roomPuck?.currentDesiredTemperature
		if (currentCoolSetpoint != desiredCool) {            
			roomPuck.setDesiredTemp(desiredCool)
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName}, ${roomPuck}'s desiredCool=${desiredCool} set in ${mode} mode",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)            
		} else {
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName}, ${roomPuck}'s desiredCool=${desiredCool} already set in ${mode} mode",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)            
		} 
	} else if (mode in ['auto', 'off']) {
		key = "desiredHeatTemp$indiceSchedule"
		def heatTemp = settings[key]
		if (!heatTemp) {
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName},about to apply default heat settings",settings.detailedNotif,
				GLOBAL_LOG_INFO)
			desiredHeat = (scale=='C') ? 21:72				// by default, 21C/72F is the target heat temp
		} else {
			desiredHeat = heatTemp.toFloat()
		}
		desiredHeat =desiredHeat + ((desiredHeatDelta)? desiredHeatDelta.toFloat():0)                    
		key = "desiredCoolTemp$indiceSchedule"
		def coolTemp = settings[key]
		if (!coolTemp) {
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName},about to apply default cool settings",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)
			desiredCool = (scale=='C') ? 23:75				// by default, 23C/75F is the target cool temp
		} else {
			desiredCool = coolTemp.toFloat()
		}
		desiredCool =desiredCool + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)                    
		def puckSetpoint=((desiredCool + desiredHeat) /2).toFloat().round(1)   // calculate the median     
		def currentPuckSetpoint=roomPuck?.currentDesiredTemperature
		if (currentPuckSetpoint != puckSetpoint) {            
			roomPuck.setDesiredTemp(puckSetpoint)
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName}, ${roomPuck}'s desired setpoint=${puckSetpoint} set in ${mode} mode",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)            
		} else {
			traceEvent(settings.logFilter,"setRoomPuckSettings>schedule ${scheduleName}, in room ${roomName}, ${roomPuck}'s desired setpoint=${puckSetpoint} already set in ${mode} mode",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)            
		} 
	} 
}
private def setFlairVentSettings(indiceSchedule,indiceZone, indiceRoom) {

	def scale = (state?.scale)?: getTemperatureScale()
	float desiredHeat, desiredCool
	boolean setClimate = false
	def key = "zoneName$indiceZone"
	def zoneName = settings[key]

	key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	key = "roomName$indiceRoom"
	def roomName = settings[key]
	key  = "desiredHeatDeltaTemp$indiceZone"
	def desiredHeatDelta =  (state?."desiredHeatTempDelta$indiceZone")? state?."desiredHeatTempDelta$indiceZone".toFloat(): settings[key]
	key  = "desiredCoolDeltaTemp$indiceZone"
	def desiredCoolDelta =  (state?."desiredCoolTempDelta$indiceZone")? state?."desiredCoolTempDelta$indiceZone".toFloat(): settings[key]
    

	String mode = thermostat?.currentValue("thermostatMode") // get the mode at the main thermostat
	traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName},about to scan room for Flair vents in ${mode} mode",settings.detailedNotif)
	if (mode.contains('heat')) {
		key = "desiredHeatTemp$indiceSchedule"
		def heatTemp = settings[key]
		if (!heatTemp) {
			traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName}, in room ${roomName},about to apply default heat settings",settings.detailedNotif)
			desiredHeat = (scale=='C') ? 21:72				// by default, 21C/72F is the target heat temp
		} else {
			desiredHeat = heatTemp.toFloat()
		}
		desiredHeat =desiredHeat + ((desiredHeatDelta)?desiredHeatDelta.toFloat():0)                    
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			if ((ventSwitch != null) && (ventSwitch.hasCommand("setRoomSetpoint"))) {
				def currentHeatSetpoint=ventSwitch?.currentRmSetpoint
				if (currentHeatSetpoint != desiredHeat) {                    
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName}, found ${ventSwitch} in room",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
					ventSwitch.setRoomSetpoint(desiredHeat)
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName},${ventSwitch} room's setpoint=${desiredHeat} set in ${mode} mode",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
				} else {
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName},${ventSwitch} room's setpoint is already set to ${desiredHeat} in ${mode} mode",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
                    
				}                    
				break                    
			}
		} /* end for */            
	} else if (mode == 'cool') {
		key = "desiredCoolTemp$indiceSchedule"
		def coolTemp = settings[key]
		if (!coolTemp) {
			traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName}, in room ${roomName},about to apply default cool settings",settings.detailedNotif)
			desiredCool = (scale=='C') ? 23:75				// by default, 23C/75F is the target cool temp
		} else {
			desiredCool = coolTemp.toFloat()
		}
		desiredCool =desiredCool + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)                    
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			if ((ventSwitch != null) && (ventSwitch.hasCommand("setRoomSetpoint"))) {
				def currentCoolSetpoint=ventSwitch?.currentRmSetpoint
				if (currentCoolSetpoint != desiredCool) {                    
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName}, found ${ventSwitch} in room",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
					ventSwitch.setRoomSetpoint(desiredCool)
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName},${ventSwitch} room's setpoint=${desiredCool} set in ${mode} mode",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
				} else {
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName},${ventSwitch} room's setpoint is already set to ${desiredCool} in ${mode} mode",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
                    
				}                    
				break                    
			}
		} /* end for */            
	} else if (mode in ['auto', 'off', 'eco']) {
		key = "desiredHeatTemp$indiceSchedule"
		def heatTemp = settings[key]
		if (!heatTemp) {
			traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName}, in room ${roomName},about to apply default heat settings",settings.detailedNotif,
				GLOBAL_LOG_INFO)
			desiredHeat = (scale=='C') ? 21:72				// by default, 21C/72F is the target heat temp
		} else {
			desiredHeat = heatTemp.toFloat()
		}
		desiredHeat =desiredHeat + ((desiredHeatDelta)? desiredHeatDelta.toFloat():0)                    
		key = "desiredCoolTemp$indiceSchedule"
		def coolTemp = settings[key]
		if (!coolTemp) {
			traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName}, in room ${roomName},about to apply default cool settings",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)
			desiredCool = (scale=='C') ? 23:75				// by default, 23C/75F is the target cool temp
		} else {
			desiredCool = coolTemp.toFloat()
		}
		desiredCool =desiredCool + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)                    
		def roomSetpoint=((desiredCool + desiredHeat) /2).toFloat().round(1)   // calculate the median     
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			if ((ventSwitch != null) && (ventSwitch.hasCommand("setRoomSetpoint"))) {
				def currentSetpoint=ventSwitch?.currentRmSetpoint
				if (currentSetpoint != roomSetpoint) {                    
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName}, found ${ventSwitch} in room",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
					ventSwitch.setRoomSetpoint(roomSetpoint)
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName},${ventSwitch} room's setpoint=${roomSetpoint} set in ${mode} mode",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
				} else {
					traceEvent(settings.logFilter,"setFlairVentSettings>schedule ${scheduleName},in room ${roomName},${ventSwitch} room's setpoint is already set to ${roomSetpoint} in ${mode} mode",
						settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
                    
				}                    
				break                    
			}
		} /* end for */            
	} 
}

private def setRoomTstatSettings(indiceSchedule,indiceZone, indiceRoom) {

	def scale = (state?.scale)?: getTemperatureScale()
	float desiredHeat, desiredCool
	boolean setClimate = false
	def key = "zoneName$indiceZone"
	def zoneName = settings[key]

	key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key = "givenClimate$indiceSchedule"
	def climateName = settings[key]

	key = "roomTstat$indiceRoom"
	def roomTstat = settings[key]

	key = "roomName$indiceRoom"
	def roomName = settings[key]

	key  = "desiredHeatDeltaTemp$indiceZone"
	def desiredHeatDelta =  (state?."desiredHeatDelta$indiceZone")? state?."desiredHeatTempDelta$indiceZone".toFloat(): settings[key]
	key  = "desiredCoolDeltaTemp$indiceZone"
	def desiredCoolDelta =  (state?."desiredCoolTempDelta$indiceZone")? state?."desiredCoolTempDelta$indiceZone".toFloat(): settings[key]

	if (roomTstat==thermostat) { 
		traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, ${roomTstat} is the master tstat, so don't do anything here...",
				settings.detailedNotif, GLOBAL_LOG_WARN)
		return
	}             
 
	key = "useMasterTstatSetpointsFlag$indiceSchedule"
	def useMasterTstatSetpoints = (settings[key]) ?: false
    

	traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply zone's temp settings at ${roomTstat}",settings.detailedNotif)
	String mode = thermostat?.currentValue("thermostatMode") // get the mode at the main thermostat
	String currentPresence= thermostat.currentValue("presence")            
	String currentRoomTstatMode=roomTstat?.currentValue("thermostatMode")
	String currentRoomTstatPresence= roomTstat?.currentValue("presence")            
    
	if (roomTstat?.hasCapability("Switch") ) {  // turn on the roomTstat if it's off      
		String currentSwitch=roomTstat?.currentSwitch 
		if (currentSwitch == 'off' && mode != 'off') {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, about to turn on ${roomTstat} as it's currently off",
				settings.detailedNotif, GLOBAL_LOG_INFO)
			roomTstat.on()            
		}            
	}             
    
	if (roomTstat?.hasCommand("eco") && (currentRoomTstatMode == 'eco') && (currentRoomTstatMode != mode)) {  // set the roomTstat to home if in eco mode      
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, about to set ${roomTstat} to home as it's currently in eco mode",
				settings.detailedNotif, GLOBAL_LOG_INFO)
			if (roomTstat.hasCommand("home")) {   
				roomTstat.home()            
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, ${roomTstat} was set to home as it's been previoulsy in eco mode",
					settings.detailedNotif, GLOBAL_LOG_INFO)
                    
			} else if (roomTstat.hasCommand("present")) {   
				roomTstat.present()            
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, ${roomTstat} was set to present as it's been previoulsy in eco mode",
					settings.detailedNotif, GLOBAL_LOG_INFO)
                    
			}     
	} else if ((climateName) && (roomTstat?.hasCommand("setThisTstatClimate"))) {
		def currentSetClimate=roomTstat?.currentValue("setClimate")
		if (climateName != currentSetClimate) {         
			try {
				roomTstat?.setThisTstatClimate(climateName)
				setClimate = true
			} catch (any) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},not able to set climate ${climateName} at the thermostat ${roomTstat}",
					settings.detailedNotif, GLOBAL_LOG_INFO)
			}                
		} else {
			setClimate = true
		}                
	} else if ((roomTstat.hasCommand("away")) && (currentRoomTstatPresence == 'non present') && (currentRoomTstatPresence != currentPresence)) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, about to set ${roomTstat} to home as it's currently away",
				settings.detailedNotif, GLOBAL_LOG_INFO)
			if (roomTstat.hasCommand("home")) {   
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, ${roomTstat} was set to home as it's been previoulsy to away",
					settings.detailedNotif, GLOBAL_LOG_INFO)
				roomTstat.home()            
			}  else if (roomTstat.hasCommand("present")) {   
				roomTstat.present()            
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, ${roomTstat} was set to present as it's been previoulsy to away",
					settings.detailedNotif, GLOBAL_LOG_INFO)
                    
			}            
	}
       
	if (mode.contains('heat')) {
		if (currentRoomTstatMode != mode) {
			try {    
				if (roomTstat.hasCommand('heat')) roomTstat.heat()
				else roomTstat.setThermostatMode('heat')                
			} catch (any) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
					true, GLOBAL_LOG_WARN,settings.detailedNotif)
				return            
			}                
		}                
		if (!setClimate) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply zone's temp settings",settings.detailedNotif)
			def heatTemp            
			if (!useMasterTstatSetpoints) {
				key = "desiredHeatTemp$indiceSchedule"
				heatTemp = settings[key]
			} else {
				heatTemp = thermostat.currentHeatingSetpoint
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, about to apply master tstat's heating setpoint ($heatTemp)",settings.detailedNotif)
			}            
			if (!heatTemp) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply default heat settings",settings.detailedNotif)
				desiredHeat = (scale=='C') ? 21:72				// by default, 21C/72F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			desiredHeat =desiredHeat + ((desiredHeatDelta)?desiredHeatDelta.toFloat():0)                    
			def currentHeatSetpoint=roomTstat?.currentHeatingSetpoint
			if (currentHeatSetpoint != desiredHeat) {            
				roomTstat.setHeatingSetpoint(desiredHeat)
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},${roomTstat}'s desiredHeat=${desiredHeat} set in ${mode} mode",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
			} else {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},${roomTstat}'s desiredHeat=${desiredHeat} already set in ${mode} mode",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
			} 
		} /* if not setClimate */
	} else if (mode == 'cool') {
		if (currentRoomTstatMode != mode) {
			try {    
				if (roomTstat.hasCommand('cool')) roomTstat.cool()
				else roomTstat.setThermostatMode('cool')                
			} catch (any) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
					true, GLOBAL_LOG_WARN,settings.detailedNotif)
				return            
			}                
		}                
		if (!setClimate) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply zone's temp settings",settings.detailedNotif)
			def coolTemp 
			if (!useMasterTstatSetpoints) {
				key = "desiredCoolTemp$indiceSchedule"
				coolTemp = settings[key]
			} else {
				coolTemp = thermostat.currentCoolingSetpoint
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, about to apply master tstat's cooling setpoint ($coolTemp)",settings.detailedNotif)
			}            
			if (!coolTemp) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply default cool settings",settings.detailedNotif)
				desiredCool = (scale=='C') ? 23:75				// by default, 23C/75F is the target cool temp
			} else {
				desiredCool = coolTemp.toFloat()
			}
			desiredCool =desiredCool + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)                    
			def currentCoolSetpoint=roomTstat?.currentCoolingSetpoint
			if (currentCoolSetpoint != desiredCool) {            
				roomTstat.setCoolingSetpoint(desiredCool)
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, ${roomTstat}'s desiredCool=${desiredCool} set in ${mode} mode",settings.detailedNotif,
					GLOBAL_LOG_INFO,settings.detailedNotif)            
			} else {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, ${roomTstat}'s desiredCool=${desiredCool} already set in ${mode} mode",settings.detailedNotif,
					GLOBAL_LOG_INFO,settings.detailedNotif)            
			} 
		} /* if not setClimate */
	} else if (mode == 'dry') {
		if (currentRoomTstatMode != mode) {
			try {    
				if (roomTstat.hasCommand('dry')) roomTstat.dry()
				else roomTstat.setThermostatMode('dry')                
			} catch (any) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
					true, GLOBAL_LOG_WARN,settings.detailedNotif)
				return            
			}                
		}                
	} else if (mode == 'auto') {
		if (!currentRoomTstatMode.contains(mode)) {
			try {    
				if (roomTstat.hasCommand('auto')) roomTstat.auto()
				else roomTstat.setThermostatMode('auto')                
			} catch (any) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
					true, GLOBAL_LOG_WARN,settings.detailedNotif)
			}                
		}                
		if (!setClimate) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply zone's temp settings",settings.detailedNotif)
			def heatTemp 
			if (!useMasterTstatSetpoints) {
				key = "desiredHeatTemp$indiceSchedule"
				heatTemp = settings[key]
			} else {
				heatTemp = thermostat.currentHeatingSetpoint
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, about to apply master tstat's heating setpoint ($heatTemp)",settings.detailedNotif)
			}            
			if (!heatTemp) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply default heat settings",settings.detailedNotif,
					GLOBAL_LOG_INFO)
				desiredHeat = (scale=='C') ? 21:72				// by default, 21C/72F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			desiredHeat =desiredHeat + ((desiredHeatDelta)?desiredHeatDelta.toFloat():0)                    
			def currentHeatSetpoint=roomTstat?.currentHeatingSetpoint
			if (currentHeatSetpoint != desiredHeat) {            
				roomTstat.setHeatingSetpoint(desiredHeat)
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},${roomTstat}'s desiredHeat=${desiredHeat} set in ${mode} mode",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
			} else {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},${roomTstat}'s desiredHeat=${desiredHeat} already set in ${mode} mode",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)                
			}
			key = "desiredCoolTemp$indiceSchedule"
			def coolTemp 
			if (!useMasterTstatSetpoints) {
				key = "desiredCoolTemp$indiceSchedule"
				coolTemp = settings[key]
			} else {
				coolTemp = thermostat.currentCoolingSetpoint
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, about to apply master tstat's cooling setpoint ($coolTemp)",settings.detailedNotif)
			}            
			if (!coolTemp) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply default cool settings",settings.detailedNotif,
					GLOBAL_LOG_INFO,settings.detailedNotif)
				desiredCool = (scale=='C') ? 23:75				// by default, 23C/75F is the target cool temp
			} else {
				desiredCool = coolTemp.toFloat()
			}
			desiredCool =desiredCool + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)                    
			def currentCoolSetpoint=roomTstat?.currentCoolingSetpoint
			if (currentCoolSetpoint != desiredCool) {            
				roomTstat.setCoolingSetpoint(desiredCool)
				traceEvent(settings.logFilter,"schedule ${scheduleName},in room ${roomName},${roomTstat}'s desiredCool=${desiredCool} set in ${mode} mode",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
			} else {
				traceEvent(settings.logFilter,"schedule ${scheduleName},in room ${roomName},${roomTstat}'s desiredCool=${desiredCool} already set in ${mode} mode",
					settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
			} 
		} /* if not setClimate */
	} else if (mode == 'eco') {
		if (currentRoomTstatMode != mode) {
			try {    
				if (roomTstat.hasCommand('eco')) roomTstat.eco()
				else roomTstat.setThermostatMode('eco')                
			} catch (any) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
					true, GLOBAL_LOG_WARN,settings.detailedNotif)
			}                
		}                
    
	} else if (mode == 'off') {
		if (currentRoomTstatMode != mode) {
			try {    
				if (roomTstat.hasCommand('off')) roomTstat.off()
				else roomTstat.setThermostatMode('off')                
			} catch (any) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
					true, GLOBAL_LOG_WARN,settings.detailedNotif)
			}                
		}                
    
	} 
}

private def setAllRoomTstatsSettings(indiceSchedule,indiceZone) {
	boolean foundRoomTstat = false
	def	key= "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	key = "includedRooms$indiceZone"
	def rooms = settings[key]
	boolean puck_present_in_zone=false    
    
	for (room in rooms) {

		def roomDetails=room.split(':')
		def indiceRoom = roomDetails[0]
		def roomName = roomDetails[1]
		key = "inactiveRoomFlag$indiceRoom"
		boolean inactiveRoom = settings[key]
		if (inactiveRoom) {
			traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},room $roomName,inactive: $inactiveRoom",settings.detailedNotif)
			continue
		}                
		key = "needOccupiedFlag$indiceRoom"
		def needOccupied = (settings[key]) ?: false
		key = "roomTstat$indiceRoom"
		def roomTstat = settings[key]
		if (roomTstat) { 
			foundRoomTstat=true
		}
		key = "pressureSensor$indiceRoom"
		def roomPuck = settings[key]
		if (needOccupied) {
			key = "motionSensor$indiceRoom"
			def motionSensor = settings[key]
			if (motionSensor != null) {
				if (isRoomOccupied(motionSensor, indiceRoom)) {
					traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},for occupied room ${roomName},about to call setRoomTstatSettings ",
						settings.detailedNotif, GLOBAL_LOG_INFO)                    
					if (roomTstat) { 
						setRoomTstatSettings(indiceSchedule,indiceZone, indiceRoom)
					}                        
					if (roomPuck && roomPuck.hasCommand("setDesiredTemp")) {
						setRoomPuckSettings(indiceSchedule,indiceZone, indiceRoom)
						puck_present_in_zone=true
					}                        
				} else {
					if (roomTstat) {
						def currentMode=roomTstat?.currentValue("thermostatMode")
						traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},room ${roomName} not occupied, not sending setpoints", settings.detailedNotif,
							GLOBAL_LOG_INFO)
						def currentPresence=roomTstat.currentValue("presence")           
						if ((settings.turnOffRoomTstatsFlag) && ((!currentMode in ['off', 'eco']) || currentPresence =='present')) {
							traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},$roomTstat to be set to eco/off/away as turnOffRoomTstatsFlag=true",
								settings.detailedNotif)            
							if (roomTstat.hasCommand('eco')) {
								roomTstat.eco()
							} else if (roomTstat.hasCommand('away')) {                    
								roomTstat.away()        
							} else if (roomTstat.hasCommand('off')) {                    
								roomTstat.off()        
							} else {
								traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule $scheduleName, $roomTstat doesn't support off/eco/away command",
								settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)
							}                
						}
					}
				}
			}
		} else {
			traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},for room ${roomName},about to call setRoomTstatSettings ",
				settings.detailedNotif)            
			if (roomTstat) { 
				setRoomTstatSettings(indiceSchedule,indiceZone, indiceRoom)
			}                        
			if (roomPuck && roomPuck.hasCommand("setDesiredTemp")) {
				setRoomPuckSettings(indiceSchedule,indiceZone, indiceRoom)
				puck_present_in_zone=true
			}                        
			
		}
	} /* end for rooms*/

	if (puck_present_in_zone) {
    
		for (room in rooms) {
			def roomDetails=room.split(':')
			def indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]
			key = "inactiveRoomFlag$indiceRoom"
			boolean inactiveRoom = settings[key]
			if (inactiveRoom) {
				traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},room $roomName,inactive: $inactiveRoom",settings.detailedNotif)
				continue
			}                
			setFlairVentSettings(indiceSchedule,indiceZone, indiceRoom) // check if any Flair Vents are present, if so set the room's setpoint                        
		} /* end for */
	}
    
    
	return foundRoomTstat
}
private def found_room_tstat_in_rooms() {
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		def key = "roomName$indiceRoom"
		def roomName = settings[key]
		key = "roomTstat$indiceRoom"
		def roomTstat = settings[key]
		if (roomTstat) { 
			traceEvent(settings.logFilter,"found_room_tstat_in_rooms>found $roomTstat in room ${roomName}...",
				settings.detailedNotif)            
			return true            
		}
	}        
	return false    
}

def turn_off_room_tstats_outside_zones(data) {   
	def indiceSchedule = data.indice
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
						getAllTempsForAverage(z, true, "off")                    
						traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule $scheduleName, ${zoneName}'s activeSchedule set to off, outside of scheduled zones",settings.detailedNotif)
						/*
						def appName = getVirtualZoneChildName()
                        def dni = "${appName}.${zoneName}.$z"  
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
			if (settings.turnOffRoomTstatsFlag) {    
				key = "roomTstat${indiceRoom}"
				def roomTstat = settings[key] 
				if (!roomTstat) {
					traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>no room tstat in $roomName, skipping room scan",settings.detailedNotif)
					continue			        
				} 
				if (roomTstat==thermostat) { 
					traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule ${scheduleName}, in room ${roomName}, ${roomTstat} is the master tstat, so don't do anything here...",
						settings.detailedNotif, GLOBAL_LOG_WARN)
					continue
				}             
                 
				traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>zone=${zoneName}: zoneDetails= ${zoneDetails}, found $roomTstat in $roomName",settings.detailedNotif)
				String currentMode=roomTstat?.currentValue("thermostatMode")
				if (currentMode in ['off', 'eco']) {
					traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>$roomTstat is already off or in eco mode, skipping room scan",settings.detailedNotif)
					continue			        
				}        
				try {     // send the off/eco/away command to the room tstat as it's outside the zones for the schedule
					traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule $scheduleName, about to send off/eco/away command to $roomTstat as $roomName is outside the zone ($zones)",
						settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)
					if (roomTstat.hasCommand('eco')) {
						roomTstat.eco()
					} else if (roomTstat.hasCommand('away')) {                    
						roomTstat.away()        
					} else if (roomTstat.hasCommand('off')) {                    
						roomTstat.off()        
					} else {
						traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>schedule $scheduleName, $roomTstat doesn't support off/eco/away command",
							settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)
					}                
				} catch (e) {
					traceEvent(settings.logFilter,"turn_off_room_tstats_outside_zones>exception $e, schedule $scheduleName, not able to send off/eco/away command to $roomTstat, as $roomName is outside the zone ($zones)",
						settings.detailedNotif, GLOBAL_LOG_WARN, settings.detailedNotif)
				}
			} /* end if turnOffRoomTstatSFlag */                
            
		} /* end if room not in active zones */       
	} /* end for each room */
}
private void set_fan_speed_in_rooms(indiceSchedule, fanSpeed) {   
	def	key= "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		key = "roomTstat${indiceRoom}"
		def roomTstat = settings[key] 
		key = "roomName$indiceRoom"
		def roomName = settings[key]
		if (!roomName || roomName=='null') {
			continue
		}
		key = "inactiveRoomFlag$indiceRoom"
		boolean inactiveRoom=settings[key]
		if (inactiveRoom) {
			traceEvent(settings.logFilter,"set_fan_speed_in_rooms>schedule $scheduleName, room $roomName, inactive:$inactiveRoom, skipping room scan",settings.detailedNotif)
			continue
		}
		        
		if (!roomTstat) {
			traceEvent(settings.logFilter,"set_fan_speed_in_rooms>no room tstat in $roomName, skipping room scan",settings.detailedNotif)
			continue			        
		}        
        
		String currentFanMode=roomTstat.currentValue("thermostatFanMode")  
        
		if (roomTstat?.hasCapability("Switch")) {  // turn on the roomTstat if it's off      
			String currentSwitch=roomTstat?.currentSwitch 
			if (currentSwitch == 'off') {
				roomTstat.on()            
			}            
		}             
                 
		if ((roomTstat.hasCommand("switchFanHigh") && (currentFanMode?.toLowerCase().contains(fanSpeed)))) {
			traceEvent(settings.logFilter,"set_fan_speed_in_rooms>$roomTstat's fan is already in $fanMode, skipping room scan",settings.detailedNotif)
			continue			        
		}
		if ((!roomTstat.hasCommand("fanHigh")) && (fanSpeed==currentFanMode)) {
			traceEvent(settings.logFilter,"set_fan_speed_in_rooms>$roomTstat's fan is already $fanMode, skipping room scan",settings.detailedNotif)
			continue			        
		}        
		if (roomTstat.hasCommand("fanHigh")) {
			String currentFanSpeed=roomTstat.currentValue("thermostatFanSpeed")        
			if ((currentFanSpeed) && (currentFanSpeed == fanSpeed)) {
				traceEvent(settings.logFilter,"set_fan_speed_in_rooms>$roomTstat's fan is already in $fanSpeed speed, skipping room scan",settings.detailedNotif)
				continue			        
			}        
		}    
		if (roomTstat.hasCommand("switchFanLevel")) {
			String currentFanSpeed=roomTstat.currentValue("fanLevel")        
			if ((currentFanSpeed) && (currentFanSpeed == fanSpeed)) {
				traceEvent(settings.logFilter,"set_fan_speed_in_rooms>$roomTstat's fan is already in $fanSpeed speed, skipping room scan",settings.detailedNotif)
				continue			        
			}        
		}    
		if (((!roomTstat.hasCommand("fanHigh") && (!roomTstat.hasCommand("switchFanHigh")) && (!roomTstat.hasCommand("fanHigh")) ) && ((fanSpeed in ['high', 'medium', 'low', 'on']) && (currentFanMode == 'on')))) {
			traceEvent(settings.logFilter,"set_fan_speed_in_rooms>$roomTstat's fan is already $fanMode, skipping room scan",settings.detailedNotif)
			continue			        
		}        
		boolean found_room_in_zone = false
		key = "includedZones$indiceSchedule"
		def zones = settings[key]
		for (zone in zones) {		// look up for the room in the includedRooms for each zone
			def zoneDetails=zone.split(':')
			traceEvent(settings.logFilter,"set_fan_speed_in_rooms>zone=${zone}: zoneDetails= ${zoneDetails}, found $roomTstat in $roomName, about to scan zone for room",settings.detailedNotif)
			def indiceZone = zoneDetails[0]
			def zoneName = zoneDetails[1]
			if (!zoneName || zoneName=='null') {
				continue
			}
			key = "inactiveZoneFlag$indiceZone"
			boolean inactiveZone=(state?."inactiveZone$indiceZone"!= null)? state?."inactiveZone$indiceZone".toBoolean() :settings[key]
			if (inactiveZone) {
				traceEvent(settings.logFilter,"set_fan_speed_in_rooms>schedule $scheduleName, zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
				continue
			}
			key = "includedRooms$indiceZone"
			def rooms = settings[key]
			if (rooms.toString().contains(roomName.toString().trim())) {
				traceEvent(settings.logFilter,"set_fan_speed_in_rooms>schedule $scheduleName, found $roomName in zone $zoneName",settings.detailedNotif)
				found_room_in_zone=true
				break                
			} 
		} /* end for each zone */
		if (found_room_in_zone) { 		        
			try {        
				if (fanSpeed=='high') {            
					if (roomTstat.hasCommand("fanHigh")) {
						roomTstat.fanHigh()   
					} else if (roomTstat.hasCommand("switchFanHigh")) {
						roomTstat.switchFanHigh()  
					} else if (roomTstat.hasCommand("highfan")) {                         
						roomTstat.highfan()  
					} else if (roomTstat.hasCommand("fanOn")) {
						roomTstat.fanOn()   
						fanSpeed='on'                        
					} 
				} else if (fanSpeed== 'medium') {
					if (roomTstat.hasCommand("fanMedium")) {
						roomTstat.fanMedium()   
					} else if (roomTstat.hasCommand("switchFanMed")) {
						roomTstat.switchFanMed()   
					} else if (roomTstat.hasCommand("mediumfan")) {                         
						roomTstat.mediumfan()  
					} else if (roomTstat.hasCommand("fanOn")) {
						roomTstat.fanOn()   
						fanSpeed='on'                        
					} 
				} else if (fanSpeed== 'low') {
					if (roomTstat.hasCommand("fanLow")) {
						roomTstat.fanLow()   
					} else if (roomTstat.hasCommand("switchFanLow")) {
						roomTstat.switchFanLow()   
					} else if (roomTstat.hasCommand("lowfan")) {                         
						roomTstat.lowfan()  
					} else if (roomTstat.hasCommand("fanOn")) {
						roomTstat.fanOn()   
						fanSpeed='on'                        
					} 
				} else if (roomTstat.hasCommand("fanAuto")) {
					roomTstat.fanAuto()   
				} 
				traceEvent(settings.logFilter,"schedule $scheduleName, roomTstat's fan $fanSpeed is set when supported",
					settings.detailedNotif, GLOBAL_LOG_INFO, settings.detailedNotif)
			} catch (e) {
				traceEvent(settings.logFilter,"exception $e, schedule $scheduleName, not able to set the fan $fanSpeed at $roomTstat",
					settings.detailedNotif, GLOBAL_LOG_WARN, settings.detailedNotif)
			}
		} /* if found_room in_zone */ 
	} /* end for each room */
}


private def getAllTempsForAverage(indiceZone,refreshSensors=false, activeInSchedule='on') {
	def tempAtSensor
	def key = "zoneName$indiceZone"
	def zoneName= settings[key]
	def mode = thermostat.currentThermostatMode    
	def adjustmentBasedOnContact=(settings.setTempAdjustmentContactFlag)?:false
	String motionValue="inactive"
	def indoorTemps = []
	key = "includedRooms$indiceZone"
	def rooms = settings[key]
	boolean isContactOpen=false 
	boolean foundTstatInRooms=false

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
			traceEvent(settings.logFilter,"getAllTempsForAverage>in zone $zoneName, room=${roomName},inactive:$inactiveRoom",settings.detailedNotif)
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
		traceEvent(settings.logFilter,"getAllTempsForAverage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
			settings.detailedNotif)        

		key = "tempSensor$indiceRoom"
		def tempSensor = settings[key]

		if ((refreshSensors) && ((tempSensor) && (tempSensor.hasCapability("Refresh")))) {
			// do a refresh to get the latest temp value
			try {        
				tempSensor.refresh()
			} catch (e) {
				traceEvent(settings.logFilter,"getAllTempsForAverage>not able to do a refresh() on $tempSensor",settings.detailedNotif, GLOBAL_LOG_INFO)
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
					traceEvent(settings.logFilter,"getAllTempsForAverage>not able to do a refresh() on $motionSensor",settings.detailedNotif, GLOBAL_LOG_INFO)
				}                
			}
			def isRoomOccupied=isRoomOccupied(motionSensor, indiceRoom)            
			if ((isRoomOccupied) || (!needOccupied)) {
				tempAtSensor = getSensorTempForAverage(indiceRoom)
				if (tempAtSensor != null) {
					indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
						traceEvent(settings.logFilter,"getAllTempsForAverage>added ${tempAtSensor.toString()} due to occupied room ${roomName} based on ${motionSensor}",
							settings.detailedNotif)
				}
				tempAtSensor = getSensorTempForAverage(indiceRoom,'roomTstat')
				if (tempAtSensor != null) {
					indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
					traceEvent(settings.logFilter,"getAllTempsForAverage>added ${tempAtSensor.toString()} due to occupied room ${roomName} based on ${motionSensor}",
						settings.detailedNotif)                        
				}
				if (isRoomOccupied) motionValue="active"    
			}
                
		} else {
			tempAtSensor = getSensorTempForAverage(indiceRoom)
			if (tempAtSensor != null) {
				traceEvent(settings.logFilter,"getAllTempsForAverage>added ${tempAtSensor.toString()} in room ${roomName}",settings.detailedNotif)
				indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
			}
			tempAtSensor = getSensorTempForAverage(indiceRoom,'roomTstat')
			if (tempAtSensor != null) {
				indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
					traceEvent(settings.logFilter,"getAllTempsForAverage>added ${tempAtSensor.toString()} in room ${roomName}",settings.detailedNotif)
			}
		}
		key = "roomTstat$indiceRoom"
		def roomTstat = settings[key]
		if (roomTstat) { 
			traceEvent(settings.logFilter,"getAllTempsForAverage>found $roomTstat in room ${roomName}...",
				settings.detailedNotif)            
			foundTstatInRooms=true            
		}
	} /* end for rooms*/

// Refresh My Virtual zone device if needed    
	def MAX_DELTA=20
	key = "virtualZoneFlag${indiceZone}"
	def virtualCreateZoneFlag = (settings[key]) ?:false
	if (virtualCreateZoneFlag) {
		def appName = getVirtualZoneChildName()
        def dni = "${appName}.${zoneName}.$indiceZone"  
		def d = getChildDevice(dni)
		if (d) {  
			def scale = (state?.scale) ?: getTemperatureScale()
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
				log.debug("TEMP: ${tempValueString}")		   
				d.sendEvent(name:"temperature", value: tempValueString, isDisplayed:true, isStateChange:isChange)
			}                
			def isChange = d.isStateChange(d, "motion", motionValue)	//check of state change
			d.sendEvent(name:"motion", value: motionValue, isDisplayed:true, isStateChange:isChange)
			def contactValue=((isContactOpen)?"open":"closed")             
			isChange = d.isStateChange(d, "contact", contactValue)	//check of state change
			d.sendEvent(name:"contact", value: contactValue, isDisplayed:true, isStateChange:isChange)
			isChange = d.isStateChange(d, "mode", mode)	//check of state change
			d.sendEvent(name:"mode", value:mode, isDisplayed:true, isStateChange:isChange)
            d.sendEvent(name:"thermostatMode", value:mode)
			isChange = d.isStateChange(d, "activeInSchedule", activeInSchedule)	//check of state change
			d.sendEvent(name:"activeInSchedule", value: activeInSchedule, isDisplayed: true, isStateChange: isChange)
            if(activeInSchedule == "on") {
            	d.sendEvent(name:"zoneSchedule", value: "Schedule Active")
            } else {
            	d.sendEvent(name:"zoneSchedule", value: "No Schedule Active")
            }
			traceEvent(settings.logFilter,"getAllTempsForAverage>$dni found, refreshed temp value with $avg_temp_in_zone, motion=$motionDetected",settings.detailedNotif)
			def coolSP, heatSP, thermostatSP
			double thermostatSetpoint            
			try {
				thermostatSP=thermostat?.currentThermostatSetpoint
			} catch (e) {
				traceEvent(settings.logFilter,"getAllTempsForAverage>no thermostat setpoint... exception $e",settings.detailedNotif)
			}            
			if (!thermostatSP) {
				traceEvent(settings.logFilter,"getAllTempsForAverage>no thermostat setpoint... will calculate it",settings.detailedNotif)
				if (mode in ['heat', 'auto', 'off', 'eco']) {                
					try {
						heatSP=(thermostat?.currentHeatingSetpoint)?:(scale=='C')?21:72
						thermostatSetpoint=heatSP.toDouble()                        
					} catch (e) {
						traceEvent(settings.logFilter,"getAllTempsForAverage>no thermostat heatingSetpoint... exceptio $e",settings.detailedNotif)
					}
				}                    
				if (mode in ['cool', 'auto', 'off', 'eco']) {                
					try {
						coolSP=(thermostat?.currentCoolingSetpoint)?:(scale=='C')?23:73
						thermostatSetpoint=coolSP.toDouble()                        
					} catch (e) {
						traceEvent(settings.logFilter,"getAllTempsForAverage>no thermostat cooling Setpoint... exceptio $e",settings.detailedNotif)
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
			def tempDelta=d.currentValue("tempDelta")
			tempDelta=(tempDelta==null)? tempDelta=0 : tempDelta.toDouble()            
			def desiredHeatDelta, desiredCoolDelta
			key  = "desiredHeatDeltaTemp$indiceZone"
			desiredHeatDelta =  settings[key]
			key  = "desiredCoolDeltaTemp$indiceZone"
			desiredCoolDelta = settings[key]
			def isChangeMainSP = d.isStateChange(d, "baselineSetpoint",tempValueString)	//check of state change            
			d.sendEvent(name:"baselineSetpoint", value: tempValueString, isStateChange:true, unit:scale)
            d.sendEvent(name:"zoneBaselineSetpoint", value: "${tempValueString} ??${scale}")
			traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating baselineSetpoint to $thermostatSetpointString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
//			if (!foundTstatInRooms) {
//				foundTstatInRooms=found_room_tstat_in_rooms()  // double check if there is any roomTstat in all rooms
//			}  
			// the delta in the app takes precedence over the ones set in the virtual zones
			if ((desiredHeatDelta != tempDelta) && (mode in ['heat', 'off','auto'])) {
				if (desiredHeatDelta) tempDelta=desiredHeatDelta.toDouble()
				if (tempDelta.toDouble().abs() <= MAX_DELTA) {            
					d.sendEvent(name:"tempDelta", value:tempDelta, isDisplayed:true, isStateChange:true)
                    log.debug("1 - ${tempDelta}")
                    d.sendEvent(name:"zoneTemperatureDelta", value:tempDelta, unit:scale)
 					traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating tempDelta to $tempDelta for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)

				} else { // reset the delta
					d.sendEvent(name:"tempDelta", value:MAX_DELTA, isDisplayed:true, isStateChange:true)
                    log.debug("3 - ${tempDelta}")
                    d.sendEvent(name:"zoneTemperatureDelta", value:MAX_DELTA, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>MAX delta is reached ($MAX_DELTA)",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}
				if (scale == 'C') {
					double newThermostatSetpoint=( thermostatSetpoint+ tempDelta.toDouble()).round(1)
					tempValueString = String.format('%2.1f', newThermostatSetpoint)
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else {
 					double newThermostatSetpoint=( thermostatSetpoint + tempDelta.toDouble()).round()       
					tempValueString = String.format('%2d', newThermostatSetpoint.intValue())           
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}                    
			}  
			// the delta in the app takes precedence over the ones set in the virtual zones
			if ((desiredCoolDelta != tempDelta) && (mode in ['cool', 'off','auto'])) {
				if (desiredCoolDelta) tempDelta=desiredCoolDelta.toDouble()
				if (tempDelta.toDouble().abs() <= MAX_DELTA) {            
					d.sendEvent(name:"tempDelta", value:tempDelta, isDisplayed:true, isStateChange:true)
                    log.debug("4 - ${tempDelta}")
                    d.sendEvent(name:"zoneTemperatureDelta", value:tempDelta, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating tempDelta to $tempDelta for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else { // reset the delta
					d.sendEvent(name:"tempDelta", value:MAX_DELTA, isDisplayed:true, isStateChange:true)
                    log.debug("5 - ${tempDelta}")
                    d.sendEvent(name:"zoneTemperatureDelta", value:MAX_DELTA, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>MAX delta is reached ($MAX_DELTA)",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}
				if (scale == 'C') {
					double newThermostatSetpoint=( thermostatSetpoint+ tempDelta.toDouble()).round(1)
					tempValueString = String.format('%2.1f', newThermostatSetpoint)
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else {
 					double newThermostatSetpoint=( thermostatSetpoint + tempDelta.toDouble()).round()       
					tempValueString = String.format('%2d', newThermostatSetpoint.intValue())           
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName}",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}                    
			}                
			if (isChangeMainSP) {
				if (scale == 'C') {
					double newThermostatSetpoint=( thermostatSetpoint + tempDelta.toDouble()).round(1)
					tempValueString = String.format('%2.1f', newThermostatSetpoint)
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName} following changes at baseline",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				} else {
 					double newThermostatSetpoint=( thermostatSetpoint + tempDelta.toDouble()).round()       
					tempValueString = String.format('%2d', newThermostatSetpoint.intValue())           
					isChange = d.isStateChange(d, "thermostatSetpoint",tempValueString)	//check of state change            
					d.sendEvent(name:"thermostatSetpoint", value: tempValueString, isStateChange:isChange, unit:scale)
					traceEvent(settings.logFilter,"getAllTempsForAverage>in $mode mode, updating thermostatSetpoint to $tempValueString for zone ${zoneName} following changes at baseline",settings.detailedNotif, GLOBAL_LOG_INFO, true)
				}
			}            
               
		} else {
			traceEvent(settings.logFilter,"getAllTempsForAverage>$dni not found, not able to update values",settings.detailedNotif)
		}
		            
	}
	return indoorTemps
    
}



private def set_fan_mode(indiceSchedule, overrideThreshold=false, overrideValue=null) {
	def	key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key = "givenFanMinTime${indiceSchedule}"
	def fanMinTime=settings[key]

	key = "fanMode$indiceSchedule"
	String fanMode = settings[key]
        
	if (fanMode == null) {
		return null   
	}
	key = "fanModeForThresholdOnlyFlag${indiceSchedule}"
	def fanModeForThresholdOnlyFlag = settings[key]

	def fanModeForThresholdOnly = (fanModeForThresholdOnlyFlag) ?: false
	if ((fanModeForThresholdOnly) && (!overrideThreshold)) {
    
		key = "moreFanThreshold$indiceSchedule"
		def moreFanThreshold = settings[key]
		traceEvent(settings.logFilter,"set_fan_mode>fanModeForThresholdOnly=$fanModeForThresholdOnly,morefanThreshold=$moreFanThreshold",settings.detailedNotif)
		if (moreFanThreshold == null) {
			return null    
		}
		if (outTempSensor != null) {

			float outdoorTemp = outTempSensor?.currentTemperature.toFloat().round(1)
        
			if (outdoorTemp < moreFanThreshold.toFloat()) {
				fanMode='off'	// fan mode should be set then at 'off'			
			}
		}            
	}    

	if (overrideValue != null) {
 		fanMode=overrideValue    
	}  
    
	/* Poll or refresh the thermostat to get latest values */
	if  (thermostat.hasCapability("Polling")) {
		try {        
			thermostat.poll()
		} catch (e) {
			traceEvent(settings.logFilter,"set_fan_mode>not able to do a poll() on ${thermostat}, exception ${e}", settings.detailedNotif, GLOBAL_LOG_ERROR)
		}                    
	}  else if  (thermostat.hasCapability("Refresh")) {
		try {        
			thermostat.refresh()
		} catch (e) {
			traceEvent(settings.logFilter,"set_fan_mode>not able to do a refresh() on ${thermostat}, exception ${e}",settings.detailedNotif, GLOBAL_LOG_ERROR)
		}                    
	}                    
    
	String currentFanMode=thermostat.currentValue("thermostatFanMode")
	if ((fanMode == currentFanMode) || ((fanMode=='off') && (!thermostat.hasCommand("fanOff")) && (currentFanMode=='auto'))) {
		traceEvent(settings.logFilter,"set_fan_mode>schedule ${scheduleName},fan already in $fanMode mode at thermostat ${thermostat}, exiting...",
			settings.detailedNotif)        
		return null
	}    

	try {
		if (fanMode=='off' && thermostat.hasCommand("fanOff")) {
			thermostat.fanOff()        
		} else if (fanMode=='on') {
			thermostat.fanOn()        
		} else if (fanMode=='circulate') {
			thermostat.fanCirculate()        
		} else {
			fanMode='auto'        
			thermostat.fanAuto()        
		}        
        
		traceEvent(settings.logFilter,"schedule ${scheduleName},set fan mode to ${fanMode} at thermostat ${thermostat} as requested",settings.detailedNotif, 
			GLOBAL_LOG_INFO,settings.detailedNotif)
	} catch (e) {
		traceEvent(settings.logFilter,"set_fan_mode>schedule ${scheduleName},not able to set fan mode to ${fanMode} (exception $e) at thermostat ${thermostat}",
			true, GLOBAL_LOG_ERROR)        
	}    
	return fanMode    
}



private def switch_thermostatMode(indiceSchedule) {

	if (outTempSensor == null) {
		return     
	}
    
	float outdoorTemp = outTempSensor.currentTemperature.toFloat().round(1)

	def key = "heatModeThreshold$indiceSchedule"
	def heatModeThreshold = settings[key]
	key = "coolModeThreshold$indiceSchedule"
	def coolModeThreshold = settings[key]
    
	if ((heatModeThreshold == null) && (coolModeThreshold ==null)) {
		traceEvent(settings.logFilter,"switch_thermostatMode>no adjustment variables set, exiting",settings.detailedNotif)
		return
	}        
	String currentMode = thermostat.currentValue("thermostatMode")
	def currentHeatPoint
	def currentCoolPoint    
	try {    
		currentHeatPoint = thermostat?.currentHeatingSetpoint
	} catch (any) {
		traceEvent(settings.logFilter,"switch_thermostatMode>currentMode=$currentMode,not able to get current Heating setpoint from $thermostat",
			settings.detailedNotif, GLOBAL_LOG_WARN)    
    
	}    
	try {    
		currentCoolPoint = thermostat?.currentCoolingSetpoint
	} catch (any) {
		traceEvent(settings.logFilter,"switch_thermostatMode>currentMode=$currentMode,not able to get current Cooling setpoint from $thermostat",
			settings.detailedNotif, GLOBAL_LOG_WARN)    
    
	}    
	traceEvent(settings.logFilter,"switch_thermostatMode>currentMode=$currentMode, outdoor temperature=$outdoorTemp, coolTempThreshold=$coolTempThreshold, heatTempThreshold=$heatTempThreshold",
		settings.detailedNotif)    
	if ((heatModeThreshold != null) && (outdoorTemp < heatModeThreshold?.toFloat())) {
		if (currentMode != "heat") {
			def newMode = "heat"
			thermostat.setThermostatMode(newMode)
			traceEvent(settings.logFilter,"switch_thermostatMode>thermostat mode set to $newMode",settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)    
			state?.scheduleHeatSetpoint=currentHeatPoint.toFloat()      // Set for later processing in adjust_more_less_heat_cool()     
		}
	} else if ((coolModeThreshold != null) && (outdoorTemp > coolModeThreshold?.toFloat())) {
		if (currentMode != "cool") {
			def newMode = "cool"
			thermostat.setThermostatMode(newMode)
			traceEvent(settings.logFilter,"switch_thermostatMode>thermostat mode set to $newMode",settings.detailedNotifGLOBAL_LOG_INFO,settings.detailedNotif)    
			state?.scheduleCoolSetpoint=currentCoolPoint.toFloat()      // Set for later processing in adjust_more_less_heat_cool() ,     
		}
	} else if (!currentMode in ['off', 'eco', 'auto']) {
			def newMode = "auto"
			thermostat.setThermostatMode(newMode)
			traceEvent(settings.logFilter,"switch_thermostatMode>thermostat mode set to $newMode",settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)    
	}    

}
   


private def adjust_tstat_for_more_less_heat_cool(data) {
	def indiceSchedule = data.indiceSchedule
	def scale = (state?.scale)?: getTemperatureScale()
	def key = "setRoomThermostatsOnlyFlag$indiceSchedule"
	def setRoomThermostatsOnlyFlag = settings[key]
	def setRoomThermostatsOnly = (setRoomThermostatsOnlyFlag) ?: false
	key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	if (setRoomThermostatsOnly) {
		traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName},all room Tstats set and setRoomThermostatsOnlyFlag= true,exiting",
			settings.detailedNotif)            
		return				    
	}    

	if (outTempSensor == null) {
		traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>no outdoor temp sensor set, exiting",settings.detailedNotif)    
		return     
	}
	
	key = "moreHeatThreshold$indiceSchedule"
	def moreHeatThreshold = settings[key]
	key = "moreCoolThreshold$indiceSchedule"
	def moreCoolThreshold = settings[key]
	key = "heatModeThreshold$indiceSchedule"
	def heatModeThreshold = settings[key]
	key = "coolModeThreshold$indiceSchedule"
	def coolModeThreshold = settings[key]


	if ((moreHeatThreshold == null) && (moreCoolThreshold ==null) && 
		(heatModeThreshold == null) && (coolModeThreshold ==null)) {
		traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>no adjustment variables set, exiting",settings.detailedNotif)
		return
	}
	
	float outdoorTemp = outTempSensor?.currentTemperature.toFloat().round(1)
	String currentMode = thermostat.currentValue("thermostatMode")
	float currentHeatPoint 
    
	try {
		currentHeatPoint= thermostat?.currentHeatingSetpoint?.toFloat()?.round(1)
	} catch (any) {
    
		traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>currentMode=$currentMode,not able to get current Heating Setpoint from $thermostat", 
			settings.detailedNotif, GLOBAL_LOG_WARN)                
	}    
	float currentCoolPoint 
	try {
		currentCoolPoint = thermostat?.currentCoolingSetpoint?.toFloat()?.round(1)
	} catch (any) {
		traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>currentMode=$currentMode,not able to get current Cooling Setpoint from $thermostat", 
			settings.detailedNotif, GLOBAL_LOG_WARN)                
	}    
    
	float targetTstatTemp    
	traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>scheduleName=$scheduleName,currentMode=$currentMode,outdoorTemp=$outdoorTemp,moreCoolThreshold=$moreCoolThreshold,  moreHeatThreshold=$moreHeatThreshold," +
		"coolModeThreshold=$coolModeThreshold,heatModeThreshold=$heatModeThreshold,currentHeatSetpoint=$currentHeatPoint,currentCoolSetpoint=$currentCoolPoint",
		settings.detailedNotif)                
	key = "givenMaxTempDiff$indiceSchedule"
	def givenMaxTempDiff = settings[key]
	def input_max_temp_diff = (givenMaxTempDiff!=null) ?givenMaxTempDiff: (scale=='C')? 2: 5 // 2C/5F temp differential is applied by default

	float max_temp_diff = input_max_temp_diff.toFloat().round(1)
	if (currentMode in ['heat', 'emergency heat', 'auto']) {
		if ((moreHeatThreshold != null) && (outdoorTemp <= moreHeatThreshold?.toFloat()))  {
			targetTstatTemp = (currentHeatPoint + max_temp_diff).round(1)
			float temp_diff = (state?.scheduleHeatSetpoint - targetTstatTemp).toFloat().round(1)
			if (temp_diff.abs() > max_temp_diff) {
				traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName}:max_temp_diff= ${max_temp_diff},temp_diff=${temp_diff},too much adjustment for more heat",
					settings.detailedNotif)                    
				targetTstatTemp = (state?.scheduleHeatSetpoint  + max_temp_diff).toFloat().round(1)
			}
			traceEvent(settings.logFilter,"heating setPoint now= ${targetTstatTemp}, outdoorTemp <=${moreHeatThreshold}",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)
			thermostat.setHeatingSetpoint(targetTstatTemp)
		} else if ((heatModeThreshold != null) && (outdoorTemp >= heatModeThreshold?.toFloat())) {
        	
			targetTstatTemp = (currentHeatPoint - max_temp_diff).round(1)
			float temp_diff = (state?.scheduleHeatSetpoint - targetTstatTemp).toFloat().round(1)
			if (temp_diff.abs() > max_temp_diff) {
				traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName}:max_temp_diff= ${max_temp_diff},temp_diff=${temp_diff},too much adjustment for heat mode",
					settings.detailedNotif)                
				targetTstatTemp = (state?.scheduleHeatSetpoint  - max_temp_diff).toFloat().round(1)
			}
			thermostat.setHeatingSetpoint(targetTstatTemp)
			traceEvent(settings.logFilter,"heating setPoint now= ${targetTstatTemp}, outdoorTemp >=${heatModeThreshold}", settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)
        
		} else {
			switch_thermostatMode(indiceSchedule)        
		}        
	}
	if (currentMode in ['cool', 'auto']) {
    
		if ((moreCoolThreshold != null) && (outdoorTemp >= moreCoolThreshold?.toFloat())) {
			targetTstatTemp = (currentCoolPoint - max_temp_diff).round(1)
			float temp_diff = (state?.scheduleCoolSetpoint - targetTstatTemp).toFloat().round(1)
			if (temp_diff.abs() > max_temp_diff) {
				traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName}:max_temp_diff= ${max_temp_diff},temp_diff=${temp_diff},too much adjustment for more cool",
					settings.detailedNotif)                
				targetTstatTemp = (state?.scheduleCoolSetpoint  - max_temp_diff).round(1)
			}
			thermostat.setCoolingSetpoint(targetTstatTemp)
			traceEvent(settings.logFilter,"cooling setPoint now= ${targetTstatTemp}, outdoorTemp >=${moreCoolThreshold}",settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)
		} else if ((coolModeThreshold!=null) && (outdoorTemp <= coolModeThreshold?.toFloat())) {
			targetTstatTemp = (currentCoolPoint + max_temp_diff).round(1)
			float temp_diff = (state?.scheduleCoolSetpoint - targetTstatTemp).toFloat().round(1)
			if (temp_diff.abs() > max_temp_diff) {
				traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName}:max_temp_diff= ${max_temp_diff},temp_diff=${temp_diff},too much adjustment for cool mode",
					settings.detailedNotif)                
				targetTstatTemp = (state?.scheduleCoolSetpoint  + max_temp_diff).round(1)
			}
			thermostat.setCoolingSetpoint(targetTstatTemp)
			traceEvent(settings.logFilter,"cooling setPoint now= ${targetTstatTemp}, outdoorTemp <=${coolModeThreshold}", settings.detailedNotif,
				GLOBAL_LOG_INFO,settings.detailedNotif)
		} else {
        
			switch_thermostatMode(indiceSchedule)        
		}        
        
	} 
    // Check if auto mode needs to be switched to 'heat' or 'cool' based on thresholds
	if (currentMode== 'auto') {
		switch_thermostatMode(indiceSchedule)        
	}
}
// Main logic to adjust the thermostat setpoints now called by runIn to avoid timeouts

def adjust_thermostat_setpoints(data) {  
	def indiceSchedule = data.indiceSchedule
	def adjustmentOutdoorTempFlag = (setAdjustmentOutdoorTempFlag)?: false
	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	boolean isResidentPresent
	key="ecoWhenAway$indiceSchedule"
	def ecoWhenAwayFlag= settings[key]
    
    
	String currentLocationMode= location.currentMode.toString().toUpperCase()
	String currentTstatMode=thermostat.currentThermostatMode
	    
	if ((currentLocationMode.contains('AWAY')) && (ecoWhenAwayFlag)) {
		if ((thermostat.hasCommand('eco') && (currentTstatMode != 'eco'))) {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoints>schedule ${scheduleName},about to set $thermostat to eco mode",
				settings.detailedNotif)            
			set_main_tstat_to_AwayOrPresent('eco')
		}            
	} else if (!currentLocationMode.contains('AWAY')) {
		if (thermostat.hasCommand('present')) {      
			def currentPresence=thermostat.currentValue("presence")
			if (currentPresence != "present") {             
				traceEvent(settings.logFilter,"adjust_thermostat_setpoints>schedule ${scheduleName},about to set $thermostat to present",settings.detailedNotif)            
				set_main_tstat_to_AwayOrPresent('present')
			}                    
		}                    
	}  
	currentTstatMode=thermostat.currentThermostatMode
    
	if (scheduleName != state?.lastScheduleName) {
		adjust_thermostat_setpoint_in_zone(indiceSchedule)
		state?.lastScheduleName=scheduleName        
	} else {
		adjust_thermostat_setpoint_in_zone(indiceSchedule)
		isResidentPresent=verify_presence_based_on_motion_in_rooms()
		if ((isResidentPresent) && (!currentTstatMode in ['eco','off'])) {   
			if (adjustmentOutdoorTempFlag) {            	
				// check the thermsostat mode based on outdoor temp's thresholds (heat, cool) if any set                
				switch_thermostatMode(indiceSchedule) 
			}                    
		}                    

	}                    

}


private def adjust_thermostat_setpoint_in_zone(indiceSchedule) {
	float MIN_SETPOINT_ADJUSTMENT_IN_CELSIUS=0.5
	float MIN_SETPOINT_ADJUSTMENT_IN_FARENHEITS=1
	float desiredHeat, desiredCool, avg_indoor_temp
	float min_indoor_temp=0,max_indoor_temp=0    
	def currentHeatingSetpoint, currentCoolingSetpoint	    
	def scale = (state?.scale)?: getTemperatureScale()
	boolean setClimate=false
	boolean found_room_tstat=false
    
	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key = "includedZones$indiceSchedule"
	def zones = settings[key]
	key = "setRoomThermostatsOnlyFlag$indiceSchedule"
	def setRoomThermostatsOnlyFlag = settings[key]
	def setRoomThermostatsOnly = (setRoomThermostatsOnlyFlag) ?: false
	def indoor_all_zones_temps=[]
	state?.activeZones = zones // save the zones for the dashboard                

	traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}: zones= ${zones}",settings.detailedNotif)
	def adjustmentTempFlag = (setAdjustmentTempFlag)?: false
	def adjustmentFanFlag = (setAdjustmentFanFlag)?: false

	for (zone in zones) {

		def zoneDetails=zone.split(':')
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>zone=${zone}: zoneDetails= ${zoneDetails}", settings.detailedNotif)
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		if (!zoneName || (zoneName=='null')) {
			continue
		}
		key = "inactiveZoneFlag$indiceZone"
		boolean inactiveZone=(state?."inactiveZone$indiceZone"!= null)? state?."inactiveZone$indiceZone".toBoolean() :settings[key]
		if (inactiveZone) {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}, zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
			continue
		}
		if (setAllRoomTstatsSettings(indiceSchedule, indiceZone)) {
			found_room_tstat=true
		}
		def indoorTemps = getAllTempsForAverage(indiceZone)
		indoor_all_zones_temps = indoor_all_zones_temps + indoorTemps
	}
	if (found_room_tstat && settings.turnOffRoomTstatsFlag) {
		traceEvent(settings.logFilter,"schedule ${scheduleName},all room Tstats set in the zone(s), about to turn off all other room tstats",
				settings.detailedNotif)      
	}                
	runIn(10, "turn_off_room_tstats_outside_zones", [data: [indice:indiceSchedule]])   
	if (setRoomThermostatsOnly && ((!adjustmentFanFlag) || (!found_room_tstat))) {
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},all room Tstats set and setRoomThermostatsOnlyFlag= true,exiting",
			settings.detailedNotif)        
		return				    
	}    
	//	Now will do the right temp calculation based on all temp sensors to apply the desired temp settings at the main Tstat correctly

	float currentTemp = thermostat?.currentTemperature.toFloat().round(1)
	String mode = thermostat?.currentValue("thermostatMode")
	currentHeatingSetpoint=thermostat?.currentValue("heatingSetpoint")    
	currentCoolingSetpoint=thermostat?.currentValue("coolingSetpoint")    
	state?.zoned_min_indoor_temp=null
	state?.zoned_max_indoor_temp=null
	state?.zoned_avg_indoor_temp=null
	state?.zoned_med_indoor_temp=null
	state?.zoned_rooms_count=null        
	if (indoor_all_zones_temps != [] ) {
		def adjustmentType= (settings.adjustmentTempMethod)?: "avg"  
		min_indoor_temp=indoor_all_zones_temps.min().round(1)
		max_indoor_temp=indoor_all_zones_temps.max().round(1)
		float med_indoor_temp= ((min_indoor_temp + max_indoor_temp)/2).round(1)
		float average_indoor_temp=(indoor_all_zones_temps.sum()/indoor_all_zones_temps.size()).round(1)        
		state?.zoned_min_indoor_temp=min_indoor_temp
		state?.zoned_max_indoor_temp=max_indoor_temp
		state?.zoned_avg_indoor_temp=average_indoor_temp
		state?.zoned_med_indoor_temp=med_indoor_temp
		state?.zoned_rooms_count=indoor_all_zones_temps.size()        
		if (adjustmentType == "min") {
			avg_indoor_temp = min_indoor_temp
		} else if (adjustmentType == "max") {
			avg_indoor_temp = max_indoor_temp
		} else if (adjustmentType == "heat min/cool max") {
			if (mode.contains('heat')) {
				avg_indoor_temp = min_indoor_temp
			} else if (mode=='cool')  {
				avg_indoor_temp = max_indoor_temp
			} else  {         
				float median = (currentCoolingSetpoint + currentHeatingSetpoint).toFloat()
				median= (median)? (median/2).round(1): (scale=='C')?21:72
				if (currentTemp > median) {
					avg_indoor_temp = max_indoor_temp
				} else {
					avg_indoor_temp = min_indoor_temp
				}                        
			} 
		} else if (adjustmentType == "med") {
			avg_indoor_temp = med_indoor_temp
		} else {        
			avg_indoor_temp = average_indoor_temp
		}            
	} else {
		avg_indoor_temp = currentTemp
	}
	traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},method=${settings.adjustmentTempMethod},all temps collected from sensors=${indoor_all_zones_temps}",
		settings.detailedNotif)    

	float temp_diff = (avg_indoor_temp - currentTemp).round(1)
	traceEvent(settings.logFilter,"schedule ${scheduleName}:avg temp= ${avg_indoor_temp},main Tstat's currentTemp= ${currentTemp},temp adjustment=${temp_diff.abs()}",
		settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)
	key = "givenMaxTempDiff$indiceSchedule"
	def givenMaxTempDiff = settings[key]
	def input_max_temp_diff = (givenMaxTempDiff!=null) ?givenMaxTempDiff: (scale=='C')? 2: 5 // 2C/5F temp differential is applied by default

	float max_temp_diff = input_max_temp_diff.toFloat().round(1)

	key = "givenMaxFanDiff$indiceSchedule"
	def givenMaxFanDiff = settings[key]
    
	def input_max_fan_diff = (givenMaxFanDiff!=null) ?givenMaxFanDiff: (scale=='C')? 2: 5 // 2C/5F temp differential is applied by default for the fan diff
	float max_fan_diff = input_max_fan_diff.toFloat().round(1)
	key = "fanModeForThresholdOnlyFlag${indiceSchedule}"
	def fanModeForThresholdOnlyFlag = settings[key]
    
	if (adjustmentFanFlag && fanModeForThresholdOnlyFlag) {
    
		key = "fanDiffCalcExcludeMasterFlag$indiceSchedule"
		def zone_sensors_temp_diff_only= settings[key]
		float temp_diff_between_sensors =max_indoor_temp - min_indoor_temp
        
		// Adjust the fan mode if avg temp differential in zone is greater than max_fan_diff set in schedule
		if ( (max_fan_diff>0) && ((temp_diff.abs() >= max_fan_diff) || (zone_sensors_temp_diff_only && temp_diff_between_sensors.abs() >= max_fan_diff ))) {
			if (!zone_sensors_temp_diff_only) {        
				traceEvent(settings.logFilter,"schedule ${scheduleName},avg_temp_diff=${temp_diff.abs()} >= ${max_fan_diff} :adjusting fan mode as temp differential with master tstat is too big",
					settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)
			} else {
				traceEvent(settings.logFilter,"schedule ${scheduleName},avg_temp_diff=${temp_diff_between_sensors.abs()} >= ${max_fan_diff}: adjusting fan mode as temp differential between zoned sensors is too big",
					settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)
			}            
			// set fan mode with overrideThreshold=true
			if (!setRoomThermostatsOnly) set_fan_mode(indiceSchedule, true, 'on')          
			if (found_room_tstat) set_fan_speed_in_rooms(indiceSchedule, 'high')                
			if (evaporativeCoolerSwitch && settings.switchToTurnOnWhenFanDiffFlag) {
				traceEvent(settings.logFilter,"schedule ${scheduleName},avg_temp_diff=${temp_diff.abs()} >= ${max_fan_diff}, turning on the $evaporativeCoolerSwitch for air circulation",
					settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)
				evaporativeCoolerSwitch.on() // turn on the switches           	
			}            
		} else {
			if (!zone_sensors_temp_diff_only) {        
				traceEvent(settings.logFilter,"schedule ${scheduleName},avg_temp_diff=${temp_diff.abs()} < ${max_fan_diff}: set fan mode to off as temp differential with master tstat is small",
					settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)
			} else {
				traceEvent(settings.logFilter,"schedule ${scheduleName},avg_temp_diff=${temp_diff_between_sensors.abs()} < ${max_fan_diff}: set fan mode to off as temp differential between zoned sensors is small",
					settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)
			}            
			if (!setRoomThermostatsOnly) set_fan_mode(indiceSchedule, true, 'off')     // set fan mode to off as the temp diff is smaller than the differential allowed    
			if (evaporativeCoolerSwitch && settings.switchToTurnOnWhenFanDiffFlag) {
				traceEvent(settings.logFilter,"schedule ${scheduleName},avg_temp_diff=${temp_diff.abs()} < ${max_fan_diff}, turning off the $evaporativeCoolerSwitch",
					settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)
				evaporativeCoolerSwitch.off() // turn off the switches           	
			}            
			if (found_room_tstat) {
				float pct_fan_diff = 100 
				if (!zone_sensors_temp_diff_only) {                
					if (max_fan_diff) {
						pct_fan_diff = ((temp_diff.abs()/ max_fan_diff) * 100)
					} 
				} else {
					if (max_fan_diff) {
						pct_fan_diff = ((temp_diff_between_sensors.abs()/ max_fan_diff) * 100)
					} 
				}
				if (pct_fan_diff >= 50) {
					set_fan_speed_in_rooms(indiceSchedule, 'medium')                
				} else if (pct_fan_diff > 20) {
					set_fan_speed_in_rooms(indiceSchedule, 'low')                
				} else {
					set_fan_speed_in_rooms(indiceSchedule, 'off')                
				}                
			} /* if found_room_tstat */                               
		}  
	}
     
	if (setRoomThermostatsOnly) {
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},all room Tstats set and setRoomThermostatsOnlyFlag= true,exiting",
			settings.detailedNotif)        
		return				    
	}    
	float min_setpoint_adjustment = (scale=='C') ? MIN_SETPOINT_ADJUSTMENT_IN_CELSIUS:MIN_SETPOINT_ADJUSTMENT_IN_FARENHEITS
	if ((adjustmentTempFlag) && (scheduleName == state?.lastScheduleName) && (temp_diff.abs() < min_setpoint_adjustment)) {  // adjust the temp only if temp diff is significant
		traceEvent(settings.logFilter,"Temperature adjustment (${temp_diff}) between sensors is small, skipping it and exiting",settings.detailedNotif,
			GLOBAL_LOG_INFO)
		return
	}                
	key = "givenClimate$indiceSchedule"
	def climateName = settings[key]
	if ((climateName) && (thermostat.hasCommand("setThisTstatClimate"))) {
		try {
			thermostat?.setThisTstatClimate(climateName)
			setClimate=true            
			thermostat.refresh() // to get the latest setpoints               
		} catch (any) {
			traceEvent(settings.logFilter,"schedule ${scheduleName},not able to set climate ${climateName} at the thermostat(s) ${thermostat}", true, GLOBAL_LOG_ERROR,true)
		}                
	}        
	if (mode in ['heat','emergency heat', 'auto']) {
		if (setClimate) {
			desiredHeat = thermostat?.currentHeatingSetpoint
			currentHeatingSetpoint = desiredHeat            
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredHeat=${desiredHeat}",
				settings.detailedNotif)            
		} else {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for heatingSetpoint",settings.detailedNotif)
			key = "desiredHeatTemp$indiceSchedule"
			def heatTemp = settings[key]
			if (!heatTemp) {
				traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}:about to apply default heat settings",settings.detailedNotif)
				desiredHeat = (scale=='C') ? 21:72 					// by default, 21C/72F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredHeat=${desiredHeat}",settings.detailedNotif)
		} 
		temp_diff = (temp_diff < (0-max_temp_diff)) ? -(max_temp_diff):(temp_diff >max_temp_diff) ?max_temp_diff:temp_diff // determine the temp_diff based on max_temp_diff
		if (!adjustmentTempFlag) temp_diff=0		    
		float targetTstatTemp = (desiredHeat - temp_diff).round(1)
		if (targetTstatTemp != currentHeatingSetpoint) thermostat?.setHeatingSetpoint(targetTstatTemp)
		traceEvent(settings.logFilter,"schedule ${scheduleName},in zones=${zones},heating setPoint now =${targetTstatTemp},adjusted by avg temp diff (${temp_diff.abs()}) between all temp sensors in zone",
			settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>lastScheduleName run=${state?.lastScheduleName}, current heating baseline=${state?.scheduleHeatSetpoint}",settings.detailedNotif)
		if ((scheduleName != state?.lastScheduleName || (!state?.scheduleHeatSetpoint))) {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>saving a new heating baseline of $desiredHeat for schedule=$scheduleName, lastScheduleName=${state?.lastScheduleName}",settings.detailedNotif)
			state?.scheduleHeatSetpoint=desiredHeat  // save the desiredHeat in state variable for the current schedule
		}        
        
          
	}
        
	if (mode in ['cool','auto']) {

		if (setClimate) {
			desiredCool = thermostat?.currentCoolingSetpoint
			currentCoolingSetpoint=desiredCool            
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredCool=${desiredCool}",
				settings.detailedNotif)            
		} else {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for coolingSetpoint",settings.detailedNotif)
			key = "desiredCoolTemp$indiceSchedule"
			def coolTemp = settings[key]
			if (!coolTemp) {
				traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},about to apply default cool settings", settings.detailedNotif)
				desiredCool = (scale=='C') ? 23:75					// by default, 23C/75F is the target cool temp
			} else {
            
				desiredCool = coolTemp.toFloat()
			}
            
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredCool=${desiredCool}",settings.detailedNotif)
		} 
		temp_diff = (temp_diff < (0-max_temp_diff)) ? -(max_temp_diff):(temp_diff >max_temp_diff) ?max_temp_diff:temp_diff // determine the temp_diff based on max_temp_diff
		if (!adjustmentTempFlag) temp_diff=0		    
		float targetTstatTemp = (desiredCool - temp_diff).round(1)
		if (targetTstatTemp != currentCoolingSetpoint) thermostat?.setCoolingSetpoint(targetTstatTemp)
		traceEvent(settings.logFilter,"schedule ${scheduleName},in zones=${zones},cooling setPoint now =${targetTstatTemp},adjusted by avg temp diff (${temp_diff.abs()}) between all temp sensors in zone",
			settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>lastScheduleName run=${state?.lastScheduleName}, current cooling baseline=${state?.scheduleCoolSetpoint}",settings.detailedNotif)
		if ((scheduleName != state?.lastScheduleName || (!state?.scheduleCoolSetpoint))) {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>saving a new cooling baseline of $desiredCool for schedule $scheduleName, lastScheduleName=${state?.lastScheduleName}",settings.detailedNotif)
			state?.scheduleCoolSetpoint=desiredCool  // save the desiredCool in state variable for the current schedule
		}        
	}
}


private def adjust_vent_settings_in_zone(indiceSchedule) {
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	float avg_indoor_temp, avg_temp_diff, total_temp_diff=0, total_temp_in_vents=0,median
	def desiredTemp    
	def indiceRoom
	boolean closedAllVentsInZone=true
	int nbVents=0,nbRooms=0,total_level_vents=0
	def switchLevel  
	def ventSwitchesOnSet=[]
	def fullyCloseVents = (fullyCloseVentsFlag) ?: false
	def adjustmentBasedOnContact=(settings.setVentAdjustmentContactFlag)?:false


	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key= "openVentsFanOnlyFlag$indiceSchedule"
	def openVentsWhenFanOnly = (settings[key])?:false
	String operatingState = thermostat?.currentThermostatOperatingState           

	if (openVentsWhenFanOnly && (operatingState.toUpperCase().contains("FAN ONLY"))) { 
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
	def indoor_all_zones_temps=[]
  
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}: zones= ${zones}",settings.detailedNotif)
	key = "setRoomThermostatsOnlyFlag$indiceSchedule"
	def setRoomThermostatsOnlyFlag = settings[key]
	def setRoomThermostatsOnly = (setRoomThermostatsOnlyFlag) ?: false
	if (setRoomThermostatsOnly) {
		traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}:all room Tstats set and setRoomThermostatsOnlyFlag= true,exiting",
			settings.detailedNotif)        
		return ventSwitchesOnSet			    
	}    
	int openVentsCount=0,closedVentsCount=0    
	String mode = thermostat?.currentThermostatMode
	float currentTempAtTstat = thermostat?.currentTemperature?.toFloat().round(1)
	if (mode.contains('heat')) {
		desiredTemp = thermostat?.currentHeatingSetpoint?.toFloat().round(1) 
	} else if (mode=='cool') {    
		desiredTemp = thermostat?.currentCoolingSetpoint?.toFloat().round(1) 
	} else if (mode in ['auto', 'off', 'eco']) {    
		median = (thermostat?.currentCoolingSetpoint + thermostat?.currentHeatingSetpoint).toFloat()
		median= (median)? (median/2).round(1): (scale=='C')?21:72
		if (currentTempAtTstat > median) {
			desiredTemp =thermostat.currentCoolingSetpoint.toFloat().round(1)            
		} else {
			desiredTemp =thermostat?.currentHeatingSetpoint?.toFloat()?.round(1)                 
		}                        
		if (currentTempAtTstat > median) {
			desiredTemp =thermostat.currentCoolingSetpoint.toFloat().round(1) 
		} else {
			desiredTemp =thermostat.currentHeatingSetpoint.toFloat().round(1)                     
		}                        
	} else {
		desiredTemp = thermostat?.currentHeatingSetpoint
		desiredTemp = (desiredTemp)? desiredTemp.toFloat().round(1): (scale=='C')?21:72
	}    
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, desiredTemp=${desiredTemp}",settings.detailedNotif)
	indoor_all_zones_temps.add(currentTempAtTstat)

	key = "setVentLevel${indiceSchedule}"
	def defaultSetLevel = settings[key] 
	key = "resetLevelOverrideFlag${indiceSchedule}"	
	boolean resetLevelOverrideFlag = settings[key]
	state?.activeZones=zones
	int min_open_level=100, max_open_level=0    
	float min_temp_in_vents=200, max_temp_in_vents=0    

	for (zone in zones) {

		def zoneDetails=zone.split(':')
		traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>zone=${zone}: zoneDetails= ${zoneDetails}",settings.detailedNotif)
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		if (!zoneName || (zoneName=='null')) {
			continue
		}
		key = "inactiveZoneFlag$indiceZone"
		boolean inactiveZone=(state?."inactiveZone$indiceZone"!= null)? state?."inactiveZone$indiceZone".toBoolean() :settings[key]
		if (inactiveZone) {
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
			continue
		}
/* Commented out to avoid any "offline" issues on some sensors following some ST platform changes.		
		boolean refreshSensorsFlag= (adjustmentTempFlag)? false :true   // refresh Sensors only when required      
 		def indoorTemps = getAllTempsForAverage(indiceZone,refreshSensorsFlag) 
*/
 		def indoorTemps = getAllTempsForAverage(indiceZone)

		if (indoorTemps != [] ) {
			indoor_all_zones_temps = indoor_all_zones_temps + indoorTemps
			            
		} else {
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, no data from temp sensors, exiting",settings.detailedNotif)
		}        
		traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, all temps collected from sensors=${indoorTemps}",settings.detailedNotif)
	} /* end for zones */

	state?.zoned_min_indoor_temp=null
	state?.zoned_max_indoor_temp=null
	state?.zoned_avg_indoor_temp=null
	state?.zoned_med_indoor_temp=null
	state?.zoned_rooms_count=null    
	if (indoor_all_zones_temps != [] ) {
		float min_indoor_temp=indoor_all_zones_temps.min().round(1)
		float max_indoor_temp=indoor_all_zones_temps.max().round(1)
		float med_indoor_temp= ((min_indoor_temp + max_indoor_temp)/2).round(1)
		float average_indoor_temp=(indoor_all_zones_temps.sum()/indoor_all_zones_temps.size()).round(1)        
		state?.zoned_min_indoor_temp=min_indoor_temp
		state?.zoned_max_indoor_temp=max_indoor_temp
		state?.zoned_avg_indoor_temp=average_indoor_temp
		state?.zoned_med_indoor_temp=med_indoor_temp
		state?.zoned_rooms_count= indoor_all_zones_temps.size()   
	}	        
	avg_indoor_temp = (indoor_all_zones_temps.sum() / indoor_all_zones_temps.size()).round(1)
	avg_temp_diff = (avg_indoor_temp - desiredTemp).round(1)
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in all zones, desiredTemp=$desiredTemp, all temps collected from sensors=${indoor_all_zones_temps}, avg_indoor_temp=${avg_indoor_temp}, avg_temp_diff=${avg_temp_diff}",
		settings.detailedNotif)    
	for (zone in zones) {
		float targetTemp=desiredTemp    
		def zoneDetails=zone.split(':')
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		if (!zoneName || (zoneName=='null')) {
			continue
		}
		key = "inactiveZoneFlag$indiceZone"
		boolean inactiveZone=(state?."inactiveZone$indiceZone"!= null)? state?."inactiveZone$indiceZone".toBoolean() :settings[key]
		if (inactiveZone) {
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, inactive:$inactiveZone", settings.detailedNotif)
			continue
		}
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		key  = "desiredHeatDeltaTemp$indiceZone"
		def desiredHeatDelta =  (state?."desiredHeatTempDelta$indiceZone")? state?."desiredHeatTempDelta$indiceZone".toFloat(): settings[key]
		key  = "desiredCoolDeltaTemp$indiceZone"
		def desiredCoolDelta =  (state?."desiredCoolTempDelta$indiceZone")? state?."desiredCoolTempDelta$indiceZone".toFloat(): settings[key]

		if (mode.contains('heat')) {
			targetTemp = targetTemp + ((desiredHeatDelta)?desiredHeatDelta.toFloat():0)  
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredHeatDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
		} else if (mode=='cool') {    
			targetTemp = targetTemp + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)  
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredCoolDelta=${desiredCoolDelta}",			
				settings.detailedNotif)     
		} else if (mode in ['auto', 'off', 'eco']) {    
			if (currentTempAtTstat > median) {
				targetTemp = targetTemp + ((desiredCoolDelta)?desiredCoolDelta.toFloat():0)  
			} else {
				targetTemp = targetTemp + ((desiredHeatDelta)?desiredHeatDelta.toFloat():0)  
			}                        
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredHeatDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredCoolDelta=${desiredCoolDelta}",			
				settings.detailedNotif)     
		} else {
			targetTemp = targetTemp + ((desiredHeatDelta)?desiredHeatDelta.toFloat():0)  
		}    
		for (room in rooms) {
			nbRooms++ 		       	
			switchLevel =null	// initially set to null for check later
			def roomDetails=room.split(':')
			indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]           
			if (!roomName || roomName=='null')  {
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
						switchLevel = (fullyCloseVents)? 0 :MIN_OPEN_LEVEL_IN_ZONE // setLevel at a minimum as the room is not occupied.
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, room ${roomName} is not occupied,vents set to mininum level=${switchLevel}",
							settings.detailedNotif,GLOBAL_LOG_INFO,settings.detailedNotif)                        
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
					if ((!closedContactLogicFlag) && isContactOpen ) {
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
				def tempAtSensor =getSensorTempForAverage(indiceRoom)			
				if (tempAtSensor == null) {
					tempAtSensor= currentTempAtTstat				            
				}
				float temp_diff_at_sensor = (tempAtSensor - targetTemp).toFloat().round(1)
				total_temp_diff = total_temp_diff + temp_diff_at_sensor 
				traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>thermostat mode = ${mode}, schedule ${scheduleName}, in zone ${zoneName}, room ${roomName}, temp_diff_at_sensor=${temp_diff_at_sensor}, avg_temp_diff=${avg_temp_diff}",
					settings.detailedNotif)                
				if ((mode=='cool') || ((mode in ['auto', 'off', 'eco']) && (currentTempAtTstat> median))) {
					avg_temp_diff = (avg_temp_diff !=0) ? avg_temp_diff : (0.1)  // to avoid divided by zero exception
					switchLevel = ((temp_diff_at_sensor / avg_temp_diff) * 100).round()
					switchLevel =( switchLevel >=0)?((switchLevel<100)? switchLevel: 100):0
					switchLevel=(temp_diff_at_sensor <=0)? ((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE): ((temp_diff_at_sensor >0) && (avg_temp_diff<0))?100:switchLevel
				} else {
					avg_temp_diff = (avg_temp_diff !=0) ? avg_temp_diff : (-0.1)  // to avoid divided by zero exception
					switchLevel = ((temp_diff_at_sensor / avg_temp_diff) * 100).round()
					switchLevel =( switchLevel >=0)?((switchLevel<100)? switchLevel: 100):0
					switchLevel=(temp_diff_at_sensor >=0)? ((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE): ((temp_diff_at_sensor <0) && (avg_temp_diff>0))?100:switchLevel
				} 
			} 
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, room ${roomName},switchLevel to be set=${switchLevel}",
				settings.detailedNotif)            
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					def temp_in_vent=getTemperatureInVent(ventSwitch)    
					// compile some stats for the dashboard                    
					if (temp_in_vent) {                                   
						min_temp_in_vents=(temp_in_vent < min_temp_in_vents)? temp_in_vent.toFloat().round(1) : min_temp_in_vents
						max_temp_in_vents=(temp_in_vent > max_temp_in_vents)? temp_in_vent.toFloat().round(1) : max_temp_in_vents
						total_temp_in_vents=total_temp_in_vents + temp_in_vent
					}                                        
					def switchOverrideLevel=null                 
					nbVents++
					if (!resetLevelOverrideFlag) {
						key = "ventLevel${j}$indiceRoom"
						switchOverrideLevel = settings[key]
					}                        
					if (switchOverrideLevel !=null) {                
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} to switchOverrideLevel =${switchOverrideLevel}%",
							settings.detailedNotif)                        
						switchLevel = ((switchOverrideLevel >= 0) && (switchOverrideLevel<= 100))? switchOverrideLevel:switchLevel                     
					} else if (defaultSetLevel!=null)  {
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} to defaultSetLevel =${defaultSetLevel}%",
							settings.detailedNotif)                        
						switchLevel = ((defaultSetLevel >= 0) && (defaultSetLevel<= 100))? defaultSetLevel:switchLevel                     
					}
					setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)                    
					traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} to switchLevel =${switchLevel}%",
						settings.detailedNotif)                    
					// compile some stats for the dashboard                    
					min_open_level=(switchLevel < min_open_level)? switchLevel.toInteger() : min_open_level
					max_open_level=(switchLevel > max_open_level)? switchLevel.toInteger() : max_open_level
					total_level_vents=total_level_vents + switchLevel.toInteger()
					if (switchLevel > MIN_OPEN_LEVEL_IN_ZONE) {      // make sure that the vents are set to a minimum level in zone, otherwise they are considered to be closed              
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
		    	
		switchLevel=MIN_OPEN_LEVEL_IN_ZONE        
		ventSwitchesOnSet=control_vent_switches_in_zones(indiceSchedule, switchLevel)		    
		traceEvent(settings.logFilter,"schedule ${scheduleName}, safeguards on: set all ventSwitches to ${switchLevel}% to avoid closing all of them",
			settings.detailedNotif, GLOBAL_LOG_INFO,settings.detailedNotif)       
	}    
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName},ventSwitchesOnSet=${ventSwitchesOnSet}",settings.detailedNotif)
	
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
							settings.detailedNotif)                        
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
			traceEvent(settings.logFilter,"turn_off_all_other_vents>closing ${closedVentsSet} using the safeguards as requested to create the desired zone(s)",
				settings.detailedNotif, GLOBAL_LOG_INFO)
			closedVentsSet.each {
				setVentSwitchLevel(null, it, MIN_OPEN_LEVEL_SMALL)
			}                
		}            
		if (fullyCloseVents) {
			traceEvent(settings.logFilter,"turn_off_all_other_vents>closing ${closedVentsSet} totally as requested to create the desired zone(s)",settings.detailedNotif,
				GLOBAL_LOG_INFO)            
			closedVentsSet.each {
				setVentSwitchLevel(null, it, 0)
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
		traceEvent(settings.logFilter,"getCurrentVentLevel>error, not able to get current vent level from ${ventSwitch}",settings.detailedNotif,
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
			true, GLOBAL_LOG_INFO,settings.detailedNotif)           
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
	if ((currentLevel < (switchLevel - MAX_LEVEL_DELTA)) ||  (currentLevel > (switchLevel + MAX_LEVEL_DELTA))) {
		traceEvent(settings.logFilter, "setVentSwitchLevel>error not able to set ${ventSwitch} to ${switchLevel}",
			true, GLOBAL_LOG_WARN,settings.detailedNotif)           
		return false           
	}    
    
	return true    
}

private def control_vent_switches_in_zones(indiceSchedule, switchLevel=100) {

	def key = "includedZones$indiceSchedule"
	def zones = settings[key]
	def ventSwitchesOnSet=[]
    
	for (zone in zones) {

		def zoneDetails=zone.split(':')
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		if (!zoneName || (zoneName=='null')) {
			continue
		}
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
    
		for (room in rooms) {
			def roomDetails=room.split(':')
			def indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]
			if (!roomName || roomName=='null')  {
				continue
			}
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
private def cToF(temp) {
	return (temp * 1.8 + 32)
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
	return "Schedule Thermostat Zones V2"
}