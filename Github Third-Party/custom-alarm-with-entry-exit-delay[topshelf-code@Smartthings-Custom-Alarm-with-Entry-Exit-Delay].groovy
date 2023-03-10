/**
 *  Custom Alarm with Entry/Exit Delay
 *
 *  Copyright 2019 Brian Lange
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
        name: "Custom Alarm with Entry/Exit Delay",
        namespace: "topshelf-code",
        author: "Brian Lange",
        description: "Alarm Entry Delay that allows for an alarm panel to be disarmed before the alarm goes off as well as entry delay allowing for disarm time.",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png") {
    appSetting "SecurityPanelAddress"
    appSetting "SecurityPanelSoundPort"
}

/**
 * Declare all of your device variables.
 */
preferences {
    section("Selected Devices") {
        input "alarmAwayButton", "capability.switch", required: true,
                title: "The panel button that you will push to start the delayed AWAY arming process.", submitOnChange: true;

        input "alarmStayButton", "capability.switch", required: true,
                title: "The panel button that you will push to start the STAY arming process.", submitOnChange: true;

        input "alarmOffButton", "capability.switch", required: true,
                title: "The panel button that you will push to turn OFF the alarm system.", submitOnChange: true;

        input "frontContactSensor", "capability.contactSensor", required: true,
                title: "Front Contact Sensor", submitOnChange: true;

        input "backContactSensor", "capability.contactSensor", required: true,
                title: "Back Contact Sensor", submitOnChange: true;

        input "garageContactSensor", "capability.contactSensor", required: true,
                title: "Garage Contact Sensor", submitOnChange: true;

        input "primaryMotionSensor", "capability.motionSensor", required: true,
                title: "Primary Motion Sensor", submitOnChange: true;

        input "primaryIgnitionContactSensor", "capability.contactSensor", required: true,
                title: "Primary contact sensor that triggers the alarm.  This should be the only sensor that SMS looks at.", submitOnChange: true;

        input "lanNouncer", "capability.speechSynthesis", required: false, multiple: true, submitOnChange: true,
                title: "LanNouncer Panel 1.";
                
        input "lanNouncerMuteButton", "capability.switch", required: true,
                title: "Button used to temporarily mute lanNouncer.", submitOnChange: true;
                
        input "lanNouncerChimeButton", "capability.switch", required: true,
                title: "Button used to play lanNouncer chime.  Typically used for chime testing.", submitOnChange: true;
    }
}

/**
 * Install
 */
def installed() {
    log.debug "Installed with settings: ${settings}";

    initialize();
}

/**
 * Updated
 */
def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe();
    initialize();
}

/**
 * SmartApp initialization.
 * Declare all of your event handlers.
 */
def initialize() {
	state.alarmTriggered = false;
    log.debug "state.alarmTriggered - ${state.alarmTriggered}";
	subscribeAll();
}

/**
 * SmartApp initialization.
 * SubscribeAll to all of your event handlers.
 */
def subscribeAll() {
    //Custom Action Title Buttons
    subscribe(alarmAwayButton, "switch.on", alarmAwayButton_On);
    subscribe(alarmAwayButton, "switch.off", alarmAwayButton_Off);
    subscribe(alarmStayButton, "switch.on", alarmStayButton_On);
    subscribe(alarmStayButton, "switch.off", alarmStayButton_Off);
    subscribe(alarmOffButton, "switch.on", alarmOffButton_On);
    subscribe(alarmOffButton, "switch.off", alarmOffButton_Off);

    //Location/Alarm
    subscribe(location, "alarmSystemStatus", alarmStatus_StatusChange);

    //Alarm sensors
    subscribe(frontContactSensor, "contact.open", frontContactSensor_Open);
    subscribe(backContactSensor, "contact.open", backContactSensor_Open);
    subscribe(garageContactSensor, "contact.open", garageContactSensor_Open);
    subscribe(primaryMotionSensor, "motion.active", primaryMotionSensor_Active);
    
    subscribe(lanNouncerMuteButton, "switch.on", lanNouncerMuteButton_On);
    subscribe(lanNouncerChimeButton, "switch.on", lanNouncerChimeButton_On);
}

//region virtual switch/button events handlers

/**
 * EventHandler for when awayDelayedExitButton turns on.
 *
 * @param evt Event handler.
 */
def alarmAwayButton_On(evt) {
    //Reset the buttons.  We don't want the events to fire when we set them to off.
    unsubscribe();
    alarmStayButton.off();
    alarmOffButton.off();
    subscribeAll();

    log.debug "alarmAwayButton_On: ${evt}";
    countDownToArm();
}

/**
 * EventHandler for when theAwayDelayedExitButton turns off.
 *
 * @param evt Event handler.
 */
def alarmAwayButton_Off(evt) {
    log.debug "alarmAwayButton_Off: ${evt}";

    alarmOffButton.on();
}

/**
 * EventHandler for when alarmStayButton turns on.
 *
 * @param evt Event handler.
 */
def alarmStayButton_On(evt) {
    //Reset the buttons.  We don't want the events to fire when we set them to off.
    unsubscribe();
    alarmAwayButton.off();
    alarmOffButton.off();
    subscribeAll();

    log.debug "alarmStayButton_On: ${evt}";
    log.debug "alarmStayButton_On - Set mode changed to Stay";
    sendLocationEvent(name: "alarmSystemStatus", value: "stay");
}

/**
 * EventHandler for when alarmStayButton turns off.
 *
 * @param evt Event handler.
 */
def alarmStayButton_Off(evt) {
    log.debug "alarmStayButton_Off: ${evt}";

    alarmOffButton.on();
}

/**
 * EventHandler for when alarmOffButton turns on.
 *
 * @param evt Event handler.
 */
def alarmOffButton_On(evt) {
	log.debug "alarmOffButton_On: ${evt}";

    //Reset the buttons.  We don't want the events to fire when we set them to off.
    unsubscribe();
	alarmAwayButton.off();
    alarmStayButton.off();
    subscribeAll();

    //Set alarm to off.
    sendLocationEvent(name: "alarmSystemStatus", value: "off");
    
    //Stop alarm is necessary.
    lanNouncer.speak("@|ALARM=STOP");
    state.alarmTriggered = false;
}

/**
 * EventHandler for when alarmOffButton turns off.
 * This event should only fire when button is turned off via UI.
 * Any programmatic off calls unsubscribe to this event first.
 *
 * @param evt Event handler.
 */
def alarmOffButton_Off(evt) {
	alarmOffButton.on();
}

/**
 * EventHandler for when lanNouncerMuteButton turns on.
 *
 * @param evt Event handler.
 */
def lanNouncerMuteButton_On(evt) {
	runIn(60, setlanNouncerMuteButtonOff); //Turn lanNouncerMuteButton off in 60 seconds.
}

/**
 * EventHandler for when lanNouncerChimeButton turns on.
 *
 * @param evt Event handler.
 */
def lanNouncerChimeButton_On(evt) {
	log.debug "lanNouncerChimeButton_On: ${evt}";
    
	//Play chime =)
    lanNouncer.speak("@|ALARM=CHIME");
    lanNouncerChimeButton.off();
}

//endregion

//region device event handlers

/**
 * EventHandler for when location.alarmSystemStatus status changes.
 * This code is primarily for when a system status changes from outside the UI such as the ST app.
 *
 * @param evt Event handler.
 */
def alarmStatus_StatusChange(evt) {
    def alarmstatus = evt.value;

    log.debug "alarmStatus_StatusChange: ${evt}";
    log.debug "alarmStatus_StatusChange - Alarm: ${alarmstatus}";

	unsubscribe();
    
    switch(alarmstatus) {
    	case "away":
            unsubscribe();
            alarmAwayButton.on();
            alarmStayButton.off();
            alarmOffButton.off();
			subscribeAll();
            break;
        case "stay":
            unsubscribe();
            alarmAwayButton.off();
            alarmStayButton.on();
            alarmOffButton.off();
            subscribeAll();
            break;
        case "off":
        	//Flush any delayed audio.
        	unschedule(lanNouncerSpeak);
            unschedule(soundAlarm);
            unschedule(setAlarmModeToAway);
            
            unsubscribe();
            alarmAwayButton.off();
            alarmStayButton.off();
            alarmOffButton.on();
            subscribeAll();
            state.alarmTriggered = false;
            break;
        default:
            //Do nothing
            return false;
            break;
    }
}

/**
 * EventHandler for when the frontContactSensor opens.
 *
 * @param evt Event handler.
 */
def frontContactSensor_Open(evt) {
    log.debug "frontContactSensor_Open - The front door has opened";
    doorOpened(evt, "door");
}

/**
 * EventHandler for when the backContactSensor opens.
 *
 * @param evt Event handler.
 */
def backContactSensor_Open(evt) {
    log.debug "backContactSensor_Open - The back door has opened";
    doorOpened(evt, "door");
}

/**
 * EventHandler for when the garageContactSensor opens.
 *
 * @param evt Event handler.
 */
def garageContactSensor_Open(evt) {
    log.debug "garageContactSensor_Open - The garage door has opened";
    doorOpened(evt, "door");
}

/**
 * EventHandler for when primaryMotionSensor turns active.
 *
 * @param evt Event handler.
 */
def primaryMotionSensor_Active(evt) {
    log.debug "primaryMotionSensor_Active - The motion sensor detected motion";
    doorOpened(evt, "primaryMotionSensor");
}

//endregion

//region methods

/**
 * Shared method for when the door opens.
 *
 * @param evt Event handler.
 */
def doorOpened(evt, source) {
	log.debug "state.alarmTriggered - ${state.alarmTriggered}";
	if (state.alarmTriggered == true) {
    	return;
    }
    
    if (source != "primaryMotionSensor"){
        def mute = lanNouncerMuteButton.currentState("switch").getValue();

        if (mute != "on"){
        	//Play chime =)
    		lanNouncer.speak("@|ALARM=CHIME");
        }
    }

    def alarm = location.currentState("alarmSystemStatus");
    def alarmstatus = alarm?.value;

    switch(alarmstatus) {
        case "stay":
            if (source == "primaryMotionSensor"){
                //Do nothing
            }
            else {
                log.debug "doorOpened - Door opened while system is armed (Stay).";
                //Sound Alarm
                //primaryIgnitionContactSensor.open();
                //primaryIgnitionContactSensor.close();
                
                //I decided to switch to countdown since my kids are always tripping 
                //  the alarm in the middle of the night while letting the dog out to go to
                //  the bathroom.  This will allow for a countdown and chance to fix things 
                //  before the alarm goes off waking up the whole neighborhood.  
                countDownToDisarm();
            }
            break;
        case "away":
            if (source == "primaryMotionSensor"){
                log.debug "doorOpened - Motion detected while system is armed (Away).";
            }
            else {
                log.debug "doorOpened - Door opened while system is armed (Away).";
            }
            countDownToDisarm();
            break;
        case "off":
            log.debug "doorOpened - Door opened while system is unarmed.";
            //Do nothing
            break;
        default:
            log.debug "doorOpened - Door opened while system status is unknown (${alarmstatus}). =(";
            //Do nothing
            break;
    }
}

/**
 * Execute code for countdown to disarm
 *
 * @param evt Relayed event from origin event handler.
 */
def countDownToDisarm(mode) {
	log.debug "state.alarmTriggered - ${state.alarmTriggered}";
	if (state.alarmTriggered == true) {
    	return;
    }
    
    state.alarmTriggered = true;
    log.debug "state.alarmTriggered - ${state.alarmTriggered}";
	
    def delayTime = 0;
    log.debug "countDownToDisarm - Counting down.";

    lanNouncer.speak("You now have T minus 60 seconds to disarm the alarm system.");

    runIn(10, lanNouncerSpeak, [overwrite: false, data: [message: "You now have T minus 50 seconds to disarm the alarm system."]]);
    runIn(20, lanNouncerSpeak, [overwrite: false, data: [message: "You now have T minus 40 seconds to disarm the alarm system."]]);
    runIn(30, lanNouncerSpeak, [overwrite: false, data: [message: "You now have T minus 30 seconds to disarm the alarm system."]]);
    runIn(40, lanNouncerSpeak, [overwrite: false, data: [message: "You now have T minus 20 seconds to disarm the alarm system."]]);
    runIn(50, lanNouncerSpeak, [overwrite: false, data: [message: "Alarm system intrusion event imminent."]]);
    runIn(60, lanNouncerSpeak, [overwrite: false, data: [message: "An intrusion has been detected.  Executing biological purge sequence."]]);
    runIn(70, soundAlarm); //Unleash the fury in 60 seconds.
}

/**
 * Execute code for countdown to arm
 *
 * @param evt Relayed event from origin event handler.
 */
def countDownToArm(mode) {
    log.debug "countDownToArm - Counting down.";

    lanNouncer.speak("You now have T minus 60 seconds to exit the facility.");
    runIn(10, lanNouncerSpeak, [overwrite: false, data: [message: "You now have T minus 50 seconds to exit the facility."]]);
    runIn(20, lanNouncerSpeak, [overwrite: false, data: [message: "You now have T minus 40 seconds to exit the facility."]]);
    runIn(30, lanNouncerSpeak, [overwrite: false, data: [message: "You now have T minus 30 seconds to exit the facility."]]);
    runIn(40, lanNouncerSpeak, [overwrite: false, data: [message: "You now have T minus 20 seconds to exit the facility."]]);
    runIn(50, lanNouncerSpeak, [overwrite: false, data: [message: "Alarm system activation event is imminent."]]);
    runIn(60, lanNouncerSpeak, [overwrite: false, data: [message: "Alarm system is now armed."]]);
	runIn(60, setAlarmModeToAway); //Arm this badboy in 60 seconds.
}

/**
 * Set the alarm mode to away.
 */
def setAlarmModeToAway() {
    def awayVal = alarmAwayButton.currentState("switch").getValue();
    log.debug "setAlarmModeToAway - Current Switch Mode Away:${awayVal}";
    if (awayVal == "on") {
        log.debug "setAlarmModeToAway - Set mode changed to Away";
        sendLocationEvent(name: "alarmSystemStatus", value: "away");
    }
}

/**
 * Have LanNouncer say something.
 */
def lanNouncerSpeak(data) {
    def stayVal = alarmStayButton.currentState("switch").getValue();
    def awayVal = alarmAwayButton.currentState("switch").getValue();
    log.debug "lanNouncerSpeak - Current Switch Mode Stay:${stayVal}, Away:${awayVal}";
    if (stayVal == "on" || awayVal == "on") {
        log.debug "lanNouncerSpeak - Message: ${data.message}";
        lanNouncer.speak(data.message);
    }
}

/**
 * Sound Alarm!!!
 */
def soundAlarm() {
    def stayVal = alarmStayButton.currentState("switch").getValue();
    def awayVal = alarmAwayButton.currentState("switch").getValue();
    log.debug "soundAlarm - Current Switch Mode Stay:${stayVal}, Away:${awayVal}";
    if (stayVal == "on" || awayVal == "on") {
        log.debug "soundAlarm - Sound Alarm!!!";
        primaryIgnitionContactSensor.open();
        primaryIgnitionContactSensor.close();
        lanNouncer.speak("@|ALARM=SIREN:CONTINUOUS");
    }
}

/**
 * Turn off lanNouncerMuteButton.
 */
def setlanNouncerMuteButtonOff() {
	lanNouncerMuteButton.off();
}

//endregion