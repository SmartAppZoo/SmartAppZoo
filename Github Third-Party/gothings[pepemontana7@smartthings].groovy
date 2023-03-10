def getVersionNum() { return "0.0.1" }
private def getVersionLabel() { return "GoThings (Connect) Version ${getVersionNum()}" }

definition(
	name: "Go Things (Connect)",
	namespace: "pepemontana7",
	author: "Jose Delgado",
	description: "Connect your Go to SmartThings.",
	category: "My Apps",
	iconUrl: "https://s3-us-west-2.amazonaws.com/jd7bucket/images/gothings.png",
	iconX2Url: "https://s3-us-west-2.amazonaws.com/jd7bucket/images/gothings%402x.png",
	singleInstance: true
) {
	appSetting "clientId"
	appSetting "apiLogin"
 	appSetting "apiPassword"

}

preferences {
	page(name: "mainPage")
    page(name: "removePage")
	page(name: "authPage")
	page(name: "thermsPage")
     page(name: "preferencesPage")    
    page(name: "addWatchdogDevicesPage")
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
    
	dynamicPage(name: "mainPage", title: "Welcome to GOTHINGS (Connect)", install: readyToInstall, uninstall: false, submitOnChange: true) {
    	def ecoAuthDesc = (atomicState.authToken != null) ? "[Connected]\n" :"[Not Connected]\n"        
		
        // If not device Handlers we cannot proceed
        if(!atomicState.initialized && !deviceHandlersInstalled) {
			section() {
				paragraph "ERROR!\n\nYou MUST add the   ${getChildSensorName()} Device Handlers to the IDE BEFORE running the setup."				
			}		
        } else {
        	readyToInstall = true
        }
        
        if(atomicState.initialized && !atomicState.authToken) {
        	section() {
				paragraph "WARNING!\n\nYou are no longer connected to the Go Things API. Please re-Authorize below."				
			}
        }       

		if(atomicState.authToken != null && atomicState.initialized != true) {
        	section() {
            	paragraph "Please click 'Done' to save your credentials. Then re-open the SmartApp to continue the setup."
            }
        }

		// Need to save the initial login to setup the device without timeouts
		if(atomicState.authToken != null && atomicState.initialized) {
        	//if (settings.thermostats?.size() > 0 && atomicState.initialized) {
            //	section("Helper SmartApps") {
              //  	href ("helperSmartAppsPage", title: "Helper SmartApps", description: "Tap to manage Helper SmartApps")
               // }            
            //}
			section("Devices") {
				LOG(" section Devices:  settings.thermostats = ${settings.thermostats} ")
				LOG(" section Devices:  settings.thermostats?.size() = ${settings.thermostats?.size()} ")

				def howManyThermsSel = settings.thermostats?.size() ?: 0
				def howManyTherms = atomicState.numAvailTherms ?: "?"
                //def howManySensors = atomicState.numAvailSensors ?: "?"
                
                // Thermostats
				atomicState.settingsCurrentTherms = settings.thermostats ?: []
    	    	href ("thermsPage", title: "GoThings", description: "Tap to select GoThings [${howManyThermsSel}/${howManyTherms}]")                
                
 
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
		section("Go Things Authentication") {
			href ("authPage", title: "go Authorization", description: "${ecoAuthDesc}Tap for go Credentials")
		}      
		if ( debugLevel(5) ) {
			section ("Debug Dashboard") {
				href ("debugDashboardPage", description: "Tap to enter the Debug Dashboard", title: "Debug Dashboard")
			}
		}
		section("Remove go (Connect)") {
			href ("removePage", description: "Tap to remove go (Connect) ", title: "Remove go (Connect)")
		}            
     
		section (getVersionLabel())
	}
}


def removePage() {
	dynamicPage(name: "removePage", title: "Remove go (Connect) and All Devices", install: false, uninstall: true) {
    	section ("WARNING!\n\nRemoving go (Connect) also removes all Devices\n") {
        }
    }
}

// Setup OAuth between SmartThings and GoThings clouds
def authPage() {
	LOG("=====> authPage() Entered", 5)

	if(!atomicState.accessToken) { //this is an access token for the 3rd party to make a call to the connect app
		atomicState.accessToken = createAccessToken()
	}

	def description = "Click to enter go Credentials"
	def uninstallAllowed = false
	def oauthTokenProvided = false

	if(atomicState.authToken) {
		description = "You are connected. Tap Done above."
		uninstallAllowed = true
		oauthTokenProvided = true
        apiRestored()
	} else {
		description = "Tap to enter go Credentials"
	}

	def redirectUrl = buildRedirectUrl //"${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}"
    LOG("authPage() --> RedirectUrl = ${redirectUrl}")
    
	// get rid of next button until the user is actually auth'd
	if (!oauthTokenProvided) {
    	LOG("authPage() --> in !oauthTokenProvided")    	
		return dynamicPage(name: "authPage", title: "go Setup", nextPage: "", uninstall: uninstallAllowed) {
			section() {
				paragraph "Tap below to log in to the GoThings service and authorize SmartThings access. Be sure to press the 'Allow' button on the 2nd page."
				href url:redirectUrl, style:"embedded", required:true, title: "go Account Authorization", description:description 
			}
		}
	} else {    	
        LOG("authPage() --> in else for oauthTokenProvided - ${atomicState.authToken}.")
        return dynamicPage(name: "authPage", title: "go Setup", nextPage: "mainPage", uninstall: uninstallAllowed) {
        	section() {
            	paragraph "Return to main menu."
                href url:redirectUrl, style: "embedded", state: "complete", title: "go Account Authorization", description: description
			}
        }           
	}
}

 // Select which Thermostats are to be used
def thermsPage(params) {
	LOG("=====> thermsPage() entered", 5)    
	LOG("token = ${atomicState.authToken}")   
	def stats = getGoThings()
    LOG("thermsPage() -> thermostat list: ${stats}")
    LOG("thermsPage() starting settings: ${settings}")
	LOG("thermsPage() params passed? ${params}")
    LOG("thermsPage() params passed? ${params}", 4, null, "trace")

    dynamicPage(name: "thermsPage", title: "Select Thermostats", params: params, nextPage: "", content: "thermsPage", uninstall: false) {    
    	section("Units") {
        	paragraph "NOTE: The units type (F or C) is determined by your Hub Location settings automatically. Please update your Hub settings (under My Locations) to change the units used. Current value is ${getTemperatureScale()}."
        }
    	section("Select Thermostats") {
			LOG("thersPage(): atomicState.settingsCurrentTherms=${atomicState.settingsCurrentTherms}   settings.thermostats=${settings.thermostats}", 4, null, "trace")
			if (atomicState.settingsCurrentTherms != settings.thermostats) {
				LOG("atomicState.settingsCurrentTherms != settings.thermostats determined!!!", 4, null, "trace")			
			} else { LOG("atomicState.settingsCurrentTherms == settings.thermostats: No changes detected!", 4, null, "trace") }
        	paragraph "Tap below to see the list of GoThings thermostats available in your Gothings account and select the ones you want to connect to SmartThings."
			input(name: "thermostats", title:"Select Thermostats", type: "enum", required:false, multiple:true, description: "Tap to choose", params: params, metadata:[values:stats], submitOnChange: true)        
        }      
    }      
}




def preferencesPage() {
    LOG("=====> preferencesPage() entered. settings: ${settings}", 5)

    dynamicPage(name: "preferencesPage", title: "Update SmartApp Preferences", nextPage: "") {
		section("SmartApp Preferences") {
        	input(name: "holdType", title:"Select Hold Type", type: "enum", required:false, multiple:false, defaultValue:  "Until I Change", description: "Until I Change", metadata:[values:["Until I Change", "Until Next Program"]])
            paragraph "The 'Smart Auto Temperature Adjust' feature determines if you want to allow the thermostat setpoint to be changed using the arrow buttons in the Tile when the thermostat is in 'auto' mode."
            input(name: "smartAuto", title:"Use Smart Auto Temperature Adjust?", type: "bool", required:false, defaultValue: false, description: "")
            input(name: "pollingInterval", title:"Polling Interval (in Minutes)", type: "enum", required:false, multiple:false, defaultValue:5, description: "5", options:["1", "3", "5", "10", "15", "30"])
            input(name: "debugLevel", title:"Debugging Level (higher # for more information)", type: "enum", required:false, multiple:false, defaultValue:3, description: "3", metadata:[values:["5", "4", "3", "2", "1", "0"]])            
            paragraph "Showing a Thermostat as a separate Sensor is useful if you need to access the actual temperature in the room where the Thermostat is located and not just the (average) temperature displayed on the Thermostat"
            input(name: "showThermsAsSensor", title:"Include Thermostats as a separate GoThings Sensor?", type: "bool", required:false, defaultValue: false, description: "")
            paragraph "Monitoring external devices can be used to drive polling and the watchdog events. Be warned, however, not to select too many devices or devices that will send too many events as this can cause issues with the connection."
            input(name: "useWatchdogDevices", title:"Monitor external devices to drive additional polling and watchdog events?", type: "bool", required:false, description: "", defaultValue:false)
            paragraph "Set the pause between pressing the setpoint arrows and initiating the API calls. The pause needs to be long enough to allow you to click the arrow again for changing by more than one degree."
            input(name: "arrowPause", title:"Delay timer value after pressing setpoint arrows", type: "enum", required:false, multiple:false, description: "4", defaultValue:5, options:["1", "2", "3", "4", "5"])
        }
	}
}

def addWatchdogDevicesPage() {
	LOG("Displaying the Watchdog Device Selection page next...", 5, null, "trace")
    dynamicPage(name: "addWatchdogDevicesPage", title: "Select Watchdog Devices", nextPage: "") {
		section("Polling and Watchdog Devices") {
        	paragraph ("Select device(s) that you wish to subscribe to in order to create additional polling events and trigger the watchdog timers. " +
            	"Do NOT select too many devices or devices that will cause excess polling. " + 
                "GoThings only updates their data every 3 minutes so any polling interval greater than that is unnecessary.")
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
        	paragraph "debugLevel: ${settings.debugLevel} (default=3 if null)"
            paragraph "holdType: ${settings.holdType} (default='Until I Change' if null)"
            paragraph "pollingInterval: ${settings.pollingInterval} (default=5 if null)"
            paragraph "showThermsAsSensor: ${settings.showThermsAsSensor} (default=false if null)"
            paragraph "smartAuto: ${settings.smartAuto} (default=false if null)"   
            paragraph "Selected Thermostats: ${settings.thermostats}"
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
            href ("removePage", description: "Tap to remove GoThings (Connect) ", title: "")
        }
    }    
}

// pages that are part of Debug Dashboard
def pollChildrenPage() {
	LOG("=====> pollChildrenPage() entered.", 5)
	 LOG("=====> pollChildrenPage() entered.")

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
	LOG("=====> refreshAuthTokenPage() entered.")
    refreshAuthToken()
    
	dynamicPage(name: "refreshAuthTokenPage", title: "") {
    	section() {
        	paragraph "refreshAuthTokenPage() was called"
        }
    }    
}

 
// End Prefernce Pages


// Preference Pages Helpers
private def Boolean testForDeviceHandlers() {
   LOG("testForDeviceHandlers() entered")

	if (atomicState.runTestOnce != null) { return atomicState.runTestOnce }
    
    def DNIAdder = now().toString()
    def d1
    def d2
    def success = true
    
	try {         
		//LOG("You MUST add the  Device Handlers to the IDE BEFORE running the setup.", 1, null, "error")
  	
		d1 = addChildDevice(app.namespace, getChildThermostatName(), "dummyThermDNI-${DNIAdder}", null, ["label":"GoT Therm:TestingForInstall", completedSetup:true])
		d2 = addChildDevice(app.namespace, getChildSensorName(), "dummySensorDNI-${DNIAdder}", null, ["label":"GOT Sensor:TestingForInstall", completedSetup:true])
	} catch (physicalgraph.app.exception.UnknownDeviceTypeException e) {
		LOG("You MUST add the ${getChildThermostatName()} and ${getChildSensorName()} Device Handlers to the IDE BEFORE running the setup.", 1, null, "error")
		success = false
	}
    
    atomicState.runTestOnce = success
    LOG("testForDeviceHandlers()  deleting")
    LOG("testForDeviceHandlers() dummyThermDNI-${DNIAdder} , dummySensorDNI-${DNIAdder}")

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
			client_secret : "aabbccdd",
			state	  : oauthState,
			redirect_uri: callbackUrl //"https://graph.api.smartthings.com/oauth/callback"
		]

		def tokenUrl = "${apiEndpoint}/token?${toQueryString(tokenParams)}"
        LOG("callback()-->tokenURL: ${tokenUrl}", 2)

		httpPost(uri: tokenUrl) { resp ->
			atomicState.refreshToken = resp.data.refresh_token
			atomicState.authToken = resp.data.access_token
            
            LOG("callback ==> Expires in ${resp?.data?.expires_in} seconds")
            atomicState.authTokenExpires = now() + (resp.data.expires_in * 1000)
            LOG("swapped token: $resp.data; atomicState.refreshToken: ${atomicState.refreshToken}; atomicState.authToken: ${atomicState.authToken}", 2)
		    LOG("swapped token: $resp.data; atomicState.refreshToken: ${atomicState.refreshToken}; atomicState.authToken: ${atomicState.authToken}")

		}

		if (atomicState.authToken) { success() } else { fail() }

	} else {
    	LOG("callback() failed oauthState != atomicState.oauthInitState", 1, null, "warn")
	}
 
}


def success() {
	def message = """
    <p>Your GoThings Account is now connected to SmartThings!</p>
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
<title>GoThings & SmartThings connection</title>
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
                <img src="https://s3-us-west-2.amazonaws.com/jd7bucket/images/gothings%402x.png" alt="gothings icon" />
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

// Get the list of GoThings Thermostats for use in the settings pages
def getGoThings() {	
	LOG("====> getGoThings() entered", 5)  
	 LOG(" in getGoThings() entered")    
  
 	//def requestBody = '{"selection":{"selectionType":"registered","selectionMatch":"","includeRuntime":true,"includeSensors":true,"includeProgram":true}}'
	def deviceListParams = [
			uri: apiEndpoint,
			path: "/devices?code=${atomicState.authToken}"
			//headers: ["Content-Type": "application/json"]
			//headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.authToken}"],
			//query: [format: 'json', body: requestBody]
			//query: [format: 'json']
	]

    def devUrl = "${apiEndpoint}/devices?code=${ atomicState.authToken}"
    LOG("getGoThings()-->devUrl: ${devUrl}", 2)
	def stats = [:]
	LOG("loggin ${deviceListParams}")
    try {
        httpGet(uri: devUrl) { resp ->
		LOG("httpGet() response: ${resp}")
        
        // Initialize the Thermostat Data. Will reuse for the Sensor List intialization
        atomicState.thermostatData = resp.data.data       	
        
            if (resp.status == 200) {
            	LOG("httpGet() in 200 Response")
				LOG("goThings resp.data.size ${resp.data.data.size()}")

                atomicState.numAvailTherms = resp.data.data.size() ?: 0
				LOG("goThings resp.data  ${resp.data}")
				LOG("goThings resp.data  ${resp.data.data}")
                LOG("goThings resp.data.size ${resp.data.data.size()}")
            	resp.data.data.each { stat ->
					def dni = [app.id, stat.ID].join('.')
					stats[dni] = getThermostatDisplayName(stat)
					LOG("getGoThings stats ${stat.ID} ${stat.Name} ")

                }
            } else {                
                LOG("httpGet() - in else: http status: ${resp.status}")
                //refresh the auth token
                if (resp.status == 500 && resp.data.status.code == 14) {
                	LOG("Storing the failed action to try later")
                    atomicState.action = "getGoThings"
                    LOG("goGetThings ==> Refreshing your auth_token!")
                    refreshAuthToken()
                } else {
                	LOG("Other error. Status: ${resp.status}  Response data: ${resp.data} ", 1)
                }
            }
        }
    } catch(Exception e) {
    	LOG("___exception getGoThings(): ${e}")
        refreshAuthToken()
    }
	atomicState.thermostatsWithNames = stats
    LOG("atomicState.thermostatsWithNames == ${atomicState.thermostatsWithNames}", 4)
	return stats
}
def getThermostatDisplayName(stat) {
	if(stat?.Name)
	    LOG("getThermostatDisplayName  ${stat.Name}")
		return stat.Name.toString()
	return (getThermostatTypeName(stat) + " (${stat.ID})").toString()
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
    LOG("initialize() --> starting in initialize.")

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
    
    atomicState.timeOfDay = getTimeOfDay()
    
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
	    
    // Setup initial polling and determine polling intervals
	atomicState.pollingInterval = getPollingInterval()
    atomicState.watchdogInterval = 15
    atomicState.reAttemptInterval = 15 // In seconds
	
    if (state.initialized) {		
    	// refresh Thermostats and Sensor full lists
		getGoThings()
    } 
   
    // Children
    def aOK = true
	if (settings.thermostats?.size() > 0) { aOK = aOK && createChildrenThermostats() }
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
        if ( settings.watchdogTemp?.size() > 0) { subscribe(settings.watchdogTemp, "temperature", userDefinedEvent) }    
    }    
    
    // Schedule the various handlers
    LOG("Spawning scheduled events from initialize()", 5, null, "trace")
    if (settings.thermostats?.size() > 0) { 
    	LOG("Spawning the poll scheduled event. (settings.thermostats?.size() - ${settings.thermostats?.size()})")
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
        
    return aOK
}

private def createChildrenThermostats() {
	LOG("createChildrenThermostats() entered: thermostats=${settings.thermostats}", 5)
    // Create the child Thermostat Devices
	def devices = settings.thermostats.collect { dni ->
		def d = getChildDevice(dni)
		if(!d) {        	
            try {
				d = addChildDevice(app.namespace, getChildThermostatName(), dni, null, ["label":"GoThing: ${atomicState.thermostatsWithNames[dni]}", completedSetup:true])			
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
    
    LOG("Created/Updated ${devices.size()} thermostats")    
    return true
}



// NOTE: For this to work correctly getGoThingsThermostats()  
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
    scheduleWatchdog(evt, true)
}

def userDefinedEvent(evt) {
	LOG("userDefinedEvent() - with evt (Device:${evt?.displayName} ${evt?.name}:${evt?.value})", 4, null, "info")
    atomicState.lastUserDefinedEventDate = getTimestamp()
    atomicState.lastUserDefinedEventInfo = "Event Info: (Device:${evt?.displayName} ${evt?.name}:${evt?.value})"    
    if ( ((now() - atomicState.lastUserDefinedEvent) / 1000 / 60) < 3) { 
    	LOG("Time since last event is less than 3 minutes. Exiting without performing additional actions.", 4)
    	return 
	}
    
    atomicState.lastUserDefinedEvent = now()
    atomicState.lastUserDefinedEventDate = getTimestamp()    
    poll()
    scheduleWatchdog(evt, true)
}

def scheduleWatchdog(evt=null, local=false) {
	def results = true    
    LOG("scheduleWhatchdog() called with: evt (${evt?.name}:${evt?.value}) & local (${local})", 4, null, "trace")
    // Only update the Scheduled timestamp if it is not a local action or from a subscription
    if ( (evt == null) && (local==false) ) {
    	atomicState.lastScheduledWatchdog = now()
        atomicState.lastScheduledWatchdogDate = getTimestamp()
	}
    
    // Check to see if we have called too soon
    def timeSinceLastWatchdog = (now() - state.lastWatchdog) / 1000 / 60
    if ( timeSinceLastWatchdog < 1 ) {
    	LOG("It has only been ${timeSinceLastWatchdog} since last scheduleWatchdog was called. Please come back later.", 2, null, "trace")
        return true
    }
    
    atomicState.lastWatchdog = now()
    atomicState.lastWatchdogDate = getTimestamp()
    
    LOG("After watchdog tagging")
	if(apiConnected() == "lost") {
    	// Possibly a false alarm? Check if we can update the token with one last fleeting try...
	    LOG("scheduleWatchdog() apiConnected() == refreshotoken next...")

        if( refreshAuthToken() ) { 
        	// We are back in business!
			LOG("scheduleWatchdog() - Was able to recover the lost connection. Please ignore any notifications received.", 1, null, "error")
        } else {        
			LOG("scheduleWatchdog() - Unable to schedule handlers do to loss of API Connection. Please ensure you are authorized.", 1, null, "error")
			return false
		}
	}
    
	def pollAlive = isDaemonAlive("poll")
    def watchdogAlive = isDaemonAlive("watchdog")
    
    LOG("scheduleWatchdog() --> pollAlive==${pollAlive}  watchdogAlive==${watchdogAlive}", 4, null, "debug")
    
    // Reschedule polling if it has been a while since the previous poll    
    if (!pollAlive) { spawnDaemon("poll") }
    if (!watchdogAlive) { spawnDaemon("watchdog") }
    
    return true
}

// Watchdog Checker
private def Boolean isDaemonAlive(daemon="all") {
    LOG("isDaemonAlive() --")

	// Daemon options: "poll", "auth", "watchdog", "all"    
    def daemonList = ["poll", "auth", "watchdog", "all"]

	daemon = daemon.toLowerCase()
    def result = true    
    		
    def timeSinceLastScheduledPoll = (atomicState.lastScheduledPoll == 0 || atomicState.lastScheduledPoll == null) ? 0 : ((now() - atomicState.lastScheduledPoll) / 1000 / 60)  // TODO: Removed toDouble() will this impact?
    def timeSinceLastScheduledWatchdog = (atomicState.lastScheduledWatchdog == 0 || atomicState.lastScheduledWatchdog == null) ? 0 : ((now() - atomicState.lastScheduledWatchdog) / 1000 / 60)
	def timeBeforeExpiry = atomicState.authTokenExpires ? ((atomicState.authTokenExpires - now()) / 1000 / 60) : 0
    
    LOG("isDaemonAlive() - now() == ${now()} for daemon (${daemon})", 5, null, "trace")
    LOG("isDaemonAlive() - Time since last poll? ${timeSinceLastScheduledPoll} -- atomicState.lastScheduledPoll == ${atomicState.lastScheduledPoll}", 4, null, "info")
    LOG("isDaemonAlive() - Time since watchdog activation? ${timeSinceLastScheduledWatchdog} -- atomicState.lastScheduledWatchdog == ${atomicState.lastScheduledWatchdog}", 4, null, "info")
    LOG("isDaemonAlive() - Time left (timeBeforeExpiry) until expiry (in min): ${timeBeforeExpiry}", 4, null, "info")
        
    if (daemon == "poll" || daemon == "all") {
    	LOG("isDaemonAlive() - Checking daemon (${daemon}) in 'poll'", 4, null, "trace")
        def maxInterval = atomicState.pollingInterval + 2
        if ( timeSinceLastScheduledPoll >= maxInterval ) { result = false }
	}	
    
    if (daemon == "watchdog" || daemon == "all") {
    	LOG("isDaemonAlive() - Checking daemon (${daemon}) in 'watchdog'", 4, null, "trace")
        def maxInterval = atomicState.watchdogInterval + 2
        LOG("isDaemonAlive(watchdog) - timeSinceLastScheduledWatchdog=(${timeSinceLastScheduledWatchdog})  Timestamps: (${atomicState.lastScheduledWatchdogDate}) (epic: ${state.lastScheduledWatchdog}) now-(${now()})", 4, null, "trace")
        if ( timeSinceLastScheduledWatchdog >= maxInterval ) { result = false }
    }
    
	if (!daemonList.contains(daemon) ) {
    	// Unkown option passed in, gotta punt
        LOG("isDaemonAlive() - Unknown daemon: ${daemon} received. Do not know how to check this daemon.", 1, null, "error")
        result = false
    }
    LOG("isDaemonAlive() - result is ${result}", 4, null, "trace")
    return result
}

private def Boolean spawnDaemon(daemon="all", unsched=true) {
	// Daemon options: "poll", "auth", "watchdog", "all"    
    def daemonList = ["poll", "auth", "watchdog", "all"]
    
    daemon = daemon.toLowerCase()
    def result = true
    
    if (daemon == "poll" || daemon == "all") {
    	LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'poll'", 4, null, "trace")
        // Reschedule the daemon
        try {
            if( unsched ) { unschedule("pollScheduled") }
            if ( canSchedule() ) { 
            	LOG("Polling Interval == ${atomicState.pollingInterval}", 4)
            	if (atomicState.pollingInterval == 1 || atomicState.pollingInterval == 3) {
                	LOG("Using schedule instead of runEvery with atomicState.pollingInterval: ${atomicState.pollingInterval}", 4)
                	schedule("* 0/${atomicState.pollingInterval} * * * ?", "pollScheduled")                    
                } else {
                	LOG("Using runEvery to setup polling with atomicState.pollingInterval: ${atomicState.pollingInterval}", 4)
        			"runEvery${atomicState.pollingInterval}Minutes"("pollScheduled")
                }
            	result = pollScheduled() && result
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

// Called by scheduled() event handler
def pollScheduled() {
	updateLastPoll(true)
	LOG("pollScheduled() - Running at ${atomicState.lastScheduledPollDate} (epic: ${atomicState.lastScheduledPoll})", 3, null, "trace")    
    return poll()
}


// Called during initialization to get the inital poll
def pollInit() {
	LOG("pollInit()", 5)
    atomicState.forcePoll = true // Initialize the variable and force a poll even if there was one recently    
	pollChildren(null) // Hit the GoThings API for update on all thermostats
}

def pollChildren(child = null) {
	def results = true   
   LOG("=====> pollChildren() - atomicState.forcePoll(${atomicState.forcePoll})  atomicState.lastPoll(${atomicState.lastPoll})  now(${now()})  atomicState.lastPollDate(${state.lastPollDate})")

	LOG("=====> pollChildren() - atomicState.forcePoll(${atomicState.forcePoll})  atomicState.lastPoll(${atomicState.lastPoll})  now(${now()})  atomicState.lastPollDate(${state.lastPollDate})", 4, child, "trace")
    
	if(apiConnected() == "lost") {
    	// Possibly a false alarm? Check if we can update the token with one last fleeting try...
        LOG("pollChildren() apiConnected() == lost, try to do a recovery, else we are done...")
        if( refreshAuthToken() ) { 
        	// We are back in business!
			LOG("pollChildren() - Was able to recover the lost connection. Please ignore any notifications received.", 1, child, "error")
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
    
   // Check to see if it is time to do an full poll to the GoThings servers. If so, execute the API call and update ALL children
    def timeSinceLastPoll = (atomicState.forcePoll == true) ? 0 : ((now() - atomicState.lastPoll?.toDouble()) / 1000 / 60) 
    LOG("Time since last poll? ${timeSinceLastPoll} -- atomicState.lastPoll == ${atomicState.lastPoll}", 3, child, "info")
    
    if ( (atomicState.forcePoll == true) || ( timeSinceLastPoll > getMinMinBtwPolls().toDouble() ) ) {
    	// It has been longer than the minimum delay OR we are doing a forced poll
        LOG("Calling the GoTHings API to fetch the latest data...", 4, child)
	   LOG("Calling the GoTHings API to fetch the latest data..., ${getChildThermostatDeviceIdsString()}")

    	pollGoThingsAPI(getChildThermostatDeviceIdsString())  // This will update the values saved in the state which can then be used to send the updates
	} else {
        LOG("pollChildren() - Not time to call the API yet. It has been ${timeSinceLastPoll} minutes since last full poll.", 4, child)
       LOG("pollChildren() - Not time to call the API yet. It has been ${timeSinceLastPoll} minutes since last full poll.")

        generateEventLocalParams() // Update any local parameters and send
    }
	
	// Iterate over all the children
	def d = getChildDevices()
    LOG("pollChildren() d = - ${d}")

    d?.each() { oneChild ->
    	LOG("pollChildren() - Processing poll data for child: ${oneChild} has ${oneChild.capabilities}", 4)
       LOG("pollChildren() - Processing poll data for child: ${oneChild} has ${oneChild.capabilities}")

    	if( oneChild.hasCapability("Thermostat") ) {
        	// We found a Thermostat, send all of its events
            LOG("pollChildren() - We found a Thermostat!", 5)
		    LOG("pollChildren()- thermostats ${atomicState.thermostats}")
		    LOG("pollChildren()- key ${oneChild.device.deviceNetworkId}")
			LOG("pollChildren()- data ${atomicState.thermostats[oneChild.device.deviceNetworkId]}")
			LOG("pollChildren()- data present? ${atomicState.thermostats[oneChild.device.deviceNetworkId]?.data}")
            oneChild.generateEvent(atomicState.thermostats[oneChild.device.deviceNetworkId]?.data)
        } else {
        	// We must have a remote sensor
            LOG("pollChildren() - Updating sensor data for ${oneChild}: ${oneChild.device.deviceNetworkId} data: ${atomicState.remoteSensorsData[oneChild.device.deviceNetworkId]?.data}", 4)
            oneChild.generateEvent(atomicState.remoteSensorsData[oneChild.device.deviceNetworkId]?.data)
        } 
    }
    return results
}

private def generateEventLocalParams() {
	// Iterate over all the children
    LOG("Entered generateEventLocalParams() ", 5)
	LOG("Entered generateEventLocalParams() ")

	def d = getChildDevices()
    d?.each() { oneChild ->
    	LOG("generateEventLocalParams() - Processing poll data for child: ${oneChild} has ${oneChild.capabilities}", 4)
            	LOG("generateEventLocalParams() - Processing poll data for child: ${oneChild} has ${oneChild.capabilities}")

    	if( oneChild.hasCapability("Thermostat") ) {
        	// We found a Thermostat, send local params as events
            LOG("generateEventLocalParams() - We found a Thermostat!")
            def data = [
            	apiConnected: apiConnected(),
				status: "active"
            ]
            
            atomicState.thermostats[oneChild.device.deviceNetworkId]?.data?.apiConnected = apiConnected()            
            oneChild.generateEvent(data)
        } else {
        	// We must have a remote sensor
            LOG("generateEventLocalParams() - Updating sensor data: ${oneChild.device.deviceNetworkId}")
			// No local params to send            
        } 
    }
}

private def pollGoThingsAPI(thermostatIdsString = "") {
	LOG("=====> pollGoThingsAPI() entered - thermostatIdsString = ${thermostatIdsString}", 2, null, "info")
	 LOG("=====> pollGoThingsAPI() entered - thermostatIdsString = ${thermostatIdsString}")

	atomicState.forcePoll = false

 
    def result = false
	
	def pollParams = [
			uri: apiEndpoint,
			path: "/devices?code=${atomicState.authToken}",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
			query: [format: 'json', body: jsonRequestBody]
	]
	def pollURL = "unset"
	if (thermostatIdsString == ""){

        pollURL = "${apiEndpoint}/devices?code=${ atomicState.authToken}"
		LOG("pollGoThingsAPI()-->pollURL: ${pollURL}  matched if  at thermostatIdsString ${thermostatIdsString}is empty" )
		
	} else{
	    pollURL = "${apiEndpoint}/devices/${thermostatIdsString}?code=${ atomicState.authToken}"
		LOG("pollGoThingsAPI()-->pollURL: ${pollURL}  matched else  at thermostatIdsString ${thermostatIdsString}, not empty" )


	}
    LOG("pollGoThingsAPI()-->pollURL: ${pollURL}")
 	try{
		httpGet(uri: pollURL) { resp ->
			if(resp.status == 200) {
				//LOG("poll results returned resp.data ${resp.data}", 2)
			   LOG("pollGoThingsAPI returned resp  ${resp}" )
			   LOG("pollGoThingsAPI returned resp.data ${resp.data}" )
			   LOG("pollGoThingsAPI returned resp.data.data ${resp.data.data}" )

				//atomicState.remoteSensors = resp.data.data //resp.data.thermostatList.remoteSensors

				atomicState.thermostatData = resp.data.data
				LOG("pollGoThingsAPI -- atomicState.thermostatData  ${atomicState.thermostatData }" )

                updateLastPoll()
               
                // Update the data and send it down to the devices as events                
				LOG("pollGoThingsAPI -- going to updateThermostatData " )

 				updateThermostatData()                
				result = true
                LOG("pollGoThingsAPI -- updateThermostatData  completed" )

                if (apiConnected() != "full") {
					apiRestored()
                    generateEventLocalParams() // Update the connection status
                    LOG("pollGoThingsAPI -- generateEventLocalParams  completed" )
					
                }
 				LOG("pollGoThingsAPI httpGet: termostats ${atomicState.thermostats} ")

			} else {
				LOG("pollGoThingsAPI() - polling children & got http status ${resp.status}", 1, null, "error")

				//refresh the auth token
				if (resp.status == 500 && resp.data.status.code == 14) {
					LOG("Resp.status: ${resp.status} Status Code: ${resp.data.status.code}. Unable to recover", 1, null, "error")
                    // Should not possible to recover from a code 14 but try anyway?
                    
                    apiLost("pollGoThingsAPI() - Resp.status: ${resp.status} Status Code: ${resp.data.status.code}. Unable to recover.")
				}
				else {
					LOG("pollGoThingsAPI() - Other responses received. Resp.status: ${resp.status} Status Code: ${resp.data.status.code}.", 1, null, "error")
				}
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {    
        LOG("pollGoThingsAPI()  HttpResponseException occured. Exception info: ${e} StatusCode: ${e.statusCode}")
        result = false
         if (e.response.data.status.code == 14) {
            atomicState.action = "pollChildren"
            LOG( "pollGoThingsAPI ==> Refreshing your auth_token! in if exception")
            if ( refreshAuthToken() ) { result = true } else { result = false }
        }
    } catch (java.util.concurrent.TimeoutException e) {
    	LOG("pollGoThingsAPI(), TimeoutException: ${e}.")
        // Do not add an else statement to run immediately as this could cause an long looping cycle if the API is offline
        if ( canSchedule() ) { runIn(atomicState.reAttemptInterval, "pollChildren") }
        result = false    
    } catch (Exception e) {
    // TODO: Handle "org.apache.http.conn.ConnectTimeoutException" as this is a transient error and shouldn't count against our retries
		LOG("pollGoThingsAPI(): General Exception: ${e}.")
        atomicState.reAttemptPoll = atomicState.reAttemptPoll + 1
        if (atomicState.reAttemptPoll > 3) {        
        	apiLost("Too many retries (${atomicState.reAttemptPoll - 1}) for polling.")
            return false
        } else {
        	LOG("pollGoThingsAPI() ==> in exception else Setting up retryPolling")
			def reAttemptPeriod = 15 // in sec
        	if ( canSchedule() ) {
            	runIn(atomicState.reAttemptInterval, "refreshAuthToken") 
			} else { 
            	LOG("Unable to schedule refreshAuthToken, running directly")
            	refreshAuthToken() 
            }
        }    	
    }
    LOG("<===== Leaving pollGoThingsAPI() results: ${result}" )
   
	return result
    
}

// poll() will be called on a regular interval using an runEveryX command
def poll() {		
    LOG("poll() - Running at ${getTimestamp()} (epic: ${now()})", 3, null, "trace")

    // Check to see if we are connected to the API or not
    if (apiConnected() == "lost") {
    	LOG("poll() - apiConnected() returned lost. Unable to poll.", 2, null, "warn")
        return false
    }    
	
	LOG("poll() - Polling children with pollChildren(null)", 4)
	return pollChildren(null) // Poll ALL the children at the same time for efficiency    
}
/*
// TODO: This may not be needed any longer
def availableModes(child) {	
	def tData = atomicState.thermostats[child.device.deviceNetworkId]
    LOG("atomicState.thermostats = ${atomicState.thermostats}", 3, child)
	LOG("Child DNI = ${child.device.deviceNetworkId}", 3, child)
	LOG("Data = ${tData}", 3, child)
	

	if(!tData)
	{
		LOG("ERROR: Device connection removed? no data for ${child.device.deviceNetworkId} after polling", 1, child, "error")
		return null
	}

	def modes = ["off"]

	if (tData.data.heatMode) modes.add("heat")
	if (tData.data.coolMode) modes.add("cool")
	if (tData.data.autoMode) modes.add("auto")
    // TODO: replace the use of auxHeatOnly with "emergencyHeat" to conform to the thermostatMode attributes allowed values
    // if (tData.data.auxHeatMode) modes.add("emergency heat")
	if (tData.data.auxHeatMode) modes.add("auxHeatOnly")

	modes
}


def currentMode(child) {
	def tData = atomicState.thermostats[child.device.deviceNetworkId]
	LOG("atomicState.thermostats = ${atomicState.thermostats}", 3, child)
	LOG("Child DNI = ${child.device.deviceNetworkId}", 3, child)
	LOG("Data = ${tData}", 3, child)
    

	if(!tData) {
		LOG("ERROR: Device connection removed? no data for ${child.device.deviceNetworkId} after polling", 1, child, "error")
		return null
	}

	def mode = tData.data.thermostatMode

	mode
}
 */

def updateThermostatData() {
	LOG("updateThermostatData entering")
	atomicState.timeOfDay = getTimeOfDay()
    LOG("updateThermostatData timeOfDay ${atomicState.timeOfDay}")

	// Create the list of thermostats and related data
	 LOG("updateThermostatData them dta ${atomicState.thermostatData}")
	 LOG("updateThermostatData therms ${atomicState.thermostats}")

	atomicState.thermostats = [atomicState.thermostatData].inject([:]) { collector, stat ->
		def dni = [ app.id, stat.ID ].join('.')
	   LOG("updateThermostatData emtered the collector stat protion")

	   LOG("updateThermostatData stat.ID = ${stat.ID}, app.ID = ${app.id} ")

		//LOG("Updating dni $dni, Got weather? ${stat.weather.forecasts[0].weatherSymbol.toString()}")
        //LOG("updateThermostatData Climates available: ${stat.program?.climates}")
        // Extract Climates
        //def climateData = stat.program?.climates
        
        // TODO: Put a wrapper here based on the thermostat brand
        def thermSensor = stat.remoteSensors.find { it.type == "thermostat" }
        def occupancy = "not supported"
        if(!thermSensor) {
			LOG("This particular thermostat does not have a built in remote sensor")
			atomicState.hasInternalSensors = false
        } else {
        	atomicState.hasInternalSensors = true
		    LOG("updateThermostatData() - thermSensor == ${thermSensor}" )
        
		   //def occupancyCap = thermSensor?.capability.find { it.type == "occupancy" }
		   //LOG("updateThermostatData() - occupancyCap = ${occupancyCap} value = ${occupancyCap.value}")
        
		    // Check to see if there is even a value, not all types have a sensor
		   // occupancy =  occupancyCap.value ?: "not supported"
        }
        //LOG("Program data: ${stat.program}  Current climate (ref): ${stat.program?.currentClimateRef}", 4)
        LOG("updateThermostatData() Program data: ${stat.program}   ")

        // Determine if an Event is running, find the first running event
        def runningEvent = null
       LOG("updateThermostatData() checking running events ${stat.events}  ")

        if ( stat.events?.size() > 0 ) {         
        	runningEvent = stat.events.find { 
            	LOG("Checking event: ${it}", 5) 
                it.running == true
            }        	
        }        
        LOG("updateThermostatData() finished checking events stat = ${stat} ")

        //def usingMetric = wantMetric() // cache the value to save the function calls
 	    LOG("updateThermostatData()   tempTemperature = ${tempTemperature}, stat.Reading = ${stat.Reading}")

        def tempWeatherTemperature = stat.Reading
        def currentClimateName = ""
		def currentClimateId = ""
        def currentFanMode = ""
 
        LOG("updateThermostatData() heading to set data")

		def data = [ 
		temperatureScale: getTemperatureScale(),
        lastPoll: atomicState.lastPollDate,
		apiConnected: apiConnected(),
		temperature: 35,
		Reading: stat.Reading,
		status: stat.Status,
		ID: stat.ID,
		actions: stat.Actions,
		thermostatOperatingState: getThermostatOperatingState(stat),
        timeOfDay: atomicState.timeOfDay,

	]

        
		
		LOG("updateThermostatData() Event Data = ${data}" )

		//collector[dni] = [data:data,climateData:climateData]
	    collector[dni] = [data:data ]
	    LOG("updateThermostatData() colector= ${collector}" )
	    LOG("updateThermostatData() colector= ${data}" )

		return collector
	}
	LOG("updateThermostatData() has ended thermostoats = ${atomicState.thermostats}" )
					
}


def getThermostatOperatingState(stat) {
    LOG("getThermostatOperatingState() -  stat = ${stat} , status = ${stat.Status}")

	//def equipStatus = (stat.equipmentStatus.size() > 0) ? stat.equipmentStatus : 'Idle'	
	//equipStatus = equipStatus.trim().toUpperCase()
    
    //LOG("getThermostatOperatingState() - equipStatus == ${equipStatus}" )
     def currentOpState = stat.Reading > 900 ? "No FLAME" : "FLAMES!!!"
	//def currentOpState = equipStatus.contains('HEAT')? 'heating' : (equipStatus.contains('COOL')? 'cooling' : 
    //	equipStatus.contains('FAN')? 'fan only': 'idle')
	return currentOpState
}


def getChildThermostatDeviceIdsString(singleStat = null) {
	if(!singleStat) {
    	LOG("getChildThermostatDeviceIdsString() - !singleStat returning the list for all thermostats", 4, null, "info")
		return thermostats.collect { it.split(/\./).last() }.join(',')
	} else {
    	// Only return the single thermostat
        LOG("Only have a single stat.", 4, singleStat, "debug")
		LOG("Only have a single stat.")

        def goThingsDevId = singleStat.device.deviceNetworkId.split(/\./).last()
        LOG("Received a single thermostat, returning the GoThings Device ID as a String: ${goThingsDevId}", 4, null, "info")
		LOG("Received a single thermostat, returning the GoThings Device ID as a String: ${goThingsDevId}")

        return goThingsDevId    	
    }
}


def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

private refreshAuthToken(child=null) {
	LOG("Entered refreshAuthToken()", 5)	
	LOG("Entered refreshAuthToken()")	

	// Update the timestamp for debugging purposes
	atomicState.lastTokenRefresh = now()
	atomicState.lastTokenRefreshDate = getTimestamp()    
    
	if(!atomicState.refreshToken) {    	
		LOG("refreshAuthToken() - There is no refreshToken stored! Unable to refresh OAuth token.", 1, child, "error")
    	apiLost("refreshAuthToken() - No refreshToken")
        return false
    } else {
		LOG("Performing a refreshAuthToken()")
        
        def refreshParams = [
                method: 'POST',
                uri   : apiEndpoint,
                path  : "/token",
                query : [grant_type: 'refresh_token', code: "${atomicState.refreshToken}", client_id: smartThingsClientId],
        ]

        LOG("refreshParams = ${refreshParams}", 4)
        def refTokenUrl = "${apiEndpoint}/token?refresh_token=${ atomicState.refreshToken}&grant_type=refresh_token&client_id=${smartThingsClientId}&scope=smartRead,smartWrite&redirect_uri${callbackUrl}&client_secret=aabbccdd"
		def jsonMap
        try {            
		   LOG("refreshAuthToken() ==> before  httpPost to  ${refTokenUrl}")

            httpPost(uri: refTokenUrl) { resp ->
				LOG("refreshAuthToken() ==> Inside httpPost resp handling.")
                if(resp.status == 200) {
                    LOG("refreshAuthToken() - 200 Response received - Extracting info." )
				    LOG("refreshAuthToken() ==> resp.data = ${resp.data}  " )

                    atomicState.reAttempt = 0 
                    apiRestored()                    
                    generateEventLocalParams() // Update the connected state at the thermostat devices
				    LOG("refreshAuthToken() ==> coming out of api_redtored and  generateEventLocalParams" )

                    jsonMap = resp.data // Needed to work around strange bug that wasn't updating state when accessing resp.data directly
                    LOG("refreshAuthToken() ==> resp.data = ${resp.data} -- jsonMap is? ${jsonMap}", 4, child)

                    if(jsonMap) {
                        LOG("refreshAuthToken() ==> resp.data == ${resp.data}, jsonMap == ${jsonMap}")
						
                        atomicState.refreshToken = jsonMap.refresh_token
                        
                        // TODO - Platform BUG: This was not updating the state values for some reason if we use resp.data directly??? 
                        // 		  Workaround using jsonMap for authToken                       
                        LOG("atomicState.authToken before: ${atomicState.authToken}")
                        def oldAuthToken = atomicState.authToken
                        atomicState.authToken = jsonMap?.access_token  
						LOG("atomicState.authToken after: ${atomicState.authToken}", 4, child)
                        if (oldAuthToken == atomicState.authToken) { 
                        	LOG("WARN: atomicState.authToken did NOT update properly! This is likely a transient problem.")
						}

                        
                        // Save the expiry time for debugging purposes
                        LOG("refreshAuthToken ==> Expires in ${resp?.data?.expires_in} seconds")
                        atomicState.authTokenExpires = (resp?.data?.expires_in * 1000) + now()
                        LOG("Updated state.authTokenExpires = ${atomicState.authTokenExpires}")

						LOG("Refresh Token = state =${atomicState.refreshToken}  == in: ${resp?.data?.refresh_token}")
                        LOG("OAUTH Token = state ${atomicState.authToken} == in: ${resp?.data?.access_token}")
                        

                        if(atomicState.action && atomicState.action != "") {
                            LOG("Token refreshed. Executing next action: ${atomicState.action}", 3, child)
                            "${atomicState.action}"()

                            // Reset saved action
                            atomicState.action = ""
                            if ( canSchedule() ) runIn(15, "pollChildren")
                        }

                    } else {
                    	LOG("No jsonMap??? ${jsonMap}")
                    }
                    
                    return true
                } else {
                    LOG("Refresh failed ${resp.status} : ${resp.status.code}!")
                    return false
                }
            }
        } catch (groovyx.net.http.HttpResponseException e) {
        	//LOG("refreshAuthToken() - HttpResponseException occured. Exception info: ${e} StatusCode: ${e.statusCode}  response? data: ${e.getResponse()?.getData()}", 1, null, "error")
            LOG("refreshAuthToken() - HttpResponseException occured. Exception info: ${e} StatusCode: ${e.statusCode}")
            if (e.statusCode != 401) {
            	runIn(atomicState.reAttemptInterval, "refreshAuthToken")
            } else if (e.statusCode == 401) {            
				atomicState.reAttempt = atomicState.reAttempt + 1
		        if (atomicState.reAttempt > 3) {                       	
    		    	apiLost("Too many retries (${atomicState.reAttempt - 1}) for token refresh.")        	    
            	    return false
		        } else {
    		    	LOG("Setting up runIn for refreshAuthToken", 4, child)
        			if ( canSchedule() ) {            			
                        runIn(atomicState.reAttemptInterval, "refreshAuthToken") 
					} else { 
    	        		LOG("Unable to schedule refreshAuthToken, running directly")						
	        	    	refreshAuthToken(child) 
    	        	}
        		}
            }
            generateEventLocalParams() // Update the connected state at the thermostat devices
            return false
		} catch (java.util.concurrent.TimeoutException e) {
			LOG("refreshAuthToken(), TimeoutException: ${e}.")
			// Likely bad luck and network overload, move on and let it try again
            if(canSchedule()) { runIn(atomicState.reAttemptInterval, "refreshAuthToken") } else { refreshAuthToken() }            
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

def resumeProgram(child, deviceId) {
	LOG("Entered resumeProgram for deviceID: ${deviceID}", 5, child)
	def result = true
    
    def previousFanMinOnTime = atomicState."previousFanMinOnTime${deviceId}"
    def currentFanMinOnTime = getFanMinOnTime(child)
    def previousHVACMode = atomicState."previousHVACMode${deviceId}"
    def currentHVACMode = getHVACMode(child)
    
    LOG("resumeProgram() - atomicState.previousHVACMode = ${previousHVACMode} current (${currentHVACMode})   atomicState.previousFanMinOnTime = ${previousFanMinOnTime} current (${currentFanMinOnTime})", 4, child)	
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
    def jsonRequestBody = '{"functions":[{"type":"resumeProgram"}],"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"}}'
	LOG("jsonRequestBody = ${jsonRequestBody}", 4, child)
    
	result = sendJson(jsonRequestBody) && result
    LOG("resumeProgram(child) with result ${result}", 3, child)

    return result
}


def spray(child = null){
	LOG("Spary from parent called child.device.id= ${child.device.currentValue('ID')}")

	def returnStatus = false
	def jsonBody = '{"action": "shoot"}'

	def cmdParams = [
			method: 'POST',
			uri: "${apiEndpoint}/devices/${child.device.currentValue('ID')}?code=${atomicState.authToken}",
			headers: ["Content-Type": "application/json"],
			body: jsonBody,
	]
	LOG("Spary from parent calling the action on device")

	
	try{
		httpPostJson(cmdParams) { resp ->
			
			LOG("spray() resp.status ${resp.status}, resp.data: ${resp.data}, returnStatus: ${returnStatus}")
				
           	// TODO: Perhaps add at least two tries incase the first one fails?
			if(resp.status == 200) {				
				LOG("Updated ${resp.data}")
				if (resp.data.scuccess == "yes") {
					LOG("Successful call to go API.", 2, child)
					apiRestored()
                    generateEventLocalParams()
				} else {
					LOG("Error return code = ${resp.data}", 1, child, "error")
				}
            
			} else {
            	// Should never get here as a non-200 response is supposed to trigger an Exception
   	        	LOG("Sent Json & got http status ${resp.status} - ${resp.status.code}", 2, child, "warn")
			} 
		}  
    } catch(Exception e) {
    	// Might need to further break down 
		LOG("spray() - Exception Sending Json: " + e, 1, child, "error")
	}
	

}

 

private def getChildThermostatName() { return "GoThings" }
private def getChildSensorName()     { return "GoThings Sensor" }
private def getServerUrl()           { return "https://graph.api.smartthings.com" }
private def getShardUrl()            { return getApiServerUrl() }
private def getCallbackUrl()         { return "${serverUrl}/oauth/callback" }
private def getBuildRedirectUrl()    { return "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${shardUrl}" }
private def getApiEndpoint()         { return "http://d5d9e88d.ngrok.io" }

// This is the API Key from the go things
private def getSmartThingsClientId() { 
	if(!appSettings.clientId) {
		return "1234"		
	} else {
		return appSettings.clientId 
    }
}


private def LOG(message, level=3, child=null, logType="debug", event=false, displayEvent=true) {
	def prefix = ""
    def logTypes = ["error", "debug", "info", "trace", "warn"]

    if(!logTypes.contains(logType)) {
    	log.error "LOG() - Received logType ${logType} which is not in the list of allowed types."
        if (event && child) { debugEventFromParent(child, "LOG() - Received logType ${logType} which is not in the list of allowed types.") }
        logType = "debug"
    }
    
    if ( logType == "error" ) { 
    	atomicState.lastLOGerror = "${message} @ ${getTimestamp()}"
        atomicState.LastLOGerrorDate = getTimestamp()        
	}
    // if ( debugLevel(0) ) { return }
	if ( debugLevel(5) ) { prefix = "LOG: " }
	if ( debugLevel(level) ) { 
    	log."${logType}" "${prefix}${message}"        
        if (event) { debugEvent(message, displayEvent) }
        if (child) { debugEventFromParent(child, message) }
	}    
	        if (event) { debugEvent("event ", displayEvent) }
        if (child) { debugEventFromParent(child, "child event ") }
}

private def debugEvent(message, displayEvent = false) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent (results)
}

private def debugEventFromParent(child, message) {
	LOG("Debug event from ")        

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
    
    if (atomicState.timeSendPush) {
        if ( (now() - state.timeSendPush) >= (1000 * 60 * 60 * 1)) { // notification is sent to remind user no more than once per hour
            sendPush("Your GoThings thermostat " + notificationMessage)
            sendActivityFeeds(notificationMessage)
            atomicState.timeSendPush = now()
        }
    } else {
        sendPush("Your GoThings thermostat " + notificationMessage)
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

/*
def wantMetric() {
	return (getTemperatureScale() == "C")
}*/


// Establish the minimum amount of time to wait to do another poll
private def  getMinMinBtwPolls() {
    // TODO: Make this configurable in the SmartApp
	return 1
}

private def getPollingInterval() {
	// return (settings.pollingInterval?.toInteger() >= 5) ? settings.pollingInterval.toInteger() : 5
    return settings.pollingInterval?.toInteger()
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

// Are we connected with the GotThings service?
private String apiConnected() {
	// values can be "full", "warn", "lost"
	if (atomicState.connected == null) atomicState.connected = "warn"
	return atomicState.connected?.toString() ?: "lost"
}


private def apiRestored() {
	atomicState.connected = "full"
	unschedule("notifyApiLost")
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
    LOG("apiLost() - ${where}: Lost connection with APIs. unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with GoThings by running the SmartApp and logging in again", 1, null, "error")
    atomicState.apiLostDump = getDebugDump()
    if (apiConnected() == "lost") {
    	LOG("apiLost() - already in lost atomicState. Nothing else to do. (where= ${where})", 5, null, "trace")
        return
    }
   
    // provide cleanup steps when API Connection is lost
	def notificationMessage = "is disconnected from SmartThings/GoThngs, because the access credential changed or was lost. Please go to the GoThings (Connect) SmartApp and re-enter your account login credentials."
    atomicState.connected = "lost"
    atomicState.authToken = null
    
    sendPushAndFeeds(notificationMessage)
	generateEventLocalParams()

    LOG("Unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with GoThings by running the SmartApp and logging in again", 0, null, "error")
    
    // Notify each child that we lost so it gets logged
    if ( debugLevel(3) ) {
    	def d = getChildDevices()
    	d?.each { oneChild ->
        	LOG("apiLost() - notifying each child: ${oneChild} of loss", 0, child, "error")
		}
    }
    
    unschedule("pollScheduled")
    unschedule("scheduleWatchdog")
    runEvery3Hours("notifyApiLost")
}

def notifyApiLost() {
	def notificationMessage = "is disconnected from SmartThings/GoThings, because the access credential changed or was lost. Please go to the GoThings (Connect) SmartApp and re-enter your account login credentials."
    if ( atomicState.connected == "lost" ) {
    	generateEventLocalParams()
		sendPushAndFeeds(notificationMessage)
        LOG("notifyApiLost() - API Connection Previously Lost. User MUST reintialize the connection with GoThings by running the SmartApp and logging in again", 0, null, "error")
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

private getFanMinOnTime(child) {
	LOG("getFanMinOnTime() - Looking up current fanMinOnTime for ${child}", 4, child)
    def devId = getChildThermostatDeviceIdsString(child)
    LOG("Looking for GoThings thermostat ${devId}", 5, child, "trace")
    
    def therm = atomicState.thermostatData?.thermostatList?.find { it.ID.toString() == devId.toString() }
    def fanMinOnTime = therm.settings?.fanMinOnTime
    LOG("fanMinOnTime found is ${fanMinOnTime} for therm ${therm?.ID}", 4, child)
	return fanMinOnTime
}

private getHVACMode(child) {
	LOG("Looking up current hvacMode for ${child}", 4, child)
    def devId = getChildThermostatDeviceIdsString(child)
    LOG("Looking for GoThings thermostat ${devId}", 5, child, "trace")
    
    def therm = atomicState.thermostatData?.thermostatList?.find { it.ID.toString() == devId.toString() }
    def hvacMode = therm.settings?.hvacMode
    LOG("hvacMode found is ${hvacMode} for therm ${therm?.ID}", 4, child)
	return hvacMode
}


def getAvailablePrograms(thermostat) {
	// TODO: Finish implementing this!
    LOG("Looking up the available Programs for this thermostat (${thermostat})", 4)
    def devId = getChildThermostatDeviceIdsString(thermostat)
    LOG("Looking for GoThings thermostat ${devId}", 5, thermostat, "trace")
    
    def therm = atomicState.thermostatData?.thermostatList?.find { it.ID.toString() == devId.toString() }
    def climates = therm?.program?.climates
    
    return climates?.collect { it.name }
}


private def whatHoldType() {
	def sendHoldType = settings.holdType ? (settings.holdType=="Temporary" || settings.holdType=="Until Next Program")? "nextTransition" : (settings.holdType=="Permanent" || settings.holdType=="Until I Change")? "indefinite" : "indefinite" : "indefinite"
	LOG("Entered whatHoldType() with ${sendHoldType}  settings.holdType == ${settings.holdType}")
	 
    return sendHoldType
}

private debugLevel(level=3) {
	// log.trace("debugLevel() -- settings.debugLevel == ${settings.debugLevel}")
    if(settings.debugLevel == "0") { 
    	// log.trace("debugLevel() - debugLvlNum == 0 triggered")
    	return false 
	}
    
	def debugLvlNum = settings.debugLevel?.toInteger() ?: 3
    def wantedLvl = level?.toInteger()    
    // log.trace("debugLvlNum = ${debugLvlNum}; wantedLvl = ${wantedLvl}")
	return ( debugLvlNum >= wantedLvl )
    
}


// Mark the poll data as "dirty" to allow a new API call to take place
private def dirtyPollData() {
	LOG("dirtyPollData() called to reset poll state", 5)
	atomicState.forcePoll = true
}