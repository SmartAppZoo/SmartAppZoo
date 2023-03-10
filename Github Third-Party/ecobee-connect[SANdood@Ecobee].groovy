/**
 *  Based on original code Copyright 2015 SmartThings
 *	Additional changes Copyright 2016 Sean Kendall Schneyer
 *  Additional changes Copyright 2017 Barry A. Burke
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
 *	Ecobee Service Manager
 *
 *	Author: scott
 *	Date: 2013-08-07
 *
 *  Last Modification:
 *      JLH - 01-23-2014 - Update for Correct SmartApp URL Format
 *      JLH - 02-15-2014 - Fuller use of ecobee API
 *      10-28-2015 DVCSMP-604 - accessory sensor, DVCSMP-1174, DVCSMP-1111 - not respond to routines
 *      StrykerSKS - 12-11-2015 - Make it work (better) with the Ecobee 3
 *		BAB - 01-20-2017 - Massive overhaul for efficiency, performance, bug fixes and UI enhancements
 *
 *	Updates by Barry A. Burke (storageanarchy@gmail.com)
 * 	Date: 2017-01-28
 *	https://github.com/SANdood/Ecobee
 *
 *  See Changelog for change history
 *
 * 	0.9.18 - Fix customer Program handling
 *	0.9.19 - Add attributes to indicate custom program names to child thermostats (smart1, smart2, etc)
 *  0.9.20 - Allow installations where no "location" is set. Useful for virtual hubs and testing
 *	0.10.1 - Massive overhaul (see https://github.com/SANdood/Ecobee)
 *	0.10.2 - Added Smart Zone helper app
 *	0.10.3 - Added Smart Circulation helper app
 *	0.10.4 - Beta Release of Barry's updated version
 *	0.10.5 - Bug Fixes by Barry
 *				- display Vacation's event.fanOnTime if in Vacation hold
 *				- rewrite Vacation with new fanMinOnTime via setVacationFanMinOnTim
 *	0.10.6 - Fix initial forcePoll in pollChildren()
 *  0.10.7 - Interim fix
 *	0.10.8 - Resolve unitialized variables
 *	0.10.9 - More error detection + Android authentication fixes
 *	0.10.10- Closed the unititalized pollingInterval problem (again)
 *	0.10.11- Found and fixed issue (in updateThermostatData); optimized ue of atomics
 *	0.10.12- Refined LOG levels
 *	0.10.13- Added sending programsList (list of available ecobee climates) to thermostat(s)
 *	0.10.14- Reduced frequency of sending of never/rarely changing objects to the Thermostat(s)
 *	0.10.15- Converted heat/cool ranges to C when appropriate
 *	0.10.16- Proactively refresh the AuthToken if the expiration time is less than the watchdog schedule
 *	0.10.17- Optimized sunrise/sunset handling, watchdogInterval and updateThermostat map creation 
 *	0.10.18- Still more optimizations
 *	0.10.19- Revamped watchdog's refreshAuthToken strategy
 *	0.10.21- Yet another attempt to fixed initialization errors on first install
 *	0.10.22- Gotcha, dagnabbit! settings.vars weren't being initialized if Preferences page never opened
 *	0.10.23- Fixed humiditySetpoint (verify valid extendedRuntime data)
 *	0.10.24- Don't adjust Heating/Cooling setpoints during 'fan only'
 *	0.10.25- Optimized updates for setpoints and programs
 * 	0.10.26- Removed pre-emptive Auth Refresh, per Ecobee API use guidelines 
 *			 Hides the timeout error if auth refresh is successful
 *			 Ensure auth refresh only occurs once, avoids multiple recovery threads
 *			 Retry sendJson if fails on Auth expiry
 *	0.10.27- Update configured/selected sensors only, and then only when there are changes
 *	0.10.28- Major overhaul of sending updates to devices for speed & efficiency.
 *			 Simplification of logging messages - optimal Debug Level is now 2
 *			 Added capability to add/remove a sensor from a climate/program
 *	1.0.0 -	 Final preparation for general release
 *	1.0.1 -	 Added "Offline" status when ecobee Cloud loses connection with thermostat (power out, network down, etc.)
 *	1.0.2 -	 Chasing another uninitialized variable issue
 *	1.0.3 -  Updates to thermostat->Ecobee Cloud connection handling
 *
 *
 */  
import groovy.json.JsonOutput

def getVersionNum() { return "1.0.3" }
private def getVersionLabel() { return "Ecobee (Connect) Version ${getVersionNum()}" }
private def getHelperSmartApps() {
	return [ 
		[name: "ecobeeContactsChild", appName: "ecobee Open Contacts",  
            namespace: "smartthings", multiple: true, 
            title: "Create new Open Contacts Handler..."],
    	[name: "ecobeeRoutinesChild", appName: "ecobee Routines",  
            namespace: "smartthings", multiple: true, 
            title: "Create new Routines/Programs Handler..."], 
        [name: "ecobeeCirculationChild", appName: "ecobee Smart Circulation",
			 namespace: "smartthings", multiple: true,
			 title: "Create new Smart Circulation Handler..."],
        [name: "ecobeeRoomChild", appName: "ecobee Smart Room",
        	namespace: "smartthings", multiple: true,
            title: "Create new Smart Room Handler..."],
		[name: "ecobeeZonesChild", appName: "ecobee Smart Zones",
			 namespace: "smartthings", multiple: true,
			 title: "Create new Smart Zone Handler..."]
	]
}
 
definition(
	name: "Ecobee (Connect)",
	namespace: "smartthings",
	author: "Sean Kendall Schneyer",
	description: "Connect your Ecobee thermostat to SmartThings.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: true
) {
	appSetting "clientId"
}

preferences {
	page(name: "mainPage")
    page(name: "removePage")
	page(name: "authPage")
	page(name: "thermsPage")
	page(name: "sensorsPage")
    page(name: "preferencesPage")    
    page(name: "addWatchdogDevicesPage")
    page(name: "helperSmartAppsPage")    
    // Part of debug Dashboard
    page(name: "debugDashboardPage")
    page(name: "pollChildrenPage")
    page(name: "updatedPage")
    page(name: "refreshAuthTokenPage")    
}

mappings {
	path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
	path("/oauth/callback") {action: [GET: "callback"]}
}


// Begin Preference Pages
def mainPage() {	
	def deviceHandlersInstalled 
    def readyToInstall 
    
    // Only create the dummy devices if we aren't initialized yet
    if (!atomicState.initialized) {
    	deviceHandlersInstalled = testForDeviceHandlers()
    	readyToInstall = deviceHandlersInstalled
	}
    if (atomicState.initialized) { readyToInstall = true }
    
	dynamicPage(name: "mainPage", title: "Welcome to ecobee (Connect)", install: readyToInstall, uninstall: false, submitOnChange: true) {
    	def ecoAuthDesc = (atomicState.authToken != null) ? "[Connected]\n" :"[Not Connected]\n"        
		
        // If not device Handlers we cannot proceed
        if(!atomicState.initialized && !deviceHandlersInstalled) {
			section() {
				paragraph "ERROR!\n\nYou MUST add the ${getChildThermostatName()} and ${getChildSensorName()} Device Handlers to the IDE BEFORE running the setup."				
			}		
        } else {
        	readyToInstall = true
        }
        
        if(atomicState.initialized && !atomicState.authToken) {
        	section() {
				paragraph "WARNING!\n\nYou are no longer connected to the ecobee API. Please re-Authorize below."				
			}
        }       

		if(atomicState.authToken != null && atomicState.initialized != true) {
        	section() {
            	paragraph "Please click 'Done' to save your credentials. Then re-open the SmartApp to continue the setup."
            }
        }

		// Need to save the initial login to setup the device without timeouts
		if(atomicState.authToken != null && atomicState.initialized) {
        	if (settings.thermostats?.size() > 0 && atomicState.initialized) {
            	section("Helper SmartApps") {
                	href ("helperSmartAppsPage", title: "Helper SmartApps", description: "Tap to manage Helper SmartApps")
                }            
            }
			section("Devices") {
				def howManyThermsSel = settings.thermostats?.size() ?: 0
                def howManyTherms = atomicState.numAvailTherms ?: "?"
                def howManySensors = atomicState.numAvailSensors ?: "?"
                
                // Thermostats
				atomicState.settingsCurrentTherms = settings.thermostats ?: []
    	    	href ("thermsPage", title: "Thermostats", description: "Tap to select Thermostats [${howManyThermsSel}/${howManyTherms}]")                
                
                // Sensors
            	if (settings.thermostats?.size() > 0) {
					atomicState.settingsCurrentSensors = settings.ecobeesensors ?: []
                	def howManySensorsSel = settings.ecobeesensors?.size() ?: 0
                    if (howManySensorsSel > howManySensors) { howManySensorsSel = howManySensors } // This is due to the fact that you can remove alread selected hiden items
            		href ("sensorsPage", title: "Sensors", description: "Tap to select Sensors [${howManySensorsSel}/${howManySensors}]")
	            }
    	    }        
	        section("Preferences") {
    	    	href ("preferencesPage", title: "Preferences", description: "Tap to review SmartApp settings.")
                LOG("In Preferences page section after preferences line", 5, null, "trace")
        	}
            if( settings.useWatchdogDevices == true ) {
            	section("Extra Poll and Watchdog Devices") {
                	href ("addWatchdogDevicesPage", title: "Watchdog Devices", description: "Tap to select Poll and Watchdog Devices.")
                }
            }
           
    	} // End if(atomicState.authToken)
        
        // Setup our API Tokens       
		section("Ecobee Authentication") {
			href ("authPage", title: "ecobee Authorization", description: "${ecoAuthDesc}Tap for ecobee Credentials")
		}      
		if ( debugLevel(5) ) {
			section ("Debug Dashboard") {
				href ("debugDashboardPage", description: "Tap to enter the Debug Dashboard", title: "Debug Dashboard")
			}
		}
		section("Remove ecobee (Connect)") {
			href ("removePage", description: "Tap to remove ecobee (Connect) ", title: "Remove ecobee (Connect)")
		}            
		
		section ("Name this instance of ecobee (Connect)") {
			label name: "name", title: "Assign a name", required: false, defaultValue: app.name, description: app.name
		}
     
		section (getVersionLabel())
	}
}


def removePage() {
	dynamicPage(name: "removePage", title: "Remove ecobee (Connect) and All Devices", install: false, uninstall: true) {
    	section ("WARNING!\n\nRemoving ecobee (Connect) also removes all Devices\n") {
        }
    }
}

// Setup OAuth between SmartThings and Ecobee clouds
def authPage() {
	LOG("=====> authPage() Entered", 5)

	if(!atomicState.accessToken) { //this is an access token for the 3rd party to make a call to the connect app
		atomicState.accessToken = createAccessToken()
	}

	def description = "Click to enter ecobee Credentials"
	def uninstallAllowed = false
	def oauthTokenProvided = false

	if(atomicState.authToken) {
		description = "You are connected. Tap Done/Next above."
		uninstallAllowed = true
		oauthTokenProvided = true
        apiRestored()
	} else {
		description = "Tap to enter ecobee Credentials"
	}

	def redirectUrl = buildRedirectUrl //"${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}"
    LOG("authPage() --> RedirectUrl = ${redirectUrl}")
    
	// get rid of next button until the user is actually auth'd
	if (!oauthTokenProvided) {
    	LOG("authPage() --> in !oauthTokenProvided")    	
		return dynamicPage(name: "authPage", title: "ecobee Setup", nextPage: "", uninstall: uninstallAllowed) {
			section() {
				paragraph "Tap below to log in to the ecobee service and authorize SmartThings access. Be sure to press the 'Allow' button on the 2nd page."
				href url:redirectUrl, style:"embedded", required:true, title: "ecobee Account Authorization", description:description 
			}
		}
	} else {    	
        LOG("authPage() --> in else for oauthTokenProvided - ${atomicState.authToken}.")
        return dynamicPage(name: "authPage", title: "ecobee Setup", nextPage: "mainPage", uninstall: uninstallAllowed) {
        	section() {
            	paragraph "Return to main menu."
                href url:redirectUrl, style: "embedded", state: "complete", title: "ecobee Account Authorization", description: description
			}
        }           
	}
}

// Select which Thermostats are to be used
def thermsPage(params) {
	LOG("=====> thermsPage() entered", 5)        
	def stats = getEcobeeThermostats()
    LOG("thermsPage() -> thermostat list: ${stats}")
    LOG("thermsPage() starting settings: ${settings}")
    LOG("thermsPage() params passed? ${params}", 4, null, "trace")

    dynamicPage(name: "thermsPage", title: "Select Thermostats", params: params, nextPage: "", content: "thermsPage", uninstall: false) {    
    	section("Units") {
        	paragraph "NOTE: The units type (F or C) is determined by your Hub Location settings automatically. Please update your Hub settings (under My Locations) to change the units used. Current value is ${getTemperatureScale()}."
        }
    	section("Select Thermostats") {
			LOG("thermsPage(): atomicState.settingsCurrentTherms=${atomicState.settingsCurrentTherms}   settings.thermostats=${settings.thermostats}", 4, null, "trace")
			if (atomicState.settingsCurrentTherms != settings.thermostats) {
				LOG("atomicState.settingsCurrentTherms != settings.thermostats determined!!!", 4, null, "trace")			
			} else { LOG("atomicState.settingsCurrentTherms == settings.thermostats: No changes detected!", 4, null, "trace") }
        	paragraph "Tap below to see the list of ecobee thermostats available in your ecobee account and select the ones you want to connect to SmartThings."
			input(name: "thermostats", title:"Select Thermostats", type: "enum", required:false, multiple:true, description: "Tap to choose", params: params, metadata:[values:stats], submitOnChange: true)        
        }      
    }      
}

def sensorsPage() {
	// Only show sensors that are part of the chosen thermostat(s)
    // Refactor to show the sensors under their corresponding Thermostats. Use Thermostat name as section header?
    LOG("=====> sensorsPage() entered. settings: ${settings}", 5)
    atomicState.sensorsPageVisited = true

	def options = getEcobeeSensors() ?: []
	def numFound = options.size() ?: 0
    
    LOG("options = getEcobeeSensors == ${options}")

    dynamicPage(name: "sensorsPage", title: "Select Sensors", nextPage: "") {
		if (numFound > 0)  {
			section("Select Sensors"){
				LOG("sensorsPage(): atomicState.settingsCurrentSensors=${atomicState.settingsCurrentSensors}   settings.ecobeesensors=${settings.ecobeesensors}", 4, null, "trace")
				if (atomicState.settingsCurrentSensors != settings.ecobeesensors) {
					LOG("atomicState.settingsCurrentSensors != settings.ecobeesensors determined!!!", 4, null, "trace")					
				} else { LOG("atomicState.settingsCurrentSensors == settings.ecobeesensors: No changes detected!", 4, null, "trace") }
				paragraph "Tap below to see the list of ecobee sensors available for the selected thermostat(s) and select the ones you want to connect to SmartThings."
                if (settings.showThermsAsSensor) { paragraph "NOTE: Also showing Thermostats as an available sensor to allow for actual temperature values to be used." }
				input(name: "ecobeesensors", title:"Select Ecobee Sensors (${numFound} found)", type: "enum", required:false, description: "Tap to choose", multiple:true, metadata:[values:options])
			}
		} else {
    		 // No sensors associated with this set of Thermostats was found
           LOG("sensorsPage(): No sensors found.", 4)
           section(""){
           		paragraph "No associated sensors were found. Click Done above."
           }
	    }        
	}
}

def preferencesPage() {
    LOG("=====> preferencesPage() entered. settings: ${settings}", 5)

    dynamicPage(name: "preferencesPage", title: "Update SmartApp Preferences", nextPage: "") {
		section("SmartApp Preferences") {
        	input(name: 'recipients', title: 'Send push notifications to', type: 'contact', required: false, multiple: true)
        	input(name: "holdType", title:"Select Hold Type", type: "enum", required:false, multiple:false, defaultValue:  "Until I Change", description: "Until I Change", metadata:[values:["Until I Change", "Until Next Program"]])
            paragraph "The 'Smart Auto Temperature Adjust' feature determines if you want to allow the thermostat setpoint to be changed using the arrow buttons in the Tile when the thermostat is in 'auto' mode."
            input(name: "smartAuto", title:"Use Smart Auto Temperature Adjust?", type: "bool", required:false, defaultValue: false, description: "")
            input(name: "pollingInterval", title:"Polling Interval (in Minutes)", type: "enum", required:false, multiple:false, defaultValue:5, description: "5", options:["1", "2", "3", "5", "10", "15", "30"])
            input(name: "debugLevel", title:"Debugging Level (higher # for more information)", type: "enum", required:false, multiple:false, defaultValue:3, description: "3", options:["5", "4", "3", "2", "1", "0"])            
            paragraph "Showing a Thermostat as a separate Sensor is useful if you need to access the actual temperature in the room where the Thermostat is located and not just the (average) temperature displayed on the Thermostat"
            input(name: "showThermsAsSensor", title:"Include Thermostats as a separate Ecobee Sensor?", type: "bool", required:false, defaultValue: false, description: "")
            paragraph "Monitoring external devices can be used to drive polling and the watchdog events. Be warned, however, not to select too many devices or devices that will send too many events as this can cause issues with the connection."
            input(name: "useWatchdogDevices", title:"Monitor external devices to drive additional polling and watchdog events?", type: "bool", required:false, description: "", defaultValue:false)
            paragraph "Set the pause between pressing the setpoint arrows and initiating the API calls. The pause needs to be long enough to allow you to click the arrow again for changing by more than one degree."
            input(name: "arrowPause", title:"Delay timer value after pressing setpoint arrows", type: "enum", required:false, multiple:false, description: "4", defaultValue:5, options:["1", "2", "3", "4", "5"])
			paragraph "Set the desired number of decimal places to display for all temperatures (recommended 1 for C, 0 for F)."
			String digits = wantMetric() ? "1" : "0"
			input(name: "tempDecimals", title:"Decimal places to display", type: "enum", required:true, multiple:false, defaultValue:digits, description: digits, options:["0", "1", "2"], submitOnChange: true)
        }
	}
}

def addWatchdogDevicesPage() {
	LOG("Displaying the Watchdog Device Selection page next...", 5, null, "trace")
    dynamicPage(name: "addWatchdogDevicesPage", title: "Select Watchdog Devices", nextPage: "") {
		section("Polling and Watchdog Devices") {
        	paragraph ("Select device(s) that you wish to subscribe to in order to create additional polling events and trigger the watchdog timers. " +
            	"Do NOT select too many devices or devices that will cause excess polling. " + 
                "Ecobee only updates their data every 3 minutes so any polling interval greater than that is unnecessary.")
     		input(name: "watchdogMotion", type:"capability.motionSensor", title: "Select Motion Sensor(s)", required:false, multiple:true)
            input(name: "watchdogTemp", type:"capability.temperatureMeasurement", title: "Select Temperature Measurement Device(s)", required:false, multiple:true)
            input(name: "watchdogSwitch", type:"capability.switch", title: "Select Switch(es)", required:false, multiple:true)
            input(name: "watchdogBattery", type:"capability.battery", title: "Select Battery(ies)", required:false, multiple:true)
            input(name: "watchdogHumidity", type:"capability.relativeHumidityMeasurement", title: "Select Humidity Sensor(s)", required:false, multiple:true)
            input(name: "watchdogLuminance", type:"capability.illuminanceMeasurement", title: "Select Illuminance Sensor(s)", required:false, multiple:true)
        }
 	}
}

def debugDashboardPage() {
	LOG("=====> debugDashboardPage() entered.", 5)    
    
    dynamicPage(name: "debugDashboardPage", title: "") {
    	section (getVersionLabel())
		section("Commands") {
        	href(name: "pollChildrenPage", title: "", required: false, page: "pollChildrenPage", description: "Tap to execute: pollChildren()")
            href(name: "refreshAuthTokenPage", title: "", required: false, page: "refreshAuthTokenPage", description: "Tap to execute: refreshAuthToken()")
            href(name: "updatedPage", title: "", required: false, page: "updatedPage", description: "Tap to execute: updated()")
        }    	
        
    	section("Settings Information") {
        	paragraph "debugLevel: ${getDebugLevel()}"
            paragraph "holdType: ${getHoldType()}"
            paragraph "pollingInterval: ${getPollingInterval()}"
            paragraph "showThermsAsSensor: ${settings.showThermsAsSensor} (default=false if null)"
            paragraph "smartAuto: ${settings.smartAuto} (default=false if null)"   
            paragraph "Selected Thermostats: ${settings.thermostats}"
            paragraph "Decimal Precision: ${getTempDecimals()}"
        }
        section("Dump of Debug Variables") {
        	def debugParamList = getDebugDump()
            LOG("debugParamList: ${debugParamList}", 4, null, "debug")
            //if ( debugParamList?.size() > 0 ) {
			if ( debugParamList != null ) {
            	debugParamList.each { key, value ->  
                	LOG("Adding paragraph: key:${key}  value:${value}", 5, null, "trace")
                	paragraph "${key}: ${value}"
                }
            }
        }
    	section("Commands") {
        	href(name: "pollChildrenPage", title: "", required: false, page: "pollChildrenPage", description: "Tap to execute command: pollChildren()")
            href ("removePage", description: "Tap to remove ecobee (Connect) ", title: "")
        }
    }    
}

// pages that are part of Debug Dashboard
def pollChildrenPage() {
	LOG("=====> pollChildrenPage() entered.", 5)
    atomicState.forcePoll = true // Reset to force the poll to happen
	pollChildren(null)
    
	dynamicPage(name: "pollChildrenPage", title: "") {
    	section() {
        	paragraph "pollChildren() was called"
        }
    }    
}

// pages that are part of Debug Dashboard
def updatedPage() {
	LOG("=====> updatedPage() entered.", 5)
    updated()
    
	dynamicPage(name: "updatedPage", title: "") {
    	section() {
        	paragraph "updated() was called"
        }
    }    
}

def refreshAuthTokenPage() {
	LOG("=====> refreshAuthTokenPage() entered.", 5)
    refreshAuthToken()
    
	dynamicPage(name: "refreshAuthTokenPage", title: "") {
    	section() {
        	paragraph "refreshAuthTokenPage() was called"
        }
    }    
}

def helperSmartAppsPage() {
	LOG("helperSmartAppsPage() entered", 5)

	LOG("SmartApps available are ${getHelperSmartApps()}", 5, null, "info")
	
    //getHelperSmartApps() {
 	dynamicPage(name: "helperSmartAppsPage", title: "Helper Smart Apps", install: true, uninstall: false, submitOnChange: true) { 
    	section("Available Helper Smart Apps") {
		getHelperSmartApps().each { oneApp ->
			LOG("Processing the app: ${oneApp}", 4, null, "trace")            
            def allowMultiple = oneApp.multiple.value
			//section ("${oneApp.appName.value}") {
            	app(name:"${oneApp.name.value}", appName:"${oneApp.appName.value}", namespace:"${oneApp.namespace.value}", title:"${oneApp.title.value}", multiple: allowMultiple)            
			//}
		}
	}
}
}
// End Preference Pages

// Preference Pages Helpers
private def Boolean testForDeviceHandlers() {
	if (atomicState.runTestOnce != null) { return atomicState.runTestOnce }
    
    def DNIAdder = now().toString()
    def d1
    def d2
    def success = true
    
	try {    	
		d1 = addChildDevice(app.namespace, getChildThermostatName(), "dummyThermDNI-${DNIAdder}", null, ["label":"Ecobee Thermostat:TestingForInstall", completedSetup:true])
		d2 = addChildDevice(app.namespace, getChildSensorName(), "dummySensorDNI-${DNIAdder}", null, ["label":"Ecobee Sensor:TestingForInstall", completedSetup:true])
	} catch (physicalgraph.app.exception.UnknownDeviceTypeException e) {
		LOG("You MUST add the ${getChildThermostatName()} and ${getChildSensorName()} Device Handlers to the IDE BEFORE running the setup.", 1, null, "error")
		success = false
	}
    
    atomicState.runTestOnce = success
    
    if (d1) deleteChildDevice("dummyThermDNI-${DNIAdder}") 
    if (d2) deleteChildDevice("dummySensorDNI-${DNIAdder}") 
    
    return success
}
// End Preference Pages Helpers

// OAuth Init URL
def oauthInitUrl() {
	LOG("oauthInitUrl with callback: ${callbackUrl}", 5)
	atomicState.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [
			response_type: "code",
			client_id: smartThingsClientId,			
			scope: "smartRead,smartWrite",
			redirect_uri: callbackUrl, //"https://graph.api.smartthings.com/oauth/callback"
			state: atomicState.oauthInitState			
	]

	LOG("oauthInitUrl - Before redirect: location: ${apiEndpoint}/authorize?${toQueryString(oauthParams)}", 4)
	redirect(location: "${apiEndpoint}/authorize?${toQueryString(oauthParams)}")
}

// OAuth Callback URL and helpers
def callback() {
	LOG("callback()>> params: $params, params.code ${params.code}, params.state ${params.state}, atomicState.oauthInitState ${atomicState.oauthInitState}", 4)
	def code = params.code
	def oauthState = params.state

	//verify oauthState == atomicState.oauthInitState, so the callback corresponds to the authentication request
	if (oauthState == atomicState.oauthInitState){
    	LOG("callback() --> States matched!", 4)
		def tokenParams = [
			grant_type: "authorization_code",
			code      : code,
			client_id : smartThingsClientId,
			state	  : oauthState,
			redirect_uri: callbackUrl //"https://graph.api.smartthings.com/oauth/callback"
		]

		def tokenUrl = "${apiEndpoint}/token?${toQueryString(tokenParams)}"
        LOG("callback()-->tokenURL: ${tokenUrl}", 2)

		httpPost(uri: tokenUrl) { resp ->
			atomicState.refreshToken = resp.data.refresh_token
			atomicState.authToken = resp.data.access_token
            
            LOG("Expires in ${resp?.data?.expires_in} seconds")
            atomicState.authTokenExpires = now() + (resp.data.expires_in * 1000)
            LOG("swapped token: $resp.data; atomicState.refreshToken: ${atomicState.refreshToken}; atomicState.authToken: ${atomicState.authToken}", 2)
		}

		if (atomicState.authToken) { success() } else { fail() }

	} else {
    	LOG("callback() failed oauthState != atomicState.oauthInitState", 1, null, "warn")
	}
}

def success() {
	def message = """
    <p>Your ecobee Account is now connected to SmartThings!</p>
    <p>Click 'Done' to finish setup.</p>
    """
	connectionStatus(message)
}

def fail() {
	def message = """
        <p>The connection could not be established!</p>
        <p>Click 'Done' to return to the menu.</p>
    """
	connectionStatus(message)
}

def connectionStatus(message, redirectUrl = null) {
	def redirectHtml = ""
	if (redirectUrl) {
		redirectHtml = """
			<meta http-equiv="refresh" content="3; url=${redirectUrl}" />
		"""
	}

	def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Ecobee & SmartThings connection</title>
<style type="text/css">
        @font-face {
                font-family: 'Swiss 721 W01 Thin';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                font-weight: normal;
                font-style: normal;
        }
        @font-face {
                font-family: 'Swiss 721 W01 Light';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                font-weight: normal;
                font-style: normal;
        }
        .container {
                width: 90%;
                padding: 4%;
                /*background: #eee;*/
                text-align: center;
        }
        img {
                vertical-align: middle;
        }
        p {
                font-size: 2.2em;
                font-family: 'Swiss 721 W01 Thin';
                text-align: center;
                color: #666666;
                padding: 0 40px;
                margin-bottom: 0;
        }
        span {
                font-family: 'Swiss 721 W01 Light';
        }
</style>
</head>
<body>
        <div class="container">
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/ecobee%402x.png" alt="ecobee icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                ${message}
        </div>
</body>
</html>
"""

	render contentType: 'text/html', data: html
}
// End OAuth Callback URL and helpers

// Get the list of Ecobee Thermostats for use in the settings pages
def getEcobeeThermostats() {	
	LOG("====> getEcobeeThermostats() entered", 5)    
 	def requestBody = '{"selection":{"selectionType":"registered","selectionMatch":"","includeRuntime":true,"includeSensors":true,"includeProgram":true}}'
	def deviceListParams = [
			uri: apiEndpoint,
			path: "/1/thermostat",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.authToken}"],
			query: [format: 'json', body: requestBody]
	]

	def stats = [:]
    try {
        httpGet(deviceListParams) { resp ->
		LOG("getEcobeeThermostats() - httpGet() response: ${resp.data}", 4)
        
        // Initialize the Thermostat Data. Will reuse for the Sensor List intialization
        atomicState.thermostatData = resp.data        	
        
            if (resp.status == 200) {
            	LOG("getEcobeeThermostats() - httpGet() in 200 Response", 3)
                atomicState.numAvailTherms = resp.data.thermostatList?.size() ?: 0
                
            	resp.data.thermostatList.each { stat ->
					def dni = [app.id, stat.identifier].join('.')
					stats[dni] = getThermostatDisplayName(stat)
                }
            } else {                
                LOG("getEcobeeThermostats() - httpGet() in else: http status: ${resp.status}", 1)
                //refresh the auth token
                if (resp.status == 500 && resp.data.status.code == 14) {
                	LOG("getEcobeeThermostats() - Storing the failed action to try later", 1)
                    atomicState.action = "getEcobeeThermostats"
                    LOG("getEcobeeThermostats() - Refreshing your auth_token!", 1)
                    refreshAuthToken()
                } else {
                	LOG("getEcobeeThermostats() - Other error. Status: ${resp.status}  Response data: ${resp.data} ", 1)
                }
            }
        }
    } catch(Exception e) {
    	LOG("___exception getEcobeeThermostats(): ${e}", 1, null, "error")
        refreshAuthToken()
    }
	atomicState.thermostatsWithNames = stats
    LOG("atomicState.thermostatsWithNames == ${atomicState.thermostatsWithNames}", 4)
	return stats
}

// Get the list of Ecobee Sensors for use in the settings pages (Only include the sensors that are tied to a thermostat that was selected)
// NOTE: getEcobeeThermostats() should be called prior to getEcobeeSensors to refresh the full data of all thermostats
Map getEcobeeSensors() {	
    LOG("====> getEcobeeSensors() entered. thermostats: ${thermostats}", 5)

	def sensorMap = [:]
    def foundThermo = null
	// TODO: Is this needed?
	atomicState.remoteSensors = [:]    

	// Now that we routinely only collect the data that has changed in atomicState.thermostatData, we need to ALWAYS refresh that data
	// here so that we are sure we have everything we need here.
	getEcobeeThermostats()
	
	atomicState.thermostatData.thermostatList.each { singleStat ->
        def tid = singleStat.identifier
		LOG("thermostat loop: singleStat.identifier == ${tid} -- singleStat.remoteSensors == ${singleStat.remoteSensors} ", 4)   
        def tempSensors = atomicState.remoteSensors
    	if (!settings.thermostats.findAll{ it.contains(tid) } ) {
        	// We can skip this thermostat as it was not selected by the user
            LOG("getEcobeeSensors() --> Skipping this thermostat: ${tid}", 5)
        } else {
        	LOG("getEcobeeSensors() --> Entering the else... we found a match. singleStat == ${singleStat.name}", 4)
                        
        	// atomicState.remoteSensors[tid] = atomicState.remoteSensors[tid] ? (atomicState.remoteSensors[tid] + singleStat.remoteSensors) : singleStat.remoteSensors
            tempSensors[tid] = tempSensors[tid] ? tempSensors[tid] + singleStat.remoteSensors : singleStat.remoteSensors
            LOG("After atomicState.remoteSensors setup...", 5)	        
                        
            LOG("getEcobeeSensors() - singleStat.remoteSensors: ${singleStat.remoteSensors}", 4)
            LOG("getEcobeeSensors() - atomicState.remoteSensors: ${atomicState.remoteSensors}", 4)
		}
        atomicState.remoteSensors = tempSensors
        
		// WORKAROUND: Iterate over remoteSensors list and add in the thermostat DNI
		// 		 This is needed to work around the dynamic enum "bug" which prevents proper deletion
        LOG("remoteSensors all before each loop: ${atomicState.remoteSensors}", 5, null, "trace")
		atomicState.remoteSensors[tid].each {
        	LOG("Looping through each remoteSensor. Current remoteSensor: ${it}", 5, null, "trace")
			if (it.type == "ecobee3_remote_sensor") {
            	LOG("Adding an ecobee3_remote_sensor: ${it}", 4, null, "trace")
				def value = "${it?.name}"
				def key = "ecobee_sensor-"+ it?.id + "-" + it?.code
				sensorMap["${key}"] = value
			} else if ( (it.type == "thermostat") && (settings.showThermsAsSensor == true) ) {            	
				LOG("Adding a Thermostat as a Sensor: ${it}", 4, null, "trace")
           	    def value = "${it?.name}"
				def key = "ecobee_sensor_thermostat-"+ it?.id + "-" + it?.name
       	       	LOG("Adding a Thermostat as a Sensor: ${it}, key: ${key}  value: ${value}", 4, null, "trace")
				sensorMap["${key}"] = value + " (Thermostat)"
       	   	} else if ( it.type == "control_sensor" && it.capability[0]?.type == "temperature") {
       	   		// We can add this one as it supports temperature
       	      	LOG("Adding a control_sensor: ${it}", 4, null, "trace")
				def value = "${it?.name}"
				def key = "control_sensor-"+ it?.id
				sensorMap["${key}"] = value    
           	} else {
           		LOG("Did NOT add: ${it}. settings.showThermsAsSensor=${settings.showThermsAsSensor}", 4, null, "trace")
           	}
		}
	} // end thermostats.each loop
	
    LOG("getEcobeeSensors() - remote sensor list: ${sensorMap}", 4)
    atomicState.eligibleSensors = sensorMap
    atomicState.numAvailSensors = sensorMap.size() ?: 0
	return sensorMap
}
     
def getThermostatDisplayName(stat) {
	if(stat?.name)
		return stat.name.toString()
	return (getThermostatTypeName(stat) + " (${stat.identifier})").toString()
}

def getThermostatTypeName(stat) {
	return stat.modelNumber == "siSmart" ? "Smart Si" : "Smart"
}

def installed() {
	LOG("Installed with settings: ${settings}", 4)	
	initialize()
}

def updated() {	
    LOG("Updated with settings: ${settings}", 4)	
    // atomicState.Events = [:]
    // atomicState.Eventss = [:]
    // atomicState.aTestMap = [:]
    
    if (!atomicState.atomicMigrate) {
    	LOG("updated() - Migrating state to atomicState...", 2, null, "warn")        
        try {
        	state.collect {
            	LOG("traversing state: ${it} name: ${it.key}  value: ${it.value}")
                atomicState."${it.key}" = it.value
            }			
            atomicState.atomicMigrate = true
        } catch (Exception e) {
        	LOG("updated() - Migration of state t- atomicState failed with exception (${e})", 1, null, "error")            
        }
        try {
        	LOG("atomicState after migration", 4)
        	atomicState.collect {
	        	LOG("Traversing atomicState: ${it} name: ${it.key}  value: ${it.value}")
    	    }
		} catch (Exception e) {
        	LOG("Unable to traverse atomicState", 2, null, "warn")
        }
        atomicState.atomicMigrate = true
    }
    initialize()
}

def initialize() {	
    LOG("=====> initialize()", 4)    
    
    atomicState.connected = "full"        
    atomicState.reAttempt = 0
    atomicState.reAttemptPoll = 0
    
	try {
		unsubscribe()
    	unschedule() // reset all the schedules
	} catch (Exception e) {
    	LOG("updated() - Exception encountered trying to unschedule(). Exception: ${e}", 2, null, "error")
    }    
    
    def nowTime = now()
    def nowDate = getTimestamp()
    
    // Initialize several variables    
	atomicState.lastScheduledPoll = nowTime
    atomicState.lastScheduledPollDate = nowDate
    atomicState.lastScheduledWatchdog = nowTime
    atomicState.lastScheduledWatchdogDate = nowDate
	atomicState.lastPoll = nowTime
    atomicState.lastPollDate = nowDate    
    atomicState.lastWatchdog = nowTime
    atomicState.lastWatchdogDate = nowDate
    atomicState.lastUserDefinedEvent = now()
    atomicState.lastUserDefinedEventDate = getTimestamp()  
    atomicState.lastRevisions = [:]
    atomicState.latestRevisions = [:]
    atomicState.skipCount = 0
    atomicState.getWeather = true
    atomicState.runtimeUpdated = true
    atomicState.thermostatUpdated = true
    atomicState.sendJsonRetry = false
    atomicState.forcePoll = true				// make sure we get ALL the data after initialization
    atomicState.hourlyForcedUpdate = 0
    atomicState.needExtendedRuntime = true		// we'll stop getting it once we decide we don't need it
    
    
    def sunriseAndSunset = getSunriseAndSunset()
    // LOG("sunriseAndSunset == ${sunriseAndSunset}")
    if(location.timeZone) {
        atomicState.sunriseTime = sunriseAndSunset.sunrise.format("HHmm", location.timeZone).toDouble()
        atomicState.sunsetTime = sunriseAndSunset.sunset.format("HHmm", location.timeZone).toDouble()
    } else if( (sunriseAndSunset !=  [:]) && (location != null) ) {
        atomicState.sunriseTime = sunriseAndSunset.sunrise.format("HHmm").toDouble()
        atomicState.sunsetTime = sunriseAndSunset.sunset.format("HHmm").toDouble()
    } else {
    	atomicState.sunriseTime = "0500".toDouble()
        atomicState.sunsetTime = "1800".toDouble()
    }
	
	// Must do this AFTER setting up sunrise/sunset
	atomicState.timeOfDay = getTimeOfDay()
	    
    // Setup initial polling and determine polling intervals
	atomicState.pollingInterval = getPollingInterval()
    atomicState.watchdogInterval = 15	// In minutes: 14/28/42/56<- scheduleWatchdog should refresh tokens with 4 minutes to spare
    atomicState.reAttemptInterval = 15 	// In seconds
	
    if (state.initialized) {		
    	// refresh Thermostats and Sensor full lists
    	getEcobeeThermostats()
    	getEcobeeSensors()
    } 
    
    // Children
    def aOK = true
	if (settings.thermostats?.size() > 0) { aOK = aOK && createChildrenThermostats() }
	if (settings.ecobeesensors?.size() > 0) { aOK = aOK && createChildrenSensors() }    
    deleteUnusedChildren()
   
	// Initial poll()
    if (settings.thermostats?.size() > 0) { pollInit() }
    
    // Add subscriptions as little "daemons" that will check on our health    
    subscribe(location, scheduleWatchdog)
    subscribe(location, "routineExecuted", scheduleWatchdog)
    subscribe(location, "sunset", sunsetEvent)
    subscribe(location, "sunrise", sunriseEvent)
    subscribe(location, "position", scheduleWatchdog)
    
    if ( settings.useWatchdogDevices == true ) {
    	if ( settings.watchdogBattery?.size() > 0) { subscribe(settings.watchdogBattery, "battery", userDefinedEvent) }
        if ( settings.watchdogHumidity?.size() > 0) { subscribe(settings.watchdogHumidity, "humidity", userDefinedEvent) }
        if ( settings.watchdogLuminance?.size() > 0) { subscribe(settings.watchdogLuminance, "illuminance", userDefinedEvent) }
        if ( settings.watchdogMotion?.size() > 0) { subscribe(settings.watchdogMotion, "motion", userDefinedEvent) }
        if ( settings.watchdogSwitch?.size() > 0) { subscribe(settings.watchdogSwitch, "switch", userDefinedEvent) }
        if ( settings.watchdogTemp?.size() > 0) { subscribe(settings.watchdogTemp, "temperature", userDefinedEvent) }    
    }    
    
    // Schedule the various handlers
    LOG("Spawning scheduled events from initialize()", 5, null, "trace")
    if (settings.thermostats?.size() > 0) { 
    	LOG("Spawning the poll scheduled event. (settings.thermostats?.size() - ${settings.thermostats?.size()})", 4)
    	spawnDaemon("poll", false) 
	} 
    spawnDaemon("watchdog", false)
    
    //send activity feeds to tell that device is connected
    def notificationMessage = aOK ? "is connected to SmartThings" : "had an error during setup of devices"
    sendActivityFeeds(notificationMessage)
    atomicState.timeSendPush = null
    if (!atomicState.initialized) {
    	atomicState.initialized = true
        // These two below are for debugging and statistics purposes
        atomicState.initializedEpic = nowTime
        atomicState.initializedDate = nowDate
	}
    log.debug getVersionLabel()
    return aOK
}

private def createChildrenThermostats() {
	LOG("createChildrenThermostats() entered: thermostats=${settings.thermostats}", 5)
    // Create the child Thermostat Devices
	def devices = settings.thermostats.collect { dni ->
		def d = getChildDevice(dni)
		if(!d) {        	
            try {
				d = addChildDevice(app.namespace, getChildThermostatName(), dni, null, ["label":"EcoTherm: ${atomicState.thermostatsWithNames[dni]}", completedSetup:true])			
			} catch (physicalgraph.app.exception.UnknownDeviceTypeException e) {
            	LOG("You MUST add the ${getChildSensorName()} Device Handler to the IDE BEFORE running the setup.", 1, null, "error")
                return false
            }
            LOG("created ${d.displayName} with id ${dni}", 4)
		} else {
			LOG("found ${d.displayName} with id ${dni} already exists", 4)            
		}
		return d
	}
    LOG("Created/Updated ${devices.size()} thermostats")    
    return true
}

private def createChildrenSensors() {
	LOG("createChildrenSensors() entered: ecobeesensors=${settings.ecobeesensors}", 5)
    // Create the child Ecobee Sensor Devices
	def sensors = settings.ecobeesensors.collect { dni ->
		def d = getChildDevice(dni)
		if(!d) {        	
            try {
				d = addChildDevice(app.namespace, getChildSensorName(), dni, null, ["label":"EcoSensor: ${atomicState.eligibleSensors[dni]}", completedSetup:true])
			} catch (physicalgraph.app.exception.UnknownDeviceTypeException e) {
            	LOG("You MUST add the ${getChildSensorName()} Device Handler to the IDE BEFORE running the setup.", 1, null, "error")
                return false
            }
            LOG("created ${d.displayName} with id $dni", 4)
		} else {
        	LOG("found ${d.displayName} with id $dni already exists", 4)
		}
		return d
	}
	LOG("Created/Updated ${sensors.size()} sensors.")
    return true
}

// NOTE: For this to work correctly getEcobeeThermostats() and getEcobeeSensors() should be called prior
private def deleteUnusedChildren() {
	LOG("deleteUnusedChildren() entered", 5)    
    
    if (settings.thermostats?.size() == 0) {
    	// No thermostats, need to delete all children
        LOG("Deleting All My Children!", 2, null, "warn")
    	getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }        
    } else {
    	// Only delete those that are no longer in the list
        // This should be a combination of any removed thermostats and any removed sensors
        def allMyChildren = getAllChildDevices()
        LOG("These are currently all of my childred: ${allMyChildren}", 5, null, "debug")
        
        // Update list of "eligibleSensors"       
        def childrenToKeep = (thermostats ?: []) + (atomicState.eligibleSensors?.keySet() ?: [])
        LOG("These are the children to keep around: ${childrenToKeep}", 4, null, "trace")
        
    	def childrenToDelete = allMyChildren.findAll { !childrenToKeep.contains(it.deviceNetworkId) }        
        
        LOG("Ready to delete these devices. ${childrenToDelete}", 4, null, "trace")
		if (childrenToDelete.size() > 0) childrenToDelete?.each { deleteChildDevice(it.deviceNetworkId) } //inherits from SmartApp (data-management)    
    }    
}

def sunriseEvent(evt) {
	LOG("sunriseEvent() - with evt (${evt?.name}:${evt?.value})", 4, null, "info")
	atomicState.timeOfDay = "day"
    atomicState.lastSunriseEvent = now()
    atomicState.lastSunriseEventDate = getTimestamp()
    if(location.timeZone) {
    	atomicState.sunriseTime = new Date().format("HHmm", location.timeZone).toInteger()
    } else {
    	atomicState.sunriseTime = new Date().format("HHmm").toInteger()
    }
	atomicState.getWeather = true	// force updating of the weather icon in the thermostat
    atomicState.forcePoll = true
    scheduleWatchdog(evt, true)    
}

def sunsetEvent(evt) {
	LOG("sunsetEvent() - with evt (${evt?.name}:${evt?.value})", 4, null, "info")
	atomicState.timeOfDay = "night"
    atomicState.lastSunsetEvent = now()
    atomicState.lastSunsetEventDate = getTimestamp()
    if(location.timeZone) {
    	atomicState.sunsetTime = new Date().format("HHmm", location.timeZone).toInteger()
	} else {
    	atomicState.sunsetTime = new Date().format("HHmm").toInteger()
    }
	atomicState.getWeather = true	// force updating of the weather icon in the thermostat
    atomicState.forcePoll = true	// force a complete poll...
    scheduleWatchdog(evt, true)
}

// Event on a monitored device
def userDefinedEvent(evt) {
    if ( ((now() - atomicState.lastUserDefinedEvent) / 60000.0) < 0.5 ) { 
    	LOG("userDefinedEvent() - time since last event is less than 30 seconds, ignoring.", 4)
    	return 
 	}
    
    LOG("userDefinedEvent() - with evt (Device:${evt?.displayName} ${evt?.name}:${evt?.value})", 4, null, "info")
    
	poll()
    atomicState.lastUserDefinedEvent = now()
    atomicState.lastUserDefinedEventDate = getTimestamp()
    atomicState.lastUserDefinedEventInfo = "Event Info: (Device:${evt?.displayName} ${evt?.name}:${evt?.value})"
    
    def lastUserWatchdog = atomicState.lastUserWatchdogEvent
    if (lastUserWatchdog) {
    	if ( ((now() - lastUserWatchdog) / 60000) < 3 ) {
    		// Don't bother running the watchdog on EVERY userDefinedEvent - that's really overkill.
    		LOG('userDefinedEvent() - polled, but time since last watchdog is less than 3 minutes, exiting without performing additional actions', 4)
    		return
        }
    }
    atomicState.lastUserWatchdogEvent = now()
    scheduleWatchdog(evt, true)
}

def scheduleWatchdog(evt=null, local=false) {
	def results = true  
    if (debugLevel(4)) {
    	def evtStr = evt ? "${evt.name}:${evt.value}" : 'null'
    	LOG("scheduleWatchdog() called with evt (${evtStr}) & local (${local})", 4, null, "trace")
    }
    
    // Only update the Scheduled timestamp if it is not a local action or from a subscription
    if ( (evt == null) && (local==false) ) {
    	atomicState.lastScheduledWatchdog = now()
        atomicState.lastScheduledWatchdogDate = getTimestamp()
        atomicState.getWeather = true								// next pollEcobeeApi for runtime changes should also get the weather object
        
        // do a forced update once an hour, just because (e.g., forces Hold Status to update completion date string)
        def counter = atomicState.hourlyForcedUpdate
        counter = (!counter) ? 1 : counter + 1
        if (counter == 4) {
        	counter = 0
            atomicState.forcePoll = true
        }
        atomicState.hourlyForcedUpdate = counter
	}
    
    // Check to see if we have called too soon
    def timeSinceLastWatchdog = (now() - atomicState.lastWatchdog) / 60000
    if ( timeSinceLastWatchdog < 1 ) {
    	LOG("It has only been ${timeSinceLastWatchdog} since last scheduleWatchdog was called. Please come back later.", 4, null, "trace")
        return true
    }
   
    atomicState.lastWatchdog = now()
    atomicState.lastWatchdogDate = getTimestamp()
    def pollAlive = isDaemonAlive("poll")
    def watchdogAlive = isDaemonAlive("watchdog")
    
    LOG("After watchdog tagging",4,null,'trace')
	if (apiConnected() == 'lost') {
    	// Possibly a false alarm? Check if we can update the token with one last fleeting try...
        if( refreshAuthToken() ) { 
        	// We are back in business!
			LOG("scheduleWatchdog() - Was able to recover the lost connection. Please ignore any notifications received.", 1, null, "warn")
        } else {        
			LOG("scheduleWatchdog() - Unable to schedule handlers do to loss of API Connection. Please ensure you are authorized.", 1, null, "error")
			return false
		}
	}

    LOG("scheduleWatchdog() --> pollAlive==${pollAlive}  watchdogAlive==${watchdogAlive}", 4, null, "debug")
    
    // Reschedule polling if it has been a while since the previous poll    
    if (!pollAlive) { spawnDaemon("poll") }
    if (!watchdogAlive) { spawnDaemon("watchdog") }
    
    return true
}

// Watchdog Checker
private def Boolean isDaemonAlive(daemon="all") {
	// Daemon options: "poll", "auth", "watchdog", "all"    
    def daemonList = ["poll", "auth", "watchdog", "all"]
    String preText = getDebugLevel() <= 2 ? '' : 'isDeamonAlive() - '
	Integer pollingInterval = getPollingInterval()

	daemon = daemon.toLowerCase()
    def result = true    
    
	LOG("isDaemonAlive() - now() == ${now()} for daemon (${daemon})", 5, null, "trace")
	
    // No longer running an auth Daemon, because we need the scheduler slot (max 4 scheduled things, poll + watchdog use 2)	
	// def timeBeforeExpiry = atomicState.authTokenExpires ? ((atomicState.authTokenExpires - now()) / 60000) : 0
    // LOG("isDaemonAlive() - Time left (timeBeforeExpiry) until expiry (in min): ${timeBeforeExpiry}", 4, null, "info")
	
    if (daemon == "poll" || daemon == "all") {
		def lastScheduledPoll = atomicState.lastScheduledPoll
		def timeSinceLastScheduledPoll = (!lastSchedulePoll || (lastScheduledPoll == 0)) ? 0 : ((now() - lastScheduledPoll) / 60000)
		LOG("isDaemonAlive() - Time since last poll? ${timeSinceLastScheduledPoll} -- lastScheduledPoll == ${lastScheduledPoll}", 4, null, "info")
    	LOG("isDaemonAlive() - Checking daemon (${daemon}) in 'poll'", 4, null, "trace")
        def maxInterval = pollingInterval + 2
        if ( timeSinceLastScheduledPoll >= maxInterval ) { result = false }
	}	
    
    if (daemon == "watchdog" || daemon == "all") {
		def lastScheduledWatchdog = atomicState.lastScheduledWatchdog
	    def timeSinceLastScheduledWatchdog = (!lastScheduledWatchdog || (atomicState.lastScheduledWatchdog == 0)) ? 0 : ((now() - lastScheduledWatchdog) / 60000)
		LOG("isDaemonAlive() - Time since watchdog activation? ${timeSinceLastScheduledWatchdog} -- lastScheduledWatchdog == ${lastScheduledWatchdog}", 4, null, "info")
    	LOG("isDaemonAlive() - Checking daemon (${daemon}) in 'watchdog'", 4, null, "trace")
        def maxInterval = atomicState.watchdogInterval + 2
        LOG("isDaemonAlive(watchdog) - timeSinceLastScheduledWatchdog=(${timeSinceLastScheduledWatchdog})  Timestamps: (${atomicState.lastScheduledWatchdogDate}) (epic: ${lastScheduledWatchdog}) now-(${now()})", 4, null, "trace")
        if ( timeSinceLastScheduledWatchdog >= maxInterval ) { result = false }
    }
    
	if (!daemonList.contains(daemon) ) {
    	// Unkown option passed in, gotta punt
        LOG("isDaemonAlive() - Unknown daemon: ${daemon} received. Do not know how to check this daemon.", 1, null, "error")
        result = false
    }
    LOG("isDaemonAlive() - result is ${result}", 4, null, "trace")
    if (!result) LOG("${preText}(${daemon}) has died", 1, null, 'warn')
    return result
}

private def Boolean spawnDaemon(daemon="all", unsched=true) {
	// Daemon options: "poll", "auth", "watchdog", "all"    
    def daemonList = ["poll", "auth", "watchdog", "all"]
	Random rand = new Random()
	
	Integer pollingInterval = getPollingInterval()
    
    daemon = daemon.toLowerCase()
    def result = true
    
    if (daemon == "poll" || daemon == "all") {
    	LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'poll'", 4, null, "trace")
        // Reschedule the daemon
        try {
            if( unsched ) { unschedule("pollScheduled") }
            if ( canSchedule() ) { 
            	LOG("Polling Interval == ${pollingInterval}", 4)
            	if ((pollingInterval < 5) && (pollingInterval > 1)) {	// choices were 1,2,3,5,10,15,30
                	LOG("Using schedule instead of runEvery with pollingInterval: ${pollingInterval}", 4)
					int randomSeconds = rand.nextInt(59)
					schedule("${randomSeconds} 0/${pollingInterval} * * * ?", "pollScheduled")                    
                } else {
                	LOG("Using runEvery to setup polling with pollingInterval: ${pollingInterval}", 4)
        			"runEvery${pollingInterval}Minute${pollingInterval!=1?'s':''}"("pollScheduled")
                }
                // Only poll now if we were recovering - if not asked to unschedule, then whoever called us will handle the first poll (as in initialize())
            	if (unsched) result = pollScheduled() && result
			} else {
            	LOG("canSchedule() is NOT allowed or result already false! Unable to schedule poll daemon!", 1, null, "error")
        		result = false
        	}
        } catch (Exception e) {
        	LOG("spawnDaemon() - Exception when performing spawn for ${daemon}. Exception: ${e}", 1, null, "error")
            result = false
        }		
    }
    
    if (daemon == "watchdog" || daemon == "all") {
    	LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'watchdog'", 4, null, "trace")
        // Reschedule the daemon
        try {
            if( unsched ) { unschedule("scheduleWatchdog") }
            if ( canSchedule() ) { 
        		"runEvery${atomicState.watchdogInterval}Minutes"("scheduleWatchdog")
            	result = result && true
			} else {
            	LOG("canSchedule() is NOT allowed or result already false! Unable to schedule daemon!", 1, null, "error")
        		result = false
        	}
        } catch (Exception e) {
        	LOG("spawnDaemon() - Exception when performing spawn for ${daemon}. Exception: ${e}", 1, null, "error")
            result = false
        }
		atomicState.lastScheduledWatchdog = now()
        atomicState.lastScheduledWatchdogDate = getTimestamp()
        atomicState.getWeather = true	// next pollEcobeeApi for runtime changes should also get the weather object
    }
    
    if (!daemonList.contains(daemon) ) {
    	// Unkown option passed in, gotta punt
        LOG("isDaemonAlive() - Unknown daemon: ${daemon} received. Do not know how to check this daemon.", 1, null, "error")
        result = false
    }
    return result
}

def updateLastPoll(Boolean isScheduled=false) {
	if (isScheduled) {
    	atomicState.lastScheduledPoll = now()
        atomicState.lastScheduledPollDate =  getTimestamp()
    } else {
    	atomicState.lastPoll = now()
        atomicState.lastPollDate = getTimestamp()
    }
}

def poll() {		
    if (debugLevel(4)) LOG("poll() - Running at ${getTimestamp()} (epic: ${now()})", 4, null, "trace")   
	pollChildren(null,null) // Poll ALL the children at the same time for efficiency    
}

// Called by scheduled() event handler
def pollScheduled() {
	updateLastPoll(true)
	LOG("pollScheduled() - Running at ${atomicState.lastScheduledPollDate} (epic: ${atomicState.lastScheduledPoll})", 4, null, "trace")    
    return poll()
}

// Called during initialization to get the inital poll
def pollInit() {
	LOG("pollInit()", 5)
    // atomicState.forcePoll = true // Initialize the variable and force a poll even if there was one recently    
	runIn(2, pollChildren, [overwrite: true]) 		// Hit the ecobee API for update on all thermostats, in 2 seconds
}

def pollChildren(deviceId = null, child=null) {  
	LOG("pollChildren() - deviceId ${deviceId} child ${child}", 4, child, 'trace')
    def forcePoll = deviceId || atomicState.forcePoll ? true : false
    atomicState.forcePoll = forcePoll

	LOG("=====> pollChildren() - atomicState.forcePoll(${forcePoll})  atomicState.lastPoll(${atomicState.lastPoll})  now(${now()})  atomicState.lastPollDate(${atomicState.lastPollDate})", 4, child, "trace")
    
	if(apiConnected() == "lost") {
    	// Possibly a false alarm? Check if we can update the token with one last fleeting try...
        LOG("apiConnected() == lost, try to do a recovery, else we are done...", 3, child, "debug")
        if( refreshAuthToken() ) { 
        	// We are back in business!
			LOG("pollChildren() - Was able to recover the lost connection. Please ignore any notifications received.", 1, child, "warn")
        } else {        
			LOG("pollChildren() - Unable to schedule handlers do to loss of API Connection. Please ensure you are authorized.", 1, child, "error")
			return false
		}
	}

    // Run a watchdog checker here
    scheduleWatchdog(null, true)    
    
    if (settings.thermostats?.size() < 1) {
    	LOG("pollChildren() - Nothing to poll as there are no thermostats currently selected", 1, child, "warn")
		return true
    }    

    // Check if anything has changed in the thermostatSummary (really don't need to call EcobeeAPI if it hasn't).
    String thermostatsToPoll = forcePoll ? getChildThermostatDeviceIdsString() : deviceId ? deviceId : getChildThermostatDeviceIdsString()
    
    boolean somethingChanged = forcePoll ? true : checkThermostatSummary(thermostatsToPoll)
    if (!forcePoll) thermostatsToPoll = atomicState.changedThermostatIds
    
    String preText = getDebugLevel() <= 2 ? '' : 'pollChildren() - ' 
    if (forcePoll || somethingChanged) {
        LOG("${preText}Requesting updates for thermostat${thermostatsToPoll.contains(',')?'s':''} ${thermostatsToPoll}${forcePoll?' (forced)':''}", 2, child, 'trace')
    	pollEcobeeAPI(thermostatsToPoll)  // This will update the values saved in the state which can then be used to send the updates
	} else {   	 
        LOG(preText+'No updates...', 2, child, 'trace')
        return true
    }
    
    Integer numSensors = 0
    if (forcePoll || somethingChanged) {
		// Iterate over ONLY THE CHILDREN THAT HAVE DATA - this is MUCH faster than iterating over ALL the children, because
        // we now only send changed data for changed thermostats
        atomicState.thermostats?.each { DNI ->
        	if (DNI.value?.data) getChildDevice(DNI.key)?.generateEvent(DNI.value.data)
        }
        atomicState.remoteSensorsData?.each { DNI ->
        	if (DNI.value?.data) getChildDevice(DNI.key)?.generateEvent(DNI.value.data)
        }
	}
    if (numSensors > 0) LOG("pollChildren() - Events transferred to ${numSensors} sensors", 3, null, 'info')
    return true
}

// enables child SmartApps to send events to child Smart Devices using only the DNI
def generateChildEvent( childDNI, dataMap) {
	getChildDevice(childDNI)?.generateEvent(dataMap)
}

// NOTE: This only updated the apiConnected state now - used to resend all the data, but that's pretty much a waste of CPU cycles now.
// 		 If the UI needs updating, the refresh now does a forcePoll on the entire device.
private def generateEventLocalParams() {
	// Iterate over all the children
    LOG("generateEventLocalParams() - updating API status", 3, null, 'info')
    def connected = apiConnected()
    def data = [
       	apiConnected: connected
    ]
    
    settings.thermostats?.each {
    	getChildDevice(it)?.generateEvent(data)
     }
 
 /*
	def d = getChildDevices()
    d?.each() { oneChild ->
    	LOG("generateEventLocalParams() - Processing data for child: ${oneChild} has ${oneChild.capabilities}", 4, "", 'info')
        
    	if( oneChild.hasCapability("Thermostat") ) {
        	// We found a Thermostat, send local params as events
            LOG("generateEventLocalParams() - We found a Thermostat!", 4)

            
            // Update the API connected state for ALL the associated thermostats
            // Note that we can't modify a Map element in atomicState directly - we have to read to a local variable, modify, then write it back
            // atomicState.thermostats[oneChild.device.deviceNetworkId]?.data?.apiConnected = apiConnected() 
            Map tempData = atomicState.thermostats
            tempData[oneChild.device.deviceNetworkId]?.data?.apiConnected = connected
            atomicState.thermostats = tempData
            oneChild.generateEvent(data)
        } else {
        	// We must have a remote sensor
            // LOG("generateEventLocalParams() - Updating sensor data: ${oneChild.device.deviceNetworkId}", 4)
			// No local params to send            
        } 
    }
    */
}

// Checks if anything has changed since the last time this routine was called, using lightweight thermostatSummary poll
// NOTE: Despite the documentation, Runtime Revision CAN change more frequently than every 3 minutes - equipmentStatus is 
//       apparently updated in real time (or at least very close to real time)
private boolean checkThermostatSummary(thermostatIdsString) {
	String preText = getDebugLevel() <= 2 ? '' : 'checkThermostatSummary - '
    
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + thermostatIdsString + '","includeEquipmentStatus":"false"}}'
	def pollParams = [
			uri: apiEndpoint,
			path: "/1/thermostatSummary",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
			query: [format: 'json', body: jsonRequestBody]
	]
	
    def result = false
	def statusCode=999
	String tstatsStr = ''     
	
	try {
		httpGet(pollParams) { resp ->
			if(resp.status == 200) {
				LOG("checkThermostatSummary() - poll results returned resp.data ${resp.data}", 4)
				statusCode = resp.data.status.code
				if (statusCode == 0) { 
                    def revisions = resp.data.revisionList
					def thermostatUpdated = false
					def runtimeUpdated = false
                    tstatsStr = ""
                    def latestRevisions = [:]
						
                    // each revision is a separate thermostat
                    revisions.each { rev ->
						def tempList = rev.split(':') as List
                        def tstat = tempList[0]
                        def latestDetails = tempList.reverse().take(4).reverse() as List	// isolate the 4 timestamps

                        def lastRevisions = atomicState.lastRevisions
                        if (lastRevisions && !(lastRevisions instanceof Map)) lastRevisions = [:]			// hack to switch from List to Map for revision cache
                        def lastDetails = lastRevisions && lastRevisions.containsKey(tstat) ? lastRevisions[tstat] : []			// lastDetails should be the prior 4 timestamps
                            
                        // check if the current tstat is updated
                        def tru = false
                        def ttu = false
						if (lastDetails) {			// if we have prior revision details
							if (lastDetails[2] != latestDetails[2]) tru = true 
							if (lastDetails[0] != latestDetails[0]) ttu = true 
                        } else {					// no priors, assume both have been updated
                            tru = true 
                            ttu = true
                        }

                        // update global flags (we update the superset of changes for all requested tstats)
                        if (tru || ttu) {
                            runtimeUpdated = (runtimeUpdated || tru)
                            thermostatUpdated = (thermostatUpdated || ttu)
                            result = true
                            tstatsStr = (tstatsStr=="") ? tstat : (tstatsStr.contains(tstat)) ? tstatsStr : tstatsStr + ",${tstat}"
                        }
                        latestRevisions[tstat] = latestDetails			// and, log the new timestamps
					}
					atomicState.latestRevisions = latestRevisions		// let pollEcobeeAPI update last with latest after it finishes the poll
                    atomicState.thermostatUpdated = thermostatUpdated	// Revised: settings, program, event, device
					atomicState.runtimeUpdated = runtimeUpdated			// Revised: runtime, equip status, remote sensors, weather?
                    atomicState.changedThermostatIds = tstatsStr    	// only these thermostats need to be requested in pollEcobeeAPI
				}
                // if we get here, we had http status== 200, but API status != 0
			} else {
				LOG("${preText}ThermostatSummary poll got http status ${resp.status}", 1, null, "error")
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {   
        result = false // this thread failed to get the summary
        if ((e.statusCode == 500) && (e.response.data.status.code == 14) /*&& ((atomicState.authTokenExpires - now()) <= 0) */){
            LOG("checkThermostatSummary() - HttpResponseException occurred: Auth_token has expired", 3, null, "info")
            atomicState.action = "pollChildren"
            LOG( "Refreshing your auth_token!", 4)
            if ( refreshAuthToken() ) {
                // Note that refreshAuthToken will reschedule pollChildren if it succeeds in refreshing the token...
                LOG( preText+'Auth_token refreshed', 2, null, 'info')
            } else {
                LOG(preText+'Auth_token refresh failed', 1, null, 'error')
            }
        } else {
        	LOG("${preText}HttpResponseException; Exception info: ${e} StatusCode: ${e.statusCode} response.data.status.code: ${e.response.data.status.code}", 1, null, "error")
        }
    } catch (java.util.concurrent.TimeoutException e) {
    	LOG("checkThermostatSummary() - TimeoutException: ${e}.", 1, null, "warn")
        // Do not add an else statement to run immediately as this could cause an long looping cycle if the API is offline
        runIn(atomicState.reAttemptInterval.toInteger(), "pollChildren", [overwrite: true])
       	result = false    
    }

    LOG("<===== Leaving checkThermostatSummary() result: ${result}, tstats: ${tstatsStr}", 4, null, "info")
	return result
}

private def pollEcobeeAPI(thermostatIdsString = '') {
	LOG("=====> pollEcobeeAPI() entered - thermostatIdsString = ${thermostatIdsString}", 4, null, "info")

	boolean forcePoll = atomicState.forcePoll		// lightweight way to use atomicStates that we don't want to change under us
    boolean thermostatUpdated = atomicState.thermostatUpdated
    boolean runtimeUpdated = atomicState.runtimeUpdated
    boolean getWeather = atomicState.getWeather
    String preText = getDebugLevel() <= 2 ? '' : 'pollEcobeeAPI() - '
	boolean somethingChanged = false
    String checkTherms
    String allMyChildren = getChildThermostatDeviceIdsString()

	// forcePoll = true
	
	if (forcePoll) {
    	// if it's a forcePoll, and thermostatIdString specified, we check only the specified thermostat, else we check them all
        checkTherms = thermostatIdsString ? thermostatIdsString :  allMyChildren
        if (checkTherms == allMyChildren) atomicState.hourlyForcedUpdated = 0		// reset the hourly check counter if we are forcePolling ALL the thermostats
        somethingChanged = true
    } else if (atomicState.lastRevisions != atomicState.latestRevisions) {
        // we already know there are changes
    	somethingChanged = true	
    } else {
        // log.debug "Shouldn't be checkingThermostatSummary() again!"
    	somethingChanged = checkThermostatSummary(thermostatIdsString)
        thermostatUpdated = atomicState.thermostatUpdated				// update these again after checkThermostatSummary
    	runtimeUpdated = atomicState.runtimeUpdated
    }
    
    // if nothing has changed, and this isn't a forced poll, just return (keep count of polls we have skipped)
    // This probably can't happen anymore...shouldn't event be here if nothing has changed and not a forced poll...
    if (!somethingChanged && !forcePoll) {  
    	LOG("<===== Leaving pollEcobeeAPI() - nothing changed, skipping heavy poll...", 4 )
       	return true		// act like we completed the heavy poll without error
	} else {
       	// if getting the weather, get for all thermostats at the same time. Else, just get the thermostats that changed
       	checkTherms = (runtimeUpdated && getWeather) ? allMyChildren : (forcePoll ? checkTherms : atomicState.changedThermostatIds)
    }  
    
    // Let's only check those thermostats that actually changed...unless this is a forcePoll - or if we are getting the weather 
    // (we need to get weather for all therms at the same time, because we only update every 15 minutes and use the cached version
    // the rest of the time)
    
	// get equipmentStatus every time
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + checkTherms + '","includeEquipmentStatus":"true"'
	
    String gw = '( equipmentStatus'
	if (forcePoll || thermostatUpdated) {
		jsonRequestBody += ',"includeSettings":"true","includeProgram":"true","includeEvents":"true"'
		gw += ' thermostat'
	}
	if (forcePoll || runtimeUpdated) {
		jsonRequestBody += ',"includeRuntime":"true"'
        gw += ' runtime'
        
        // only get extendedRuntime if we have a humidifier/dehumidifier (depending upon HVAC mode at the moment)
        if (atomicState.needExtendedRuntime) {
        	jsonRequestBody += ',"includeExtendedRuntime":"true"'
            gw += ' extendedRuntime'
        }

        // only get sensorData if we have any sensors configured
		if (forcePoll || settings.ecobeesensors?.size() > 0) {
			jsonRequestBody += ',"includeSensors":"true"'
			gw += ' sensors'
		}
        if (getWeather) {
        	jsonRequestBody += ',"includeWeather":"true"'		// time to get the weather report (only changes every 15 minutes or so - watchdog sets this when it runs)
            gw += ' weather'
        }
		gw += ' )'
		LOG("${preText}Getting ${gw} for thermostat${checkTherms.contains(',')?'s':''} ${checkTherms}", 2, null, 'info')
	}
	jsonRequestBody += '}}'   
	LOG("pollEcobeeAPI() - jsonRequestBody is: ${jsonRequestBody}", 4, null, 'trace')
 
    def result = false
	
	def pollParams = [
			uri: apiEndpoint,
			path: "/1/thermostat",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
			query: [format: 'json', body: jsonRequestBody]
	]

	def tidList = []
	try{
		httpGet(pollParams) { resp ->
			if(resp.status == 200) {
				if (resp.data) atomicState.thermostatData = resp.data		// this now only stores the most recent collection of changed data
															// this may not be the entire set of data for all of our thermostats, so the rest 
                                                            // of this code will calculate updates from the individual data objects (which 
                                                            // always cache the latest values recieved)

                // Update the atomicState caches for each received data object(s)
                def tempSettings = [:]
                def tempProgram = [:]
                def tempEvents = [:]
                def tempRuntime = [:]
                def tempExtendedRuntime = [:]
                def tempWeather = [:]
                def tempSensors = [:]
                def tempEquipStat = [:]
                
                // collect the returned data into temporary individual caches (because we can't update individual Map items in an atomicState Map)
                resp.data.thermostatList.each { stat ->
                
					String tid = stat.identifier.toString()
                    tidList += [tid]
                    LOG("pollEcobeeAPI() - Parsing data for thermostat ${tid}", 3, null, 'info')
                    
					tempEquipStat[tid] = stat.equipmentStatus 			// always store ("" is a valid return value)
                    
                    if (forcePoll || thermostatUpdated) {
                        if (stat.settings) tempSettings[tid] = stat.settings
                        if (stat.program) tempProgram[tid] = stat.program
                        if (stat.events) tempEvents[tid] = stat.events
                    }
 					if (forcePoll || runtimeUpdated) {
 						if (stat.runtime) tempRuntime[tid] = stat.runtime
                        if (stat.extendedRuntime) tempExtendedRuntime[tid] = stat.extendedRuntime
                        if (stat.remoteSensors) tempSensors[tid] = stat.remoteSensors 
                        if (stat.weather) tempWeather[tid] = stat.weather
                    }
                }
				
                // OK, we have all the data received stored in their appropriate temporary cache, let's update the atomicState caches
				if (tempEquipStat != [:]) {
					if (atomicState.equipmentStatus) tempEquipStat = atomicState.equipmentStatus + tempEquipStat
					atomicState.equipmentStatus = tempEquipStat
				}
                
                if (forcePoll || thermostatUpdated) {
                	if (tempProgram != [:]) {
                   		if (atomicState.program) tempProgram = atomicState.program + tempProgram 
                   		atomicState.program = tempProgram
                	}
                	if (tempEvents != [:]) {
                   		if (atomicState.events) tempEvents = atomicState.events + tempEvents
                   		atomicState.events = tempEvents
                	} 
                	if (tempSettings != [:]) {
                   		if (atomicState.settings) tempSettings = atomicState.settings + tempSettings 
                   		atomicState.settings = tempSettings
                        // While we are here, let's find out if we really need the extendedRuntime data...
						// We do it here because: a) it only changes when settings changes, and b) we already have the settings Map in hand
                        // (thus it saves the cost of copying atomicState.settings later)
                       	boolean needExtRT = false
			       		tempSettings.each { 
            				if (!needExtRT && checkTherms.contains(it.key)){
            					switch (it.value.hvacMode) {
            						case 'heat':
                                    case 'auxHeatOnly':
                     					if (it.value.hasHumidifier && (it.value.humidifierMode != 'off')) needExtRT = true 
                        				break;
                    				case 'cool':
                    					if ((it.value.dehumidifierMode == 'on') && (it.value.hasDehumidifier || 
                                                	(it.value.dehumidifyWithAC && (it.value.dehumidifyOvercoolOffset != 0)))) needExtRT = true 
                        				break;
                    				case 'auto':
                        				if ((it.value.hasHumidifier && (it.value.humidifierMode != 'off')) || 
                                        	(it.value.dehumidifierMode == 'on') && (it.value.hasDehumidifier || 
                                                	(it.value.dehumidifyWithAC && (it.value.dehumidifyOvercoolOffset != 0)))) needExtRT = true
                        				break;
                				}	
            				}
        				}
                        atomicState.needExtendedRuntime = needExtRT
                    }                   
                }
                if (forcePoll || runtimeUpdated) {
                    if (tempRuntime != [:]) {
                    	if (atomicState.runtime) tempRuntime = atomicState.runtime + tempRuntime
                    	atomicState.runtime = tempRuntime
                    }
                    if (tempExtendedRuntime != [:]) {
                    	if (atomicState.extendedRuntime) tempExtendedRuntime = atomicState.extendedRuntime + tempExtendedRuntime
                    	atomicState.extendedRuntime = tempExtendedRuntime
                    }
                    if (tempSensors != [:]) {
                    	if (atomicState.remoteSensors) tempSensors = atomicState.remoteSensors + tempSensors 
                    	atomicState.remoteSensors = tempSensors
                    }
                    
                    if (tempWeather != [:]) {
                    	if (atomicState.weather) tempWeather = atomicState.weather + tempWeather 
                    	atomicState.weather = tempWeather
                    }
				}
                
                // OK, Everything is safely stored, let's get to parsing the objects and finding the actual changed data
                updateLastPoll()
                LOG("pollEcobeeAPI() - Parsing complete", 3, null, 'info')

                // Update the data and send it down to the devices as events
                if (settings.thermostats) updateThermostatData()  	// update thermostats first (some sensor data is dependent upon thermostat changes)
				if (settings.ecobeesensors) updateSensorData()		// Only update configured sensors (and then only if they have changes)
                
                
                // pollChildren() will actually send all the events we just created, once we return
				// just a bit more housekeeping...
				result = true
                atomicState.lastRevisions = atomicState.latestRevisions		// copy the Map
               	if (forcePoll) atomicState.forcePoll = false				// it's ok to clear the flag now
                if (runtimeUpdated) atomicState.runtimeUpdated = false
                if (thermostatUpdated) { atomicState.thermostatUpdated = false; atomicState.hourlyForcedUpdate = 0 }
                if (runtimeUpdated && getWeather) atomicState.getWeather = false
                
                // Not sure why this is here...it just tells the children that we are once again connected to the Ecobee API Cloud
                if (apiConnected() != "full") {
					apiRestored()
                    generateEventLocalParams() // Update the connection status
                }
                
                def tNames = resp.data.thermostatList?.name.toString()
                def numStats = tidList?.size()
                if (preText == '') {	// avoid calling debugLevel() again) {
					LOG("Updated ${numStats} thermostat${(numStats>1)?'s':''}: ${tidList}", 2, null, 'info')
                } else {
                	LOG("pollEcobeeAPI() - Updated ${numStats} thermostat${(numStats>1)?'s':''}: ${tNames}/${tidList} ${atomicState.thermostats}", 4, null, 'info')
				}
			} else {
				LOG("${preText}Polling children & got http status ${resp.status}", 1, null, "error")

				//refresh the auth token
				if (resp.status == 500 && resp.data.status.code == 14) {
					LOG("${preText}Resp.status: ${resp.status} Status Code: ${resp.data.status.code}. Unable to recover", 1, null, "error")
                    // Should not be possible to recover from a code 14 but try anyway?
                    
                    apiLost("pollEcobeeAPI() - Resp.status: ${resp.status} Status Code: ${resp.data.status.code}. Unable to recover.")
				}
				else {
					LOG("pollEcobeeAPI() - Other responses received. Resp.status: ${resp.status} Status Code: ${resp.data.status.code}.", 1, null, "error")
				}
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {  
    	result = false  // this thread failed
    	if ((e.statusCode == 500) && (e.response.data.status.code == 14)) {
           	LOG("pollEcobeAPI() - HttpResponseException occurred: Auth_token has expired", 3, null, "info")
           	atomicState.action = "pollChildren"
            LOG( "pollEcobeeAPI() - Refreshing your auth_token!", 4)
            if ( refreshAuthToken() ) { 
            	// Note that refreshAuthToken will reschedule pollChildren if it succeeds in refreshing the token...
                LOG( preText+'Auth_token refreshed', 2, null, 'info')
            } else {
                LOG( preText+'Auth_token refresh failed', 1, null, 'error')
            }
        } else {
        	LOG("${preText}HttpResponseException; Exception info: ${e} StatusCode: ${e.statusCode} response.data.status.code: ${e.response.data.status.code}", 1, null, "error")
        }
    } catch (java.util.concurrent.TimeoutException e) {
    	LOG("${preText}TimeoutException: ${e}.", 1, null, "warn")
        // Do not add an else statement to run immediately as this could cause an long looping cycle if the API is offline
        runIn(atomicState.reAttemptInterval.toInteger(), "pollChildren", [overwrite: true]) 
        result = false    
    } catch (Exception e) {
    // TODO: Handle "org.apache.http.conn.ConnectTimeoutException" as this is a transient error and shouldn't count against our retries
		LOG("${preText}General Exception: ${e}.", 1, null, "error")
        atomicState.reAttemptPoll = atomicState.reAttemptPoll + 1
        if (atomicState.reAttemptPoll > 3) {        
        	apiLost("${preText}Too many retries (${atomicState.reAttemptPoll - 1}) for polling.")
            return false
        } else {
        	LOG(preText+'Setting up retryPolling')
			// def reAttemptPeriod = 15 // in sec
        	if ( canSchedule() ) {
            	runIn(atomicState.reAttemptInterval.toInteger(), "refreshAuthToken") 
			} else { 
            	LOG(preText+'Unable to schedule refreshAuthToken, running directly')
            	refreshAuthToken() 
            }
        }    	
    }
    LOG("<===== Leaving pollEcobeeAPI() results: ${result}", 5)
   
	return result
}

def updateSensorData() {
	LOG("Entered updateSensorData() ${atomicState.remoteSensors}", 5)
 	def sensorCollector = [:]
    def sNames = []
    Integer precision = getTempDecimals()
    def forcePoll = atomicState.forcePoll
    def thermostatUpdated = atomicState.thermostatUpdated
    String preText = getDebugLevel() <= 2 ? '' : 'updateSensorData() - '
    
    atomicState.thermostatData.thermostatList.each { singleStat ->
    	def tid = singleStat.identifier
        
        def isConnected = atomicState.runtime[tid]?.connected
    
		atomicState.remoteSensors[tid].each {
        	String sensorDNI = ''
        	switch (it.type) {
            	case 'ecobee3_remote_sensor':
                	// Ecobee3 remote temp/occupancy sensor
                	sensorDNI = "ecobee_sensor-" + it?.id + "-" + it?.code
                    break;
                case 'control_sensor':
                	// SmartSI style control sensor (temp or humidity)
                	sensorDNI = "control_sensor-" + it?.id
                    break;
                case 'thermostat':
                	// The thermostat itself
                	sensorDNI = "ecobee_sensor_thermostat-"+ it?.id + "-" + it?.name
                    break;
     		} 
			LOG("updateSensorData() - Sensor ${it.name}, sensorDNI == ${sensorDNI}", 4)
            	              
            if (sensorDNI && settings.ecobeesensors?.contains(sensorDNI)) {		// only process the selected/configured sensors
				def temperature = ''
				def occupancy = ''
                Integer humidity = null
                            
				it.capability.each { cap ->
					if (cap.type == "temperature") {
                   		LOG("updateSensorData() - Sensor (DNI: ${sensorDNI}) temp is ${cap.value}", 4)
                       	if ( cap.value.isNumber() ) { // Handles the case when the sensor is offline, which would return "unknown"
							temperature = cap.value as Double
							temperature = (temperature / 10).toDouble()  //.round(precision) // wantMetric() ? (temperature / 10).toDouble().round(1) : (temperature / 10).toDouble().round(1)
                       	} else if (cap.value == 'unknown') {
                       		// TODO: Do something here to mark the sensor as offline?
                            if (isConnected) {
                            	// No need to log this unless thermostat is actually connected to the Ecobee Cloud
                           		LOG("${preText}Sensor ${it.name} temperature is 'unknown'. Perhaps it is unreachable?", 1, null, 'warn')
                            }
                           	// Need to mark as offline somehow
                           	temperature = "unknown"   
                       	} else {
                       		LOG("${preText}Sensor ${it.name} (DNI: ${sensorDNI}) returned ${cap.value}.", 1, null, "error")
                       	}
					} else if (cap.type == 'occupancy') {
						if(cap.value == 'true') {
							occupancy = 'active'
						} else if (cap.value == 'unknown') {
                       		// Need to mark as offline somehow
                           	LOG("${preText}Sensor ${it.name} occupancy is 'unknown'", 2, null, 'warn')
                           	occupancy = 'unknown'
                       	} else {
							occupancy = 'inactive'
						}                            
					} else if (cap.type == 'humidity') {		// only thermostats have humidity
                    	if (cap.value?.isNumber()) humidity = cap.value.toInteger()
                    }
				}
                                            				
				def sensorData = [:]
                def lastList = atomicState."${sensorDNI}" as List
                def listSize = 5
                if ( !lastList || (lastList.size() < listSize)) {
                    lastList = [999,'x',-1,'default','null']	// initilize the list 
                    sensorData = [ thermostatId: tid ]		// this will never change, but we need to send it at least once
                }
                    
                if (forcePoll) sensorData << [ thermostatId: tid /*, vents: 'default'*/ ] 		// belt, meet suspenders
                    
                // temperature usually changes every time, goes in *List[0]
                def currentTemp = ((temperature == 'unknown') ? 'unknown' : myConvertTemperatureIfNeeded(temperature, "F", precision+1))
				if (forcePoll || (lastList[0] != currentTemp)) { sensorData << [ temperature: currentTemp ] }
                def sensorList = [currentTemp]
                    
                // occupancy generally only changes every 30 minutes, in *List[1]
               	if (forcePoll || ((occupancy != "") && (lastList[1] != occupancy))) { sensorData << [ motion: occupancy ] }
                sensorList += [occupancy]
                    
                // precision can change at any time, *List[2]
                if (forcePoll || (lastList[2] != precision)) { sensorData << [ decimalPrecision: precision ] }
                sensorList += [precision]
                    
                // currentProgramName doesn't change all that often, but it does change
                def currentPrograms = atomicState.currentProgramName
                def currentProgramName = (currentPrograms && currentPrograms.containsKey(tid)) ? currentPrograms[tid] : 'default' // not set yet, we will have to get it later. 
                if (forcePoll || (lastList[3] != currentProgramName)) { sensorData << [ currentProgramName: currentProgramName ] }
                sensorList += [currentProgramName]
                
                // not every sensor has humidity
               	if (forcePoll || ((humidity != null) && (lastList[4] != humidity))) { sensorData << [ humidity: humidity ] }
                sensorList += [humidity]
                
               	// collect the climates that this sensor is included in
                def sensorClimates = [:]
                def climatesList = []
               	if (forcePoll || thermostatUpdated) {	// these will only change when the program object changes
                	def statProgram = atomicState.program[tid]
                	if (statProgram?.climates) {
                		statProgram.climates.each { climate ->
                    		def sids = climate.sensors?.collect { sid -> sid.name}
                        	if (sids.contains(it.name)) {
                        		sensorClimates << ["${climate.name}":'on']
                                climatesList += ['on']
                        	} else {
                        		sensorClimates << ["${climate.name}":'off']
                                climatesList += ['off']
                        	}
                    	}
                	}
                	LOG("updateSensorData() - sensor ${it.name} is used in ${sensorClimates}", 4, null, 'trace')
                } 
                
                // check to see if the climates have changed. Notice that there can be 0 climates if the thermostat data wasn't updated this time;
                // note also that we will send the climate data every time the thermostat IS updated, even if they didn't change, becuase we don't 
                // store them in the atomicState unless the thermostat object changes...
                if (forcePoll || (lastList.size() < (listSize+climatesList.size())) || (lastList.drop(listSize) != climatesList)) {	
                    sensorData << sensorClimates
                    sensorList += climatesList
                }
                   
                if (forcePoll || (sensorData != [:])) {
					sensorCollector[sensorDNI] = [name:it.name,data:sensorData]
                    atomicState."${sensorDNI}" = sensorList
                    LOG("updateSensorData() - sensorCollector being updated with sensorData: ${sensorData}", 4)
                    sNames += it.name
                }
            } // End sensor is valid and selected in settings
		} // End [tid] sensors loop
	} // End thermostats loop
    
	atomicState.remoteSensorsData = sensorCollector
    if (preText=='') {
    	def sz = sNames.size()
        LOG("Device data updated for ${sz} sensor${sz!=1?'s':''}${sNames?' '+sNames:''}",2, null, 'info')
    } else {
		LOG("updateSensorData() - Updated these remoteSensors: ${sensorCollector}", 4, null, 'trace') 
    }
}

def updateThermostatData() {
	// atomicState.timeOfDay = getTimeOfDay() // shouldn't need to do this - sunrise/sunset events are maintaining it
	def runtimeUpdated = atomicState.runtimeUpdated
    def thermostatUpdated = atomicState.thermostatUpdated
	boolean usingMetric = wantMetric() // cache the value to save the function calls
	def forcePoll = atomicState.forcePoll
    Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
    Integer userPrecision = getTempDecimals()						// user's requested display precision
    String preText = getDebugLevel() <= 2 ? '' : 'updateThermostatData() - '
    def tstatNames = []
    def thermostatsWithNames = atomicState.thermostatsWithNames
    
    def needExtendedRuntime = false

	atomicState.thermostats = atomicState.thermostatData.thermostatList.inject([:]) { collector, stat ->
		// we use atomicState.thermostatData because it holds the latest Ecobee API response, from which we can determine which stats actually
        // had updated data. Thus the following work is done ONLY for tstats that have updated data
		def tid = stat.identifier

		LOG("updateThermostatData() - Updating event data for thermostat ${tstatName} (${tid})", 3, null, 'info')

	// grab a local copy from the atomic storage all at once (avoid repetive reads from backing store)
    	def es = atomicState.equipmentStatus
		def equipStatus = (es && es[tid]) ? es[tid] : ''
        if (equipStatus == '') equipStatus = 'idle'
        def changeEquip = atomicState.changeEquip ? atomicState.changeEquip : [:]
        def lastEquipStatus = (changeEquip && changeEquip[tid]) ? changeEquip[tid] : 'unknown'
        def statSettings = atomicState.settings ? atomicState.settings[tid] : [:]
        def program = atomicState.program ? atomicState.program[tid] : [:]
        def events = atomicState.events ? atomicState.events[tid] : [:]
        def runtime = atomicState.runtime ? atomicState.runtime[tid] : [:]
        def extendedRuntime = atomicState.extendedRuntime ? atomicState.extendedRuntime[tid] : [:]
		// not worth it - weather is only accessed twice, and it is a LOT of data - we will access it directly from the atomicState cache
        // def weather = atomicState.weather ? atomicState.weather[tid] : [:]    
        
    // Handle things that only change when runtime object is updated)
        def occupancy = 'not supported'
        Double tempTemperature
        Double tempHeatingSetpoint
        Double tempCoolingSetpoint
        def tempWeatherTemperature
        
        if (forcePoll || runtimeUpdated) {
			// Occupancy (motion)
        	// TODO: Put a wrapper here based on the thermostat brand
        	def thermSensor = ''
            if (atomicState.remoteSensors) { thermSensor = atomicState.remoteSensors[tid].find { it.type == 'thermostat' } }
            
        	def hasInternalSensors
        	if(!thermSensor) {
				LOG('updateThermostatData() - This thermostat does not have a built in remote sensor', 4, null, 'info')
				hasInternalSensors = false
        	} else {
        		hasInternalSensors = true
				LOG("updateThermostatData() - thermSensor == ${thermSensor}", 4 )
        
				def occupancyCap = thermSensor?.capability.find { it.type == 'occupancy' }
                if (occupancyCap) {
					LOG("updateThermostatData() - occupancyCap = ${occupancyCap} value = ${occupancyCap.value}", 4, null, 'info')
        
					// Check to see if there is even a value, not all types have a sensor
                    occupancy = occupancyCap.value ? occupancyCap.value : 'not supported'
                }
        	}
            // 'not supported' is reported as 'inactive'
			if (hasInternalSensors) { occupancy = (occupancy == 'true') ? 'active' : ((occupancy == 'unknown') ? 'unknown' : 'inactive') }            
            
			// Temperatures
            // NOTE: The thermostat always present Fahrenheit temps with 1 digit of decimal precision. We want to maintain that precision for inside temperature so that apps like vents and Smart Circulation
            // 		 can operate efficiently. So, while the user can specify a display precision of 0, 1 or 2 decimal digits, we ALWAYS keep and send max decimal digits and let the device handler adjust for display
            //		 For Fahrenheit, we keep the 1 decimal digit the API provides, for Celsius we allow for 2 decimal digits as a result of the mathematical calculation
            
			tempTemperature = myConvertTemperatureIfNeeded( (runtime.actualTemperature.toDouble() / 10.0), "F", apiPrecision)
            Double tempHeatAt = runtime.desiredHeat.toDouble()
            Double tempCoolAt = runtime.desiredCool.toDouble()
            if ((equipStatus == 'idle') || (equipStatus == 'fan')) {	// Show trigger point if idle; tile shows "Heating at 69.5" vs. "Heating to 70.0"
            	tempHeatAt = tempHeatAt - statSettings.stage1HeatingDifferentialTemp.toDouble()
                tempCoolAt = tempCoolAt + statSettings.stage1CoolingDifferentialTemp.toDouble()
            }
        	tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt / 10.0), 'F', apiPrecision)
        	tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt / 10.0), 'F', apiPrecision)
        	if (atomicState.weather && (atomicState.weather.size() > 0)) {
            	tempWeatherTemperature = myConvertTemperatureIfNeeded( ((atomicState.weather[tid].forecasts[0].temperature.toDouble() / 10.0)), "F", apiPrecision)
        	}
        }
        
	// handle[tid] things that only change when the thermostat object is updated
		def heatHigh
		def heatLow
		def coolHigh
		def coolLow
		def heatRange
		def coolRange
		
		def hasHeatPump
		def hasForcedAir
		def hasElectric
		def hasBoiler
		def auxHeatMode
		Double tempHeatDiff = 0.0
        Double tempCoolDiff = 0.0

		if (forcePoll || thermostatUpdated) {
            tempHeatDiff = statSettings.stage1HeatingDifferentialTemp.toDouble() / 10.0
            tempCoolDiff = statSettings.stage1CoolingDifferentialTemp.toDouble() / 10.0
            
			// RANGES
			// UI works better with the same ranges for both heat and cool...
			// but the device handler isn't using these values for the UI right now (can't dynamically define the range)			
			heatHigh = myConvertTemperatureIfNeeded((statSettings.heatRangeHigh.toDouble() / 10.0), 'F', userPrecision)
			heatLow =  myConvertTemperatureIfNeeded((statSettings.heatRangeLow.toDouble()  / 10.0), 'F', userPrecision)
			coolHigh = myConvertTemperatureIfNeeded((statSettings.coolRangeHigh.toDouble() / 10.0), 'F', userPrecision)
			coolLow =  myConvertTemperatureIfNeeded((statSettings.coolRangeLow.toDouble()  / 10.0), 'F', userPrecision)
			
			// calculate these anyway (for now) - it's easier to read the range while debugging
			heatRange = (heatLow && heatHigh) ? "(${Math.round(heatLow)}..${Math.round(heatHigh)})" : (usingMetric ? '(5..35)' : '(45..95)')
			coolRange = (coolLow && coolHigh) ? "(${Math.round(coolLow)}..${Math.round(coolHigh)})" : (usingMetric ? '(5..35)' : '(45..95)')
			
			// EQUIPMENT SPECIFICS
			hasHeatPump =  statSettings.hasHeatPump
			hasForcedAir = statSettings.hasForcedAir
			hasElectric =  statSettings.hasElectric
			hasBoiler =    statSettings.hasBoiler
			auxHeatMode =  (hasHeatPump) && (hasForcedAir || hasElectric || hasBoiler) // auxHeat = emergencyHeat if using a heatPump
		}
 
	// handle things that depend on both thermostat and runtime objects
		// EVENTS
		// Determine if an Event is running, find the first running event (only changes when thermostat object is updated)
    	def runningEvent = [:]
        def currentClimateName = ''
		def currentClimateId = ''
        def currentClimate = ''
        def currentFanMode = ''
        def climatesList = []
        def statMode = statSettings.hvacMode
        def fanMinOnTime = statSettings.fanMinOnTime
		
		// what program is supposed to be running now?
        def scheduledClimateId = 'unknown'
		def scheduledClimateName = 'Unknown'
        def schedClimateRef = ''
        if (program) {
        	scheduledClimateId = program.currentClimateRef 
        	schedClimateRef = program.climates.find { it.climateRef == scheduledClimateId }
            scheduledClimateName = schedClimateRef.name
            program.climates?.each { climatesList += '"'+it.name+'"' } // read with: programsList = new JsonSlurper().parseText(theThermostat.currentValue('programsList'))
        }
		LOG( "scheduledClimateId: ${scheduledClimateId}, scheduledClimateName: ${scheduledClimateName}, climatesList: ${climatesList.toString()}", 4, null, 'info')
        
		// check which program is actually running now
		if (events?.size()) {         
        	runningEvent = events.find { it.running == true }        	
		}
        
        // store the currently running event (in case we need to modify or delete it later, as in Vacation handling)
        def tempRunningEvent = [:]
       	tempRunningEvent[tid] = runningEvent ? runningEvent : [:]
        if (atomicState.runningEvent) tempRunningEvent = atomicState.runningEvent + tempRunningEvent
        atomicState.runningEvent = tempRunningEvent
            
		def thermostatHold = ''
        String holdEndsAt = ''
        
        def isConnected = runtime?.connected
        if (!isConnected) LOG("isConnected: ${isConnected}",2,null,'warn')
        if (!isConnected) {
        	// Ecobee Cloud lost connection with the thermostat - power or network outage
            currentClimateName = 'Offline'
            currentClimate = currentClimateName
            currentClimateId = 'offline'
            occupancy = 'unknown'
            
            holdEndsAt = fixDateTimeString( runtime.lastModified.take(10), runtime.lastModified.drop(11), stat.thermostatTime )
        }
        
        if (runningEvent && isConnected) {
            holdEndsAt = fixDateTimeString( runningEvent.endDate, runningEvent.endTime, stat.thermostatTime)
			thermostatHold = runningEvent.type
            LOG("Found a running Event: ${runningEvent}", 4) 
            def tempClimateRef = runningEvent.holdClimateRef ? runningEvent.holdClimateRef : ''
        	if ( runningEvent.type == 'hold' ) {
            	if (tempClimateRef != '') {
					currentClimate = (program.climates.find { it.climateRef == tempClimateRef }).name
               		currentClimateName = 'Hold: ' + currentClimate
                } else if (runningEvent.name == 'auto') {		// Handle the "auto" climates (includes fan on override, and maybe the Smart Recovery?)
                    if ((statMode == "heat") && (runningEvent.fan != schedClimateRef.heatFan)) {
                       	currentClimateName = 'Hold: Fan'
                    } else if ((statMode == 'cool') && (runningEvent.fan != schedClimateRef.coolFan)) {
                    	currentClimateName = 'Hold: Fan'
                    } else {
                        currentClimateName = 'Auto'
                    }
                } else if (runningEvent.name == 'hold') {		// just a temperature hold, probably from the keypad
                	currentClimateName = 'Hold: ' + (statMode == 'heat' ? tempHeatingSetpoint : (statMode == 'cool' ? tempCoolingSetpoint : '??')) + '°'
                }// if we can't tell which hold is in effect, leave currentClimate, currentClimateName and currentClimateId blank/null/empty
			} else if (runningEvent.type == 'vacation' ) {
               	currentClimateName = 'Vacation'
                fanMinOnTime = runningEvent.fanMinOnTime
            } else if (runningEvent.type == 'quickSave' ) {
               	currentClimateName = 'Quick Save'                
            } else if (runningEvent.type == 'autoAway' ) {
             	currentClimateName = 'Auto Away'
            } else if (runningEvent.type == 'autoHome' ) {
               	currentClimateName = 'Auto Home'
            } else {                
               	currentClimateName = runningEvent.type
            }
            // currentClimateId = runningEvent.holdClimateRef
			currentClimateId = tempClimateRef
		} else if (isConnected) {
			if (scheduledClimateId) {
        		currentClimateId = scheduledClimateId
        		currentClimateName = scheduledClimateName
				currentClimate = scheduledClimateName
			} else {
        		LOG(preText+'Program or running Event missing', 1, null, 'warn')
            	currentClimateName = ''
        		currentClimateId = '' 
                currentClimate = ''
        	}
		}
        LOG("updateThermostatData() - currentClimateName set = ${currentClimateName}  currentClimateId set = ${currentClimateId}", 4, null, 'info')
		
        if (runningEvent) {
        	currentFanMode = atomicState.circulateFanModeOn ? "circulate" : atomicState.offFanModeOn ? "off" : runningEvent.fan
        } else {
        	currentFanMode = runtime.desiredFanMode
		}
		
		// HUMIDITY
		def humiditySetpoint = 0
        def humidity = runtime.desiredHumidity
        def dehumidity = runtime.desiredDehumidity
        def hasHumidifier = (statSettings?.hasHumidifier && (statSettings?.humidifierMode != 'off'))
        def hasDehumidifier = ((statSettings?.dehumidifierMode == 'on') && (statSettings?.hasDehumidifier || 
        						(statSettings?.dehumidifyWithAC && (statSettings?.dehumidifyOvercoolOffset != 0)))) // fortunately, we can hide the details from the device handler
        if (hasHumidifier && (extendedRuntime && extendedRuntime.desiredHumidity && extendedRuntime.desiredHumidity[2])) {
        	humidity = extendedRuntime.desiredHumidity[2]		// if supplied, extendedRuntime gives the actual target (Frost Control)
        }
        if (hasDehumidifier && (extendedRuntime && extendedRuntime.desiredDehumidity && extendedRuntime.desiredDehumidity[2])) {
        	dehumidity = extendedRuntime.desiredDehumidity[2]	
		}
		switch (statMode) {
			case 'heat':
				if (hasHumidifier) humiditySetpoint = humidity
				break;
			case 'cool':
            	if (hasDehumidifier) humiditySetpoint = dehumidity
				break;
			case 'auto':
				if (hasHumidifier && hasDehumidifier) { humiditySetpoint = "${humidity}-${dehumidity}" }
                else if (hasHumidifier) { humiditySetpoint = humidity }
                else if (hasDehumidifier) { humiditySetpoint = dehumidity }
				break;
		}
        
		// EQUIPMENT STATUS
		def heatStages = statSettings.heatStages
		def coolStages = statSettings.coolStages 
		String equipOpStat = 'idle'	// assume we're idle - this gets fixed below if different
        String thermOpStat = 'idle'
        Boolean isHeating = false
        Boolean isCooling = false
        Boolean smartRecovery = false
        
        // Let's deduce if we are in Smart Recovery mode
        if (equipStatus.contains('ea')) {
        	isHeating = true
            if ((statSettings?.disablePreHeating == false) && (tempTemperature > (tempHeatingSetpoint + tempHeatDiff))) {
            	smartRecovery = true
            	equipStatus = equipStatus + ',smartRecovery'
            }
        } else if (equipStatus.contains('oo')) {
        	isCooling = true
			if ((statSettings?.disablePreCooling == false) && (tempTemperature < (tempCoolingSetpoint - tempCoolDiff))) {
            	smartRecovery = true
            	equipStatus = equipStatus + ',smartRecovery'
            }
        }
            
        if (forcePoll || (equipStatus != lastEquipStatus)) {
			if (equipStatus == 'fan') {
				equipOpStat = 'fan only'
            	thermOpStat = equipOpStat
                
			} else if (isHeating) {					// heating
            	thermOpStat = 'heating'
                equipOpStat = 'heating'
            	if (smartRecovery) thermOpStat = 'heating (smart recovery)'
				// Figure out which stage we are running now
                if 		(equipStatus.contains('t1')) 	{ equipOpStat = (auxHeatMode) ? 'emergency' : (heatStages > 1) ? 'heat 1' : 'heating' }
				else if (equipStatus.contains('t2')) 	{ equipOpStat = 'heat 2' }
				else if (equipStatus.contains('t3')) 	{ equipOpStat = 'heat 3' }
				else if (equipStatus.contains('p2')) 	{ equipOpStat = 'heat pump 2' }
				else if (equipStatus.contains('p3')) 	{ equipOpStat = 'heat pump 3' }
				else if (equipStatus.contains('mp')) 	{ equipOpStat = 'heat pump' }
                // We now have icons that depict when we are humidifying or dehumidifying...
				if (equipStatus.contains('hu')) 		{ equipOpStat += ' hum' }	// humidifying if heat
                
			} else if (isCooling) {				// cooling
        		thermOpStat = 'cooling'
                equipOpStat = 'cooling'
                if (smartRecovery) thermOpStat = 'cooling (smart recovery)'
				if 		(equipStatus.contains('l1')) { equipOpStat = (coolStages == 1) ? 'cooling' : 'cool 1' }
				else if (equipStatus.contains('l2')) { equipOpStat = 'cool 2' }
                
				if (equipStatus.contains('de')) { equipOpStat += ' deh' }	// dehumidifying if cool
                
			} else if (equipStatus.contains('de')) { // These can also run independent of heat/cool
        		equipOpStat = 'dehumidifier' 
                
        	} else if (equipStatus.contains('hu')) { 
        		equipOpStat = 'humidifier' 
        	} // also: economizer, ventilator, compHotWater, auxHotWater
		}
        
		// Update the API link state and the lastPoll data. If we aren't running at a high debugLevel >= 4, then supply simple
		// poll status instead of the date/time (this simplifies the UI presentation, and reduces the chatter in the devices'
		// message log
        def apiConnection = apiConnected()
        def lastPoll = (debugLevel(4)) ? atomicState.lastPollDate : (apiConnection=='full') ? 'Succeeded' : (apiConnection=='warn') ? 'Incomplete' : 'Failed'
        
        def statusMsg
        if (isConnected) {
			statusMsg = (holdEndsAt == '') ? '' : ((thermostatHold=='hold') ? 'Hold' : (thermostatHold=='vacation') ? 'Vacation' : 'Event')+" ends ${holdEndsAt}"
		} else {
        	statusMsg = "Last updated ${holdEndsAt}"
            equipOpStat = 'offline'					// override if Ecobee Cloud has lost connection with the thermostat
            thermOpStat = 'offline'
        }
        
        // Okey dokey - time to queue all this data into the atomicState.thermostats queue
        def changeCloud =  atomicState.changeCloud  ? atomicState.changeCloud  : [:]
		def changeConfig = atomicState.changeConfig ? atomicState.changeConfig : [:]
        def changeNever =  atomicState.changeNever  ? atomicState.changeNever  : [:]
        def changeRarely = atomicState.changeRarely ? atomicState.changeRarely : [:]
        def changeOften =  atomicState.changeOften  ? atomicState.changeOften  : [:]
        //changeEquip was initialized earlier
        
        def data = [:]
        
        // Runtime stuff that changes most frequently - we test them 1 at a time, and send only the ones that change
        // Send these first, as they generally are the reason anything else changes (so the thermostat's notification log makes sense)
		if (forcePoll || runtimeUpdated) {
            def oftenList = [tempTemperature,occupancy,runtime.actualHumidity,tempHeatingSetpoint,tempCoolingSetpoint,humiditySetpoint,userPrecision]
            def lastOList = []
            lastOList = changeOften[tid]
            if (forcePoll || !lastOList) lastOList = [999,"x",-1,-1,-1,-1,-1] 
            if (lastOList[0] != tempTemperature) data += [temperature: String.format("%.${apiPrecision}f", tempTemperature.toDouble().round(apiPrecision)),]
            if (lastOList[1] != occupancy) data += [motion: occupancy,]
            if (lastOList[2] != runtime.actualHumidity) data += [humidity: runtime.actualHumidity,]
            // send these next two also when the userPrecision changes
            if ((lastOList[3] != tempHeatingSetpoint) || (lastOList[6] != userPrecision)) data += [heatingSetpoint: String.format("%.${userPrecision}f", tempHeatingSetpoint.toDouble().round(userPrecision)),]
            if ((lastOList[4] != tempCoolingSetpoint) || (lastOList[6] != userPrecision)) data += [coolingSetpoint: String.format("%.${userPrecision}f", tempCoolingSetpoint.toDouble().round(userPrecision)),]
            if (lastOList[5] != humiditySetpoint) data += [humiditySetpoint: humiditySetpoint,]
           	changeOften[tid] = oftenList
            atomicState.changeOften = changeOften
            
            // Weather only changes every 15 minutes, so NBD - send it every time we get it, even if it hasn't changed
        	if (atomicState.getWeather) {				
        		def wSymbol = ''
            	def wTemp = ''
        		if (tempWeatherTemperature.isNumber()) {
            		wSymbol = atomicState.weather[tid].forecasts[0].weatherSymbol.toString()
                	wTemp = String.format("%.${userPrecision}f", tempWeatherTemperature.toDouble().round(userPrecision))
            	}
                data += [
                	weatherSymbol: wSymbol,
					weatherTemperature: wTemp,
                ]
            }
		}
        
        // API link to Ecobee's Cloud status - doesn't change unless things get broken
        def cloudList = [lastPoll,apiConnection,isConnected]
        if (forcePoll || (changeCloud == [:]) || !changeCloud.containsKey(tid) || (changeCloud[tid] != cloudList)) { 
            data += [
        		lastPoll: lastPoll,
            	apiConnected: apiConnection,
        	]
            changeCloud[tid] = cloudList
            atomicState.changeCloud = changeCloud
        }
        
        // SmartApp configuration settings that almost never change (Listed in order of frequency that they should change normally)
        Integer dbgLevel = getDebugLevel()
        String tmpScale = getTemperatureScale()
		def configList = [timeOfDay,userPrecision,dbgLevel,tmpScale] 
        if (forcePoll || (changeConfig == [:]) || !changeConfig.containsKey(tid) || (changeConfig[tid] != configList)) { 
            data += [
        		timeOfDay: timeOfDay,
           		decimalPrecision: userPrecision,
				temperatureScale: tmpScale,			
				debugLevel: dbgLevel,
        	]
            changeConfig[tid] = configList
            atomicState.changeConfig = changeConfig
        }
        
        // Equipment operating status - I *think* this can change with either thermostat or runtime changes
        if (forcePoll || (lastEquipStatus != equipStatus)) { 
            data += [
        		equipmentStatus: 		  equipStatus,
            	thermostatOperatingState: thermOpStat,
            	equipmentOperatingState:  equipOpStat,
        	]
            changeEquip[tid] = equipStatus
            atomicState.changeEquip = changeEquip
        }
        
		// Thermostat configuration settinngs
        if (forcePoll || thermostatUpdated) {	// new settings, programs or events
        	def autoMode = statSettings?.autoHeatCoolFeatureEnabled
        	
            // Thermostat configuration stuff that almost never changes - if any one changes, send them all
        	def neverList = [statMode,autoMode,coolStages,heatStages,heatHigh,heatLow,coolHigh,coolLow,heatRange,coolRange,climatesList,
        						hasHeatPump,hasForcedAir,hasElectric,hasBoiler,auxHeatMode,hasHumidifier,hasDehumidifier,tempHeatDiff,tempCoolDiff] 
 			if (forcePoll || (changeNever == [:]) || !changeNever.containsKey(tid) || (changeNever[tid] != neverList)) { 
                def heatDiff = String.format("%.${apiPrecision}f", tempHeatDiff.toDouble().round(apiPrecision))
            	def coolDiff = String.format("%.${apiPrecision}f", tempCoolDiff.toDouble().round(apiPrecision))
            	data += [
					coolMode: (coolStages > 0),
            		coolStages: coolStages,
					heatMode: (heatStages > 0),
            		heatStages: heatStages,
					autoMode: autoMode,
                	thermostatMode: statMode,
            		heatRangeHigh: heatHigh,
            		heatRangeLow: heatLow,
            		coolRangeHigh: coolHigh,
            		coolRangeLow: coolLow,
					heatRange: heatRange,
					coolRange: coolRange,  
                	programsList: climatesList,
                
                	hasHeatPump: hasHeatPump,
            		hasForcedAir: hasForcedAir,
            		hasElectric: hasElectric,
            		hasBoiler: hasBoiler,
					auxHeatMode: auxHeatMode,
            		hasHumidifier: hasHumidifier,
					hasDehumidifier: hasDehumidifier,
                	heatDifferential: heatDiff,
                	coolDifferential: coolDiff,
            	]
            	changeNever[tid] = neverList
            	atomicState.changeNever = changeNever
        	}
            
            // Thermostat operational things that rarely change, (a few times a day at most)
         	def rarelyList = [fanMinOnTime,isConnected,thermostatHold,holdEndsAt,statusMsg,currentClimateName,currentClimateId,scheduledClimateName,
            					scheduledClimateId,currentFanMode]
		    if (forcePoll || (changeRarely == [:]) || !changeRarely.containsKey(tid) || (changeRarely[tid] != rarelyList)) { 
            	data += [
          			thermostatHold: thermostatHold,
                	holdEndsAt: holdEndsAt,
               		holdStatus: statusMsg,
 					currentProgramName: currentClimateName,
					currentProgramId: currentClimateId,
					currentProgram: currentClimate,
					scheduledProgramName: scheduledClimateName,
					scheduledProgramId: scheduledClimateId,
					scheduledProgram: scheduledClimateName,
                    thermostatFanMode: currentFanMode,
                	fanMinOnTime: fanMinOnTime,
          		]
            	changeRarely[tid] = rarelyList
            	atomicState.changeRarely = changeRarely
                
                // Save the Program when it changes so that we can get to it easily for the child sensors
        		def tempProgram = [:]
                tempProgram[tid] = currentClimateName
        		if (atomicState.currentProgramName) tempProgram = atomicState.currentProgramName + tempProgram
        		atomicState.currentProgramName = tempProgram
        	}            
    	}
        

        LOG("updateThermostatData() - Event data updated for thermostat ${tstatName} (${tid}) = ${data}", 4, null, 'trace')

		// it is possible that thermostatSummary indicated things have changed that we don't care about...
		if (data != [:]) {
        	def DNI = [ app.id, stat.identifier ].join('.')
        	def tstatName = (thermostatsWithNames && thermostatsWithNames.containsKey(DNI)) ? thermostatsWithNames[DNI] : 'noName'
        	tstatNames += [tstatName]
			collector[DNI] = [thermostatId:tid, data:data]
        }
		return collector
	}
    Integer nSize = tstatNames.size()
    LOG("${preText}Device data updated for ${nSize} thermostat${nSize!=1?'s':''} ${nSize!=0?tstatNames:''}", 2, null, 'info')
}

def getChildThermostatDeviceIdsString(singleStat = null) {
	if(!singleStat) {
    	LOG('getChildThermostatDeviceIdsString() - !singleStat returning the list for all thermostats', 4, null, 'info')
		return thermostats.collect { it.split(/\./).last() }.join(',')
	} else {
    	// Only return the single thermostat
        LOG('Only have a single stat.', 4, null, 'debug')
        def ecobeeDevId = singleStat.device.deviceNetworkId.split(/\./).last()
        LOG("Received a single thermostat, returning the Ecobee Device ID as a String: ${ecobeeDevId}", 4, null, 'info')
        return ecobeeDevId as String  	
    }
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

private refreshAuthToken(child=null) {
	LOG('Entered refreshAuthToken()', 5)	

	def timeBeforeExpiry = atomicState.authTokenExpires ? atomicState.authTokenExpires - now() : 0
    // check to see if token was recently refreshed (eliminate multiple concurrent threads)
	if (timeBeforeExpiry > 2000) {
    	LOG("refreshAuthToken() - skipping, token expires in ${timeBeforeExpiry/1000} seconds",3,null,'info')
    	return true
    }
    
	atomicState.lastTokenRefresh = now()
	atomicState.lastTokenRefreshDate = getTimestamp()    
    
	if (!atomicState.refreshToken) {    	
		LOG('refreshAuthToken() - There is no refreshToken stored! Unable to refresh OAuth token', 1, child, "error")
    	apiLost("refreshAuthToken() - No refreshToken")
        return false
    } else {
		LOG('Performing a refreshAuthToken()', 4)
        
        def refreshParams = [
                method: 'POST',
                uri   : apiEndpoint,
                path  : '/token',
                query : [grant_type: 'refresh_token', code: "${atomicState.refreshToken}", client_id: smartThingsClientId],
        ]

        LOG("refreshParams = ${refreshParams}", 4)

		def jsonMap
        try {            
            httpPost(refreshParams) { resp ->
				LOG("Inside httpPost resp handling.", 4, child, "trace")
                if(resp.status == 200) {
                    LOG('refreshAuthToken() - 200 Response received - Extracting info.', 4, child, 'trace' )
                    atomicState.reAttempt = 0 
                    apiRestored()                    
                    generateEventLocalParams() // Update the connected state at the thermostat devices
                    
                    jsonMap = resp.data // Needed to work around strange bug that wasn't updating state when accessing resp.data directly
                    LOG("resp.data = ${resp.data} -- jsonMap is? ${jsonMap}", 4, child)

                    if(jsonMap) {
                        LOG("resp.data == ${resp.data}, jsonMap == ${jsonMap}", 4, child)
						
                        atomicState.refreshToken = jsonMap.refresh_token
                        
                        // TODO - Platform BUG: This was not updating the state values for some reason if we use resp.data directly??? 
                        // 		  Workaround using jsonMap for authToken                       
                        LOG("atomicState.authToken before: ${atomicState.authToken}", 4, child, "trace")
                        def oldAuthToken = atomicState.authToken
                        atomicState.authToken = jsonMap?.access_token  
						LOG("atomicState.authToken after: ${atomicState.authToken}", 4, child, "trace")
                        if (oldAuthToken == atomicState.authToken) { 
                        	LOG('WARN: atomicState.authToken did NOT update properly! This is likely a transient problem', 1, child, 'warn')
						}
                        
                        // Save the expiry time for debugging purposes
                        atomicState.authTokenExpires = (resp?.data?.expires_in * 1000) + now()
                        LOG("refreshAuthToken() - Success! Token expires in ${String.format("%.2f",resp?.data?.expires_in/60)} minutes", 3, child, 'info')
                        if (debugLevel(4)) {
                        	LOG("Updated state.authTokenExpires = ${atomicState.authTokenExpires}", 4, child, 'trace')
                            LOG("Refresh Token = state =${atomicState.refreshToken} == in: ${resp?.data?.refresh_token}", 4, child, 'trace')
                        	LOG("OAUTH Token = state ${atomicState.authToken} == in: ${resp?.data?.access_token}", 4, child, 'trace')
                        }
                        
                        def action = atomicState.action
                        // Reset saved action
                        atomicState.action = ''
                        if (action) { // && atomicState.action != "") {
                            LOG("Token refreshed. Rescheduling aborted action: ${action}", 4, child, 'trace')
                            runIn( 5, "${action}", [overwrite: true]) // this will collapse multiple threads back into just one
                        }
                    } else {
                    	LOG("No jsonMap??? ${jsonMap}", 2, child, 'trace')
                    }               
                    return true
                } else {
                    LOG("refreshAuthToken() - Failed ${resp.status} : ${resp.status.code}!", 1, child, 'error')
                    return false
                }
            }
        } catch (groovyx.net.http.HttpResponseException e) {
        	def result = false
        	//LOG("refreshAuthToken() - HttpResponseException occurred. Exception info: ${e} StatusCode: ${e.statusCode}  response? data: ${e.getResponse()?.getData()}", 1, null, "error")
            LOG("refreshAuthToken() - HttpResponseException occurred. Exception info: ${e} StatusCode: ${e.statusCode}", 1, child, "error")
            if (e.statusCode != 401) {
            	runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true])
            } else if (e.statusCode == 401) {            
				atomicState.reAttempt = atomicState.reAttempt + 1
		        if (atomicState.reAttempt > 3) {                       	
    		    	apiLost("Too many retries (${atomicState.reAttempt - 1}) for token refresh.")        	    
            	    result = false
		        } else {
    		    	LOG("Setting up runIn for refreshAuthToken", 4, child)
        			if ( canSchedule() ) {            			
                        runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true]) 
					} else { 
    	        		LOG("Unable to schedule refreshAuthToken, running directly", 4, child)						
	        	    	result = refreshAuthToken(child) 
    	        	}
        		}
            }
            generateEventLocalParams() // Update the connected state at the thermostat devices
            return result
		} catch (java.util.concurrent.TimeoutException e) {
			LOG("refreshAuthToken(), TimeoutException: ${e}.", 1, child, "error")
			// Likely bad luck and network overload, move on and let it try again
            if(canSchedule()) { runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true]) } else { refreshAuthToken() }            
            return false
        } catch (Exception e) {
        	LOG("refreshAuthToken(), General Exception: ${e}.", 1, child, "error")            
            /*
            atomicState.reAttempt = atomicState.reAttempt + 1
	        if (atomicState.reAttempt > 3) {                       	
   		    	apiLost("Too many retries (${atomicState.reAttempt - 1}) for token refresh.")        	    
           	    return false
	        } else {
       			if ( canSchedule() ) {
           			// atomicState.connected = "warn"
					runIn(atomicState.reAttemptInterval, "refreshAuthToken") 
				} else { 
   	        		LOG("Unable to schedule refreshAuthToken, running directly", 2, child, "warn")
					// atomicState.connected = "warn"
        	    	refreshAuthToken(child) 
   	        	}
       		} */
            return false
        }
    }
}

def resumeProgram(child, String deviceId, resumeAll=true) {
	LOG("Entered resumeProgram for deviceId: ${deviceId} with child ${child}", 2, child, 'trace')
	def result = true
	String allStr = resumeAll ? 'true' : 'false'
    
    def previousFanMinOnTime = atomicState."previousFanMinOnTime${deviceId}"
    def currentFanMinOnTime = getFanMinOnTime(child)
    def previousHVACMode = atomicState."previousHVACMode${deviceId}"
    def currentHVACMode = getHVACMode(child)
    // def currentHoldType = child.currentValue("thermostatHold")		// TODO: If we are in a vacation hold, need to delete the vacation
    // theoretically, if currentHoldType == "", we can skip all of this...theoretically
    
    LOG("resumeProgram() - atomicState.previousHVACMode = ${previousHVACMode} current (${currentHVACMode})   atomicState.previousFanMinOnTime = ${previousFanMinOnTime} current (${currentFanMinOnTime})", 3, child, 'info')	
    if ((previousHVACMode != null) && (currentHVACMode != previousHVACMode)) {
    	// Need to reset the HVAC Mode back to the previous state
        if (currentHVACMode == "off") { atomicState.offFanModeOn = false }
        if (currentHVACMode == "circulate") { atomicState.circulateFanModeOn = false }
        
        LOG("getHVACMode(child) != atomicState.previousHVACMode${deviceId} (${previousHVACMode})", 5, child, "trace")
        result = setHVACMode(child, deviceId, previousHVACMode)       
    }
    
    if ((previousFanMinOnTime != null) && (currentFanMinOnTime != previousFanMinOnTime)) {
    	// Need to reset the fanMinOnTime back to the previous settings              
        
        LOG("getFanMinOnTime(child) != atomicState.previousFanMinOnTime${deviceId} (${previousFanMinOnTime})", 5, child, "trace")
        def fanResult = setFanMinOnTime(child, deviceId, previousFanMinOnTime)
        result = result && fanResult      
    }
        
    // 					   {"functions":[{"type":"resumeProgram"}],"selection":{"selectionType":"thermostats","selectionMatch":"YYY"}}
    def jsonRequestBody = '{"functions":[{"type":"resumeProgram"}],"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '","resumeAll":"' + allStr + '"}}'
	LOG("jsonRequestBody = ${jsonRequestBody}", 4, child)
    
	result = sendJson(jsonRequestBody) && result
    LOG("resumeProgram(${resumeAll}) returned ${result}", 3, child,'info')
    return result
}

def setHVACMode(child, deviceId, mode) {
	LOG("setHVACMode(${mode})", 4, child)
    def thermostatSettings = ',"thermostat":{"settings":{"hvacMode":"'+mode+'"}}'
    def thermostatFunctions = ''
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
    def result = sendJson(child, jsonRequestBody)
    LOG("setHVACMode(${mode}) returned ${result}", 3, child,'info')    

    return result
}

def setFanMinOnTime(child, deviceId, howLong) {
	LOG("setFanMinOnTime(${howLong})", 4, child)
    
    if (!howLong.isNumber() || (howLong.toInteger() < 0) || howLong.toInteger() > 55) {
    	LOG("setFanMinOnTime() - Invalid Argument ${howLong}",4,child,'warn')
        return false
    }
    
    def thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":'+howLong+'}}'
    def thermostatFunctions = ''
    def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
    def result = sendJson(child, jsonRequestBody)
    LOG("setFanMinOnTime(${howLong}) returned ${result}", 4, child,'info')    

    return result
}

def setVacationFanMinOnTime(child, deviceId, howLong) {
	LOG("setVacationFanMinOnTime(${howLong})", 4, child)   
    if (!howLong.isNumber() || (howLong.toInteger() < 0) || howLong.toInteger() > 55) {		// Documentation says 60 is the max, but the Ecobee3 thermostat maxes out at 55 (makes 60 = 0)
    	LOG("setVacationFanMinOnTime() - Invalid Argument ${howLong}",4,child,'warn')
        return false
    }
    
    def evt = atomicState.runningEvent[deviceId]
    def hasEvent = true
    if (!evt) hasEvent = false						// no running event defined
    if (evt.running != true) hasEvent = false		// shouldn't have saved it if it wasn't running
    if (evt.type != "vacation") hasEvent = false	// we only override vacation fanMinOnTime setting
  	if (!hasEvent) {
    	LOG("setVacationFanMinOnTime() - Vacation not active on thermostatId ${deviceId}", 4, child, 'warn')
        return false
    }
    if (evt.fanMinOnTime.toInteger() == howLong.toInteger()) return true	// didn't need to do anything!
    
    if (deleteVacation(child, deviceId, evt.name)) { // apparently on order to change something in a vacation, we have to delete it and then re-create it..
  
    def thermostatSettings = ''
    def thermostatFunctions = '{"type":"createVacation","params":{"name":"' + evt.name + '","coolHoldTemp":"' + evt.coolHoldTemp + '","heatHoldTemp":"' + evt.heatHoldTemp + 
    							'","startDate":"' + evt.startDate + '","startTime":"' + evt.startTime + '","endDate":"' + evt.endDate + '","endTime":"' + evt.endTime + 
                                '","fan":"' + evt.fan + '","fanMinOnTime":"' + "${howLong}" + '"}}'
    def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
    LOG("before sendJson() jsonRequestBody: ${jsonRequestBody}", 4, child, "info")
    
    def result = sendJson(child, jsonRequestBody)
    LOG("setVacationFanMinOnTime(${howLong}) returned ${result}", 4, child, 'info') 

    return result
    }
}

def deleteVacation(child, deviceId, vacationName=null ) {
	def vacaName = vacationName
	if (!vacaName) {		// no vacationName specified, let's find out if one is currently running
        def evt = atomicState.runningEvent[deviceId]
    	if (!evt ||  (evt.running != true) || (evt.type != "vacation") || !evt.name) {
    		LOG("deleteVacation() - Vacation not active on thermostatId ${deviceId}", 4, child, 'warn')
        	return true	// Asked us to delete the current vacation, and there isn't one - I'd still call that a success!
        }
        vacaName = evt.name as String		// default names are Very Big Numbers
    }

    def thermostatSettings = ''
    def thermostatFunctions = '{"type":"deleteVacation","params":{"name":"' + vacaName + '"}}'
    def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
    def result = sendJson(child, jsonRequestBody)
    LOG("deleteVacation() returned ${result}", 4, child,'info') 

    return result
}

def setHold(child, heating, cooling, deviceId, sendHoldType=null, fanMode="", extraParams=[]) {
	int h = (getTemperatureScale() == "C") ? (cToF(heating) * 10) : (heating * 10)
	int c = (getTemperatureScale() == "C") ? (cToF(cooling) * 10) : (cooling * 10)
    
	LOG("setHold() - h: ${heating}(${h}), c: ${cooling}(${c}), ${sendHoldType}", 3, child, 'info')
    
	if (fanMode != "") { 
		tstatSettings << [fan:"${fanMode}"] 
	}
        
    if (extraParams != []) {
    	tstatSettings << extraParams
    }                
    
	def jsonRequestBody 
    if (sendHoldType) {
    	jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":[{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + '","holdType":"' + sendHoldType + '"}}]}'
	} else {
    	jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":[{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + '"}}]}'
    }
    //      			   {"selection":{"selectionType":"thermostats","selectionMatch":"XXX"},             "functions":[{"type":"setHold","params":{"coolHoldTemp":"730","heatHoldTemp":"510","holdType":"nextTransition"}}],}
	//def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '","includeRuntime":true},"functions": [{ "type": "setHold", "params": { "coolHoldTemp": '+c+',"heatHoldTemp": '+h+', "holdType": '+sendHoldType+' } } ]}'
    LOG("about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child)
    
	def result = sendJson(child, jsonRequestBody)
    LOG("setHold() returned ${result}", 4, child,'info')
    return result
}

def setMode(child, mode, deviceId) {
	LOG("setMode(${mode}) for ${deviceId}", 5, child)
        
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"hvacMode":"'+"${mode}"+'"}}}'
    //                     {"selection":{"selectionType":"thermostats","selectionMatch":"XXX"},             "thermostat":{"settings":{"hvacMode":"cool"}}}    
	LOG("Mode Request Body = ${jsonRequestBody}", 4, child)
    
	def result = sendJson(jsonRequestBody)
    LOG("setMode to ${mode} with result ${result}", 4, child)
	if (result) {
    	LOG("setMode(${mode}) returned ${result}", 4, child, "info")
    	child.generateQuickEvent("thermostatMode", mode, 15)
    } else {
    	LOG("setMode(${mode}) - Failed", 1, child, "warn")
    }

	return result
}

def setFanMode(child, fanMode, deviceId, sendHoldType=null) {
	LOG("setFanMode(${fanMode}) for DeviceID: ${deviceId}", 5, child)    
        
    // These values are ignored anyway when setting the fan
    def h = child.device.currentValue("heatingSetpoint")
    def c = child.device.currentValue("coolingSetpoint")
    
    def theHoldType = sendHoldType ? sendHoldType : whatHoldType()
    
    // Per this thread: http://developer.ecobee.com/api/topics/qureies-related-to-setfan
    // def extraParams = [isTemperatureRelative: "false", isTemperatureAbsolute: "false"]
	def thermostatSettings = ''
    def thermostatFunctions = ''     
    if (fanMode == "circulate") {    	
    	fanMode = "auto"        
        LOG("fanMode == 'circulate'", 5, child, "trace")        
        // Add a minimum circulate time here
        // TODO: Need to capture the previous fanMinOnTime and return to that value when the mode is changed again?
        atomicState."previousFanMinOnTime${deviceId}" = getFanMinOnTime(child)
        atomicState."previousHVACMode${deviceId}" = getHVACMode(child)        
		atomicState.circulateFanModeOn = true
        atomicState.offFanModeOn = false
        
        thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":15}}'
        thermostatFunctions = '{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + '","holdType":"' + theHoldType + '","fan":"'+fanMode+'","isTemperatureAbsolute":false,"isTemperatureRelative":false}}'
    } else if (fanMode == "off") {
    	// How to turn off the fan http://developer.ecobee.com/api/topics/how-to-turn-fan-off
        // NOTE: Once you turn it off it does not automatically come back on if you select resume program
        atomicState."previousFanMinOnTime${deviceId}" = getFanMinOnTime(child)
        atomicState."previousHVACMode${deviceId}" = getHVACMode(child)        
    	atomicState.circulateFanModeOn = false    
        atomicState.offFanModeOn = true
        fanMode = "auto"        
        thermostatSettings = ',"thermostat":{"settings":{"hvacMode":"off","fanMinOnTime":0}}'
        thermostatFunctions = ''
    } else {
		atomicState.circulateFanModeOn = false    
        atomicState.offFanModeOn = false
        thermostatSettings = ''
        thermostatFunctions = '{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + '","holdType":"' + theHoldType + '","fan":"'+fanMode+'","isTemperatureAbsolute":false,"isTemperatureRelative":false}}'
    }    
	
    // {"selection":{"selectionType":"thermostats","selectionMatch":"312989153500"},"functions":[{"type":"setHold","params":{"coolHoldTemp":"73","heatHoldTemp":"66","holdType":"nextTransition","fan":"circulate","isTemperatureAbsolute":false,"isTemperatureRelative":false}}],"thermostat":{"settings":{"fanMinOnTime":15}}}
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
    LOG("about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child)
    
	def result = sendJson(child, jsonRequestBody)
    LOG("setFanMode(${fanMode}) returned ${result}", 4, child, 'info')
    
    return result    
}

def setProgram(child, program, String deviceId, sendHoldType=null) {
	// NOTE: Will use only the first program if there are two with the same exact name
	LOG("setProgram(${program}) for ${deviceId} - child ${child}", 2, child, 'info')    
    // def climateRef = program.toLowerCase()   
    	
    def climates = atomicState.program[deviceId].climates
    def climate = climates?.find { it.name.toString() == program.toString() }  // vacation holds can have a number as their name
    LOG("climates - {$climates}", 5, child)
    LOG("climate - {$climate}", 5, child)
    def climateRef = climate?.climateRef.toString()
    
	LOG("setProgram() - climateRef = {$climateRef}", 4, child)
	
    if (climate == null) { return false }
    
	def jsonRequestBody
    if (sendHoldType) {
    	jsonRequestBody = '{"functions":[{"type":"setHold","params":{"holdClimateRef":"'+climateRef+'","holdType":"'+sendHoldType+'"}}],"selection":{"selectionType":"thermostats","selectionMatch":"'+deviceId+'"}}'
    } else {
    	jsonRequestBody = '{"functions":[{"type":"setHold","params":{"holdClimateRef":"'+climateRef+'"}}],"selection":{"selectionType":"thermostats","selectionMatch":"'+deviceId+'"}}'
    }

    LOG("about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child)    
	def result = sendJson(child, jsonRequestBody)	
    LOG("setProgram(${climateRef}) returned ${result}", 4, child, 'info')
    
    return result
}

def addSensorToProgram(child, deviceId, sensorId, programId) {
	LOG("addSensorToProgram(${child},${deviceId},${sensorId},${programId})",4,child,'trace')
	String preText = getDebugLevel() <= 2 ? '' : 'addSensorToProgram() - '
    
    // we basically have to edit the program object in place, and then return the entire thing back to the Ecobee API
	def program = atomicState.program[deviceId]
    if (!program) {
    	return false
    }
    def result = false
    int c = 0
    int s = 0
    while ( program.climates[c] ) {	
    	if (program.climates[c].climateRef == programId) {
        	String tempSensor = "${sensorId}:1"
        	while (program.climates[c].sensors[s]) {
            	if (program.climates[c].sensors[s].id == tempSensor ) {
					LOG("addSensorToProgram() - ${sensorId} is already in the ${programId.capitalize()} program on thermostat ${deviceId}",4,child,'info')
               		return true	// mission accomplished - sensor is already in sensors list 
            	}
                s++
            }
            program.climates[c].sensors << [id:"${tempSensor}"] // add this sensor to the sensors list
            result = true
        }
        if (result) {
        	def programJson = JsonOutput.toJson(program)
            def thermostatSettings = ',"thermostat":{"program":' + programJson +'}'
    		def thermostatFunctions = ''
    		def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
    		result = sendJson(child, jsonRequestBody)
    		LOG("addSensorToProgram() returned ${result}", 4, child,'trace') 
        	if (result) {
        		LOG("${preText}Added sensor ${sensorId} to the ${programId.capitalize()} program on thermostat ${deviceId}",3,child,'info')
            	return true
            }
            break
        }
        c++
    }
    LOG('addSensorToProgram() - Something went wrong',4,child,'trace')
    return false
}

def deleteSensorFromProgram(child, deviceId, sensorId, programId) {
	LOG("deleteSensorFromProgram(${child},${deviceId},${sensorId},${programId})",4,child,'trace')
    String preText = getDebugLevel() <= 2 ? '' : 'deleteSensorFromProgram() - '
    
	def program = atomicState.program[deviceId]
    if (!program) {
    	return false
    }
    int c = 0
    int s = 0
    while ( program.climates[c] ) {	
    	if (program.climates[c].climateRef == programId) { // found the program we want to delete from
        	String tempSensor = "${sensorId}:1"
            def currentSensors = program.climates[c].sensors
        	while (program.climates[c].sensors[s]) {
            	if (program.climates[c].sensors[s].id == tempSensor ) {
                
                	// found it, now we need to delete it - subtract it from the list of sensors                    
                    program.climates[c].sensors = program.climates[c].sensors - program.climates[c].sensors[s]   
        			def programJson = JsonOutput.toJson(program)
            		def thermostatSettings = ',"thermostat":{"program":' + programJson +'}'
    				def thermostatFunctions = ''
    				def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
    				def result = sendJson(child, jsonRequestBody)
    				LOG("deleteSensorFromProgram() returned ${result}", 4, child,'trace') 
        			if (result) {
        				LOG("${preText}Deleted sensor ${sensorId} from the ${programId.capitalize()} program on thermostat ${deviceId}",3,child,'info')
            			return true
            		}
           			break  
            	}
                s++
            }
        }
        c++
    }
    // didn't find the specified climate
    LOG("${preText}Sensor ${sensorId} not found in the ${programId.capitalize()} program on thermostat ${deviceId}",4,child,'info') // didn't find sensor in the specified climate: Success!
    return true
}

// API Helper Functions
private def sendJson(child=null, String jsonBody) {
    
	def returnStatus
    def result = false
    
	def cmdParams = [
			uri: apiEndpoint,
			path: "/1/thermostat",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
			body: jsonBody
	]
	
	try{
		httpPost(cmdParams) { resp ->
   	    	returnStatus = resp.data.status.code

			LOG("sendJson() resp.status ${resp.status}, resp.data: ${resp.data}, returnStatus: ${returnStatus}", 4, child)
				
           	// TODO: Perhaps add at least two tries incase the first one fails?
			if(resp.status == 200) {				
				LOG("Updated ${resp.data}", 4)
				returnStatus = resp.data.status.code
				if (resp.data.status.code == 0) {
					LOG("Successful call to ecobee API.", 4, child)
                    result = true
					apiRestored()
                    generateEventLocalParams()
				} else {
					LOG("Error return code = ${resp.data.status.code}", 1, child, "error")
				}
                // Reset saved state
                atomicState.savedActionJsonBody = null
        		atomicState.savedActionChild = null
			} else {
            	// Should never get here as a non-200 response is supposed to trigger an Exception
   	        	LOG("Sent Json & got http status ${resp.status} - ${resp.status.code}", 2, child, "warn")
			} // resp.status if/else
		} // HttpPost
	} catch (groovyx.net.http.HttpResponseException e) {
    	result = false // this thread failed...hopefully we can succeed after we refresh the auth_token
        if ((e.statusCode == 500) && (e.response.data.status.code == 14)) {
        	LOG("sendJson() - HttpResponseException occurred: Auth_token has expired", 3, null, "info")
            // atomicState.savedActionJsonBody = jsonBody
        	// atomicState.savedActionChild = child.deviceNetworkId
        	// atomicState.action = "sendJsonRetry"
            atomicState.action = ""					// we don't want refreshAuthToken to sendJsonRetry - we will retry ourselves instead
           	LOG( "Refreshing your auth_token!", 4)
           	if ( refreshAuthToken() ) { 
                LOG( "sendJson() - Auth_token refreshed", 2, null, 'info')
                if (!atomicState.sendJsonRetry) {
                	atomicState.sendJsonRetry = true 		// retry only once
                    LOG( "sendJson() - Retrying once...", 2, null, 'info')
 					result = sendJson( child, jsonBody )	// recursively re-attempt now that the token was refreshed
                    LOG( "sendJson() - Retry ${result ? 'succeeded!' : 'failed.'}", 2, null, "${result ? 'info' : 'warn'}")
                    atomicState.sendJsonRetry = false
                }
            } else {
                LOG( "sendJson() - Auth_token refresh failed", 1, null, 'error') 
            }
        } else {
        	LOG("sendJson() - HttpResponseException occurred. Exception info: ${e} StatusCode: ${e.statusCode}", 1, null, "error")
        }
    } catch(Exception e) {
    	// Might need to further break down 
		LOG("sendJson() - Exception Sending Json: " + e, 1, child, "error")
        // atomicState.connected = "warn"
        // generateEventLocalParams()
	}
	return result
}

private def sendJsonRetry() {
	LOG("sendJsonRetry() called", 4)
    def child = null
    if (atomicState.savedActionChild) {
    	child = getChildDevice(atomicState.savedActionChild)
    }
    
    if (atomicState.savedActionJsonBody == null) {
    	LOG("sendJsonRetry() - no saved JSON Body to send!", 2, child, "warn")
        return false
    }    
    
    return sendJson(child, atomicState.savedActionJsonBody)
}

private def getChildThermostatName() { return "Ecobee Thermostat" }
private def getChildSensorName()     { return "Ecobee Sensor" }
private def getServerUrl()           { return "https://graph.api.smartthings.com" }
private def getShardUrl()            { return getApiServerUrl() }
private def getCallbackUrl()         { return "${serverUrl}/oauth/callback" }
private def getBuildRedirectUrl()    { return "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${shardUrl}" }
private def getApiEndpoint()         { return "https://api.ecobee.com" }

// This is the API Key from the Ecobee developer page. Can be provided by the app provider or use the appSettings
private def getSmartThingsClientId() { 
	if(!appSettings.clientId) {
		return "obvlTjUuuR2zKpHR6nZMxHWugoi5eVtS"		
	} else {
		return appSettings.clientId 
    }
}

private def LOG(message, level=3, child=null, logType='debug', event=false, displayEvent=true) {
	def dbgLevel = debugLevel(level)
    if (!dbgLevel) return 		// let's not waste CPU cycles if we don't have to...
    
	def prefix = ""
    def logTypes = ['error', 'debug', 'info', 'trace', 'warn']
    
    if(!logTypes.contains(logType)) {
    	log.error "LOG() - Received logType (${logType}) which is not in the list of allowed types"
        if (event && child) { debugEventFromParent(child, "LOG() - Invalid logType ${logType}") }
        logType = 'debug'
    }
    
    if ( logType == 'error' ) { 
    	atomicState.lastLOGerror = "${message} @ ${getTimestamp()}"
        atomicState.LastLOGerrorDate = getTimestamp()        
	}
    // if ( debugLevel(0) ) { return }
	if ( debugLevel(5) ) { prefix = 'LOG: ' }
	if ( dbgLevel ) { 
    	log."${logType}" "${prefix}${message}"        
        if (event) { debugEvent("${message}", displayEvent) }
        if (child) { debugEventFromParent(child, message) }
	}    
}

private def debugEvent(message, displayEvent = false) {
	def results = [
		name: 'appdebug',
		descriptionText: message,
		displayed: displayEvent
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent (results)
}

private def debugEventFromParent(child, message) {
	 def data = [
            	debugEventFromParent: message
            ]         
	if (child) { child.generateEvent(data) }
}

// TODO: Create a more generic push capability to send notifications
// send both push notification and mobile activity feeds
private def sendPushAndFeeds(notificationMessage) {
	LOG("sendPushAndFeeds >> notificationMessage: ${notificationMessage}", 1, null, "warn")
	LOG("sendPushAndFeeds >> atomicState.timeSendPush: ${atomicState.timeSendPush}", 1, null, "warn")
    
    def msg = "Your Ecobee thermostat(s) at ${location.name} " + notificationMessage		// for those that have multiple locations, tell them where we are
    if (atomicState.timeSendPush) {
        if ( (now() - state.timeSendPush) >= (1000 * 60 * 60 * 1)) { // notification is sent to remind user no more than once per hour
        	if (location.contactBookEnabled && settings.recipients) {
				sendNotificationToContacts(msg, settings.recipients, [event: true]) 
    		} else {
				sendPush(msg)
    		}
            sendActivityFeeds(notificationMessage)
            atomicState.timeSendPush = now()
        }
    } else {
        if (location.contactBookEnabled && settings.recipients) {
			sendNotificationToContacts(msg, settings.recipients, [event: true]) 
    	} else {
			sendPush(msg)
    	}
        sendActivityFeeds(notificationMessage)
        atomicState.timeSendPush = now()
    }
    // This is done in apiLost now
    // atomicState.authToken = null
}

private def sendActivityFeeds(notificationMessage) {
    def devices = getChildDevices()
    devices.each { child ->
        child.generateActivityFeedsEvent(notificationMessage) //parse received message from parent
    }
}

// Helper Functions
// Creating my own as it seems that the built-in version only works for a device, NOT a SmartApp
def myConvertTemperatureIfNeeded(scaledSensorValue, cmdScale, precision) {
	if ( (cmdScale != "C") && (cmdScale != "F") && (cmdScale != "dC") && (cmdScale != "dF") ) {
    	// We do not have a valid Scale input, throw a debug error into the logs and just return the passed in value
        LOG("Invalid temp scale used: ${cmdScale}", 2, null, "error")
        return scaledSensorValue
    }

	def returnSensorValue 
    
	// Normalize the input
	if (cmdScale == "dF") { cmdScale = "F" }
    if (cmdScale == "dC") { cmdScale = "C" }

	LOG("About to convert/scale temp: ${scaledSensorValue}", 5, null, "trace", false)
	if (cmdScale == getTemperatureScale() ) {
    	// The platform scale is the same as the current value scale
        returnSensorValue = scaledSensorValue.round(precision)
    } else if (cmdScale == "F") {		    	
    	returnSensorValue = fToC(scaledSensorValue).round(precision)
    } else {
    	returnSensorValue = cToF(scaledSensorValue).round(precision)
    }
    LOG("returnSensorValue == ${returnSensorValue}", 5, null, "trace", false)
    return returnSensorValue
}

def wantMetric() {
	return (getTemperatureScale() == "C")
}

private Double cToF(temp) {
	LOG("cToF entered with ${temp}", 5, null, "info")
	return (temp * 1.8 + 32) as Double
    // return celsiusToFahrenheit(temp)
}
private Double fToC(temp) {	
	LOG("fToC entered with ${temp}", 5, null, "info")
	return (temp - 32) / 1.8 as Double
    // return fahrenheitToCelsius(temp)
}

// Establish the minimum amount of time to wait to do another poll
private def getMinMinBtwPolls() {
    // TODO: Make this configurable in the SmartApp
	return 1
}

// need these next 4 get routines because settings variable defaults aren't set unless the "Preferences" page is actually opened/rendered
def Integer getPollingInterval() {
    return (settings.pollingInterval?.isNumber() ? settings.pollingInterval.toInteger() : 5)
}

private Integer getTempDecimals() {
	return ( settings.tempDecimals?.isNumber() ? settings.tempDecimals.toInteger() : (wantMetric() ? 1 : 0))
}

private Integer getDebugLevel() {
	return ( settings.debugLevel?.isNumber() ? settings.debugLevel.toInteger() : 3)
}

private String getHoldType() {
	return ( settings.holdType ? settings.holdType : 'Until I Change')
}

private def String getTimestamp() {
	// There seems to be some possibility that the timeZone will not be returned and will cause a NULL Pointer Exception
	def timeZone = location?.timeZone ?: ""
    // LOG("timeZone found as ${timeZone}", 5)
    if(timeZone == "") {
    	return new Date().format("yyyy-MM-dd HH:mm:ss z")
    } else {
		return new Date().format("yyyy-MM-dd HH:mm:ss z", timeZone)
	}
}

private def getTimeOfDay() {
	def nowTime 
    if(location.timeZone) {
    	nowTime = new Date().format("HHmm", location.timeZone).toDouble()
    } else {
    	nowTime = new Date().format("HHmm").toDouble()
    }
    LOG("getTimeOfDay() - nowTime = ${nowTime}", 4, null, "trace")
    if ( (nowTime < atomicState.sunriseTime) || (nowTime > atomicState.sunsetTime) ) {
    	return "night"
    } else {
    	return "day"
    }
}

// Are we connected with the Ecobee service?
private String apiConnected() {
	// values can be "full", "warn", "lost"
	if (atomicState.connected == null) atomicState.connected = "warn"
	return atomicState.connected?.toString() ?: "lost"
}

private def apiRestored() {
	atomicState.connected = "full"
	unschedule("notifyApiLost")
    atomicState.reAttemptPoll = 0
}

private def getDebugDump() {
	 def debugParams = [when:"${getTimestamp()}", whenEpic:"${now()}", 
				lastPollDate:"${atomicState.lastPollDate}", lastScheduledPollDate:"${atomicState.lastScheduledPollDate}", 
				lastScheduledWatchdogDate:"${atomicState.lastScheduledWatchdogDate}",
				lastTokenRefreshDate:"${atomicState.lastTokenRefreshDate}", 
                initializedEpic:"${atomicState.initializedEpic}", initializedDate:"${atomicState.initializedDate}",
                lastLOGerror:"${atomicState.lastLOGerror}", authTokenExpires:"${atomicState.authTokenExpires}"
			]    
	return debugParams
}

private def apiLost(where = "[where not specified]") {
    LOG("apiLost() - ${where}: Lost connection with APIs. unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with Ecobee by running the SmartApp and logging in again", 1, null, "error")
    atomicState.apiLostDump = getDebugDump()
    if (apiConnected() == "lost") {
    	LOG("apiLost() - already in lost atomicState. Nothing else to do. (where= ${where})", 5, null, "trace")
        return
    }
   
    // provide cleanup steps when API Connection is lost
	def notificationMessage = "is disconnected from SmartThings/Ecobee, because the access credential changed or was lost. Please go to the Ecobee (Connect) SmartApp and re-enter your account login credentials."
    atomicState.connected = "lost"
    atomicState.authToken = null
    
    sendPushAndFeeds(notificationMessage)
	generateEventLocalParams()

    LOG("Unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with Ecobee by running the SmartApp and logging in again", 0, null, "error")
    
    // Notify each child that we lost so it gets logged
    if ( debugLevel(3) ) {
    	def d = getChildDevices()
    	d?.each { oneChild ->
        	LOG("apiLost() - notifying each child: ${oneChild.device.displayName} of loss", 1, child, "error")
		}
    }
    unschedule("pollScheduled")
    unschedule("scheduleWatchdog")
    runEvery3Hours("notifyApiLost")
}

def notifyApiLost() {
	def notificationMessage = "is disconnected from SmartThings/Ecobee, because the access credential changed or was lost. Please go to the Ecobee (Connect) SmartApp and re-enter your account login credentials."
    if ( atomicState.connected == "lost" ) {
    	generateEventLocalParams()
		sendPushAndFeeds(notificationMessage)
        LOG("notifyApiLost() - API Connection Previously Lost. User MUST reintialize the connection with Ecobee by running the SmartApp and logging in again", 0, null, "error")
	} else {
    	// Must have restored connection
        unschedule("notifyApiLost")
    }    
}

private String childType(child) {
	// Determine child type (Thermostat or Remote Sensor)
    if ( child.hasCapability("Thermostat") ) { return getChildThermostatName() }
    if ( child.name.contains( getChildSensorName() ) ) { return getChildSensorName() }
    return "Unknown"
}

private def getFanMinOnTime(child) {
	LOG("getFanMinOnTime() - Looking up current fanMinOnTime for ${child.device.displayName}", 4, child)
    String devId = getChildThermostatDeviceIdsString(child)
    LOG("getFanMinOnTime() Looking for ecobee thermostat ${devId}", 5, child, "trace")
    
    def fanMinOnTime = atomicState.settings[devId]?.fanMinOnTime
    LOG("getFanMinOnTime() fanMinOnTime is ${fanMinOnTime} for therm ${devId}", 4, child)
	return fanMinOnTime
}

private String getHVACMode(child) {
	LOG("Looking up current hvacMode for ${child.device.displayName}", 4, child)
    String devId = getChildThermostatDeviceIdsString(child)
    // LOG("getHVACMode() Looking for ecobee thermostat ${devId}", 5, child, "trace")
    
    def hvacMode = atomicState.settings[devId].hvacMode		// FIXME
	LOG("getHVACMode() hvacMode is ${hvacMode} for therm ${devId}", 4, child)
	return hvacMode
}

def getAvailablePrograms(thermostat) {
	// TODO: Finish implementing this!
    LOG("Looking up the available Programs for this thermostat (${thermostat})", 4)
    String devId = getChildThermostatDeviceIdsString(thermostat)
    // LOG("getAvailablePrograms() Looking for ecobee thermostat ${devId}", 5, thermostat, "trace")

    def climates = atomicState.program[devId].climates
    
    return climates?.collect { it.name }
}

private def whatHoldType() {
	def sendHoldType = settings.holdType ? (settings.holdType=="Temporary" || settings.holdType=="Until Next Program")? "nextTransition" : (settings.holdType=="Permanent" || settings.holdType=="Until I Change")? "indefinite" : "indefinite" : "indefinite"
	// LOG("Entered whatHoldType() with ${sendHoldType}  settings.holdType == ${settings.holdType}")
	 
    return sendHoldType
}

private def debugLevel(level=3) {
	// log.trace("debugLevel() -- settings.debugLevel == ${settings.debugLevel}")
    // if(settings.debugLevel == "0") { 
    	// log.trace("debugLevel() - debugLvlNum == 0 triggered")
    	// return false 
	//}
    
	// def debugLvlNum = getDebugLevel()
    // def wantedLvl = level?.toInteger()    
    // log.trace("debugLvlNum = ${debugLvlNum}; wantedLvl = ${wantedLvl}")
    Integer dbgLevel = getDebugLevel()
    return (dbgLevel == 0) ? false : (dbgLevel >= level?.toInteger())
	// return ( getDebugLevel() >= level?.toInteger() )
}

private String fixDateTimeString( String dateStr, String timeStr, String thermostatTime) {
	String todayDate = thermostatTime.take(10)
	// date is in ecobee format: YYYY-MM-DD or null (meaning "today")
	// time is in ecobee format: HH:MM:SS
    String resultStr = ''
    String myDate = ''
    String myTime = ''
    String tTime = timeStr
    
    if (dateStr == '2035-01-01' ) { 		// to Infinity
        myDate = 'a long time from now'
        tTime = ''
    } else if (dateStr == todayDate) {
    	myDate = 'today'    
    } else {
		myDate = dateStr.drop(5)	// get MM-DD
        String delim = myDate.substring(2,3)
        int month = myDate.take(2).toInteger()
        int days = myDate.drop(3).toInteger()
        int todayMonth = todayDate.drop(5).take(2).toInteger()
        if (month == todayMonth) {
        	int todayDays = todayDate.drop(8).toInteger()
            if (todayDays+1 == days) myDate = 'tomorrow'
            if (todayDays-1 == days) myDate = 'yesterday'
        } else {
        	myDate = "on ${month}${delim}${days}"
        }
	}
    
    if (tTime != '') {
		myTime = timeStr.take(5)		// get HH:MM
		int hours = myTime.take(2).toInteger()
        int mins = myTime.drop(3).toInteger()
		if (hours < 12) {
        	if (hours==0) hours = 12
			myTime = "${hours}" + myTime.drop(2) + 'am'
		} else {
        	if (hours==12) hours = 24		// hack
			myTime = "${hours-12}" + myTime.drop(2) + "pm"
		}
	}
    if (myDate || myTime) {
    	resultStr = myTime ? "${myDate} at ${myTime}" : "${myDate}"
    }
    return resultStr
}
