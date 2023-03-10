/**
 * Push Events
 */
definition(
    name: "Push Events",
    namespace: "jasonrwise77",
    author: "Jason Wise",
    description: "Push events to another hub",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/Push%20Events.png",
    iconX2Url: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/Push%20Events.png",
    iconX3Url: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/Push%20Events.png"
)

preferences {
	page(name: "main")
}

def main(){
	return (
    	dynamicPage(name: "main", title: "Push Events", uninstall: true, install: true){
      		section("Monitor these devices...") {
            	input "presenceDevices", "capability.presenceSensor", title: "Presence Devices", required:false, multiple: true
            	input "motionDevices", "capability.motionSensor", title: "Motion Sensors", required:false, multiple: true
            	input "contactDevices", "capability.contactSensor", title: "Contact Sensors", required:false, multiple: true
            	input "accelerationDevices", "capability.accelerationSensor", title: "Acceleration Sensors", required:false, multiple: true
            	input "switchDevices", "capability.switch", title: "Switches", required:false, multiple: true
                input "modePush", "bool", title: "Enable mode events"
                input name: "logEnable", type: "bool", title: "Enable debug logging"
			}
    		section("Status") {
    			input "enabled", "bool", title: "Enabled?" 
    		}
    		section ("Device to push data to") {
    			input "ip", "text", title:"IP"
        		input "port", "text", title:"Port", defaultValue: "39501"
    		}
        }
    )
}

def installed() {
	updated()
}


def updated() {
	unsubscribe()
	initialize()
}


def initialize() {
    subscribe(presenceDevices, "presence", handleDeviceEvent)
    subscribe(motionDevices, "motion", handleDeviceEvent)
    subscribe(contactDevices, "contact", handleDeviceEvent)
    subscribe(accelerationDevices, "acceleration", handleDeviceEvent)
    subscribe(switchDevices, "switch", handleDeviceEvent)
    if (modePush) subscribe(location, modeEvent)
    state.zigbeeId = getHubZigbeeId()
}

def modeEvent(evt){
def msg = """POST / HTTP/1.1
HOST: ${ip}:${port}
CONTENT-TYPE: text/plain
DEVICE-NETWORK-ID: ${state.zigbeeId}
CONTENT-LENGTH: ${evt.value.length()}

${evt.value}
"""
	if(enabled) {
		if (logEnable) log.debug "Name: Mode, DNI: ${state.zigbeeId}, value: ${evt.value}"
		sendHubCommand(new physicalgraph.device.HubAction(msg, physicalgraph.device.Protocol.LAN, "${ip}:${port}"))
    }    
}

def handleDeviceEvent(evt) {
def msg = """POST / HTTP/1.1
HOST: ${ip}:${port}
CONTENT-TYPE: text/plain
DEVICE-NETWORK-ID: ${evt?.device?.deviceNetworkId}
CONTENT-LENGTH: ${evt.value.length()}

${evt.value}
"""
	if(enabled) {
		if (logEnable) log.debug "Name: ${evt.device.displayName}, DNI: ${evt.device.deviceNetworkId}, value: ${evt.value}"
		sendHubCommand(new physicalgraph.device.HubAction(msg, physicalgraph.device.Protocol.LAN, "${ip}:${port}"))
    }

}

def getHubZigbeeId(){
    def hubID
    if (myHub){
        hubID = myHub.zigbeeId
    } else {
        def hubs = location.hubs.findAll{ it.type == physicalgraph.device.HubType.PHYSICAL } 
        //log.debug "hub count: ${hubs.size()}"
        if (hubs.size() == 1) hubID = hubs[0].zigbeeId 
    }
    //log.debug "hubID: ${hubID}"
    return hubID
}
