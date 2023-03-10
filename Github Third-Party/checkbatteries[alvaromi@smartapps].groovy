/**
 *  CheckBatteries
 *
 *  Copyright 2017 abchez
 *
 */
definition(
    name: "CheckBatteries",
    namespace: "abchez",
    author: "abchez",
    description: "Check for Low Battery levels",
    category: "My Apps",
    iconUrl: "http://clipart-library.com/image_gallery/13028.jpg",
    iconX2Url: "http://clipart-library.com/image_gallery/13028.jpg",
    iconX3Url: "http://clipart-library.com/image_gallery/13028.jpg")


preferences {
	section {
		input "batteryPoweredDevices", "capability.battery", multiple: true, title: "Battery-powered devices to monitor"
        input "minLevelCritical", "number", range: "0..100", defaultValue: 10, required: true, title: "Notify critical level (%)"
        input "minLevelWarning", "number", range: "0..100", defaultValue: 25, required: false, title: "Notify warning level (%)"
        input "phoneNumber", "number", range: "1111111111..9999999999", required: false, title: "Notify phone number (optional)" 
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule();
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    if (batteryPoweredDevices.size() > 0) {
    	checkWarningBatteryLevels()
        checkCriticalBatteryLevels()
        if (minLevelWarning > minLevelCritical) {
            schedule("0 0 10 ? * MON,WED,SAT", checkWarningBatteryLevels)
        }
        schedule("0 0 10 ? * *", checkCriticalBatteryLevels)
    }
}

// event handlers
def checkWarningBatteryLevels() {
    def warningLevelDevices = 
    batteryPoweredDevices
    	.findAll { it.currentBattery <= minLevelWarning && it.currentBattery > minLevelCritical }
        .sort { it.currentBattery }
    sendMessage (warningLevelDevices, "WARNING")
}

def checkCriticalBatteryLevels() {
    def criticalLevelDevices = batteryPoweredDevices
    	.findAll { it.currentBattery <= minLevelCritical }
        .sort { it.currentBattery }
    sendMessage(criticalLevelDevices, "CRITICAL")
}

def sendMessage(devices, String headMessage)
{
	if (devices.size() == 0) return;
    
    def msg = headMessage + " : " + devices.collect { "${it.displayName} : ${it.currentBattery}%" }.join("\n")
    
    log.debug msg
    sendPush(msg);
    
    if (phoneNumber) {
        sendSmsMessage ("${phoneNumber}", msg.substring(0,Math.min(msg.length(),140)))
    }
}