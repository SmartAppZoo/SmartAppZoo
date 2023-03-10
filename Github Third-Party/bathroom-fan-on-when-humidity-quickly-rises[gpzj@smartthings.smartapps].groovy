/**
 *  Bathroom Fan On When Humidity Quickly Rises (And shut off on the decline)
 *
 *  Author: Justin Wildeboer
 */

definition(
    name: "Bathroom Fan On When Humidity Quickly Rises",
    namespace: "gpzj",
    author: "Justin Wildeboer",
    description: "Bathroom Fan On When Humidity Quickly Rises (And shut off on the decline)",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet@2x.png"
)

preferences {
  section("Humidity Sensor") {
    input "humiditySensor", "capability.relativeHumidityMeasurement", title: "Choose Humidity Sensor"
  }
  section("Bathroom Fan") {
    input "bathroomFan", "capability.switch", title: "Choose Bathroom Fan Switch"
  }
}

def installed() {
  log.debug "${app.label} installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "${app.label} updated with settings: ${settings}"
  initialize()
}

def uninstall() {
  log.debug "${app.label} uninstalled"
  unsubscribe()
}

def initialize() {
    subscribe(humiditySensor, "humidity", humidityChangeCheck)
}

def humidityChangeCheck(evt) {
  log.debug "handler $evt.name: $evt.value"
  bathroomFan.on()
}