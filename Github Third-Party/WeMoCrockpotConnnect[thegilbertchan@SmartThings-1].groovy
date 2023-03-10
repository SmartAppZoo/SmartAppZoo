/**
 *  Wemo Crockpot Service Manager
 *
 *  Author: Kevin Tierney
 *  Date: 2013-09-06
 */
definition(
    name: "Wemo Crockpot (Connect)",
    namespace: "wemo",
    author: "Kevin Tierney",
    description: "Allows you to integrate your WeMo Crockpot with SmartThings.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/wemo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/wemo@2x.png"
)

preferences {
	page(name:"firstPage", title:"Wemo Device Setup", content:"firstPage")
}

private discoverAllWemoTypes()
{
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:Belkin:device:crockpot:1", physicalgraph.device.Protocol.LAN))
}

private getFriendlyName(String deviceNetworkId) {
	sendHubCommand(new physicalgraph.device.HubAction("""GET /setup.xml HTTP/1.1
HOST: ${deviceNetworkId}

""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}

private verifyDevices() {
	def crockpots = getWemoCrockpots().findAll { it?.value?.verified != true }

	crockpots.each {
		getFriendlyName((it.value.ip + ":" + it.value.port))
	}
}

def firstPage()
{
	if(canInstallLabs())
	{
		int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
		state.refreshCount = refreshCount + 1
		def refreshInterval = 5

		//log.debug "REFRESH COUNT :: ${refreshCount}"

		if(!state.subscribe) {
			subscribe(location, null, locationHandler, [filterEvents:false])
			state.subscribe = true
		}

		//ssdp request every 25 seconds
		if((refreshCount % 5) == 0) {
			discoverAllWemoTypes()
		}

		//setup.xml request every 5 seconds except on discoveries
		if(((refreshCount % 1) == 0) && ((refreshCount % 5) != 0)) {
			verifyDevices()
		}


        def crockpotsDiscovered = crockpotsDiscovered()

		return dynamicPage(name:"firstPage", title:"Discovery Started!", nextPage:"", refreshInterval: refreshInterval, install:true, uninstall: selectedSwitches != null || selectedMotions != null || selectedLightSwitches != null) {
			section("Select a device...") {
                input "selectedCrockpots", "enum", required:false, title:"Select Wemo Crockpots \n(${crockpotsDiscovered.size() ?: 0} found)", multiple:true, options:crockpotsDiscovered
			}
		}
	}
	else
	{
		def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

		return dynamicPage(name:"firstPage", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
			section("Upgrade") {
				paragraph "$upgradeNeeded"
			}
		}
	}
}

def devicesDiscovered() {
    def crockpots = getWemoCrockpots()
	def list = []
	list = crockpotst{ [app.id, it.ssdpUSN].join('.') }
}


def crockpotsDiscovered() {
	def crockpots = getWemoCrockpots().findAll { it?.value?.verified == true }
	def map = [:]
	crockpots.each {
		def value = it.value.name ?: "Crock-Pot® Slow Cooker ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}



def getWemoCrockpots()
{
	if (!state.crockpots) { state.crockpots = [:] }
	state.crockpots
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()

	runIn(5, "subscribeToDevices") //initial subscriptions delayed by 5 seconds
	runIn(10, "refreshDevices") //refresh devices, delayed by 10 seconds
	runIn(900, "doDeviceSync" , [overwrite: false]) //setup ip:port syncing every 15 minutes

	// SUBSCRIBE responses come back with TIMEOUT-1801 (30 minutes), so we refresh things a bit before they expire (29 minutes)
	runIn(1740, "refresh", [overwrite: false])
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()

	runIn(5, "subscribeToDevices") //subscribe again to new/old devices wait 5 seconds
	runIn(10, "refreshDevices") //refresh devices again, delayed by 10 seconds
}

def resubscribe() {
	log.debug "Resubscribe called, delegating to refresh()"
	refresh()
}

def refresh() {
	log.debug "refresh() called"
	//reschedule the refreshes
	runIn(1740, "refresh", [overwrite: false])
	refreshDevices()
}

def refreshDevices() {
	log.debug "refreshDevices() called"
	def devices = getAllChildDevices()
	devices.each { d ->
		log.debug "Calling refresh() on device: ${d.id}"
		d.refresh()
	}
}

def subscribeToDevices() {
	log.debug "subscribeToDevices() called"
	def devices = getAllChildDevices()
	devices.each { d ->
		d.subscribe()
	}
}
def addCrockpots() {
	def crockpots = getWemoCrockpots()

	selectedCrockpots.each { dni ->
		def selectedCrockpot = crockpots.find { it.value.mac == dni } ?: switches.find { "${it.value.ip}:${it.value.port}" == dni }
		def d
		if (selectedCrockpot) {
			d = getChildDevices()?.find {
				it.dni == selectedCrockpot.value.mac || it.device.getDataValue("mac") == selectedCrockpot.value.mac
			}
		}

		if (!d) {
			log.debug "Creating WeMo Crockpot with dni: ${selectedCrockpot.value.mac}"
			d = addChildDevice("wemo", "Wemo Crockpot", selectedCrockpot.value.mac, selectedCrockpot?.value.hub, [
				"label": selectedCrockpot?.value?.name ?: "Wemo Crockpot",
				"data": [
					"mac": selectedCrockpot.value.mac,
					"ip": selectedCrockpot.value.ip,
					"port": selectedCrockpot.value.port
				]
			])

			log.debug "Created ${d.displayName} with id: ${d.id}, dni: ${d.deviceNetworkId}"
		} else {
			log.debug "found ${d.displayName} with id $dni already exists"
		}
	}
}

def initialize() {
	// remove location subscription afterwards
	 unsubscribe()
	 state.subscribe = false
    
    if (selectedCrockpots)
	{
		addCrockpots()
	}
}

def locationHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseDiscoveryMessage(description)
	parsedEvent << ["hub":hub]

if (parsedEvent?.ssdpTerm?.contains("Belkin:device:crockpot")) {

		def crockpots = getWemoCrockpots()

		if (!(crockpots."${parsedEvent.ssdpUSN.toString()}"))
		{ //if it doesn't already exist
			crockpots << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
		}
		else
		{ // just update the values

			log.debug "Device was already found in state..."

			def d = crockpots."${parsedEvent.ssdpUSN.toString()}"
			boolean deviceChangedValues = false

			if(d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
				d.ip = parsedEvent.ip
				d.port = parsedEvent.port
				deviceChangedValues = true
				log.debug "Device's port or ip changed..."
			}

			if (deviceChangedValues) {
				def children = getChildDevices()
				log.debug "Found children ${children}"
				children.each {
					if (it.getDeviceDataByName("mac") == parsedEvent.mac) {
						log.debug "updating ip and port, and resubscribing, for device ${it} with mac ${parsedEvent.mac}"
						it.subscribe(parsedEvent.ip, parsedEvent.port)
					}
				}
			}

		}

	} 

	
	else if (parsedEvent.headers && parsedEvent.body) {

		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		def body = new XmlSlurper().parseText(bodyString)

		if (body?.device?.deviceType?.text().startsWith("urn:Belkin:device:crockpot:1"))
		{
			def crockpots = getWemoCrockpots()
			def wemoCrockpot = crockpots.find {it?.key?.contains(body?.device?.UDN?.text())}
			if (wemoCrockpot)
			{
				wemoCrockpot.value << [name:body?.device?.friendlyName?.text(), verified: true]
			}
			else
			{
				log.error "/setup.xml returned a wemo device that didn't exist"
			}
		}


	}
}

private def parseDiscoveryMessage(String description) {
	def device = [:]
	def parts = description.split(',')
	parts.each { part ->
		part = part.trim()
		if (part.startsWith('devicetype:')) {
			def valueString = part.split(":")[1].trim()
			device.devicetype = valueString
		}
		else if (part.startsWith('mac:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				device.mac = valueString
			}
		}
		else if (part.startsWith('networkAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				device.ip = valueString
			}
		}
		else if (part.startsWith('deviceAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				device.port = valueString
			}
		}
		else if (part.startsWith('ssdpPath:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				device.ssdpPath = valueString
			}
		}
		else if (part.startsWith('ssdpUSN:')) {
			part -= "ssdpUSN:"
			def valueString = part.trim()
			if (valueString) {
				device.ssdpUSN = valueString
			}
		}
		else if (part.startsWith('ssdpTerm:')) {
			part -= "ssdpTerm:"
			def valueString = part.trim()
			if (valueString) {
				device.ssdpTerm = valueString
			}
		}
		else if (part.startsWith('headers')) {
			part -= "headers:"
			def valueString = part.trim()
			if (valueString) {
				device.headers = valueString
			}
		}
		else if (part.startsWith('body')) {
			part -= "body:"
			def valueString = part.trim()
			if (valueString) {
				device.body = valueString
			}
		}
	}

	device
}

def doDeviceSync(){
	log.debug "Doing Device Sync!"
	runIn(900, "doDeviceSync" , [overwrite: false]) //schedule to run again in 15 minutes

	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	discoverAllWemoTypes()
}

def pollChildren() {
	def devices = getAllChildDevices()
	devices.each { d ->
		//only poll switches?
		d.poll()
	}
}

def delayPoll() {
	log.debug "Executing 'delayPoll'"

	runIn(5, "pollChildren")
}



private Boolean canInstallLabs()
{
	return hasAllHubsOver("000.011.00603")
}

private Boolean hasAllHubsOver(String desiredFirmware)
{
	return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

private List getRealHubFirmwareVersions()
{
	return location.hubs*.firmwareVersionString.findAll { it }
}
