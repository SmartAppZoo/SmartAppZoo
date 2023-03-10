/**
 *  Send push alert if overhead garage door is left open for too long.
 *
 *  Author: Rusty Kroboth
 */
definition(
    name: "Garage Door Open Reminder",
    namespace: "rkroboth",
    author: "Rusty Kroboth",
    description: "Get a SmartThings app push notification if the garage door is left open too long",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Send a push alert when this door is open...") {
		input "garageDoorSensors", "capability.contactSensor", title: "Which?", multiple: true
	}
	section("For this long...") {
		input "maxOpenTime", "number", title: "Minutes?"
	}
    section("Mute alerts?"){
        input "mute_alerts", "bool", title: "Mute alerts?"
    }
}

def installed()
{
	subscribe(garageDoorSensors, "contact", onChange)
}

def updated()
{
	unsubscribe()
	subscribe(garageDoorSensors, "contact", onChange)
    onChange([]);
}

def onChange(evt) {

    def door_status = "open";
    garageDoorSensors.each {
        if (it.currentState("contact").value == "closed"){
            door_status = null; 
        }
    }
    state.door_status = door_status;
    
    if (state.door_status == "open") {
        log.trace "Garage door is currently open."
        schedulePushAlert(maxOpenTime * 60);
    }
    else {
        state.last_scheduled_push_alert_time = null;
        log.trace "Garage door is currently closed."
    }
}

def schedulePushAlert(seconds){
    def d = new Date();
    def scheduled_push_alert_time = d.format("yyyy/MM/dd HH:mm:ss");
    state.last_scheduled_push_alert_time = scheduled_push_alert_time;
    runIn(seconds, sendPushAlert, [data : [scheduled_push_alert_time : scheduled_push_alert_time]])
}

def sendPushAlert(data){

    def scheduled_push_alert_time = data.scheduled_push_alert_time

    if (!state.last_scheduled_push_alert_time){
        return;
    }
    if (scheduled_push_alert_time != state.last_scheduled_push_alert_time){
        return;
    }

    state.last_scheduled_push_alert_time = null;
    
    if (mute_alerts){
        log.trace "Alerts are muted... Not sending push."
        return;
    }
    
	if (state.door_status != "open"){
		log.trace "Garage door has been closed... not sending push."
        return;
    }

    log.trace "${garageDoorSensors} is still open... sending push alert."
    sendPush "Your garage door has been open for more than ${maxOpenTime} minutes!"
    schedulePushAlert(300);

}


