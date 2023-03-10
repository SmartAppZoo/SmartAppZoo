/**
 *  Blue Iris Server Integration for SmartThings allows you to see the status of your
 *  cameras on a Blue Iris Server.
 *
 *  Please visit http://devicebuilder.github.io/Blue_Iris_Server_for_ST for more information.
 *
 *  Version 1.0.0  (02/15/2015)
 *
 *  The latest version of this file can be found on GitHub at:
 *  <https://github.com/devicebuilder/Blue_Iris_Server_for_ST/Blue Iris Server.groovy>
 *  must thank various developers like statusbits, minollo, ken from blue iris, etc
 *
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2015 khiangseow@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import groovy.json.JsonSlurper

definition(
    name: "Blue Iris Server",
    namespace: "khiangseow@gmail.com",
    author: "Khiang Seow",
    description: "Integration to Blue Iris server for SmartThings.",
    category: "Safety & Security",
    iconUrl: "http://blueirissoftware.com/wp-content/themes/roots/assets/img/logo.png",
    iconX2Url: "http://blueirissoftware.com/wp-content/themes/roots/assets/img/logo.png",
    oauth: [displayName:"Blue Iris Server Smartapp", displayLink:"http://devicebuilder.github.io/Blue_Iris_Server_for_ST"]
)

preferences {
    page name:"pageSetup"
    page name:"pageAbout"
    page name:"pageServerSettings"
    page name:"pageServerStatus"
    page name:"pageCameraStatus"
}

// Show setup page
def pageSetup() {
    TRACE("pageSetup()")

    if (state.version != buildNumber()) {
        setupInit()
        return pageAbout()
    }

    def serverStatus  //whether the server is up or not.
    if (state.session) {
        serverStatus = "enabled."
    } else {
        serverStatus = "not configured."
    }

    def pageProperties = [
        name:       "pageSetup",
        title:      "Status",
        nextPage:   null,
        install:    true,
        uninstall:  state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph "Blue Iris Server is ${serverStatus}"
            if (state.serverLink) {
                href "pageServerStatus", title:"Server Status", description:"Tap to open"
    	        if (state.cameras.size()) {
	               href "pageCameraStatus", title:"Camera Status", description:"Tap to open"
        	    }
            }
        }
        section("Setup Menu") {
            href "pageServerSettings", title:"Blue Iris Server Settings", description:"Tap to open"
            href "pageAbout", title:"About Blue Iris Server", description:"Tap to open"
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

// Show "About" page
def pageAbout() {
    TRACE("pageAbout()")

    def textAbout =
        "${textVersion()}\n${textCopyright()}\n\n" +
        "You can contribute to the development of this app by making " +
        "donation to khiangseow@gmail.com via PayPal."

    def hrefInfo = [
        url:        "http://devicebuilder.github.io/Blue_Iris_Server_for_ST",
        style:      "embedded",
        title:      "Tap here for more information...",
        description:"http://devicebuilder.github.io/Blue_Iris_Server_for_ST",
        required:   false,
    ]

    def pageProperties = [
        name:       "pageAbout",
        title:      "About",
        nextPage:   "pageSetup",
        install:    true,
        uninstall:  state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
            href hrefInfo
        }
        section("License") {
            paragraph textLicense()
        }
    }
}

/* Show "Server Status" page - information based on JSON interface for status
	Get (and optionally set) the state of the traffic signal icon, active global profile as well as the schedule's hold/run state:
	signal: a single digit 0 for red, 1 for green, 2 for yellow.
	profile: a single digit 0-7 for the profile number to set; or -1 to change the hold/run state.  This functions the same it does on the local UI, so sending a profile change a second time will set the schedule to it's "hold" state.
	dio: the state of a DIO output.  An array of 0's and 1's is returned, or you may set a particular value by sending an object with:
	output: an output number 0-7
	force: true or false
	msec: the number of milliseconds to hold the output enabled if force is not specified.
	play: play a sound file from the application Sounds folder.
	The follow values are also returned:
	lock: the state of the schedule run/hold button: 0 for run, 2 for temp, 1 for hold
	clips: a text value describing the number of clips in the New and Stored folders along with disc usage statistics
	warnings: the number of new warnings since the log command was last used
	alerts: the number of new alerts since the alerts command was last used
	cpu: the server's CPU usage overall (not just Blue Iris) expressed as a percentage.
	mem: a string representation of the Blue Iris process memory usage
	uptime: a string representation of the time in days:hours:minutes:seconds that Blue Iris has been running
 */
def pageServerStatus() {
    TRACE("pageServerStatus()")

    def pageProperties = [
        name:       "pageServerStatus",
        title:      "Server Status",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        def serverStatus = "BI Server "

		if (state.session) {
            if (state.server.signal == "0") {
                serverStatus += "is RED, "
            } else if (state.server.signal == "1") {
                serverStatus += "is GREEN, "
            } else if (state.server.signal == "2") {
                serverStatus += "is YELLOW, "
            }
            if (state.server.profile) {
                serverStatus += "using global profile ${state.server.profile}, "
            }
            if (state.server.dio) {
                serverStatus += "DIO state ${state.server.dio}, "
            }
            if (state.server.play) {
                serverStatus += "sound file is ${state.server.play}, "
            }
            if (state.server.lock == "1") {
                serverStatus += "schedule button is on hold, "
            } else if (state.server.lock == "0") {
                serverStatus += "schedule button is in run mode, "
            } else if (state.server.lock == "2") {
                serverStatus += "schedule button is on temp mode, "
            }
            if (state.server.clips) {
                serverStatus += "Clips ${state.server.clips}, "
            }
            if (state.server.warnings) {
                serverStatus += "${state.server.warnings} warnings since last log command, "
            }
            if (state.server.alerts) {
                serverStatus += "${state.server.alerts} alerts since last alert command, "
            }
            if (state.server.cpu) {
                serverStatus += "CPU at ${state.server.cpu}%, "
            }
            if (state.server.mem) {
                serverStatus += "Memory usage at ${state.server.mem}, "
            }
            if (state.server.uptime) {
                serverStatus += "running for ${state.server.uptime}. "
            }
        } else {
        	serverStatus = "Need to set up server information first."
        }
        section ("Current") {
        	paragraph serverStatus
        }
    }
}

/* Show "Cameras' Status" page - information based on JSON interface for camlist
	optionDisplay: the camera or group name ((Note: one online JSON documentation shows this as optionsDisplay and it is wrong)
	optionValue: the camera or group short name, used for other requests and commands requiring a camera short name
	FPS: the current number of frames/second delivered from the camera
	color: 24-bit RGB value (red least significant) representing the camera's display color
	clipsCreated: the number of clips created since the camera stats were last reset
	isAlerting: true or false; currently sending an alert
	isEnabled: true or false
	isOnline true or false
	isMotion: true or false
	isNoSignal: true or false
	isPaused: true or false
	isTriggered: true or false
	isRecording: true or false
	isYellow: true or false; the yellow caution icon
	profile: the camera's currently active profile, or as overridden by the global schedule or the UI profile buttons.
	ptz: is PTZ supported, true or false
	audio: is audio supported, true or false
	width: width of the standard video frame
	height: height of the standard video frame
	nTriggers: number of trigger events since last reset
	nNoSignal: number of no signal events since last reset
	nClips: number of no recording events since last reset
 */
def pageCameraStatus() {
    TRACE("pageCameraStatus()")
	log.info ("pageCameraStatus()")
    
    def pageProperties = [
        name:       "pageCameraStatus",
        title:      "Camera Status",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
		
        login()
 		state.cameras.each() {
        
            def cameraStatus = "${it.optionDisplay} / ${it.optionValue} is "

            if (it.isEnabled) {
                cameraStatus += "enabled, "
            }
            if (it.isAlerting) {
                cameraStatus += "alerting, "
            }
            if (it.isOnline) {
                cameraStatus += "online, "
            }
            if (it.isMotion) {
                cameraStatus += "motion detecting, "
            }
            if (it.isNoSignal) {
                cameraStatus += "no signal, "
            }
            if (it.isPaused) {
                cameraStatus += "paused, "
            }
            if (it.isTriggered) {
                cameraStatus += "motion triggered, "
            }
            if (it.isRecording) {
                cameraStatus += "recording, "
            }
            if (it.isYellow) {
                cameraStatus += "has yellow icon, "
            }
            if (it.profile) {
                cameraStatus += "using profile ${it.profile}, "
            }
            if (it.ptz) {
                cameraStatus += "ptz supported, "
            }
            if (it.audio) {
                cameraStatus += "audio supported, "
            }
            if (it.width) {
                cameraStatus += "video ${it.width} x  "
            }
            if (it.height) {
                cameraStatus += "${it.height}, "
            }
            if (it.nTriggers) {
                cameraStatus += "${it.nTriggers} triggers since last reset, "
            }
            if (it.nNoSignal) {
                cameraStatus += "${it.nNoSignal} of No Signals since last reset, "
            }
            if (it.nClips) {
                cameraStatus += "${it.nClips} of new recordings since last reset."
            }
            log.info "status: ${cameraStatus}"

            def href2Info = [
                url:        "${state.serverLink}/mjpg/${it.optionValue}?session=${state.session}",
                style:      "embedded",
                title:      "Tap here for see video feed...",
                description:"Video stream for ${it.optionValue}",
                required:   false,
            ]
            def href1Info = [
                url:        "${state.serverLink}/image/${it.optionValue}?session=${state.session}",
                style:      "embedded",
                title:      "Tap here for see snapshot...",
                description:"Snapshot for ${it.optionValue}",
                required:   false,
            ]
            section("${it.optionDisplay}") {
                paragraph cameraStatus
                if (state.session) {
                	href href1Info
                    //href href2Info -- video stream does not work well.
                }
            }
		}
	}
}

// Show "Blue Iris Server Settings" page
def pageServerSettings() {
    TRACE("pageServerSettings()")

    def helpServerSettings =
        "URL is the external or WAN IP address to access the Blue Iris Server. " +
        "Do not use the internal LAN IP address and do not include http://" +
        "\nPort is the port for Blue Iris Web Server. If going through a router, " +
        "this is the port forwarding settings. For example, the router is " +
        "configured to forward port 6758 to port 81 of the BI Server, then enter " +
        "6758 in this field." +
        "\nUsername specified on the Blue Iris Server. Be sure this user is an " +
        "administrator on the server." +
        "\nPassword for the username."

    def inputURL = [
        name:           "url",
        type:           "string",
        title:          "Blue Iris web server's URL",
        required:       true
    ]

    def inputPort = [
        name:           "port",
        type:           "string",
        title:          "Blue Iris web server's port",
        required:       true
    ]

    def inputUsername = [
        name:           "username",
        type:           "string",
        title:          "Username of administrator",
        required:       true
    ]

    def inputPassword = [
        name:           "password",
        type:           "string",
        title:          "password for user",
        required:       true
    ]


    def pageProperties = [
        name:       "pageServerSettings",
        title:      "Blue Iris Server Settings",
        nextPage:   "pageSetup",
        uninstall:  state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph helpServerSettings
            input inputURL
            input inputPort
            input inputUsername
            input inputPassword
        }
    }
}

/* --- End of pages definitions --- */

def installed() {
    TRACE("installed()")

    initialize()
    state.installed = true
}

def updated() {
    TRACE("updated()")

    unsubscribe()
    unschedule()
    initialize()
}

private def setupInit() {
    TRACE("setupInit()")

    state.version = buildNumber()
    if (state.installed == null) {
        state.installed = false
        state.cameras = []
        state.session = ""
        state.serverLink = ""
    }
}

private def initialize() {
    TRACE("${app.name}. ${textVersion()}. ${textCopyright()}")

    state._init_ = true
	state.serverLink = "http://" + settings.url + ":" + settings.port
	unschedule()
    // subscribe(location, onLocation) // consider later what to do if the mode changes
    STATE()
    state._init_ = false
    runEvery30Minutes(getStatus)
    getStatus()
}

/* things to consider - send notification that I am unable to acess the web server */
private login() {
def errorMsg = "Could not login to Blue Iris Server :-("
def uriLink = state.serverLink
def sessionID = ""
def hash = []

	try {
    	httpPostJson(uri: uriLink, path: '/json', body: ["cmd":"login"]) { response1 ->
			
            if (response1.data.result == "fail")
            {
                sessionID = response1.data.session
            	state.session = sessionID
                hash = settings.username + ":" + sessionID + ":" + settings.password
                hash = hash.encodeAsMD5()
                
				httpPostJson(uri: uriLink, path: '/json',  body: ["cmd":"login","session":sessionID,"response":hash]) { response2 ->
               
                    if (response2.data.result == "fail") {
						log.error errorMsg
                     } else {
                     	//log.debug "Logged in!"
						//log.info "session: ${state.session}"
                     }
                }
            } else {
					log.error 'I should not be getting a success result at this point'
            }
    	}
    }  catch(Exception e) {
    	log.error e
    }
}

private logout() {

	if (sessionID) {
    	httpPostJson(uri: state.serverLink, path: '/json', body: ["cmd":"logout","session":state.session]) { response ->
				setMessage("Logged out")
        }
		state.session=""
    }
}

private getStatus() {
	login()
    getServerStatus()
    getCamList()
    logout()
}

/* TO-DO for personal use, can ignore a bunch of these settings. Just need to know the server is up and running */
private getServerStatus() {

	TRACE("getServerStatus()")
	if (state.session) {
		httpPostJson(uri: state.serverLink, path: '/json',  body: ["cmd":"status","session":state.session]) { response3 ->
            //log.debug "$response3.data.data"
    		if (response3.data.data.result == "fail") {
        		statusStr = "\nUnable to get status for BI Server: $response3"
        	} else {
                state.server = ["signal": response3.data.data.signal, 
                				"profile" : response3.data.data.profile, 
                				"dio" : response3.data.data.dio, 
                                "play" : response3.data.data.play, 
                                "lock" : response3.data.data.lock,
                				"clips" : response3.data.data.clips, 
                                "warnings" : response3.data.data.warnings,
								"alerts" : response3.data.data.alerts, 
                                "cpu" : response3.data.data.cpu,
                				"mem" : response3.data.data.mem, 
                                "uptime" : response3.data.data.uptime,
                				"tzone" : response3.data.data.tzone]
            }
       	}
    }
}

/* TO-DO for personal use, can ignore a bunch of these settings. Just need to know the camera is up and running and consider triggering recording in the future */
private getCamList() {

	TRACE("getCamList()")
	if (state.session) {
		httpPostJson(uri: state.serverLink, path: '/json',  body: ["cmd":"camlist","session":state.session]) { response3 ->
    		if (response3.data.data.result == "fail") {
        		statusStr = "\nUnable to get status for BI Server: $response3"
        	} else {
            	state.cameras = []
            	response3.data.data.each() {
                	def allCameras = (it.optionDisplay == "+All cameras") || (it.optionDisplay == "+All cameras cycle")
                    if (!allCameras) {
                        def camera = [
                                    optionDisplay: it.optionDisplay, 
                                    optionValue : it.optionValue, 
                                    FPS : it.FPS, 
                                    color : it.color, 
                                    clipsCreated : it.clipsCreated, 
                                    isAlerting : it.isAlerting, 
                                    isEnabled : it.isEnabled, 
                                    isOnline : it.isOnline, 
                                    isMotion : it.isMotion, 
                                    isNoSignal : it.isNoSignal, 
                                    isPaused : it.isPaused, 
                                    isTriggered : it.isTriggered, 
                                    isRecording : it.isRecording, 
                                    isYellow : it.isYellow, 
                                    profile : it.profile, 
                                    ptz : it.ptz, 
                                    audio : it.audio, 
                                    width : it.width, 
                                    height : it.height, 
                                    nTriggers : it.nTriggers, 
                                    nNoSignal : it.nNoSignal, 
                                    nClips : it.nClips
                        ]
                        state.cameras << camera
					}
				}
			}
       	}
    }
}

private def buildNumber() {
    return 000001
}

private def textVersion() {
    def text = "Version 1.0.0 (02/17/2015)"
}

private def textCopyright() {
    def text = "Copyright © 2015 khiangseow@gmail.com"
}

private def textLicense() {
    def text =
        "This program is free software: you can redistribute it and/or " +
        "modify it under the terms of the GNU General Public License as " +
        "published by the Free Software Foundation, either version 3 of " +
        "the License, or (at your option) any later version.\n\n" +
        "This program is distributed in the hope that it will be useful, " +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " +
        "General Public License for more details.\n\n" +
        "You should have received a copy of the GNU General Public License " +
        "along with this program. If not, see <http://www.gnu.org/licenses/>."
}

private def TRACE(message) {
    //log.debug message
}

private def STATE() {
    //log.trace "settings: ${settings}"
    //log.trace "state: ${state}"
}

