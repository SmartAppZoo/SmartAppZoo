/**========================================================
 *  Camera Photo/Video When...
 * ========================================================
 *
 *  Author: Rob Landry
 *
 *  URL: http://github.com/roblandry/camera-record.app.groovy
 *
 *  Date: 3/5/15
 *
 *  Version: 1.0
 *
 *  Description: This app was designed to work with the Android IP Camera device (http://github.com/roblandry/android-ip-camera.device). 
 *  You can record video or take a burst of photos when a trigger is activated. Acceptable triggers are motion, contact, acceleration, 
 *  switch, presence, smoke, water, button, modechange, and schedule. There is also a time delay to allow the camera to continue recording 
 *  once the trigger has stopped (ie. motion ceased, continue recording for 5 minutes).
 *
// ========================================================
 *
 *  Copyright: 2015 Rob Landry
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
// ========================================================
 *
 * TODO: Time lapse
 * TODO: Time Schedule
 *
 */

definition(
	name: "Camera Photo/Video when...",
	namespace: "roblandry",
	author: "Rob Landry",
	description: "This app was designed to work with the Android IP Camera device (http://github.com/roblandry/android-ip-camera.device). You can record video or take a burst of photos when a trigger is activated. Acceptable triggers are motion, contact, acceleration, switch, presence, smoke, water, button, modechange, and schedule. There is also a time delay to allow the camera to continue recording once the trigger has stopped (ie. motion ceased, continue recording for 5 minutes).",
	category: "SmartThings Labs",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png"
)

// ========================================================
// PREFERENCES
// ========================================================
preferences {
	page(name: "setup", title: "Setup")
	page(name: "choseDevices", title: "choseDevices")
	page(name: "theCamera", title: "theCamera")
	page(name: "theMessage", title: "theMessage")
	page(name: "cameraPref", title: "cameraPref")
	page(name: "cameraBurst", title: "cameraBurst")
	page(name: "motionPref", title: "motionPref")
}

// ========================================================
// PAGES
// ========================================================
def setup() {
	state.choseDevices = "incomplete"
	state.theCamera = "incomplete"
	state.theMessage = "incomplete"
	state.cameraPerf = "incomplete"
	state.cameraBurst = "incomplete"
	state.motionPerf = "incomplete"

	hrefState()
	/*def hrefParams = [
		install: false,
		nextPage: null
	]*/

	dynamicPage(name: "setup", install: true, uninstall: true) {
		section {
			paragraph "Author: Rob Landry"
			paragraph "Version: 1.0"
			paragraph "Date: 3/5/2015"
		}

		section {
			href(name: "hrefNotRequired",
			title: "SmartThings",
			required: false,
			external: true,
			url: "http://www.github.com/roblandry/",
			description: "tap to view my github in mobile browser")
		}

		section {

			href(
				name: "cameraPref", 
				page: "cameraPref", 
				title: "Camera Preferences",
				params: hrefParams, 
				//description: "includes params: ${hrefParams}",
				state: state.cameraPerf
			)

			if (photoOrVideo) {
			href(
				name: "choseDevices", 
				page: "choseDevices", 
				title: "Chose Devices",
				params: hrefParams, 
				//description: "includes params: ${hrefParams}",
				state: state.choseDevices
			)
     
			href(
				name: "theCamera", 
				page: "theCamera", 
				title: "Chose Camera",
				params: hrefParams, 
				//description: "includes params: ${hrefParams}",
				state: state.theCamera
			)
     
			href(
				name: "theMessage", 
				page: "theMessage", 
				title: "Send Message",
				params: hrefParams, 
				//description: "includes params: ${hrefParams}",
				state: state.theMessage
			)

			if (photoOrVideo == "photo") {
				href(
					name: "cameraBurst", 
					page: "cameraBurst", 
					title: "Photo Preferences",
					params: hrefParams, 
					//description: "includes params: ${hrefParams}",
					state: state.cameraBurst
				)
			}

			if (motion) {
				href(
					name: "motionPref", 
					page: "motionPref", 
					title: "Motion Preferences",
					params: hrefParams, 
					//description: "includes params: ${hrefParams}",
					state: state.cameraPerf
				)
			}
			}
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}
def choseDevices() {
	dynamicPage(name: "choseDevices", title: "Select devices to trigger the camera...", install: false, nextPage: "setup") {
		section {
			input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
			input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
			input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
			input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
			input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
			input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
			input "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
			input "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
			input "button1", "capability.button", title: "Button Press", required:false, multiple:true
			input "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true
			input "timeOfDay", "time", title: "At a Scheduled Time", required: false
		}
	}
}
def theCamera() {
	dynamicPage(name: "theCamera", title: "The IP Camera...", install: false, nextPage: "setup") {

		section {
			if (photoOrVideo == "video") {
				input "record", "capability.imageCapture", required: true, multiple: true
			} else if (photoOrVideo == "photo"){
				input "photo", "capability.imageCapture", required: true, multiple: true
			}

		}
	}
}
def theMessage() {
	dynamicPage(name: "theMessage", title: "Send a Message...", install: false, nextPage: "setup") {
		section {
			input "messageText", "text", title: "Message Text", required: false
			input("recipients", "contact", title: "Send notifications to") {
				input "phone", "phone", title: "Phone Number", required: false
			}
		}
	}
}
def cameraPref() {
	dynamicPage(name: "cameraPref", title: "Video/Photo Preferences...", install: false, nextPage: "setup") {
		section {
			input "photoOrVideo", "enum", title: "Record video or take photos?", required: true, 
			options: ["video":"Video","photo":"Photo"]
		}
	}
}
def cameraBurst() {
	dynamicPage(name: "cameraBurst", title: "Photo Burst Preferences...", install: false, nextPage: "setup") {
		section {
			input "burstCount", "number", title: "How many photos? (default 5)", defaultValue:5, required: false
		}
	}
}
def motionPref() {
	dynamicPage(name: "motionPref", title: "Motion Preferences...", install: false, nextPage: "setup") {
		section {
			paragraph "Motion sensor delay..."
			input "delayMinutes", "number", title: "Minutes", required: false, defaultValue: 0
		}
	}
}

// ========================================================
// HELPERS
// ========================================================
def hrefState() {
	if (motion || contact || acceleration || mySwitch || arrivalPresence || departurePresence || smoke || water || button1 || triggerModes || timeOfDay) { state.choseDevices = "complete" } else { state.choseDevices = "incomplete" }
	if (photo || record) { state.theCamera = "complete" } else { state.theCamera = "incomplete" }
	if (messageText) { state.theMessage = "complete" } else { state.theMessage = "" }
	if (photoOrVideo) { state.cameraPerf = "complete" } else { state.cameraPerf = "incomplete" }
	if (burstCount) { state.cameraBurst = "complete" } else { state.cameraBurst = "" }
	if (delayMinutes) { state.motionPerf = "complete" } else { state.motionPerf = "" }
}

// ========================================================
// INSTALL/UNINSTALL/INIT
// ========================================================
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
	state.record = false
	state.motionStopTime = false
	state.trigger = false
	state.buttonPush = false
	subscribe(contact, "contact", eventHandler)
	subscribe(acceleration, "acceleration", eventHandler)
	subscribe(motion, "motion", eventHandler)
	subscribe(mySwitch, "switch", eventHandler)
	subscribe(arrivalPresence, "presence", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke", eventHandler)
	subscribe(smoke, "carbonMonoxide", eventHandler)
	subscribe(water, "water", eventHandler)
	subscribe(button1, "button", eventHandler)

	if (triggerModes) {
		subscribe(location, modeChangeHandler)
	}

	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}
}

// ========================================================
// HANDLERS
// ========================================================
// The event handler
def eventHandler(evt) {
	log.debug "Event Handler: ${evt.name}: ${evt.value}, State: ${state}"

	getTriggerState(evt)

	if (recipients && state.trigger) { sendMessage(evt) }

	if (photoOrVideo == "photo" && state.trigger) {
		cameraTake()
	} else if (photoOrVideo == "video") {
		if (state.trigger) { startRecording(evt) }
		else { stopRecording(evt) }
	}
}

// Send a message
def sendMessage(evt) {
	log.debug "$evt.name: $evt.value, $messageText"
	log.info "Message Sent."

	if (location.contactBookEnabled) {
		sendNotificationToContacts(messageText, recipients)
	} else {
		sendPush(messageText)
		if (phone) {
			sendSms(phone, messageText)
		}
	}
}

// Take a photo
def cameraTake(){
	log.info "Photo Captured"
	photo.take()
	(1..((burstCount ?: 5) - 1)).each {
		photo.take(delay: (500 * it))
	}
}

// Start Recording Logic
def startRecording(evt) {
	log.debug "StartRecording: ${evt.name}: ${evt.value}, State: ${state}"
	if(!state.record){
		recordOn()
	}
}

// Stop Recording Logic
def stopRecording(evt) {
	log.debug "StopRecording: ${evt.name}: ${evt.value}, State: ${state}"
	if (state.record) {
		state.motionStopTime = now()

		// if we are delaying the turnoff, then schedule it, otherwise do it!
		if(delayMinutes) {

			// This should replace any existing off schedule
			unschedule("turnOffAfterDelay")
			runIn(delayMinutes*60, "turnOffAfterDelay", [overwrite: false])
		} else {
			turnOffAfterDelay()
		}
	}
}

// Start Recording
def recordOn() { 
	log.info "Commenced Recording"
	state.record = true
	record.on() 
}

// Stop Recording
def recordOff() { 
	log.info "Finished Recording"
	state.record = false
	record.off() 
}

// Schedule to Stop Recording
def turnOffAfterDelay() {
	log.debug "turnOffAfterDelay: State: ${state}"

	// if there is a motion allowed, we are in an active program, and a time was specified...
	if (state.motionStopTime && state.record) {

		// get the time elapsed
		def elapsed = now() - state.motionStopTime

		// if we are at the time to turn off...
		if (elapsed >= (delayMinutes ?: 0) * 60000L) {
			recordOff()
		}
	}
}

// Get the trigger state
def getTriggerState(evt) {
	switch(evt.name) {
		case "contact":
			if (evt.value == "open") {state.trigger = true}
			if (evt.value == "closed") {state.trigger = false}
			break;
		case "acceleration":
			if (evt.value == "active") {state.trigger = true}
			if (evt.value == "inactive") {state.trigger = false}
			break;
		case "motion":
			if (evt.value == "active") {state.trigger = true}
			if (evt.value == "inactive") {state.trigger = false}
			break;
		case "switch":
			if (evt.value == "on") {state.trigger = true}
			if (evt.value == "off") {state.trigger = false}
			break;
		case "presence":
			if (evt.value == "present") {state.trigger = true}
			if (evt.value == "not present") {state.trigger = false}
			break;
		case "smoke":
			if ((evt.value == "detected") || (evt.value == "tested")) {state.trigger = true}
			if (evt.value == "clear") {state.trigger = false}
			break;
		case "carbonMonoxide":
			if ((evt.value == "detected") || (evt.value == "tested")) {state.trigger = true}
			if (evt.value == "clear") {state.trigger = false}
			break;
		case "water":
			if (evt.value == "wet") {state.trigger = true}
			if (evt.value == "dry") {state.trigger = false}
			break;
		case "button":
			if (!state.buttonPush) {
				state.buttonPush = true
				state.trigger = true
			}
			if (state.buttonPush) {
				state.buttonPush = false
				state.trigger = false
			}
			break;
	}
}

// Mode Change Handler
def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
	log.info "Mode changed to ${evt.value}."
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
}

// At a scheduled time handler
def scheduledTimeHandler() {
	log.trace "scheduledTimeHandler()"
	eventHandler(null)
}