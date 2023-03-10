/**
 *  Camera Reboot
 *
 *  Version: 1.11
 *
 *  Copyright 2015 Rob Landry
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
	name: 		"Camera Reboot",
	namespace: 	"roblandry",
	author: 	"Rob Landry",
	description: 	"Turn a switch/outlet off and then on when camera is unreachable.",
	category: 	"Convenience",
	iconUrl:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches.png",
	iconX2Url:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png",
	iconX3Url:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png")


preferences {
	// the info section
	section("Info") {
		paragraph "Author:  Rob Landry"
		paragraph "Version: 1.11"
		paragraph "Date:    7/23/2015"
	}

	// the devices section
	section("Switches") {
		input "vSwitch", "capability.switch", title: "Switch to force reboot", multiple: false, required: false
		input "cameraSwitch", "capability.switch", title: "Switch to cycle", multiple: false
	}

	// the preferences section
	section("Camera") {
		paragraph "Camera Settings..."
		input "cameraName", "text", title: "Camera Name", required: true
		input "ipAddress", "text", title: "Camera IP Address", required: true
		input "cameraPort", "number", title: "Camera Port Number", required: true, defaultValue: 80
		input "adminUsername", "text", title: "Camera Admin Username", required: true, defaultValue: "admin"
		input "adminPassword", "text", title: "Camera Admin Password", required: true
	}

	// the push notification section
	section("Send Push Notification?") {
		input "sendPush", "bool", required: false,title: "Send Push Notification when unreachable?"
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
	subscribe(vSwitch, "switch", switchHandler)
	checkOnline()
	scheduleHandler()
}

def switchHandler(evt) {
	if (evt.value == "on") {
		scheduleHandler()
	}
}

//create a schedule to check for errors... 5 minutes
def scheduleHandler() {
	unschedule("checkOnline")
	runIn(300, "checkOnline", [overwrite: false])
	//runIn(60, "checkOnline", [overwrite: false])
}

//check if unable to reach camera
def checkOnline() {
	def errorMsg
	def isError=false

	log.debug("Checking if ${cameraName} is ONLINE")

	scheduleHandler()

	def params = [
		uri: "http://${adminUsername}:${adminPassword}@${ipAddress}:${cameraPort}",
		path: "/image/jpeg.cgi"
	]

	try {
		httpGet(params) { resp ->
			log.info("${cameraName} is ONLINE.")
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		def status = e.response.status
		if (status == 404) {
			isError=true
			errorMsg="Error 404"
		}
	} catch (java.net.NoRouteToHostException e) {
		isError=true
		errorMsg="No route to Host"
	} catch (java.net.ConnectException e) {
		isError=true
		errorMsg="Connection refused"
	} catch (org.apache.http.conn.ConnectTimeoutException e) {
		isError=true
		errorMsg="Connection timed out"
	}

	if(isError) {
		log.debug(errorMsg)
		rebootCamera(errorMsg)
	}
}

//if error exists, reboot camera
def rebootCamera(errorMsg) {
	log.info("${cameraName} is OFFLINE, Fixing that now.")
	if (sendPush) {
		try {
			sendPush("The ${cameraName} is offline: (${errorMsg}).")
			//log.info("${cameraName} is OFFLINE: (${errorMsg}).")
		} catch (physicalgraph.exception.UncheckedException e) {
			log.error("Smartthings Error: ${e}")
		}
	}
	log.info("Turning switch off.")
	cameraSwitch.off()
	log.info("Turning switch on.")
	cameraSwitch.on()
}