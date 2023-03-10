/**
 *  Intel Device (Connect)
 *
 *  Author: Adam's Account
 *  Date: 2014-04-02
 */
preferences {
	section("Title") {
		page(name:"concentratorDiscovery", title:"Intel Device Setup", content:"concentratorDiscovery")
		page(name:"deviceDiscovery", title:"Intel Device Setup", content:"deviceDiscovery", refreshTimeout:5)
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
	if (selectedConcentrator)
	{
		addConcentrator()
	}
	if (selectedDevices)
	{
		addDevices()
	}
}

def addConcentrator() {
	def concentrators = getConcentrators()
	def concentrator = concentrators.find {it.value.mac  == selectedConcentrator}

	if(concentrator) {
		def dni = concentrator.value.ip + ":" + concentrator.value.port
		def d = getChildDevice(dni)
		if(!d) {
			d = addChildDevice("smartthings", "Intel Bridge Device", dni, concentrator.value.hub, ["data":["mac": selectedConcentrator]])
			log.debug "created ${d.displayName} with id ${d.deviceNetworkId}"
		}
		else
		{
			log.debug "found ${d.displayName} with id $dni already exists"
		}
	}
}

def addDevices() {

	def devices = getDevices()

	selectedDevices.each { dni ->
		def d = getChildDevice(dni)
		def newDevice = devices.find { (app.id + "/" + it.value.id) == dni }
		if(!d) {
			def type = fingerprint(newDevice)
			if (type) {
				log.debug "fingerprint thinks it's a $type device...."
				d = addChildDevice("smartthings", type, dni, newDevice.value.hub, ["label":newDevice.value.device])
				log.debug "created ${d.displayName} with id $dni"
			} else {
				log.warn "couldn't find a type for the device's features: ${device.featuresLink}"
			}
		} else {
			log.debug "found ${d.displayName} with id $dni already exists"
		}
	}
}

def fingerprint(device){
	def intelSwitch = ["switch"]
	def intelCamera = ["motion", "facedetect", "streams"]

	if (device.value.featuresLink.containsAll(intelSwitch)) return "Intel Switch"
	if (device.value.featuresLink.containsAll(intelCamera)) return "Intel Camera"

	return false
}

def discover() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discover mdns/dns-sd ._smartthings._tcp._site", physicalgraph.device.Protocol.LAN))
}

def discoverIntelDevices() {

	def concentrators = getConcentrators()

	def host = "10.1.101.47:9080"
	def path = "/"

	def concentrator = concentrators."${selectedConcentrator}"
	if (concentrator)
	{
		host = convertHexToIP(concentrator.ip) + ":" + convertHexToInt(concentrator.port)
		path = concentrator.mdnsPath
	}

	//device endpoint
	if (state.devListLink) {
		path = state.devListLink
	}

	def hubAction = new physicalgraph.device.HubAction(
		method: "GET",
		path: path,
		headers: [HOST:host]
	)

	sendHubCommand(hubAction)
}

//////PAGE 1
def concentratorDiscovery()
{

	int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
	state.refreshCount = refreshCount + 1
	def refreshInterval = 5

	log.debug "REFRESH COUNT :: ${refreshCount}"

	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	if((refreshCount % 3) == 0) {
		discover()
	}

	def concentratorsDiscovered = concentratorsDiscovered()

	return dynamicPage(name:"concentratorDiscovery", title:"Concentrator Discovery Started!", nextPage:"deviceDiscovery", refreshInterval: refreshInterval, uninstall: true) {
		section("Please wait while we discover your device. Select your device below once discovered.") {
			input "selectedConcentrator", "enum", required:false, title:"Select Devices \n(${concentratorsDiscovered.size() ?: 0} found)", multiple:false, options:concentratorsDiscovered
		}
	}
}

//////PAGE 2
def deviceDiscovery()
{
	int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
	state.deviceRefreshCount = deviceRefreshCount + 1
	def refreshInterval = 3

	def options = devicesDiscovered() ?: []

	if((deviceRefreshCount % 3) == 0) {
		discoverIntelDevices()
	}

	def devicesDiscovered = devicesDiscovered()

	return dynamicPage(name:"deviceDiscovery", title:"Device Discovery Started!", nextPage:"deviceDiscovery", refreshInterval: refreshInterval, install:true, uninstall: true) {
		section("Please wait while we discover your device. Select your device below once discovered.") {
			input "selectedDevices", "enum", required:false, title:"Select Devices \n(${devicesDiscovered.size() ?: 0} found)", multiple:true, options:devicesDiscovered
		}
	}
}

def devicesDiscovered() {
	def devices = getDevices()
	def map = [:]
	devices.each {
		def value = it.value.device
		def key = app.id +"/"+ it.value.id
		map["${key}"] = value
	}
	map
}

def getDevices()
{
	if (!state.devices) { state.devices = [:] }
	state.devices
}

def concentratorsDiscovered() {
	def concentrators = getConcentrators()
	def map = [:]
	concentrators.each {
		def value = it.value.name ?: "mDNS Device: ${it.value.mac}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}

def getConcentrators()
{
	if (!state.concentrators) { state.concentrators = [:] }
	state.concentrators
}

def locationHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]

	//INTEL CONCENTRATOR FOUND
	if (parsedEvent?.mdnsPath)
	{
		def concentrators = getConcentrators()

		if (!(concentrators."${parsedEvent?.mac?.toString()}"))
		{ //dev does not exist
			concentrators << ["${parsedEvent.mac.toString()}":parsedEvent]
		}
		else
		{ // update the values

			log.debug "Device was already found in state..."
			def d = concentrators."${parsedEvent.mac.toString()}"
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
	else if (parsedEvent.headers && parsedEvent.body)
	{
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())

		def headers = parseHeaders(headerString)
        log.debug "INTEL HEADERS: $headers"
		def body = new groovy.json.JsonSlurper().parseText(bodyString)


		switch(headers["content-location"]) {
			//BASE RESOURCE
			case "/":
				if (body.devListLink)
				{
					state.devListLink = "/" + body.devListLink
				}

				if (body.subListLink)
				{
					state.subListLink = "/" + body.subListLink
				}
			break

			//DEV LIST
			case "/dev":

				def devices = getDevices()

				body.each { device ->
					device << ["hub":parsedEvent.hub]
					if (!(devices."${device.id}"))
					{ //device does not exist
						devices << ["${device.id}":device]
					}
				}
			break

			//SUB LIST
			case "/sub":

			break

			//DEFAULT
			default:
			log.error "FAILED TO GET CURRENT-LOCATION FROM HEADERS"
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

private Map stringToMap(String str, String delimiter = ',') {
	if (str) {
		def pairs = str.split(delimiter)
		def map = pairs.inject([:]) { data, pairString ->
			def pair = pairString.split(':')
			if(pair.size() > 1) {
				data[pair[0].trim()] = pair.size() > 2 ? pair[1..-1].join(':')?.trim() : pair[1]?.trim()
			} else {
				data[pair[0].trim()] = null
			}
			return data
		}
		return map
	}
	else {
		return [:]
	}
}

private parseHeaders(str){
	Map header = [:]

	str.eachLine { line, lineNumber ->
		if (lineNumber == 0) {
			header.status = line
			return
		}
		header << stringToMap(line)
	}

	header
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
	def d = getChildDevices().find {it.getTypeName() == "Intel Bridge Device"}

	if (d) {
		def parts = d.deviceNetworkId.split(":")
		def ip = convertHexToIP(parts[0])
		def port = convertHexToInt(parts[1])
		return ip + ":" + port
	}
	else
	{
		log.error "Failed to find host address!"
	}

}

////////////////////////////////////////////
//HTTP METHODS
////////////////////////////////////////////
def get(path) {
	log.debug "GET:  $path"
	def hubAction = new physicalgraph.device.HubAction(
		method: "GET",
		path: path,
		headers: [HOST:getHostAddress()]
	)
}

def put(path, body) {
	log.debug "PUT:  $path"
	def hubAction = new physicalgraph.device.HubAction(
		method: "PUT",
		path: path,
		headers: [HOST:getHostAddress()],
		body: body
	)
}

def post(path, body) {
	log.debug "POST:  $path"
	def hubAction = new physicalgraph.device.HubAction(
		method: "POST",
		path: path,
		headers: [HOST:getHostAddress()],
		body: body
	)
}

////////////////////////////////////////////
//CHILD DEVICE METHODS
////////////////////////////////////////////
def parse(childDevice, description) {
	def parsedEvent = parseEventMessage(description)

	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		def body = new groovy.json.JsonSlurper().parseText(bodyString)
	} else {
		log.debug "parse - got something other than headers, body..."
		return []
	}
}

def on(childDevice) {
	log.debug "Executing 'on'"
	sendHubCommand(put("/dev/${getId(childDevice)}/switch", [status: on]))
}

def off(childDevice) {
	log.debug "Executing 'off'"
	sendHubCommand(put("lights/${getId(childDevice)}/switch", [status: off]))
}

private getId(childDevice) {
	return childDevice.device.deviceNetworkId.split("/")[-1]
}
////////////////////////////////////////////
//CHILD DEVICE METHODS
////////////////////////////////////////////