/**
 *  Heating Control
 *
 *  Copyright 2017 Anders Sveen &lt;anders@f12.no&gt;
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
 * TODO:
 * - Devices on removal of rooms?
 *
 */
definition(
        name: "Heating Control",
        namespace: "smartthings.f12.no",
        author: "Anders Sveen <anders@f12.no>",
        description: "Manages heating for several rooms and several modes.",
        category: "Green Living",
        iconUrl: "http://www.iconsdb.com/icons/preview/soylent-red/temperature-xxl.png",
        iconX2Url: "https://cdn.thinglink.me/api/image/900776185863077889/1240/10/scaletowidth",
        iconX3Url: "https://cdn.thinglink.me/api/image/900776185863077889/1240/10/scaletowidth")


preferences {
    page(name: "setupPage")
}

def setupPage() {
    if (!settings["numberOfRooms"]) {
        settings["numberOfRooms"] = 1
    }
    dynamicPage(name: "setupPage", title: "Set up heat control", install: true, uninstall: true) {
        section("General options and defaults", hideable: true) {
            input "numberOfRooms", "number", title: "Number of rooms", defaultValue: 1, submitOnChange: true
            paragraph "This temperature will be used across all rooms if no more specific setting is found."
            input "defaultMainTemp", "decimal", title: "Default thermostat temperature"
        }
        section("The separate rooms are listed below. If only sensor and switches are specified in a room, the defaults above will be used.")
        (1..settings["numberOfRooms"]).each { roomNumber ->
            def modeSections = modeCountForRoom(roomNumber)
            if (modeSections == null) {
                modeSections = 1
            }
            def roomTitle = "Unnamed room"
            def hidden = false
            if (settings["room${roomNumber}Name"]) {
                roomTitle = settings["room${roomNumber}Name"]
                hidden = true
            }
            section("Room: ${roomTitle}", hideable: true, hidden: hidden) {
                input "room${roomNumber}Name", "text", title: "Name", description: "Name for convenience"
                input "room${roomNumber}Sensor", "capability.temperatureMeasurement", title: "Temperature Sensor"
                input "room${roomNumber}Switches", "capability.switch", title: "Switches to manage", multiple: true
                input "room${roomNumber}MainTemp", "decimal", title: "Thermostat temperature", required: false, description: "The desired temperature for the room."

                (1..modeSections).each { modeNumber ->
                    paragraph " ===   Mode specific temperature   ==="
                    input "room${roomNumber}Mode${modeNumber}Modes", "mode", title: "Modes for alternative temperature", required: false, multiple: true
                    input "room${roomNumber}Mode${modeNumber}Temp", "decimal", title: "Alternative temperature", required: false, submitOnChange: true
                }
                input "room${roomNumber}Mode${modeSections + 1}Modes", "mode", title: "(+) Select modes here to add more sections", required: false, multiple: true, submitOnChange: true
            }
        }
    }
}

def modeCountForRoom(roomNumber) {
    return settings.count { key, value -> key.startsWith("room${roomNumber}Mode") && key.endsWith("Modes") && !value.empty }
}

def settingsToRooms() {
    def roomMap = [:]
    (1..settings["numberOfRooms"]).each { int roomNumber ->
        def currentRoomMap = [:]
        def modesMap = [:]
        currentRoomMap["modes"] = modesMap
        currentRoomMap["Number"] = roomNumber

        settings
                .findAll { key, value -> key.startsWith("room${roomNumber}") }
                .each { key, value ->
                    if (key.startsWith("room${roomNumber}Mode")) {
                        // TODO This will fail if modes > 9
                        int modeNumber = Integer.parseInt(key.replaceAll("room${roomNumber}Mode", "").take(1))
                        def attributeName = key.replaceAll("room${roomNumber}Mode${modeNumber}", "")
                        if (!modesMap.containsKey(modeNumber)) {
                            modesMap[modeNumber] = [:]
                        }
                        modesMap[modeNumber][attributeName] = value
                    } else {
                        def attributeName = key.replaceAll("room${roomNumber}", "")
                        currentRoomMap[attributeName] = value
                    }
                }
        roomMap[roomNumber] = currentRoomMap
    }

    return roomMap
}

def Double findDesiredTemperature(Map room, mode) {
    Double desiredTemp = defaultMainTemp
    Double roomModeTemp = null

    room.modes.each { modeNumber, modeSettings ->
        modeSettings.Modes.each { oneMode ->
            if (oneMode.equals(mode)) {
                roomModeTemp = modeSettings.Temp
            }
        }
    }

    if (roomModeTemp) {
        log.debug("Selected temp based on mode (${location.currentMode.name}) for room '${room.Name}'")
        desiredTemp = roomModeTemp
    } else if (room.MainTemp) {
        log.debug("Selected temp based on default for room for room '${room.Name}'")
        desiredTemp = room.MainTemp
    } else {
        log.debug("Selected default value for any room for room '${room.Name}'")
    }

    return desiredTemp
}

def modeHandler(event) {
    log.debug("Received mode change event. Setting temp and evaluating all rooms.")
    settingsToRooms().each { key, room ->
        def newSetpoint = findDesiredTemperature(room, location.currentMode.name)
        def thermostat = getThermostateDeviceForRoom(key)
        thermostat.updateMode("idle", newSetpoint)
        thermostat.evaluate(room.Switches)
    }
}

def temperatureHandler(evt) {
    settingsToRooms()
            .findAll { key, room -> room.Sensor.toString().equals(evt.getDevice().toString()) }
            .each { key, room ->
                log.debug("Found sensor ${room.Sensor} for ${room.Name}, handling...")
                def thermostat = getThermostateDeviceForRoom(key)
                thermostat.updateTemperature(evt.value)
                thermostat.evaluate(room.Switches)
            }
}

def evaluateAllRooms(child) {
    settingsToRooms().each { key, room ->
        def thermostat = getThermostateDeviceForRoom(key)
        thermostat.evaluate(room.Switches)
    }
}

def deleteChildDevices() {
    if (getChildDevices().size > 0) {
        getChildDevices().each { device ->
            deleteChildDevice(device.deviceNetworkId)
        }
    }
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def uninstalled() {
    deleteChildDevices()
}

def initialize() {
    subscribe(location, modeHandler)
    settingsToRooms().each { roomNumber, room ->
        subscribe(room.Sensor, "temperature", temperatureHandler)
        log.debug("Subscribed to sensor '${room.Sensor}'")
        def thermostatDevice = getThermostateDeviceForRoom(roomNumber)
        if (!thermostatDevice) {
            addChildDevice(app.namespace, "Heating Control Thermostat", "heating-control-room-${roomNumber}", null, [name: "${room.Name} - Thermostat", room: room])
            thermostatDevice = getThermostateDeviceForRoom(roomNumber)
        }
        log.debug("Added virtual thermostat for ${room.Name}")
        thermostatDevice.updateMode("idle", findDesiredTemperature(room, location.currentMode.name))
        thermostatDevice.evaluate(room.Switches)
    }
}

def getThermostateDeviceForRoom(int roomNumber) {
    return getChildDevice("heating-control-room-${roomNumber}")
}

def getThermostatSetpointForRoom(int roomNumber) {
    return getThermostateDeviceForRoom(roomNumber).currentHeatingSetpoint
}
