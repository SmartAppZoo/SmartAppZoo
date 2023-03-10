definition(
        name: "AutoBridge",
        namespace: "autobridge",
        author: "autobridge",
        description: "AutoBridge connector for SmartThings",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section {
        input "actuators", "capability.actuator", multiple: true
        input "sensors", "capability.sensor", multiple: true
        input "containerID", "text", title: "ID of container to add"
        input "containerValidationKey", "text", title: "Validation key for container to add"
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def uninstalled() {
    getChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

def initialize() {
	if (containerID != null)
    	getContainerValidationKeyStringMap()[containerID] = containerValidationKey

    subscribe(location, null, processLocationEvent, [filterEvents: false])

	sensors
    	.plus(actuators)
        .each { device ->
        	device.getCapabilities()
            	.collectMany { it.getAttributes() }
                .each { subscribe(device, it.name, processDeviceEvent) }        	
        }

    searchForDevices()
    runEvery30Minutes(searchForDevices)
}

def searchForDevices() {
    log.info "Searching for devices..."
    
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:AutoBridge:1", physicalgraph.device.Protocol.LAN))
    
    // doesn't work yet, but we'll leave it here in case it's supported in the future, as it is most reliable with android
    //sendHubCommand(new physicalgraph.device.HubAction("lan discovery mdns/dns-sd _autobridge._tcp.", physicalgraph.device.Protocol.LAN))
}

def parseDniPath(deviceNetworkId) {
    def match = (deviceNetworkId =~ /^(.*?)\:(.*?)\:(.*)$/)[0]
    return [targetContainerID: match[1], sourceContainerID: match[2], deviceID: match[3]]
}

def getChildDeviceInfos() {
    return getAllChildDevices().collect { [device: it, path: parseDniPath(it.deviceNetworkId)] }
}

def getAssignedDevice(deviceNetworkId) {
    return actuators.find { it.deviceNetworkId == deviceNetworkId } ?: sensors.find { it.deviceNetworkId == deviceNetworkId }
}

def getSubscribeEventsMap() {
    if (state.subscribeEventsMap == null) state.subscribeEventsMap = [:]
    return state.subscribeEventsMap
}

def getContainerHostMap() {
    if (state.containerHostMap == null) state.containerHostMap = [:]
    return state.containerHostMap
}

def getContainerValidationKeyStringMap() {
    if (state.containerValidationKeyStringMap == null) state.containerValidationKeyStringMap = [:]
    return state.containerValidationKeyStringMap
}

def getPropertyNameValueMap(device) {
    device.getSupportedAttributes().collectEntries { [(it.name): device.currentValue(it.name)] }
}

def tryDeleteChildDevice(deviceNetworkId) {
	try {
		deleteChildDevice(deviceNetworkId)
    } catch (ex) {
    	log.error("Error deleting child device '${deviceNetworkId}': ${ex}")
    }
}

def processDeviceEvent(event) {
	getSubscribeEventsMap()
        .collect()
        .findAll { it.value > new Date().getTime() }
        .each { log.info("Sending event to '${it.key}' from '${event.device.name}'; '${event.name}': '${event.value}'") }
        .each { sendMessage(it.key, [deviceID: event.device.deviceNetworkId, propertyName: event.name, propertyValue: event.value]) }
}

def processLocationEvent(event) {
    def lanMessage = parseLanMessage(event.description)

    if (lanMessage?.ssdpTerm == "urn:schemas-upnp-org:device:AutoBridge:1") {
        log.info("Processing SSDP message from ${lanMessage.networkAddress}...")

        def containerID = lanMessage.ssdpUSN.split(':')[1]
        getContainerHostMap()[containerID] = lanMessage.networkAddress
    } else if (lanMessage?.mdnsPath) {
        log.info("Processing MDNS message from ${lanMessage.networkAddress}...")
        
        log.info("RECEIVED MDNS, NEED TO IMPLEMENT")
    } else if (lanMessage?.json?.operation != null) {
    	def containerID = lanMessage.header.split()[1].split('/').last()
        def operation = lanMessage?.json?.operation
        def validationKeyString = getContainerValidationKeyStringMap()[containerID]

        if (validationKeyString != null) {
            def dateString = lanMessage.headers["Date"]
            def date = Date.parse("EEE, dd MMM yyyy HH:mm:ss z", dateString)

            if (Math.abs(date.getTime() - new Date().getTime()) > 60000)
            throw new Exception("Date is out of valid range")

            def providedSignatureString = lanMessage.headers["Authorization"]
            def actualSignatureString = computeSignatureString(validationKeyString, lanMessage.headers["Date"], lanMessage.body)

            //log.info("providedSignatureString: ${providedSignatureString}, actualSignatureString: ${actualSignatureString}")

            if (providedSignatureString != actualSignatureString)
            	throw new Exception("Signature is not valid")
        }

        if (operation == "syncSources") {
            def sourceIDs = lanMessage.json.sourceIDs.toSet()

            log.info("Syncing device sources from '${containerID}' for ${sourceIDs.size()} sources...")

            getChildDeviceInfos()
                .findAll { it.path.targetContainerID == containerID }
                .findAll { !sourceIDs.contains(it.path.sourceContainerID) }
                .each { tryDeleteChildDevice(it.device.deviceNetworkId) }

            searchForDevices()
        } else if (operation == "syncSourceDevices") {
            def existingDeviceIDChildDeviceMap = getChildDeviceInfos()
                .findAll { it.path.targetContainerID == containerID && it.path.sourceContainerID == lanMessage.json.sourceID }
                .collectEntries { [(it.path.deviceID): it.device] }

            def deviceIDs = lanMessage.json.devices.collect { it.deviceID }.toSet()

            log.info("Syncing devices from '${containerID}' for ${deviceIDs.size()} devices...")

            existingDeviceIDChildDeviceMap
                .findAll { !deviceIDs.contains(it.key) }
                .each { tryDeleteChildDevice(it.value.deviceNetworkId) }

            lanMessage.json.devices.each {
                def existingChildDevice = existingDeviceIDChildDeviceMap[it.deviceID]

                if (existingChildDevice == null) {
                    addChildDevice(it.namespace, it.typeName, containerID + ':' + lanMessage.json.sourceID + ':' + it.deviceID, location.getHubs()[0].id, [name: it.name, label: it.name, completedSetup: true])
                } else {
                	// TODO need to test; seriously doubt this works
                    if (existingChildDevice.name != it.name) {
			            log.info("Renaming device '${existingChildDevice.name}' to '${it.name}'...")

						if (existingChildDevice.name == existingChildDevice.label)
                            existingChildDevice.label = it.name

                        existingChildDevice.name = it.name
                    }
                }
            }
        } else if (operation == "syncDeviceState") {
            def childDevice = getChildDevice(containerID + ':' + lanMessage.json.sourceID + ':' + lanMessage.json.deviceID);

            log.info("Syncing state from '${containerID}' for device '${lanMessage.json.deviceID}' (${childDevice?.name}); setting property '${lanMessage.json.propertyName}' to '${lanMessage.json.propertyValue}'...")

			// so the event coming in to processLocationEvent gets truncated based on:
            // https://community.smartthings.com/t/event-data-limits/154109/14
            // we'll have to figure out another way
            if (lanMessage.json.propertyName == "image" && lanMessage.json.propertyValue != "")
                childDevice?.storeImage(
                    java.util.UUID.randomUUID().toString().replaceAll('-', ''),
                    new ByteArrayInputStream(lanMessage.json.propertyValue.decodeBase64())
                )
            else
                childDevice?.sendEvent(name: lanMessage.json.propertyName, value: lanMessage.json.propertyValue)

            // doesn't seem to care if some handlers don't implement this
            childDevice?.onEventSent(lanMessage.json.propertyName, lanMessage.json.propertyValue);
        } else if (operation == "getDevices") {
            log.info("Getting devices for '${containerID}'...")

            searchForDevices()

            sendMessage(
                containerID,
                [
                    devices: actuators
                        .plus(sensors)
                        .toSet()
                        .collect { [
                            deviceID: it.deviceNetworkId,
                            name: it.label ?: it.name,
                            capabilityNames: it.getCapabilities().collect { it.name }.toList(),
                            propertyNameValueMap: getPropertyNameValueMap(it)
                        ] }
                ]
            )
        } else if (operation == "getDeviceState") {
            log.info("Getting device state for '${containerID}' for device '${lanMessage.json.deviceID}'...")

            def assignedDevice = getAssignedDevice(lanMessage.json.deviceID)

            if (assignedDevice)
                sendMessage(
                    containerID,
                    [
                        deviceID: lanMessage.json.deviceID,
                        propertyNameValueMap: getPropertyNameValueMap(assignedDevice)
                    ]
                )
        } else if (operation == "invokeDeviceCommand") {
            log.info("Invoking device command for '${containerID}' for device '${lanMessage.json.deviceID}', command '${lanMessage.json.commandName}'...")
            getAssignedDevice(lanMessage.json.deviceID)?."${lanMessage.json.commandName}"()
        } else if (operation == "subscribeEvents") {
            log.info("Setting subscription for'${containerID}'...")
            getSubscribeEventsMap()[containerID] = new Date(new Date().getTime() + lanMessage.json.expirationMillisecondCount).getTime()
        }
    }
}

def requestChildStateChange(childDeviceNetworkId, propertyName, propertyValue) {
    log.info("Requesting state change for '$childDeviceNetworkId' for property '$propertyName' to '$propertyValue'...")

    def dniPath = parseDniPath(childDeviceNetworkId)
    sendMessage(dniPath.targetContainerID, [sourceID: dniPath.sourceContainerID, deviceID: dniPath.deviceID, propertyName: propertyName, propertyValue: propertyValue])
}

def requestChildRefresh(childDeviceNetworkId) {
    log.info("Requesting refresh for '$childDeviceNetworkId'...")

    def dniPath = parseDniPath(childDeviceNetworkId)
    sendMessage(dniPath.targetContainerID, [sourceID: dniPath.sourceContainerID, deviceID: dniPath.deviceID])
}

def computeSignatureString(validationKeyString, dateString, bodyString) {
    if (validationKeyString == null)
        return "";

    def hmacAlgorithm = javax.crypto.Mac.getInstance("HmacSHA256")
    hmacAlgorithm.init(new javax.crypto.spec.SecretKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(validationKeyString), "HmacSHA256"))

    def signatureSource = "$dateString$bodyString".getBytes("UTF-8")
    def signatureBytes = hmacAlgorithm.doFinal(signatureSource)

    return org.apache.commons.codec.binary.Base64.encodeBase64String(signatureBytes)
}

def sendMessage(containerID, message) {
    def dateString = new Date().format("EEE, dd MMM yyyy HH:mm:ss z", TimeZone.getTimeZone("UTC"))

    def bodyString = new groovy.json.JsonBuilder([
        containerID: containerID,
        message: message,
    ]).toString()
    
    def host = getContainerHostMap()[containerID] + ":040B" // port 1035
    def signatureString = computeSignatureString(getContainerValidationKeyStringMap()[containerID], dateString, bodyString)
    
    //log.info("Sending message of ${bodyString.length()} chars on date '${dateString}' to '${host}': ${bodyString}")
	//log.info("computeSignatureString(${getContainerValidationKeyStringMap()[containerID]}, ${dateString}, ${bodyString})=${signatureString}")

    sendHubCommand(new physicalgraph.device.HubAction([
        method: 'POST',
        path: "/container/" + containerID,
        headers: [
            Host: host,
            Date: dateString,
            Authorization: signatureString,
        ],
        body: bodyString
    ]))
}
