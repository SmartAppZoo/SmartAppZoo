/**
 *  Laundry Monitor
 *
 *  Copyright 2016 Dario Rossi
 *  Based on from here: https://github.com/bmmiller/SmartThings/blob/master/smartapp.laundrymonitor/smartapp.laundrymonitor.groovy
 *  Changes:
 *  - Added ability to show date timestamps for current cycle or if no cycle active, last cycle
 *  - Added a trigger to run code at least one more time for wait time after wattage drops below threshold since if it doesn't change after that last drop below, it wouldn't end cycle
 *  - Added repeat notifications is laundry done not cleared with selected device trigger, and only when selected modes are active.
 *  - Added ability to select which devices used for notifications (speech, music and lights - switches and colour capable)
 *  - Added ability to select which device resets the laundry complete
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
 
import groovy.time.* 
 
definition(
    name: "Laundry Monitor",
    namespace: "dario.rossi",
    author: "Dario Rossi",
    description: "This application is a modification of the SmartThings Laundry Monitor SmartApp.  Instead of using a vibration sensor, this utilizes Power (Wattage) draw from an Aeon Smart Energy Meter.",
    category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Appliances/appliances8-icn.png",
	iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Appliances/appliances8-icn@2x.png")
    //iconUrl: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-HotTubTuner.png",
    //iconX2Url: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-HotTubTuner%402x.png")
	//iconUrl: "http://www.vivevita.com/wp-content/uploads/2009/10/recreation_sign_laundry.png",
    //iconX2Url: "http://www.vivevita.com/wp-content/uploads/2009/10/recreation_sign_laundry.png")


preferences {
	page(name: "mainPage", title: "Laundry Monitor Setup", install: true, uninstall: true)
}
def mainPage() {
	dynamicPage(name: "mainPage") {
        section("Laundry Status") {
            def currentLaundryStatus = ""
            def startDate = ""
            def stopDate = ""
            def emptyDate = ""
            def stoppedAtDateTime = "Unknown"
            def startedAtDateTime = "Unknown"
            def emptiedAtDateTime = "Unknown"
            def df = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a")
            df.setTimeZone(location.timeZone)

            if ( atomicState.timeStartedAt != null ) {
                startDate = (atomicState.timeStartedAt)
                startedAtDateTime = df.format(startDate)
            }
            if ( atomicState.timeStoppedAt != null ) {
                stopDate = (atomicState.timeStoppedAt)
                stoppedAtDateTime = df.format(stopDate)
            }
            if ( atomicState.timeEmptiedAt != null ) {
                emptyDate = (atomicState.timeEmptiedAt)
                emptiedAtDateTime = df.format(emptyDate)
            }

            if ( atomicState.machineState.equals("Running") || atomicState.machineState.equals("midCycleCheck") ) {
                    currentLaundryStatus = "Cycle Active\nStarted at:${startedAtDateTime}\nPrevious stopped at: ${stoppedAtDateTime}\nPrevious load emptied at: ${emptiedAtDateTime}\n"
            }
            else if (atomicState.machineState.equals("notRunning")) {
                	currentLaundryStatus = "Cycle Not Active.\nLast cycle Info:\nStarted at ${startedAtDateTime}\nEnded at ${stoppedAtDateTime}\nEmptied at ${emptiedAtDateTime}"
			}
            else if (atomicState.machineState.equals("needsToBeEmptied")) {
                	currentLaundryStatus = "Cycle Not Active.\nLast cycle Info:\nStarted at ${startedAtDateTime}\nEnded at ${stoppedAtDateTime}\nLoad needs to be emptied."
			}
            paragraph currentLaundryStatus.trim()
        }	
        section("Tell me when this washer/dryer has stopped:") {
            input "selectedWasherOrDryer", "capability.powerMeter"
        }
        def anythingSet = anythingSet()
		if (anythingSet) {
			section("Use this/these device(s) to reset laundry complete notifications.  If no device selected, then will reset automatically after first notification.") {
				ifSet "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
				ifSet "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
				ifSet "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
				ifSet "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
				ifSet "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
				ifSet "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
				ifSet "button1", "capability.button", title: "Button Press", required:false, multiple:true
			}
		}
		def hideablesection = anythingSet || app.installationState == "COMPLETE"
		def sectionTitle = anythingSet ? "Select additional triggers" : "Use this/these additional device(s) to reset laundry complete notifications.  If no device selected, then will reset automatically after first notification."
		
        section(sectionTitle, hideable: hideableSection, hidden: true) {
			ifUnset "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
			ifUnset "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
			ifUnset "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
			ifUnset "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
			ifUnset "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
			ifUnset "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
			ifUnset "button1", "capability.button", title: "Button Press", required:false, multiple:true		}

        section("Notifications") {
            input "sendPushMessage", "bool", title: "Push Notifications?"
            input "phone", "phone", title: "Send a text message?", required: false
            paragraph "For multiple SMS recipients, separate phone numbers with a semicolon(;)"      
        }
		section("Notification Messages") {
            input "sendStartMessage", "bool", title: "Send a cycle start message?", defaultValue: true, required: false
            input "startMessage", "text", title: "Cycle Started Message", description: "Cycle Start Message", defaultValue: "Laundry Cycle Started.", required: true
            input "sendCompleteMessage", "bool", title: "Send a cycle complete message?", defaultValue: true, required: false
            input "completionMessage", "text", title: "Cycle Ended Message", description: "Cycle Ended Message", defaultValue: "Laundry Cycle Complete.", required: true
        }
        section("System Variables") {
            input "minimumWattage", "decimal", title: "Minimum running wattage", required: false, defaultValue: 50
            input "minimumOffTime", "decimal", title: "Minimum amount of below wattage time to trigger off (secs)", required: false, defaultValue: 60
            input "repeatNotificationsTime", "decimal", title: "Time in between repeat notifications if laundry not emptied/reset based on selected device. If no device selected, will auto reset. (secs)", required: false, defaultValue: 300
            input "modesRepeatNotifications", "mode", title: "Only send repeat complete notifications in these mode(s)", multiple: true
		}        
        section ("More Notification Options", hidden: hideOptionsSection(), hideable: true) {
            input "switches", "capability.switch", title: "Blink these switches:", required:false, multiple:true
            input "numFlashes", "number", title: "Number of times to flash", description: "1-50 times", required: false, range: "1..50"
			input "flashOnFor", "number", title: "On For (default 1000ms)", description: "milliseconds", required: false
            input "flashOffFor", "number", title: "Off For (default 1000ms)", description: "milliseconds", required: false
            input "speech", "capability.speechSynthesis", title:"Announce messages via these devices: ", multiple: true, required: false
            input "sonos", "capability.musicPlayer", title: "Play messages on these speakers:", required: true
            input "resumePlaying", "bool", title: "Resume currently playing music after notification", required: false, defaultValue: true
            input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false, defaultValue: 50
        }
		section("Choose light effects if available for specific light...", hidden: colorSwitches, hideable: true) {
			input "color", "enum", title: "Color?", required: false, multiple:false, options: ["White","Red","Green","Blue","Yellow","Orange","Purple","Pink"]
			input "lightLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
		}
        section(mobileOnly:true) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
	}
}

def installed() {
	log.debug "SmartApp Installed with settings: ${settings}"
    log.debug "SmartApp Installed with atomicState: ${atomicState}"
	initialize()
}

def updated() {
	log.debug "SmartApp Updated with settings: ${settings}"
    log.debug "SmartApp Updated with atomicState: ${atomicState}"
	unsubscribe()
	initialize()
}

def initialize() {
    log.debug "SmartApp Initializing"    
    subscribe(selectedWasherOrDryer, "power", powerInputHandler)
	subscribe(contact, "contact.open", resetNotificationHandler)
	subscribe(contactClosed, "contact.closed", resetNotificationHandler)
	subscribe(acceleration, "acceleration.active", resetNotificationHandler)
	subscribe(motion, "motion.active", resetNotificationHandler)
	subscribe(mySwitch, "switch.on", resetNotificationHandler)
	subscribe(mySwitchOff, "switch.off", resetNotificationHandler)
	subscribe(button1, "button.pushed", resetNotificationHandler)
	// If Machine State is unknown, set the default value
	if (atomicState.machineState == null ) {
		log.debug "Initialize - Setting default machine state"
        atomicState.machineState = "notRunning"
		log.debug "Initialize - atomicState: ${atomicState}"
	}
	// Only check washing state if midcycle was on while app settings are being modified
    if (atomicState.machineState.equals("midCycleCheck")) {
		log.debug "Initialize - atomicState: ${atomicState}"
        log.debug "Initialize runIn: minimumOffTime: ${minimumOffTime}, checkMachineState"
        runIn(minimumOffTime, "checkMachineState")
	}
    // Check empty state if wash not emptied state on while app settings are being modified
    if (atomicState.machineState.equals("needsToBeEmptied")) {
		log.debug "Initialize - atomicState: ${atomicState}"
		log.debug "Initialize - runIn: repeatNotificationsTime: ${repeatNotificationsTime}, checkMachineState"
    	runIn(repeatNotificationsTime, "checkMachineState")
	}
    //initialize light states
	atomicState.previous = [:]
    //initialize light capabilities
    atomicState.capabilities = [:]
	//store capabilities of selected switches if they have color, level or just on/off
    switches.each {
        if (it.hasCapability("Color Control")) {
            state.capabilities[it.id] = "color"
        }
        if (it.hasCapability("Switch Level")) {
            state.capabilities[it.id] = "level"
        }
        else {
            state.capabilities[it.id] = "switch"
        }
    }
}

def checkMachineState() {
	def latestPower = selectedWasherOrDryer.currentValue("power")
    log.debug "checkMachineState - Power: ${latestPower}W, minimumWattage: ${minimumWattage}"
    log.debug "checkMachineState - Start atomicState: ${atomicState}"
    
	if ( (atomicState.machineState.equals("notRunning") || atomicState.machineState.equals("needsToBeEmptied") ) && (latestPower > minimumWattage) ) {
    	log.info "checkMachineState - Cycle started."
		atomicState.machineState = "Running"
        atomicState.timeStartedAt = now()
        atomicState.timeStoppedAt = null
        atomicState.timeEmptiedAt = null
		if (sendStartMessage) {
        	log.trace "checkMachineState - Calling Cycle Start Notifications"
			sendNotifications(startMessage)
        }
	}
	else if ( atomicState.machineState.equals("Running") && (latestPower < minimumWattage) ) {
		log.info "checkMachineState - Mid Cycle Check"
		atomicState.machineState = "midCycleCheck"
		atomicState.midCycleTime = now()
		log.debug "checkMachineState - runIn: minimumOffTime: ${minimumOffTime}, checkMachineState"
		runIn(minimumOffTime, "checkMachineState")
	}
	else if ( atomicState.machineState.equals("midCycleCheck") && (latestPower > minimumWattage) ) {
	    log.info "checkMachineState - Resetting Mid Cycle Check."
        atomicState.machineState = "Running"
		atomicState.midCycleTime = null
	}
	else if ( atomicState.machineState.equals("midCycleCheck") && (latestPower < minimumWattage) ) {
		log.info "checkMachineState - Machine Needs To Be Emptied."
        atomicState.machineState = "needsToBeEmptied"
		atomicState.timeStoppedAt = now()
		log.debug "checkMachineState - Cycle complete startedAt: ${atomicState.timeStartedAt}, stoppedAt: ${atomicState.timeStoppedAt}"
        def validModeForNotifications = modesRepeatNotifications?.find{it == location.mode.trim()}
//        log.debug "checkMachineState - sendCompleteMessage: ${sendCompleteMessage} AND modesRepeatNotifications?.find{it == location.mode.trim()}: ${modesRepeatNotifications?.find{it == location.mode.trim()}} AND anythingSet: ${anythingSet()}"
//        log.debug "checkMachineState - validModeForNotifications is: ${validModeForNotifications}"
//        if ( validModeForNotifications != null ) {
//	       	log.debug "checkEmptyState - Modes have been selected for Notifications and current mode valid for a notification amd matches the current location mode."
//		}
        if (sendCompleteMessage && (validModeForNotifications != null) && anythingSet()) {
			log.debug "checkMachineState - Checking Empty State - Not Running and Not Empty - Send Notifications because correct mode, call sendNotification with message: ${completionMessage}"
            sendNotifications(completionMessage)
    	}
        log.debug "checkMachineState - Recheck Empty State later to see if emptied runIn: repeatNotificationsTime: ${repeatNotificationsTime}, checkEmptyState"		
        runIn(repeatNotificationsTime, "checkMachineState")
	}
    else if ( atomicState.machineState.equals("needsToBeEmptied") ) {
		log.debug "Still needs to be emptied, notify again..."
        def validModeForNotifications = modesRepeatNotifications?.find{it == location.mode.trim()}
        if (sendCompleteMessage && (validModeForNotifications != null) && anythingSet()) {
			log.debug "checkMachineState - Checking Empty State - Not Running and Not Empty - Send Notifications because correct mode, call sendNotification with message: ${completionMessage}"
            sendNotifications(completionMessage)
    	}
        log.debug "checkMachineState - Recheck Empty State later to see if emptied runIn: repeatNotificationsTime: ${repeatNotificationsTime}, checkEmptyState"		
        runIn(repeatNotificationsTime, "checkMachineState")

    }
    log.debug "checkMachineState - End atomicState: ${atomicState}"
}

def sendNotifications(notificationMessageText) {
	log.debug "sendNotifications - notificationMessageText: ${notificationMessageText}"
    log.debug "sendNotifications - switches: ${switches}"
    log.debug "sendNotifications - colorSwitches: ${colorSwitches}"
    log.debug "sendNotifications - speakers: ${sonos}"
	log.debug "sendNotifications - atomicState: ${atomicState}"
    if (phone) {
		if ( phone.indexOf(";") > 1){
			def phones = phone.split(";")
			for ( def i = 0; i < phones.size(); i++) {
				sendSms(phones[i], notificationMessageText)
			}
		}
        else {
			sendSms(phone, notificationMessageText)
		}
	}
    if (sendPushMessage) {
		log.debug "sendNotifications - Sending Push Notifications with message: ${notificationMessageText}"
        sendPush(notificationMessageText)
	}
	if ( (switches || colorSwitches) && atomicState.machineState.equals("needsToBeEmptied") ) {
		log.debug "sendNotifications - Blink Selected Switches: ${switches}"
        blinkSwitchesNotifications()
	}
	if (speech) { 
		log.debug "sendNotifications: Announce on selected speech devices: ${speech}"
        speech.speak(notificationMessageText) 
	}
    if (notificationMessageText) {
		atomicState.sound = textToSpeech(notificationMessageText instanceof List ? notificationMessageText[0] : notificationMessageText) // not sure why this is (sometimes) needed)
	}
    else {
		atomicState.sound = textToSpeech("You selected the custom message option but did not enter a valid message in the ${app.label} Smart App")
	}
	if (resumePlaying){
		sonos.playTrackAndResume(atomicState.sound.uri, atomicState.sound.duration, volume)
	}
	else {
		sonos.playTrackAndRestore(atomicState.sound.uri, atomicState.sound.duration, volume)
	}
}

def blinkSwitchesNotifications() {
	log.debug "switchesNotifications - Turn on following switches: ${switches} and following color switches ${colorSwitches}"
	log.debug "switchesNotifications - atomicState: ${atomicState}"

    try {
        def doFlash = true
        def numFlash = settings.numFlashes ?: 3
        def onFor = settings.flashOnFor ?: 1000
        def offFor = settings.flashOffFor ?: 1000

		setLightOptions(settings.switches)

        log.debug "LAST ACTIVATED IS: ${atomicState.lastActivated}"
        if (atomicState.lastActivated) {
            def elapsed = now() - atomicState.lastActivated
            def sequenceTime = (numFlash + 1) * (onFor + offFor)
            doFlash = elapsed > sequenceTime
            log.debug "DO FLASH: ${doFlash}, ELAPSED: ${elapsed}, LAST ACTIVATED: ${atomicState.lastActivated}"
        }

        if (doFlash) {
            log.debug "FLASHING ${numFlash} times"
            atomicState.lastActivated = now()
            log.debug "LAST ACTIVATED SET TO: ${atomicState.lastActivated}"
            def initialActionOn =  settings.switches.collect{it.currentSwitch != "on"}
            def delay = 0L
            numFlash.times {
                log.debug "Switch on after ${delay} msec"
                settings.switches.eachWithIndex {s, i ->
                    if (initialActionOn[i]) {
                        s.on(delay:delay)
                    }
                    else {
                        s.off(delay:delay)
                    }
                }
                delay += onFor
                log.debug "Switch off after ${delay} msec"
                settings.switches.eachWithIndex {s, i ->
                    if (initialActionOn[i]) {
                        s.off(delay:delay)
                    }
                    else {
                        s.on(delay:delay)
                    }
                }
                delay += offFor
            }

            def restoreDelay = (delay/1000) + 1
            log.debug "restore flash devices after ${restoreDelay} seconds"
            runIn(restoreDelay, blinkSwitchesRestoreLights)
        }

    } catch(ex) {
        log.error "Error Flashing Lights: $ex"
    }
}

def setLightOptions(lights) {
    def color = settings.lightColor
    def level = (settings.lightLevel as Integer) ?: 100

// default to Red
    def hueColor = 100
    def saturation = 100

    if (color) {
        switch(color) {
            case "White":
            hueColor = 52
            saturation = 19
            break;
            case "Blue":
            hueColor = 70
            break;
            case "Green":
            hueColor = 39
            break;
            case "Yellow":
            hueColor = 25
            break;
            case "Orange":
            hueColor = 10
            break;
            case "Purple":
            hueColor = 75
            break;
            case "Pink":
            hueColor = 83
            break;
            case "Red":
            hueColor = 100
            break;
		}
    }

    switchesPreviousState(lights)
	//Set options for lights with color and level capability to selected settings
    lights.each {
        if (settings.lightColor && it.hasCapability("Color Control")) {
            def newColorValue = [hue: hueColor, saturation: saturation, level: level]
            log.debug "$it.id - new light color values = $newColorValue"
            it.setColor(newColorValue)
        } 

        if (settings.lightLevel && it.hasCapability("Switch Level")) {
            log.debug "$it.id - new light level = $level"
            it.setLevel(level)
        } 
    }
}
def blinkSwitchesRestoreLights() {
    try {
        log.debug "restoring blinking lights"
        restoreLightOptions(settings.switches)
    } catch(ex) {
        log.error "Error restoring blinking lights: $ex"
    }
}

def restoreLightOptions(lights) {
    lights.each {
        if (settings.lightColor && it.hasCapability("Color Control")) {
            log.debug "${it.id} - restore light color"
            it.setColor(state.previous[it.id]) 
        } 

        if (settings.lightLevel && it.hasCapability("Switch Level")) {
            def level = state.previous[it.id].level ?: 100
            log.debug "${it.id} - restore light level = ${level}"
            it.setLevel(level) 
        }

        def lightSwitch = state.previous[it.id].switch ?: "off"
        log.debug "${it.id} - turn light ${lightSwitch}"
        if (lightSwitch == "on") {
            it.on()
        } else {
            it.off()
        }
    }
}

def switchesPreviousState(lights) {
    lights.each {
        if (it.hasCapability("Color Control")) {
            log.debug "save light color values"
            state.previous[it.id] = [
                "switch": it.currentValue("switch"),
                "level" : it.currentValue("level"),
                "hue": it.currentValue("hue"),
                "saturation": it.currentValue("saturation")
            ]
        } else if (it.hasCapability("Switch Level")) {
            log.debug "save light level"
            state.previous[it.id] = [
                "switch": it.currentValue("switch"),
                "level" : it.currentValue("level"),
            ]
        } else {
            log.debug "save light switch"
            state.previous[it.id] = [
                "switch": it.currentValue("switch"),
            ]
        }
    }
}

def powerInputHandler(evt) {
	log.debug "powerInputHandler - Power Input Handler Value Changed to ${selectedWasherOrDryer.currentValue("power")}"
	checkMachineState()
}

def resetNotificationHandler(evt) {
	log.debug "resetNotificationHandler - Device triggered to reset needsToBeEmptied state"
    log.debug "resetNotificationHandler - start atomicState: ${atomicState}"
    if (atomicState.machineState.equals("needsToBeEmptied")) {
    	atomicState.machineState = "notRunning"
        atomicState.timeEmptiedAt = now()
	}
    log.debug "resetNotificationHandler - end atomicState: ${atomicState}"
}

private hideOptionsSection() {
  (switches || switchesColor || speech || sonos) ? false : true
}

private anythingSet() {
	for (name in ["motion","contact","contactClosed","acceleration","mySwitch","mySwitchOff","button1"]) {
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