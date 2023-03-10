/*
 * Shade Control
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
  name: "Shade Control",
  author: "Samuel Kadolph",
  description: "Control window shades with wall switches",
  category: "Convenience",
  iconUrl: "http://cdn.device-icons.smartthings.com/Home/home9-icn.png",
  iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home9-icn@2x.png",
  iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home9-icn@3x.png"
)

preferences {
  section("What shades do you want to control?") {
    input "shades", "capability.windowShade", title: "Open and close these shades", multiple: true
  }

  section("Which switches do you want to use?") {
    input "switches", "capability.switch", title: "Controlled by these switches", multiple: true
  }
}

def installed() {
  log.debug("installed() ${settings}")

  initialize()
}

def handleButtonEvent(event) {
  def data = new groovy.json.JsonSlurper().parseText(event.data)

  log.debug("handleButtonEvent(value:${event.value}, data:${event.data})")

  if (event.value == "pushed") {
    if (data.buttonNumber == 5) {
      log.info("Opening shades ${shades}")

      shades.open()
    } else if (data.buttonNumber == 6) {
      log.info("Closing shades ${shades}")

      shades.close()
    }
  }
}

def updated() {
  log.debug("updated() ${settings}")

  unsubscribe()
  initialize()
}

private def initialize() {
  subscribe(switches, "button", handleButtonEvent)
}
