/**
 *  Samsung TV Automation Integration
 *
 *  Copyright 2017 Travis Illig
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
  name: "Samsung TV Automation Proxy",
  namespace: "tillig",
  author: "Travis Illig",
  description: "Enables remote control of a Samsung TV via a simple REST service that can be integrated into other automation systems.",
  category: "My Apps",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


// Samsung TV supports commands:
// [on, off, refresh, deviceNotification, volumeUp, volumeDown, setVolume, mute, unmute, setPictureMode, setSoundMode, showMessage]


preferences {
  section("TV To Control") {
    input("television", "capability.samsungTV", required: true, title: "Select a TV")
  }
}


mappings {
  path("/tv") {
    action: [
      GET: "listTelevision"
    ]
  }
  path("/tv/mute/:value") {
    action: [
      PUT: "updateMute"
    ]
  }
  path("/tv/power/:value") {
    action: [
      PUT: "updatePower"
    ]
  }
  path("/tv/volume/:value") {
    action: [
      PUT: "updateVolume"
    ]
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
  television.capabilities.each { cap ->
    log.debug "This device supports the ${cap.name} capability"
    cap.attributes.each { attr ->
      log.debug "-- ${cap.name} Attribute: ${attr.name}"
    }
    cap.commands.each { cmd ->
      log.debug "-- ${cap.name} Command: ${cmd.name}"
    }
  }
}

def listTelevision() {
  return [
    name: television.displayName,
    power: television.currentValue("switch"),
    volume: television.currentValue("volume"),

    // mute is "mute" or "unmute" which is great for them but not really human friendly
    mute: television.currentValue("mute") == "unmute" ? "off" : "on",
    pictureMode: television.currentValue("pictureMode"),
    soundMode: television.currentValue("soundMode")
  ]
}

def updateMute() {
  def command = params.value
  switch(command) {
    case "on":
      log.debug "Turning mute on."
      television.mute()
      break
    case "off":
      log.debug "Turning mute off."
      television.unmute()
      break
    default:
      httpError(400, "$value is not a valid mute setting. Use 'on' or 'off'.")
  }
}

def updatePower() {
  def command = params.value
  switch(command) {
    case "on":
      log.debug "Turning television on."
      television.on()
      break
    case "off":
      log.debug "Turning television off."
      television.off()
      break
    default:
      httpError(400, "$value is not a valid power setting. Use 'on' or 'off'.")
  }
}

def updateVolume() {
  def value = params.value
  try {
    value = value.toInteger()
  }
  catch (e) {
    httpError(400, "$value is not a valid volume setting. Use an integer between 0 and 100.")
  }

  if (value < 0 || value > 100) {
    httpError(400, "$value is not a valid volume setting. Use an integer between 0 and 100.")
  }

  log.debug "Setting volume to ${value}"
  television.setVolume(value)
}
