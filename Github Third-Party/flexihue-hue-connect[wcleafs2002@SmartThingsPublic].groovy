/**
 *  FLEXiHue Service Manager (Hue Connect)
 *
 *  Author: SmartThings / Anthony Pastor
 *
 *  To-Do:
 *  	- DNI = MAC address
 *
 */
 
definition(
	name: "FLEXiHue (Hue Connect)",
    namespace: "info_fiend",
    author: "Anthony Pastor",
	description: "Allows you to connect your Philips Hue lights using the FLEXiHue device type (which contains custom attributes and commands for storage of Scene data) with SmartThings and control them from your Things area or Dashboard in the SmartThings Mobile app. Adjust colors by going to the Thing detail screen for your FLEXiHue lights (tap the gear on FLEXiHue tiles).  Please update your Hue Bridge first, outside of the SmartThings app, using the Philips Hue app.",
	category: "SmartThings Labs",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png"
)

preferences {
	page(name:"mainPage", title:"Hue Device Setup", content:"mainPage", refreshTimeout:5)
	page(name:"bridgeDiscovery", title:"Hue Bridge Discovery", content:"bridgeDiscovery", refreshTimeout:5)
	page(name:"bridgeBtnPush", title:"Linking with your FHue", content:"bridgeLinking", refreshTimeout:5)
	page(name:"bulbDiscovery", title:"FLEXiHue Device Setup", content:"bulbDiscovery", refreshTimeout:5)
}

def mainPage() {
	if(canInstallLabs()) {
		def bridges = bridgesDiscovered()
		if (state.username && bridges) {
			return bulbDiscovery()
		} else {
			return bridgeDiscovery()
		}
	} else {
		def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

		return dynamicPage(name:"bridgeDiscovery", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
			section("Upgrade") {
				paragraph "$upgradeNeeded"
			}
		}
	}
}

def bridgeDiscovery(params=[:])
{
	def bridges = bridgesDiscovered()
	int bridgeRefreshCount = !state.bridgeRefreshCount ? 0 : state.bridgeRefreshCount as int
	state.bridgeRefreshCount = bridgeRefreshCount + 1
	def refreshInterval = 3

	def options = bridges ?: []
	def numFound = options.size() ?: 0

	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	//bridge discovery request every 15 //25 seconds
	if((bridgeRefreshCount % 5) == 0) {
		discoverBridges()
	}

	//setup.xml request every 3 seconds except on discoveries
	if(((bridgeRefreshCount % 1) == 0) && ((bridgeRefreshCount % 5) != 0)) {
		verifyHueBridges()
	}

	return dynamicPage(name:"bridgeDiscovery", title:"Discovery Started!", nextPage:"bridgeBtnPush", refreshInterval:refreshInterval, uninstall: true) {
		section("Please wait while we discover your Hue Bridge. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedHue", "enum", required:false, title:"Select Hue Bridge (${numFound} found)", multiple:false, options:options
		}
	}
}

def bridgeLinking()
{
	int linkRefreshcount = !state.linkRefreshcount ? 0 : state.linkRefreshcount as int
	state.linkRefreshcount = linkRefreshcount + 1
	def refreshInterval = 3

	def nextPage = ""
	def title = "Linking with your Hue"
	def paragraphText = "Press the button on your Hue Bridge to setup a link."
	if (state.username) { //if discovery worked
		nextPage = "bulbDiscovery"
		title = "Success! - click 'Next'"
		paragraphText = "Linking to your hub was a success! Please click 'Next'!"
	}

	if((linkRefreshcount % 2) == 0 && !state.username) {
		sendDeveloperReq()
	}

	return dynamicPage(name:"bridgeBtnPush", title:title, nextPage:nextPage, refreshInterval:refreshInterval) {
		section("Button Press") {
			paragraph """${paragraphText}"""
		}
	}
}

def bulbDiscovery()
{
	int bulbRefreshCount = !state.bulbRefreshCount ? 0 : state.bulbRefreshCount as int
	state.bulbRefreshCount = bulbRefreshCount + 1
	def refreshInterval = 3

	def options = bulbsDiscovered() ?: []
	def numFound = options.size() ?: 0

	if((bulbRefreshCount % 3) == 0) {
		discoverHueBulbs()
        log.trace "FLEXiHue SM: END DISCOVERY"
	}

	return dynamicPage(name:"bulbDiscovery", title:"Bulb Discovery Started!", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) {
		section("Please wait while we discover your Hue Bulbs. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedBulbs", "enum", required:false, title:"Select Hue Bulbs (${numFound} found)", multiple:true, options:options
		}
		section {
			def title = bridgeDni ? "Hue bridge (${bridgeHostname})" : "Find bridges"
			href "bridgeDiscovery", title: title, description: "", state: selectedHue ? "complete" : "incomplete", params: [override: true]

		}
	}
}

private discoverBridges() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:basic:1", physicalgraph.device.Protocol.LAN))
}

private sendDeveloperReq() {
	def token = app.id
	sendHubCommand(new physicalgraph.device.HubAction([
		method: "POST",
		path: "/api",
		headers: [
			HOST: bridgeHostnameAndPort
		],
		body: [devicetype: "$token-0", username: "$token-0"]], bridgeDni))
}

private discoverHueBulbs() {
	sendHubCommand(new physicalgraph.device.HubAction([
		method: "GET",
		path: "/api/${state.username}/lights",
		headers: [
			HOST: bridgeHostnameAndPort
		]], bridgeDni))
}

private verifyHueBridge(String deviceNetworkId) {
	log.trace "FLEXiHue SM: verifyHueBridge($deviceNetworkId)"
	sendHubCommand(new physicalgraph.device.HubAction([
		method: "GET",
		path: "/description.xml",
		headers: [
			HOST: ipAddressFromDni(deviceNetworkId)
		]], deviceNetworkId))
}

private verifyHueBridges() {
	def devices = getHueBridges().findAll { it?.value?.verified != true }
	log.trace "FLEXiHue SM: UNVERIFIED BRIDGES!: $devices"
	devices.each {
		verifyHueBridge((it?.value?.ip + ":" + it?.value?.port))
	}
}

Map bridgesDiscovered() {
	def vbridges = getVerifiedHueBridges()
	def map = [:]
	vbridges.each {
		def value = "${it.value.name}"
		def key = it.value.ip + ":" + it.value.port
		map["${key}"] = value
	}
	map
}

Map bulbsDiscovered() {
	def bulbs =  getHueBulbs()
	def map = [:]
	if (bulbs instanceof java.util.Map) {
		bulbs.each {
			def value = "${it?.value?.name}"
			def key = app.id +"/"+ it?.value?.id
			map["${key}"] = value
		}
	} else { //backwards compatable
		bulbs.each {
			def value = "${it?.name}"
			def key = app.id +"/"+ it?.id
			map["${key}"] = value
		}
	}
	map
}


def getHueBulbs() {
	log.trace state.bulbs
	log.trace "FLEXiHue SM: FLEXiHUE BULBS:"
	state.bulbs = state.bulbs ?: [:]
}


def getHueBridges() {
	state.bridges = state.bridges ?: [:]
}

def getVerifiedHueBridges() {
	getHueBridges().findAll{ it?.value?.verified == true }
}

def installed() {
	log.trace "FLEXiHue SM: Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.trace "FLEXiHue SM: Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	// remove location subscription aftwards
	log.debug "FLEXiHue SM: INITIALIZE"
	state.subscribe = false
	state.bridgeSelectedOverride = false
	def bridge = null

	if (selectedHue) {
		addBridge()
        bridge = getChildDevice(selectedHue)
	}
    
	if (selectedBulbs) {
		addBulbs()
	}

	if (bridge) {
		subscribe(bridge, "bulbList", bulbListHandler)
        runEvery15Minutes("doDeviceSync")
	}   
}

def uninstalled() {

	unschedule()
    
}

// Handles events to add new bulbs
def bulbListHandler(evt) {
	def bulbs = [:]
	log.trace "FLEXiHue SM: Adding bulbs to state..."
	state.bridgeProcessedLightList = true
	evt.jsonData.each { k,v ->
		log.trace "FLEXiHue SM:  $k: $v"
		if (v instanceof Map) {
			bulbs[k] = [id: k, name: v.name, type: v.type, hub:evt.value]
		}
	}
	state.bulbs = bulbs
	log.trace "FLEXiHue SM:  ${bulbs.size()} bulbs found"
}


def addBulbs() {
	def bulbs = getHueBulbs()
	selectedBulbs.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newHueBulb
			if (bulbs instanceof java.util.Map) {
				newHueBulb = bulbs.find { (app.id + "/" + it.value.id) == dni }
				if (newHueBulb?.value?.type?.equalsIgnoreCase("Dimmable light")) {
					d = addChildDevice("info_fiend", "FLEXiHue Lux Bulb", dni, newHueBulb?.value.hub, ["label":newHueBulb?.value.name])
				} else {
					d = addChildDevice("info_fiend", "FLEXiHue Bulb", dni, newHueBulb?.value.hub, ["label":newHueBulb?.value.name])
				}
			} else { 
            	//backwards compatable
				newHueBulb = bulbs.find { (app.id + "/" + it.id) == dni }
				d = addChildDevice("info_fiend", "FLEXiHue Bulb", dni, newHueBulb?.hub, ["label":newHueBulb?.name])
			}

			log.trace "FLEXiHue SM: created ${d.displayName} with id $dni"
			d.refresh()
		} else {
			log.trace "FLEXiHue SM: found ${d.displayName} with id $dni already exists, type: '$d.typeName'"
			if (bulbs instanceof java.util.Map) {
            	def newHueBulb = bulbs.find { (app.id + "/" + it.value.id) == dni }
				if (newHueBulb?.value?.type?.equalsIgnoreCase("Dimmable light") && d.typeName == "Hue Bulb") {
					d.setDeviceType("FLEXiHue Lux Bulb")
				}
			}
		}
	}
}

def addBridge() {
	def vbridges = getVerifiedHueBridges()
	def vbridge = vbridges.find {(it.value.ip + ":" + it.value.port) == selectedHue}

	if(vbridge) {
		def d = getChildDevice(selectedHue)
		if(!d) {
			d = addChildDevice("info_fiend", "FLEXiHue Bridge", selectedHue, vbridge.value.hub, ["data":["mac": vbridge.value.mac]]) // ["preferences":["ip": vbridge.value.ip, "port":vbridge.value.port, "path":vbridge.value.ssdpPath, "term":vbridge.value.ssdpTerm]]

			log.trace "FLEXiHue SM: created ${d.displayName} with id ${d.deviceNetworkId}"

			sendEvent(d.deviceNetworkId, [name: "networkAddress", value: convertHexToIP(vbridge.value.ip) + ":" +  convertHexToInt(vbridge.value.port)])
			sendEvent(d.deviceNetworkId, [name: "serialNumber", value: vbridge.value.serialNumber])
		}
		else
		{
			log.trace "FLEXiHue SM: found ${d.displayName} with id $selectedHue already exists"
		}
	}
}


def locationHandler(evt) {
	log.trace "FLEXiHue SM: LOCATION HANDLER: $evt.description"
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]

	if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:basic:1"))
	{ //SSDP DISCOVERY EVENTS
		log.trace "FLEXiHue SM: SSDP DISCOVERY EVENTS"
		def bridges = getHueBridges()

		if (!(bridges."${parsedEvent.ssdpUSN.toString()}"))
		{ //bridge does not exist
			log.trace "FLEXiHue SM: Adding bridge ${parsedEvent.ssdpUSN}"
			bridges << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
		}
		else
		{ // update the values

			log.trace "FLEXiHue SM: Device was already found in state..."

			def d = bridges."${parsedEvent.ssdpUSN.toString()}"
			def host = parsedEvent.ip + ":" + parsedEvent.port
			if(d.ip != parsedEvent.ip || d.port != parsedEvent.port || host != state.hostname) {

				log.trace "FLEXiHue SM: Device's port or ip changed..."
				state.hostname = host
				d.ip = parsedEvent.ip
				d.port = parsedEvent.port
				d.name = "Philips hue ($bridgeHostname)"

				app.updateSetting("selectedHue", host)

				childDevices.each {
					if (it.getDeviceDataByName("mac") == parsedEvent.mac) {
						log.trace "updating dni for device ${it} with mac ${parsedEvent.mac}"
						it.setDeviceNetworkId((parsedEvent.ip + ":" + parsedEvent.port)) //could error if device with same dni already exists
					}
				}
			}
		}
	}
	else if (parsedEvent.headers && parsedEvent.body)
	{ // HUE BRIDGE RESPONSES
		log.trace "FLEXiHue SM: HUE BRIDGE RESPONSES"
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		def type = (headerString =~ /Content-type:.*/) ? (headerString =~ /Content-type:.*/)[0] : null
		def body

		if (type?.contains("xml"))
		{ // description.xml response (application/xml)
			body = new XmlSlurper().parseText(bodyString)

			if (body?.device?.modelName?.text().startsWith("Philips hue bridge"))
			{
				def bridges = getHueBridges()
				def bridge = bridges.find {it?.key?.contains(body?.device?.UDN?.text())}
				if (bridge)
				{
					bridge.value << [name:body?.device?.friendlyName?.text(), serialNumber:body?.device?.serialNumber?.text(), verified: true]
				}
				else
				{
					log.error "FLEXiHue SM: /description.xml returned a bridge that didn't exist"
				}
			}
		}
		else if(type?.contains("json"))
		{ //(application/json)
			body = new groovy.json.JsonSlurper().parseText(bodyString)

			if (body?.success != null)
			{ //POST /api response (application/json)
				if (body?.success?.username)
				{
					state.username = body.success.username[0]
					state.hostname = selectedHue
				}
			}
			else if (body.error != null)
			{
				//TODO: handle retries...
				log.error "FLEXiHue SM: ERROR: application/json ${body.error}"
			}
			else
			{ //GET /api/${state.username}/lights response (application/json)
				if (!body?.state?.on) { //check if first time poll made it here by mistake
					def bulbs = getHueBulbs()
					log.trace "FLEXiHue SM: Adding bulbs to state!"
					body.each { k,v ->
						bulbs[k] = [id: k, name: v.name, type: v.type, hub:parsedEvent.hub]
					}
				}
			}
		}
	}
	else {
		log.trace "FLEXiHue SM: NON-HUE EVENT $evt.description"
	}
}

private def parseEventMessage(Map event) {
	//handles bridge attribute events
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

def doDeviceSync(){
	log.trace "FLEXiHue SM: Doing FLEXiHue Device Sync!"

	//shrink the large bulb lists
	convertBulbListToMap()

	poll()

	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	discoverBridges()
}


/////////////////////////////////////
//CHILD DEVICE METHODS
/////////////////////////////////////

def parse(childDevice, description) {
	def parsedEvent = parseEventMessage(description)

	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		log.trace "parse() - ${bodyString}"
		def body = new groovy.json.JsonSlurper().parseText(bodyString)
		log.trace "FLEXiHue SM: BODY - $body"
		if (body instanceof java.util.HashMap)
		{ //poll response
			def bulbs = getChildDevices()
            //for each bulb

             for (bulb in body) {
                def d = bulbs.find{it.deviceNetworkId == "${app.id}/${bulb.key}"}
                 if (d) {
	               	log.trace "FLEXiHue SM: Reading Poll for Bulbs"
		            if (bulb.value.state.reachable) {
                    	def levelCheck = d.currentValue("level")
                        def switchCheck = d.currentValue("switch")
                        if (levelCheck == 0 && switchCheck == "on") {
                        	log.trace "FLEXiHue SM: Not Changing Switch to off because ${d.displayName} is on & has level 0."
                        } else {
                        	log.trace "FLEXiHue SM: Changing Switch & Level values."
		            		sendEvent(d.deviceNetworkId, [name: "switch", value: bulb.value?.state?.on ? "on" : "off"])
		            		sendEvent(d.deviceNetworkId, [name: "level", value: Math.round(bulb.value.state.bri * 100 / 255)])
                        }    
/**						if (bulb.value.state.sceneSwitch != null) {
                        	log.debug "FLEXiHue SM: updating b/c bulb.value.state.sceneSwitch != null."
                            log.debug "bulb.value.state.offTime is ${bulb.value.state.offTime}, bulb.value.state.sceneSwitch is ${bulb.value.state.sceneSwitch}, and bulb.value.state.sceneName is ${bulb.value.state.sceneName}."
	                        sendEvent(d.deviceNetworkId, [name: "offTime", value: bulb.value.state.offTime])
			            	sendEvent(d.deviceNetworkId, [name: "sceneSwitch", value: bulb.value.state.sceneSwitch])                                                      
			            	sendEvent(d.deviceNetworkId, [name: "sceneName", value: bulb.value.state.sceneName])   
            			} else {
	                        log.debug "FLEXiHue SM: replacing b/c bulb.value.state.sceneSwitch == null."
	                        sendEvent(d.deviceNetworkId, [name: "offTime", value: 15])
			            	sendEvent(d.deviceNetworkId, [name: "sceneSwitch", value: "current"])                                                      
			            	sendEvent(d.deviceNetworkId, [name: "sceneName", value: "-none-"])   	
                            
                        }
                        if (bulb.value.state.sat) {
							def hue = Math.min(Math.round(bulb.value.state.hue * 100 / 65535), 65535) as int
							def sat = Math.round(bulb.value.state.sat * 100 / 255) as int
							def hex = colorUtil.hslToHex(hue, sat)
							sendEvent(d.deviceNetworkId, [name: "color", value: hex])    
						}
**/                        
					} else {
               
						sendEvent(d.deviceNetworkId, [name: "switch", value: "off"])
/**			            sendEvent(d.deviceNetworkId, [name: "level", value: 100])
						sendEvent(d.deviceNetworkId, [name: "offTime", value: 15])
		            	sendEvent(d.deviceNetworkId, [name: "sceneSwitch", value: "current"])   
		            	sendEvent(d.deviceNetworkId, [name: "sceneName", value: "-none-"])   
**/                        
			            if (bulb.value.state.sat) {
		    	        	def hue = 23
		        	    	def sat = 56
		            		def hex = colorUtil.hslToHex(23, 56)
		            		sendEvent(d.deviceNetworkId, [name: "color", value: hex])  
		            	}    
					}
				}
			}
   		}     
		else
		{ //put response
			def hsl = [:]
			body.each { payload ->
				log.debug $payload
				if (payload?.success)
				{

					def childDeviceNetworkId = app.id + "/"
					def eventType
					body?.success[0].each { k,v ->
						log.trace "********************************************************"
						log.debug "********************************************************"

						childDeviceNetworkId += k.split("/")[2]
						if (!hsl[childDeviceNetworkId]) hsl[childDeviceNetworkId] = [:]
						eventType = k.split("/")[4]
						log.trace "FLEXiHue SM: eventType: $eventType"
						switch(eventType) {
							case "on":
								sendEvent(childDeviceNetworkId, [name: "switch", value: (v == true) ? "on" : "off"])
								break
							case "bri":
								sendEvent(childDeviceNetworkId, [name: "level", value: Math.round(v * 100 / 255)])
								break
							case "sat":
								hsl[childDeviceNetworkId].saturation = Math.round(v * 100 / 255) as int
								break
							case "hue":
								hsl[childDeviceNetworkId].hue = Math.min(Math.round(v * 100 / 65535), 65535) as int
								break
						}
					}

				}
				else if (payload.error)
				{
					log.error "FLEXiHue SM: JSON error - ${body?.error}"
				}

			}

			hsl.each { childDeviceNetworkId, hueSat ->
				if (hueSat.hue && hueSat.saturation) {
					def hex = colorUtil.hslToHex(hueSat.hue, hueSat.saturation)
					log.trace "FLEXiHue SM: sending ${hueSat} for ${childDeviceNetworkId} as ${hex}"
					sendEvent(hsl.childDeviceNetworkId, [name: "color", value: hex])
				}
			}

		}
	} else {
		log.trace "FLEXiHue SM: parse - got something other than headers,body..."
		return []
	}
}

def on(childDevice, transitiontime, percent) {
	log.trace "FLEXiHue SM: executing 'On' with percent ${percent}."
	def level = Math.min(Math.round(percent * 255 / 100), 255)
	def value = [on: true, bri: level]
    value.transitiontime = transitiontime * 10
	log.trace "FLEXiHue SM: Executing 'on'"
	put("lights/${getId(childDevice)}/state", value)
}

def off(childDevice, transitiontime) {
	def value = [on: false]
    value.transitiontime = transitiontime * 10
	log.trace "FLEXiHue SM: Executing 'off'"
	put("lights/${getId(childDevice)}/state", value)
}

def setLevel(childDevice, Number percent, Number transitiontime) {
	log.trace "FLEXiHue SM: Executing 'setLevel'"
    log.trace "incoming level is ${percent}."
	def level = Math.min(Math.round(percent * 255 / 100), 255)
	def value = [bri: level, on: percent > 0, transitiontime: transitiontime * 10]					// on: percent > 0
	put("lights/${getId(childDevice)}/state", value)
}

def setSaturation(childDevice, Number percent, Number transitiontime) {
	log.trace "FLEXiHue SM: Executing 'setSaturation($percent)'"
	def level = Math.min(Math.round(percent * 255 / 100), 255)
	put("lights/${getId(childDevice)}/state", [sat: level, transitiontime: transitiontime * 10])
}


def setHue(childDevice, percent, transitiontime) {
	log.trace "FLEXiHue SM: Executing 'setHue($percent)'"
	def level =	Math.min(Math.round(percent * 65535 / 100), 65535)
	put("lights/${getId(childDevice)}/state", [hue: level, transitiontime: transitiontime * 10])
}

def setColor(childDevice, color) {
	log.trace "FLEXiHue SM: Executing 'setColor($color)'"
	def hue =	Math.min(Math.round(color.hue * 65535 / 100), 65535)
	def sat = Math.min(Math.round(color.saturation * 255 / 100), 255)

	def value = [sat: sat, hue: hue]
	if (color.level != null) {
		value.bri = Math.min(Math.round(color.level * 255 / 100), 255)
		value.on = value.bri > 0
	}																						// value.on = value.bri > 0

	if (color.transitiontime != null){
		value.transitiontime = color.transitiontime * 10
	}

	if (color.switch) {
		value.on = color.switch == "on"
	}

	log.trace "FLEXiHue SM: sending put command $value"
	put("lights/${getId(childDevice)}/state", value)
}

def nextLevel(childDevice) {
	def level = device.latestValue("level") as Integer ?: 0
	if (level < 100) {
		level = Math.min(25 * (Math.round(level / 25) + 1), 100) as Integer
	}
	else {
		level = 25
	}
	setLevel(childDevice,level)
}

private getId(childDevice) {
	if (childDevice.device?.deviceNetworkId?.startsWith("HUE")) {
		log.trace childDevice.device?.deviceNetworkId[3..-1]
		return childDevice.device?.deviceNetworkId[3..-1]
	}
	else {
		return childDevice.device?.deviceNetworkId.split("/")[-1]
	}
}

private poll() {
	def uri = "/api/${state.username}/lights/"
	log.trace "GET:  $uri"
	sendHubCommand(new physicalgraph.device.HubAction("""GET ${uri} HTTP/1.1
HOST: ${selectedHue}

""", physicalgraph.device.Protocol.LAN, "${selectedHue}"))
}

private put(path, body) {
	def uri = "/api/${state.username}/$path"
	def bodyJSON = new groovy.json.JsonBuilder(body).toString()
	def length = bodyJSON.getBytes().size().toString()

	log.debug "FLEXiHue SM: PUT:  $uri"
	log.debug "FLEXiHue SM: BODY: ${bodyJSON}"

	sendHubCommand(new physicalgraph.device.HubAction("""PUT $uri HTTP/1.1
HOST: ${selectedHue}
Content-Length: ${length}

${bodyJSON}
""", physicalgraph.device.Protocol.LAN, "${selectedHue}"))

}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
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

def convertBulbListToMap() {
	try {
		if (state.bulbs instanceof java.util.List) {
			def map = [:]
			state.bulbs.unique {it.id}.each { bulb ->
				map << ["${bulb.id}":["id":bulb.id, "name":bulb.name, "hub":bulb.hub]]
			}
			state.bulbs = map
		}
	}
	catch(Exception e) {
		log.error "FLEXiHue SM: Caught error attempting to convert bulb list to map: $e"
	}
}


def ipAddressFromDni(dni) {
	if (dni) {
		def segs = dni.split(":")
		convertHexToIP(segs[0]) + ":" +  convertHexToInt(segs[1])
	} else {
		null
	}
}

def getBridgeDni() {
	state.hostname
}

def getBridgeHostname() {
	def dni = state.hostname
	if (dni) {
		def segs = dni.split(":")
		convertHexToIP(segs[0])
	} else {
		null
	}
}

def getBridgeHostnameAndPort() {
	def result = null
	def dni = state.hostname
	if (dni) {
		def segs = dni.split(":")
		result = convertHexToIP(segs[0]) + ":" +  convertHexToInt(segs[1])
	}
	log.trace "FLEXiHue SM: result = $result"
	result
}