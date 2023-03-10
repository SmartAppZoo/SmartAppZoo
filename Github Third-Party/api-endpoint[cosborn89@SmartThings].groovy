/**
 *  App Endpoint API Access Example
 *
 *  Author: SmartThings
 */


// Automatically generated. Make future change here.
definition(
    name: "API Endpoint",
    namespace: "tierneykev",
    author: "Kevin Tierney",
    description: "Modified version of the API endpoint example",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences {
	section("Allow Endpoint to Control These Things...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
        input "motion", "capability.motionSensor", title: "Which Motion Sensors?", multiple: true, required: false
        input "contacts", "capability.contactSensor", title: "Which Contact Sensors?", multiple: true, required: false
	}
}

mappings {
	path("/devices/:deviceType"){ action: [GET: "listDevices"]}
	path("/devices/:deviceType/:id"){ action: [GET: "showDevices"]}
    path("/devices/:deviceType/:id/:cmd"){ action: [GET: "updateDevices"]}

}

def installed() {}
def updated() {}
def deviceHandler(evt) {}

def listDevices(){
	switch(params.deviceType){
    case "switches":
    	switches: switches?.collect{[type: "switch", id: it.id, name: it.displayName, status: it.currentValue('switch')]}?.sort{it.name}
    	break
    case "motion":
    	 motion: motion?.collect{[type: "motion", id: it.id, name: it.displayName, status: it.currentValue('motion')]}?.sort{it.name}
    	break
    case "contact":
        contacts: contacts?.collect{[type: "contact", id: it.id, name: it.displayName, status: it.currentValue('contact')]}?.sort{it.name}
        break
    }
}

def showDevices() {
	switch(params.deviceType){
    case "switches":
    	show(switches, "switch")
        break
    case "motion":
    	show(motion, "motion")
        break       
    case "contact":
    	show(contacts, "contact")
        break            
    }
}


void updateDevices() {
    switch(params.deviceType){
    case "switches":
    	update(switches)
        break
    }
	
}


private void update(devices) {
	log.debug "update, request: params: ${params}, devices: $devices.id"
    def command = params.cmd

    
    def device = devices.find { it.id == params.id }

    if (!device) { 
        httpError(404, "Device not found")
    } else {
        if(command == "toggle"){
            if(device.currentValue('switch') == "on")
            device.off();
            else
                device.on();
                          
        } else {
            device."$command"()
        }
    }

}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = type 
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
	}
}