/**
 *  JSON API for SmartThings
 *
 *  Copyright 2017 Benjamin Lodi
 *
 */
definition(
  name: "JSON API for SmartThings",
  namespace: "brlodi",
  author: "Benjamin Lodi",
  description: "API for JSON with complete set of devices",
  category: "SmartThings Labs",
  // Sorry in advance to PDLove for temporarily borrowing your icon, and permanently your idea
  iconUrl:   "https://raw.githubusercontent.com/pdlove/homebridge-smartthings/master/smartapps/JSON%401.png",
  iconX2Url: "https://raw.githubusercontent.com/pdlove/homebridge-smartthings/master/smartapps/JSON%402.png",
  iconX3Url: "https://raw.githubusercontent.com/pdlove/homebridge-smartthings/master/smartapps/JSON%403.png",
  oauth: true
)

preferences {
  section("Expose these devices via the API") {
    // paragraph "Some devices (like many virtual switches) may not appear in the \"Most Devices\" list, but you should see them in one of the others."
    // paragraph "If a device appears in more than one list, it is safe to select it multiple times. If you have a very large number of duplicated devices, however, it may start to affect the API's response time."
    input "refreshList", "capability.refresh", title: "Most Devices", multiple: true, required: false
    input "sensorList", "capability.sensor", title: "Other Sensors", multiple: true, required: false
    input "actuatorList", "capability.actuator", title: "Other Actuators", multiple: true, required: false
  }
  
  // section("Configure PubNub") {
  //   input "pubnubSubscribeKey", "text", title: "PubNub Subscription Key", multiple: false, required: false
  //   input "pubnubPublishKey", "text", title: "PubNub Publish Key", multiple: false, required: false
  //   input "subChannel", "text", title: "Channel (Can be anything)", multiple: false, required: false
  // }
}


def initialize() {
  if (!state.accessToken) createAccessToken()
  if (!state.externalSubscribers) state.externalSubscribers = []
  subscribeToAllDeviceEvents()
}

def installed() {
	log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
  initialize()
}


mappings {
    path("/test") { action: [ GET: "apiTest" ] }
    path("/location") { action: [ GET: "apiGetLocation" ] }
    path("/devices") { action: [ GET: "apiGetDevices" ] }
    path("/devices/:deviceId") { action: [ GET: "apiGetDevices" ] }
    path("/devices/:deviceId/attributes") { action: [ GET: "apiGetDeviceAttributes" ] }
    path("/devices/:deviceId/attributes/:attributeName") { action: [ GET: "apiGetDeviceAttribute" ] }
    path("/run") { action: [ PUT: "apiRunCommand" ] }
    path("/subscribers") {
      action: [
        POST: "apiRegisterExternalSubscriber",
        DELETE: "apiRemoveExternalSubscriber"
      ]
    }
}

def getDeviceList() {
  (refreshList + sensorList + actuatorList - null).toSet()
}

def subscribeToAllDeviceEvents() {
  deviceList.each { device ->
    device.supportedAttributes.each { attribute ->
      log.debug "Subscribing to $attribute.name for device $device.displayName"
      subscribe(device, attribute.name, deviceChangeHandler)
    }
  }
}

def deviceChangeHandler(event) {
  log.debug event
}
 
def buildJsonApiTopLevel(metaField, dataField) {
  def response = [:]
  if (metaField) response['meta'] = metaField
  if (dataField) response['data'] = dataField
  return response
}

def apiGetDevices() {
  def targetDevices = []
  if (params.deviceId) {
    targetDevices << deviceList.find { it.id == params.deviceId }
  } else {
    targetDevices.addAll deviceList
  } // TODO: add filtering option
  
  def result = []
  targetDevices.each { 
    if (it) {
      result << [
        id: it.id,
        type: "Device",
        attributes: [
          networkId: it.deviceNetworkId,
          name: it.name,
          displayName: it.displayName,
          status: it.status,
          manufacturer: it.manufacturerName,
          model: it.modelName,
          lastActivity: it.lastActivity,
          capabilities: it.capabilities.collect { capability -> (capability.name) },
          supportedCommands: it.supportedCommands.collect { command -> (command.name) },
          supportedAttributes:it.supportedAttributes.collect { attribute -> (attribute.name) }
        ]
      ]
    } else {
      log.error("Error parsing device ''" + it)
    }
  }
  
  if (params.deviceId) {
    result = result[0]
  }
      
  buildJsonApiTopLevel(null, result)
}

def apiGetLocation() {
  def loc = [
    id: location.id,
    type: "Location",
    attributes: [
      name: location.name,
      latitude: location.latitude,
      longitude: location.longitude,
      zipCode: location.zipCode,
      timeZone: location.timeZone.displayName,
      temperatureScale: location.temperatureScale,
      hubIp: location.hubs[0].localIP,
      currentMode: location.mode
    ]
  ]
  buildJsonApiTopLevel(null, loc)
}

def apiRunCommand() {
  if (!request.JSON) {
    log.error "No command object provided"
    return
  }
  
  def devices = deviceList.findAll { it.id in request.JSON.devices }
  def command = request.JSON.command
  def args = request.JSON.args
  
  devices.each {
    try {
      it."$command"(*args)
    } catch (IllegalArgumentException e) {
      log.info "Device $it.id ($it.name) does not support command $command. Skipping..."
    }
  }
}

def apiTest() {
  [ meta: "Response successful" ]
}

def apiRegisterExternalSubscriber() {
  if (!request.JSON) {
    log.error "No subscriber object provided"
    return
  }
  
  def newExtSub = [
    ip: request.JSON.ipAddress,
    port: request.JSON.port,
    lastUpdate: now()
  ]
  
  state.externalSubscribers << newExtSub
  
  // TODO: Send test update to new subscriber
  
  buildJsonApiTopLevel([
    message: "Registered $newExtSub.ip:$newExtSub.port to receive updates for $location.name",
    timestamp: "$newExtSub.lastUpdate"
  ], null)
}

def apiRemoveExternalSubscriber() {
  if (!request.JSON) {
    log.error "No subscriber object provided"
    return
  }
  
  def targetExtSub = state.externalSubscribers.find {
    it.ip == request.JSON.ipAddress && it.port == request.JSON.port
  }
  
  if (!targetExtSub) {
    // Respond with error
  }
  
  state.externalSubscribers.removeElement targetExtSub
  
  buildJsonApiTopLevel([
    message: "Removed $newExtSub.ip:$newExtSub.port from subscribers of $location.name",
    timestamp: now()
  ], null)
}
