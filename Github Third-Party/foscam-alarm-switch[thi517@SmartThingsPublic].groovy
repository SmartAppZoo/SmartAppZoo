/**
 *  Foscam Alarm Switch
 *
 *  Copyright 2014 skp19
 *
 */
definition(
    name: "Foscam Alarm Switch",
    namespace: "thi",
    author: "thi",
    description: "Enable Foscam alarm.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Which switch?") {
		input "alarmSwitch", "capability.switch", required: true
	}
    section("Enable these Foscam alarms...") {
		input "cameras", "capability.imageCapture", multiple: true, required: true
        input "notify", "bool", title: "Notification?", required: false
	}
    section("Only between these times...") {
    	input "startTime", "time", title: "Start Time", required: false
        input "endTime", "time", title: "End Time", required: false
    }
}

def installed() {
    subscribe(alarmSwitch, "switch.on",  alarmOn)
    subscribe(alarmSwitch, "switch.off", alarmOff)
}

def updated() {
	unsubscribe()
    installed()
}

def alarmOff(evt) {
    cameras?.alarmOff()
    sendMessage("Foscam alarm disabled") 
}

def alarmOn(evt) {
    cameras?.alarmOn()
    sendMessage("Foscam alarm enabled") 
}

def sendMessage(msg) {
	if (notify) {
		sendPush msg
	}
}