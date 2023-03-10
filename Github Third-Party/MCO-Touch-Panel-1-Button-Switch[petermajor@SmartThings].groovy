definition(
	name: "MCO Touch Panel 1 Button Switch",
	namespace: "petermajor",
	author: "Peter Major",
	description: "App to sync the endpoints on the MCO Touch Panel Switch with devices",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
preferences {
	section("1 Button Switch") {
		input "master", "capability.zwMultichannel", title: "Switch?", multiple: false, required: true
	}
	section("Controls this switch") {
		input "switch1", "capability.switch", title: "Button 1", multiple: false, required: false
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
	subscribe(master, "switch.on", onMaster)
	subscribe(master, "switch.off", offMaster)
    
	subscribe(switch1, "switch.on", onSwitch)
	subscribe(switch1, "switch.off", offSwitch)
}

def onMaster(evt) {
    log.debug "MCO-1-APP-onMaster $evt"
	switch1.on()
}

def offMaster(evt) {
    log.debug "MCO-1-APP-offMaster $evt"
	switch1.off()
}

def onSwitch(evt) {
    log.debug "MCO-1-APP-onSwitch $evt"
	master.on()
}

def offSwitch(evt) {
    log.debug "MCO-1-APP-offSwitch $evt"
	master.off()
}
