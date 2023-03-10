/**
 *  Generic UPnP Service Manager
 *
 *  Copyright 2016 SmartThings
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
                name: "Jacuzzi J-LX Service Manager",
                namespace: "smartthings",
                author: "SmartThings",
                description: "UPnP Service Manager SmartApp for Jacuzzi J-LX Hot Tubs",
                category: "SmartThings Labs",
                iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
                iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
        page(name: "searchTargetSelection", title: "UPnP Search Target", nextPage: "deviceDiscovery") {
                section("Search Target") {
                        input "searchTarget", "string", title: "Search Target", defaultValue: "urn:schemas-upnp-org:device:jacuzzi:1", required: true
                }
        }
        page(name: "deviceDiscovery", title: "UPnP Device Setup", content: "deviceDiscovery")
}

def deviceDiscovery() {
        def options = [:]
        def devices = getVerifiedDevices()
        devices.each {
            log.debug "found device ${it}"
            def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
            def key = it.value.mac
            options["${key}"] = value
        }

        ssdpSubscribe()

        ssdpDiscover()
        verifyDevices()

        return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", 
                           nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
            section("Please wait while we discover your UPnP Device. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
                input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)",
                    multiple: true, options: options
            }
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
        unsubscribe()
        unschedule()

        ssdpSubscribe()

        if (selectedDevices) {
                addDevices()
        }

        runEvery5Minutes("ssdpDiscover")
}

void ssdpDiscover() {
        sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${searchTarget}", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
        subscribe(location, "ssdpTerm.${searchTarget}", ssdpHandler)
}

Map verifiedDevices() {
        def devices = getVerifiedDevices()
        def map = [:]
        devices.each {
                def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
                def key = it.value.mac
                map["${key}"] = value
        }
        map
}

void verifyDevices() {
    def devices = getDevices().findAll { it?.value?.verified != true }
    devices.each {
        int port = convertHexToInt(it.value.deviceAddress)
        String ip = convertHexToIP(it.value.networkAddress)
        String host = "${ip}:${port}"
        sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
    }
}

def getVerifiedDevices() {
    getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
    if (!state.devices) {
        state.devices = [:]
    }
    state.devices
}

def addDevices() {
    def devices = getDevices()

    selectedDevices.each { dni ->
        def selectedDevice = devices.find { it.value.mac == dni }
        def d
        if (selectedDevice) {
            d = getChildDevices()?.find {
                it.deviceNetworkId == selectedDevice.value.mac
            }
        }

        if (!d) {
            log.debug "Creating Jacuzzi J-LX with dni: ${selectedDevice.value.mac}"
            addChildDevice("smartthings", "Jacuzzi J-LX", selectedDevice.value.mac, selectedDevice.value.hub, [
                "label": selectedDevice?.value?.name ?: "Jacuzzi J-LX",
                "data": [
                    "mac": selectedDevice.value.mac,
                    "ip": selectedDevice.value.networkAddress,
                    "port": selectedDevice.value.deviceAddress
                ]
            ])
        }
    }
}

def ssdpHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    def devices = getDevices()
    String ssdpUSN = parsedEvent.ssdpUSN.toString()
    if (devices."${ssdpUSN}") {
        def d = devices."${ssdpUSN}"
        if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
            d.networkAddress = parsedEvent.networkAddress
            d.deviceAddress = parsedEvent.deviceAddress
            def child = getChildDevice(parsedEvent.mac)
            if (child) {
                child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
            }
        }
    } else {
        devices << ["${ssdpUSN}": parsedEvent]
    }
}

def sync(ip, port) {
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port")
    if (ip && ip != existingIp) {
        updateDataValue("ip", ip)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
    }
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
    def body = hubResponse.xml
    def devices = getDevices()
    def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
    if (device) {
        device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
    }
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
