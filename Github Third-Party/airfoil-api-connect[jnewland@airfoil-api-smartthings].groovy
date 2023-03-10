definition(
  name: "Airfoil API Connect",
  namespace: "jnewland",
  author: "Jesse Newland",
  description: "Connect to a local copy of Airfoil API to add and control Airfoil connected Speakers",
  category: "SmartThings Labs",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  page(name: "config")
}

def config() {
  dynamicPage(name: "config", title: "Airfoil API", install: true, uninstall: true) {

    section("Please enter the details of the running copy of Airfoil API you want to control") {
      input(name: "ip", type: "text", title: "IP", description: "Airfoil API IP", required: true, submitOnChange: true)
      input(name: "port", type: "text", title: "Port", description: "Airfoil API port", required: true, submitOnChange: true)
      input(name: "name", type: "text", title: "Name", description: "Computer Name", required: true, submitOnChange: true)
    }

    if (ip && port) {
      int speakerRefreshCount = !state.speakerRefreshCount ? 0 : state.speakerRefreshCount as int
      state.speakerRefreshCount = speakerRefreshCount + 1
      doDeviceSync()

      def options = getSpeakers().collect { s ->
        if (s.name == name) {
          null
        } else if (s.name == "Computer") {
          null
        } else {
          s.name
        }
      }
      options.removeAll([null])
      log.trace "Speaker options: ${options}"
      def numFound = options.size() ?: 0

      if (name) {
        section("Please wait while we discover your speakers. Select your devices below once discovered.") {
          input name: "selectedSpeakers", type: "enum", required:false, title:"Select Speakers (${numFound} found)", multiple:true, options:options
        }
      }
    }
  }
}

def installed() {
  initialize()
}

def updated() {
  initialize()
}

def initialize() {
  state.subscribe = false
  unsubscribe()

  if (selectedSpeakers) {
    addSpeakers()
  }
  if (ip) {
    doDeviceSync()
    runEvery5Minutes("doDeviceSync")
  }
}

def uninstalled() {
  unschedule()
  unsubscribe()
}

def addSpeakers() {
  def speakers = getSpeakers()
  speakers.collect { s ->
    selectedSpeakers.findAll { selected ->
      selected == s.name
    }.first {
      def dni = app.id + "/" + s.id
      def d = getChildDevice(dni)
      if(!d) {
        d = addChildDevice("airfoil", "Airfoil Speaker", dni, null, ["label": "${s.name}@${name}"])
        log.debug "created ${d.displayName} with id $dni"
        d.refresh()
      } else {
        log.debug "found ${d.displayName} with id $dni already exists, type: '$d.typeName'"
      }
      s.dni = dni
      return s
    }
  }
  atomicState.speakers = speakers
  log.trace "Set atomicState.speakers to ${speakers}"
}

def locationHandler(evt) {
  def description = evt.description
  def hub = evt?.hubId

  def parsedEvent = parseEventMessage(description)
  parsedEvent << ["hub":hub]

  if (parsedEvent.headers && parsedEvent.body)
  {
    def headerString = new String(parsedEvent.headers.decodeBase64())
    def bodyString = new String(parsedEvent.body.decodeBase64())
    def body = new groovy.json.JsonSlurper().parseText(bodyString)
    log.trace "Airfoil API response: ${body}"

    if (body instanceof java.util.HashMap)
    { //POST /speakers/*/* response
      def speakers = atomicState.speakers.collect { s ->
        if (s.id == body.id) {
          body
        } else {
          s
        }
      }
      atomicState.speakers = speakers
      log.trace "Set atomicState.speakers to ${speakers}"
      def dni = app.id + "/" + body.id
      def d = getChildDevice(dni)
      if (d) {
        if (body.connected == "true") {
          sendEvent(d.deviceNetworkId, [name: "switch", value: "on"])
        } else {
          sendEvent(d.deviceNetworkId, [name: "switch", value: "off"])
        }
        if (body.volume) {
          def level = Math.round(body.volume * 100.00)
          sendEvent(d.deviceNetworkId, [name: "level", value: level])
        }
      }
    }
    else if (body instanceof java.util.List)
    { //GET /speakers response (application/json)
      def bodySize = body.size() ?: 0
      if (bodySize > 0 ) {
        atomicState.speakers = body
        body.each { s ->
          def dni = app.id + "/" + s.id
          def d = getChildDevice(dni)
          if (d) {
            if (s.connected == "true") {
              sendEvent(d.deviceNetworkId, [name: "switch", value: "on"])
            } else {
              sendEvent(d.deviceNetworkId, [name: "switch", value: "off"])
            }
            if (s.volume) {
              def level = Math.round(s.volume * 100.00)
              sendEvent(dni, [name: "level", value: level])
            }
          }
        }
        log.trace "Set atomicState.speakers to ${speakers}"
      }
    }
    else
    {
      //TODO: handle retries...
      log.error "ERROR: unknown body type"
    }
  }
  else {
    log.trace "UNKNOWN EVENT $evt.description"
  }
}

def getSpeakers() {
  atomicState.speakers ?: [:]
}

private def parseEventMessage(Map event) {
  return event
}

private def parseEventMessage(String description) {
  def event = [:]
  def parts = description.split(',')
  parts.each { part ->
    part = part.trim()
    if (part.startsWith('devicetype:')) {
      def valueString = part.split(":")[1].trim()
      event.devicetype = valueString
    }
    else if (part.startsWith('mac:')) {
      def valueString = part.split(":")[1].trim()
      if (valueString) {
        event.mac = valueString
      }
    }
    else if (part.startsWith('networkAddress:')) {
      def valueString = part.split(":")[1].trim()
      if (valueString) {
        event.ip = valueString
      }
    }
    else if (part.startsWith('deviceAddress:')) {
      def valueString = part.split(":")[1].trim()
      if (valueString) {
        event.port = valueString
      }
    }
    else if (part.startsWith('ssdpPath:')) {
      def valueString = part.split(":")[1].trim()
      if (valueString) {
        event.ssdpPath = valueString
      }
    }
    else if (part.startsWith('ssdpUSN:')) {
      part -= "ssdpUSN:"
      def valueString = part.trim()
      if (valueString) {
        event.ssdpUSN = valueString
      }
    }
    else if (part.startsWith('ssdpTerm:')) {
      part -= "ssdpTerm:"
      def valueString = part.trim()
      if (valueString) {
        event.ssdpTerm = valueString
      }
    }
    else if (part.startsWith('headers')) {
      part -= "headers:"
      def valueString = part.trim()
      if (valueString) {
        event.headers = valueString
      }
    }
    else if (part.startsWith('body')) {
      part -= "body:"
      def valueString = part.trim()
      if (valueString) {
        event.body = valueString
      }
    }
  }

  event
}

def doDeviceSync(){
  poll()

  if(!state.subscribe) {
    subscribe(location, null, locationHandler, [filterEvents:false])
    state.subscribe = true
  }
}

def on(childDevice) {
  log.debug "Executing 'on'"
  post("/speakers/${getId(childDevice)}/connect", "", getId(childDevice))
}

def off(childDevice) {
  log.debug "Executing 'off'"
  post("/speakers/${getId(childDevice)}/disconnect", "", getId(childDevice))
}

def setLevel(childDevice, level) {
  post("/speakers/${getId(childDevice)}/volume", "${level}", getId(childDevice))
}

private getId(childDevice) {
  return childDevice.device?.deviceNetworkId.split("/")[-1]
}

private poll() {
  def uri = "/speakers"
  log.debug "GET:  $uri"
  sendHubCommand(new physicalgraph.device.HubAction("""GET ${uri} HTTP/1.1
HOST: ${ip}:${port}

""", physicalgraph.device.Protocol.LAN, "${ip}:${port}"))
}

private post(path, text, dni) {
  def uri = "$path"
  def length = text.getBytes().size().toString()

  log.debug "POST:  $uri"
  sendHubCommand(
      new physicalgraph.device.HubAction(
        """POST ${uri} HTTP/1.1\r\nHOST: $ip:$port\r\nContent-length: ${length}\r\nContent-type: text/plain\r\n\r\n${text}\r\n""",
        physicalgraph.device.Protocol.LAN,
        dni
      )
  )

}
