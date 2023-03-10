/**
 *  Espruino Monitor Test 1
 *
 *  Copyright 2018 Ian Ellison-Taylor
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
    name: "Espruino Monitor Test 1",
    namespace: "reszolve.com",
    author: "Ian Ellison-Taylor",
    description: "Espruino test project",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "deviceDiscovery", title: "IoT Device Setup", content: "deviceDiscovery")
}

def deviceDiscovery() {
	def options = [:]
	def devices = getDevices()
	devices.each {
        def value = "Espruino Device ${it.value.ssdpUSN.split(':')[1][-4..-1]}"
        def key = it.value.ssdpUSN
        options["${key}"] = value
	}
    
    unsubscribe();
    ssdpSubscribe();
	ssdpDiscover();
    
	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait while we discover your device, discovery can take a minutes or more. Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options, submitOnChange: true
		}
	}
}

preferences {
	section("Title") {
		// TODO: put inputs here
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
	log.debug "initialize v0.0.3"
    
    unsubscribe();
    unschedule();
    
    ssdpSubscribe();
	ssdpDiscover();
    
    if (selectedDevices) {
		addDevices()
	}
    
    runEvery5Minutes("ssdpDiscover");
}

def addDevices() {
	def devices = getDevices()

	selectedDevices.each { dni ->
		def selectedDevice = devices.find { it.value.ssdpUSN == dni }
		def d
		if (selectedDevice) {
			d = getChildDevices()?.find {
				it.deviceNetworkId == selectedDevice.value.ssdpUSN
			}
		}

		if (!d) {
			log.debug "Creating device with dni: ${selectedDevice.value.ssdpUSN}"
			addChildDevice("reszolve-com", "Espruino Device", selectedDevice?.value.ssdpUSN, selectedDevice?.value.hub, [
				"label": selectedDevice?.value?.name ?: "Espruino",
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress
				]
			])
		}
	}
}

void ssdpSubscribe() {
	log.debug "ssdpSubscribe"
    subscribe(location, "ssdpTerm.urn:schemas-reszolve-com:device:espruino:1", ssdpHandler)
}

void ssdpDiscover() {
	log.debug "ssdpDiscover"
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-reszolve-com:device:espruino:1", physicalgraph.device.Protocol.LAN))
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

def ssdpHandler(evt) {
    def description = evt.description
    log.debug "ssdpHandler $description"
    def hub = evt?.hubId

    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    def devices = getDevices()
	def ssdpUSN = parsedEvent.ssdpUSN
    // log.debug parsedEvent
	if (devices."${ssdpUSN}") {
		def d = devices."${ssdpUSN}"
		if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
			d.networkAddress = parsedEvent.networkAddress
			d.deviceAddress = parsedEvent.deviceAddress
			def child = getChildDevice(ssdpUSN)
			if (child) {
				child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
			}
		}
	} else {
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

def parse(String description) {
    log.debug "parse description: $description"
}
