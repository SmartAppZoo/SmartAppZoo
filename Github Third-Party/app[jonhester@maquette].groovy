import org.apache.commons.codec.binary.Base64

/**
 *  MQTT
 *
 *  Copyright 2016 Jon Hester
 *
 */
definition(
    name: "Maquette",
    namespace: "jonhester",
    author: "Jon Hester",
    description: "Connects Smartthings to MQTT",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences(oauthPage: "deviceAuthorization") {
    // deviceAuthorization page is simply the devices to authorize
    page(name: "deviceAuthorization", title: "", nextPage: "otherPage",
         install: false, uninstall: true) {
        section("Select Devices to Authorize") {
            input "switches", "capability.switch", title: "Switches?", multiple: true, required: false
            input "contactSensors", "capability.contactSensor", required: false, multiple: true
      		input "temperatureSensors", "capability.temperatureMeasurement", required: false, multiple: true
      		input "batterySensors", "capability.battery", required: false, multiple: true
      		input "motionSensors", "capability.motionSensor", required: false, multiple: true
        }

    }

    page(name: "otherPage")
}

def otherPage() {
    dynamicPage(name: "otherPage", title: "Server Settings", install: true) {
        section("Maquette Instance") {
            input "uri", "text", title: "URL", description: "Maquette instance url", required: true
            input "username", "text", title: "Username", description: "Maquette username", required: true
            input "password", "password", title: "Password", description: "Maquette password", required: true
        }
    }
}

mappings {
  path("/set") {
    action: [
    PUT: "requestHandler"
    ]
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
  subscribe(switches, "switch", eventHandler)
  subscribe(contactSensors, "contact", eventHandler)
  subscribe(temperatureSensors, "temperature", eventHandler)
  subscribe(motionSensors, "motion", eventHandler)
  subscribe(batterySensors, "battery", eventHandler)
}

def requestHandler() {
  def name = request.JSON?.name;
  def type = request.JSON?.type;
  def value = request.JSON?.value;

  if (!name || !type || !value) {
    httpError(400, "malformed payload")
    return
  }

  switch (type) {
    case "switch":
      switchHandler(name, type, value)
      break
  }
}

def switchHandler(name, type, value) {
  switches.each {
    if (it.displayName == name) {
      if (value == "on") {
        it.on();
        return [message: "$name turned on"]
        } else if (value == "off") {
          it.off();
          return [message: "$name turned off"]
        }
      }
    }
  }

def eventHandler(evt) {

  def data = "${settings.username}:${settings.password}"
  def bytes = data.bytes

  Base64 coder = new Base64()

  def encodedData = coder.encode(bytes)
  String basicAuth = new String(encodedData)
  
  log.debug "token ${basicAuth}"
  log.debug "uri ${settings.uri}"

  def json_body = [
    id: evt.deviceId,
    value: evt.value,
    type: evt.name,
    name: evt.displayName
  ]

  def json_params = [
    uri: "${settings.uri}/push",
    success: success,
    body: json_body,
    headers: [
      Authorization: "Basic ${basicAuth}"
    ]
  ]

  try {
    httpPostJson(json_params)
  } catch (e) {
    log.error "http post failed: $e"
  }
}

def success = { response ->
  log.debug "Request was successful, $response"
}
