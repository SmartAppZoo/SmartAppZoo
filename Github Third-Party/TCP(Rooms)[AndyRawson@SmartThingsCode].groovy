/*
 *  TCP-Bulbs-Service-Manager.groovy
 *
 *  Author: todd@wackford.net
 *
 ******************************************************************************
 *                        Setup Namespace & OAuth
 ******************************************************************************
 *
 * Namespace:			"wackford"
 *
 * OAuth:				"Enabled"
 *
 ******************************************************************************
 *                                 Changes
 ******************************************************************************
 *
 *  Change 1:	2014-03-06
 *					Initial Release
 *
 *  Change 2:	2014-03-15
 *					a. Documented Header
 *					b. Fixed on()/off() during level changes			
 *					c. Minor code cleanup and UI changes
 *
 *
 * Change 3 (lieberman) 2014-04-02	a. Added RoomGetCarousel to poll()
 * 					b. Added checks in locationHandler() to sync ST status with TCP status
 ******************************************************************************
 *                                   Code
 ******************************************************************************
 */

definition(
    name: "Tcp Bulbs (Rooms)",
    namespace: "wackford",
    author: "SmartThings",
    description: "Connect your TCP bulbs to SmartThings.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/tcp.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/tcp@2x.png"
)

preferences {
	page(name:"Information", title: "Information Needed to Setup Rooms", content: "displayInfo", nextPage: "Gateway", install: false)
	page(name:"Gateway", title: "Begin Connected by TCP Device Discovery", content: "discoverGateway", nextPage: "bulbDiscovery", install: false)
	page(name:"bulbDiscovery", title:"TCP Device Setup", content:"bulbDiscovery", refreshTimeout:5)
}

def displayInfo() {

		def infoNeeded = """To use this app you must first setup rooms in the TCP app.

Setup rooms in the TCP app under Configure, click Manage Rooms and Create New Room."""

		return dynamicPage(name:"Information", title:"Information Needed to Setup Rooms", nextPage:"Gateway", install:false, uninstall: true) {
			section {
				paragraph "$infoNeeded"
			}
		}
}

def discoverGateway() {
	log.debug "In discoverGateway"
	if(canInstallLabs()) {
		state.subscribe = false
		state.gateway = []

		if(!state.subscribe) {
			subscribe(location, null, locationHandler, [filterEvents:false])
			state.subscribe = true
		}

		//send out the search for the gateway, we'll pick up response in locationEvt
		sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:greenwavereality-com:service:gop:1", physicalgraph.device.Protocol.LAN))

		bulbDiscovery()
	}
	else
	{
		def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

		return dynamicPage(name:"Gateway", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
			section {
				paragraph "$upgradeNeeded"
			}
		}

	}
}

private bulbDiscovery() {
	log.debug "In bulbDiscovery"

	def data = "<gwrcmds><gwrcmd><gcmd>RoomGetCarousel</gcmd><gdata><gip><fields>name,control,status</fields></gip></gdata></gwrcmd></gwrcmds>"

	def qParams = [
		cmd: "GWRBatch",
		data: "${data}",
		fmt: "xml"
	]

	def cmd = "/gwr/gop.php?" + toQueryString(qParams)

	// Check for bulbs 
	state.bulbs = state.bulbs ?: [:]

	if (state.bulbs.size() == 0) {
		sendCommand(cmd)
	}

	def options = bulbsDiscovered() ?: []
	def numFound = options.size() ?: 0

	return dynamicPage(name:"bulbDiscovery", title:"Discovering TCP Rooms!", nextPage:"", refreshInterval: 3, install:true, uninstall: true) {
		section("Please wait while we look for TCP Rooms: ${numFound} discovered") {
			input "selectedBulbs", "enum", required:false, title:"Tap here to select rooms to setup", multiple:true, options:options
		}
	}
}

def installed() {
	initialize();
}

def updated() {
	initialize();
}

def initialize() {
	if (selectedBulbs) {
		addBulbs()
	}
}

def addBulbs() {
	log.debug " in addBulbs"

	def bulbs = getBulbs()
	def name  = "Dimmer Switch"
	def deviceFile = "TCP Bulb"

	selectedBulbs.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newBulb = bulbs.find { (it.value.id) == dni }

			d = addChildDevice("wackford", deviceFile, dni, null, [name: "${name}", label: "${newBulb?.value.name}", completedSetup: true])

			if (newBulb?.value.state == "1")   //set ST device state as we find the TCP state to be
				d.on()
			else
				d.off()

			d.setLevel(newBulb?.value.level)

		} else {
			log.debug "We already added this device"
		}
	}
}

def getBulbs()
{
	state.bulbs = state.bulbs ?: [:]
}

Map bulbsDiscovered() {
	def bulbs =  getBulbs()
	log.debug bulbs
	def map = [:]
	if (bulbs instanceof java.util.Map) {
		bulbs.each {
			def value = "${it?.value?.name}"
			def key = it?.value?.id
			map["${key}"] = value
		}
	} else { //backwards compatable
		bulbs.each {
			def value = "${it?.name}"
			def key = it?.id
			map["${key}"] = value
		}
	}
	map
}

def locationHandler(evt) {
	log.debug "In locationHandler()"

	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]

	if (parsedEvent.ssdpTerm?.contains("urn:greenwavereality-com:service:gop:1"))
	{
		//stuff the gateway data
		state.gateway = []
		state.gateway = ([ 'ip' 		: parsedEvent.ip,
			'port'    	: "0050", //tcp returns zeros for some reason, dunno
			'type'   	: 'gateway',
			'dni'     	: parsedEvent.ssdpUSN,
			'path'		: parsedEvent.ssdpPath
		])
	}

	if (parsedEvent.headers && parsedEvent.body)
	{
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())

		def type = (headerString =~ /Content-Type:.*/) ? (headerString =~ /Content-Type:.*/)[0] : null

		def body = new XmlSlurper().parseText(bodyString)

		def devices = []
		def bulbIndex = 1
		def lastRoomName = null

		body.gwrcmd.gdata.gip.'*'.each({

			if (it.name() == 'room')
			{

						if (state.bulbs == null) {
							state.bulbs = [:]
						}
						def bulbDid = it.rid.text() as String
						def theBulb = getChildDevice( bulbDid )
						def bulbState = it.device[0].state.text()
						def bulbLevel = it.device[0].level.text() ? it.device.level[0].text() as Integer : 0


							log.debug( "Logging ${bulbDid}, ${theBulb}, ${bulbState}, ${bulbLevel}" )


						if ( theBulb ) {

							log.debug( "bulb exists in state and the bulb's state is ${bulbState}" )
							def currentBulbState = theBulb.currentValue("switch")
							def currentBulbLevel = theBulb.currentValue( "level" ) as Integer

							if ( currentBulbState == "on" && bulbState == "0" ) {
								log.debug( "ST thinks the bulb is on, but TCP says the bulb is off" )
								sendEvent( bulbDid, [name:"switch",value:"off"] )
							}

							if ( currentBulbState == "off" && bulbState == "1" ) {
								log.debug( "ST thinks the bulb is off, but TCP says the bulb is on" )
								sendEvent( bulbDid, [name:"switch",value:"on"] )
							}

							if ( currentBulbLevel != bulbLevel ) {
								log.debug( "ST thinks the bulb level is ${currentBulbLevel} but TCP says the level is ${bulbLevel}" )
								sendEvent( bulbDid, [name: "level", value: bulbLevel] )
								sendEvent( bulbDid, [name: "switch.setLevel", value:bulbLevel] )

							}
						} else {

							state.bulbs[it.rid.text()] = [  id 		: it.rid.text(),
								name 	: "${it.name.text()}",
								state	: it.device[0].state.text(),
								level 	: it.device[0].level.text() ]
							lastRoomName = roomName
							bulbIndex++
						}
					
				
			}
		})
	}
}

private def parseEventMessage(Map event) {
	//handles  attribute events
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

private sendCommand(data)
{
	def deviceNetworkId = state.gateway.ip + ":" + state.gateway.port

	sendHubCommand(new physicalgraph.device.HubAction("""GET $data HTTP/1.1\r\nHOST: $deviceNetworkId\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}

/**************************************************************************
 Child Device Call In Methods
 **************************************************************************/
def on(childDevice) {
	log.debug "Got On request from child device"

	def dni = childDevice.device.deviceNetworkId

	def data = "<gip><version>1</version><token>1234567890</token><rid>${dni}</rid><value>1</value></gip>"

	def qParams = [
		cmd: "RoomSendCommand",
		data: "${data}",
		fmt: "xml"
	]

	def cmd = "/gwr/gop.php?" + toQueryString(qParams)

	sendCommand(cmd)
}

def off(childDevice) {
	log.debug "Got Off request from child device"

	def dni = childDevice.device.deviceNetworkId

	def data = "<gip><version>1</version><token>1234567890</token><rid>${dni}</rid><value>0</value></gip>"

	def qParams = [
		cmd: "RoomSendCommand",
		data: "${data}",
		fmt: "xml"
	]

	def cmd = "/gwr/gop.php?" + toQueryString(qParams)

	sendCommand(cmd)
}

def setLevel(childDevice, value) {
	log.debug "Got setLevel request from child device"

	def dni = childDevice.device.deviceNetworkId

	def data = "<gip><version>1</version><token>1234567890</token><rid>${dni}</rid><value>${value}</value><type>level</type></gip>"

	def qParams = [
		cmd: "RoomSendCommand",
		data: "${data}",
		fmt: "xml"
	]

	def cmd = "/gwr/gop.php?" + toQueryString(qParams)

	sendCommand(cmd)
}

def poll(childDevice) {
	log.debug "In poll()"
	def data = "<gwrcmds><gwrcmd><gcmd>RoomGetCarousel</gcmd><gdata><gip><fields>name,control,status</fields></gip></gdata></gwrcmd></gwrcmds>"

	def qParams = [
		cmd: "GWRBatch",
		data: "${data}",
		fmt: "xml"
	]

	def cmd = "/gwr/gop.php?" + toQueryString(qParams)
	sendCommand(cmd)
}

/******************************************************************************
 Helper Methods
 ******************************************************************************/
String toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def debugEvent(message, displayEvent) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	log.debug "Generating AppDebug Event: ${results}"
	sendEvent (results)
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

