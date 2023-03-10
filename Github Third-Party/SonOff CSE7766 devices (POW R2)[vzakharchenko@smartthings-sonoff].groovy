definition(
        name: "SonOff CSE7766 devices (POW R2) ",
        minHubCoreVersion: '000.021.00001',
        executeCommandsLocally: true,
        namespace: "vzakharchenko",
        author: "Vasiliy Zakharchenko",
        description: "Create/Delete SonOff devices",
        category: "My Apps",
        iconUrl: "https://cdn-media.itead.cc/media/catalog/product/s/o/sonoff_03.jpg",
        iconX2Url: "https://cdn-media.itead.cc/media/catalog/product/s/o/sonoff_03.jpg")


preferences {
    page(name: "config", refreshInterval: 5)
}

def config() {
    def devices = searchDevicesType("Sonoff CSE7766 Switch");
    if (!state.clearDevices) {
        state.sonoffMacDevices = [:];
        state.clearDevices = true
    }
    if (!state.subscribe) {
        subscribe(location, "ssdpTerm." + getURN(), locationHandler)
        state.subscribe = true
    }
    if (state.sonoffMacDevices == null) {
        state.sonoffMacDevices = [:];
    }
    int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
    state.refreshCount = refreshCount + 1
    def refreshInterval = refreshCount == 0 ? 2 : 5
    //ssdp request every fifth refresh
    if ((refreshCount % 2) == 0) {
        ssdpDiscover();
    }

    dynamicPage(name: "config", title: "SonOff Manager", refreshInterval: refreshInterval, install: true, uninstall: true) {
        section("Offline Timeout") {
            input "offlineTimeout", "enum", multiple: false, required: true, title: "Select Offline timeout in minutes", options: ["5", "10", "15", "20", "30", "45", "60", "never"]
        }
        section("on this hub...") {
            input "theHub", "hub", multiple: false, required: true, defaultValue: state.hub
        }

        if (state.sonoffMacDevices == null) {
            state.sonoffMacDevices = [:];
        }
        if (state.sonoffDevicesTimes == null) {
            state.sonoffDevicesTimes = [:];
        }
        section("Device List") {
            def deviceOptions = [];
            state.sonoffMacDevices.each { mac, ip ->


                def device = devices.find { device -> device.getDeviceNetworkId() == mac }
                if (device == null) {
                    deviceOptions.push(mac);
                }
            }
            debug("deviceOptions:$deviceOptions")
            input "sonoffs", "enum", multiple: true, required: false, title: "Select Devices (${deviceOptions.size()} found)", options: deviceOptions
        }


    }
}


def installed() {
    createAccessToken()
    getToken()
    debug("applicationId: $app.id")
    debug("accessToken: $state.accessToken")
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    unsubscribe();
    state.clearDevices = false
    state.subscribe = false;
    if (sonoffs) {
        sonoffs.each {
            def mac = it;

            def ip = state.sonoffMacDevices.get(mac);
            debug("mac = ${mac}");
            def sonoffDevice = searchDevicesType("Sonoff CSE7766 Switch").find {
                return it.getDeviceNetworkId() == mac
            };
            if (sonoffDevice == null) {
                sonoffDevice = addChildDevice("vzakharchenko", "Sonoff CSE7766 Switch", mac, theHub.id, [label: "Sonoff(${mac}) POWR2", name: "Sonoff(${mac}) POWR2"])
            }
            sonoffDevice.setIp(ip);
            sonoffDevice.setPort("80");
            apiPost(ip, 80, "/config", null,
                    "applicationId=${app.id}" +
                            "&accessToken=${state.accessToken}"
                    , "application/x-www-form-urlencoded")
        }
    }
    if (!state.subscribe) {
        subscribe(location, "ssdpTerm." + getURN(), locationHandler)
        state.subscribe = true

    }
    if (offlineTimeout == "never") {
        state.offlineTimeOut = 0;
    } else {
        state.offlineTimeOut = offlineTimeout.toInteger();
    }
    runEvery5Minutes(healthCheck)
}


mappings {
    path("/init") {
        action:
        [
                GET: "init"
        ]
    }
    path("/get/subscribe") {
        action:
        [
                GET: "subscribe"
        ]
    }

    path("/get/info") {
        action:
        [
                GET: "infoGET"
        ]
    }
}

def init() {
    def mac = modifyMac(params.mac);
    state.sonoffMacDevices.put(mac, params.ip);
    def relay = params.relay;
    def sonoffDevice = searchDevicesType("Sonoff CSE7766 Switch").find {
        return it.getDeviceNetworkId() == mac
    };
    if (sonoffDevice != null) {
        debug("init: ${params.ip}:${sonoffDevice.getDeviceNetworkId()}:${relay}");
        if (relay.equals("on")) {
            sonoffDevice.forceOn();
        } else if (relay.equals("off")) {
            sonoffDevice.forceOff();
        }
        sonoffDevice.markDeviceOnline();
        updateActiveTime(mac)
        sonoffDevice.setIp(params.ip);
        sonoffDevice.subscribeCommand();
    }

    debug("init: $params")
    return "OK"
}

def updateActiveTime(mac) {
    def date = new Date();
    debug("update time for ${mac} states: ${state.sonoffDevicesTimes}");
    def sonoffDevice = searchDevicesType("Sonoff CSE7766 Switch").find {
        return it.getDeviceNetworkId() == mac
    };
    if (sonoffDevice != null) {
        sonoffDevice.markDeviceOnline();
    }
    state.sonoffDevicesTimes.put(mac, date.getTime());
}


def subscribe() {
    def json = request.JSON;
    def mac = modifyMac(params.mac);
    state.sonoffMacDevices.put(mac, params.ip);
    def sonoffDevice = searchDevicesType("Sonoff CSE7766 Switch").find {
        return it.getDeviceNetworkId() == mac
    };
    if (sonoffDevice) {
        sonoffDevice.markDeviceOnline();
        updateActiveTime(mac);
        sonoffDevice.setIp(params.ip);
        sonoffDevice.subscribeCommand();
    }
    return "OK"
}

def infoGET() {
    def s = "undefined";
    def name = "undefined";
    def deviceName = "undefined";
    debug("params=${params}");
    def mac = modifyMac(params.mac);
    debug("mac=${mac}");
    state.sonoffMacDevices.put(mac, params.ip);
    def sonoffDevice = searchDevicesType("Sonoff CSE7766 Switch").find {
        return it.getDeviceNetworkId() == mac
    };
    if (sonoffDevice != null) {
        updateActiveTime(mac);
        def switchData = sonoffDevice.currentState("switch");
        if (switchData) {
            s = switchData.value
            name = switchData.linkText;
        }
        deviceName = sonoffDevice.getLabel();
        sonoffDevice.setIp(params.ip);
    }
    return [status: s, name: name, deviceName: deviceName]
}

def getURN() {
    return "urn:sonoff:device:vzakharchenko:e:1"
}

void ssdpDiscover() {
    debug("send lan discovery " + getURN())
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery " + getURN(), physicalgraph.device.Protocol.LAN))
}

def locationHandler(evt) {
    def description = evt?.description
    debug("event: ${description}");
    def urn = getURN()
    def hub = evt?.hubId
    def parsedEvent = parseEventMessage(description)
    state.hub = hub
    if (parsedEvent?.ssdpTerm?.contains(urn)) {
        if (state.sonoffMacDevices == null) {
            state.sonoffMacDevices = [:];
        }
        def ip = convertHexToIP(parsedEvent.ip)
        state.sonoffMacDevices.put(modifyMac(parsedEvent.mac), ip);
        checkSonOff(parsedEvent);
    }

}

def checkSonOff(parsedEvent) {
    def curTime = new Date().getTime();

    def devices = searchDevicesType("Sonoff CSE7766 Switch");
    def device = devices.find {
        return it.getDeviceNetworkId() == modifyMac(parsedEvent.mac)
    };
    if (device) {
        device.setIp(convertHexToIP(parsedEvent.ip));
        device.setPort(convertHexToInt(parsedEvent.port));
        state.sonoffDevicesTimes.put(modifyMac(parsedEvent.mac), curTime)
    }
}

def String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]), convertHexToInt(hex[2..3]), convertHexToInt(hex[4..5]), convertHexToInt(hex[6..7])].join(".")
}

def Integer convertHexToInt(hex) {
    Integer.parseInt(hex, 16)
}

private def parseEventMessage(String description) {
    def event = [:]
    def parts = description.split(',')

    parts.each
            { part ->
                part = part.trim()
                if (part.startsWith('devicetype:')) {
                    def valueString = part.split(":")[1].trim()
                    event.devicetype = valueString
                } else if (part.startsWith('mac:')) {
                    def valueString = part.split(":")[1].trim()
                    if (valueString) {
                        event.mac = valueString
                    }
                } else if (part.startsWith('networkAddress:')) {
                    def valueString = part.split(":")[1].trim()
                    if (valueString) {
                        event.ip = valueString
                    }
                } else if (part.startsWith('deviceAddress:')) {
                    def valueString = part.split(":")[1].trim()
                    if (valueString) {
                        event.port = valueString
                    }
                } else if (part.startsWith('ssdpPath:')) {
                    def valueString = part.split(":")[1].trim()
                    if (valueString) {
                        event.ssdpPath = valueString
                    }
                } else if (part.startsWith('ssdpUSN:')) {
                    part -= "ssdpUSN:"
                    def valueString = part.trim()
                    if (valueString) {
                        event.ssdpUSN = valueString

                        def uuid = getUUIDFromUSN(valueString)

                        if (uuid) {
                            event.uuid = uuid
                        }
                    }
                } else if (part.startsWith('ssdpTerm:')) {
                    part -= "ssdpTerm:"
                    def valueString = part.trim()
                    if (valueString) {
                        event.ssdpTerm = valueString
                    }
                } else if (part.startsWith('headers')) {
                    part -= "headers:"
                    def valueString = part.trim()
                    if (valueString) {
                        event.headers = valueString
                    }
                } else if (part.startsWith('body')) {
                    part -= "body:"
                    def valueString = part.trim()
                    if (valueString) {
                        event.body = valueString
                    }
                }
            }

    event
}

def getUUIDFromUSN(usn) {
    def parts = usn.split(":")

    for (int i = 0; i < parts.size(); ++i) {
        if (parts[i] == "uuid") {
            return parts[i + 1]
        }
    }
}


def getToken() {
    if (!state.accessToken) {
        try {
            getAccessToken()
            DEBUG("Creating new Access Token: $state.accessToken")
        } catch (ex) {
            DEBUG("Did you forget to enable OAuth in SmartApp IDE settings")
            DEBUG(ex)
        }
    }
}

def searchDevicesType(devType) {
    def typeDevices = []
    childDevices.each {
        if (it.getTypeName() == devType) {
            typeDevices.add(it)
        }
    }
    return typeDevices
}

def healthCheck() {
    ssdpDiscover();
    def timeout = 1000 * 60 * state.offlineTimeOut;
    def curTime = new Date().getTime();


    def devices = searchDevicesType("Sonoff CSE7766 Switch");


    devices.each {
        if (timeout > 0) {
            def mac = it.getDeviceNetworkId();
            def ip = state.sonoffMacDevices.get(mac);
            def activeDate = state.sonoffDevicesTimes.get(mac);
            if ((curTime - timeout) > activeDate) {
                it.markDeviceOffline();
                debug("ip ${ip} offline ${curTime - timeout} > ${activeDate} ")
            } else {
                it.markDeviceOnline();
                debug("ip ${ip} online ${curTime - timeout} < ${activeDate} ")
            }
        } else {
            it.markDeviceOnline();
        }
    }
}
/*
def locationHandler(evt) {
    def description = evt.description
    def msg = parseLanMessage(description)
    def json = msg.json
    def ip = json.ip;
    def relay = json.relay;
    debug("ip $ip : $relay");
    def sonoffDevice = searchDevicesType("Virtual Switch").find {
        return it.getDeviceNetworkId() == json.mac
    };
    if (sonoffDevice != null) {
        updateActiveTime(json.mac)
        if (relay.equals("on")) {

            sonoffDevice.on();
        } else if (relay.equals("off")) {
            sonoffDevice.off();
        }
    }
}*/

def apiGet(ip, port, path, query) {
    def url = "${ip}:${port}";
    log.debug "request:  ${url}/${path} query= ${query}"
    def result = new physicalgraph.device.HubAction(
            method: 'GET',
            path: "/${path}",
            headers: [
                    HOST  : url,
                    Accept: "*/*",
                    test  : "testData"
            ],
            query: query
    )

    return sendHubCommand(result)
}

def apiPost(ip, port, path, query, body, contentType) {
    def url = "${ip}:${port}";
    log.debug "request:  ${url}${path} query= ${query} body=${body} contentType=${contentType} "
    def result = new physicalgraph.device.HubAction(
            method: 'POST',
            path: path,
            headers: [
                    HOST          : url,
                    Accept        : "*/*",
                    "Content-Type": contentType
            ],
            body: body
    )

    return sendHubCommand(result)
}

private def modifyMac(String macString) {
    return macString.replaceAll(":", "")
}

def debug(message) {
    def debug = false
    if (debug) {
        log.debug message
    }
}
