/**
 *  dashful
 *
 *  Copyright 2017 Titus Woo
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
    name: "test1",
    namespace: "tituswoo",
    author: "Titus Woo",
    description: "Simpler automation",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "deviceSelectionPage", title: "Device Selection", nextPage: "authorizationPage", uninstall: true) {
        section("Allow Homeflow to control these devices") {
            input "lights", "capability.switch", title: "Lights...", multiple: true, required: false
            // input "Actuator", "capability.actuator", title: "Devices", multiple: true, required: false
            // input "Sensor", "capability.sensor", title: "Sensors", multiple: true, required: false
        }
    }

    page(name: "authorizationPage", title: "Connect your SmartThings account", install: true, uninstall: true)
}

def authorizationPage() {
    def accountCode = authAccessToken()
    dynamicPage(name: "authorizationPage") {
        section("Get Setup") {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                  title: "$accountCode",
                  required: false,
                  "Enter this code on your Homeflow dashboard."
        }
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    subscribeToAll()
}

def authAccessToken() {
    if (!state.accessToken) {
        state.accessToken = createAccessToken()
    }
    def json = new groovy.json.JsonBuilder([
        accessToken: state.accessToken,
        hubId: location.hubs[0].id
    ])
    def params = [
        uri: "http://2277f470.ngrok.io/api/auth/smartthings/token",
        body: json
    ]

    try {
        httpPostJson(params) { resp -> 
            return resp.data.accountCode
        }
    } catch (e) {
      log.error "authAccessToken: $e"
    }
}

def subscribeToAll() {
    List<String> attributes = []

	settings.each { inputName, devicesInInput ->
        devicesInInput.each { device ->
        	device.supportedAttributes.each { attrName ->
            	attributes.push("$attrName")
            }
        }
    }

    def allAttributes = attributes.toSet()
    // TODO: sub to events individually: https://github.com/LeBlaaanc/SmartTiles/blob/master/SmartTiles.groovy#L466
    allAttributes.each { attrName ->
    	subscribe(Actuator, "$attrName", eventHandler, [filterEvents: false])
        subscribe(Sensor, "$attrName", eventHandler, [filterEvents: false])
    }

    // subscribe(lights, "switch.on", eventHandler)
	// subscribe(lights, "switch.off", eventHandler)
}

def getAllDevices() {
	def devices = []
    for (String key : settings.keySet()) {
    	settings["$key"].each { device ->
            devices << device
        }
    }

	return devices
}

def getAllDevicesAndMassage() {
  def devices = getAllDevices()
  def devicesList = []

  devices.each { device ->

    def deviceObject = [:]

    deviceObject.name = device.name
    deviceObject.id = device.id
    deviceObject.label = device.label
    deviceObject.displayName = device.displayName
    deviceObject.capabilities = device.capabilities.collect { c -> c.name }

    device.supportedAttributes.each { attribute ->
      def currentAttributeName = "current${attribute.name.capitalize()}"
      deviceObject["$attribute.name"] = device["$currentAttributeName"]
    }

    if (!devicesList.findAll { it.id == device.id }) {
    	devicesList.push(deviceObject)
    }
  }

  return devicesList
}

def eventHandlerHandler () {
    def devices = getAllDevicesAndMassage()
    def json = groovy.json.JsonOutput.toJson(devices)
    def data = [
        devices: json,
        hubId: location.hubs[0].id
    ]

    def params = [
      uri: "http://2277f470.ngrok.io/api/smartthings/update",
      body: data
    ]
    
    log.debug "making POST request to server"

    log.info "sending update..."
    log.info data
    log.info "========="

    try {
    //   httpPostJson(params)
        httpPostJson(params) { resp -> 
            log.debug resp.data
        }
    } catch (e) {
      log.error "eventHandler: $e"
    }
}

def getDevice(id) {
	def devices = getAllDevices()
    def device = devices.find { it.id == id }
    if (!device) {
    	return false
    }
    return device
}

def eventHandler(event) {
    // runIn(1, eventHandlerHandler, [overwrite: true])
    eventHandlerHandler()
}

def getDevicesList() {
    def devices = getAllDevicesAndMassage()
    def json = groovy.json.JsonOutput.toJson(devices)
    render data: json, status: 200
}

def updateDevice() {
	def deviceId = params.id
    def command = request.JSON?.command

    if (!deviceId) {
    	httpError(400, "deviceId parameter is required.")
    }

    def device = getDevice(deviceId)

    if (!device) {
    	httpError(500, "Could not find device with id ${deviceId}")
    }

    try {
      switch (command) {
        case "setLevel":
          def level = request.JSON?.level
          device.on()
          device.setLevel(level)
          break
        case "setHue":
          def hue = request.JSON?.hue
          device.on()
          device.setHue(hue)
          break
        default:
          device."$command"()
      }
    } catch (e) {
    	httpError(500, "Could not execute command '${command}' to device with id ${deviceId}")
    }
}

mappings {
	path("/devices") {
    	action: [
        	GET: "getDevicesList",
        ]
    }
    path("/device/:id") {
    	action: [
        	PUT: "updateDevice"
        ]
    }
}
