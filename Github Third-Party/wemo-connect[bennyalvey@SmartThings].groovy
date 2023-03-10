/**
 *  Wemo Service Manager
 *
 *  Author: superuser
 *  Date: 2014-01-03
 */
preferences {
	page(name:"firstPage", title:"Wemo Device Setup", content:"firstPage")
}

private discoverSwitch()
{
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:Belkin:device:controllee:1", physicalgraph.device.Protocol.LAN))
}

private discoverMotion()
{
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:Belkin:device:sensor:1", physicalgraph.device.Protocol.LAN))
}

private discoverAllWemoTypes()
{
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:Belkin:device:controllee:1/urn:Belkin:device:sensor:1", physicalgraph.device.Protocol.LAN))
}

private getFriendlyName(String deviceNetworkId) {
	sendHubCommand(new physicalgraph.device.HubAction("""GET /setup.xml HTTP/1.1
HOST: ${deviceNetworkId}

""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}

private verifyDevices() {
	def switches = getWemoSwitches().findAll { it?.value?.verified != true }
	def motions = getWemoMotions().findAll { it?.value?.verified != true }
	def devices = switches + motions
	devices.each {
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

		log.debug "REFRESH COUNT :: ${refreshCount}"

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

		def switchesDiscovered = switchesDiscovered()
		def motionsDiscovered = motionsDiscovered()

		return dynamicPage(name:"firstPage", title:"Discovery Started!", nextPage:"", refreshInterval: refreshInterval, install:true, uninstall: true) {
			section("Please wait while we discover your WeMo. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
				input "selectedSwitches", "enum", required:false, title:"Select Wemo Switches \n(${switchesDiscovered.size() ?: 0} found)", multiple:true, options:switchesDiscovered
				input "selectedMotions", "enum", required:false, title:"Select Wemo Motions \n(${motionsDiscovered.size() ?: 0} found)", multiple:true, options:motionsDiscovered
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
	def switches = getWemoSwitches()
	def motions = getWemoMotions()
	def devices = switches + motions
	def list = []

	list = devices?.collect{ [app.id, it.ssdpUSN].join('.') }
}

def switchesDiscovered() {
	def switches = getWemoSwitches().findAll { it?.value?.verified == true }
	def map = [:]
	switches.each {
		def value = it.value.name ?: "WeMo Switch ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.ip + ":" + it.value.port
		map["${key}"] = value
	}
	map
}

def motionsDiscovered() {
	def motions = getWemoMotions().findAll { it?.value?.verified == true }
	def map = [:]
	motions.each {
		def value = it.value.name ?: "WeMo Motion ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.ip + ":" + it.value.port
		map["${key}"] = value
	}
	map
}

def getWemoSwitches()
{
	if (!state.switches) { state.switches = [:] }
	state.switches
}

def getWemoMotions()
{
	if (!state.motions) { state.motions = [:] }
	state.motions
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()

	runIn(5, "subscribeToDevices") //initial subscriptions delayed by 5 seconds
	runIn(300, "doDeviceSync" , [overwrite: false]) //setup ip:port syncing every 5 minutes
	
	// SUBSCRIBE responses come back with TIMEOUT-1801 (30 minutes), so we refresh things a bit before they expire (29 minutes)
	runIn(1740, "refresh", [overwrite: false])
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()

	runIn(5, "subscribeToDevices") //subscribe again to new/old devices wait 5 seconds
}

def resubscribe() {
	log.debug "Resubscribe called, delegating to refresh()"
	refresh()
}

def refresh() {
	log.debug "refresh() called"
	//reschedule the refreshes
	runIn(1740, "refresh", [overwrite: false])

	def devices = getAllChildDevices()
	devices.each { d ->
		// This is intended to do the same thing the WeMo iOS app does when the "refresh" button is pressed,
		// according to live traffic captures performed with Wireshark.
		log.debug "Calling refresh() on device: ${d.id}"
		d.refresh()
	}
}

def subscribeToDevices() {
	def devices = getAllChildDevices()
	devices.each { d ->
		d.subscribe()
	}
}

def addSwitches() {
	def switches = getWemoSwitches()

	selectedSwitches.each { dni ->
		def d = getChildDevice(dni)

		if(!d)
		{
			def newWemoSwitch = switches.find { (it.value.ip + ":" + it.value.port) == dni }
			d = addChildDevice("smartthings", "Wemo Switch", dni, newWemoSwitch?.value?.hub, ["label":newWemoSwitch?.value?.name ?: "Wemo Switch", "data":["mac": newWemoSwitch?.value?.mac]]) //, "preferences":["ip": newWemoSwitch.value.ip, "port":newWemoSwitch.value.port, "path":newWemoSwitch.value.ssdpPath, "term":newWemoSwitch.value.ssdpTerm]])

			log.debug "created ${d.displayName} with id $dni"
			//d.subscribe()

		}
		else
		{
			log.debug "found ${d.displayName} with id $dni already exists"
		}
	}
}

def addMotions() {
	def motions = getWemoMotions()

	selectedMotions.each { dni ->
		def d = getChildDevice(dni)

		if(!d)
		{
			def newWemoMotion = motions.find { (it.value.ip + ":" + it.value.port) == dni }
			d = addChildDevice("smartthings", "Wemo Motion", dni, newWemoMotion?.value?.hub, ["label":newWemoMotion?.value?.name ?: "Wemo Motion", "data":["mac": newWemoMotion?.value?.mac]]) //, "preferences":["ip": newWemoMotion.value.ip, "port":newWemoMotion.value.port, "usn":newWemoMotion.value.ssdpUSN, "path":newWemoMotion.value.ssdpPath, "term":newWemoMotion.value.ssdpTerm]])

			log.debug "created ${d.displayName} with id $dni"
			//d.subscribe()
		}
		else
		{
		   log.debug "found ${d.displayName} with id $dni already exists"
		}
	}
}

def initialize() {
	// remove location subscription afterwards
	 unsubscribe()
	 state.subscribe = false

	if (selectedSwitches)
	{
		addSwitches()
	}

	if (selectedMotions)
	{
		addMotions()
	}
}

def locationHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseDiscoveryMessage(description)
	parsedEvent << ["hub":hub]

	if (parsedEvent?.ssdpTerm?.contains("Belkin:device:controllee")) {

		def switches = getWemoSwitches()

		if (!(switches."${parsedEvent.ssdpUSN.toString()}"))
		{ //if it doesn't already exist
			switches << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
		}
		else
		{ // just update the values

			log.debug "Device was already found in state..."

			def d = switches."${parsedEvent.ssdpUSN.toString()}"
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
						log.debug "updating dni for device ${it} with mac ${parsedEvent.mac}"
						it.setDeviceNetworkId((parsedEvent.ip + ":" + parsedEvent.port)) //could error if device with same dni already exists
						it.subscribe()
					}
				}
			}

		}

	}
	else if (parsedEvent?.ssdpTerm?.contains("Belkin:device:sensor")) {

		def motions = getWemoMotions()

		if (!(motions."${parsedEvent.ssdpUSN.toString()}"))
		{ //if it doesn't already exist
			motions << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
		}
		else
		{ // just update the values

			log.debug "Device was already found in state..."

			def d = motions."${parsedEvent.ssdpUSN.toString()}"
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
						log.debug "updating dni for device ${it} with mac ${parsedEvent.mac}"
						it.setDeviceNetworkId((parsedEvent.ip + ":" + parsedEvent.port)) //could error if device with same dni already exists
						it.subscribe()
					}
				}
			}
		}

	}
	else if (parsedEvent.headers && parsedEvent.body) {

		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		def body = new XmlSlurper().parseText(bodyString)

		if (body?.device?.deviceType?.text().startsWith("urn:Belkin:device:controllee:1"))
		{
			def switches = getWemoSwitches()
			def wemoSwitch = switches.find {it?.key?.contains(body?.device?.UDN?.text())}
			if (wemoSwitch)
			{
				wemoSwitch.value << [name:body?.device?.friendlyName?.text(), verified: true]
			}
			else
			{
				log.error "/setup.xml returned a wemo device that didn't exist"
			}
		}

		if (body?.device?.deviceType?.text().startsWith("urn:Belkin:device:sensor")) //?:1
		{
			def motions = getWemoMotions()
			def wemoMotion = motions.find {it?.key?.contains(body?.device?.UDN?.text())}
			if (wemoMotion)
			{
				wemoMotion.value << [name:body?.device?.friendlyName?.text(), verified: true]
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
	runIn(300, "doDeviceSync" , [overwrite: false]) //schedule to run again in 5 minutes

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

/*def poll() {
	log.debug "Executing 'poll'"
	runIn(600, "poll", [overwrite: false]) //schedule to run again in 10 minutes

	def lastPoll = getLastPollTime()
	def currentTime = now()
	def lastPollDiff = currentTime - lastPoll
	log.debug "lastPoll: $lastPoll, currentTime: $currentTime, lastPollDiff: $lastPollDiff"
	setLastPollTime(currentTime)

	doDeviceSync()
}


def setLastPollTime(currentTime) {
	state.lastpoll = currentTime
}

def getLastPollTime() {
	state.lastpoll ?: now()
}

def now() {
	new Date().getTime()
}*/



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

