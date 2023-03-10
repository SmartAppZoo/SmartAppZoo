/**
 *  Sprinkler Service Manager
 *
 *  Copyright 2016 Will Shelton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Sprinkler Service Manager",
    namespace: "ibwilbur",
    author: "Will Shelton",
    description: "Manages the discovery and installation of sprinkler controllers",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "discovery", title: "Controller discovery", content: "controllerDiscovery", refreshTimeout: 5)
}

Map controllersDiscovered() {
	def controllers = getVerifiedControllers()
	def map = [:]
	controllers.each {
		def value = "${it.value.name}"
		def key = it.value.ip + ":" + it.value.port + ":sprinkler"
		map["${key}"] = value
	}
	map
}

def controllerDiscovery() {
	int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
    state.refreshCount = refreshCount + 1
    def refreshInterval = 3
    def options = controllersDiscovered() ?: []
    def numFound = options.size() ?: 0;
    
    if (!state.subscribe) {
    	subscribe(location, null, locationHandler, [filterEvents: false])
        state.subscribe = true
    }
    
    if ((refreshCount % 8) == 0) {
    	discoverControllers()
    }
    
    if (((refreshCount % 1) == 0) && ((refreshCount % 8) != 0)) {
    	verifyControllers()
    }
    
    return dynamicPage(name: "discovery", title: "Discovery started!", nextPage: "", refreshInterval: refreshInterval, install: true, uninstall: true) {
    	section("Please wait while we discover your device.  Select your device below once discovery is complete") {
        	input "selectedControllers", "enum", required: false, title: "Select controllers (${numFound} found)", multiple: true, options: options
        }
    }
}

def installed() {
	log.trace "Installed with settings: ${settings}"
    initialize()
}

def updated() {
	log.trace "Updated with settings: ${settings}"
    unschedule()
    initialize()
}

def uninstalled() {
	def devices = getChildDevices()
	log.trace "Deleting ${devices.size()} devices"
	devices.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

def initialize() {
    unsubscribe()
    state.subscribe = false
    
    unschedule()
    runEvery3Hours(scheduledActionsHandler)
    
    if (selectedControllers) {
    	addControllers()
    }
}

def scheduledActionsHandler() {
	syncDevices()
}

def syncDevices() {
	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	discoverControllers()
}

def getControllers() {
	state.controllers = state.controllers ?: [:]
}

def getVerifiedControllers()
{
	getControllers().findAll{ it?.value?.verified == true }
}

def locationHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    def parsedEvent = parseEventMessage(description)
    parsedEvent << ["hub":hub]
    
    if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:IrrigationController:1")) {
    	// discovery events      
    	log.trace "Sprinkler controller found"       
        def controllers = getControllers()
        
        if (!(controllers."${parsedEvent.ssdpUSN.toString()}")) {
        
        	log.trace "Adding a new sprinkler controller"
        	controllers << ["${parsedEvent.ssdpUSN.toString()}": parsedEvent]
            
        } else {
        
        	log.trace "Sprinkler controller was previously discovered.  Checking for changes."
            def d = controllers."${parsedEvent.ssdpUSN.toString()}"

            boolean deviceChangedValues = false
            
            if (d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
            	d.ip = parsedEvent.ip
                d.port = parsedEvent.port
                deviceChangedValues = true
                log.trace "Device IP or Port changed"
            }
            
            if (deviceChangedValues) {
            	def children = getChildDevices()
                children.each {
                	if (it.getDeviceDataByName("mac") == parsedEvent.mac) {
                    	log.trace "Updating DNI for device {$it} with MAC ${parsedEvent.mac}"
                        it.setDeviceNetworkId(parsedEvent.ip + ":" + parsedEvent.port + ":sprinkler")
                    }
                }
            }
        }
    } else if (parsedEvent.headers && parsedEvent.body) {
    	// responses
        def headerString = new String(parsedEvent.headers.decodeBase64())
        def bodyString = new String(parsedEvent.body.decodeBase64())
        def type = (headerString =~ /Content-Type:.*/) ? (headerString =~ /Content-Type:.*/)[0] : null
        def body
       
		if (type?.contains("xml")) {
        	body = new XmlSlurper().parseText(bodyString)
            
            if (body?.device?.deviceType?.text().contains("IrrigationController")) {
            	def controllers = getControllers()
                def controller = controllers.find { it?.key?.contains(body?.device?.UDN?.text()) }
                if (controller) {
                	log.trace "Updating sprinkler controller with verified information"
                	controller.value << [
                    	name: body?.device?.friendlyName.text(), 
                        controlUrl: body?.device?.serviceList?.service?.controlURL?.text(),
                        serviceType: body?.device?.serviceList?.service?.serviceType?.text(),                        
                        verified: true
                        ]
					//log.trace "Updated info: " + controller
                } else {
                	log.error "/upnp/device/desc.xml returned a device that doesn't exist"
                }
            }
		}       
    }
}

def discoverControllers() {
	log.trace "Discovering sprinkler controllers"
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:IrrigationController:1", physicalgraph.device.Protocol.LAN))
}

def verifyControllers() {
	def devices = getControllers().findAll { it?.value?.verified != true }
	if(devices) {
		log.warn "UNVERIFIED CONTROLLER(S)!: $devices"
	}
	devices.each {
    	//log.debug "Sprinkler Controller: " + it
		verifyController(it?.value?.ip + ":" + it?.value?.port + ":sprinkler", it.value.ssdpPath)
	}
}

private verifyController(deviceNetworkId, path) {
    def temp = deviceNetworkId.tokenize(":")
	String ip = getHostAddress(temp[0] + ":" + temp[1])
    ///upnp/device/desc.xml?udn=6bd5eabd-b7c8-4f7b-ae6c-a30ccdeb5988
	sendHubCommand(new physicalgraph.device.HubAction("""GET ${path} HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${ip}"))
}

def addControllers() {
	def controllers = getVerifiedControllers()
	def runSubscribe = false
	selectedControllers.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newPlayer = controllers.find { (it.value.ip + ":" + it.value.port + ":sprinkler") == dni }
            if (newPlayer) {
                d = addChildDevice("ibwilbur", "Sprinkler Controller", dni, newPlayer?.value.hub, [label:"${newPlayer?.value.name}"])
                
                //log.debug "Control Url: " + newPlayer?.value.controlUrl
                //log.debug "Service Type: " + newPlayer?.value.serviceType
                //log.debug "Target Host: " + convertHexToIP(newPlayer?.value.ip)
                
                d.setRuntimeParams(newPlayer?.value.controlUrl, newPlayer?.value.serviceType, convertHexToIP(newPlayer?.value.ip))
                log.trace "Created ${d.displayName} with id $dni"
                runSubscribe = true

            } else {
            	log.debug "Unable to find new controller"
            }
		} else {
			log.trace "${d.displayName} with ID $dni already exists"
		}
	}
}

def parseEventMessage(String description) {
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