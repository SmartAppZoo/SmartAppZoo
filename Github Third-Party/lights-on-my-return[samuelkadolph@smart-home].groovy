/*
 * Lights On My Return
 *
 * MIT License
 *
 * Copyright (c) 2018 Samuel Kadolph
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

definition(
  namespace: "smart-home",
  name: "Lights on My Return",
  author: "Samuel Kadolph",
  description: "Turn lights on when you get home and off afterwards",
  category: "Safety & Security",
  iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn.png",
  iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn@2x.png",
  iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn@3x.png"
)

preferences {
  page(name: "prefPage")
}

def installed() {
  log.debug("installed() ${settings}")

  initialize()
}

def handlePresenceEvent(event) {
  log.debug("handlePresenceEvent(value:${event.value}, data:${event.data})")

  if (event.value != "present") {
    return
  }

  if (!checkConditions()) {
    log.info("Conditions not met, skipping")
    return
  }

  lights.eachWithIndex { light, i ->
    if (state.lightsToTurnOff.contains(i)) {
      log.info("'${light.label}' is already on, will restart timer")
    } else if (light.currentSwitch == "on") {
      log.info("'${light.label}' is already on, skipping")
    } else {
      log.info("Turning on '${light.label}'")

      light.on()

      state.lightsToTurnOff << i
    }
  }

  if (state.lightsToTurnOff.size() > 0) {
    runIn(delay * 60, turnOffLights)
  }
}

def turnOffLights() {
  def indexes = state.lightsToTurnOff

  state.lightsToTurnOff = []

  indexes.each { i ->
    def light = lights[i]

    log.info("Turning off '${light.label}'")

    light.off()
  }
}

def prefPage() {
  dynamicPage(name: "prefPage", install: true, uninstall: true) {
    section("Turn these lights on") {
      input "lights", "capability.switch", title: "Which lights?", multiple: true
    }

    section("When one of these people comes home") {
      input "people", "capability.presenceSensor", title: "Which people?", multiple: true
    }

    section("And turn the lights off after") {
      input "delay", "decimal", title: "Number of minutes", defaultValue: 5
    }

    section("Only when") {
      input "conditions", "enum", title: "When?", options: ["always":"Always", "custom":"Between Specific Times", "illuminance":"It's Dark Enough", "sunset":"Between Sunset and Sunrise"], defaultValue: "always", submitOnChange: true

      switch(conditions) {
        case "always":
          break
        case "custom":
          input "windowStart", "time", title: "From", required: true
          input "windowEnd", "time", title: "Until", required: true
          break
        case "illuminance":
          input "illuminanceDevice", "capability.illuminanceMeasurement", title: "When this device", required: true
          input "illuminanceMax", "number", title: "Reports below this lux value", defaultValue: 5
          break
        case "sunset":
          input "sunsetOffset", "number", title: "Minutes before sunset", defaultValue: 0
          input "sunriseOffset", "number", title: "Minutes after sunrise", defaultValue: 0
      }
    }

    section (mobileOnly: true) {
      label title: "Assign a name", required: false
      mode title: "Set for specific mode(s)", required: false
    }
  }
}

def updated() {
  log.debug("updated() ${settings}")

  unschedule(turnOffLights)
  unsubscribe()
  initialize()
}

private def checkConditions() {
  switch(conditions) {
    case "always":
      return true
    case "illuminance":
      return illuminanceDevice.currentIlluminance < illuminanceMax
    case "sunset":
      def sns = getSunriseAndSunset(sunsetOffset: "-$sunsetOffset", sunriseOffset: "$sunriseOffset")
      return timeOfDayIsBetween(sns.sunset, sns.sunrise, new Date(), location.timeZone)
    case "custom":
      return timeOfDayIsBetween(windowStart, windowEnd, new Date(), location.timeZone)
  }
}

private def initialize() {
  state.lightsToTurnOff = []

  subscribe(people, "presence", handlePresenceEvent)
}
