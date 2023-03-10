/**
 *  Presence Detection
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
  name: "Presence Detection",
  namespace: "ptoledo",
  author: "Pedro Toledo",
  description: "An app to identify the active zones at the apartment considering a set of motion sensor.",
  category: "SmartThings Labs",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section("Set the sensors") {
    paragraph("Select your motion sensors for each area")
    input "sensorEntrada", "capability.motionSensor", title: "Pick your Entrada sensors", required: false, multiple: true
    input "sensorLiving", "capability.motionSensor", title: "Pick your Living sensors", required: false, multiple: true
    input "sensorCocina", "capability.motionSensor", title: "Pick your Cocina sensors", required: false, multiple: true
    input "sensorPasillo", "capability.motionSensor", title: "Pick your Pasillo sensors", required: false, multiple: true
	input "sensorEstudio", "capability.motionSensor", title: "Pick your Estudio sensors", required: false, multiple: true
    input "sensorComputador", "capability.switch", title: "Pick your Computador sensors", required: false, multiple: true
    input "sensorBanoi", "capability.motionSensor", title: "Pick your Baño Invitado sensors", required: false, multiple: true
    input "sensorDormitorio", "capability.motionSensor", title: "Pick your Dormitorio sensors", required: false, multiple: true
    input "sensorCloset", "capability.motionSensor", title: "Pick your Closet sensors", required: false, multiple: true
    input "sensorBanop", "capability.motionSensor", title: "Pick your Baño Principal sensors", required: false, multiple: true
    input "sensorPersonas", "capability.presenceSensor", title: "Pick your Personas sensors", required: false, multiple: true
  }
  section("Set the information flag switches") {
    paragraph("Select your switches to communicate presence")
    input "presenceSwitchEntrada", "capability.switch", title: "Pick your Entrada switch", required: true
    input "presenceSwitchLiving", "capability.switch", title: "Pick your Living switch", required: true
    input "presenceSwitchCocina", "capability.switch", title: "Pick your Cocina switch", required: true
    input "presenceSwitchPasillo", "capability.switch", title: "Pick your Pasillo switch", required: true
    input "presenceSwitchEstudio", "capability.switch", title: "Pick your Estudio switch", required: true
    input "presenceSwitchBanoi", "capability.switch", title: "Pick your Baño Invitado switch", required: true
    input "presenceSwitchDormitorio", "capability.switch", title: "Pick your Dormitorio switch", required: true
    input "presenceSwitchCloset", "capability.switch", title: "Pick your Closet switch", required: true    
    input "presenceSwitchBanop", "capability.switch", title: "Pick your Baño Principal switch", required: true
    input "presenceSwitchOutside", "capability.switch", title: "Pick your Outside switch", required: true
  }
  section("Shut-down triggering") {
    paragraph("Set the time to change to Outide")
    input "outsideDelay", "number", required: true, title: "Set outside delay time"
    paragraph("Set the modes in wich this can be triggered")
    input "outsideModes", "mode", title: "Select the allowed mode(s)", multiple: true, required: false
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
  subscribe(sensorEntrada, "motion", motionEntrada)
  subscribe(sensorLiving, "motion", motionLiving)
  subscribe(sensorCocina, "motion", motionCocina)
  subscribe(sensorPasillo, "motion", motionPasillo)
  subscribe(sensorEstudio, "motion", motionEstudio)
  subscribe(sensorComputador, "switch", motionEstudio)
  subscribe(sensorBanoi, "motion", motionBanoi)
  subscribe(sensorDormitorio, "motion", motionDormitorio)
  subscribe(sensorCloset, "motion", motionCloset)
  subscribe(sensorBanop, "motion", motionBanop)
  state.prev0 = 0;
  state.prev1 = 0;
}

def motionEvent(presence){
  def presenceSwitch = [presenceSwitchEntrada,
                        presenceSwitchLiving,
                        presenceSwitchCocina,
	                    presenceSwitchPasillo,
    	                presenceSwitchEstudio,
	                    presenceSwitchBanoi,
	                    presenceSwitchDormitorio,
	                    presenceSwitchCloset,
	                    presenceSwitchBanop,
	                    presenceSwitchOutside]  
  if(presence != state.prev0 || presenceSwitchOutside.currentSwitch == "on") {
  	log.debug("Changing presence to "+presence)
  	state.prev1 = state.prev0
  	state.prev0 = presence
  	presenceSwitch.each{
	  if (it.displayName != state.prev0 && it.displayName != state.prev1 && it.currentSwitch == "on") {
	    log.debug("Changing presence to "+presence+" OFF : "+it.displayName)
	    it.off()
	  }
	  if ((it.displayName == state.prev0 || it.displayName == state.prev1) && it.currentSwitch == "off") {
	    log.debug("Changing presence to "+presence+" ON  : "+it.displayName)
	    it.on()
	  }
  	}     
  }
  runIn(outsideDelay, motionOutside)
}

def motionEntrada(evt){
  if(evt.value == "active") {
    motionEvent("Presencia Entrada")
  }
}

def motionLiving(evt){
  if(evt.value == "active") {
    motionEvent("Presencia Living")
  }
}

def motionCocina(evt){
  if(evt.value == "active") {
    motionEvent("Presencia Cocina")
  }
}

def motionPasillo(evt) {
  if(evt.value == "active") {
    motionEvent("Presencia Pasillo")
  }
}

def motionEstudio(evt) {
  if(evt.value == "active" || evt.value == "on") {
    motionEvent("Presencia Estudio")
  }
}

def motionBanoi(evt){
  if(evt.value == "active") {
    motionEvent("Presencia Baño I.")
  }
}

def motionDormitorio(evt){
  if(evt.value == "active") {
    motionEvent("Presencia Dormitorio")
  }
}

def motionCloset(evt){
  if(evt.value == "active") {
    motionEvent("Presencia Closet")
  }
}

def motionBanop(evt) {
  if(evt.value == "active") {
    motionEvent("Presencia Baño P.")
  }
}

def motionOutside(evt) {
  def somebody = 0
  sensorPersonas.each{
    if(it.presence == "present"){
      somebody += 1
    }
  }
  if (presenceSwitchEntrada.currentSwitch == "on" && somebody == 0) {
    motionEvent("Presencia Outside")
  } else {
    runIn(outsideDelay, motionOutside)
  }
}
