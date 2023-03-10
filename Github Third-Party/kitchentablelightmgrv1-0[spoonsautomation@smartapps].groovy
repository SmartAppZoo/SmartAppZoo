/**
 *  KitchenRoomApp
 *
 *  Copyright 2019 Steve Spooner
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
	name: "KitchenTableLightMgrV1.0",
	namespace: "spoonsautomation",
	author: "Steve Spooner",
	description: "Prototype for Kitchen Lighting Manager V1.0",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "configure")
}

def configure() {
	dynamicPage(name: "configure", title: "Configure Default Routines", install: true, uninstall: true) {
		
		section("Virtual Room Monitor") {
			input name: "roomName", type: "text", title: "Room Monitor Name", description: "This smart Apps instance name, used for logging.", required: true, defaultValue: "LivingRoomMonitor"
			input name: "sceneChangesEnabled", type: "bool", title: "Scene Changes Enabled Enabled", description: "Allow SceneManager to update scenes?", required: true, defaultValue: true
			input name: "motionEnabled", type: "bool", title: "Motion Enabled", description: "Used to enable/disable auto motion lights.", required: true, defaultValue: true
			input name: "debugLvl", type: "bool", title: "Debug Logging Enabled", description: "Debug logging should only be used while troubleshooting.", required: true, defaultValue: true
			input name: "testMode", type: "bool", title: "TestMode, use short timings for testing motion", description: "In test mode, initialDimTimeoutSeconds=20, sceneMaxOnSeconds=40, warningDimTimeoutSeconds=20.", required: true, defaultValue: false
		}

		section("Motion detectors:") {
			input "motionPrimary", "capability.motionSensor", required: true, title: "Primary room motion detector:"
			input "motionSecondary", "capability.motionSensor", required: false, title: "Secondary room motion detector:"
			input "MotionAdjacent", "capability.motionSensor", required: false, title: "Motion Sensor for adjacent room:"
		}
		section("Scene timer values:") {
			input "initialDimTimeoutSeconds", "number",  title: "Seconds to wait for additional motion when in dim state:  ", required: true, defaultValue: 20
			input "sceneMaxOnSeconds", "number",  title: "Maximum time Scene will stay on with no motion before starting warning dim:  ", required: true, defaultValue: 20
			input "sceneTimerInitialIncrement", "number",  title: "Initial Increment for Scene Timer (min value = 1 second):  ", required: true, defaultValue: 15
			input "sceneTimerMultiplier", "number",  title: "Multiplier for Scene Timer Increment (minimum value = 1)  ", required: true, defaultValue: 2
			input "warningDimTimeoutDefaultSeconds", "number",  title: "Seconds to wait Warn before turning off:  ", required: true, defaultValue: 20
		}

		 def actions = location.helloHome?.getPhrases()*.label
		if (actions) {
			  //actions.sort()
			   section("Default On/Off/Dim Routines") {
					log.trace actions
					input "dimRoutine", "enum", title: "Default routine to use for Dim state", options: actions, required: true
					input "onRoutine", "enum", title: "Default routine to use for On state", options: actions, required: true
					input "offRoutine", "enum", title: "Default routine to use for Off state", options: actions, required: true
			   }
		  }

		  section("Switch to use for Override") {
			  //input "theswitch", "capability.switch", required: true
			  input "theOverrideSwitch", "capability.button", title: "GE Dimmer Switch to use for Override: ", required: true
			  input "overrideOnMinutes", "number",  title: "Minutes to remain ON when override:  ", required: true, defaultValue: 20
			  input "overrideOffMinutes", "number", title: "Minutes to remain OFF when override: ", required: true, defaultValue: 20
			  //input "lightSwitchMode", "enum", title: "Modes", required: true, multiple:true, options: [[OFF:"OFF"], [INITIAL_DIM:"INITIAL_DIM"],[RUNNING_SCENE:"RUNNING_SCENE"], [ON_HOLD:"ON_HOLD"], [OFF_HOLD:"OFF_HOLD"], [WARNING_DIM:"WARNING_DIM"] ]
		  }
		  section("Fade On/Off these lights") {
			  input "monitorLights", "capability.switch", title: "Which lights do you want to control?", multiple: true, submitOnChange: true
		  }

		 section("Virtual Room Switch Device?") {
			 input "virtualRoomSwitch", "capability.switch", required: true
		 }

	}
}

def installed() {
	log.info "$roomName: Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.info "$roomName: Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


// inputs to control what to do with the lights (turn on, turn on and set color, turn on
// and set level)
//def actionInputs() {
//	if (monitorLights) {
//		input "action", "enum", title: "What do you want to do?", options: actionOptions(), required: true, submitOnChange: true
//		if (action == "color") {
//			input "color", "enum", title: "Color", required: true, multiple:false, options: [
//				["Soft White":"Soft White - Default"],
//				["White":"White - Concentrate"],
//				["Daylight":"Daylight - Energize"],
//				["Warm White":"Warm White - Relax"],
//				"Red","Green","Blue","Yellow","Orange","Purple","Pink"]
//
//		}
//		if (action == "level" || action == "color") {
//			input "level", "enum", title: "Dimmer Level", options: [[10:"10%"],[20:"20%"],[40:"40%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: "80"
//		}
//	}
//}

// utility method to get a map of available actions for the selected switches
//def actionMap() {
//	def map = [on: "Turn On", off: "Turn Off"]
//	if (monitorLights.find{it.hasCommand('setLevel')} != null) {
//		map.level = "Turn On & Set Level"
//	}
//	if (monitorLights.find{it.hasCommand('setColor')} != null) {
//		map.color = "Turn On & Set Color"
//	}
//
//}

// utility method to collect the action map entries into maps for the input
//def actionOptions() {
//	actionMap().collect{[(it.key): it.value]}
//}

// ============ Initialize ===============
def initialize() {
	atomicState.isRunning = false
	
	if (testMode) {
		log.warn "TEST MODE!! initialDimTimeoutSeconds=20, sceneMaxOnSeconds=40, warningDimTimeoutSeconds=20"
	}
	
	atomicState.remainingOnSeconds = 0;
	atomicState.sceneTimerIncrement = sceneTimerInitialIncrement;
	
	atomicState.timeWhenScheduleChekSet = now()
	atomicState.roomModeUpdatedTime = atomicState.timeWhenScheduleChekSet
	atomicState.roomMode = "MODE_INIT"
	atomicState.holdOff1Minute = 0;
	atomicState.overrideDimerLevel = 50

	
	updateRoomMode("MODE_OFF")


	//"MODE_OFF"
	//"MODE_INITIAL_DIM"
	//"MODE_WARNING_DIM"
	//"RUNNING_SCENE"
	//"MODEON_HOLD"
	//"MODE+OFF_HOLD"
	//"MODE_WARNING_DIM"
	//"MODE_MANUAL"

	subscribe(motionPrimary, "motion", motionDetectedHandler)
	//subscribe(virtualRoomSwitch, "DimmerOverride", dimmerOverrideHandler)
	subscribe(virtualRoomSwitch, "level", dimmerLevelHandler)
	subscribe(virtualRoomSwitch, "switch", switchHandler)
	
	// general handler for detecting that a routine has executed, may be handy later
	//subscribe(location, "routineExecuted", routineChanged)
	
	if (sceneChangesEnabled) {
		subscribe(location, "selectedOnScene", onSceneChangeHandler)
		subscribe(location, "selectedOffScene", offSceneChangeHandler)
		subscribe(location, "selectedDimScene", dimSceneChangeHandler)
	}
	
	subscribe(location, "motionLightsEnabledState", motionLightsStateChangeHandler)

	
	if (MotionAdjacent) {
		subscribe(MotionAdjacent, "motion.active", MotionAdjacentHandler)
	}
	if (motionSecondary) {
		subscribe(motionSecondary, "motion.active", altRoomMotionHandler)
	}
	if (theOverrideSwitch) {
		if (deviceHasCapability(theOverrideSwitch, "Motion Sensor") == true) {
			log.info "$roomName: Subscribing to overrideButtonHandler"
			subscribe(theOverrideSwitch, "button", overrideButtonHandler)
			theOverrideSwitch.setLevel(50)
			theOverrideSwitch.off()
		} else {
			// NOT USED FOR NOW...
			// NOT SURE PHYSICAL SWITCH DHT WILL BE DIFFERENT THAN EVENT ON/OFF STATE
			//log.debug "Subscribing to overrideSwitchHandler!!!!"
			//subscribe(theOverrideSwitch, "switch", overrideSwitchHandler)
		}
		subscribe(theOverrideSwitch, "level", overrideDimmerLevelHandler)
	}
	
	virtualRoomSwitch.push()
	virtualRoomSwitch.setTimeRemaining(now())


	//virtualRoomSwitch.setLevel(25)

	// initialize default routines
	def routines = location.helloHome?.getPhrases()*.label.toArray()
	//atomicState.onRoutine  = routines[onRoutine.toInteger()]
	//atomicState.offRoutine = routines[offRoutine.toInteger()]
	//atomicState.dimRoutine = routines[dimRoutine.toInteger()]
	atomicState.onRoutine  = virtualRoomSwitch.currentState("selectedOnScene").value
	atomicState.offRoutine = virtualRoomSwitch.currentState("selectedOffScene").value
	atomicState.dimRoutine = virtualRoomSwitch.currentState("selectedDimScene").value
	
	atomicState.motionLightsEnabled = virtualRoomSwitch.currentState("selectedDimScene").value == 1 ? true : false
	
	log.info "$roomName: ON  ROUTINE $atomicState.onRoutine"
	log.info "$roomName: OFF ROUTINE $atomicState.offRoutine"
	log.info "$roomName: DIM ROUTINE $atomicState.dimRoutine"
	
	virtualRoomSwitch.setStatus("MODE_OFF")

/************
	log.debug "checkDeviceCapability for monitorLights"
	checkDeviceStatus(monitorLights, "Lights")
	log.debug "++++++++++++++++++++++++"
	log.debug "checkDeviceCapability for VirtualRoom"
	checkDeviceCapability(virtualRoomSwitch)
***********/
}



/*****************
Capabilities:
	Actuator
	Button
	Refresh
	Sensor
	Sound Sensor
	Switch
	Switch Level
	
Custom Attributes:
	selectedOnScene
	selectedOffScene
	selectedDimScene
	secondsRemaining
	motionLightsEnabledState
	
Custom Commands:

	setStatus
	push
	setDimScene
	setOnScene
	setOffScene
	setTimeRemaining
 ******************/


//=========================================================================================================================
// Event Handlers
//=========================================================================================================================

def onSceneChangeHandler(evt) {
//	def currentScene = virtualRoomSwitch.currentState("selectedScene");
//	if (debugLvl) {log.debug "$roomName: sceneChangeHandler EventHandler: ${evt.name}"}
//	if (debugLvl) {log.debug "$roomName: sceneChangeHandler called: $currentScene.value"}
//	if (debugLvl) {log.debug "$roomName: sceneChangeHandler evt value: ${evt.value}"}
//	if (debugLvl) {log.debug "$roomName: sceneChangeHandler evt displayName: ${evt.displayName}"}
//
//	def data = parseJson(evt.data)
//	if (debugLvl) {log.debug "$roomName: event data: ${data}"}
//	if (debugLvl) {log.debug "$roomName: event sceneType: ${data.sceneType}"}
//	if (debugLvl) {log.debug "$roomName: event cause: ${data.cause}"}
	
	atomicState.onRoutine = evt.value
	log.info "$roomName: onSceneChangeHandler(), SceneManager changed ON routine, new OnRoutine = [$evt.value]"

	// if mode =  MODE_RUNNING_SCENE, then execute new scene
	def roomMode = atomicState.roomMode
	if ("MODE_RUNNING_SCENE" == roomMode) {
		incrementRemainingOnSeconds()
		doRunScene(true)
	}
}

def offSceneChangeHandler(evt) {
	log.info "$roomName: offSceneChangeHandler(), SceneManager changed OFF routine, new OffRoutine = [$evt.value]"
	atomicState.offRoutine = evt.value
}

def dimSceneChangeHandler(evt) {
	log.info "$roomName: dimSceneChangeHandler(), SceneManager changed DIM routine, new DimRoutine = [$evt.value]"
	atomicState.dimRoutine = evt.value
}

def motionLightsStateChangeHandler(evt) {
	//def motionLightsEnabledCurrentState = virtualRoomSwitch.currentState("motionLightsEnabledState");
	def bMmotionLightsEnabled = evt.value == 1 ? true : false
	def data = parseJson(evt.data)
	if (debugLvl) {log.debug "$roomName: motionLightsStateChangeHandler evt value: ${evt.value}"}
	if (debugLvl) {log.debug "$roomName: event data: ${data}"}
	if (debugLvl) {log.debug "$roomName: event motionLightsStartTime: ${data.motionLightsStartTime}"}
	if (debugLvl) {log.debug "$roomName: event motionLightsEndTime: ${data.motionLightsEndTime}"}
}

// general handler for detecting that a routine has executed, may be handy later
//def routineChanged(evt) {
//	// log.debug "routineChanged: $evt"
//	if (debugLvl) {log.debug "$roomName: routineChanged EventHandler: ${evt.name}: ${evt.displayName}"}
//	//if (debugLvl) {log.debug "evt value: ${evt.value}"}
//	//if (debugLvl) {log.debug "evt displayName: ${evt.displayName}"}
//	//if (debugLvl) {log.debug "evt descriptionText: ${evt.descriptionText}"}
//}

def overrideDimmerLevelHandler(evt) {
	def currentDimmerLevel = theOverrideSwitch.currentValue("level")
	
	// update the virtualSwitch DTH that the OverrideSwich level has been set
	// I expect to use the to change scenes when long press of the override dimmer button
	virtualRoomSwitch.setLevel(currentDimmerLevel)
	log.warn "$roomName: overrideDimmerLevelChanged newLevel: $currentDimmerLevel"
}

def dimmerLevelHandler(evt) {
	def currentDimmerLevel = virtualRoomSwitch.currentState("level");
	if (debugLvl) {log.debug "$roomName: dimmerLevelHandler called Level: $currentDimmerLevel.value"}
}

def dimmerOverrideHandler(evt) {
	def currentDimmerOverride = virtualRoomSwitch.currentState("DimmerOverride");
	if (debugLvl) {log.debug "$roomName: dimmerOverrideHandler called: $currentDimmerOverride.value"}
	
}

def switchHandler(evt) {
	def currentSwitchValue = virtualRoomSwitch.currentState("switch");
	if (debugLvl) {log.debug "$roomName: switchHandler called (event: $evt.value): $currentSwitchValue.value"}
}

// NOT USED FOR NOW... NOT SURE PHYSICAL SWITCH DHT WILL BE DIFFERENT THAN EVENT ON/OFF STATE
def overrideSwitchHandler(evt) {
	if ((atomicState.isRunning != null) && (atomicState.isRunning == true)) {
		if (debugLvl) {log.debug "$roomName: overrideSwitchHandler called atomicState.isRunning !!!"}
		return
	}
	atomicState.isRunning = true as Boolean

	def currentSwitchValue = theOverrideSwitch.currentState("switch");
	if (debugLvl) {log.debug "$roomName: overrideSwitchHandler called (event: $evt.value): $currentSwitchValue.value"}
	if (currentSwitchValue.value == "off") {
		doOffHold(overrideOffMinutes)
		updateVirtualRoomTimeRemaining(overrideOffMinutes*60)
	}
	else if (currentSwitchValue.value == "on") {
		doOnHold(overrideOnMinutes)
		updateVirtualRoomTimeRemaining(overrideOnMinutes*60)
	}
	atomicState.isRunning = false
}


def overrideButtonHandler(evt) {
	if ((atomicState.isRunning != null) && (atomicState.isRunning == true)) {
		if (debugLvl) {log.debug "$roomName: overrideButtonHandler called atomicState.isRunning !!!"}
		return
	}
	atomicState.isRunning = true as Boolean
	
	try {

		def currentSwitchValue = theOverrideSwitch.currentState("switch");
		def eventDescription = evt.descriptionText
		def roomMode = atomicState.roomMode
		if (debugLvl) {log.debug "$roomName: overrideButtonHandler called (event: $evt.value)"}
		
		//Up/Double
		if (eventDescription?.startsWith("Up/Double")) {
			log.info "$roomName: Up/Double PRESSED with roomMode: $roomMode"
					// change to current scene and enter HOLD_ON state
					doRunScene(true)
					doOnHold(overrideOnMinutes)
					updateVirtualRoomTimeRemaining(overrideOnMinutes*60)
		}
		else if (eventDescription?.startsWith("Down/Double")) {
			log.info "$roomName: Down/Double PRESSED with roomMode: $roomMode"
			// only enter HOLD_OFF state if already off
			if ("MODE_OFF" == roomMode) {
				atomicState.holdOff1Minute = 0
				doOffHold(overrideOffMinutes)
				updateVirtualRoomTimeRemaining(overrideOffMinutes*60)
			}
		}
		else if (eventDescription?.startsWith("On/Up")) {
	    	atomicState.roomModeUpdatedTime = now()
			incrementRemainingOnSeconds()
			log.info "$roomName: On/Up PRESSED with roomMode: $roomMode"
            if ("MODE_RUNNING_SCENE" == roomMode || "MODE_ON_HOLD" == roomMode) {
            	// if running scene, then just increment level, don't restart scene
				incrementMonitorLevels()
				doRunScene(false)
            }
            else {
				// up button pressed and NOT MODE_RUNNING_SCENE -- just execute the scene
				atomicState.sceneTimerIncrement = sceneTimerInitialIncrement
				if (testMode) {
					atomicState.remainingOnSeconds = 40
					setNextScheduleCheck(40)
					updateVirtualRoomTimeRemaining(40)
				}
				doRunScene(true)
			}
		}
		else if (eventDescription?.startsWith("Off/Down")) {
			log.info "$roomName: Off/Down  PRESSED with roomMode: $roomMode"
			// down button pressed when override switch is on
			// do off scene and enter HOLD_OFF state 1 minute
			if ("MODE_OFF" != roomMode && "MODE_OFF_HOLD" != roomMode) {
				if (!decrementMonitorLevels()) {
					atomicState.holdOff1Minute = 1
					doFadeToOff(true)
				}
			}
		}
		else {
			log.warn "$roomName: overrideButtonHandler unknown description: $eventDescription Event: $evt.name: $evt.value"
		}
	} catch (e) {
		log.warn "caught exception in overrideButtonHandler: $e"
	}
	atomicState.isRunning = false
}

def MotionAdjacentHandler(evt) {
	def roomMode = state.roomMode
	if (debugLvl) {log.debug("adjacentRoomMotionHandler $evt.name: $evt.value, roomMode: $roomMode")}
	if (evt.value == "active" && roomMode == "MODE_OFF") {
		log.info "$roomName: AdjacentRoomMotionHandler default: calling scheduleCheck(), roomMode: roomMode"
		scheduleCheck()
	}
}

def altRoomMotionHandler(evt) {
	def roomMode = atomicState.roomMode
	log.info "$roomName: AltRoomMotionHandler default: calling scheduleCheck(), state.roomMode: $roomMode"
	scheduleCheck()
}

def motionDetectedHandler(evt) {
	//def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
	//def roomSelectorArray = [virtualRoomSwitch].toArray()
	def roomMode = atomicState.roomMode
	if (debugLvl) {log.debug "$roomName: MotionDetectedHandler $evt.name: $evt.value, roomMode: $roomMode"}

	switch (roomMode) {
		default:
			if (evt.value == "active") {
				log.info "$roomName: motionDetectedHandler default: calling scheduleCheck(), state.roomMode: $state.roomMode"
				scheduleCheck()
			}
		break;
	}
}

//=========================================================================================================================
// Check Schedule
//=========================================================================================================================


def scheduleCheck() {
	if ((atomicState.isRunning != null) && (atomicState.isRunning == true)) {
		if (debugLvl) {log.debug "$roomName: scheduleCheck called atomicState.isRunning !!!"}
		return
	}
	if (!isGlobalMotionLightsEnabled()) {
		if (debugLvl) {log.debug "$roomName: scheduleCheck called Global MotionLights = Disabled"}
		return
	}
	
	try {
		atomicState.isRunning = true as Boolean
		
		def motionState = motionPrimary.currentState("motion")
		if (motionSecondary) {
			def motionStateSec = motionSecondary.currentState("motion")
		}
		def roomMode = atomicState.roomMode
		def elapsed = (int)(now() - atomicState.roomModeUpdatedTime) + 1000
		
		def warningDimTimeoutSeconds = warningDimTimeoutDefaultSeconds
		if (testMode) {warningDimTimeoutSeconds = 20 }
		
		if (atomicState.roomModeUpdatedTime == 0) {
		   elapsed = 0
		}
		log.info "$roomName: scheduleCheck() state.roomMode: $roomMode  elapsed = $elapsed ms."
	
		switch (roomMode) {
			case "MODE_OFF":
				def adjacentMotionState = MotionAdjacent?.currentState("motion")
				
				// when holdOff1Minute==true, ignore motion fir 1 minute, this happens when manual switch is pressed
				if (atomicState.holdOff1Minute) {
						def threshold = 60000
						if (elapsed >= threshold) {
							atomicState.holdOff1Minute = false
							log.info "$roomName: MODE_OFF 1 minute Hold_Off expired"
							updateVirtualRoomTimeRemaining(remainingSeconds)
							break;
						}
						def remainingSeconds = (threshold - elapsed)/1000
						if (remainingSeconds < 0) {
							remainingSeconds = 0}
						else {
							setNextScheduleCheck(remainingSeconds)
						}
						updateVirtualRoomTimeRemaining(remainingSeconds)
						log.info "$roomName: MODE_OFF holdOff1Minute - Ignoring Motion for $remainingSeconds seconds"
						break;
				}
				
				def monitoredLightIsOn = isAnyMonitorOn()
				log.info "$roomName MODE: MODE_OFF MotionAdjacent.motion= $adjacentMotionState.value"
				if (motionState.value == "active") {
					if (!motionEnabled) {
						log.info "$roomName: Motion Detected, Motion Auto Lights Disabled"
						atomicState.isRunning = false
						return
					}
				
					incrementRemainingOnSeconds()
					if (!monitoredLightIsOn ) {
						doRunScene(true)
					}
					else {
						log.info "$roomName: MODE_OFF Monitored Light is on, ignoring $roomName motion= $motionState.value"
					}
				}
				else if (adjacentMotionState.value== "active") {
					if (!motionEnabled) {
						log.info "$roomName: Motion Detected, Motion Auto Lights Disabled"
						atomicState.isRunning = false
						return
					}
					setNextScheduleCheck(initialDimTimeoutSeconds)
					if (testMode) {
						updateVirtualRoomTimeRemaining(30 * 1000)
					}
					else {
						updateVirtualRoomTimeRemaining(initialDimTimeoutSeconds)
					}
					if (!monitoredLightIsOn ) {
						doInitialDim()
					}
					else {
						log.info "$roomName: MODE_OFF Monitored Light is on, ignoring Adjacent.motion= $adjacentMotionState.value"
					}
				 }
				 else {
					// in Off mode, make sure everything is off every overrideOnMinutes + 10 minutes
					log.info "$roomName: MODE_OFF forcing off for safety."
					setNextScheduleCheck((overrideOnMinutes+10) * 60)
					updateVirtualRoomTimeRemaining(0)
					doFadeToOff(true)
				 }
			break;
	
			case "MODE_INITIAL_DIM":
				log.info "$roomName: MODE_INITIAL_DIM"
				def threshold = 1000 * initialDimTimeoutSeconds
				if (testMode) { threshold = 30 * 1000}
				if (motionState.value == "active" || (motionSecondary && motionStateSec.value == "active")) {
					incrementRemainingOnSeconds()
					doRunScene(true)
				}
				if (elapsed >= threshold) {
					if (debugLvl) {log.debug "$roomName: MODE_INITIAL_DIM: Motion has stayed inactive too long since last check ($thresholdForScene ms):  turning lights off"}
	
					if (!additionalMotionDetected) {
					   atomicState.roomMode = "MODE_OFF"
					   updateVirtualRoomTimeRemaining(0)
					   doFadeToOff(true)
					}
				}
		   break;
	
			case "MODE_RUNNING_SCENE":
				log.info "$roomName: MODE_RUNNING_SCENE"
				def threshold = 1000 * atomicState.remainingOnSeconds
				if (motionState.value == "active" || (motionSecondary && motionStateSec.value == "active")) {
					incrementRemainingOnSeconds()
				}
				else if (elapsed >= threshold) {
					if (debugLvl) {log.debug "$roomName: MODE_RUNNING_SCENE: Motion has stayed inactive too long:  turning lights off"}
					setNextScheduleCheck(warningDimTimeoutSeconds)
					updateVirtualRoomTimeRemaining(warningDimTimeoutSeconds)
					doWarningDim()
				}
			break;
			//sceneTimeoutSeconds
		   case "MODE_WARNING_DIM":
				log.info "$roomName: WARNING_DIM"
				def threshold = 1000 * warningDimTimeoutSeconds
				if (motionState.value == "active" || (motionSecondary && motionStateSec.value == "active")) {
					incrementRemainingOnSeconds()
					doRunScene(true)
				}
				if (elapsed >= threshold) {
					if (debugLvl) {log.debug "$roomName: MODE_INITIAL_DIM: Motion has stayed inactive too long since last check ($thresholdForScene ms):  turning lights off"}
					setNextScheduleCheck(6000)
					updateVirtualRoomTimeRemaining(0)
					doFadeToOff(true)
				}
			break;
	
			case "MODE_ON_HOLD":
				log.info "$roomName:  MODE_ON_HOLD"
				def threshold = 1000 * overrideOnMinutes * 60
				if (elapsed >= threshold) {
					if (debugLvl) {log.debug "$roomName: MODE_ON_HOLD: Hold time expired, setting mode to MODE_WARNING_DIM, then turning lights off"}
					setNextScheduleCheck(warningDimTimeoutSeconds)
					updateVirtualRoomTimeRemaining(warningDimTimeoutSeconds)
					doWarningDim()
				}
			break;
	
			case "MODE_OFF_HOLD":
				log.info "$roomName: MODE_OFF_HOLD"
				def threshold = 1000 * overrideOffMinutes * 60
				if (elapsed >= threshold) {
					if (debugLvl) {log.debug "$roomName: OFF_HOLD: Hold time expired, setting mode to MODE_OFF"}
					setNextScheduleCheck(0)
					atomicState.holdOff1Minute = 0
					doFadeToOff(true)
				}
			break;
	
			default:
	
		   break;
		}
	} catch (e) {
		log.warn "caught exception in scheduleCheck: $e"
	}
	atomicState.isRunning = false
}

//=========================================================================================================================
//  Room Modes
//=========================================================================================================================


def doInitialDim() {
		updateRoomMode("MODE_INITIAL_DIM")
		virtualRoomSwitch.setStatus("INITIAL_DIM")
		execRoutineDim(5)
}

def doWarningDim() {
	if (isAnyMonitorOn()) {
		updateRoomMode("MODE_WARNING_DIM")
		log.info "$roomName: doWarningDim()"
		execRoutineDim(10)
		virtualRoomSwitch.setStatus("WARNING_DIM")
	}
}

def doFadeToOff(bForce) {
	log.info "$roomName: doFadeToOff() running!"
	updateRoomMode("MODE_OFF")
	atomicState.remainingOnSeconds = 0
	atomicState.sceneTimerIncrement = sceneTimerInitialIncrement
	virtualRoomSwitch.setStatus("OFF")
	execLightsOff(bForce)
}

def doRunScene(restart) {
	updateRoomMode( "MODE_RUNNING_SCENE")
	virtualRoomSwitch.setStatus("ON_SCENE")
	if (!testMode) { 
		setNextScheduleCheck(warningDimTimeoutDefaultSeconds)
	} else { setNextScheduleCheck(30) }
	if (restart) execOnRoutine()
}

def doOnHold(minutes) {
	updateRoomMode("MODE_ON_HOLD")
	virtualRoomSwitch.setStatus("ON_HOLD")
	setNextScheduleCheck(minutes*60)
	execOnRoutine()
}

def doOffHold(minutes) {
	updateRoomMode("MODE_OFF_HOLD")
	virtualRoomSwitch.setStatus("OFF_HOLD")
	setNextScheduleCheck(minutes*60)
	execLightsOff(true)
}


//=========================================================================================================================
//  Utility
//=========================================================================================================================


def execLightsOff(bforce) {
	def offRoutine = atomicState.offRoutine
	if (bforce || atomicState.currentRoutineName != offRoutine) {
		if (offRoutine) {
			log.info "$roomName: Executing Off Routine: ${offRoutine}"
			atomicState.currentRoutineName = offRoutine;
			location.helloHome?.execute(offRoutine)
		}
		else if (monitorLights) {
			log.info "$roomName: Executing Off for all selected lights"
			monitorLights?.off()
		}
	}
	else {
		log.info "$roomName: execLightsOff(): Already executing Current OFF Routine: ${offRoutine}"
	}
}

def execRoutineDim(level) {
	def dimRoutine = atomicState.dimRoutine
	if ( dimRoutine) {
		log.info "$roomName: Executing Dim Routine: ${dimRoutine}"
		atomicState.currentRoutineName = dimRoutine
		location.helloHome?.execute(dimRoutine)
	}
//	else if (monitorLights) {
//		log.info "$roomName: Executing setLevel/On for all selected lights"
//		monitorLights.setLevel(level)
//		monitorLights.on()
//	}
}

def execOnRoutine() {
	def onRoutine = atomicState.onRoutine
	if (atomicState.currentRoutineName != onRoutine) {
		if (onRoutine) {
			log.info "$roomName: Executing Current On Routine: ${onRoutine}"
			atomicState.currentRoutineName = onRoutine;
			location.helloHome?.execute(onRoutine)
		}
	}
	else {
		log.info "$roomName: execOnRoutine(): Already executing Current On Routine: ${onRoutine}"
	}
}

private isAnyMonitorOn() {
 def currentSwitch= monitorLights.currentSwitch
	def onLights = currentSwitch.findAll { switchVal ->
	   switchVal == "on" ? true : false
	}
	//log.warn "${onLights.size()} out of ${monitorLights.size()} devices are on"
	log.warn "$roomName: ${onLights.size()} devices are on"
	return onLights.size() > 0;
}


private executeNextScene() {
	def currentRoutine = atomicState.currentRoutineName
	if (currentRoutine == atomicState.offRoutine) {
		execRoutineDim(40)
	}
	else if (currentRoutine == atomicState.onRoutine) {
		execRoutineDim(40)
	}
	else {
		execOnRoutine()
	}
}


def setNextScheduleCheck(seconds) {
	if (seconds == 0) {
		log.info "$roomName: next scheduleCheck Cancled!"
		unschedule(scheduleCheck)
		atomicState.timeWhenScheduleChekSet = -1
	}
	else {
		if (debugLvl) {log.debug "$roomName: next scheduleCheck in: $seconds seconds"}
		atomicState.timeWhenScheduleChekSet = now()
		runIn(seconds, scheduleCheck, [overwrite: true])
	}
}

def setRemainingOnSeconds(onSecondsValue) {
	atomicState.remainingOnSeconds = onSecondsValue
	log.info "$roomName: room has $atomicState.remainingOnSeconds remaining ON Second before dimming"
	setNextScheduleCheck(onSecondsValue)
	updateVirtualRoomTimeRemaining(onSecondsValue)
}

def incrementRemainingOnSeconds() {
	def remainingOnSeconds = atomicState.remainingOnSeconds
	if ( remainingOnSeconds < 1) {
		if (!testMode) {
			remainingOnSeconds = sceneTimerInitialIncrement
			atomicState.sceneTimerIncrement = sceneTimerInitialIncrement
			atomicState.remainingOnSeconds = remainingOnSeconds
		} else { atomicState.remainingOnSeconds = 40 }
	}
	else {
		if (!testMode) {
			// multiply initial increment to exponentially add time
			def newIncrementValue = atomicState.sceneTimerIncrement * sceneTimerMultiplier
			remainingOnSeconds += newIncrementValue
			atomicState.sceneTimerIncrement = newIncrementValue
			if (remainingOnSeconds > sceneMaxOnSeconds) {
			   remainingOnSeconds = sceneMaxOnSeconds
			}
			if (atomicState.sceneTimerIncrement > sceneMaxOnSeconds) {
				atomicState.sceneTimerIncrement = sceneMaxOnSeconds
			}
			if (debugLvl) {
				log.debug "$roomName: incrementRemainingOnSeconds remainingOnSeconds= $remainingOnSeconds seconds"
				log.debug "$roomName:  atomicState.sceneTimerIncrement= $atomicState.sceneTimerIncrement seconds"	
			}
			atomicState.remainingOnSeconds = remainingOnSeconds
		}
		else { atomicState.remainingOnSeconds = 40 }
	}
	log.info "$roomName: room has $atomicState.remainingOnSeconds remaining ON Second before dimming"
	setNextScheduleCheck(atomicState.remainingOnSeconds)
	updateVirtualRoomTimeRemaining(atomicState.remainingOnSeconds)
}

def updateVirtualRoomTimeRemaining(secondsRemaining) {
	def msDateMsWhenExpired = now()
	if (secondsRemaining > 0) {
		msDateMsWhenExpired += (secondsRemaining * 1000)
	}
	virtualRoomSwitch.setTimeRemaining(msDateMsWhenExpired)
}

def updateRoomMode(mode) {
	def previousMode = atomicState.roomMode
	if (previousMode != mode) {
		 atomicState.roomMode = mode
		//Long timeNow = now()
		//def elapsed = timeNow - atomicState.roomModeUpdatedTime
		atomicState.roomModeUpdatedTime = now()
		log.info "$roomName: updateRoomMode() $previousMode ======> $mode"
	 }
}


def isGlobalMotionLightsEnabled() {
	return virtualRoomSwitch.currentState("motionLightsEnabledState").getIntegerValue() == 1 ? true : false
}

def deviceHasCapability(device, capability) {
	def switchState = device.currentValue("switch")
	if (debugLvl) {log.debug "$roomName: Current state is: $switchState"}
	def myDeviceCaps = device.capabilities
	def isButton = false
	if (myDeviceCaps) {
		myDeviceCaps.each {cap ->
			def capName = cap.name
			if (capName == capability) {
				   log.info "$roomName: Capability name: CONTAINS Button"
				   isButton = true
			}
			if (debugLvl) {log.debug "$roomName: Capability name: $capName}"}
		}
	}
	return isButton
}

//=========================================================================================================================
//=========================================================================================================================


def incrementMonitorLevels() {
	def minimumLevel = 5
	def reset = false
	def newLevel = 0
	
	monitorLights.each {
		if (it.hasCommand('setLevel')) {
			def currentLevel = it.currentValue("level")
			log.debug "$roomName: setMonitorLevels() current level=$currentLevel"
			if (currentLevel < minimumLevel)
				 newLevel = minimumLevel
			else if (currentLevel < 10)
				 newLevel = 10
			else if (currentLevel < 20)
				 newLevel = 20
			else if (currentLevel < 35)
				 newLevel = 35
			else if (currentLevel < 50)
				 newLevel = 50
			else
				 newLevel = 80
					 
			log.info "$roomName: Setting new MonitorLight level from: %${currentLevel} to: %$newLevel"
			it.setLevel(newLevel)
		}
	}
	// set the virtualRoomSwitch to the level of the last device
	if (newLevel > 0 && virtualRoomSwitch) {
		virtualRoomSwitch.setLevel(newLevel)
	}
}

def decrementMonitorLevels() {
	
	// returns continueRunningScene==false when level is below minimumLevel
	def minimumLevel = 5
	def continueRunningScene = false
	def newLevel = 0
	
	monitorLights.each {
		if (it.hasCommand('setLevel')) {
			def currentLevel = it.currentValue("level")
			log.debug "$roomName: setMonitorLevels() current level=$currentLevel"
			if (currentLevel > 80) {
				 newLevel = 80
				 continueRunningScene = true
			}
			else if (currentLevel > 50) {
				 newLevel = 50
				 continueRunningScene = true
			}
			else if (currentLevel > 35) {
				 newLevel = 35
				 continueRunningScene = true
			}
			else if (currentLevel > 20) {
				 newLevel = 20
				 continueRunningScene = true
			}
			else if (currentLevel > 10) {
				 newLevel = 10
				 continueRunningScene = true
			}
			else if (currentLevel > minimumLevel) {
				 newLevel = minimumLevel
				 continueRunningScene = true
			}
			else {
				continueRunningScene = false
			}
					 
			log.info "$roomName: Setting new MonitorLight level from: %${currentLevel} to: %$newLevel"
			
			if (continueRunningScene) {
				log.info "$roomName: Setting new MonitorLight level from: %${currentLevel} to: %$newLevel"
				it.setLevel(newLevel)
			}
			else 
				log.info "$roomName: MonitorLight == minimum level %${minimumLevel} returning continueRunningScene==false"
		}
	}
	// set the virtualRoomSwitch to the level of the last device
	if (newLevel > 0 && continueRunningScene && virtualRoomSwitch) {
		virtualRoomSwitch.setLevel(newLevel)
	}
	return  continueRunningScene
}

private checkDeviceStatus(devices, devicesName) {
	//log.info "checking Device Status for: ${devicesName}"
	def n = 0;
	devices.each {
	 //def device = devices[n].currentValue
		def currDevice = devices[n]
		def switchState = devices[n].currentValue("switch")
		checkDeviceCapability(currDevice)
		log.debug "======= Current state for device $n is: $switchState ========="
		checkDeviceCapability(currDevice)
		n++
	}
	// returns a list of the values for all switches
	def currDevices = devices.currentSwitch
	def onSwitches = currDevices.findAll { switchVal ->
	   switchVal == "on" ? true : false
	}
	log.warn "${onSwitches.size()} out of ${devices.size()} devices are on"
 }


 private checkDeviceCapability(device) {
	def switchState = device.currentValue("switch")
	log.debug "Current state is: $switchState"
	   // log each capability supported by the "mySwitch" device, along
	   // with all its supported commands
	   def myDeviceCaps = device.capabilities
	   if (myDeviceCaps) {
		   myDeviceCaps.each {cap ->
			  cap.commands.each {comm ->
				  log.debug "----Command name: ${comm.name}"
			  }
			  log.debug "Capability name: ${cap.name}"
		   }
	   }
	   def supportedCommands = device.supportedCommands
	   log.debug "------------------- "
	   supportedCommands.each {
		   log.debug "command name: ${it.name}"
	   }
	   log.debug "---- All Supported Commands ----- "
	   log.debug "==================================="
 }