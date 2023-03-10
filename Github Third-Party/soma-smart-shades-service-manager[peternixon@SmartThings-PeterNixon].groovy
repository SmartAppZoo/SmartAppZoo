/*
*  SOMA Smart Shades Service Manager
*  Category: SmartApp Service Manager
* 
*/

definition(
    name: "SOMA Smart Shades Service Manager",
    namespace: "peternixon",
    author: "Peter Nixon",
    description: "Creates a SOMA Smart Shades Device..",
    category: "My Apps",
    iconUrl: "https://github.com/chancsc/icon/raw/master/standard-tile%401x.png",
    iconX2Url: "https://github.com/chancsc/icon/raw/master/standard-tile@2x.png",
    iconX3Url: "https://github.com/chancsc/icon/raw/master/standard-tile@3x.png",
    singleInstance: true
    )

preferences {
	input("bridgeIp", "text", title: "SOMA Connect IP", required: true)
}

def initialize() {
	log.debug("bridgeIp is ${bridgeIp}")

    log.debug("initialize() Soma Connect with settings ${settings}")
    if (!bridgeIp) {
        log.info "Device IP needs to be configured under device settings!"
        return
    }

    def hub = location.hubs[0]

    log.debug "id: ${hub.id}"
    log.debug "zigbeeId: ${hub.zigbeeId}"
    log.debug "zigbeeEui: ${hub.zigbeeEui}"

    // PHYSICAL or VIRTUAL
    log.debug "type: ${hub.type}"

    log.debug "name: ${hub.name}"
    log.debug "firmwareVersionString: ${hub.firmwareVersionString}"
    log.debug "localIP: ${hub.localIP}"
    log.debug "localSrvPortTCP: ${hub.localSrvPortTCP}"

    def shadeLabel = "SOMA Connect Bridge"
    def bridgePort = '3000'
    def porthex = convertPortToHex(bridgePort)
    def hosthex = convertIPtoHex(bridgeIp)
    def deviceId = "$hosthex:$porthex"
    
    log.debug(deviceId)
    def existing = getChildDevice(deviceId)

    if (!existing) {
        def childDevice = addChildDevice("peternixon", "SOMA Connect Bridge", deviceId, hub.id, [
				"label": "SOMA Connect",
				"data": [
                    "bridgeIp": bridgeIp,
					"bridgePort": bridgePort
				]
			])
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updated() {
    log.debug("updated() Soma Connect with settings ${settings}")
	return initialize()
}

def installed() {
    log.debug("installed() Soma Connect with settings ${settings}")
    return initialize()
}

def refresh() {
    log.info "Device ID: $device.deviceNetworkId refresh() was triggered"
    return initialize()
}

def ping() {
    log.info "Device ID: $device.deviceNetworkId ping() was triggered"
	return refresh()
}

def poll() {
    log.info "Device ID: $device.deviceNetworkId poll() was triggered"
    return initialize()
}

private getHostAddress() {
    return convertHexToIP(bridgeIp) + ":" + convertHexToInt('3000')
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}
