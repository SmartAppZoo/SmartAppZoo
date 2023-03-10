/**
 *  OSRAM Lightify 4x Switch Binder
 *
 *  Copyright 2017 Another User
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
 * Most original code is written by Motley74 aka Michael Hudson.
 * Original source: https://github.com/motley74/SmartThingsPublic/blob/master/devicetypes/motley74/osram-lightify-dimming-switch.src/osram-lightify-dimming-switch.groovy
 */
definition(
    name: "OSRAM Lightify 4x Switch Binder",
    namespace: "AnotherUser",
    author: "AnotherUser",
    description: "Use to bind dimmable lights/switches in ST to the buttons on a OSRAM Lightify Dimming Switch",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")



preferences {
  section("Which OSRAM Lightify Dimming Switch..."){
    input(name: "switch1", type: "capability.button", title: "Which switch?", required: true)
  }
  section("Which device(s) to control on button 1 & 2."){
    input(name: "targets", type: "capability.switch", title: "Which Target(s)?", multiple: true, required: true)
  }
  section("Which device(s) to control on button 3 & 4."){
    input(name: "targets2", type: "capability.switch", title: "Which Target(s)?", multiple: true, required: true)
  }
  section("Set level for button 1 hold..."){
    input(name: "upLevel", type: "number", range: "10..90", title: "Button 1 level?",  required: true)
  }
  section("Set level for button 2 hold..."){
    input(name: "downLevel", type: "number", range: "10..90", title: "Button 2 level?",  required: true)
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
  subscribe(switch1, "button.pushed", buttonPushedHandler)
  subscribe(switch1, "button.held", buttonHeldHandler)
  //subscribe(switch1, "button.released", buttonReleasedHandler)
}

def buttonPushedHandler(evt) {
  def buttonNumber = parseJson(evt.data)?.buttonNumber
  //basically editing and extending this block is all I can take credit for in this App.
  if (buttonNumber==1) {
    log.debug "Button 1 pushed (on)"
    targets.on()
    targets.setLevel(100)
  } else if (buttonNumber==2) {
    log.debug "Button 2 pushed (off)"
    targets.off()
  } else if (buttonNumber==3) {
    log.debug "Button 3 pushed (on)"
    targets2.on()
  } else if (buttonNumber==4) {
    log.debug "Button 4 pushed (off)"
    targets2.off()
  }
}

def buttonHeldHandler(evt) {
  log.debug "buttonHeldHandler invoked with ${evt.data}"
  //def ButtonNumber = evt.jsonData.buttonNumber
  def buttonNumber = parseJson(evt.data)?.buttonNumber
  //def levelDirection = parseJson(evt.data)?.levelData[0]
  //def levelStep = parseJson(evt.data)?.levelData[1]
  if (buttonNumber==1) {
    log.debug "Button 1 held (Setting brightness to $upLevel)"
    targets.setLevel(upLevel)
  } else {
    log.debug "Button 2 held (Setting brightness to $downLevel)"
    targets.setLevel(downLevel)
  }
}

//def buttonReleasedHandler(evt) {
//  log.debug "buttonReleasedHandler invoked with ${evt.data}"
//}