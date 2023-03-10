/**
 *  Check for unresponsive devices
 *
 *  Author: Carlo Innocenti
 *  Date: 2014-04-04
 */

// Automatically generated. Make future change here.
definition(
    name: "Unresponsive devices monitor",
    namespace: "",
    author: "Carlo Innocenti",
    description: "Check for unresponsive devices",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("When device is unresponsive") {
    	input "devices", "capability.polling", title:"Pollable device", multiple: true
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
	subscribe(devices, "responsive", responsiveHandler)

	state.unresponsiveNoticeSent = [:]
	updateResponsiveStatus()
}

def send(text) {
    if (phoneNumber == null) {
    	sendPush(text)
    }
    else {
        sendSms(phoneNumber, text) 
    }
}

def updateResponsiveStatus() {
    for (device in devices) {
    	if (device.currentValue("responsive") != null) {
        log.debug "Device '${device.displayName}' has responsive == ${device.currentValue("responsive")}"
            if (device.currentValue("responsive") == "false") {
                log.debug "Device '${device.displayName}' is unsresponsive"
                if (!state.unresponsiveNoticeSent.containsKey(device.id)) {
                    send("${device.displayName} is unresponsive; id: ${device.id}")
                }
                state.unresponsiveNoticeSent[(device.id)] = true            	
            }
            else {
                if (state.unresponsiveNoticeSent.containsKey(device.id)) {
                    log.debug "Device '${device.displayName}' is responsive again"
                    state.unresponsiveNoticeSent.remove(device.id)
                }
            }
        } else {
			log.debug "Device '${device}' is returning null responsive status... ignoring..."
        }
    }
}

def responsiveHandler(evt) {
	log.debug "responsiveHandler: ${evt}"
	updateResponsiveStatus()
}

