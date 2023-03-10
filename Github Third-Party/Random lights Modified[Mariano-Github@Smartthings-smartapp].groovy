/**
 *  Copyright 2016 SmartThings
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
 *  Random lights on-off (from modified code of Gentle Wake Up)
 *
 *  Author: Steve Vlaminck and Modified by Mariano Colmenanejo 2021-02-04
 *  Date: 2013-03-11
 *
 * 	https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime.png
 * 	https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime%402x.png
 * 	Random lights on-off turns on-off your lights with random delay until app stop with Controller device.
 * 	This App is made from modification of original code of Gentle wake Up. 
 * 	After installation, select devices with "switch" capability and set min and max interval for random on-Off.
 * 	Handled with created Controller device with DTH "Gentle wake Up Controller".
 *
 *  Febraury 2021: To install in the new app, you must also create and publish for you DTH "Gentle wake Up Controller". Virtual switch created with your personalized name, that starts and stops the random lighst on-off. 
 *  When turning OFF the virtual switch, stop random on-off secuence and execute yours completion preferences. Virtual switch can be controlled from an automation or smartlighting app.
 */
definition(
	name: "Random lights Modified",
	namespace: "smartthings",
	author: "SmartThings mod by MCC",
	description: "Random turn on-off yours lights.",
	category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance@2x.png",
    pausable: true
)

preferences {
	page(name: "rootPage")
	page(name: "schedulingPage")
	page(name: "completionPage")
	page(name: "numbersPage")
	page(name: "controllerExplanationPage")
	page(name: "unsupportedDevicesPage")
}

def rootPage() {
	dynamicPage(name: "rootPage", title: "", install: true, uninstall: true) {

		section("What to On- Off") {
			input(name: "dimmers", type: "capability.switch", title: "Devices to Control", description: null, multiple: true, required: true, submitOnChange: true)
			if (dimmers) {
				href(name: "toNumbersPage", page: "numbersPage", title: "Random between Min and Max time", description: numbersPageHrefDescription(), state: "complete")
			}
		}

		if (dimmers) {

			section("Rules For Control") {
				href(name: "toSchedulingPage", page: "schedulingPage", title: "Automation", description: schedulingHrefDescription() ?: "Set rules for when to start", state: schedulingHrefDescription() ? "complete" : "")
				href(name: "toCompletionPage", title: "Completion Actions", page: "completionPage", state: completionHrefDescription() ? "complete" : "", description: completionHrefDescription() ?: "Set rules for what to do when Random completes")
			}

			section {
				// TODO: fancy label
				label(title: "Label This SmartApp", required: false, defaultValue: "", description: "Highly recommended", submitOnChange: true)
			}
		}
	}
}

def numbersPage() {
	dynamicPage(name:"numbersPage", title:"") {

		section {
			paragraph(name: "pGraph", title: "These lights will Control", fancyDeviceString(dimmers))
		}

		section {
            input(name: "MinDuration", type: "number", title: "Min. Time for Turn ON-OFF in minutes", description: "1", required: false, defaultValue: 1)
            input(name: "MaxDuration", type: "number", title: "Max. Time for Turn ON-OFF in minutes", description: "15", required: false, defaultValue: 15)
            
		}
	}
}

def weekdays() {
	["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"]
}

def weekends() {
	["Saturday", "Sunday"]
}

def schedulingPage() {
	dynamicPage(name: "schedulingPage", title: "Rules For Automatically Control Your Lights") {

		section("Allow Automatic Random on-off") {
			input(name: "days", type: "enum", title: "On These Days", description: "Every day", required: false, multiple: true, options: weekdays() + weekends())
		}

		section("Start Random on-off...") {
			input(name: "startTime", type: "time", title: "Start At This Time", description: null, required: false)
            input(name: "stopTime", type: "time", title: "Stop At This Time", description: null, required: false)
			input(name: "modeStart", title: "When Entering This Mode", type: "mode", required: false, mutliple: false, submitOnChange: true, description: null)
			if (modeStart) {
				input(name: "modeStop", title: "Stop when leaving '${modeStart}' mode", type: "bool", required: false)
			}
		}
	}
}

def completionPage() {
	dynamicPage(name: "completionPage", title: "Completion Rules") {

		section("Switches") {
			input(name: "completionSwitches", type: "capability.switch", title: "Set these switches", description: null, required: false, multiple: true, submitOnChange: true)
			if (completionSwitches) {
				input(name: "completionSwitchesState", type: "enum", title: "To", description: null, required: false, multiple: false, options: ["on", "off"], defaultValue: "on")
				input(name: "completionSwitchesLevel", type: "number", title: "Optionally, Set Dimmer Levels To", description: null, required: false, multiple: false, range: "(0..99)")
			}
		}

		section("Notifications") {
			input("recipients", "contact", title: "Send notifications to", required: false) {
				if (completionPhoneNumber) {
					input(name: "completionPhoneNumber", type: "phone", title: "Text This Number", description: "Phone number", required: false)
				}
				input(name: "completionPush", type: "bool", title: "Send A Push Notification", description: "Phone number", required: false)
			}
			input(name: "completionMusicPlayer", type: "capability.musicPlayer", title: "Speak Using This Music Player", required: false)
			input(name: "completionMessage", type: "text", title: "With This Message", description: null, required: false)
		}

		section("Location Modes") {
			input(name: "completionMode", type: "mode", title: "Change ${location.name} Mode To", description: null, required: false)
			//input(name: "completionPhrase", type: "enum", title: "Execute The Phrase", description: null, required: false, multiple: false, options: location.helloHome.getPhrases().label)
		}

		section("Delay") {
			input(name: "completionDelay", type: "number", title: "Delay This Many Minutes Before Executing These Actions", description: "0", required: false)
		}
	}
}

// ========================================================
// Handlers
// ========================================================

def installed() {
	log.debug "Random lights on-off' with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Random lights on-off' with settings: ${settings}"
	unschedule()

	def controller = getController()
	if (controller) {
		controller.label = app.label
	}

	initialize()
}

private initialize() {
	stop("settingsChange")

	if (startTime) {
		log.debug "scheduling routine to run at $startTime"
		schedule(startTime, "scheduledStart")
        //log.debug "starTime= ${startTime}"
	}
	if (stopTime) {
		log.debug "scheduling routine to run at $stopTime"
		schedule(stopTime, "scheduledStop")
        //log.debug "stopTime= ${stopTime}"
	}

	// TODO: make this an option
	subscribe(app, appHandler)

	subscribe(location, locationHandler)

	if (manualOverride) {
		//subscribe(dimmers, "switch.off", stopDimmersHandler)
	}

	if (!getAllChildDevices()) {
		// create controller device and set name to the label used here
		def dni = "${new Date().getTime()}"
		log.debug "app.label: ${app.label}" + " dni: ${dni}"
		addChildDevice("smartthings", "Gentle Wake Up Controller", dni, null, ["label": app.label])
		state.controllerDni = dni
	}
}

def appHandler(evt) {
	log.debug "appHandler evt: ${evt.value}"
	if (evt.value == "touch") {
		if (atomicState.running) {
			stop("appTouch")
		} else {
			start("appTouch")
		}
	}
}

def locationHandler(evt) {
	log.debug "locationHandler evt: ${evt.value}"

	if (!modeStart) {
		return
	}

	def isSpecifiedMode = (evt.value == modeStart)
	def modeStopIsTrue = (modeStop && modeStop != "false")

	if (isSpecifiedMode && canStartAutomatically()) {
		start("modeChange")
	} else if (!isSpecifiedMode && modeStopIsTrue) {
		stop("modeChange")
	}
}

def stopDimmersHandler(evt) {
    return
}

// ========================================================
// Scheduling
// ========================================================

def scheduledStart() {
	if (canStartAutomatically()) {
		start("schedule")
	}
}

def scheduledStop() {
 stop("schedule")
}

public def start(source) {
	// Not reSTART dimming when is running. For example, when smartlighting app resend activation for virtual switch (28/01/2021 MCC)
    if (atomicState.runCounter >= 1) {
      return
    }
    log.trace "START" + "${source}"

	sendStartEvent(source)

	atomicState.running = true
	atomicState.runCounter = 1
	atomicState.start = new Date().getTime()
    
    //log.debug "starTime= ${startTime}"
    //log.debug "stopTime= ${stopTime}"
    //log.debug "start= ${atomicState.start}"

// go to schedule ON-OFF
    //controlOnOff()
    lightsControl()
}

public def stop(source) {
	log.trace "STOP" + "${source}"

	sendStopEvent(source)
    
    completion()

	atomicState.running = false
	atomicState.start = 0
	atomicState.runCounter = 0
	unschedule()
    log.debug "STOPPED"
}

//===========================
// schedule ON-OFF 
//===========================

private controlOnOff(timeCalculated) {

	if (!atomicState.running) {
		return
	}

  log.debug "Control ON-OFF: START New Delay"
  def newTime = timeCalculated
  log.debug "newTime Running= ${newTime / 60} minutes"

 // wait random time for next on-off change
  runIn(newTime, 'lightsControl', [overwrite: true])
}

// change devices on-off
private lightsControl() {
 log.debug "Control lights"
	
    // Calculate random device to control between dimmers.size
    def deviceNum = new Random().nextInt(dimmers.size())
    log.debug "deviceNum= ${deviceNum}"

    def dimmer = dimmers[deviceNum] 
    log.debug "dimmer= ${dimmer}"
    def Increment = 0
    if (dimmer.currentValue("switch") == "off") {
			dimmer.on()
            Increment = 0
           log.debug "Turn ON"
       } else {
       dimmer.off()
       Increment = 3600
      log.debug "Turn OFF"
     }

 //controlOnOff()
 log.debug "Increment= ${Increment}"
 //timeCalculate(Increment)
//}

// Calculate new random time on-off
//private timeCalculate(increment) {
 def Lower = 1
 def Upper = 15
 log.debug "Calculando"
 if (MinDuration == null) {
  Lower = 1 
 } else {
    Lower = MinDuration
 }
 if (MaxDuration == null) {
  Upper = 15
 } else {
    Upper = MaxDuration
 }
 
 log.debug "Def.Max= ${Upper} minutes, Def.Min= ${Lower} minutes."

 // Calculate random newtime between minDuration and MaxDuration
 def timeCalculated = new Random().nextInt((Upper * 60) - (Lower * 60)) + (Lower * 60) + Increment

 //log.debug "Calculated newTime= ${timeCalculated / 60} minutes"
 //return timeCalculate
 controlOnOff(timeCalculated)
}

// ========================================================
// Controller
// ========================================================

def sendStartEvent(source) {
	log.trace "sendStartEvent(${source})"
	def eventData = [
			name: "sessionStatus",
			value: "running",
            descriptionText: "${app.label} has started dimming",
			displayed: true,
			linkText: app.label,
			isStateChange: true
	]
	if (source == "modeChange") {
		eventData.descriptionText += " because of a mode change"
	} else if (source == "schedule") {
		eventData.descriptionText += " as scheduled"
	} else if (source == "appTouch") {
		eventData.descriptionText += " because you pressed play on the app"
	} else if (source == "controller") {
		eventData.descriptionText += " because you pressed play on the controller"
	}

	sendControllerEvent(eventData)
}

def sendStopEvent(source) {
	log.trace "sendStopEvent(${source})"
	def eventData = [
			name: "sessionStatus",
			value: "stopped",
            descriptionText: "${app.label} has stopped dimming",
			displayed: true,
			linkText: app.label,
			isStateChange: true
	]
	if (source == "modeChange") {
		eventData.descriptionText += " because of a mode change"
		eventData.value += "cancelled"
	} else if (source == "schedule") {
		eventData.descriptionText = "${app.label} has finished dimming"
	} else if (source == "appTouch") {
		eventData.descriptionText += " because you pressed play on the app"
		eventData.value += "cancelled"
	} else if (source == "controller") {
		eventData.descriptionText += " because you pressed stop on the controller"
		eventData.value += "cancelled"
	} else if (source == "settingsChange") {
		eventData.descriptionText += " because the settings have changed"
		eventData.value += "cancelled"
	} else if (source == "manualOverride") {
		eventData.descriptionText += " because the dimmer was manually turned off"
		eventData.value += "cancelled"
	}

	// send 100% completion event
	sendTimeRemainingEvent(100)

	// send a non-displayed 0% completion to reset tiles
	sendTimeRemainingEvent(0, false)

	// send sessionStatus event last so the event feed is ordered properly
	sendControllerEvent(eventData)
}

def sendTimeRemainingEvent(percentComplete, displayed = true) {
	log.trace "sendTimeRemainingEvent(${percentComplete})"

	def percentCompleteEventData = [
			name: "percentComplete",
			value: percentComplete as int,
			displayed: displayed,
			isStateChange: true
	]
	sendControllerEvent(percentCompleteEventData)

	def duration = sanitizeInt(duration, 30)
	def timeRemaining = duration - (duration * (percentComplete / 100))
	def timeRemainingEventData = [
			name: "timeRemaining",
			value: displayableTime(timeRemaining),
			displayed: displayed,
			isStateChange: true
	]
	sendControllerEvent(timeRemainingEventData)
}

def sendControllerEvent(eventData) {
	def controller = getController()
	if (controller) {
		controller.controllerEvent(eventData)
	}
}

def getController() {
	def dni = state.controllerDni
	if (!dni) {
		log.warn "no controller dni"
		return null
	}
	def controller = getChildDevice(dni)
	if (!controller) {
		log.warn "no controller"
		return null
	}
	log.debug "controller: ${controller}"
	return controller
}

// ========================================================
// Setting levels
// ========================================================


// ========================================================
// Completion
// ========================================================

private completion() {
	log.trace "Starting completion block"

	if (!atomicState.running) {
		return
	}

	handleCompletionSwitches()

	handleCompletionMessaging()

	handleCompletionModesAndPhrases()
}

private handleCompletionSwitches() {
	completionSwitches.each { completionSwitch ->

		def isDimmer = hasSetLevelCommand(completionSwitch)

		if (completionSwitchesLevel && isDimmer) {
			completionSwitch.setLevel(completionSwitchesLevel)
		} else {
			def command = completionSwitchesState ?: "on"
			completionSwitch."${command}"()
		}
	}
}

private handleCompletionMessaging() {
	if (completionMessage) {
		if (location.contactBookEnabled) {
			sendNotificationToContacts(completionMessage, recipients)
		} else {
			if (completionPhoneNumber) {
				sendSms(completionPhoneNumber, completionMessage)
			}
			if (completionPush) {
				sendPush(completionMessage)
			}
		}
		if (completionMusicPlayer) {
			speak(completionMessage)
		}
	}
}

private handleCompletionModesAndPhrases() {

	if (completionMode) {
		setLocationMode(completionMode)
	}

	if (completionPhrase) {
		location.helloHome.execute(completionPhrase)
	}

}

def speak(message) {
	def sound = textToSpeech(message)
	def soundDuration = (sound.duration as Integer) + 2
	log.debug "Playing $sound.uri"
	completionMusicPlayer.playTrack(sound.uri)
	log.debug "Scheduled resume in $soundDuration sec"
	runIn(soundDuration, resumePlaying, [overwrite: true])
}

def resumePlaying() {
	log.trace "resumePlaying()"
	def sonos = completionMusicPlayer
	if (sonos) {
		def currentTrack = sonos.currentState("trackData").jsonValue
		if (currentTrack.status == "playing") {
			sonos.playTrack(currentTrack)
		} else {
			sonos.setTrack(currentTrack)
		}
	}
}

// ========================================================
// Helpers
// ========================================================

def setLevelsInState() {
	def startLevels = [:]
	dimmers.each { dimmer ->
		if (usesOldSettings()) {
			startLevels[dimmer.id] = defaultStart()
		} else if (hasStartLevel()) {
			startLevels[dimmer.id] = startLevel
		} else {
			def dimmerIsOff = dimmer.currentValue("switch") == "off"
			startLevels[dimmer.id] = dimmerIsOff ? 0 : dimmer.currentValue("level")
		}
	}

	atomicState.startLevels = startLevels
}

def canStartAutomatically() {

	def today = new Date().format("EEEE")
	log.debug "today: ${today}, days: ${days}"

	if (!days || days.contains(today)) {// if no days, assume every day
		return true
	}

	log.trace "should not run"
	return false
}

int totalRunTimeMillis() {
	int minutes = sanitizeInt(duration, 30)
	convertToMillis(minutes)
}

int convertToMillis(minutes) {
	def seconds = minutes * 60
	def millis = seconds * 1000
	return millis
}

def timeRemaining(percentComplete) {
	def normalizedPercentComplete = percentComplete / 100
	def duration = sanitizeInt(duration, 30)
	def timeElapsed = duration * normalizedPercentComplete
	def timeRemaining = duration - timeElapsed
	return timeRemaining
}

int millisToEnd(percentComplete) {
	convertToMillis(timeRemaining(percentComplete))
}

String displayableTime(timeRemaining) {
	def timeString = "${timeRemaining}"
	def parts = timeString.split(/\./)
	if (!parts.size()) {
		return "0:00"
	}
	def minutes = parts[0]
	if (parts.size() == 1) {
		return "${minutes}:00"
	}
	def fraction = "0.${parts[1]}" as double
	def seconds = "${60 * fraction as int}".padLeft(2, "0")
	return "${minutes}:${seconds}"
}

private dimmersContainUnsupportedDevices() {
	def found = dimmers.find { hasSetLevelCommand(it) == false }
	return found != null
}

private hasSetLevelCommand(device) {
	return hasCommand(device, "setLevel")
}

private hasSetColorCommand(device) {
	return hasCommand(device, "setColor")
}

private hasCommand(device, String command) {
	return (device.supportedCommands.find { it.name == command } != null)
}

private dimmersWithSetColorCommand() {
	def colorDimmers = []
	dimmers.each { dimmer ->
		if (hasSetColorCommand(dimmer)) {
			colorDimmers << dimmer
		}
	}
	return colorDimmers
}

private int sanitizeInt(i, int defaultValue = 0) {
	try {
		if (!i) {
			return defaultValue
		} else {
			return i as int
		}
	}
	catch (Exception e) {
		log.debug e
		return defaultValue
	}
}

private completionDelaySeconds() {
	int completionDelayMinutes = sanitizeInt(completionDelay)
	int completionDelaySeconds = (completionDelayMinutes * 60)
	return completionDelaySeconds ?: 0
}

private stepDuration() {
	int minutes = sanitizeInt(duration, 30)
	int stepDuration = (minutes * 60) / 100
	return stepDuration ?: 1
}

private debug(message) {
	log.debug "${message}\nstate: ${state}"
}

public smartThingsDateFormat() { "yyyy-MM-dd'T'HH:mm:ss.SSSZ" }

public humanReadableStartDate() {
	new Date().parse(smartThingsDateFormat(), startTime).format("h:mm a", timeZone(startTime))
}

public humanReadableStopDate() {
	new Date().parse(smartThingsDateFormat(), stopTime).format("h:mm a", timeZone(stopTime))
}

def fancyString(listOfStrings) {

	def fancify = { list ->
		return list.collect {
			def label = it
			if (list.size() > 1 && it == list[-1]) {
				label = "and ${label}"
			}
			label
		}.join(", ")
	}

	return fancify(listOfStrings)
}

def fancyDeviceString(devices = []) {
	fancyString(devices.collect { deviceLabel(it) })
}

def deviceLabel(device) {
	return device.label ?: device.name
}

def schedulingHrefDescription() {

	def descriptionParts = []
	if (days) {
		if (days == weekdays()) {
			descriptionParts << "On weekdays,"
		} else if (days == weekends()) {
			descriptionParts << "On weekends,"
		} else {
			descriptionParts << "On ${fancyString(days)},"
		}
	}

	descriptionParts << "${fancyDeviceString(dimmers)} will start random cycle"

	if (startTime) {
		descriptionParts << "at ${humanReadableStartDate()}"
	}
    if (stopTime) {
		descriptionParts << "and will Stop at ${humanReadableStopDate()}"
	}
 
	if (modeStart) {
		if (startTime) {
			descriptionParts << "or"
		}
		descriptionParts << "when ${location.name} enters '${modeStart}' mode"
	}

	if (descriptionParts.size() <= 1) {
		// dimmers will be in the list no matter what. No rules are set if only dimmers are in the list
		return null
	}

	return descriptionParts.join(" ")
}

def completionHrefDescription() {

	def descriptionParts = []
	def example = "Switch1 will be turned on. Switch2, Switch3, and Switch4 will be dimmed to 50%. The message '<message>' will be spoken, sent as a text, and sent as a push notification. The mode will be changed to '<mode>'. The phrase '<phrase>' will be executed"

	if (completionSwitches) {
		def switchesList = []
		def dimmersList = []


		completionSwitches.each {
			def isDimmer = completionSwitchesLevel ? hasSetLevelCommand(it) : false

			if (isDimmer) {
				dimmersList << deviceLabel(it)
			}

			if (!isDimmer) {
				switchesList << deviceLabel(it)
			}
		}


		if (switchesList) {
			descriptionParts << "${fancyString(switchesList)} will be turned ${completionSwitchesState ?: 'on'}."
		}

		if (dimmersList) {
			descriptionParts << "${fancyString(dimmersList)} will be dimmed to ${completionSwitchesLevel}%."
		}

	}

	if (completionMessage && (completionPhoneNumber || completionPush || completionMusicPlayer)) {
		def messageParts = []

		if (completionMusicPlayer) {
			messageParts << "spoken"
		}
		if (completionPhoneNumber) {
			messageParts << "sent as a text"
		}
		if (completionPush) {
			messageParts << "sent as a push notification"
		}

		descriptionParts << "The message '${completionMessage}' will be ${fancyString(messageParts)}."
	}

	if (completionMode) {
		descriptionParts << "The mode will be changed to '${completionMode}'."
	}

	if (completionPhrase) {
		descriptionParts << "The phrase '${completionPhrase}' will be executed."
	}

	return descriptionParts.join(" ")
}

def numbersPageHrefDescription() {
    def title = "Between ${MinDuration} and ${MaxDuration} minutes, Randomly One of the All Devices Selected turn on-off or off-on"
	return title
}

def usesOldSettings() {
	!hasEndLevel()
}

def hasStartLevel() {
	return (startLevel != null && startLevel != "")
}

def hasEndLevel() {
	return (endLevel != null && endLevel != "")
}
