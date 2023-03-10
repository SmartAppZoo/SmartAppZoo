/**
 *  Light Control
 *
 *  Copyright 2016 Pedro Toledo
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
    name:        "Light Control",
    namespace:   "ptoledo",
    author:      "Pedro Toledo",
    description: "To control the lights on/off according to presence detection",
    category:    "SmartThings Labs",
    iconUrl:     "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url:   "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url:   "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth:        true
)
{
    appSetting   "accessToken"
}

preferences {
  page(name: "Configure the areas", install: true) {
    section("Set the information flag switches") {
      paragraph("Select your switches to communicate presence")
      input "presenceSwitchLiving", "capability.switch", title: "Pick your Living switch"
      input "presenceSwitchCocina", "capability.switch", title: "Pick your Cocina switch"
      input "presenceSwitchPasillo", "capability.switch", title: "Pick your Pasillo switch"
      input "presenceSwitchEstudio", "capability.switch", title: "Pick your Estudio switch"
      input "presenceSwitchComputador", "capability.switch", title: "Pick your Computador switch"
      input "presenceSwitchBanoi", "capability.switch", title: "Pick your Baño Invitado switch"
      input "presenceSwitchDormitorio", "capability.switch", title: "Pick your Dormitorio switch"
      input "presenceSwitchCloset", "capability.switch", title: "Pick your Closet switch"
      input "presenceSwitchBanop", "capability.switch", title: "Pick your Baño Principal switch"
    }
    section("Set the light switch for each room"){
      input "lightsLiving", "capability.switch", title: "Pick your Living lights", multiple: true, required: false
      input "lightsCocina", "capability.switch", title: "Pick your Cocina lights", multiple: true, required: false
      input "lightsPasillo", "capability.switch", title: "Pick your Pasillo lights", multiple: true, required: false
      input "lightsEstudio", "capability.switch", title: "Pick your Estudio lights", multiple: true, required: false
      input "lightsBanoi", "capability.switch", title: "Pick your Baño Invitado lights", multiple: true, required: false
      input "lightsDormitorio", "capability.switch", title: "Pick your Dormitorio lights", multiple: true, required: false
      input "lightsCloset", "capability.switch", title: "Pick your Closet lights", multiple: true, required: false
      input "lightsBanop", "capability.switch", title: "Pick your Baño Principal lights", multiple: true, required: false

    }
    section("Configuration"){
      input "offDelay", "number", title: "Set the delay to turn off a light at exiting the zone", required: true
      input "presenceSwitchOutside", "capability.switch", title: "Pick your Outside switch"
      input "lightsOutside", "capability.switch", title: "Pick your Outside event lights", multiple: true, required: false
    }
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
  subscribe(presenceSwitchLiving, "switch", lightsLivingHandler)
  subscribe(presenceSwitchCocina, "switch", lightsCocinaHandler)
  subscribe(presenceSwitchPasillo, "switch", lightsPasilloHandler)
  subscribe(presenceSwitchEstudio, "switch", lightsEstudioHandler)
  subscribe(presenceSwitchBanoi, "switch", lightsBanoiHandler)
  subscribe(presenceSwitchDormitorio, "switch", lightsDormitorioHandler)
  subscribe(presenceSwitchCloset, "switch", lightsClosetHandler)
  subscribe(presenceSwitchBanop, "switch", lightsBanopHandler)
  subscribe(presenceSwitchOutside, "switch", lightsOutsideHandler)
}

def lightsLivingHandler(evt) {
  if(evt.value == "on") {
    lightsLiving.on()
  } else {
    runIn(offDelay, offLiving)
  }
}

def offLiving() {
  if(presenceSwitchLiving.currentswitch == "off" && presenceSwitchCocina.currentswitch == "off" && presenceSwitchEntrada.currentswitch == "off"){
    lightsLiving.off()
  } else {
    runIn(offDelay, offLiving)
  }
}

def lightsCocinaHandler(evt) {
  if(evt.value == "on") {
    lightsCocina.on()
  } else {
    runIn(offDelay, offCocina)
  }
}

def offCocina() {
  if(presenceSwitchCocina.currentswitch == "off"){
    lightsCocina.off()
  }
}

def lightsPasilloHandler(evt) {
  if(evt.value == "on") {
    lightsPasillo.on()
  } else {
    runIn(offDelay, offPasillo)
  }
}

def offPasillo() {
  if(presenceSwitchPasillo.currentswitch == "off"){
    lightsPasillo.off()
  } else {
    runIn(offDelay, offPasillo)
  }
}

def lightsEstudioHandler(evt) {
  if(evt.value == "on") {
    lightsEstudio.on()
  } else {
    runIn(offDelay, offEstudio)
  }
}

def offEstudio() {
  if(presenceSwitchEstudio.currentswitch == "off" && presenceSwitchComputador.currentswitch == "off"){
    lightsEstudio.off()
  } else if (presenceSwitchComputador.currentswitch != "off"){
    runIn(offDelay, offEstudio)
  }
}

def lightsBanoiHandler(evt) {
  if(evt.value == "on") {
    lightsBanoi.on()
  } else {
    runIn(offDelay, offBanoi)
  }
}

def offBanoi() {
  if(presenceSwitchBanoi.currentswitch == "off"){
    lightsBanoi.off()
  }
}

def lightsDormitorioHandler(evt) {
  if(evt.value == "on") {
    lightsDormitorio.on()
  } else {
    runIn(offDelay, offDormitorio)
  }
}

def offDormitorio() {
  if(presenceSwitchDormitorio.currentswitch == "off"){
    lightsDormitorio.off()
  } else {
    runIn(offDelay, offDormitorio)
  }
}

def lightsClosetHandler(evt) {
  if(evt.value == "on") {
    lightsCloset.on()
  } else {
    runIn(offDelay, offCloset)
  }
}

def offCloset() {
  if(presenceSwitchCloset.currentswitch == "off"){
    lightsCloset.off()
  } else {
    runIn(offDelay, offCloset)
  }
}

def lightsBanopHandler(evt) {
  if(evt.value == "on") {
    lightsBanop.on()
  } else {
    runIn(offDelay, offBanop)
  }
}

def offBanop() {
  if(presenceSwitchBanop.currentswitch == "off"){
    lightsBanop.off()
  } else {
    runIn(offDelay, offBanop)
  }
}

def lightsOutsideHandler(evt) {
  if(evt.value == "on") {
    lightsOutside.off()
  }
}
