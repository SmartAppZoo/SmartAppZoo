/**
 *  Change Notification
 *
 *  Copyright 2019 DON YONCE
 *
 */
definition(
    name: "WebCMS server notification",
    namespace: "dcyonce",
    author: "DON YONCE",
    description: "Send a notification to WebCMS when a device status changes",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Server") {
        input "Server", "text", title: "Server", description: "Enter name of IP address of notification Server", required: true
	}
	section("Select Devices") {
        input "AllDevices", "capability.refresh", title: "Select Devices", required: false, multiple: true
        input "PresenceSensors", "capability.presenceSensor", title: "Select people", required: false, multiple: true
        // input "TemperatureSensors", "capability.temperatureMeasurement", title: "Select temperature sensors", required: false, multiple: true
	    // input "ContactSensors", "capability.contactSensor", title: "Select contact sensors", required: false, multiple: true
        // input "Switches", "capability.switch", title: "Select switches", required: false, multiple: true
        // input "MotionDetectors", "capability.motionSensor", title: "Select motion detectors", required: false, multiple: true
        // input "AccelerationSensors", "capability.accelerationSensor", title: "Select acceleration sensors", required: false, multiple: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// working
	subscribe(AllDevices, "temperature", ChangeHandler)
	subscribe(PresenceSensors, "presence", ChangeHandler)
	subscribe(AllDevices, "contact", ChangeHandler)
	subscribe(AllDevices, "switch", ChangeHandler)
	subscribe(AllDevices, "motion", ChangeHandler)
	subscribe(AllDevices, "acceleration", ChangeHandler)
	subscribe(AllDevices, "lock", ChangeHandler)
	subscribe(AllDevices, "lastActivity", ChangeHandler)
	subscribe(AllDevices, "power", ChangeHandler)
	subscribe(AllDevices, "humidity", ChangeHandler)
	subscribe(AllDevices, "battery", ChangeHandler)

	// may not work
	subscribe(AllDevices, "rssi", ChangeHandler)
	subscribe(AllDevices, "water", ChangeHandler)
	subscribe(AllDevices, "level", ChangeHandler)
	subscribe(AllDevices, "smoke", ChangeHandler)
	
}

def ChangeHandler(evt) {
    UpdateServer(evt)
}

// Sort of works
private UpdateServer(evt) {
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/Services/AtlantisDevelopment/SmartThings.asmx/StatusChange",
        headers: [
            HOST: "${Server}:80"
        ],
        query: [
        	DeviceID: "${evt.deviceId}", 
            Name: "${evt.displayName}", 
            DeviceType: "${evt.device.typeName}", 
            Manufacturer: "${evt.device.manufacturerName}", 
            Model: "${evt.device.modelName}", 
            Location: "${location.name}",
            Capability: "${evt.name}", 
            Value: "${evt.value}"
            ]
    	)
    //sendHubCommand(new physicalgraph.device.HubAction("""GET /Services/AtlantisDevelopment/SmartThings.asmx/StatusChange?Device=Device&Setting=${Device.name}&Value=${Device.value} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN))
    sendHubCommand(result)
}
