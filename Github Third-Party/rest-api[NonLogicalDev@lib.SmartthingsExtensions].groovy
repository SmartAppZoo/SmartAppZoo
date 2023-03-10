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
 */

definition(
        name: "REST Api",
        namespace: "nonlogical",
        author: "Oleg Utkin",
        description: "Rest API to my device information.",
        category: "SmartThings API",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)

//------------------------------------------------------------
// Pages
//------------------------------------------------------------

preferences {
    page(name:"pageMain")
}

def pageMain() {
    return dynamicPage(name:"pageMain", title:"", install:true, uninstall:true) {
        section("Devices:") {
            input "devices", "capability.sensor", title: "Devices:", multiple: true
        }
        section("LoggerHost:") {
            input "lanIP",   "text", title: "Lan IP:"
            input "lanPORT", "text", title: "Lan PORT:"
        }
    }
}

def installed() {
    if (!state.accessToken) {
        createAccessToken()
    }
}

def uninstalled() {
    if (state.accessToken) {
        revokeAccessToken()
    }
}

//------------------------------------------------------------
// API Definition
//------------------------------------------------------------

mappings {
    path("/devices")                    { action: [ GET:  "api_listDevices" ] }
    path("/device/:id")                 { action: [ GET:  "api_deviceDetails" ]}
    path("/device/:id/attribute/:name") { action: [ GET:  "api_deviceGetAttributeValue" ] }
    path("/device/:id/command/:name")   { action: [ POST: "api_deviceCommand" ] }
}

def api_listDevices() {
    return devices.collect { device ->
        def dMap = serializeDevice(device)
        dMap.events = listDeviceEvents(device.id, 10).collect { evt ->
            serializeEvent(evt)
        }
        return dMap
    }
}

def api_deviceDetails() {
    def device = getDeviceById(params.id)

    def dMap = serializeDevice(device)
    dMap.events = listDeviceEvents(device.id, 100000).collect { evt ->
        serializeEvent(evt)
    }

    return dMap
}

def api_deviceGetAttributeValue() {
    def aVal = deviceGetAttributeValue(params.id, params.name)
    return aVal
}

def api_deviceCommand() {
    return deviceCommand(params.id, params.name, params.arg)
}

//------------------------------------------------------------
// Lookup Methods
//------------------------------------------------------------

def getDeviceById(p_id) {
    return devices.find { it.id == p_id }
}

def getDeviceAttributeValue(p_id, p_name) {
    return getDeviceById(p_id).currentValue(p_name)
}

def getDeviceEvents(p_id, p_max_evt) {
    return getDeviceById(p_id).events(max: p_max_evt)
}

//------------------------------------------------------------
// Commands
//------------------------------------------------------------

def deviceCommand(p_id, p_name, p_arg) {
    def device = getDeviceById(p_d)
    def name = p_name
    def args = p_arg

    if (args == null) {
        args = []
    } else if (args instanceof String) {
        args = [args]
    }

    log.debug "device command: ${name} ${args}"
    switch(args.size) {
        case 0:
            device."$name"()
            break;
        case 1:
            device."$name"(args[0])
            break;
        case 2:
            device."$name"(args[0], args[1])
            break;
        default:
            throw new Exception("Unhandled number of args")
    }
}

//------------------------------------------------------------
// Serialization Methods
//------------------------------------------------------------

static def serializeDevice(device) {
    return [
            id: device.id,
            name: device.name,
            label: device.label,

            display_name: device.displayName,

            make: device.manufacturerName,
            model: device.modelName,

            attributes: device.supportedAttributes.collect {
                it.name
            },

            commands: device.supportedCommands.collect {[
                    name: it.name,
                    args: it.arguments.collect {[
                            "" + it
                    ]}
            ]},
    ]
}

static def serializeEvent(e) {
    return [
            id: "${e.id}",

            device_name: e.device.label,
            device_id:   e.device.id,

            sdate:  e.isoDate,
            rdate:  e.date.time,

            metric: e.name,
            rvalue: e.value,
            svalue: e.stringValue,
    ]
}
