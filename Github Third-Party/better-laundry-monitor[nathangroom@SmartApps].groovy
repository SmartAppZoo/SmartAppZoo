/**
 *  Alert on Power Consumption
 *
 *  Copyright 2015 Kevin Tierney
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

import groovy.time.*

definition(
  name: "Better Laundry Monitor",
  namespace: "tierneykev",
  author: "Kevin Tierney",
  description: "Using a switch with powerMonitor capability, monitor the laundry cycle and alert when it's done.",
  category: "Green Living",
  iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Appliances/appliances8-icn.png",
  iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Appliances/appliances8-icn@2x.png")


preferences {
  section ("When this device stops drawing power") {
    input "meter", "capability.powerMeter", multiple: false, required: true
  }
section ("Power Thresholds", hidden: true, hideable: true) {
    input "startThreshold", "number", title: "start cycle when power raises above (W)", description: "8", required: true
    input "endThreshold", "number", title: "stop cycle when power drops below (W)", description: "4", required: true
  }

section ("Send this message") {
    input "message", "text", title: "Notification message", description: "Laudry is done!", required: true
  }

  section (title: "Notification method") {
    input "sendPushMessage", "bool", title: "Send a push notification?"
    input "speechOut", "capability.speechSynthesis", title:"Speak Via: (Speech Synthesis)",multiple: true, required: false
    input "player", "capability.musicPlayer", title:"Speak Via: (Music Player -> TTS)",multiple: true, required: false
    input "phone", "phone", title: "Send a text message to:", required: false
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
  subscribe(meter, "power", handler)
  atomicState.cycleOn = false
  
}

def handler(evt) {
  def latestPower = meter.currentValue("power")
  log.trace "Power: ${latestPower}W"
  log.trace "State: ${atomicState.cycleOn}"

  //Added latestpower < 1000 to deal with spikes that triggered false alarms
  if (!atomicState.cycleOn && latestPower >= startThreshold && latestPower < 1000) {
    atomicState.cycleOn = true   
    log.trace "Cycle started."
  }
  // If the washer stops drawing power, the cycle is complete, send notification.
  else if (atomicState.cycleOn && latestPower < endThreshold) {
    send(message)
    if(speechOut){speakMessage(message)}
    if(player){musicPlayerTTS(message)}
    atomicState.cycleOn = false
    atomicState.cycleEnd = now()
    log.trace "State: ${atomicState.cycleOn}"
  }
}


private send(msg) {
  if (sendPushMessage) {
    sendPush(msg)
  }

  if (phone) {
    sendSms(phone, msg)
  }

  log.debug msg
}

private speakMessage(msg) {
speechOut.speak(msg)
}
private musicPlayerTTS(msg) {
	player.playText(msg)
}

private hideOptionsSection() {
    (phone || switches || hues || color || lightLevel) ? false : true
}
