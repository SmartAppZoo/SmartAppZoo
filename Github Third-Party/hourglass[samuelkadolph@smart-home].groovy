/*
 * Hourglass
 *
 * MIT License
 *
 * Copyright (c) 2019 Samuel Kadolph
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
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
  name: "Hourglass",
  author: "Samuel Kadolph",
  description: "Turn things on and off at when you want",
  category: "Safety & Security",
  iconUrl: "https://raw.githubusercontent.com/samuelkadolph/smart-home/main/icons/hourglass.png",
  iconX2Url: "https://raw.githubusercontent.com/samuelkadolph/smart-home/main/icons/hourglass@2x.png",
  iconX3Url: "https://raw.githubusercontent.com/samuelkadolph/smart-home/main/icons/hourglass@3x.png"
)

preferences {
  page(name: "prefPage")
}

def installed() {
  log.debug("installed() ${settings}")

  initialize()
}

def positionChangeHandler(event) {
  log.debug("positionChangeHandler(${event})")

  unschedule(turnOff)
  unschedule(turnOn)
  unsubscribe()
  initialize()
}

def prefPage() {
  dynamicPage(name: "prefPage", install: true, uninstall: true) {
    section("What do you want to happen?") {
      input "onSwitches", "capability.switch", title: "Turn on these lights or switches", multiple: true, required: false
      input "offSwitches", "capability.switch", title: "Turn off these lights or switches", multiple: true, required: false
      input "dimmerLevel", "enum", title: "Set dimmers to this level", options: [10: "10%", 20: "20%", 30: "30%", 40: "40%", 50: "50%", 60: "60%", 70: "70%", 80: "80%", 90: "90%", 100: "100%"], required: false
    }

    triggerPrefs("on")

    triggerPrefs("off")

    section {
      label title: "Assign a name", required: false
      mode title: "Set for specific mode(s)", required: false
    }
  }
}

def sunriseTimeHandler(event) {
  log.debug("sunriseTimeHandler(${event})")

  handleSunrise(event.value, true)
}

def sunsetTimeHandler(event) {
  log.debug("sunsetTimeHandler(${event})")

  handleSunset(event.value, true)
}

def turnOff() {
  log.info("Turning off ${offSwitches}")

  offSwitches.off()
}

def turnOn() {
  log.info("Turning on ${onSwitches}")

  onSwitches.each { sw ->
    if (dimmerLevel != null && sw.hasCapability("Switch Level")) {
      sw.setLevel(dimmerLevel.toInteger())
    } else {
      sw.on()
    }
  }
}

def updated() {
  log.debug("updated() ${settings}")

  unschedule(turnOff)
  unschedule(turnOn)
  unsubscribe()
  initialize()
}

private def handleSunrise(String sunrise, boolean triggeredByEvent) {
  handleSuntime(sunrise, "sunrise", onSunriseOffset, offSunriseOffset, triggeredByEvent)
}

private def handleSunset(String sunset, boolean triggeredByEvent) {
  handleSuntime(sunset, "sunset", onSunsetOffset, offSunsetOffset, triggeredByEvent)
}

private def handleSuntime(String time, String trigger, Number onOffset, Number offOffset, boolean triggeredByEvent) {
  def suntime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", time)

  if (onTrigger == trigger) {
    if (triggeredByEvent && onOffset > 0) {
      runIn(onOffset * 60, turnOn)
    } else {
      runOnce(new Date(suntime.time + (onOffset * 60 * 1000)), turnOn)
    }
  }

  if (offTrigger == trigger) {
    if (triggeredByEvent && offOffset > 0) {
      runIn(offOffset * 60, turnOff)
    } else {
      runOnce(new Date(suntime.time + (offOffset * 60 * 1000)), turnOff)
    }
  }
}

private def initialize() {
  if (onTrigger == "sunrise" || offTrigger == "sunrise") {
    subscribe(location, "sunriseTime", sunriseTimeHandler)
    handleSunrise(location.currentValue("sunriseTime"), false)
  }

  if (onTrigger == "sunset" || offTrigger == "sunset") {
    subscribe(location, "sunsetTime", sunsetTimeHandler)
    handleSunset(location.currentValue("sunsetTime"), false)
  }

  if (onTrigger == "custom") {
    schedule(onTime, turnOn)
  }

  if (offTrigger == "custom") {
    schedule(offTime, turnOff)
  }

  subscribe(location, "position", positionChangeHandler)
}

private def triggerPrefs(String name) {
  section("When do you want them to turn ${name}?") {
    input "${name}Trigger", "enum", title: "At", options: ["custom": "A Specific Time", "never": "Never", "sunrise": "Sunrise", "sunset":"Sunset"], defaultValue: "custom", submitOnChange: true

    switch(settings["${name}Trigger"]) {
      case "never":
        break
      case "sunrise":
        input "${name}SunriseOffset", "number", title: "Minutes after sunrise", range: "*..*", defaultValue: 0
        break
      case "sunset":
        input "${name}SunsetOffset", "number", title: "Minutes after sunset", range: "*..*", defaultValue: 0
        break
      default:
        input "${name}Time", "time", title: "When", required: true
    }
  }
}
