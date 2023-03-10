/**
include(`app_constants.m4')
include(`header.m4')
 */

definition(
        // include(`definition.m4')
)

preferences {
    page(name: "deviceDiscovery", title:"Device Discovery")
}

def installed() {
    log.trace "__name__ installed with settings: ${settings}"
    initialize()
}

def uninstalled() {
    removeDevicesExcept([])
    log.trace "__name__ uninstalled"
}

def updated() {
    log.trace "__name__ updated with settings: ${settings}"

    state.deviceRefreshes = 0
    removeDevicesExcept(selectedDevices)
    populateDevices(selectedDevices)

    initialize()
}

def initialize() {
    log.debug "Initializing __name__"
    unschedule()
    unsubscribe()
    subscribe(location, null, onLocation, [filterEvents:false])
    runEvery10Minutes("startDeviceDiscovery")
}

// <editor-fold desc="Device Discovery - returns map foundDevices [mac: ip]">

def deviceDiscovery() {
    def refreshInterval = 3

    state.foundDevices = state.foundDevices ?: [:]
    state.deviceRefreshes = (state.deviceRefreshes ?: 0) + 1

    if (state.deviceRefreshes == 1) {
        subscribe(location, null, onLocation, [filterEvents:false])
    }

    if (state.deviceRefreshes % 5 == 1) {
        startDeviceDiscovery()
    }

    dynamicPage(name:"deviceDiscovery", title:"Device Discovery Started!", refreshInterval:refreshInterval, install:true, uninstall: true) {
        section("Please wait while we discover your devices. This could take a few minutes. Select your device below once discovered.") {
            input "selectedDevices", "enum", required:true, title:"Select Device (${state.foundDevices.size()} found)", multiple:true, options: state.foundDevices
        }
    }
}

def startDeviceDiscovery() {
    // This SSDP type must be kept synced between the device and the app, or the device will not be discovered.
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-blinds:device:WifiBlinds:1", physicalgraph.device.Protocol.LAN))
}

def onLocation(evt) {
    def parsedEvent = parseLanMessage(evt.description)

    // Avoid the mistake the Hue Connect app makes - check for a specific device type rather than Basic.
    if (parsedEvent?.ssdpTerm?.contains("urn:schemas-blinds:device:WifiBlinds:1")) {
        discoveredDevice(parsedEvent)
    }
    return parsedEvent
}

private discoveredDevice(parsedEvent) {
    // Found a device. Check if it already exists, and add it if not.
    log.info "Found __name__ device via SSDP: ${parsedEvent}"
    if (!parsedEvent?.mac) {
        throw new IllegalArgumentException("__name__ SSDP discovery event doesn't seem to have a MAC address.")
    }
    def foundDevices = state.foundDevices
    def newIp = convertHexToIP(parsedEvent.networkAddress)
    def lastIp = foundDevices.put(parsedEvent.mac, newIp)
    if (lastIp && lastIp != newIp) {
        log.info "__name__ device (${parsedEvent.mac}) IP changed: ${lastIp} to ${newIp}. Updating."

        def device = getChildDevice("${parsedEvent.mac}")
        device?.sendEvent(name:"ip", value: newIp)
        device?.updateDataValue("ip", newIp)
        device?.refresh()
    }
}

def populateDevices(selected) {
    log.info "Creating SmartThings device nodes for ${selected.size()} __name__ devices."
    return state.foundDevices.collect({ mac, ip -> asDevice(mac, ip) })
}

private asDevice(mac, ip) {
    log.debug "Creating or referencing __name__ device ${mac} -> ${ip}."
    def device = getChildDevice("${mac}")

    if (!device) {
        device = addChildDevice("__namespace__", "__device_id__", "${mac}", getSmartThingsHub().id,
                                [name: "__human_name__", completedSetup: true])
        device.sendEvent(name:"ip", value: ip)
        device.updateDataValue("ip", ip)
        device.refresh()
        log.info "Successfully added node for __name__ device ${mac}."
    } else {
        log.info "Device ${mac} already exists, using the existing instance."
    }
    return device
}

def removeDevicesExcept(keptDnis) {
    def toRemove = getChildDevices().findAll { !keptDnis.contains(it.deviceNetworkId) }
    log.info "Removing ${toRemove.size()} de-selected devices. Keeping: ${keptDnis}"
    toRemove.each {
        log.debug "Removing device ${it.deviceNetworkId}"
        deleteChildDevice(it.deviceNetworkId)
    }

    state.foundDevices.keySet().retainAll(keptDnis)
}

// </editor-fold>

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private getSmartThingsHub() {
    return location.hubs.find { it.localIP } ?: location.hubs[0]
}