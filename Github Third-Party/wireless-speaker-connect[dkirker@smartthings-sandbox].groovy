/**
 *  Sonos Service Manager
 *
 *  Author: SmartThings
 */
definition(
	name: "Wireless Speaker (Connect)",
	namespace: "com.openmobl.app.dlnaconnect",
	author: "SmartThings",
	description: "Allows you to control your DLNA compatible speaker from the SmartThings app. Perform basic functions like play, pause, stop, change track, and check artist and song name from the Things screen.",
	category: "SmartThings Labs",
	iconUrl: "http://www.byothe.fr/wp-content/uploads/2013/12/Logo-DLNA.jpg",
	iconX2Url: "http://www.byothe.fr/wp-content/uploads/2013/12/Logo-DLNA.jpg"
)

preferences {
	page(name:"dlnaDiscovery", title:"DLNA Device Setup", content:"dlnaDiscovery", refreshTimeout:5)
}
//PAGES
def dlnaDiscovery()
{
	if(canInstallLabs())
	{
		int dlnaRefreshCount = !state.dlnaRefreshCount ? 0 : state.dlnaRefreshCount as int
		state.dlnaRefreshCount = dlnaRefreshCount + 1
		def refreshInterval = 3

		def options = dlnaDiscovered() ?: []

		def numFound = options.size() ?: 0

		if(!state.subscribe) {
			log.trace "subscribe to location"
			subscribe(location, null, locationHandler, [filterEvents:false])
			state.subscribe = true
		}

		//sonos discovery request every 5 //25 seconds
		if((dlnaRefreshCount % 8) == 0) {
			discoverDlna()
		}

		//setup.xml request every 3 seconds except on discoveries
		if(((dlnaRefreshCount % 1) == 0) && ((dlnaRefreshCount % 8) != 0)) {
			verifyDlnaPlayer()
		}

		return dynamicPage(name:"dlnaDiscovery", title:"Discovery Started!", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) {
			section("Please wait while we discover your speakers. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
				input "selectedDlna", "enum", required:false, title:"Select DLNA (${numFound} found)", multiple:true, options:options
			}
		}
	}
	else
	{
		def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

		return dynamicPage(name:"dlnaDiscovery", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
			section("Upgrade") {
				paragraph "$upgradeNeeded"
			}
		}
	}
}

private discoverDlna()
{
	//consider using other discovery methods
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:service:AVTransport:1", physicalgraph.device.Protocol.LAN))
}


private verifyDlnaPlayer() {
	def devices = getDlnaPlayer().findAll { it?.value?.verified != true }

	if(devices) {
		log.warn "UNVERIFIED PLAYERS!: $devices"
	}

	devices.each {
		verifyDlna((it?.value?.ip + ":" + it?.value?.port), it?.value?.ssdpPath)
	}
}

private verifyDlna(String deviceNetworkId, String ssdpPath) {

	log.trace "dni: $deviceNetworkId"
	String ip = getHostAddress(deviceNetworkId)

	log.trace "ip:" + ip

	//sendHubCommand(new physicalgraph.device.HubAction("""GET $ssdpPath HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
	def result = new physicalgraph.device.HubAction([
		method: "GET",
        path: ssdpPath,
        headers: [Host: ip, Accept: "*/*"]],
        deviceNetworkId
	)
	sendHubCommand(result)
}

Map dlnaDiscovered() {
	def vdlna = getVerifiedDlnaPlayer()
	def map = [:]
	vdlna.each {
		def value = "${it.value.name} (${it.value.model})"
		def key = it.value.ip + ":" + it.value.port
		map["${key}"] = value
	}
	map
}

def getDlnaPlayer()
{
	state.dlna = state.dlna ?: [:]
}

def getVerifiedDlnaPlayer()
{
	getDlnaPlayer().findAll{ it?.value?.verified == true }
}

def getSpecificDlnaPlayer(child)
{
	def dni = child.device.deviceNetworkId
	def dlna = getDlnaPlayer()
	def player = dlna.find { (it.value.ip + ":" + it.value.port) == dni }
    
    return player
}

def installed() {
	log.trace "Installed with settings: ${settings}"
	initialize()}

def updated() {
	log.trace "Updated with settings: ${settings}"
	unschedule()
	initialize()
}

def uninstalled() {
	def devices = getChildDevices()
	log.trace "deleting ${devices.size()} Speaker"
	devices.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

def initialize() {
	// remove location subscription aftwards
	unsubscribe()
	state.subscribe = false

	unschedule()
	scheduleActions()

	if (selectedDlna) {
		addDlna()
	}

	scheduledActionsHandler()
}

def scheduledActionsHandler() {
	log.trace "scheduledActionsHandler()"
	syncDevices()
	refreshAll()

	// TODO - for auto reschedule
	if (!state.threeHourSchedule) {
		scheduleActions()
	}
}

private scheduleActions() {
	def sec = Math.round(Math.floor(Math.random() * 60))
	def min = Math.round(Math.floor(Math.random() * 60))
	def hour = Math.round(Math.floor(Math.random() * 3))
	def cron = "$sec $min $hour/3 * * ?"
	log.debug "schedule('$cron', scheduledActionsHandler)"
	schedule(cron, scheduledActionsHandler)

	// TODO - for auto reschedule
	state.threeHourSchedule = true
	state.cronSchedule = cron
}

private syncDevices() {
	log.trace "Doing DLNA Device Sync!"
	//runIn(300, "doDeviceSync" , [overwrite: false]) //schedule to run again in 5 minutes

	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	discoverDlna()
}

private refreshAll(){
	log.trace "refreshAll()"
	childDevices*.refresh()
	log.trace "/refreshAll()"
}

def addDlna() {
	def players = getVerifiedDlnaPlayer()
	def runSubscribe = false
	selectedDlna.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newPlayer = players.find { (it.value.ip + ":" + it.value.port) == dni }
			log.trace "newPlayer = $newPlayer"
			log.trace "dni = $dni"
			d = addChildDevice("com.openmobl.device.dlna", "DLNA Player", dni, newPlayer?.value.hub, [label:"${newPlayer?.value.name} DLNA"])
			log.trace "created ${d.displayName} with id $dni"

			d.setModel(newPlayer?.value.model)
			log.trace "setModel to ${newPlayer?.value.model}"

            /*d.setDataValue("controlUrl", newPlayer?.value.controlURL)
            d.setDataValue("avTransEventUrl", newPlayer?.value.avTransEventUrl)
            d.setDataValue("renderingControlUrl", newPlayer?.value.renderingControlUrl)
            d.setDataValue("renderingControlEventUrl", newPlayer?.value.renderingControlEventUrl)*/

			runSubscribe = true
		} else {
			log.trace "found ${d.displayName} with id $dni already exists"
		}
	}
}

def locationHandler(evt) {
	log.info "LOCATION HANDLER: $evt.description"
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]

	if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:service:AVTransport:1"))
	{ //SSDP DISCOVERY EVENTS

		log.trace "dlna found"
		def dlna = getDlnaPlayer()

		if (!(dlna."${parsedEvent.ssdpUSN.toString()}"))
		{ //sonos does not exist
			dlna << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
		}
		else
		{ // update the values

			log.trace "Device was already found in state..."

			def d = dlna."${parsedEvent.ssdpUSN.toString()}"
			boolean deviceChangedValues = false

			if(d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
				d.ip = parsedEvent.ip
				d.port = parsedEvent.port
				deviceChangedValues = true
				log.trace "Device's port or ip changed..."
			}

			if (deviceChangedValues) {
				def children = getChildDevices()
				children.each {
					if (it.getDeviceDataByName("mac") == parsedEvent.mac) {
						log.trace "updating dni for device ${it} with mac ${parsedEvent.mac}"
						it.setDeviceNetworkId((parsedEvent.ip + ":" + parsedEvent.port)) //could error if device with same dni already exists
					}
				}
			}
		}
	}
	else if (parsedEvent.headers && parsedEvent.body)
	{ // SONOS RESPONSES
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())

		def type = (headerString.toLowerCase() =~ /content-type:.*/) ? (headerString.toLowerCase() =~ /content-type:.*/)[0] : null
		def body
		log.trace "DLNA REPONSE TYPE: $type"
        log.trace "headers: ${parsedEvent.headers} body: ${parsedEvent.body}"
        log.trace "headers: ${headerString} body: ${bodyString}"
		if (type?.contains("xml"))
		{ // description.xml response (application/xml)
			body = new XmlSlurper().parseText(bodyString)

			//if (body?.device?.modelName?.text().startsWith("Sonos") && !body?.device?.modelName?.text().toLowerCase().contains("bridge") && !body?.device?.modelName?.text().contains("Sub"))
			//{
				def dlna = getDlnaPlayer()
				def player = dlna.find {it?.key?.contains(body?.device?.UDN?.text())}
				if (player)
				{
                	def avTransport = body?.device?.serviceList?.service?.find {it.serviceType?.text() == "urn:schemas-upnp-org:service:AVTransport:1"}
                    def renderControl = body?.device?.serviceList?.service?.find {it.serviceType?.text() == "urn:schemas-upnp-org:service:RenderingControl:1"}
                    
					player.value << [name:body?.device?.friendlyName?.text(),model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), controlUrl: avTransport?.controlURL?.text(), avTransEventUrl: avTransport?.eventSubURL?.text(), renderingControlUrl: renderControl.controlURL?.text(), renderingControlEventUrl: renderControl.eventSubURL?.text(), verified: true]
				}
				else
				{
					log.error "/xml/device_description.xml returned a device that didn't exist"
				}
			//}
		}
		else if(type?.contains("json"))
		{ //(application/json)
			body = new groovy.json.JsonSlurper().parseText(bodyString)
			log.trace "GOT JSON $body"
		}

	}
	else {
		log.trace "cp desc: " + description
		//log.trace description
	}
}

private def parseEventMessage(Map event) {
	//handles sonos attribute events
	return event
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
		else if (part.startsWith('ssdpPath:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ssdpPath = valueString
			}
		}
		else if (part.startsWith('ssdpUSN:')) {
			part -= "ssdpUSN:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpUSN = valueString
			}
		}
		else if (part.startsWith('ssdpTerm:')) {
			part -= "ssdpTerm:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpTerm = valueString
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


/////////CHILD DEVICE METHODS
def parse(childDevice, description) {
	def parsedEvent = parseEventMessage(description)

	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		log.trace "parse() - ${bodyString}"

		def body = new groovy.json.JsonSlurper().parseText(bodyString)
	} else {
		log.trace "parse - got something other than headers,body..."
		return []
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress(d) {
	def parts = d.split(":")
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
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

