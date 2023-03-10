definition(name: "GroveStreams",
           namespace: "CyborgMaster",
           author: "Jeremy Mickelson",
           description: "Log to GroveStreams",
           category: "My Apps",
           iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
           iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
           iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section("Log devices...") {
    input("temperatures", "capability.temperatureMeasurement",
          title: "Temperatures", required:false, multiple: true)
    input("humidities", "capability.relativeHumidityMeasurement",
          title: "Humidities", required:false, multiple: true)
    input("thermostats", "capability.thermostat",
          title: "Thermostats", required: false, multiple: true)
    input("contacts", "capability.contactSensor",
          title: "Doors open/close", required: false, multiple: true)
    input("accelerations", "capability.accelerationSensor",
          title: "Accelerations", required: false, multiple: true)
    input("motions", "capability.motionSensor",
          title: "Motions", required: false, multiple: true)
    input("presence", "capability.presenceSensor",
          title: "Presence", required: false, multiple: true)
    input("switches", "capability.switch",
          title: "Switches", required: false, multiple: true)
    input("batteries", "capability.battery",
          title: "Batteries", required: false, multiple: true)
  }

  section ("GroveStreams Feed PUT API key...") {
    input "channelKey", "text", title: "API key"
  }
}

def installed() {
  initialize()
}

def updated() {
  log.debug "Updating..."
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  subscribe(temperatures, "temperature", handleTemperatureEvent)
  subscribe(humidities, "humidity", handleHumidityEvent)
  subscribe(thermostats, "thermostatOperatingState", handleThermostatOperatingStateEvent)
  subscribe(contacts, "contact", handleContactEvent)
  subscribe(accelerations, "acceleration", handleAccelerationEvent)
  subscribe(motions, "motion", handleMotionEvent)
  subscribe(presence, "presence", handlePresenceEvent)
  subscribe(switches, "switch", handleSwitchEvent)
  subscribe(batteries, "battery", handleBatteryEvent)
  if (atomicState.queue == null) {
    log.debug "Initializing queue"
    atomicState.queue = []
  }
  if (atomicState.lock == null) {
    log.debug "Initializing lock"
    atomicState.lock = []
  }
  schedule("27 * * * * ?", processQueue)
}

def handleTemperatureEvent(evt) {
  queueValue(evt) { it.toString() }
}

def handleHumidityEvent(evt) {
  queueValue(evt) { it.toString() }
}

def handleThermostatOperatingStateEvent(evt) {
  queueValue(evt) { it == "idle" ? "false" : "true" }
}

def handleBatteryEvent(evt) {
  queueValue(evt) { it.toString() }
}

def handleContactEvent(evt) {
  queueValue(evt) { it == "open" ? "true" : "false" }
}

def handleAccelerationEvent(evt) {
  queueValue(evt) { it == "active" ? "true" : "false" }
}

def handleMotionEvent(evt) {
  queueValue(evt) { it == "active" ? "true" : "false" }
}

def handlePresenceEvent(evt) {
  queueValue(evt) { it == "present" ? "true" : "false" }
}

def handleSwitchEvent(evt) {
  queueValue(evt) { it == "on" ? "true" : "false" }
}

private queueValue(evt, Closure convert) {
  def jsonPayload = [compId: evt.displayName, streamId: evt.name, data: convert(evt.value), time: now()]
  log.debug "Appending to queue ${jsonPayload}"

  synchronized(atomicState.lock) {
    def queue = atomicState.queue
    queue << jsonPayload
    atomicState.queue = queue
  }

  log.debug "Queue ${atomicState.queue}"
}

def processQueue() {
  log.debug "Processing queue"
  def url = "https://grovestreams.com/api/feed?api_key=${channelKey}"
  def header = ["X-Forwarded-For": location.id]
  if (atomicState.queue != null && atomicState.queue.size() > 0) {
    synchronized(atomicState.lock) {
      log.debug "Events: ${atomicState.queue}"
      try {
        httpPutJson(["uri": url, "header": header, "body": atomicState.queue]) {
          response ->
            if (response.status != 200 ) {
              log.debug "GroveStreams logging failed, status = ${response.status}"
            } else {
              log.debug "GroveStreams accepted event(s)"
              atomicState.queue = []
            }
        }
      } catch(e) {
        def errorInfo = "Error sending value: ${e}"
        log.error errorInfo
        if (e instanceof groovyx.net.http.ResponseParseException) {
          log.debug "Simple parse error, events were still accepted.  Clearing queue"
          atomicState.queue = []
        }
      }
    }
  }
}
