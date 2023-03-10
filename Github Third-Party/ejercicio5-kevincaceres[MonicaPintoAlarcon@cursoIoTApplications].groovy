/**
 *  Ejercicio5
 *
 *  Copyright 2018 Kevin Caceres Luna
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
    name: "Ejercicio5-KevinCaceres",
    namespace: "Ejercicio5-solucionAlumnos",
    author: "Kevin Caceres Luna",
    description: "Ejercicio 5",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Selecciona el sensor de movimiento:") {
	    input "elSensorMov","capability.motionSensor",
		title: "Sensor Movimiento?", required: true, multiple: false
	}
	section("Selecciona la luz a encender/apagar:") {
		input "elInterruptor","capability.switch",
		title: "Interruptor?", required: true, multiple: false
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
    subscribe(location,"sunset",sunsetHandler)
    //subscribe(location,"sunrise",sunriseHandler)
    subscribe(elSensorMov, "motion", movimientoDetectadoManejador)
}
def sunriseHandler(evt){
	log.debug "Se puso el sol"
    elInterruptor.off()
}	
def sunsetHandler(evt){
	log.debug "El sol se escondio"
    
}

def movimientoDetectadoManejador(evt) {
	log.debug "movimientoDetectadoManejador called: $evt"
    def noParams = getSunriseAndSunset()
    log.debug "sunrise ${noParams.sunrise}"
    log.debug "sunset ${noParams.sunset}"
	log.debug "sunset ${noParams.sunset.time}"
    log.debug "now ${now()}"
    
    if(evt.value=='active')
    	if(now()>noParams.sunset.time){
    		elInterruptor.on()
            log.debug "Es de noche"
   		 }else{
         	log.debug "Es de dia"
         }
         
	else if (evt.value=='inactive')
    	elInterruptor.off()
	
    
}