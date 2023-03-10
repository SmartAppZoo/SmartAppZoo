/**
 *  mDNS Discovery Example
 *
 *  Author: smartthings
 *  Date: 2014-04-02
 */
preferences {
	section("Title") {
		page(name:"firstPage", title:"mDNS Device Setup", content:"firstPage")
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
	// TODO: subscribe to attributes, devices, locations, etc.
}

def discover() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discover mdns/dns-sd ._smartthings._tcp._site", physicalgraph.device.Protocol.LAN))
}

def firstPage()
{
		int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
		state.refreshCount = refreshCount + 1
		def refreshInterval = 5

		//log.debug "REFRESH COUNT :: ${refreshCount}"

		if(!state.subscribe) {
			subscribe(location, null, locationHandler, [filterEvents:false])
			state.subscribe = true
		}

		if((refreshCount % 3) == 0) {
			discover()
		}

		def devicesDiscovered = devicesDiscovered()

		return dynamicPage(name:"firstPage", title:"Discovery Started!", nextPage:"", refreshInterval: refreshInterval, install:true, uninstall: true) {
			section("Please wait while we discover your device. Select your device below once discovered.") {
				input "selectedDevices", "enum", required:false, title:"Select Devices \n(${devicesDiscovered.size() ?: 0} found)", multiple:true, options:devicesDiscovered
			}
		}
}

def devicesDiscovered() {
	def devices = getDevices()
	def map = [:]
	devices.each {
		def value = it.value.name ?: "mDNS Device: ${it.value.mac}"
		def key = it.value.ip + ":" + it.value.port
		map["${key}"] = value
	}
	map
}

def getDevices()
{
	if (!state.devices) { state.devices = [:] }
	state.devices
}

def locationHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]

	if (parsedEvent?.mdnsPath)
	{
		def devices = getDevices()

		if (!(devices."${parsedEvent?.mac?.toString()}"))
		{ //device does not exist
			devices << ["${parsedEvent.mac.toString()}":parsedEvent]
		}
		else
		{ // update the values

			log.debug "Device was already found in state..."
			def d = device."${parsedEvent.mac.toString()}"
			boolean deviceChangedValues = false

			if(d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
				d.ip = parsedEvent.ip
				d.port = parsedEvent.port
				deviceChangedValues = true
				log.debug "mdns device's port or ip changed..."
			}

			if (deviceChangedValues) {
				def children = getChildDevices()
				children.each {
					if (it.getDeviceDataByName("mac") == parsedEvent.mac) {
						log.debug "updating dni for device ${it} with mac ${parsedEvent.mac}"
						it.setDeviceNetworkId((parsedEvent.ip + ":" + parsedEvent.port))
					}
				}
			}
		}
	}
}

private def parseEventMessage(String description) {
	def event = [:]
	def parts = description.split(',')
	parts.each { part ->
		part = part.trim()
		if (part.startsWith('devicetype:')) {
			def valueString = part.split(":")[1].trim()
			event.devicetype = valueString
		}
		else if (part.startsWith('mac:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.mac = valueString
			}
		}
		else if (part.startsWith('networkAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ip = valueString
			}
		}
		else if (part.startsWith('deviceAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.port = valueString
			}
		}
		else if (part.startsWith('mdnsPath:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.mdnsPath = valueString
			}
		}
		else if (part.startsWith('headers')) {
			part -= "headers:"
			def valueString = part.trim()
			if (valueString) {
				event.headers = valueString
			}
		}
		else if (part.startsWith('body')) {
			part -= "body:"
			def valueString = part.trim()
			if (valueString) {
				event.body = valueString
			}
		}
	}

	event
}