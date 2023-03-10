/**
 *  Copyright 2017 Kenny K
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
		name: "Simple LAN Device Manager",
		namespace: "r3dey3",
		author: "Kenny K",
		description: "Service Manager SmartApp for devices on the local LAN",
		category: "",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        singleInstance: true
)

/*****************************************************************************/
preferences {
	page(name: "deviceDiscovery", title: "Device Setup", content: "deviceDiscovery")
}

/* Configuration page for selecting devices */
def deviceDiscovery() {
	//state.devices = [:]
	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
		def value = it.value.name ?: "Device ${it.value.ssdpUSN}"
		options[it.key] = value
	}

	ssdpSubscribe()
	ssdpDiscover()
	verifyDevices()

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 30, install: true, uninstall: true) {
		section("Please wait while we discover your Devices. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

/*****************************************************************************/
/* Standard install/update/uninstall handlers */
def installed() {
	initialize()
}
def updated() {
	unsubscribe()
	initialize()
}
def uninstalled() {
   state.devices = [:]
}

def initialize() {
	unsubscribe()
	unschedule()
	ssdpSubscribe()

	if (selectedDevices) {
		addDevices()
	}

    subscribe(location, null, responseHandler, [filterEvents:false])

	runEvery5Minutes("ssdpDiscover")
}

//Send discovery command
void ssdpDiscover() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:st-wifi-dev", physicalgraph.device.Protocol.LAN))
}
// Subscribe to discovery responses
void ssdpSubscribe() {
	subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:st-wifi-dev", ssdpHandler)
}

// Get full device information and confirm we can communicate
void verifyDevices() {
//	def devices = getDevices().findAll { it?.value?.verified != true }
	def devices = getDevices()
	devices.each {
    	// we don't use getDeviceAddress here because value is not a child device
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

/******************************************************************************
 * Helper methods for device tracking
 */
def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

//Add newly selected devices
def addDevices() {
	def devices = getDevices()

	selectedDevices.each { dni ->
		def selectedDevice = devices.find{ it.key == dni }
        selectedDevice = selectedDevice?.value
		if (selectedDevice) {
			def d = getChildDevices()?.find {
				it.deviceNetworkId == dni
			}
            if (!d) {
                log.debug "Creating ${selectedDevice.modelName} with dni: ${dni}"
                def dev = addChildDevice(selectedDevice.manufacturer, selectedDevice.modelName, "${dni}", selectedDevice?.hub, [
                    "label": selectedDevice?.name ?: "New SmartThings LAN Device",
                    "data": [
                        "mac": selectedDevice.mac,
                        "ip": selectedDevice.networkAddress,
                        "port": selectedDevice.deviceAddress,
                        "ssdpPath": selectedDevice.ssdpPath
                    ]
                ])
                dev.refresh()
            }
        }
	}
}
/*******************************************************************************
 * Device search handling
 * Handle responses to the ssdp query
 */
def ssdpHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]
	def devices = getDevices()
	String ssdpUSN = parsedEvent.ssdpUSN.toString()
	if (devices."${ssdpUSN}") {
		def d = devices."${ssdpUSN}"
		if (d.mac != parsedEvent.mac || d.networkAddress != parsedEvent.networkAddress ||
        		d.deviceAddress != parsedEvent.deviceAddress || d.ssdpPath != parsedEvent.ssdpPath) {
            def dni = ssdpUSN
            def child = getChildDevice(dni)
			devices."${ssdpUSN}" = parsedEvent
            if (child) {
            	child.updateDataValue("ip", parsedEvent.networkAddress)
                child.updateDataValue("port", parsedEvent.deviceAddress)
                child.updateDataValue("mac", parsedEvent.mac)
                child.updateDataValue("ssdpPath", parsedEvent.ssdpPath)
			}
            else {
            	devices."${ssdpUSN}".verified = false
			}
		}
	} else {
        log.trace "newDevice ${parsedEvent}"
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

/* Handle responses from the ssdp description query (xml or json) */
void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
    def devices = getDevices()
    def body = null
    if (hubResponse.json != null) {
    	body = hubResponse.json
    }
    else if (hubResponse.xml != null) {
		def xml = hubResponse.xml
		body = [
			UDN: xml?.device?.UDN?.text(),
			friendlyName: xml?.device?.friendlyName?.text(),
			modelName: xml?.device?.modelName?.text(),
            serialNumber: xml?.device?.serialNumber?.text(),
            deviceType: xml?.device?.deviceType?.text(),
            manufacturer: xml?.device?.manufacturer?.text(),
            manufacturerURL: xml?.device?.manufacturerURL?.text(),
            modelDescription: xml?.device?.modelDescription?.text(),
            modelNumber: xml?.device?.modelNumber?.text(),
            modelURL: xml?.device?.modelURL?.text(),
		]
	}
    else {
    	return
    }
    def device = devices?.find { it?.key?.equals(body?.UDN) }
    if (device) {
        def name = body?.friendlyName
        if (body?.friendlyName == null || body?.friendlyName?.equals("")) {
            name = body?.modelName
        }

        device.value << [name: name,
			verified: true]
        device.value << body
    }
}
/*******************************************************************************
 * Process responses from devices and pass to device handler
 */
def responseHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId
 	def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]
	def d = getChildDevices()?.find {
		(it.getDataValue("ip") == parsedEvent?.ip && it.getDataValue("port") == parsedEvent?.port) ||
        (it.getDataValue("mac")  == parsedEvent?.mac && (parsedEvent?.headers?.sid == null || it.deviceNetworkId == parsedEvent?.headers?.sid))
	}
	if (d) {
    	d.parseResponse(parsedEvent)
    }
}
/*******************************************************************************
 * Helper methods for devices to communicate out
 */
def GET(device, url) {
  def host=getDeviceAddress(device)
  def hubAction = new physicalgraph.device.HubAction([method: "GET",
	path: url,
    headers: [HOST:host]
    ]
  )
  sendHubCommand(hubAction)
}

def POST(device, url, args=[]) {
    def host=getDeviceAddress(device)
    //device.log("POST($device, $url, $args) - $host")
    def hubAction = new physicalgraph.device.HubAction(
        method: "POST",
        path: url,
        body: args,
        headers: [Host:host]
    )
    //device.log(hubAction)
    sendHubCommand(hubAction)
}
def SUBSCRIBE(device, url, hub, args=[]) {
    //device.log("SUBSCRIBE($device, $url, $callback, $args)")
    def host = getDeviceAddress(device)
    def callback = GetCallBackAddress(hub)
    def hubAction = new physicalgraph.device.HubAction(
        method: "SUBSCRIBE",
        path: url,
        headers: [
            HOST: host,
            CALLBACK: "http://${callback}/",
            NT: "upnp:event",
            TIMEOUT: "Second-120"
        ],
        body: args,
    )
    sendHubCommand(hubAction)
}
def GetCallBackAddress(hub) {
	hub.getDataValue("localIP") + ":" + hub.getDataValue("localSrvPortTCP")
}
/*******************************************************************************
 * Helper Functions
 */
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
def getDeviceAddress(device) {
 return convertHexToIP(device.getDataValue("ip")) +":"+convertHexToInt(device.getDataValue("port"))
}

