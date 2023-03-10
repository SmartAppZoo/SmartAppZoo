/**
 *  Copyright 2015 SmartThings
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
 *  Sonos Custom Message
 *
 *  Author: SmartThings
 *  Date: 2015-8-31
 *  Custom Sonos Notify With Sound edition with Morgan Freeman voice impersonation
 *  2015-09-03 - Added 24 new phrases and removed any without Morgan Freeman voice
 */
definition(
	name: "Morgan Freeman Notify with Sound",
	namespace: "smartthings",
	author: "SmartThings",
	description: "Play a sound or custom message through your Sonos when the mode changes or other events occur.",
	category: "SmartThings Labs",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos@2x.png"
)

preferences {
	page(name: "mainPage", title: "Play a message on your Sonos when something happens", install: true, uninstall: true)
	page(name: "chooseTrack", title: "Select a song or station")
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
			input "actionType", "enum", title: "Action?", required: true, defaultValue: "Custom Message", options: [
				"Mail has arrived",
				"Motion detected",
				"Motion in basement",
				"Motion in garage",
				"Motion in game room",
				"Motion in bedroom",
				"Motion in living room",
				"Motion in theater",
				"Motion in wine cellar",
				"Motion in kitchen",
				"A door opened",
				"A door closed",
				"Front door opened",
				"Front door closed",
				"Front door locked",
				"Front door unlocked",
				"Back door opened",
				"Back door closed",
				"Back door locked",
				"Back door unlocked",
				"Basement door opened",
				"Basement door closed",
				"Basement door locked",
				"Basement door unlocked",	
				"Garage door opened",	
				"Garage door closed",	
				"Patio door opened",	
				"Patio door closed",	
				"Patio door locked",	
				"Patio door unlocked",
				"Cleaning Supplies Closet Opened",
				"Liquor Cabinet Opened",	
				"Water detected in basement",	
				"Water detected in kitchen",	
				"Water detected in garage",
				"Smartthings detected a flood",	
				"Smoke detected in kitchen",	
				"Smoke detected in garage",	
				"Smoke detected in basement",
				"Smartthings detected carbon monoxide",
				"Smartthings detected smoke",
				"Someone is arriving",	
				"Boss is arriving",	
				"Boyfriend is arriving",	
				"Coworker is arriving",	
				"Daughter is arriving",	
				"Friend is arriving",	
				"Girlfriend is arriving",	
				"Roommate is arriving",	
				"Son is arriving",	
				"Wife is arriving",		
				"Welcome Home",
				"Good Morning",
				"Good Night",
				"Vacate the premises",
				"Searching for car keys",
				"Setting the mood",
				"Starting Movie Mode",
				"Starting Party Mode",
				"Starting Romance Mode",
				"Turning off all the lights",
				"Turning off the TV",
				"Turning off the air conditioner",
				"Turning off the bar lights",
				"Turning off the chandelier",
				"Turning off the family room lights",
				"Turning off the hallway lights",
				"Turning off the kitchen light",
				"Turning off the light",
				"Turning off the lights",
				"Turning off the mood lights",
				"Turning on the TV",
				"Turning on the air conditioner",
				"Turning on the bar lights",
				"Turning on the chandelier",
				"Turning on the family room lights",
				"Turning on the hallway lights",
				"Turning on the kitchen light",
				"Turning on the light",
				"Turning on the lights",
				"Turning on the mood lights",
				"Turning on the light",
				"Turning on the lights",
				"Turning on the mood lights"]
			input "message","text",title:"Play this message", required:false, multiple: false
		}
		section {
			input "sonos", "capability.musicPlayer", title: "On this Sonos player", required: true
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
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/mail+has+arrived.mp3", duration: "10"]
			break;
		case "A door opened":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/a+door+opened.mp3", duration: "10"]
			break;
		case "A door closed":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/a+door+closed.mp3", duration: "10"]
			break;
		case "Motion detected":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/motion+detected+generic.mp3", duration: "3"]
			break;
		case "Motion in basement":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Motion+in+Basement.mp3", duration: "3"]
			break;
		case "Motion in garage":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Motion+in+Garage.mp3", duration: "3"]
			break;
		case "Motion in game room":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/motion+in+game+room.mp3", duration: "3"]
			break;
		case "Motion in bedroom":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/motion+in+bedroom.mp3", duration: "3"]
			break;
		case "Motion in living room":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/motion+in+living+room.mp3", duration: "3"]
			break;
		case "Motion in theater":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/motion+in+theater.mp3", duration: "3"]
			break;
		case "Motion in wine cellar":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/motion+in+wine+cellar.mp3", duration: "3"]
			break;
		case "Motion in kitchen":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/motion+in+kitchen.mp3", duration: "3"]
			break;
		case "Back door opened":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/back+door+opened.mp3", duration: "3"]
			break;
		case "Basement door opened":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Basement+Door+has+been+opened.mp3", duration: "3"]
			break;
		case "Cleaning Supplies Closet Opened":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Cleaning+Supplies+Cabinet+has+been+opened.mp3", duration: "3"]
			break;
		case "Front door opened":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Front+Door+has+been+opened.mp3", duration: "3"]
			break;
		case "Good Morning":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Good+morning.mp3", duration: "3"]
			break;
		case "Good Night":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Good+night.mp3", duration: "3"]
			break;
		case "Liquor Cabinet Opened":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Liquor+Cabinet+has+been+opened.mp3", duration: "3"]
			break;
		case "Patio door opened":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Patio+Door+has+been+opened.mp3", duration: "3"]
			break;
		case "Searching for car keys":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Searching+for+car+keys.mp3", duration: "3"]
			break;
		case "Setting the mood":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Setting+the+mood.mp3", duration: "3"]
			break;
		case "Starting Movie Mode":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Starting+Movie+Mode.mp3", duration: "3"]
			break;
		case "Starting Party Mode":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Starting+Party+Mode.mp3", duration: "3"]
			break;
		case "Starting Romance Mode":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Starting+Romance+Mode.mp3", duration: "3"]
			break;
		case "Turning off all the lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+all+the+lights.mp3", duration: "3"]
			break;
		case "Turning off the TV":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+TV.mp3", duration: "3"]
			break;
		case "Turning off the air conditioner":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+air+conditioner.mp3", duration: "3"]
			break;
		case "Turning off the bar lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+bar+lights.mp3", duration: "3"]
			break;
		case "Turning off the chandelier":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+chandelier.mp3", duration: "3"]
			break;
		case "Turning off the family room lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+family+room+lights.mp3", duration: "3"]
			break;
		case "Turning off the hallway lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+hallway+lights.mp3", duration: "3"]
			break;
		case "Turning off the kitchen light":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+kitchen+light.mp3", duration: "3"]
			break;
		case "Turning off the light":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+light.mp3", duration: "3"]
			break;
		case "Turning off the lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+lights.mp3", duration: "3"]
			break;
		case "Turning off the mood lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+off+the+mood+lights.mp3", duration: "3"]
			break;
		case "Turning on the TV":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+TV.mp3", duration: "3"]
			break;
		case "Turning on the air conditioner":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+air+conditioner.mp3", duration: "3"]
			break;
		case "Turning on the bar lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+bar+lights.mp3", duration: "3"]
			break;
		case "Turning on the chandelier":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+chandelier.mp3", duration: "3"]
			break;
		case "Turning on the family room lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+family+room+lights.mp3", duration: "3"]
			break;
		case "Turning on the hallway lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+hallway+lights.mp3", duration: "3"]
			break;
		case "Turning on the kitchen light":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+kitchen+light.mp3", duration: "3"]
			break;
		case "Turning on the light":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+light.mp3", duration: "3"]
			break;
		case "Turning on the lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+lights.mp3", duration: "3"]
			break;
		case "Turning on the mood lights":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Turning+on+the+mood+lights.mp3", duration: "3"]
			break;
		case "Vacate the premises":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Vacate+the+premesis.mp3", duration: "6"]
			break;
		case "Front door closed":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/front+door+closed.mp3", duration: "6"]
			break;
		case "Front door locked":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/front+door+locked.mp3", duration: "6"]
			break;
		case "Front door unlocked":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/front+door+unlocked.mp3", duration: "6"]
			break;
		case "Back door closed":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/back+door+closed.mp3", duration: "6"]
			break;
		case "Back door locked":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/back+door+locked.mp3", duration: "6"]
			break;
		case "Back door unlocked":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/back+door+unlocked.mp3", duration: "6"]
			break;
		case "Basement door closed":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/basement+door+closed.mp3", duration: "6"]
			break;
		case "Basement door locked":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/basement+door+locked.mp3", duration: "6"]
			break;
		case "Basement door unlocked":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/basement+door+unlocked.mp3", duration: "6"]
			break;
		case "Garage door opened":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/garage+door+opened.mp3", duration: "6"]
			break;
		case "Garage door closed":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/garage+door+closed.mp3", duration: "6"]
			break;
		case "Patio door closed":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/patio+door+closed.mp3", duration: "6"]
			break;
		case "Patio door locked":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/patio+door+locked.mp3", duration: "6"]
			break;
		case "Patio door unlocked":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/patio+door+unlocked.mp3", duration: "6"]
			break;
		case "Water detected in basement":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Water+in+basement.mp3", duration: "6"]
			break;
		case "Water detected in kitchen":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Water+in+kitchen.mp3", duration: "6"]
			break;
		case "Water detected in garage":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Water+in+garage.mp3", duration: "6"]
			break;
		case "Smoke detected in kitchen":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Water+in+kitchen.mp3", duration: "6"]
			break;
		case "Smoke detected in garage":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Smoke+in+garage.mp3", duration: "6"]
			break;	
		case "Smoke detected in basement":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Smoke+in+basement.mp3", duration: "6"]
			break;
		case "Someone is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Someone+is+arriving.mp3", duration: "6"]
			break;
		case "Boss is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Boss+is+arriving.mp3", duration: "6"]
			break;
		case "Boyfriend is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Boyfriend+is+arriving.mp3", duration: "6"]
			break;
		case "Coworker is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Coworker+is+arriving.mp3", duration: "6"]
			break;
		case "Daughter is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Daughter+is+arriving.mp3", duration: "6"]
			break;
		case "Friend is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Friend+is+arriving.mp3", duration: "6"]
			break;
		case "Girlfriend is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Girlfriend+arriving.mp3", duration: "6"]
			break;
		case "Roommate is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/roommate+is+arriving.mp3", duration: "6"]
			break;
		case "Son is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/son+is+arriving.mp3", duration: "6"]
			break;		
		case "Wife is arriving":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/wife+is+arriving.mp3", duration: "6"]
			break;
		case "Smartthings detected a flood":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/SmartThings+detected+flood.mp3", duration: "6"]
			break;
		case "Smartthings detected carbon monoxide":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/SmartThings+detected+CO.mp3", duration: "6"]
			break;
		case "Smartthings detected smoke":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/SmartThings+detected+smoke.mp3", duration: "6"]
			break;
		case "Welcome Home":
			state.sound = [uri: "https://s3.amazonaws.com/smartthingssmartapps/Welcome+Home.mp3", duration: "6"]
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