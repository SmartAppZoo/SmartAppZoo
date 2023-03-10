/**
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
 *
 *	This smartapp can be used in combination with the VLC Thing device type created by @geko to play notifications
 *  on any computer, and then can be streamed to any Bluetooth speaker (e.g. - Amazon Echo)
 *
 *  See VLC Thing installation instructions found here: https://github.com/statusbits/smartthings/blob/master/VlcThing.md
 *
 *  Date: 2016-01-03
 *  Custom Notify With Sound edition with Alexa voice
 */
definition(
	name: "Alexa Voice Notifications",
	namespace: "nelemansc",
	author: "nelemansc",
	description: "Play a sound when the mode changes or other events occur.",
	category: "SmartThings Labs",
	iconUrl: "https://d3rnbxvnd0hlox.cloudfront.net/images/channels/1172726678/icons/large.png",
	iconX2Url: "https://d3rnbxvnd0hlox.cloudfront.net/images/channels/1172726678/icons/large.png"
)

preferences {
	page(name: "mainPage", title: "Play a message when something happens", install: true, uninstall: true)
	page(name: "chooseTrack", title: "Select a phrase")
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def mainPage() {
	dynamicPage(name: "mainPage") {
		def anythingSet = anythingSet()
		if (anythingSet) {
			section("Play message when"){
				ifSet "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
				ifSet "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
				ifSet "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
				ifSet "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
				ifSet "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
				ifSet "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
				ifSet "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
				ifSet "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
				ifSet "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
				ifSet "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
				ifSet "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
				ifSet "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true
				ifSet "timeOfDay", "time", title: "At a Scheduled Time", required: false
			}
		}
		def hideable = anythingSet || app.installationState == "COMPLETE"
		def sectionTitle = anythingSet ? "Select additional triggers" : "Play message when..."

		section(sectionTitle, hideable: hideable, hidden: true){
			ifUnset "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
			ifUnset "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
			ifUnset "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
			ifUnset "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
			ifUnset "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
			ifUnset "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
			ifUnset "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
			ifUnset "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
			ifUnset "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
			ifUnset "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
			ifUnset "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
			ifUnset "triggerModes", "mode", title: "System Changes Mode", description: "Select mode(s)", required: false, multiple: true
			ifUnset "timeOfDay", "time", title: "At a Scheduled Time", required: false
		}
		section{
			input "actionType", "enum", title: "Action?", required: true, options: [
				"Mail has arrived",
				"Motion detected - Generic",
				"Motion in basement",
				"Motion in garage",
				"Motion in front yard",
				"Motion in back yard",
				"Motion at front door",
				"Motion at back door",
				"Front door opened",
				"Front door unlocked",
				"Back door opened",
				"Back door unlocked",
				"Basement door opened",
				"Basement door unlocked",	
				"Garage door opened",		
				"Patio door opened",			
				"Patio door unlocked",
				"Water detected - Generic",	
				"Water detected in kitchen",	
				"Water detected in garage",	
				"Water detected in basement",	
				"Smoke detected - Generic",
				"Smoke detected in kitchen",	
				"Smoke detected in garage",	
				"Smoke detected in basement",	
				"Boyfriend is arriving",
				"Girlfriend is arriving",
				"Husband is arriving",	
				"Wife is arriving",	
				"Son is arriving",
				"Daughter is arriving",	
				"Welcome Home",
				"Good Morning",
				"Good Night",]
				
			input "message","text",title:"Play this message", required:false, multiple: false
		}
		section {
			input "sonos", "capability.musicPlayer", title: "On this speaker", required: true
		}
		section("More options", hideable: true, hidden: true) {
			input "resumePlaying", "bool", title: "Resume currently playing music after notification", required: false, defaultValue: true
			href "chooseTrack", title: "Or play this music or radio station", description: song ? state.selectedSong?.station : "Tap to set", state: song ? "complete" : "incomplete"

			input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
			input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			if (settings.modes) {
            	input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            }
			input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}

def chooseTrack() {
	dynamicPage(name: "chooseTrack") {
		section{
			input "song","enum",title:"Play this track", required:true, multiple: false, options: songOptions()
		}
	}
}

private songOptions() {

	// Make sure current selection is in the set

	def options = new LinkedHashSet()
	if (state.selectedSong?.station) {
		options << state.selectedSong.station
	}
	else if (state.selectedSong?.description) {
		// TODO - Remove eventually? 'description' for backward compatibility
		options << state.selectedSong.description
	}

	// Query for recent tracks
	def states = sonos.statesSince("trackData", new Date(0), [max:30])
	def dataMaps = states.collect{it.jsonValue}
	options.addAll(dataMaps.collect{it.station})

	log.trace "${options.size()} songs in list"
	options.take(20) as List
}

private saveSelectedSong() {
	try {
		def thisSong = song
		log.info "Looking for $thisSong"
		def songs = sonos.statesSince("trackData", new Date(0), [max:30]).collect{it.jsonValue}
		log.info "Searching ${songs.size()} records"

		def data = songs.find {s -> s.station == thisSong}
		log.info "Found ${data?.station}"
		if (data) {
			state.selectedSong = data
			log.debug "Selected song = $state.selectedSong"
		}
		else if (song == state.selectedSong?.station) {
			log.debug "Selected existing entry '$song', which is no longer in the last 20 list"
		}
		else {
			log.warn "Selected song '$song' not found"
		}
	}
	catch (Throwable t) {
		log.error t
	}
}

private anythingSet() {
	for (name in ["motion","contact","contactClosed","acceleration","mySwitch","mySwitchOff","arrivalPresence","departurePresence","smoke","water","button1","timeOfDay","triggerModes","timeOfDay"]) {
		if (settings[name]) {
			return true
		}
	}
	return false
}

private ifUnset(Map options, String name, String capability) {
	if (!settings[name]) {
		input(options, name, capability)
	}
}

private ifSet(Map options, String name, String capability) {
	if (settings[name]) {
		input(options, name, capability)
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(app, appTouchHandler)
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)
	subscribe(button1, "button.pushed", eventHandler)

	if (triggerModes) {
		subscribe(location, modeChangeHandler)
	}

	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}

	if (song) {
		saveSelectedSong()
	}

	loadText()
}

def eventHandler(evt) {
	log.trace "eventHandler($evt?.name: $evt?.value)"
	if (allOk) {
		log.trace "allOk"
		def lastTime = state[frequencyKey(evt)]
		if (oncePerDayOk(lastTime)) {
			if (frequency) {
				if (lastTime == null || now() - lastTime >= frequency * 60000) {
					takeAction(evt)
				}
				else {
					log.debug "Not taking action because $frequency minutes have not elapsed since last action"
				}
			}
			else {
				takeAction(evt)
			}
		}
		else {
			log.debug "Not taking action because it was already taken today"
		}
	}
}
def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
}

def scheduledTimeHandler() {
	eventHandler(null)
}

def appTouchHandler(evt) {
	takeAction(evt)
}

private takeAction(evt) {

	log.trace "takeAction()"

	if (song) {
		sonos.playSoundAndTrack(state.sound.uri, state.sound.duration, state.selectedSong, volume)
	}
	else if (resumePlaying){
		sonos.playTrackAndResume(state.sound.uri, state.sound.duration, volume)
	}
	else {
		sonos.playTrackAndRestore(state.sound.uri, state.sound.duration, volume)
	}

	if (frequency || oncePerDay) {
		state[frequencyKey(evt)] = now()
	}
	log.trace "Exiting takeAction()"
}

private frequencyKey(evt) {
	"lastActionTimeStamp"
}

private dayString(Date date) {
	def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
	if (location.timeZone) {
		df.setTimeZone(location.timeZone)
	}
	else {
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
	}
	df.format(date)
}

private oncePerDayOk(Long lastTime) {
	def result = true
	if (oncePerDay) {
		result = lastTime ? dayString(new Date()) != dayString(new Date(lastTime)) : true
		log.trace "oncePerDayOk = $result"
	}
	result
}

// TODO - centralize somehow
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting, location?.timeZone).time
		def stop = timeToday(ending, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private getTimeLabel()
{
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
// TODO - End Centralize

private loadText() {
	switch ( actionType) {
		case "Mail has arrived":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/MailArrived.mp3", duration: "10"]
			break;
		case "Motion detected - Generic":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/MotionDetected.mp3", duration: "3"]
			break;
		case "Motion in basement":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/MotionBasement.mp3", duration: "3"]
			break;
		case "Motion in garage":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/MotionGarage.mp3", duration: "3"]
			break;
		case "Motion in front yard":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/MotionFrontYard.mp3", duration: "3"]
			break;
		case "Motion in back yard":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/MotionBackYard.mp3", duration: "3"]
			break;
		case "Motion at front door":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/MotionFrontDoor.mp3", duration: "3"]
			break;
		case "Motion at back door":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/MotionBackDoor.mp3", duration: "3"]
			break;
		case "Back door opened":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/OpenedBackDoor.mp3", duration: "3"]
			break;
		case "Basement door opened":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/OpenedBasementDoor.mp3", duration: "3"]
			break;
		case "Front door opened":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/OpenedFrontDoor.mp3", duration: "3"]
			break;
		case "Garage door opened":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/OpenedGarageDoor.mp3", duration: "3"]
			break;
		case "Patio door opened":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/OpenedPatioDoor.mp3", duration: "3"]
			break;
		case "Good Morning":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/GoodMorning.mp3", duration: "3"]
			break;
		case "Good Night":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/GoodNight.mp3", duration: "3"]
			break;
		case "Front door unlocked":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/UnlockedFrontDoor.mp3", duration: "6"]
			break;
		case "Back door unlocked":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/UnlockedBackDoor.mp3", duration: "6"]
			break;
		case "Basement door unlocked":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/UnlockedBasementDoor.mp3", duration: "6"]
			break;
		case "Patio door unlocked":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/UnlockedPatioDoor.mp3", duration: "6"]
			break;
		case "Water detected - Generic":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/WaterDetected.mp3", duration: "6"]
			break;
		case "Water detected in basement":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/WaterDetectedBasement.mp3", duration: "6"]
			break;
		case "Water detected in kitchen":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/WaterDetectedKitchen.mp3", duration: "6"]
			break;
		case "Water detected in garage":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/WaterDetectedGarage.mp3", duration: "6"]
			break;
		case "Smoke detected - Generic":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/SmokeDetected.mp3", duration: "6"]
			break;
		case "Smoke detected in kitchen":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/SmokeDetectedKitchen.mp3", duration: "6"]
			break;
		case "Smoke detected in garage":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/SmokeDetectedGarage.mp3", duration: "6"]
			break;	
		case "Smoke detected in basement":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/SmokeDetectedBasement.mp3", duration: "6"]
			break;
		case "Boyfriend is arriving":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/ArrivingBoyfriend.mp3", duration: "6"]
			break;
		case "Girlfriend is arriving":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/ArrivingGirlfriend.mp3", duration: "6"]
			break;
		case "Husband is arriving":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/ArrivingHusband.mp3", duration: "6"]
			break;
		case "Wife is arriving":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/ArrivingWife.mp3", duration: "6"]
			break;
		case "Son is arriving":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/ArrivingSon.mp3", duration: "6"]
			break;	
		case "Daughter is arriving":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/ArrivingDaughter.mp3", duration: "6"]
			break;	
		case "Welcome Home":
			state.sound = [uri: "http://s3.amazonaws.com/alexavoicenotifications/WelcomeHome.mp3", duration: "6"]
			break;
		default:
			if (message) {
				state.sound = textToSpeech(message instanceof List ? message[0] : message) // not sure why this is (sometimes) needed)
			}
			else {
				state.sound = textToSpeech("You selected the custom message option but did not enter a message in the $app.label Smart App")
			}
			break;
	}
}