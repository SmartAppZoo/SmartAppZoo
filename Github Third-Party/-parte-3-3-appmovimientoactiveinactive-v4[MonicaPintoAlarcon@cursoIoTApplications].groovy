/**
 *  AppMovimientoActiveInactive-v4
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
    name: "(Parte 3) 3. AppMovimientoActiveInactive-v4",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Esta ser\u00EDa la versi\u00F3n completa de la aplicaci\u00F3n",
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
	log.debug "motionDetectedHandler called: $evt"
    theswitch.on()
}

//La implementación del manejador de eventos es diferente en esta versión
def motionInactiveDetectedHandler(evt){
	log.debug "motionDetectedHandler called: $evt"
    //No apagamos la luz directamente
    //SmartThing esperará el número de minutos indicado y luego llamará al método checkMotion
    runIn(60*minutes,checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"

    // get the current state object for the motion sensor
    def motionState = themotion.currentState("motion")
    log.debug "motionState = $motionState"

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time
		log.debug "Elapsed time: $elapsed"
        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

            if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            theswitch.off()
            } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
            //Esta línea no viene en la solución que da SmartThings, pero sin esto la luz podría quedarse encendida para siempre
            //Si no ha pasado el tiempo suficiente, volvemos a comprobarlo pasado nuevamente el número de minutos indicado por el usuario
            runIn(60*minutes,checkMotion)
        }
    } else {
            // Motion active; just log it and do nothing
            log.debug "Motion is active, do nothing and wait for inactive"
    }
}