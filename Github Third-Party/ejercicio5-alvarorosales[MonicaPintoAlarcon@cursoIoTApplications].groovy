/**
 *  Ejercicio5
 *
 *  Copyright 2018 Alvaro Rosales
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
    name: "Ejercicio5-AlvaroRosales",
    namespace: "Ejercicio5-solucionAlumnos",
    author: "Alvaro Rosales",
    description: "Ejercicio 5",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Página de instalación") {
		input "sensores", "capability.motionSensor", required:false, multiple:true
        input "interruptor","capability.switch", required:false
	}
}

def installed() {
	subscribe(location, "sunset", sunsetHandler)
    subscribe(location, "sunrise", sunriseHandler)
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {

}

def sunsetHandler(evt){
	log.debug"Se pone el Sol"
    sendPush("Se ha puesto el Sol")
    subscribe(sensores, "motion", encenderluces)
}

def sunriseHandler(evt){
	log.debut"Sale el Sol"
    sendPush("Ha salido el Sol")
    interruptor.off()
    unsubscribe(sensores)
}

def encenderluces(evt){
	log.debut"encenderluces called: $evt"
    if (evt.value=='active')
		Interruptor.on()
    else Interruptor.off()
}