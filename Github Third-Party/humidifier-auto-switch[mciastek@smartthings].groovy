definition(
    name: "Humidifier auto-switch",
    namespace: "mciastek",
    author: "Mirek Ciastek",
    description: "Turn on/off humidifier connected with smart plug for given humidity and in given time range",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    pausable: true
)

preferences {
  section("Monitor humidity") {
    input "humiditySensor", "capability.relativeHumidityMeasurement", required: true
  }

  section("When humidity drops below") {
    input "humidity", "number", title: "Humidity?", required: true
  }

  section("Turn on between what times?") {
    input "fromTime", "time", title: "From", required: false
    input "toTime", "time", title: "To", required: false
  }

  section("Toggle plug") {
    input "plug", "capability.switch", required: true
  }
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

def initialize() {
  triggerOnHumidity(humiditySensor.humidityState)

  subscribe(humiditySensor, "humidity", humidityHandler)

  if (fromTime && toTime) {
    schedule(toTime, scheduledTurnOff)
  }
}

def humidityHandler(evt) {
  log.trace "humidity: $evt.value, $evt"

  triggerOnHumidity(evt)
}

private triggerOnHumidity(humidityState) {
  def minHumidity = humidity
  def between = isBetween()

  if (humidityState.doubleValue < minHumidity) {
    log.debug "Humidity fallen below $minHumidity"

    if (between) {
      log.debug "Is within selected time range"
      turnOn()
    } else {
      log.debug "Is out of selected time range"
      turnOff()
    }
  } else {
    turnOff()
  }
}

private isBetween() {
  if (!fromTime && !toTime) {
    return true
  }

  return timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
}

private scheduledTurnOff() {
  log.debug "The time is $toTime"
  turnOff()
}

private turnOn() {
  if (plug.currentSwitch != 'on') {
    log.debug "Turning on ${plug.displayName}..."
    plug.on()
  }
}

private turnOff() {
  if (plug.currentSwitch != 'off') {
    log.debug "Turning off ${plug.displayName}..."
    plug.off()
  }
}
