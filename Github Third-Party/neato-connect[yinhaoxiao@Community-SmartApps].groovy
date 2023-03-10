/**
 *  Neato (Connect)
 *
 *  Copyright 2016 Alex Lee Yuk Cheung
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  VERSION HISTORY
 *	19-01-2018:	1.2.3 - Allow contact sensors to trigger clean if conditions are met.
 *	17-01-2018:	1.2.2 - Allow contact sensors to restrict Botvac start.
 *	06-01-2018:	1.2.1e - Fix null pointer exception on new installations.
 *	05-01-2018:	1.2.1d - Another attempt to remove null reference when Botvac is removed.
 *	05-01-2018:	1.2.1c - Attempt to remove null reference when Botvac is removed.
 *	14-10-2017:	1.2.1b - Fix to setting Smart Home Monitor.
 *	20-09-2017:	1.2.1 BETA - Allow option for a SmartSchedule 'day' be measured from midnight rather than last cleaning time.
 *	06-07-2017: 1.2h - Bug fix. Fix to smart schedule event handler typo preventing SHM mode changing. Fix to allow delayed start for multiple botvacs.
 *	30-05-2017: 1.2g - Bug fix. Null botvac ID generated when no trigger smart schedule is set.
 *	23-03-2017: 1.2f - Bug fix. Neato Botvac null pointer when start delay is set.
 *	16-03-2017: 1.2e - Bug fix. Enforce single instance of app.
 *  16-03-2017: 1.2d - Bug fix. Schedule not reset automatically when clean starts in some scenarios.
 *					 - Bug fix. Switch triggers not working.
 *  06-03-2017: 1.2c - Bug fix. Schedule ignored when SS notifications are turned off for mode and switch triggers.
 *  02-03-2017: 1.2b - Critical error fix that stopped cleaning completely.
 *  23-02-2017: 1.2 - Add delay option for clean when using Mode as trigger. Add option to disable notification before scheduled clean.
 *  27-01-2017: 1.2 BETA Release 2 - Fix to scheduler.
 *  25-01-2017: 1.2 BETA Release 1b - Minor fix to SmartSchedule menus.
 *  24-01-2017: 1.2 BETA Release 1 - Individual SmartSchedule for each Botvac. (Loses SmartSchedule from earlier versions).
 *
 *  17-01-2017: 1.1.7b - Clean up display and formatting for multiple Botvacs.
 *  12-01-2017: 1.1.7 - Add authentication scope for Maps. Added reauthentication option.
 *
 *  26-11-2016: 1.1.6 - Enforce SHM mode if SHM is changed during a clean.
 *
 *  01-11-2016: 1.1.5 - Improved handling of lost credentials to Neato. Better time zone handling.
 *
 *	24-10-2016: 1.1.4b - Bug fix. Override switch handler fix to prevent false negatives. 
 *	23-10-2016: 1.1.4 - Improve error notification from device status.
 *
 *	21-10-2016: 1.1.3b - Force poll on settings update.
 *	20-10-2016: 1.1.3 - Allow device handler to display smart scheduling information.
 *
 *	20-10-2016: 1.1.2b - Bug fix. SmartSchedule does not operate if force clean option is disabled.
 *	19-10-2016:	1.1.2 - Option to specify "no trigger" in SmartSchedule. Notification when Force clean is due in 24 hours.
 						Separate Smart schedule time markers from force clean time markers.
 *
 *	19-10-2016:	1.1.1b - Unschedule auto dock if cleaning is resumed.
 *	18-10-2016: 1.1.1 - Allow smart schedule to also be triggered on presence and switch events. Add option to specify how override switches work (all or any).
 *
 *	18-10-2016: 1.1d - Bug fix. Custom state validation errors and error saving page message when upgrading from 1.0 to 1.1.
 *	18-10-2016: 1.1c - Bug fix. Smart schedule was not updating last clean time properly when Botvac was activated.
 *	17-10-2016: 1.1b - Set last clean value to new devices for smart schedule.
 *	17-10-2016: 1.1 - SmartSchedule functionality and minor fixes 
 *
 *	15-10-2016: 1.0c - Fix to auto SHM mode not triggering
 *	14-10-2016: 1.0b - Minor fix to preference list
 *	14-10-2016: 1.0 - Initial Version
 */
definition(
    name: "Neato (Connect)",
    namespace: "alyc100",
    author: "Alex Lee Yuk Cheung",
    description: "Integration to Neato Robotics Connected Series robot vacuums",
    category: "",
    iconUrl: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png",
    iconX2Url: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png",
    iconX3Url: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png",
    oauth: true,
    singleInstance: true)

{
	appSetting "clientId"
	appSetting "clientSecret"
}


preferences {
	page(name: "auth", title: "Neato", nextPage:"", content:"authPage", uninstall: true, install:true)
    page(name: "selectDevicePAGE")
    page(name: "preferencesPAGE")
    page(name: "notificationsPAGE")
    page(name: "smartSchedulePAGE")
    page(name: "timeIntervalPAGE")
}

mappings {
    path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
    path("/oauth/callback") {action: [GET: "callback"]}
}

def authPage() {
    log.debug "authPage()"

	if(!atomicState.accessToken) { //this is to access token for 3rd party to make a call to connect app
		atomicState.accessToken = createAccessToken()
	}

	def description
	def uninstallAllowed = false
	def oauthTokenProvided = false

	if(atomicState.authToken) {
		description = "You are connected."
		uninstallAllowed = true
		oauthTokenProvided = true
	} else {
		description = "Click to enter Neato Credentials"
	}

	def redirectUrl = buildRedirectUrl
	log.debug "RedirectUrl = ${redirectUrl}"
	// get rid of next button until the user is actually auth'd
	if (!oauthTokenProvided) {
		return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall:uninstallAllowed) {
        	section { headerSECTION() }
			section() {
				paragraph "Tap below to log in to the Neato service and authorize SmartThings access."
				href url:redirectUrl, style:"embedded", required:true, title:"Neato", description:description
			}
		}
    } else {
		updateDevices()
        //Disable push option if contact book is enabled
   	 	if (location.contactBookEnabled) {
    		settings.sendPush = false
    	}
        
        dynamicPage(name: "auth", uninstall: false, install: false) {
        	section { headerSECTION() }
            
			section ("Choose your Neato Botvacs:") {
				href("selectDevicePAGE", title: null, description: devicesSelected() ? "Devices:" + getDevicesSelectedString() : "Tap to select your Neato Botvacs", state: devicesSelected())
        	}
            if (devicesSelected() == "complete") {
           		section ("SmartSchedule Configuration:") {
					if (selectedBotvacs.size() > 0) {
                		selectedBotvacs.each() {
                            //Migrate settings from v1.1 and earlier to v1.1.1
        					if (settings["smartScheduleEnabled#$it"] && settings["ssScheduleTrigger#$it"] == null) {
        						settings["ssScheduleTrigger#$it"] = "mode"
        					}
                            def ssEnabled = smartScheduleSelected(it)
                            href("smartSchedulePAGE", params: ["botvacId": it], title: "SmartSchedule for ${state.botvacDevices[it]}", description: settings["smartScheduleEnabled#$it"] ? "${getSmartScheduleString(it)}" : "Tap to configure SmartSchedule for ${state.botvacDevices[it]}", state: ssEnabled, required: false, submitOnChange: false)
        				}
                	}
                }
            	section ("Preferences:") {
					href("preferencesPAGE", title: null, description: preferencesSelected() ? getPreferencesString() : "Tap to configure preferences", state: preferencesSelected())
        		}
           	 	section ("Notifications:") {
					href("notificationsPAGE", title: null, description: notificationsSelected() ? getNotificationsString() : "Tap to configure notifications", state: notificationsSelected())
        		}
               
        		def botvacList = ""
                section("Botvac Status:") {
                	getChildDevices().each { childDevice -> 
						try {
                            paragraph image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/neato_botvac_image.png", "${childDevice.displayName} is ${childDevice.currentStatus}. Battery is ${childDevice.currentBattery}%"
						}
        				catch (e) {
           					log.trace "Error checking status."
            				log.trace e
        				}
					}
                }
           	}
            section() {
				paragraph "Tap below to reauthenticate to the Neato service and reauthorize SmartThings access."
				href url:redirectUrl, style:"embedded", required:false, title:"Neato", description:description
			}
        }
	}
}

def selectDevicePAGE() {
	updateDevices()
	dynamicPage(name: "selectDevicePAGE", title: "Devices", uninstall: false, install: false) {
    	section { headerSECTION() }
    	section() {
			paragraph "Tap below to see the list of Neato Botvacs available in your Neato account and select the ones you want to connect to SmartThings."
    		input "selectedBotvacs", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/neato_botvac_image.png", required:false, title:"Select Neato Devices \n(${state.botvacDevices.size() ?: 0} found)", multiple:true, options:state.botvacDevices
        }
    }
}

def smartSchedulePAGE(params) {
	log.debug "PARAMS: $params"
    if (params.containsKey("botvacId")) state.configBotvacId = params?.botvacId
	def botvacId = state.configBotvacId
	return dynamicPage(name: "smartSchedulePAGE", title: "SmartSchedule for ${state.botvacDevices[botvacId]}", install: false, uninstall: false) { 
    	section() {
        	paragraph "Configure a dymanic schedule for your Botvac so that it can clean on a regular interval but based on mode, presence sensor or switch triggers."
        	input "smartScheduleEnabled#$botvacId", "bool", title: "Enable SmartSchedule?", required: false, defaultValue: false, submitOnChange: true
        }
            if (settings["smartScheduleEnabled#$botvacId"]) {
            	section() {
            		input ("ssEnableWarning#$botvacId", "bool", title: "Enable schedule notification before cleaning", required: false, defaultValue: true)
                }
            	section("Configure your cleaning interval and schedule triggers:") {
        			//SmartSchedule configuration options.
                	//Configure regular cleaning interval in days
                	input ("ssCleaningInterval#$botvacId", "number", title: "Set your ideal cleaning interval in days", required: true, defaultValue: 3)
                    
                    //Define when day should be mesaured
                    paragraph "[BETA] If enabled then a day is calculated from midnight before the last clean."
                    input ("ssIntervalFromMidnight#$botvacId", "bool", title: "Measure day interval from midnight before last clean?", required: false, defaultValue: false)
                    
                    //Define smart schedule trigger
                    input("ssScheduleTrigger#$botvacId", "enum", title: "How do you want to trigger the schedule?",  multiple: false, required: true, submitOnChange: true, options: ["mode": "Away Modes", "switch": "Switches", "presence": "Presence", "none": "No Triggers"])
        
                    //Define your away modes
                    if (settings["ssScheduleTrigger#$botvacId"] == "mode") { 
                    	input ("ssAwayModes#$botvacId", "mode", title:"Specify your away modes:", multiple: true, required: true)
                    }
                    if (settings["ssScheduleTrigger#$botvacId"] == "switch") { 
                    	input ("ssSwitchTrigger#$botvacId", "capability.switch", title:"Which switches?", multiple: true, required: true) 
                        input ("ssSwitchTriggerCondition#$botvacId", "enum", title:"Trigger schedule when:", multiple: false, required: true, options: ["any": "Any switch turns on", "all": "All switches are on"], defaultValue: "any") 
                    }
                    if (settings["ssScheduleTrigger#$botvacId"] == "presence") { 
                    	input ("ssPeopleAway#$botvacId", "capability.presenceSensor", title:"Which presence sensors?", multiple: true, required: true) 
                        input ("ssPeopleAwayCondition#$botvacId", "enum", title:"Trigger schedule when:", multiple: false, required: true, options: ["any": "Someone leaves", "all": "Everyone is away"], defaultValue: "all") 
                    }
                    
                    if (settings["ssScheduleTrigger#$botvacId"] != "none") { 
                    	input ("ssStartDelay#$botvacId", "number", title:"Set start delay time (minutes):", required: true, defaultValue: 0) 
                    }
                }
                section("SmartSchedule restrictions:") {
					//Define time of day
                	paragraph "Set SmartSchedule restrictions so that your Botvacs don't start unless below conditions are met."
                    def greyedOutTime = greyedOutTime(settings["starting#$botvacId"], settings["ending#$botvacId"])
                    def timeLabel = getTimeLabel(settings["starting#$botvacId"], settings["ending#$botvacId"])
                	href ("timeIntervalPAGE", params: ["botvacId": botvacId], title: "Operate Botvac only during a certain time", description: timeLabel, state: greyedOutTime, refreshAfterSelection:true)
                	//Define allowed days of operation
                	input ("days#$botvacId", "enum", title: "Operate Botvac only on certain days of the week", multiple: true, required: false,
		         		options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"])
                    //Define contact sensors
                    input ("ssRestrictContactSensors#$botvacId", "capability.contactSensor", title:"Set SmartSchedule restriction contact sensors", multiple: true, required: false, submitOnChange: true)
                    if (settings["ssRestrictContactSensors#$botvacId"]) {
                    	input ("ssRestrictContactSensorsCondition#$botvacId", "enum", title:"Start Botvac only when:", multiple: false, required: true, options: ["allclosed": "All selected contacts are closed", "anyclosed": "Any selected contacts are closed", "allopen": "All selected contacts are open", "anyopen": "Any selected contacts are open"], defaultValue: "allclosed")
                    }
                	
                }
                section("SmartSchedule overrides:") {
                //Define override switches to restart SmartSchedule countdown
                paragraph "Routine override switches/buttons will cancel the next scheduled clean and reset the interval countdown when switched on."
                	input ("ssOverrideSwitch#$botvacId", "capability.switch", title:"Set SmartSchedule override switches", multiple: true, required: false, submitOnChange: true)
                     if (settings["ssOverrideSwitch#$botvacId"]) {
                    	input ("ssOverrideSwitchCondition#$botvacId", "enum", title:"Override schedule when:", multiple: false, required: true, options: ["any": "Any selected switch turns on", "all": "All selected switches are on"], defaultValue: "any") 
                    }
                }
                section("Notifications:") {
                paragraph "Turn on SmartSchedule notifications. You can configure specific recipients via Notification settings section."
                	input "ssNotification", "bool", title: "Enable SmartSchedule notifications?", required: false, defaultValue: true
              	}  
            }
        }
    
}

def timeIntervalPAGE(params) {
	def botvacId = params.botvacId
	return dynamicPage(name: "timeIntervalPAGE", title: "Only during a certain time", refreshAfterSelection:true) {
		section {
			input "starting#$botvacId", "time", title: "Starting", required: false
			input "ending#$botvacId", "time", title: "Ending", required: false
		}
	}
}

def notificationsPAGE() {
	return dynamicPage(name: "notificationsPAGE", title: "Notifications", install: false, uninstall: false) {   
		section(){
        	input("recipients", "contact", title: "Send notifications to", required: false, submitOnChange: true) {
				input "sendPush", "bool", title: "Send notifications via Push?", required: false, defaultValue: false, submitOnChange: true
            }
            input "sendSMS", "phone", title: "Send notifications via SMS?", required: false, defaultValue: null, submitOnChange: true
            if ((location.contactBookEnabled && settings.recipients) || settings.sendPush || settings.sendSMS != null) {
				input "sendBotvacOn", "bool", title: "Notify when Botvacs are on?", required: false, defaultValue: false
				input "sendBotvacOff", "bool", title: "Notify when Botvacs are off?", required: false, defaultValue: false
				input "sendBotvacError", "bool", title: "Notify on Botvacs have an error?", required: false, defaultValue: true
				input "sendBotvacBin", "bool", title: "Notify when Botvacs have a full bin?", required: false, defaultValue: true
                def smartScheduleEnabled = false
                if (selectedBotvacs.size() > 0) {
                	selectedBotvacs.each() {
                    	if (settings["smartScheduleEnabled#$it"]) smartScheduleEnabled = true
            		}
                }
                if (smartScheduleEnabled) {
            		input "ssNotification", "bool", title: "Enable SmartSchedule notifications?", required: false, defaultValue: true
                }
            }
		}
    }
}

def preferencesPAGE() {
	return dynamicPage(name: "preferencesPAGE", title: "Preferences", install: false, uninstall: false) {   
		
        section("Force Clean"){
        	paragraph "If Botvac has been inactive for a number of days specified, then force a clean."
        	input "forceClean", "bool", title: "Force clean after elapsed time?", required: false, defaultValue: false, submitOnChange: true
            if (forceClean != false) {
        		input ("forceCleanDelay", "number", title: "Number of days before force clean (in days)", required: false, defaultValue: 7)
            }
        }
        section("Auto Dock") {
        	paragraph "When Botvac is paused, automatically send to base after a specified number of seconds."
			input "autoDock", "bool", title: "Auto dock Botvac after pause?", required: false, defaultValue: true, submitOnChange: true
            if (autoDock != false) {
            	input ("autoDockDelay", "number", title: "Auto dock delay after pause (in seconds)", required: false, defaultValue: 60)
            }
		}
		section("Auto Smart Home Monitor..."){
       	 	paragraph "If Smart Home Monitor is set to Arm(Away), auto Set Smart Home Monitor to Arm(Stay) when cleaning and reset when done. If Smart Home Monitor is Disarmed during cleaning, then this will not reactivate SHM."
			input "autoSHM", "bool", title: "Auto set Smart Home Monitor?", required: false, defaultValue: false, submitOnChange: true
			
		}
    }
}

def headerSECTION() {
	return paragraph (image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png",
                  "${textVersion()}")
} 

def oauthInitUrl() {
	log.debug "oauthInitUrl with callback: ${callbackUrl}"

	atomicState.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [
			response_type: "code",
			scope: "public_profile control_robots maps",
			client_id: clientId(),
			state: atomicState.oauthInitState,
			redirect_uri: callbackUrl
	]

	redirect(location: "${apiEndpoint}/oauth2/authorize?${toQueryString(oauthParams)}")
}

// The toQueryString implementation simply gathers everything in the passed in map and converts them to a string joined with the "&" character.
String toQueryString(Map m) {
        return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def callback() {
	log.debug "callback()>> params: $params, params.code ${params.code}"

	def code = params.code
	def oauthState = params.state

	if (oauthState == atomicState.oauthInitState) {
		def tokenParams = [
			grant_type: "authorization_code",
			code      : code,
			client_id : clientId(),
            client_secret: clientSecret(),
			redirect_uri: callbackUrl
		]

		def tokenUrl = "https://beehive.neatocloud.com/oauth2/token?${toQueryString(tokenParams)}"

		httpPost(uri: tokenUrl) { resp ->
			atomicState.refreshToken = resp.data.refresh_token
			atomicState.authToken = resp.data.access_token
		}

		if (atomicState.authToken) {
			success()
		} else {
			fail()
		}

	} else {
		log.error "callback() failed oauthState != atomicState.oauthInitState"
	}

}

// Example success method
def success() {
	def message = """
        <p>Your Neato Account is now connected to SmartThings!</p>
        <p>Click 'Done' to finish setup.</p>
    """
	displayMessageAsHtml(message)
}

def fail() {
	def message = """
        <p>The connection could not be established!</p>
        <p>Click 'Done' to return to the menu.</p>
    """
	displayMessageAsHtml(message)
}

def displayMessageAsHtml(message) {
    def redirectHtml = ""
	if (redirectUrl) { redirectHtml = """<meta http-equiv="refresh" content="3; url=${redirectUrl}" />""" }

	def html = """
		<!DOCTYPE html>
		<html>
		<head>
		<meta name="viewport" content="width=640">
		<title>SmartThings & Neato connection</title>
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
						<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
						<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
						<img src="https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png" alt="neato icon" width="205" />
						${message}
				</div>
		</body>
		</html>
		"""
	render contentType: 'text/html', data: html
}

private refreshAuthToken() {
	log.debug "refreshing auth token"

	if(!atomicState.refreshToken) {
		log.warn "Can not refresh OAuth token since there is no refreshToken stored"
	} else {
		def refreshParams = [
			method: 'POST',
			uri   : "https://beehive.neatocloud.com",
			path  : "/oauth2/token",
			query : [grant_type: 'refresh_token', refresh_token: "${atomicState.refreshToken}"],
		]

		def notificationMessage = "Neato is disconnected from SmartThings, because the access credential changed or was lost. Please go to the Neato (Connect) SmartApp and re-enter your account login credentials."
		//changed to httpPost
		try {
			def jsonMap
			httpPost(refreshParams) { resp ->
				if(resp.status == 200) {
					log.debug "Token refreshed...calling saved RestAction now!"
					saveTokenAndResumeAction(resp.data)
			    }
            }
		} catch (groovyx.net.http.HttpResponseException e) {
			log.error "refreshAuthToken() >> Error: e.statusCode ${e.statusCode}"
			def reAttemptPeriod = 300 // in sec
			if (e.statusCode != 401) { // this issue might comes from exceed 20sec app execution, connectivity issue etc.
				runIn(reAttemptPeriod, "refreshAuthToken")
			} else if (e.statusCode == 401) { // unauthorized
            	if (!atomicState.reAttempt) atomicState.reAttempt = 0
				atomicState.reAttempt = atomicState.reAttempt + 1
				log.warn "reAttempt refreshAuthToken to try = ${atomicState.reAttempt}"
				if (atomicState.reAttempt <= 3) {
					runIn(reAttemptPeriod, "refreshAuthToken")
				} else {
					messageHandler(notificationMessage, true)
                    atomicState.authToken = null
					atomicState.reAttempt = 0
                    
				}
			}
		}
	}
}

private void saveTokenAndResumeAction(json) {
    log.debug "saveTokenAndResumeAction: token response json: $json"
    if (json) {
        atomicState.refreshToken = json?.refresh_token
        atomicState.authToken = json?.access_token
        if (atomicState.action) {
            log.debug "got refresh token, executing next action: ${atomicState.action}"
            "${atomicState.action}"()
        }
    } else {
        log.warn "did not get response body from refresh token response"
    }
    atomicState.action = ""
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
	unschedule()
    //Initialise variables
    if (state.lastClean == null) {
    	state.lastClean = [:]
    }
    if (state.smartSchedule == null) {
    	state.smartSchedule = [:]
    }
    if (state.forceCleanNotificationSent == null) {
    	state.forceCleanNotificationSent = [:]
    }
    if (state.botvacOnTimeMarker == null) {
    	state.botvacOnTimeMarker = [:]
    }
    state.remove("taskStartTimes")
    if (selectedBotvacs) addBotvacs()
    
    getChildDevices().each { childDevice -> 	
    	def botvacId = childDevice.deviceNetworkId
    	//subscribe to events for smartSchedule
    	if (settings["smartScheduleEnabled#$botvacId"]) {
    		//store last mode selected
    		if ((!state.lastTriggerMode) || (state.lastTriggerMode instanceof String)) state.lastTriggerMode = [:]
        
        	if (settings["ssScheduleTrigger#$botvacId"] == "mode") { subscribe(location, "mode", smartScheduleHandler, [filterEvents: false]) }
        	else if (settings["ssScheduleTrigger#$botvacId"] == "switch") { subscribe(settings["ssSwitchTrigger#$botvacId"], "switch.on", smartScheduleHandler, [filterEvents: false]) }
        	else if (settings["ssScheduleTrigger#$botvacId"] == "presence") { subscribe(settings["ssPeopleAway#$botvacId"], "presence", smartScheduleHandler, [filterEvents: false]) }
            
        	subscribe(settings["ssOverrideSwitch#$botvacId"], "switch.on", smartScheduleHandler, [filterEvents: false])
            subscribe(settings["ssRestrictContactSensors#$botvacId"], "contact", smartScheduleHandler, [filterEvents: false])
    	}
    
   		if (state.botvacOnTimeMarker[botvacId] == null) state.botvacOnTimeMarker[botvacId] = now()
        //subscribe to events for notifications if activated
    	if (settings["smartScheduleEnabled#$botvacId"] || preferencesSelected() == "complete" || notificationsSelected() == "complete") {
        	subscribe(childDevice, "status.cleaning", eventHandler, [filterEvents: false])
        }
        if (preferencesSelected() == "complete" || notificationsSelected() == "complete") {
        	subscribe(childDevice, "status.ready", eventHandler, [filterEvents: false])
            subscribe(childDevice, "status.error", eventHandler, [filterEvents: false])
            subscribe(childDevice, "status.paused", eventHandler, [filterEvents: false])
            subscribe(childDevice, "bin.full", eventHandler, [filterEvents: false])
        }
        //initialise force clean flags
        if (settings.forceClean) {
        	if (state.forceCleanNotificationSent[botvacId] == null) state.forceCleanNotificationSent[botvacId] = false
        }
        //subscribe to events for smartSchedule
    	if (settings["smartScheduleEnabled#$botvacId"]) {
        	//Initialize flags for Smart Schedule
            if (state.smartSchedule[botvacId] == null) state.smartSchedule[botvacId] = false
        	if (state.lastClean[botvacId] == null) {
            	if (settings["ssIntervalFromMidnight#$botvacId"]) {
                	state.lastClean[botvacId] = (new Date()).clearTime().getTime()
                } else {
                	state.lastClean[botvacId] = now()
                }
            }
            //Trigger has changed so reset all smart schedule flags
            if ((state.lastTriggerMode.containsKey(botvacId)) && (state.lastTriggerMode[botvacId] != settings["ssScheduleTrigger#$botvacId"])) {
            	log.debug "Smart schedule trigger mode has changed. Resetting smart schedule flag."
            	state.smartSchedule[botvacId] = false
            	state.lastTriggerMode[botvacId] = settings["ssScheduleTrigger#$botvacId"]
            }
        }
        childDevice.poll()
    }
    runIn(getNextTimeInSeconds(), timeHandler)
    runEvery5Minutes('pollOn') // Asynchronously refresh devices so we don't block
    
}

def uninstalled() {
	log.info("Uninstalling, removing child devices...")
	unschedule()
	removeChildDevices(getChildDevices())
}

def updateDevices() {
	log.debug "Executing 'updateDevices'"
	if (!state.devices) {
		state.devices = [:]
	}
	def devices = devicesList()
    state.botvacDevices = [:]
    def selectors = []
	devices.each { device -> 
    	if (device.serial != null) {
        	selectors.add("${device.serial}|${device.secret_key}")
            def value
        	value = "Neato Botvac - " + device.name
			def key = device.serial + "|" + device.secret_key
			state.botvacDevices["${key}"] = value
      	}
	}    
    log.debug "selectors: $selectors"
    //Remove devices if does not exist on the Neato platform
    getChildDevices().findAll { !selectors.contains("${it.deviceNetworkId}") }.each {
		log.info("Deleting ${it.deviceNetworkId}")
        try {
			deleteChildDevice(it.deviceNetworkId)
        } catch (physicalgraph.exception.NotFoundException e) {
        	log.info("Could not find ${it.deviceNetworkId}. Assuming manually deleted.")
        } catch (physicalgraph.exception.ConflictException ce) {
        	log.info("Device ${it.deviceNetworkId} in use. Please manually delete.")
        }
	} 
    if (selectedBotvacs) {
    	selectedBotvacs.retainAll(selectors as Object[])
    }
}

def addBotvacs() {
	log.debug "Executing 'addBotvacs'"
	updateDevices()

	selectedBotvacs.each { device ->
    	
        def childDevice = getChildDevice("${device}")
        
        if (!childDevice) { 
    		log.info("Adding Neato Botvac device ${device}: ${state.botvacDevices[device]}")
            
        	def data = [
                name: state.botvacDevices[device],
				label: state.botvacDevices[device],
			]
            childDevice = addChildDevice(app.namespace, "Neato Botvac Connected Series", "$device", null, data)
            childDevice.refresh()
           
			log.debug "Created ${state.botvacDevices[device]} with id: ${device}"
		} else {
			log.debug "found ${state.botvacDevices[device]} with id ${device} already exists"
		}
	}
}

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}

def devicesList() {
	logErrors([]) {
		def resp = beehiveGET("/users/me/robots")
        def notificationMessage = "Neato is disconnected from SmartThings, because the access credential changed or was lost. Please go to the Neato (Connect) SmartApp and re-enter your account login credentials."
		if (resp.status == 200) {
			return resp.data
		} else if (resp.status == 401) {
        	atomicState.action = "updateDevices"
        	if (!atomicState.reAttempt) atomicState.reAttempt = 0
        	atomicState.reAttempt = atomicState.reAttempt + 1
			log.warn "reAttempt refreshAuthToken to try = ${atomicState.reAttempt}"
			if (atomicState.reAttempt <= 3) {
				runIn(reAttemptPeriod, "refreshAuthToken")
			} else {
				messageHandler(notificationMessage, true)
                atomicState.authToken = null
				atomicState.reAttempt = 0
			}
        }
        else {
        	log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
            runIn(reAttemptPeriod, "refreshAuthToken")
			return []
		}
	}
}

def devicesSelected() {
	return (selectedBotvacs) ? "complete" : null
}

def getDevicesSelectedString() {
	updateDevices()
	def listString = ""
	selectedBotvacs.each { childDevice -> 
        if (null != state.botvacDevices) {
        	listString += "\n• " + state.botvacDevices[childDevice]
        }
    }
    return listString
}

def smartScheduleSelected(botvacId) {
	return settings["smartScheduleEnabled#$botvacId"] ? "complete" : null
}

def getSmartScheduleString(botvacId) {
	def listString = ""
    if (settings["smartScheduleEnabled#$botvacId"]) {
    	listString += "SmartSchedule set for every ${settings["ssCleaningInterval#$botvacId"]} days "
        if (settings["ssScheduleTrigger#$botvacId"] == "mode") {listString += "when mode is ${settings["ssAwayModes#$botvacId"]}."}
        else if (settings["ssScheduleTrigger#$botvacId"] == "switch") {
        	if (settings["ssSwitchTriggerCondition#$botvacId"] == "any") {
            	listString += "when any of ${settings["ssSwitchTrigger#$botvacId"]} turns on."
            } else {
            	listString += "when ${settings["ssSwitchTrigger#$botvacId"]} are all on."
            }
       	}
        
        else if (settings["ssScheduleTrigger#$botvacId"] == "presence") {
        	if (settings["ssPeopleAwayCondition#$botvacId"] == "any") {
            	listString += "when one of ${settings["ssPeopleAway#$botvacId"]} leaves."
            } else {
            	listString += "when ${settings["ssPeopleAway#$botvacId"]} are all away."
            }
        }  
     
        listString += "\n\nThe following restrictions apply:\n"
        if (settings["starting#$botvacId"]) listString += "• ${getTimeLabel(settings["starting#$botvacId"], settings["ending#$botvacId"])}\n" 
        if (settings["days#$botvacId"]) listString += "• Only on ${settings["days#$botvacId"]}.\n"
        if (settings["ssOverrideSwitch#$botvacId"]) {
        	if (settings["ssOverrideSwitchCondition#$botvacId"] == "any") {
            	listString += "• Override schedule if any of ${settings["ssOverrideSwitch#$botvacId"]} turns on."
            } else {
        		listString += "• Override schedule if ${settings["ssOverrideSwitch#$botvacId"]} are all on."
            }
        }
    }
    return listString
}

def preferencesSelected() {
	return (settings.forceClean || settings.autoDock || settings.autoSHM) ? "complete" : null
}

def getPreferencesString() {
	def listString = ""
	if (settings.forceClean) listString += "• Force clean after ${settings.forceCleanDelay} days\n"
  	if (settings.autoDock) listString += "• Auto Dock after ${settings.autoDockDelay} seconds\n"
    if (settings.autoSHM) listString += "• Automatically set Smart Home Monitor\n"
  	
  	if (listString != "") listString = listString.substring(0, listString.length() - 1)
    return listString
}

def notificationsSelected() {
    return ((location.contactBookEnabled && settings.recipients) || settings.sendPush || settings.sendSMS != null) && (settings.sendBotvacOn || settings.sendBotvacOff || settings.sendBotvacError || settings.sendBotvacBin || settings.ssNotification) ? "complete" : null
}

def getNotificationsString() {
	def listString = ""
    if (location.contactBookEnabled && settings.recipients) { 
    	listString += "Send the following notifications to " + settings.recipients
    }
    else if (settings.sendPush) {
    	listString += "Send the following notifications"
    }
    
    if (!settings.recipients && !settings.sendPush && settings.sendSMS != null) {
    	listString += "Send the following SMS to ${settings.sendSMS}"
    }
    else if (settings.sendSMS != null) {
    	listString += " and SMS to ${settings.sendSMS}"
    }
    
    if ((location.contactBookEnabled && settings.recipients) || settings.sendPush || settings.sendSMS != null) {
    	listString += ":\n"
  		if (settings.sendBotvacOn) listString += "• Botvac On\n"
  		if (settings.sendBotvacOff) listString += "• Botvac Off\n"
  		if (settings.sendBotvacError) listString += "• Botvac Error\n"
  		if (settings.sendBotvacBin) listString += "• Bin Full\n"
    	if (settings.ssNotification) listString += "• SmartSchedule\n"
    }
    if (listString != "") listString = listString.substring(0, listString.length() - 1)
    return listString
}

//Beehive API Access
def beehiveGET(path, body = [:]) {
	try {
        log.debug("Beginning API GET: ${beehiveURL(path)}, ${beehiveRequestHeaders()}")

        httpGet(uri: beehiveURL(path), contentType: 'application/json', headers: beehiveRequestHeaders()) {response ->
			logResponse(response)
			return response
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

Map beehiveRequestHeaders() {
	return [
        'Accept': 'application/vnd.neato.nucleo.v1',
        'Content-Type': 'application/*+json',
        'X-Agent': '0.11.3-142',
        'Authorization': "Bearer ${atomicState.authToken}"
    ]
}

def logResponse(response) {
	log.info("Status: ${response.status}")
	log.info("Body: ${response.data}")
}

def logErrors(options = [errorReturn: null, logObject: log], Closure c) {
	try {
		return c()
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error("got error: ${e}, body: ${e.getResponse().getData()}")
		return options.errorReturn
	} catch (java.net.SocketTimeoutException e) {
		log.warn "Connection timed out, not much we can do here"
		return options.errorReturn
	}
}

// Implement event handlers
def eventHandler(evt) {
	log.debug "Executing 'eventHandler' for ${evt.displayName}"
	def msg
    if (evt.value == "paused") {
    log.trace "Setting auto dock for ${evt.displayName}"
    	//If configured, set to dock automatically after one minute.
        if (settings.autoDock) {
        	runIn(settings.autoDockDelay, scheduleAutoDock)
        }
    }
	else if (evt.value == "error") {
    	unschedule(pollOn)
        unschedule(scheduleAutoDock)
        runEvery5Minutes('pollOn')
		sendEvent(linkText:app.label, name:"${evt.displayName}", value:"error",descriptionText:"${evt.displayName} has an error", eventType:"SOLUTION_EVENT", displayed: true)
		log.trace "${evt.displayName} has an error"
		msg = "${evt.displayName} has an error: " + evt.device.latestState('statusMsg').stringValue.minus('HAS A PROBLEM - ')
		if (settings.sendBotvacError) {
        	messageHandler(msg, false)
		}
     }
	 else if (evt.value == "cleaning") {
     	unschedule(pollOn)
        unschedule(scheduleAutoDock)
        //Increase poll interval during cleaning
        schedule("0 0/1 * * * ?", pollOn)
        //Record last cleaning time for device
        log.debug "$evt.device.deviceNetworkId has started cleaning"
        if (settings["ssIntervalFromMidnight#$evt.device.deviceNetworkId"]) {
        	state.lastClean[evt.device.deviceNetworkId] = (new Date()).clearTime().getTime()
        } else {
        	state.lastClean[evt.device.deviceNetworkId] = now()
        }
        state.botvacOnTimeMarker[evt.device.deviceNetworkId] = now()
        log.debug "$evt.device.deviceNetworkId has started cleaning"
        if (settings.forceClean) { state.forceCleanNotificationSent[evt.device.deviceNetworkId] = false }
        //Remove SmartSchedule flag
        state.smartSchedule[evt.device.deviceNetworkId] = false
		sendEvent(linkText:app.label, name:"${evt.displayName}", value:"on",descriptionText:"${evt.displayName} is on", eventType:"SOLUTION_EVENT", displayed: true)
		msg = "${evt.displayName} is on"
		if (settings.sendBotvacOn) {
			messageHandler(msg, false)
		}
        setSHMToStay()
     }
	 else if (evt.value == "full") {
     	unschedule(pollOn)
        runEvery5Minutes('pollOn')
		sendEvent(linkText:app.label, name:"${evt.displayName}", value:"bin full",descriptionText:"${evt.displayName} bin is full", eventType:"SOLUTION_EVENT", displayed: true)
		log.trace "${evt.displayName} bin is full"
		msg = "${evt.displayName} bin is full"
		if (settings.sendBotvacBin) {
			messageHandler(msg, false)
		}
	 }
     else if (evt.value == "ready") {
     	unschedule(pollOn)
        unschedule(scheduleAutoDock)
        runEvery5Minutes('pollOn')
		sendEvent(linkText:app.label, name:"${evt.displayName}", value:"off",descriptionText:"${evt.displayName} is off", eventType:"SOLUTION_EVENT", displayed: true)
		log.trace "${evt.displayName} is off"
		msg = "${evt.displayName} is off"
		if (settings.sendBotvacOff) {
			messageHandler(msg, false)
		}
	}
}

def timeHandler(evt) {
	smartScheduleHandler(evt)
}

def smartScheduleHandler(evt) {
	if (evt != null) {
		log.debug "Executing 'smartScheduleHandler' for ${evt.displayName}"
    } else {
    	log.debug "Executing 'smartScheduleHandler' for scheduled event"
    }
    //Update scheduler
    runIn(getNextTimeInSeconds(), timeHandler)
    getChildDevices().each { childDevice ->
    	def botvacId = childDevice.deviceNetworkId
    	//If switch on for override event
    	if (evt != null && evt.name == "switch") {
    		def switchInList = false
    		for (switchName in settings["ssOverrideSwitch#$botvacId"].name) {
        		if (switchName == evt.device.name) {
            		switchInList = true
            		break
            	}
        	}
        	log.debug "Swtich found in override switch list: $switchInList"
    		if (switchInList) {
    			def executeOverride = true
        		//If override switch condition is ALL...
    			if (settings["ssOverrideSwitchCondition#$botvacId"] == "all") {
        			//Check all switches in override switch settings are on
            		for (switchVal in settings["ssOverrideSwitch#$botvacId"].currentSwitch) {
        				if (switchVal == "off") {
            				executeOverride = false
            				break
        				}
    				}
        		}
        
        		if (executeOverride) {
    				//Reset last clean date to current time
                	resetSmartScheduleForDevice(botvacId)
                    childDevice.poll()
        		}	
        		if (settings.ssNotification) {
        			messageHandler("Neato SmartSchedule has reset schedule for ${childDevice.name} as override switch ${evt.displayName} is on.", false)
        		}
        	}
        }
    	//If mode change event, schedule trigger, contact sensor or presence trigger
    	//Check conditions, time and day have been met and execute clean. If no trigger is specified rely on pollOn method to start clean.
        if (settings["ssScheduleTrigger#$botvacId"] != "none") {
           	def delay = 0
            if (settings["ssStartDelay#$botvacId"]) delay = settings["ssStartDelay#$botvacId"] * 60
            if (delay > 0) {
        		runIn(delay, startConditionalClean, [data: [botvacId: botvacId], overwrite: false])
            } else {
               	startConditionalClean([botvacId: botvacId])
            }
        }
    	  	
    }
}

def scheduleAutoDock() {
	log.debug "Executing 'scheduleAutoDock'"
	getChildDevices().each { childDevice ->
		if (childDevice.latestState('status').stringValue == 'paused') {
			childDevice.dock()
		}
	}
}

def pollOn() {
	log.debug "Executing 'pollOn'"
    
    def activeCleaners = false
    log.debug "Last clean states: ${state.lastClean}"
    log.debug "Smart schedule states: ${state.smartSchedule}"
    log.debug "Botvac ON time markers: ${state.botvacOnTimeMarker}"
	getChildDevices().each { childDevice ->
    	def botvacId = childDevice.deviceNetworkId
    	state.pollState = now()
		childDevice.poll()
        if (childDevice.currentSwitch == "off") {
        	//Update smart schedule state. Create notification when clean is due.
        	if (settings["smartScheduleEnabled#$botvacId"] && state.lastClean != null && state.lastClean[botvacId] != null) { 
        		def t = now() - state.lastClean[botvacId]
            	log.debug "$childDevice.displayName schedule marker at " + state.lastClean[botvacId] + ". ${t/86400000} days has elapsed since. ${settings["ssCleaningInterval#$botvacId"] - (t/86400000)} days to scheduled clean."
            
            	//Set SmartSchedule flag if SmartSchedule has not been set already, interval has elapsed and trigger conditions are not met
            	if ((settings["ssScheduleTrigger#$botvacId"] == "none") && ((settings["ssCleaningInterval#$botvacId"] - (t/86400000)) < 1) && (!state.smartSchedule[botvacId]) && (settings["ssEnableWarning#$botvacId"])) {
            		//hour calculation for notification of next clean
                	state.smartSchedule[botvacId] = true
                	if (settings.ssNotification) {
                		messageHandler("Neato SmartSchedule has scheduled ${childDevice.displayName} for a clean in 24 hours (date and time restrictions permitting). Please clear obstacles and leave internal doors open ready for the clean.", false)
                	}
            	} else if ((!getTriggerConditionsOk(botvacId)) && (t > (settings["ssCleaningInterval#$botvacId"] * 86400000)) && (!state.smartSchedule[botvacId]) && (settings["ssEnableWarning#$botvacId"])) {
            		state.smartSchedule[botvacId] = true
            		if (settings.ssNotification) {
                		def reason = "you're next away"
                    	if (settings["ssScheduleTrigger#$botvacId"] == "switch") { reason = "your selected switches turn on" }
                    	else if (settings["ssScheduleTrigger#$botvacId"] == "presence") { reason = "your selected presence sensors leave"}
                		messageHandler("Neato SmartSchedule has scheduled ${childDevice.displayName} for a clean when " + reason + " (date and time restrictions permitting). Please clear obstacles and leave internal doors open ready for the clean.", false)
                	}
            	}
                //If no trigger has been set for smart schedule, execute clean when interval time has elapsed
        		if ((settings["ssScheduleTrigger#$botvacId"] == "none") && (state.smartSchedule[botvacId] || (!settings["ssEnableWarning#$botvacId"])) && (t > (settings["ssCleaningInterval#$botvacId"] * 86400000))) {
        			startConditionalClean([botvacId: botvacId])
        		}
            }
            //Update force clean state and create notification when clean is due.
            if (settings.forceClean && state.botvacOnTimeMarker != null && state.botvacOnTimeMarker[botvacId] != null) {
            	def t = now() - state.botvacOnTimeMarker[botvacId]
            	log.debug "$childDevice.displayName ON time marker at " + state.botvacOnTimeMarker[botvacId] + ". ${t/86400000} days has elapsed since. ${settings.forceCleanDelay - (t/86400000)} days to force clean."
            	
                //Create 24 hour warning for force clean.
            	if ((state.forceCleanNotificationSent != null) && (!state.forceCleanNotificationSent[botvacId]) && ((settings.forceCleanDelay - (t/86400000)) < 1)) {
            		//Send notification when force clean is due
            		log.debug "Force clean due within 24 hours"
					messageHandler(childDevice.displayName + " has not cleaned for " + (settings.forceCleanDelay - 1) + " days. Forcing a clean in 24 hours. Please clear obstacles and leave internal doors open ready for the clean.", true)
                	state.forceCleanNotificationSent[botvacId] = true
            	}
            
            	//Execute force clean (no conditions need checking)
				if (t > (settings.forceCleanDelay * 86400000)) {
            		log.debug "Force clean activated as ${t/86400000} days has elapsed"
					messageHandler(childDevice.displayName + " has not cleaned for " + settings.forceCleanDelay + " days. Forcing a clean.", true)
                    resetSmartScheduleForDevice(botvacId)
                	childDevice.on()
        		}
       	 	}
        } 
        if (childDevice.currentStatus == "cleaning") {
        	//Search for active cleaners
       		activeCleaners = true
        }
	}
    
    //Set SHM mode depending on whether there are active cleaners.
    if (activeCleaners) {
    	setSHMToStay()
    } else {
    	setSHMToAway()
	}
    
    //If SHM is disarmed because of external event, then disable auto SHM mode
    if (location.currentState("alarmSystemStatus")?.value == "off") {
    	state.autoSHMchange = "n"
    }
}

//Access methods for device type
def isSmartScheduleEnabled(botvacId) {
	return settings["smartScheduleEnabled#$botvacId"]
}

def timeToSmartScheduleClean(botvacId) {
	log.debug "Executing 'timeToSmartScheduleClean' with device $botvacId"
	def result = -1
    if (settings["smartScheduleEnabled#$botvacId"] && state.lastClean != null && state.lastClean[botvacId] != null) {
    	result = (state.lastClean[botvacId] + (settings["ssCleaningInterval#$botvacId"] * 86400000)) - now()
    }
    log.debug "Time to smart schedule clean: $result milliseconds"
    result
}

def timeToForceClean(botvacId) {
	log.debug "Executing 'timeToForceClean' with device $botvacId"
	def result = -1
    if (settings.forceClean && state.botvacOnTimeMarker != null && state.botvacOnTimeMarker[botvacId] != null) {
    	result = (state.botvacOnTimeMarker[botvacId] + (settings.forceCleanDelay * 86400000)) - now()
    }
    log.debug "Time to force clean: $result milliseconds"
    result
}

def autoDockDelayValue() {
	log.debug "Executing 'autoDockDelayValue'"
	def result = -1
	if (settings.autoDock) {
		result = settings.autoDockDelay
    }
    log.debug "Auto dock delay: $result seconds"
    result
}

def resetSmartScheduleForDevice(botvacId) {
	log.debug "Executing 'resetSmartScheduleForDevice' with device $botvacId"
	if (settings["smartScheduleEnabled#$botvacId"] && state.lastClean != null && state.smartSchedule != null) {
		//Reset last clean date to current time
   		state.lastClean[botvacId] = now()
        if (settings["ssIntervalFromMidnight#$botvacId"]) {
        	state.lastClean[botvacId] = (new Date()).clearTime().getTime()
        } else {
            state.lastClean[botvacId] = now()
        }
   		//Remove existing SmartSchedule flag
    	state.smartSchedule[botvacId] = false
    }
    /**
    //DEBUG PURPOSES ONLY. FAKE TIME ON OVERRIDE SWITCH AND INCREASE POLL
    //state.lastClean[deviceNetworkId] = Date.parseToStringDate("Thu Oct 13 01:23:45 UTC 2016").getTime()
    state.lastClean[botvacId] = 1476868627993
    state.botvacOnTimeMarker[botvacId] = 1476889942741
    unschedule(pollOn)
    schedule("0 0/1 * * * ?", pollOn)
    log.debug "Fake data loaded.... " + (now() - state.lastClean[botvacId])/86400000 
    **/
    
}

//Helper methods
def setSHMToStay() {
	if (settings.autoSHM) {
		if (location.currentState("alarmSystemStatus")?.value == "away") {
			sendEvent(linkText:app.label, name:"Smart Home Monitor", value:"stay",descriptionText:"Smart Home Monitor was set to stay", eventType:"SOLUTION_EVENT", displayed: true)
			log.trace "Smart Home Monitor is set to stay"
			sendLocationEvent(name: "alarmSystemStatus", value: "stay")
			state.autoSHMchange = "y"
        	messageHandler("Smart Home Monitor is set to stay as a Neato Botvac is cleaning", true)
    	}
    }
}

def setSHMToAway() {
	if (settings.autoSHM) {
		if (location.currentState("alarmSystemStatus")?.value == "stay" && state.autoSHMchange == "y") {
			sendEvent(linkText:app.label, name:"Smart Home Monitor", value:"away",descriptionText:"Smart Home Monitor was set back to away", eventType:"SOLUTION_EVENT", displayed: true)
			log.trace "Smart Home Monitor is set back to away"
			sendLocationEvent(name: "alarmSystemStatus", value: "away")
			state.autoSHMchange = "n"
            messageHandler("Smart Home Monitor is set to away as all Neato Botvacs are off", true)
		}
	}
}

def startConditionalClean(data) {
	def botvacId = data.botvacId
	log.debug "Executing 'startConditionalClean for $botvacId'"
	if (getAllOk(botvacId)) {
    	def botvacDevice = getChildDevice(botvacId)
    	//If smartSchedule flag has been set, start clean.
         if ((state.smartSchedule[botvacId]) || (!settings["ssEnableWarning#$botvacId"])) {
         	if (settings.ssNotification) {
                messageHandler("Neato SmartSchedule has started ${botvacDevice.displayName} cleaning.", false)
            }
            resetSmartScheduleForDevice(botvacId)
            botvacDevice.on()
         }   	
     }
}

def adjustTimeforTimeZone(originalTime) {
	if (getTimeZone()) {
		def adjustedTime = timeToday(originalTime, location.timeZone)
    	def timeNow = now() + (2*1000) 
    	if (adjustedTime.time < timeNow) { 
			adjustedTime = adjustedTime + 1
    	}
    	return adjustedTime
    }
    return originalTime
}

def getNextTimeInSeconds() {
	def nextTime = null
    getChildDevices().each { childDevice ->
    	def time
        def botvacId = childDevice.deviceNetworkId
    	if (settings["starting#$botvacId"]) {
        	time = adjustTimeforTimeZone(settings["starting#$botvacId"])
        } else {
        	time = timeToday("00:01", location.timeZone)
        }
    	def t = timeTodayAfter(new Date(), time.format("HH:mm", getTimeZone()), getTimeZone())
		if (nextTime) {
        	nextTime = (nextTime > t.getTime()) ? t.getTime() : nextTime
        } else {
        	nextTime = t.getTime()
        }
	}
    def seconds = Math.ceil((nextTime - now()) / 1000)
	log.debug "Scheduling ST job to run in ${seconds}s, at ${nextTime}"
	return seconds as Integer
}

def messageHandler(msg, forceFlag) {
	log.debug "Executing 'messageHandler for $msg. Forcing is $forceFlag'"
	if (settings.sendSMS != null && !forceFlag) {
		sendSms(settings.sendSMS, msg) 
	}
    if (location.contactBookEnabled && settings.recipients) {
    	sendNotificationToContacts(msg, settings.recipients)
    } else if (settings.sendPush || forceFlag) {
		sendPush(msg)
	}
}

private getAllOk(botvacId) {
	getTriggerConditionsOk(botvacId) && getDaysOk(botvacId) && getTimeOk(botvacId) && getScheduleOk(botvacId) && getContactSensorsOk(botvacId)
}

private getScheduleOk(botvacId) {
	def t = (now() - state.lastClean[botvacId]) + 2
    def result = t > (settings["ssCleaningInterval#$botvacId"] * 86400000)
    log.trace "scheduleOk for $botvacId = $result"
    result
}

private getTriggerConditionsOk(botvacId) {
	//Calculate, depending on smart schedule trigger mode, whether conditions currently match
    def result = true
    
    if (settings["ssScheduleTrigger#$botvacId"] == "mode") {
    	result = location.mode in settings["ssAwayModes#$botvacId"] 
    } else if (settings["ssScheduleTrigger#$botvacId"] == "switch") {
    	if (settings["ssSwitchTriggerCondition#$botvacId"] == "any") {
        	result = "on" in settings["ssSwitchTrigger#$botvacId"].currentSwitch
        } else {
        	for (switchVal in settings["ssSwitchTrigger#$botvacId"].currentSwitch) {
        		if (switchVal == "off") {
            		result = false
            		break
        		}
    		}
        }
    } else if (settings["ssScheduleTrigger#$botvacId"] == "presence") {
    	if (settings["ssPeopleAwayCondition#$botvacId"] == "any") {
        	result = "not present" in settings["ssPeopleAway#$botvacId"].currentPresence
        } else {
        	for (person in settings["ssPeopleAway#$botvacId"]) {
        		if (person.currentPresence == "present") {
            		result = false
            		break
        		}
    		}
        }
    } 
    
    log.trace "triggerConditionsOk for $botvacId = $result"
    result
}

private getDaysOk(botvacId) {
	def result = true
	if (settings["days#$botvacId"]) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (getTimeZone()) { df.setTimeZone(location.timeZone) }
		def day = df.format(new Date())
		result = settings["days#$botvacId"].contains(day)
	}
	log.trace "daysOk for $botvacId = $result"
	result
}

private getTimeOk(botvacId) {
	def result = true
	if (settings["starting#$botvacId"] && settings["ending#$botvacId"]) {
		def currTime = now()
		def start = timeToday(settings["starting#$botvacId"], location.timeZone).time
		def stop = timeToday(settings["ending#$botvacId"], location.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk for $botvacId = $result"
	result
}

private getContactSensorsOk(botvacId) {
	def result = true
	def currContacts = settings["ssRestrictContactSensors#$botvacId"]?.currentContact
    if (currContacts) {
    	if (settings["ssRestrictContactSensorsCondition#$botvacId"] == "allclosed") { 
    		if (currContacts.contains("open")) { result = false }
    	}
    	else if (settings["ssRestrictContactSensorsCondition#$botvacId"] == "anyclosed") { 
    		result = currContacts.findAll {contactVal -> contactVal == "closed" ? true : false}
    	}
    	else if (settings["ssRestrictContactSensorsCondition#$botvacId"] == "allopen") { 
    		if (currContacts.contains("closed")) { result = false }
    	}
    	else if (settings["ssRestrictContactSensorsCondition#$botvacId"] == "anyopen") { 
    		result = currContacts.findAll {contactVal -> contactVal == "open" ? true : false}
    	}
    }
	log.trace "contactSesnorsOk for $botvacId = $result"
	result
}

private hhmm(time, fmt = "h:mm a z") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
    if (getTimeZone()) { f.setTimeZone(location.timeZone ?: timeZone(time)) }
	f.format(t)
}

def getTimeLabel(starting, ending){
	def timeLabel = "Tap to set"

    if(starting && ending){
    	timeLabel = "Between" + " " + hhmm(starting) + " "  + "and" + " " +  hhmm(ending)
    }
    else if (starting) {
		timeLabel = "Start at" + " " + hhmm(starting)
    }
    else if(ending){
    timeLabel = "End at" + hhmm(ending)
    }
	timeLabel
}

def greyedOutTime(starting, ending){
	def result = ""
    if (starting || ending) {
    	result = "complete"
    }
    result
}

def getTimeZone() {
	def tz = null
	if(location?.timeZone) { tz = location?.timeZone }
	if(!tz) { log.warn "No time zone has been retrieved from SmartThings. Please try to open your ST location and press Save." }
	return tz
}

def getChildName()           { return "Neato BotVac" }
def getServerUrl()           { return "https://graph.api.smartthings.com" }
def getShardUrl()            { return getApiServerUrl() }
def getCallbackUrl()         { return "https://graph.api.smartthings.com/oauth/callback" }
def getBuildRedirectUrl()    { return "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${shardUrl}" }
def getApiEndpoint()         { return "https://apps.neatorobotics.com" }
def getSmartThingsClientId() { return appSettings.clientId }
def beehiveURL(path = '/') 	 { return "https://beehive.neatocloud.com${path}" }
private def textVersion() {
    def text = "Neato (Connect)\nVersion: 1.2.3\nDate: 19012018(1700)"
}

private def textCopyright() {
    def text = "Copyright © 2018 Alex Lee Yuk Cheung"
}

def clientId() {
	if(!appSettings.clientId) {
		return "3ba64237d07f43e2e6ecff97de60916b73c4b06df71e9ad35ec02d7b3b513881"
	} else {
		return appSettings.clientId
	}
}

def clientSecret() {
	if(!appSettings.clientSecret) {
		return "e7fd560dab04efdd38488f918a2a8b0c097157d765e19003360fc458f5119bde"
	} else {
		return appSettings.clientSecret
	}
}