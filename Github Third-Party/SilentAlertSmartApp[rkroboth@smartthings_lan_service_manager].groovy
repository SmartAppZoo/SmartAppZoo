/**
 *  Send push alert if selected contacts or motion sensors are opened while the custom alarm device is in silent mode.
  * (requires an alarm device using the AlarmDeviceHandler from this repo)
 *
 *  Author: Rusty Kroboth
 */
definition(
    name: "Silent Alarm Push Alert",
    namespace: "rkroboth",
    author: "Rusty Kroboth",
    description: "Send push alert if selected contacts or motion sensors are opened while the custom alarm device is in silent mode",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm.png@2x.png"
)

preferences {
    section("Send a push alert when these devices open or are active...") {
        input "contactsensor", "capability.contactSensor", title: "Which Doors?", multiple: true
        input "motionsensor", "capability.motionSensor", title: "Which Motion Sensors?", multiple: true
    }
    section("While this alarm is in silent mode...") {
        input "theAlarm", "capability.alarm", title: "Which Alarm?"
    }
}

def installed()
{
    initialize()
}

def updated()
{
    unsubscribe()
    initialize()
}

def initialize(){
    reset_recent_push_was_sent();
    subscribe(contactsensor, "contact", onChange)
    subscribe(motionsensor, "motion", onChange)
}

def onChange(evt) {
	log.debug "Processing event event: name ${evt.name}, value ${evt.value}"

    def alarm_arm_state = theAlarm.currentState("alarm_arm_state");
    
    if (alarm_arm_state.value != "silent"){
        log.debug "Alarm is not in silent mode (current state is ${alarm_arm_state.value})."
        return;
    }
    if (evt.value != "open" && evt.value != "active") {
        log.debug "Event was not open (for contact) or active (for motion sensor)."
        return;
    }
    if (state.recent_push_was_sent == true){
        log.debug "Silent alarm tripped again, but not sending more than one push alert in under 60 secs."
        return;
    }
    state.recent_push_was_sent = true;
	log.debug "Silent Alarm has been tripped, sending push alert"
    sendPush "Silent Alarm has been tripped"
    runIn(60, reset_recent_push_was_sent)
}

def reset_recent_push_was_sent(){
    state.recent_push_was_sent = false;
}


