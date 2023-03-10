/**
 *  Notify Me When the Battery is Low
 *
 *  Author: SmartThings
 */
preferences {
	page(name: "root", title: "Battery Monitor", install: true) {
		section("Monitor the following devices:") {
			input "battery", "capability.battery", title: "Which Devices?", required: true, multiple: true
		}
		section("Notification method") {
			input "pushNotification", "bool", title: "Push notification", description: " ", required: false
			input "phone", "phone", title: "Text message at", description: "Tap to enter phone number", required: false
		}
	}
}

def installed()
{
	subscribe(battery, "battery", lowBatteryHandler, [filterEvents: false])
}

def updated()
{
	unsubscribe()
	subscribe(battery, "battery", lowBatteryHandler, [filterEvents: false])
}


def lowBatteryHandler(evt) {
	def val = evt.value.toInteger()
	def device = evt.device

	if(!state."warningHistory-$device.id") { state."warningHistory-$device.id" = [] }
	if(val > 30) {
		//Batteries aren't low or have been replaced since last set of warnings
		state."warningHistory-$device.id" = []
	} else if(val <= 15 && val > 5) {
		sendLowBatteryWarning(device, "15%", val)
	} else if(val <= 5) {
		sendLowBatteryWarning(device, "5%", val)
	}
}

def sendNotification(device, value) {
	def msg = "Your device: ${device} has only ${value}% battery remaining"
	log.debug "Low battery detected, sending message: '$msg', push:$pushNotification, phone:$phone"

	if (pushNotification) {
		sendPush(msg)
	}
	if (phone) {
		sendSms(phone, msg)
	}
}

def sendLowBatteryWarning(device, warningLevel, value) {
	if(!hasSentWarningForLevel(device, warningLevel)) {
		log.debug "Has not sent warning for battery level yet, sending notification for device: $device"
		sendNotification(device, value)
		state."warningHistory-$device.id" << warningLevel
	} else {
		log.debug "Has already sent warning for current battery level, not sending notification for device: $device"
	}
}

def hasSentWarningForLevel(device, warningLevel) {
	state."warningHistory-$device.id".contains(warningLevel)
}