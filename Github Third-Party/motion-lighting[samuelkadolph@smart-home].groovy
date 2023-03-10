/*
 *  Motion Lighting
 *
 *  Copyright (c) 2019 Samuel Kadolph
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

definition(
  namespace: "smart-home",
  name: "Motion Lighting",
  author: "Samuel Kadolph",
  description: "Automatically turn lights on when motion is detected",
  category: "Convenience",
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

def handleMotionEvent(event) {
  log.debug("handleMotionEvent(value:${event.value}, data:${event.data}")

  if (!checkConditions()) {
    log.debug("Conditions not met, skipping")
    return
  }

  if (event.value == "active") {
    lights.on()
  } else {
    lights.off()
  }

  // if we turned it on and switch turned off, stay off until no motion?
  // if it's already on, don't turn it on
}

def handleSwitchEvent(event) {
  log.debug("handleSwitchEvent(value:${event.value}, data:${event.data}")
}


def prefPage() {
  dynamicPage(name: "prefPage", install: true, uninstall: true) {
    section("Turn these lights on") {
      input "lights", "capability.switch", title: "Which lights?", multiple: true
    }

    section("When there is motion detected by") {
      input "sensors", "capability.motionSensor", title: "Which sensors?", multiple: true
    }

    section("And turn the lights off after") {
      input "delay", "decimal", title: "Number of minutes", defaultValue: 5
    }

    section("Only when") {
      input "conditions", "enum", title: "When?", options: ["always":"Always", "custom":"Specific Times", "illuminance":"It's Dark Enough", "sunset":"Sunset and Sunrise"], defaultValue: "always", submitOnChange: true

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

  unsubscribe()
  initialize()
}

private def initialize() {
  subscribe(lights, "switch", handleSwitchEvent)
  subscribe(sensors, "motion", handleMotionEvent)

  if (!state.lights) {
    state.lights = []
  }
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
