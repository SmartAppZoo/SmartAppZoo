/**
 *  Low Battery Notification 
 *
 *  Author: t@hw.sg
 *  Date: 2013-10-19
 */

// Automatically generated. Make future change here.
definition(
    name: "Low battery monitor",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Low battery monitor",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("When battery drops below") {
    	input "devices", "capability.battery", title:"Battery Operated Devices", multiple: true
	}
    section("Battery level") {
    	input "level", "number", title:"Battery Level"
    }
    section("Send sms (leave blank for push)") {
    	input "phoneNumber", "phone", title: "Phone number", required: false 
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	if (level < 5 || level > 90) {
    	sendPush("Battery level should be between 5 and 90")
       	return false
    }
	subscribe(devices, "battery", batteryHandler)

	state.lowBattNoticeSent = [:]
	updateBatteryStatus()
}

def send(text) {
    if (phoneNumber == null) {
    	sendPush(text)
    }
    else {
        sendSms(phoneNumber, text) 
    }
}

def updateBatteryStatus() {
    for (device in devices) {
    	if (device.currentBattery != null) {
            if (device.currentBattery < level) {
                log.debug "Device '${device.displayName}' is below battery level: '${device.currentBattery}' < '${level}'"
                if (!state.lowBattNoticeSent.containsKey(device.id)) {
                    send("${device.displayName}'s battery is at ${device.currentBattery}%; id: ${device.id}")
                }
                state.lowBattNoticeSent[(device.id)] = true            	
            }
            else {
                if (state.lowBattNoticeSent.containsKey(device.id) && device.currentBattery > 40) {
                    log.debug "Device '${device.displayName}' is back at a good battery level: '${device.currentBattery}' >= '${level}'"
                    state.lowBattNoticeSent.remove(device.id)
                }
            }
        } else {
			log.debug "Device '${device}' is returning null battery status... ignoring..."
        }
    }
}

def batteryHandler(evt) {
	log.debug "batteryHandler: ${evt}"
	updateBatteryStatus()
}

