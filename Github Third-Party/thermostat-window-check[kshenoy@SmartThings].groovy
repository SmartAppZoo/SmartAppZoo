/**
 *  Thermostat Window Check
 *
 *  Author: brian@bevey.org
 *  Date: 7/31/14
 *
 *  If your heating or cooling system come on, it gives you notice if there are
 *  any windows or doors left open, preventing the system from working
 *  optimally.
 */

definition(
  name: "Thermostat Window Check",
  namespace: "kshenoy",
  author: "kshenoy",
  description: "If your heating or cooling system come on, it gives you notice if there are any windows or doors left open, preventing the system from working optimally.",
  category: "My Apps",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  section("Things to check?") {
    input "sensors", "capability.contactSensor", multiple: true
  }

  section("Thermostats to monitor") {
    input "thermostats", "capability.thermostat", multiple: true
  }

  section("Notifications") {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
    input "phone", "phone", title: "Send a Text Message?", required: false
  }

  section("Delay to wait before sending notification. (defaults to 30 seconds)") {
    input "msgDelay", "decimal", title: "Number of seconds", required: false
  }

  section("Turn thermostat off automatically?") {
    input "turnOffTherm", "enum", metadata: [values: ["Yes", "No"]], required: false
  }

  section("Delay to wait before turning thermostat off (defaults to 1 minute)") {
    input "turnOffDelay", "decimal", title: "Number of minutes", required: false
  }
}

def installed() {
  init()
}

def updated() {
  unsubscribe()
  init()
}

def init() {
  subscribe(thermostats, "thermostatMode", thermoChange);
  subscribe(sensors, "contact.open", windowChange);
}

def thermoChange(evt) {
  if ((evt.value == "heat") || (evt.value == "cool")) {
    def open = sensors.findAll { it?.latestValue("contact") == "open" }

    if (open) {
      def plural = open.size() > 1 ? "are" : "is"
      send("${open.join(', ')} ${plural} still open and the thermostat just came on.")

      thermoShutOffTrigger()
    }

    else {
      log.info("Thermostat came on and nothing is open.");
    }
  }
}

def windowChange(evt) {
  def heating = thermostats.findAll { it?.latestValue("thermostatMode") == "heat" }
  def cooling = thermostats.findAll { it?.latestValue("thermostatMode") == "cool" }

  if(heating || cooling) {
    def open = sensors.findAll { it?.latestValue("contact") == "open" }
    def tempDirection = heating ? "heating" : "cooling"
    def plural = open.size() > 1 ? "were" : "was"
    scheduleMsg("${open.join(', ')} ${plural} opened and the thermostat is still ${tempDirection}.")
  }

  thermoShutOffTrigger()
}

def scheduleMsg(msg) {
  log.info("Starting timer to send notification")
  def delay = (msgDelay != null && msgDelay != "") ? msgDelay : 30
  if (delay == 0 || msgDelay == null || msgDelay == "") {
    send(msg)
  }
  else {
    runIn(delay, "send", [data: [msg: msg]])
  }
}

def thermoShutOffTrigger() {
  if(turnOffTherm == "Yes") {
    log.info("Starting timer to turn off thermostat")
    def delay = (turnOffDelay != null && turnOffDelay != "") ? turnOffDelay * 60 : 60
    state.turnOffTime = now()

    runIn(delay, "thermoShutOff")
  }
}

def thermoShutOff() {
  def open = sensors.findAll { it?.latestValue("contact") == "open" }
  def tempDirection = heating ? "heating" : "cooling"
  def plural = open.size() > 1 ? "are" : "is"

  log.info("Checking if we need to turn thermostats off")

  if(open.size()) {
    def heating = thermostats.findAll { it?.latestValue("thermostatMode") == "heat" }
    def cooling = thermostats.findAll { it?.latestValue("thermostatMode") == "cool" }

    if((heating) || (cooling)) {
      send("Thermostats turned off: ${open.join(', ')} ${plural} open and thermostats ${tempDirection}.")
      log.info("Windows still open, turning thermostats off")
    }

    thermostats?.off()
  }

  else {
    log.info("Looks like everything is shut now - no need to turn off thermostats")
  }
}

private send(data) {
  def msg = data.msg
  def open = sensors.findAll { it?.latestValue("contact") == "open" }
  def tempDirection = heating ? "heating" : "cooling"
  def plural = open.size() > 1 ? "are" : "is"

  log.info("Checking if any windows are still open")

  if(open.size()) {
    def heating = thermostats.findAll { it?.latestValue("thermostatMode") == "heat" }
    def cooling = thermostats.findAll { it?.latestValue("thermostatMode") == "cool" }

    if((heating) || (cooling)) {
      if(sendPushMessage != "No") {
        log.debug("Sending push message")
        sendPush(msg)
      }
      if(phone) {
        log.debug("Sending text message")
        sendSms(phone, msg)
      }
      log.debug(msg)
    }
  }
  else {
    log.info("Looks like everything is shut now - no need to send a message")
  }
}