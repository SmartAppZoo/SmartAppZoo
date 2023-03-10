/**
 *    Smoke Alarm Monitor [SmartApp]
 *        Uses a simple ESP8266 circuit to detect the sound of a (not-so-smart) smoke alarm
 *
 *    Copyright 2017 Ross Lipenta
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License. You may obtain a copy of the License at:
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *    on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *    for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Smoke Alarm Monitor Manager",
    namespace: "rlipenta",
    author: "Ross Lipenta",
    description: "Monitor sound smoke alarm",
    category: "Safety & Security",
    singeInstance: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "selectDevice", title: "Searching for Device...", content: "selectDevice")
}

/**
 * Called when the Application is initialized
 */
def initialize() {
    log.trace "Executing 'initialize'"
    unsubscribe()
    addDevices()
    unschedule()
    runEvery5Minutes("subscribeToDevices")
    subscribeToDevices()
}

/**
 * Returns the SSDP URN for discovery
 */
def searchTarget() { return "urn:schemas-upnp-org:device:RmlAlarmMonitorSensor:1" }

/**
 * Generates the select device UI
 *
 *  Will start SSDP discovery
 */
def selectDevice() {
    log.trace "Executing 'selectDevice'"
    def refreshInterval = 3

    ssdpSubscribe()

    ssdpDiscover()

    def deviceMap = getDeviceMap()

    return dynamicPage(name: "selectDevice", title: "", nextPage: "", refreshInterval: refreshInterval, install: true, uninstall: true) {
            section("") {
                input ("selectDevice", "enum", required: false, title: "Select monitor (${deviceMap.size() ?: 0} found)", multiple: false, options: deviceMap)
            }
    }
}

/**
 * Creates a simple device map used to display the devices for user selection
 */
def getDeviceMap() {
    def devices = getDevices()
    def map = [:]
    devices.each {
            def value = "Alarm Monitor (" + convertHexToIP(it.value.networkAddress) + ")"
            def key = it.value.ssdpUSN.toString()
            map["${key}"] = value
    }
    return map
}

/**
 * Gets a list of devices that have been discovered
 */
def getDevices() {
    if (!state.devices) { state.devices = [:] }
    log.debug "state.devices has ${state.devices.size()} devices currently."
    return state.devices
}

/**
 * Starts the SSDP discovery
 */
void ssdpDiscover() {
    log.trace "Lan discovery ${searchTarget()}"
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${searchTarget()}", physicalgraph.device.Protocol.LAN))
}

/**
 * Subscribes to the SSDP discovery event
 */
void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.${searchTarget()}", ssdpHandler)
}

/**
 * Event handler for the SSDP discovery
 *
 *  This will obtain all of the necessary information about the
 *  device and add it to the devices collection for display as
 *  a possible device to select. It will also update information
 *  about an existing device if it is determined that the IP or
 *  Port of the device has changed.
 */
def ssdpHandler(evt) {
    log.trace "Executing 'ssdpHandler'"
    log.trace "Received Response: " + evt.description

    if(evt.name == "ping") { return "" }
    def description = evt.description
    def hub = evt?.hubId
    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]
    if (parsedEvent?.ssdpTerm?.contains("${searchTarget()}")) {
        def devices = getDevices()
        def ip = convertHexToIP(parsedEvent.networkAddress)
        def port = convertHexToInt(parsedEvent.deviceAddress)
        if (!(devices."${parsedEvent.ssdpUSN.toString()}")) { //if it doesn't already exist
            log.debug "Parsed Event: " + parsedEvent
            devices << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
        } else { // just update the values
            def device = devices."${parsedEvent.ssdpUSN.toString()}"
            boolean deviceChangedValues = false
            if(device.ip != ip || device.port != port) {
                device.ip = ip
                device.port = port
                deviceChangedValues = true
            }
            if (deviceChangedValues) {
                def children = getChildDevices()
                children.each {
                    if (it.getDeviceDataByName("ssdpUSN") == parsedEvent.ssdpUSN) {
                        it.updateDataValue("ip", ip)
                        it.updateDataValue("port", port)
                    }
                }
            }
        }
    }
}

/**
 * AddDevices is called to add the selected device to the SmartThings hub
 */
def addDevices() {
    log.trace "Executing 'addDevices'"

    def ssdpUSN = selectDevice
    def devices = getDevices()
    def dni = devices[ssdpUSN].mac

    // Check if child already exists
    def device = getChildDevices()?.find { it.device.deviceNetworkId == dni }

    if (!device) {
        def ip = devices[ssdpUSN].networkAddress
        def port = devices[ssdpUSN].deviceAddress
        log.debug "Adding ${dni} for ${ssdpUSN} / ${ip}:${port}"
        device = addChildDevice("rlipenta", "Smoke Alarm Monitor", dni, devices[ssdpUSN].hub, [
            "label": "Smoke Alarm Monitor",
            "data": [
                    "ip": ip,
                    "port": port,
                    "ssdpUSN": ssdpUSN,
                    "ssdpPath": devices[ssdpUSN].ssdpPath,
                    "mac": devices[ssdpUSN].mac
            ]
        ])
    } else {
        log.debug "This device already exists"
    }
}

/**
 * Sync is called when a device's settings are updated
 */
def sync() {
    log.debug "Executing 'sync'"
}

/**
 * Installed is called when a user selects and installs a device
 */
def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

/**
 * Updated is called when a device's settings are updated
 */
def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

/**
 * Subscribes to all child devices
 */
def subscribeToDevices() {
    log.trace "Executing 'subscribeToDevices'"
    def devices = getAllChildDevices()
    devices.each { d ->
        d.subscribe()
    }
}

/**
 *  Convert hex (e.g. port number) to decimal number
 *
 *  @hex a hexidecimal number to convert to an integer
 *  @return an integer
 */
private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

/**
 * Convert internal hex representation of IP address to dotted quad
 *
 * @hex a hexidecimal representation of the IP address
 * @return an IP address in dotted quad notation
 */
private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
