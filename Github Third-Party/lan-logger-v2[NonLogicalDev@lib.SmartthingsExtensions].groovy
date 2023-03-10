/**
 *  REST Api
 *
 *  Copyright 2018 Oleg Utkin
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
 *  Loosely modelled after:
 *    https://github.com/codersaur/SmartThings/blob/master/smartapps/influxdb-logger/influxdb-logger.groovy
 */

definition(
    name: "LAN Logger V2",

    namespace: "nonlogical",
    author: "Oleg Utkin",

    description: "Logs data from sensors to a lan server.",
    category: "SmartThings Labs",

    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX4Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)

preferences {
    page(name:"pageMain")
}

//------------------------------------------------------------
// Pages
//------------------------------------------------------------

def pageMain() {
    return dynamicPage(name:"pageMain", title:"", install:true, uninstall:true) {
        section("Devices:") {
            input "devices", "capability.sensor", title: "Devices:", multiple: true
        }
        section("LoggerHost:") {
            input "lanIP", "text", title: "Lan IP:"
            input "lanPORT", "text", title: "Lan PORT:"
            input "lanPATH", "text", title: "Lan PATH:"
        }
    }
}

//------------------------------------------------------------
// IO Handler
//------------------------------------------------------------

def lanRequest(method, hIP, hPort, hPath, data) {
    sendHubCommand(new physicalgraph.device.HubAction([
        method: method,

        path:   hPath,
        headers: [
            "HOST": "${hIP}:${hPort}",
        ],

        body: data,
    ], null, [ callback: lanResponse ]))
}

def lanResponse(physicalgraph.device.HubResponse hubResponse) {
    log.debug "RES:HEAD: ${hubResponse.headers}"
    log.debug "RES:BODY: ${hubResponse.body}"
}

def postLANMessage(data) {
    try {
        lanRequest("POST", state.lanIP, state.lanPORT, state.lanPATH, data)
    } catch (all) {
        log.debug "OOPS"
    }
}

//------------------------------------------------------------
// Lifecycle
//------------------------------------------------------------

def installed() {
    log.debug "[LFC] INSTALLING..."
    saveSettings()

    postLANMessage([
        type: "state", data: "installed"
    ])
}

def updated() {
    log.debug "[LFC] UPDATING..."
    saveSettings()

    postLANMessage([
        type: "state", data: "updated"
    ])
}

def uninstalled() {
    log.debug "[LFC] UNINSTALLING..."
    state.subs = []

    postLANMessage([
        type: "state", data: "uninstalled"
    ])
}

def onPoll() {
    log.debug "POLLING..."
    state.subs.each { s ->
        onDevicePoll(getDeviceById(s.deviceId), s.capName, s.attrName)
    }
}

//------------------------------------------------------------
// HELPERS
//------------------------------------------------------------

def formatISODate(date) {
    return date.format( "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" )
}

def getCapacityForAttr(devId, attrName) {
    return state.handlers["${devId}:${attrName}"]
}

def setCapacityForAttr(devId, attrName, capName) {
    state.handlers["${devId}:${attrName}"] = capName
}

def clearCapacityForAttrs() {
    state.handlers = [:]
}

def getDeviceById(p_id) {
    return settings.devices.find { it.id == p_id }
}

//------------------------------------------------------------

def saveSettings() {
    // Set LAN Connection Properties.
    log.debug "SAVE.SETTINGS: ${settings}"
    state.lanIP = settings.lanIP
    state.lanPORT = settings.lanPORT
    state.lanPATH = settings.lanPATH

    // Set subscriptions.
    log.debug "SAVE.SUBSCRIPTIONS::"
    state.subs = connectToDevices(settings.devices)
    postLANMessage([
        type: "subscribe",
        data: state.subs.collect {
            ["${getDeviceById(it.deviceId).id}", "${it.capName}:${it.attrName}"]
        },
    ])

    // Set schedule.
    log.debug "SAVE.SCHEDULE::"
    try {
        unschedule()
    } catch (all) {}
    runEvery5Minutes("onPoll")
    log.debug "SAVE.DONE::"
}

def connectToDevices(devices) {
    unsubscribe()
    clearCapacityForAttrs()

    def subs = []

    devices.each { dev ->
        def sdev = deviceObjectToMap(dev, true)

        log.debug("DEVTYPE: ${sdev.type}")
        sdev.capabilities.each { cap ->
            cap.attributes.each { attr ->
                log.debug "SUBSCRIBING: ${dev}:{${cap.name}}:(${attr})"
                setCapacityForAttr(dev.id, attr, cap.name)
                subscribe(dev, attr, onDeviceEvent)

                subs.add([
                    deviceId: dev.id,
                    attrName: attr,
                    capName: cap.name
                ])
            }
        }
    }

    return subs
}

//------------------------------------------------------------
// Event Handling
//------------------------------------------------------------

def onDevicePoll(device, capName, attrName) {
    def evt = serializePoll(device, capName, attrName)
    if (evt == null) {
        log.debug "POLLING_EVENT:SKIP ${device.label}::${capName}:${attrName} :: No Data"
        return
    }

    log.debug "POLLING_EVENT: ${device.label}::${capName}:${attrName} :: ${evt}"
    postLANMessage([
        type: "event",
        data: evt,
    ])
}

def onDeviceEvent(e) {
    def attrName = e.name
    def capName = getCapacityForAttr(e.device.id, attrName)
    def evt = serializeEvent(e, capName)

    log.debug "DEVICE_EVENT: ${e.device.label}::${capName}:${attrName} :: ${evt}"
    postLANMessage([
        type: "event",
        data: evt,
    ])
}

//------------------------------------------------------------
// Serialization Methods
//------------------------------------------------------------

def serializeEvent(e, capName) {
    def attrName = e.name

    return [
        kind: "event",

        id:   "" + e.id,
        ts:   e.date.time,
        date: formatISODate(e.date),

        device: deviceObjectToMap(e.device, false),
        metric: decodeMetric(e, capName, attrName),
    ]
}

def serializePoll(device, capName, attrName) {
    def id = UUID.randomUUID()
    def date = new Date()
    def state = device.latestState(attrName)

    if (state == null) return null

    return [
        kind: "poll",

        id:   "" + id,
        ts:   date.time,
        date: formatISODate(date),

        device: deviceObjectToMap(device, false),
        metric: decodeMetric(state, capName, attrName),
    ]
}

//------------------------------------------------------------
// Simplifying Methods
//------------------------------------------------------------

def hubObjectToMap(h) {
    return [
        id: "" + h.id,

        name: h.name,
        status: h.status,
        localIP: h.localIP,

        onBatteryPower: h.hub.getDataValue("batteryInUse"),
        uptime: h.hub.getDataValue("uptime"),

        fw: h.firmwareVersionString,
    ]
}

def deviceObjectToMap(d, full=true) {
    def output = [
        id:    "" + d.id,

        name:  d.name,
        label: d.label,
        display: d.displayName,

        make:  d.manufacturerName,
        model: d.modelName,
        type:  d.typeName,

        groupId: d?.device?.groupId,
    ]

    if (full) {
        output << [
            capabilities: d.capabilities.collect {[
                name: it.name.toLowerCase().replace(" ", "-"),

                attributes: it.attributes.collect {
                    it.name
                },

                commands: it.commands.collect {[
                    name: it.name,
                    args: it.arguments.collect {[
                        "" + it
                    ]}
                ]},
            ]},

            allAttributes: d.supportedAttributes.collect {
                it.name
            },

            allCommands: d.supportedCommands.collect {[
                name: it.name,
                args: it.arguments.collect {[
                    "" + it
                ]}
            ]},
        ]
    }

    return output
}

//------------------------------------------------------------
// Value Decoding
//------------------------------------------------------------

def decodeMetric(state, capName, metricName) {
    def output = [:]

    // Fully qualified metric name if available.
    if (capName != null) {
        output.name = "${capName}.${metricName}"
    } else {
        output.name = metricName
    }

    def dValue = translateMetricValue(state, metricName)

    output << [
        "unit":      state.unit,
        "str-value": state.value,

        "type":  dValue.type,
        "value": dValue.value,
    ]

    return output
}

def translateMetricValue(e, metricName) {
    def sv = decodeStateValue(e)

    def type = sv.type
    def value = sv.value

    switch (metricName) {

    ///// LOGICAL:
    //--------------------------------------------------------------------------------/
    // acceleration (L)
    // contact (L)
    // motion (L)
    // presence (L)
    // status (L)
    // switch (L)
    // tamper (L)
    // thermostatMode (L)
    // thermostatOperatingState (S)
    //--------------------------------------------------------------------------------/
        case "acceleration":
            type = "boolean"
            value = (value == "active") ? true : false
            break

        case "contact":
            type = "boolean"
            value = (value == "open") ? true : false
            break

        case "motion":
            type = "boolean"
            value = (value == "active") ? true : false
            break

        case "presence":
            type = "boolean"
            value = (value == "present") ? true : false
            break

        case "switch":
            type = "boolean"
            value = (value == "on") ? true : false
            break

        case "tamper":
            type = "boolean"
            value = (value == "detected") ? true : false
            break

    ///// Raw Value Otherwise:
    //--------------------------------------------------------------------------------/
        default:
            break
    }

    return [
        type: type,
        value: value,
    ]
}

def decodeStateValue(e) {
    try {
        return [
            type: "numeric",
            value: e.doubleValue,
        ]
    } catch (all) {}

    try {
        return [
            type: "vector",
            value: e.xyzValue,
        ]
    } catch (all) {}

    if (e.dateValue != null) {
        return [
            type: "date",
            value: e.dateValue,
        ]
    }

    return [
        type: "string",
        value: e.value,
    ]
}

