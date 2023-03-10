/**
 *  puestaSol
 *
 *  Copyright 2018 Arturo de Guzman Manzano
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
    name: "Ejercicio5-ArturoDeGuzman",
    namespace: "Ejercicio5-solucionAlumnos",
    author: "Arturo de Guzman Manzano",
    description: "Aplicacion donde las luces se apagan cuando sale el sol y se encienden cuando el sol se ha puesto y se detecta movimiento",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences{
    page(name: "paginaInicial",title: "Pagina inicial",
    	nextPage: "paginaDos",
    	install: false, uninstall: true){
        section("Selecciona el sensor de movimiento:") {
    		input "elSensorMov", "capability.motionSensor",
        	title: "Sensor de movimiento?", required: true, multiple: false
    	}
    	section("Selecciona la luz a encender/apagar:"){
    		input "elInterruptor", "capability.switch",
        	title: "Interruptor?", required: true, multiple: false
    	}
       
       section("Notificaciones:") 
        {
            input("tipoNotificacion","enum", options: ["push","sms","ambos"],
            title:"Seleccione tipo de notificacion")
        }
        
    }
	page(name:"paginaDos", title:"Tipo de notificacion al usuario", install:true, uninstall:true);
}

def paginaDos(){
	dynamicPage(name:"paginaDos")
    {
        if(tipoNotificacion == "sms" || tipoNotificacion == "1" || tipoNotificacion == "ambos" || tipoNotificacion == "2")
        {
            section()
            {
                input ("receptores", "phone", title:"Seleccione los contactos:")
            }
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
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(location, "sunset", sunsetHandler)
    subscribe(location, "sunrise", sunriseHandler)
    def sunriseSunset = getSunriseAndSunset()
    state.sunrise = sunriseSunset.sunrise.time
    log.debug "Hora de salida del sol: $sunriseSunset.sunrise"
    state.sunset = sunriseSunset.sunset.time
    log.debug "Hora de puesta del sol: $sunriseSunset.sunset"
    subscribe(elSensorMov, "motion.active", movimientoDetectadoManejador)
}

def sunsetHandler(evt) {
	log.debug "Sun has set!"

}

def sunriseHandler(evt) {
	log.debug "Sun has risen!"
    def sunriseSunset = getSunriseAndSunset()
    state.sunrise = sunriseSunset.sunrise.time
	state.sunset = sunriseSunset.sunset.time
}

def movimientoDetectadoManejador(evt) {
	log.debug "movimientoDetectadoManejador called: $evt"
    
    if (evt.value == 'active' && (now() > state.sunset))
    {
    	log.debug "La hora actual es superior a la hora de puesta de sol, se encienden las luces!"
    	elInterruptor.on()
    }
    else if (evt.value == 'active' && (now() > state.sunrise) && (now() < state.sunset))
    { 
    	log.debug "La hora actual esta entre la salida y la puesta del sol, se apagan las luces!"
    	elInterruptor.off()
    }
}