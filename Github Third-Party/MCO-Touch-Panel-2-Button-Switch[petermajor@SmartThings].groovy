definition(
	name: "MCO Touch Panel 2 Button Switch",
	namespace: "petermajor",
	author: "Peter Major",
	description: "App to sync the endpoints on the MCO Touch Panel Switch with devices",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
preferences {
	section("2 Button Switch") {
		input "master", "capability.zwMultichannel", title: "Switch?", multiple: false, required: true
	}
	section("Controls these switches") {
		input "switch1", "capability.switch", title: "Button 1", multiple: false, required: false
		input "switch2", "capability.switch", title: "Button 2", multiple: false, required: false
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

def initialize()
{   
	subscribe(master, "epEvent", endpointEvent)
    
	subscribe(switch1, "switch.on", onHandler)
	subscribe(switch2, "switch.on", onHandler)
	subscribe(switch1, "switch.off", offHandler)
	subscribe(switch2, "switch.off", offHandler)    
}

def endpointEvent(evt) {
    log.debug "MCOAPP-endpointEvent $evt"

	def values = evt.value.split(":")
	def endpoint = values[0]
	def payload = values[1]

	def theswitch = getSwitch(endpoint)

	if (payload == "200300"){
    	log.debug "MCOAPP-endpointEvent-turning switch off"
		theswitch.off();
	} else if (payload == "2003FF"){
    	log.debug "MCOAPP-endpointEvent-turning switch on"
		theswitch.on();
	} else {
    	log.debug "MCOAPP-endpointEvent-unexpected payload"
	}
}

def onHandler(evt) {
    log.debug "MCOAPP-onHandler $evt"
	def endpoint = getEndpoint(evt.deviceId)
	master.on(endpoint)
}

def offHandler(evt) {
    log.debug "MCOAPP-offHandler $evt"
	def endpoint = getEndpoint(evt.deviceId)
	master.off(endpoint)
}

def getSwitch(endpoint) {
	def result
	switch (endpoint) {
		case "1":
			result = switch1
			break
		case "2":
			result = switch2
			break
	}

	result
}

def getEndpoint(deviceId) {
	def result
	switch (deviceId) {
		case switch1?.id:
			result = 1
			break
		case switch2?.id:
			result = 2
			break
	}
	result
}

