definition(
  name: "RingFloodlightServer Connector",
  namespace: "GvnCampbell",
  author: "Gavin Campbell",
  description: "Manage Ring Floodlight devices through a local server.",
  category: "SmartThings Labs",
  iconUrl: "https://cdn.rawgit.com/GvnCampbell/SmartThings-RingFloodlightServer/master/smartapps/gvncampbell/ringfloodlightserver-connector.src/ring.png",
  iconX2Url: "https://cdn.rawgit.com/GvnCampbell/SmartThings-RingFloodlightServer/master/smartapps/gvncampbell/ringfloodlightserver-connector.src/ring2x.png",
  iconX3Url: "https://cdn.rawgit.com/GvnCampbell/SmartThings-RingFloodlightServer/master/smartapps/gvncampbell/ringfloodlightserver-connector.src/ring2x.png")
    
preferences {
  page(name: "config")
}

def config() {
	dynamicPage(name: "config", title: "RingFloodlightServer Local Server Settings", install: true, uninstall: true) {

		section("Please enter the details of the running copy of the local RingFloodlightServer server you want to connect to") {
		  input(name: "ip", type: "text", title: "IP", description: "RingFloodlightServer Server IP", required: true, submitOnChange: false)
		  input(name: "port", type: "text", title: "Port", description: "RingFloodlightServer Server Port", required: true, submitOnChange: true)
		  input(name: "username", type: "text", title: "Username", description: "Ring username", required: true, submitOnChange: true)
		  input(name: "password", type: "text", title: "Password", description: "Ring password", required: true, submitOnChange: true)
		}

		doDeviceSync()

		def options = getDevices().collect { s ->
			s.description
		}
		options.removeAll([null])

		def numFound = options.size() ?: 0

		if (ip && port && username && password) {
			section("Select your devices below.") {
		  		input name: "selectedDevices", type: "enum", required:false, title:"Select Devices (${numFound} found)", multiple:true, options:options
			}
		}

  	}
}

def doDeviceSync(){
	def logprefix = "[doDeviceSync] "
  	logger logprefix + "Starting..."

  	if(!state.subscribe) {
  		logger logprefix + "Subscribing."
    	subscribe(location, null, locationHandler, [filterEvents:false])
    	state.subscribe = true
  	}
        
	poll()
}

def getDevices() {
  def logprefix = "[getDevices] "
  logger logprefix + "Started."
  logger logprefix + "Return: " + state.devices ?: [:]
  state.devices ?: [:]
}

def installed() {
  def logprefix = "[installed] "
  logger logprefix + "Started."
  initialize()
}

def updated() {
  def logprefix = "[updated] "
  logger logprefix + "Started."
  initialize()
}

def initialize() {
	def logprefix = "[initialize] "
	logger logprefix + "Started."

	state.subscribe = false
	unsubscribe()

    if (state.devices == null) {
    	state.devices = []
    }

	if (ip && port) {
		doDeviceSync()
	}

	if (selectedDevices) {
    	addDevices()
    }
}

def uninstalled() {
  def logprefix = "[uninstalled] "
  logger logprefix + "Started."
  unschedule()
}

def locationHandler(evt) {
	def logprefix = "[locationHandler] "
  	logger logprefix + "Starting."

  	def description = evt.description
  	def hub = evt?.hubId

  	def parsedEvent = parseLanMessage(description)
  	parsedEvent << ["hub":hub]

  	if (parsedEvent.headers && parsedEvent.body && parsedEvent?.data?.service == 'ringfloodlightserver') {
    	def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
    	if (body instanceof java.util.HashMap) {
			logger logprefix + "Body is a Map."

			body.devices.each {
            	logger logprefix + "it:" + it
				def dni = app.id + "/" + it.id
				def d = getChildDevice(dni)
				if (d) {
        			logger logprefix + "Child device found."
                    sendEvent(d.deviceNetworkId, [name: "ping", value: "ok"])
					if (it.led_status == "on") {
            			logger logprefix + "Switch on."
						sendEvent(d.deviceNetworkId, [name: "light", value: "on"])
					} else if (it.led_status == "off") {
            			logger logprefix + "Switch off."
						sendEvent(d.deviceNetworkId, [name: "light", value: "off"])
					}
                    if (it.motion == "on") {
            			logger logprefix + "Motion on."
						sendEvent(d.deviceNetworkId, [name: "motion", value: "active"])
                    } else if (it.motion == "off") {
            			logger logprefix + "Motion off."
						sendEvent(d.deviceNetworkId, [name: "motion", value: "inactive"])
                    }
                    if (it.siren_status.seconds_remaining > 0) {
                    	logger logprefix + "Siren on."
                        sendEvent(d.deviceNetworkId, [name: "alarm", value: "siren"])
                    } else if (it.siren_status.seconds_remaining == 0) {
                    	logger logprefix + "Siren off."
                        sendEvent(d.deviceNetworkId, [name: "alarm", value: "off"])                    
                    }
        		} else {
        			logger logprefix + "Child device not found. Will add if doesn't exist in list."
                    def foundDevice = false
                    state.devices.each { existingDevice -> 
                    	if (existingDevice.id == it.id) { // device already in device list
                        	foundDevice = true                     
                        }                        
                    }
                    if (foundDevice == false) {
                    	logger logprefix + "    New device added to device list."
                    	state.devices.add(it)
    	    			state.devices.unique()
                    } else {
                    	logger logprefix + "    New device already in device list."
					}
				}
        	}
    	}
  	}
}



private def parseEventMessage(Map event) {
  def logprefix = "[parseEventMessage Map] "
  logger logprefix + "Started."
  return event
}

def addDevices() {
	def logprefix = "[addDevices] "
	logger logprefix + "Started."

	def devices = getDevices()
	logger logprefix + "devices: " + devices

	state.devices.each {
     	def dni = app.id + "/" + it.id
      	def d = getChildDevice(dni)
      	if(!d) {
        	d = addChildDevice("GvnCampbell", "Ring Floodlight", dni, null, ["label": "${it.description}"])
        	logger logprefix + "Created ${d.displayName} with id $dni."
        	d.refresh()
      	} else {
        	logger logprefix + "Found ${d.displayName} with id $dni already exists, type: '$d.typeName'."
      	}
    }
}

def on(childDevice) {
  	def logprefix = "[on] "
  	logger logprefix + "Started..."
	def un = java.net.URLEncoder.encode(username, "UTF-8")
    def pw = java.net.URLEncoder.encode(password, "UTF-8")
    def id = getId(childDevice)
	put("", "", "", "?u=${un}&p=${pw}&q=lights&id=${id}&state=on")
}

def off(childDevice) {
  	def logprefix = "[off] "
  	logger logprefix + "Started..."
	def un = java.net.URLEncoder.encode(username, "UTF-8")
    def pw = java.net.URLEncoder.encode(password, "UTF-8")
    def id = getId(childDevice)
	put("", "", "", "?u=${un}&p=${pw}&q=lights&id=${id}&state=off")
}
def sirenOn(childDevice) {
  	def logprefix = "[sirenOn] "
  	logger logprefix + "Started."
	def un = java.net.URLEncoder.encode(username, "UTF-8")
    def pw = java.net.URLEncoder.encode(password, "UTF-8")
    def id = getId(childDevice)
	put("", "", "", "?u=${un}&p=${pw}&q=siren&id=${id}&state=on")
}

def sirenOff(childDevice) {
  	def logprefix = "[sirenOff] "
  	logger logprefix + "Started."
	def un = java.net.URLEncoder.encode(username, "UTF-8")
    def pw = java.net.URLEncoder.encode(password, "UTF-8")
    def id = getId(childDevice)
	put("", "", "", "?u=${un}&p=${pw}&q=siren&id=${id}&state=off")
}
private getId(childDevice) {
  def logprefix = "[getId] "
  logger logprefix + "Started."
  return childDevice?.device?.deviceNetworkId.split("/")[-1]
}

private poll() {
	def logprefix = "[poll] "
	logger logprefix + "Started."

    def un = java.net.URLEncoder.encode(username, "UTF-8")
    def pw = java.net.URLEncoder.encode(password, "UTF-8")
	put("", "", "", "?u=${un}&p=${pw}")
    
    deviceMonitor()
}


//************************************************************************************
//
//  Hanndle HTTP Communication
//
//************************************************************************************

private put(path, text, dni, q = "") {
  def logprefix = "[put] "
  logger logprefix + "Started."

  def hubaction = new physicalgraph.device.HubAction([
        method: "PUT",
        path: path + "/" + q,
        body: text,
        headers: [ HOST: "$ip:$port", "Content-Type": "application/json" ]]
    )
    logger logprefix + "hubaction: " + hubaction
    sendHubCommand(hubaction)
}

private logger(text) {
	def loggingToggle = true
    if (loggingToggle) {
		log.debug text
	}
}

private deviceMonitor() {
	try { unschedule("doDeviceSync") } catch (e) { }    
    runIn(10, doDeviceSync)
}