/**
 *  MediaRenderer Service Manager v 1.9.0
 *
 *  Author: SmartThings - Ulises Mujica 
 */
 
definition(
	name: "MediaRender (Connect)",
	namespace: "mujica",
	author: "SmartThings - Ulises Mujica",
	description: "Allows you to control your Media Render from the SmartThings app. Perform basic functions like play, pause, stop, change track, and check artist and song name from the Things screen.",
	category: "SmartThings Labs",
	iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.secondary.smartapps-tile?displaySize=2x",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.secondary.smartapps-tile?displaySize=2x"
)

preferences {
    page(name: "MainPage", title: "Search and config your Media Renderers",nextPage:"", install:true, uninstall: true){
    	section("") {
            href(name: "discover",title: "Discovery process",required: false,page: "mediaRendererDiscovery",description: "tap to start searching")
        }
        section("Options", hideable: true, hidden: true) {
            input("refreshMRInterval", "number", title:"Enter refresh players interval (min)",defaultValue:"5", required:false)
        }
    }
    page(name: "mediaRendererDiscovery", title:"Discovery Started!", nextPage:"")
}

def mediaRendererDiscovery()
{
	if(canInstallLabs())
	{
		int mediaRendererRefreshCount = !state.mediaRendererRefreshCount ? 0 : state.mediaRendererRefreshCount as int
		state.mediaRendererRefreshCount = mediaRendererRefreshCount + 1
		def refreshInterval = 5

		def options = mediaRenderersDiscovered() ?: []

		def numFound = options.size() ?: 0

		if(!state.subscribe) {
			subscribe(location, null, locationHandler, [filterEvents:false])
			state.subscribe = true
		}

		//mediaRenderer discovery request every 5 //25 seconds
		if((mediaRendererRefreshCount % 8) == 0) {
			discoverMediaRenderers()
		}

		//setup.xml request every 3 seconds except on discoveries
		if(((mediaRendererRefreshCount % 1) == 0) && ((mediaRendererRefreshCount % 8) != 0)) {
			verifyMediaRendererPlayer()
		}
		
		return dynamicPage(name:"mediaRendererDiscovery", title:"Discovery Started!", nextPage:"", refreshInterval:refreshInterval) {
			section("Please wait while we discover your MediaRenderer. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
				input "selectedMediaRenderer", "enum", required:false, title:"Select Media Renderer (${numFound} found)", multiple:true, options:options
			}
		}
	}
	else
	{
		def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

		To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

		return dynamicPage(name:"mediaRendererDiscovery", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
			section("Upgrade") {
				paragraph "$upgradeNeeded"
			}
		}
	}
}

private discoverMediaRenderers()
{
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaRenderer:1", physicalgraph.device.Protocol.LAN))
}


private verifyMediaRendererPlayer() {
	def devices = getMediaRendererPlayer().findAll { it?.value?.verified != true }
	
	devices.each {
		verifyMediaRenderer((it?.value?.ip + ":" + it?.value?.port), it?.value?.ssdpPath)
	}
}

private verifyMediaRenderer(String deviceNetworkId, String ssdpPath) {
	String ip = getHostAddress(deviceNetworkId)
	if(!ssdpPath){
		ssdpPath = "/"
	}

	sendHubCommand(new physicalgraph.device.HubAction("""GET $ssdpPath HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}

Map mediaRenderersDiscovered() {
	def vmediaRenderers = getVerifiedMediaRendererPlayer()
	def map = [:]
	vmediaRenderers.each {
		def value = "${it.value.name}"
		def key = it.value.ip + ":" + it.value.port
		map["${key}"] = value
	}
	map
}

def getMediaRendererPlayer()
{
	state.mediaRenderers = state.mediaRenderers ?: [:]
}

def getVerifiedMediaRendererPlayer()
{
	getMediaRendererPlayer().findAll{ it?.value?.verified == true }
}

def installed() {
	initialize()}

def updated() {
	unschedule()
	initialize()
}

def uninstalled() {
	def devices = getChildDevices()
	devices.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

def initialize() {
	// remove location subscription aftwards
	unsubscribe()
	state.subscribe = false

	unschedule()

	if (selectedMediaRenderer) {
		addMediaRenderer()
	}
    scheduleActions()
    scheduledRefreshHandler()
}

def scheduledRefreshHandler() {
	refreshAll()
}

def scheduledActionsHandler() {
    syncDevices()
	runIn(60, scheduledRefreshHandler) 

}

private scheduleActions() {
	def minutes = Math.max(settings.refreshMRInterval.toInteger(),3)
    def cron = "0 0/${minutes} * * * ?"
   	schedule(cron, scheduledActionsHandler)
}



private syncDevices() {
	log.debug "syncDevices()"
	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	discoverMediaRenderers()
}

private refreshAll(){
	log.trace "refresh all"
	childDevices*.refresh()
}

def addMediaRenderer() {
	def players = getVerifiedMediaRendererPlayer()
	def runSubscribe = false
	selectedMediaRenderer.each { dni ->
		def d = getChildDevice(dni)
		log.trace "dni $dni"
		if(!d) {
			def newPlayer = players.find { (it.value.ip + ":" + it.value.port) == dni }
			if (newPlayer){
				//MediaRenderer
				d = addChildDevice("mujica", "DLNA Player", dni, newPlayer?.value.hub, [label:"${newPlayer?.value.name} Speaker","data":["model":newPlayer?.value.model,"avtcurl":newPlayer?.value.avtcurl,"avteurl":newPlayer?.value.avteurl,"rccurl":newPlayer?.value.rccurl,"rceurl":newPlayer?.value.rceurl,"udn":newPlayer?.value.udn,"dni":dni]])
			}
			runSubscribe = true
		} 
	}
}

def locationHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseEventMessage(description)
    def msg = parseLanMessage(description)
	parsedEvent << ["hub":hub]

	if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:MediaRenderer:1"))
	{ //SSDP DISCOVERY EVENTS
		log.debug "MediaRenderer device found" + parsedEvent
		def mediaRenderers = getMediaRendererPlayer()

		
		if (!(mediaRenderers."${parsedEvent.ssdpUSN.toString()}"))
		{ //mediaRenderer does not exist
			mediaRenderers << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
		}
		else
		{ // update the values

			def d = mediaRenderers."${parsedEvent.ssdpUSN.toString()}"
			boolean deviceChangedValues = false
			if(d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
				d.ip = parsedEvent.ip
				d.port = parsedEvent.port
				deviceChangedValues = true
			}
			if (deviceChangedValues) {
                def children = getChildDevices()
				children.each {
                    if (parsedEvent.ssdpUSN.toString().contains(it.getDataValue("udn"))) {
						it.setDeviceNetworkId((parsedEvent.ip + ":" + parsedEvent.port)) //could error if device with same dni already exists
						it.updateDataValue("dni", (parsedEvent.ip + ":" + parsedEvent.port))
						log.trace "Updated Device IP"
					}
				}
			}
		}
	}
	else if (parsedEvent.headers && parsedEvent.body)
	{ // MEDIARENDER RESPONSES
        def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())

		def type = (headerString =~ /Content-Type:.*/) ? (headerString =~ /Content-Type:.*/)[0] : null
		def body
		if (bodyString?.contains("xml"))
		{ // description.xml response (application/xml)
			body = new XmlSlurper().parseText(bodyString)

			// Avoid add sonos devices	
            
			if ( !body?.device?.modelName?.text().startsWith("Sonos") && body?.device?.deviceType?.text().contains("urn:schemas-upnp-org:device:MediaRenderer:1"))
			{
				def avtcurl = ""
				def avteurl = ""
				def rccurl = ""
				def rceurl = ""

				
                
                
				body?.device?.serviceList?.service?.each{
				  if (it?.serviceType?.text().contains("AVTransport")) {
						avtcurl = it?.controlURL.text()
						avteurl = it?.eventSubURL.text()
					}
					else if (it?.serviceType?.text().contains("RenderingControl")) {
						rccurl = it?.controlURL.text()
						rceurl = it?.eventSubURL.text()
					}
				}
				
				
				def mediaRenderers = getMediaRendererPlayer()
				def player = mediaRenderers.find {it?.key?.contains(body?.device?.UDN?.text())}
				if (player)
				{
					player.value << [name:body?.device?.friendlyName?.text(),model:body?.device?.modelName?.text(), serialNumber:body?.device?.UDN?.text(), verified: true,avtcurl:avtcurl,avteurl:avteurl,rccurl:rccurl,rceurl:rceurl,udn:body?.device?.UDN?.text()]
				}
				
			}
		}
		else if(type?.contains("json"))
		{ //(application/json)
			body = new groovy.json.JsonSlurper().parseText(bodyString)
		}
		
	}
	
}

private def parseEventMessage(Map event) {
	//handles mediaRenderer attribute events
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

		def body = new groovy.json.JsonSlurper().parseText(bodyString)
	} else {
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