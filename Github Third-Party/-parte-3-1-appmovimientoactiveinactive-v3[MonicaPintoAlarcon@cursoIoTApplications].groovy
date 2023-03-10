/**
 *  AppMovimientoActiveInactive-v3
 *
 *  Copyright 2016 Monica Pinto
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
    name: "(Parte 3) 1. AppMovimientoActiveInactive-v3",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "En esta versi\u00F3n de la aplicaci\u00F3n la luz solo se apagar\u00E1 si el sensor de movimiento lleva inactivo un n\u00FAmero de minutos indicado por el usuario de la aplicaci\u00F3n durante su instalaci\u00F3n",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Listen to this motion detector:") {
        input "themotion", "capability.motionSensor", required: true, title: "When?"
    }
    section("Turn on this light") {
        input "theswitch", "capability.switch", required: true
    }
    //Nuevo de esta versión
    section("Minutes to wait before turning off the light"){
    	input "minutes", "number", required: true, title: "Minutes?"
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
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(themotion,"motion.active",motionActiveDetectedHandler)
    subscribe(themotion,"motion.inactive",motionInactiveDetectedHandler)
}

// TODO: implement event handlers
def motionActiveDetectedHandler(evt){
	log.debug "motionDetectedHandler called: $evt.value"
    theswitch.on()
}

//La implementación del manejador de eventos es diferente en esta versión
def motionInactiveDetectedHandler(evt){
	log.debug "motionDetectedHandler called: $evt.value"
    //No apagamos la luz directamente
    //SmartThing esperará el número de minutos indicado y luego llamará al método checkMotion
    log.debug "Tarea planificada ..."
    runIn(60*minutes,checkMotion)
    //runIn(60*minutes,checkMotion,[overwrite:false])
}

def checkMotion() {
    log.debug "Tarea ejecutada ..."
    theswitch.off()
}