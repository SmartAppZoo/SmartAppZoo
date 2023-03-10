/**
*  DSC Alarm Manager (Test)
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
    name: "DSC Alarm Manager (Test)",
    namespace: "TenStartups",
    author: "Marc Lennox (marc.lennox@gmail.com)",
    description: "Integrate SmartThings with a DSC Alarm panel in order to control and receive events from your alarm system.",
    category: "My Apps",
    iconUrl: "http://luckyscontractors.com/photo/mwsecurityproscom/home-security-professionals.jpg",
    iconX2Url: "http://luckyscontractors.com/photo/mwsecurityproscom/home-security-professionals.jpg",
    iconX3Url: "http://luckyscontractors.com/photo/mwsecurityproscom/home-security-professionals.jpg")

preferences {
    page(name: "deviceDiscovery", title: "DSC Alarm Discovery", content: "deviceDiscovery")
}

mappings {
    path("/update") {
        action: [POST: "processUpdate"]
    }
}

def ssdpUSN() {
    return "urn:schemas-upnp-org:service:DSCAlarmManager:11"
}

def deviceDiscovery() {
    def partitionDevices = partitionChoices()
    def contactZoneDevices = contactZoneChoices()
    def motionZoneDevices = motionZoneChoices()

    ssdpSubscribe()
    ssdpDiscover()

    return dynamicPage(name: "deviceDiscovery", title: "Started DSC Alarm device discovery...", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
        section("Please wait while we discover your DSC Alarm devices. Select the devices you want to control in SmartThings below once they have been discovered.") {
		    input("userCode", "password", title: "Alarm user code", description: "Alarm keypad user code", required: true)
            input "selectedPartitions", "enum", required: false, title: "Partition devices (${partitionDevices.size() ?: 0} found)", multiple: true, submitOnChange: true, options: partitionDevices
            input "selectedContactZones", "enum", required: false, title: "Contact zone devices (${contactZoneDevices.size() ?: 0} found)", multiple: true, submitOnChange: true, options: contactZoneDevices
            input "selectedMotionZones", "enum", required: false, title: "Motion zone devices (${motionZoneDevices.size() ?: 0} found)", multiple: true, submitOnChange: true, options: motionZoneDevices
        }
    }
}

def installed() {
    log.debug("Installed with settings: ${settings}")
    createAccessToken()
    initialize()
}

def updated() {
    log.debug("Updated with settings: ${settings}")
    unsubscribe()
    initialize()
}

def uninstalled() {
    revokeAccessToken()
}

def initialize() {
    unsubscribe()
    unschedule()
    ssdpSubscribe()
    createSelectedDevices()
    deleteUnselectedDevices()

	getChildDevices().findAll { it.name == 'DSC Alarm Partition' }.each { partitionDevice ->
	    syncDeviceDataValue(partitionDevice, "userCode", settings.userCode)
    }

	subscribe(location, "alarmSystemStatus", alarmStatusHandler)

    runEvery5Minutes("ssdpDiscover")
}

def alarmStatusHandler(evt) {
    log.debug "Alarm system status is ${evt.value}"
    getChildDevices().findAll { it.name == 'DSC Alarm Partition' && it.integrateSHM() }.each { partitionDevice ->
        if (evt.value == 'off') {
            partitionDevice.disarm()
        } else if (evt.value == 'stay') {
            partitionDevice.armStay()
        } else if (evt.value == 'away') {
            partitionDevice.armAway()
        }
    }
}

def selectedDevices() {
    def selected = []
    if (selectedPartitions) {
        selected += selectedPartitions
    }
    if (selectedContactZones) {
        selected += selectedContactZones
    }
    if (selectedMotionZones) {
        selected += selectedMotionZones
    }
    selected
}

def discoveredPartitionDevices() {
    if (!state.discoveredPartitionDevices) {
        state.discoveredPartitionDevices = [:]
    }
    state.discoveredPartitionDevices
}

def discoveredContactZoneDevices() {
    if (!state.discoveredContactZoneDevices) {
        state.discoveredContactZoneDevices = [:]
    }
    state.discoveredContactZoneDevices
}

def discoveredMotionZoneDevices() {
    if (!state.discoveredMotionZoneDevices) {
        state.discoveredMotionZoneDevices = [:]
    }
    state.discoveredMotionZoneDevices
}

Map partitionChoices() {
    def map = [:]
    discoveredPartitionDevices().sort({ it.value.label }).each {
        map[it.value.dni] = it.value.label
    }
    map
}

Map contactZoneChoices() {
    def map = [:]
    discoveredContactZoneDevices().sort({ it.value.label }).each {
        map[it.value.dni] = it.value.label
    }
    map
}

Map motionZoneChoices() {
    def map = [:]
    discoveredMotionZoneDevices().sort({ it.value.label }).each {
        map[it.value.dni] = it.value.label
    }
    map
}

void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.${ssdpUSN()}", ssdpDiscoveryHandler)
}

void ssdpDiscover() {
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${ssdpUSN()}", physicalgraph.device.Protocol.LAN))
}

void ssdpDiscoveryHandler(event) {
    def parsedEvent = parseLanMessage(event?.description)
    String ipAddress = convertHexToIP(parsedEvent?.networkAddress)
    int ipPort = convertHexToInt(parsedEvent?.deviceAddress)
    sendHubCommand(
        new physicalgraph.device.HubAction(
            """GET ${parsedEvent.ssdpPath} HTTP/1.1\r\nHOST: ${ipAddress}:${ipPort}\r\n\r\n""",
            physicalgraph.device.Protocol.LAN,
            null,
            [ callback: ssdpDescriptionHandler ]
        )
    )
}

void ssdpDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
    log.debug("Received SSDP description response")

    def discoveredDevices = hubResponse.json?.device

    def discoveredPartitions = [:]
    def discoveredContactZones = [:]
    def discoveredMotionZones = [:]

    discoveredDevices.each { device ->
        def deviceAttrs = [
            hubId: hubResponse.hubId,
            dni: device.network_id,
            label: device.name,
            externalId: device.id,
            externalUid: device.uid,
            ipAddress: device.ip_address,
            ipPort: device.ip_port,
        ]
        switch (device.type) {
            case 'Partition':
            deviceAttrs << [ name: 'DSC Alarm Partition', handler: 'DSC Alarm Partition' ]
            discoveredPartitions[device.network_id] = deviceAttrs
            break
            case 'ContactZone':
            deviceAttrs << [ name: 'DSC Alarm Contact Zone', handler: 'DSC Alarm Contact Zone' ]
            discoveredContactZones[device.network_id] = deviceAttrs
            break
            case 'MotionZone':
            deviceAttrs << [ name: 'DSC Alarm Motion Zone', handler: 'DSC Alarm Motion Zone' ]
            discoveredMotionZones[device.network_id] = deviceAttrs
            break
        }
        def childDevice = getChildDevice(deviceAttrs.dni)
        if (childDevice) {
            refreshChildDevice(childDevice, deviceAttrs)
        }
    }

    // Reset state maps
    state.discoveredPartitionDevices = discoveredPartitions
    state.discoveredContactZoneDevices = discoveredContactZones
    state.discoveredMotionZoneDevices = discoveredMotionZones
}

void refreshChildDevice(childDevice, deviceAttrs) {
    syncDeviceNameAndLabel(childDevice, deviceAttrs.name, deviceAttrs.label)
    syncDeviceDataValue(childDevice, "externalId", deviceAttrs.externalId)
    syncDeviceDataValue(childDevice, "externalUid", deviceAttrs.externalUid)
    syncDeviceDataValue(childDevice, "ipAddress", deviceAttrs.ipAddress)
    syncDeviceDataValue(childDevice, "ipPort", deviceAttrs.ipPort)
}

void updateChildDeviceToken(childDevice) {
    sendHubCommand(
        new physicalgraph.device.HubAction(
            method: "POST",
            path: "/api/device/${childDevice.getDataValue("externalId")}/token/${state.accessToken}",
            headers: [ HOST: "${childDevice.getDataValue("ipAddress")}:${childDevice.getDataValue("ipPort")}" ]
        )
    )
}

void deleteChildDeviceToken(childDevice) {
    sendHubCommand(
        new physicalgraph.device.HubAction(
            method: "DELETE",
            path: "/api/device/${childDevice.getDataValue("externalId")}/token",
            headers: [ HOST: "${childDevice.getDataValue("ipAddress")}:${childDevice.getDataValue("ipPort")}" ]
        )
    )
}

def createSelectedDevices(devices) {
    def selectedDevices = selectedPartitions.collect { dni -> discoveredPartitionDevices()[dni] } +
        selectedContactZones.collect { dni -> discoveredContactZoneDevices()[dni] } +
            selectedMotionZones.collect { dni -> discoveredMotionZoneDevices()[dni] }

    selectedDevices.each { device ->
        def childDevice = getChildDevice(device.dni)
        if (!childDevice) {
            log.debug("Adding child device ${device.label}")
            def data = [
                "externalId": device.externalId,
                "externalUid": device.uid,
                "ipAddress": device.ipAddress,
                "ipPort": device.ipPort,
            ]
            if (device.handler == 'DSC Alarm Partition') {
                data["userCode"] = settings.userCode
            }
            childDevice = addChildDevice(
                "TenStartups", device.handler, device.dni, device.hubId, [
                    "name": device.name,
                    "label": device.label,
                    "data": data,
                    completedSetup: true
                ]
            )
        }
        updateChildDeviceToken(childDevice)
        childDevice.refresh()
    }
}

def deleteUnselectedDevices(devices) {
    (getChildDevices().collect { it.deviceNetworkId } - selectedDevices()).each { dni ->
        def childDevice = getChildDevice(dni)
        if (!childDevice) {
            return
        }
        log.debug("Removing child device ${childDevice.label}")
        deleteChildDeviceToken(childDevice)
        deleteChildDevice(dni)
    }
}

def syncDeviceNameAndLabel(device, name, label) {
    if (name && device.name != name) {
        log.debug("Changing device name from '${device.name}' to '${name}'")
        device.name = name
    }
    if (label && device.label != label) {
        log.debug("Changing device label from '${device.label}' to '${label}'")
        device.label = label
    }
}

def syncDeviceDataValue(device, name, value) {
    if (value && device.getDataValue(name) != value) {
        log.debug("Changing data value '${name}' name from '${device.getDataValue(name)}' to '${value}'")
        device.updateDataValue(name, value)
    }
}

def processUpdate() {
    log.debug("Received update message ${request.JSON}")

    if (!request.JSON?.device?.network_id) {
        return httpError(422, "Missing device attributes in update message")
    }

    if (!request.JSON?.data) {
        return httpError(400, "Missing data in update message")
    }

    def childDevice = getChildDevice(request.JSON.device.network_id)

	if (request.JSON.data != null) {
        if (childDevice) {
            childDevice.processStatusUpdate(request.JSON.data)
            return [ status: 'OK', data: request.JSON.data ]
        } else {
            log.debug("Device ${request.JSON.device.network_id} not found, deleting remote token")
            return httpError(404, "Device with network ID ${request.JSON.device.network_id} does not exist")
        }
    } else {
        log.debug("Update data empty")
        return httpError(404, "Update data empty")
    }
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
