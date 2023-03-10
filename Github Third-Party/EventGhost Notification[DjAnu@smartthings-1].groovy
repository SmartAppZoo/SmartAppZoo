/**
 *  EventGhost Notification
 *
 *  Author: Richard
 */
definition(
    name: "EventGhost Notification",
    author: "Richard",
    description: "Send Smartthings Device Status to Eventghost via webserver plugin",
    category: "My Apps",
    iconUrl: "http://s13.postimg.org/hydblwntv/Event_Ghost.png",
    iconX2Url: "http://s13.postimg.org/hydblwntv/Event_Ghost.png"
)

preferences {
	section("Choose one or more, when..."){
		input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
        input "Nomotion", "capability.motionSensor", title: "No Motion Here", required: false, multiple: true
		input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
		input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
		input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
		input "Noacceleration", "capability.accelerationSensor", title: "No Acceleration Detected", required: false, multiple: true
        input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
		input "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true 
        input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
		input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true 
		input "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
		input "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
        input "locksLocked", "capability.lock", title: "Lock Locked?",  required: false, multiple:true
        input "locksUnlocked", "capability.lock", title: "Lock Unlock?",  required: false, multiple:true
		input "setMode", "mode", title: "Mode?",  required: false, multiple:true
	}

    
    section("Server address and port number"){
		input "server", "text", title: "Server IP", description: "Your Server IP", required: true
		input "port", "number", title: "Port", description: "Port Number", required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
    initialize()
}

def initialize() {
	subscribe(app, touch)

}


def touch(evt) {
 	def ip = "${settings.server}:${settings.port}"
	def deviceNetworkId = "1234"	
    sendHubCommand(new physicalgraph.device.HubAction("""GET /?Hello%20From%20Smartthings HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))

}        

def subscribeToEvents() {
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(Noacceleration, "acceleration.inactive", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
    subscribe(Nomotion, "motion.inactive", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler2)
	subscribe(departurePresence, "presence.not present", eventHandler3)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)
    subscribe(locksLocked, "lock.locked", eventHandler)
    subscribe(locksUnlocked, "lock.unlocked", eventHandler)
    subscribe(location, modeChangeHandler)

}

def eventHandler(evt) {
	def toReplace = evt.displayName
	def replaced = toReplace.replaceAll(' ', '%20')
    def name = replaced
    def value = evt.value
    def ip = "${settings.server}:${settings.port}"
	def deviceNetworkId = "1234"
	sendHubCommand(new physicalgraph.device.HubAction("""GET /?${name}%20${value} HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}


def eventHandler2(evt) {
	def toReplace = evt.displayName
	def replaced = toReplace.replaceAll(' ', '%20')
    def name = replaced
    def ip = "${settings.server}:${settings.port}"
	def deviceNetworkId = "1234"
    sendHubCommand(new physicalgraph.device.HubAction("""GET /?${name}%20Present HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
					
}

def eventHandler3(evt) {
	def toReplace = evt.displayName
	def replaced = toReplace.replaceAll(' ', '%20')
    def name = replaced
	def ip = "${settings.server}:${settings.port}"
	def deviceNetworkId = "1234"
    sendHubCommand(new physicalgraph.device.HubAction("""GET /?${name}%20Not%20Present HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
					
} 

def modeChangeHandler(evt) {
	def mode = evt.value
    def ip = "${settings.server}:${settings.port}"
	def deviceNetworkId = "1234"
    sendHubCommand(new physicalgraph.device.HubAction("""GET /?${mode} HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
					
}